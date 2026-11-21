package dev.abr.web.web;

import dev.abr.web.rsdt.Rsdt;
import dev.abr.web.rsdt.RsdtValues;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 住居の編集フォーム（主キー以外の 17 列）と入力チェック
 *
 * <p>前後の空白を取る・空文字を null にするのは、コントローラーの @InitBinder（StringTrimmerEditor）がやる。 @Size
 * は文字数（コードポイント単位ではなく char 単位）で数える。今のデータに 4 バイト文字があるかは調べていない。
 */
public class RsdtForm {

  private static final String MAX = "{max} 文字以内で入力してください";

  @NotBlank(message = "必須です")
  @Size(max = 20, message = MAX)
  private String city;

  @Size(max = 20, message = MAX)
  private String ward;

  @Size(max = 80, message = MAX)
  private String oazaCho;

  @Size(max = 20, message = MAX)
  private String chome;

  @Size(max = 80, message = MAX)
  private String koaza;

  @Size(max = 10, message = MAX)
  private String machiazaDist;

  @Size(max = 20, message = MAX)
  private String blkNum;

  @Size(max = 20, message = MAX)
  private String rsdtNum;

  @Size(max = 20, message = MAX)
  private String rsdtNum2;

  @Size(max = 1, message = MAX)
  private String basicRsdtDiv;

  @Size(max = 1, message = MAX)
  private String rsdtAddrFlg;

  @Size(max = 1, message = MAX)
  private String rsdtAddrMtdCode;

  @Size(max = 1, message = MAX)
  private String statusFlg;

  @IsoDate private String efctDate;

  @IsoDate private String abltDate;

  @Size(max = 2, message = MAX)
  private String srcCode;

  @Size(max = 255, message = MAX)
  private String remarks;

  /** 今の行の値で埋めたフォーム */
  public static RsdtForm of(Rsdt r) {
    var f = new RsdtForm();
    f.city = r.city();
    f.ward = r.ward();
    f.oazaCho = r.oazaCho();
    f.chome = r.chome();
    f.koaza = r.koaza();
    f.machiazaDist = r.machiazaDist();
    f.blkNum = r.blkNum();
    f.rsdtNum = r.rsdtNum();
    f.rsdtNum2 = r.rsdtNum2();
    f.basicRsdtDiv = r.basicRsdtDiv();
    f.rsdtAddrFlg = r.rsdtAddrFlg();
    f.rsdtAddrMtdCode = r.rsdtAddrMtdCode();
    f.statusFlg = r.statusFlg();
    f.efctDate = r.efctDate() == null ? null : r.efctDate().toString();
    f.abltDate = r.abltDate() == null ? null : r.abltDate().toString();
    f.srcCode = r.srcCode();
    f.remarks = r.remarks();
    return f;
  }

  /** 入力チェックに通ったあとで、更新に使う形にする */
  public RsdtValues toValues() {
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
        IsoDate.Validator.parse(efctDate),
        IsoDate.Validator.parse(abltDate),
        srcCode,
        remarks);
  }

  // Spring がフォームの値を入れる・Thymeleaf が値を読むための getter と setter

  public String getCity() {
    return city;
  }

  public void setCity(String v) {
    city = v;
  }

  public String getWard() {
    return ward;
  }

  public void setWard(String v) {
    ward = v;
  }

  public String getOazaCho() {
    return oazaCho;
  }

  public void setOazaCho(String v) {
    oazaCho = v;
  }

  public String getChome() {
    return chome;
  }

  public void setChome(String v) {
    chome = v;
  }

  public String getKoaza() {
    return koaza;
  }

  public void setKoaza(String v) {
    koaza = v;
  }

  public String getMachiazaDist() {
    return machiazaDist;
  }

  public void setMachiazaDist(String v) {
    machiazaDist = v;
  }

  public String getBlkNum() {
    return blkNum;
  }

  public void setBlkNum(String v) {
    blkNum = v;
  }

  public String getRsdtNum() {
    return rsdtNum;
  }

  public void setRsdtNum(String v) {
    rsdtNum = v;
  }

  public String getRsdtNum2() {
    return rsdtNum2;
  }

  public void setRsdtNum2(String v) {
    rsdtNum2 = v;
  }

  public String getBasicRsdtDiv() {
    return basicRsdtDiv;
  }

  public void setBasicRsdtDiv(String v) {
    basicRsdtDiv = v;
  }

  public String getRsdtAddrFlg() {
    return rsdtAddrFlg;
  }

  public void setRsdtAddrFlg(String v) {
    rsdtAddrFlg = v;
  }

  public String getRsdtAddrMtdCode() {
    return rsdtAddrMtdCode;
  }

  public void setRsdtAddrMtdCode(String v) {
    rsdtAddrMtdCode = v;
  }

  public String getStatusFlg() {
    return statusFlg;
  }

  public void setStatusFlg(String v) {
    statusFlg = v;
  }

  public String getEfctDate() {
    return efctDate;
  }

  public void setEfctDate(String v) {
    efctDate = v;
  }

  public String getAbltDate() {
    return abltDate;
  }

  public void setAbltDate(String v) {
    abltDate = v;
  }

  public String getSrcCode() {
    return srcCode;
  }

  public void setSrcCode(String v) {
    srcCode = v;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String v) {
    remarks = v;
  }
}
