package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.config.drain.NodeDrainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Node drain control: take this Core instance out of the Gateway's routing pool, watch it go quiet,
 * then deploy and restart it. See {@code docs/controlled-plugin-deployment.md}.
 *
 * <p>{@code GET /routable} is what the Gateway load balancer probes
 * ({@code spring.cloud.loadbalancer.health-check.path.default}). The other three are operator
 * actions.
 */
@RestController
@RequestMapping("/api/v1/admin/node")
public class NodeDrainResource {

    private static final Logger LOG = LoggerFactory.getLogger(NodeDrainResource.class);

    /**
     * Portal user forwarded by the Gateway, which is the only party that knows who is logged in.
     * Absent or blank records as {@code unknown} rather than failing: refusing a drain because an
     * identity header was missing would block the very action an operator needs during an incident.
     */
    private static final String REQUESTED_BY = "X-Requested-By";

    private final NodeDrainService drainService;
    private final ObjectProvider<HealthEndpoint> healthEndpoint;

    public NodeDrainResource(NodeDrainService drainService, ObjectProvider<HealthEndpoint> healthEndpoint) {
        this.drainService = drainService;
        this.healthEndpoint = healthEndpoint;
    }

    /**
     * GET /api/v1/admin/node/routable — the load-balancer probe.
     *
     * <p>200 when this instance should receive traffic, 503 when it should not. Spring Cloud
     * LoadBalancer's {@code HealthCheckServiceInstanceListSupplier} only looks at the status code,
     * so this does not have to be an actuator endpoint — and deliberately is not. A
     * {@code HealthIndicator} contributes to the aggregate {@code /actuator/health} as well as to
     * its group, and there is no supported way to keep it out of the aggregate; a drained node
     * would then be indistinguishable from a crashed one on the Gateway's own health page, which
     * reads {@code /actuator/health} per instance. Answering here keeps the two states separate.
     *
     * <p>It still consults the aggregate health, so the pre-existing behaviour — an instance whose
     * database is down drops out of the pool — is preserved rather than replaced by a flag that is
     * always true.
     */
    @GetMapping("/routable")
    public ResponseEntity<Map<String, Object>> routable() {
        boolean draining = drainService.isDraining();
        boolean healthy = aggregateHealthIsUp();
        boolean routable = !draining && healthy;

        Map<String, Object> body = Map.of(
                "routable", routable,
                "state", drainService.getState().name(),
                "health", healthy ? "UP" : "NOT_UP");

        return routable
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /** GET /api/v1/admin/node/drain — drain state and what is still running here. */
    @GetMapping("/drain")
    public ResponseEntity<NodeDrainService.Status> status() {
        return ResponseEntity.ok(drainService.status());
    }

    /**
     * POST /api/v1/admin/node/drain — stop accepting new work.
     *
     * <p>200 whether or not this call changed anything; draining an already-draining node is not an
     * error, and an operator retrying after a timeout must not see a failure. {@code changed} says
     * which it was.
     *
     * <p>This instance keeps serving whatever it already accepted. It is removed from the pool at
     * the LB's next probe, so poll {@code GET /drain} until {@code quiet} before deploying.
     */
    @PostMapping("/drain")
    public ResponseEntity<Map<String, Object>> drain(
            @RequestHeader(value = REQUESTED_BY, required = false) String requestedBy) {
        boolean changed = drainService.drain(requestedBy);
        LOG.info("REST request to drain this node by {} (changed={})", requestedBy, changed);
        return ResponseEntity.ok(Map.of("changed", changed, "status", drainService.status()));
    }

    /** POST /api/v1/admin/node/undrain — resume accepting new work. */
    @PostMapping("/undrain")
    public ResponseEntity<Map<String, Object>> undrain(
            @RequestHeader(value = REQUESTED_BY, required = false) String requestedBy) {
        boolean changed = drainService.undrain(requestedBy);
        LOG.info("REST request to undrain this node by {} (changed={})", requestedBy, changed);
        return ResponseEntity.ok(Map.of("changed", changed, "status", drainService.status()));
    }

    /**
     * Treats an unavailable health endpoint as healthy: the probe's job is to report drain state,
     * and failing every probe because the actuator is switched off would empty the pool.
     */
    private boolean aggregateHealthIsUp() {
        HealthEndpoint endpoint = healthEndpoint.getIfAvailable();
        if (endpoint == null) {
            return true;
        }
        try {
            return org.springframework.boot.health.contributor.Status.UP.equals(endpoint.health().getStatus());
        } catch (RuntimeException ex) {
            LOG.warn("Health aggregation failed while answering the routable probe: {}", ex.getMessage());
            return false;
        }
    }
}
