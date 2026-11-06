microG Maps 起動時現在地 v1.0.0
Hiro Morphe Patches v1.0.360

対象
・添付の microG Maps 26.36.04.973607363（グーグルマップ v2.8.5）
・versionCode: 1068763346
・パッケージ: org.ungoogled.android.apps.maps.microg
・前回添付された microG Maps v2.8.5 の元APKを使用してください。

パッチ名
起動時に現在地を表示

動作
アイコンから通常起動した際、Maps本来の現在地表示を使って地図を現在地へ移動します。
位置情報がまだ取得できていない場合は最大60秒待ちます。取得前に画面へ触れた場合は、
自動移動を中止して操作を優先します。
外部リンクで指定された場所や、経路表示・ナビの画面はそのまま表示します。
GPS・Androidの位置情報設定・権限を自動変更する機能ではありません。

使い方
1. Morphe ManagerでHiro Morphe Patchesをv1.0.360へ更新するか、
   Maps_Startup_Location_v1.0.0.mppを読み込みます。
2. 前回のmicroG Maps v2.8.5の元APKを選択します。
3. 「起動時に現在地を表示」を選び、パッチを適用します。
4. 作成したAPKをインストールします。
5. Androidの位置情報をオンにし、このアプリへの位置情報使用を許可して起動します。

カテゴリ行も非表示にする場合
統合版には従来の「検索欄の下のカテゴリ行を非表示」も収録しています。
元APKへ2項目を選んで適用してください。
単独版は今回の起動時現在地の項目だけを含みます。
単独版と統合版を重複して選択する必要はありません。

検証範囲
検証した入力、MPPの識別値、コード・資源の保持、パッチ適用結果は
QA_Maps_Startup_Location_v1.0.0.jsonに記録します。
Android実機・エミュレーター上での現在地移動は未確認です。
端末上のMorphe更新マークも未確認です。
Managerで項目を見つけられるよう、通常版とmicroG版の両パッケージ名を登録しています。
適用対象は上記の元の添付APKです。版番号だけが一致する通常版APKへの対応を意味しません。
対象コードが異なる場合や、この起動時現在地パッチを重ねて適用した場合は検査で中止します。
ソースの更新だけではインストール済みアプリは変わりません。

固定配布ページ
https://hiro-morphe-patches.otoha10.chatgpt.site

Morphe Manager更新ソース
https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/main/patches-bundle.json
