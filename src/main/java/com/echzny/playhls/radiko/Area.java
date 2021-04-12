package com.echzny.playhls.radiko;

import lombok.Data;
import lombok.val;

import java.util.Optional;

/**
 * 地域情報
 */
@Data
public class Area {
  private final String id;
  private final String code;
  private final String name;
  private final BroadcasterType radioType;

  /**
   * コンストラクタ
   * @param radioType ラジオの種類
   * @param id        地域 ID (unique)
   * @param code      地域コード
   * @param name      地域名
   */
  public Area(String code, String name, BroadcasterType radioType) {
    this.id = String.format("%s:%s", code, radioType.getCode());
    this.code = code;
    this.name = name;
    this.radioType = radioType;
  }

  /**
   * id から地域コードを取得する
   * @param id
   * @return
   */
  public static Optional<String> getCodeFromId(String id) {
    if (Util.isNullOrEmpty(id)) {
      return Optional.empty();
    }

    val split = id.split(":");
    if (split.length != 2) {
      return Optional.empty();
    }

    return Optional.of(split[0]);
  }

  public static Optional<BroadcasterType> getBroadcasterTypeFromId(String id) {
    if (Util.isNullOrEmpty(id)) {
      return Optional.empty();
    }

    val split = id.split(":");
    if (split.length != 2) {
      return Optional.empty();
    } else {
      return Optional.of(BroadcasterType.valueOf(split[1]));
    }
  }
}
