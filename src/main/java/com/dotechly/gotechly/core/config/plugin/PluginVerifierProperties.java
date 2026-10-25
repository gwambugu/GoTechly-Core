package com.dotechly.gotechly.core.config.plugin;



import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("gotechly.plugins.verification")
public record PluginVerifierProperties(
        boolean enabled,
        String truststorePath,
        String truststorePassword,
        String truststoreType,
        boolean requireAllJarEntriesSigned,
        boolean requireManifestForYaml
) {
}