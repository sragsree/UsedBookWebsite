/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(
        csrf ->
            csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers(
                    "/",
                    "/index.html",
                    "/admin.html",
                    "/app.js",
                    "/styles.css",
                    "/image/**",
                    "/error",
                    "/api/csrf")
                .permitAll()
                .requestMatchers("/book/inventory")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/book/bookList", "/book/{id}")
                .permitAll()
                .requestMatchers("/book/searchBook")
                .permitAll()
                .requestMatchers("/book/**", "/api/prices/**", "/api/rentals/**")
                .hasRole("ADMIN")
                .requestMatchers("/user/newUser")
                .permitAll()
                .requestMatchers("/payment/**", "/shipping/**", "/user/forgetPassword")
                .denyAll()
                .anyRequest()
                .authenticated());
    http.httpBasic(
        basic ->
            basic.authenticationEntryPoint(
                (req, res, error) -> res.sendError(401, "Sign in required")));
    http.logout(
        logout ->
            logout
                .logoutUrl("/api/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)));
    http.headers(
        headers ->
            headers.contentSecurityPolicy(
                csp ->
                    csp.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'"
                            + " data:; frame-ancestors 'none'")));
    return http.build();
  }
}
