package com.narrowtux.fmm.io.tasks;

import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.io.ModpackDetectorVisitor;
import com.narrowtux.fmm.util.Util;
import com.narrowtux.fmm.model.Mod;
import com.narrowtux.fmm.model.Version;
import javafx.concurrent.Task;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class ModDownloadTask extends Task<Mod> implements Pauseable {
    public static final int DOWNLOAD_BUFFER_SIZE = 4096;
    public static final int CONNECT_TIMEOUT = 5000; // 5 seconds
    public static final int READ_TIMEOUT = 10 * 60 * 1000; // 10 minutes
    private final URL url;
    private final String expectedName;
    private final Version expectedVersion;
    private static Random random = new Random();
    private Path downloadingFile;
    private long lastUpdate = 0;
    private long started, ended;

    public ModDownloadTask(URL url, String expectedName, Version expectedVersion) {
        this.url = url;
        this.expectedName = expectedName;
        this.expectedVersion = expectedVersion;
    }

    @Override
    protected Mod call() throws Exception {
        try {
            updateMessage("Preparing");
            updateProgress(-1, 100);
            updateMessage("Connecting to server");
            if (expectedName != null) {
                updateTitle("Download " + expectedName + (expectedVersion != null ? "#" + expectedVersion.toString() : ""));
            } else {
                updateTitle("Download: " + url.toString());
            }
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.connect();
            InputStream stream = conn.getInputStream();
            long contentLength = conn.getContentLengthLong();
            updateProgress(0, contentLength);
            downloadingFile = Datastore.getInstance().getFMMDir().resolve("downloads").resolve("Download-tmp-" + random.nextLong() + ".part");
            Files.createDirectories(downloadingFile.getParent());
            FileOutputStream writer = new FileOutputStream(downloadingFile.toFile());
            byte buf[] = new byte[DOWNLOAD_BUFFER_SIZE];
            int read = 0;
            long totalRead = 0;
            updateMessage("Downloading");
            started = System.currentTimeMillis();
            int currentReadingSpeed = 32;
            do {
                if (isCancelled()) {
                    updateMessage("Cancelled");
                    failed();
                    return null;
                }
                read = stream.read(buf, 0, currentReadingSpeed);

                if (currentReadingSpeed < DOWNLOAD_BUFFER_SIZE) {
                    currentReadingSpeed *= 2;
                }
                if (read > 0) {
                    totalRead += read;
                    writer.write(buf, 0, read);
                    if (System.currentTimeMillis() - lastUpdate > 200) {
                        updateMessage("Downloading: " + Util.formatBytes(totalRead) + " / " + Util.formatBytes(contentLength));
                        updateProgress(totalRead, contentLength);
                        lastUpdate = System.currentTimeMillis();
                    }
                }
            } while (read > 0);
            ended = System.currentTimeMillis();
            updateMessage("Took " + (ended - started) + " ms to download");
            updateProgress(totalRead, contentLength);
            updateMessage("Installing");
            Mod mod = ModpackDetectorVisitor.parseMod(downloadingFile);
            if (mod == null) {
                updateMessage("File was not a mod");
                failed();
            } else {
                if (expectedName != null) {
                    if (!expectedName.equals(mod.getName())) {
                        updateMessage("This file does not contain the expected mod");
                        failed();
                    }
                }
                if (expectedVersion != null) {
                    if (!expectedVersion.equals(mod.getVersion())) {
                        updateMessage("This file does not contain the expected version of the mod");
                        failed();
                    }
                }
                mod.setPath(Datastore.getInstance().getFMMDir().resolve("mods").resolve(mod.getName() + "_" + mod.getVersion().toString() + ".zip"));
                updateMessage("Done");
                succeeded();
                return mod;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void failed() {
        super.failed();

        // delete part file
        try {
            Files.deleteIfExists(downloadingFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void updateMessage(String s) {
        super.updateMessage(s);
        System.out.println("Download task" + " - " + s);
    }
}
