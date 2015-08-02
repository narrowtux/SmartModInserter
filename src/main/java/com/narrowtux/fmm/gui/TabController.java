package com.narrowtux.fmm.gui;

import javafx.scene.Node;
import javafx.scene.control.Tab;

public abstract class TabController extends Controller {
    public abstract Tab getTab();

    @Override
    public Node getRoot() {
        return getTab().getContent();
    }
}
