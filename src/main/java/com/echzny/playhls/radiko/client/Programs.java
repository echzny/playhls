/**
 * Copyright (C) 2018 LODESTAR COMMUNICATIONS LTD. All Rights Reserved.
 */
package com.echzny.playhls.radiko.client;

import com.echzny.playhls.radiko.JsoupSession;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 番組情報の取得を行う
 */
@Slf4j
public class Programs {
  private static final String PROGRAM_BY_STATION_ID_AND_DATE_URL =
      "http://radiko.jp/v3/program/station/date/[YYYYMMDD]/[STATION_ID].xml";
  private static final String PROGRAM_WEEKLY_URL =
      "http://radiko.jp/v3/program/station/weekly/[STATION_ID].xml";

  private final JsoupSession session;

  /**
   * コンストラクタ
   * @param session JsoupSession のインスタンス
   */
  public Programs(@NonNull JsoupSession session) {
    this.session = session;
  }

  /**
   * 指定チャンネル・日付の番組情報を取得して返す
   * @param channelCode 対象のチャンネルのコード
   * @param date    日付
   * @return 取得した番組情報（XML）
   * @throws IOException サーバーとの通信エラー
   */
  public Document getListOfOneDay(@NonNull String channelCode,
                                  @NonNull LocalDate date) throws IOException {
    val res = session
        .createConnection(PROGRAM_BY_STATION_ID_AND_DATE_URL
            .replace("[YYYYMMDD]", date.format(
                DateTimeFormatter.ofPattern("yyyyMMdd")))
            .replace("[STATION_ID]", channelCode))
        .parser(Parser.xmlParser())
        .execute();
    session.updateByResponse(res);

    return res.parse();
  }

  /**
   * 約２周間分（今日を含めて15日分）の番組情報を取得して返す
   * @param channelCode 取得する対象のチャンネルのコード
   * @return 取得した番組情報（XML）
   * @throws IOException サーバーとの通信エラー
   */
  public Document getListOfTwoWeeks(@NonNull String channelCode)
      throws IOException {
    val res = session.createConnection(PROGRAM_WEEKLY_URL
        .replace("[STATION_ID]", channelCode))
        .parser(Parser.xmlParser())
        .execute();
    session.updateByResponse(res);

    return res.parse();
  }
}
