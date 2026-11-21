package dev.abr.web.web;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** ページャーに出すページ番号 */
public final class Pager {

  private Pager() {}

  /** 今のページの前後 2 つと、最初と最後。間が空くところには null（「…」）を入れる */
  public static List<Integer> items(int page, int totalPages) {
    var pages = new TreeSet<Integer>();
    pages.add(1);
    pages.add(totalPages);
    for (int p = page - 2; p <= page + 2; p++) {
      pages.add(p);
    }
    pages.removeIf(p -> p < 1 || p > totalPages);

    List<Integer> items = new ArrayList<>();
    Integer prev = null;
    for (int p : pages) {
      if (prev != null && p - prev > 1) {
        items.add(null);
      }
      items.add(p);
      prev = p;
    }
    return items;
  }
}
