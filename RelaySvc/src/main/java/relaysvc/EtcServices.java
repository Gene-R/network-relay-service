/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author eugener
 */
final class EtcServicesEntry {

    private final String line;
    private final String name;
    private final PortProto portProto;
    private final List<String> aliases;
    private final String comment;

    public EtcServicesEntry(EtcServicesEntry entry, int port) {
        this.name = entry.getName();
        this.portProto = new PortProto(port, entry.getPortProto().getProto());
        this.aliases = entry.getAliases();
        this.comment = entry.getComment() + " for port " + port;
        this.line = this.toString();
    }

    public EtcServicesEntry(String line) {
        this.aliases = new ArrayList<String>();
        this.line = line;
        String tmp = line;
        int ind = line.lastIndexOf("#");
        if (ind != -1) {
            this.comment = line.substring(ind);
            tmp = line.substring(0, ind);
        } else {
            this.comment = "";
        }
        String[] ss = tmp.split("\\s{1,}");
        if (ss.length < 2) {
            throw new IllegalArgumentException("Malformed entry in " + line);
        }
        this.name = ss[0];
        this.portProto = new PortProto(ss[1]);
        if (ss.length > 2) {
            for (int i = 2; i < ss.length; i++) {
                aliases.add(ss[i]);
            }
        }
    }

    public String getLine() {
        return line;
    }

    public String getName() {
        return name;
    }

    public PortProto getPortProto() {
        return portProto;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("\t");

        sb.append(portProto);

        sb.append("\t");
        for (String alias : aliases) {
            sb.append(alias);
            sb.append(" ");
        }
        sb.append("\t");
        sb.append(comment);
        return sb.toString();
    }
}

public class EtcServices {

    public static final int TCP_MAX_PORT = 65535;
    public static final int TCP_MIN_PORT = 1;
    public static final int TCP_USER_DEFINED_MIN = 1024;

    private Map<PortProto, EtcServicesEntry> portProtoEntries;
    private Map<Integer, EtcServicesEntry> portEntries;
    private Set<Integer> allocatedPorts;

    public void load() throws IOException {
        portProtoEntries = new HashMap<PortProto, EtcServicesEntry>();
        portEntries = new HashMap<Integer, EtcServicesEntry>();
        allocatedPorts = new HashSet<Integer>();
        String path = "/etc/services";
        boolean win = System.getProperties().getProperty("os.name").toLowerCase().startsWith("windows");
        if (win) {
            String windir = System.getenv("windir");
            if (windir == null) {
                throw new IOException("WINDIR is not set");
            }
            path = windir + "\\system32\\drivers\\etc\\services";
        }
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            EtcServicesEntry entry = new EtcServicesEntry(line);
            portProtoEntries.put(entry.getPortProto(), entry);
            portEntries.put(entry.getPortProto().getPort(), entry);
            if (!entry.getPortProto().getExtraPorts().isEmpty()) {
                for (Integer portNum : entry.getPortProto().getExtraPorts()) {
                    EtcServicesEntry entry2 = new EtcServicesEntry(entry, portNum);
                    portProtoEntries.put(entry2.getPortProto(), entry2);
                    portEntries.put(entry2.getPortProto().getPort(), entry2);
                }
            }
        }
    }

    public boolean isPortTaken(int port, Proto proto) {
        checkLegalRange(port);
        EtcServicesEntry entry = portProtoEntries.get(new PortProto(port, proto));
        return entry != null;
    }

    public static void checkLegalRange(int port) {
        if (port > TCP_MAX_PORT || port < TCP_MIN_PORT) {
            throw new IllegalArgumentException("Port " + port + " is outside of legal range: " + TCP_MIN_PORT + "-" + TCP_MAX_PORT);
        }
    }

    public static boolean canStartSrvSocket(int port) throws IOException {
        checkLegalRange(port);
        try {
            ServerSocket srvSock = new ServerSocket(port);
            srvSock.close();
            return true;
        } catch (BindException e) {
            return false;
        }
    }

    public Deque<Integer> getUnusedPortsInc(int from, int to, boolean checkSrvSocket) throws IOException { // inclusive
        return getUnusedPortsInc(from, to, checkSrvSocket, 0); // 0 - unlimited size
    }

    public Deque<Integer> getUnusedPortsInc(int from, int to, boolean checkSrvSocket, int requiredListSize) throws IOException { // inclusive with 
        Deque<Integer> ret = new LinkedList<Integer>();
        for (int i = from; i <= to; i++) {
            EtcServicesEntry e = portEntries.get(i);
            if (e == null && !allocatedPorts.contains(i)) {
                if (checkSrvSocket && !canStartSrvSocket(i)) {
                    continue;
                }
                allocatedPorts.add(i);
                ret.add(i);
                if (requiredListSize > 0 && ret.size() == requiredListSize) {
                    return ret;
                }
            }
        }
        if (requiredListSize > 0 && ret.size() < requiredListSize) {
            throw new IOException("Could not allocate the required list of unused ports");
        }
        return ret;
    }

    public Deque<Integer> getUnusedPortsInc(boolean checkListener) throws IOException {
        return EtcServices.this.getUnusedPortsInc(TCP_MIN_PORT, TCP_MAX_PORT, checkListener);
    }

}
