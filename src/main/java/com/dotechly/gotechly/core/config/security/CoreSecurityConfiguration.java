package com.dotechly.gotechly.core.config.security;


import com.dotechly.gotechly.core.config.drain.InFlightRequestFilter;
import com.dotechly.gotechly.core.config.drain.NodeDrainService;
import com.dotechly.gotechly.core.service.idempotency.IdempotencyService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({InternalApiKeyProperties.class, IdempotencyProperties.class})
public class CoreSecurityConfiguration {

    @Bean
    public InternalApiKeyFilter internalApiKeyFilter(InternalApiKeyProperties properties) {
        return new InternalApiKeyFilter(properties);
    }

    @Bean
    public IdempotencyFilter idempotencyFilter(IdempotencyService idempotencyService,
                                               IdempotencyProperties properties) {
        return new IdempotencyFilter(idempotencyService, properties);
    }

    /**
     * The in-flight counter is registered as a plain servlet filter, <b>not</b> added to the
     * Spring Security chain.
     *
     * <p>Two reasons, the first learned the hard way. {@code HttpSecurity.addFilterBefore(f, X.class)}
     * resolves {@code X}'s position from a registry that is only populated when {@code X} is itself
     * added — so referencing {@link InternalApiKeyFilter} <em>before</em> the line that adds it fails
     * at startup with {@code "does not have a registered order"}, taking the whole application down.
     * Call order and chain order are different things there, and the coupling is easy to reintroduce.
     *
     * <p>Second, and the reason this is the better home anyway: registered here at
     * {@code HIGHEST_PRECEDENCE} the counter wraps the security filter chain itself, so a request
     * rejected by authentication or one that matches no route is still counted for as long as it
     * lives. A drained node must not report itself quiet while anything at all is still in the
     * container.
     *
     * <p>Declared only as a {@code FilterRegistrationBean} — exposing the filter as a bare
     * {@code Filter} bean as well would have Boot auto-register a second copy.
     */
    @Bean
    public FilterRegistrationBean<InFlightRequestFilter> inFlightRequestFilterRegistration(
            NodeDrainService drainService) {
        FilterRegistrationBean<InFlightRequestFilter> registration =
                new FilterRegistrationBean<>(new InFlightRequestFilter(drainService));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        registration.setName("inFlightRequestFilter");
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           InternalApiKeyFilter apiKeyFilter,
                                           IdempotencyFilter idempotencyFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid API key\"}");
                        })
                )
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                // Dedup runs AFTER authentication so unauthenticated probes can't claim keys.
                .addFilterAfter(idempotencyFilter, InternalApiKeyFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                // The Gateway LB probe. permitAll for the same reason /actuator/health is:
                                // it is how the LB decides whether this instance exists, so gating it on the
                                // internal API key would turn one misconfigured key into "No servers
                                // available". mTLS (client-auth: need) is still the transport boundary, and
                                // the response carries only this node's routability — no payment data.
                                // The drain/undrain MUTATIONS below stay authenticated.
                                "/api/v1/admin/node/routable"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
