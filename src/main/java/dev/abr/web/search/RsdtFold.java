package dev.abr.web.search;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Elasticsearch で検索するときに、文字をそろえる
 *
 * <p>MySQL のテーブルは utf8mb4_unicode_ci なので、LIKE や = は次の違いを同じ文字として扱う。Elasticsearch の wildcard
 * 型は文字をそのまま比べるので、入れる側（abr-batch）と検索する側（ここ）の両方でそろえる。
 *
 * <ul>
 *   <li>全角と半角（「3」で「３丁目」が見つかる）、大文字と小文字 → NFKC ＋ 小文字
 *   <li>濁点・半濁点（「は」と「パ」）→ NFD に分けて、濁点・半濁点の記号を消す
 *   <li>カタカナとひらがな（「あ」と「ア」）→ ひらがなにする
 *   <li>小さい仮名（「ヶ」と「ケ」）→ 大きい仮名にする
 * </ul>
 *
 * <p>CJK 互換漢字（例：U+FA10 の「塚」）はそのまま残す（MySQL は U+585A の「塚」と別の文字として扱うため）。
 *
 * <p>abr-batch の src/lib/fold.ts、Rails 版・Laravel 版・Symfony 版の RsdtFold、TS 版の fold.ts
 * と同じ処理。直すときはすべてを直す
 */
public final class RsdtFold {

  private RsdtFold() {}

  /** CJK 互換漢字（U+F900〜U+FAFF、U+2F800〜U+2FA1F）以外の並び */
  private static final Pattern NOT_CJK_COMPAT =
      Pattern.compile("[^\\x{F900}-\\x{FAFF}\\x{2F800}-\\x{2FA1F}]+");

  /** 濁点・半濁点（結合文字 U+3099・U+309A） */
  private static final Pattern VOICED_MARKS = Pattern.compile("[\\x{3099}\\x{309A}]");

  private static final String SMALL = "ぁぃぅぇぉっゃゅょゎゕゖ";
  private static final String LARGE = "あいうえおつやゆよわかけ";

  public static String call(String s) {
    Matcher m = NOT_CJK_COMPAT.matcher(s);
    return m.replaceAll(r -> Matcher.quoteReplacement(foldPart(r.group())));
  }

  private static String foldPart(String s) {
    String t = Normalizer.normalize(s, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    t = VOICED_MARKS.matcher(Normalizer.normalize(t, Normalizer.Form.NFD)).replaceAll("");
    var sb = new StringBuilder(t.length());
    t.codePoints()
        .map(c -> c >= 0x30A1 && c <= 0x30F6 ? c - 0x60 : c) // カタカナ → ひらがな
        .map(c -> {
          int i = SMALL.indexOf(c);
          return i >= 0 ? LARGE.charAt(i) : c;
        })
        .forEach(sb::appendCodePoint);
    return sb.toString();
  }
}
