package com.dotechly.gotechly.core.config.plugin;


import java.time.Instant;

public record VerificationRecord(
        String fileName,
        FileType type,
        Status status,
        String reason,
        String sha256,
        String claimedBy,
        Instant checkedAt
) {
    public enum FileType { JAR, CONFIG }
    public enum Status { VERIFIED, REJECTED, SKIPPED, MISCONFIGURED }
}
