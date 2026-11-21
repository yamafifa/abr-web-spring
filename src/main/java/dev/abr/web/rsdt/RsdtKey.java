package dev.abr.web.rsdt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 住居の主キー（5 列）。URL の id は 5 列を "_" でつないだもの（例：131016_0001001_006_001_）。
 *
 * <p>rsdt2_id は空文字が多い（NULL は無い。テーブルの既定値が ''）ので、末尾が "_" で終わることがある。
 */
public record RsdtKey(
    String lgCode, String machiazaId, String blkId, String rsdtId, String rsdt2Id) {

  /** 主キーの列名（画面に出す順） */
  public static final List<String> COLUMNS =
      List.of("lg_code", "machiaza_id", "blk_id", "rsdt_id", "rsdt2_id");

  /** URL の id を主キーにする。正しくない形なら空 */
  public static Optional<RsdtKey> fromParam(String param) {
    if (param == null) {
      return Optional.empty();
    }
    // -1 を付けると、末尾の空文字も残る（"a_b_c_d_" → 5 つ）
    String[] parts = param.split("_", -1);
    if (parts.length != 5) {
      return Optional.empty();
    }
    for (int i = 0; i < 4; i++) {
      if (parts[i].isEmpty()) {
        return Optional.empty();
      }
    }
    return Optional.of(new RsdtKey(parts[0], parts[1], parts[2], parts[3], parts[4]));
  }

  /** URL の id。Rails 版の to_param と同じ形 */
  public String routeId() {
    return String.join("_", lgCode, machiazaId, blkId, rsdtId, rsdt2Id);
  }

  /** 列名 → 値（画面に出すため） */
  public Map<String, String> toMap() {
    Map<String, String> m = new LinkedHashMap<>();
    List<String> values = List.of(lgCode, machiazaId, blkId, rsdtId, rsdt2Id);
    for (int i = 0; i < COLUMNS.size(); i++) {
      m.put(COLUMNS.get(i), values.get(i));
    }
    return m;
  }
}
