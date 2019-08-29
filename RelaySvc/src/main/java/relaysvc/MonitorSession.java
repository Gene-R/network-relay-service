/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

/**
 *
 * @author eugener
 */
public class MonitorSession implements Runnable {

    private final Configuration cfg = Configuration.getInstance();
    private final Logger logger = cfg.getLogger();
    private final Socket sock;
    private final MonitorService service;
    private final Date created;

    public MonitorSession(Socket sock, MonitorService service) {
        this.sock = sock;
        this.service = service;
        this.created = new Date();
    }

    @Override
    public void run() {
        InputStreamReader in = null;
        PrintWriter out = null;
        try {
            logger.debug("Opened management console for " + sock);
            in = new InputStreamReader(sock.getInputStream());
            out = new PrintWriter(sock.getOutputStream());
            out.println("*** TCP Relay Management Console " + Utils.getCurTS() + "***");
            out.flush();
            while (true) {
                String cmd = "";
                int n;
                while ((n = in.read()) != '\n') {
                    cmd += (char) n;
                }
                cmd = cmd.trim();
                if (!cmd.isEmpty()) {

                    try {
                        logger.debug("Processing command [" + sock + "] for " + sock);
                        if (cmd.toLowerCase().equals("help")) {
                            out.println("show set ................................... to display configuration");
                            out.println("who ........................................ to list all active management terminal");
                            out.println("stat ....................................... to list all configured rules and statistics");
                            out.println("kill terminal <N> .......................... to kill management terminal by number");
                            out.println("enable|disable session trace <session ID> .. to enable/disable brief hex trace for session");
                            out.println("enable|disable session hex <session ID> .... to enable/disable full hex dump for session");
                            out.println("enable|disable rule trace <rule ID> ........ to enable/disable brief hex trace for rule");
                            out.println("enable|disable rule hex <rule ID> .......... to enable/disable full hex dump for rule");
                            out.println("enable|disable debug ....................... to enable/disable debug for service and trace all sessions");
                            out.println("set policy <rule #> <policy>................ to set new policy for specified proxy rule");
                            out.println("reset ...................................... to read and reset all rules from the scratch");
                            out.println("quit ....................................... to end this sessions");
                            out.println("help ....................................... this help");
                        } else if (cmd.toLowerCase().equals("who")) {
                            int c = 1;
                            for (MonitorSession session : service.getSessions()) {
                                out.println("Terminal #" + c++ + " " + session);
                            }
                        } else if (cmd.toLowerCase().equals("stat")) {
                            int c = 1;
                            out.println("**************************************");
                            out.println("***   TCP Relay rules statistics   ***");
                            out.println("**************************************");
                            out.println("Total rules:        " + Utils.getHumanReadableLong(cfg.getProxyConfigsManager().getProxies().size()));
                            Date ts = cfg.getProxyConfigsManager().getCreated();
                            out.println("Rules loaded on:    " + (ts != null ? Utils.getTS(ts) : "ERROR"));
                            out.println("Total sessions:     " + Utils.getHumanReadableLong(cfg.getTotalSessions()));
                            out.println("Total bytes in/out: " + Utils.getHumanReadableLong(cfg.getTotaIn()) + " / " + Utils.getHumanReadableLong(cfg.getTotalOut()));
                            out.println();
                            out.println("-= Statistics per rule =-");
                            for (Proxy proxy : cfg.getProxyConfigsManager().getProxies()) {
                                out.println();
                                out.println("Rule " + c++);
                                out.println("Config: " + proxy.getConfig());
                                out.println("Run-time: " + "sessions per rule: " + Utils.getHumanReadableLong(proxy.getRuleSessions()) + ", "
                                        + "rule bytes in/out: " + Utils.getHumanReadableLong(proxy.getTotaRulelIn())
                                        + " / " + Utils.getHumanReadableLong(proxy.getTotalRuleOut()));
                                out.println("HEX_DUMP=" + proxy.isPrintHexDump() + ", TRACE=" + proxy.isPrintTrace());
                                int count = 1;
                                for (ProxySession session : proxy.getSessions()) {
                                    if (session.getConfig().getConfigUuid().equals(proxy.getConfig().getConfigUuid())) {
                                        out.println("  ==> " + count++ + ") "
                                                + session.getSessionId() + ", " + Utils.getTS(session.getCreated()));
                                        out.println("        " + session);
                                        //String ss = "HEX_DUMP=" + session.isPrintHexDump() + ", TRACE=" + proxy.isPrintTrace();
                                    }
                                }
                            }
                            logger.printErrorStack(out);
                        } else if (cmd.toLowerCase().startsWith("kill terminal ")) {
                            String[] kk = cmd.split(" ");
                            if (kk.length == 3) {
                                int index = Integer.valueOf(kk[2].trim()) - 1;
                                if (index < service.getSessions().size() && index >= 0) {
                                    MonitorSession session = service.getSessions().get(index);
                                    if (session.equals(this)) {
                                        out.println("ATTENTION: you cannot kill your current terminal you are working with right now. Use 'quit' command instead");
                                    } else {
                                        session.sock.close();
                                        service.purge(session);
                                        out.println("The management terminall is killed");
                                    }
                                } else {
                                    out.println("ATTENTION: invalid terminal number");
                                }
                            } else {
                                out.println("ERROR: wrong number of arguments");
                            }

                        } else if (cmd.toLowerCase().startsWith("enable debug")
                                || cmd.toLowerCase().startsWith("disable debug")) {
                            boolean enable = cmd.toLowerCase().startsWith("enable");
                            cfg.setDebug(enable);
                            out.println("Debug was " + (enable ? "enabled" : "disabled"));
                        } else if (cmd.toLowerCase().startsWith("enable session hex ")
                                || cmd.toLowerCase().startsWith("disable session hex ")
                                || cmd.toLowerCase().startsWith("enable session trace ")
                                || cmd.toLowerCase().startsWith("disable session trace ")
                                || cmd.toLowerCase().startsWith("enable rule hex ")
                                || cmd.toLowerCase().startsWith("disable rule hex ")
                                || cmd.toLowerCase().startsWith("enable rule trace ")
                                || cmd.toLowerCase().startsWith("disable rule trace ")) {
                            boolean isHex = cmd.toLowerCase().contains(" hex ");
                            boolean isRule = cmd.toLowerCase().contains(" rule ");
                            String[] kk = cmd.split(" ");
                            boolean found = false;
                            boolean enable = cmd.toLowerCase().startsWith("enable ");
                            if (kk.length == 4) {
                                String id = kk[3].trim();
                                int count = 0;
                                int indNumber = Integer.valueOf(id);
                                if (indNumber < 1 || indNumber > cfg.getProxyConfigsManager().getProxies().size()) {
                                    throw new IllegalArgumentException("rule id is out of range: " + indNumber);
                                }
                                for (Proxy proxy : cfg.getProxyConfigsManager().getProxies()) {
                                    count++;
                                    if (!isRule) {
                                        for (ProxySession session : proxy.getSessions()) {
                                            if (session.getSessionId().equals(id)) {
                                                if (isHex) {
                                                    session.getSrcReader().setPrintHexDump(enable);
                                                } else {
                                                    session.getSrcReader().setPrintTrace(enable);
                                                }
                                                if (session.getDstReader() != null) {
                                                    if (isHex) {
                                                        session.getDstReader().setPrintHexDump(enable);
                                                    } else {
                                                        session.getDstReader().setPrintTrace(enable);
                                                    }
                                                }
                                                out.println((isHex ? "hex dump" : "trace") + " for specified session is " + (enable ? "enabled" : "disabled"));
                                                found = true;
                                            }
                                        }
                                    } else {
                                        if (count == indNumber) {
                                            if (isHex) {
                                                proxy.setPrintHexDump(enable);
                                            } else {
                                                proxy.setPrintTrace(enable);
                                            }
                                            out.println((isHex ? "hex dump" : "trace") + " for specified rule is " + (enable ? "enabled" : "disabled"));
                                            found = true;
                                            break;
                                        }
                                    }
                                }
                                if (!found) {
                                    out.println("ERROR: could not find specified session ID: " + id);
                                }
                            } else {
                                out.println("ERROR: wrong number of arguments");
                            }
                        } else if (cmd.toLowerCase().startsWith("set policy ")) {
                            String[] kk = cmd.split(" ");
                            if (kk.length == 4) {
                                int ruleIndex = Integer.valueOf(kk[1].trim()) - 1;
                                if (ruleIndex < cfg.getProxyConfigsManager().getProxies().size() && ruleIndex >= 0) {

                                } else {
                                    out.println("ATTENTION: invalid proxy entry number");
                                }
                            } else {
                                out.println("ERROR: wrong number of arguments");
                            }
                        } else if (cmd.toLowerCase().equals("reset")) {
                            logger.resetErrorStack();
                            cfg.getProxyConfigsManager().reset();
                            logger.printErrorStack(out);
                            out.println("The configuration was reset.");
                            if (!cfg.isResetGracefulMode()) {
                                out.println("Active proxy sessions were terminated.");
                            } else {
                                out.println("Active proxy sessions will be closed in graceful mode.");
                            }
                            logger.debug("The configuration was reset from the management console:  " + sock);
                        } else if (cmd.toLowerCase().equals("show set")) {
                            //cfg.getProxyConfigsManager().reset();
                            out.println("*** TCP Relay settings ***");
                            for (String rulePath : cfg.getConfigRulesPaths()) {
                                out.println("Rules config file ............  " + rulePath);
                            }
                            out.println("log file  ....................  " + cfg.getLogFile());
                            out.println("lock file ....................  " + cfg.getLockFile());
                            out.println("Config file  .................  " + cfg.getConfigFile());
                            out.println("Config file loaded on ........  " + new Date(cfg.getCfgLoadedOn()));
                            out.println("Config file modified on ......  " + new Date(cfg.getCfgModifiedOn()));
                            out.println(Configuration.PROP_SERVICE_PORT + " .................  " + cfg.getServicePort());
                            out.println(Configuration.PROP_RESET_GRACEFUL_MODE + " ..........  " + (cfg.isResetGracefulMode() ? "1" : "0"));
                            out.println(Configuration.PROP_TCP_READ_TIMEOUT + " .............  " + cfg.getTcpReadTimeout());
                            out.println(Configuration.PROP_IO_BUFFER_SIZE + " ...............  " + cfg.getIoBufferSize());
                            out.println(Configuration.PROP_MAX_SVC_TERMINALS + " ............  " + cfg.getMaxSvcSessions());
                            out.println(Configuration.PROP_MAX_INCOMING_QUEUE + " ...........  " + cfg.getMaxIncomingQueue());
                            out.println(Configuration.PROP_DEBUG + " ........................  " + (cfg.isDebug() ? "1" : "0"));

                        } else if (cmd.toLowerCase().equals("quit")) {
                            logger.debug("Closed management console for " + sock);
                            break;
                        } else {
                            out.println("ERROR: Wrong command or syntax error: " + cmd);
                        }
                    } catch (Exception ex) {
                        logger.error(ex.getMessage() + " for " + sock);
                        out.println("ERROR: " + ex.getMessage());
                    }
                }
                out.println("OK");
                out.flush();
            }

        } catch (IOException ex) {
            logger.debug("monitor session: " + ex.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (!sock.isClosed()) {
                    sock.close();
                }

            } catch (IOException iex) {
                logger.error(iex.getMessage() + " for " + sock);
            }
            service.purge(this);
        }

    }

    @Override
    public String toString() {
        return Utils.getTS(created) + " " + sock.toString();
    }
}
