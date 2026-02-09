package com.lorman.ref.spring.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.INSTANCE)
                .authorizeExchange(registry -> registry
                        .pathMatchers("/actuator/health", "/actuator/health/**", "/dummy").permitAll()
                        .pathMatchers(HttpMethod.GET, "/auta/**").hasRole("READ")
                        .pathMatchers(HttpMethod.POST, "/auta").hasRole("WRITE")
                        .pathMatchers(HttpMethod.PUT, "/auta/**").hasRole("UPDATE")
                        .pathMatchers(HttpMethod.DELETE, "/auta/**").hasRole("WRITE")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(this::jwtAuthenticationConverter))
                )
                .build();
    }

    private Mono<AbstractAuthenticationToken> jwtAuthenticationConverter(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return Mono.just(new JwtAuthenticationToken(jwt, authorities));
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object rolesObj = jwt.getClaim("roles");
        if (rolesObj instanceof Collection<?> col) {
            return col.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
        if (rolesObj instanceof String s) {
            return List.of(new SimpleGrantedAuthority(s.startsWith("ROLE_") ? s : "ROLE_" + s));
        }
        return List.of();
    }

    @Bean
    public NimbusReactiveJwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }

    /**
     * Simple no-op security context repository to keep the app stateless for WebFlux.
     */
    enum NoOpServerSecurityContextRepository implements org.springframework.security.web.server.context.ServerSecurityContextRepository {
        INSTANCE;

        @Override
        public Mono<Void> save(org.springframework.web.server.ServerWebExchange exchange, org.springframework.security.core.context.SecurityContext context) {
            return Mono.empty();
        }

        @Override
        public Mono<org.springframework.security.core.context.SecurityContext> load(org.springframework.web.server.ServerWebExchange exchange) {
            return Mono.empty();
        }
    }
}
