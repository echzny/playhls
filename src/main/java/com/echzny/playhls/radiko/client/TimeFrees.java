/**
 * Copyright (C) 2018 LODESTAR COMMUNICATIONS LTD. All Rights Reserved.
 */
package com.echzny.playhls.radiko.client;

import com.echzny.playhls.radiko.JsoupSession;
import com.echzny.playhls.radiko.Util;
import lombok.NonNull;
import lombok.val;
import org.jsoup.Connection;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * タイムフリー放送の情報を取得する
 */
public class TimeFrees {
  private static final String TIME_FREE_M3U8_URL
      = "https://radiko.jp/v2/api/ts/playlist.m3u8";

  private final JsoupSession session;
  private final Auths auths;

  /**
   * コンストラクタ
   * @param session JsoupSession のインスタンス
   * @param auth AuthClient のインスタンス
   */
  public TimeFrees(@NonNull JsoupSession session, @NonNull Auths auth) {
    this.session = session;
    this.auths = auth;
  }

  /**
   * タイムフリーのm3u8 URIを取得する
   * @param program 対象の番組情報
   * @return m3u8 URI
   * @throws IOException サーバーとの通信エラー
   * @throws URISyntaxException 不正なm3u8 URIが含まれている
   */
  public URI getM3U8(String channelCode,
                     @NonNull LocalDateTime start,
                     @NonNull LocalDateTime finish)
      throws IOException, URISyntaxException {
    if (Util.isNullOrEmpty(channelCode)) {
      throw new IllegalArgumentException();
    }

    val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    val url = TIME_FREE_M3U8_URL;
    val authToken = auths.getToken();
    val ft = start.format(formatter);
    val to = finish.format(formatter);

    val res = session.createConnection(url)
        .ignoreContentType(true)
        .header("X-Radiko-AuthToken", authToken)
        .data("station_id", channelCode)
        .data("ft", ft)
        .data("to", to)
        .method(Connection.Method.POST)
        .execute();
    session.updateByResponse(res);

    val doc = res.parse();
    val el = doc.getElementsByTag("body").first();

    if (Objects.isNull(el)) {
      throw new NoSuchElementException("time free url not found, res.body: "
          + res.body());
    }

    val p = Pattern.compile("#EXT-X-STREAM-INF:.*(https.*\\.m3u8)");
    val m = p.matcher(el.text());

    if (!m.find()) {
      throw new NoSuchElementException(
          "#EXT-X-STREAM-INF section not found, res.body: " + res.body());
    }

    return new URI(m.group(1));
  }
}
