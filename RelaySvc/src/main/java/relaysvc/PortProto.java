/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author eugener
 */
public class PortProto {

    private final int port;
    private final Proto proto;
    private List<Integer> extraPorts = new ArrayList<Integer>();

    public PortProto(int port, Proto proto) {
        this.port = port;
        this.proto = proto;
    }

    public PortProto(String str) {
        String[] portProto = str.split("/");
        if (portProto.length != 2) {
            throw new IllegalArgumentException("Malformed <port number/protocol> in " + str);
        }
        String ports = portProto[0];
        if (ports.matches("^[0-9]*-[0-9]*$")) {
            String ss[] = ports.split("-");
            this.port = Integer.valueOf(ss[0]);
            int endPorts = Integer.valueOf(ss[1]);
            if (endPorts > this.port) {
                for (int i = this.port + 1; i <= endPorts; i++) {
                    extraPorts.add(i);
                }
            }
        } else {
            this.port = Integer.valueOf(ports);
        }
        if (portProto[1].equals("tcp")) {
            this.proto = Proto.TCP;
        } else if (portProto[1].equals("udp")) {
            this.proto = Proto.UDP;
        } else {
            throw new IllegalArgumentException("Illegal protocol " + portProto[1] + " in " + str);
        }
    }

    public List<Integer> getExtraPorts() {
        return extraPorts;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PortProto)) {
            return false;
        }
        PortProto another = (PortProto) obj;
        return port == another.getPort() && proto == another.getProto();
    }

    @Override
    public int hashCode() {
        return port + proto.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.port);
        sb.append("/");
        sb.append(this.proto.name());
        return sb.toString();
    }

    public int getPort() {
        return port;
    }

    public Proto getProto() {
        return proto;
    }

}
