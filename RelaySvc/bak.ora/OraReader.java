/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import oracle.net.ns.DataPacket;
import oracle.net.ns.NSProtocol;
import oracle.net.ns.SessionAtts;
import oracle.net.ns.*;

/**
 *
 * @author eugener
 */
public class OraReader extends TcpReader {

    private StringBuilder sqlBuf;
    List<ORATransformer> oraTransformers;

    public OraReader(boolean usedAsInput, Proxy proxy, int bufferSize) {
        super(usedAsInput, proxy, bufferSize);
        List<RelayDestination> dstAddresses = proxy.getDstAddresses();
        if (dstAddresses.size() > 1) {
            oraTransformers = new ArrayList<ORATransformer>();
            for (int i = 1; i < dstAddresses.size(); i++) {
                //oraTransformers.add(new ORATransformer(dstAddresses.get(i).getConfigFile()));
            }
        }
    }

    @Override
    public void init(InputStream in, OutputStream out, Socket sock1, Socket sock2) {

        this.in = in;
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
                incTotalBytes(size);
                if (usedAsInput) {
                    proxy.incTotalRuleIn(size);
                    // Perform ORA transformation for incoming traffic only
                    List<TNS> tps = TNS.getPackets(Arrays.copyOfRange(bytes, 0, size));
                    sqlBuf = new StringBuilder();
                    for (TNS tp : tps) {
                        printBytes(size);
                        printHexDump(tp.getBuffer());
                        boolean sqlBufEmpty = sqlBuf.toString().isEmpty();
                        // filter out packets
                        if (!sqlBufEmpty
                                || (tp.getPacketType() == TNS.PACKET_TYPE_DATA
                                && tp.getFunctionId() == TNS.DATA_TYPE_TTI_FUNCTION_CALL
                                //&& (tp.getSubFunctionId() == T4CTTIfun.OSQL7 || tp.getSubFunctionId() == T4CTTIfun.OALL7 || tp.getSubFunctionId() == T4CTTIfun.OALL8))) {
                                && (tp.getSubFunctionId() == T4CTTIfun.OSQL7))) {

                            if (tp.getPacketTail() != TNS.SQL_EOF || !sqlBufEmpty) {
                                if (sqlBufEmpty) {
                                    sqlBuf.append(getStrFromBytes(tp.getBuffer(), 32, tp.getPacketLength()));
                                } else {
                                    sqlBuf.append(getStrFromBytes(tp.getBuffer(), 10, tp.getPacketLength() - 2));
                                }
                            } else {
                                sqlBuf.append(getStrFromBytes(tp.getBuffer(), 32, tp.getPacketLength() - 2));
                            }
                        }

                    }
                    if (!sqlBuf.toString().isEmpty()) {
                        processTransformations(sqlBuf.toString());
                    }
                } else {
                    proxy.incTotalRuleOut(size);
                    printBytes(size);
                    printHexDump(Arrays.copyOfRange(bytes, 0, size));
                }
                out.write(bytes, 0, size);
            } while (true);
            String s = "";
        } catch (SocketException ex) {
            if (!ex.getMessage().toLowerCase().contains("closed")) {
                logger.error("OraReader().run(): SocketException: " + ex.getMessage());
            }
        } catch (Exception ex) {
            logger.error("OraReader().run(): " + ex.getMessage());
        } finally {
            super.close();
            proxy.purgeSession(this);
        }
    }

    private void processTransformations(String sql) {
        try {
            if (isPrintTrace()) {
                StringBuilder sb = new StringBuilder(Utils.getCurTS());
                sb.append("\t");
                sb.append(sql.trim());
                sb.append("\n");
                getHexDumper().addPayLoad(Type.SQL, null, sb);
//        for (ORATransformer ot : oraTransformers) {
//            ot.send(sql);
//        }
            }
        } catch (IOException ex) {
            logger.error("OraReader().processTransformations(): " + ex.getMessage());
        }
    }

    private String getStrFromBytes(byte[] bytes, int start, int end) throws UnsupportedEncodingException {
        byte[] a = Arrays.copyOfRange(bytes, start, end);
        return new String(a, "UTF-8");
    }

    private void initializeAdapter() {
        try {
            String paramString = "(ADDRESS=(PROTOCOL=TCP)(HOST=10.100.66.102)(PORT=1521))";
            Properties props = new Properties();
            props.put("DISABLE_OOB", "1");
            props.put("USE_ZERO_COPY_IO", "0");
            props.put("T4CConnection.hashCode", "99");

//            TcpNTAdapter adapter = new TcpNTAdapter(paramString, props);
//            adapter.connect();
            NSProtocol nsp = new NSProtocolStream();
            nsp.connect(paramString, props);
            SessionAtts sa = new SessionAtts(nsp, 2048, 2048, false, false);
            InputStream is = sa.getInputStream();
            DataPacket p = new DataPacket(sa);

        } catch (Exception ex) {
            StackTraceElement[] stack = new Throwable().getStackTrace();
            logger.error(ex.getMessage());
        }
    }

    @Override
    public void printHexDump(byte[] bytes) throws IOException {
        if (isPrintHexDump()) {
            //logger.prinln(getHexDump(bytes, new TNS(bytes).toString()));
            getHexDump(bytes, new TNS(bytes).toString());
        }
    }

}
