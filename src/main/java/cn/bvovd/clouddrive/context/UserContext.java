package cn.bvovd.clouddrive.context;

/**
 * 用户上下文持有者（基于 ThreadLocal）
 * 用于在当前请求线程中存储当前登录用户的 ID
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE_HOLDER = new ThreadLocal<>();
    /**
     * 设置当前用户ID（由拦截器调用）
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前用户ID（由业务层调用）
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void setRole(Integer role) { ROLE_HOLDER.set(role); }
    public static Integer getRole() { return ROLE_HOLDER.get(); }
    /**
     * 清除当前用户ID（拦截器 afterCompletion 中调用，防止内存泄漏）
     */
    public static void remove() {
        USER_ID_HOLDER.remove();
        ROLE_HOLDER.remove();
    }
}