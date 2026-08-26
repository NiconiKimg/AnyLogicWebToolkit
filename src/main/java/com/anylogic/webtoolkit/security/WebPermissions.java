package com.anylogic.webtoolkit.security;

import java.util.EnumSet;
import java.util.Set;

/**
 * Manages the set of {@link Permission}s granted to a
 * {@link com.anylogic.webtoolkit.ui.WebDialog} instance.
 *
 * <p>By default the following permissions are granted:
 * {@link Permission#MODEL_READ}, {@link Permission#PROJECT_FILES},
 * {@link Permission#INTERNET}, and {@link Permission#CLIPBOARD}.
 */
public class WebPermissions {

    private final Set<Permission> allowed = EnumSet.of(
        Permission.MODEL_READ,
        Permission.PROJECT_FILES,
        Permission.INTERNET,
        Permission.CLIPBOARD
    );

    /**
     * Grants the given permission.
     *
     * @param p the permission to allow
     */
    public void allow(Permission p)  { allowed.add(p); }

    /**
     * Revokes the given permission.
     *
     * @param p the permission to deny
     */
    public void deny(Permission p)   { allowed.remove(p); }

    /**
     * @param p the permission to check
     * @return {@code true} if the permission is currently granted
     */
    public boolean isAllowed(Permission p) { return allowed.contains(p); }

    /**
     * Asserts that the given permission is granted.
     *
     * @param p the required permission
     * @throws SecurityException if the permission is not granted
     */
    public void requireOrThrow(Permission p) {
        if (!isAllowed(p))
            throw new SecurityException("Permission denied: " + p);
    }
}
