#!/usr/bin/env python3
"""Rebase reviewed black-preview repair onto live chroma 1.7.7; never overwrite it."""
import sys,json,hashlib,shutil,zipfile,importlib.util
from pathlib import Path
HERE=Path(__file__).resolve().parent
PINS={'ULike_HQ_Texture_Online_v1.7.7.mpp':'d0a04a4a3cc0f95064d41073b886f69c9bfd2581ea681390cd25095e705ef368','Hiro_Morphe_Patches_v1.0.110.mpp':'3e5f885f6e807cfbc6c76e27fc82ef55a3241264bf9c0cb6ced5e92236b329c8'}
def sha(b):return hashlib.sha256(b).hexdigest()
def prepare(root,base_dir):
 spec=importlib.util.spec_from_file_location('preview_base',base_dir/'reproduce177.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base);base.prepare(root)
 p=root/'build177.py';s=p.read_text();a=s.index('PINS=');b=s.index('\nTOOLS=');s=s[:a]+'PINS='+repr(PINS)+s[b:];s=s.replace('v1.7.6','v1.7.7').replace('v1.0.109','v1.0.110').replace('Reproduce v1.7.7 from published v1.7.7','Reproduce v1.7.8 from published chroma v1.7.7');(root/'build178.py').write_text(s);p.unlink()
 p=root/'package170.py';s=p.read_text();a=s.index('PINNED=');b=s.index('\nCLASS=');s=s[:a]+'PINNED='+repr({'standalone':PINS['ULike_HQ_Texture_Online_v1.7.7.mpp'],'integrated':PINS['Hiro_Morphe_Patches_v1.0.110.mpp']})+s[b:];p.write_text(s)
 p=root/'Transform177.java';s=p.read_text().replace('ULike Capture v1.7.7','ULike Capture v1.7.8').replace('ULike Capture v1.7.6','ULike Capture v1.7.7');p.write_text(s)
 p=root/'release170.json';rel=json.loads(p.read_text());rel['timestamp']='2026-10-02T09:57:00';rel['patch_description']=rel['patch_description'].replace('v1.7.7','v1.7.8')+' 先行公開されたv1.7.7の保存写真の色ムラ抑制も維持。';rel['standalone'].update(version='1.7.8',filename='ULike_HQ_Texture_Online_v1.7.8.mpp');rel['integrated'].update(version='1.0.111',filename='Hiro_Morphe_Patches_v1.0.111.mpp');p.write_text(json.dumps(rel,ensure_ascii=False,indent=2)+'\n')
 notes=base.NOTES.replace('v1.0.110','v1.0.111').replace('v1.7.7','v1.7.8').replace('各1431','各1453').replace('3999エントリ','3999エントリ')
 notes+='\n作業中に先行公開されたULike v1.7.7／総合v1.0.110の保存写真の色ムラ抑制を引き継いでいます。最新の公開バイナリを基準とし、色ムラ補正のクラス・設定・保存処理を再変更しません。\n'
 (root/'CHANGES178.md').write_text('# ULike v1.7.8 / Hiro Morphe v1.0.111\n\n'+notes);(root/'CHANGES177.md').unlink();(root/'release_pipeline/RELEASE_NOTES.txt').write_text(notes)
 (root/'release_pipeline/publish178.py').write_text("import publish\npublish.VERSION,publish.PREVIOUS_APP='1.7.8','1.7.7'\npublish.BUNDLE,publish.PREVIOUS='1.0.111','1.0.110'\npublish.TAG='ulike-v1.7.8'\nif __name__=='__main__':publish.main()\n")
 if (HERE/'LOCAL_QA.json').exists():shutil.copyfile(HERE/'LOCAL_QA.json',root/'release_pipeline/LOCAL_QA.json')
 shutil.copyfile(__file__,root/'rebase178.py')
def finish(root,dist):
 raw=(HERE/'LOCAL_QA.json').read_bytes();q=json.loads(raw);assert (q['version'],q['bundle_version'],q['previous_bundle_version'])==('1.7.8','1.0.111','1.0.110') and q['android_device_tested'] is False
 for item in q['artifacts']:
  data=(dist/item['file']).read_bytes();assert len(data)==item['bytes'] and sha(data)==item['sha256'],'Not the locally rebuilt latest-base bytes: '+item['file']
 for n,t in {'HOST_PREVIEW177.txt':'130 assertions','HOST_MANUAL170.txt':'596 assertions','HOST_STARTUP175.txt':'60 assertions','HOST_STARTUP173.txt':'64 assertions','HOST_LIFECYCLE172.txt':'44 assertions','HELPER_REFERENCES.txt':'1453 total payload/runtime methods','RUNTIME_TRANSFORM.txt':'preserved=1275, changed=4, added=10','ACCESS_AFTER.txt':'PRIVATE_ACCESS_VIOLATIONS=0'}.items():assert t in (root/'qa170'/n).read_text(),n
 for n,t in q['local_applied_evidence'].items():assert Path(n).name==n;(root/'qa170'/n).write_text(t)
 for n in ['HOST_TESTS.txt','HOST_FAST167_TESTS.txt']:shutil.copyfile(root/'regression170/qa'/n,root/'qa170'/n)
 (dist/'QA_ULike_v1.7.8.json').write_bytes(raw);files=[]
 for f in root.rglob('*'):
  if not f.is_file():continue
  n=f.relative_to(root).as_posix()
  if (f.parent==root and f.suffix in {'.py','.java','.md','.sh','.json'} and f.name!='SOURCE_FILES.json') or (n.startswith(('src170/','stubs170/','host170/','regression170/')) and f.suffix=='.java') or n=='regression170/test.sh' or (n.startswith('qa170/') and f.suffix in {'.txt','.json'}) or (n.startswith('release_pipeline/') and f.suffix in {'.py','.md','.txt','.json'}):files.append(n)
 (root/'SOURCE_FILES.json').write_text(json.dumps({n:sha((root/n).read_bytes()) for n in sorted(files)},indent=2)+'\n');files.append('SOURCE_FILES.json')
 with zipfile.ZipFile(dist/'ULike_v1.7.8_sources_and_QA.zip','w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for n in sorted(files):
   i=zipfile.ZipInfo(n,(2026,10,2,9,57,0));i.compress_type=zipfile.ZIP_DEFLATED;i.external_attr=0o100644<<16;z.writestr(i,(root/n).read_bytes())
 print('PASS v1.7.8 latest-base byte binding; 1060 host checks; local original APK rebuild evidence; no device test.')
if __name__=='__main__':
 if sys.argv[1]=='prepare':prepare(Path(sys.argv[2]).resolve(),Path(sys.argv[3]).resolve())
 elif sys.argv[1]=='finish':finish(Path(sys.argv[2]).resolve(),Path(sys.argv[3]).resolve())
 else:raise SystemExit('prepare SOURCE BASE_OVERLAY or finish SOURCE DIST')
