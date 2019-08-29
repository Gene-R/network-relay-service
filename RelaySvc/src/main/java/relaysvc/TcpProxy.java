/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author eugener
 */
public class TcpProxy extends Proxy {

    protected ServerSocket srvSocket;

    public TcpProxy(ProxyConfig config) throws Exception {
        super(config);
    }

    @Override
    public void handler() throws IOException {
        if (listenerHostname.equals("*")) {
            srvSocket = new ServerSocket(listenerPort, cfg.getMaxIncomingQueue());
            logger.debug("Hostname for for TCP port " + listenerPort + " declares wildcard '*'. Will listen on all available IP addresses");
        } else {
            InetAddress sourceAddress = InetAddress.getByName(listenerHostname);
            srvSocket = new ServerSocket(listenerPort, cfg.getMaxIncomingQueue(), sourceAddress);
            logger.debug("Listening TCP port " + listenerPort + " only for " + sourceAddress);
        }

        while (true) {
            if (srvSocket == null) {
                break;
            }
            Socket srcSocket = srvSocket.accept();
            logger.debug("Accepted incoming connection from: " + srcSocket);
            incRuleSessions();
            InputStream srcInput = srcSocket.getInputStream();
            OutputStream srcOutput = srcSocket.getOutputStream();
            int count = 0;
            Socket dstSocket;
            boolean connected = false;
            int balancerIndex = getCurrentBalancerIndex();
            do { // trying to estanlish connection with destination peer
                count++;
                dstSocket = new Socket();
                dstSocket.setSoTimeout(cfg.getTcpReadTimeout() * 1000);
                try {
                    InetSocketAddress dstAddress = dstAddresses.get(balancerIndex).getInetSockAddress();
                    dstSocket.connect(dstAddress);
                } catch (IOException ex) {
                    // try another address
                }
                connected = dstSocket.isConnected();
                if (connected) {
                    if (config.getPolicy() == RelayPolicy.ROUND_ROBIN) {
                        setNextBalancerIndex();
                    }
                    break;
                }
                if (config.getPolicy() == RelayPolicy.ACTIVATE_IF_STOPPED) {
                    File cmdLine = dstAddresses.get(balancerIndex + 1).getConfigFile();
                    logger.debug("... staring specified TCP listener process: " + cmdLine.getAbsolutePath());
                    startFromCfg(cmdLine.getAbsolutePath());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        //
                    }
                } else {
                    balancerIndex = setNextBalancerIndex();
                }

            } while (count < dstAddresses.size());

            if (connected) {
                logger.debug("TCP redirection from " + srcSocket + " to " + dstSocket);
                InputStream dstInput = dstSocket.getInputStream();
                OutputStream dstOutput = dstSocket.getOutputStream();

                TcpReader srcReader = getSrcReaderInstance();
                TcpReader dstReader = getDstReaderInstance();

                srcReader.init(srcInput, dstOutput, srcSocket, dstSocket);
                dstReader.init(dstInput, srcOutput, dstSocket, srcSocket);

                ProxySession session = new ProxySession(srcReader, dstReader, config, this);
                new Thread(session).start();
                addSession(session);
                logger.debug("Opened proxy session: " + this);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("TcpRelay balancer could not connect to any of the below specified service(s): ");
                int n = 1;
                for (RelayDestination ia : dstAddresses) {
                    if (ia.getInetSockAddress() != null) {
                        sb.append("(").append(n++).append(") ").append(ia.getInetSockAddress().toString()).append(" ");
                    }
                }
                logger.error(sb.toString());
                PrintWriter pw = new PrintWriter(srcOutput);
                pw.print(sb.toString());
                pw.flush();
                srcSocket.close();
            }
        }
    }

    protected TcpReader getSrcReaderInstance() {
        return new TcpReader(true, this, config.getCfg().getIoBufferSize());
    }

    protected TcpReader getDstReaderInstance() {
        return new TcpReader(false, this, config.getCfg().getIoBufferSize());
    }

    @Override
    public void close() {
        try {
            cfg.getProxyConfigsManager().getProxies().remove(this);
            logger.debug("Closing active listener: " + srvSocket);
            srvSocket.close();

            if (!cfg.isResetGracefulMode()) {
                for (ProxySession proxySession : getSessions()) {
                    if (proxySession.getConfig().getConfigUuid().equals(this.getConfig().getConfigUuid())) {
                        logger.debug("Closing active session in the progress: " + proxySession);
                        if (((TcpReader) proxySession.srcReader).sock1 != null && !((TcpReader) proxySession.srcReader).sock1.isClosed()) {
                            ((TcpReader) proxySession.srcReader).sock1.close();
                        }
                        if (((TcpReader) proxySession.srcReader).sock2 != null && !((TcpReader) proxySession.srcReader).sock2.isClosed()) {
                            ((TcpReader) proxySession.srcReader).sock2.close();
                        }
                    }
                }
            }
            srvSocket = null;
        } catch (Exception ex) {
            logger.error("Could not close active listener: " + srvSocket);
        }
    }

    @Override
    public void purgeSession(Reader session) {
        try {
            ((TcpReader) session).in.close();
            ((TcpReader) session).out.close();
            if (!((TcpReader) session).sock1.isClosed()) {
                ((TcpReader) session).sock1.close();
            }
            if (!((TcpReader) session).sock2.isClosed()) {
                ((TcpReader) session).sock2.close();
            }
        } catch (IOException iex) {
            logger.error("TcpProxy.purge(): " + iex.getMessage());
        }
    }

    public void startFromCfg(String cfg) throws IOException {
        try {
            BufferedReader br = new BufferedReader(new FileReader(cfg));
            String s, line = "";
            StringBuilder sb = new StringBuilder();
            while ((s = br.readLine()) != null) {
                line = s.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                sb.append(line);
                sb.append(" ");
            }
            line = sb.toString().trim();
            if (line.isEmpty()) {
                throw new IllegalArgumentException("Specified startup file [" + cfg + "] is empty.");
            }
            ProcessBuilder builder = new ProcessBuilder(line);
            //builder.redirectErrorStream(true);
            Process process = builder.start();
            logger.debug("started command line: " + line);
            //
//            InputStream stdOut = process.getInputStream();
//            byte[] bytes = new byte[512];
//            int size;
//            while ((size = stdOut.read(bytes)) > 0) {
//                String line = "";
//                for (int i = 0; i < size; i++) {
//                    line += (char) bytes[i];
//                }
//                String s = line.toLowerCase();
//                ret.add(line.trim());
//            }
        } catch (Exception ex) {
            logger.error("startCmdLine: " + ex.getMessage());
        }
    }

}
