/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author eugener
 */
public class CmdLineParser {

    public static final String ARG_CFG_FILE = "--cfgfile";
    public static final String ARG_PROXY_FILE = "--proxyfile";
    public static final String ARG_GET_FREE_PORTS = "--get_free_ports";
    public static final String ARG_GEN_PROXY_CFG = "--gen_proxyfile";
    public static final String ARG_GEN_PROXY_XML = "--gen_proxyfile_xml";

    private String[] args;
    private Map<String, String> keysVals;

    public CmdLineParser(String[] args) {
        this.args = args;
        keysVals = new HashMap<String, String>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if (args[i].equals(ARG_CFG_FILE)) {
                    keysVals.put(args[i], getSubArgForIndex(i));
                } else if (args[i].equals(ARG_PROXY_FILE)) {
                    keysVals.put(args[i] + i, getSubArgForIndex(i));
                } else if (args[i].equals(ARG_GET_FREE_PORTS)) {
                    keysVals.put(args[i], getSubArgForIndex(i));
                } else if (args[i].equals(ARG_GEN_PROXY_CFG)) {
                    keysVals.put(args[i], getSubArgForIndex(i));
                } else if (args[i].equals(ARG_GEN_PROXY_XML)) {
                    keysVals.put(args[i], getSubArgForIndex(i));
                } else {
                    throw new IllegalArgumentException("Specified unsupported argument " + args[i]);
                }
            }
        }
    }

    private String getSubArgForIndex(int index) {
        if (args.length - 1 > index) {
            return args[index + 1];
        }
        throw new IllegalArgumentException(args[index] + " requires subargument");
    }

    public String[] getByPrefix(String prefix) {
        List<String> lst = new ArrayList<String>();
        Set<String> keys = keysVals.keySet();
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                lst.add(keysVals.get(key));
            }
        }
        return lst.toArray(new String[0]);
    }

    public Map<String, String> getKeysVals() {
        return keysVals;
    }

    public static void printUsage() {
        System.out.println("Usage: RelayService [" + CmdLineParser.ARG_CFG_FILE + " path1] "
                + CmdLineParser.ARG_PROXY_FILE + " path2 " + CmdLineParser.ARG_PROXY_FILE + " path3 ...");
        System.out.println("\tWhere: " + CmdLineParser.ARG_PROXY_FILE + " could be either in plain text or XML format.\n");
        System.out.println("Usage: RelayService " + CmdLineParser.ARG_GET_FREE_PORTS + " <start-end>");
        System.out.println("\texample: RelayService " + CmdLineParser.ARG_GET_FREE_PORTS + " 25001-25010\n");
        System.out.println("Usage: RelayService " + CmdLineParser.ARG_GEN_PROXY_CFG + "|" + CmdLineParser.ARG_GEN_PROXY_XML + " <host1:port1,host2:port2,...>");
        System.out.println("\texample: RelayService " + CmdLineParser.ARG_GEN_PROXY_XML + " orasrv:1521,labapp1:24765,labapp2:27123\n");
    }

}
