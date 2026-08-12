package com.recursive.app;

import javafx.application.Application;

/**
 * Plain-class entry point for packaged distributions. Launching a class that
 * extends {@code Application} directly makes the JDK launcher require the
 * javafx modules in the runtime image; delegating here lets JavaFX load from
 * the classpath, which is how jpackage runs the shaded jar.
 */
public final class GuiLauncher {

    private GuiLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(RecursiveApplication.class, args);
    }
}
