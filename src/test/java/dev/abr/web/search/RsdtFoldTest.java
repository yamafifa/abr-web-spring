package dev.abr.web.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RsdtFoldTest {

  @Test
  void 全角と半角と大文字と小文字をそろえる() {
    assertThat(RsdtFold.call("３丁目ＡＢｃ")).isEqualTo("3丁目abc");
  }

  @Test
  void カタカナをひらがなにして濁点と半濁点を消す() {
    assertThat(RsdtFold.call("パガあ")).isEqualTo("はかあ");
  }

  @Test
  void 小さい仮名を大きくする() {
    assertThat(RsdtFold.call("ヶヵっ")).isEqualTo("けかつ");
  }

  @Test
  void CJK互換漢字はそのまま残す() {
    String compat = Character.toString(0xFA10); // 塚（互換漢字）。MySQL は U+585A の「塚」と別の文字として扱う
    assertThat(RsdtFold.call(compat)).isEqualTo(compat);
    assertThat(RsdtFold.call("塚")).isEqualTo("塚");
  }
}
