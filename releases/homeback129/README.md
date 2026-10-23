# TikTok パッチ適用時のアプリ分岐修正 v1.1.19

TikTok パッチ適用時のアプリ分岐修正 v1.1.19 / Hiro Morphe Patches v1.0.278

TikTok 47.4.3への「戻る1回で終了・履歴削除」適用中に、Instagram専用処理が呼ばれ「Instagram 452 Reels Back method missing」で停止する問題への修正です。複数のアプリが同じ難読化クラス名を持つ場合、従来の処理がそれだけでInstagram 452の処理へ進む条件を修正しました。

対象アプリのパッケージ名を確認してから、InstagramやXなどの専用処理を実行します。TikTokにInstagram用のクラス検査を適用しません。実際のInstagramへ適用する場合の内部構造検査は維持します。

今回変更するのはパッチ適用側です。組み込む戻る処理のDEXは公開済み総合版v1.0.277と完全一致を要求し、TikTokのホーム・プロフィール・コメント欄等の既存動作方針を保持します。他のパッチ、ULike v1.9.114、配布済み資源も保持します。

検証内容は同梱QAに記録します。旧公開版で同じ例外を生むクラス名衝突を検証用DEXで再現し、修正版で対象パッケージごとに正しく分岐することを確認します。全ての既存ホスト回帰試験、DEX構造、単体・総合MPPの読み込み、5アプリ×3パッチャー×2MPPの30通りの合成APK適用・再構築を検査します。24通りの非Instagramケースには同名クラスを含めます。CIの2回の新規ビルドは、ローカルで固定したMPP・QAと完全一致が必要です。

TikTok 47.4.3の実APKは今回未取得のため、実APKへの適用、123パッチの組み合わせ、実機でのインストール・戻る操作・履歴削除は未確認です。合成APKによる適用検証を実機確認として扱いません。

Morphe ManagerのHiro Morphe Patchesをv1.0.278へ更新し、元のTikTok APKへ「戻る1回で終了・履歴削除」を再適用してください。ソースの更新だけではインストール済みTikTokは変わりません。

単体版: OneBack_Exit_NoRecents_v1.1.19.mpp
総合版: Hiro_Morphe_Patches_v1.0.278.mpp
公開先: https://github.com/hiro191u3n2/hiro-morphe-patches/releases/tag/oneback-v1.1.19

## 原因と変更範囲

TikTokへ適用中の例外に `Instagram452Boundaries.reels` が現れた報告に対応します。難読化された `LX/02Hr;` 等のクラス名はアプリ間で同じになることがあります。従来の共通スキャナはアプリの識別前にInstagram専用の検査へ進むため、TikTok内の同名クラスでInstagram用メソッドが見つからず停止しました。

対象パッケージを明示して専用処理を選びます。検証用の同名クラスはTikTokの両パッケージ、X、Trip.comで保持されることを確認し、実際のInstagramの構造不一致に対する停止は保持します。今回の修正はパッチ適用側だけを対象にし、runtimeのソースと配布DEXは前版と同一です。

## 検証記録の意味

`publication/local-scope-validation.json` は、今回のQA、配布MPP、runtime同一性、合成APK検証を結び付けた記録です。TikTok 47.4.3の実APK検証記録ではありません。旧版を使ったエラー再現と修正版の回帰試験は `src/test-dex` の試験から実行します。過去のInstagram実APK証拠を今回の確認結果として移していません。

ビルドと公開準備は `publication/BUILD_LOCAL.md` に記載します。公開処理は現在のmain/dev、基準277の配布物、全ソースと固定した成果物を検証し、2回のCIビルドとローカルのMPP・QAが一致した場合にのみ進みます。Managerの変更通知対象はTikTokです。
