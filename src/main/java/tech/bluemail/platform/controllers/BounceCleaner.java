/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.controllers;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import tech.bluemail.platform.controllers.StatsCalculator;
import tech.bluemail.platform.controllers.SuppressionManager;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.exceptions.ThreadException;
import tech.bluemail.platform.interfaces.Controller;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.models.admin.Server;
import tech.bluemail.platform.orm.Database;
import tech.bluemail.platform.parsers.TypesParser;
import tech.bluemail.platform.security.License;
import tech.bluemail.platform.workers.BounceWorker;

public class BounceCleaner
implements Controller {
    public static volatile int COUNT = 0;
    public static volatile int INDEX = 1;

    public BounceCleaner() throws Exception {
        License.check();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void start(String[] parameters) throws Exception {
        int proccessId = TypesParser.safeParseInt(parameters[1]);
        boolean errorOccured = false;
        try {
            String listName = parameters[2];
            int ispId = TypesParser.safeParseInt(parameters[3]);
            int userId = TypesParser.safeParseInt(parameters[4]);
            List<Server> servers = new ArrayList();
            if (proccessId == 0) {
                throw new Exception("No Proccess Id Found !");
            }
            if (listName == null) throw new Exception("No List Name Found !");
            if ("".equals(listName)) {
                throw new Exception("No List Name Found !");
            }
            if (parameters.length > 5) {
                int serverId = TypesParser.safeParseInt(parameters[5]);
                StatsCalculator controllerCalcul = new StatsCalculator();
                String[] args = new String[]{"send_stats", parameters[5]};
                controllerCalcul.start(args);
                Server serverObj = (Server)Server.first(Server.class, "id = ? AND status_id = ?", new Object[]{serverId, 1});
                servers.add(serverObj);
            } else {
                StatsCalculator controllerCalcul = new StatsCalculator();
                String[] args = new String[]{"send_stats"};
                if (controllerCalcul != null) {
                    controllerCalcul.start(parameters);
                }
                servers = Server.all(Server.class, "status_id = ?", new Object[]{1});
            }
            if (servers == null) throw new Exception("No Servers Found To Clean Bounce From !");
            if (servers.isEmpty()) throw new Exception("No Servers Found To Clean Bounce From !");
            ExecutorService serversExecutor = Executors.newFixedThreadPool(servers.size() > 15 ? 15 : servers.size());
            BounceWorker worker = null;
            for (Server server : servers) {
                if (server == null) continue;
                worker = new BounceWorker(proccessId, listName, server, userId, ispId);
                worker.setUncaughtExceptionHandler(new ThreadException());
                serversExecutor.submit(worker);
            }
            serversExecutor.shutdown();
            serversExecutor.awaitTermination(10L, TimeUnit.DAYS);
            return;
        }
        catch (Exception e) {
            this.interruptProccess(proccessId);
            Logger.error(e, BounceCleaner.class);
            errorOccured = true;
            return;
        }
        finally {
            if (!errorOccured) {
                this.finishProccess(proccessId);
            }
        }
    }

    public static synchronized void updateProccess(int proccessId, String type) throws DatabaseException {
        int progress = (int)((double)BounceCleaner.getIndex() / (double)BounceCleaner.getCount() * 100.0);
        String update = "bounce".equalsIgnoreCase(type) ? " , hard_bounce = hard_bounce + 1 " : " , clean = clean + 1 ";
        Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET progress = '" + progress + "%' " + update + " WHERE Id = ?", new Object[]{proccessId}, 0);
        BounceCleaner.updateIndex();
    }

    public static synchronized int getIndex() {
        return INDEX;
    }

    public static synchronized void updateIndex() {
        ++INDEX;
    }

    public static synchronized void updateCount(int size) {
        COUNT += size;
    }

    public static synchronized int getCount() {
        return COUNT;
    }

    public void interruptProccess(int proccessId) {
        try {
            Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET status = 'error' , finish_time = ?  WHERE id = ?", new Object[]{new Timestamp(System.currentTimeMillis()), proccessId}, 0);
            return;
        }
        catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }

    public void finishProccess(int proccessId) {
        try {
            Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET status = 'completed' , progress = '100%' , finish_time = ?  WHERE id = ?", new Object[]{new Timestamp(System.currentTimeMillis()), proccessId}, 0);
            return;
        }
        catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }
}

