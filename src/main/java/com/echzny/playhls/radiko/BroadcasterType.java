package com.echzny.playhls.radiko;

/**
 * 放送局一覧
 */
public enum BroadcasterType {
  radiko, radiru2;

  /**
   * 放送局コードを取得する
   * @return
   */
  public String getCode() {
    return this.name();
  }

  /**
   * 放送局名を取得する
   * @return
   */
  public String getName() {
    switch (this) {
      case radiko:
        return "radiko.jp";
      case radiru2:
        return "らじる★らじる";
      default:
        throw new IllegalArgumentException("存在しない RadioType です");
    }
  }

  /**
   * 放送局コードの文字列からインスタンスを生成する
   * @param code
   * @return
   */
  public static BroadcasterType ofCode(String code) {
    return BroadcasterType.valueOf(code);
  }
}
