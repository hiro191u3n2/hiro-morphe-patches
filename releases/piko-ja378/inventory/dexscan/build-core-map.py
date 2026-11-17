import base64, json, hashlib, re
from pathlib import Path
root=Path(__file__).parents[2]
rows=[]
for line in (root/'inventory/dexscan/all-const-strings.tsv').read_text().splitlines():
    a=line.split('\t')
    rows.append(dict(file=a[0],class_=a[1],method=a[2],parameterTypes=a[3].split('|') if a[3] else [],returnType=a[4],expectedCount=int(a[5]),source=base64.b64decode(a[6]).decode()))
out=[]
def add(short, method, source, ja, evidence):
    matches=[r for r in rows if r['class_'].endswith('/'+short+';') and (method is None or r['method']==method) and r['source']==source]
    assert matches,(short,method,source)
    for r in matches:
        slug=re.sub('[^a-z0-9]+','_',source.lower()).strip('_')[:47]
        h=hashlib.sha256((source+'\0'+ja).encode()).hexdigest()[:8]
        out.append(dict(**{'class':r['class_']},method=r['method'],parameterTypes=r['parameterTypes'],returnType=r['returnType'],source=source,ja=ja,resource='hiro_piko_newx_ui_'+slug+'_'+h,expectedCount=r['expectedCount'],evidence=evidence))

for source,ja in [
    ('For You topics','おすすめのトピック'),
    ('Select topics to show in your For You timeline','「おすすめ」タイムラインに表示するトピックを選択'),
    ('Show only 1 topic','選択した1個のトピックのみ表示'),
    ('Show only ','選択した'),
    (' topics','個のトピックのみ表示'),
    ('Reset','リセット'),
]: add('ForYouTopicFilterSheet',None,source,ja,'upstream extensions/newx/.../timeline/ForYouTopicFilterSheet.java: sheet title/subtitle and updateActionButton TextView text')
for source,ja in [
    ('No filtered replies for this post','このポストにフィルターで非表示にした返信はありません'),
    ('Filtered replies','非表示の返信'),
    ('1 reply hidden by verified accounts filter','認証済みアカウントフィルターで1件の返信を非表示にしました'),
    (' replies hidden by verified accounts filter','件の返信を認証済みアカウントフィルターで非表示にしました'),
    ('Copied reply text','返信のテキストをコピーしました'),
    ('(No text content)','（テキストなし）'),
    ('Whitelisted ✓','許可リストに追加済み ✓'),
    ('+ Whitelist','＋ 許可リストに追加'),
    ('Added @','@'),
    (' to whitelist',' を許可リストに追加しました'),
    (' is already whitelisted',' はすでに許可リストに登録されています'),
    ('Failed to whitelist: ','許可リストに追加できませんでした: '),
]: add('FilteredRepliesDialog',None,source,ja,'upstream extensions/newx/.../filteredreplies/FilteredRepliesDialog.java: sheet title/subtitle, button, reply body placeholder, Toast')

for source,ja in [
    ('At least 2 images are required to merge','結合には2枚以上の画像が必要です'),
    ('Downloading and merging ','画像のダウンロード・結合中（'),
    (' images...','枚）…'),
]: add('MediaMerger','downloadAndMerge',source,ja,'upstream extensions/newx/.../misc/MediaMerger.java: NewXInAppNotification.showForUser')
for source,ja in [
    ('Download folder is no longer available — tap download again to choose a new one','保存先フォルダーを利用できません。もう一度ダウンロードを押して選び直してください'),
    ('Failed to save merged image: ','結合画像を保存できませんでした: '),
    ('Failed to download image slice ','分割画像をダウンロードできませんでした: '),
    ('Failed to read dimensions of slice ','分割画像のサイズを読み取れませんでした: '),
    ('Failed to decode image slice ','分割画像を読み込めませんでした: '),
    ('Merged image already exists: ','結合画像は保存済みです: '),
    ('Merged image saved: ','結合画像を保存しました: '),
    ('Out of memory while stitching images','画像の結合中にメモリが不足しました'),
    ('Failed to merge images','画像を結合できませんでした'),
]: add('MediaMerger','performMerge',source,ja,'upstream extensions/newx/.../misc/MediaMerger.java: notification/reportDownloadStatus; diagnostics-only lambdas excluded')
for source,ja in [
    ('Could not start download','ダウンロードを開始できませんでした'),
    ('Download folder is no longer available — tap download again to choose a new one','保存先フォルダーを利用できません。もう一度ダウンロードを押して選び直してください'),
    ('Already downloaded','ダウンロード済みです'),
    ('Download started','ダウンロードを開始しました'),
]: add('NativeDownloadRouter','enqueue',source,ja,'upstream extensions/newx/.../misc/NativeDownloadRouter.java: postStatus -> NewXInAppNotification.tryShowForUser')
for method,source,ja in [
    ('onCancelled','Download cancelled','ダウンロードをキャンセルしました'),
    ('onCompleted','Saved ','保存しました: '),
    ('onFailed','Download folder is no longer available — tap download again to choose a new one','保存先フォルダーを利用できません。もう一度ダウンロードを押して選び直してください'),
    ('onFailed','No connection — tap Retry when online','接続されていません。オンラインになったら「再試行」を押してください'),
    ('onFailed','Could not save ','保存できませんでした: '),
]: add('NewXDownloader$StatusListener',method,source,ja,'upstream extensions/newx/.../misc/NewXDownloader.java: StatusListener notification/reportDownloadStatus')
add('NewXDownloader','install','Downloads','ダウンロード','upstream NewXDownloader.java: NotificationListener channel name; InlineDownloadButtonPatch startup hook executes after sharedExtensionPatch finalizer inserts Utils.setContext at index zero')

# Shared download text providers are interface methods returned directly to notification/UI rendering.
for method,source,ja in [
    ('cancelAction','Cancel','キャンセル'),('deleteAction','Delete','削除'),
    ('downloadCompleted','Download Completed','ダウンロード完了'),
    ('downloadFailed','Download failed','ダウンロードに失敗しました'),
    ('folderLost','Download folder is no longer available','保存先フォルダーを利用できません'),
    ('noConnection','No connection — tap Retry when online','接続されていません。オンラインになったら「再試行」を押してください'),
    ('retryAction','Retry','再試行'),('shareAction','Share','共有'),
    ('shareChooserTitle','Share ','共有: '),('shareChooserTitle','Share','共有'),
    ('waitingForConnection','Waiting for connection','接続待ち'),
]: add('EnglishNotificationTexts',method,source,ja,'shared.mpe piko-extension-library 0.10.1-dev.5 DEX: NotificationTexts method returns literal; consumed by DownloadNotifications notification/action/chooser UI')
add('DownloadNotifications','speedText','Calculating speed…','速度を計算中…','shared.mpe piko-extension-library 0.10.1-dev.5 DEX: negative-rate progress text returned to notification; numeric MB/s, KB/s, B/s left unchanged')
add('DownloadNotifications','createNotificationChannel','Downloads','ダウンロード','shared.mpe DEX: default NotificationChannel visible name; channel identifier unchanged')
add('RetryReceiver','onReceive','Downloads','ダウンロード','shared.mpe DEX: retry flow NotificationListener channel display name; download action identifiers unchanged')
for source,ja in [
    (' downloads started','件のダウンロードを開始しました'),
    ('1 download started','1件のダウンロードを開始しました'),
    ('1 needs a new folder','1件は保存先の再選択が必要です'),
    (' need a new folder','件は保存先の再選択が必要です'),
    ('1 failed','1件失敗しました'),(' failed','件失敗しました'),
    ('1 already downloaded','1件はダウンロード済みです'),
    (' already downloaded','件はダウンロード済みです'),
    ('Could not start download','ダウンロードを開始できませんでした'),
    ('Download folder is no longer available — tap download again to choose a new one','保存先フォルダーを利用できません。もう一度ダウンロードを押して選び直してください'),
    (' media already downloaded','件のメディアはダウンロード済みです'),
    ('Already downloaded','ダウンロード済みです'),
    ('Download started','ダウンロードを開始しました'),
]: add('EnglishDownloadTexts','get',source,ja,'shared.mpe DEX: DownloadText switch return text; InlineDownloadButton.java passes provider into DownloadMessages.summary then user notification; Unknown text id diagnostic excluded')
add('PikoSettingsActivity','configureToolbar','Back','戻る','shared.mpe DEX: ImageButton.setContentDescription accessibility label')
add('TrayView','<init>','Close','閉じる','shared.mpe DEX: close button content description')

out.sort(key=lambda x:(x['class'],x['method'],x['parameterTypes'],x['source']))
(root/'translations/core-hardcoded-ja.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n')
print(f'{len(out)} mappings, {len(set(x["resource"] for x in out))} resources')
