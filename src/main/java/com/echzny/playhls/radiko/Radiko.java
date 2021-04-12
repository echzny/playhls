package com.echzny.playhls.radiko;

import com.echzny.playhls.radiko.client.Client;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;

/**
 * radiko の再生・録音を管理するクラス
 */
@Slf4j
public class Radiko extends Broadcaster {
  @Getter private final Client client = new Client();

  // radiko 地域コード（JP*）とリージョンコード（Area.id）の対応表
  private static final Map<String, String> regionCodes = Map.ofEntries(
      Map.entry("JP1", "hokkaido-tohoku"),
      Map.entry("JP2", "hokkaido-tohoku"),
      Map.entry("JP3", "hokkaido-tohoku"),
      Map.entry("JP4", "hokkaido-tohoku"),
      Map.entry("JP5", "hokkaido-tohoku"),
      Map.entry("JP6", "hokkaido-tohoku"),
      Map.entry("JP7", "hokkaido-tohoku"),
      Map.entry("JP8", "kanto"),
      Map.entry("JP9", "kanto"),
      Map.entry("JP10", "kanto"),
      Map.entry("JP11", "kanto"),
      Map.entry("JP12", "kanto"),
      Map.entry("JP13", "kanto"),
      Map.entry("JP14", "kanto"),
      Map.entry("JP15", "hokuriku-koushinetsu"),
      Map.entry("JP16", "hokuriku-koushinetsu"),
      Map.entry("JP17", "hokuriku-koushinetsu"),
      Map.entry("JP18", "hokuriku-koushinetsu"),
      Map.entry("JP19", "hokuriku-koushinetsu"),
      Map.entry("JP20", "hokuriku-koushinetsu"),
      Map.entry("JP21", "chubu"),
      Map.entry("JP22", "chubu"),
      Map.entry("JP23", "chubu"),
      Map.entry("JP24", "chubu"),
      Map.entry("JP25", "kinki"),
      Map.entry("JP26", "kinki"),
      Map.entry("JP27", "kinki"),
      Map.entry("JP28", "kinki"),
      Map.entry("JP29", "kinki"),
      Map.entry("JP30", "kinki"),
      Map.entry("JP31", "chugoku-shikoku"),
      Map.entry("JP32", "chugoku-shikoku"),
      Map.entry("JP33", "chugoku-shikoku"),
      Map.entry("JP34", "chugoku-shikoku"),
      Map.entry("JP35", "chugoku-shikoku"),
      Map.entry("JP36", "chugoku-shikoku"),
      Map.entry("JP37", "chugoku-shikoku"),
      Map.entry("JP38", "chugoku-shikoku"),
      Map.entry("JP39", "chugoku-shikoku"),
      Map.entry("JP40", "kyushu"),
      Map.entry("JP41", "kyushu"),
      Map.entry("JP42", "kyushu"),
      Map.entry("JP43", "kyushu"),
      Map.entry("JP44", "kyushu"),
      Map.entry("JP45", "kyushu"),
      Map.entry("JP46", "kyushu"),
      Map.entry("JP47", "kyushu"));
  public static final List<String> nhkStationIdList = Arrays.asList("JOIK",
      "JOHK", "JOAK", "JOCK", "JOBK", "JOFK", "JOZK", "JOLK", "JOAB", "JOAK-FM");
  public static final List<String> exclusiveIdList = Arrays.asList("JOAB");

  private final List<String> allowedChannelCodes = new ArrayList<>();
  private String currentAreaCode;

  /**
   * コンストラクタ
   * @param activation
   * @param setting
   * @param recordService
   */
  public Radiko() {
    super(BroadcasterType.radiko);
  }

  /**
   * セッションの初期化と radiko プレミアムが有効な場合ログイン処理を行う
   * @throws IOException
   */
  protected void initSession() throws IOException {
    client.getSession().clearCookie();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void init() throws IOException, URISyntaxException {
    log.trace("start init");
    log.trace("finish init");
  }

  /**
   * 地域・チャンネル一覧の初期化と更新
   * @param premium
   * @throws IOException
   */
  protected void update() throws IOException {
    log.trace("start update");

    initSession();

    currentAreaCode = client.getAreas().getCode();
    allowedChannelCodes.clear();

    // 放送可能な地域やチャンネル一覧を取得する（premiumの場合もNHKは該当地域のNHKしか視聴できない）
    val current = client.getStations().getListByAreaCode(currentAreaCode);
    val currentStationList = current.getElementsByTag("stations");

    for (val stations : currentStationList) {
      val stationsList = stations.getElementsByTag("station");

      for (val station : stationsList) {
        allowedChannelCodes.add(Util.getInnerTextByTag(station, "id"));
      }
    }

    // 地域一覧とチャンネル一覧を取得
    val fullList = client.getStations().getList().getElementsByTag("stations");
    val areaList = new ArrayList<Area>();
    val channelList = new ArrayList<Channel>();

    for (val stations : fullList) {
      val area = Parser.area(stations);
      areaList.add(area);

      val stationList = stations.getElementsByTag("station");

      for (val station : stationList) {
        val channel = Parser.channel(station, area);

        if (exclusiveIdList.contains(channel.getCode())) {
          continue;
        }

        channelList.add(channel);
      }
    }

    updateAreaList(areaList);
    updateChannelList(channelList);

    log.trace("finish update");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Area> getDefaultArea() {
    val regionCode = regionCodes.get(currentAreaCode);

    return getAreaList().stream()
        .filter(area -> area.getCode().equals(regionCode))
        .findFirst();
  }

  /**
   * elements を解析して番組情報を取得する
   * @param elements
   * @param channel
   * @return
   */
  protected List<Program> elementsToProgramList(@NonNull Elements elements,
                                                @NonNull Channel channel) {
    val programList = new ArrayList<Program>();

    for (val element : elements) {
      val innerElements = element.getElementsByTag("prog");

      for (val innerElement : innerElements) {
        val newProgram = Parser.program(innerElement, channel);

        if (Objects.nonNull(newProgram)) {
          val name = newProgram.getName();

          // 一部の番組は休止中の時間帯も番組情報が送られてくるのでスキップする
          if (Util.isNullOrEmpty(name)
              || name.equals("番組休止中") || name.equals("放送休止中")) {
            continue;
          }

          programList.add(newProgram);
        }
      }
    }

    return programList;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Program> getProgramListOfOneDay(@NonNull Channel channel,
                                              @NonNull LocalDate date)
      throws IOException {
    initSession();

    val doc = client.getPrograms().getListOfOneDay(channel.getCode(), date);
    val progsList = doc.getElementsByTag("progs");

    return elementsToProgramList(progsList, channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Program> getProgramListOfTwoWeeks(@NonNull Channel channel)
      throws IOException {
    initSession();

    val doc = client.getPrograms().getListOfTwoWeeks(channel.getCode());
    val progsList = doc.getElementsByTag("progs");

    return elementsToProgramList(progsList, channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Boolean isPlayable(Channel channel) {
    return allowedChannelCodes.contains(channel.getCode());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Boolean isPlayable(@NonNull Program program) {
    if (program.isUnAired()) {
      // 放送前はfalse
      return false;
    }

    if (program.isAfterOnAir()) {
      if (nhkStationIdList.contains(program.getChannelCode())) {
        // NHKの番組はタイムフリー非対応なのでfalse
        return false;
      }

      if (program.getOnAirDate().isBefore(LocalDate.now().minusDays(7))) {
        // 1週間以上前の番組はタイムフリー対応外なのでfalse
        return false;
      }
    }

    // 上記以外の場合は、視聴が許可されているチャンネルならtrue
    return isAllowedChannelCode(program.getChannelCode());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Boolean isRecordable(@NonNull Program program) {
    if (program.isAfterOnAir()) {
      if (nhkStationIdList.contains(program.getChannelCode())) {
        // NHKの番組はタイムフリー非対応なので、放送終了後は必ずfalseを返す
        return false;
      }

      if (program.getOnAirDate().isBefore(LocalDate.now().minusDays(7))) {
        // 1週間以上前の番組はタイムフリー対応外なのでfalse
        return false;
      }
   }

    return isAllowedChannelCode(program.getChannelCode());
  }

  /**
   * 指定されたチャンネルが視聴可能か確認する
   * @param channelCode
   * @return
   */
  protected Boolean isAllowedChannelCode(String channelCode) {
    return allowedChannelCodes.contains(channelCode);
  }
}
