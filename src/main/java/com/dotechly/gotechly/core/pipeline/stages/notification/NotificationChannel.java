package com.dotechly.gotechly.core.pipeline.stages.notification;

/**
 * The notification channels Core can dispatch, and everything that differs between them.
 *
 * <p>Each is a separate registered pipeline stage, so a corridor enables SMS, e-mail or both by
 * listing the stage names in its pipeline config — no extra table, and the decision is visible in
 * the same place as the rest of the pipeline.
 */
public enum NotificationChannel {

    /** {@code sms-notification} — template {@code <paymentType>-SMS}, override key {@code smsTemplate}. */
    SMS("sms-notification", "smsTemplate", "SMS", "sms", 4500),

    /** {@code email-notification} — template {@code <paymentType>-EMAIL}, override key {@code emailTemplate}. */
    EMAIL("email-notification", "emailTemplate", "EMAIL", "email", 4600);

    private final String stageName;
    private final String overrideKey;
    private final String templateSuffix;
    private final String fieldPrefix;
    private final int defaultOrder;

    NotificationChannel(String stageName, String overrideKey, String templateSuffix,
                        String fieldPrefix, int defaultOrder) {
        this.stageName = stageName;
        this.overrideKey = overrideKey;
        this.templateSuffix = templateSuffix;
        this.fieldPrefix = fieldPrefix;
        this.defaultOrder = defaultOrder;
    }

    /** Name this stage is referenced by in {@code default-stages} and plugin {@code pipeline:} config. */
    public String stageName() {
        return stageName;
    }

    /**
     * {@code Details.additionalFields} key a plugin sets to choose the template for one
     * transaction, overriding the {@code <paymentType>-<suffix>} convention.
     */
    public String overrideKey() {
        return overrideKey;
    }

    /** Suffix of the conventional {@code template_lookup.template_name}. */
    public String templateSuffix() {
        return templateSuffix;
    }

    /**
     * Prefix for the outcome keys written back to {@code Details.additionalFields} —
     * {@code smsNotificationStatus}, {@code emailNotificationHttpStatus}, and so on.
     */
    public String fieldPrefix() {
        return fieldPrefix;
    }

    /**
     * Advisory ordering only: the pipeline runs stages in the order the config lists them. Both sit
     * after {@code db-update} (1001) and before {@code callback} (5000), which is where they belong
     * when a pipeline is assembled from {@code defaultOrder} rather than an explicit list.
     */
    public int defaultOrder() {
        return defaultOrder;
    }
}
