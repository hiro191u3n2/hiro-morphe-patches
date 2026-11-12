# Yahoo!乗換案内 v1.2.6 / Hiro Morphe Patches v1.0.372

Yahoo!乗換案内8.5.9で「広告・不要欄を非表示」を適用すると、
`NearMeLayout.apply` が「検索結果の画面を確認できません。」で停止する問題への修正版です。

## 原因と変更

Patcherの必要時リソース展開では、`get("res", false)`から得たディレクトリには
展開済みのファイルしかありません。旧ヘルパーは対象画面を展開せずに列挙していたため、
APK内に存在する`fragment_search_result_list.xml`を見つけられませんでした。

レイアウトと文字列リソースをパッチ全体の先頭で明示的に展開してから、
従来の不要欄非表示と画面判定を実行します。後半のNearMe処理だけで展開すると、
先に実行される従来の非表示が対象なしで抜けるため、入口でも展開を保証します。
旧SDKでは展開APIの不在だけを許容し、APIが存在して展開に失敗した場合はエラーを保持します。
画面構造の確認を省略して成功扱いにはしません。

変更対象はパッチ処理用の`NearMeLayout`と、既存処理の先頭に追加する展開呼び出しです。
既存の広告・NearMe・
「発着を入れ替えて再検索」の非表示、経路一覧・再検索・検索条件変更、
Yahoo実行コードの保持処理と他アプリのパッチを維持します。

## 適用方法

1. Morphe ManagerでHiro Morphe Patchesを**1.0.372**へ更新します。
2. 元の未改造Yahoo!乗換案内**8.5.9**のAPK／APKSを選択します。
3. **広告・不要欄を非表示**を再適用し、生成したAPKをインストールします。

ソース更新だけではインストール済みアプリには反映されません。
今回の修正は統合版に含まれます。古いYahoo単独版への更新を案内するものではありません。

配布ページ: https://hiro-morphe-patches.otoha10.chatgpt.site/

Manager更新ソース: https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/main/patches-bundle.json

## 検証と再現

検証結果は`QA_YahooTransit_v1.2.6.json`と同梱の検証記録に保存します。
実APKへのホスト上でのパッチ適用・資源再構築と、Android端末上の表示確認は区別します。
Android実機・エミュレーターの表示・インストール動作は未確認です。

入力は公開配布されたYahoo!乗換案内8.5.9（versionCode482）のbase APKです。
SHA-256: `84b7abec9db2910e80fb7dffa9f14301978059d9e4c3e2445efebd9f530bff4b`。
このbaseはsplit配布の一部で、利用者向けの単体インストールAPKとして配布しません。
原本APK・テスト出力APKは本リリースに含めません。

取得元: https://y.en.aptoide.com/app

上流仕様: https://github.com/MorpheApp/morphe-patcher/blob/d19545e8505e0329cc640723cb8ff91eddd3cd05/src/main/kotlin/app/morphe/patcher/patch/ResourcePatchContext.kt

ビルドは`src/stage_tools.py`で固定ツールを取得後、`src/build.py --help`の
`--base`・`--apk`・`--tools`・`--work`・`--dist`を指定して実行します。
既存の公開版1.0.371のハッシュを確認し、ヘルパーと展開の入口だけを変更して新しい版を作成します。
