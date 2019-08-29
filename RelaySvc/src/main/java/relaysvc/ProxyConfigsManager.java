/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import generated.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

/**
 *
 * @author eugener
 */
public class ProxyConfigsManager {

    private final Configuration cfg = Configuration.getInstance();
    private final Logger logger = cfg.getLogger();
    private List<Proxy> proxies;
    private volatile List<Proxy> listenerThreads;
    private final Object listenerThreadsLock = new Object();
    private Date created;

    public ProxyConfigsManager(String[] configPaths) throws IllegalArgumentException {
        this.listenerThreads = new ArrayList<Proxy>();
        if (configPaths == null || configPaths.length == 0) {
            throw new IllegalArgumentException("the list of config files cannot be empty");
        }
        cfg.setConfigRulesPaths(configPaths);
    }

    public void start() throws IOException {
        proxies = new ArrayList<Proxy>();
        cfg.reset();
        List<ProxyConfig> proxyLst = new ArrayList<ProxyConfig>();
        for (String cp : cfg.getConfigRulesPaths()) {
            if (cp.endsWith(".xml")) {
                ProxyXml xml = new ProxyXml(cp);
                xml.load();
                proxyLst.addAll(xml.getProxyConfigs());
            } else {
                File f = new File(cp);
                if (!f.isFile()) {
                    throw new IOException("specified confif file does not exist: " + cp);
                }
                BufferedReader input = null;
                String line = "";
                int configLine = 0;
                try {
                    input = new BufferedReader(new FileReader(cp));
                    while ((line = input.readLine()) != null) {
                        try {
                            configLine++;
                            line = line.trim();
                            if (line.isEmpty() || line.startsWith("#")) {
                                continue;
                            }
                            ProxyConfig rc = new ProxyConfig(line, cp + ":" + configLine);
                            proxyLst.add(rc);
                            logger.debug("Processed proxy configuration line: " + rc.getSourceLineInfo());
                        } catch (Exception ex) {
                            String s = ex.getMessage() + " for >>>" + line + "<<<";
                            logger.error(s);
                            //throw new IOException(s);
                        }
                    }
                } finally {
                    if (input != null) {
                        input.close();
                    }
                }
            }
        }

        synchronized (listenerThreadsLock) {
            try {
                for (ProxyConfig rc : proxyLst) {
                    Proxy proxy = Proxy.getProxyInstance(rc);
                    proxies.add(proxy);
                    Thread t = new Thread(proxy);
                    listenerThreads.add(proxy);
                    t.start();
                    logger.debug("Initialized " + rc.getDescription());
                }
            } catch (Exception ex) {
                logger.error("ProxyConfigsManager.start(): " + ex.getMessage());
            }
        }

        created = new Date();
    }

    public List<Proxy> getProxies() {
        synchronized (listenerThreadsLock) {
            return proxies;
        }
    }

    public void reset() throws IOException {
        synchronized (listenerThreadsLock) {
            for (Proxy listener : listenerThreads) {
                listener.close();
            }
            listenerThreads.removeAll(listenerThreads);
        }
        start();
    }

    public Date getCreated() {
        return created;
    }

}
