/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.orm;

import java.io.File;
import java.util.HashMap;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.orm.Connector;
import tech.bluemail.platform.registry.Packager;
import tech.bluemail.platform.utils.Mapper;

public class Database {
    public static synchronized void init() {
        try {
            String dataSourcePath = new File(System.getProperty("base.path")).getAbsolutePath() + "/applications/bluemail/configs/databases.ini";
            HashMap<String, String> map = Mapper.readProperties(dataSourcePath);
            if (map == null) return;
            if (map.isEmpty()) return;
            String[] databases = new String[]{"master", "lists"};
            String defaultKey = "master";
            Packager.getInstance().getRegistry().put("default-databases-key", defaultKey);
            String[] arrstring = databases;
            int n = arrstring.length;
            int n2 = 0;
            while (n2 < n) {
                String databaseKey = arrstring[n2];
                Connector connector = new Connector();
                connector.setKey(databaseKey);
                connector.setDriver(String.valueOf(Mapper.getMapValue(map, databaseKey + ".type", (Object)"pgsql")));
                if ("pg".equalsIgnoreCase(connector.getDriver())) {
                    connector.setDriver("pgsql");
                }
                connector.setHost(String.valueOf(Mapper.getMapValue(map, databaseKey + ".host", (Object)"")).trim());
                connector.setPort(Integer.parseInt(String.valueOf(Mapper.getMapValue(map, databaseKey + ".port", (Object)"0"))));
                connector.setUsername(String.valueOf(Mapper.getMapValue(map, databaseKey + ".user", (Object)"")).trim());
                connector.setPassword(String.valueOf(Mapper.getMapValue(map, databaseKey + ".password", (Object)"")).trim());
                connector.setName(String.valueOf(Mapper.getMapValue(map, databaseKey + ".dbname", (Object)"")).trim());
                connector.iniDataSource();
                Packager.getInstance().getRegistry().put(databaseKey, connector);
                ++n2;
            }
            return;
        }
        catch (Exception e) {
            Logger.error(e, Database.class);
        }
    }

    public static synchronized boolean exists(String key) {
        if (!Packager.getInstance().getRegistry().containsKey(key)) return false;
        if (Packager.getInstance().getRegistry().get(key) == null) return false;
        if (!(Packager.getInstance().getRegistry().get(key) instanceof Connector)) return false;
        return true;
    }

    public static synchronized Connector get(String key) {
        if (!Database.exists(key)) return null;
        Connector connector = (Connector)Packager.getInstance().getRegistry().get(key);
        return connector;
    }

    public static synchronized Connector getDefault() {
        if (!Database.exists((String)Packager.getInstance().getRegistry().get("default-databases-key"))) return null;
        Connector connector = (Connector)Packager.getInstance().getRegistry().get((String)Packager.getInstance().getRegistry().get("default-databases-key"));
        return connector;
    }

    public static synchronized void setDefault(String key) {
        if (!Database.exists((String)Packager.getInstance().getRegistry().get("default-databases-key"))) return;
        Packager.getInstance().getRegistry().put("default-databases-key", key);
    }
}

