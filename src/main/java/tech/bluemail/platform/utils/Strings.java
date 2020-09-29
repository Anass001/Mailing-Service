/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.utils;

import java.util.Random;

public class Strings {
    public static String getSaltString(int length, boolean letters, boolean uppercase, boolean numbers, boolean specialCharacters) {
        String SALTCHARS = "";
        if (letters) {
            SALTCHARS = SALTCHARS + "abcdefghijklmnopqrstuvwxyz";
            if (uppercase) {
                SALTCHARS = SALTCHARS + "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            }
        }
        if (numbers) {
            SALTCHARS = SALTCHARS + "1234567890";
        }
        if (specialCharacters) {
            SALTCHARS = SALTCHARS + "@\\\\/_*$&-#[](){}";
        }
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < length) {
            int index = (int)(rnd.nextFloat() * (float)SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();
    }

    public static String randomizeCase(String str) {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(str.length());
        char[] arrc = str.toCharArray();
        int n = arrc.length;
        int n2 = 0;
        while (n2 < n) {
            char c = arrc[n2];
            sb.append(rnd.nextBoolean() ? Character.toLowerCase(c) : Character.toUpperCase(c));
            ++n2;
        }
        return sb.toString();
    }
}

