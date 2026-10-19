#!/usr/bin/env python3
"""Compare exact APK entries and enumerate allowed pinned-aapt reconstruction deltas."""
import argparse,hashlib,json,re,subprocess,zipfile
from pathlib import Path
from difflib import SequenceMatcher
p=argparse.ArgumentParser();p.add_argument('aapt2');p.add_argument('before');p.add_argument('single');p.add_argument('bundle');p.add_argument('output');a=p.parse_args()
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def dump(path,mode):
 cmd=[a.aapt2,'dump',mode,path]
 if mode=='xmltree':cmd+=['--file','AndroidManifest.xml']
 return subprocess.check_output(cmd,text=True).splitlines()
bs,ss,us=[zipfile.ZipFile(x) for x in (a.before,a.single,a.bundle)]
assert set(ss.namelist())==set(us.namelist())
assert all(ss.read(n)==us.read(n) for n in ss.namelist()),'Single/bundle outputs differ'
resources=0
for n in bs.namelist():
 if n.endswith('/') or re.fullmatch(r'classes\d*\.dex',n) or n.startswith('META-INF/') or n in ('AndroidManifest.xml','resources.arsc'):continue
 assert n in ss.namelist() and bs.read(n)==ss.read(n),n
 resources+=1
b=dump(a.before,'resources');s=dump(a.single,'resources');assert len(b)==len(s)
expected={
 'attr/colorOnBackground':('      () (attr) type=any','      () (attr) type=reference|string|integer|boolean|color|float|dimension|fraction'),
 'attr/comment_downvote_selected_state_color':('      () (attr) type=any','      () (attr) type=reference|string|integer|boolean|color|float|dimension|fraction'),
 'attr/igds_color_form_field_right_add_on_button_tint_color':('      () (attr) type=any','      () (attr) type=reference|string|integer|boolean|color|float|dimension|fraction'),
 'attr/mediaSegmentedButtonStyle':('      () (attr) type=enum','      () (attr) type='),
 'dimen/intent_aware_ad_rifu_card_large_height':('      () 275.273438dp','      () 275.265625dp')}
current=None;deltas=[];public=0
for x,y in zip(b,s):
 if x.startswith('    resource '):current=x.strip().split()[2]
 if x==y:continue
 if x.startswith('    resource ') and y==x+' PUBLIC':public+=1;continue
 assert current in expected and (x,y)==expected[current],(current,x,y)
 deltas.append({'resource':current,'before':x.strip(),'after':y.strip()})
assert {r['resource'] for r in deltas}==set(expected)
b=[re.sub(r' \(line=\d+\)','',x) for x in dump(a.before,'xmltree')];s=[re.sub(r' \(line=\d+\)','',x) for x in dump(a.single,'xmltree')]
back='A: http://schemas.android.com/apk/res/android:enableOnBackInvokedCallback(0x0101066c)=true'
app=b.index('        E: application');next_child=next(i for i in range(app+1,len(b)) if b[i].startswith('            E:'))
assert '          '+back in b[app:next_child],'Input app did not already enable predictive Back'
expected_activities={
'app.morphe.extension.instagram.patches.dm.DeletedMessagesActivity',
'app.morphe.extension.instagram.patches.instants.InstantsVaultActivity',
'app.morphe.extension.instagram.settings.SettingsActivity',
'app.morphe.extension.instagram.settings.preference.fragments.BackupPrefActivity',
'app.morphe.extension.instagram.settings.preference.fragments.RestorePrefActivity',
'app.morphe.extension.crimera.downloader.FolderPickerActivity',
'app.morphe.extension.instagram.settings.SettingsShortcutActivity'}
added=[]
for tag,i,j,k,l in SequenceMatcher(a=b,b=s,autojunk=False).get_opcodes():
 if tag=='equal':continue
 assert tag=='insert' and s[k:l]==['              '+back],(tag,b[i:j],s[k:l])
 start=max(t for t in range(k) if s[t]=='            E: activity')
 name=next(re.search(r'="([^"]+)"',line).group(1) for line in s[start:k] if 'android:name(0x01010003)=' in line)
 added.append(name)
assert set(added)==expected_activities and len(added)==7
report={'result':'PASS','physical_android_tested':False,'input_sha256':sha(a.before),'single_output_sha256':sha(a.single),'bundle_output_sha256':sha(a.bundle),'all_single_bundle_zip_entry_bytes_equal':True,'byte_exact_noncode_entries':resources,'resource_table':{'aapt_public_flags_added':public,'other_deltas':deltas,'all_other_decoded_values_equal':True},'manifest':{'input_application_back_flag_already_true':True,'only_changes_besides_line_numbers':'Seven explicit activity flags equal to existing inherited application value','activities':added},'aapt2_sha256':sha(a.aapt2)}
Path(a.output).write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False,indent=2))
