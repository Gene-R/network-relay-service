/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author eugener
 */
abstract public class Reader implements Runnable {

    private final Configuration cfg = Configuration.getInstance();
    protected final Logger logger = Logger.getInstance();
    private volatile long totalBytes;
    private final Object totalBytesLock = new Object();
    protected final boolean usedAsInput;
    protected final Proxy proxy;
    private volatile boolean printHexDump;
    private volatile boolean printTrace;
    private final static Object printHexDumpLock = new Object();
    private final static Object printTraceLock = new Object();
    private String sessionId;
    private final TraceDumper traceDumper;
    protected int bufferSize;

    public Reader(boolean usedAsInput, Proxy proxy, int bufferSize) {
        this.proxy = proxy;
        this.usedAsInput = usedAsInput;
        this.bufferSize = bufferSize;
        this.printHexDump = proxy.getConfig().isHexDump();
        this.printTrace = proxy.getConfig().isTrace();
        this.traceDumper = new TraceDumper(cfg.getHexDumpDir() + File.separator + proxy.getConfig().getCfgDumpSubDir());
    }

    @Override
    abstract public String toString();

    public void incTotalBytes(int n) {
        synchronized (this.totalBytesLock) {
            this.totalBytes = this.totalBytes + n;
        }
    }

    public long getTotalBytes() {
        synchronized (this.totalBytesLock) {
            return totalBytes;
        }
    }

    public void printHexDump(byte[] bytes) throws IOException {
        if (isPrintHexDump()) {
            getHexDump(bytes);
        }
    }

    public void close() {
        traceDumper.close();
    }

    private String getPayloadSummary(int size) {
        StringBuilder sb = new StringBuilder();
        sb.append("=> ");
        sb.append(usedAsInput ? " input:" : " output:");
        sb.append(size);
        sb.append(" byte(s)");
        return sb.toString();
    }

    public void printBytes(int size) {
        if (cfg.isDebug() || isPrintTrace()) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(Utils.getCurTS());
                sb.append("\t");
                sb.append(getPayloadSummary(size));
                traceDumper.addPayLoad(Type.HEX, null, sb);
            } catch (IOException ex) {
                logger.error("Reader().printBytes(): " + ex.getMessage());
            }
        }
    }

    public void getHexDump(byte[] bytes) throws IOException {
        getHexDump(bytes, null);
    }

    public synchronized void getHexDump(byte[] bytes, String header) throws IOException {
        StringBuilder hexDump = new StringBuilder();
        if (!cfg.isDebug() && !isPrintTrace()) {
            hexDump.append(Utils.getCurTS());
            hexDump.append("\t");
        }
        if (header != null) {
            hexDump.append(header);
        }
        traceDumper.addPayLoad(Type.HEX, bytes, hexDump);
    }

    public boolean isPrintHexDump() {
        synchronized (printHexDumpLock) {
            return printHexDump || proxy.isPrintHexDump();
        }
    }

    public void setPrintHexDump(boolean printHexDump) {
        synchronized (printHexDumpLock) {
            this.printHexDump = printHexDump;
        }
    }

    public boolean isPrintTrace() {
        synchronized (printTraceLock) {
            return printTrace || proxy.isPrintTrace();
        }
    }

    public void setPrintTrace(boolean printTrace) {
        synchronized (printTraceLock) {
            this.printTrace = printTrace;
        }
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
        traceDumper.open(sessionId);
    }

    public TraceDumper getHexDumper() {
        return traceDumper;
    }

}
