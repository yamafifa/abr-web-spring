package dev.abr.web.web;

import dev.abr.web.search.RsdtEsListQuery;
import dev.abr.web.search.RsdtListQuery;
import dev.abr.web.usecase.SearchRsdtEs;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Elasticsearch で検索する一覧（DB の一覧とは別の画面。ページは search_after で送る）
 *
 * <p>編集・削除は DB 検索と同じ画面（RsdtController）を使い、戻り先をこの画面にする。 URL は Thymeleaf の中で作れないので（PagerView
 * と同じ理由）、ここで作って渡す
 */
@Controller
public class EsRsdtController {

  private final SearchRsdtEs searchRsdtEs;

  public EsRsdtController(SearchRsdtEs searchRsdtEs) {
    this.searchRsdtEs = searchRsdtEs;
  }

  @GetMapping("/es")
  public String index(HttpServletRequest request, Model model) {
    var q = RsdtEsListQuery.fromParams(request.getParameterMap());
    var result = searchRsdtEs.call(q.criteria(), q.perPage(), q.after(), q.before(), q.last());
    model.addAttribute("query", q);
    model.addAttribute("result", result);
    // 編集・削除のあとに戻ってくる URL（今の URL。同じ after・before なら同じページが出る）
    model.addAttribute("back", esUrl(q.toParams()));
    model.addAttribute("pager", EsPagerView.of(q.firstPage(), result.prevKey(), result.nextKey()));
    model.addAttribute("dbUrl", RsdtController.indexUrl(q.toDbQuery().toParams()));
    model.addAttribute("perPageOptions", RsdtListQuery.PER_PAGE_OPTIONS);
    model.addAttribute("currentPage", "es");
    return "es/index";
  }

  static String esUrl(Map<String, String> params) {
    var q = new LinkedMultiValueMap<String, String>();
    params.forEach(q::add);
    return UriComponentsBuilder.fromPath("/es").queryParams(q).encode().build().toUriString();
  }
}
