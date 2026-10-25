package com.dotechly.gotechly.core.pipeline.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Enables async execution and configures a dedicated thread pool for audit persistence.
 *
 * <p>The audit executor is bounded: if the queue fills under heavy load,
 * {@link ThreadPoolExecutor.CallerRunsPolicy} causes the calling thread to
 * execute the audit write synchronously — so audit records are <strong>never</strong> dropped,
 * but payment response latency degrades gracefully under extreme back-pressure.</p>
 *
 * <p>On shutdown, pending audit writes are drained before the JVM exits.</p>
 */
@Configuration
@EnableAsync
public class AuditAsyncConfiguration {

    @Bean("auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
