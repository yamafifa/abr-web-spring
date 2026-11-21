package dev.abr.web.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Elasticsearch 検索の画面（本物の Elasticsearch を使う） */
@SpringBootTest
@AutoConfigureMockMvc
class EsRsdtControllerTest {

  @Autowired MockMvc mvc;

  private String html(String url) throws Exception {
    var res = mvc.perform(get(url)).andReturn().getResponse();
    assertThat(res.getStatus()).isEqualTo(200);
    return res.getContentAsString();
  }

  @Test
  void 件数と1ページ分と次へと最後へのリンクを出す() throws Exception {
    String html = html("/es?town=上北沢&chome=3&blk=1");

    assertThat(html).contains(">21</span> 件", "1–15 件目", "after=131121_0001003_001_015_", "last=1");
    assertThat(Pattern.compile("/rsdt/[0-9_]+/edit").matcher(html).results().count()).isEqualTo(15);
  }

  @Test
  void 編集と削除のリンクに今のURLを戻り先として付ける() throws Exception {
    String html = html("/es?town=上北沢&chome=3&blk=1&after=131121_0001003_001_015_");

    assertThat(html).contains("16–21 件目", "/rsdt/131121_0001003_001_016_/edit?back=");
    assertThat(html).contains("after%3D131121_0001003_001_015_");
  }

  @Test
  void 同じ条件のDB検索へのリンクがありDB検索からも戻れる() throws Exception {
    assertThat(html("/es?town=上北沢")).contains("href=\"/?town=%E4%B8%8A%E5%8C%97%E6%B2%A2\"");
    assertThat(html("/?town=上北沢")).contains("href=\"/es?town=%E4%B8%8A%E5%8C%97%E6%B2%A2\"");
  }
}
