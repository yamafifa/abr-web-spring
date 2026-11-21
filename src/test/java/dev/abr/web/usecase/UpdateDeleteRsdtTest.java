package dev.abr.web.usecase;

import static dev.abr.web.RsdtFixtures.EXISTING_ID;
import static dev.abr.web.RsdtFixtures.MISSING_ID;
import static dev.abr.web.RsdtFixtures.TOTAL_ROWS;
import static dev.abr.web.RsdtFixtures.countRows;
import static dev.abr.web.RsdtFixtures.existing;
import static dev.abr.web.RsdtFixtures.find;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.abr.web.rsdt.RsdtMapper;
import dev.abr.web.rsdt.RsdtValues;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 1 件ごとに、DB への書き込みを取り消す（@Transactional） */
@SpringBootTest
@Transactional
class UpdateDeleteRsdtTest {

  @Autowired GetRsdt getRsdt;
  @Autowired UpdateRsdt updateRsdt;
  @Autowired DeleteRsdt deleteRsdt;
  @Autowired RsdtMapper mapper;

  /** city と remarks・efctDate だけ変えた値（ほかは今の値のまま） */
  private RsdtValues changed(String city, String remarks, LocalDate efct) {
    var v = existing(mapper, EXISTING_ID).values();
    return new RsdtValues(
        city,
        v.ward(),
        v.oazaCho(),
        v.chome(),
        v.koaza(),
        v.machiazaDist(),
        v.blkNum(),
        v.rsdtNum(),
        v.rsdtNum2(),
        v.basicRsdtDiv(),
        v.rsdtAddrFlg(),
        v.rsdtAddrMtdCode(),
        v.statusFlg(),
        efct,
        v.abltDate(),
        v.srcCode(),
        remarks);
  }

  @Test
  void GetRsdtはidで1件取る() {
    assertThat(getRsdt.call(EXISTING_ID).map(r -> r.address())).contains("千代田区 内幸町一丁目 6-1");
  }

  @Test
  void GetRsdtは無い行と正しくないidなら空() {
    assertThat(getRsdt.call(MISSING_ID)).isEmpty();
    assertThat(getRsdt.call("bad")).isEmpty();
  }

  @Test
  void UpdateRsdtは主キー以外の列を更新する() {
    var status = updateRsdt.call(EXISTING_ID, changed("千代田区", "テスト", LocalDate.of(2026, 1, 2)));

    assertThat(status).isEqualTo(UpdateRsdtStatus.UPDATED);
    var after = existing(mapper, EXISTING_ID);
    assertThat(after.remarks()).isEqualTo("テスト");
    assertThat(after.efctDate()).isEqualTo(LocalDate.of(2026, 1, 2));
    assertThat(after.city()).isEqualTo("千代田区");
  }

  @Test
  void UpdateRsdtはnullの列をNULLにする() {
    updateRsdt.call(
        EXISTING_ID,
        new RsdtValues(
            "千代田区", null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null));
    var after = existing(mapper, EXISTING_ID);

    assertThat(after.blkNum()).isNull();
    assertThat(after.efctDate()).isNull();
    assertThat(after.oazaCho()).isNull();
  }

  @Test
  void UpdateRsdtは無い行と正しくないidならNOT_FOUND() {
    var v = changed("x", null, null);

    assertThat(updateRsdt.call(MISSING_ID, v)).isEqualTo(UpdateRsdtStatus.NOT_FOUND);
    assertThat(updateRsdt.call("bad", v)).isEqualTo(UpdateRsdtStatus.NOT_FOUND);
    assertThat(countRows(mapper)).isEqualTo(TOTAL_ROWS);
  }

  @Test
  void UpdateRsdtはcityが空なら例外にしてDBを変えない() {
    assertThatThrownBy(() -> updateRsdt.call(EXISTING_ID, changed(" ", null, null)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(existing(mapper, EXISTING_ID).city()).isEqualTo("千代田区");
  }

  @Test
  void UpdateRsdtはほかの行を変えない() {
    var neighbor = existing(mapper, "131016_0001001_006_002_");
    updateRsdt.call(EXISTING_ID, changed("千代田区", "テスト", null));

    assertThat(existing(mapper, "131016_0001001_006_002_")).isEqualTo(neighbor);
  }

  @Test
  void DeleteRsdtは1行だけ削除する() {
    deleteRsdt.call(EXISTING_ID);

    assertThat(find(mapper, EXISTING_ID)).isNull();
    assertThat(countRows(mapper)).isEqualTo(TOTAL_ROWS - 1);
  }

  @Test
  void DeleteRsdtは無い行と正しくないidでも失敗しない() {
    deleteRsdt.call(MISSING_ID);
    deleteRsdt.call("bad");
    deleteRsdt.call("");

    assertThat(countRows(mapper)).isEqualTo(TOTAL_ROWS);
  }
}
