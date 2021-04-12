package com.echzny.playhls.radiko;

import lombok.NonNull;
import lombok.val;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 番組や地域、チャンネルなどの情報を生成する
 */
public class Parser {
  /**
   * XML を解析しエリア情報を生成する
   * @param el              解析対象の XML 要素
   * @param enablePremium   radiko プレミアム有効
   * @param currentAreaCode 現在地の地域コード
   * @return 生成したインスタンス
   */
  public static Area area(@NonNull Element el) {
    val code = el.attr("region_id");

    return new Area(code, el.attr("region_name"), BroadcasterType.radiko);
  }

  /**
   * XML を解析しチャンネル情報を生成する
   * @param el
   * @param area
   * @return
   */
  public static Channel channel(@NonNull Element el,
                                @NonNull Area area) {
    val code = Util.getInnerTextByTag(el, "id");
    return new Channel(code, Util.getInnerTextByTag(el, "name"), area.getId());
  }

  /**
   * XMLを解析して番組情報を生成する
   * @param el
   * @param channel
   * @return
   */
  public static Program program(@NonNull Element el,
                                @NonNull Channel channel) {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    val ft = el.attr("ft");
    val start = LocalDateTime.parse(ft, formatter);
    val to = el.attr("to");
    val id = el.attr("id") + "-" + ft + "-" + to;
    val finish = LocalDateTime.parse(to, formatter);
    String subTitle = Util.getInnerTextByTag(el, "desc");

    if (!Util.isNullOrEmpty(subTitle)) {
      subTitle = Jsoup.parse(subTitle).text();  // remove html tag

      if (subTitle.length() > 256) {
        subTitle = subTitle.substring(0, 256 - 3) + "...";
      }
    }

    return new Program(
        id,
        channel.getId(),
        channel.getName(),
        Util.getInnerTextByTag(el, "title"),
        subTitle,
        Util.getInnerTextByTag(el, "pfm"),
        Util.getInnerTextByTag(el, "url"),
        Util.getInnerTextByTag(el, "img"),
        Util.getInnerTextByTag(el, "info"),
        start,
        finish,
        null);
  }
}
