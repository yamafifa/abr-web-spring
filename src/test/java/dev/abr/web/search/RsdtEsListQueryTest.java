package dev.abr.web.search;

import static dev.abr.web.RsdtFixtures.EXISTING_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RsdtEsListQueryTest {

  private static Map<String, String[]> params(String... kv) {
    var m = new LinkedHashMap<String, String[]>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put(kv[i], new String[] {kv[i + 1]});
    }
    return m;
  }

  @Test
  void 検索条件と件数とafterをURLから読みURLに戻せる() {
    var q = RsdtEsListQuery.fromParams(params("town", "上北沢", "per", "30", "after", EXISTING_ID));

    assertThat(q.criteria().town()).isEqualTo("上北沢");
    assertThat(q.perPage()).isEqualTo(30);
    assertThat(q.after()).isEqualTo(EXISTING_ID);
    assertThat(q.before()).isNull();
    assertThat(q.last()).isFalse();
    assertThat(q.toParams())
        .containsExactly(
            Map.entry("town", "上北沢"), Map.entry("per", "30"), Map.entry("after", EXISTING_ID));
  }

  @Test
  void キーの形でないafterとbeforeは無視する() {
    var q = RsdtEsListQuery.fromParams(params("after", "1 OR 1=1", "before", "x"));

    assertThat(q.after()).isNull();
    assertThat(q.before()).isNull();
  }

  @Test
  void firstPageはページの位置だけを外す() {
    var q = RsdtEsListQuery.fromParams(params("chome", "3", "before", EXISTING_ID, "last", "1"));

    assertThat(q.firstPage().toParams()).containsExactly(Map.entry("chome", "3"));
  }
}
