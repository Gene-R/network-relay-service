/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

/**
 *
 * @author eugener
 */
public class TcpReader extends Reader {

    protected InputStream in;
    protected OutputStream out;
    protected Socket sock1;
    protected Socket sock2;

    public TcpReader(boolean usedAsInput, Proxy proxy, int bufferSize) {
        super(usedAsInput, proxy, bufferSize);
    }

    public void init(InputStream in, OutputStream out, Socket sock1, Socket sock2) {
        this.in = new BufferedInputStream(in);
        this.out = out;
        this.sock1 = sock1;
        this.sock2 = sock2;
    }

    @Override
    public void run() {
        try {
            do {
                int size = 0;
                byte[] bytes = new byte[bufferSize];
                size = in.read(bytes);
                if (size == -1 || size == 0 || sock1.isClosed() || sock2.isClosed()) {
                    break;
                }
                out.write(bytes, 0, size);
                printBytes(size);
                printHexDump(Arrays.copyOfRange(bytes, 0, size));
                incTotalBytes(size);
                if (usedAsInput) {
                    proxy.incTotalRuleIn(size);
                } else {
                    proxy.incTotalRuleOut(size);
                }
            } while (true);
        } catch (SocketException ex) {
            if (!ex.getMessage().toLowerCase().contains("closed")) {
                logger.error("TcpReader().run(): SocketException: " + ex.getMessage());
            }
        } catch (IOException ex) {
            logger.error("TcpReader().run(): " + ex.getMessage());
        } finally {
            super.close();
            proxy.purgeSession(this);
        }
    }

    @Override
    public String toString() {
        return sock1.getInetAddress() + ":" + sock1.getLocalPort() + "-" + sock1.getPort()
                + " -> "
                + sock2.getInetAddress() + ":" + sock2.getLocalPort() + "-" + sock2.getPort();
    }

}
