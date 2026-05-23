package com.outline.server.security;

import com.outline.server.user.User;

public final class CurrentUser {
    private static final ThreadLocal<User> HOLDER = new ThreadLocal<>();

    private CurrentUser() {}

    public static void set(User user) {
        HOLDER.set(user);
    }

    public static User get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
