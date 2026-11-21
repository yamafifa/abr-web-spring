package dev.abr.web.web;

import static dev.abr.web.RsdtFixtures.EXISTING_ID;
import static dev.abr.web.RsdtFixtures.MISSING_ID;
import static dev.abr.web.RsdtFixtures.TOTAL_ROWS;
import static dev.abr.web.RsdtFixtures.countRows;
import static dev.abr.web.RsdtFixtures.existing;
import static dev.abr.web.RsdtFixtures.find;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.abr.web.rsdt.Rsdt;
import dev.abr.web.rsdt.RsdtMapper;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 画面を通したテスト（MockMvc でリクエストを送る。JS は動かない）
 *
 * <p>更新・削除は、ブラウザと同じように Origin ヘッダーを付けて送る（SameOriginFilter が見る）。1 件ごとに DB
 * への書き込みを取り消す（@Transactional）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RsdtControllerTest {

  private static final String SAME_ORIGIN = "http://localhost";

  @Autowired MockMvc mvc;
  @Autowired RsdtMapper mapper;

  private String html(MockHttpServletRequestBuilder req) throws Exception {
    return mvc.perform(req).andReturn().getResponse().getContentAsString();
  }

  /** name="..." の入力欄のタグ */
  private static String inputTag(String html, String name) {
    var m = Pattern.compile("<input[^>]*name=\"" + name + "\"[^>]*>").matcher(html);
    assertThat(m.find()).as(name + " の入力欄").isTrue();
    return m.group();
  }

  /** 今の値で埋めたフォームに、一部を変えて PATCH を送る準備 */
  private MockHttpServletRequestBuilder patchForm(
      String id, String back, Rsdt base, Map<String, String> overrides) {
    var req = patch("/rsdt/" + id).queryParam("back", back).header("Origin", SAME_ORIGIN);
    // param() は同じ名前を足していくので、先に 1 つの Map にまとめる（2 つ送ると "a,b" として読まれる）
    var values = new java.util.LinkedHashMap<String, String>();
    if (base != null) {
      Rsdt.ATTRIBUTE_RULES.forEach(
          rule -> values.put(rule.property(), base.formValues().get(rule.column())));
    }
    values.putAll(overrides);
    values.forEach(req::param);
    return req;
  }

  // ---- 一覧

  @Test
  void 一覧は検索結果と件数を出す() throws Exception {
    var html =
        html(get("/").queryParam("town", "上北沢").queryParam("chome", "3").queryParam("blk", "1"));

    assertThat(html).contains("上北沢３丁目 1-1").contains(">21</span> 件");
  }

  @Test
  void 一覧の編集と削除のリンクに今の検索条件とページを付ける() throws Exception {
    var html =
        html(
            get("/")
                .queryParam("town", "上北沢")
                .queryParam("chome", "3")
                .queryParam("blk", "1")
                .queryParam("page", "2"));
    // 戻り先の URL（/?town=%E4%B8%8A...）を、さらに URL のクエリに入れるので、2 回エンコードされる
    // （Thymeleaf は、クエリの中の / と ? はエンコードしない）
    String back =
        "back=/?town%3D%25E4%25B8%258A%25E5%258C%2597%25E6%25B2%25A2%26chome%3D3%26blk%3D1%26page%3D2";

    assertThat(html)
        .contains("href=\"/rsdt/131121_0001003_001_016_/edit?" + back + "\"")
        .contains("action=\"/rsdt/131121_0001003_001_016_?" + back + "\"");
  }

  @Test
  void 一覧は0件のときそう出す() throws Exception {
    assertThat(html(get("/").queryParam("city", "存在しない市"))).contains("該当する住居がありません");
  }

  @Test
  void 入力値はHTMLとしてではなく文字として出す() throws Exception {
    var html = html(get("/").queryParam("city", "<script>x</script>"));

    assertThat(html)
        .doesNotContain("<script>x</script>")
        .contains("&lt;script&gt;x&lt;/script&gt;");
  }

  // ---- 編集画面

  @Test
  void 編集画面は今の値と戻り先を出す() throws Exception {
    var html = html(get("/rsdt/" + EXISTING_ID + "/edit").queryParam("back", "/?page=2"));

    assertThat(inputTag(html, "city"))
        .contains("value=\"千代田区\"")
        .contains("data-original=\"千代田区\"");
    assertThat(inputTag(html, "efctDate"))
        .contains("type=\"date\"")
        .contains("data-column=\"efct_date\"");
    assertThat(html).contains("href=\"/?page=2\"");
  }

  @Test
  void 編集画面は外のサイトへの戻り先を使わない() throws Exception {
    for (String back : new String[] {"//evil.example", "/\\evil.example", "https://evil.example"}) {
      var res =
          mvc.perform(get("/rsdt/" + EXISTING_ID + "/edit").queryParam("back", back))
              .andReturn()
              .getResponse();

      assertThat(res.getStatus()).isEqualTo(200);
      assertThat(res.getContentAsString()).doesNotContain("evil.example");
    }
  }

  @Test
  void 編集画面は無い行と正しくないidなら404() throws Exception {
    var res = mvc.perform(get("/rsdt/" + MISSING_ID + "/edit")).andReturn().getResponse();
    assertThat(res.getStatus()).isEqualTo(404);
    assertThat(res.getContentAsString()).contains("この住居は見つかりません");

    mvc.perform(get("/rsdt/bad/edit")).andExpect(status().isNotFound());
  }

  // ---- 更新

  @Test
  void 更新して戻り先へお知らせ付きでredirectする() throws Exception {
    // 前後の空白は取り、空文字は NULL
    mvc.perform(
            patchForm(
                EXISTING_ID,
                "/?page=2",
                existing(mapper, EXISTING_ID),
                Map.of("remarks", "  テスト  ", "koaza", "", "efctDate", "2026-01-02")))
        .andExpect(redirectedUrl("/?page=2"))
        .andExpect(flash().attribute("notice", "更新しました"));

    var after = existing(mapper, EXISTING_ID);
    assertThat(after.remarks()).isEqualTo("テスト");
    assertThat(after.koaza()).isNull();
    assertThat(after.efctDate()).hasToString("2026-01-02");
  }

  static Stream<Arguments> invalidInputs() {
    return Stream.of(
        Arguments.of("city が空", "city", "  ", "必須です"),
        Arguments.of("city が長すぎる（21 文字）", "city", "あ".repeat(21), "20 文字以内で入力してください"),
        Arguments.of("無い日付", "efctDate", "2026-02-30", "日付の形式が正しくありません（YYYY-MM-DD）"),
        Arguments.of("別の書き方の日付", "abltDate", "2026/01/01", "日付の形式が正しくありません（YYYY-MM-DD）"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidInputs")
  void 入力エラーなら編集画面をエラー付きで出してDBは変えない(String name, String field, String value, String message)
      throws Exception {
    var before = existing(mapper, EXISTING_ID);

    var res =
        mvc.perform(patchForm(EXISTING_ID, "/", before, Map.of(field, value, "remarks", "入力した値")))
            .andReturn()
            .getResponse();

    assertThat(res.getStatus()).isEqualTo(422);
    var html = res.getContentAsString();
    assertThat(html).contains(message).contains("入力内容を確認してください");
    assertThat(inputTag(html, field)).contains("aria-invalid=\"true\"");
    // 入力した値をそのまま出す
    assertThat(inputTag(html, "remarks")).contains("value=\"入力した値\"");
    assertThat(existing(mapper, EXISTING_ID)).isEqualTo(before);
  }

  @Test
  void 削除された行の更新はお知らせを出して戻り先に戻す() throws Exception {
    mvc.perform(patchForm(MISSING_ID, "/?page=3", null, Map.of("city", "千代田区")))
        .andExpect(redirectedUrl("/?page=3"))
        .andExpect(flash().attribute("alert", "この行はすでに削除されています"));
    assertThat(countRows(mapper)).isEqualTo(TOTAL_ROWS);
  }

  @Test
  void お知らせはredirect先の画面に出る() throws Exception {
    var flashMap =
        mvc.perform(
                delete("/rsdt/" + MISSING_ID).queryParam("back", "/").header("Origin", SAME_ORIGIN))
            .andReturn()
            .getFlashMap();

    assertThat(html(get("/").flashAttrs(flashMap)))
        .contains("data-controller=\"flash\"")
        .contains("削除しました");
  }

  @Test
  void 別のサイトから送られた更新は受け付けない() throws Exception {
    var req =
        patch("/rsdt/" + EXISTING_ID)
            .param("city", "千代田区")
            .param("remarks", "なりすまし")
            .header("Origin", "https://evil.example");
    mvc.perform(req).andExpect(status().isForbidden());
    // 送り元が分からないもの（Origin も Referer も無い）も断る
    mvc.perform(patch("/rsdt/" + EXISTING_ID).param("city", "千代田区"))
        .andExpect(status().isForbidden());

    assertThat(existing(mapper, EXISTING_ID).remarks()).isNull();
  }

  // ---- 削除

  @Test
  void 削除して戻り先へお知らせ付きでredirectする() throws Exception {
    mvc.perform(
            delete("/rsdt/" + EXISTING_ID)
                .queryParam("back", "/?page=2")
                .header("Origin", SAME_ORIGIN))
        .andExpect(redirectedUrl("/?page=2"))
        .andExpect(flash().attribute("notice", "削除しました"));
    assertThat(find(mapper, EXISTING_ID)).isNull();
  }

  @Test
  void 削除の外のサイトへの戻り先は一覧に変える() throws Exception {
    mvc.perform(
            delete("/rsdt/" + MISSING_ID)
                .queryParam("back", "//evil.example")
                .header("Origin", SAME_ORIGIN))
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void 別のサイトから送られた削除は受け付けない() throws Exception {
    mvc.perform(delete("/rsdt/" + EXISTING_ID).header("Origin", "https://evil.example"))
        .andExpect(status().isForbidden());
    assertThat(find(mapper, EXISTING_ID)).isNotNull();
  }
}
