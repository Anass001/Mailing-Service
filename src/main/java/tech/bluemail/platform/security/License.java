/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.security;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import tech.bluemail.platform.logging.Logger;

public class License {
    public static void check() throws Exception {
    }

    public static InetAddress getCurrentIp() {
        try {
            InetAddress ia;
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            block2 : do {
                if (!networkInterfaces.hasMoreElements()) return null;
                NetworkInterface ni = networkInterfaces.nextElement();
                Enumeration<InetAddress> nias = ni.getInetAddresses();
                do {
                    if (!nias.hasMoreElements()) continue block2;
                } while ((ia = nias.nextElement()).isLinkLocalAddress() || ia.isLoopbackAddress() || !(ia instanceof Inet4Address));
                break;
            } while (true);
            return ia;
        }
        catch (SocketException e) {
            Logger.error(e, License.class);
        }
        return null;
    }
}

