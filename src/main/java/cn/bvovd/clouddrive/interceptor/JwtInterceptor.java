package cn.bvovd.clouddrive.interceptor;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.exception.BusinessException;
import cn.bvovd.clouddrive.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头获取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            throw new BusinessException("未登录或登录已过期");
        }

        String token = authHeader.substring(7);

        // 2. 校验 Token 是否有效
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            throw new BusinessException("Token 无效或已过期，请重新登录");
        }

        // 3. 解析 userId
        Long userId = jwtUtil.getUserIdFromToken(token);
        Integer role = jwtUtil.getRoleFromToken(token);
        // 4. ★ 存入 ThreadLocal，供后续业务使用
        UserContext.setUserId(userId);
        UserContext.setRole(role);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // ★ 关键：请求结束后必须清除，防止内存泄漏（因为 Tomcat 使用线程池复用线程）
        UserContext.remove();
    }
}