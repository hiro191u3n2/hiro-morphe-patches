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
