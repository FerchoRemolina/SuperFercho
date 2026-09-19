package com.superfercho.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.PasswordHasher;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class IdentitySecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    PasswordHasher passwordHasher(PasswordEncoder passwordEncoder) {
        return new BCryptPasswordHasher(passwordEncoder);
    }

    @Bean
    JwtAccessTokenService jwtAccessTokenService(JwtProperties jwtProperties, Clock clock) {
        return new JwtAccessTokenService(jwtProperties, clock);
    }

    @Bean
    CurrentUserProvider currentUserProvider() {
        return new SpringSecurityCurrentUserProvider();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAccessTokenService jwtAccessTokenService,
            ObjectMapper objectMapper)
            throws Exception {
        SecurityProblemDetailResponses problemResponses = new SecurityProblemDetailResponses(objectMapper);
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtAccessTokenService, problemResponses);
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new ProblemDetailAuthenticationEntryPoint(problemResponses))
                        .accessDeniedHandler(new ProblemDetailAccessDeniedHandler(problemResponses)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/customers")
                        .permitAll()
                        .requestMatchers("/error")
                        .permitAll()
                        .requestMatchers(CatalogGetRequestMatcher.adminView())
                        .hasRole("ADMIN")
                        .requestMatchers(CatalogGetRequestMatcher.publicView())
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/categories",
                                "/api/v1/categories/{categoryId}/activate",
                                "/api/v1/categories/{categoryId}/deactivate")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/{categoryId}")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/products",
                                "/api/v1/products/{productId}/activate",
                                "/api/v1/products/{productId}/deactivate",
                                "/api/v1/products/{productId}/price")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/{productId}")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/{orderId}/status")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/{paymentId}")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/knowledge/documents",
                                "/api/v1/knowledge/documents/{documentId}/process",
                                "/api/v1/knowledge/documents/{documentId}/deactivate",
                                "/api/v1/knowledge/documents/{documentId}/reactivate")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/knowledge/documents",
                                "/api/v1/knowledge/documents/{documentId}",
                                "/api/v1/knowledge/search")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/knowledge/documents/{documentId}/content")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/addresses", "/api/v1/addresses/**")
                        .hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/cart", "/api/v1/cart/**")
                        .hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/shopping-lists", "/api/v1/shopping-lists/**")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/{orderId}")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/{orderId}/cancel")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/assistant/chat")
                        .hasRole("CUSTOMER")
                        .anyRequest()
                        .denyAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
