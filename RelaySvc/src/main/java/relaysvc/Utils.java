/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.IOException;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/**
 *
 * @author eugener
 */
public class Utils {

    public static String getCurTS() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        return f.format(cal.getTime());
    }

    public static String getTS(Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        return f.format(date.getTime());
    }

    public static String getHumanReadableLong(long number) {
        String s = Long.toString(number);
        StringBuilder ret = new StringBuilder(s);
        for (int i = s.length() - 1; i > 0; i -= 3) {
            if (i <= 2) {
                break;
            }
            ret.insert(i - 2, ",");
        }
        return ret.toString();
    }

    public static int getRandomIntInRange(int min, int max) {
        Random rn = new Random();
        int range = max - min + 1;
        return rn.nextInt(range) + min;
    }

    public static int getEphemeralPort() {
        return getEphemeralPort(32768);
    }

    public static int getEphemeralPort(int startPort) {
        if (startPort < 1 || startPort > 65535) {
            throw new IllegalArgumentException("The straring port number is out of range: " + startPort);
        }
        for (int n = startPort; n < 65535; n++) {
            try {
                ServerSocket srvSock = new ServerSocket(n);
                srvSock.close();
                return n;
            } catch (IOException ex) {
                //try next
            }
        }
        throw new IllegalStateException("Could not allocate ephemeral port number");

    }

}
