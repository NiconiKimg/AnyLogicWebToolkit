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

/**
 * Static utilities for native file system access from JavaScript.
 *
 * <p>Each method is wired to a built-in bridge command and is not intended
 * for direct use from AnyLogic model code. File dialogs are shown on the
 * AWT event dispatch thread; read and write operations are synchronous.
 */
public class WebFileSystem {

    private static final WebToolkitLogger LOG = WebToolkitLogger.get("FILES");

    /**
     * Opens a native OS file-open dialog and returns the selected path.
     *
     * <p>The future completes with {@code null} if the user cancels.
     * Runs on the Swing EDT; safe to call from any thread.
     *
     * @param title       dialog title, or {@code null} for the default ("Select File")
     * @param defaultName pre-filled filename, or {@code null} for none
     * @return a future that completes with the absolute path, or {@code null} if cancelled
     */
    public static CompletableFuture<String> openDialog(String title, String defaultName) {
        CompletableFuture<String> future = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            try {
                // AWT FileDialog requires a parent Frame; an invisible one is used here
                Frame parent = new Frame();
                FileDialog fd = new FileDialog(parent, title != null ? title : "Select File", FileDialog.LOAD);
                if (defaultName != null) fd.setFile(defaultName);

                fd.setVisible(true);

                future.complete(fd.getFile() != null
                    ? new File(fd.getDirectory(), fd.getFile()).getAbsolutePath()
                    : null);
                parent.dispose();
            } catch (Exception e) {
                LOG.error("Error opening OpenDialog", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Opens a native OS file-save dialog and returns the selected path.
     *
     * <p>The future completes with {@code null} if the user cancels.
     * Runs on the Swing EDT; safe to call from any thread.
     *
     * @param title       dialog title, or {@code null} for the default ("Save File As")
     * @param defaultName pre-filled filename, or {@code null} for none
     * @return a future that completes with the absolute path, or {@code null} if cancelled
     */
    public static CompletableFuture<String> saveDialog(String title, String defaultName) {
        CompletableFuture<String> future = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            try {
                Frame parent = new Frame();
                FileDialog fd = new FileDialog(parent, title != null ? title : "Save File As", FileDialog.SAVE);
                if (defaultName != null) fd.setFile(defaultName);

                fd.setVisible(true);

                future.complete(fd.getFile() != null
                    ? new File(fd.getDirectory(), fd.getFile()).getAbsolutePath()
                    : null);
                parent.dispose();
            } catch (Exception e) {
                LOG.error("Error opening SaveDialog", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Reads a file and returns its contents encoded as a Base64 string.
     *
     * @param absPath absolute path to the file
     * @return the Base64-encoded file contents
     * @throws Exception if the file does not exist or cannot be read
     */
    public static String readBase64(String absPath) throws Exception {
        Path path = Paths.get(absPath).normalize();
        if (!Files.exists(path)) throw new Exception("File does not exist: " + path);
        return Base64.getEncoder().encodeToString(Files.readAllBytes(path));
    }

    /**
     * Reads a file and returns its contents as a UTF-8 string.
     *
     * @param absPath absolute path to the file
     * @return the file contents
     * @throws Exception if the file does not exist or cannot be read
     */
    public static String readText(String absPath) throws Exception {
        Path path = Paths.get(absPath).normalize();
        if (!Files.exists(path)) throw new Exception("File does not exist: " + path);
        return Files.readString(path);
    }

    /**
     * Decodes a Base64 string and writes the result to a file, overwriting it if it exists.
     *
     * @param absPath       absolute path to the target file
     * @param base64Content Base64-encoded bytes to write
     * @throws Exception if the content cannot be decoded or the file cannot be written
     */
    public static void writeBase64(String absPath, String base64Content) throws Exception {
        Path path = Paths.get(absPath).normalize();
        Files.write(path, Base64.getDecoder().decode(base64Content));
    }

    /**
     * Writes a UTF-8 string to a file, overwriting it if it exists.
     *
     * @param absPath absolute path to the target file
     * @param content the text to write
     * @throws Exception if the file cannot be written
     */
    public static void writeText(String absPath, String content) throws Exception {
        Files.writeString(Paths.get(absPath).normalize(), content);
    }
}
