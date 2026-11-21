package dev.abr.web.web;

import dev.abr.web.search.RsdtListQuery;
import java.util.List;

/**
 * 画面に出すページャー（リンクの URL まで作っておく）
 *
 * <p>Thymeleaf 3.1 は、th:href の中で Bean のメソッドを呼ぶと「この場所では使えない」とエラーにするので、URL はコントローラーで作って渡す。
 *
 * @param items ページ番号と URL。page が null のところは「…」
 * @param prevUrl 前のページ（無ければ null）
 * @param nextUrl 次のページ（無ければ null）
 */
public record PagerView(List<Item> items, int page, String prevUrl, String nextUrl) {

  public record Item(Integer page, String url) {}

  public static PagerView of(RsdtListQuery query, int page, int totalPages) {
    List<Item> items =
        Pager.items(page, totalPages).stream()
            .map(p -> new Item(p, p == null ? null : url(query, p)))
            .toList();
    return new PagerView(
        items,
        page,
        page > 1 ? url(query, page - 1) : null,
        page < totalPages ? url(query, page + 1) : null);
  }

  private static String url(RsdtListQuery query, int page) {
    return RsdtController.indexUrl(query.withPage(page).toParams());
  }
}
