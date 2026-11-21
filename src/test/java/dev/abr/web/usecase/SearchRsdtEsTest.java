package dev.abr.web.usecase;

import static dev.abr.web.RsdtFixtures.existing;
import static org.assertj.core.api.Assertions.assertThat;

import dev.abr.web.es.EsClient;
import dev.abr.web.rsdt.Rsdt;
import dev.abr.web.rsdt.RsdtMapper;
import dev.abr.web.search.RsdtSearchCriteria;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

/** 本物の Elasticsearch（abr-batch の npm run es:index で作ったインデックス）を使う */
@SpringBootTest
class SearchRsdtEsTest {

  /** 世田谷区 上北沢３丁目 の blk_num=1（21 件） */
  private static final RsdtSearchCriteria KAMIKITAZAWA_3_1 =
      new RsdtSearchCriteria("", "上北沢", "3", "1", "");

  @Autowired SearchRsdtEs search;
  @Autowired SearchRsdt searchDb;
  @Autowired RsdtMapper mapper;
  @Autowired ObjectMapper json;

  private SearchRsdtEsResult es(RsdtSearchCriteria c, int perPage) {
    return search.call(c, perPage, null, null, false);
  }

  private static List<String> ids(List<Rsdt> rows) {
    return rows.stream().map(Rsdt::routeId).toList();
  }

  @Test
  void DB検索と同じ件数と同じ順で返す() {
    var r = es(KAMIKITAZAWA_3_1, 100);

    assertThat(r.total()).isEqualTo(21);
    assertThat(ids(r.rows())).isEqualTo(ids(searchDb.call(KAMIKITAZAWA_3_1, 1, 100).rows()));
  }

  @Test
  void 画面に出す値はDBと同じ() {
    var row = es(KAMIKITAZAWA_3_1, 1).rows().getFirst();

    assertThat(row).isEqualTo(existing(mapper, row.routeId()));
  }

  @Test
  void 次へと前へで重ならずに行き来できる() {
    var p1 = es(KAMIKITAZAWA_3_1, 10);
    var p2 = search.call(KAMIKITAZAWA_3_1, 10, p1.nextKey(), null, false);
    var p3 = search.call(KAMIKITAZAWA_3_1, 10, p2.nextKey(), null, false);

    assertThat(List.of(p1.from(), p2.from(), p3.from())).containsExactly(1L, 11L, 21L);
    assertThat(p1.prevKey()).isNull();
    assertThat(p3.nextKey()).isNull();
    assertThat(Stream.of(p1, p2, p3).flatMap(p -> ids(p.rows()).stream()).toList())
        .isEqualTo(ids(es(KAMIKITAZAWA_3_1, 100).rows()));
    assertThat(ids(search.call(KAMIKITAZAWA_3_1, 10, null, p3.prevKey(), false).rows()))
        .isEqualTo(ids(p2.rows()));
  }

  @Test
  void 前へで1ページに満たないときは先頭の1ページを出す() {
    var p2 = search.call(KAMIKITAZAWA_3_1, 10, es(KAMIKITAZAWA_3_1, 5).nextKey(), null, false);
    var back = search.call(KAMIKITAZAWA_3_1, 10, null, p2.prevKey(), false);

    assertThat(back.from()).isEqualTo(1);
    assertThat(back.rows()).hasSize(10);
    assertThat(back.prevKey()).isNull();
  }

  @Test
  void 最後へは最後の1ページ分を出す() {
    var r = search.call(KAMIKITAZAWA_3_1, 10, null, null, true);

    assertThat(r.from()).isEqualTo(12);
    assertThat(r.rows()).hasSize(10);
    assertThat(r.nextKey()).isNull();
  }

  @Test
  void MySQLと同じく全角と半角を同じ文字として探す() {
    var c = new RsdtSearchCriteria("", "上北沢", "3", "", "");

    assertThat(es(c, 1).total()).isEqualTo(searchDb.call(c, 1, 1).total());
  }

  @Test
  void ワイルドカードの記号はただの文字として探す() {
    assertThat(es(new RsdtSearchCriteria("*", "", "", "", ""), 1).total()).isZero();
    assertThat(es(new RsdtSearchCriteria("", "?", "", "", ""), 1).total()).isZero();
  }

  @Test
  void インデックスの名前と作成日時を返す() {
    var r = es(new RsdtSearchCriteria("存在しない市", "", "", "", ""), 1);

    assertThat(r.index()).matches("rsdt_\\d{8}-\\d{6}");
    assertThat(r.builtAt()).matches("\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d");
  }

  @Test
  void Elasticsearchに接続できないときはエラーの文を返す() {
    var r =
        new SearchRsdtEs(new EsClient("http://127.0.0.1:1", json))
            .call(KAMIKITAZAWA_3_1, 10, null, null, false);

    assertThat(r.rows()).isEmpty();
    assertThat(r.total()).isZero();
    assertThat(r.error()).contains("接続できません");
  }
}
