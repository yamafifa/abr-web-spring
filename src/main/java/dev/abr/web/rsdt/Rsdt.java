package dev.abr.web.rsdt;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 住居表示-住居マスター（mt_rsdtdsp_rsdt）の 1 行
 *
 * <p>テーブルは abr-batch で作ったものを共用する。主キーは 5 列の複合キー（{@link RsdtKey}）。 MyBatis が SELECT
 * の結果を、列名と同じ名前（camelCase）の引数に渡して作る（application.properties の
 * arg-name-based-constructor-auto-mapping）。
 */
public record Rsdt(
    String lgCode,
    String machiazaId,
    String blkId,
    String rsdtId,
    String rsdt2Id,
    String city,
    String ward,
    String oazaCho,
    String chome,
    String koaza,
    String machiazaDist,
    String blkNum,
    String rsdtNum,
    String rsdtNum2,
    String basicRsdtDiv,
    String rsdtAddrFlg,
    String rsdtAddrMtdCode,
    String statusFlg,
    LocalDate efctDate,
    LocalDate abltDate,
    String srcCode,
    String remarks) {

  /**
   * 主キー以外の 17 列の決まり（最大文字数はテーブル定義に合わせる）。画面の項目名は列名のまま
   *
   * @param column 列名（画面に出す名前）
   * @param property フォームのプロパティ名
   * @param max 最大文字数（日付は 0）
   */
  public record AttributeRule(
      String column, String property, int max, boolean required, boolean date) {}

  public static final List<AttributeRule> ATTRIBUTE_RULES =
      List.of(
          new AttributeRule("city", "city", 20, true, false),
          new AttributeRule("ward", "ward", 20, false, false),
          new AttributeRule("oaza_cho", "oazaCho", 80, false, false),
          new AttributeRule("chome", "chome", 20, false, false),
          new AttributeRule("koaza", "koaza", 80, false, false),
          new AttributeRule("machiaza_dist", "machiazaDist", 10, false, false),
          new AttributeRule("blk_num", "blkNum", 20, false, false),
          new AttributeRule("rsdt_num", "rsdtNum", 20, false, false),
          new AttributeRule("rsdt_num2", "rsdtNum2", 20, false, false),
          new AttributeRule("basic_rsdt_div", "basicRsdtDiv", 1, false, false),
          new AttributeRule("rsdt_addr_flg", "rsdtAddrFlg", 1, false, false),
          new AttributeRule("rsdt_addr_mtd_code", "rsdtAddrMtdCode", 1, false, false),
          new AttributeRule("status_flg", "statusFlg", 1, false, false),
          new AttributeRule("efct_date", "efctDate", 0, false, true),
          new AttributeRule("ablt_date", "abltDate", 0, false, true),
          new AttributeRule("src_code", "srcCode", 2, false, false),
          new AttributeRule("remarks", "remarks", 255, false, false));

  public RsdtKey key() {
    return new RsdtKey(lgCode, machiazaId, blkId, rsdtId, rsdt2Id);
  }

  /** URL の id */
  public String routeId() {
    return key().routeId();
  }

  /** 画面に出す住所（例：千代田区 内幸町一丁目 6-1） */
  public String address() {
    String town =
        joinNonEmpty(
            " ", city, ward, Objects.toString(oazaCho, "") + Objects.toString(chome, ""), koaza);
    String num = joinNonEmpty("-", blkNum, rsdtNum, rsdtNum2);
    return num.isEmpty() ? town : town + " " + num;
  }

  /** 編集できる 17 列の値（列名 → 値。日付は YYYY-MM-DD、NULL は空文字）。編集画面の「変更前」に使う */
  public Map<String, String> formValues() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("city", city);
    m.put("ward", ward);
    m.put("oaza_cho", oazaCho);
    m.put("chome", chome);
    m.put("koaza", koaza);
    m.put("machiaza_dist", machiazaDist);
    m.put("blk_num", blkNum);
    m.put("rsdt_num", rsdtNum);
    m.put("rsdt_num2", rsdtNum2);
    m.put("basic_rsdt_div", basicRsdtDiv);
    m.put("rsdt_addr_flg", rsdtAddrFlg);
    m.put("rsdt_addr_mtd_code", rsdtAddrMtdCode);
    m.put("status_flg", statusFlg);
    m.put("efct_date", efctDate == null ? null : efctDate.toString());
    m.put("ablt_date", abltDate == null ? null : abltDate.toString());
    m.put("src_code", srcCode);
    m.put("remarks", remarks);
    m.replaceAll((k, v) -> v == null ? "" : v);
    return m;
  }

  /** 編集できる 17 列（更新に使う形） */
  public RsdtValues values() {
    return new RsdtValues(
        city,
        ward,
        oazaCho,
        chome,
        koaza,
        machiazaDist,
        blkNum,
        rsdtNum,
        rsdtNum2,
        basicRsdtDiv,
        rsdtAddrFlg,
        rsdtAddrMtdCode,
        statusFlg,
        efctDate,
        abltDate,
        srcCode,
        remarks);
  }

  private static String joinNonEmpty(String sep, String... parts) {
    return Stream.of(parts).filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining(sep));
  }
}
