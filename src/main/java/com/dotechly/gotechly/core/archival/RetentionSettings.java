package com.dotechly.gotechly.core.archival;

import com.dotechly.gotechly.core.repository.AppConfigsRepository;
import com.dotechly.gotechly.core.service.impl.AppConfigsCryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves an archival retention window from {@code app_configs}, so every node reads one value out
 * of the database instead of whatever its own packaged {@code application.yml} happens to say.
 *
 * <p>The yml value survives as the fallback, not as the answer: a missing row, a blank one or a
 * value that is not a number all fall back to it and log. An archival job must never be cancelled by
 * a bad config row — skipping a night is a real cost, and the operator who fat-fingered the row is
 * not watching at 00:00.
 *
 * <h2>Why this reads the repository and not {@code AppConfigsService.findByConfigId}</h2>
 * That method is {@code @Cacheable} on a {@code ConcurrentMapCacheManager} with no TTL, and a
 * {@code save}/{@code delete} evicts only the JVM that served the write. A peer node would therefore
 * pin the first value it ever read and keep using it until restart — the exact opposite of the one
 * source of truth this class exists to provide. A direct read is also free here: one row on a unique
 * index, once per job, once per night.
 *
 * <p>The value is still run through {@link AppConfigsCryptoService#decryptIfNeeded} so a row someone
 * marked {@code is_sensitive} resolves rather than arriving as {@code enc:v1:…} and failing to parse.
 * That call throws on undecryptable ciphertext, which is why it sits inside the same catch as the
 * parse.
 */
@Component
public class RetentionSettings {

    /**
     * {@code app_configs.config_id} for each archival window, in days.
     *
     * <p>Deliberately <b>not</b> {@code core.}-prefixed. {@code PluginLoader.createContextForPlugin}
     * merges every {@code core.*} row into every plugin's {@code PluginContext}, and a Core archival
     * setting has no business appearing in a plugin's property map. The ids otherwise mirror the yml
     * key paths so the two map one-to-one for whoever is reading a config dump.
     */
    public static final String PAYMENT_LOG_RETENTION = "eod.payment-log-retention-days";
    /** @see #PAYMENT_LOG_RETENTION */
    public static final String AUDIT_LOG_RETENTION = "eod.audit-log-retention-days";
    /** @see #PAYMENT_LOG_RETENTION */
    public static final String DEFERRED_RECOVERY_RETENTION = "eod.deferred-recovery-retention-days";

    private final Logger log = LoggerFactory.getLogger(RetentionSettings.class);

    private final AppConfigsRepository appConfigsRepository;
    private final AppConfigsCryptoService crypto;

    public RetentionSettings(AppConfigsRepository appConfigsRepository, AppConfigsCryptoService crypto) {
        this.appConfigsRepository = appConfigsRepository;
        this.crypto = crypto;
    }

    /**
     * Retention window in days for {@code configId}, falling back to {@code fallbackDays} when the
     * row is absent or unusable.
     *
     * <p>Negative values are clamped to 0 whichever source they came from: a negative window puts the
     * cutoff in the future, which would archive rows written after the run started.
     */
    public int days(String configId, int fallbackDays) {
        int fallback = Math.max(0, fallbackDays);

        String raw;
        try {
            raw = appConfigsRepository.findByConfigId(configId)
                    .map(row -> crypto.decryptIfNeeded(row.getConfigValue()))
                    .orElse(null);
        } catch (RuntimeException e) {
            // Includes the DB being unreachable and decryptIfNeeded rejecting the stored value.
            log.warn("Could not read retention config '{}' — using the configured fallback of {} day(s).",
                    configId, fallback, e);
            return fallback;
        }

        if (raw == null || raw.isBlank()) {
            log.info("No app_configs row for '{}' — using the configured fallback of {} day(s). "
                    + "Insert the row to set retention centrally for every node.", configId, fallback);
            return fallback;
        }

        int days;
        try {
            days = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("app_configs['{}'] is '{}', which is not a whole number of days — "
                    + "using the configured fallback of {} day(s).", configId, raw, fallback);
            return fallback;
        }

        if (days < 0) {
            log.warn("app_configs['{}'] is {}, which would put the cutoff in the future — treating it as 0.",
                    configId, days);
            return 0;
        }
        return days;
    }
}
