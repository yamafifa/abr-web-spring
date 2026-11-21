package dev.abr.web.search;

/**
 * 住居の検索条件。空文字の条件は使わない
 *
 * <ul>
 *   <li>city: city・ward の部分一致
 *   <li>town: oaza_cho・koaza の部分一致
 *   <li>chome: chome の部分一致
 *   <li>blkNum・rsdtNum: 完全一致
 * </ul>
 */
public record RsdtSearchCriteria(
    String city, String town, String chome, String blkNum, String rsdtNum) {

  public static final RsdtSearchCriteria EMPTY = new RsdtSearchCriteria("", "", "", "", "");

  public RsdtSearchCriteria {
    city = city == null ? "" : city;
    town = town == null ? "" : town;
    chome = chome == null ? "" : chome;
    blkNum = blkNum == null ? "" : blkNum;
    rsdtNum = rsdtNum == null ? "" : rsdtNum;
  }

  // 部分一致の LIKE に渡す値（RsdtMapper.xml で使う）
  public String likeCity() {
    return like(city);
  }

  public String likeTown() {
    return like(town);
  }

  public String likeChome() {
    return like(chome);
  }

  private static String like(String s) {
    return "%" + escapeLike(s) + "%";
  }

  /** LIKE の特別な文字（% _ \）を、ただの文字として探せるようにする（MySQL の既定のエスケープ文字は \） */
  public static String escapeLike(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
