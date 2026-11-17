# Miscellaneous NewX UI literal translation inventory

Source: https://github.com/crimera/piko/tree/7acb08ae14de946e3b6b5a9837a860a7b2667caf
Version: v3.12.0-dev.2. The NewX and shared MPE bytes equal v3.12.0-dev.1.
Source license: upstream `LICENSE` (GPLv3) and `NOTICE`; ObjectBrowser carries Copyright (C) 2026 piko. Preserve the upstream NOTICE in the complete source/derivative distribution.

`misc-hardcoded-ja.json` has 79 exact class/method/descriptor/literal entries, 63 resource names, and 79 matching const-string instructions. Counts and synthetic method names come from `inventory/dexscan/all-const-strings.tsv`, generated from the actual MPE DEX. Each allowlist source value was traced to a visible control, accessibility label, notification, chooser, clipboard description, or toast. Japanese phrasing for filtered replies and download settings matches the 485-resource translation set.

Included classes and entries:

- FilteredRepliesPostOptionHandler: 5
- InlineDownloadButton: 15
- MediaPickerDialog: 15
- NewXCrashCopyReceiver: 5
- NewXCrashHandler: 9
- NewXCrashPostOptionHandler: 2
- NewXObjectBrowserHandler: 3
- ResolutionChooserDialog: 3
- ObjectBrowser: 22

Dynamic text is translated by its actual constant segments. In particular `selectedCount + " of " + total + " selected"` becomes `selectedCount + " / " + total + " 件を選択中"`; item counts and username values retain their original order and values. `From @` becomes `投稿者：@`. Java type syntax and reflected field/method names remain intact.

## Intentional exclusions

- Action names, setting keys, MIME strings, URL fragments, class names, search-index terms, logging messages, internal exceptions that are swallowed/logged, generated crash-report technical headings/values and report truncation markers.
- ObjectBrowser Java notation such as `package:`, `extends:`, `implements:`, `param`, `List<`, `null`, and `<access denied>` diagnostic sentinel.
- `NewXCrashShareReceiver` does not exist in this version. The share UI is built directly in NewXCrashHandler.shareIntent().
- InlineDownloadButton's `Image`, `Video` and universal `GIF` are NOT changed at creation: the labels participate in media-type logic. MediaPickerDialog.resolveIconType() uses lowercase label containing `gif` or `video`; MediaPickerDialog.isImage() has an `equalsIgnoreCase("image")` fallback. Translating these source labels would risk changing icons or selection behavior.

## Exact display-only sites for a future Image/Video localization hook

1. `Lapp/morphe/extension/newx/misc/MediaPickerDialog;->show(Landroid/content/Context;Ljava/util/List;Ljava/lang/String;Lapp/morphe/extension/newx/misc/MediaPickerDialog$OnMediaSelectedListener;)V`
   - DEX instruction 182, code-unit address 356: `iget-object ... InlineDownloadButton$DownloadItem;->label:Ljava/lang/String;`.
   - This label is appended to a StringBuilder, optionally followed by a space and 1-based item number.
   - Instruction 199, address 393: `invoke-virtual ... ListItem;->setTitle(Ljava/lang/CharSequence;)V`.
2. `Lapp/morphe/extension/newx/misc/ResolutionChooserDialog;->show(Landroid/content/Context;Lapp/morphe/extension/newx/misc/InlineDownloadButton$DownloadItem;Ljava/lang/String;Lapp/morphe/extension/newx/misc/ResolutionChooserDialog$OnResolutionSelectedListener;)V`
   - Instruction 53, address 109: `iget-object v5,v3,... DownloadItem;->label:Ljava/lang/String;`.
   - Instruction 54, address 111: `invoke-virtual {v4,v5},... ListItem;->setTitle(Ljava/lang/CharSequence;)V`.
3. Both route through `Lapp/morphe/extension/newx/ui/ListItem;->setTitle(Ljava/lang/CharSequence;)V`, whose source lines 145–146 call `titleView.setText(title)`. Avoid a global hook there because many unrelated list rows and potentially dynamic user text share this setter. A future translation should intercept only the two proven rendering call sites, without modifying DownloadItem.label.

The image-quality display strings Original/Large/Medium/Small ARE translated: their separately stored qualityKey values (`original`, `large`, `medium`, `small`) remain untouched.
