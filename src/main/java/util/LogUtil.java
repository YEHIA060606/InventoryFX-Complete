package util;

public class LogUtil {

    public static void log(String msg) {
        System.out.println("[LOG] " + msg);
    }

    public static void error(String msg) {
        System.err.println("[ERROR] " + msg);
    }
}
