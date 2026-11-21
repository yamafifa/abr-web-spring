package dev.abr.web;

import dev.abr.web.rsdt.Rsdt;
import dev.abr.web.rsdt.RsdtKey;
import dev.abr.web.rsdt.RsdtMapper;
import dev.abr.web.search.RsdtSearchCriteria;

/**
 * DB を使うテストで共通に使うもの
 *
 * <p>テストは本物の DB（abr-batch と共用）を使い、1 件ごとに @Transactional でトランザクションを取り消す。テーブルを作り直すことはしない。
 */
public final class RsdtFixtures {

  private RsdtFixtures() {}

  /** 元のデータにある行（千代田区 内幸町一丁目 6-1） */
  public static final String EXISTING_ID = "131016_0001001_006_001_";

  /** 無い行 */
  public static final String MISSING_ID = "131016_0001001_006_999_99999";

  /** 元のデータの全件数 */
  public static final long TOTAL_ROWS = 1_752_573L;

  public static Rsdt find(RsdtMapper mapper, String id) {
    return RsdtKey.fromParam(id).flatMap(mapper::findByKey).orElse(null);
  }

  public static Rsdt existing(RsdtMapper mapper, String id) {
    Rsdt r = find(mapper, id);
    if (r == null) {
      throw new AssertionError(id + " が DB に無い（abr-batch で npm run import:v3 をして元に戻す）");
    }
    return r;
  }

  public static long countRows(RsdtMapper mapper) {
    return mapper.count(RsdtSearchCriteria.EMPTY);
  }
}
