package dev.abr.web.rsdt;

import java.time.LocalDate;

/**
 * 住居の、主キー以外の 17 列（更新に使う）。文字の列の null は NULL のまま入れる
 *
 * @param city 必須（NOT NULL）
 */
public record RsdtValues(
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
    String remarks) {}
