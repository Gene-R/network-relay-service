/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import generated.*;
import generated.ObjectFactory;
import generated.Relaysvc;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author eugener
 */
public class ProxyConfig {

    private final Configuration cfg = Configuration.getInstance();
    public static final String FLAG_HEXDUMP = "HEX_DUMP";
    public static final String FLAG_TRACE = "TRACE";

    public enum Protocols {
        TCP, UDP, ORA
    }

    private final Protocols protocol;
    private final String src;
    private final List<String> dsts;
    private final String dstsStr;
    private RelayPolicy policy;
    private final String sourceLineInfo;
    private final String configUuid;
    private boolean hexDump;
    private boolean trace;

    public ProxyConfig(String cfgLine, String sourceLineInfo) {
        this.sourceLineInfo = sourceLineInfo;
        String[] ss = cfgLine.split("\\|");
        if (ss.length < 4 || ss.length > 5) {
            throw new IllegalArgumentException("Malformed configuration line [" + cfgLine + "] in " + sourceLineInfo);
        }
        String p = ss[0].trim().toUpperCase();
        if (p.equals("TCP")) {
            this.protocol = Protocols.TCP;
        } else if (p.equals("UDP")) {
            this.protocol = Protocols.UDP;
        } else if (p.equals("ORA")) {
            this.protocol = Protocols.ORA;
        } else {
            throw new IllegalArgumentException(sourceLineInfo + ": Unsupported protocol: " + p);
        }
        this.src = ss[1].trim();
        this.dstsStr = ss[2].trim();
        this.dsts = Arrays.asList(this.dstsStr.split(","));
        String s = ss[3].trim();
        if (s.equals("FIRST_ON_SUCCESS")) {
            this.policy = RelayPolicy.FIRST_ON_SUCCESS;
        } else if (s.equals("ACTIVATE_IF_STOPPED")) {
            this.policy = RelayPolicy.ACTIVATE_IF_STOPPED;
        } else if (s.equals("ROUND_ROBIN")) {
            this.policy = RelayPolicy.ROUND_ROBIN;
        } else if (s.equals("ORA_TRANSFORM")) {
            this.policy = RelayPolicy.ORA_TRANSFORM;
        } else if (s.equals("NONE")) {
            this.policy = RelayPolicy.NONE;
        } else {
            throw new IllegalArgumentException(sourceLineInfo + ": Unsupported policy: " + ss[2]);
        }
        if (ss.length == 5) {
            String[] flags = ss[4].split(",");
            for (String f : flags) {
                String flag = f.trim().toUpperCase();
                if (flag.equals(FLAG_HEXDUMP)) {
                    this.hexDump = true;
                } else if (flag.equals(FLAG_TRACE)) {
                    this.trace = true;
                } else { // there could be other flags in the future
                    throw new IllegalArgumentException(sourceLineInfo + ": Unsupported flag: " + flag);
                }
            }
        }
        this.configUuid = src.replace(":", "_").replace("*", "port");
    }

    public String getSrc() {
        return src;
    }

    public List<String> getDsts() {
        return dsts;
    }

    public RelayPolicy getPolicy() {
        return policy;
    }

    public String getSourceLineInfo() {
        return sourceLineInfo;
    }

    public Relaysvc.Proxy toXmlProxy() {
        ObjectFactory factory = new ObjectFactory();
        Relaysvc.Proxy proxy = factory.createRelaysvcProxy();
        proxy.setType(Protocols.TCP.name());
        proxy.setPolicy(this.getPolicy().name());
        proxy.setSrc(this.getSrc());
        Relaysvc.Proxy.Dsts destinations = factory.createRelaysvcProxyDsts();
        for (String dst : this.getDsts()) {
            destinations.getDst().add(dst);
        }
        proxy.setDsts(destinations);

        Relaysvc.Proxy.Flags flags = factory.createRelaysvcProxyFlags();
        if (this.isHexDump()) {
            flags.getFlag().add(ProxyConfig.FLAG_HEXDUMP);
        }
        if (this.isTrace()) {
            flags.getFlag().add(ProxyConfig.FLAG_TRACE);
        }
        proxy.setFlags(flags);
        return proxy;

    }

    @Override
    public String toString() {

        return protocol + " " + src + " -> " + dstsStr + ", " + policy + ", "
                + FLAG_HEXDUMP + "=" + hexDump + ", " + FLAG_TRACE + "=" + trace;
    }

    public String getCfgDumpSubDir() {
        return protocol.name() + File.separator + configUuid;
    }

    public String getDescription() {
        return this + " (" + sourceLineInfo + ")";
    }

    public void setPolicy(RelayPolicy policy) {
        this.policy = policy;
    }

    public String getConfigUuid() {
        return configUuid;
    }

    public Protocols getProtocol() {
        return protocol;
    }

    public boolean isHexDump() {
        return hexDump;
    }

    public boolean isTrace() {
        return trace;
    }

    public Configuration getCfg() {
        return cfg;
    }

}
