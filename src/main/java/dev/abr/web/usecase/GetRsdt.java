package dev.abr.web.usecase;

import dev.abr.web.rsdt.Rsdt;
import dev.abr.web.rsdt.RsdtKey;
import dev.abr.web.rsdt.RsdtMapper;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** 住居を 1 件取る。無い・正しくない id なら空 */
@Service
public class GetRsdt {

  private final RsdtMapper rsdts;

  public GetRsdt(RsdtMapper rsdts) {
    this.rsdts = rsdts;
  }

  public Optional<Rsdt> call(String id) {
    return RsdtKey.fromParam(id).flatMap(rsdts::findByKey);
  }
}
