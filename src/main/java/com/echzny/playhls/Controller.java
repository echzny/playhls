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
  //private static final String SAMPLE_HLS_URL =
  //    "https://radio-stream.nhk.jp/hls/live/2023229/nhkradiruakr1/master.m3u8";
  //private static final String SAMPLE_HLS_URL = "ff-16b-2c-44100hz.m4a";
  private static final String SAMPLE_HLS_URL =
      "http://f-radiko.smartstream.ne.jp/TBS/_definst_/simul-stream.stream/playlist.m3u8";
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

                val media = new Media(SAMPLE_HLS_URL);

                player = new MediaPlayer(media);
                view = new MediaView(player);
                borderPane.setCenter(view);
                player.play();
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
