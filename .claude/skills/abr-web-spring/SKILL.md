---
name: abr-web-spring
description: abr-web-spring（住居表示-住居マスターを検索・一覧・編集・削除する Java・Spring Boot・MyBatis のアプリ）で、機能の追加・修正・テスト・動作確認・ドキュメント更新をするときの手順と、これまでに分かった落とし穴。検索条件や画面の追加、record・Mapper（SQL）・フォーム・ユースケース・コントローラーのどこに書くかの判断、Maven や npm のパッケージの追加に使う。
---

# abr-web-spring の作業手順と知見

## 前提

- Java 25（Temurin 25.0.5.1、mise の global）＋ Spring Boot 4.1.2 ＋ MyBatis（mybatis-spring-boot-starter 4.1.0、MyBatis 3.5.19）＋ Thymeleaf 3.1 ＋ Bean Validation（Hibernate Validator 9.1）＋ MySQL Connector/J 9.7 ＋ JUnit 6
- ビルドは Maven Wrapper（`./mvnw`、Maven 3.10.0）。画面の JS・CSS は npm（Vite 8.3.6・Tailwind 4.3.5・Stimulus 3.2.2）
- DB は **abr-batch の MySQL を共用**（ポート 3307、テーブル `mt_rsdtdsp_rsdt`、1,752,573 件）。開発とテストは同じ DB（`abr`）
- 勉強用に、同じものを TS・Rails・Laravel・Symfony でも作った。機能・画面・デザイン・ユースケースの名前（`SearchRsdt`・`GetRsdt`・`UpdateRsdt`・`DeleteRsdt`）をそろえる
- サーバーは 127.0.0.1:3000（ほかの版と同じ）

## してはいけないこと（DB を共用しているため）

- **Flyway・Liquibase・Spring Data JPA（Hibernate）を入れない。** 表を作ったり変えたりする機能がある。入れるならユーザーの了解を取る。入っていないことは `SchemaSafetyTest` が確かめる
- `spring.sql.init.mode=never` を変えない。`schema.sql`・`data.sql` を置かない
- DDL（`CREATE`・`ALTER`・`DROP`・`TRUNCATE`）を Mapper に書かない

## 書く場所の決まり

| 場所 | 書くもの |
|---|---|
| `rsdt/Rsdt.java` | 1 行の record（22 列）、17 列の決まり（`ATTRIBUTE_RULES`）、住所（`address()`）、フォームに出す値（`formValues()`） |
| `rsdt/RsdtKey.java`・`RsdtValues.java` | 主キーと URL の id の変換、更新に使う 17 列 |
| `rsdt/RsdtMapper.java` ＋ `resources/mapper/RsdtMapper.xml` | SQL はここだけ。検索条件は `<sql id="whereSearch">` |
| `search/` | 検索条件（LIKE のエスケープ `likeXxx()` も）と、一覧の URL ⇔ 状態 |
| `usecase/` | ユースケース（`call` を持つ `@Service`）。Mapper をコンストラクターで受け取る。書き込みは `@Transactional` |
| `web/RsdtForm.java`・`IsoDate.java` | 入力チェック（Bean Validation の注釈）とエラーの文 |
| `web/RsdtController.java` | パラメーターを読む → ユースケースを呼ぶ → 表示・redirect だけ |
| `templates/`・`frontend/` | Thymeleaf と Stimulus・CSS |

- 1 行は record（変えられない）。値を変えるときは `RsdtValues` を作って UPDATE に渡す
- 入力エラーのときは、redirect せずに編集画面を 422 で出す
- フォームの項目名は Java のプロパティ名（`oazaCho`）。画面に出す列名と、確認モーダルの `data-column` は列名（`oaza_cho`）

### 検索条件を足すとき

1. `search/RsdtSearchCriteria.java` の項目（部分一致なら `likeXxx()` も）
2. `resources/mapper/RsdtMapper.xml` の `whereSearch`
3. `search/RsdtListQuery.java` の `fromParams`・`toParams`（URL の名前と上限の文字数）
4. `templates/rsdt/index.html` の検索フォーム
5. テスト（`RsdtListQueryTest`・`SearchRsdtTest`・`RsdtControllerTest`）と README

部分一致か完全一致かは、値を見てから決める（`blk_num`・`rsdt_num` は全件が半角数字なので完全一致）。

## テストと確認

```bash
./mvnw test             # 67 件、約 30 秒（コンパイルを含む）
./mvnw spotless:check   # 指摘 0 件を保つ。直すときは ./mvnw spotless:apply
npm run build           # JS・CSS のビルドが通るか
```

- DB を使うテストは `@Transactional` で 1 件ごとに取り消す。MockMvc は同じスレッドで動くので、画面のテストの書き込みも取り消される
- テストで使う行は [RsdtFixtures](../../../src/test/java/dev/abr/web/RsdtFixtures.java)（`EXISTING_ID`・`MISSING_ID`・`TOTAL_ROWS`）
- 画面のテストで更新・削除を送るときは、`header("Origin", "http://localhost")` を付ける（`SameOriginFilter` のため）
- MockMvc の `param()` は、同じ名前を足していく。上書きしたいときは、先に Map にまとめてから渡す（2 つ送ると "a,b" として読まれ、日付のチェックに落ちた）
- テストは「全件 1,752,573 件」を前提にしている。件数が違うと落ちるので、先に DB を元に戻す
- 確認のあとは、DB が元のままか必ず確かめる。変わっていたら abr-batch で `npm run import:v3`（約 15 秒）で戻す

```bash
docker compose -f ../abr-batch/compose.yaml exec -T db mysql -uabr -pabr abr -e 'select count(*) from mt_rsdtdsp_rsdt; checksum table mt_rsdtdsp_rsdt'
# 元のデータ：1752573 件、チェックサム 967527444
```

### 画面の確認（ブラウザが無い環境）

- 一覧・編集画面は `curl` で HTML を取って `grep` する（タグが複数行になるので `tr '\n' ' '` でつなぐ）
- 更新・削除は curl で送れる（CSRF のトークンは無い。`Origin` ヘッダーを付ける）。成功する更新・削除は本物の DB を書き換えるので、送ったら DB を元に戻す

```bash
# 無い行の削除（DB は変わらない）→ 302、Location が戻り先
curl -s -o /dev/null -D - -X POST 'http://127.0.0.1:3000/rsdt/131016_0001001_006_999_99999?back=/' \
  -H 'Origin: http://127.0.0.1:3000' --data '_method=delete'
# 入力エラー（DB は変わらない）→ 422 と、エラーの文の入った編集画面
curl -s -o /dev/null -w '%{http_code}\n' -X POST 'http://127.0.0.1:3000/rsdt/131016_0001001_006_002_?back=/' \
  -H 'Origin: http://127.0.0.1:3000' --data '_method=patch&city=&efctDate=2026-02-30'
```

- 見た目・モーダルの動きは確かめられないので、「確かめていない」とユーザーに伝える

### サーバーの起動と停止

```bash
npm run build && ./mvnw spring-boot:run
P=$(ss -ltnp | grep ':3000 ' | grep -o 'pid=[0-9]*' | cut -d= -f2); [ -n "$P" ] && kill $P   # 止める（Maven も一緒に終わる）
```

- Java・Thymeleaf・設定を変えたら、起動し直す（Thymeleaf はテンプレートをキャッシュする）
- `pkill -f` は使わない（そのコマンドを実行しているシェル自身にも当たることがある）

## 分かっている落とし穴

| 症状 | 原因と対処 |
|---|---|
| Spring Initializr が「mybatis は Boot 4.1 と合わない」と言う | Initializr の対応表が古い（Boot 4.0 まで）。MyBatis の README では 4.1.0 が Boot 4.1 に対応。ひな形は MyBatis 無しで作り、pom.xml に足した |
| Windows のブラウザから localhost:3000 を開けない | Java が IPv6 のソケットで待ち受けていた。`-Djava.net.preferIPv4Stack=true`（pom.xml に書いてある）。`ss -ltn` で `127.0.0.1:3000` になっているか見る |
| redirect 先の URL に `;jsessionid=...` が付く | `server.servlet.session.tracking-modes=cookie`（設定済み） |
| 確認モーダルで、空の列が全部「変わった」と出る | Thymeleaf は値が空文字の属性を出さない。`confirm_controller.js` で、`data-original` が無ければ空文字として扱う |
| `th:href="${@bean.method(...)}"` がエラー | Thymeleaf 3.1 の制限。URL はコントローラーで作って渡す（`PagerView`） |
| JS を直したのに動きが変わらない | ビルドのファイル名にハッシュが無いので、ブラウザのキャッシュ。強制再読み込み。起動中なら `target/classes/static/build/` にも写す |
| `@InitBinder` の空白取りが効かないように見えた | 本当の原因は、テストで同じ項目を 2 回送っていたこと（上の MockMvc の注意）。今は `@InitBinder`（名前の指定なし）で、このコントローラーの全部のフォームに効く |
| Spotless が「整形できない」で止まる | google-java-format 1.37.0 は Java 25 で動かなかった（原因は調べていない）。2.0.0 を使う |

## データの性質

- 主キーの URL の形は、5 列を `_` でつないだもの（例：`131016_0001001_006_001_`）。`rsdt2_id` が空（NULL ではなく `''`）なので末尾が `_`。`split("_", -1)` で末尾の空文字も残す
- `chome` は「一丁目」と「３丁目」が混ざっている。照合順序で全角と半角は同じになるが、漢数字は別
- 部分一致はインデックスが効かず、件数を数えるのに全件を読む

## 書いてはいけないこと

- **列の日本語名を書かない。** 画面の項目名は CSV の列名のまま。ユーザーが言った呼び方（「市区町村」「町字」「丁目」）は使ってよい
- 実際に測っていない数字や、試していない動きを、確かめたこととして書かない

## パッケージの追加

- 何を入れるか伝えて、了解を取ってから入れる
- Maven は pom.xml に版を書く（Spring Boot が管理しているものは、版を書かずに Boot に任せる）。npm は `npm install -E <pkg>@<版> --before=<日付>`
- できれば出てから 2 週間以上たった版にする

## ドキュメントとコミット

- README は入口（できること・動かし方・構成・テスト・作ってみて分かったこと）。起動と停止は `docs/start-stop.md`
- コミットと push は、ユーザーの了解を取ってから。メッセージは日本語で、最後に `Co-Authored-By` の行を付ける
- `target/`・`node_modules/`・`src/main/resources/static/build/` は Git に入れない（`.gitignore` 済み）
