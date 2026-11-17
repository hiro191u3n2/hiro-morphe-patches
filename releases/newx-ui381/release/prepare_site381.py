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
COMBINED='Hiro_Morphe_Patches_v1.0.381.mpp'
SINGLE='NewX_UI_Options_v1.1.1.mpp'
APP_QA='QA_NewX_UI_Options_v1.1.1.json'
BUILD_QA='QA_build381.json'
README='README_NewX_UI_Options_v1.1.1.md'
SOURCE='NewX_UI_Options_v1.1.1_sources_and_QA.zip'
PATCH='NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）'
BASE_BYTES=48325446
BASE_SHA='3672fa208fc53f791e8e5c34b1f401709fdb1f19a701e9c2018d6ce0b01a201a'
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

def collect_sources(root, approved_reviews=()):
    """Strict authored-source allowlist; decoded APK directories are never walked."""
    paths=[root/name for name in ROOT_SOURCES if (root/name).is_file()]
    paths.extend(p for p in (root/'src').rglob('*') if p.is_file() and p.suffix in {'.java','.py','.xml','.md'})
    for directory in ['chat','bars','views']:
        paths.extend(p for p in (root/directory).glob('*') if p.is_file() and
                     (p.suffix in {'.java','.py','.md'} or p.name=='QA.json'))
    for directory in ['refresh','views-runtime']:
        paths.extend(p for p in (root/directory).rglob('*') if p.is_file() and
                     (p.suffix in {'.java','.py','.md'} or p.name=='QA.json'))
    if (root/'views-runtime/benchmark.json').is_file():
        paths.append(root/'views-runtime/benchmark.json')
    for review in approved_reviews:
        relative=Path(review)
        require(not relative.is_absolute() and '..' not in relative.parts and relative.parts and
                relative.parts[0] in {'performance-review','count-review'} and relative.suffix in {'.json','.md'},
                'Only approved sanitized performance/count review JSON or Markdown can be included')
        require((root/relative).is_file() and not(root/relative).is_symlink(), 'Approved review is missing or linked: '+str(relative))
        paths.append(root/relative)
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
    parser.add_argument('--approved-review',action='append',default=[],help='Explicitly approved sanitized performance-review/ or count-review/ JSON or Markdown path')
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
    require(feed['version']=='1.0.380','Existing Site baseline must be 1.0.380')
    require((public/'source').read_bytes()==(public/'patches-bundle.json').read_bytes(),'Baseline aliases differ')
    require((public/'CHANGELOG.md').read_text().startswith('# [1.0.380]'),'Baseline changelog differs')
    require(not(site/'scripts/newx-ui381').exists(),'Release381 already staged; inspect instead of overwriting')
    source=collect_sources(root,args.approved_review)
    source[APP_QA]=source['verification/final-validation.json']=app_raw
    source[BUILD_QA]=source['verification/build-evidence.json']=build_raw
    source[README]=(HERE/README).read_bytes()
    source['README.md']=source[README]
    require(sum(map(len,source.values()))<=32*1024*1024,'Source archive exceeds expected size')
    source_zip=zipped(source)
    note='''【統合MPP v1.0.381：Xの表示回数整形の重複処理を削減】
・UI設定をv1.1.1へ更新。表示回数の全桁表示で元の省略整形と全桁整形を重ねて実行していた処理を整理し、数値フォーマッターの毎回生成を減らしました。スクロール中に繰り返される整形処理の負担を下げる修正です。
・「NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）」の名称と4つのオン・オフ、保存済みの設定値は変更しません。元の数値に基づく表示回数の全桁表示と、いいね・返信・リポスト数の表示は保持します。
・対象はX 12.43.0-prod.01とPiko 3.12.0-dev.1／dev.2のNewX項目。Hiroをv1.0.381へ更新し、未改造のX APKMへ同じ表示設定項目とPiko NewXを適用し直し、生成APKをインストールしてください。ソース更新だけではインストール済みXは変わりません。
・設定場所は「Piko設定 → Hiroの表示設定」です。表示回数・おすすめ・上下固定の変更後はXを再起動し、120Hzの変更後は設定画面からXへ戻ります。120Hzはアプリからの優先要求で、省電力・発熱などOS側の制限は残ります。
・Piko dev.1、dev.2、dev.2＋Clone、単独UI設定MPPの構成で適用・APK再構築と最終コード監査を確認。Android実機でのスクロール改善量・最終表示・120Hz到達は未確認です。具体的な試験条件と結果は公開QAを参照してください。'''
    feed.update(version='1.0.381',created_at='2026-11-17T09:30:00',download_url='https://hiro-morphe-patches.otoha10.chatgpt.site/'+COMBINED,description=note+'\n\n'+feed['description'])
    feed_raw=(json.dumps(feed,ensure_ascii=False,indent=2)+'\n').encode()
    changelog='# [1.0.381](https://hiro-morphe-patches.otoha10.chatgpt.site) (2026-11-17)\n\n### Features\n\n* **X:** '+' '.join(line.removeprefix('・') for line in note.splitlines()[1:])+'\n\n'+(public/'CHANGELOG.md').read_text()
    writes={public/'patches-bundle.json':feed_raw,public/'source':feed_raw,public/'CHANGELOG.md':changelog.encode(),public/APP_QA:app_raw,public/BUILD_QA:build_raw,public/README:source[README],public/SOURCE:source_zip,public/SINGLE:artifacts[SINGLE]}
    for name in ['stage-newxui381.py','validate_newxui381.py']:writes[site/'scripts'/name]=(HERE/name).read_bytes()
    for name,raw in source.items():writes[site/'scripts/newx-ui381/source'/name]=raw
    # Reuse the bounded Worker implementation, with independently pinned prior assets.
    old_recipe=json.loads((site/'scripts/mpp-release-data/380/Hiro_Morphe_Patches_v1.0.380.mpp.delta380.json').read_bytes())
    reused={s['asset']['name']:{k:v for k,v in s['asset'].items()if k!='name'} for s in old_recipe['segments']}
    worker=(site/'worker/delta380-mpp.ts').read_text().replace('380','381')
    worker=re.sub(r'const BASE = .*?;', 'const BASE = "Hiro_Morphe_Patches_v1.0.380.mpp";',worker,count=1)
    worker=re.sub(r'const BASE_BYTES = \d+;',f'const BASE_BYTES = {BASE_BYTES};',worker,count=1)
    worker=re.sub(r'const BASE_SHA256 = "[a-f0-9]+";',f'const BASE_SHA256 = "{BASE_SHA}";',worker,count=1)
    worker=re.sub(r'const TARGET_BYTES = \d+;',f'const TARGET_BYTES = {len(artifacts[COMBINED])};',worker,count=1)
    worker=re.sub(r'const TARGET_SHA256 = "[a-f0-9]+";',f'const TARGET_SHA256 = "{sha(artifacts[COMBINED])}";',worker,count=1)
    start=worker.index('const REUSED_PARTS:');end=worker.index('\n};',start)+3
    worker=worker[:start]+'const REUSED_PARTS: Record<string, { bytes: number; sha256: string }> = '+json.dumps(reused,indent=2)+';'+worker[end:]
    writes[site/'worker/delta381-mpp.ts']=worker.encode()
    index=(site/'worker/index.ts').read_text()
    index=replace_once(index,'import { serveDelta380Mpp } from "./delta380-mpp";','import { serveDelta380Mpp } from "./delta380-mpp";\nimport { serveDelta381Mpp } from "./delta381-mpp";')
    index=replace_once(index,'async function serveMpp(request: Request, env: Env): Promise<Response> {','async function serveMpp(request: Request, env: Env): Promise<Response> {\n  const newXPerformanceDelta = await serveDelta381Mpp(request, env);\n  if (newXPerformanceDelta) return newXPerformanceDelta;')
    writes[site/'worker/index.ts']=index.encode()
    tests=(site/'tests/delta380-mpp.test.mjs').read_text().replace('380','381').replace('NewX_UI_Options_v1.1.0.mpp',SINGLE)
    tests=tests.replace(str(BASE_BYTES),str(len(artifacts[COMBINED]))).replace(BASE_SHA,sha(artifacts[COMBINED]))
    tests=replace_once(tests,'assert.equal(target.length, 49945);',f'assert.equal(target.length, {len(artifacts[SINGLE])});')
    tests=replace_once(tests,"assert.equal(hash(target), '17ca92ef30c3c582b665ac8118c0521b0db834c02519dcfd68317c265678b194');",f"assert.equal(hash(target), '{sha(artifacts[SINGLE])}');")
    require('Math.floor(target.length / 2)' in tests,'Standalone resume range must be size-independent')
    location="  if (name.startsWith('Hiro_Morphe_Patches_v1.0.377.mpp.literal')) return path.join(site, 'scripts/mpp-release-data/377', name);"
    tests=replace_once(tests,location,location+"\n  if (name.startsWith('Hiro_Morphe_Patches_v1.0.380.mpp.literal')) return path.join(site, 'scripts/mpp-release-data/380', name);")
    writes[site/'tests/delta381-mpp.test.mjs']=tests.encode()
    rendered=(site/'tests/rendered-html.test.mjs').read_text()
    rendered=replace_once(rendered,r'class="source-version">v1\.0\.380<\/span>',r'class="source-version">v1\.0\.381<\/span>')
    rendered=replace_once(rendered,'assert.equal(descriptor.version, "1.0.380");','assert.equal(descriptor.version, "1.0.381");')
    anchor='  assert.doesNotMatch(html, developmentPreviewMeta);'
    ui_checks='''
  const performanceSection = html.match(/<section\\b[^>]*aria-labelledby="newx-performance-heading"[^>]*>(.*?)<\\/section>/s)?.[1];
  assert.ok(performanceSection, "NewX formatting performance section is rendered");
  assert.match(performanceSection, /重複処理/);
  assert.match(performanceSection, /数値フォーマッター/);
  assert.match(performanceSection, /4つの設定/);
  assert.match(performanceSection, /実機でのスクロール改善量は未確認/);
  assert.ok(performanceSection.includes('href="/Hiro_Morphe_Patches_v1.0.381.mpp"'));
  assert.ok(performanceSection.includes('href="/NewX_UI_Options_v1.1.1.mpp"'));
'''
    rendered=replace_once(rendered,anchor,anchor+ui_checks)
    writes[site/'tests/rendered-html.test.mjs']=rendered.encode()
    build_sh=(site/'scripts/build-verified.sh').read_text()
    for action in ['materialize','stage']:
        anchor=f'python3 "${{script_dir}}/stage-newxui380.py" {action}'
        require(anchor in build_sh,'Build anchor missing');build_sh=build_sh.replace(anchor,anchor+f'\npython3 "${{script_dir}}/stage-newxui381.py" {action}')
    anchor='TEST_BUILT_WORKER=1 MPP380_STAGED="${mpp_payload_dir}" node --test "${SITES_PROJECT_ROOT}/tests/delta380-mpp.test.mjs"'
    build_sh=replace_once(build_sh,anchor,anchor+'\nTEST_BUILT_WORKER=1 MPP381_STAGED="${mpp_payload_dir}" node --test "${SITES_PROJECT_ROOT}/tests/delta381-mpp.test.mjs"')
    writes[site/'scripts/build-verified.sh']=build_sh.encode()
    validator=(site/'scripts/validate_fast_mpp.py').read_text()
    anchor='    version = bundle["version"]\n'
    early='''    if version == "1.0.381":
        from validate_newxui381 import validate_release, reconstruct_release
        validate_changelog(changelog_path or bundle_path.with_name("CHANGELOG.md"), version, bundle["created_at"][:10])
        target = mpp_path or bundle_path.with_name("Hiro_Morphe_Patches_v1.0.381.mpp")
        if not target.exists(): target = bundle_path.parent / "_mpp" / "Hiro_Morphe_Patches_v1.0.381.mpp.bin"
        if target.exists():
            validate_release(bundle_path, target, bundle, validate_dex=validate_dex, dex_contract=dex_contract, dex_strings=dex_strings)
        else:
            raw = reconstruct_release(target)
            with tempfile.TemporaryDirectory(prefix="newxui381-validation-") as temp:
                recovered = Path(temp) / "Hiro_Morphe_Patches_v1.0.381.mpp"
                recovered.write_bytes(raw)
                validate_release(bundle_path, recovered, bundle, validate_dex=validate_dex, dex_contract=dex_contract, dex_strings=dex_strings)
        return
'''
    validator=replace_once(validator,anchor,anchor+early);writes[site/'scripts/validate_fast_mpp.py']=validator.encode()
    page=(site/'app/page.tsx').read_text()
    old='<p className="lead">Xの表示回数を省略せず全桁表示する設定を追加しました。おすすめ非表示・上下タブ固定・120Hz優先も個別に切り替えられます。</p>'
    new='<p className="lead">Xの表示回数整形の重複処理を減らしました。既存の4つの表示設定と保存済みの設定値を保持します。</p>'
    page=replace_once(page,old,new)
    section='''      <section className="incident shell" aria-labelledby="newx-performance-heading">
        <p className="kicker">X / 12.43.0-prod.01 / UI設定 v1.1.1 / Hiro v1.0.381</p>
        <h2 id="newx-performance-heading">表示回数整形の重複処理を削減</h2>
        <p>全桁表示の際に元の省略整形と全桁整形を重ねて実行する処理を整理し、数値フォーマッターの毎回生成を減らしました。スクロール中に繰り返される整形処理の負担を下げる修正です。</p>
        <p>既存の4つの設定と保存済みの設定値は保持します。表示回数は元の数値から全桁表示し、いいね・返信・リポスト数の表示は変更しません。設定場所は<strong>Piko設定 → Hiroの表示設定</strong>です。</p>
        <p>Morphe Managerで<strong>v1.0.381</strong>へ更新し、元の未改造<strong>X 12.43.0-prod.01のAPKM一式</strong>へ、<strong>Piko 3.12.0-dev.1またはdev.2のNewX項目</strong>と<strong>「NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）」</strong>を適用し直してください。生成APKのインストールが必要です。</p>
        <p><a href="/Hiro_Morphe_Patches_v1.0.381.mpp">統合MPP v1.0.381</a> ／ <a href="/NewX_UI_Options_v1.1.1.mpp">UI設定だけのMPP v1.1.1</a> ／ <a href="/README_NewX_UI_Options_v1.1.1.md">導入方法と検証範囲</a></p>
        <p>実機でのスクロール改善量は未確認です。適用・コード監査と端末上の動作確認は区別しています。<a href="/QA_NewX_UI_Options_v1.1.1.json">適用検証記録</a> ／ <a href="/NewX_UI_Options_v1.1.1_sources_and_QA.zip">ソースと検証資料</a></p>
      </section>

'''
    anchor='      <section className="incident shell" aria-labelledby="newx-views-heading">'
    page=replace_once(page,anchor,section+anchor);writes[site/'app/page.tsx']=page.encode()
    card=(site/'app/source-card.tsx').read_text();writes[site/'app/source-card.tsx']=replace_once(card,'v1.0.380','v1.0.381').encode()
    ignore=(site/'.gitignore').read_text();writes[site/'.gitignore']=(ignore.rstrip()+'\n/public/'+COMBINED+'\n').encode()
    for path,raw in writes.items():
        if path.name in [COMBINED,SINGLE,APP_QA,BUILD_QA,SOURCE,README]:require(not path.exists()or path.read_bytes()==raw,'Existing release asset differs: '+str(path))
    report={'result':'READY','files':len(writes),'source_entries':len(source),'source_zip_bytes':len(source_zip),'artifacts':build['artifacts']}
    if args.check_only:print(json.dumps(report,ensure_ascii=False));return
    # Prepare/reconstruct the delta before changing the Site metadata or code.
    spec=importlib.util.spec_from_file_location('_prepare_ui381',HERE/'stage-newxui381.py');delta=importlib.util.module_from_spec(spec);spec.loader.exec_module(delta)
    with tempfile.TemporaryDirectory(prefix='newxui381-')as temporary:
        prepared=Path(temporary);report['delivery']=delta.prepare(site,prepared,root/'dist'/COMBINED)
        for path,raw in writes.items():path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(raw)
        destination=site/'scripts/mpp-release-data/381';destination.mkdir(parents=True,exist_ok=True)
        for path in prepared.iterdir():shutil.copyfile(path,destination/path.name)
    (public/COMBINED).write_bytes(artifacts[COMBINED])
    print(json.dumps(report,ensure_ascii=False))

if __name__=='__main__':main()
