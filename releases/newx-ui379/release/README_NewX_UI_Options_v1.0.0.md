# NewX UI Options v1.0.0 / Hiro Morphe Patches v1.0.379

X 12.43.0-prod.01向けに、3つの表示設定を追加します。

| 設定 | 動作 |
|---|---|
| チャットのおすすめを非表示 | おすすめ見出し・連絡先同期案内・推薦ユーザー一覧をまとめて隠します。既存の会話と検索は保持します。 |
| 上下のタブを固定 | ホームなどのスクロール時に上部バーと下部タブを表示したままにします。 |
| 120Hzを優先 | Xの画面に120Hzを要求します。同じ解像度の対応モードを優先します。 |

設定場所は **X内のPiko設定 → Hiroの表示設定** です。3項目はそれぞれオン・オフでき、初期状態はすべてオンです。チャットと上下固定の変更後はアプリを再起動してください。120Hzの変更は設定画面からXへ戻った際に適用されます。オフにすると元の表示・リフレッシュレート指定へ戻ります。

120Hzはアプリからの優先要求です。端末の省電力・発熱・画面仕様などによるOSの制限を解除するものではありません。ADB・root・システム全体の設定変更は使用しません。

## 導入

1. Morphe ManagerでHiro Morphe Patchesを **v1.0.379** に更新します。
2. 未改造の **X 12.43.0-prod.01のAPKM一式** を選びます。
3. **Piko 3.12.0-dev.1またはdev.2のNewX用項目** と、Hiroの **「NewX: おすすめ非表示・上下タブ固定・120Hz設定」** を選択します。新しいHiro項目は初期状態では未選択です。
4. パッチを適用し、生成したAPKをインストールします。

ソースの更新だけではインストール済みXは変わりません。Piko側のNewX設定を使用するため、Piko NewXとの同時適用が必要です。単独MPPはこの追加機能だけの配布形式であり、Pikoなしで動作する形式ではありません。総合版と単独版で同じ追加機能を重複選択しないでください。

既存のNewX日本語化・カテゴリ保持・絶対日時・戻る操作の項目も引き続き選べます。古いX向けの同名項目とNewX項目を同時に選ばないでください。

## 検証とソース

実際に完了した検証は `QA_NewX_UI_Options_v1.0.0.json` と `QA_build379.json` に記録します。パッチ適用・APK再構築・コード監査と、Android実機での表示・操作確認は別の検証です。実機の最終表示・動作・120Hz到達は未確認です。

ソースと検証資料は `NewX_UI_Options_v1.0.0_sources_and_QA.zip` に収録します。元APK、改造APK、ユーザーの添付画像・動画、ビルド用コンパイラは公開物に含めません。

## 再ビルド

Java 21とPython 3.12を使用します。ソースZIPを展開したフォルダーに次の入力を用意してください。取得元・バージョン・SHA-256は `toolchain.json` に記載しています。

| フォルダー | 必要な入力 |
|---|---|
| `tools/` | `ecj.jar`、`d8.jar`、`morphe.jar`（Morphe Desktop 2.2.0／Patcher 2.4.0） |
| `inputs/` | `Hiro_Morphe_Patches_v1.0.378.mpp`、`piko-3.12.0-dev.1.mpp`、`piko-3.12.0-dev.2.mpp`、未改造の `X_12.43.0-prod.01.apkm` |

```bash
python3 build.py
python3 integrate.py
python3 verify_packages.py --case dev1
python3 verify_packages.py --case dev2
python3 verify_packages.py --case dev2-clone
python3 verify_packages.py --case standalone
python3 verify_packages.py --case missing-piko
```

`build.py` はパッチ本体と実行拡張をコンパイルし、コンパイル用スタブを配布物から除外します。`integrate.py` はv1.0.378の既存ZIPデータとプロバイダー定義を保持して総合版へ追加します。`verify_packages.py` は元APKMへパッチを適用して再構築します。`missing-piko` はPikoなしの適用が中止されることを確認する負例です。最終APKのコード・リソース監査用ソースは `review/`、機能別のホスト試験は `chat/`、`bars/`、`refresh/` に収録します。

配布ページ: https://hiro-morphe-patches.otoha10.chatgpt.site

更新ソース: https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/main/patches-bundle.json
