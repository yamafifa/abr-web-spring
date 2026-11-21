package dev.abr.web.usecase;

import static dev.abr.web.RsdtFixtures.EXISTING_ID;
import static dev.abr.web.RsdtFixtures.TOTAL_ROWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.abr.web.rsdt.Rsdt;
import dev.abr.web.search.RsdtSearchCriteria;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SearchRsdtTest {

  @Autowired SearchRsdt search;

  private static RsdtSearchCriteria c(
      String city, String town, String chome, String blk, String rsdt) {
    return new RsdtSearchCriteria(city, town, chome, blk, rsdt);
  }

  @Test
  void 条件が無ければ全件を主キーの順に1ページ分返す() {
    var r = search.call(RsdtSearchCriteria.EMPTY, 1, 15);

    assertThat(r.total()).isEqualTo(TOTAL_ROWS);
    assertThat(r.rows()).hasSize(15);
    assertThat(r.totalPages()).isEqualTo((int) ((TOTAL_ROWS + 14) / 15));
    assertThat(r.rows().getFirst().routeId()).isEqualTo(EXISTING_ID); // 元のデータの先頭の行
    List<String> ids = r.rows().stream().map(Rsdt::routeId).toList();
    assertThat(ids).isSorted();
  }

  @Test
  void 町字と丁目とblkNumで絞り込む() {
    // 丁目は全角の「３丁目」も半角の 3 で見つかる（MySQL の照合順序のため）
    var r = search.call(c("", "上北沢", "3", "1", ""), 1, 100);

    assertThat(r.total()).isEqualTo(21);
    assertThat(r.rows())
        .allSatisfy(x ->
            assertThat(List.of(x.oazaCho(), x.chome(), x.blkNum()))
                .containsExactly("上北沢", "３丁目", "1"));
  }

  @Test
  void 丁目の漢数字と数字は別の文字() {
    // 「三丁目」では「３丁目」が出ない
    assertThat(search.call(c("", "上北沢", "三丁目", "", ""), 1, 15).total()).isZero();
  }

  @Test
  void 市区町村は部分一致() {
    var r = search.call(c("世田谷", "", "", "", ""), 1, 15);

    assertThat(r.total()).isPositive();
    assertThat(r.rows()).allSatisfy(x -> assertThat(x.city()).contains("世田谷"));
  }

  @Test
  void blkNumとrsdtNumは完全一致() {
    // 「1」で 10・21 は出ない
    var r = search.call(c("", "上北沢", "3", "1", "1"), 1, 15);

    assertThat(r.rows()).extracting(Rsdt::rsdtNum).containsOnly("1");
  }

  @Test
  void パーセントとアンダースコアはただの文字として探す() {
    assertThat(search.call(c("%", "", "", "", ""), 1, 15).total()).isZero();
    assertThat(search.call(c("", "_", "", "", ""), 1, 15).total()).isZero();
  }

  @Test
  void 最後のページより後ろを指定されたら最後のページを返す() {
    var r = search.call(c("", "上北沢", "3", "1", ""), 99, 15);

    assertThat(List.of(r.page(), r.totalPages(), r.rows().size())).containsExactly(2, 2, 6);
  }

  @Test
  void 件数が0でもtotalPagesは1() {
    var r = search.call(c("存在しない市", "", "", "", ""), 1, 15);

    assertThat(r.total()).isZero();
    assertThat(List.of(r.totalPages(), r.page())).containsExactly(1, 1);
    assertThat(r.rows()).isEmpty();
  }

  @Test
  void perPageが0以下なら例外() {
    assertThatThrownBy(() -> search.call(RsdtSearchCriteria.EMPTY, 1, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
