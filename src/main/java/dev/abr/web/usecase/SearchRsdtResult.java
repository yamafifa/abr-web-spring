package dev.abr.web.usecase;

import dev.abr.web.rsdt.Rsdt;
import java.util.List;

/**
 * @param page 実際に表示したページ
 * @param totalPages 0 件でも 1
 */
public record SearchRsdtResult(List<Rsdt> rows, long total, int page, int totalPages) {}
