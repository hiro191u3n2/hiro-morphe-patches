# ULike v1.9.100 / Hiro Morphe Patches v1.0.233

公開済み v1.9.99 / v1.0.232 の単体・総合 MPP を SHA-256 で固定して更新する。

## 再構築

JDK Temurin 21.0.8+9、Android NDK r27c、Morphe Desktop 1.16.0、Android 35 / D8 の既存固定値を使用する。Python 3.12系と NumPy 2.3.5 を明示して用意する。

1. `src/declare19100.py --roots src/production19100.json --output manifest.json` でレビュー済みの本番ヘルパー一覧とソースを固定する。
2. `src/build19100.py --input input --tools tools --work work/build --output dist --jdk tools/jdk-21.0.8+9 --ndk tools/android-ndk-r27c --declaration manifest.json` で単体・総合 MPP を構築する。
3. `src/host_audit19100.py --source src --work work/host --jdk tools/jdk-21.0.8+9 --ndk tools/android-ndk-r27c` で現在の本番コードに対するホスト試験を実行する。
4. `src/apply_original19100.py --input ORIGINAL.apks --mpp dist/ULike_HQ_Texture_Online_v1.9.100.mpp --work work/application --jdk tools/jdk-21.0.8+9 --tools tools` で固定ハッシュの私有原本 APKS に完成 MPP を適用し、FULL DEX・リソース再構築・arm64・既存 native 保持・全ヘルパー参照を検証する。
5. `src/finalize19100.py --dist dist --declaration manifest.json --host-report work/host/result.json --application-report work/application/application-report.json --notes RELEASE_NOTES.txt` で現在の証拠を MPP のハッシュに結び付け、QA・ソース ZIP・SHA256SUMS を生成する。
6. `src/validate19100.py --root . --host-report work/host/result.json --application-report work/application/application-report.json --output work/validation19100.json` で公開物を独立に検証する。
7. `publication/publish19100.py --dist dist --repo REPOSITORY --validation work/validation19100.json --local-only`、`--preflight-only` を実行後、同じ引数からフラグを外して公開する。

## 保持範囲

レビューした本番 Java ヘルパー群、ノイズ前段 native、GPU native を再構築する。その他の runtime クラス、他アプリ、10 個の既存 native、既存の原本 method 契約を保持する。既存シャッター onTouchEvent 計測を変換・逆変換で検証し、methods.dex と methods.tsv はバイト単位で保持する。既存 timing/camera observer は正確な位置に再適用する。

47件の native／shader／CPU 依存ソースを公開済み v1.9.99 から凍結して同梱する。今回の変更を許すパスはノイズ前段 C、モデル shader、強ノイズ shader、GPU engine C の4件に限定し、そのうち実際に変更したファイルだけをレビュー済み SHA-256 で固定する。その他の native 入力、ビルド手順、コンパイル引数、依存分類 manifest は v1.9.99 と一致を必須にする。現在の shader と計算経路から source fingerprint を生成し、古い実装の認定を新しい実装へ誤用しない。

同じ NDK r27c と固定引数で GPU・NR1955 の2ライブラリを独立に再ビルドし、完成 MPP の全 ELF バイトとの一致を要求する。さらに凍結した v1.9.99 ソースと当時の fingerprint から両ライブラリを逆ビルドし、公開済み v1.9.99 の全 ELF と完全一致することを要求する。ELF の部分マスクで差分を容認しない。この構造証明だけで画素一致を主張せず、現在の本番経路に対する別の画素・中間値回帰試験を必須にする。負例試験は両 ELF、各 native ソース、凍結基準、変更許可、コンパイラ引数、証拠フィールドの破損を拒否する。

既存 JNI 宣言と export を保持し、GPU program 失敗詳細を取得する診断 JNI 1本だけを追加する。native installer は GPU と NR1955 の既存2行のハッシュ・サイズだけを更新し、逆変換で残る10行・12行構成・展開処理を保持する。

今回の変更は前段CPUの重複計算、モデル準備の重複除算・二乗と強ノイズのモデル補間の共用、前段GPU検証の入力保持量と実行時ピーク予算の分離、およびGPU program作成失敗の記録を対象とする。前段の実行時にはメモリ余力を再確認し、各診断を対象区間の正しい撮影記録へ結び付ける。演算順と画素一致の検証、既存の全画素一致・信頼度一致・速度採用条件、CPU代替、所有権、撮影優先の中断・解放を保持する。ホスト試験の時間は実機保存時間として扱わない。

既存32群を現在の本番ソースで再実行する。ソースが意図的に変更された前段の試験は、新しい adapter で旧128画素ケース・16区間境界・15,100計画ケースを保持し、凍結 v1.9.99 と現在の演算結果を比較する追加試験も必須とする。撮影と保存Bitmapの対応、前段GPUの認定失効・予約順序、連結の32行／128行基準選択、保護マスク再利用、強ノイズ候補、保存履歴の検証も保持する。過去の実行結果を今回の試験結果として流用しない。

通常撮影のメモリ回収は未使用領域だけを対象とし、実行中・所有中の領域を解放しない。固定の予備 64 MiB と既存の上限を維持する。本番補正のメモリ不足を検出した撮影では仕上げ候補の追加画像コピーを見送る。それ以外の検証予約は維持する。起動時の標準 GPU 実出力照合、既存の認定条件、CPU 代替、保存画質・解像度・HEIF条件は維持する。

ProcessingTiming1947.VERSION は宣言した単体 MPP の Version と完全一致を必須とし、ソース、構築 DEX、実適用した APK、最終 QA のそれぞれで照合する。ログ全体のコピー機能と既存メニュー機能も現在のコードで再検証する。

各工程の変更一覧・実行したホスト試験・未測定の項目は生成 QA に記録する。過去の試験回数を今回の実行回数に加えない。Android 実機の初回 GPU 採用・画質・保存速度はホスト試験では確定しない。2,000 ms 以下の達成は未測定。

## 公開処理

Release の draft 資産を検証した後に公開し、固定 SHA の配布ブランチから MPP を再取得して一致を確認する。Manager の main/dev は現在の両 HEAD を照合し、独立した index で他の変更を保持して atomic push する。1 MiB を超える検証履歴は同じ固定 commit の raw メディアで再取得し、サイズと Git blob SHA を照合する。途中失敗時は receipt を残し、既存の Release や配布ブランチを自動上書きしない。

単一メソッドだけの固定 DEX シードを変換・逆変換の検証に用い、無関係な原本メソッドを含めない。利用者の写真、原本 APKS、生成 APK、私有ログ、ツールチェーンは公開しない。
