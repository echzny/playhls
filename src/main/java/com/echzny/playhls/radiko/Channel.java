package com.echzny.playhls.radiko;

import lombok.Data;
import lombok.val;

import java.util.Optional;

/**
 * チャンネル情報
 */
@Data
public class Channel implements GetPrimaryKey {
  private final String id;
  private final String code;
  private final String name;
  private final String areaId;

  /**
   * コンストラクタ
   * @param id       チャンネル ID (unique)
   * @param code     チャンネルコード
   * @param name     チャンネル名
   * @param areaId   地域 ID
   */
  public Channel(String code, String name, String areaId) {
    this.id = String.format("%s:%s", code, areaId);
    this.code = code;
    this.name = name;
    this.areaId = areaId;
  }

  public Optional<String> getAreaCode() {
    return Area.getCodeFromId(areaId);
  }

  public Optional<BroadcasterType> getBroadcasterType() {
    return Area.getBroadcasterTypeFromId(areaId);
  }

  @Override
  public String getPrimaryKey() {
    return id;
  }

  public static Optional<String> getCodeFromId(String id) {
    if (Util.isNullOrEmpty(id)) {
      return Optional.empty();
    }

    val split = id.split(":");
    if (split.length == 3) {
      return Optional.of(split[0]);
    } else {
      return Optional.empty();
    }
  }

  public static Optional<String> getAreaIdFromId(String id) {
    if (Util.isNullOrEmpty(id)) {
      return Optional.empty();
    }

    val split = id.split(":");
    if (split.length == 3) {
      return Optional.of(split[1] + ":" + split[2]);
    } else {
      return Optional.empty();
    }
  }
}
