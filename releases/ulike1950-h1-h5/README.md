ULike v1.9.50／Hiro Morphe Patches v1.0.183

H1～H5：主ノイズ除去のネイティブ化、残留ノイズのGPU仕上げ、転送・同期集約、作業データ共有、検証結果の再利用を統合しました。基準の1.9.49と同じ画素演算、画質設定、保存解像度・形式・圧縮品質、撮影・保存順序と工程別時間記録を維持します。

実際のネイティブC/JNI、MesaソフトウェアGPUシェーダー、ホスト回帰試験、元APKSへの単体・総合版適用を確認済みです。Galaxy実機の速度・画質は未確認です。詳しくは RELEASE_NOTES.txt と QA_ULike_v1.9.50.json を参照してください。

再ビルド例:

    python3 src/build1950.py --input INPUT --tools TOOLS --ndk NDK_R27C --work BUILD --output DIST --jdk JDK21
    python3 src/validate1950.py --original ORIGINAL_APKS --build BUILD --dist DIST --tools TOOLS --work VALIDATION --jdk JDK21
    python3 src/finalize1950.py --dist DIST --source src --validation VALIDATION/validation.json

INPUTにはmanifest.jsonの固定SHA256に対応する基準の単体・総合MPPを置きます。TOOLSは固定チェックサムのmorphe.jar、android.jar、d8.jar、JDKはTemurin21.0.8+9-LTSを使います。ネイティブライブラリはAndroid NDK r27cでビルドします。ホストシェーダー試験にはMesa EGL/OpenGL ESが必要です。

Morphe Managerのソース更新後、未改造 ULike 5.6.2（740）へパッチを再適用し、作成されたアプリを更新インストールしてください。ソース更新だけではインストール済みアプリは変わりません。
