package dev.abr.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * 共用 DB を壊す仕組みが入っていないことを確かめる
 *
 * <p>DB は abr-batch と共用しているので、表を作り直す道具（Flyway・Liquibase）を入れない。起動時に SQL のファイルも流さない。
 */
@SpringBootTest
class SchemaSafetyTest {

  @Autowired Environment env;

  @Test
  void 表を作り直す道具が入っていない() {
    for (String c :
        new String[] {
          "org.flywaydb.core.Flyway", "liquibase.Liquibase", "org.hibernate.SessionFactory"
        }) {
      assertThat(isPresent(c)).as(c + " が入っている").isFalse();
    }
  }

  @Test
  void 起動時にSQLのファイルを流さない() {
    assertThat(env.getProperty("spring.sql.init.mode")).isEqualTo("never");
  }

  private static boolean isPresent(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
