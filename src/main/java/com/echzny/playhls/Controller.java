package com.echzny.playhls;

import com.echzny.playhls.radiko.Radiko;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Objects;

@Slf4j
public class Controller {
  @FXML
  protected BorderPane borderPane;
  @FXML
  protected ToggleButton button;
  @FXML
  protected ToggleGroup toggleGroup;
  private MediaPlayer player;
  private MediaView view;

  private Radiko radiko;

  @FXML
  protected void initialize() {
    radiko = new Radiko();

    button.setUserData("main");
    toggleGroup.selectedToggleProperty().addListener(new WeakChangeListener<>(
        (observable, oldValue, newValue) -> {
          if (oldValue != newValue) {
            if (Objects.nonNull(view)) {
              borderPane.getChildren().remove(view);
              view.setMediaPlayer(null);
              view = null;
            }
            if (Objects.nonNull(player)) {
              player.stop();
              player = null;
            }

            try {
              if (Objects.nonNull(newValue)) {
                val proxy = PlayHLS.getRadioProxySelector();
                proxy.setRadikoToken(radiko.getClient().getAuths().getToken());
                val media = new Media(Config.HLS_URL);

                player = new MediaPlayer(media);
                view = new MediaView(player);
                borderPane.setCenter(view);
                player.play();
                media.onErrorProperty().addListener((observable1, oldValue1, newValue1) -> {
                  if (Objects.nonNull(newValue) && oldValue != newValue) {
                    log.error(newValue.toString());
                  }
                });
                button.setText("Pause");
              } else {
                button.setText("Play");
              }
            } catch (Exception e) {
              log.error(e.getMessage(), e);
            }
          }
        })
    );
  }
}
