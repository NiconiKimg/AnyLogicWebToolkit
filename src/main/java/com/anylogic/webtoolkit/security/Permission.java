package com.anylogic.webtoolkit.security;

/**
 * Configurable access permissions for a {@link com.anylogic.webtoolkit.ui.WebDialog} instance.
 *
 * @see WebPermissions
 */
public enum Permission {

    /** Read access to simulation state (used by custom model-read commands). */
    MODEL_READ,

    /** Write access to simulation state (reserved for future use). */
    MODEL_WRITE,

    /** Permission to invoke commands that trigger simulation logic. */
    MODEL_COMMAND,

    /** Access to files within the AnyLogic project directory. */
    PROJECT_FILES,

    /** Access to files outside the AnyLogic project directory. */
    EXTERNAL_FILES,

    /** Outbound internet access from the embedded browser. */
    INTERNET,

    /** Clipboard read/write access (reserved for future use). */
    CLIPBOARD,

    /** Access to the Chrome DevTools panel. */
    DEVTOOLS,

    /** Permission to control simulation execution (speed, pause, stop). */
    SIMULATION_CONTROL
}
