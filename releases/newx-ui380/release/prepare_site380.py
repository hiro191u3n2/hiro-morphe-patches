#!/usr/bin/env python3
"""Prepare the existing Site locally after final QA; never pushes or publishes.

All file updates are preflighted before writing. Previously published versions
and resources are retained. Use --check-only to inspect the proposed change.
"""
from __future__ import annotations
import argparse
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import re
import shutil
import tempfile
import zipfile

HERE=Path(__file__).resolve().parent
COMBINED='Hiro_Morphe_Patches_v1.0.380.mpp'
SINGLE='NewX_UI_Options_v1.1.0.mpp'
APP_QA='QA_NewX_UI_Options_v1.1.0.json'
BUILD_QA='QA_build380.json'
README='README_NewX_UI_Options_v1.1.0.md'
SOURCE='NewX_UI_Options_v1.1.0_sources_and_QA.zip'
PATCH='NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）'
BASE_BYTES=48318820
BASE_SHA='bb6fc511cce4ba4f35f2af68838a3bb7d744c1d8bd04cb8f03e6868e35ed7a8f'
ROOT_SOURCES={'build.py','integrate.py','verify_packages.py','finalize_qa.py','zip_helpers.py','MergeProviders.java',
              'toolchain.json','piko-selected.json','LICENSE','NOTICE','README.md'}
OMIT_DIRS={'analysis','tools','inputs','work','build','generated','classes','testclasses',
           'patch-dex','runtime-dex','dex','piko-dex','__pycache__','dist','delivery-work',
           'storage-test','.git','inventory','inventories'}

def require(ok,message):
    if not ok: raise ValueError(message)

def sha(raw): return hashlib.sha256(raw).hexdigest()

def replace_once(text,old,new):
    require(text.count(old)==1,'Expected one source anchor: '+old[:100])
    return text.replace(old,new,1)

def zipped(files):
    buffer=io.BytesIO()
    with zipfile.ZipFile(buffer,'w')as z:
        for name,raw in sorted(files.items()):
            info=zipfile.ZipInfo(name,(2026,11,17,0,0,0));info.create_system=3;info.external_attr=0o100644<<16
            z.writestr(info,raw,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
    with zipfile.ZipFile(io.BytesIO(buffer.getvalue()))as z:
        require(z.testzip()is None and all(z.read(n)==b for n,b in files.items()),'Source ZIP verification failed')
    return buffer.getvalue()

def collect_sources(root):
    """Strict authored-source allowlist; decoded APK directories are never walked."""
    paths=[root/name for name in ROOT_SOURCES if (root/name).is_file()]
    paths.extend(p for p in (root/'src').rglob('*') if p.is_file() and p.suffix in {'.java','.py','.xml','.md'})
    for directory in ['chat','bars','views']:
        paths.extend(p for p in (root/directory).glob('*') if p.is_file() and
                     (p.suffix in {'.java','.py','.md'} or p.name=='QA.json'))
    for directory in ['refresh','views-runtime']:
        paths.extend(p for p in (root/directory).rglob('*') if p.is_file() and
                     (p.suffix in {'.java','.py','.md'} or p.name=='QA.json'))
    if (root/'count-review/review.json').is_file():paths.append(root/'count-review/review.json')
    for directory in ['review','verification']:
        paths.extend(p for p in (root/directory).glob('*') if p.is_file() and p.suffix in {'.java','.py','.md','.json'})
    paths.extend(p for p in (root/'release').glob('*') if p.is_file() and p.suffix in {'.py','.md'})
    output={}
    for path in sorted(set(paths)):
        rel=path.relative_to(root)
        if path.is_symlink() or any(part in OMIT_DIRS or part.endswith('-work') for part in rel.parts):continue
        raw=path.read_bytes();raw.decode('utf-8');require(len(raw)<=16*1024*1024,'Oversized source: '+str(rel))
        output[rel.as_posix()]=raw
    require({'build.py','integrate.py','verify_packages.py','zip_helpers.py','MergeProviders.java',
             'toolchain.json','piko-selected.json','LICENSE','NOTICE'} <= set(output), 'Build/source provenance files missing')
    return output

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--site',type=Path,required=True)
    parser.add_argument('--source-root',type=Path,default=HERE.parent)
    parser.add_argument('--validation',type=Path)
    parser.add_argument('--build-evidence',type=Path)
    parser.add_argument('--check-only',action='store_true')
    args=parser.parse_args();site=args.site.resolve();root=args.source_root.resolve();public=site/'public'
    app_raw=(args.validation or root/'verification/final-validation.json').read_bytes()
    build_raw=(args.build_evidence or root/'dist/build-evidence.json').read_bytes()
    app,build=json.loads(app_raw),json.loads(build_raw)
    require(app.get('result')=='PASS' and build.get('result')=='PASS','Final application and build QA must be PASS')
    cases={'dev1','dev2','dev2-clone','standalone'}
    require(cases <= set(app.get('application_tests',{})) and all(app['application_tests'][c].get('result')=='PASS' for c in cases),'Four final application cases are required')
    artifacts={name:(root/'dist'/name).read_bytes() for name in [COMBINED,SINGLE]}
    for name,raw in artifacts.items():
        identity={'bytes':len(raw),'sha256':sha(raw)}
        require(build.get('artifacts',{}).get(name)==identity,'Build artifact identity differs: '+name)
        require(app.get('artifacts',{}).get(name)==identity,'Application artifact identity differs: '+name)
    feed=json.loads((public/'patches-bundle.json').read_bytes())
    require(feed['version']=='1.0.379','Existing Site baseline must be 1.0.379')
    require((public/'source').read_bytes()==(public/'patches-bundle.json').read_bytes(),'Baseline aliases differ')
    require((public/'CHANGELOG.md').read_text().startswith('# [1.0.379]'),'Baseline changelog differs')
    require(not(site/'scripts/newx-ui380').exists(),'Release380 already staged; inspect instead of overwriting')
    source=collect_sources(root)
    source[APP_QA]=source['verification/final-validation.json']=app_raw
    source[BUILD_QA]=source['verification/build-evidence.json']=build_raw
    source[README]=(HERE/README).read_bytes()
    source.setdefault('README.md',source[README])
    require(sum(map(len,source.values()))<=32*1024*1024,'Source archive exceeds expected size')
    source_zip=zipped(source)
    note='''【統合MPP v1.0.380：Xの表示回数を省略せず全桁表示】
・UI設定をv1.1.0へ更新し、項目名を「NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）」に変更。X 12.43.0-prod.01とPiko 3.12.0-dev.1／dev.2のNewX項目に対応します。Managerでの初期選択はオフです。
・X内の「Piko設定 → Hiroの表示設定」に「表示回数を省略せず表示」を追加。初期オンで、タイムラインの表示回数を3Kなどの省略表記から3,000のような全桁表記へ変更します。元の数値データから整形し、省略済み文字列から値を推測しません。いいね・返信・リポスト数の表示は変更しません。
・4項目は個別にオン・オフできます。既存のおすすめ非表示・上下バー固定・120Hz優先と設定値を保持します。表示回数・おすすめ・上下固定の変更後はXを再起動してください。120Hzの変更は設定画面からXへ戻る際に反映します。120Hzはアプリからの要求であり、省電力・発熱などOS側の制限は残ります。
・Hiroを更新し、未改造のX APKMへ新しい名称の項目とPiko NewXを一緒に適用し、生成APKをインストールしてください。旧UI設定v1.0.0との重複選択は不要です。ソース更新だけではインストール済みXは変わりません。
・Piko dev.1、dev.2、dev.2＋Clone、単独UI設定MPPの構成で適用・APK再構築と最終コード監査を確認。詳しい結果は公開QAを参照。Android実機の表示・操作・120Hz到達は未確認です。'''
    feed.update(version='1.0.380',created_at='2026-11-17T00:00:00',download_url='https://hiro-morphe-patches.otoha10.chatgpt.site/'+COMBINED,description=note+'\n\n'+feed['description'])
    feed_raw=(json.dumps(feed,ensure_ascii=False,indent=2)+'\n').encode()
    changelog='# [1.0.380](https://hiro-morphe-patches.otoha10.chatgpt.site) (2026-11-17)\n\n### Features\n\n* **X:** '+note.split('\n',1)[1].replace('\n',' ').replace('・','')+'\n\n'+(public/'CHANGELOG.md').read_text()
    writes={public/'patches-bundle.json':feed_raw,public/'source':feed_raw,public/'CHANGELOG.md':changelog.encode(),public/APP_QA:app_raw,public/BUILD_QA:build_raw,public/README:source[README],public/SOURCE:source_zip,public/SINGLE:artifacts[SINGLE]}
    for name in ['stage-newxui380.py','validate_newxui380.py']:writes[site/'scripts'/name]=(HERE/name).read_bytes()
    for name,raw in source.items():writes[site/'scripts/newx-ui380/source'/name]=raw
    # Reuse the bounded Worker implementation, with independently pinned prior assets.
    old_recipe=json.loads((site/'scripts/mpp-release-data/379/Hiro_Morphe_Patches_v1.0.379.mpp.delta379.json').read_bytes())
    reused={s['asset']['name']:{k:v for k,v in s['asset'].items()if k!='name'} for s in old_recipe['segments']}
    worker=(site/'worker/delta379-mpp.ts').read_text().replace('379','380')
    worker=re.sub(r'const BASE = .*?;', 'const BASE = "Hiro_Morphe_Patches_v1.0.379.mpp";',worker,count=1)
    worker=re.sub(r'const BASE_BYTES = \d+;',f'const BASE_BYTES = {BASE_BYTES};',worker,count=1)
    worker=re.sub(r'const BASE_SHA256 = "[a-f0-9]+";',f'const BASE_SHA256 = "{BASE_SHA}";',worker,count=1)
    worker=re.sub(r'const TARGET_BYTES = \d+;',f'const TARGET_BYTES = {len(artifacts[COMBINED])};',worker,count=1)
    worker=re.sub(r'const TARGET_SHA256 = "[a-f0-9]+";',f'const TARGET_SHA256 = "{sha(artifacts[COMBINED])}";',worker,count=1)
    start=worker.index('const REUSED_PARTS:');end=worker.index('\n};',start)+3
    worker=worker[:start]+'const REUSED_PARTS: Record<string, { bytes: number; sha256: string }> = '+json.dumps(reused,indent=2)+';'+worker[end:]
    writes[site/'worker/delta380-mpp.ts']=worker.encode()
    index=(site/'worker/index.ts').read_text()
    index=replace_once(index,'import { serveDelta379Mpp } from "./delta379-mpp";','import { serveDelta379Mpp } from "./delta379-mpp";\nimport { serveDelta380Mpp } from "./delta380-mpp";')
    index=replace_once(index,'async function serveMpp(request: Request, env: Env): Promise<Response> {','async function serveMpp(request: Request, env: Env): Promise<Response> {\n  const newXViewsDelta = await serveDelta380Mpp(request, env);\n  if (newXViewsDelta) return newXViewsDelta;')
    writes[site/'worker/index.ts']=index.encode()
    tests=(site/'tests/delta379-mpp.test.mjs').read_text().replace('379','380').replace('NewX_UI_Options_v1.0.0.mpp',SINGLE)
    tests=tests.replace(str(BASE_BYTES),str(len(artifacts[COMBINED]))).replace(BASE_SHA,sha(artifacts[COMBINED]))
    tests=replace_once(tests,'assert.equal(target.length, 42940);',f'assert.equal(target.length, {len(artifacts[SINGLE])});')
    tests=replace_once(tests,"assert.equal(hash(target), '1a5bcfb4ed5cf2d2757ca066d8667053b01dd97538f7df547fbdbe3e1b3d430d');",f"assert.equal(hash(target), '{sha(artifacts[SINGLE])}');")
    require('Math.floor(target.length / 2)' in tests,'Standalone resume range must be size-independent')
    location="  if (name.startsWith('Hiro_Morphe_Patches_v1.0.377.mpp.literal')) return path.join(site, 'scripts/mpp-release-data/377', name);"
    tests=replace_once(tests,location,location+"\n  if (name.startsWith('Hiro_Morphe_Patches_v1.0.379.mpp.literal')) return path.join(site, 'scripts/mpp-release-data/379', name);")
    writes[site/'tests/delta380-mpp.test.mjs']=tests.encode()
    rendered=(site/'tests/rendered-html.test.mjs').read_text()
    rendered=replace_once(rendered,r'class="source-version">v1\.0\.379<\/span>',r'class="source-version">v1\.0\.380<\/span>')
    rendered=replace_once(rendered,'assert.equal(descriptor.version, "1.0.379");','assert.equal(descriptor.version, "1.0.380");')
    anchor='  assert.doesNotMatch(html, developmentPreviewMeta);'
    ui_checks='''
  const viewCountSection = html.match(/<section\\b[^>]*aria-labelledby="newx-views-heading"[^>]*>(.*?)<\\/section>/s)?.[1];
  assert.ok(viewCountSection, "NewX full view count section is rendered");
  assert.match(viewCountSection, /Hiroの表示設定/);
  assert.match(viewCountSection, /3,000/);
  assert.match(viewCountSection, /元の数値データ/);
  assert.match(viewCountSection, /いいね・返信・リポスト/);
  assert.match(viewCountSection, /再起動/);
  assert.ok(viewCountSection.includes('href="/Hiro_Morphe_Patches_v1.0.380.mpp"'));
  assert.ok(viewCountSection.includes('href="/NewX_UI_Options_v1.1.0.mpp"'));
'''
    rendered=replace_once(rendered,anchor,anchor+ui_checks)
    writes[site/'tests/rendered-html.test.mjs']=rendered.encode()
    build_sh=(site/'scripts/build-verified.sh').read_text()
    for action in ['materialize','stage']:
        anchor=f'python3 "${{script_dir}}/stage-newxui379.py" {action}'
        require(anchor in build_sh,'Build anchor missing');build_sh=build_sh.replace(anchor,anchor+f'\npython3 "${{script_dir}}/stage-newxui380.py" {action}')
    anchor='TEST_BUILT_WORKER=1 MPP379_STAGED="${mpp_payload_dir}" node --test "${SITES_PROJECT_ROOT}/tests/delta379-mpp.test.mjs"'
    build_sh=replace_once(build_sh,anchor,anchor+'\nTEST_BUILT_WORKER=1 MPP380_STAGED="${mpp_payload_dir}" node --test "${SITES_PROJECT_ROOT}/tests/delta380-mpp.test.mjs"')
    writes[site/'scripts/build-verified.sh']=build_sh.encode()
    validator=(site/'scripts/validate_fast_mpp.py').read_text()
    anchor='    version = bundle["version"]\n'
    early='''    if version == "1.0.380":
        from validate_newxui380 import validate_release, reconstruct_release
        validate_changelog(changelog_path or bundle_path.with_name("CHANGELOG.md"), version, bundle["created_at"][:10])
        target = mpp_path or bundle_path.with_name("Hiro_Morphe_Patches_v1.0.380.mpp")
        if not target.exists(): target = bundle_path.parent / "_mpp" / "Hiro_Morphe_Patches_v1.0.380.mpp.bin"
        if target.exists():
            validate_release(bundle_path, target, bundle, validate_dex=validate_dex, dex_contract=dex_contract, dex_strings=dex_strings)
        else:
            raw = reconstruct_release(target)
            with tempfile.TemporaryDirectory(prefix="newxui380-validation-") as temp:
                recovered = Path(temp) / "Hiro_Morphe_Patches_v1.0.380.mpp"
                recovered.write_bytes(raw)
                validate_release(bundle_path, recovered, bundle, validate_dex=validate_dex, dex_contract=dex_contract, dex_strings=dex_strings)
        return
'''
    validator=replace_once(validator,anchor,anchor+early);writes[site/'scripts/validate_fast_mpp.py']=validator.encode()
    page=(site/'app/page.tsx').read_text()
    old='<p className="lead">Xのチャットおすすめ非表示・上下タブ固定・120Hz優先を、アプリ内で個別に切り替えられる項目を追加しました。</p>'
    new='<p className="lead">Xの表示回数を省略せず全桁表示する設定を追加しました。おすすめ非表示・上下タブ固定・120Hz優先も個別に切り替えられます。</p>'
    page=replace_once(page,old,new)
    section='''      <section className="incident shell" aria-labelledby="newx-views-heading">
        <p className="kicker">X / 12.43.0-prod.01 / UI設定 v1.1.0 / Hiro v1.0.380</p>
        <h2 id="newx-views-heading">表示回数を3Kから3,000のような全桁表記へ</h2>
        <p>X内の<strong>Piko設定 → Hiroの表示設定</strong>に<strong>表示回数を省略せず表示</strong>を追加。初期オンで、タイムラインの元の数値データを桁区切り付きで表示します。いいね・返信・リポスト数の表示は変更しません。切り替えた後はXを再起動してください。</p>
        <p>既存のおすすめ非表示・上下バー固定・120Hz優先と設定値は保持し、4項目を個別にオン・オフできます。120Hzはアプリからの優先要求で、省電力・発熱などOS側の制限は残ります。</p>
        <p>Morphe Managerで<strong>v1.0.380</strong>へ更新し、元の未改造<strong>X 12.43.0-prod.01のAPKM一式</strong>へ、<strong>Piko 3.12.0-dev.1またはdev.2のNewX項目</strong>と<strong>「NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）」</strong>を一緒に適用してください。新項目の初期選択はオフです。生成APKのインストールが必要です。</p>
        <p><a href="/Hiro_Morphe_Patches_v1.0.380.mpp">統合MPP v1.0.380</a> ／ <a href="/NewX_UI_Options_v1.1.0.mpp">UI設定だけのMPP v1.1.0</a> ／ <a href="/README_NewX_UI_Options_v1.1.0.md">導入方法と検証範囲</a></p>
        <p>既存のNewX日本語化・カテゴリ保持・絶対日時・戻る操作も引き続き選べます。実機の表示・操作・120Hz到達は未確認です。<a href="/QA_NewX_UI_Options_v1.1.0.json">適用検証記録</a> ／ <a href="/NewX_UI_Options_v1.1.0_sources_and_QA.zip">ソースと検証資料</a></p>
      </section>

'''
    anchor='      <section className="incident shell" aria-labelledby="newx-ui-heading">'
    page=replace_once(page,anchor,section+anchor);writes[site/'app/page.tsx']=page.encode()
    card=(site/'app/source-card.tsx').read_text();writes[site/'app/source-card.tsx']=replace_once(card,'v1.0.379','v1.0.380').encode()
    ignore=(site/'.gitignore').read_text();writes[site/'.gitignore']=(ignore.rstrip()+'\n/public/'+COMBINED+'\n').encode()
    for path,raw in writes.items():
        if path.name in [COMBINED,SINGLE,APP_QA,BUILD_QA,SOURCE,README]:require(not path.exists()or path.read_bytes()==raw,'Existing release asset differs: '+str(path))
    report={'result':'READY','files':len(writes),'source_entries':len(source),'source_zip_bytes':len(source_zip),'artifacts':build['artifacts']}
    if args.check_only:print(json.dumps(report,ensure_ascii=False));return
    # Prepare/reconstruct the delta before changing the Site metadata or code.
    spec=importlib.util.spec_from_file_location('_prepare_ui380',HERE/'stage-newxui380.py');delta=importlib.util.module_from_spec(spec);spec.loader.exec_module(delta)
    with tempfile.TemporaryDirectory(prefix='newxui380-')as temporary:
        prepared=Path(temporary);report['delivery']=delta.prepare(site,prepared,root/'dist'/COMBINED)
        for path,raw in writes.items():path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(raw)
        destination=site/'scripts/mpp-release-data/380';destination.mkdir(parents=True,exist_ok=True)
        for path in prepared.iterdir():shutil.copyfile(path,destination/path.name)
    (public/COMBINED).write_bytes(artifacts[COMBINED])
    print(json.dumps(report,ensure_ascii=False))

if __name__=='__main__':main()
