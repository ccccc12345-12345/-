package com.example.fishing.common;

/**
 * 当前登录用户上下文
 *
 * 角色定义：0-普通用户 1-商家 2-平台管理员 3-员工
 */
public final class CurrentUser {

    public static final int ROLE_USER = 0;
    public static final int ROLE_MERCHANT = 1;
    public static final int ROLE_ADMIN = 2;
    public static final int ROLE_STAFF = 3;

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    private CurrentUser() {}

    public static void set(Context ctx) {
        CONTEXT.set(ctx);
    }

    public static Context getContext() {
        return CONTEXT.get();
    }

    public static Long get() {
        Context ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getUserId();
    }

    /**
     * 获取当前请求的有效鱼塘ID。
     * 普通管理员强制使用其绑定的鱼塘ID；超级管理员/普通用户使用请求传入的 pondId。
     */
    public static Long getEffectivePondId(Long requestPondId) {
        Context ctx = CONTEXT.get();
        if (ctx != null && ctx.isNormalAdmin()) {
            return ctx.getPondId();
        }
        return requestPondId;
    }

    public static void remove() {
        CONTEXT.remove();
    }

    public static class Context {
        private Long userId;
        private Integer role;
        private Integer adminType;
        private Long pondId;
        private Long staffId;
        private Long staffMerchantId;
        private String staffRole;

        public Context(Long userId, Integer role, Integer adminType, Long pondId) {
            this(userId, role, adminType, pondId, null, null, null);
        }

        public Context(Long userId, Integer role, Integer adminType, Long pondId,
                       Long staffId, Long staffMerchantId, String staffRole) {
            this.userId = userId;
            this.role = role;
            this.adminType = adminType;
            this.pondId = pondId;
            this.staffId = staffId;
            this.staffMerchantId = staffMerchantId;
            this.staffRole = staffRole;
        }

        public Long getUserId() {
            return userId;
        }

        public Integer getRole() {
            return role;
        }

        public Integer getAdminType() {
            return adminType;
        }

        public Long getPondId() {
            return pondId;
        }

        public Long getStaffId() {
            return staffId;
        }

        public Long getStaffMerchantId() {
            return staffMerchantId;
        }

        public String getStaffRole() {
            return staffRole;
        }

        public boolean isUser() {
            return role != null && role == ROLE_USER;
        }

        public boolean isMerchant() {
            return role != null && role == ROLE_MERCHANT;
        }

        public boolean isAdmin() {
            return role != null && role == ROLE_ADMIN;
        }

        public boolean isStaff() {
            return role != null && role == ROLE_STAFF;
        }

        public boolean isSuperAdmin() {
            return isAdmin() && (adminType == null || adminType == 0);
        }

        public boolean isNormalAdmin() {
            return isAdmin() && adminType != null && adminType == 1;
        }

        /**
         * 商家账号的 merchantId 即其用户ID；员工账号返回关联的商家ID
         */
        public Long getMerchantId() {
            if (isMerchant()) {
                return userId;
            }
            if (isStaff()) {
                return staffMerchantId;
            }
            return null;
        }
    }
}
