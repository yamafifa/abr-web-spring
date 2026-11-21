package dev.abr.web.usecase;

import dev.abr.web.rsdt.RsdtMapper;
import dev.abr.web.search.RsdtSearchCriteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 住居を検索して、1 ページ分を返す */
@Service
public class SearchRsdt {

  private final RsdtMapper rsdts;

  public SearchRsdt(RsdtMapper rsdts) {
    this.rsdts = rsdts;
  }

  @Transactional(readOnly = true)
  public SearchRsdtResult call(RsdtSearchCriteria criteria, int page, int perPage) {
    if (perPage <= 0) {
      throw new IllegalArgumentException("perPage が正しくありません: " + perPage);
    }
    long total = rsdts.count(criteria);
    int totalPages = (int) Math.max((total + perPage - 1) / perPage, 1);
    // 最後のページより後ろを指定されたら、最後のページを出す
    int current = Math.min(Math.max(page, 1), totalPages);
    var rows = rsdts.search(criteria, (current - 1) * perPage, perPage);
    return new SearchRsdtResult(rows, total, current, totalPages);
  }
}
