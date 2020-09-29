/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.parsers;

public class TypesParser {
    public static int safeParseInt(Object numericValue) {
        try {
            return Integer.parseInt(String.valueOf(numericValue));
        }
        catch (NumberFormatException numberFormatException) {
            return 0;
        }
    }
}

