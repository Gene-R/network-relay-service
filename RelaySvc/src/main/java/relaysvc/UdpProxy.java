/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 *
 * @author eugener
 */
public class UdpProxy extends Proxy {

    protected DatagramSocket svcSock;
    protected InetSocketAddress srcAddress;
    protected InetSocketAddress dstAddress;
    protected int ephemeralPort;

    public UdpProxy(ProxyConfig config) throws Exception {
        super(config);
        ephemeralPort = Utils.getEphemeralPort();
    }

    @Override
    public void handler() throws IOException {
        if (listenerHostname.equals("*")) {
            srcAddress = new InetSocketAddress(listenerPort);
            svcSock = new DatagramSocket(listenerPort);
            logger.debug("Hostname for for UDP port " + listenerPort + " declares wildcard '*'. Will bind to all available IP addresses");
        } else {
            srcAddress = new InetSocketAddress(InetAddress.getByName(listenerHostname), listenerPort);
            svcSock = new DatagramSocket(srcAddress);
            svcSock.connect(srcAddress);
            logger.debug("Binding for UDP port " + listenerPort + " only for " + srcAddress);
        }
        int count = 0;
        boolean connected = false;
        int balancerIndex = getCurrentBalancerIndex();
        do {
            count++;
            try {
                dstAddress = dstAddresses.get(balancerIndex).getInetSockAddress();
                connected = true;
            } catch (Exception ex) {
                logger.error("UDP connect: " + ex.getMessage());
            }

            if (connected) {
                if (config.getPolicy() == RelayPolicy.ROUND_ROBIN) {
                    setNextBalancerIndex();
                }
                break;
            }
            balancerIndex = setNextBalancerIndex();
        } while (count < dstAddresses.size());

        if (connected) {
            logger.debug("UDP redirection from " + svcSock.getLocalSocketAddress() + ":" + svcSock.getLocalPort() + " to " + dstAddress);
            UdpReader udpReader = new UdpReader(true, this, config.getCfg().getIoBufferSize());
            udpReader.init(svcSock, dstAddress);
            ProxySession session = new ProxySession(udpReader, null, config, this);
            incRuleSessions();
            new Thread(session).start();
            addSession(session);
            logger.debug("Opened proxy session: " + this);
        } else {
            logger.error("ERROR: Relay service balancer could not connect to any of the below specified service(s):");
            int n = 1;
            for (RelayDestination ia : dstAddresses) {
                logger.error(n++ + ") " + ia.getInetSockAddress().toString());
            }
        }

    }

    @Override
    public void close() {
        try {
            cfg.getProxyConfigsManager().getProxies().remove(this);
            logger.debug("Closing active listener: " + svcSock);
            svcSock.close();

            if (!cfg.isResetGracefulMode()) {
                for (ProxySession proxySession : getSessions()) {
                    if (proxySession.getConfig().getConfigUuid().equals(this.getConfig().getConfigUuid())) {
                        logger.debug("Closing active session in the progress: " + proxySession);
                        if (((UdpReader) proxySession.srcReader).srcSock != null && !((UdpReader) proxySession.srcReader).srcSock.isClosed()) {
                            ((UdpReader) proxySession.srcReader).srcSock.close();
                        }
                    }
                }
            }
            svcSock = null;
        } catch (Exception ex) {
            logger.error("Could not close active listener: " + svcSock);
        }
    }

    @Override
    public void purgeSession(Reader session) {
        //
    }
}
