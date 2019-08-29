/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import relaysvc.ProxyConfig.Protocols;

/**
 *
 * @author eugener
 */
abstract public class Proxy implements Runnable {

    protected final Configuration cfg = Configuration.getInstance();
    protected final Logger logger = Logger.getInstance();
    protected final String listenerHostname;
    protected final int listenerPort;
    protected final List<RelayDestination> dstAddresses;
    protected final ProxyConfig config;
    protected final List<ProxySession> sessions;
    protected int currentBalancerIndex;
    protected volatile long totalRuleIn;
    protected volatile long totalRuleOut;
    protected volatile long ruleSessions;
    private final Object totalRuleInLock = new Object();
    private final Object totalRuleOutLock = new Object();
    private final Object ruleSessionsLock = new Object();
    protected final Object sessionsLock = new Object();
    private volatile boolean printHexDump;
    private volatile boolean printTrace;
    private final static Object printHexDumpLock = new Object();
    private final static Object printTraceLock = new Object();

    abstract public void handler() throws IOException;

    abstract public void close();

    abstract public void purgeSession(Reader session);

    public Proxy(ProxyConfig config) throws Exception {
        this.sessions = new ArrayList<ProxySession>();
        this.config = config;
        String[] ss = config.getSrc().split(":");
        if (ss.length != 2) {
            throw new IllegalArgumentException("Proxy source has to use 'hostname:port' format");
        }
        listenerHostname = ss[0];
        listenerPort = Integer.valueOf(ss[1]);
        dstAddresses = new ArrayList<RelayDestination>();
        for (String dst : config.getDsts()) {
            dstAddresses.add(new RelayDestination(dst, this.config.getPolicy()));
        }

    }

    @Override
    public String toString() {
        return config.toString();
    }

    public static Proxy getProxyInstance(ProxyConfig rc) throws Exception {
        Protocols p = rc.getProtocol();
        if (null == p) {
            throw new NullPointerException("ProxyConfig cannot be null in getProxyInstance()");
        }
        switch (p) {
            case TCP:
                return new TcpProxy(rc);
            case UDP:
                return new UdpProxy(rc);
            default:
                //return new OraProxy(rc);
                throw new IllegalArgumentException("Unsupported ProxyConfig: " + rc);
        }
    }

    protected int getCurrentBalancerIndex() {
        return currentBalancerIndex;
    }

    protected int setNextBalancerIndex() {
        int size = dstAddresses.size();
        if (size == 1) {
            return 0;
        }
        currentBalancerIndex++;
        if (currentBalancerIndex >= size) {
            currentBalancerIndex = 0;
        }
        return currentBalancerIndex;
    }

    @Override
    public void run() {
        try {
            handler();
        } catch (SocketException ex) {
            if (!ex.getMessage().toLowerCase().contains("closed")) {
                logger.error("Proxy().run(): SocketException: " + ex.getMessage() + " for " + config);
            }
        } catch (IOException ex) {
            logger.error("Proxy.run(): " + ex.getMessage() + " for " + config);
        }
    }

    public ProxyConfig getConfig() {
        return this.config;
    }

    List<ProxySession> getSessions() {
        synchronized (sessionsLock) {
            return this.sessions;
        }
    }

    void addSession(ProxySession session) {
        synchronized (sessionsLock) {
            sessions.add(session);
        }
    }

    public long getTotaRulelIn() {
        synchronized (totalRuleInLock) {
            return totalRuleIn;
        }
    }

    public void incTotalRuleIn(int n) {
        synchronized (totalRuleInLock) {
            this.totalRuleIn = this.totalRuleIn + n;
            cfg.incTotalIn(n);
        }
    }

    public long getTotalRuleOut() {
        synchronized (totalRuleOutLock) {
            return totalRuleOut;
        }
    }

    public void incTotalRuleOut(int n) {
        synchronized (totalRuleOutLock) {
            this.totalRuleOut = this.totalRuleOut + n;
            cfg.incTotalOut(n);
        }
    }

    public void incRuleSessions() {
        synchronized (ruleSessionsLock) {
            this.ruleSessions++;
            cfg.incTotalSessions();
        }
    }

    public long getRuleSessions() {
        synchronized (ruleSessionsLock) {
            return this.ruleSessions;
        }
    }

    public List<RelayDestination> getDstAddresses() {
        return dstAddresses;
    }

    public boolean isPrintHexDump() {
        synchronized (printHexDumpLock) {
            return printHexDump;
        }
    }

    public void setPrintHexDump(boolean printHexDump) {
        synchronized (printHexDumpLock) {
            this.printHexDump = printHexDump;
        }
    }

    public boolean isPrintTrace() {
        synchronized (printTraceLock) {
            return printTrace;
        }
    }

    public void setPrintTrace(boolean printTrace) {
        synchronized (printTraceLock) {
            this.printTrace = printTrace;
        }
    }

}
