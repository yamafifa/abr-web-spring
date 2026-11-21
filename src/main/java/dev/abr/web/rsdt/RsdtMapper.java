package dev.abr.web.rsdt;

import dev.abr.web.search.RsdtSearchCriteria;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 住居の SQL（中身は src/main/resources/mapper/RsdtMapper.xml）
 *
 * <p>MyBatis がこのインターフェースの実装を作る。SQL を組み立てるのはここだけ。
 */
@Mapper
public interface RsdtMapper {

  Optional<Rsdt> findByKey(@Param("key") RsdtKey key);

  /** 行をロックして取る（SELECT ... FOR UPDATE）。トランザクションの中で使う */
  Optional<Rsdt> findByKeyForUpdate(@Param("key") RsdtKey key);

  long count(@Param("c") RsdtSearchCriteria criteria);

  /** 検索条件に合う行を、主キーの順に offset 件目から limit 件 */
  List<Rsdt> search(
      @Param("c") RsdtSearchCriteria criteria,
      @Param("offset") int offset,
      @Param("limit") int limit);

  /** 主キー以外の 17 列を更新する。更新した件数を返す */
  int update(@Param("key") RsdtKey key, @Param("v") RsdtValues values);

  /** 削除した件数を返す */
  int delete(@Param("key") RsdtKey key);
}
