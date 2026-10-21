# Yahoo!乗換案内 広告非表示 v1.2.3 / Hiro v1.0.268

## 原因と変更

v1.2.2は広告を囲む前に、祖先のどこかにData Bindingのタグがあるだけで処理を見送っていました。実際のホームなどは画面rootがData Bindingに属しており、広告に再表示防止用の親が付かない状態を実リソースで再現しました。通常ViewのIDやbinding_Nタグは公式ランタイムが子孫から再帰的に解決するため、親の追加を一律に避ける必要はありませんでした。

v1.2.3は通常の広告ViewをIDのない非表示の親で保護し、元のID・型・Bindingタグを保持します。include/mergeの境界は直接の親と兄弟順を維持します。Constraint/Relative等のアンカー付きレイアウトとXML rootは型・階層を保ち、その場で非表示にします。実SDKのAdViewGroupの判定漏れと、PRラベルで確認できる運賃画面の広告専用バナーも対象に追加しました。通常の検索・運行・緊急情報は保持します。

既存の総合版1.0.267のうちYahoo用helper 1クラスを更新し、ほかの260 DEXクラスとバンドル内の既存リソースを保持します。

## 検証

- ソースと正確な出荷DEXの変換版を別classpathで検証します。
- 本番のPatchBundleLoaderで登録済みパッチを読み込み、Morphe Desktop 1.16.0 / 1.19.0-dev.4の両SDKで公開execute経路を実行します。
- アプリに同梱されていた版に合わせ、未改変の公式Data Binding 8.13.2のmapBindingsを実行します。通常Viewの同一性とinclude/merge境界の正常系・意図的に壊した負例を確認します。Android Viewはホスト用stubであり、アプリの生成Bindingクラスはテスト用対応表で代用します。
- 過去の改造済み8.4.1/8.5.0の実リソースを各SDKで処理し、570レイアウトのencode/redecode、元のID/tag/相対構造、通常検索部品、反復時の一致を確認します。実APKの全文は公開せず、ローカル実行結果をsrc/evidenceに保存し、helper・テストソース・出荷DEXのSHA256で結び付けます。CIはその一致を検査します。
- CIで2回の独立ビルドを行い、ローカル検証済みMPP/QAと同じバイト列であることを確認してから公開します。

取得できた2つの過去APKにはアプリのDEXがありません。したがって、未改造APKへの完全適用・アプリ生成Bindingクラス・Androidの実描画・実機動作は未確認です。広告専用のConstraint子は階層を変えず複数属性で非表示にするため、アプリが全属性を動的に上書きする挙動までは保証していません。

再ビルド・公開手順はpublication/README.md、依存の公式URLとSHA256はsrc/tool-sources.jsonを参照してください。出荷MPPはDEXのみで、ホスト検証用のJVM bridgeや入力APKは含みません。
