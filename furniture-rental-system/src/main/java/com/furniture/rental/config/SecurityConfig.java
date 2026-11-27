package com.furniture.rental.config;

import com.furniture.rental.entity.User;
import com.furniture.rental.repository.UserRepository;
import com.furniture.rental.security.JwtAuthenticationFilter;
import com.furniture.rental.security.JwtService;
import com.furniture.rental.security.LegacyAwarePasswordEncoder;
import com.furniture.rental.security.LoginAttemptListener;
import com.furniture.rental.security.LoginAttemptService;
import com.furniture.rental.security.LoginProtectionProperties;
import com.furniture.rental.security.LoginRateLimitFilter;
import com.furniture.rental.service.CustomUserDetailsService;
import com.furniture.rental.security.JsonAccessDeniedHandler;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
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
import com.furniture.rental.security.JsonAuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import java.util.LinkedHashMap;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(LoginProtectionProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtService jwtService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, JwtService jwtService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider,
            LoginAttemptService loginAttempts,
            LoginProtectionProperties loginProperties) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/auth/login")
                        .ignoringRequestMatchers(request -> {
                            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
                            return header != null && header.startsWith("Bearer ");
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/", "/home", "/register", "/login", "/css/**", "/js/**", "/images/**",
                                "/uploads/**", "/api/auth/**", "/about", "/category/**")
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/furniture/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/furniture/*/reviews", "/reviews/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
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
                            String token = jwtService.resolveToken(request);
                            if (token != null) {
                                jwtService.revokeToken(token);
                            }
                            response.addHeader(HttpHeaders.SET_COOKIE, jwtService.clearAuthCookie().toString());
                            boolean admin = authentication != null && authentication.getAuthorities().stream()
                                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
                            response.sendRedirect(admin ? "/login?logout=true" : "/?logout=true");
                        }))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(entryPoint())
                        .accessDeniedHandler(new JsonAccessDeniedHandler()))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, LogoutFilter.class)
                .addFilterBefore(new LoginRateLimitFilter(loginAttempts, loginProperties),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new LegacyAwarePasswordEncoder();
    }

    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher publisher) {
        return new DefaultAuthenticationEventPublisher(publisher);
    }

    @Bean
    public LoginAttemptService loginAttemptService(LoginProtectionProperties properties) {
        return new LoginAttemptService(properties);
    }

    @Bean
    public LoginAttemptListener loginAttemptListener(LoginAttemptService attempts,
            LoginProtectionProperties properties) {
        return new LoginAttemptListener(attempts, properties);
    }

    private AuthenticationEntryPoint entryPoint() {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> map = new LinkedHashMap<>();
        map.put(new AntPathRequestMatcher("/api/**"), new JsonAuthenticationEntryPoint());
        DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(map);
        entryPoint.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));
        return entryPoint;
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
