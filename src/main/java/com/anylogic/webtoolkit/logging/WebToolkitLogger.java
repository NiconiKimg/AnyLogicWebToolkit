package com.anylogic.webtoolkit.logging;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Minimal console logger used internally by the toolkit.
 *
 * <p>All output is written to {@link System#out} with the format:
 * {@code [HH:mm:ss] WEBTK.<name> [LEVEL] message}.
 *
 * <p>The global minimum level can be changed via {@link #setLevel};
 * messages below that level are silently discarded.
 */
public class WebToolkitLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Log levels in ascending severity order. */
    public enum Level { DEBUG, INFO, WARN, ERROR }

    private static Level globalLevel = Level.INFO;
    private final String name;

    /**
     * @param name the component name that appears in log output (e.g. {@code "BRIDGE"})
     */
    public WebToolkitLogger(String name) { this.name = name; }

    /**
     * Returns a new logger for the given component name.
     *
     * @param name the component name
     * @return a new {@link WebToolkitLogger} instance
     */
    public static WebToolkitLogger get(String name) { return new WebToolkitLogger(name); }

    /**
     * Sets the global minimum log level. Messages below this level are discarded.
     *
     * @param l the minimum level to emit
     */
    public static void setLevel(Level l) { globalLevel = l; }

    public void info(String msg)               { log(Level.INFO,  msg); }
    public void warn(String msg)               { log(Level.WARN,  msg); }
    public void error(String msg)              { log(Level.ERROR, msg); }
    public void error(String msg, Throwable t) { log(Level.ERROR, msg + " — " + t.getMessage()); }
    public void debug(String msg)              { log(Level.DEBUG, msg); }

    private void log(Level level, String msg) {
        if (level.ordinal() < globalLevel.ordinal()) return;
        System.out.printf("[%s] WEBTK.%s [%s] %s%n",
            LocalTime.now().format(FMT), name, level, msg);
    }
}
