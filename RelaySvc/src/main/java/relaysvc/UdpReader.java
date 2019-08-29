/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketOptions;

/**
 *
 * @author eugener
 */
class UdpReader extends Reader {

    protected DatagramSocket srcSock;
    protected InetSocketAddress dstAddr;

    public UdpReader(boolean usedAsInput, Proxy proxy, int bufferSize) {
        super(usedAsInput, proxy, bufferSize);
    }

    void init(DatagramSocket srcSock, InetSocketAddress dstAddr) {
        this.srcSock = srcSock;
        this.dstAddr = dstAddr;
    }

    @Override
    public void run() {
        byte[] buff = new byte[SocketOptions.SO_RCVBUF];
        try {
            do {
                DatagramPacket inPacket = new DatagramPacket(buff, buff.length, srcSock.getLocalSocketAddress());
                srcSock.receive(inPacket);
                int size = inPacket.getLength();
                incTotalBytes(size);
                UDP u = new UDP(inPacket);
                if (u.getPort() != dstAddr.getPort()) {
                    proxy.incTotalRuleIn(size);
                    DatagramPacket outPacket = new DatagramPacket(u.getData(), u.getSize(), dstAddr);
                    srcSock.send(outPacket);
                    logger.debug("Forwarded UDP packet " + u.getMsgType() + ": " + u.getAddress() + ":" + u.getPort() + " -> " + dstAddr + ", length: " + u.getSize());
                } else {
                    proxy.incTotalRuleOut(size);
//                    DatagramPacket outPacket = new DatagramPacket(u.getData(), u.getSize(), dstAddr);
//                    srcSock.send(outPacket);
//                    logger.debug("UDP RECEIVED: " + u.getMsgType() + ":   " + u.getAddress() + ":" + u.getPort() + " -> " + dstAddr + ", length: " + u.getSize());
                    logger.debug("Received UDP packet  " + u.getMsgType() + ": " + u.getAddress() + ":" + u.getPort() + ", length: " + u.getSize());
                }
                printHexDump(u.getData());

            } while (true);
        } catch (SocketException ex) {
            if (!ex.getMessage().toLowerCase().contains("closed")) {
                logger.error("UdpReader().run(): SocketException: " + ex.getMessage());
            }
        } catch (IOException ex) {
            logger.error("UdpReader().run(): " + ex.getMessage());
        } finally {
            super.close();
            proxy.purgeSession(this);
        }
    }

    @Override
    public String toString() {
        if (!srcSock.isClosed()) {
            return srcSock.getLocalSocketAddress().toString();
        } else {
            return "closed";
        }
    }

}
