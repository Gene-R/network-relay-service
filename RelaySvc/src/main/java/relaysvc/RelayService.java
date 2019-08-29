/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import generated.ObjectFactory;
import generated.Relaysvc;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import static relaysvc.CmdLineParser.*;

/**
 *
 * @author eugener
 */
public class RelayService {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            try {
                CmdLineParser cmd = new CmdLineParser(args);
                if (cmd.getKeysVals().get(ARG_GET_FREE_PORTS) != null) {
                    String[] ss = cmd.getKeysVals().get(ARG_GET_FREE_PORTS).split("-");
                    if (ss.length != 2) {
                        throw new IllegalArgumentException(ARG_GET_FREE_PORTS + " n1-n2");
                    }
                    int min = Integer.valueOf(ss[0]);
                    int max = Integer.valueOf(ss[1]);
                    EtcServices etcSvc = new EtcServices();
                    etcSvc.load();
                    Deque<Integer> ports = etcSvc.getUnusedPortsInc(min, max, true);
                    for (Integer i : ports) {
                        System.out.println(i);
                    }
                    System.exit(0);
                } else if (cmd.getKeysVals().get(ARG_GEN_PROXY_CFG) != null || cmd.getKeysVals().get(ARG_GEN_PROXY_XML) != null) {
                    String[] ss;
                    List<ProxyConfig> proxies = new ArrayList<ProxyConfig>();
                    boolean isCfg = cmd.getKeysVals().get(ARG_GEN_PROXY_CFG) != null;
                    if (isCfg) {
                        ss = cmd.getKeysVals().get(ARG_GEN_PROXY_CFG).split(",");
                    } else {
                        ss = cmd.getKeysVals().get(ARG_GEN_PROXY_XML).split(",");
                    }
                    EtcServices etcSvc = new EtcServices();
                    etcSvc.load();
                    Deque ports = etcSvc.getUnusedPortsInc(20000, 30000, true, ss.length);
                    for (String s : ss) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(ProxyConfig.Protocols.TCP.name());
                        sb.append("|");
                        sb.append("*:");
                        sb.append(ports.poll());
                        sb.append("|");
                        sb.append(s);
                        sb.append("|");
                        sb.append(RelayPolicy.ROUND_ROBIN.name());
                        if (isCfg) {
                            System.out.println(sb.toString());
                        } else {
                            proxies.add(new ProxyConfig(sb.toString(), "generated"));
                        }
                    }
                    if (!isCfg) {
                        ObjectFactory factory = new ObjectFactory();
                        Relaysvc relaySvc = factory.createRelaysvc();
                        for (ProxyConfig pc : proxies) {
                            relaySvc.getProxy().add(pc.toXmlProxy());
                        }
                        JAXBContext context = JAXBContext.newInstance("generated");
                        Marshaller marshaller = context.createMarshaller();
                        marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
                        marshaller.marshal(relaySvc, System.out);
                    }
                    System.exit(0);
                }

                // starts as daemon
                String cfgFile = cmd.getKeysVals().get(ARG_CFG_FILE);
                Configuration cfg = Configuration.getInstance();
                if (cfgFile != null) {
                    cfg.loadConfigFile(cfgFile);
                    System.out.println("Specified service configuration file: " + cfgFile);
                }
                if (!lockInstance()) {
                    System.out.println("Already running");
                    System.exit(1);
                }
                args = cmd.getByPrefix(ARG_PROXY_FILE);
                if (args.length == 0) {
                    throw new IllegalArgumentException("at least one --proxyfile ... argument must be specified");
                }
                new Thread(MonitorService.getInstance()).start();
                ProxyConfigsManager proxyConfigsManager = new ProxyConfigsManager(args);
                cfg.setProxyConfigsManager(proxyConfigsManager);
                proxyConfigsManager.start();
                System.out.println("Relay service is started on port "
                        + cfg.getServicePort() + ". See log file ["
                        + cfg.getLogFile() + "] for details.");
                cfg.getLogger().info("RelaySvc has started");
            } catch (Exception ex) {
                System.err.println("ERROR: " + ex.getMessage());
                CmdLineParser.printUsage();
                System.exit(1);
            }
        } else {
            CmdLineParser.printUsage();
        }

    }

    private static boolean lockInstance() {
        final File file = new File(Configuration.getInstance().getLockFile());
        try {
            final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            final FileLock fileLock = randomAccessFile.getChannel().tryLock();
            if (fileLock != null) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        try {
                            fileLock.release();
                            randomAccessFile.close();
                            file.delete();
                        } catch (Exception e) {
                            System.err.println("Unable to remove lock file: " + file.getAbsolutePath() + ".\nERROR: " + e.getMessage());
                        }
                    }
                });
                return true;
            }
        } catch (Exception e) {
            System.err.println("Unable to create and/or lock file: " + file.getAbsolutePath() + ".\nERROR: " + e.getMessage());
        }
        return false;
    }

}
