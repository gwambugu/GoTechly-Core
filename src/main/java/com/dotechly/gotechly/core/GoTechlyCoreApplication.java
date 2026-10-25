package com.dotechly.gotechly.core;

import com.dotechly.gotechly.core.config.AppConstants;
import com.dotechly.gotechly.core.config.CRLFLogConverter;
import com.dotechly.gotechly.core.util.DefaultProfileUtil;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

@SpringBootApplication
@EnableScheduling
public class GoTechlyCoreApplication {
    private static final Logger LOG = LoggerFactory.getLogger(GoTechlyCoreApplication.class);

    @Autowired
    private Environment env;

    public static ConfigurableApplicationContext applicationContext;


    /**
     * Initializes flend.
     * <p>
     * Spring profiles can be configured with a program argument --spring.profiles.active=your-active-profile
     * <p>
     * You can find more information on how profiles work with JHipster on <a href="https://www.jhipster.tech/profiles/">https://www.jhipster.tech/profiles/</a>.
     */
    @PostConstruct
    public void initApplication() {
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (
                activeProfiles.contains(AppConstants.SPRING_PROFILE_DEVELOPMENT) &&
                        activeProfiles.contains(AppConstants.SPRING_PROFILE_PRODUCTION)
        ) {
            LOG.error(
                    "You have misconfigured your application! It should not run " + "with both the 'dev' and 'prod' profiles at the same time."
            );
        }
        if (
                activeProfiles.contains(AppConstants.SPRING_PROFILE_DEVELOPMENT) &&
                        activeProfiles.contains(AppConstants.SPRING_PROFILE_CLOUD)
        ) {
            LOG.error(
                    "You have misconfigured your application! It should not " + "run with both the 'dev' and 'cloud' profiles at the same time."
            );
        }
    }

    public static void main(String[] args) {
        //System.setProperty("pf4j.mode", "development");
        SpringApplication app = new SpringApplication(GoTechlyCoreApplication.class);
        DefaultProfileUtil.addDefaultProfile(app);
        applicationContext = app.run(args);
        logApplicationStartup(applicationContext.getEnvironment());
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = Optional.ofNullable(env.getProperty("server.ssl.key-store")).map(key -> "https").orElse("http");
        String applicationName = env.getProperty("spring.application.name");
        String serverPort = env.getProperty("server.port");
        String contextPath = Optional.ofNullable(env.getProperty("server.servlet.context-path"))
                .filter(StringUtils::isNotBlank)
                .orElse("/");
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            LOG.warn("The host name could not be determined, using `localhost` as fallback");
        }
        LOG.info(
                CRLFLogConverter.CRLF_SAFE_MARKER,
                """
                        
                        ----------------------------------------------------------
                        \tApplication '{}' is running! Access URLs:
                        \tLocal: \t\t{}://localhost:{}{}
                        \tExternal: \t{}://{}:{}{}
                        \tProfile(s): \t{}
                        ----------------------------------------------------------""",
                applicationName,
                protocol,
                serverPort,
                contextPath,
                protocol,
                hostAddress,
                serverPort,
                contextPath,
                env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles()
        );
    }


}
