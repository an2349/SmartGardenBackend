package com.andeptrai.iot.jwtS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher; // Loại bỏ import này
import org.springframework.security.web.util.matcher.AntPathRequestMatcher; // Thêm import này
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableWebSecurity
public class JwtConfig {

    private final JwtFilter jwtFilter;

    public JwtConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        // Loại bỏ dòng này: MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector).servletPath("/");
        http
            .csrf(csrf -> csrf.disable()) // Tắt CSRF cho API stateless (dùng JWT)
            .authorizeHttpRequests(auth -> auth
                // Sử dụng AntPathRequestMatcher để match các đường dẫn permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/ws/device/**")).permitAll()
                .anyRequest().authenticated() // Tất cả các request khác yêu cầu xác thực
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Đảm bảo phiên là stateless cho JWT
            .headers(headers -> headers.frameOptions(frame -> frame.disable())) // Cho phép hiển thị H2 Console trong iframe
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); // Thêm bộ lọc JWT trước bộ lọc xác thực Username/Password
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}