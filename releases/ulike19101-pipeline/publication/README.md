# ULike v1.9.101 / Hiro Morphe Patches v1.0.234

公開済み v1.9.100 / v1.0.233 の単体・総合 MPP を SHA-256 で固定して更新する。

## 再構築

JDK Temurin 21.0.8+9、Android NDK r27c、Morphe Desktop 1.16.0、Android 35 / D8 の既存固定値を使用する。Python 3.12系と NumPy 2.3.5 を明示して用意する。

1. `src/declare19101.py --roots src/production19101.json --output manifest.json` でレビュー済みの本番ヘルパー一覧とソースを固定する。
2. `src/build19101.py --input input --tools tools --work work/build --output dist --jdk tools/jdk-21.0.8+9 --ndk tools/android-ndk-r27c --declaration manifest.json` で単体・総合 MPP を構築する。
3. `src/host_audit19101.py --source src --work work/host --jdk tools/jdk-21.0.8+9 --ndk tools/android-ndk-r27c` で現在の本番コードに対するホスト試験を実行する。
4. `src/apply_original19101.py --input ORIGINAL.apks --mpp dist/ULike_HQ_Texture_Online_v1.9.101.mpp --work work/application --jdk tools/jdk-21.0.8+9 --tools tools` で固定ハッシュの私有原本 APKS に完成 MPP を適用し、FULL DEX・リソース再構築・arm64・既存 native 保持・全ヘルパー参照を検証する。
5. `src/finalize19101.py --dist dist --declaration manifest.json --host-report work/host/result.json --application-report work/application/application-report.json --notes RELEASE_NOTES.txt` で現在の証拠を MPP のハッシュに結び付け、QA・ソース ZIP・SHA256SUMS を生成する。
6. `src/validate19101.py --root . --host-report work/host/result.json --application-report work/application/application-report.json --output work/validation19101.json` で公開物を独立に検証する。
7. `publication/publish19101.py --dist dist --repo REPOSITORY --validation work/validation19101.json --local-only`、`--preflight-only` を実行後、同じ引数からフラグを外して公開する。

## 変更と保持範囲

今回の変更は保存後検証の診断履歴に限定する。上限付きのスカラー記録を永続化し、プロセス識別、現在と保存時の状態、実際に復元した認定記録数を区別する。診断の保存失敗や破損は画素処理・認定採否・保存処理を止めない。この版より前に失われた履歴は復元できない。

レビュー済みの本番 Java ヘルパーを再構築し、診断専用 VerificationJournal19101 を追加する。他アプリ、既存の原本 method 契約、methods.dex と methods.tsv はバイト単位で保持する。既存シャッター onTouchEvent 計測と timing/camera observer は変換・逆変換で検証する。

47 件の native／shader／CPU 依存ソースは公開済み v1.9.100 と完全一致を必須にする。計算依存の分類 manifest は、新しい診断ヘルパーを除外一覧へ追加する 1 件だけを許可する。既存の計算依存分類を緩めず、現在のソースから fingerprint を生成する。

GPU ライブラリは同じ NDK r27c と固定引数で再構築する。独立した正方向の再ビルドが完成 MPP の全 ELF と一致し、生成 fingerprint だけを v1.9.100 へ戻した逆ビルドが公開済み v1.9.100 の全 ELF と一致することを要求する。部分的な ELF マスクは使わない。NR1955 を含む残り 11 native は公開済み v1.9.100 のバイトを保持する。既存 JNI 宣言と export に追加・変更はなく、installer は GPU の 1 行だけを更新し、11 行と全 12 行の構成を逆変換で保持する。負例試験は native ソース、分類、生成入力、ELF、証拠フィールドの破損を拒否する。

既存 37 群を現在の本番ソースで再実行し、履歴の保存・再起動時の区別・復元件数の追加 3 群と合わせて 40 群を必須にする。画素・中間値一致、信頼度、速度採用条件、CPU 代替、所有権、撮影優先の中断と解放、撮影と保存 Bitmap の対応、補正基準選択などの既存 gate を保持する。過去の試験結果を今回の実行結果として流用しない。

ProcessingTiming1947.VERSION は単体 MPP の Version と完全一致を必須とし、ソース、構築 DEX、実適用 APK、最終 QA で照合する。ログ全体のコピー機能と既存メニュー機能も現在のコードで再検証する。起動時の標準 GPU 実出力照合、保存画質・解像度・HEIF 条件は維持する。

各変更と実行した試験を生成 QA に記録する。ホスト試験の時間を実機保存時間として扱わない。Android 実機での動作・画質・保存速度は未確認であり、速度向上や 2,000 ms 以下の達成は主張しない。

## 公開処理

Release の draft 資産を検証した後に公開し、固定 SHA の配布ブランチから MPP を再取得して一致を確認する。Manager の main/dev は現在の両 HEAD を照合し、独立した index で他の変更を保持して atomic push する。1 MiB を超える検証履歴は同じ固定 commit の raw メディアで再取得し、サイズと Git blob SHA を照合する。途中失敗時は receipt を残し、既存の Release や配布ブランチを自動上書きしない。

単一メソッドだけの固定 DEX シードを変換・逆変換の検証に用い、無関係な原本メソッドを含めない。利用者の写真、原本 APKS、生成 APK、私有ログ、ツールチェーンは公開しない。
