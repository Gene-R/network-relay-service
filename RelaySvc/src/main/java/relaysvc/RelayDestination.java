/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 *
 * @author eugener
 */
public class RelayDestination {

    private InetSocketAddress inetSockAddress;
    private File configFile;

    public RelayDestination(String dstInput, RelayPolicy policy) throws IOException {
        dstInput = dstInput.trim();
        if ((policy == RelayPolicy.ORA_TRANSFORM || policy == RelayPolicy.ACTIVATE_IF_STOPPED) && dstInput.startsWith("file://")) {
            String path = dstInput.replace("file://", "");
            File f = new File(path);
            if (!f.exists()) {
                throw new IOException("Specified file [" + path + "] for policy " + policy.name() + " does not exit: " + f.getCanonicalPath());
            }
            configFile = f;
        } else {
            String[] ss = dstInput.trim().split(":");
            if (ss.length == 2) {
                inetSockAddress = new InetSocketAddress(ss[0], Integer.valueOf(ss[1]));
            } else {
                throw new IOException("The destination address for non-transformation specific policies has to use 'hostname:port' format: " + dstInput);
            }
        }
    }

    public InetSocketAddress getInetSockAddress() {
        return inetSockAddress;
    }

    public File getConfigFile() {
        return configFile;
    }

}
