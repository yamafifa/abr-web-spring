package dev.abr.web.usecase;

import dev.abr.web.es.EsClient;
import dev.abr.web.es.EsException;
import dev.abr.web.rsdt.Rsdt;
import dev.abr.web.search.RsdtFold;
import dev.abr.web.search.RsdtSearchCriteria;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/**
 * Elasticsearch（abr-batch の es:index で作った別名 rsdt）で住居を検索して、1 ページ分を返す
 *
 * <ul>
 *   <li>条件の意味は DB 検索（RsdtMapper.xml）と同じ。部分一致は wildcard、完全一致は term で探す
 *   <li>文字は RsdtFold でそろえてから探す（MySQL の utf8mb4_unicode_ci と同じように見つけるため）
 *   <li>並びは主キー順。ページは search_after で送る（Elasticsearch は先頭から 10,000 件より先へ from で進めないため）
 * </ul>
 */
@Service
public class SearchRsdtEs {

  public static final String ALIAS = "rsdt";

  /** 部分一致で探す条件：条件の値の取り出し方と、探す列（どれかに当たればよい） */
  private record Wildcard(Function<RsdtSearchCriteria, String> value, List<String> columns) {}

  private static final List<Wildcard> WILDCARDS =
      List.of(
          new Wildcard(RsdtSearchCriteria::city, List.of("city", "ward")),
          new Wildcard(RsdtSearchCriteria::town, List.of("oaza_cho", "koaza")),
          new Wildcard(RsdtSearchCriteria::chome, List.of("chome")));

  /** 完全一致で探す条件：条件の値の取り出し方 → 列 */
  private static final Map<Function<RsdtSearchCriteria, String>, String> EXACTS =
      new LinkedHashMap<>();

  static {
    EXACTS.put(RsdtSearchCriteria::blkNum, "blk_num");
    EXACTS.put(RsdtSearchCriteria::rsdtNum, "rsdt_num");
  }

  private final EsClient client;

  public SearchRsdtEs(EsClient client) {
    this.client = client;
  }

  /** 1 ページ分の結果（主キー順） */
  private record Page(List<JsonNode> hits, long total, String index) {}

  public SearchRsdtEsResult call(
      RsdtSearchCriteria criteria, int perPage, String after, String before, boolean last) {
    if (perPage <= 0) {
      throw new IllegalArgumentException("perPage が正しくありません: " + perPage);
    }
    try {
      var filters = filters(criteria);
      Page page = fetch(filters, perPage, after, before, last);
      // 「前へ」で 1 ページに満たなかったら、先頭まで来ているので、先頭の 1 ページを出す
      if (before != null && page.hits().size() < perPage) {
        page = fetch(filters, perPage, null, null, false);
      }
      var hits = page.hits();
      String firstKey = hits.isEmpty() ? null : key(hits.getFirst());
      // このページより前に何件あるか（何件目からかを出すため）
      long beforeCount = 0;
      if (firstKey != null) {
        var withRange = new ArrayList<>(filters);
        withRange.add(Map.of("range", Map.of("key", Map.of("lt", firstKey))));
        beforeCount = count(withRange);
      }
      return new SearchRsdtEsResult(
          hits.stream().map(SearchRsdtEs::toRsdt).toList(),
          page.total(),
          hits.isEmpty() ? 0 : beforeCount + 1,
          beforeCount > 0 ? firstKey : null,
          beforeCount + hits.size() < page.total() ? key(hits.getLast()) : null,
          page.index() != null ? page.index() : aliasTarget(),
          null);
    } catch (EsException e) {
      return SearchRsdtEsResult.failure(
          e.notFound()
              ? "Elasticsearch にインデックス " + ALIAS + " がありません。abr-batch で npm run es:index を実行してください"
              : e.getMessage());
    }
  }

  private static List<Object> filters(RsdtSearchCriteria c) {
    List<Object> filters = new ArrayList<>();
    for (var w : WILDCARDS) {
      String v = w.value().apply(c);
      if (v.isEmpty()) {
        continue;
      }
      // * ? \ は、ただの文字として探す
      String pattern = "*" + RsdtFold.call(v).replaceAll("([\\\\*?])", "\\\\$1") + "*";
      filters.add(
          Map.of(
              "bool",
              Map.of(
                  "should",
                  w.columns().stream()
                      .map(col ->
                          Map.of("wildcard", Map.of("fold." + col, Map.of("value", pattern))))
                      .toList(),
                  "minimum_should_match",
                  1)));
    }
    EXACTS.forEach((value, col) -> {
      String v = value.apply(c);
      if (!v.isEmpty()) {
        filters.add(Map.of("term", Map.of("fold." + col, RsdtFold.call(v))));
      }
    });
    return filters;
  }

  /** 1 ページ分を取る。逆順で取ったときは、主キー順に並べ直して返す */
  private Page fetch(List<Object> filters, int perPage, String after, String before, boolean last) {
    boolean desc = before != null || last;
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("track_total_hits", true);
    body.put("size", perPage);
    body.put("sort", List.of(Map.of("key", desc ? "desc" : "asc")));
    body.put("query", Map.of("bool", Map.of("filter", filters)));
    String cursor = after != null ? after : before;
    if (cursor != null) {
      body.put("search_after", List.of(cursor));
    }
    JsonNode res = client.post("/" + ALIAS + "/_search", body);
    List<JsonNode> hits = new ArrayList<>();
    res.path("hits").path("hits").forEach(hits::add);
    String index = hits.isEmpty() ? null : hits.getFirst().path("_index").asString();
    if (desc) {
      hits = hits.reversed();
    }
    return new Page(hits, res.path("hits").path("total").path("value").asLong(), index);
  }

  private long count(List<Object> filters) {
    return client
        .post("/" + ALIAS + "/_count", Map.of("query", Map.of("bool", Map.of("filter", filters))))
        .path("count")
        .asLong();
  }

  private String aliasTarget() {
    var names = client.get("/_alias/" + ALIAS).propertyNames().iterator();
    return names.hasNext() ? names.next() : null;
  }

  private static String key(JsonNode hit) {
    return hit.path("_source").path("key").asString();
  }

  /** 画面の部品（住所の表示・編集のリンクなど）を DB 検索と同じにするため、Rsdt にする（DB には読みに行かない） */
  private static Rsdt toRsdt(JsonNode hit) {
    Map<String, String> columns = new HashMap<>();
    hit.path("_source")
        .properties()
        .forEach(
            e -> columns.put(e.getKey(), e.getValue().isString() ? e.getValue().asString() : null));
    return Rsdt.fromColumns(columns);
  }
}
