# Yahoo!乗換案内 中央の発着入替ボタン非表示 v1.2.5 / Hiro v1.0.270

## 変更する操作

検索結果の中央にある「発着を入れ替えて再検索」ボタンを非表示にします。上下の検索・再検索などの操作と、v1.0.269に収録した広告・NearMeの非表示処理、ManagerのJVM実行対応を保持します。

対象は `view_search_btn.xml` とその各レイアウト構成内で、文言が「発着を入れ替えて再検索」と確認できる単独のボタンです。`@string` 参照は文字列リソースを解決して判定し、未確認のIDを推測して使いません。元のView、ID、Bindingタグ、親子関係を残し、対象の表示・寸法・操作属性を非表示用に設定します。親コンテナ全体や上下の操作部品は対象にしません。

8.5.6で対象を安全に特定できない場合は適用を失敗させます。対象を変更しないまま成功扱いにすることを防ぎます。過去の8.4.1/8.5.0の保存リソースにはこの中央ボタンがありません。

## 配布内容の保持

基準は公開済み総合版1.0.269です。MPPのentry一覧は同一で、変更するentryは次の3件に限定します。

- `META-INF/MANIFEST.MF`
- `classes.dex`
- `app/hiro/yahootransit/patches/NearMeLayout.class`

残り121 entriesと、対象helper以外の62 JVMクラスはバイト単位で保持します。`LoaderAudit`でDEX内のクラス一覧が同一であることと、変更がパッチ用の `NearMeLayout` 1クラスだけであることを検査します。他のDEXクラスは正規化した定義を比較して保持を確認します。JVM用helperは最終出荷DEXから変換して収録し、Android用とJVM用に同じ処理を含めます。

## 完成MPPの検証と公開条件

1. `ReverseSearchButtonTest`で中央ボタンの判定と非表示、上下の操作・ID・Bindingタグ・親構造の保持、繰返し適用、対象不明時の失敗を確認します。広告・NearMe・公式Data Bindingの回帰試験も完成MPPのコードで実行します。
2. 公式Manager 2.0.0-dev.15の実 `PatchPipeline.selectedPatches` とPatcher 2.0.0-dev.22に、基準MPPと完成MPPをそのまま渡します。選択・依存処理・再構築・出力の再読込を確認します。選択試験はテストと公式hostのみ、scheduler試験はこれに公式APK由来のframework資源だけを加えたclasspathです。検証用Yahooコードやbridgeは追加しません。
3. schedulerには独自作成した最小XMLと空の合成クラスから作るAPKを使用し、基準269では中央ボタンが残り、270では中央だけが非表示になることを比較します。このAPKのversionNameは8.5.6ですが、実アプリの原本ではありません。
4. 保存済み8.4.1/8.5.0のリソースには、最終MPPを直接読み込んで2つのSDKで計4回の実行・再構築・再読込を行います。これは既存機能保持の証拠であり、新しい中央ボタンの非表示を実アプリで確認した結果ではありません。証拠は今回のMPPとDEXのSHAに結び付けます。
5. source freezeの後、CIの独立した2回のビルドでMPPとQAがバイト単位で一致し、ローカルで検証した生成物とも一致することを公開条件にします。Releaseの実ダウンロード内容を照合し、main/devのManagerフィードを同時更新します。他アプリの内容とULikeの公開設定を保持します。

## 検証の範囲

この新機能を実際のYahoo!乗換案内8.5.6原本APKやAndroid実機で確認した結果はありません。原本8.5.6 APKは未取得です。過去の8.4.1/8.5.0の保存リソースは改造済みで原本DEXを含みません。公式Data Bindingの試験は未改変の探索処理をホストのView stubで実行し、実アプリの生成BindingクラスやAndroid描画は実行しません。

元アプリのAPK・DEX・実XML・添付画像・端末ログと、生成した合成APKは公開ソースやRelease assetsに含めません。公開するのはMPP、QA、独自作成したソース・最小fixture、リリースノート、チェックサムです。

依存URLとSHA256は `src/tool-sources.json`、公開手順は `publication/README.md`、結果は `QA_YahooTransit_v1.2.5.json` を参照してください。
