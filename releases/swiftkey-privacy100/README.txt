SwiftKey Beta テレメトリ停止オプション v1.0.0
統合 Hiro Morphe Patches v1.0.362

配布ページ
https://hiro-morphe-patches.otoha10.chatgpt.site

対象
Microsoft SwiftKey Beta / com.touchtype.swiftkey.beta
9.13.16.4 / versionCode 1236271168
版番号に加えて対象メソッドの内容も照合します。通常版SwiftKey、別ビルド、
対象コードが異なるAPK、同じ停止パッチを適用済みのAPKには適用しません。

使い方
1. Morphe ManagerでHiro Morphe Patchesをv1.0.362へ更新します。
2. 対応する元のSwiftKey Beta APK/APKSを選びます。
3. パッチ一覧で「テレメトリ・解析・エラー報告を停止」を選びます。
   この項目は初期状態では未選択です。必要な場合だけ有効にしてください。
4. 入力改善も必要なら、従来の「日本語入力・予測変換を改善しSamsung絵文字に統一」
   を別途選択します。新しい停止項目は、従来の入力改善項目に依存しません。
5. パッチを実行し、作成したAPKを端末へインストールして反映します。
   パッチソースを更新しただけでは、インストール済みアプリは変わりません。

単独版を使う場合
SwiftKeyBeta_Privacy_v1.0.0.mppだけでも、新しい停止項目を選択できます。
統合版と単独版で同じ項目を重複して読み込まず、どちらか一方を使ってください。
停止済みAPKへ再適用するのではなく、停止パッチを入れる前の対応入力から作り直します。
停止を解除するときも、対応入力からこの項目を選ばずに作り直します。

変更内容
- ファーストパーティのGenericRecord収集入口と、2種類のアップロード処理を停止。
  呼び出し元が期待する成功結果を返し、不要な再試行を避けます。
- Adjust SDKの初期化とライフサイクルproviderを停止。
  停止したAdjustの応答を待つ処理も外し、最大30秒の不要な待機を避けます。
- Firebase Sessionsをmanifest設定で無効化。
- 従来のGoogle Analyticsのreceiver/service/job serviceを無効化。
- アプリのexceptions_report_enabledをfalseへ変更。
- Firebase Crashlyticsの自動収集を無効化。既存設定がtrueでも自動収集の判定をfalseにします。

維持する内容
既存の日本語入力改善パッチv1.8.33とその実行コード・辞書は変更していません。
今回の変更は検証済みの6メソッド、4コンポーネント、2つのmetadata、1つのboolに限定します。
キーボードの入力・変換・学習・同期、FCM、Firebase初期化、Google Install Referrer、
ディープリンクなどの処理を一括停止する変更は行いません。

停止範囲について
これは指定した解析・自動報告経路を止めるオプションです。端末上の既存ログを削除する機能はありません。
Crashlyticsは通常の自動収集オフ経路を使うため、SDKが端末内へクラッシュ記録を残す場合があります。
アカウント同期やオンライン機能を含む、アプリの全通信を禁止する機能ではありません。

検証範囲
過去に提供されたSwiftKey Beta v2のAPKSをMorphe 2.2の標準マージ処理で統合して確認しました。
この検証入力には以前の改造が入っています。未改造の純正APKでの実行試験とは区別してください。
元APKSのbaseと統合後APKの3個のDEXが一致することを確認したうえで、
単独版と統合版のそれぞれから新しい停止項目だけを選択して適用します。
適用後の全クラス・メソッドとリソースを独立監査し、許可した変更以外を保持しているか検査します。
旧Morphe APIとMorphe 2.2でのパッチ検出、初期状態が未選択、既存入力項目との独立性、
対象外の入力や改変済みの停止対象コードを拒否するガードも検査します。
Android実機・エミュレーターでの入力操作、通信キャプチャ、電池消費の測定は未実施です。
既存入力改善パッチと新項目の同時適用を、未改造の純正APKで実行したという主張はしていません。
具体的な結果・ハッシュはQA_SwiftKey_Privacy_v1.0.0.jsonを参照してください。

再ビルド
src/build.pyの --help を参照してください。JDK 21.0.13、同スクリプトに固定した
morphe.jar / morphe-2.2.jar / android.jar / r8.jar と、基準の統合MPP v1.0.361が必要です。
出力例:
python3 src/build.py --base /path/Hiro_Morphe_Patches_v1.0.361.mpp \
  --tools /path/build-tools --java-home /path/jdk-21.0.13+9 \
  --work /writable/fresh-build --dist /writable/empty-dist --timestamp 2026-11-06T18:14:00

検証の再実行
src/verify.pyでも、既知のAPKSと統合済みAPKから、ガード・単独版/統合版適用・DEX/資源監査・QA生成を一括実行できます。
--helpで入力パスを確認してください。出力先には新規の作業ディレクトリを指定します。
src/verification/MergeInput.javaをmorphe-2.2.jarのclasspathでコンパイルし、APKSを統合します。
PrivacyGuardTest guards INPUT.apkでバージョン・コードのガードを確認します。
Morphe 2.2 CLIでは以下のようにこの項目だけを適用します:
java -jar morphe-2.2.jar patch INPUT.apk -p PATCH.mpp --exclusive \
  -e 'テレメトリ・解析・エラー報告を停止' --unsigned --bytecode-mode STRIP_FAST -o OUTPUT.apk
src/test/PrivacyAudit.javaのdiff INPUT.apk OUTPUT.apkでDEXを監査します。
両APKをapktool 3.0.3 decode -sで新規ディレクトリへ展開したうえで、
src/audit_resources.pyに --original-apk / --patched-apk / --original-decoded /
--patched-decoded / --original-apk-sha256 / --output を渡してリソースを監査します。
作業ディレクトリが書き込み可能になるようTMPDIRとMORPHE_DATA_DIRを指定してください。

配布物にはMicrosoftの元APK、端末データ、署名鍵を含めません。
