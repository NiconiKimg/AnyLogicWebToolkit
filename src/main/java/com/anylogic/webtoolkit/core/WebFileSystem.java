package com.anylogic.webtoolkit.core;

import com.anylogic.webtoolkit.logging.WebToolkitLogger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class WebFileSystem {

    private static final WebToolkitLogger LOG = WebToolkitLogger.get("FILES");

    public static CompletableFuture<String> openDialog(String title, String defaultName) {
        CompletableFuture<String> future = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            try {
                // We obtain an invisible dummy window to use the AWT native FileDialog
                Frame parent = new Frame();
                FileDialog fd = new FileDialog(parent, title != null ? title : "Select File", FileDialog.LOAD);
                if (defaultName != null) fd.setFile(defaultName);
                
                fd.setVisible(true);
                
                if (fd.getFile() != null) {
                    future.complete(new File(fd.getDirectory(), fd.getFile()).getAbsolutePath());
                } else {
                    future.complete(null); // Cancelled
                }
                parent.dispose();
            } catch (Exception e) {
                LOG.error("Error opening OpenDialog", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public static CompletableFuture<String> saveDialog(String title, String defaultName) {
        CompletableFuture<String> future = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            try {
                Frame parent = new Frame();
                FileDialog fd = new FileDialog(parent, title != null ? title : "Save File As", FileDialog.SAVE);
                if (defaultName != null) fd.setFile(defaultName);
                
                fd.setVisible(true);
                
                if (fd.getFile() != null) {
                    future.complete(new File(fd.getDirectory(), fd.getFile()).getAbsolutePath());
                } else {
                    future.complete(null); // Cancelled
                }
                parent.dispose();
            } catch (Exception e) {
                LOG.error("Error opening SaveDialog", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public static String readBase64(String absPath) throws Exception {
        Path path = Paths.get(absPath).normalize();
        if (!Files.exists(path)) throw new Exception("File does not exist: " + path.toString());
        
        byte[] bytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    public static String readText(String absPath) throws Exception {
        Path path = Paths.get(absPath).normalize();
        if (!Files.exists(path)) throw new Exception("File does not exist: " + path.toString());
        
        return Files.readString(path);
    }

    public static void writeBase64(String absPath, String base64Content) throws Exception {
        Path path = Paths.get(absPath).normalize();
        byte[] bytes = Base64.getDecoder().decode(base64Content);
        Files.write(path, bytes);
    }
    
    public static void writeText(String absPath, String content) throws Exception {
        Path path = Paths.get(absPath).normalize();
        Files.writeString(path, content);
    }
}
