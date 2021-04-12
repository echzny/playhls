package com.echzny.playhls.radiko;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.*;

/**
 * 再生可能な Item の情報を管理する
 */
@EqualsAndHashCode
@ToString
public abstract class PlayableItem {
  @Getter
  private final String id;
  @Getter
  private final PlayableItemType playableItemType;

  // 再生中フラグ
  private final BooleanProperty playing;
  public BooleanProperty playingProperty() {
    return playing;
  }
  public Boolean isPlaying() {
    return playing.get();
  }
  public void setPlaying(@NonNull Boolean value) {
    playing.set(value);
  }

  /**
   * コンストラクタ
   * @param id
   * @param playableItemType
   */
  protected PlayableItem(String id,
                         @NonNull PlayableItemType playableItemType) {
    if (Util.isNullOrEmpty(id)) {
      throw new IllegalArgumentException();
    }

    this.id = id;
    this.playableItemType = playableItemType;

    if (!Util.isNullOrEmpty(id)) {
      this.playing = new SimpleBooleanProperty(true);
    } else {
      this.playing = new SimpleBooleanProperty(false);
    }
  }

  public abstract BroadcasterType getBroadcasterType();
}
