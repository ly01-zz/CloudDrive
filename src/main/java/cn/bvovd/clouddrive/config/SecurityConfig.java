package cn.bvovd.clouddrive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // 禁用 CSRF（前后端分离项目必须）
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()   // 所有请求暂时放行（开发阶段）
                )
                .formLogin(form -> form.disable())  // 禁用默认登录页
                .httpBasic(basic -> basic.disable()); // 禁用 HTTP Basic 认证（消除你看到的密码提示）
        return http.build();
    }
}