# TikTok 設定の日本語化 v1.0.0

Hiro Morphe Patches v1.0.279 に追加する日本語化パッチです。対象は [icysymmetra版 TikTok パッチ v0.9.0-dev.1](https://github.com/icysymmetra/tiktok-patches-for-morphe/releases/tag/v0.9.0-dev.1) が追加する **Metra 設定**。設定名、説明、ダイアログ、メニュー、各種メッセージの **2,181 項目**に日本語訳を追加します。

## 適用方法

1. Morphe Manager で Hiro Morphe Patches を **v1.0.279** に更新するか、単体の `TikTok_Settings_Japanese_v1.0.0.mpp` を読み込みます。
2. 上流の TikTok パッチを **v0.9.0-dev.1** にそろえ、上流の **Settings** と **「TikTok設定を日本語化」**を一緒に選択します。日本語化パッチは既定では未選択です。
3. TikTok APK に適用し、端末または TikTok のアプリ言語を日本語に設定します。日本語を優先する言語設定で、Metra の設定が日本語になります。

ソースの更新だけではインストール済み TikTok は変わりません。上流 Settings が必要です。上流 v0.8.1 には対応する多言語機構がないため、この日本語化の対象に含めません。

対象パッケージは `com.zhiliaoapp.musically` と `com.ss.android.ugc.trill` です。上流の多言語拡張が組み込まれた入力にも適用できます。各ソースを選んだ順序に左右されないよう、上流拡張の追加が終わる最終処理で日本語辞書を接続します。対応する拡張が見つからない場合は、必要な Settings の選択を案内して停止します。

## 翻訳範囲と仕組み

上流の `L10nTranslations.of` に日本語タグ `ja` 用の辞書を追加します。元の辞書取得処理を保持し、日本語以外は上流の表示に従います。日本語辞書の初期化は 60 項目ずつ、計 37 メソッドに分割しています。設定値や機能ゲートの値、通信処理は変えません。

SIM プリセットの国名、変更履歴本文、ライセンス原文、診断の生データは翻訳対象外です。TikTok、Metra、形式名などの固有名や技術表記は、意味を保つため原文を使います。翻訳元と翻訳結果は `src/l10n/en.csv` と `src/l10n/ja.csv` で確認できます。

総合版は公開済み v1.0.278 を基準に、新しい日本語化のローダーと辞書を追加します。以前のローダークラスは意味的に同一であることを検証し、既存のクラスファイル・拡張・リソースはバイト単位で保持します。戻る操作の修正を含む既存パッチや ULike の配布方針を変更しません。

## 検証と限界

全 2,181 項目のキー、書式指定子、改行を照合します。ホスト検証では上流の実際の多言語ソースを使い、日本語選択、他言語のフォールバック、文の整形を確認します。Android API のホスト用代替実装は、端末表示を再現するものではありません。

公開済み Metra 拡張の実 DEX を合成 APK に組み込み、単体・総合 MPP × TikTok の 2 パッケージ × 3 条件（拡張が既存、上流を先に適用、日本語化を先に適用）の **12 通り**を検証します。多言語機構がない入力の拒否、再適用でフックを重複させないこと、元の上流実装の保持も検査します。

**TikTok 47.4.3 の実 APK 適用、Android 17 の実機表示、123 パッチの同時適用は未確認です。** 合成 APK で使用するのは実際に公開された Metra 拡張ですが、TikTok 本体や実機操作を検証したことにはなりません。

公開処理はローカル 2 回と CI 2 回を新規ビルドし、MPP と QA がすべて完全一致した場合に進みます。GitHub Release の全 6 資産とコミット固定の配布 URL を再ダウンロードして検証し、main/dev の Manager フィードを明示的なリース付きで同時更新します。既存の変更履歴と `ULike_ACTIVE.json` を保持し、通知対象は TikTok のみにします。

- 単体: `TikTok_Settings_Japanese_v1.0.0.mpp`
- 総合: `Hiro_Morphe_Patches_v1.0.279.mpp`
- QA: `QA_TikTok_Settings_Japanese_v1.0.0.json`
- ソース: `TikTok_Settings_Japanese_v1.0.0_sources_and_QA.zip`
- [公開ページ](https://github.com/hiro191u3n2/hiro-morphe-patches/releases/tag/tiktok-japanese-v1.0.0)
- ビルド・公開準備: `publication/BUILD_LOCAL.md`

GPL-3.0 の LICENSE と、上流・派生元の NOTICE を保持しています。ソース ZIP に翻訳 CSV、ビルド・公開スクリプト、QA を含めます。TikTok APK は配布しません。
