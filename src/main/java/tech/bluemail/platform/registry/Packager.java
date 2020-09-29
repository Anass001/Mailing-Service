/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.registry;

import java.util.HashMap;

public class Packager {
    private HashMap<String, Object> registry = new HashMap();
    private static Packager instance;

    public static Packager getInstance() {
        if (instance != null) return instance;
        instance = new Packager();
        return instance;
    }

    private Packager() {
    }

    public HashMap<String, Object> getRegistry() {
        return this.registry;
    }

    public void setRegistry(HashMap<String, Object> registry) {
        this.registry = registry;
    }
}

