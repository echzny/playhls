package com.echzny.playhls;

import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class Controller {
  @FXML protected ToggleButton button;
  @FXML protected ToggleGroup toggleGroup;

  @FXML
  protected void initialize() {
    button.setUserData("main");
    toggleGroup.selectedToggleProperty().addListener(new WeakChangeListener<>(
        (observable, oldValue, newValue) -> {
          if (oldValue != newValue) {
            if (Objects.nonNull(newValue)) {
              button.setText("Pause");
            } else {
              button.setText("Play");
            }
          }
        })
    );
  }
}
