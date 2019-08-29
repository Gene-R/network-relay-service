/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 *
 * @author eugener
 */
public class Logger {

    private Queue<String> errorStack = new LinkedBlockingDeque<String>();
    private Configuration cfg = Configuration.getInstance();
    private static Logger instance;
    private final static Object errorStackLock = new Object();

    private Logger() {

    }

    public synchronized static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    private synchronized void addToFile(String str) {
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(cfg.getLogFile(), true)));
            pw.println(str);
            pw.close();
        } catch (IOException ioex) {
            System.err.println("ERROR: " + ioex.getMessage());
        }

    }

    public void info(String str) {
        prinln(Utils.getCurTS() + " INFO " + str);
    }

    public void debug(String str) {
        if (cfg.isDebug()) {
            prinln(Utils.getCurTS() + " DEBUG " + str);
        }
    }

    public Queue<String> getErrorStack() {
        synchronized (errorStackLock) {
            return errorStack;
        }
    }

    public void resetErrorStack() {
        synchronized (errorStackLock) {
            errorStack.clear();
        }
    }

    public void printErrorStack(PrintWriter writer) {
        for (String err : getErrorStack()) {
            writer.println();
            writer.println(err);
        }
    }

    public void error(String str, boolean stderrOut) {
        if (!str.toLowerCase().contains("socketexception: connection reset")) {
            String ts = Utils.getCurTS();
            synchronized (errorStackLock) {
                String s = "ERROR: " + ts + " : " + str;
                //if (!errorStack.contains(s)) {
                errorStack.offer(s);
                //}
            }
            prinln(ts + " ERROR " + str); // write to log file
            if (stderrOut) {
                System.err.println("ERROR: " + str);
            }
        }
    }

    public void error(String str) {
        error(str, false);
    }

    public void prinln(String str) {
        addToFile(str);
    }

}
