ULike1.9.49／総合1.0.182自動公開

G10方針のG1・G3・G5・G6・G9を正確な1.9.48／1.0.181基準に重ねます。manifest.jsonは元APKSに適用確認したMPP・QA・ソースをSHA256で固定します。ワークフローはJDK21.0.8、NDKr27cで再ビルドし、MesaのソフトウェアOpenGL ES3.1で実際の整数シェーダーをCPU基準と比較します。元APKSと再構築APKは公開物に含めません。

ローカルの読み取り専用検査はpublish1949.py --local-only、公開前のManagerフィード検査は--preflight-onlyです。公開時は主・開発フィードを同時更新し、他アプリの資源と変更履歴を保持します。Galaxy実機速度・画質は未確認です。
