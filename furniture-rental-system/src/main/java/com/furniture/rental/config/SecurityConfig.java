package com.furniture.rental.config;

import com.furniture.rental.entity.User;
import com.furniture.rental.repository.UserRepository;
import com.furniture.rental.security.JwtAuthenticationFilter;
import com.furniture.rental.security.JwtService;
import com.furniture.rental.security.LegacyAwarePasswordEncoder;
import com.furniture.rental.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtService jwtService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, JwtService jwtService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/auth/login"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/", "/home", "/register", "/login", "/css/**", "/js/**", "/images/**",
                                "/uploads/**", "/api/auth/**", "/about", "/category/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/furniture/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/furniture/*/reviews", "/reviews/**").hasRole("CUSTOMER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/customer/**", "/rent/**", "/cart", "/cart/**", "/reviews/**")
                        .hasRole("CUSTOMER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            UserDetails principal = (UserDetails) authentication.getPrincipal();
                            String token = jwtService.generateToken(principal);
                            response.addHeader(HttpHeaders.SET_COOKIE, jwtService.createAuthCookie(token).toString());
                            boolean admin = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
                            response.sendRedirect(admin ? "/admin/dashboard" : "/");
                        })
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.addHeader(HttpHeaders.SET_COOKIE, jwtService.clearAuthCookie().toString());
                            response.sendRedirect("/");
                        }))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new LegacyAwarePasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder, UserDetailsPasswordService passwordUpgradeService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsPasswordService(passwordUpgradeService);
        return provider;
    }

    @Bean
    public UserDetailsPasswordService passwordUpgradeService(UserRepository userRepository,
            CustomUserDetailsService userDetailsService) {
        return (userDetails, newPassword) -> {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found during password upgrade"));
            user.setPassword(newPassword);
            userRepository.save(user);
            return userDetailsService.loadUserByUsername(user.getUsername());
        };
    }
}
