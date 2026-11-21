package dev.abr.web.search;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Elasticsearch 検索の画面の URL が表す状態（検索条件・1 ページの件数・どこから表示するか）
 *
 * <p>ページ番号の代わりに、主キーの文字列（キー）でページを送る（search_after）
 *
 * <ul>
 *   <li>after：このキーの次から 1 ページ分（「次へ」）
 *   <li>before：このキーの手前の 1 ページ分（「前へ」）
 *   <li>last：最後の 1 ページ分（「最後へ」）
 *   <li>どれも無ければ、先頭から 1 ページ分
 * </ul>
 */
public record RsdtEsListQuery(
    RsdtSearchCriteria criteria, int perPage, String after, String before, boolean last) {

  /** キーの形（例：131016_0001001_006_001_）。おかしな値は無視する */
  private static final Pattern KEY_FORMAT =
      Pattern.compile("\\A\\d{6}_\\d{7}_\\d{3}_\\d{3}_(\\d{5})?\\z");

  public static RsdtEsListQuery fromParams(Map<String, String[]> params) {
    var base = RsdtListQuery.fromParams(params); // 検索条件と件数の読み方は DB 検索と同じ
    return new RsdtEsListQuery(
        base.criteria(),
        base.perPage(),
        key(params, "after"),
        key(params, "before"),
        "1".equals(first(params, "last")));
  }

  /** ページの位置を外したもの（検索条件と件数だけ） */
  public RsdtEsListQuery firstPage() {
    return new RsdtEsListQuery(criteria, perPage, null, null, false);
  }

  public RsdtEsListQuery withAfter(String key) {
    return new RsdtEsListQuery(criteria, perPage, key, null, false);
  }

  public RsdtEsListQuery withBefore(String key) {
    return new RsdtEsListQuery(criteria, perPage, null, key, false);
  }

  public RsdtEsListQuery withLast() {
    return new RsdtEsListQuery(criteria, perPage, null, null, true);
  }

  /** 同じ条件・件数の DB 検索（1 ページ目） */
  public RsdtListQuery toDbQuery() {
    return new RsdtListQuery(criteria, perPage, 1);
  }

  /** URL のクエリ（空の条件・既定値は省く） */
  public Map<String, String> toParams() {
    var m = toDbQuery().toParams();
    if (after != null) {
      m.put("after", after);
    }
    if (before != null) {
      m.put("before", before);
    }
    if (last) {
      m.put("last", "1");
    }
    return m;
  }

  private static String first(Map<String, String[]> params, String name) {
    String[] v = params.get(name);
    return v == null || v.length == 0 ? null : v[0];
  }

  private static String key(Map<String, String[]> params, String name) {
    String v = first(params, name);
    return v != null && KEY_FORMAT.matcher(v).matches() ? v : null;
  }
}
