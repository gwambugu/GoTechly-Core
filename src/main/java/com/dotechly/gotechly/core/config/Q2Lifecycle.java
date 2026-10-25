package com.dotechly.gotechly.core.config;

import org.jpos.q2.Q2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Spring-managed lifecycle for jPOS Q2.
 * <p>
 * Starts Q2 <b>after</b> {@link PluginLoader} has loaded all plugin JARs,
 * and injects a composite classloader into Q2's {@code mainClassLoader} field
 * via reflection so that Q2's internal {@code QClassLoader} (which uses
 * {@code mainClassLoader} as its parent) can find participant classes that
 * live inside plugin JARs.
 * <p>
 * This preserves class identity: both the plugin's {@code URLClassLoader} and
 * Q2's {@code QClassLoader} resolve plugin classes through the same loader,
 * avoiding {@code ClassCastException} issues.
 * <p>
 * <b>The composite must delegate to a live view of {@link PluginLoader}, never to a
 * snapshot.</b> Every hot-reload closes the old plugin {@code URLClassLoader} and
 * builds a new one, and a JAR dropped into {@code plugins/} after boot gets a
 * classloader that did not exist when Q2 started. A composite holding a boot-time
 * copy therefore ends up delegating to closed or superseded loaders, and the next
 * Q2 deploy of that plugin's XML fails with a bare
 * {@code ClassNotFoundException} on the channel/participant class — while the
 * plugin itself still reports a clean registration, because {@code PluginLoader}
 * resolved it through its own current loader. The two disagreeing is the tell.
 * <p>
 * For the same reason the composite is installed even when no plugin is loaded at
 * boot: an empty {@code plugins/} dir at startup must not permanently cut Q2 off
 * from every plugin added later.
 */
@Component
public class Q2Lifecycle implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(Q2Lifecycle.class);

    private final PluginLoader pluginLoader;

    @Value("${gotechly.q2.deploy-dir:deploy}")
    private String deployDir;

    private Q2 q2;
    private volatile boolean running;

    public Q2Lifecycle(PluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
    }

    @Override
    public void start() {
        // Always install the composite, even with zero plugins loaded right now: it reads
        // PluginLoader live on every lookup, so plugins loaded or hot-reloaded after Q2 has
        // started are picked up without rebuilding anything.
        ClassLoader compositeClassLoader =
                new CompositeClassLoader(getClass().getClassLoader(), pluginLoader);
        LOG.info("Building composite classloader for Q2 (live view of PluginLoader; {} plugin classloader(s) at Q2 start).",
                pluginLoader.getPluginClassLoaders().size());

        q2 = new Q2(deployDir);

        // Q2 assigns mainClassLoader = getClass().getClassLoader() in its CONSTRUCTOR, and
        // only builds QClassLoader(server, libDir, loaderName, mainClassLoader) later in run().
        // Injecting between 'new Q2(...)' and 'q2.start()' is therefore the one window where
        // the field is settable and still read afterwards -- do not move this.
        try {
            Field mainCLField = Q2.class.getDeclaredField("mainClassLoader");
            mainCLField.setAccessible(true);
            mainCLField.set(q2, compositeClassLoader);
            LOG.info("Injected composite classloader into Q2.mainClassLoader.");
        } catch (NoSuchFieldException e) {
            LOG.error("Q2.mainClassLoader field not found -- jPOS version may have changed. " +
                    "Fallback: place plugin JARs in deploy/lib/.", e);
        } catch (java.lang.reflect.InaccessibleObjectException | IllegalAccessException e) {
            LOG.error("Cannot reflectively access Q2.mainClassLoader. " +
                    "Add JVM arg: --add-opens org.jpos.jpos/org.jpos.q2=ALL-UNNAMED " +
                    "or place plugin JARs in deploy/lib/.", e);
        }

        q2.start();
        LOG.info("jPOS Q2 started with deploy dir '{}'.", deployDir);
        running = true;
    }

    @Override
    public void stop() {
        if (q2 != null) {
            LOG.info("Stopping jPOS Q2...");
            q2.shutdown();
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    /**
     * A classloader that first delegates to its parent (the app classloader), and if the
     * class is not found there, asks each currently-loaded plugin classloader in turn.
     * <p>
     * The delegate list is re-read from {@link PluginLoader} on <b>every</b> lookup and is
     * deliberately not cached. Holding a copy is the bug this class exists to avoid: a
     * hot-reload closes the old plugin {@code URLClassLoader} (a closed one just answers
     * {@code ClassNotFoundException} for everything, silently) and replaces it with an
     * instance a cached list would never see.
     */
    private static final class CompositeClassLoader extends ClassLoader {

        static {
            registerAsParallelCapable();
        }

        private final PluginLoader pluginLoader;

        CompositeClassLoader(ClassLoader parent, PluginLoader pluginLoader) {
            super(parent);
            this.pluginLoader = pluginLoader;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            for (ClassLoader delegate : pluginLoader.getPluginClassLoaders()) {
                try {
                    return delegate.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                    // not this plugin -- try the next
                }
            }
            throw new ClassNotFoundException(name);
        }

        @Override
        protected URL findResource(String name) {
            for (ClassLoader delegate : pluginLoader.getPluginClassLoaders()) {
                URL url = delegate.getResource(name);
                if (url != null) {
                    return url;
                }
            }
            return null;
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            List<URL> found = new ArrayList<>();
            for (ClassLoader delegate : pluginLoader.getPluginClassLoaders()) {
                for (URL url : Collections.list(delegate.getResources(name))) {
                    if (!found.contains(url)) {
                        found.add(url);
                    }
                }
            }
            return Collections.enumeration(found);
        }
    }
}
