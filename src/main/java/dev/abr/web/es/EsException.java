package dev.abr.web.es;

/** Elasticsearch に接続できない・エラーが返ったとき。notFound は 404（インデックスが無いなど） */
public class EsException extends RuntimeException {

  private final boolean notFound;

  public EsException(String message, boolean notFound) {
    super(message);
    this.notFound = notFound;
  }

  public boolean notFound() {
    return notFound;
  }
}
