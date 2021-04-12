/**
 * Copyright (C) 2018 LODESTAR COMMUNICATIONS LTD. All Rights Reserved.
 */
package com.echzny.playhls.radiko;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.Map;
import java.util.Objects;

/**
 * Cookie を保持し、Jsoup の connection 作成時に自動的に Cookie を付加する
 */
public class JsoupSession {
  private final static int DEFAULT_TIMEOUT = 10000;
  @Getter
  @Setter
  private Map<String, String> cookies = null;

  /**
   * Jsoup.connection を作成し、cookkie を設定して返す
   * @param url 接続先の url
   * @return 作成したconnectionのインスタンス
   */
  public Connection createConnection(@NonNull String url) {
    Connection con = Jsoup.connect(url).timeout(DEFAULT_TIMEOUT);
    if (!Objects.isNull(cookies)) {
      con.cookies(cookies);
    }

    return con;
  }

  /**
   * レスポンスを元にcookieを保存する
   * @param res 通信のレスポンス
   */
  public void updateByResponse(@NonNull Connection.Response res) {
    Map<String, String> cookies = res.cookies();
    if (Objects.nonNull(cookies) && cookies.size() != 0) {
      this.cookies = cookies;
    }
  }

  /**
   * Cookie を削除する
   */
  public void clearCookie() {
    cookies = null;
  }
}
