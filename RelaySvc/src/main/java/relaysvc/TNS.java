/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author eugener
 */
public class TNS {

    // Packet type
    public static final int PACKET_TYPE_CONNECT = 1;
    public static final int PACKET_TYPE_ACCEPT = 2;
    public static final int PACKET_TYPE_ACK = 3;
    public static final int PACKET_TYPE_REFUSE = 4;
    public static final int PACKET_TYPE_DATA = 6;
    public static final int PACKET_TYPE_NULL = 7;
    public static final int PACKET_TYPE_ABORT = 9;
    public static final int PACKET_TYPE_RESEND = 11;
    public static final int PACKET_TYPE_MARKER = 12;
    public static final int PACKET_TYPE_ATTENTION = 13;
    public static final int PACKET_TYPE_CONTROL = 14;

    //TTI Function ID
    public static final int DATA_TYPE_PROTOCOL_NEGOTIATION = 0x01;
    public static final int DATA_TYPE_DATA_REPRESENTATION_EXCHANGE = 0x02;
    public static final int DATA_TYPE_TTI_FUNCTION_CALL = 0x03;
    public static final int DATA_TYPE_SERVER_SENT_OK = 0x08;
    public static final int DATA_TYPE_EXTENDED_TTI_FUNCTION = 0x11;
    public static final int DATA_TYPE_EXT_PROC_WITH_SVC_REG = 0x20;

    public static final int ORA_VER_10R2 = 0x139;
    public static final int ORA_VER_9R2 = 0x138;
    public static final int ORA_VER_9I = 0x137;
    public static final int ORA_VER_8 = 0x136;
    //
    public static final int SQL_EOF = 0x0101;

    //header
    private int packetLength;  // 2 bytes  0-1
    private int packetChekSum;  // 2 bytes 2-3
    private byte packetType;  // 1 byte     4
    private byte headerFlags;  // 1 byte    5
    private int headerCheckSum;  // 2 bytes  6-7

    // data
    private int dataFlags;  // 2 bytes  8-9  - for disconnect is set to 0x0040, otherwise, in most cases would be 0x0000
//  data will continue from previous buffer if 01 01 bytes were not met at the end of the query.    
    private byte functionId;  // 1 bytes   10
    private byte subFunctionId;  // 1 bytes   11
    private byte sequenceNumber;  // 1 bytes   12

    //
    private int packetTail;
    private byte[] buffer;

    //ub2 - unsigned short 
    //sb2 - signed short
    //eb4 - int 
    //ub4 - unsigned int 
    //sb4 - signed int
    byte[] STANDARD_SDD_MAX_DD = {0, 72, 0, 0, 15, 0, 0, 0, 0, 0, 0, 2, 0, 25, -1, -26, 0, 0, 0, 26, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    public TNS(byte[] buffer) {
        try {
            this.buffer = buffer;
            if (buffer.length < 8) {
                throw new IllegalArgumentException("TNS buffer header less than 8 bytes");
            }
            //header
            packetLength = byte2short(Arrays.copyOfRange(buffer, 0, 2));
            if (packetLength > 2) {
                packetChekSum = byte2short(Arrays.copyOfRange(buffer, 2, 4)); // in O12 is used with first two to keep length

                packetType = buffer[4];
                headerFlags = buffer[5];
                headerCheckSum = byte2short(Arrays.copyOfRange(buffer, 6, 8));

                //data
                if (buffer.length > 12) {
                    dataFlags = byte2short(Arrays.copyOfRange(buffer, 8, 10));
                    functionId = buffer[10];
                    subFunctionId = buffer[11];
                    sequenceNumber = buffer[12];
                }
                packetTail = byte2short(Arrays.copyOfRange(buffer, packetLength - 2, packetLength));
            }
        } catch (Exception ex) {
            String s = "";
        }

    }

    private short byte2short(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return bb.getShort();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("Packet length:   ");
        sb.append(packetLength);
        sb.append("\n");
        sb.append("Packet type:     ");
        sb.append(packetType);
        sb.append("\n");
        sb.append("Function ID:     ");
        sb.append(functionId);
        sb.append("\n");
        sb.append("Sub-function ID: ");
        sb.append(subFunctionId);
        sb.append("\n");
        sb.append("Sequence #:      ");
        sb.append(sequenceNumber);
        sb.append("\n");
        sb.append("Tail #:          ");
        sb.append(packetTail);
        sb.append("\n\n");
        return sb.toString();
    }

    public int getPacketLength() {
        return packetLength;
    }

    public int getPacketChekSum() {
        return packetChekSum;
    }

    public byte getPacketType() {
        return packetType;
    }

    public byte getHeaderFlags() {
        return headerFlags;
    }

    public int getHeaderCheckSum() {
        return headerCheckSum;
    }

    public int getDataFlags() {
        return dataFlags;
    }

    public byte getFunctionId() {
        return functionId;
    }

    public byte getSubFunctionId() {
        return subFunctionId;
    }

    public byte getSequenceNumber() {
        return sequenceNumber;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getPacketTail() {
        return packetTail;
    }

    public static List<TNS> getPackets(byte[] bytes) throws IOException {
        List<TNS> ret = new ArrayList<TNS>();
        TNS t = new TNS(bytes);
        if (bytes.length > t.getPacketLength()) {
            ret.add(new TNS(Arrays.copyOfRange(bytes, 0, t.getPacketLength())));
            int start = t.getPacketLength();
            ret.addAll(getPackets(Arrays.copyOfRange(bytes, start, bytes.length))); //;
        } else if (bytes.length == t.getPacketLength()) {
            ret.add(t);
        } else {
            throw new IOException("The size of received data in stream is less that specified in the TNS packet header: " + bytes.length + " vs. " + t.getPacketLength());
        }
        return ret;
    }

}
