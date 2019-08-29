/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.Properties;
import oracle.net.ns.NetException;
import oracle.net.nt.NTAdapter;

/**
 *
 * @author eugener
 */
public class MyNTAdapter implements NTAdapter {

    InputStream inputStream;

    static final boolean DEBUG = false;
    int port;
    String host;
    private SocketChannel socketChannel;
    protected Socket socket;
    protected int sockTimeout;
    protected Properties socketOptions;

    public MyNTAdapter(InputStream inputStream, Socket socket) throws NLException {

        this.socket = socket;
        this.host = socket.getLocalAddress().getHostAddress();
        this.port = socket.getLocalPort();
        if ((this.port < 0) || (this.port > 65535)) {
            throw new NLException(new NetException(116).getMessage());
        }
    }

    public void connect()
            throws IOException {
        String str = (String) this.socketOptions.get(Integer.valueOf(2));

        boolean bool = Boolean.parseBoolean((String) this.socketOptions.get(Integer.valueOf(18)));

        InetAddress[] arrayOfInetAddress = InetAddress.getAllByName(this.host);
        if ((bool) && (arrayOfInetAddress.length > 1)) {
            arrayOfInetAddress = getAddressesInCircularOrder(this.host, arrayOfInetAddress);
        }
        int i = arrayOfInetAddress.length;

        int j = 0;
        Boolean localBoolean = Boolean.valueOf(Boolean.parseBoolean((String) this.socketOptions.get(Integer.valueOf(20))));
        do {
            InetAddress localInetAddress = arrayOfInetAddress[j];
            j++;
            i--;
            if (!localBoolean.booleanValue()) {
                this.socket = new Socket();
            }
            try {
                if (localBoolean.booleanValue()) {
                    this.socketChannel = SocketChannel.open(new InetSocketAddress(localInetAddress, this.port));

                    this.socket = this.socketChannel.socket();
                } else {
                    this.socket.connect(new InetSocketAddress(localInetAddress, this.port), Integer.parseInt(str));
                }
            } catch (IOException localIOException) {
                try {
                    if (this.socket != null) {
                        this.socket.close();
                    }
                } catch (Exception localException) {
                }
                if (i <= 0) {
                    throw localIOException;
                }
            }
        } while (j < arrayOfInetAddress.length);
        setOption(3, str);
        setSocketOptions();
    }

    public void setSocketOptions()
            throws IOException {
        String str;
        if ((str = (String) this.socketOptions.get(Integer.valueOf(0))) != null) {
            setOption(0, str);
        }
        if ((str = (String) this.socketOptions.get(Integer.valueOf(1))) != null) {
            setOption(1, str);
        }
    }

    public void disconnect()
            throws IOException {
        try {
            this.socket.close();
        } finally {
            this.socket = null;
        }
    }

    public InputStream getInputStream()
            throws IOException {
        return this.socket.getInputStream();
    }

    public OutputStream getOutputStream()
            throws IOException {
        return this.socket.getOutputStream();
    }

    public void setOption(int paramInt, Object paramObject)
            throws IOException, NetException {
        String str;
        switch (paramInt) {
            case 0:
                str = (String) paramObject;
                this.socket.setTcpNoDelay(str.equals("YES"));

                break;
            case 1:
                str = (String) paramObject;
                if (str.equals("YES")) {
                    this.socket.setKeepAlive(true);
                }
                break;
            case 3:
                this.sockTimeout = Integer.parseInt((String) paramObject);
                this.socket.setSoTimeout(this.sockTimeout);
                break;
        }
    }

    public Object getOption(int paramInt)
            throws IOException, NetException {
        switch (paramInt) {
            case 1:
                return "" + this.sockTimeout;
            case 3:
                return Integer.toString(this.socket.getSoTimeout());
        }
        return null;
    }

    public void abort()
            throws NetException, IOException {
        if (this.socket != null) {
            try {
                this.socket.setSoLinger(true, 0);
            } catch (Exception localException) {
            }
            this.socket.close();
        }
    }

    public void sendUrgentByte(int paramInt)
            throws IOException {
        this.socket.sendUrgentData(paramInt);
    }

    public boolean isCharacteristicUrgentSupported()
            throws IOException {
        try {
            return !this.socket.getOOBInline();
        } catch (IOException localIOException) {
        }
        return false;
    }

    public void setReadTimeoutIfRequired(Properties paramProperties)
            throws IOException, NetException {
        String str = (String) paramProperties.get("oracle.net.READ_TIMEOUT");
        if (str == null) {
            str = "0";
        }
        setOption(3, str);
    }

    public String toString() {
        return "host=" + this.host + ", port=" + this.port + "\n    socket_timeout=" + this.sockTimeout + ", socketOptions=" + this.socketOptions.toString() + "\n    socket=" + this.socket;
    }

    private static Hashtable<String, InetAddress[]> inetaddressesCache = new Hashtable();
    private static Hashtable<String, Integer> circularOffsets = new Hashtable();

    static final synchronized InetAddress[] getAddressesInCircularOrder(String paramString, InetAddress[] paramArrayOfInetAddress) {
        InetAddress[] arrayOfInetAddress1 = (InetAddress[]) inetaddressesCache.get(paramString);
        Integer localInteger = (Integer) circularOffsets.get(paramString);
        if ((arrayOfInetAddress1 == null) || (!areEquals(arrayOfInetAddress1, paramArrayOfInetAddress))) {
            localInteger = new Integer(0);
            arrayOfInetAddress1 = paramArrayOfInetAddress;
            inetaddressesCache.put(paramString, paramArrayOfInetAddress);
            circularOffsets.put(paramString, localInteger);
        }
        InetAddress[] arrayOfInetAddress2 = getCopyAddresses(arrayOfInetAddress1, localInteger.intValue());
        circularOffsets.put(paramString, new Integer((localInteger.intValue() + 1) % arrayOfInetAddress1.length));
        return arrayOfInetAddress2;
    }

    private static final boolean areEquals(InetAddress[] paramArrayOfInetAddress1, InetAddress[] paramArrayOfInetAddress2) {
        if (paramArrayOfInetAddress1.length != paramArrayOfInetAddress2.length) {
            return false;
        }
        for (int i = 0; i < paramArrayOfInetAddress1.length; i++) {
            if (!paramArrayOfInetAddress1[i].equals(paramArrayOfInetAddress2[i])) {
                return false;
            }
        }
        return true;
    }

    private static final InetAddress[] getCopyAddresses(InetAddress[] paramArrayOfInetAddress, int paramInt) {
        InetAddress[] arrayOfInetAddress = new InetAddress[paramArrayOfInetAddress.length];
        for (int i = 0; i < paramArrayOfInetAddress.length; i++) {
            arrayOfInetAddress[i] = paramArrayOfInetAddress[((i + paramInt) % paramArrayOfInetAddress.length)];
        }
        return arrayOfInetAddress;
    }

    public boolean isConnectionSocketKeepAlive()
            throws SocketException {
        return this.socket.getKeepAlive();
    }

    public InetAddress getInetAddress() {
        return this.socket.getInetAddress();
    }

    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }

//    public NTAdapter.NetworkAdapterType getNetworkAdapterType() {
//        return NTAdapter.NetworkAdapterType.TCP;
//    }
}
