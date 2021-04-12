package com.echzny.playhls.radiko;

import lombok.Getter;
import lombok.ToString;
import lombok.val;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 番組情報
 */
@Getter
@ToString
public class Program extends PlayableItem {
  private final String code;
  private final String channelId;
  private final String channelName;
  private final String name;
  private final String subTitle;
  private final String castMember;
  private final String webSite;
  private final String thumbnailUrl;
  private final String description;
  private final LocalDateTime start;
  private final LocalDateTime finish;
  private final String m3u8;

  /**
   * コンストラクタ
   * @param code
   * @param channelId
   * @param channelName
   * @param name
   * @param subTitle
   * @param castMember
   * @param webSite
   * @param thumbnailUrl
   * @param description
   * @param start
   * @param finish
   * @param m3u8
   */
  public Program(String code, String channelId, String channelName, String name,
                 String subTitle, String castMember, String webSite,
                 String thumbnailUrl, String description, LocalDateTime start,
                 LocalDateTime finish, String m3u8) {
    super(String.format("%s:%s", code, channelId), PlayableItemType.PROGRAM);

    this.code = code;
    this.channelId = channelId;
    this.channelName = channelName;
    this.name = name;
    this.subTitle = subTitle;
    this.castMember = castMember;
    this.webSite = webSite;
    this.thumbnailUrl = thumbnailUrl;
    this.description = description;
    this.start = start;
    this.finish = finish;
    this.m3u8 = m3u8;
  }

  /**
   * 放送サービス種別を取得
   * @return
   */
  public BroadcasterType getBroadcasterType() {
    return BroadcasterType.valueOf(getSplitedChannelId()[2]);
  }

  /**
   * 地域 ID を取得
   * @return
   */
  public String getAreaId() {
    return getSplitedChannelId()[1] + ":" + getSplitedChannelId()[2];
  }

  /**
   * 地域コードを取得
   * @return
   */
  public String getAreaCode() {
    return getSplitedChannelId()[1];
  }

  /**
   * チャンネルコードを取得
   * @return
   */
  public String getChannelCode() {
    return getSplitedChannelId()[0];
  }

  /**
   * チャンネル ID を区切り文字（:）で分割して返す
   * @return
   */
  protected String[] getSplitedChannelId() {
    val channelId = getChannelId();

    if (Util.isNullOrEmpty(channelId)) {
      throw new IllegalArgumentException("channelId is null or empty");
    }

    val splited = channelId.split(":");

    if (splited.length != 3) {
      throw new IllegalArgumentException("invalid channelId: " + channelId);
    }

    return splited;
  }

  /**
   * 29h 形式での放送日を返す
   * @return
   */
  public LocalDate getOnAirDate() {
    val start = getStart();

    if (start.getHour() < 5) {
      return start.toLocalDate().minusDays(1);
    } else {
      return start.toLocalDate();
    }
  }

  /**
   * 29h 形式での放送年月日の文字列を取得
   * @return
   */
  public String getOnAirDateString() {
    return Util.toOnAirDateString(getStart());
  }

  /**
   * 放送前の確認
   * @return
   */
  public Boolean isUnAired() {
    return LocalDateTime.now().isBefore(getStart());
  }

  /**
   * 放送中を確認
   * @return
   */
  public Boolean isOnAir() {
    val now = LocalDateTime.now();

    return now.isAfter(getStart()) && now.isBefore(getFinish());
  }

  /**
   * 放送終了後を確認
   * @return
   */
  public Boolean isAfterOnAir() {
    return LocalDateTime.now().isAfter(getFinish());
  }

  /**
   * 29h 形式での放送開始時間の文字列を取得
   * @return
   */
  public String getStartTimeString() {
    return Util.localDateTimeTo29HString(getStart());
  }

  /**
   * 29h 形式での放送終了時間の文字列を取得
   * @return
   */
  public String getFinishTimeString() {
    return Util.localDateTimeTo29HString(getFinish());
  }
}
