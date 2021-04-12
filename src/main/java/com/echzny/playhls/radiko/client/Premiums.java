/**
 * Copyright (C) 2018 LODESTAR COMMUNICATIONS LTD. All Rights Reserved.
 */
package com.echzny.playhls.radiko.client;

import com.echzny.playhls.radiko.JsoupSession;
import com.echzny.playhls.radiko.Util;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.json.JSONObject;
import org.jsoup.Connection;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Objects;

/**
 * radiko プレミアムのログインやログアウトなどを行う
 */
@Slf4j
public class Premiums {
  private static final String LOGIN_URL
      = "https://radiko.jp/ap/member/login/login";
  private static final String LOGIN_PAGE_URL
      = "https://radiko.jp/ap/member/login/login_page";
  private static final String LOGIN_CHECK_URL =
      "https://radiko.jp/ap/member/webapi/member/login/check";
  private static final String LOGOUT_URL
      = "http://radiko.jp/ap/member/webapi/member/logout";

  private final JsoupSession session;
  private Boolean isLoggedIn = false;

  /**
   * コンストラクタ
   * @param session JsoupSession のインスタンス
   */
  public Premiums(@NonNull JsoupSession session) {
    this.session = session;
  }

  /**
   * ログイン状態
   * @return true ログイン済み
   */
  public Boolean isLoggedIn() {
    return isLoggedIn;
  }

  /**
   * radiko プレミアムにログイン
   * @param mail プレミアムアカウントのメールアドレス
   * @param pass パスワード
   * @return true ログイン成功, false 失敗
   * @throws IOException サーバーとの通信エラー
   */
  public Boolean login(String mail, String pass)
      throws IOException {
    if (Util.isNullOrEmpty(mail) || Util.isNullOrEmpty(pass)) {
      throw new IllegalArgumentException();
    }

    val res = session.createConnection(LOGIN_URL)
        .referrer(LOGIN_PAGE_URL)
        .data("mail", mail)
        .data("pass", pass)
        .method(Connection.Method.POST)
        .execute();
    session.updateByResponse(res);

    val doc = res.parse();
    val err = doc
        .getElementsByAttributeValueMatching("class", "caution mb\\d+")
        .first();

    if (Objects.nonNull(err)) {
      isLoggedIn = false;
      log.error("login failed, status code: " + res.statusCode() + ". err: "
          + err.toString() + ", mail: " + mail);

      return false;
    } else {
      isLoggedIn = true;

      return true;
    }
  }

  /**
   * ログアウトする
   * @return true 処理成功, false 失敗
   * @throws IOException サーバーとの通信エラー
   */
  public Boolean logout() throws IOException {
    if (!isLoggedIn) {
      return true;
    }

    val res = session.createConnection(LOGOUT_URL)
        .method(Connection.Method.GET)
        .ignoreContentType(true)
        .execute();
    session.updateByResponse(res);

    val json = new JSONObject(res.body());

    if (json.has("status")
        && Integer.parseInt(json.get("status").toString())
        == HttpURLConnection.HTTP_OK) {
      isLoggedIn = false;

      return true;
    } else {
      log.error("logout failed, message: " + json.toString());

      return false;
    }
  }

  /**
   * ログイン状況を返す
   * @return ログイン状況
   * @throws IOException サーバーとの通信エラー、もしくはログインしていない
   */
  public RadikoStatus getStatus() throws IOException {
    val res = session.createConnection(LOGIN_CHECK_URL)
        .method(Connection.Method.GET)
        .ignoreContentType(true)
        .execute();
    session.updateByResponse(res);

    val json = new JSONObject(res.body());

    if (!json.has("status")
        || Integer.parseInt(json.get("status").toString())
        != HttpURLConnection.HTTP_OK) {
      isLoggedIn = false;

      throw new IOException("could not get status, message: "
          + json.toString());
    }

    isLoggedIn = true;

    return RadikoStatus.of(json);
  }

  /**
   * radiko へのログイン状態を管理する
   */
  @Data
  public static class RadikoStatus {
    private String areaFree;    // "1"
    private String paidMember;  // "1
    private String userKey;     // "vowJs24"

    /**
     * jsonを解析しログイン状態インスタンスを作成する
     * @param json 解析対象のjson
     * @return 作成したインスタンス
     */
    public static RadikoStatus of(@NonNull JSONObject json) {
      val status = new RadikoStatus();

      if (json.has("areafree")) {
        status.setAreaFree(json.get("areafree").toString());
      }
      if (json.has("paid_member")) {
        status.setPaidMember(json.get("paid_member").toString());
      }
      if (json.has("user_key")) {
        status.setUserKey(json.get("user_key").toString());
      }

      return status;
    }
  }
}
