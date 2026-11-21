package dev.abr.web.es;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Elasticsearch の REST API を呼ぶ（専用のライブラリは入れず、Java 標準の HttpClient と、Spring Boot に入っている Jackson を使う）
 *
 * <p>接続先は application.properties の abr.es.url（abr-batch の compose.yaml の http://127.0.0.1:9200）
 */
@Component
public class EsClient {

  private final String url;
  private final ObjectMapper json;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

  public EsClient(@Value("${abr.es.url}") String url, ObjectMapper json) {
    this.url = url.replaceAll("/+$", "");
    this.json = json;
  }

  public JsonNode post(String path, Object body) {
    return send(
        HttpRequest.newBuilder(URI.create(url + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))),
        path);
  }

  public JsonNode get(String path) {
    return send(HttpRequest.newBuilder(URI.create(url + path)).GET(), path);
  }

  private JsonNode send(HttpRequest.Builder req, String path) {
    HttpResponse<String> res;
    try {
      res =
          http.send(
              req.timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new EsException("Elasticsearch に接続できません（" + url + "）: " + e, false);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EsException("Elasticsearch への問い合わせが中断されました", false);
    }
    if (res.statusCode() == 404) {
      throw new EsException("Elasticsearch が 404 を返しました（" + path + "）", true);
    }
    if (res.statusCode() >= 300) {
      String body = res.body();
      throw new EsException(
          "Elasticsearch が "
              + res.statusCode()
              + " を返しました: "
              + body.substring(0, Math.min(300, body.length())),
          false);
    }
    return json.readTree(res.body());
  }
}
