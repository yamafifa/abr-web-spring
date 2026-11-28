package dev.abr.web.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RsdtListQueryTest {

  private static Map<String, String[]> params(String... kv) {
    var m = new java.util.LinkedHashMap<String, String[]>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put(kv[i], new String[] {kv[i + 1]});
    }
    return m;
  }

  @Test
  void URLのクエリを検索条件と件数とページにする() {
    // 前後の空白は取る
    var q =
        RsdtListQuery.fromParams(
            params(
                "city", " 世田谷 ", "town", "上北沢", "chome", "3", "blk", "1", "rsdt", "2", "per", "50",
                "page", "3"));

    assertThat(q.criteria()).isEqualTo(new RsdtSearchCriteria("世田谷", "上北沢", "3", "1", "2"));
    assertThat(q.perPage()).isEqualTo(50);
    assertThat(q.page()).isEqualTo(3);
  }

  static Stream<Map<String, String[]>> badParams() {
    return Stream.of(
        params(),
        params("per", "20", "page", "0"),
        params("per", "abc", "page", "x"),
        params("page", "-1"),
        params("per", "15.5", "page", "1.5"),
        params("page", "99999999999"));
  }

  @ParameterizedTest
  @MethodSource("badParams")
  void おかしな値は既定値にする(Map<String, String[]> p) {
    var q = RsdtListQuery.fromParams(p);

    assertThat(q.perPage()).isEqualTo(15);
    assertThat(q.page()).isEqualTo(1);
  }

  @Test
  void 同じ名前が何回来ても最初の値を使う() {
    var q =
        RsdtListQuery.fromParams(
            Map.of("city", new String[] {"千代田", "港"}, "per", new String[] {"30", "50"}));

    assertThat(q.criteria().city()).isEqualTo("千代田");
    assertThat(q.perPage()).isEqualTo(30);
  }

  @Test
  void 長すぎる条件は上限の文字数で切る() {
    // 文字数はマルチバイトでも 1 文字
    var q = RsdtListQuery.fromParams(params("city", "あ".repeat(100), "blk", "1".repeat(30)));

    assertThat(q.criteria().city()).hasSize(80);
    assertThat(q.criteria().blkNum()).hasSize(20);
  }

  @Test
  void 半角カナは全角カナにする() {
    // 濁点・半濁点は 1 文字にまとめる
    var q = RsdtListQuery.fromParams(params("city", "ｶﾞｰﾃﾞﾝ", "town", "ﾊﾟｰｸ", "chome", "ｳﾞ"));

    assertThat(q.criteria().city()).isEqualTo("ガーデン");
    assertThat(q.criteria().town()).isEqualTo("パーク");
    assertThat(q.criteria().chome()).isEqualTo("ヴ");
  }

  @Test
  void 半角カナのほかは変えない() {
    // 全角英数字・丸数字・互換漢字
    assertThat(RsdtListQuery.fromParams(params("town", "３Ａ①塚ﾊﾟ")).criteria().town())
        .isEqualTo("３Ａ①塚パ");
  }

  @Test
  void 半角カナを全角にしてから長さで切る() {
    assertThat(RsdtListQuery.fromParams(params("chome", "ﾊﾟ".repeat(20))).criteria().chome())
        .isEqualTo("パ".repeat(20));
  }

  @Test
  void URLのクエリに戻すとき空の条件と既定値は省く() {
    assertThat(RsdtListQuery.fromParams(params()).toParams()).isEmpty();
    assertThat(
            RsdtListQuery.fromParams(params("town", "上北沢", "blk", "1", "per", "30", "page", "2"))
                .toParams())
        .containsExactly(
            Map.entry("town", "上北沢"),
            Map.entry("blk", "1"),
            Map.entry("per", "30"),
            Map.entry("page", "2"));
  }

  @Test
  void withPageはページだけ変える() {
    assertThat(RsdtListQuery.fromParams(params("city", "港", "page", "5")).withPage(1).toParams())
        .containsExactly(Map.entry("city", "港"));
  }
}
