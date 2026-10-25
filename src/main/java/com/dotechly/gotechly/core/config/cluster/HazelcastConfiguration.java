package com.dotechly.gotechly.core.config.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Embedded Hazelcast cluster for cross-instance coordination in multi-instance Core deployments.
 * <p>
 * Each Core JVM is a cluster member (embedded — no separate Hazelcast servers). Members discover
 * each other over TCP/IP using a static member list (multicast is intentionally disabled, as it is
 * usually blocked in production). Two facilities are built on top of this instance:
 * <ul>
 *   <li>{@link ClusterConfigRefresher} — fans out approved app_configs / error_codes changes so
 *       every instance refreshes its loaded plugins without a restart.</li>
 *   <li>{@code IdempotencyService} — a cluster-wide {@code IMap} with TTL for POST-safe retry
 *       dedup (the Redis-free replacement for Idempotency-Key handling).</li>
 * </ul>
 * Spring Boot's {@code HazelcastAutoConfiguration} builds the {@code HazelcastInstance} from this
 * {@link Config} bean.
 * <p>
 * With an empty member list Hazelcast forms a single-member cluster, so dev / single-instance runs
 * work unchanged. In production set {@code gotechly.hazelcast.members} to every Core host:port.
 */
@Configuration
public class HazelcastConfiguration {

    public static final String IDEMPOTENCY_MAP = "idempotency";
    public static final String RENDEZVOUS_MAP = "payment-rendezvous";

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastConfiguration.class);

    @Bean
    public Config hazelcastConfig(
            @Value("${gotechly.hazelcast.cluster-name:gotechly-core}") String clusterName,
            @Value("${gotechly.hazelcast.port:5701}") int port,
            @Value("${gotechly.hazelcast.port-auto-increment:true}") boolean portAutoIncrement,
            @Value("${gotechly.hazelcast.port-count:3}") int portCount,
            @Value("${gotechly.hazelcast.members:}") List<String> members,
            @Value("${gotechly.idempotency.ttl-seconds:86400}") int idempotencyTtlSeconds,
            @Value("${gotechly.idempotency.map-backup-count:1}") int idempotencyBackups,
            @Value("${gotechly.rendezvous.key-ttl-seconds:30}") int rendezvousTtlSeconds) {

        Config config = new Config();
        config.setClusterName(clusterName);
        // Keep the cluster quiet on diagnostics unless explicitly enabled.
        config.setProperty("hazelcast.logging.type", "slf4j");
        config.setProperty("hazelcast.phone.home.enabled", "false");

        NetworkConfig net = config.getNetworkConfig();
        // Auto-increment lets co-located instances (e.g. 3 dev JVMs on localhost) each grab the
        // next free port (5701, 5702, 5703...). On separate hosts the first port is used.
        net.setPort(port).setPortAutoIncrement(portAutoIncrement).setPortCount(Math.max(1, portCount));

        JoinConfig join = net.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(true);
        // Filter blanks so an unset/empty property yields a lone-member cluster rather than a
        // member named "" (which Hazelcast would reject).
        List<String> cleaned = members.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        join.getTcpIpConfig().setMembers(cleaned);

        // Idempotency store: self-expiring, backed up so a single member loss doesn't drop keys.
        MapConfig idem = config.getMapConfig(IDEMPOTENCY_MAP);
        idem.setTimeToLiveSeconds(idempotencyTtlSeconds);
        idem.setBackupCount(Math.max(0, idempotencyBackups));

        // Payment rendezvous store: short-lived cross-instance hand-off of async results. Entries
        // self-expire well within the caller's wait window, so a result that never gets consumed
        // (parked caller already timed out / gone) is reclaimed automatically. No backup needed —
        // a lost result simply degrades to the caller's timeout, same as the old Valkey path.
        MapConfig rendezvous = config.getMapConfig(RENDEZVOUS_MAP);
        rendezvous.setTimeToLiveSeconds(rendezvousTtlSeconds);
        rendezvous.setBackupCount(0);

        LOG.info("Hazelcast embedded cluster '{}' on port {} (auto-increment={}, count={}) — " +
                        "members={}, idempotency TTL={}s, backups={}, rendezvous TTL={}s",
                clusterName, port, portAutoIncrement, portCount,
                cleaned.isEmpty() ? "[self only]" : cleaned, idempotencyTtlSeconds, idempotencyBackups,
                rendezvousTtlSeconds);
        return config;
    }

    @Bean
    public HazelcastInstance hazelcastInstance(Config hazelcastConfig) {
        return Hazelcast.newHazelcastInstance(hazelcastConfig);
    }
}
