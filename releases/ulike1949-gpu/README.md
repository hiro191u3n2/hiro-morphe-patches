ULike v1.9.49／Hiro Morphe Patches v1.0.182

G10を基本方針にG1・G3・G5・G6・G9を採用しました。最大4枚の合成準備とノイズ判定の整数集計をGPU計算へ移し、中間データ、周辺参照画素、コンテキスト・計算プログラム・バッファを再利用します。結果一致と転送・同期込みの速度条件が成立しない場合は既存のCPU処理を使います。浮動小数点の合成・画質計算、保存設定、前版のM案、撮影ごとの工程別時間記録を保持します。

基準版は ULike v1.9.48／総合版 v1.0.181 です。既存ネイティブライブラリと他アプリの改造を保持し、専用GPUライブラリを追加します。ホストの実シェーダー計算・回帰試験と元APKSへの単体・総合版適用は確認済み、Galaxy実機の速度・画質は未確認です。詳細は RELEASE_NOTES.txt と QA_ULike_v1.9.49.json を参照してください。

再ビルド例:

    python3 src/build1949.py --input INPUT --tools TOOLS --ndk NDK_R27C --work BUILD --output DIST
    python3 src/validate1949.py --original ORIGINAL_APKS --build BUILD --dist DIST --tools TOOLS --work VALIDATION
    python3 src/finalize1949.py --dist DIST --source src --validation VALIDATION/validation.json

INPUTにはmanifest.jsonの固定SHA256に対応する基準の単体・総合MPPを置きます。TOOLSは固定チェックサムのmorphe.jar、android.jar、d8.jar、JDKはTemurin21.0.8+9-LTSを使います。追加GPUライブラリをAndroid NDK r27cでビルドします。ホストのシェーダー試験にはMesa EGL/OpenGL ESが必要です。元APKSの検証結果は両方のMPPのSHA256と一致するものだけを受け付けます。

Morphe Managerのソース更新後、未改造 ULike 5.6.2（740）へパッチを再適用し、作成されたアプリを更新インストールしてください。ソース更新だけではインストール済みアプリは変わりません。
