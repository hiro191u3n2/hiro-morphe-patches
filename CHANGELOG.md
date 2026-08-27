# [1.0.43](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-27)

* **Trip.com:** 航空券＋ホテル画面の境界を古い「料金割引情報」見出しから、現在の青い「検索」ボタンを含むフォーム直後へ変更しました。広告カード・予約履歴・以降を枠・高さ・余白ごと非表示にし、単一wrapper構造とReactによる遅延追加・再表示も対象画面の次のlayout内で再抑止します。起動を遅くしていた全ReactRootView共通フック、最大24,000 View走査、約6秒の再試行タイマー、対象外rootの常駐listenerは完全撤去。CRN URLを確認して航空券＋ホテル／従来の列車専用ルートだけを監視するため、ホームと起動経路のView走査・timer・layout監視はゼロです。広告・AI・おすすめ・追跡遮断、航空券・ホテル個別画面、MyPlan、Instagramを含む他5アプリの改造は維持します。

# [1.0.42](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-25)

* **Instagram:** v1.0.41のflatなRenderCore／Litho／Bloks provider tree対応を維持しながら、タイトル・両回答・閉じる候補を同じ接続rootと同じ局所領域に限定するconnected-root locality safetyを追加しました。host anchor、未接続の照合結果、別Window／別treeの候補は操作対象にせず、同じカード内で検証できるACTION_DISMISSまたは正確な「閉じる」だけを実行します。「興味なし」「興味あり」は引き続き判定専用で絶対に操作せず、通常投稿・キャプション・ストーリーズ・リール・DMと起動速度、従来の設定完了ウィンドウ抑止、他5アプリの実行データを維持します。

# [1.0.41](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-25)

* **Instagram:** 「この投稿に興味がありますか？」がflatなRenderCore／Litho／Bloks provider treeに表示され、カード自身のACTION_DISMISSだけでは閉じられない構造へ対応しました。仮想UIのhost treeを有限範囲でたどり、タイトルと「興味なし」「興味あり」が同じ局所カードにそろったことを再確認してから、カード自身または外側1段の検証済みclose wrapperだけを閉じます。「興味なし」「興味あり」は判定専用で絶対に操作せず、有限長のイベント再試行により通常投稿・キャプション・ストーリーズ・リール・DMと起動速度、従来の設定完了ウィンドウ抑止、他5アプリの実行データを維持します。

# [1.0.40](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-25)

* **Instagram:** 記事内の「この投稿に興味がありますか？」カードを、タイトル・「興味なし」・「興味あり」が同じ局所領域にそろった場合だけ描画前に閉じるよう対応しました。推薦フィードバックは選択せず、カード自身のACTION_DISMISSまたは同じカード内の正確な「閉じる」だけを実行します。Android 16、通常View、RenderCore、Litho／Bloks、別Window、遅延表示・スクロール後の再表示へ対応し、通常投稿・キャプション・ストーリーズ・リール・DM、従来の「アカウントの設定を完了」抑止、Trip.comを含む他5アプリの実行データを維持します。

# [1.0.39](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-24)

* **Instagram:** GitHub更新表示対応ソースからv1.0.38で再パッチした端末へ更新通知を発生させる確認版です。Instagramの実行コード、拡張DEX、Android用classes.dex、Trip.comを含む他5アプリの全実行データはv1.0.38と同一で、「アカウントの設定を完了」の描画前同期抑止をそのまま維持します。

# [1.0.38](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-24)

* **Instagram:** 「アカウントの設定を完了」を作ったレイアウト通知が有限再探索の待機中に無視され、最長6秒表示されていた問題を修正しました。固有タイトルを常時の浅いイベント検知で捕捉し、見つかった時だけActivity画面・別Windowを同じGlobalLayout内で同期判定します。重い全ツリー走査は遅延フェーズごとに1回へ集約。単独の正確な「後で」だけを候補にし、押せない集約ノード・複数候補・仮想ID・Android 16の空検索・フォーカス喪失・バースト完了後の遅延表示へ対応しました。押下／再試行は12回上限を必ず消費し、同じタイトルの無期限再開を抑止します。危険な内部コード書換え・祖先View非表示・Pre-drawブロック・無期限ポーリングは使わず、Trip.com v1.10.10と既存4アプリの実行データを維持します。

# [1.0.37](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-24)

* **Trip.com:** 列車・航空券＋ホテル画面で、消した下部が最短32msだけ表示されてから消えていた処理を修正しました。初回GlobalLayout内で保留中の遅延処理を取り消して同期検出し、Pre-draw／Drawより前に対象枠を高さ0・GONEへ変更します。遅延追加も次のGlobalLayoutで描画前に抑止し、スクロール停止、入力欄・タブ・検索ボタン、起動共通監視の撤去、既存5アプリの改造を維持します。

# [1.0.36](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-24)

* **Trip.com:** 航空券＋ホテル画面で検索フォーム下の灰色余白まで縦に動いていた問題を修正しました。削除境界ではなく残す最後の要素の下端で内容高を切り、厳密な対象画面の検出後だけReactScrollViewのアニメーションを止めて縦スクロールを無効化します。入力欄・タブ・検索ボタン、赤線下削除、起動共通監視の撤去、v1.0.35のDEX修復、既存5アプリの改造は維持します。

# [1.0.35](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-24)

* **Trip.com:** v1.0.34でAndroid DEXの文字列ID順序が崩れ、Morpheが統合MPP全体を読み込めずカスタムパッチ一覧が消えていた問題を修正しました。パッチ一覧DEXをD8で全参照ごと再構築し、統合版v1.0.35／Trip.com単体v1.10.8として復旧。列車・航空券＋ホテルの赤線下削除、スクロール範囲切詰め、起動共通監視の撤去、既存5アプリの改造は維持しています。

# [1.0.34](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-24)

* **Trip.com:** 列車・航空券＋ホテル画面で文字確定前に判定が空振りしていた処理を廃止し、React画面ルート接続後だけの有限検査へ変更しました。「鉄道予約が選ばれる理由」または「航空券＋ホテル料金割引情報」の親セクションから末尾を高さ0・GONEにし、スクロール内容高も境界位置まで切り詰めます。全React子追加・テキスト更新・スクロール設定の共通フックは撤去し、既存の広告・おすすめ・追跡遮断、航空券／ホテル個別画面、マイプラン修正、Instagram v1.2.0も維持します。

# [1.0.33](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-24)

* **Instagram:** 初回起動時の画面接続前でも待ち受けを残し、「アカウントの設定を完了」ポップアップをAndroid 16の空検索、Litho／Bloksの仮想UI、別Windowでの遅延表示にも対応する有限再探索で「後で」を自動実行して閉じるよう強化しました。難読化内部メソッドへの直接書換え、描画前ブロック、無期限監視は使用しません。

# [1.0.32](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-24)

* **Trip.com:** 列車画面の「鉄道予約が選ばれる理由」と航空券＋ホテル画面の「料金割引情報」から下を、親枠・高さ・余白ごと非表示にしました。全View追加・全スクロール設定の共通フックは停止したまま、軽量なテキスト確定フックだけで対象の外側縦スクロールを停止します。既存の広告・おすすめ・追跡遮断、航空券・ホテル個別画面、マイプラン修正も維持します。

# [1.0.31](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-24)

* **Trip.com:** GitHub更新表示対応ソースへの移行確認用に統合版をv1.0.31へ更新しました。Trip.comの実行コード・拡張DEX・置換データはv1.0.30と同一です。

# [1.0.30](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-24)

* **Trip.com:** 起動時にも実行されていたReact共通クラスの全View追加・全テキスト更新・スクロール設定フックと未使用拡張DEX登録を撤去しました。AIボタン、広告・おすすめ・追跡通信、航空券・ホテル個別画面の下部非表示は維持し、航空券＋ホテル画面の動的下部カットは起動速度優先で停止します。

# [1.0.29](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-24)

* **Instagram:** 起動不能の原因だった難読化内部メソッドへの直接書換え、描画前ブロック、無期限監視を撤去し、描画後のイベント駆動ワンショット方式でポップアップ抑止を再収録しました。

# [1.0.28](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-24)

* **Instagram:** v1.0.27の内部コード書換えによる起動不能を回避するため、Instagramパッチを配布対象から一時隔離しました。既にパッチ済みのアプリは元の未改変版へ戻してください。

# [1.0.27](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-23)

* **Instagram:** アカウント設定完了ポップアップの表示可否リクエスト停止、生成前のNUX抑止、仮想UIの描画前ガードと継続監視を統合しました。
* **Trip.com:** 起動用通信保護拡張を1クラスへ最小化し、重複していたメインスレッド処理と未使用計測を削減しました。
* **TikTok:** シークバーと動画プレビューを維持しながら、起動拡張を必要な3クラスへ最小化しました。
* **Y!乗換案内:** 検索結果下部のYDN広告と「検索結果に関するご指摘」を余白ごと非表示にしました。
* **Amazon Shopping:** スポンサー商品非表示を維持し、統合MPP内の拡張ファイル名衝突による起動不能を修正しました。
* **Hanull Reader:** 記事一覧の広告・PR行を余白ごと非表示にしました。

# [1.0.1](https://github.com/hiro191u3n2/hiro-morphe-patches/commits/main) (2026-08-13)

* **Trip.com:** Trip.com用MPPをv1.4.0へ更新し、投稿履歴・下部サポート欄の非表示を強化しました。

# 1.0.0 (2026-08-13)

* **Trip.com:** 統合パッチへ収録しました。
* **TikTok:** 統合パッチへ収録しました。
* **Y!乗換案内:** 統合パッチへ収録しました。
* **Amazon Shopping:** 統合パッチへ収録しました。
* **Hanull Reader:** 統合パッチへ収録しました。
