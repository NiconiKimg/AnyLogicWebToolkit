package com.anylogic.webtoolkit;

import com.anylogic.webtoolkit.core.WebRuntime;
import com.anylogic.webtoolkit.ui.WebDialog;

/**
 * Manual test to verify that the Chromium window opens and loads the page
 * through the embedded LocalHttpServer without AnyLogic.
 */
public class ManualSmokeTest {
    public static void main(String[] args) {
        System.out.println("Initializing WebRuntime...");
        WebRuntime.getInstance().initialize();

        System.out.println("Opening WebDialog...");
        WebDialog dialog = new WebDialog("examples/AnyLogicModels/BasicExample/index.html");
        dialog.setTitle("WebToolkit Test Window - LocalHttpServer");

        dialog.getBridge().setDefaultCommandHandler((cmd, cmdArgs, cb) -> {
            System.out.println("[Bridge] Received command: " + cmd);
            cb.success("Pong from Java!");
        });

        dialog.open();
        System.out.println("WebDialog opened! You should see the window loaded via LocalHttpServer.");
    }
}
