# ULike v1.9.99 / Hiro Morphe Patches v1.0.232

公開済み v1.9.98 / v1.0.231 の単体・総合 MPP を SHA-256 で固定して更新する。

## 再構築

JDK Temurin 21.0.8+9、Android NDK r27c、Morphe Desktop 1.16.0、Android 35 / D8 の既存固定値を使用する。Python 3.12系と NumPy 2.3.5 を明示して用意する。

1. `src/declare1999.py --roots src/production1999.json --output manifest.json` でレビュー済みの本番ヘルパー一覧とソースを固定する。
2. `src/build1999.py --input input --tools tools --work work/build --output dist --jdk tools/jdk-21.0.8+9 --ndk tools/android-ndk-r27c --declaration manifest.json` で単体・総合 MPP を構築する。
3. `src/host_audit1999.py --source src --work work/host --jdk tools/jdk-21.0.8+9 --ndk tools/android-ndk-r27c` で現在の本番コードに対するホスト試験を実行する。
4. `src/apply_original1999.py --input ORIGINAL.apks --mpp dist/ULike_HQ_Texture_Online_v1.9.99.mpp --work work/application --jdk tools/jdk-21.0.8+9 --tools tools` で固定ハッシュの私有原本 APKS に完成 MPP を適用し、FULL DEX・リソース再構築・arm64・既存 native 保持・全ヘルパー参照を検証する。
5. `src/finalize1999.py --dist dist --declaration manifest.json --host-report work/host/result.json --application-report work/application/application-report.json --notes RELEASE_NOTES.txt` で現在の証拠を MPP のハッシュに結び付け、QA・ソース ZIP・SHA256SUMS を生成する。
6. `src/validate1999.py --root . --host-report work/host/result.json --application-report work/application/application-report.json --output work/validation1999.json` で公開物を独立に検証する。
7. `publication/publish1999.py --dist dist --repo REPOSITORY --validation work/validation1999.json --local-only`、`--preflight-only` を実行後、同じ引数からフラグを外して公開する。

## 保持範囲

レビューした本番 Java ヘルパー群を再構築する。その他の runtime クラス、他アプリ、11 個の既存 native、既存の原本 method 契約を保持する。v1.9.98 のシャッター onTouchEvent 計測を変換・逆変換で検証し、methods.dex と methods.tsv はバイト単位で保持する。既存 timing/camera observer は正確な位置に再適用する。

47件の native／shader／CPU 依存ソース、コンパイラ引数、依存分類 manifest を v1.9.98 の SHA-256 に固定する。GPU native に埋め込まれた Java 計算経路の source fingerprint は現在のソースから再生成する。Java の候補選択・計算経路は分類 manifest の numerical_java に含まれ、今回の変更も fingerprint 更新の対象になる。診断表示だけの ProcessingTiming1947 は既存分類で対象外であり、数値経路の変更と区別する。古い撮影条件の認定を新しい実装へ誤用しないため、この fingerprint を更新する。それに伴う GNU build-id と文字列参照の再配置も変わり得る。文字列プールと命令内参照の再配置があり得るため、ELF の許容マスクを広げず、同じ NDK で逆ビルドする。生成ヘッダーが全 shader と現在の fingerprint から厳密に再現できることを確認し、fingerprint 定義の1リテラルだけを v1.9.98 の値へ戻す。同一のコンパイラ呼出しを現在のヘッダーでも繰り返し、完成 MPP に含める .99 ELF と完全一致することを確認する。その他すべての生成ヘッダー・ソース・コンパイル／リンク引数を保持した逆ビルド結果は、公開済み v1.9.98 の ELF 全体とバイト単位で一致することを必須にする。全 shader、GPU/CPU 演算ソース、全 JNI 宣言/export を保持する。native installer は GPU 1行のハッシュ・サイズだけを更新し、逆変換でその他の実装を保持する。

今回の変更は撮影IDと保存対象 Bitmap の対応、前段GPUの認定失効と検証予約、通常撮影と同じ補正経路を基準にした連結候補の比較を対象とする。入力サイズ・実処理画素数・撮影メタデータの欠落や曖昧さを明示し、出力サイズだけで入力解像度の一致や速度改善を判断しない。保存済みの写真へ別撮影の観測を後付けしない。

前段GPUと連結候補は従来の全画素一致・信頼度一致・速度条件を満たした後だけ本番採用する。起動時の標準GPU、CPU代替、使用中領域の所有権、撮影優先の中断・解放契約を保持する。前段の画像コピーは検証枠の予約後に実施する。連結候補は通常撮影と同じ32行／128行経路で比較し、基準状態の変化で古い認定を失効させる。比較の進行をCPU基準・標準GPU・連結候補へ分けて記録する。

補正ポリシーの保護マスク再利用、強ノイズ追加候補、直近16枚の保存成功履歴、128行認定復元表示の既存検証を残す。今回の修正から得られるホスト証拠を追加するが、過去の実行結果を新しい検証結果として扱わない。

通常撮影のメモリ回収は未使用領域だけを対象とし、実行中・所有中の領域を解放しない。固定の予備 64 MiB と既存の上限を維持する。本番補正のメモリ不足を検出した撮影では仕上げ候補の追加画像コピーを見送る。それ以外の検証予約は維持する。起動時の標準 GPU 実出力照合、既存の認定条件、CPU 代替、保存画質・解像度・HEIF条件は維持する。

ProcessingTiming1947.VERSION は宣言した単体 MPP の Version と完全一致を必須とし、ソース、構築 DEX、実適用した APK、最終 QA のそれぞれで照合する。ログ全体のコピー機能と既存メニュー機能も現在のコードで再検証する。

各工程の変更一覧・実行したホスト試験・未測定の項目は生成 QA に記録する。過去の試験回数を今回の実行回数に加えない。Android 実機の初回 GPU 採用・画質・保存速度はホスト試験では確定しない。2,000 ms 以下の達成は未測定。

## 公開処理

Release の draft 資産を検証した後に公開し、固定 SHA の配布ブランチから MPP を再取得して一致を確認する。Manager の main/dev は現在の両 HEAD を照合し、独立した index で他の変更を保持して atomic push する。1 MiB を超える検証履歴は同じ固定 commit の raw メディアで再取得し、サイズと Git blob SHA を照合する。途中失敗時は receipt を残し、既存の Release や配布ブランチを自動上書きしない。

単一メソッドだけの固定 DEX シードを変換・逆変換の検証に用い、無関係な原本メソッドを含めない。利用者の写真、原本 APKS、生成 APK、私有ログ、ツールチェーンは公開しない。
