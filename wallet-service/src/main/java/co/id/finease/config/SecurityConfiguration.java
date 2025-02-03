package co.id.finease.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;

    private final CorsFilter corsFilter;

    public SecurityConfiguration(SessionAuthenticationFilter sessionAuthenticationFilter, CorsFilter corsFilter) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
        this.corsFilter = corsFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/v1/**").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(corsFilter, SecurityContextPersistenceFilter.class)
                .addFilterBefore(sessionAuthenticationFilter, FilterSecurityInterceptor.class);
        return http.build();
    }


}