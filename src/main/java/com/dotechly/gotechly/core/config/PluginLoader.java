package com.dotechly.gotechly.core.config;

import com.dotechly.gotechly.core.config.admin.SchemeEchoRegistry;
import com.dotechly.gotechly.core.config.inward.*;
import com.dotechly.gotechly.core.config.iso.Iso8583PluginRegistry;
import com.dotechly.gotechly.core.config.outward.*;
import com.dotechly.gotechly.core.config.plugin.PluginVerifier;
import com.dotechly.gotechly.core.pipeline.PipelineRegistry;
import com.dotechly.gotechly.core.pipeline.config.PipelineProperties;
import com.dotechly.gotechly.core.service.impl.AppConfigsService;
import com.dotechly.gotechly.core.service.impl.EndpointConfigsService;
import com.dotechly.gotechly.core.util.general.CustomHttpClient;
import com.dotechly.gotechly.spi.service.*;
import com.dotechly.gotechly.spi.service.admin.SchemeEchoHandler;
import com.dotechly.gotechly.spi.service.auth.OAuth2TokenProvider;
import com.dotechly.gotechly.spi.service.inward.*;
import com.dotechly.gotechly.spi.service.inward.linkedwallet.LinkedWalletBankAccountHandler;
import com.dotechly.gotechly.spi.service.iso.Iso8583Plugin;
import com.dotechly.gotechly.spi.service.iso.StanProvider;
import com.dotechly.gotechly.spi.service.outward.OutPaymentRequestProcessingHandler;
import com.dotechly.gotechly.spi.service.outward.OutResultRequestProcessingHandler;
import com.dotechly.gotechly.spi.service.outward.OutStatusRequestProcessingHandler;
import com.dotechly.gotechly.spi.service.outward.OutValidationRequestHandler;
import com.dotechly.gotechly.spi.service.outward.account.LinkedAccount;
import com.dotechly.gotechly.spi.service.outward.inquiry.DarajaInquiryHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Component
public class PluginLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);
    private static final String SHARED_CONFIG_FILE = "plugins-config.yml";
    private static final String PLUGIN_CONFIG_SUFFIX = "-plugin-config.yml";
    /**
     * Fixed name of a plugin's config when it ships <em>inside</em> the JAR (resources root).
     * Preferred over the external sibling {@code {paymentType}-plugin-config.yml}: it deploys
     * atomically with the JAR and is covered directly by the JAR signature (no SHA-256 manifest
     * claim needed). One file per JAR, so the name is not payment-type-qualified. Signature is
     * gated by {@link PluginVerifier#verifyJar(File)} before this is ever read.
     */
    private static final String IN_JAR_CONFIG_FILE = "plugin-config.yml";

    /** Debounce delay (ms) after a WatchService event before processing. */
    private static final long HOT_RELOAD_DEBOUNCE_MS = 2000;

    @Value("${gotechly.plugins.dir:plugins}")
    private String pluginDir;

    @Value("${gotechly.plugins.hot-reload:true}")
    private boolean hotReloadEnabled;

    /**
     * DB keys (full, including the "core." prefix) that must exist in app_configs
     * before any plugin loads. Empty by default. Configured via
     * {@code gotechly.plugins.required-global-configs} in application.yml.
     * Treated as a deployment-wide invariant: missing entries → refuse to start
     * Core. The override {@code GOTECHLY_PLUGIN_VERIFY=false} also skips this
     * check (same toggle that disables signature/manifest verification in dev).
     *
     * NOTE: Bound via {@link Binder} in {@code @PostConstruct} rather than
     * {@code @Value}, because {@code @Value} cannot resolve YAML list properties
     * (it expects a comma-separated string). Lazy-initialized to {@link List#of()}
     * so the field is never null even if binding is skipped.
     */
    private List<String> requiredGlobalConfigs = List.of();

    @Value("${gotechly.plugins.verification.enabled:true}")
    private boolean verificationEnabled;

    // Outward registries
    private final OutValidationHandlerRegistry outValidationRegistry;
    private final OutPaymentProcessingHandlerRegistry outProcessingRegistry;
    private final OutResultProcessingHandlerRegistry outResultRegistry;
    private final OutStatusProcessingHandlerRegistry outStatusRegistry;

    // Inward registries
    private final InValidationHandlerRegistry inValidationRegistry;
    private final InPaymentProcessingHandlerRegistry inProcessingRegistry;
    private final InResultProcessingHandlerRegistry inResultRegistry;
    private final InStatusProcessingHandlerRegistry inStatusRegistry;
    // Inward callback (report an inbound transaction's outcome back to the scheme) and inbound
    // credit-reversal capabilities — optional per plugin, used by asynchronous inbound corridors.
    private final InCallbackHandlerRegistry inCallbackRegistry;
    private final InCreditReversalHandlerRegistry inCreditReversalRegistry;

    // Daraja inquiry capability registry (optional per plugin)
    private final DarajaInquiryHandlerRegistry darajaInquiryRegistry;

    // Linked-wallet <-> bank-account capability registry (inbound category, optional per plugin)
    private final LinkedWalletBankAccountRegistry linkedWalletRegistry;

    // Account/alias (proxy) linkage capability registry (outbound category, optional per plugin)
    private final LinkedAccountRegistry linkedAccountRegistry;
    private final SchemeEchoRegistry schemeEchoRegistry;

    // ISO 8583 (TCP) capability registry — plugins serve over jPOS, registered here for
    // lifecycle and portal visibility only.
    private final Iso8583PluginRegistry iso8583PluginRegistry;

    private final AppConfigsService appConfigsService;
    private final EndpointConfigsService endpointConfigsService;
    private final CustomHttpClient customHttpClient;
    private final OAuth2TokenProvider oauth2TokenProvider;
    private final PluginVerifier pluginVerifier;

    /** Shared services available to all plugins */
    private final Map<Class<?>, Object> sharedServices = new HashMap<>();

    /** Fallback per-plugin configuration from shared plugins-config.yml */
    private Map<String, Map<String, String>> sharedPluginConfigs = Collections.emptyMap();

    private File pluginDirectory;

    /** Per-JAR tracking: jar absolute path -> loaded state */
    private final Map<String, LoadedPlugin> loadedPlugins = new ConcurrentHashMap<>();

    private final PipelineRegistry pipelineRegistry;
    private final ResponseCodeLookup responseCodeLookup;
    private final PaymentLogLookup paymentLogLookup;
    private final CachedReceiverNamesLookup cachedReceiverNamesLookup;
    private final Environment environment;

    /** Raw (un-flattened) YAML config per payment type, retained for pipeline parsing. */
    private Map<String, Map<String, Object>> rawPluginConfigs = new ConcurrentHashMap<>();

    /** WatchService thread handle */
    private Thread watchThread;
    private volatile boolean watcherRunning;

    /** Payment types that have been manually disabled via admin API */
    private final Set<String> disabledPaymentTypes = ConcurrentHashMap.newKeySet();

    /** Map from paymentType to the JAR path that provides it, for re-enable */
    private final Map<String, String> paymentTypeToJarPath = new ConcurrentHashMap<>();

    /** Config-only (composite) payment types — no JAR, only pipeline config with delegate stages */
    private final Map<String, ConfigOnlyPlugin> configOnlyPlugins = new ConcurrentHashMap<>();

    /** DE11 source handed to plugins that build ISO 8583 themselves (RNDPS). */
    private final StanProvider stan;
    private final ExecutePipeline executePipeline;//ExecutePipeLineInPluginImp

    private record ConfigOnlyPlugin(
            String configFileName,
            String paymentType,
            Map<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> pipelineConfig
    ) {}

    public PluginLoader(
            OutValidationHandlerRegistry outValidationRegistry,
            OutPaymentProcessingHandlerRegistry outProcessingRegistry,
            OutResultProcessingHandlerRegistry outResultRegistry,
            OutStatusProcessingHandlerRegistry outStatusRegistry,
            InValidationHandlerRegistry inValidationRegistry,
            InPaymentProcessingHandlerRegistry inProcessingRegistry,
            InResultProcessingHandlerRegistry inResultRegistry,
            InStatusProcessingHandlerRegistry inStatusRegistry,
            InCallbackHandlerRegistry inCallbackRegistry,
            InCreditReversalHandlerRegistry inCreditReversalRegistry,
            DarajaInquiryHandlerRegistry darajaInquiryRegistry,
            LinkedWalletBankAccountRegistry linkedWalletRegistry,
            LinkedAccountRegistry linkedAccountRegistry,
            SchemeEchoRegistry schemeEchoRegistry,
            Iso8583PluginRegistry iso8583PluginRegistry,
            AppConfigsService appConfigsService,
            EndpointConfigsService endpointConfigsService,
            PipelineRegistry pipelineRegistry,
            ResponseCodeLookup responseCodeLookup, PaymentLogLookup paymentLogLookup,
            CachedReceiverNamesLookup cachedReceiverNamesLookup,
            Environment environment,
            CustomHttpClient customHttpClient,
            OAuth2TokenProvider oauth2TokenProvider,
            PluginVerifier pluginVerifier,
            StanProvider stan,
            ExecutePipeline executePipeline) {
        this.outValidationRegistry = outValidationRegistry;
        this.outProcessingRegistry = outProcessingRegistry;
        this.outResultRegistry = outResultRegistry;
        this.outStatusRegistry = outStatusRegistry;
        this.inValidationRegistry = inValidationRegistry;
        this.inProcessingRegistry = inProcessingRegistry;
        this.inResultRegistry = inResultRegistry;
        this.inStatusRegistry = inStatusRegistry;
        this.inCallbackRegistry = inCallbackRegistry;
        this.inCreditReversalRegistry = inCreditReversalRegistry;
        this.darajaInquiryRegistry = darajaInquiryRegistry;
        this.linkedWalletRegistry = linkedWalletRegistry;
        this.linkedAccountRegistry = linkedAccountRegistry;
        this.schemeEchoRegistry = schemeEchoRegistry;
        this.iso8583PluginRegistry = iso8583PluginRegistry;
        this.appConfigsService = appConfigsService;
        this.endpointConfigsService = endpointConfigsService;
        this.pipelineRegistry = pipelineRegistry;
        this.responseCodeLookup = responseCodeLookup;
        this.paymentLogLookup = paymentLogLookup;
        this.cachedReceiverNamesLookup = cachedReceiverNamesLookup;
        this.environment = environment;
        this.customHttpClient = customHttpClient;
        this.oauth2TokenProvider = oauth2TokenProvider;
        this.pluginVerifier = pluginVerifier;
        this.stan = stan;
        this.executePipeline = executePipeline;
    }

    // =========================================================================
    // Internal tracking record for a loaded JAR
    // =========================================================================

    private record LoadedPlugin(
            File jarFile,
            URLClassLoader classLoader,
            // Outward handlers
            List<OutValidationRequestHandler> outValidationHandlers,
            List<OutPaymentRequestProcessingHandler> outProcessingHandlers,
            List<OutResultRequestProcessingHandler> outResultHandlers,
            List<OutStatusRequestProcessingHandler> outStatusHandlers,
            // Inward handlers
            List<InValidationRequestHandler> inValidationHandlers,
            List<InPaymentRequestProcessingHandler> inProcessingHandlers,
            List<InResultRequestProcessingHandler> inResultHandlers,
            List<InStatusRequestProcessingHandler> inStatusHandlers,
            // Inward callback (outcome reported back to the scheme) and inbound credit-reversal
            // handlers — optional, used by asynchronous inbound corridors such as R-NDPS pacs.008
            List<InwardCallback> inCallbackHandlers,
            List<InwardReversalResultHandler> inCreditReversalHandlers,
            // Daraja inquiry handlers (optional capability — typically only MPESA)
            List<DarajaInquiryHandler> darajaInquiryHandlers,
            // Linked-wallet <-> bank-account handlers (inbound category, optional — e.g. MKESHO)
            List<LinkedWalletBankAccountHandler> linkedWalletHandlers,
            // Account/alias (proxy) linkage handlers (outbound category, optional — e.g. RNDPS acmt)
            List<LinkedAccount> linkedAccountHandlers,
            // Scheme echo / heartbeat handlers (administrative, no pipeline — e.g. RNDPS admn.005)
            List<SchemeEchoHandler> schemeEchoHandlers,
            // ISO 8583 (TCP) capabilities — served by the plugin's own jPOS deploy XML
            List<Iso8583Plugin> iso8583Plugins,
            long loadedAtMillis
    ) {}

    // =========================================================================
    // Startup
    // =========================================================================

    @PostConstruct
    public void loadPlugins() {
        verifyRequiredGlobalConfigs();

        pluginDirectory = new File(pluginDir);

        if (!pluginDirectory.exists()) {
            LOG.warn("Plugin directory '{}' does not exist. No plugins will be loaded. Resolved path: {}",
                    pluginDir, pluginDirectory.getAbsolutePath());
            return;
        }

        if (!pluginDirectory.isDirectory()) {
            LOG.error("Plugin path '{}' exists but is not a directory. Resolved path: {}",
                    pluginDir, pluginDirectory.getAbsolutePath());
            return;
        }

        // Register shared services — use CustomHttpClient's pooled WebClient instead of a bare instance
        sharedServices.put(WebClient.class, customHttpClient.getWebClient());
        sharedServices.put(AppConfigsLookup.class, appConfigsService);
        sharedServices.put(EndPointConfigsLookup.class, endpointConfigsService);
        sharedServices.put(ResponseCodeLookup.class, responseCodeLookup);
        sharedServices.put(PaymentLogLookup.class, paymentLogLookup);
        sharedServices.put(CachedReceiverNamesLookup.class, cachedReceiverNamesLookup);
        sharedServices.put(OAuth2TokenProvider.class, oauth2TokenProvider);
        sharedServices.put(StanProvider.class, stan);
        sharedServices.put(ExecutePipeline.class, executePipeline);

        LOG.info("Shared services registered: {}", sharedServices.keySet());

        // Load shared config as fallback
        sharedPluginConfigs = loadSharedConfig();

        // Load all existing JARs (signature verification happens inside loadPluginJar)
        File[] jars = pluginDirectory.listFiles(f -> f.getName().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            LOG.info("Plugin directory '{}' contains no JAR files.", pluginDirectory.getAbsolutePath());
        } else {
            LOG.info("Found {} plugin JAR(s) in '{}'", jars.length, pluginDirectory.getAbsolutePath());
            for (File jar : jars) {
                loadPluginJar(jar);
            }
        }

        // Load config-only (composite) payment types — these have no JAR, only pipeline config
        loadConfigOnlyPaymentTypes();

        logRegistrySummary();

        // Start hot-reload watcher
        if (hotReloadEnabled) {
            startWatcher();
        } else {
            LOG.info("Plugin hot-reload is disabled.");
        }
    }

    /**
     * Validates that every key listed in
     * {@code gotechly.plugins.required-global-configs} exists in {@code app_configs}.
     * These are deployment-wide invariants used by every plugin (e.g. mTLS
     * keystore credentials for plugin → Core callbacks). If any are missing
     * we refuse to start: starting Core with absent globals would let plugins
     * load and fail later inside payment flow, which is much harder to debug.
     *
     * Skipped when {@code gotechly.plugins.verification.enabled=false} (same
     * dev toggle that disables JAR signature checks).
     */
    private void verifyRequiredGlobalConfigs() {
        // Bind the YAML list via Binder — @Value cannot read YAML arrays (it
        // splits comma-separated strings only), which is why this preflight
        // was previously a silent no-op. Bind once here lazily.
        this.requiredGlobalConfigs = Binder.get(environment)
                .bind("gotechly.plugins.required-global-configs", Bindable.listOf(String.class))
                .orElse(List.of());

        if (requiredGlobalConfigs.isEmpty()) {
            LOG.info("No gotechly.plugins.required-global-configs declared — global preflight skipped.");
            return;
        }

        LOG.info("Required global AppConfigs preflight: checking {} key(s): {}",
                requiredGlobalConfigs.size(), requiredGlobalConfigs);

        if (!verificationEnabled) {
            LOG.warn("Plugin verification disabled — skipping required-global-configs preflight. " +
                    "Missing rows will surface as runtime failures inside plugins.");
            return;
        }

        List<String> missing = new ArrayList<>();
        for (String key : requiredGlobalConfigs) {
            if (key == null || key.isBlank()) continue;
            if (appConfigsService.findByConfigId(key.trim()).isEmpty()) {
                missing.add(key.trim());
            }
        }

        if (!missing.isEmpty()) {
            String msg = "Required global app_configs rows are missing: " + missing
                    + ". Populate them (config_id values exact, including the 'core.' prefix) "
                    + "or remove from gotechly.plugins.required-global-configs. "
                    + "Override for dev: GOTECHLY_PLUGIN_VERIFY=false.";
            LOG.error("PLUGIN_GLOBAL_CONFIG_MISSING {}", msg);
            throw new IllegalStateException(msg);
        }
        LOG.info("Required global AppConfigs preflight passed: {} key(s) present.", requiredGlobalConfigs.size());
    }

    // =========================================================================
    // Shutdown
    // =========================================================================

    @PreDestroy
    public void shutdown() {
        LOG.info("Shutting down PluginLoader...");

        // Stop watcher thread
        watcherRunning = false;
        if (watchThread != null) {
            watchThread.interrupt();
            try {
                watchThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Unload all plugins
        for (String jarPath : new ArrayList<>(loadedPlugins.keySet())) {
            unloadPluginJar(jarPath);
        }

        LOG.info("PluginLoader shutdown complete.");
    }

    // =========================================================================
    // Load a single plugin JAR
    // =========================================================================

    private synchronized void loadPluginJar(File jar) {
        String jarPath = jar.getAbsolutePath();

        // Verify signature + register in-JAR manifest claims BEFORE touching registry state.
        // If rejected, the existing loaded version (if any) keeps running untouched.
        String verifyReason = pluginVerifier.verifyJar(jar);
        if (verifyReason != null) {
            quarantine(jar, verifyReason);
            return;
        }

        // The AppConfigs cache is in-memory and per-JVM: a config added via the admin UI/API on
        // another Core instance does NOT evict this instance's cache. Both the preflight below and
        // createContextForPlugin() read config through appConfigsByPrefix, so without this a redeploy
        // here would keep failing on a stale (empty) cached view until Core restarts. Evict before
        // (re)load so this instance reads ground truth from the DB.
        appConfigsService.evictCaches();

        // Preflight: required AppConfigs declared in the (now-verified) signed manifest
        // must all be present in the DB before we touch the classloader. Missing entries
        // → quarantine to *.MISCONFIGURED so ops sees a filesystem signal in addition to logs.
        String configReason = pluginVerifier.verifyRequiredConfigs(jar, appConfigsService);
        if (configReason != null) {
            quarantineMisconfigured(jar, configReason);
            return;
        }

        LOG.info("Loading plugin JAR: {}", jar.getName());

        // If already loaded (hot-reload case), unload first
        if (loadedPlugins.containsKey(jarPath)) {
            LOG.info("JAR '{}' is already loaded. Unloading before reload.", jar.getName());
            unloadPluginJar(jarPath);
        }

        try {
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jar.toURI().toURL()},
                    getClass().getClassLoader()
            );

            // Read the plugin's own config bundled inside the (already signature-verified) JAR,
            // once per load — threaded into each handler's PluginContext below. Null if the JAR
            // ships no in-JAR config, in which case createContextForPlugin falls back to the
            // external sibling '{paymentType}-plugin-config.yml' / shared config as before.
            Map<String, Object> inJarConfig = readInJarConfig(jar);

            // Outward handler lists
            List<OutValidationRequestHandler> outValHandlers = new ArrayList<>();
            List<OutPaymentRequestProcessingHandler> outProcHandlers = new ArrayList<>();
            List<OutResultRequestProcessingHandler> outResHandlers = new ArrayList<>();
            List<OutStatusRequestProcessingHandler> outStatHandlers = new ArrayList<>();

            // Inward handler lists
            List<InValidationRequestHandler> inValHandlers = new ArrayList<>();
            List<InPaymentRequestProcessingHandler> inProcHandlers = new ArrayList<>();
            List<InResultRequestProcessingHandler> inResHandlers = new ArrayList<>();
            List<InStatusRequestProcessingHandler> inStatHandlers = new ArrayList<>();
            List<InwardCallback> inCallbackHandlers = new ArrayList<>();
            List<InwardReversalResultHandler> inCreditReversalHandlers = new ArrayList<>();
            List<DarajaInquiryHandler> darajaInquiryHandlers = new ArrayList<>();
            List<LinkedWalletBankAccountHandler> linkedWalletHandlers = new ArrayList<>();
            List<LinkedAccount> linkedAccountHandlers = new ArrayList<>();
            List<SchemeEchoHandler> schemeEchoHandlers = new ArrayList<>();
            List<Iso8583Plugin> iso8583Plugins = new ArrayList<>();

            // --- Outward handlers ---

            for (OutValidationRequestHandler handler :
                    ServiceLoader.load(OutValidationRequestHandler.class, classLoader)) {
                String pt = handler.paymentType();
                handler.init(createContextForPlugin(pt, inJarConfig));
                outValidationRegistry.register(pt, handler);
                outValHandlers.add(handler);
                LOG.info("  Registered outward validation handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            for (OutPaymentRequestProcessingHandler handler :
                    ServiceLoader.load(OutPaymentRequestProcessingHandler.class, classLoader)) {
                String pt = handler.paymentType();
                handler.init(createContextForPlugin(pt, inJarConfig));
                outProcessingRegistry.register(pt, handler);
                outProcHandlers.add(handler);
                LOG.info("  Registered outward payment processing handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            for (OutResultRequestProcessingHandler handler :
                    ServiceLoader.load(OutResultRequestProcessingHandler.class, classLoader)) {
                String pt = handler.paymentType();
                handler.init(createContextForPlugin(pt, inJarConfig));
                outResultRegistry.register(pt, handler);
                outResHandlers.add(handler);
                LOG.info("  Registered outward result processing handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            for (OutStatusRequestProcessingHandler handler :
                    ServiceLoader.load(OutStatusRequestProcessingHandler.class, classLoader)) {
                String pt = handler.paymentType();
                handler.init(createContextForPlugin(pt, inJarConfig));
                outStatusRegistry.register(pt, handler);
                outStatHandlers.add(handler);
                LOG.info("  Registered outward status processing handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            // --- Inward handlers ---

            for (InValidationRequestHandler handler :
                    ServiceLoader.load(InValidationRequestHandler.class, classLoader)) {
                String pt = handler.paymentType();
                handler.init(createContextForPlugin(pt, inJarConfig));
                inValidationRegistry.register(pt, handler);
                inValHandlers.add(handler);
                LOG.info("  Registered inward validation handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            for (InPaymentRequestProcessingHandler handler :
                    ServiceLoader.load(InPaymentRequestProcessingHandler.class, classLoader)) {
                String pt = handler.paymentType();
                handler.init(createContextForPlugin(pt, inJarConfig));
                inProcessingRegistry.register(pt, handler);
                inProcHandlers.add(handler);
                LOG.info("  Registered inward payment processing handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            for (InResultRequestProcessingHandler handler :
                    ServiceLoader.load(InResultRequestProcessingHandler.class, classLoader)) {
                String pt = handler.paymentType();
                handler.init(createContextForPlugin(pt, inJarConfig));
                inResultRegistry.register(pt, handler);
                inResHandlers.add(handler);
                LOG.info("  Registered inward result processing handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            for (InStatusRequestProcessingHandler handler :
                    ServiceLoader.load(InStatusRequestProcessingHandler.class, classLoader)) {
                String pt = handler.paymentType();
                handler.init(createContextForPlugin(pt, inJarConfig));
                inStatusRegistry.register(pt, handler);
                inStatHandlers.add(handler);
                LOG.info("  Registered inward status processing handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            for (InwardCallback handler :
                    ServiceLoader.load(InwardCallback.class, classLoader)) {
                String pt = handler.paymentType();
                handler.init(createContextForPlugin(pt, inJarConfig));
                inCallbackRegistry.register(pt, handler);
                inCallbackHandlers.add(handler);
                LOG.info("  Registered inward callback handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            for (InwardReversalResultHandler handler :
                    ServiceLoader.load(InwardReversalResultHandler.class, classLoader)) {
                String pt = handler.paymentType();
                handler.init(createContextForPlugin(pt, inJarConfig));
                inCreditReversalRegistry.register(pt, handler);
                inCreditReversalHandlers.add(handler);
                LOG.info("  Registered inward credit-reversal handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            // --- Optional capabilities ---

            for (DarajaInquiryHandler handler :
                    ServiceLoader.load(DarajaInquiryHandler.class, classLoader)) {
                String pt = handler.paymentType();
                handler.init(createContextForPlugin(pt, inJarConfig));
                darajaInquiryRegistry.register(pt, handler);
                darajaInquiryHandlers.add(handler);
                LOG.info("  Registered Daraja inquiry handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            for (LinkedWalletBankAccountHandler handler :
                    ServiceLoader.load(LinkedWalletBankAccountHandler.class, classLoader)) {
                String pt = handler.paymentType();
                if (pt == null || pt.isBlank()) {
                    LOG.error("  Skipping linked-wallet handler class='{}': paymentType() is blank",
                            handler.getClass().getName());
                    continue;
                }
                handler.init(createContextForPlugin(pt, inJarConfig));
                linkedWalletRegistry.register(pt, handler);
                linkedWalletHandlers.add(handler);
                LOG.info("  Registered linked-wallet handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            for (LinkedAccount handler :
                    ServiceLoader.load(LinkedAccount.class, classLoader)) {
                String pt = handler.paymentType();
                if (pt == null || pt.isBlank()) {
                    LOG.error("  Skipping linked-account handler class='{}': paymentType() is blank",
                            handler.getClass().getName());
                    continue;
                }
                handler.init(createContextForPlugin(pt, inJarConfig));
                linkedAccountRegistry.register(pt, handler);
                linkedAccountHandlers.add(handler);
                LOG.info("  Registered linked-account handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            // --- Scheme echo / heartbeat ---
            // Administrative liveness, not a payment phase: no PaymentLogSPI, no PipelineContext,
            // no stage list. Same registration ceremony as every other capability.

            for (SchemeEchoHandler handler :
                    ServiceLoader.load(SchemeEchoHandler.class, classLoader)) {
                String pt = handler.paymentType();
                if (pt == null || pt.isBlank()) {
                    LOG.error("  Skipping scheme-echo handler class='{}': paymentType() is blank",
                            handler.getClass().getName());
                    continue;
                }
                handler.init(createContextForPlugin(pt, inJarConfig));
                schemeEchoRegistry.register(pt, handler);
                schemeEchoHandlers.add(handler);
                LOG.info("  Registered scheme-echo handler: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            // --- ISO 8583 (TCP) capabilities ---
            // These serve inbound requests over the plugin's own jPOS deploy XML, not REST. Core
            // only injects dependencies via init() and tracks them for portal visibility and
            // hot-reload; the plugin's init() publishes what its jPOS components need into
            // NameRegistrar, and its destroy() takes it back out.

            for (Iso8583Plugin handler :
                    ServiceLoader.load(Iso8583Plugin.class, classLoader)) {
                String pt = handler.paymentType();
                String invalid = validateIso(pt);
                if (invalid != null) {
                    LOG.error("  PLUGIN_ISO_INVALID Skipping ISO plugin class='{}': {}",
                            handler.getClass().getName(), invalid);
                    continue;
                }
                handler.init(createContextForPlugin(pt, inJarConfig));
                iso8583PluginRegistry.register(pt, handler);
                iso8583Plugins.add(handler);
                LOG.info("  Registered ISO 8583 plugin: paymentType='{}', class='{}', version='{}'",
                        pt, handler.getClass().getName(), handler.version());
            }

            int totalHandlers = outValHandlers.size() + outProcHandlers.size() + outResHandlers.size()
                    + outStatHandlers.size()
                    + inValHandlers.size() + inProcHandlers.size() + inResHandlers.size()
                    + inStatHandlers.size()
                    + inCallbackHandlers.size()
                    + inCreditReversalHandlers.size()
                    + darajaInquiryHandlers.size()
                    + linkedWalletHandlers.size()
                    + linkedAccountHandlers.size()
                    + iso8583Plugins.size();
            if (totalHandlers == 0) {
                LOG.warn("  No handler implementations found in '{}'. " +
                                "Ensure META-INF/services files exist in the JAR. Closing ClassLoader.",
                        jar.getName());
                classLoader.close();
                return;
            }

            loadedPlugins.put(jarPath, new LoadedPlugin(
                    jar, classLoader,
                    List.copyOf(outValHandlers),
                    List.copyOf(outProcHandlers),
                    List.copyOf(outResHandlers),
                    List.copyOf(outStatHandlers),
                    List.copyOf(inValHandlers),
                    List.copyOf(inProcHandlers),
                    List.copyOf(inResHandlers),
                    List.copyOf(inStatHandlers),
                    List.copyOf(inCallbackHandlers),
                    List.copyOf(inCreditReversalHandlers),
                    List.copyOf(darajaInquiryHandlers),
                    List.copyOf(linkedWalletHandlers),
                    List.copyOf(linkedAccountHandlers),
                    List.copyOf(schemeEchoHandlers),
                    List.copyOf(iso8583Plugins),
                    System.currentTimeMillis()
            ));

            // Track paymentType -> JAR path mapping for admin enable/disable
            Set<String> pts = new LinkedHashSet<>();
            outValHandlers.forEach(h -> pts.add(h.paymentType()));
            outProcHandlers.forEach(h -> pts.add(h.paymentType()));
            outResHandlers.forEach(h -> pts.add(h.paymentType()));
            outStatHandlers.forEach(h -> pts.add(h.paymentType()));
            inValHandlers.forEach(h -> pts.add(h.paymentType()));
            inProcHandlers.forEach(h -> pts.add(h.paymentType()));
            inResHandlers.forEach(h -> pts.add(h.paymentType()));
            inStatHandlers.forEach(h -> pts.add(h.paymentType()));
            inCallbackHandlers.forEach(h -> pts.add(h.paymentType()));
            inCreditReversalHandlers.forEach(h -> pts.add(h.paymentType()));
            darajaInquiryHandlers.forEach(h -> pts.add(h.paymentType()));
            linkedWalletHandlers.forEach(h -> pts.add(h.paymentType()));
            iso8583Plugins.forEach(h -> pts.add(h.paymentType()));
            for (String pt : pts) {
                paymentTypeToJarPath.put(pt, jarPath);
            }

            // Register plugin-provided pipeline config with PipelineRegistry
            for (String pt : pts) {
                Map<String, Object> rawConfig = rawPluginConfigs.get(pt);
                if (rawConfig == null) continue;
                Map<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> pipelineCfg =
                        parsePipelineConfig(pt, rawConfig);
                if (pipelineCfg != null) {
                    pipelineRegistry.registerPaymentTypePipeline(pt, pipelineCfg);
                    LOG.info("  Registered plugin pipeline config for paymentType='{}': {}",
                            pt, pipelineCfg.keySet());
                }
            }

        } catch (Exception e) {
            LOG.error("Failed to load plugin JAR '{}': {}", jar.getName(), e.getMessage(), e);
        }
    }

    // =========================================================================
    // Unload a single plugin JAR
    // =========================================================================

    private synchronized void unloadPluginJar(String jarPath) {
        LoadedPlugin plugin = loadedPlugins.remove(jarPath);
        if (plugin == null) {
            LOG.warn("No loaded plugin found for path: {}", jarPath);
            return;
        }

        LOG.info("Unloading plugin JAR: {}", plugin.jarFile().getName());

        // 1. Call destroy() on all handlers before closing ClassLoader

        // Outward
        for (OutValidationRequestHandler h : plugin.outValidationHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "outward-validation", h::destroy);
        }
        for (OutPaymentRequestProcessingHandler h : plugin.outProcessingHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "outward-payment-processing", h::destroy);
        }
        for (OutResultRequestProcessingHandler h : plugin.outResultHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "outward-result-processing", h::destroy);
        }
        for (OutStatusRequestProcessingHandler h : plugin.outStatusHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "outward-status-processing", h::destroy);
        }

        // Inward
        for (InValidationRequestHandler h : plugin.inValidationHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "inward-validation", h::destroy);
        }
        for (InPaymentRequestProcessingHandler h : plugin.inProcessingHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "inward-payment-processing", h::destroy);
        }
        for (InResultRequestProcessingHandler h : plugin.inResultHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "inward-result-processing", h::destroy);
        }
        for (InStatusRequestProcessingHandler h : plugin.inStatusHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "inward-status-processing", h::destroy);
        }
        for (InwardCallback h : plugin.inCallbackHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "inward-callback", h::destroy);
        }
        for (InwardReversalResultHandler h : plugin.inCreditReversalHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "inward-credit-reversal", h::destroy);
        }

        // Optional capabilities
        for (DarajaInquiryHandler h : plugin.darajaInquiryHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "daraja-inquiry", h::destroy);
        }
        for (LinkedWalletBankAccountHandler h : plugin.linkedWalletHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "linked-wallet", h::destroy);
        }
        for (LinkedAccount h : plugin.linkedAccountHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "linked-account", h::destroy);
        }
        for (SchemeEchoHandler h : plugin.schemeEchoHandlers()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "scheme-echo", h::destroy);
        }
        // ISO plugins clean up their own NameRegistrar entries in destroy(). A reload runs this
        // unload fully before the new JAR's init() re-publishes, so the new entries survive; do not
        // reorder unload after load or the old plugin's destroy() will wipe the new context.
        for (Iso8583Plugin h : plugin.iso8583Plugins()) {
            destroySafely(h.paymentType(), h.getClass().getName(), "iso", h::destroy);
        }

        // 2. Deregister from all registries

        // Outward
        for (OutValidationRequestHandler h : plugin.outValidationHandlers()) {
            outValidationRegistry.unregister(h.paymentType());
        }
        for (OutPaymentRequestProcessingHandler h : plugin.outProcessingHandlers()) {
            outProcessingRegistry.unregister(h.paymentType());
        }
        for (OutResultRequestProcessingHandler h : plugin.outResultHandlers()) {
            outResultRegistry.unregister(h.paymentType());
        }
        for (OutStatusRequestProcessingHandler h : plugin.outStatusHandlers()) {
            outStatusRegistry.unregister(h.paymentType());
        }

        // Inward
        for (InValidationRequestHandler h : plugin.inValidationHandlers()) {
            inValidationRegistry.unregister(h.paymentType());
        }
        for (InPaymentRequestProcessingHandler h : plugin.inProcessingHandlers()) {
            inProcessingRegistry.unregister(h.paymentType());
        }
        for (InResultRequestProcessingHandler h : plugin.inResultHandlers()) {
            inResultRegistry.unregister(h.paymentType());
        }
        for (InStatusRequestProcessingHandler h : plugin.inStatusHandlers()) {
            inStatusRegistry.unregister(h.paymentType());
        }
        for (InwardCallback h : plugin.inCallbackHandlers()) {
            inCallbackRegistry.unregister(h.paymentType());
        }
        for (InwardReversalResultHandler h : plugin.inCreditReversalHandlers()) {
            inCreditReversalRegistry.unregister(h.paymentType());
        }
        for (DarajaInquiryHandler h : plugin.darajaInquiryHandlers()) {
            darajaInquiryRegistry.unregister(h.paymentType());
        }
        for (LinkedWalletBankAccountHandler h : plugin.linkedWalletHandlers()) {
            linkedWalletRegistry.unregister(h.paymentType());
        }
        for (LinkedAccount h : plugin.linkedAccountHandlers()) {
            linkedAccountRegistry.unregister(h.paymentType());
        }
        for (SchemeEchoHandler h : plugin.schemeEchoHandlers()) {
            schemeEchoRegistry.unregister(h.paymentType());
        }
        for (Iso8583Plugin h : plugin.iso8583Plugins()) {
            iso8583PluginRegistry.unregister(h.paymentType());
        }

        // 3. Deregister plugin pipeline configs
        Set<String> unloadPts = new LinkedHashSet<>();
        plugin.outValidationHandlers().forEach(h -> unloadPts.add(h.paymentType()));
        plugin.outProcessingHandlers().forEach(h -> unloadPts.add(h.paymentType()));
        plugin.outResultHandlers().forEach(h -> unloadPts.add(h.paymentType()));
        plugin.outStatusHandlers().forEach(h -> unloadPts.add(h.paymentType()));
        plugin.inValidationHandlers().forEach(h -> unloadPts.add(h.paymentType()));
        plugin.inProcessingHandlers().forEach(h -> unloadPts.add(h.paymentType()));
        plugin.inResultHandlers().forEach(h -> unloadPts.add(h.paymentType()));
        plugin.inStatusHandlers().forEach(h -> unloadPts.add(h.paymentType()));
        plugin.inCallbackHandlers().forEach(h -> unloadPts.add(h.paymentType()));
        plugin.inCreditReversalHandlers().forEach(h -> unloadPts.add(h.paymentType()));
        plugin.darajaInquiryHandlers().forEach(h -> unloadPts.add(h.paymentType()));
        plugin.linkedWalletHandlers().forEach(h -> unloadPts.add(h.paymentType()));
        for (String pt : unloadPts) {
            pipelineRegistry.deregisterPaymentTypePipeline(pt);
            // Cancel any scheduled token refreshes owned by this paymentType so they don't
            // run with stale credentials after the JAR reloads with new config (see
            // OAuth2TokenProvider hot-reload contract).
            try {
                oauth2TokenProvider.unregisterProfile(pt);
            } catch (Exception e) {
                LOG.warn("  Failed to unregister token profile for '{}': {}", pt, e.getMessage());
            }
        }

        // 4. Close the ClassLoader (releases file lock on JAR)
        try {
            plugin.classLoader().close();
            LOG.info("  Closed ClassLoader for JAR: {}", plugin.jarFile().getName());
        } catch (IOException e) {
            LOG.error("  Failed to close ClassLoader for '{}': {}",
                    plugin.jarFile().getName(), e.getMessage(), e);
        }
    }

    /**
     * Validates an ISO 8583 plugin's parameters. Returns an error description if invalid, or
     * {@code null} if the plugin is acceptable.
     */
    private String validateIso(String paymentType) {
        if (paymentType == null || paymentType.isBlank()) {
            return "paymentType() is blank";
        }
        return null;
    }

    private void destroySafely(String paymentType, String className, String handlerType, Runnable destroyAction) {
        try {
            destroyAction.run();
            LOG.info("  Destroyed {} handler: paymentType='{}', class='{}'",
                    handlerType, paymentType, className);
        } catch (Exception e) {
            LOG.error("  Error destroying {} handler '{}': {}",
                    handlerType, className, e.getMessage(), e);
        }
    }

    // =========================================================================
    // WatchService for Hot-Reload
    // =========================================================================

    private void startWatcher() {
        watcherRunning = true;
        watchThread = new Thread(() -> {
            LOG.info("Plugin hot-reload watcher started for: {}", pluginDirectory.getAbsolutePath());
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                pluginDirectory.toPath().register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);

                while (watcherRunning) {
                    WatchKey key;
                    try {
                        key = watchService.poll(1, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (key == null) continue;

                    // Collect events, then debounce
                    Set<String> createdOrModified = new LinkedHashSet<>();
                    Set<String> deleted = new LinkedHashSet<>();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                        Path filename = (Path) event.context();
                        if (filename == null || !filename.toString().endsWith(".jar")) continue;

                        File jarFile = new File(pluginDirectory, filename.toString());
                        String absPath = jarFile.getAbsolutePath();

                        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            deleted.add(absPath);
                            createdOrModified.remove(absPath);
                        } else {
                            createdOrModified.add(absPath);
                            deleted.remove(absPath);
                        }
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        LOG.warn("Plugin directory watch key is no longer valid. Stopping watcher.");
                        break;
                    }

                    if (createdOrModified.isEmpty() && deleted.isEmpty()) continue;

                    // Debounce: wait for file copy to complete
                    LOG.info("Hot-reload: detected changes. Waiting {}ms for file operations to complete...",
                            HOT_RELOAD_DEBOUNCE_MS);
                    try {
                        Thread.sleep(HOT_RELOAD_DEBOUNCE_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    // Process deletions
                    for (String jp : deleted) {
                        // WatchService ENTRY_DELETE events are unreliable: an NFS/SMB plugins dir,
                        // an atomic rename-over deploy (mv new.jar foo.jar), or an AV/backup scanner
                        // all emit spurious DELETEs for JARs that are still on disk (this happens on
                        // a private per-instance dir too, not only a shared one). The matching CREATE
                        // can also be coalesced or arrive a poll cycle later. If we unloaded on the
                        // raw event we would deregister a live plugin and never reload it (no .jar
                        // CREATE follows), silently emptying the registry on this instance while the
                        // file — and the other instances — are fine. Re-check existence after the
                        // debounce and only unload if the JAR is truly gone; otherwise leave the
                        // loaded plugin in place (a real byte change surfaces as a separate MODIFY
                        // event and is handled by the reload branch below).
                        if (new File(jp).exists()) {
                            LOG.warn("Hot-reload: ignoring DELETE for '{}' — JAR still present on disk "
                                    + "(spurious event or atomic replace). Not unloading.", jp);
                            continue;
                        }
                        LOG.info("Hot-reload: JAR removed: {}", jp);
                        unloadPluginJar(jp);
                    }

                    // Process creates/modifications (reload)
                    for (String jp : createdOrModified) {
                        File jarFile = new File(jp);
                        if (jarFile.exists()) {
                            LOG.info("Hot-reload: JAR added/modified: {}", jarFile.getName());
                            // Reload shared config in case it changed alongside the JAR
                            sharedPluginConfigs = loadSharedConfig();
                            loadPluginJar(jarFile);
                        }
                    }

                    logRegistrySummary();
                }
            } catch (IOException e) {
                LOG.error("Plugin hot-reload watcher failed: {}", e.getMessage(), e);
            }
            LOG.info("Plugin hot-reload watcher stopped.");
        }, "plugin-hot-reload-watcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    // =========================================================================
    // Admin / introspection
    // =========================================================================

    /**
     * Returns a snapshot of all loaded plugin metadata.
     * Used by the admin REST endpoint.
     */
    public List<PluginInfo> getLoadedPluginInfos() {
        List<PluginInfo> result = new ArrayList<>();

        for (LoadedPlugin plugin : loadedPlugins.values()) {
            Set<String> paymentTypes = new LinkedHashSet<>();
            List<PluginInfo.HandlerInfo> handlers = new ArrayList<>();

            // Outward handlers
            for (OutValidationRequestHandler h : plugin.outValidationHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "outward-validation", h.version(), h.mode()));
            }
            for (OutPaymentRequestProcessingHandler h : plugin.outProcessingHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "outward-payment-processing", h.version(), h.mode()));
            }
            for (OutResultRequestProcessingHandler h : plugin.outResultHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "outward-result-processing", h.version(), h.mode()));
            }
            for (OutStatusRequestProcessingHandler h : plugin.outStatusHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "outward-status-processing", h.version(), h.mode()));
            }

            // Inward handlers
            for (InValidationRequestHandler h : plugin.inValidationHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "inward-validation", h.version(), h.mode()));
            }
            for (InPaymentRequestProcessingHandler h : plugin.inProcessingHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "inward-payment-processing", h.version(), h.mode()));
            }
            for (InResultRequestProcessingHandler h : plugin.inResultHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "inward-result-processing", h.version(), h.mode()));
            }
            for (InStatusRequestProcessingHandler h : plugin.inStatusHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "inward-status-processing", h.version(), h.mode()));
            }
            for (InwardCallback h : plugin.inCallbackHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "inward-callback", h.version(), h.mode()));
            }
            for (InwardReversalResultHandler h : plugin.inCreditReversalHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "inward-credit-reversal", h.version(), h.mode()));
            }

            // Linked-wallet <-> bank-account handlers (inbound category)
            for (LinkedWalletBankAccountHandler h : plugin.linkedWalletHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "linked-wallet", h.version(), "INWARD"));
            }

            // Account/alias (proxy) linkage handlers (outbound category)
            for (LinkedAccount h : plugin.linkedAccountHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "linked-account", h.version(), "OUTWARD"));
            }

            // Scheme echo / heartbeat handlers — administrative, neither inward nor outward
            for (SchemeEchoHandler h : plugin.schemeEchoHandlers()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "scheme-echo", h.version(), "ADMIN"));
            }

            // ISO 8583 (TCP) capabilities — no inward/outward mode
            for (Iso8583Plugin h : plugin.iso8583Plugins()) {
                paymentTypes.add(h.paymentType());
                handlers.add(new PluginInfo.HandlerInfo(
                        h.paymentType(), h.getClass().getName(),
                        "iso", h.version(), "ISO"));
            }

            result.add(new PluginInfo(
                    plugin.jarFile().getName(),
                    Set.copyOf(paymentTypes),
                    List.copyOf(handlers)));
        }

        // Include config-only (composite) payment types
        for (ConfigOnlyPlugin cop : configOnlyPlugins.values()) {
            List<PluginInfo.HandlerInfo> handlers = new ArrayList<>();

            // Build handler info from pipeline config — show delegated stages
            for (Map.Entry<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> dirEntry :
                    cop.pipelineConfig().entrySet()) {
                String direction = dirEntry.getKey();
                for (Map.Entry<String, PipelineProperties.ApiTypeStagesConfig> apiEntry :
                        dirEntry.getValue().entrySet()) {
                    String apiType = apiEntry.getKey();
                    String stageList = String.join(", ", apiEntry.getValue().getStages());
                    handlers.add(new PluginInfo.HandlerInfo(
                            cop.paymentType(),
                            stageList,
                            direction + "-" + apiType,
                            "config-only",
                            "delegate"));
                }
            }

            result.add(new PluginInfo(
                    cop.configFileName(),
                    Set.of(cop.paymentType()),
                    List.copyOf(handlers),
                    true));
        }

        return result;
    }

    /**
     * Disables all handlers for a given payment type by unloading its JAR.
     * The JAR path is remembered so the plugin can be re-enabled later.
     */
    public synchronized boolean disablePlugin(String paymentType) {
        if (disabledPaymentTypes.contains(paymentType)) {
            LOG.warn("Plugin '{}' is already disabled.", paymentType);
            return false;
        }
        String jarPath = paymentTypeToJarPath.get(paymentType);
        if (jarPath == null) {
            LOG.warn("No JAR found for paymentType '{}'. Cannot disable.", paymentType);
            return false;
        }
        LOG.info("Disabling plugin for paymentType '{}' (JAR: {})", paymentType, jarPath);
        unloadPluginJar(jarPath);
        disabledPaymentTypes.add(paymentType);
        logRegistrySummary();
        return true;
    }

    /**
     * Re-enables a previously disabled plugin by reloading its JAR.
     */
    public synchronized boolean enablePlugin(String paymentType) {
        if (!disabledPaymentTypes.contains(paymentType)) {
            LOG.warn("Plugin '{}' is not disabled.", paymentType);
            return false;
        }
        String jarPath = paymentTypeToJarPath.get(paymentType);
        if (jarPath == null) {
            LOG.warn("No JAR path recorded for paymentType '{}'. Cannot enable.", paymentType);
            return false;
        }
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            LOG.error("JAR file '{}' no longer exists. Cannot enable plugin '{}'.", jarPath, paymentType);
            return false;
        }
        LOG.info("Enabling plugin for paymentType '{}' (JAR: {})", paymentType, jarFile.getName());
        sharedPluginConfigs = loadSharedConfig();
        loadPluginJar(jarFile);
        disabledPaymentTypes.remove(paymentType);
        logRegistrySummary();
        return true;
    }

    /**
     * Returns the set of payment types that have been manually disabled.
     */
    public Set<String> getDisabledPaymentTypes() {
        return Set.copyOf(disabledPaymentTypes);
    }

    // =========================================================================
    // In-place config refresh (no JAR reload / no signature re-verification)
    // =========================================================================

    /**
     * Re-reads configuration (YAML + DB) and re-initializes the already-loaded REST handler
     * instances for {@code paymentType} <em>in place</em> — without reloading the JAR from disk,
     * re-verifying its signature, or churning the classloader. This lets an approved
     * {@code app_configs} / {@code error_codes} change take effect without a Core restart
     * (config that handlers freeze at {@code init()} time — e.g. preloaded error-code maps and
     * HTTP-client pool/timeout settings — is otherwise stale until restart).
     *
     * <p>Each matching handler is {@code destroy()}-ed then {@code init()}-ed with a fresh
     * {@link PluginContext}. There is a brief (sub-second) per-handler window where its in-memory
     * resources (e.g. the HTTP connection pool) are being rebuilt; in-flight requests during that
     * window may fail and should be retried by the caller. Intended for infrequent, approved
     * config changes, not a hot path. The method is {@code synchronized} on the same monitor as
     * {@link #loadPluginJar}/{@link #unloadPluginJar} so it cannot interleave with a JAR (un)load.
     *
     * <p>ISO 8583 (TCP) servers/listeners are intentionally skipped — their lifecycle is bound to
     * port reservations and jPOS deploy XML, not REST config. Config-only composites hold no
     * in-memory state to refresh. {@code endpoint_configs} changes are already picked up live
     * (handlers read those through the cached lookup per request), so this is not needed for them.
     *
     * @return {@code true} if at least one handler was re-initialized
     */
    public synchronized boolean refreshPluginConfig(String paymentType) {
        if (paymentType == null || paymentType.isBlank()) {
            return false;
        }
        if (disabledPaymentTypes.contains(paymentType)) {
            LOG.info("Plugin '{}' is disabled; skipping config refresh.", paymentType);
            return false;
        }
        String jarPath = paymentTypeToJarPath.get(paymentType);
        if (jarPath == null) {
            LOG.debug("No JAR-backed plugin for paymentType '{}'; nothing to refresh.", paymentType);
            return false;
        }
        LoadedPlugin plugin = loadedPlugins.get(jarPath);
        if (plugin == null) {
            LOG.warn("Plugin for paymentType '{}' (JAR '{}') is not currently loaded; skipping refresh.",
                    paymentType, jarPath);
            return false;
        }

        // Evict the in-memory AppConfigs cache so createContextForPlugin() reads committed DB
        // values (the cache is per-JVM; see the equivalent eviction in loadPluginJar). The plugin's
        // own preloaded error-code map is rebuilt by its init() below; ErrorCodesService.save
        // already evicted those caches on the approval path.
        appConfigsService.evictCaches();

        Map<String, Object> inJarConfig = readInJarConfig(plugin.jarFile());

        LOG.info("Refreshing config for paymentType='{}' (JAR '{}')...", paymentType, plugin.jarFile().getName());
        int count = 0;
        for (OutValidationRequestHandler h : plugin.outValidationHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "outward-validation",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (OutPaymentRequestProcessingHandler h : plugin.outProcessingHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "outward-payment-processing",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (OutResultRequestProcessingHandler h : plugin.outResultHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "outward-result-processing",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (OutStatusRequestProcessingHandler h : plugin.outStatusHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "outward-status-processing",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (InValidationRequestHandler h : plugin.inValidationHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "inward-validation",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (InPaymentRequestProcessingHandler h : plugin.inProcessingHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "inward-payment-processing",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (InResultRequestProcessingHandler h : plugin.inResultHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "inward-result-processing",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (InStatusRequestProcessingHandler h : plugin.inStatusHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "inward-status-processing",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (InwardCallback h : plugin.inCallbackHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "inward-callback",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (InwardReversalResultHandler h : plugin.inCreditReversalHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "inward-credit-reversal",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (DarajaInquiryHandler h : plugin.darajaInquiryHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "daraja-inquiry",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (LinkedWalletBankAccountHandler h : plugin.linkedWalletHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "linked-wallet",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (LinkedAccount h : plugin.linkedAccountHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "linked-account",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }
        for (SchemeEchoHandler h : plugin.schemeEchoHandlers()) {
            if (paymentType.equals(h.paymentType()))
                count += reinitHandler(paymentType, h.getClass().getName(), "scheme-echo",
                        h::destroy, h::init, inJarConfig) ? 1 : 0;
        }

        LOG.info("Config refresh for paymentType='{}' complete — re-initialized {} handler(s).",
                paymentType, count);
        return count > 0;
    }

    /**
     * Refreshes every currently-loaded JAR-backed plugin. Used when a cross-plugin global (an
     * {@code app_configs} row keyed {@code core.*}) changes, since that affects the merged
     * {@link PluginContext} of <em>all</em> plugins. Disabled / unloaded payment types are
     * skipped by {@link #refreshPluginConfig}.
     */
    public void refreshAllPluginConfigs() {
        Set<String> pts = Set.copyOf(paymentTypeToJarPath.keySet());
        LOG.info("Refreshing config for all loaded plugins: {}", pts);
        for (String pt : pts) {
            refreshPluginConfig(pt);
        }
    }

    /**
     * destroy()+init() a single handler instance with a freshly-built context. Destroy failures
     * are logged but do not abort the re-init; an init failure leaves the (still-registered)
     * handler in whatever state its own init() reached and is logged as PLUGIN_CONFIG_REFRESH_FAILED.
     */
    private boolean reinitHandler(String paymentType, String className, String handlerType,
                                  Runnable destroyAction,
                                  java.util.function.Consumer<PluginContext> initAction,
                                  Map<String, Object> inJarConfig) {
        try {
            destroyAction.run();
        } catch (Exception e) {
            LOG.warn("  Config-refresh: destroy() failed for {} handler '{}': {}",
                    handlerType, className, e.getMessage(), e);
        }
        try {
            initAction.accept(createContextForPlugin(paymentType, inJarConfig));
            LOG.info("  Re-initialized {} handler: paymentType='{}', class='{}'",
                    handlerType, paymentType, className);
            return true;
        } catch (Exception e) {
            LOG.error("  PLUGIN_CONFIG_REFRESH_FAILED {} handler '{}' (paymentType='{}'): " +
                    "init after refresh threw: {}", handlerType, className, paymentType, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Returns a snapshot of all active plugin classloaders.
     * Used by Q2Lifecycle to build a composite classloader so jPOS Q2
     * can instantiate participant classes defined in plugin JARs.
     */
    public List<ClassLoader> getPluginClassLoaders() {
        return loadedPlugins.values().stream()
                .map(LoadedPlugin::classLoader)
                .map(cl -> (ClassLoader) cl)
                .toList();
    }

    // =========================================================================
    // Configuration loading
    // =========================================================================

    private Map<String, String> loadConfigForPlugin(String paymentType, Map<String, Object> inJarConfig) {
        // 0. Prefer config bundled inside the JAR (signed via the JAR signature, deploys atomically).
        //    Its signature was already gated by PluginVerifier.verifyJar before the JAR was loaded.
        if (inJarConfig != null && !inJarConfig.isEmpty()) {
            Map<String, Object> section = selectConfigSection(inJarConfig, paymentType);
            if (section != null && !section.isEmpty()) {
                // Retain raw (un-flattened) section so the 'pipeline' key can be parsed separately.
                rawPluginConfigs.put(paymentType, section);
                Map<String, String> props = new HashMap<>();
                flattenYamlMap("", section, props);
                LOG.info("  Loaded in-JAR config '{}' for paymentType '{}': {} properties",
                        IN_JAR_CONFIG_FILE, paymentType, props.size());
                return props;
            }
        }

        // 1. Try external per-plugin sibling file (must be claimed by the verified sibling JAR).
        //    Retained for composites/config-only types and backward compatibility.
        File perPluginFile = new File(pluginDirectory, paymentType + PLUGIN_CONFIG_SUFFIX);
        if (perPluginFile.exists()) {
            String verifyReason = pluginVerifier.verifyConfig(perPluginFile);
            if (verifyReason != null) {
                quarantine(perPluginFile, verifyReason);
            } else {
                Map<String, String> props = loadYamlAsStringMap(perPluginFile, paymentType);
                if (props != null) {
                    LOG.info("  Loaded config from '{}': {} properties", perPluginFile.getName(), props.size());
                    return props;
                }
            }
        }

        // 2. Fall back to shared config
        Map<String, String> shared = sharedPluginConfigs.get(paymentType);
        if (shared != null && !shared.isEmpty()) {
            LOG.info("  Loaded config from '{}' [{}] section: {} properties",
                    SHARED_CONFIG_FILE, paymentType, shared.size());
            return shared;
        }

        return Collections.emptyMap();
    }

    /**
     * Reads {@code plugin-config.yml} from the resources root of a plugin JAR, if present.
     * Returns the parsed (un-flattened) YAML map, or {@code null} if the JAR ships no in-JAR
     * config or it could not be read/parsed.
     * <p>
     * Trust note: {@link PluginVerifier#verifyJar(File)} runs before this and rejects any JAR
     * whose {@code plugin-config.yml} is not signed by a trusted CA, so by the time we read here
     * the bytes are already known-good. We still open the JAR in verifying mode for consistency.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readInJarConfig(File jar) {
        try (JarFile jf = new JarFile(jar, true)) {
            JarEntry entry = jf.getJarEntry(IN_JAR_CONFIG_FILE);
            if (entry == null) {
                return null;
            }
            byte[] bytes;
            try (InputStream in = jf.getInputStream(entry)) {
                bytes = in.readAllBytes();
            }
            Object raw = new Yaml().load(new ByteArrayInputStream(bytes));
            if (!(raw instanceof Map<?, ?>)) {
                LOG.warn("In-JAR config '{}' in '{}' is empty or not a YAML map — ignoring.",
                        IN_JAR_CONFIG_FILE, jar.getName());
                return null;
            }
            return (Map<String, Object>) raw;
        } catch (Exception e) {
            LOG.error("Failed to read in-JAR config '{}' from '{}': {}",
                    IN_JAR_CONFIG_FILE, jar.getName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Selects the config section for {@code paymentType} from an in-JAR config map, using the
     * same single-vs-multi format detection as {@link #loadConfigOnlyPaymentTypes()}:
     * <ul>
     *   <li><b>Single-type</b>: a top-level {@code pipeline:} key is present — the whole map is
     *       this plugin's config (the common case, e.g. MPESA).</li>
     *   <li><b>Multi-type</b>: no top-level {@code pipeline:} key — top-level keys are payment
     *       type names; the section is {@code map.get(paymentType)} (or {@code null} if absent).</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> selectConfigSection(Map<String, Object> raw, String paymentType) {
        if (raw.containsKey("pipeline")) {
            return raw;
        }
        Object section = raw.get(paymentType);
        return (section instanceof Map<?, ?>) ? (Map<String, Object>) section : null;
    }

    /**
     * Loads a YAML file and flattens it to a string map.
     * If paymentType is non-null, also stores the raw (un-flattened) map in rawPluginConfigs
     * so that the 'pipeline' section can be parsed separately.
     */
    private Map<String, String> loadYamlAsStringMap(File file, String paymentType) {
        try (FileInputStream fis = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, Object> raw = yaml.load(fis);
            if (raw == null || raw.isEmpty()) {
                LOG.warn("'{}' is empty.", file.getName());
                return null;
            }
            // Store raw config for pipeline parsing if per-plugin file
            if (paymentType != null) {
                rawPluginConfigs.put(paymentType, raw);
            }
            Map<String, String> result = new HashMap<>();
            flattenYamlMap("", raw, result);
            return result;
        } catch (Exception e) {
            LOG.error("Failed to load config from '{}': {}", file.getName(), e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void flattenYamlMap(String prefix, Map<String, Object> map, Map<String, String> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            // Skip 'pipeline' key — it's parsed separately by parsePipelineConfig()
            if ("pipeline".equals(entry.getKey()) && prefix.isEmpty()) {
                continue;
            }
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                flattenYamlMap(key, (Map<String, Object>) nested, result);
            } else {
                result.put(key, value == null ? "" : String.valueOf(value));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, String>> loadSharedConfig() {
        File configFile = new File(pluginDirectory, SHARED_CONFIG_FILE);
        if (!configFile.exists()) {
            return Collections.emptyMap();
        }
        try (FileInputStream fis = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> raw = yaml.load(fis);
            if (raw == null || raw.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Map<String, String>> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                String paymentType = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Map<?, ?> mapValue) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedMap = (Map<String, Object>) mapValue;
                    // Store raw config for pipeline parsing
                    rawPluginConfigs.put(paymentType, typedMap);
                    // Flatten for plugin properties (skips 'pipeline' key)
                    Map<String, String> stringMap = new HashMap<>();
                    flattenYamlMap("", typedMap, stringMap);
                    result.put(paymentType, stringMap);
                }
            }
            LOG.info("Loaded shared config '{}' with sections: {}", SHARED_CONFIG_FILE, result.keySet());
            return result;
        } catch (Exception e) {
            LOG.error("Failed to load '{}': {}", configFile.getAbsolutePath(), e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private PluginContext createContextForPlugin(String paymentType, Map<String, Object> inJarConfig) {
        Map<String, String> pluginProps = loadConfigForPlugin(paymentType, inJarConfig);

        // Merge order (lowest to highest priority):
        //   1. Plugin YAML (sibling *-plugin-config.yml, signed via manifest hash)
        //   2. DB globals: rows where config_id starts with "core." — applied to
        //      every plugin. Prefix is RETAINED so plugin code can tell at the
        //      call site that a value comes from Core's environment, not from
        //      its own namespace (e.g. ctx.getProperty("core.mtls-keystore-password")).
        //   3. Programmatic globals (Spring environment-derived: core.payment-type,
        //      core.base-url, core.internal-api-key*). Injected with putIfAbsent
        //      so DB globals can override them when ops needs to (e.g. setting
        //      core.base-url to an LB URL instead of localhost:port).
        //   4. DB per-plugin: rows where config_id starts with "{paymentType}." —
        //      prefix STRIPPED. Wins over globals, including DB globals, by design:
        //      a plugin can opt into a different cert by setting
        //      MPESA.core.mtls-keystore-password (strips to core.mtls-keystore-password
        //      and overrides the global value for this plugin only).

        // 1. YAML
        Map<String, String> props = new HashMap<>(pluginProps);

        // 2. DB globals — prefix retained
        Map<String, String> globalConfigs = appConfigsService.findByConfigIdPrefix("core.");
        int globalsInjected = 0;
        for (Map.Entry<String, String> e : globalConfigs.entrySet()) {
            String key = e.getKey();
            if (!key.startsWith("core.") || key.length() <= "core.".length()) continue;
            props.put(key, e.getValue());
            globalsInjected++;
        }
        if (globalsInjected > 0) {
            LOG.info("  Injected {} global AppConfigs entries (prefix='core.') into plugin context for '{}'",
                    globalsInjected, paymentType);
        }

        // 3. Programmatic globals — putIfAbsent so DB globals can override
        props.putIfAbsent("core.payment-type", paymentType);

        String scheme = environment.getProperty("server.ssl.key-store") != null ? "https" : "http";
        String port = environment.getProperty("server.port", "8080");
        props.putIfAbsent("core.base-url", scheme + "://localhost:" + port);

        String apiKey = environment.getProperty("gotechly.security.internal-api-key.secret");
        String apiKeyHeader = environment.getProperty("gotechly.security.internal-api-key.header-name", "X-Internal-Api-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            props.putIfAbsent("core.internal-api-key", apiKey);
            props.putIfAbsent("core.internal-api-key-header", apiKeyHeader);
        }

        // 4. DB per-plugin — prefix stripped, wins over everything
        String dbPrefix = paymentType + ".";
        Map<String, String> dbConfigs = appConfigsService.findByConfigIdPrefix(dbPrefix);
        int dbInjected = 0;
        for (Map.Entry<String, String> e : dbConfigs.entrySet()) {
            String key = e.getKey();
            if (!key.startsWith(dbPrefix)) continue;
            String stripped = key.substring(dbPrefix.length());
            if (stripped.isEmpty()) continue;
            props.put(stripped, e.getValue());
            dbInjected++;
        }
        if (dbInjected > 0) {
            LOG.info("  Injected {} per-plugin AppConfigs entries (prefix='{}') into plugin context for '{}'",
                    dbInjected, dbPrefix, paymentType);
        }

        DefaultPluginContext ctx = new DefaultPluginContext(props);
        sharedServices.forEach((type, instance) -> ctx.registerService((Class) type, instance));
        if (pluginProps.isEmpty() && dbInjected == 0 && globalsInjected == 0) {
            LOG.warn("  No config found for paymentType '{}'. Looked for: in-JAR '{}', sibling '{}', '{}' [{}] section, AppConfigs prefixes 'core.' and '{}'.",
                    paymentType, IN_JAR_CONFIG_FILE, paymentType + PLUGIN_CONFIG_SUFFIX, SHARED_CONFIG_FILE, paymentType, dbPrefix);
        }
        return ctx;
    }

    // =========================================================================
    // Plugin pipeline config parsing
    // =========================================================================

    /**
     * Parses the 'pipeline' section from a plugin's raw YAML config into the structure
     * expected by {@link PipelineRegistry#registerPaymentTypePipeline}.
     * <p>
     * Expected YAML structure under the payment type key:
     * <pre>
     * pipeline:
     *   inward:
     *     validation:
     *       stages: [aml-screening, in-validation-handler]
     *       compensation:
     *         aml-blocked: [callback]
     *   outward:
     *     validation:
     *       stages: [db-logger, aml-screening, out-validation-handler]
     * </pre>
     *
     * @return parsed direction config, or null if no pipeline section exists
     */
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> parsePipelineConfig(
            String paymentType, Map<String, Object> rawConfig) {
        Object pipelineObj = rawConfig.get("pipeline");
        if (pipelineObj == null) {
            return null;
        }
        if (!(pipelineObj instanceof Map<?, ?>)) {
            LOG.warn("'pipeline' key for paymentType '{}' is not a map — ignoring.", paymentType);
            return null;
        }

        Map<String, Object> pipelineMap = (Map<String, Object>) pipelineObj;
        Map<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> directionConfig = new HashMap<>();

        for (Map.Entry<String, Object> dirEntry : pipelineMap.entrySet()) {
            String direction = dirEntry.getKey();
            if (!(dirEntry.getValue() instanceof Map<?, ?>)) {
                LOG.warn("pipeline.{} for paymentType '{}' is not a map — skipping.", direction, paymentType);
                continue;
            }
            Map<String, Object> apiTypeMap = (Map<String, Object>) dirEntry.getValue();
            Map<String, PipelineProperties.ApiTypeStagesConfig> apiConfigs = new HashMap<>();

            for (Map.Entry<String, Object> apiEntry : apiTypeMap.entrySet()) {
                String apiType = apiEntry.getKey();
                if (!(apiEntry.getValue() instanceof Map<?, ?>)) {
                    LOG.warn("pipeline.{}.{} for paymentType '{}' is not a map — skipping.",
                            direction, apiType, paymentType);
                    continue;
                }
                Map<String, Object> stagesMap = (Map<String, Object>) apiEntry.getValue();

                PipelineProperties.ApiTypeStagesConfig config = new PipelineProperties.ApiTypeStagesConfig();

                // Parse stages list
                Object stagesObj = stagesMap.get("stages");
                if (stagesObj instanceof List<?> stagesList) {
                    config.setStages(stagesList.stream()
                            .map(String::valueOf)
                            .toList());
                } else {
                    LOG.warn("pipeline.{}.{}.stages for paymentType '{}' is missing or not a list — skipping.",
                            direction, apiType, paymentType);
                    continue;
                }

                // Parse optional compensation map
                Object compObj = stagesMap.get("compensation");
                if (compObj instanceof Map<?, ?> compMap) {
                    Map<String, List<String>> compensation = new HashMap<>();
                    for (Map.Entry<?, ?> compEntry : compMap.entrySet()) {
                        String haltCode = String.valueOf(compEntry.getKey());
                        if (compEntry.getValue() instanceof List<?> compStages) {
                            compensation.put(haltCode, compStages.stream()
                                    .map(String::valueOf)
                                    .toList());
                        } else if ("compensation".equals(haltCode)) {
                            // There is no nested compensation form. `compensation:` binds as
                            // Map<String, List<String>>, so a nested 'compensation:' key has a Map
                            // value and would otherwise be dropped here without a trace — the halt
                            // codes under it never register and the compensation silently never runs.
                            LOG.warn("pipeline.{}.{}.compensation for paymentType '{}' contains a nested " +
                                            "'compensation:' key — there is no nested form. Move its halt codes up one " +
                                            "level so they are siblings of the other halt codes; as written they are " +
                                            "ignored and no compensation will run for them.",
                                    direction, apiType, paymentType);
                        } else {
                            LOG.warn("pipeline.{}.{}.compensation.{} for paymentType '{}' is not a list of stage " +
                                            "names — ignoring. No compensation will run for haltCode '{}'.",
                                    direction, apiType, haltCode, paymentType, haltCode);
                        }
                    }
                    if (!compensation.isEmpty()) {
                        config.setCompensation(compensation);
                    }
                }

                apiConfigs.put(apiType, config);
            }

            if (!apiConfigs.isEmpty()) {
                directionConfig.put(direction, apiConfigs);
            }
        }

        return directionConfig.isEmpty() ? null : directionConfig;
    }

    // =========================================================================
    // Config-only (composite) payment types
    // =========================================================================

    /**
     * Scans the plugins directory for {@code *-plugin-config.yml} files whose payment type(s)
     * were not already loaded from a JAR. Supports two formats:
     * <ul>
     *   <li><b>Single-type</b>: top-level {@code pipeline:} key — payment type derived from filename</li>
     *   <li><b>Multi-type</b>: top-level keys are payment type names, each containing its own
     *       {@code pipeline:} section (e.g., {@code IMT-OUTBOUND-plugin-config.yml} with
     *       {@code IMT-MPESA}, {@code IMT-VISA}, {@code IMT-AIRTEL} as top-level keys)</li>
     * </ul>
     * Validates that all referenced stages and delegate payment types exist.
     * Valid configs are registered with {@link PipelineRegistry};
     * invalid ones are renamed to {@code *.BAD}.
     */
    @SuppressWarnings("unchecked")
    private void loadConfigOnlyPaymentTypes() {
        File[] configFiles = pluginDirectory.listFiles(
                f -> f.getName().endsWith(PLUGIN_CONFIG_SUFFIX)
                        && !f.getName().endsWith(".BAD")
                        && !f.getName().endsWith(".UNVERIFIED")
                        && !f.getName().endsWith(".MISCONFIGURED"));
        if (configFiles == null || configFiles.length == 0) {
            return;
        }

        Set<String> jarPaymentTypes = collectAllJarPaymentTypes();

        for (File configFile : configFiles) {
            String fileName = configFile.getName();
            String fileLabel = fileName.substring(0, fileName.length() - PLUGIN_CONFIG_SUFFIX.length());

            // Skip if a JAR already claimed this exact filename-derived payment type — that
            // sibling YAML was already verified by loadConfigForPlugin during JAR load.
            // (for single-type files, this is the payment type; for multi-type, fileLabel is just a group name)
            if (jarPaymentTypes.contains(fileLabel)) {
                continue;
            }

            // Composite (JAR-less) config: must be claimed by a delegating plugin JAR's manifest.
            String verifyReason = pluginVerifier.verifyConfig(configFile);
            if (verifyReason != null) {
                quarantine(configFile, verifyReason);
                continue;
            }

            // Load raw YAML
            Map<String, Object> raw;
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Yaml yaml = new Yaml();
                raw = yaml.load(fis);
            } catch (Exception e) {
                LOG.error("Failed to parse '{}': {}", fileName, e.getMessage(), e);
                renameConfigToBad(configFile, fileLabel, "Failed to parse YAML: " + e.getMessage());
                continue;
            }
            if (raw == null || raw.isEmpty()) {
                renameConfigToBad(configFile, fileLabel, "File is empty");
                continue;
            }

            // Detect format: single-type (has top-level 'pipeline' key) vs multi-type
            if (raw.containsKey("pipeline")) {
                // Single-type format: payment type from filename
                if (jarPaymentTypes.contains(fileLabel)) continue;

                rawPluginConfigs.put(fileLabel, raw);
                loadSingleConfigOnlyType(configFile, fileName, fileLabel, raw);
            } else {
                // Multi-type format: each top-level key is a payment type
                List<String> failedTypes = new ArrayList<>();

                for (Map.Entry<String, Object> entry : raw.entrySet()) {
                    String paymentType = entry.getKey();

                    if (jarPaymentTypes.contains(paymentType)) {
                        LOG.info("Skipping config-only paymentType '{}' from '{}' — already loaded from JAR",
                                paymentType, fileName);
                        continue;
                    }

                    if (!(entry.getValue() instanceof Map<?, ?>)) {
                        failedTypes.add(paymentType + ": value is not a map");
                        continue;
                    }

                    Map<String, Object> typeConfig = (Map<String, Object>) entry.getValue();
                    rawPluginConfigs.put(paymentType, typeConfig);

                    String error = loadSingleConfigOnlyType(configFile, fileName, paymentType, typeConfig);
                    if (error != null) {
                        failedTypes.add(paymentType + ": " + error);
                    }
                }

                // If ALL payment types in the file failed, rename to .BAD
                long totalTypes = raw.size();
                if (!failedTypes.isEmpty() && failedTypes.size() == totalTypes) {
                    renameConfigToBad(configFile, fileLabel,
                            "All payment types failed validation: " + String.join("; ", failedTypes));
                } else if (!failedTypes.isEmpty()) {
                    LOG.warn("Config '{}': some payment types failed: {}", fileName, failedTypes);
                }
            }
        }
    }

    /**
     * Loads, validates, and registers a single config-only payment type.
     *
     * @return error description if invalid, null if successfully registered
     */
    private String loadSingleConfigOnlyType(File configFile, String fileName,
                                            String paymentType, Map<String, Object> rawConfig) {

        LOG.info("Found config-only plugin config: '{}' for paymentType '{}'", fileName, paymentType);

        Map<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> pipelineCfg =
                parsePipelineConfig(paymentType, rawConfig);
        if (pipelineCfg == null || pipelineCfg.isEmpty()) {
            String error = "No valid pipeline section found";
            LOG.warn("Config-only plugin '{}' paymentType '{}': {}", fileName, paymentType, error);
            return error;
        }

        // Validate all stages and delegate references
        String validationError = validatePipelineConfig(paymentType, pipelineCfg);
        if (validationError != null) {
            LOG.warn("Config-only plugin '{}' paymentType '{}': {}", fileName, paymentType, validationError);
            return validationError;
        }

        // Register with PipelineRegistry
        pipelineRegistry.registerPaymentTypePipeline(paymentType, pipelineCfg);

        configOnlyPlugins.put(paymentType, new ConfigOnlyPlugin(fileName, paymentType, pipelineCfg));

        LOG.info("Registered config-only payment type '{}' from '{}'", paymentType, fileName);
        return null;
    }

    /**
     * Validates that all stage names in the pipeline config exist as registered stage beans,
     * and that all delegate payment types (from {@code >PaymentType} syntax) have at least
     * one handler registered in any registry.
     *
     * @return error description if invalid, null if valid
     */
    private String validatePipelineConfig(String paymentType,
                                          Map<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> pipelineCfg) {

        Set<String> registeredStages = pipelineRegistry.getRegisteredStageNames();
        Set<String> allRegisteredHandlerTypes = collectAllRegisteredHandlerPaymentTypes();

        for (Map.Entry<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> dirEntry :
                pipelineCfg.entrySet()) {
            String direction = dirEntry.getKey();
            for (Map.Entry<String, PipelineProperties.ApiTypeStagesConfig> apiEntry :
                    dirEntry.getValue().entrySet()) {
                String apiType = apiEntry.getKey();
                List<String> allStages = new ArrayList<>(apiEntry.getValue().getStages());

                // Also validate compensation stages
                if (apiEntry.getValue().getCompensation() != null) {
                    for (List<String> compStages : apiEntry.getValue().getCompensation().values()) {
                        allStages.addAll(compStages);
                    }
                }

                for (String nameSpec : allStages) {
                    String stageName;
                    String delegatePt = null;
                    int delegateIdx = nameSpec.indexOf('>');
                    if (delegateIdx > 0 && delegateIdx < nameSpec.length() - 1) {
                        stageName = nameSpec.substring(0, delegateIdx);
                        delegatePt = nameSpec.substring(delegateIdx + 1);
                    } else {
                        stageName = nameSpec;
                    }

                    // Validate stage exists
                    if (!registeredStages.contains(stageName)) {
                        return String.format(
                                "Stage '%s' in pipeline.%s.%s does not exist. Available: %s",
                                stageName, direction, apiType, registeredStages);
                    }

                    // Validate delegate payment type has handlers
                    if (delegatePt != null && !allRegisteredHandlerTypes.contains(delegatePt)) {
                        return String.format(
                                "Delegate payment type '%s' in stage '%s' (pipeline.%s.%s) " +
                                        "has no registered handlers. Available: %s",
                                delegatePt, nameSpec, direction, apiType, allRegisteredHandlerTypes);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Collects all payment types that have at least one handler registered in any registry.
     */
    private Set<String> collectAllRegisteredHandlerPaymentTypes() {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(outValidationRegistry.getRegisteredPaymentTypes());
        all.addAll(outProcessingRegistry.getRegisteredPaymentTypes());
        all.addAll(outResultRegistry.getRegisteredPaymentTypes());
        all.addAll(outStatusRegistry.getRegisteredPaymentTypes());
        all.addAll(inValidationRegistry.getRegisteredPaymentTypes());
        all.addAll(inProcessingRegistry.getRegisteredPaymentTypes());
        all.addAll(inResultRegistry.getRegisteredPaymentTypes());
        all.addAll(inStatusRegistry.getRegisteredPaymentTypes());
        return all;
    }

    /**
     * Collects all payment types loaded from JAR-based plugins.
     */
    private Set<String> collectAllJarPaymentTypes() {
        Set<String> pts = new LinkedHashSet<>();
        for (LoadedPlugin plugin : loadedPlugins.values()) {
            plugin.outValidationHandlers().forEach(h -> pts.add(h.paymentType()));
            plugin.outProcessingHandlers().forEach(h -> pts.add(h.paymentType()));
            plugin.outResultHandlers().forEach(h -> pts.add(h.paymentType()));
            plugin.outStatusHandlers().forEach(h -> pts.add(h.paymentType()));
            plugin.inValidationHandlers().forEach(h -> pts.add(h.paymentType()));
            plugin.inProcessingHandlers().forEach(h -> pts.add(h.paymentType()));
            plugin.inResultHandlers().forEach(h -> pts.add(h.paymentType()));
            plugin.inStatusHandlers().forEach(h -> pts.add(h.paymentType()));
        }
        return pts;
    }

    /**
     * Quarantines a file that failed plugin signature verification by renaming it to
     * {@code *.UNVERIFIED}. Mirrors {@link #renameConfigToBad} but for signature failures
     * vs structural failures.
     */
    private void quarantine(File file, String reason) {
        LOG.error("Rejecting plugin file '{}': {}", file.getName(), reason);
        File bad = new File(file.getParentFile(), file.getName() + ".UNVERIFIED");
        if (bad.exists() && !bad.delete()) {
            LOG.warn("Could not remove existing quarantine file '{}'; rename may fail.", bad.getName());
        }
        if (!file.renameTo(bad)) {
            LOG.error("Could not quarantine '{}'. Please remove or replace the file manually.",
                    file.getName());
        }
    }

    /**
     * Quarantines a JAR whose required-configs preflight failed by renaming it to
     * {@code *.MISCONFIGURED}. Distinct from {@link #quarantine(File, String)} (signature
     * failure) because the failure is operational (DB rows missing), not a trust issue.
     */
    private void quarantineMisconfigured(File jar, String reason) {
        LOG.error("Plugin '{}' failed required-configs preflight: {}", jar.getName(), reason);
        pluginVerifier.recordMisconfigured(jar, reason);
        File bad = new File(jar.getParentFile(), jar.getName() + ".MISCONFIGURED");
        if (bad.exists() && !bad.delete()) {
            LOG.warn("Could not remove existing quarantine file '{}'; rename may fail.", bad.getName());
        }
        if (!jar.renameTo(bad)) {
            LOG.error("Could not quarantine '{}'. Please remove or replace the file manually.",
                    jar.getName());
        }
    }

    private void renameConfigToBad(File configFile, String paymentType, String reason) {
        LOG.warn("Config-only plugin '{}' is invalid: {}. Renaming to .BAD", configFile.getName(), reason);
        File badFile = new File(configFile.getParentFile(), configFile.getName() + ".BAD");
        if (badFile.exists()) {
            badFile.delete();
        }
        if (!configFile.renameTo(badFile)) {
            LOG.error("Failed to rename '{}' to '{}'. Please remove or fix the file manually.",
                    configFile.getName(), badFile.getName());
        }
    }

    private void logRegistrySummary() {
        LOG.info("Plugin registry snapshot (ALL currently-registered plugins — cumulative state, " +
                "NOT a list of what was just (re)loaded this cycle):");
        LOG.info("  Outward validation handlers: {}", outValidationRegistry.getRegisteredPaymentTypes());
        LOG.info("  Outward payment processing handlers: {}", outProcessingRegistry.getRegisteredPaymentTypes());
        LOG.info("  Outward result processing handlers: {}", outResultRegistry.getRegisteredPaymentTypes());
        LOG.info("  Outward status processing handlers: {}", outStatusRegistry.getRegisteredPaymentTypes());
        LOG.info("  Inward validation handlers: {}", inValidationRegistry.getRegisteredPaymentTypes());
        LOG.info("  Inward payment processing handlers: {}", inProcessingRegistry.getRegisteredPaymentTypes());
        LOG.info("  Inward result processing handlers: {}", inResultRegistry.getRegisteredPaymentTypes());
        LOG.info("  Inward status processing handlers: {}", inStatusRegistry.getRegisteredPaymentTypes());
        LOG.info("  Inward callback handlers: {}", inCallbackRegistry.getRegisteredPaymentTypes());
        LOG.info("  Inward credit-reversal handlers: {}", inCreditReversalRegistry.getRegisteredPaymentTypes());
        LOG.info("  Daraja inquiry handlers: {}", darajaInquiryRegistry.getRegisteredPaymentTypes());
        LOG.info("  Linked-wallet handlers: {}", linkedWalletRegistry.getRegisteredPaymentTypes());
        LOG.info("  Linked-account (alias/proxy) handlers: {}", linkedAccountRegistry.getRegisteredPaymentTypes());
        LOG.info("  Scheme-echo (heartbeat) handlers: {}", schemeEchoRegistry.getRegisteredPaymentTypes());
        LOG.info("  ISO 8583 plugins: {}", iso8583PluginRegistry.getRegisteredPaymentTypes());
        LOG.info("  Total loaded JARs: {}", loadedPlugins.size());
        if (!configOnlyPlugins.isEmpty()) {
            LOG.info("  Config-only payment types: {}", configOnlyPlugins.keySet());
        }
    }
}
