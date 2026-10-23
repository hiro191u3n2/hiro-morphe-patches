# ULike v1.9.114 / Hiro Morphe Patches v1.0.276

公開済み単体v1.9.113と総合版v1.0.275を固定ハッシュで取得し、並行して公開されたHomeBack更新を含む他アプリの変更をバイト単位で保持する。

## 変更範囲

標準Strong profile4の輝度重みとguide内の中心値を再利用する。profile6にも同じguideの共有を適用する。標準の除算方法、サンプル、累積順序と精度を維持し、tiled profile7と他のプログラムは変更しない。追加候補の10%以上の速度条件を緩和しない。

前段nativeのtransformとtransform_neonへrestrictを付け、入力と一時領域の非重複をコンパイラに伝える。native前段と残差GPU入力エクスポーターの全4呼び出しが、別々のWorkspace配列を渡すことを確認する。演算式と順序は変更しない。係数並列化と固定重み表の試作は採用しない。

補正は既存の所有権が確立されたCPUマスク行に対し、元の補間と丸めの上下限が同じ値になる場合に限り定数として利用する。範囲外・非有限値、回転、認識されないコールバックを推測して定数化しない。

## 再構築と検証

Temurin21.0.8+9、Android NDK r27c、固定ハッシュのMorphe Desktop1.16.0・Android35・D8、NumPy2.3.5を使用する。

1. `src/declare19101.py`で全ソース・本番ヘルパー・公開スクリプトを固定する。
2. `src/build19101.py`でGPU1960とNR1955を再構築し、旧公開ELFの同一NDK再構築、変更範囲、JNI ABI、完全DEX、他アプリ保持、ヘルパー参照を確認する。
3. `src/host_audit19101.py`で新旧の全出力・中間値、実JNI/GLES、arm64エミュレーション、既存認定条件、取消、メモリ・所有権の回帰試験を実行する。過去ソースの検査には厳密な逆差分を使い、今回の出力試験は今回の実ソースを実行する。
4. `src/apply_original19101.py`で未改造ULike5.6.2(740)APKSへ完成MPPを新たに適用し、FULL DEX、リソース再構築、arm64、元native保持、適用後メソッドを確認する。
5. `src/finalize19101.py`と`src/validate19101.py`で実行証拠を配布MPPのSHA-256へ結び付ける。旧版の適用証拠は利用しない。
6. `publication/publish19101.py --local-only`、`--preflight-only`後に公開する。19101のスクリプト名と既存schemaは互換のため保持するが、版値は1.9.114/1.0.276で確認する。

Android実機の保存時間と画質は未測定。ホストとエミュレーションの速度はGalaxyの速度を表さない。2秒以下の達成を主張しない。

## 公開

既存タグや配布branchを上書きしない。公開直前のmain/dev、元MPP、ULike_ACTIVEを照合し、競合時は新しい総合版へ再構築する。固定commitの資産を再取得して照合し、main/devへの更新は直接子commitとatomic leaseで行う。

原本APKS・生成APK・個人写真・診断ログ・ツールチェーンは公開せず、ソース・試験・匿名化した構造証拠だけを公開する。
