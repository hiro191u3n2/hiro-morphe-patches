MX Player Pro 起動修正・タグ一覧非表示 v1.0.1
統合版: Hiro Morphe Patches v1.0.359
公開日: 2026-11-06

対象
・MX Player Pro 1.93.4 (versionCode 2001002584)
・パッケージ: com.mxtech.videoplayer.pro
・ARM64版。ネイティブライブラリを含むAPK、または必要な分割APKをそろえたAPKS。
・元のAPK/APKSと、今回確認したv1.0.358適用済みAPKに対応。
・既知のファイルと一致しない版、別ABI、ネイティブ分割APKがない入力は明示的にエラーにします。

変更点
v1.0.358のタグ非表示パッチを適用すると、MX Player本体が再署名とDEX変更を検知し、
ネイティブ処理で異常終了する問題に対応します。
起動・再生・通信に必要な初期化を実行したうえで、該当する異常終了への分岐3か所だけを修正します。
フォルダ画面上部のMusic / Privacy / MXShare / Video Playlistなどのタグ行と上下余白は引き続き非表示です。
すでに非表示を適用した対象APKでは、その処理を二重に追加しません。
アカウント・購入・ライセンスのJavaコード、その他のアプリ用パッチは変更しません。

使い方
1. Morphe ManagerでHiro Morphe Patchesをv1.0.359へ更新します。
   単独MPPを使う場合はMX_Player_Hide_Tags_v1.0.1.mppを読み込みます。
2. 元のMX Player Pro 1.93.4のAPK/APKS、または今回のv1.0.358適用済みAPKを選びます。
3. 従来と同じ「フォルダ画面のタグ一覧を非表示」を選択してパッチを実行します。
   起動修正はこの項目に含まれるため、追加のチェック項目はありません。
4. 作成したAPKをインストールします。同じMorphe Managerの署名鍵を使ってください。
ソースを更新しただけでは、インストール済みアプリには反映されません。

検証範囲
・FULLモード: 元コードの検証用統合APKへ、単独MPPと統合MPPの両方を適用・再構築して合格。
・STRIP_FASTモード: 今回アップロードされた起動不能APKへ、単独MPPと統合MPPの両方を適用・再構築して合格。
・各モードで単独/統合MPPが出力したAPKの全エントリ内容が一致。
・対象外の15,326クラス、4,337リソースを保持。既存のタグ非表示を維持。
・libmx-bh.soの3命令以外を保持。元ファイル・修正後ファイルの全体SHA-256を照合。
・MPP読込、既知の修正済み入力への再適用、別版・破損・ABI不一致の拒否を確認。
・統合MPP内の他アプリ用パッチ・資源を保持。
・Android 17/API 37のエミュレーターを準備しましたが、実行環境のプロセス識別とADB接続の問題で、
  アプリのインストール・起動・動画再生の試験には進めませんでした。実動作の合格は記録していません。
  詳細はRUNTIME_MX_Player_Hide_Tags_v1.0.1.jsonを参照してください。
・利用者の実機での起動・再生、端末上のManager更新マーク表示は未確認です。

検証用APKと元アプリ本体は配布しません。MPPにはパッチの実装を収録しています。
ソースと検証記録: MX_Player_Hide_Tags_v1.0.1_sources.zip

ファイル照合
MX_Player_Hide_Tags_v1.0.1.mpp
  16,445 bytes
  SHA-256: b921dd3faf027a8f75b159d243bb2b6280a87bf18e94ca0fdf5463f774657a08
Hiro_Morphe_Patches_v1.0.359.mpp
  48,288,445 bytes
  SHA-256: 3a7a5bd31fb02c504659e5b8401fdd87825ea9e7b2c53ff6f92b980ca9c79f08

固定配布ページ
https://hiro-morphe-patches.otoha10.chatgpt.site
通常の更新ソース
https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/main/patches-bundle.json
