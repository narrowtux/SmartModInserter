package com.narrowtux.fmm.gui;

import com.narrowtux.fmm.model.Mod;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class DependencyResolveDialog extends Dialog<Set<Mod>> implements Initializable {
    public static final Set<Mod> CANCELLED = Collections.emptySet();

    public DependencyResolveDialog(List<Set<Mod>> availableSolutions) throws IOException {
        this.availableSolutions = availableSolutions;

        load();
    }

    private void load() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(clazz -> this);
        loader.setLocation(getClass().getResource("/dependencydialog.fxml"));
        loader.load();
    }

    private List<Set<Mod>> availableSolutions;
    private BooleanProperty remember = new SimpleBooleanProperty(false);

    @FXML
    private TreeTableView solutions;
    @FXML
    private TreeTableColumn modColumn;
    @FXML
    private TreeTableColumn versionColumn;
    @FXML
    private CheckBox rememberCheck;
    @FXML
    private VBox root;
    @FXML
    private Button useButton;

    private TreeItem rootItem = new TreeItem(null);

    public boolean getRemember() {
        return remember.get();
    }

    public BooleanProperty rememberProperty() {
        return remember;
    }

    public void setRemember(boolean remember) {
        this.remember.set(remember);
    }


    public void onUse(ActionEvent actionEvent) {
        if (updateResult()) {
            close();
        }
    }

    private boolean updateResult() {
        TreeItem item = solutions.getSelectionModel().getModelItem(solutions.getSelectionModel().getSelectedIndex());
        if (!(item instanceof ModSetTreeItem)) {
            item = item.getParent();
        }
        if (item instanceof ModSetTreeItem) {
            setResult(((ModSetTreeItem) item).getSolution());
            return true;
        }
        return false;
    }

    public void onCancel(ActionEvent actionEvent) {
        try {
            setResult(CANCELLED);
            close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ModSetTreeItem extends TreeItem {
        public ModSetTreeItem(Object o, Set<Mod> solution) {
            super(o);
            this.solution = solution;
        }

        Set<Mod> solution;

        public Set<Mod> getSolution() {
            return solution;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        getDialogPane().setContent(root);
        solutions.setRoot(rootItem);

        useButton.disableProperty().bind(solutions.getSelectionModel().selectedItemProperty().isNull());

        remember.bind(rememberCheck.selectedProperty());
        int i = 1;
        for (Set<Mod> solution : availableSolutions) {
            ModSetTreeItem solutionItem = new ModSetTreeItem("Solution " + i, solution);
            rootItem.getChildren().add(solutionItem);
            for (Mod mod : solution) {
                TreeItem modItem = new TreeItem(mod);
                solutionItem.getChildren().add(modItem);
            }
            solutionItem.setExpanded(true);
            i++;
        }
        rootItem.setExpanded(true);

        modColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures, ObservableValue>() {
            @Override
            public ObservableValue call(TreeTableColumn.CellDataFeatures features) {
                Object value = features.getValue().getValue();
                if (value instanceof String) {
                    return new SimpleStringProperty((String) value);
                }
                if (value instanceof Mod) {
                    return ((Mod) value).nameProperty();
                }
                return null;
            }
        });
        versionColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures, ObservableValue>() {
            @Override
            public ObservableValue call(TreeTableColumn.CellDataFeatures features) {
                Object value = features.getValue().getValue();
                if (value instanceof Mod) {
                    return ((Mod) value).versionProperty().asString();
                }
                return null;
            }
        });

    }
}
