package dev.abr.web.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SameOriginFilterTest {

  private static MockHttpServletRequest request(String origin, String referer) {
    var r = new MockHttpServletRequest("POST", "/rsdt/x");
    r.setServerName("127.0.0.1");
    r.setServerPort(3000);
    if (origin != null) {
      r.addHeader("Origin", origin);
    }
    if (referer != null) {
      r.addHeader("Referer", referer);
    }
    return r;
  }

  @Test
  void 同じサイトからなら通す() {
    assertThat(SameOriginFilter.isSameOrigin(request("http://127.0.0.1:3000", null))).isTrue();
    // Origin が無ければ Referer を見る
    assertThat(SameOriginFilter.isSameOrigin(request(null, "http://127.0.0.1:3000/rsdt/x/edit")))
        .isTrue();
  }

  @Test
  void 別のサイトやポートからなら断る() {
    assertThat(SameOriginFilter.isSameOrigin(request("https://evil.example", null))).isFalse();
    assertThat(SameOriginFilter.isSameOrigin(request("http://127.0.0.1:8080", null))).isFalse();
    assertThat(SameOriginFilter.isSameOrigin(request("null", "https://evil.example/"))).isFalse();
  }

  @Test
  void 送り元が分からなければ断る() {
    assertThat(SameOriginFilter.isSameOrigin(request(null, null))).isFalse();
    assertThat(SameOriginFilter.isSameOrigin(request("::bad::", null))).isFalse();
  }
}
