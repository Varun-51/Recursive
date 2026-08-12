package com.recursive.app.ui;

import javafx.scene.Node;

/**
 * One user-facing screen of the application. Screens are built lazily by
 * the {@link MainWindow} navigation on first selection.
 */
public interface Screen {

    /**
     * Builds the screen's root node. Called once per screen; the returned
     * node is cached and re-shown on later selections.
     */
    Node build();

    /** Heading shown in the window header while this screen is active. */
    String title();
}
