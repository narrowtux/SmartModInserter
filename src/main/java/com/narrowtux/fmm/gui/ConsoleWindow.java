package com.narrowtux.fmm.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConsoleWindow extends Stage {
    private Scene scene;
    private TabPane tabs = new TabPane();
    private ObservableList<Process> processes = FXCollections.observableArrayList();

    public ConsoleWindow() {
        super(StageStyle.DECORATED);
        init();
    }

    public void init() {
        setWidth(640);
        setHeight(480);
        scene = new Scene(tabs);
        this.setScene(scene);

        getProcesses().addListener((ListChangeListener<Process>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Process added : change.getAddedSubList()) {
                        ProcessTab tab = new ProcessTab(added);
                        tabs.getTabs().add(tab);
                        tabs.getSelectionModel().select(tab);
                    }
                }
                if (change.wasRemoved()) {
                    // TODO
                }
            }
        });
    }

    public ObservableList<Process> getProcesses() {
        return processes;
    }

    private class ProcessTab extends Tab {
        private final Process process;
        private final VBox vbox;
        private TextArea log = new TextArea();
        private final Thread reader;
        private final HBox hbox;

        public ProcessTab(Process process) {
            this.process = process;

            hbox = new HBox();
            vbox = new VBox(log, hbox);

            Button killButton = new Button("Kill");
            killButton.setOnAction((e) -> {
                process.destroyForcibly();
            });

            hbox.setAlignment(Pos.CENTER_RIGHT);
            hbox.getChildren().add(killButton);

            VBox.setVgrow(log, Priority.ALWAYS);
            this.setContent(vbox);
            log.setWrapText(true);
            log.setFont(Font.font("Courier", 12));

            reader = new Thread(() -> {
                BufferedReader stream = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                try {
                    while ((line = stream.readLine()) != null) {
                        final String tmpLine = line;
                        Platform.runLater(() -> {
                            String old = log.getText();
                            old += tmpLine;
                            old += "\n";
                            log.setText(old);
                            log.setScrollTop(Double.MAX_VALUE);
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
            reader.start();
        }

        public Process getProcess() {
            return process;
        }
    }
}
