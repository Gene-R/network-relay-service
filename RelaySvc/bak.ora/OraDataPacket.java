/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import oracle.net.ns.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

public class OraDataPacket
        extends OraPacket
        implements SQLnetDef {

    static final boolean DEBUG2 = false;
    protected int pktOffset;
    protected int dataFlags;
    protected boolean isBufferFull = false;
    protected boolean isBufferEmpty = false;
    protected int availableBytesToSend = 0;
    protected int availableBytesToRead = 0;
    protected int sessionIdSize = 0;

    public boolean poolEnabled = false;
    protected boolean dataEOF;
    protected long timestampLastIO;
    protected byte[] sessionId;
    protected boolean attemptingReconnect = false;

    public OraDataPacket(InputStream inputStream, OutputStream outputStream, byte[] buffer) {
        super(inputStream, outputStream, buffer);
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        createBuffer(buffer.length);
    }

    protected void receive()
            throws IOException, NetException {
        super.receive();

        this.dataOff = (this.pktOffset = 10);
        this.dataLen = (this.length - this.dataOff - (this.poolEnabled ? 16 : 0));
        this.dataFlags = (this.buffer[8] & 0xFF);
        this.dataFlags <<= 8;
        this.dataFlags |= this.buffer[9] & 0xFF;
        if ((this.type == 6) && ((this.dataFlags & 0x40) != 0)) {
            this.dataEOF = true;
        }
        if ((this.type == 6) && (0 == this.dataLen)) {
            this.type = 7;
        }
        if (this.poolEnabled) {
            this.timestampLastIO = System.currentTimeMillis();
        }
    }

    protected void send()
            throws IOException {
        send(0);
    }

    protected void send(int paramInt)
            throws IOException {
        this.buffer[8] = ((byte) (paramInt / 256));
        this.buffer[9] = ((byte) (paramInt % 256));

        setBufferLength(this.pktOffset);
        synchronized (this.outputStream) {
            if (this.poolEnabled) {
                int j = 20;
                int i;
                do {
                    i = 0;
                    try {
                        this.outputStream.write(this.buffer, 0, this.pktOffset);
                    } catch (SocketException localSocketException) {
                        if (localSocketException.getMessage().startsWith("Connection reset")) {
                            j--;
                            if (j > 0) {
                                if (!this.attemptingReconnect) {
                                    //ns.reconnectIfRequired(false);// NSProtocol reconnect()
                                }
                                i = 1;
                                continue;
                            }
                        }
                        throw localSocketException;
                    }
                } while (i != 0);
            } else {
                this.outputStream.write(this.buffer, 0, this.pktOffset);
            }
        }
        this.pktOffset = 10;
        this.availableBytesToSend = 0;
        this.isBufferFull = false;
        if (this.poolEnabled) {
            this.timestampLastIO = System.currentTimeMillis();
        }
    }

    protected int putDataInBuffer(byte[] paramArrayOfByte, int start, int end)
            throws IOException {
        int i = this.buffer.length - this.sessionIdSize - this.pktOffset <= end ? this.buffer.length - this.sessionIdSize - this.pktOffset : end;
        if (i > 0) {
            System.arraycopy(paramArrayOfByte, start, this.buffer, this.pktOffset, i);
            this.pktOffset += i;

            this.isBufferFull = (this.pktOffset == this.buffer.length - this.sessionIdSize);

            this.availableBytesToSend = (this.dataOff < this.pktOffset ? this.pktOffset - this.dataOff : 0);
        }
        return i;
    }

    public long readLongLSB(int paramInt)
            throws BreakNetException, NetException, IOException {
        long l = 0L;

        int i = paramInt;
        for (int j = 0; i > 0; this.availableBytesToRead -= 1) {
            l |= (this.buffer[this.pktOffset] & 0xFF) << 8 * j;
            i--;
            j++;
            this.pktOffset += 1;
        }
        return l;
    }

    public long readLongMSB(int paramInt)
            throws BreakNetException, NetException, IOException {
        long l = 0L;
        for (int i = paramInt; i > 0; this.availableBytesToRead -= 1) {
            l |= (this.buffer[this.pktOffset] & 0xFF) << 8 * (i - 1);
            i--;
            this.pktOffset += 1;
        }
        return l;
    }

    protected int getDataFromBuffer(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
            throws NetException {
        int i = this.availableBytesToRead <= paramInt2 ? this.availableBytesToRead : paramInt2;
        if (i > 0) {
            System.arraycopy(this.buffer, this.pktOffset, paramArrayOfByte, paramInt1, i);
            this.pktOffset += i;

            this.isBufferEmpty = (this.pktOffset == this.length);

            this.availableBytesToRead -= i;
        }
        return i;
    }

    protected void setBufferLength(int paramInt)
            throws NetException {
        if (this.poolEnabled) {
            System.arraycopy(this.sessionId, 0, this.buffer, this.pktOffset, 16);

            paramInt += 16;
            this.pktOffset += 16;
        }
        if (this.isLargeSDU) {
            this.buffer[3] = ((byte) (paramInt & 0xFF));
            this.buffer[2] = ((byte) (paramInt >> 8 & 0xFF));
            this.buffer[1] = ((byte) (paramInt >> 16 & 0xFF));
            this.buffer[0] = ((byte) (paramInt >> 24 & 0xFF));
        } else {
            this.buffer[0] = ((byte) (paramInt / 256));
            this.buffer[1] = ((byte) (paramInt % 256));
        }
    }

    protected void initialize(int paramInt) {
        this.dataOff = (this.pktOffset = 10);
        this.dataLen = (paramInt - this.dataOff);
        this.dataFlags = 0;
        this.sessionIdSize = (this.poolEnabled ? 16 : 0);
    }

    public static final boolean TRACE = false;
}
