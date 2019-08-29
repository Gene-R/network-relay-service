/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 *
 * @author eugener
 */
class HexDumperPayload {

    private Type type;
    private byte[] bytes;
    private StringBuilder hexDump;

    public HexDumperPayload(Type type, byte[] bytes, StringBuilder hexDump) {
        this.type = type;
        this.bytes = bytes;
        this.hexDump = hexDump;
    }

    public Type getType() {
        return type;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public StringBuilder getHexDump() {
        return hexDump;
    }

}

enum Type {
    HEX, SQL
}

public class TraceDumper implements Runnable {

    protected final relaysvc.Logger logger = relaysvc.Logger.getInstance();
    private Deque<HexDumperPayload> queue = new LinkedBlockingDeque<HexDumperPayload>();
    private String dumpDir;
    private PrintWriter hexWriter;
    private PrintWriter sqlWriter;
    private String sessionId;
    private boolean interrupted;

    public TraceDumper(String dumpDir) {
        this.dumpDir = dumpDir;
        File f = new File(dumpDir);
        if (f.exists() && f.isFile()) {
            f.delete();
        }
        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }

    public void open(String sessionId) {
        this.sessionId = sessionId;
        new Thread(this).start();
    }

    private PrintWriter getWriter(Type type) {

        try {
            if (type == Type.HEX) {
                if (hexWriter == null) {
                    hexWriter = new PrintWriter(new BufferedWriter(new FileWriter(dumpDir + File.separator + "TRACE-" + sessionId, true)));
                }
                return hexWriter;
            }
            if (sqlWriter == null) {
                sqlWriter = new PrintWriter(new BufferedWriter(new FileWriter(dumpDir + File.separator + "SQL-" + sessionId, true)));
            }
            return sqlWriter;
        } catch (IOException ex) {
            logger.error("TraceDumper.getWriter(): " + ex);
        }
        return null;
    }

    @Override
    public void run() {
        logger.debug("Started trace dumper for ID: " + sessionId);
        HexDumperPayload payload = null;
        while (!interrupted) {
            try {
                while (!queue.isEmpty()) {
                    payload = queue.poll();
                    writeHexDump(payload.getType(), payload.getBytes(), payload.getHexDump());
                }
                Thread.sleep(500);
            } catch (Exception ex) {
                logger.error("TraceDumper.run(): " + ex);
            }
        }
        if (hexWriter != null) {
            hexWriter.close();
        }
        if (sqlWriter != null) {
            sqlWriter.close();
        }
        logger.debug("Closed trace dumper for ID: " + sessionId);
    }

    public void addPayLoad(Type type, byte[] bytes, StringBuilder hexDump) throws IOException {
        queue.offer(new HexDumperPayload(type, bytes, hexDump));
    }

    private void writeHexDump(Type type, byte[] bytes, StringBuilder hexDump) {
        try {
            if (bytes != null) {
                StringBuilder hexLine = new StringBuilder();
                int countColumn = 0;
                for (int i = 0; i < bytes.length; i++) {
                    if (countColumn == 0) {
                        hexLine.append(String.format("%04X  ", i));
                    }
                    hexLine.append(String.format("%02X ", bytes[i]));
                    if (i == bytes.length - 1) {
                        hexDump.append(getHexDumpLine(hexLine, bytes, i, countColumn));
                        break;
                    }
                    if (countColumn == 7) {
                        hexLine.append("  ");
                    }

                    if (countColumn == 15) {
                        hexDump.append(getHexDumpLine(hexLine, bytes, i, countColumn));
                        countColumn = 0;
                        hexLine = new StringBuilder();
                        continue;
                    }
                    countColumn++;
                }
            }
            getWriter(type).print(hexDump.append("\n").toString());
            getWriter(type).flush();
        } catch (IOException ex) {
            logger.error("TraceDumper.writeHexDump(): " + ex);
        }
    }

    public void close() {
        interrupted = true;
    }

    private String getHexDumpLine(
            StringBuilder hex,
            byte[] bytes,
            int curIndex,
            int countColumn) throws UnsupportedEncodingException {
        byte[] buf = new byte[countColumn + 1];
        for (int i = 0; i < countColumn + 1; i++) {
            buf[i] = bytes[(curIndex - countColumn) + i];
        }
        buf = Arrays.copyOfRange(bytes, curIndex - countColumn, curIndex + 1);
        String s = new String(buf, "UTF-8");

        StringBuilder sb = new StringBuilder();
        char[] ca = s.toCharArray();
        for (int i = 0; i < ca.length; i++) {
            char chr = ca[i];
            if (chr < 32) {
                sb.append(".");
            } else {
                sb.append(chr);
            }
            if (i == 7) {
                sb.append(" ");
            }
        }
        // Add padding
        if (countColumn < 15) {
            if (countColumn <= 7) {
                hex.append("  ");
            }
            for (int i = 0; i < 15 - countColumn; i++) {
                hex.append("   ");
            }
        }
        hex.append("  >>>");
        hex.append(sb.toString());
        hex.append("<<<");
        hex.append("\n");
        return hex.toString();
    }
}
