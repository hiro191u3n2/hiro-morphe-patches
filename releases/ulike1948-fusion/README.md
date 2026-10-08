ULike v1.9.48／Hiro Morphe Patches v1.0.181

M1・M2・M4・M8・M3・M6・M7・M10の合成準備・計算再利用・共有CPU枠内の並列化を採用しました。画質と保存設定を保持し、前版と画素・合成結果をホストで比較します。GPU処理は追加していません。撮影ごとの合成・ノイズ除去・補正・圧縮・保存の時間記録を保持し、表示と保存済み結果のバージョン識別を1.9.48へ更新します。

基準版は ULike v1.9.47／総合版 v1.0.180 です。ネイティブライブラリと他アプリの改造を保持します。ホスト試験と元APKSへの単体・総合版適用は確認済み、Galaxy実機の速度・画質は未確認です。詳細は RELEASE_NOTES.txt と QA_ULike_v1.9.48.json を参照してください。

再ビルド例:

    python3 src/build1948.py --input INPUT --tools TOOLS --work BUILD --output DIST
    python3 src/validate1948.py --original ORIGINAL_APKS --build BUILD --dist DIST --tools TOOLS --work VALIDATION
    python3 src/finalize1948.py --dist DIST --source src --validation VALIDATION/validation.json

INPUTにはmanifest.jsonの固定SHA256に対応する基準の単体・総合MPPを置きます。TOOLSは固定チェックサムのmorphe.jar、android.jar、d8.jar、JDKはTemurin21.0.8+9-LTSを使います。ネイティブライブラリは基準MPPから保持し、NDKは不要です。元APKSの検証結果は両方のMPPのSHA256と一致するものだけを受け付けます。

Morphe Managerのソース更新後、未改造 ULike 5.6.2（740）へパッチを再適用し、作成されたアプリを更新インストールしてください。ソース更新だけではインストール済みアプリは変わりません。
