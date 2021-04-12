/**
 * Copyright (C) 2018 LODESTAR COMMUNICATIONS LTD. All Rights Reserved.
 */
package com.echzny.playhls.radiko.client;

import com.echzny.playhls.radiko.JsoupSession;
import com.echzny.playhls.radiko.Radiko;
import com.echzny.playhls.radiko.Util;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;

/**
 * ステーション一覧を取得する。
 */
@Slf4j
public class Stations {
  private static final String STATION_LIST_FULL
      = "http://radiko.jp/v3/station/region/full.xml";
  private static final String STATION_LIST_PER_AREA
      = "http://radiko.jp/v3/station/list/[AREA_ID].xml";
  private static final String STATION_STREAM_URL
      = "http://radiko.jp/v2/station/stream_smh_multi/[STATION_ID].xml";

  private final JsoupSession session;
  private final Premiums premium;
  private final Areas area;

  /**
   * コンストラクタ
   * @param session JsoupSession のインスタンス
   * @param premium PremiumClient のインスタンス
   * @param area AreaClient のインスタンス
   */
  public Stations(@NonNull JsoupSession session, @NonNull Premiums premium,
                  @NonNull Areas area) {
    this.session = session;
    this.premium = premium;
    this.area = area;
  }

  /**
   * ステーション一覧を取得する
   * @return 一覧の収められたXML
   * @throws IOException サーバーの通信エラー
   */
  public Document getList() throws IOException {
    val res = session.createConnection(STATION_LIST_FULL)
        .parser(Parser.xmlParser())
        .execute();
    session.updateByResponse(res);

    return res.parse();
  }

  /**
   * 現在の視聴可能エリアのステーションを取得する
   * @return ステーション一覧の収められたXML
   * @throws IOException サーバーとの通信エラー
   */
  public Document getListByAreaCode(String areaCode) throws IOException {
    if (Util.isNullOrEmpty(areaCode)) {
      throw new IllegalArgumentException();
    }

    val res = session.createConnection(
        STATION_LIST_PER_AREA.replace("[AREA_ID]", areaCode))
        .parser(Parser.xmlParser())
        .execute();
    session.updateByResponse(res);

    return res.parse();
  }

  /**
   * 再生中のm3u8 URIを取得する
   * @param stationId 取得対象のステーションID
   * @return m3u8 URI
   * @throws IOException サーバーとの通信エラー
   * @throws URISyntaxException 不正なm3u8 URIが含まれている
   */
  public URI getStreamUrl(String stationId)
      throws IOException, URISyntaxException {
    if (Util.isNullOrEmpty(stationId)) {
      throw new IllegalArgumentException();
    }

    val url = STATION_STREAM_URL.replace("[STATION_ID]", stationId);
    val res = session.createConnection(url)
        .ignoreContentType(true)
        .parser(Parser.xmlParser())
        .execute();
    session.updateByResponse(res);

    String areaFree = "0";

    if (premium.isLoggedIn() && !Radiko.nhkStationIdList.contains(stationId)) {
      areaFree = "1";
    }
    val doc = res.parse();
    val elements = doc.getElementsByTag("url");

    for (val el : elements) {
      if (!el.attr("areafree").equals(areaFree)) {
        continue;
      } else {
        return new URI(el.getElementsByTag("playlist_create_url")
            .first().text());
      }
    }

    throw new NoSuchElementException("stationId not found");
  }
}
