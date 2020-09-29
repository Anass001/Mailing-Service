/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.utils;

public class Domains {
    public static String getDomainName(String url) {
        try {
            String[] parts = url.split("\\.");
            if (parts.length <= 2) return url;
            String domain = "";
            if (parts.length <= 3) return domain + parts[parts.length - 2] + "." + parts[parts.length - 1];
            domain = parts[parts.length - 3] + ".";
            return domain + parts[parts.length - 2] + "." + parts[parts.length - 1];
        }
        catch (Exception e) {
            return url;
        }
    }
}

