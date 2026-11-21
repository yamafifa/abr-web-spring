package dev.abr.web.web;

import dev.abr.web.rsdt.Rsdt;
import dev.abr.web.search.RsdtEsListQuery;
import dev.abr.web.search.RsdtListQuery;
import dev.abr.web.usecase.DeleteRsdt;
import dev.abr.web.usecase.GetRsdt;
import dev.abr.web.usecase.SearchRsdt;
import dev.abr.web.usecase.UpdateRsdt;
import dev.abr.web.usecase.UpdateRsdtStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 画面からの操作を受け取る
 *
 * <p>ここでやるのは「パラメーターを読む → ユースケースを呼ぶ → 表示・redirect」だけ。 ユースケースはコンストラクターで受け取る（Spring が作って渡す）。{id} は主キー
 * 5 列を "_" でつないだもの（例：/rsdt/131016_0001001_006_001_/edit）
 */
@Controller
public class RsdtController {

  private final SearchRsdt searchRsdt;
  private final GetRsdt getRsdt;
  private final UpdateRsdt updateRsdt;
  private final DeleteRsdt deleteRsdt;

  public RsdtController(
      SearchRsdt searchRsdt, GetRsdt getRsdt, UpdateRsdt updateRsdt, DeleteRsdt deleteRsdt) {
    this.searchRsdt = searchRsdt;
    this.getRsdt = getRsdt;
    this.updateRsdt = updateRsdt;
    this.deleteRsdt = deleteRsdt;
  }

  /** フォームの値の前後の空白を取り、空文字は null にする */
  @InitBinder
  void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
  }

  /** 一覧（検索・ページャー） */
  @GetMapping("/")
  public String index(HttpServletRequest request, Model model) {
    var query = RsdtListQuery.fromParams(request.getParameterMap());
    var result = searchRsdt.call(query.criteria(), query.page(), query.perPage());
    model.addAttribute("query", query);
    model.addAttribute("result", result);
    // 編集・削除のあとに戻ってくる URL（今の検索条件とページ）
    model.addAttribute("back", indexUrl(query.withPage(result.page()).toParams()));
    model.addAttribute("pager", PagerView.of(query, result.page(), result.totalPages()));
    model.addAttribute("perPageOptions", RsdtListQuery.PER_PAGE_OPTIONS);
    model.addAttribute(
        "esUrl",
        EsRsdtController.esUrl(
            new RsdtEsListQuery(query.criteria(), query.perPage(), null, null, false).toParams()));
    model.addAttribute("currentPage", "db");
    return "rsdt/index";
  }

  @GetMapping("/rsdt/{id}/edit")
  public String edit(
      @PathVariable String id,
      @RequestParam(required = false) String back,
      Model model,
      HttpServletResponse response) {
    var rsdt = getRsdt.call(id);
    if (rsdt.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return "rsdt/not-found";
    }
    return editView(model, rsdt.get(), RsdtForm.of(rsdt.get()), safeBack(back));
  }

  /**
   * 更新。入力チェックに通らなければ、redirect せずに編集画面をそのまま出す（422）
   *
   * <p>Laravel 版は「編集画面に redirect して、入力値とエラーをセッションで渡す」。Spring は Symfony 版と同じく、この形がふつう
   */
  @PatchMapping("/rsdt/{id}")
  public String update(
      @PathVariable String id,
      @RequestParam(required = false) String back,
      @Valid @ModelAttribute("form") RsdtForm form,
      BindingResult errors,
      Model model,
      HttpServletResponse response,
      RedirectAttributes flash) {
    String safe = safeBack(back);
    var rsdt = getRsdt.call(id);
    if (rsdt.isEmpty()) {
      flash.addFlashAttribute("alert", "この行はすでに削除されています");
      return "redirect:" + safe;
    }
    if (errors.hasErrors()) {
      response.setStatus(422);
      model.addAttribute("alert", "入力内容を確認してください");
      return editView(model, rsdt.get(), form, safe);
    }
    var status = updateRsdt.call(id, form.toValues());
    if (status == UpdateRsdtStatus.UPDATED) {
      flash.addFlashAttribute("notice", "更新しました");
    } else {
      flash.addFlashAttribute("alert", "この行はすでに削除されています");
    }
    return "redirect:" + safe;
  }

  @DeleteMapping("/rsdt/{id}")
  public String delete(
      @PathVariable String id,
      @RequestParam(required = false) String back,
      RedirectAttributes flash) {
    deleteRsdt.call(id);
    flash.addFlashAttribute("notice", "削除しました");
    return "redirect:" + safeBack(back);
  }

  private String editView(Model model, Rsdt rsdt, RsdtForm form, String back) {
    model.addAttribute("rsdt", rsdt);
    model.addAttribute("form", form);
    model.addAttribute("back", back);
    model.addAttribute("rules", Rsdt.ATTRIBUTE_RULES);
    return "rsdt/edit";
  }

  /** 一覧の URL（日本語などはエンコードする） */
  static String indexUrl(Map<String, String> params) {
    var q = new LinkedMultiValueMap<String, String>();
    params.forEach(q::add);
    return UriComponentsBuilder.fromPath("/").queryParams(q).encode().build().toUriString();
  }

  /** 戻り先は、このサイトの中のパスだけを許す（外のサイトへ飛ばされないように） */
  static String safeBack(String back) {
    return back != null
            && back.startsWith("/")
            && !back.startsWith("//")
            && !back.contains("\\")
            && !back.contains("\r")
            && !back.contains("\n")
        ? back
        : "/";
  }
}
