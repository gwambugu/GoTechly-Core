package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util;

import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.tm.bean.AppQBean;
import org.jpos.util.NameRegistrar;

/**
 * Picks the jPOS transaction-manager queue for a country, and resolves the {@code appQBean} that
 * owns the space configuration.
 *
 * <p>Both lookups happen <b>before</b> anything is placed on the space, so a failure in either is a
 * {@link CbsNotPlacedException} — definitively not posted. The previous inline {@code switch} threw
 * a bare {@link IllegalArgumentException} on an unrecognised country code, which escaped the stage
 * entirely: {@code PipelineExecutor} logged it, left {@code context.isHalted()} false, and no
 * compensation ran, so the scheme was never told anything about a transfer that had never even been
 * attempted.
 */
public final class CbsQueueResolver {

    private CbsQueueResolver() {
    }

    public static AppQBean appQBean() {
        try {
            return (AppQBean) NameRegistrar.get("appQBean");
        } catch (NameRegistrar.NotFoundException e) {
            throw new CbsNotPlacedException("Core-banking dispatcher 'appQBean' is not registered", e);
        }
    }

    public static String nextQueue(String countryCode) {
        if (countryCode == null) {
            throw new CbsNotPlacedException("No sender country code — cannot route to a core-banking queue");
        }
        return switch (countryCode) {
            case "KE" -> AppQBean.nextKeFinTransactionManagerQueue();
            case "UG" -> AppQBean.nextUgFinTransactionManagerQueue();
            case "RW" -> AppQBean.nextRwFinTransactionManagerQueue();
            case "TZ" -> AppQBean.nextTzFinTransactionManagerQueue();
            case "CD" -> AppQBean.nextCdFinTransactionManagerQueue();
            case "SS" -> AppQBean.nextSsFinTransactionManagerQueue();
            default -> throw new CbsNotPlacedException(
                    "No core-banking queue for country code '" + countryCode + "'");
        };
    }

    /** Country code off the sender, tolerating a log with no sender block. */
    public static String countryCode(com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI log) {
        return (log == null || log.getSender() == null) ? null : log.getSender().getCountryCode();
    }
}
