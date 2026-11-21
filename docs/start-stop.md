[← README に戻る](../README.md)

# サーバーの起動と停止

## 起動

### 1. DB を起動する（abr-batch のフォルダで）

```bash
cd ~/work/abr-batch
npm run db:up
```

すでに動いていれば不要です。

### 2. アプリを起動する（abr-web-spring のフォルダで）

```bash
cd ~/work/abr-web-spring
npm run build
./mvnw spring-boot:run
```

ブラウザで http://127.0.0.1:3000 を開きます（起動は数秒。初回は Maven とライブラリのダウンロードで時間がかかります）。

- **Java・Thymeleaf・`application.properties` を変えたとき**は、Ctrl + C で止めて、起動し直します
- **JS・CSS（`frontend/`）を変えたとき**は、`npm run build` をもう一度実行してから、アプリを起動し直します
  - ビルドしたファイルは、起動のときに `target/classes` に写されたものが使われるため
  - ファイル名にハッシュを付けていないので、ブラウザで強制再読み込み（Ctrl + Shift + R）もします
- 待ち受けは IPv4 の 127.0.0.1:3000 です（`pom.xml` に `-Djava.net.preferIPv4Stack=true` を書いてある。無いと Windows のブラウザから開けなかった）

## 停止

**自分のターミナルで起動した場合**：そのターミナルで **Ctrl + C** を押します。

**バックグラウンドで動いているものや、どこで動いているか分からない場合**：

```bash
P=$(ss -ltnp | grep ':3000 ' | grep -o 'pid=[0-9]*' | cut -d= -f2); [ -n "$P" ] && kill $P
```

`./mvnw spring-boot:run` は、Maven の中から別の Java（アプリ本体）を起動します。3000 番を使っているアプリ本体を止めると、Maven も終わります（試して確かめた）。

止まったかどうかは、次のコマンドで確かめます。何も表示されなければ止まっています。

```bash
ss -ltn | grep :3000
```

**DB も止めたい場合**（abr-batch のフォルダで）：

```bash
npm run db:down
```

DB を止めてもデータは消えません。

## 注意

- ほかの版（TS・Rails・Laravel・Symfony）も 3000 番ポートを使います。ほかの版が動いていると起動できないので、先に止めてください
- DB が止まっていると、画面を開けません（起動のときに止まるのか、画面を開いたときにエラーになるのかは試していません）
