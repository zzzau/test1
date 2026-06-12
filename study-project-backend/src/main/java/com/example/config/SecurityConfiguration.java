package com.example.config;

import com.alibaba.fastjson.JSON;
import com.example.entity.RestBean;
import com.example.service.AuthorizeService;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Resource
    AuthorizeService authorizeService;

    @Resource
    DataSource dataSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // 开启 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 前后端分离项目，开发阶段关闭 CSRF
                .csrf(csrf -> csrf.disable())

                // 配置接口访问权限
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )

                // 配置登录
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler(this::onAuthenticationSuccess)
                        .failureHandler(this::onAuthenticationFailure)
                        .permitAll()
                )

                // 记住我
                .rememberMe(remember -> remember
                        .rememberMeParameter("remember")
                        .tokenRepository(tokenRepository())
                        .tokenValiditySeconds(3600 * 24 * 7)
                        .userDetailsService(authorizeService)
                )

                // 配置未登录访问接口时的处理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(this::onAuthenticationEntryPoint)
                )

                // 配置退出登录
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(this::onLogoutSuccess)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )

                .build();
    }

    /**
     * Remember Me 持久化 Token
     */
    @Bean
    public PersistentTokenRepository tokenRepository() {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);

        // 第一次运行用 true，让 Spring Security 自动建表
        // 建表成功后改成 false
        repository.setCreateTableOnStartup(false);

        return repository;
    }

    /**
     * CORS 跨域配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 开发阶段允许所有前端地址
        configuration.setAllowedOriginPatterns(List.of("*"));

        // 允许携带 Cookie
        configuration.setAllowCredentials(true);

        // 允许请求方式
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));

        // 允许所有请求头
        configuration.setAllowedHeaders(List.of("*"));

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }


    /**
     * 配置密码加密方式
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity security) throws Exception {
        AuthenticationManagerBuilder builder =
                security.getSharedObject(AuthenticationManagerBuilder.class);

        builder
                .userDetailsService(authorizeService)
                .passwordEncoder(passwordEncoder());

        return builder.build();
    }

    /**
     * 登录成功
     */
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json;charset=utf-8");

        response.getWriter().write(JSON.toJSONString(RestBean.success("登录成功")));
    }

    /**
     * 退出登录成功
     */
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json;charset=utf-8");

        response.getWriter().write(JSON.toJSONString(RestBean.success("退出登录成功")));
    }

    /**
     * 登录失败
     */
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        System.out.println("登录失败，用户名 = " + request.getParameter("username"));
        System.out.println("失败原因 = 用户名或密码错误");

        response.setStatus(401);
        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json;charset=utf-8");

        response.getWriter().write(JSON.toJSONString(RestBean.failure(401, "用户名或密码错误")));
    }

    /**
     * 未登录访问接口
     */
    public void onAuthenticationEntryPoint(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        response.setStatus(401);
        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json;charset=utf-8");

        response.getWriter().write(JSON.toJSONString(RestBean.failure(401, "请先登录")));
    }
}
