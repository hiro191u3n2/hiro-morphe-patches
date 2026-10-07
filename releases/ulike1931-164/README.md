# ULike 1.9.31 / Hiro Morphe Patches 1.0.164

インカメラで終了した後の再起動時に、撮影プレビューが黒くなる経路への修正です。ULike 1.9.30 と総合版 1.0.163 を固定基準とし、元アプリは ULike 5.6.2（740）、`com.gorgeous.liteinternational` です。

## 確認した不備

元アプリの `onCaptureStarted` はプレビュー開始の戻り値を破棄します。入力が後から届いても、開始要求は自動再送されません。既存 `PreviewStart1927` の回復は背面を対象としていたため、前面では準備前の開始失敗が残りました。

また、元アプリの `TECapturePipeline.isValid` は寸法とリスナーを確認するだけで、`SurfaceTexture` の解放状態は確認しません。既存 `PreviewInputs1929` の再作成も背面に限定されていました。元実装を使ったホスト試験で、この2件を修正前に再現し、修正版で検証しています。

## 修正の範囲

`FrontPreview1931` は現在の前面カメラの所有者・設定・前景世代・ネイティブ処理スレッド・実カメラの同一性を確認します。実際に未準備状態を観測した開始失敗だけに、最大2回・5秒以内の開始要求の再送を行います。Camera1 の `H` と Camera2 の `K` を別々に確認し、Camera2 専用のレンズルートを必須にはしません。

解放済みの OES texture は元アプリの `newSurfaceTexture` フラグを使い、元のプレビュー開始処理に再作成させます。通常の前面 Camera2 モードでは、設定によって有効になる deferred surface を使わず、実際の出力を待ちます。元アプリでこの設定の標準値は false です。

停止・終了・前後切替・モード切替・バックグラウンド移行では保留を取り消します。撮影保存・録画中と表示ウィンドウの未準備中は再送を待機させます。正常開始の戻り値0を受けた後は再送しません。

旧 helper 自体を保持し、背面の回復は従来処理へ委譲します。前後カメラの記憶、前面時の背面レンズUI非表示、黒帯操作、画質・美顔・保存処理、および総合版の他アプリを保持します。provider manager の同一個体再利用という仮説は元アプリの書き込み箇所の監査で成立しないことを確認したため、`ProviderLifecycle1929` は変更していません。

## 検証資料

最終的な結果と配布物の SHA-256 は `QA_ULike_v1.9.31.json`、`manifest.json`、`evidence/validation.json`、公開後の `publication_ULike_v1.9.31.json` で照合します。

- `VerifyFront1931`: 既存 native 5メソッドの bridge 変更、追加5メソッドの取消 prefix、runtime 3メソッドの差分を限定し、その他のメソッド契約を保持。
- `host_front1931.py`: 元 helper と修正版を同じ決定的な native/Android 模型で検証。`front-baseline` に固定基準の原文を含め、旧リリースの隣接ディレクトリに依存しません。
- `VerifyNativeAbi1931`: 新 helper の直接参照・reflection・継承関係を元APKと照合。
- `VerifyFrontAbi1931`: 元APKと再構築APKの native 宣言、texture 再作成フラグ、provider所有権の経路を独立検証。
- `validate1931.py`: 単体・総合の両MPPを元APKSへ適用し、arm64-v8a、APK再構築、DEXヘッダ・署名・チェックサム、全組込メソッド契約、追加コードと変更 native メソッドの型解析を確認。
- `publication`: 配布物の固定ハッシュと公開前検証を必須とし、main/devの配信情報を既存履歴を保持したまま同時更新。

Galaxy実機での撮影画面復帰・撮影保存は未確認です。ホスト試験・APK再構築・DEX解析は、Android/ARTやカメラハードウェア上での実行確認ではありません。

## 再ビルド

JDK21、`build1931.py` の固定ハッシュに一致する Morphe Desktop・Android SDK・D8、および元の単体1.9.30・総合1.0.163を用意してください。

```bash
python3 src/build1931.py --input INPUT_DIR --tools TOOLS_DIR --work BUILD_DIR --output DIST_DIR --jdk JDK21_DIR
python3 src/validate1931.py --original ULike_5.6.2_740.apks --build BUILD_DIR --dist DIST_DIR --tools TOOLS_DIR --work VALIDATION_DIR --jdk JDK21_DIR
python3 src/finalize1931.py --dist DIST_DIR --source src --validation VALIDATION_DIR/validation.json
```

`BUILD_DIR` と `VALIDATION_DIR` は新規ディレクトリを指定します。元APKS・再構築APK・ユーザーのスクリーンショットは公開物に含めません。

## 適用

Morphe Managerでソースを更新し、未改造のULike 5.6.2（740）へ「高画質撮影・質感美肌・素材通信を復旧」を再適用して、生成されたアプリを更新インストールしてください。単体版と総合版はどちらか一方を使用します。ソース更新だけではインストール済みULikeの処理は変わりません。
