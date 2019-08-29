/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author eugener
 */
public class MonitorService implements Runnable {

    private final Configuration cfg = Configuration.getInstance();
    private final Logger logger = cfg.getLogger();

    private static MonitorService instance;
    private int numOfSessions = 0;
    private List<MonitorSession> sessions;

    private MonitorService() {
        sessions = new ArrayList<MonitorSession>();
    }

    public synchronized static MonitorService getInstance() {
        if (instance == null) {
            instance = new MonitorService();
        }
        return instance;
    }

    @Override
    public void run() {
        try {
            ServerSocket sourceListener = new ServerSocket(cfg.getServicePort());
            while (true) {
                logger.debug("Initialized management service: " + sourceListener);
                Socket sock = sourceListener.accept();
                logger.debug("Attempt to establish connection on service port from: " + sock);
                if (numOfSessions >= cfg.getMaxSvcSessions()) {
                    PrintWriter pw = new PrintWriter(sock.getOutputStream());
                    pw.println("ATTENTION: you have reached the maximum number (" + cfg.getMaxSvcSessions()
                            + ") of allowed concurrent configuration sessions. Either use another session or increase the number of sessions in TCP Relay configuration file.");
                    int c = 1;
                    for (MonitorSession session : sessions) {
                        pw.println(c++ + ") " + session);
                    }
                    pw.flush();
                    pw.close();
                    sock.close();
                }
                numOfSessions++;
                MonitorSession s = new MonitorSession(sock, this);
                sessions.add(s);
                new Thread(s).start();
            }
        } catch (IOException ex) {
            logger.error("MonitorService.run(): " + ex.getMessage(), true);
            System.exit(1);
        }
    }

    public synchronized void purge(MonitorSession session) {
        int ind = sessions.indexOf(session);
        if (ind != -1) {
            sessions.remove(ind);
            numOfSessions--;
        } else {
            logger.error("Cannot purge management console session: " + session);
        }
    }

    public List<MonitorSession> getSessions() {
        return sessions;
    }

}
