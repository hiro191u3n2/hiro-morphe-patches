Berry Browser MessageQueue・全バージョン表示 v1.3.0 / Hiro Morphe Patches v1.0.271

【追加するパッチ】
「ロックフリーMessageQueueを有効化」を追加します。
対象SDKが37未満のBerryは37に引き上げ、Android 17標準のロックフリーMessageQueueを利用する対象にします。既に37以上のAPKは対象SDKを下げません。Android 16以前のOSに新しいMessageQueueを移植する機能ではありません。

今回確認した保存済みのBerry 3.83.74（383740）は、元からcompileSdk/targetSdkが37、minSdkが32でした。この入力で対象SDKの変更による新たな高速化を確認したという意味ではありません。端末OSの実装・設定にも依存するため、実際に利用しているMessageQueueを実行時に診断します。Android 17実機での有効化、操作感、速度改善は未測定です。

Android 17の対象SDK変更では他の動作変更も有効になります。ローカルネットワークアクセス権限の宣言と起動時の許可確認を追加し、LAN内のルーター・NAS等にアクセスするための許可を利用者が選べるようにします。診断のための外部送信は追加しません。

【全バージョン対応表示】
Morpheの互換性情報からBerryのバージョン制限を外します。従来の「プレビュー作成を後回しにして表示中タブを優先」「共有先ブラウザの表示を個別に切替」「片手ズーム」と、新しいMessageQueueパッチが対象です。
既存3パッチの適用時チェックは残します。既存3パッチは確認済み3.83.74以外、または対象コードが一致しないAPKでは適用を止めます。今回の全バージョン表示は、全てのBerry版で既存改造が動くと保証するものではありません。新しいMessageQueueパッチはバージョン番号に依存しない構造チェックを使います。

【配布・検証】
Android用classes.dexとManagerのJVM実行用クラスを同じMPPに収録します。既存3機能の拡張データと他アプリのパッチを保持します。
保存済みBerry APKに対するローカル適用試験の証拠を、入力APKのハッシュと配布MPPのハッシュに結び付けます。公開CIはその証拠を検証した上で、再ビルド・公式Managerのパッチ選択・Morpheの適用と再構築・合成APK試験を行います。公開CIで私有APKを再実行するものではありません。入力APK、APKS、端末内データは公開物に含めません。
Android実機の操作確認と速度計測は未実施です。

【導入】
Morphe ManagerのソースをHiro Morphe Patches v1.0.271へ更新し、Berry Browserの「ロックフリーMessageQueueを有効化」を選択して元のAPK/APKSへ適用してください。3.83.74では必要な既存3項目も選択できます。他の版では、適用時の版チェックが残る既存3項目を選択すると中止する場合があります。
従来と同じ署名キーで更新インストールしてください。ソース更新だけではインストール済みBerryに反映されません。

単独版: Berry_Browser_v1.3.0.mpp
総合版: Hiro_Morphe_Patches_v1.0.271.mpp
公開先: https://github.com/hiro191u3n2/hiro-morphe-patches/releases/tag/berry-browser-v1.3.0
Android公式仕様: https://developer.android.com/about/versions/17/behavior-changes-17
