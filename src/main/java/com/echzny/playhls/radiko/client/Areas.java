/**
 * Copyright (C) 2018 LODESTAR COMMUNICATIONS LTD. All Rights Reserved.
 */
package com.echzny.playhls.radiko.client;

import com.echzny.playhls.radiko.JsoupSession;
import com.echzny.playhls.radiko.Util;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jsoup.Connection;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * エリアIDを取得する
 */
@Slf4j
public class Areas {
  private static final String AREA_CHECK_URL = "http://radiko.jp/area/";
  private final JsoupSession session;
  private String areaCode = "";

  /**
   * コンストラクタ
   * @param session JsoupSession のインスタンス
   */
  public Areas(JsoupSession session) {
    this.session = session;
  }

  /**
   * 現在のエリアIDを取得する
   * @return エリアID
   * @throws IOException            radikoへの接続に失敗
   * @throws NoSuchElementException radikoから取得した情報にエリアIDが見つからない
   */
  public String getCode() throws IOException, NoSuchElementException {
    if (!Util.isNullOrEmpty(areaCode)) {
      return areaCode;
    }

    val res = session.createConnection(AREA_CHECK_URL)
        .ignoreContentType(true)
        .method(Connection.Method.GET)
        .execute();
    session.updateByResponse(res);

    val js = res.body();
    val p = Pattern.compile("JP[0-9]+");
    val m = p.matcher(js);

    while (m.find()) {
      // document.write('<span class="JP13">TOKYO JAPAN</span>'); -> JP13
      areaCode = m.group().replace("'", "").trim();
    }

    if (Util.isNullOrEmpty(areaCode)) {
      throw new NoSuchElementException("area id not found");
    }

    return areaCode;
  }
}
