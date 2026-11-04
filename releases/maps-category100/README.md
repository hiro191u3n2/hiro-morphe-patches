# Maps 検索欄下のカテゴリ行を非表示 v1.0.0

添付されたmicroG版Google Maps `org.ungoogled.android.apps.maps.microg` 26.36.04.973607363（1068763346）向け。
検索欄下の「自宅の住所を設定・レストラン・コーヒー・コンビニ」等のカテゴリ行を非表示にします。

単独版 `Maps_Hide_Category_Row_v1.0.0.mpp` と統合版 `Hiro_Morphe_Patches_v1.0.346.mpp` のいずれかを使用します。
対象APKへの適用・再構築と変更範囲、既存統合パッチの保持を検証します。Android実機の表示・操作は未確認です。

ビルドは `src/build.py --base baseline345.mpp --tools tools --work work --dist dist --apk supplied.apk`。
CIではAPKを公開せず、MPPの再ビルド、固定したローカル検証済み成果物との一致、既存配布内容の保持を確認します。
