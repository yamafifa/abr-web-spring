package dev.abr.web.usecase;

import dev.abr.web.rsdt.RsdtKey;
import dev.abr.web.rsdt.RsdtMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 住居を削除する。すでに無ければ、消せたものとして扱う（同じ削除を 2 回送られても失敗にしない） */
@Service
public class DeleteRsdt {

  private final RsdtMapper rsdts;

  public DeleteRsdt(RsdtMapper rsdts) {
    this.rsdts = rsdts;
  }

  @Transactional
  public void call(String id) {
    RsdtKey.fromParam(id).ifPresent(rsdts::delete);
  }
}
