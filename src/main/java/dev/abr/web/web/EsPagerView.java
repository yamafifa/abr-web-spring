package dev.abr.web.web;

import dev.abr.web.search.RsdtEsListQuery;

/**
 * Elasticsearch 検索のページ送り（search_after）。ページ番号は無く、最初・前・次・最後へ進む
 *
 * <p>どの URL も、そちらへ進めないときは null
 */
public record EsPagerView(String firstUrl, String prevUrl, String nextUrl, String lastUrl) {

  /**
   * @param query ページの位置を外した検索条件
   */
  public static EsPagerView of(RsdtEsListQuery query, String prevKey, String nextKey) {
    return new EsPagerView(
        prevKey == null ? null : EsRsdtController.esUrl(query.toParams()),
        prevKey == null ? null : EsRsdtController.esUrl(query.withBefore(prevKey).toParams()),
        nextKey == null ? null : EsRsdtController.esUrl(query.withAfter(nextKey).toParams()),
        nextKey == null ? null : EsRsdtController.esUrl(query.withLast().toParams()));
  }

  public boolean visible() {
    return prevUrl != null || nextUrl != null;
  }
}
