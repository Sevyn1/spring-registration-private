package io.github.sevyn1.access;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class AccessSecurity {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService accountDetails(AccountStore store) {
    return username -> {
      var account =
          store
              .find(Accounts.normalize(username))
              .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
      return User.withUsername(account.username()).password(account.hash()).roles("MEMBER").build();
    };
  }

  @Bean
  SecurityFilterChain accessFilter(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/",
                        "/index.html",
                        "/styles.css",
                        "/app.js",
                        "/api/csrf",
                        "/api/accounts",
                        "/api/session",
                        "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errors ->
                errors.authenticationEntryPoint(
                    (req, res, e) -> {
                      res.setStatus(401);
                      res.setContentType("application/json");
                      res.getWriter().write("{\"message\":\"Sign in to continue.\"}");
                    }))
        .formLogin(
            login ->
                login
                    .loginProcessingUrl("/api/session")
                    .successHandler((req, res, auth) -> res.setStatus(204))
                    .failureHandler(
                        (req, res, e) -> {
                          res.setStatus(401);
                          res.setContentType("application/json");
                          res.getWriter()
                              .write("{\"message\":\"Username or password incorrect.\"}");
                        }))
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/session/logout")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    csp ->
                        csp.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self'; img-src"
                                + " 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri"
                                + " 'self'; form-action 'self'")))
        .build();
  }
}
