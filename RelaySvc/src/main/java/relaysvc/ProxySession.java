/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.util.Date;
import java.util.UUID;

/**
 *
 * @author eugener
 */
public class ProxySession implements Runnable {

    protected final Logger logger = Logger.getInstance();
    protected final Reader srcReader;
    protected final Reader dstReader;
    private final ProxyConfig config;
    private final Date created;
    private final Proxy proxy;
    private final String sessionId;

    public ProxySession(Reader srcReader, Reader dstReader, ProxyConfig config, Proxy proxy) {
        this.srcReader = srcReader;
        this.dstReader = dstReader;
        this.config = config;
        this.created = new Date();
        this.proxy = proxy;
        this.sessionId = UUID.randomUUID().toString();
        this.srcReader.setSessionId(sessionId);
        if (dstReader != null) {
            this.dstReader.setSessionId(sessionId);
        }
    }

    @Override
    public String toString() {
        if (dstReader != null) {
            return srcReader + ", session bytes in/out: " + Utils.getHumanReadableLong(srcReader.getTotalBytes())
                    + " / " + Utils.getHumanReadableLong(dstReader.getTotalBytes()) 
                    + ", HEX_DUMP=" + srcReader.isPrintHexDump() + ", TRACE=" + srcReader.isPrintTrace();
        } else {
            return srcReader + ", session bytes: " + Utils.getHumanReadableLong(srcReader.getTotalBytes()) 
                    + ", HEX_DUMP=" + srcReader.isPrintHexDump() + ", TRACE=" + srcReader.isPrintTrace();
        }
    }

    @Override
    public void run() {

        try {
            Thread r1 = new Thread(srcReader);
            Thread r2 = null;
            r1.start();
            if (dstReader != null) {
                r2 = new Thread(dstReader);
                r2.start();
            }
            r1.join();
            if (r2 != null) {
                r2.join();
            }

            if (proxy != null) {
                synchronized (proxy.sessionsLock) {
                    int ind = proxy.sessions.indexOf(this);
                    if (ind != -1) {
                        proxy.sessions.remove(ind);
                        logger.debug("Closed proxy session: " + this);
                    } else {
                        logger.error("Cannot purge proxy session: " + this);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("ProxySession.run(): " + ex.getMessage());
        }

    }

    public Reader getSrcReader() {
        return this.srcReader;
    }

    public Reader getDstReader() {
        return this.dstReader;
    }

    public ProxyConfig getConfig() {
        return this.config;
    }

    public Date getCreated() {
        return this.created;
    }

    public String getSessionId() {
        return sessionId;
    }

}
