package dev.abr.web.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PagerTest {

  /** 今のページの前後 2 つと最初・最後（間は null。期待値の「-」が null） */
  @ParameterizedTest(name = "{0}")
  @CsvSource(
      delimiter = ';',
      value = {
        "1 ページだけ; 1; 1; 1",
        "少ない; 2; 5; 1 2 3 4 5",
        "最初; 1; 100; 1 2 3 - 100",
        "真ん中; 50; 100; 1 - 48 49 50 51 52 - 100",
        "最後; 100; 100; 1 - 98 99 100",
        "1 つだけ飛ぶところも null; 5; 10; 1 - 3 4 5 6 7 - 10",
        "すき間が無ければ null を入れない; 4; 10; 1 2 3 4 5 6 - 10"
      })
  void ページ番号を今のページの前後2つと最初最後にする(String name, int page, int total, String expected) {
    List<Integer> want =
        Arrays.stream(expected.split(" "))
            .map(s -> s.equals("-") ? null : Integer.valueOf(s))
            .toList();

    assertThat(Pager.items(page, total)).isEqualTo(want);
  }
}
