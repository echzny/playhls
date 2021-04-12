/**
 * Copyright (C) 2018 LODESTAR COMMUNICATIONS LTD. All Rights Reserved.
 */
package com.echzny.playhls.radiko.client;

import com.echzny.playhls.radiko.JsoupSession;
import com.echzny.playhls.radiko.Util;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * radiko の authToken 取得と認可を行う
 */
@Slf4j
public class Auths {
  private static final String AUTH_URL1 = "https://radiko.jp/v2/api/auth1";
  private static final String AUTH_APP = "pc_html5";
  private static final String AUTH_APP_VERSION = "0.0.1";
  private static final String AUTH_USER = "dummy_user";
  private static final String AUTH_DEVICE = "pc";
  private static final String AUTH_URL2 = "https://radiko.jp/v2/api/auth2";
  private static final String COMMON_JS_URL
      = "http://radiko.jp/apps/js/playerCommon.js";

  private final JsoupSession session;
  private String basePartialKey = "";

  /**
   * コンストラクタ
   * @param session JsoupSession のインスタンス
   */
  public Auths(@NonNull JsoupSession session) {
    this.session = session;
  }

  /**
   * authToken を取得する
   * @return authToken
   * @throws IOException サーバーとの通信失敗
   */
  public String getToken() throws IOException {
    val res1 = auth1();

    if (res1.statusCode() != HttpURLConnection.HTTP_OK) {
      throw new HttpStatusException("could not get authClient token",
          res1.statusCode(), res1.url().toString());
    }

    val token = res1.header("X-Radiko-AuthToken");
    val length = Integer.parseInt(res1.header("X-Radiko-KeyLength"));
    val offset = Integer.parseInt(res1.header("X-Radiko-KeyOffset"));
    val partialKey = getPartialKey(length, offset);
    val encoded = Base64.getEncoder().encodeToString(partialKey.getBytes());
    val res2 = auth2(token, encoded); // token を有効化

    if (res2.statusCode() != HttpURLConnection.HTTP_OK) {
      throw new HttpStatusException("could not activate authClient token",
          res2.statusCode(), res2.url().toString());
    }

    return token;
  }

  /**
   * token と partialKey を取得する
   * @return
   * @throws IOException
   */
  protected Response auth1() throws IOException {
    val res = session.createConnection(AUTH_URL1)
        .header("pragma", "no-cache")
        .header("X-Radiko-App", AUTH_APP)
        .header("X-Radiko-App-Version", AUTH_APP_VERSION)
        .header("X-Radiko-User", AUTH_USER)
        .header("X-Radiko-Device", AUTH_DEVICE)
        .method(Method.GET)
        .execute();
    session.updateByResponse(res);

    return res;
  }

  /**
   * token を有効化する
   * @param token
   * @param partialKey
   * @return
   * @throws IOException
   */
  protected Response auth2(String token, String partialKey)
      throws IOException {
    if (Util.isNullOrEmpty(token) || Util.isNullOrEmpty(partialKey)) {
      throw new IllegalArgumentException();
    }

    val res = session.createConnection(AUTH_URL2)
        .header("x-radiko-authtoken", token)
        .header("x-radiko-device", AUTH_DEVICE)
        .header("x-radiko-partialkey", partialKey)
        .header("x-radiko-user", AUTH_USER)
        .method(Method.GET)
        .execute();
    session.updateByResponse(res);

    return res;
  }

  /**
   * radiko で実際に使用されている Javascript のプレイヤーから basePartialKey を取得する
   * @return
   * @throws IOException
   */
  protected String getBasePartialKey() throws IOException {
    if (!Util.isNullOrEmpty(this.basePartialKey)) {
      return this.basePartialKey;
    }

    val res = session.createConnection(COMMON_JS_URL)
        .ignoreContentType(true)
        .method(Method.GET)
        .execute();
    session.updateByResponse(res);

    val js = res.body();
    val p = Pattern.compile("new RadikoJSPlayer.*\\{");
    val m = p.matcher(js);

    while (m.find()) {
      /* 次の一文から key を引っこ抜く
       new RadikoJSPlayer($audio[0], 'pc_html5', 'bcd151073c03b352e1ef2fd66c32209da9ca0afa', {...});
       -> bcd151073c03b352e1ef2fd66c32209da9ca0afa */
      this.basePartialKey = m.group().split(",")[2].replace("'", "").trim();
    }

    if (Util.isNullOrEmpty(this.basePartialKey)) {
      throw new NoSuchElementException("base partial key not found");
    }

    return this.basePartialKey;
  }

  /**
   * basePartialKey から partialKey を取得する
   * @param length
   * @param offset
   * @return
   * @throws IOException
   */
  protected String getPartialKey(@NonNull Integer length,
                                 @NonNull Integer offset) throws IOException {
    val partialKey = getBasePartialKey();

    if (Util.isNullOrEmpty(partialKey)) {
      throw new IllegalArgumentException("partialKey is empty");
    }

    if (length > partialKey.length()
        || (length + offset) > partialKey.length()) {
      throw new IllegalArgumentException(String.format(
          "invalid partial key, key: '%s', length: '%d', offset: '%d'.",
          partialKey, length, offset));
    }

    return partialKey.substring(offset, offset + length);
  }
}
