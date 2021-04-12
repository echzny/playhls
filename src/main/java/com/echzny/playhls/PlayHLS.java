package com.echzny.playhls;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import javafx.application.Application;
import javafx.stage.Stage;

import java.net.ProxySelector;

@Slf4j
public class PlayHLS extends Application {
  @Getter private static RadioProxySelector radioProxySelector;
  @Override
  public void start(Stage stage) {
    try {
      // UI の準備
      BorderPane root = FXMLLoader.load(PlayHLS.class.getResource("PlayHLS.fxml"));
      stage.setScene(new Scene(root));
      stage.show();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  public static void main(String[] args) {
    try {
      radioProxySelector = new RadioProxySelector();
      ProxySelector.setDefault(radioProxySelector);
      launch(args);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
