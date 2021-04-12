/**
 * Copyright (C) 2018 LODESTAR COMMUNICATIONS LTD. All Rights Reserved.
 */
package com.echzny.playhls.radiko;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinNT;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import lombok.NonNull;
import lombok.val;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.jsoup.nodes.Element;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 各クラスで共通して利用するユーティリティ群
 */
public class Util {
  private static final String CRYPT_KEY = "mN25zxS67fDZvVBt";
  private static final String CRYPT_ALGORITHM = "AES";
  private static final String NETRADIO_RECORDER_FOLDER_NAME = "NetRadioRecorder";

  /**
   * DB へ保存するために image を byte array へ変換する
   * @param image
   * @param extension
   * @return 変換後の byte array
   */
  public static byte[] bufferedImageToByteArray(@NonNull BufferedImage image) {
    val output = new ByteArrayOutputStream();

    try {
      ImageIO.write(image, "png", output);

      return output.toByteArray();
    } catch (IOException e) {
      LogManager.getLogger().error(e.getMessage(), e);

      return null;
    }
  }

  public static InputStream bufferedImageToInputStream(@NonNull BufferedImage image) {
    return new ByteArrayInputStream(bufferedImageToByteArray(image));
  }

  public static byte[] imageToByteArray(@NonNull Image image) {
    return bufferedImageToByteArray(SwingFXUtils.fromFXImage(image, null));
  }

  /**
   * 暗号化・復号化に使用する cipher を生成する
   * @param mode
   * @param source
   * @param secretKey
   * @param algorithm
   * @return
   * @throws InvalidKeyException
   * @throws NoSuchAlgorithmException
   * @throws NoSuchPaddingException
   * @throws IllegalBlockSizeException
   * @throws BadPaddingException
   */
  protected static byte[] cipher(int mode,
                                 byte[] source,
                                 String secretKey,
                                 String algorithm)
      throws InvalidKeyException, NoSuchAlgorithmException,
      NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {

    val secretKeyBytes = secretKey.getBytes();
    val secretKeySpec = new SecretKeySpec(secretKeyBytes, algorithm);
    val cipher = Cipher.getInstance(algorithm);

    cipher.init(mode, secretKeySpec);

    return cipher.doFinal(source);
  }

  /**
   * TEMPディレクトリに作成可能な一時ファイルのパスを返す
   * @param prefix e.x. "HOGE → HOGExxxxx.xxx"
   * @param suffix e.x. "FUGA → xxxxxxxxx.FUGA"
   * @return 作成可能な一時ファイルのパス
   * @throws IOException 一時ファイルの作成に失敗
   */
  public static Path createTempFilePath(String prefix, String suffix)
      throws IOException {
    return Files.createTempFile(Paths.get(System.getProperty("java.io.tmpdir")),
        prefix, suffix);
  }

  /**
   * 暗号化された文字列を複合して返す
   * see https://blogs.yahoo.co.jp/dk521123/32780473.html
   * @param encrypted 暗号化された文字列
   * @return 復号化された文字列
   */
  public static String decrypt(String encrypted) {
    try {
      if (Util.isNullOrEmpty(encrypted)) {
        return null;
      }

      val encryptBytes = Base64.decodeBase64(encrypted);
      val originalBytes = cipher(Cipher.DECRYPT_MODE, encryptBytes, CRYPT_KEY,
          CRYPT_ALGORITHM);

      return new String(originalBytes);
    } catch (IllegalArgumentException | InvalidKeyException
        | NoSuchAlgorithmException | NoSuchPaddingException
        | IllegalBlockSizeException | BadPaddingException e) {
      LogManager.getLogger().error(e.getMessage(), e);

      return null;
    }
  }

  /**
   * 文字列を暗号化して返す
   * see https://blogs.yahoo.co.jp/dk521123/32780473.html
   * @param original 元の文字列
   * @return 暗号化された文字列
   */
  public static String encrypt(String original) {
    try {
      if (Util.isNullOrEmpty(original)) {
        return null;
      }

      val originalBytes = original.getBytes();
      val encryptBytes = cipher(Cipher.ENCRYPT_MODE, originalBytes, CRYPT_KEY,
          CRYPT_ALGORITHM);
      val encryptBytesBase64 = Base64.encodeBase64(encryptBytes, false);

      return new String(encryptBytesBase64);
    } catch (IllegalArgumentException | InvalidKeyException
        | NoSuchAlgorithmException | NoSuchPaddingException
        | IllegalBlockSizeException | BadPaddingException e) {
      LogManager.getLogger().error(e.getMessage(), e);

      return null;
    }
  }

  /**
   * 指定されたパス一覧に指定された名前のファイルが存在するか確認し、存在した場合そのパスを返す
   * @param target 対象ファイルの名前
   * @param paths  ファイルを探すパス一覧
   * @return 発見したファイルのパス
   * @throws FileNotFoundException 対象ファイルが見つからない
   */
  protected static Path findFileInPaths(@NonNull String target,
                              @NonNull List<String> paths)
      throws FileNotFoundException {
    String foundPath = "";

    for (String path : paths) {
      val file = new File(path + target);

      if (isExistsFile(file) && file.isFile()) {
        foundPath = path + target;

        break;
      }
    }

    if (foundPath.isEmpty()) {
      throw new FileNotFoundException(target);
    }

    return FileSystems.getDefault().getPath(foundPath).toAbsolutePath();
  }

  /**
   * ファイルが存在するか確認する
   * @param file
   * @return
   */
  public static boolean isExistsFile(File file) {
    return Objects.nonNull(file) && file.exists();
  }

  /**
   * `%APPDATA%` もしくは `~/Library/Application Support` へのパスを返す
   * @return パス
   */
  public static Path getAppDataPath() {
    String appDir = "";

    if (Platform.isWindows()) {
      appDir = System.getenv("AppData");
    } else if (Platform.isMac()) {
      appDir = System.getProperty("user.home") + "/Library/Application Support";
    } else {
      throw new UnsupportedOperationException("Not support this platform");
    }

    return Paths.get(appDir, "hoge");
  }

  /**
   * javapackager で作成した launcher のパスを返す
   * ※ launcher 環境以外では正しい情報を取得できないので注意
   * @return
   */
  public static Optional<Path> getAppLauncherPath() {
    Path launcherPath = null;

    if (Platform.isWindows()) {
      // Windows はランチャーで起動すると user.dir の末尾が lib になる
      String userDir = System.getProperty("user.dir");

      if (userDir.endsWith(File.separator + "lib")) {
        Path path = Paths.get(userDir).getParent();
        launcherPath = Objects.isNull(path) ?
            null : Paths.get(path.toString(), "hoge" + ".exe");
      }
    } else {  // mac
      // jpackager が作成するランチャーが arg0 を環境変数にセットするのでこれを取得する
      String path = System.getProperty("java.launcher.path");

      if (!Util.isNullOrEmpty(path)) {
        int index = path.indexOf(".app");

        if (index != -1) {  // found
          launcherPath = Paths.get(path.substring(0, index) + ".app");
        }
      }
    }

    if (Objects.isNull(launcherPath)) {
      // ランチャーのパスがわからない場合は代わりに本クラスへのパスを返す
      return Optional.of(getOwnPath());
    } else {
      return Optional.of(launcherPath);
    }
  }

  /**
   * デフォルトの録音したファイルの保存先フォルダーのパスを返す
   * @return 保存先フォルダーのパス
   */
  public static Path getDefaultSaveFolderPath() {
    return Paths.get(Util.getDocumentPath().toString(), "hoge");
  }

  /**
   * 現在のユーザーのドキュメントフォルダのパスを返す
   * @return ドキュメントフォルダのパス
   */
  public static Path getDocumentPath() {
    if (Platform.isWindows()) {
      return FileSystemView.getFileSystemView().getDefaultDirectory().toPath();
    } else if (Platform.isMac()) {
      return Paths.get(System.getProperty("user.home"), "Documents");
    } else {
      throw new UnsupportedOperationException(
          "Windows と macOS 以外の環境には対応していません");
    }
  }

  /**
   * ffmpeg 関連プログラムの種類
   */
  public enum FFType { FFMPEG, FFPLAY, FFPROBE }

  /**
   * FFmpeg/FFplay/FFprobe 実行ファイルの存在するパスを調べて返す
   * @param type 必要な実行ファイルの種類
   * @return ファイルのパス
   * @throws FileNotFoundException FFmpeg/FFplay/FFprobe が見つからない
   */
  public static Path getFFPath(@NonNull FFType type)
      throws FileNotFoundException {
    String exeFileName = type.name().toLowerCase();

    if (Platform.isWindows()) {
      exeFileName += ".exe";
    }

    return getFilePath(exeFileName);
  }

  /**
   * カレントディレクトリと親ディレクトリに指定されたファイルが存在するか確認し、フルパスを取得する
   * @param fileName
   * @return
   * @throws FileNotFoundException
   */
  public static Path getFilePath(String fileName) throws FileNotFoundException {
    // チェック対象のパス一覧を生成する
    val sep = File.separator;
    val paths = new ArrayList<String>();
    paths.add("." + sep);   // current dir
    paths.add(".." + sep);  // parent dir

    // for develop environment
    if (Platform.isWindows()) {
      paths.add("../tools/win/");
    } else if (Platform.isMac()) {
      paths.add("../tools/mac/");
    } else {
      throw new UnsupportedOperationException("Not support this platform");
    }

    return findFileInPaths(fileName, paths);
  }

  /**
   * 最初に発見した tag 内のテキストを抽出して返す
   * @param el      解析対象のXML要素
   * @param tagName 対象のタグ
   * @return 取得したテキスト
   */
  public static String getInnerTextByTag(@NonNull Element el,
                                         @NonNull String tagName) {
    val tag = el.getElementsByTag(tagName);

    if (Objects.nonNull(tag) && Objects.nonNull(tag.first())) {
      return tag.first().text();
    } else {
      return "";
    }
  }

  /**
   * ネットラジオレコーダーのデフォルトの保存先へのパスを返す
   * @return パス
   */
  public static Path getNetRadioRecorderPath() {
    return Paths.get(getDocumentPath().toString(),
        NETRADIO_RECORDER_FOLDER_NAME);
  }

  /**
   * 実行中の自分自身（jar/exe/classファイル）を示すフルパスを返す
   * @return 自分自身のフルパス文字列
   */
  public static Path getOwnPath() {
    val target = Util.class;
    String path = target.getResource("/" + target.getName()
        .replaceAll("\\.", "/") + ".class").getPath();

    // if JAR, "file:/hoge/fuga!class.hogege" to "/hoge/fuga"
    if (path.matches("^file:.+")) {
      path = path.substring("file:".length(), path.lastIndexOf("!"));
    }

    // if Windows, "/C:/hoge/fuga" to "C:/hoge/fuga"
    if (Platform.isWindows()) {
      while (path.charAt(0) == '/' && path.length() > 2) {
        path = path.substring(1, path.length());
      }
    }

    path = path.replace("/", File.separator);

    return FileSystems.getDefault().getPath(path);
  }

  /**
   * LocalTime を 05:00〜29:00 に変換し、00:00 から経過した時間（分）を返す
   * @param lt
   * @return 00:00 から経過した時間（分）
   */
  public static int localTimeTo29HMin(@NonNull LocalTime lt) {
    if (lt.isBefore(LocalTime.of(5, 0, 0, 1))) {
      return (24 + lt.getHour()) * 60 + lt.getMinute();
    } else {
      return lt.getHour() * 60 + lt.getMinute();
    }
  }

  /**
   * LocalDateTime を "05:00" 〜 "28:59" の形式の文字列へ変換して返す
   * @param ldt
   * @return 変換した文字列
   */
  public static String localDateTimeTo29HString(@NonNull LocalDateTime ldt) {
    int hour = ldt.getHour();

    if (ldt.toLocalTime().isBefore(LocalTime.of(5, 0, 0))) {
      hour += 24;
    }

    val minute = ldt.getMinute();

    return String.format("%02d:%02d", hour, minute);
  }

  /**
   * 時刻を "午前10時00分" の様な文字列に変換する
   * @return 変換後の文字列
   */
  public static String localTimeToLongString(@NonNull LocalTime time) {
    return time.format(DateTimeFormatter.ofPattern("ahh時mm分", Locale.JAPAN));
  }

  /**
   * 時刻を "23時00分" の様な24時間形式の文字列に変換する
   * @return 変換後の文字列
   */
  public static String localTimeToMiddleString(@NonNull LocalTime time) {
    return time.format(DateTimeFormatter.ofPattern("HH時mm分"));
  }

  /**
   * 時刻を "AM1000" の様な文字列に変換する
   * @return 変換後の文字列
   */
  public static String localTimeToShortString(@NonNull LocalTime time) {
    return time.format(DateTimeFormatter.ofPattern("ahhmm", Locale.ENGLISH));
  }

  /**
   * 時刻を "2300" の様な24時間形式の文字列に変換する
   * @return 変換後の文字列
   */
  public static String localTimeToExtraShortString(@NonNull LocalTime time) {
    return time.format(DateTimeFormatter.ofPattern("HHmm"));
  }

  /**
   * 日付を "2000年12月31日" の様な文字列に変換する
   * @return 変換後の文字列
   */
  public static String localDateToLongString(@NonNull LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
  }

  /**
   * 日付を "2000-12-31" の様な文字列に変換する
   * @return 変換後の文字列
   */
  public static String localDateToMiddleString(@NonNull LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }

  /**
   * 日付を "12月31日" の様な文字列に変換する
   * @return 変換後の文字列
   */
  public static String localDateToShortString(@NonNull LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern("M月d日"));
  }

  /**
   * 開始日時を29h形式での "yyyy/MM/dd" 文字列へ変換して返す
   * ※ 29h形式だと 00:00〜04:59 は前日の日付になる
   * @param ldt
   * @return 変換した文字列
   */
  public static String toOnAirDateString(@NonNull LocalDateTime ldt) {
    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    if (ldt.toLocalTime().isBefore(LocalTime.of(5, 0,0, 0))) {
      return ldt.toLocalDate().minusDays(1).format(formatter);
    } else {
      return ldt.toLocalDate().format(formatter);
    }
  }

  /**
   * ファイル名から拡張子を取り除く
   *
   * @param fileName
   * @return
   */
  public static String removeExtension(String fileName) {
    if (isNullOrEmpty(fileName) || !fileName.contains(".")) {
      return fileName;
    }

    return fileName.substring(0, fileName.lastIndexOf('.'));
  }

  /**
   * 拡張子を取得する
   *
   * @param fileName
   * @return
   */
  public static String getExtension(String fileName) {
    if (isNullOrEmpty(fileName) || !fileName.contains(".")) {
      return "";
    }

    return fileName.substring(fileName.lastIndexOf('.') + 1);
  }

  public static Image trimImageToSquare(@NonNull Image image) {
    Double height = image.getHeight();
    Double width = image.getWidth();
    if (width > height) {
      return new WritableImage(image.getPixelReader(),
          (int) Math.round((width - height) / 2), 0,
          height.intValue(), height.intValue());
    } else {
      return new WritableImage(image.getPixelReader(), 0,
          (int) Math.round((height - width) / 2),
          width.intValue(), width.intValue());
    }
  }

  public static BufferedImage trimBufferedImageToSquare(@NonNull BufferedImage image) {
    val height = image.getHeight();
    val width = image.getWidth();
    if (width > height) {
      return image.getSubimage(Math.round((width - height) / 2), 0, height, height);
    } else {
      return image.getSubimage(0, Math.round((height - width) / 2), width, width);
    }
  }

  public static boolean isNullOrEmpty(String str) {
    return Objects.isNull(str) || str.isEmpty();
  }

  public static String removeProhibitedCharactersOfFileName(String target) {
    String validFileName = target.replace("^\\.+", "").replaceAll("[\\\\/:*?\"<>|\r\n\t]", "");
    if(validFileName.length() == 0) {
      throw new IllegalArgumentException(
          "File Name " + target + " results in a empty fileName!");
    }
    return validFileName;
  }
}
