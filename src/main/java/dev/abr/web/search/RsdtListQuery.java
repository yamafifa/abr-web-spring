package dev.abr.web.search;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 一覧画面の URL が表す状態（検索条件・1 ページの件数・ページ番号）
 *
 * @param page 1 から
 */
public record RsdtListQuery(RsdtSearchCriteria criteria, int perPage, int page) {

  public static final List<Integer> PER_PAGE_OPTIONS = List.of(15, 30, 50, 100);

  public static final int DEFAULT_PER_PAGE = 15;

  /** 入力の長さの上限（テーブルの列の長さに合わせる） */
  private static final int MAX_CITY = 80;

  private static final int MAX_TOWN = 80;

  private static final int MAX_CHOME = 20;

  private static final int MAX_NUM = 20;

  /**
   * URL のクエリを一覧の状態にする。おかしな値は既定値にする。前後の空白は取る。半角カナは全角カナにする
   *
   * <p>URL の名前は、短くしたいので blkNum → blk、rsdtNum → rsdt にしている
   */
  public static RsdtListQuery fromParams(Map<String, String[]> params) {
    var criteria =
        new RsdtSearchCriteria(
            first(params, "city", MAX_CITY),
            first(params, "town", MAX_TOWN),
            first(params, "chome", MAX_CHOME),
            first(params, "blk", MAX_NUM),
            first(params, "rsdt", MAX_NUM));
    Integer per = toInt(first(params, "per", 10));
    Integer page = toInt(first(params, "page", 10));
    return new RsdtListQuery(
        criteria,
        per != null && PER_PAGE_OPTIONS.contains(per) ? per : DEFAULT_PER_PAGE,
        page != null && page > 0 ? page : 1);
  }

  public RsdtListQuery withPage(int newPage) {
    return new RsdtListQuery(criteria, perPage, newPage);
  }

  /** URL のクエリ（空の条件・既定値は省く） */
  public Map<String, String> toParams() {
    Map<String, String> m = new LinkedHashMap<>();
    putIfNotEmpty(m, "city", criteria.city());
    putIfNotEmpty(m, "town", criteria.town());
    putIfNotEmpty(m, "chome", criteria.chome());
    putIfNotEmpty(m, "blk", criteria.blkNum());
    putIfNotEmpty(m, "rsdt", criteria.rsdtNum());
    if (perPage != DEFAULT_PER_PAGE) {
      m.put("per", Integer.toString(perPage));
    }
    if (page > 1) {
      m.put("page", Integer.toString(page));
    }
    return m;
  }

  /** 同じ名前が何回来ても、最初の値を使う。半角カナを全角カナにしてから、文字数（コードポイント）で上限を切る */
  private static String first(Map<String, String[]> params, String name, int max) {
    String[] values = params.get(name);
    if (values == null || values.length == 0 || values[0] == null) {
      return "";
    }
    String s = zenkakuKana(values[0].strip());
    return s.codePointCount(0, s.length()) > max ? s.substring(0, s.offsetByCodePoints(0, max)) : s;
  }

  private static final Pattern HANKAKU_KANA = Pattern.compile("[\\uFF61-\\uFF9F]+");

  /**
   * 半角カナ（U+FF61〜U+FF9F）だけを全角カナにする（「ﾊﾟ」→「パ」）。ほかの文字は変えない
   *
   * <p>MySQL は「ﾊﾟ」を「ﾊ」と「ﾟ」の 2 文字として探すので、Elasticsearch 検索（RsdtFold で 1 文字にする）と件数が合わなくなるため。 NFKC
   * を全体にかけると、互換漢字（例：U+FA10 の「塚」）や「①」まで変わるので、半角カナの並びにだけかける
   *
   * <p>ほかの版（abr-web-rails・abr-web-laravel・abr-web-symfony・abr-web-ts）と abr-batch の es:verify
   * にも同じ処理がある。直すときはすべてを直す
   */
  static String zenkakuKana(String s) {
    return HANKAKU_KANA
        .matcher(s)
        .replaceAll(
            m -> Matcher.quoteReplacement(Normalizer.normalize(m.group(), Normalizer.Form.NFKC)));
  }

  private static Integer toInt(String s) {
    if (!s.matches("-?[0-9]{1,9}")) {
      return null;
    }
    return Integer.valueOf(s);
  }

  private static void putIfNotEmpty(Map<String, String> m, String name, String value) {
    if (!value.isEmpty()) {
      m.put(name, value);
    }
  }
}
