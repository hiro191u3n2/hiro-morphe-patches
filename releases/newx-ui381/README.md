# NewX UI Options v1.1.1 / Hiro Morphe Patches v1.0.381

表示回数を全桁表示する際の処理を見直しました。元の省略整形と全桁整形を重ねて実行する処理を整理し、数値フォーマッターの毎回生成を減らしています。スクロール中に繰り返される整形処理の負担を下げる修正です。

**Android実機でのスクロール改善量は未確認です。** ホスト上の試験・パッチ適用・APK再構築・コード監査と、端末上の体感やフレーム時間は区別します。具体的な試験条件と結果は公開QAに記録します。

## 導入

1. Morphe ManagerでHiro Morphe Patchesを **v1.0.381** に更新します。
2. 未改造の **X 12.43.0-prod.01のAPKM一式** を選びます。
3. **Piko 3.12.0-dev.1またはdev.2のNewX用項目** と、Hiroの **「NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）」** を選択します。
4. パッチを適用し、生成したAPKをインストールします。

ソースの更新だけではインストール済みXは変わりません。項目名はv1.1.0と同じです。旧UI設定との重複選択は不要です。総合版と単独版で同じ追加機能を重複選択しないでください。単独MPPもPiko NewXと一緒に使用します。

## 継続する設定

設定場所は **X内のPiko設定 → Hiroの表示設定** です。4項目は個別にオン・オフでき、設定名・保存キー・保存済みの値を変更しません。新規導入時の初期値はすべてオンです。

| 設定 | 動作 |
|---|---|
| 表示回数を省略せず表示 | 元の数値から3,000のような全桁表記へ変更します。省略済み文字列から値を推測しません。いいね・返信・リポスト数は変更しません。 |
| チャットのおすすめを非表示 | おすすめ見出し・連絡先同期案内・推薦ユーザー一覧をまとめて隠します。既存の会話と検索は保持します。 |
| 上下のタブを固定 | ホームなどのスクロール時に上部バーと下部タブを表示したままにします。 |
| 120Hzを優先 | 同じ解像度の対応モードを優先して、Xの画面に120Hzを要求します。 |

表示回数・チャット・上下固定の変更後はXを再起動してください。120Hzの変更は設定画面からXへ戻った際に反映されます。120Hzはアプリからの優先要求であり、省電力・発熱・画面仕様などによるOS側の制限は残ります。ADB・root・システム全体の設定変更は使用しません。

既存のNewX日本語化・カテゴリ保持・絶対日時・戻る操作も引き続き選べます。

## 検証とソース

実際に完了した検証は `QA_NewX_UI_Options_v1.1.1.json` と `QA_build381.json` に記録します。実機の最終表示・操作・120Hz到達は未確認です。

ホストJVMの整形器だけを対象とする試験（Java 21、en-US、50,000回×5回）では、1回の試験で確保したメモリの中央値が旧版59,598,480バイトから新版5,998,480バイトになりました。条件・各測定値はソース内の `views-runtime/benchmark.json` に収録します。この数値はAndroid実機のFPSや操作遅延、ネイティブ側の処理量を測ったものではありません。

ソースと検証資料は `NewX_UI_Options_v1.1.1_sources_and_QA.zip` に収録します。元APK、改造APK、元APKの展開物・解析ダンプ、ユーザーの添付画像・動画、ビルド用コンパイラは公開物に含めません。

## 再ビルド

Java 21とPython 3.12を使用します。取得元・バージョン・SHA-256と正確な入力名は `toolchain.json` に記載しています。

| フォルダー | 必要な入力 |
|---|---|
| `tools/` | `ecj.jar`、`d8.jar`、`morphe.jar`（Morphe Desktop 2.2.0／Patcher 2.4.0） |
| `inputs/` | `Hiro_Morphe_Patches_v1.0.380.mpp`、旧UI設定 `NewX_UI_Options_v1.1.0.mpp`、`piko-3.12.0-dev.1.mpp`、`piko-3.12.0-dev.2.mpp`、未改造の `X_12.43.0-prod.01.apkm` |

```bash
python3 build.py
python3 integrate.py
python3 verify_packages.py --case dev1
python3 verify_packages.py --case dev2
python3 verify_packages.py --case dev2-clone
python3 verify_packages.py --case standalone
python3 verify_packages.py --case missing-piko
```

`integrate.py` はv1.0.380内のUI設定プロバイダー・実行拡張だけを新しい版へ置き換え、無関係なパッチのデータと定義を保持します。コンパイル用スタブは配布MPPから除外します。ホスト試験のソースは各機能フォルダーと `views-runtime/`、最終APK監査用ソースは `review/` に収録します。

配布ページ: https://hiro-morphe-patches.otoha10.chatgpt.site

更新ソース: https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/main/patches-bundle.json
