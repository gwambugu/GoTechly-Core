package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util;

import java.util.concurrent.atomic.AtomicInteger;

import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.apache.commons.lang3.StringUtils;

public class UniqueNumberGenerator {

    private static final AtomicInteger counter = new AtomicInteger(0);
    private static long lastTimeMillis = System.currentTimeMillis();

    public static synchronized String getStan(PaymentLogSPI paymentLogSPI) {
        long currentTime = System.currentTimeMillis();
        if (currentTime != lastTimeMillis) {
            lastTimeMillis = currentTime;
            counter.set(0);
        }
        int count = counter.getAndIncrement();
        long x = currentTime * 1000 + count; // up to 1000 unique IDs per ms
        String xStr = String.valueOf(x);
        xStr = xStr.replaceFirst("0+$", "");
        if (paymentLogSPI != null) {
            String ref = getLastThreeChars(paymentLogSPI.getDetails().getRequestId());
            if (StringUtils.isNumeric(ref)) {
                xStr = xStr + ref;
            }
        }
        String stan = xStr.length() > 6 ? xStr.substring(xStr.length() - 6) : xStr;
        return stan;
    }


    public static String getLastThreeChars(String input) {
        if (input == null || input.length() < 3) {
            return input; // or return ""; depending on your requirement
        }
        return input.substring(input.length() - 3);
    }
}
