package dev.abr.web.usecase;

import dev.abr.web.rsdt.RsdtKey;
import dev.abr.web.rsdt.RsdtMapper;
import dev.abr.web.rsdt.RsdtValues;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 住居の主キー以外の列を更新する
 *
 * <p>入力のチェックは、ここに来る前に Bean Validation（RsdtForm）で済ませてある。ほかの版と同じく、ユースケースは「入力エラー」を返さない。
 */
@Service
public class UpdateRsdt {

  private final RsdtMapper rsdts;

  public UpdateRsdt(RsdtMapper rsdts) {
    this.rsdts = rsdts;
  }

  /**
   * @throws IllegalArgumentException city が空のとき（画面からは来ないが、ほかから呼ばれたときのため）
   */
  @Transactional
  public UpdateRsdtStatus call(String id, RsdtValues values) {
    if (values.city() == null || values.city().isBlank()) {
      throw new IllegalArgumentException("city は必須です");
    }
    Optional<RsdtKey> key = RsdtKey.fromParam(id);
    // 読んでから更新するまでの間に、ほかの画面で消されないように行をロックする（SELECT ... FOR UPDATE）
    if (key.isEmpty() || rsdts.findByKeyForUpdate(key.get()).isEmpty()) {
      return UpdateRsdtStatus.NOT_FOUND;
    }
    rsdts.update(key.get(), values);
    return UpdateRsdtStatus.UPDATED;
  }
}
