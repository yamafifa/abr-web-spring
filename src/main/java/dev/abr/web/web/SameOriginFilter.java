package dev.abr.web.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CSRF 対策（なりすまし送信を防ぐ）。データを変えるリクエストは、同じサイトから送られたものだけを通す
 *
 * <p>ブラウザは、フォームを送るときに Origin ヘッダー（送り元のサイト）を付ける。それがこのサイトと同じかを見る。Origin が無ければ Referer
 * を見て、どちらも無ければ断る。Symfony 8 の既定（ステートレスの CSRF）と同じ考え方。 Spring Security は入れていない（ログインが無いアプリなので）。
 */
@Component
public class SameOriginFilter extends OncePerRequestFilter {

  private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (SAFE_METHODS.contains(request.getMethod()) || isSameOrigin(request)) {
      chain.doFilter(request, response);
      return;
    }
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "別のサイトから送られたリクエストは受け付けません");
  }

  static boolean isSameOrigin(HttpServletRequest request) {
    String source = request.getHeader("Origin");
    if (source == null || source.isEmpty() || "null".equals(source)) {
      source = request.getHeader("Referer");
    }
    if (source == null || source.isEmpty()) {
      return false;
    }
    try {
      URI u = URI.create(source);
      int port = u.getPort() != -1 ? u.getPort() : defaultPort(u.getScheme());
      return request.getScheme().equalsIgnoreCase(u.getScheme())
          && request.getServerName().equalsIgnoreCase(u.getHost())
          && request.getServerPort() == port;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static int defaultPort(String scheme) {
    return "https".equalsIgnoreCase(scheme) ? 443 : 80;
  }
}
