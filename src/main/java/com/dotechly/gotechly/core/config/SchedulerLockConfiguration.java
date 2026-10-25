package com.dotechly.gotechly.core.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.sql.DataSource;

/**
 * Distributed lock backing for @Scheduled methods so that in a multi-instance
 * Core deployment only one instance runs each scheduled job per fire time.
 *
 * usingDbTime() makes ShedLock evaluate lock_until against the database clock
 * (CURRENT_TIMESTAMP) instead of the JVM clock — eliminates the cross-instance
 * clock-skew failure mode where two Cores disagree about whether a lock has
 * expired. Works on both Postgres (dev) and Oracle (prod).
 *
 * defaultLockAtMostFor is the hard ceiling: if the instance holding the lock
 * crashes without releasing it, the lock auto-expires after this duration and
 * another instance can take over on the next fire. Tune per-job via the
 * lockAtMostFor attribute on @SchedulerLock.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class SchedulerLockConfiguration {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build());
    }

    /**
     * The scheduler every @Scheduled method runs on. Declared explicitly rather than left to
     * Boot's auto-configuration, which cannot supply it here.
     *
     * <p>{@code OAuth2TokenProviderConfiguration} publishes a {@code ScheduledExecutorService}
     * bean for proactive token refresh. Boot's auto-configured {@code taskScheduler} is
     * {@code @ConditionalOnMissingBean({SchedulingConfigurer.class, TaskScheduler.class,
     * ScheduledExecutorService.class})}, so that bean silently suppresses it — and
     * {@code ScheduledAnnotationBeanPostProcessor}, finding no {@code TaskScheduler}, falls back
     * to the lone {@code ScheduledExecutorService}. Net effect: EOD archival, audit archival and
     * EchoService all ran on the single-threaded {@code oauth2-token-refresh} pool, and
     * {@code spring.task.scheduling.pool.size} was silently ignored.
     *
     * <p>A {@code TaskScheduler} bean is resolved ahead of the {@code ScheduledExecutorService}
     * fallback, so defining this restores the intended separation. Do not delete it when the
     * OAuth2 scheduler bean changes shape — verify with a thread name in the scheduled job's log
     * line: it must read {@code gotechly-sched-*}, never {@code oauth2-token-refresh}.
     */
    @Bean
    public TaskScheduler taskScheduler(
            @Value("${spring.task.scheduling.pool.size:5}") int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("gotechly-sched-");
        // Let in-flight EOD work finish rather than being cut mid-transaction on shutdown.
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
