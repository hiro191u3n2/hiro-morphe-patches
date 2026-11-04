# Maps 検索欄下のカテゴリ行を非表示 v1.0.1

通常版Google Mapsを選ぶとパッチが一覧に表示されなかった、対応パッケージの登録漏れを修正します。
`com.google.android.apps.maps` と `org.ungoogled.android.apps.maps.microg` の両方を登録します。対象は26.36.04.973607363（1068763346）です。

検索欄下の「自宅の住所を設定・レストラン・コーヒー・コンビニ」等を非表示にする処理と、対象メソッドの構造検査はv1.0.0から維持します。

単独版 `Maps_Hide_Category_Row_v1.0.1.mpp` と統合版 `Hiro_Morphe_Patches_v1.0.347.mpp` のいずれかを使用します。
2種類のMorphe SDKで両パッケージの選択を検証し、添付microG版APKへの適用・再構築と変更範囲、既存統合パッチの保持を確認します。
通常版Google Mapsの実APK全体への適用と、Android実機の表示・操作は未確認です。

ビルドは `src/build.py --base baseline346.mpp --tools tools --work work --dist dist --apk supplied.apk`。
CIではAPKを公開せず、MPPの再ビルド、固定したローカル検証済み成果物との一致、既存配布内容の保持を確認します。
