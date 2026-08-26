package com.anylogic.webtoolkit.security;

import java.util.EnumSet;
import java.util.Set;

/** Gestiona permisos de una WebApp. */
public class WebPermissions {

    private final Set<Permission> allowed = EnumSet.of(
        Permission.MODEL_READ,
        Permission.PROJECT_FILES,
        Permission.INTERNET,
        Permission.CLIPBOARD
    );

    public void allow(Permission p)  { allowed.add(p); }
    public void deny(Permission p)   { allowed.remove(p); }
    public boolean isAllowed(Permission p) { return allowed.contains(p); }

    public void requireOrThrow(Permission p) {
        if (!isAllowed(p))
            throw new SecurityException("Permission denied: " + p);
    }
}
