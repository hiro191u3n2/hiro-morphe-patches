# MX Player Pro タグ行非表示 v1.0.0

MX Player Pro タグ一覧非表示 v1.0.0
統合版: Hiro Morphe Patches v1.0.358
公開日: 2026-11-06

対象: MX Player Pro 1.93.4 (versionCode 2001002584)
パッケージ: com.mxtech.videoplayer.pro
パッチ名: フォルダ画面のタグ一覧を非表示

フォルダ画面上部のMusic / Privacy / MXShare / Video Playlistなどが並ぶ
横一列を、上下の余白ごと非表示にします。

使い方
1. Morphe ManagerのHiro Morphe Patchesをv1.0.358に更新。
2. 元のMX Player Pro 1.93.4のAPK/APKSを選択。
3. 「フォルダ画面のタグ一覧を非表示」を選択してパッチを実行。
4. 作成されたアプリをインストール。
単独MPPを読み込む場合も選ぶパッチは同じです。
ソースの更新だけではインストール済みアプリに変更は反映されません。

検証範囲
・添付APKに対するパッチ適用とAPK再構築。
・単独MPP／統合MPP双方からのパッチ読込。
・変更はタグ行を追加する専用処理への即時return-void 1命令のみ。
・その他のアプリ実行コード、マニフェスト、画像等のリソースを保持。
・既存の統合版パッチと資源を保持。
・Android実機／エミュレーターでの表示・操作確認は未実施。

固定配布ページ
https://hiro-morphe-patches.otoha10.chatgpt.site
更新ソース
https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/main/patches-bundle.json
