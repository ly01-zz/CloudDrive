package cn.bvovd.clouddrive.exception;

/**
 * 未认证/未授权异常（由 JwtInterceptor 抛出，全局处理器统一返回 HTTP 401）
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
