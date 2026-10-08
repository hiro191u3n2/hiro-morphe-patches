ULike v1.9.48／総合版 v1.0.181 の公開手順

ULike v1.9.47／総合版 v1.0.180 の固定チェックサムを基準にします。main・devのどちらかの公開版、ダウンロード先、ULike_ACTIVE.jsonまたはHEADが進んだ場合は停止し、別の改造を上書きしません。

manifest.jsonは実際に検証した単体・総合MPP、QA、ソースZIP、説明、チェックサムの6点のサイズとSHA256を固定します。既存の画質・時間表示・保存処理、独立した画素比較と撮影準備・中断テスト、元APKSへの適用検証が済んだ成果物だけを公開します。CIは固定したJDK21.0.8とMorphe／Android／D8でソースを再ビルドし、元APKS適用検証が識別したMPPと一致したことを確認します。ネイティブライブラリは基準版と同一で、NDK再ビルドは行いません。元APKS、再構築APK、写真、署名鍵は公開成果物・CIに含めません。

公開前のローカル検証:

    python3 releases/ulike1948-fusion/publication/publish1948.py \
      --dist DIST --repo . --expected releases/ulike1948-fusion/manifest.json \
      --baseline INPUT/Hiro_Morphe_Patches_v1.0.180.mpp \
      --standalone-baseline INPUT/ULike_HQ_Texture_Online_v1.9.47.mpp --local-only

GitHub上の確認だけを行う場合は--preflight-onlyを指定します。公開時はGitHub Releaseの各ファイルをダウンロードしてSHA256を再確認し、固定コミットのMPPダウンロード先を作成します。mainとdevのManager配信情報・ULikeの更新履歴・ULike_ACTIVE.jsonを同時更新し、公開後のダウンロード結果も確認します。

M1・M2・M4・M8・M3・M6・M7・M10を統合し、GPU処理は追加していません。画質計算・解像度・保存形式・圧縮設定を維持し、工程別時間記録はバージョン識別文字列だけ更新します。Galaxy実機での速度・画質は未確認です。Morphe Managerでソース更新した後、元ULike5.6.2（740）へパッチを再適用し、作成されたアプリを更新インストールする必要があります。
