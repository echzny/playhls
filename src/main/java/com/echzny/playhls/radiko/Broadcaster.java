package com.echzny.playhls.radiko;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ラジオの再生・録音サービスの基底クラス
 */
@Getter
public abstract class Broadcaster {
  private static final LocalTime fiveOClock = LocalTime.of(5, 0);
  private final BroadcasterType broadcasterType;                // 放送サービス種別

  protected final List<Area> areaList = new ArrayList<>();        // 地域一覧
  protected final List<Channel> channelList = new ArrayList<>();  // チャンネル一覧

  /**
   * コンストラクタ
   * @param broadcasterType
   */
  protected Broadcaster(@NonNull BroadcasterType broadcasterType) {
    this.broadcasterType = broadcasterType;
  }

  /**
   * 地域一覧を更新する
   * @param newList
   */
  public void updateAreaList(@NonNull List<Area> list) {
    if (list.size() > 0) {
      areaList.clear();
      areaList.addAll(list);
    }
  }

  /**
   * チャンネル一覧を更新する
   * @param list
   */
  public void updateChannelList(@NonNull List<Channel> list) {
    if (list.size() > 0) {
      channelList.clear();
      channelList.addAll(list);
    }
  }

  /**
   * 指定された地域に該当するチャンネル一覧を取得する
   * @param areaId
   * @return
   */
  public List<Channel> getChannelListByAreaId(String areaId) {
    return channelList.stream()
        .filter(channel -> channel.getAreaId().equals(areaId))
        .collect(Collectors.toList());
  }

  /**
   * id に該当する地域情報を取得する
   * @param areaId
   * @return
   */
  public Optional<Area> getAreaById(String areaId) {
    return areaList.stream()
        .filter(area -> area.getId().equalsIgnoreCase(areaId))
        .findFirst();
  }

  /**
   * id に該当するチャンネル情報を取得する
   * @param channelId
   * @return
   */
  public Optional<Channel> getChannelById(String channelId) {
    return channelList.stream()
        .filter(channel -> channel.getId().equalsIgnoreCase(channelId))
        .findFirst();
  }

  /**
   * 初期化処理
   */
  public abstract void init() throws IOException, URISyntaxException;

  /**
   * デフォルトの地域情報を取得する
   * @return
   */
  public abstract Optional<Area> getDefaultArea();

  /**
   * 番組表一覧を取得する
   * @param channel
   * @param date
   * @return
   */
  public abstract List<Program> getProgramListOfOneDay(Channel channel,
                                                       LocalDate date)
      throws IOException;

  /**
   * 7日前から6日後までの直近2週間分の番組表を取得
   * @param channel
   * @return
   */
  public abstract List<Program> getProgramListOfTwoWeeks(Channel channel)
      throws IOException;

  /**
   * 視聴可能なチャンネルか確認する
   * @param channel
   * @return
   */
  public abstract Boolean isPlayable(Channel channel);

  /**
   * 再生が可能か確認（放送前やタイムフリー対象外は false）
   * @param program
   * @return
   */
  public abstract Boolean isPlayable(Program program);

  /**
   * 録音が可能か確認（放送開始前や終了後でも予約録音やタイムフリーなどで録音可能な場合は true）
   * @param program
   * @return
   */
  public abstract Boolean isRecordable(Program program);

  /**
   * 地域名を取得する
   * @param program
   * @return
   */
  public String getAreaName(@NonNull Program program) {
    val res = getAreaById(program.getAreaId());

    if (res.isPresent()) {
      return res.get().getName();
    } else {
      return "";
    }
  }
}
