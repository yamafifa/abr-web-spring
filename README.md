# abr-web-spring

ABR（アドレス・ベース・レジストリ）の住居表示-住居マスター（`mt_rsdtdsp_rsdt`、東京都 1,752,573 件）を検索して、一覧表示・編集・削除する Web アプリです。
勉強用に、同じものを TypeScript・Ruby on Rails・PHP Laravel・PHP Symfony・Java で作るうちの Java 版です。

- Java 25（Temurin）＋ Spring Boot 4.1.2 ＋ MyBatis（mybatis-spring-boot-starter 4.1.0）＋ Thymeleaf ＋ Bean Validation ＋ Tailwind CSS 4 ＋ Stimulus（Vite）＋ JUnit 6
- ビルドは Maven（`./mvnw`。Maven 3.10.0 を初回に `~/.m2` に落とす）
- 構成は「Controller → ユースケース → Mapper（SQL）→ record」。ユースケースの名前は、ほかの版とそろえています
- DB は [abr-batch](https://github.com/yamafifa/abr-batch) の MySQL を共用します（テーブルも abr-batch で作ったもの）

## できること

ほかの版と同じです。

- **検索**：市区町村（`city`・`ward`）、町字（`oaza_cho`・`koaza`）、丁目（`chome`）の部分一致と、`blk_num`・`rsdt_num` の完全一致。条件は URL に残ります
- **一覧**：主キー順。1 ページ 15 件（30・50・100 件も選べます）。ページャー付き
- **編集**：主キー以外の 17 列。「更新」を押すと、変わった項目をモーダルで見せてから保存します
- **削除**：一覧と編集画面から。モーダルで確認してから削除します

## 動かし方

1. abr-batch で DB を起動して、データを入れておく（abr-batch の README「動かし方」を参照）
2. このリポジトリで：

```bash
npm install
npm run build              # JS と CSS を src/main/resources/static/build/ に作る
./mvnw spring-boot:run     # http://127.0.0.1:3000
```

- 起動と停止の詳しいやり方は [docs/start-stop.md](docs/start-stop.md) にあります
- 編集・削除したデータは、abr-batch で `npm run import:v3`（約 15 秒）を実行すると元に戻せます
- **表を作り直す道具（Flyway・Liquibase）は入れていません。** MyBatis は SQL を自分で書くので、表を勝手に作ったり変えたりしません。起動時に SQL のファイルも流しません（`spring.sql.init.mode=never`）。入っていないことはテスト（`SchemaSafetyTest`）で確かめています

## 構成

| 場所 | 内容 |
|---|---|
| `src/main/java/dev/abr/web/rsdt/Rsdt.java` | 1 行を表す record（22 列）。17 列の決まり（`ATTRIBUTE_RULES`）、住所の表示 |
| `src/main/java/dev/abr/web/rsdt/RsdtKey.java` | 主キー（5 列）と、URL の id ⇔ 主キーの変換 |
| `src/main/java/dev/abr/web/rsdt/RsdtMapper.java`・`src/main/resources/mapper/RsdtMapper.xml` | MyBatis。SQL は XML に書く（検索の条件は `<if>` で組み立てる） |
| `src/main/java/dev/abr/web/search/` | 検索条件（`RsdtSearchCriteria`。LIKE のエスケープもここ）と、一覧の URL ⇔ 検索条件・件数・ページ（`RsdtListQuery`） |
| `src/main/java/dev/abr/web/usecase/` | ユースケース：`SearchRsdt`（ページ番号の補正もここ）・`GetRsdt`・`UpdateRsdt`・`DeleteRsdt` |
| `src/main/java/dev/abr/web/web/RsdtForm.java`・`IsoDate.java` | 編集フォームと入力チェック（Bean Validation）。日付の決まりは自分で作った |
| `src/main/java/dev/abr/web/web/RsdtController.java` | パラメーターを読む → ユースケースを呼ぶ → 表示・redirect。戻り先のチェックもここ |
| `src/main/java/dev/abr/web/web/SameOriginFilter.java` | CSRF 対策（なりすまし送信を防ぐ）。データを変えるリクエストは、同じサイトから送られたものだけを通す |
| `src/main/java/dev/abr/web/web/Pager.java`・`PagerView.java` | ページャーに出すページ番号と、そのリンクの URL |
| `src/main/resources/templates/` | Thymeleaf：レイアウト（お知らせを含む）・部品（確認モーダル・削除ボタン・ページャー）・一覧・編集・404 |
| `frontend/` | Stimulus（確認モーダル・お知らせを消す・件数を変えたら送る。Rails 版と同じもの）と CSS。`npm run build` で Vite がまとめる |

ほかの版との違い：

- **SQL を自分で書きます**（MyBatis）。ほかの版は ORM（Prisma・Active Record・Eloquent・Doctrine）が SQL を作ります。複合主キーも、WHERE に 5 列を書くだけです
- **1 行は record（変えられないクラス）です。** 更新は、17 列をまとめた `RsdtValues` を作って、UPDATE 文に渡します
- 入力エラーのときは、redirect せずに編集画面を 422 で出します（Symfony 版・Rails 版と同じ）。フォームの項目名は Java のプロパティ名（`oazaCho` など）です
- **CSRF 対策は自分で書きました。** Spring Security は、ログインの無いアプリには大きすぎるので入れていません。ブラウザがフォームに付ける `Origin` ヘッダー（送り元のサイト）を見て、別のサイトや送り元の分からないリクエストは 403 にします（Symfony 8 の既定と同じ考え方）
- 前後の空白を取る・空文字を null にするのは、コントローラーの `@InitBinder`（`StringTrimmerEditor`）がやります

## テスト

```bash
./mvnw test             # 67 件、約 30 秒（コンパイルを含む）
./mvnw spotless:check   # 書き方のチェック（直すときは ./mvnw spotless:apply。google-java-format）
```

- `RsdtKeyTest`・`RsdtListQueryTest`・`PagerTest`・`SameOriginFilterTest` は Spring を起動しないテストです
- `SearchRsdtTest`・`UpdateDeleteRsdtTest` はユースケースを本物の DB で試すテスト、`RsdtControllerTest` は画面を通したテスト（MockMvc）です
- DB を使うテストは、1 件ごとに `@Transactional` で取り消すので、DB の中身は変わりません
- テストも開発も同じ DB（`abr`）を使います

## 作ってみて分かったこと

- **Spring Initializr は、MyBatis を Spring Boot 4.0 までしか選ばせなかった**：MyBatis 側の説明では 4.1.0 が Boot 4.1 に対応しているので、MyBatis を外してひな形を作り、`pom.xml` に手で足しました。また、Initializr では Boot 4.1.2 を選べなかったので、4.1.3 で作ってから版を書き換えました
- **MyBatis は record にも結果を入れられた**：`arg-name-based-constructor-auto-mapping` を有効にすると、列名（camelCase に変換したもの）と同じ名前のコンストラクター引数に値を渡します。XML の中の `#{c.city}` も、record の `city()` を読めました（テストで確かめた）
- **Windows のブラウザから開けなかった**：Java は既定で IPv6 のソケットで待ち受け（`[::ffff:127.0.0.1]:3000`）、WSL の中の curl では開けたのに、Windows の Chrome からは開けませんでした。`-Djava.net.preferIPv4Stack=true` を付けたら開けました（`pom.xml` の spring-boot-maven-plugin に書いた）
- **初めての redirect で URL に `;jsessionid=...` が付いた**：お知らせをセッションに入れたとき、まだ Cookie が無いので、Tomcat が URL にセッション ID を付けました。`server.servlet.session.tracking-modes=cookie` で止めました
- **Thymeleaf は、値が空文字の属性を出さない**：`data-original=""` が出ず、確認モーダルで空の列が全部「変わった」と出ました。JS の側で、属性が無ければ空文字として扱うようにしました
- **Thymeleaf 3.1 は、`th:href` の中で Bean のメソッドを呼べない**：「この場所では使えない」というエラーになったので、ページャーの URL はコントローラーで作って渡しました
- **Vite の出力のファイル名にハッシュを付けていないので、ブラウザが古い JS を使い続けた**：直したあとも動きが変わらず、強制的に読み直したら直りました。開発中に JS を変えたら、ブラウザで強制再読み込み（Ctrl + Shift + R）をします
- **google-java-format 1.37.0 は、この環境（Java 25）で動かなかった**：Spotless が「整形できない」とだけ言って止まりました。原因は調べていません。2.0.0 にしたら動きました
- **npm は、依存パッケージに数日前の版を選ぶ**：`npm install --before=2026-11-07` で入れ直しました
- **データの性質**（ほかの版と同じ）：丁目は「一丁目」と「３丁目」の書き方が混ざっていて、「3」で「３丁目」は見つかりますが、「三丁目」は見つかりません。画面の項目名は CSV の列名のままです
