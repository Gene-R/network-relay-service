/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;

/**
 *
 * @author eugener
 */
public class Configuration {

    public final static String PROP_LOGFILE = "logfile";
    public final static String PROP_LOCK_FILE = "lock_file";
    public final static String PROP_IO_BUFFER_SIZE = "io_buffer_size";
    public final static String PROP_TCP_READ_TIMEOUT = "tcp_read_timeout";
    public final static String PROP_MAX_INCOMING_QUEUE = "max_incoming_queue";
    public final static String PROP_SERVICE_PORT = "service_port";
    public final static String PROP_MAX_SVC_TERMINALS = "max_svc_terminals";
    public final static String PROP_RESET_GRACEFUL_MODE = "reset_graceful_mode";
    public final static String PROP_HEX_DUMP_DIR = "hex_dump_dir";
    public final static String PROP_DEBUG = "debug";

    private static Configuration instance;
    private String configFile;
    private Properties props;
    private String[] configRulesPaths;
    private Logger logger;

    private long cfgLoadedOn;
    private long cfgModifiedOn;
    private volatile long totalIn;
    private volatile long totalOut;
    private volatile long totalSessions;
    private ProxyConfigsManager proxyConfigsManager;
    private final Object totalInLock = new Object();
    private final Object totalOutLock = new Object();
    private final Object totalSessionsLock = new Object();
    private final Object debugLock = new Object();

    // props from properties file initialized with defaults
    private String logFile = "tcprelay.log";
    private String lockFile = "tcprelay.lock";
    private String hexDumpDir = "HexDumps";
    private int tcpReadTimeout = 10;
    private int ioBufferSize = 16384;
    private int maxIncomingQueue = 1000;
    private int servicePort = 0;
    private int maxSvcSessions = 2;
    private boolean resetGracefulMode = false;
    private boolean debug = false;

    private Configuration() {
        //reset();
        Timer t = new Timer();
        t.schedule(new cfgFileMonitor(this), 10000, 10000);
    }

    public static synchronized Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    public synchronized void loadConfigFile() throws IOException {
        if (configFile != null && !configFile.isEmpty()) {
            loadConfigFile(configFile);
        }
    }

    public synchronized void loadConfigFile(String configFile) throws IOException {
        this.configFile = configFile;
        this.props = new Properties();
        InputStream is = new FileInputStream(configFile);
        props.load(is);
        File f = new File(configFile);
        cfgModifiedOn = f.lastModified();

        String s = props.getProperty(PROP_LOGFILE);
        if (s != null) {
            this.logFile = s;
        }

        s = props.getProperty(PROP_LOCK_FILE);
        if (s != null) {
            this.lockFile = s;
        }

        s = props.getProperty(PROP_HEX_DUMP_DIR);
        if (s != null) {
            this.hexDumpDir = s;
        }

        s = props.getProperty(PROP_TCP_READ_TIMEOUT);
        if (s != null) {
            int n = Integer.valueOf(s);
            if (n < 0) {
                n = 0;
            }
            this.tcpReadTimeout = n;
        }

        s = props.getProperty(PROP_IO_BUFFER_SIZE);
        if (s != null) {
            int n = Integer.valueOf(s);
            if (n < 1024) {
                n = 1024;
            }
            this.ioBufferSize = n;
        }

        s = props.getProperty(PROP_MAX_INCOMING_QUEUE);
        if (s != null) {
            int n = Integer.valueOf(s);
            if (n < 1000) {
                n = 1000;
            }
            this.maxIncomingQueue = n;
        }

        s = props.getProperty(PROP_SERVICE_PORT);
        if (s != null) {
            this.servicePort = Integer.valueOf(s);
        }

        s = props.getProperty(PROP_MAX_SVC_TERMINALS);
        if (s != null) {
            this.maxSvcSessions = Integer.valueOf(s);
        }

        s = props.getProperty(PROP_RESET_GRACEFUL_MODE);
        if (s != null) {
            this.resetGracefulMode = s.equals("1");
        }
        s = props.getProperty(PROP_DEBUG);
        if (s != null) {
            this.debug = s.equals("1");
        }
        cfgLoadedOn = new Date().getTime();
    }

    public final void reset() {
        synchronized (totalSessionsLock) {
            totalSessions = 0;
        }
        synchronized (totalInLock) {
            totalIn = 0;
        }
        synchronized (totalOutLock) {
            totalOut = 0;
        }
        try {
            loadConfigFile();
        } catch (IOException ex) {
            logger.error("Configuration.reset(): " + ex.getMessage());
        }
    }

    public String getConfigFile() {
        return configFile == null || configFile.isEmpty() ? "(none)" : configFile;
    }

    public String getLogFile() {
        return logFile;
    }

    public String getLockFile() {
        return lockFile;
    }

    public int getTcpReadTimeout() {
        return tcpReadTimeout;
    }

    public int getMaxIncomingQueue() {
        return maxIncomingQueue;
    }

    public int getServicePort() {
        if (servicePort == 0) {
            servicePort = Utils.getEphemeralPort(5000);
        }
        return servicePort;
    }

    public int getMaxSvcSessions() {
        return maxSvcSessions;
    }

    public ProxyConfigsManager getProxyConfigsManager() {
        return proxyConfigsManager;
    }

    public void setProxyConfigsManager(ProxyConfigsManager proxyConfigsManager) {
        this.proxyConfigsManager = proxyConfigsManager;
    }

    public long getTotaIn() {
        synchronized (totalInLock) {
            return totalIn;
        }
    }

    public void incTotalIn(int n) {
        synchronized (totalInLock) {
            this.totalIn = this.totalIn + n;
        }
    }

    public long getTotalOut() {
        synchronized (totalOutLock) {
            return totalOut;
        }
    }

    public void incTotalOut(int n) {
        synchronized (totalOutLock) {
            this.totalOut = this.totalOut + n;
        }
    }

    public void incTotalSessions() {
        synchronized (totalSessionsLock) {
            totalSessions++;
        }
    }

    public long getTotalSessions() {
        synchronized (totalSessionsLock) {
            return totalSessions;
        }
    }

    public long getCfgLoadedOn() {
        return cfgLoadedOn;
    }

    public long getCfgModifiedOn() {
        return cfgModifiedOn;
    }

    public String[] getConfigRulesPaths() {
        return configRulesPaths;
    }

    public void setConfigRulesPaths(String[] configRulesPaths) {
        this.configRulesPaths = configRulesPaths;
    }

    public boolean isResetGracefulMode() {
        return resetGracefulMode;
    }

    public boolean isDebug() {
        synchronized (debugLock) {
            return debug;
        }
    }

    public int getIoBufferSize() {
        return ioBufferSize;
    }

    public String getHexDumpDir() {
        return hexDumpDir;
    }

    public void setDebug(boolean debug) {
        synchronized (debugLock) {
            this.debug = debug;
        }
    }

    public Logger getLogger() {
        return logger = Logger.getInstance();
    }

    class cfgFileMonitor extends java.util.TimerTask {

        private Configuration cfg;

        public cfgFileMonitor(Configuration configuration) {
            this.cfg = configuration;
        }

        @Override
        public void run() {
            File f = new File(configFile);
            long cfgLastModificationTime = f.lastModified();
            if (cfgLastModificationTime > cfg.cfgLoadedOn) {
                logger.debug("CFG Monitor reloaded outdated config file");
                try {
                    cfg.loadConfigFile();
                } catch (IOException ex) {
                    logger.error("CFG Monitor reload: " + ex.getMessage());
                }
            }

        }

    }
}
