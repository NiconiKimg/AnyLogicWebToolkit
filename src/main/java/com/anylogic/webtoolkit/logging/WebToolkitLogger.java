package com.anylogic.webtoolkit.logging;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Logger sencillo con prefijo WEBTK. */
public class WebToolkitLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public enum Level { DEBUG, INFO, WARN, ERROR }

    private static Level globalLevel = Level.INFO;
    private final String name;

    public WebToolkitLogger(String name) { this.name = name; }

    public static WebToolkitLogger get(String name) { return new WebToolkitLogger(name); }
    public static void setLevel(Level l) { globalLevel = l; }

    public void info(String msg)            { log(Level.INFO,  msg); }
    public void warn(String msg)            { log(Level.WARN,  msg); }
    public void error(String msg)           { log(Level.ERROR, msg); }
    public void error(String msg, Throwable t) { log(Level.ERROR, msg + " — " + t.getMessage()); }
    public void debug(String msg)           { log(Level.DEBUG, msg); }

    private void log(Level level, String msg) {
        if (level.ordinal() < globalLevel.ordinal()) return;
        System.out.printf("[%s] WEBTK.%s [%s] %s%n",
            LocalTime.now().format(FMT), name, level, msg);
    }
}
