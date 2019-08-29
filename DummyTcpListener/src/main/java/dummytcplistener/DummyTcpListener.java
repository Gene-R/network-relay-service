/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dummytcplistener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/**
 *
 * @author eugener
 */
class ThreadWorker implements Runnable {

    class Reader implements Runnable {

        ThreadWorker parent;

        public Reader(ThreadWorker parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                while (!sock.isClosed()) {
                    String line = reader.readLine();
                    if (line.equalsIgnoreCase("quit")) {
                        parent.println("Closing: " + sock);
                        System.out.println("conection is closed for " + sock);
                        sock.close();
                        break;
                    }
                }
            } catch (Exception ex) {
                System.err.println("ERROR: " + ex.getMessage());
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException ex) {
                    //
                }
            }
        }

    }

    Socket sock;
    int interval;
    String label;
    BufferedWriter writer;

    public ThreadWorker(Socket sock, int interval, String label) {
        this.sock = sock;
        this.interval = interval;
        this.label = label;
    }

    void println(String str) {
        if (!sock.isClosed()) {
            try {
                writer.append(str);
                writer.newLine();
                writer.flush();
            } catch (IOException ex) {
                System.err.println("ERROR: " + ex.getMessage());
            }
        }
    }

    @Override
    public void run() {

        try {
            writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            new Thread(new Reader(this)).start();
            println("Type 'quit' to close this session");
            while (true) {
                println(label + " : " + new Date().toString());
                Thread.sleep(interval * 1000);
            }
        } catch (Exception ex) {
            System.out.println("ERROR: " + ex.getMessage());
        } finally {
            if (!sock.isClosed()) {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException ex) {
                    //
                }
            }
        }

    }
}

public class DummyTcpListener {

    static final int MAX_PORT_NUM = 65535;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: DummyTcpListener <port> [interval [label]]");
            System.exit(1);
        }
        try {
            int port = Integer.parseInt(args[0]);
            if (port < 1023 || port > MAX_PORT_NUM) {
                throw new IllegalArgumentException("Specified port is out range [1023-" + MAX_PORT_NUM + "]: " + port);
            }
            int interval = 1;
            String label = "Dummy service";
            if (args.length >= 1) {
                interval = Integer.parseInt(args[1]);
            }

            if (args.length >= 2) {
                label = args[2];
            }

            ServerSocket srvSocket = new ServerSocket(port);
            System.out.println("Listening port: " + port);
            while (true) {
                Socket inSock = srvSocket.accept();
                System.out.println("Incomming connection from " + inSock.getInetAddress());
                Thread t = new Thread(new ThreadWorker(inSock, interval, label));
                t.start();
            }
        } catch (Exception ex) {
            System.out.println("ERROR: " + ex.getMessage());
        }

    }

}
