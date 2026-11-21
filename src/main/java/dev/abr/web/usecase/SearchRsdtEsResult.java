package dev.abr.web.usecase;

import dev.abr.web.rsdt.Rsdt;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @param from このページの先頭が何件目か（0 件なら 0）
 * @param prevKey 「前へ」のリンクに使うキー。null なら前のページは無い
 * @param nextKey 「次へ」のリンクに使うキー。null なら次のページは無い
 * @param index 検索したインデックスの名前（rsdt_日時）
 * @param error 失敗したときの文
 */
public record SearchRsdtEsResult(
    List<Rsdt> rows,
    long total,
    long from,
    String prevKey,
    String nextKey,
    String index,
    String error) {

  private static final Pattern INDEX_TIME =
      Pattern.compile("_(\\d{4})(\\d\\d)(\\d\\d)-(\\d\\d)(\\d\\d)(\\d\\d)\\z");

  static SearchRsdtEsResult failure(String error) {
    return new SearchRsdtEsResult(List.of(), 0, 0, null, null, null, error);
  }

  /** 最後の行の番号（0 件なら 0） */
  public long to() {
    return from == 0 ? 0 : from + rows.size() - 1;
  }

  /** インデックスを作った日時（名前の rsdt_20261121-215617 から読む） */
  public String builtAt() {
    if (index == null) {
      return null;
    }
    var m = INDEX_TIME.matcher(index);
    return m.find()
        ? m.group(1)
            + "-"
            + m.group(2)
            + "-"
            + m.group(3)
            + " "
            + m.group(4)
            + ":"
            + m.group(5)
            + ":"
            + m.group(6)
        : null;
  }
}
