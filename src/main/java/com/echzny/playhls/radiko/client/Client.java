/**
 * Copyright (C) 2018 LODESTAR COMMUNICATIONS LTD. All Rights Reserved.
 */
package com.echzny.playhls.radiko.client;

import com.echzny.playhls.radiko.JsoupSession;
import lombok.Getter;

/**
 * 各情報取得用クライアントを作成して保持する。
 */
public class Client {
  @Getter
  private final JsoupSession session = new JsoupSession();
  @Getter
  private final Areas areas = new Areas(session);
  @Getter
  private final Auths auths = new Auths(session);
  @Getter
  private final Premiums premiums = new Premiums(session);
  @Getter
  private final Stations stations = new Stations(session, premiums, areas);
  @Getter
  private final Programs programs = new Programs(session);
  @Getter
  private final TimeFrees timeFrees = new TimeFrees(session, auths);
}
