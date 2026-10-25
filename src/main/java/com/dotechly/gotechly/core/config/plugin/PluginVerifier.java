package com.dotechly.gotechly.core.config.plugin;

import com.dotechly.gotechly.spi.service.AppConfigsLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
/**
 * Verifies plugin authenticity.
 * <p>
 * Trust model: each plugin JAR is signed with jarsigner using a cert that chains to a trusted CA
 * in the configured plugin truststore. Inside each signed JAR, {@code META-INF/plugin-manifest.yml}
 * lists the external YAML config files the plugin vouches for, with their SHA-256 hashes.
 * <p>
 * At startup {@link com.dotechly.gotechly.core.config.PluginLoader} calls
 * {@link #verifyJar(File)} for every JAR first (which registers the claim map), then
 * {@link #verifyConfig(File)} for every YAML. Any file that fails verification is quarantined
 * by the caller (renamed to {@code *.UNVERIFIED}).
 */
@Component
@EnableConfigurationProperties(PluginVerifierProperties.class)
public class PluginVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(PluginVerifier.class);
    private static final String IN_JAR_MANIFEST = "META-INF/plugin-manifest.yml";
    /**
     * Fixed name of a plugin's own configuration when it ships <em>inside</em> the signed JAR
     * (resources root). Because it is a JAR entry, the {@code jarsigner} signature covers it
     * directly — no SHA-256 manifest claim is needed (unlike external, JAR-sibling YAMLs which
     * still go through {@link #verifyConfig(File)}). One file per JAR, so the name need not be
     * payment-type-qualified.
     */
    private static final String IN_JAR_CONFIG = "plugin-config.yml";
    private static final int BUF = 8192;

    private final PluginVerifierProperties props;
    private final Set<X509Certificate> trustedRoots = new HashSet<>();
    private final Map<String, ClaimedFile> expectedHashes = new ConcurrentHashMap<>();
    private final Map<String, VerificationRecord> records = new ConcurrentHashMap<>();
    /** Required-configs declared in each verified JAR's manifest, keyed by JAR file name. */
    private final Map<String, RequiredConfigsClaim> requiredConfigsByJar = new ConcurrentHashMap<>();

    public record ClaimedFile(String sha256, String claimingJar) {
    }

    /**
     * Required AppConfigs declared in a plugin's signed manifest.
     *
     * @param paymentType    the plugin's payment type — used as the {@code configId} prefix
     *                       when bulk-fetching from {@link AppConfigsLookup}
     * @param pluginWide     keys that must exist as {@code {paymentType}.{key}}
     * @param serviceScoped  per-service-type keys; outer map key is the service type
     *                       (e.g. {@code OUTWARD_VALIDATION}), inner list is the required
     *                       config IDs that must exist as {@code {paymentType}.{serviceType}.{key}}
     */
    public record RequiredConfigsClaim(
            String paymentType,
            List<String> pluginWide,
            Map<String, List<String>> serviceScoped
    ) {
    }

    public record VerifierSummary(
            boolean enabled,
            int trustedRootCount,
            int pinnedFileCount,
            int verifiedCount,
            int rejectedCount,
            int skippedCount,
            int misconfiguredCount
    ) {
    }

    public PluginVerifier(PluginVerifierProperties props) throws Exception {
        this.props = props;
        if (!props.enabled()) {
            LOG.warn("Plugin signature verification is DISABLED. DO NOT USE IN PROD.");
            return;
        }
        loadTruststore();
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Verifies a plugin JAR. Returns {@code null} on success; otherwise a human-readable
     * rejection reason. Successful verification registers any claims from the JAR's
     * {@code META-INF/plugin-manifest.yml} for later use by {@link #verifyConfig(File)}.
     */
    public String verifyJar(File jar) {
        if (!props.enabled()) {
            record(jar, VerificationRecord.FileType.JAR,
                    VerificationRecord.Status.SKIPPED, "verification disabled", null, null);
            return null;
        }
        String hash = safeHash(jar);
        String sigReason = verifyJarSignature(jar);
        if (sigReason != null) {
            record(jar, VerificationRecord.FileType.JAR,
                    VerificationRecord.Status.REJECTED, sigReason, hash, null);
            return sigReason;
        }
        String claimsReason = extractAndRegisterClaims(jar);
        if (claimsReason != null) {
            record(jar, VerificationRecord.FileType.JAR,
                    VerificationRecord.Status.REJECTED, claimsReason, hash, null);
            return claimsReason;
        }
        // If the plugin ships its config inside the JAR (the new, atomic deployment model),
        // make sure that entry is itself signed by a trusted CA. verifyJarSignature already
        // enforces this when require-all-jar-entries-signed=true, but we re-assert it here so
        // the guarantee survives that flag being turned off — otherwise an attacker could
        // append an unsigned plugin-config.yml to an otherwise validly-signed JAR.
        String inJarConfigReason = verifyInJarConfigSigned(jar);
        if (inJarConfigReason != null) {
            record(jar, VerificationRecord.FileType.JAR,
                    VerificationRecord.Status.REJECTED, inJarConfigReason, hash, null);
            return inJarConfigReason;
        }
        record(jar, VerificationRecord.FileType.JAR,
                VerificationRecord.Status.VERIFIED, null, hash, null);
        return null;
    }

    /**
     * Verifies a plugin YAML config. Returns {@code null} on success; otherwise a rejection reason.
     * Requires that at least one successfully-verified JAR has claimed this file by name.
     */
    public String verifyConfig(File yml) {
        if (!props.enabled() || !props.requireManifestForYaml()) {
            record(yml, VerificationRecord.FileType.CONFIG,
                    VerificationRecord.Status.SKIPPED, "verification disabled", null, null);
            return null;
        }
        String hash = safeHash(yml);
        ClaimedFile claim = expectedHashes.get(yml.getName());
        if (claim == null) {
            String reason = "Not claimed by any verified plugin JAR";
            record(yml, VerificationRecord.FileType.CONFIG,
                    VerificationRecord.Status.REJECTED, reason, hash, null);
            return reason;
        }
        if (!claim.sha256().equalsIgnoreCase(hash)) {
            String reason = "SHA-256 mismatch (expected=" + claim.sha256()
                    + " actual=" + hash
                    + " claimedBy=" + claim.claimingJar() + ")";
            record(yml, VerificationRecord.FileType.CONFIG,
                    VerificationRecord.Status.REJECTED, reason, hash, claim.claimingJar());
            return reason;
        }
        record(yml, VerificationRecord.FileType.CONFIG,
                VerificationRecord.Status.VERIFIED, null, hash, claim.claimingJar());
        return null;
    }

    /**
     * Validates that every AppConfigs key declared as required in the JAR's signed manifest
     * is actually present in the database. Returns {@code null} on success, otherwise a
     * human-readable rejection reason listing the missing keys. Does NOT update verification
     * records — the caller decides how to react (typically by quarantining to {@code .MISCONFIGURED}
     * and recording via {@link #recordMisconfigured(File, String)}).
     * <p>
     * No-op when verification is disabled (returns null) since the manifest is not parsed
     * in that mode and there are no required-configs claims to enforce.
     *
     * @param jar    the plugin JAR (must have already passed {@link #verifyJar(File)})
     * @param lookup the live {@link AppConfigsLookup} bean from Core
     */
    public String verifyRequiredConfigs(File jar, AppConfigsLookup lookup) {
        if (!props.enabled()) {
            return null;
        }
        RequiredConfigsClaim claim = requiredConfigsByJar.get(jar.getName());
        if (claim == null) {
            // No required-configs declared — nothing to enforce.
            return null;
        }
        String prefix = claim.paymentType() + ".";
        Map<String, String> dbConfigs = lookup.findByConfigIdPrefix(prefix);

        List<String> missing = new ArrayList<>();
        for (String key : claim.pluginWide()) {
            String fullKey = prefix + key;
            String val = dbConfigs.get(fullKey);
            if (val == null || val.isBlank()) {
                missing.add(fullKey);
            }
        }
        for (Map.Entry<String, List<String>> svc : claim.serviceScoped().entrySet()) {
            String svcPrefix = prefix + svc.getKey() + ".";
            for (String key : svc.getValue()) {
                String fullKey = svcPrefix + key;
                String val = dbConfigs.get(fullKey);
                if (val == null || val.isBlank()) {
                    missing.add(fullKey);
                }
            }
        }
        if (missing.isEmpty()) {
            return null;
        }
        return "Missing required AppConfigs entries: " + missing;
    }

    /**
     * Records a {@code MISCONFIGURED} verification entry. Called by the loader after
     * quarantining a JAR whose required-configs preflight failed.
     */
    public void recordMisconfigured(File jar, String reason) {
        record(jar, VerificationRecord.FileType.JAR,
                VerificationRecord.Status.MISCONFIGURED, reason, safeHash(jar), null);
    }

    public Collection<VerificationRecord> allRecords() {
        return Collections.unmodifiableCollection(records.values());
    }

    public VerifierSummary summary() {
        int verified = 0, rejected = 0, skipped = 0, misconfigured = 0;
        for (VerificationRecord r : records.values()) {
            switch (r.status()) {
                case VERIFIED -> verified++;
                case REJECTED -> rejected++;
                case SKIPPED -> skipped++;
                case MISCONFIGURED -> misconfigured++;
            }
        }
        return new VerifierSummary(
                props.enabled(),
                trustedRoots.size(),
                expectedHashes.size(),
                verified, rejected, skipped, misconfigured);
    }

    /** Clears claims + records so {@link com.dotechly.gotechly.core.config.PluginLoader} can re-scan. */
    public synchronized void rescan() {
        expectedHashes.clear();
        requiredConfigsByJar.clear();
        records.clear();
    }

    /**
     * Forgets all config claims and required-config declarations previously registered
     * by a JAR of the given file name. Called at the start of (re)registration so a JAR
     * can be redeployed — including with a changed config hash — without colliding with
     * its own stale claim. Idempotent; a no-op if the JAR has no recorded claims.
     */
    public void forgetClaimsBy(String jarName) {
        boolean removed = expectedHashes.values().removeIf(c -> jarName.equals(c.claimingJar()));
        requiredConfigsByJar.remove(jarName);
        if (removed) {
            LOG.debug("Forgot prior config claims registered by '{}' before re-registering.", jarName);
        }
    }

    // =========================================================================
    // Internals
    // =========================================================================

    private void loadTruststore() throws Exception {
        Resource resource = new DefaultResourceLoader().getResource(props.truststorePath());
        if (!resource.exists()) {
            throw new FileNotFoundException(
                    "Plugin truststore not found at '" + props.truststorePath()
                            + "'. Supported prefixes: classpath:, file:, or a filesystem path.");
        }
        KeyStore ks = KeyStore.getInstance(props.truststoreType());
        try (InputStream in = resource.getInputStream()) {
            ks.load(in, props.truststorePassword().toCharArray());
        }
        for (String alias : Collections.list(ks.aliases())) {
            if (ks.isCertificateEntry(alias)) {
                trustedRoots.add((X509Certificate) ks.getCertificate(alias));
            }
        }
        LOG.info("Plugin truststore loaded: {} trusted root(s) from {}",
                trustedRoots.size(), props.truststorePath());
    }

    private String verifyJarSignature(File jar) {
        try (JarFile jf = new JarFile(jar, true)) {
            Enumeration<JarEntry> entries = jf.entries();
            boolean sawSignedEntry = false;
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                if (e.isDirectory()) continue;
                String name = e.getName();
                if (name.equalsIgnoreCase("META-INF/MANIFEST.MF")
                        || name.endsWith(".SF")
                        || name.endsWith(".RSA")
                        || name.endsWith(".DSA")
                        || name.endsWith(".EC")) {
                    drain(jf, e);
                    continue;
                }
                drain(jf, e);
                CodeSigner[] signers = e.getCodeSigners();
                if (signers == null || signers.length == 0) {
                    if (props.requireAllJarEntriesSigned()) {
                        return "Unsigned entry: " + name;
                    }
                    continue;
                }
                sawSignedEntry = true;
                if (!signerChainsToTrustedRoot(signers)) {
                    return "Entry signed but does not chain to trusted CA: " + name;
                }
            }
            if (!sawSignedEntry) return "JAR has no signed entries";
            return null;
        } catch (SecurityException se) {
            return "Signature verification failed: " + se.getMessage();
        } catch (IOException ioe) {
            return "JAR read failed: " + ioe.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractAndRegisterClaims(File jar) {
        try (JarFile jf = new JarFile(jar, true)) {
            JarEntry entry = jf.getJarEntry(IN_JAR_MANIFEST);
            if (entry == null) {
                // Signed JAR that ships no external configs — allowed
                return null;
            }
            byte[] bytes;
            try (InputStream in = jf.getInputStream(entry)) {
                bytes = in.readAllBytes();
            }
            CodeSigner[] signers = entry.getCodeSigners();
            if (signers == null || !signerChainsToTrustedRoot(signers)) {
                return IN_JAR_MANIFEST + " is not signed by a trusted CA";
            }
            Map<String, Object> raw = new Yaml().load(new ByteArrayInputStream(bytes));
            if (raw == null) return IN_JAR_MANIFEST + " is empty";

            Object claimsObj = raw.getOrDefault("claims", List.of());
            if (!(claimsObj instanceof List)) {
                return IN_JAR_MANIFEST + ": 'claims' must be a list";
            }
            List<Map<String, Object>> claims = (List<Map<String, Object>>) claimsObj;

            // Re-deployment hygiene: a JAR is frequently reloaded with a CHANGED config
            // (a new SHA-256 in this very manifest). Drop any claims previously registered
            // by a JAR of this same file name BEFORE re-registering. Without this, the
            // putIfAbsent below (first claim wins permanently) finds this JAR's OWN stale
            // claim from a prior load — possibly a load that failed at a later stage, e.g.
            // required-configs preflight, leaving the claim behind — and reports it as
            // "Conflicting claim ... also claimed by <itself>", wrongly quarantining the JAR.
            // Genuine conflicts (a DIFFERENT JAR claiming the same file) are still detected
            // because we only forget claims owned by this exact file name.
            forgetClaimsBy(jar.getName());

            for (Map<String, Object> c : claims) {
                Object nameObj = c.get("name");
                Object hashObj = c.get("sha256");
                if (!(nameObj instanceof String name) || !(hashObj instanceof String hash)) {
                    return "Malformed claim entry in " + IN_JAR_MANIFEST;
                }
                String normHash = hash.toLowerCase(Locale.ROOT);
                ClaimedFile newClaim = new ClaimedFile(normHash, jar.getName());
                ClaimedFile prior = expectedHashes.putIfAbsent(name, newClaim);
                if (prior != null && !prior.sha256().equals(normHash)) {
                    return "Conflicting claim for '" + name + "' (also claimed by "
                            + prior.claimingJar() + ")";
                }
            }

            // Optional: required-configs section, used for DB-config preflight.
            String reqError = parseAndRegisterRequiredConfigs(jar, raw);
            if (reqError != null) {
                return reqError;
            }
            return null;
        } catch (SecurityException se) {
            return "Signature check failed while reading manifest: " + se.getMessage();
        } catch (Exception e) {
            return "Failed to read " + IN_JAR_MANIFEST + ": " + e.getMessage();
        }
    }

    /**
     * Ensures that {@code plugin-config.yml} bundled inside the JAR (if present) is signed by a
     * trusted CA. Returns {@code null} when the entry is absent (a plugin may ship no in-JAR config)
     * or properly signed; otherwise a rejection reason. Reading the entry through a verifying
     * {@link JarFile} triggers per-entry signature validation; we then assert the code signers
     * chain to a trusted root independently of {@code require-all-jar-entries-signed}.
     */
    private String verifyInJarConfigSigned(File jar) {
        try (JarFile jf = new JarFile(jar, true)) {
            JarEntry entry = jf.getJarEntry(IN_JAR_CONFIG);
            if (entry == null) {
                return null; // signed JAR that ships no in-JAR config — allowed
            }
            try (InputStream in = jf.getInputStream(entry)) {
                in.readAllBytes(); // drain to trigger signature verification for this entry
            }
            CodeSigner[] signers = entry.getCodeSigners();
            if (signers == null || !signerChainsToTrustedRoot(signers)) {
                return IN_JAR_CONFIG + " is not signed by a trusted CA";
            }
            return null;
        } catch (SecurityException se) {
            return "Signature check failed while reading " + IN_JAR_CONFIG + ": " + se.getMessage();
        } catch (IOException ioe) {
            return "Failed to read " + IN_JAR_CONFIG + ": " + ioe.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String parseAndRegisterRequiredConfigs(File jar, Map<String, Object> manifest) {
        Object section = manifest.get("required-configs");
        if (section == null) {
            return null;
        }
        if (!(section instanceof Map<?, ?>)) {
            return IN_JAR_MANIFEST + ": 'required-configs' must be a map";
        }
        Map<String, Object> rc = (Map<String, Object>) section;

        Object ptObj = manifest.get("paymentType");
        if (!(ptObj instanceof String paymentType) || paymentType.isBlank()) {
            return IN_JAR_MANIFEST + ": 'paymentType' is required when 'required-configs' is present";
        }

        List<String> pluginWide = List.of();
        Object pwObj = rc.get("plugin-wide");
        if (pwObj != null) {
            if (!(pwObj instanceof List<?> pwList)) {
                return IN_JAR_MANIFEST + ": 'required-configs.plugin-wide' must be a list";
            }
            pluginWide = new ArrayList<>(pwList.size());
            for (Object e : pwList) {
                if (!(e instanceof String s) || s.isBlank()) {
                    return IN_JAR_MANIFEST + ": 'required-configs.plugin-wide' entries must be non-empty strings";
                }
                pluginWide.add(s);
            }
        }

        Map<String, List<String>> serviceScoped = new LinkedHashMap<>();
        Object ssObj = rc.get("service-scoped");
        if (ssObj != null) {
            if (!(ssObj instanceof Map<?, ?> ssMap)) {
                return IN_JAR_MANIFEST + ": 'required-configs.service-scoped' must be a map";
            }
            for (Map.Entry<?, ?> e : ssMap.entrySet()) {
                if (!(e.getKey() instanceof String svcType) || svcType.isBlank()) {
                    return IN_JAR_MANIFEST + ": 'required-configs.service-scoped' keys must be non-empty strings";
                }
                if (!(e.getValue() instanceof List<?> keys)) {
                    return IN_JAR_MANIFEST + ": 'required-configs.service-scoped." + svcType + "' must be a list";
                }
                List<String> svcKeys = new ArrayList<>(keys.size());
                for (Object k : keys) {
                    if (!(k instanceof String s) || s.isBlank()) {
                        return IN_JAR_MANIFEST + ": 'required-configs.service-scoped." + svcType
                                + "' entries must be non-empty strings";
                    }
                    svcKeys.add(s);
                }
                serviceScoped.put(svcType, List.copyOf(svcKeys));
            }
        }

        requiredConfigsByJar.put(jar.getName(),
                new RequiredConfigsClaim(paymentType, List.copyOf(pluginWide), Map.copyOf(serviceScoped)));
        return null;
    }

    private boolean signerChainsToTrustedRoot(CodeSigner[] signers) {
        for (CodeSigner cs : signers) {
            List<? extends Certificate> chain = cs.getSignerCertPath().getCertificates();
            for (Certificate c : chain) {
                if (trustedRoots.contains(c)) return true;
            }
            if (!chain.isEmpty() && trustedRoots.contains(chain.get(chain.size() - 1))) {
                return true;
            }
        }
        return false;
    }

    private void record(File f, VerificationRecord.FileType type,
                        VerificationRecord.Status status, String reason,
                        String hash, String claimedBy) {
        VerificationRecord r = new VerificationRecord(
                f.getName(), type, status, reason, hash, claimedBy, Instant.now());
        records.put(f.getName(), r);
        if (status == VerificationRecord.Status.REJECTED) {
            LOG.error("PLUGIN_REJECTED file={} type={} reason={}", f.getName(), type, reason);
        } else if (status == VerificationRecord.Status.MISCONFIGURED) {
            LOG.error("PLUGIN_MISCONFIGURED file={} type={} reason={}", f.getName(), type, reason);
        }
    }

    private String safeHash(File f) {
        try {
            return sha256Hex(f);
        } catch (Exception e) {
            return null;
        }
    }

    private static void drain(JarFile jf, JarEntry e) throws IOException {
        try (InputStream in = jf.getInputStream(e)) {
            byte[] buf = new byte[BUF];
            while (in.read(buf) != -1) {
                // read to trigger per-entry signature check
            }
        }
    }

    private static String sha256Hex(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
            byte[] buf = new byte[BUF];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}