#!/usr/bin/env python3
"""Reproduce only the already-reviewed 1.6.6 bytes. Fail closed on every mismatch."""
import base64, hashlib, io, json, shutil, subprocess, tarfile, zipfile
from pathlib import Path, PurePosixPath

ROOT=Path.cwd()
SOURCE=ROOT/'source166'
BASE=ROOT/'baseline165'
BUILD=ROOT/'build166'
DIST=ROOT/'dist'
PARTS=ROOT/'releases/ulike-v1.6.6'
SOURCE_SHA='605c1dc3d2dc59c4a604543ab50a83e477c477c39e7637c2410f0d02bd7aac4c'
PINS={
 'ULike_v1.6.5_sources_and_QA.zip':'25c15a650020f61ca8f44767c7d3511de84d32128ce85e509c8f0d898c406a10',
 'ULike_HQ_Texture_Online_v1.6.5.mpp':'cb9c34d360b5f84aa97ebfcf0a852af6136b68035848037cef6126e131f93c28',
 'Hiro_Morphe_Patches_v1.0.98.mpp':'7f5d3baba1083ea1343e01ba7387216faf05d3c0e382e2435fccd0e939b2ce87',
}

def sha(b): return hashlib.sha256(b).hexdigest()
def run(*args):
 print('RUN', ' '.join(map(str,args)),flush=True)
 subprocess.run(list(map(str,args)),check=True)
def safe_name(n):
 p=PurePosixPath(n)
 assert not p.is_absolute() and '..' not in p.parts,n
 return p

def main():
 for d in [SOURCE,BASE,BUILD,DIST]: d.mkdir(exist_ok=True)
 parts=sorted(PARTS.glob('source.b64.*'))
 assert [p.name for p in parts]==[f'source.b64.{i:03}' for i in range(4)]
 raw=base64.b64decode(''.join(''.join(p.read_text().split()) for p in parts),validate=True)
 assert sha(raw)==SOURCE_SHA,'Reviewed source archive differs'
 with tarfile.open(fileobj=io.BytesIO(raw),mode='r:xz') as t:
  seen=set()
  for m in t.getmembers():
   p=safe_name(m.name)
   assert m.isfile() and m.name not in seen,m.name
   assert p.suffix.lower() not in {'.apk','.apks','.apkm','.so','.jks','.keystore','.p12'}
   seen.add(m.name)
  t.extractall(SOURCE,filter='data')
  source_members=sorted(seen)
 for name,digest in PINS.items():
  assert sha((ROOT/'input'/name).read_bytes())==digest,name
 with zipfile.ZipFile(ROOT/'input/ULike_v1.6.5_sources_and_QA.zip') as z:
  assert len(z.namelist())==len(set(z.namelist())) and z.testzip() is None
  for n in z.namelist(): safe_name(n)
  z.extractall(BASE)
 old=ROOT/'input/ULike_HQ_Texture_Online_v1.6.5.mpp'
 integrated=ROOT/'input/Hiro_Morphe_Patches_v1.0.98.mpp'
 (BUILD/'oldpatch').mkdir(exist_ok=True)
 (BUILD/'newpatch').mkdir(exist_ok=True)
 (BUILD/'assets').mkdir(exist_ok=True)
 with zipfile.ZipFile(old) as z:
  for n in ['methods.dex','runtime.dex']:
   (BUILD/('old-'+n)).write_bytes(z.read('ulike/'+n))
  (BUILD/'assets.tsv').write_bytes(z.read('ulike/assets.tsv'))
  for i in range(3):
   (BUILD/f'assets/{i:04}.bin').write_bytes(z.read(f'ulike/assets/{i:04}.bin'))
  contracts={line.split('\t',1)[0]:line for line in z.read('ulike/methods.tsv').decode().splitlines() if line}
  delta={line.split('\t',1)[0]:line for line in (SOURCE/'inputs/contracts-delta.tsv').read_text().splitlines() if line}
  contracts.update(delta)
  (BUILD/'methods.tsv').write_text(''.join(contracts[k]+'\n' for k in sorted(contracts)))
  for n in z.namelist():
   if n.endswith('.class'):
    for folder in ['oldpatch','newpatch']:
     p=BUILD/folder/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(n))
 with zipfile.ZipFile(integrated) as z: (BUILD/'old-integrated.dex').write_bytes(z.read('classes.dex'))
 run('bash',SOURCE/'test.sh')
 toolcp=str(ROOT/'tools/morphe.jar')+':'+str(ROOT/'tools/asm.jar')
 classes=BUILD/'toolclasses';classes.mkdir(exist_ok=True)
 run('javac','-cp',toolcp,'-d',classes,BASE/'integration/MergePayloads.java',BASE/'integration/UpdatePatchStrings.java',SOURCE/'AssembleReviewed166.java')
 cp=str(classes)+':'+toolcp
 run('java','-cp',cp,'AssembleReviewed166',BUILD/'old-methods.dex',BUILD/'old-runtime.dex',SOURCE/'inputs/methods-delta.dex',SOURCE/'inputs/helpers.dex',BUILD/'methods.tsv',BUILD)
 release=json.loads((SOURCE/'release.json').read_text())
 (BUILD/'description.txt').write_text(release['patch_description']+'\n')
 loader='app/hiro/ulike/patches/UlikeHqMaxPatch.class'
 run('java','-cp',cp,'UpdatePatchStrings',BUILD/'oldpatch'/loader,BUILD/'newpatch'/loader,'v1.6.5',BUILD/'description.txt')
 expected=json.loads((SOURCE/'expected-payloads.json').read_text())
 assert sha((BUILD/'newpatch'/loader).read_bytes())==expected['newpatch/'+loader]
 class_files=sorted((BUILD/'newpatch').rglob('*.class'))
 # D8 build mode is selected solely by matching the immutable, previously tested hash.
 # No alternate generated payload can be published.
 for mode in [[],['--release'],['--debug']]:
  out=BUILD/'newdex'
  if out.exists(): shutil.rmtree(out)
  out.mkdir()
  run('java','-cp',ROOT/'tools/r8.jar','com.android.tools.r8.D8',*mode,'--min-api','26','--output',out,*class_files)
  if sha((out/'classes.dex').read_bytes())==expected['newdex/classes.dex']: break
 else: raise AssertionError('No D8 mode reproduces the reviewed loader')
 run('java','-cp',cp,'MergePayloads','loaders',BUILD/'old-integrated.dex',BUILD/'newdex/classes.dex',BUILD/'integrated-new.dex')
 for name,digest in expected.items(): assert sha((BUILD/name).read_bytes())==digest,'Payload differs: '+name
 run('python3',SOURCE/'package_mpp.py','--old-standalone',old,'--old-integrated',integrated,'--build',BUILD,'--release',SOURCE/'release.json','--output',DIST)
 expected_artifacts=json.loads((SOURCE/'expected-artifacts.json').read_text())
 qa_raw=(SOURCE/'release_pipeline/LOCAL_QA.json').read_bytes();qa=json.loads(qa_raw)
 assert qa['artifacts']==expected_artifacts
 for a in expected_artifacts:
  b=(DIST/a['file']).read_bytes()
  assert len(b)==a['bytes'] and sha(b)==a['sha256'],a['file']
 (DIST/'QA_ULike_v1.6.6.json').write_bytes(qa_raw)
 # Retain the audited predecessor publisher, changing only the version/branch parameters.
 for n in ['publish.py','manager_metadata.py']:
  shutil.copyfile(BASE/'release_pipeline'/n,SOURCE/'release_pipeline'/n)
 wrapper="import publish\npublish.VERSION, publish.PREVIOUS_APP = '1.6.6', '1.6.5'\npublish.BUNDLE, publish.PREVIOUS = '1.0.99', '1.0.98'\npublish.TAG = 'ulike-v1.6.6'\npublish.UPLOAD_BRANCHES = publish.BRANCHES + ('work/ulike-lens-focus-v166',)\nif __name__ == '__main__': publish.main()\n"
 (SOURCE/'release_pipeline/publish166.py').write_text(wrapper)
 for n in ['MergePayloads.java','UpdatePatchStrings.java']:
  p=SOURCE/'predecessor-integration'/n;p.parent.mkdir(exist_ok=True);shutil.copyfile(BASE/'integration'/n,p)
 shutil.copyfile(PARTS/'build_reviewed.py',SOURCE/'build_reviewed.py')
 members=sorted(set(source_members+['build_reviewed.py','release_pipeline/publish.py','release_pipeline/manager_metadata.py','release_pipeline/publish166.py','predecessor-integration/MergePayloads.java','predecessor-integration/UpdatePatchStrings.java']))
 with zipfile.ZipFile(DIST/'ULike_v1.6.6_sources_and_QA.zip','w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for n in members:
   zi=zipfile.ZipInfo(n,(2026,10,1,12,22,34));zi.compress_type=zipfile.ZIP_DEFLATED;zi.external_attr=0o100644<<16
   z.writestr(zi,(SOURCE/n).read_bytes())
 result={'status':'PASS_EXACT_BYTES_REPRODUCED','source_archive_sha256':SOURCE_SHA,'artifacts':expected_artifacts,'device_tested':False}
 (DIST/'CI_REPRODUCTION.json').write_text(json.dumps(result,indent=2)+'\n')
 print(json.dumps(result,indent=2),flush=True)

if __name__=='__main__':main()
