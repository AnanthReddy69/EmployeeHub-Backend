package com.anurag.ai.config;

import com.anurag.ai.service.MyUserDetailsService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class Security {

    private final JWTFilter jwtFilter;
    private final MyUserDetailsService userDetailsService;

    public Security(
            JWTFilter jwtFilter,
            MyUserDetailsService userDetailsService) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(
            PasswordEncoder encoder) {
        DaoAuthenticationProvider provider =
            new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(encoder);

        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider provider) throws Exception {

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(provider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/employees/register",
                    "/api/employees/login"
                ).permitAll()

                .requestMatchers("/error").permitAll()

                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/employees/*/role"
                ).hasRole("MANAGER")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/employees/**"
                ).hasAnyRole("HR", "MANAGER")

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/employees/**"
                ).hasAnyRole("HR", "MANAGER")

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/employees/**"
                ).hasAnyRole("EMPLOYEE", "HR", "MANAGER")

                .requestMatchers(
                    "/api/attendance/check-in",
                    "/api/attendance/check-out",
                    "/api/attendance/me"
                ).hasAnyRole("EMPLOYEE", "HR", "MANAGER")

                .requestMatchers("/api/attendance/**")
                    .hasAnyRole("HR", "MANAGER")

                .requestMatchers(
                    HttpMethod.POST, "/api/leaves"
                ).hasAnyRole("EMPLOYEE", "HR", "MANAGER")

                .requestMatchers(
                    HttpMethod.GET, "/api/leaves/me"
                ).hasAnyRole("EMPLOYEE", "HR", "MANAGER")

                .requestMatchers("/api/leaves/**")
                    .hasAnyRole("HR", "MANAGER")

                .requestMatchers(
                    HttpMethod.GET, "/api/announcements/**"
                ).hasAnyRole("EMPLOYEE", "HR", "MANAGER")

                .requestMatchers(
                    HttpMethod.POST, "/api/announcements/**"
                ).hasAnyRole("HR", "MANAGER")

                .anyRequest().authenticated()
            )
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();

        cors.setAllowedOrigins(List.of(
    "http://localhost:5173",
    "https://employeehub-frontend-xt0h.onrender.com"
));
        cors.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        cors.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "Accept"
        ));
        cors.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}