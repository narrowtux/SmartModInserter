package com.narrowtux.fmm.io;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.narrowtux.fmm.io.tasks.ModDownloadTask;
import com.narrowtux.fmm.io.tasks.TaskService;
import com.narrowtux.fmm.model.Datastore;
import com.narrowtux.fmm.model.Version;
import com.narrowtux.fmm.util.OS;
import com.narrowtux.fmm.util.OSXAppleEventHelper;
import com.narrowtux.fmm.util.OpenUriAppleEventHandler;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;

public class URISchemeHandler {
    private OpenUriAppleEventHandler handler = (URI uri) -> {
        Gson gson = new Gson();
        String json = new String(Base64.getDecoder().decode(uri.getRawSchemeSpecificPart().substring(2)));
        System.out.println(json);
        final JsonObject contents = gson.fromJson(json, JsonObject.class);
        JsonObject release = contents.get("releases").getAsJsonArray().get(0).getAsJsonObject();
        String name = contents.get("name").getAsString();
        Version version = Version.valueOf(release.get("version").getAsString());
        JsonArray files = release.get("files").getAsJsonArray();
        JsonElement urlAsElement = JsonNull.INSTANCE;
        int file = 0;
        while (urlAsElement.isJsonNull()) {
            urlAsElement = files.get(0).getAsJsonObject().get("mirror");
            if (urlAsElement.isJsonNull()) {
                urlAsElement = files.get(0).getAsJsonObject().get("url");
            }
            file ++;
            if (files.size() - 1 < file) {
                break;
            }
        }
        if (urlAsElement.isJsonNull()) {
            Alert error = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
            error.setTitle("No files");
            error.setContentText("Can't download mod " + name + "#" + version + " because there are no files.");
            error.show();
            return;
        }
        try {
            URL url = new URL(urlAsElement.getAsString());
            Platform.runLater(() -> {
                try {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "asd", ButtonType.YES, ButtonType.NO);
                    alert.setTitle("Download mod");
                    alert.setContentText("Do you want to download " + contents.get("title").getAsString() + "?");

                    alert.showAndWait().filter(type -> type == ButtonType.YES)
                            .ifPresent(type -> {

                                ModDownloadTask task = new ModDownloadTask(url, name, version);
                                task.valueProperty().addListener((obs, ov, nv) -> {
                                    if (nv != null) {
                                        nv.setUnread(true);
                                        Datastore.getInstance().getMods().add(nv);
                                    }
                                });
                                TaskService.getInstance().submit(task);
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (MalformedURLException e) {
            Alert error = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
            error.setTitle("Mod has invalid URL");
            error.setContentText(e.getMessage() + "\nURL: " + urlAsElement.getAsString());
            error.show();
            e.printStackTrace();
            return;
        }
    };

    public void start(String args[]) {
        if (args.length > 0) {
            try {
                URI uri = new URI(args[0]);
                handler.handleURI(uri);
            } catch (URISyntaxException e) {
            }
        }

        if (OS.isMac()) {
            OSXAppleEventHelper.setOpenURIAppleEventHandler(handler);
        }
    }
}
