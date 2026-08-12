package com.recursive.app.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;

import java.util.function.Function;

/**
 * Small factory for the plain text columns most screens use. Kept in one
 * place so the screens do not each reinvent a value factory.
 */
public final class TableColumns {

    private TableColumns() {
    }

    public static <T> TableColumn<T, String> string(String heading, double width,
                                                    Function<T, String> extractor) {
        TableColumn<T, String> column = new TableColumn<>(heading);
        column.setPrefWidth(width);
        column.setCellValueFactory(data -> new SimpleStringProperty(extractor.apply(data.getValue())));
        return column;
    }
}
