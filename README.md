# hiro-morphe-patches

X **12.43.0-prod.01** 向けに、Piko NewXの設定・メニューを日本語化する項目を **Hiro v1.0.378** に追加しました。Piko **3.12.0-dev.1／dev.2** のNewX項目と「NewX: Pikoの設定・メニューを日本語化」を元の未改造APKMへ一緒に適用してください。初期状態では未選択です。実APKMの4構成で適用・再構築と完成APKの静的監査に成功。Android実機・エミュレーターの表示・操作は未確認です。

[配布・導入](https://hiro-morphe-patches.otoha10.chatgpt.site/#newx-japanese-heading) / [実装](releases/piko-ja378/README.md) / [完全なソースとQA](https://hiro-morphe-patches.otoha10.chatgpt.site/Piko_NewX_Japanese_v1.0.0_sources_and_QA.zip) / [完成APK検証](https://hiro-morphe-patches.otoha10.chatgpt.site/QA_Piko_NewX_Japanese_v1.0.0.json)

Google Maps **26.44.06.1006895251** 用のHiro追加5項目は **Hiro1.0.374 / Maps Add-ons1.1.0**。グーグルマップ2.9.10と併用し、元APKへ再適用してください。旧26.36は2.9.9との対応を維持しています。

[配布・導入](https://hiro-morphe-patches.otoha10.chatgpt.site/#maps-2644-heading) / [ソースとQA](releases/maps2644/Maps_Hiro_Addons_v1.1.0_sources_and_QA.zip) / [検証結果](releases/maps2644/QA_Maps_Hiro_Addons_v1.1.0.json)

Google Maps 26.36.04.973607363 の互換ソースは [maps-26.36-source.json](maps-26.36-source.json)。公式の未改変MPP v2.9.9を固定し、26.44専用のv2.9.10による適用エラーを回避します。Hiro v1.0.373の5項目と併用検証済みです。

[使い方](releases/maps2636-compat/Maps_26.36_compat_README.txt) / [検証記録](releases/maps2636-compat/QA_Maps_26.36_compat.json) / [配布ページ](https://hiro-morphe-patches.otoha10.chatgpt.site/#maps-2636-compat-heading)
