# ULike v1.9.95 / Hiro Morphe Patches v1.0.228

公開済み v1.9.94 / v1.0.227 の単体・総合 MPP を SHA-256 で固定して更新する。

## 再構築

JDK Temurin 21.0.8+9、Android NDK r27c、Morphe Desktop 1.16.0、Android 35 / D8 の既存固定値を使用する。

1. `src/declare1995.py --roots src/production1995.json --output manifest.json` でレビュー済みの本番ヘルパー一覧とソースを固定する。
2. `src/build1995.py --input input --tools tools --work work/build --output dist --jdk tools/jdk-21.0.8+9 --ndk tools/android-ndk-r27c --declaration manifest.json` で単体・総合 MPP を構築する。
3. `src/host_audit1995.py --source src --work work/host --jdk tools/jdk-21.0.8+9 --ndk tools/android-ndk-r27c` で現在の本番コードに対するホスト試験を実行する。
4. `src/apply_original1995.py --input ORIGINAL.apks --mpp dist/ULike_HQ_Texture_Online_v1.9.95.mpp --work work/application --jdk tools/jdk-21.0.8+9 --tools tools` で固定ハッシュの私有原本 APKS に完成 MPP を適用し、FULL DEX・リソース再構築・arm64・既存 native 保持・全ヘルパー参照を検証する。
5. `src/finalize1995.py --dist dist --declaration manifest.json --host-report work/host/result.json --application-report work/application/application-report.json --notes RELEASE_NOTES.txt` で現在の証拠を MPP のハッシュに結び付け、QA・ソース ZIP・SHA256SUMS を生成する。
6. `src/validate1995.py --root . --host-report work/host/result.json --application-report work/application/application-report.json --output work/validation1995.json` で公開物を独立に検証する。
7. `publication/publish1995.py --dist dist --repo REPOSITORY --validation work/validation1995.json --local-only`、`--preflight-only` を実行後、同じ引数からフラグを外して公開する。

## 保持範囲

レビューした本番ヘルパー群と GPU native だけを再構築する。その他の runtime クラス、他アプリ、11 個の既存 native、既存の原本 method 契約をすべて保持する。v1.9.94 のシャッター onTouchEvent 計測も逆変換で検証し、methods.dex と methods.tsv をバイト単位で保持する。既存の timing/camera observer は正確な位置に再適用し、GPU native は同一ソースから再構築して v1.9.94 と完全一致を必須とする。native installer の逆変換を検証し、class バイトも保持する。

既存 JNI の宣言と export を保持し、追加 JNI を許可しない。

ProcessingTiming1947.VERSION は宣言した単体 MPP の Version と完全一致を必須とし、ソース、構築 DEX、実適用した APK、最終 QA のそれぞれで照合する。古い画面ヘッダーの埋込み文字列も拒否する。

各工程の変更一覧・実行したホスト試験・未測定の項目は生成 QA に記録する。昔の試験回数を今回の実行回数に加えない。Android 実機の初回 GPU 採用・画質・保存速度はホスト試験では確定しない。

## 公開処理

Release の draft 資産を検証した後に公開し、固定 SHA の配布ブランチから MPP を再取得して一致を確認する。Manager の main/dev は現在の両 HEAD を照合し、独立した index で他の変更を保持して atomic push する。途中失敗時は receipt を残し、既存の Release や配布ブランチを自動上書きしない。

単一メソッドだけの固定 DEX シードを変換・逆変換の検証に用い、無関係な原本メソッドを含めない。

利用者の写真、原本 APKS、生成 APK、私有ログ、ツールチェーンは公開しない。
