package com.qbank.config;

import com.qbank.interceptor.LoginInterceptor;
import com.qbank.mapper.UserMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置(服务端渲染版): 登录拦截器 + 静态资源放行
 * 认证基于 Session(会话), 由 LoginInterceptor 统一处理
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UserMapper userMapper;

    public WebConfig(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Bean
    public LoginInterceptor loginInterceptor() {
        return new LoginInterceptor(userMapper);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login", "/register",
                        "/css/**", "/js/**", "/vendor/**", "/images/**",
                        "/favicon.ico", "/error"
                );
    }
}
