package dev.abr.web.rsdt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.abr.web.search.RsdtSearchCriteria;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RsdtKeyTest {

  @Test
  void URLのidを主キーの5列にする() {
    // rsdt2_id が空なら末尾が _
    assertThat(RsdtKey.fromParam("131016_0001001_006_001_"))
        .contains(new RsdtKey("131016", "0001001", "006", "001", ""));
    assertThat(RsdtKey.fromParam("131016_0001001_006_999_99999").map(RsdtKey::rsdt2Id))
        .contains("99999");
    assertThat(new RsdtKey("131016", "0001001", "006", "001", "").routeId())
        .isEqualTo("131016_0001001_006_001_");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {"bad", "131016_0001001_006_001", "131016_0001001_006_001__x", "131016__006_001_"})
  void 正しくないidは空(String id) {
    assertThat(RsdtKey.fromParam(id)).isEqualTo(Optional.empty());
  }

  @Test
  void LIKEの特別な文字をエスケープする() {
    assertThat(RsdtSearchCriteria.escapeLike("50%_a\\b")).isEqualTo("50\\%\\_a\\\\b");
  }
}
