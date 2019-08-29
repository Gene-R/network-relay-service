/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 *
 * @author eugener
 */
public class UDP {

    private final String hostname;
    private final String address;
    private final int port;
    private final int size;
    private final byte[] data;

    private String msgType; //13-21
    private InetAddress agentIpAddr; //31-74
    private int agentPort; //64-65

    public UDP(DatagramPacket datagram) {
        this.hostname = datagram.getAddress().getHostName();
        this.address = datagram.getAddress().getHostAddress();
        this.port = datagram.getPort();
        this.size = datagram.getLength();
        this.data = datagram.getData();

        try {
            msgType = new String(Arrays.copyOfRange(data, 12, 22), "UTF-8");
            if (msgType.startsWith("DSA")) {
                msgType = "ORB " + msgType.substring(0, msgType.indexOf(0));
            } else {
                msgType = "OTHER";
            }
            agentIpAddr = InetAddress.getByAddress(Arrays.copyOfRange(data, 72, 76));
            agentPort = new BigInteger(Arrays.copyOfRange(data, 63, 65)).intValue();
        } catch (Exception ex) {
            //
        }

    }

    public byte[] repalceIpAddr(byte[] in, String newIpAddr) throws UnknownHostException {
        InetAddress ia = InetAddress.getByName(newIpAddr);
        byte[] newAddr = ia.getAddress();
        byte[] out = in;
        System.arraycopy(newAddr, 0, out, 72, 4);
        return out;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public int getSize() {
        return size;
    }

    public byte[] getData() {
        return data;
    }

    public String getAddress() {
        return address;
    }

    public String getMsgType() {
        return msgType;
    }

    public InetAddress getAgentIpAddr() {
        return agentIpAddr;
    }

    public int getAgentPort() {
        return agentPort;
    }

    @Override
    public String toString() {
        return hostname + "/" + address + ":" + port + ", lenght: " + size;
    }

}
