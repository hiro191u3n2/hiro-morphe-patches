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
COMBINED='Hiro_Morphe_Patches_v1.0.382.mpp'
SINGLE='NewX_UI_Options_v1.1.2.mpp'
APP_QA='QA_NewX_UI_Options_v1.1.2.json'
BUILD_QA='QA_build382.json'
README='README_NewX_UI_Options_v1.1.2.md'
SOURCE='NewX_UI_Options_v1.1.2_sources_and_QA.zip'
PATCH='NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）'
BASE_BYTES=48325768
BASE_SHA='8266fbf1274b8f9651bb67b0a55264564036b8b90499995591f267b3277341c7'
ROOT_SOURCES={'build.py','integrate.py','rollback.py','rollback_build.py','verify_packages.py','finalize_qa.py','zip_helpers.py','MergeProviders.java',
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
    if (root/'count-review/review.json').is_file():
        paths.append(root/'count-review/review.json')
    for directory in ['review','verification']:
        paths.extend(p for p in (root/directory).glob('*') if p.is_file() and p.suffix in {'.java','.py','.md','.json'})
    paths.extend(p for p in (root/'release').glob('*') if p.is_file() and p.suffix in {'.py','.md'})
    output={}
    for path in sorted(set(paths)):
        rel=path.relative_to(root)
        if path.is_symlink() or any(part in OMIT_DIRS or part.endswith('-work') for part in rel.parts):continue
        if 'benchmark' in path.name.lower():continue
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
    require(app.get('result')=='PASS' and app.get('version')=='1.0.382' and build.get('result')=='PASS','Current382 application and build QA must be PASS')
    require(app.get('application_tests',{}).get('dev2',{}).get('result')=='PASS','New382 dev2 application case is required')
    require(build.get('rollback_payloads_match380') is True,'Exact380 rollback payload proof is required')
    baseline_raw=(public/'QA_NewX_UI_Options_v1.1.0.json').read_bytes()
    baseline=json.loads(baseline_raw)
    cases={'dev1','dev2','dev2-clone','standalone'}
    require(baseline.get('version')=='1.0.380' and baseline.get('result')=='PASS' and cases<=set(baseline.get('application_tests',{})) and all(baseline['application_tests'][c].get('result')=='PASS' for c in cases),'Historical380 application evidence is required')
    artifacts={name:(root/'dist'/name).read_bytes() for name in [COMBINED,SINGLE]}
    for name,raw in artifacts.items():
        identity={'bytes':len(raw),'sha256':sha(raw)}
        require(build.get('artifacts',{}).get(name)==identity,'Build artifact identity differs: '+name)
        require(app.get('artifacts',{}).get(name)==identity,'Application artifact identity differs: '+name)
    feed=json.loads((public/'patches-bundle.json').read_bytes())
    require(feed['version']=='1.0.381','Existing Site baseline must be 1.0.381')
    require((public/'source').read_bytes()==(public/'patches-bundle.json').read_bytes(),'Baseline aliases differ')
    require((public/'CHANGELOG.md').read_text().startswith('# [1.0.381]'),'Baseline changelog differs')
    require(not(site/'scripts/newx-ui382').exists(),'Release382 already staged; inspect instead of overwriting')
    source=collect_sources(root)
    source[APP_QA]=source['verification/final-validation.json']=app_raw
    source[BUILD_QA]=source['verification/build-evidence.json']=build_raw
    source['verification/baseline380-validation.json']=baseline_raw
    source[README]=(HERE/README).read_bytes()
    source['README.md']=source[README]
    require(sum(map(len,source.values()))<=32*1024*1024,'Source archive exceeds expected size')
    source_zip=zipped(source)
    note='''【統合MPP v1.0.382：v381の変更を取り下げ、v380のコードへ復元】
・v381適用後にスクロールがさらに重くなったとの報告を受け、v381の表示回数整形の変更を取り下げました。v382は統合MPP v380およびUI設定 v1.1.0と、マニフェストの版情報を除く全コード・リソースが一致する復元版です。
・Managerで更新として受け取れるよう、配布版番号をv1.0.382、UI設定版番号をv1.1.2に進めています。新たな最適化は加えていません。「NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）」と4つのオン・オフ、保存済み設定はそのままです。
・Hiroをv1.0.382へ更新し、未改造のX 12.43.0-prod.01のAPKM一式へ、Piko 3.12.0-dev.1／dev.2のNewX項目と同じ表示設定項目を適用し直し、生成APKをインストールしてください。ソース更新だけではインストール済みXは戻りません。
・設定場所は「Piko設定 → Hiroの表示設定」です。表示回数・おすすめ・上下固定の変更後はXを再起動し、120Hzの変更後は設定画面からXへ戻ります。120HzはOS側の制限を受けるアプリからの優先要求です。
・v382のdev.2構成を改めて適用・APK再構築し、復元ペイロードとv380の一致を検証しました。他構成の根拠は公開済みv380のdev.1／dev.2／dev.2＋Clone／単独UI設定MPPの検証記録で、v382での再試験とは区別します。Android実機での復元後の体感は未確認です。'''
    feed.update(version='1.0.382',created_at='2026-11-17T10:00:26',download_url='https://hiro-morphe-patches.otoha10.chatgpt.site/'+COMBINED,description=note+'\n\n'+feed['description'])
    feed_raw=(json.dumps(feed,ensure_ascii=False,indent=2)+'\n').encode()
    changelog='# [1.0.382](https://hiro-morphe-patches.otoha10.chatgpt.site) (2026-11-17)\n\n### Features\n\n* **X:** '+' '.join(line.removeprefix('・') for line in note.splitlines()[1:])+'\n\n'+(public/'CHANGELOG.md').read_text()
    writes={public/'patches-bundle.json':feed_raw,public/'source':feed_raw,public/'CHANGELOG.md':changelog.encode(),public/APP_QA:app_raw,public/BUILD_QA:build_raw,public/README:source[README],public/SOURCE:source_zip,public/SINGLE:artifacts[SINGLE]}
    for name in ['stage-newxui382.py','validate_newxui382.py']:writes[site/'scripts'/name]=(HERE/name).read_bytes()
    for name,raw in source.items():writes[site/'scripts/newx-ui382/source'/name]=raw
    # Reuse the bounded Worker implementation, with independently pinned prior assets.
    old_recipe=json.loads((site/'scripts/mpp-release-data/381/Hiro_Morphe_Patches_v1.0.381.mpp.delta381.json').read_bytes())
    reused={s['asset']['name']:{k:v for k,v in s['asset'].items()if k!='name'} for s in old_recipe['segments']}
    worker=(site/'worker/delta381-mpp.ts').read_text().replace('381','382')
    worker=re.sub(r'const BASE = .*?;', 'const BASE = "Hiro_Morphe_Patches_v1.0.381.mpp";',worker,count=1)
    worker=re.sub(r'const BASE_BYTES = \d+;',f'const BASE_BYTES = {BASE_BYTES};',worker,count=1)
    worker=re.sub(r'const BASE_SHA256 = "[a-f0-9]+";',f'const BASE_SHA256 = "{BASE_SHA}";',worker,count=1)
    worker=re.sub(r'const TARGET_BYTES = \d+;',f'const TARGET_BYTES = {len(artifacts[COMBINED])};',worker,count=1)
    worker=re.sub(r'const TARGET_SHA256 = "[a-f0-9]+";',f'const TARGET_SHA256 = "{sha(artifacts[COMBINED])}";',worker,count=1)
    start=worker.index('const REUSED_PARTS:');end=worker.index('\n};',start)+3
    worker=worker[:start]+'const REUSED_PARTS: Record<string, { bytes: number; sha256: string }> = '+json.dumps(reused,indent=2)+';'+worker[end:]
    writes[site/'worker/delta382-mpp.ts']=worker.encode()
    index=(site/'worker/index.ts').read_text()
    index=replace_once(index,'import { serveDelta381Mpp } from "./delta381-mpp";','import { serveDelta381Mpp } from "./delta381-mpp";\nimport { serveDelta382Mpp } from "./delta382-mpp";')
    index=replace_once(index,'async function serveMpp(request: Request, env: Env): Promise<Response> {','async function serveMpp(request: Request, env: Env): Promise<Response> {\n  const newXRollbackDelta = await serveDelta382Mpp(request, env);\n  if (newXRollbackDelta) return newXRollbackDelta;')
    writes[site/'worker/index.ts']=index.encode()
    tests=(site/'tests/delta381-mpp.test.mjs').read_text().replace('381','382').replace('NewX_UI_Options_v1.1.1.mpp',SINGLE)
    tests=tests.replace(str(BASE_BYTES),str(len(artifacts[COMBINED]))).replace(BASE_SHA,sha(artifacts[COMBINED]))
    tests=replace_once(tests,'assert.equal(target.length, 50236);',f'assert.equal(target.length, {len(artifacts[SINGLE])});')
    tests=replace_once(tests,"assert.equal(hash(target), '078bee06a01a3289a654fbba5eedd952619467e5a7ff2a7fe7da9ca297061161');",f"assert.equal(hash(target), '{sha(artifacts[SINGLE])}');")
    require('Math.floor(target.length / 2)' in tests,'Standalone resume range must be size-independent')
    location="  if (name.startsWith('Hiro_Morphe_Patches_v1.0.377.mpp.literal')) return path.join(site, 'scripts/mpp-release-data/377', name);"
    tests=replace_once(tests,location,location+"\n  if (name.startsWith('Hiro_Morphe_Patches_v1.0.381.mpp.literal')) return path.join(site, 'scripts/mpp-release-data/381', name);")
    writes[site/'tests/delta382-mpp.test.mjs']=tests.encode()
    rendered=(site/'tests/rendered-html.test.mjs').read_text()
    rendered=replace_once(rendered,r'class="source-version">v1\.0\.381<\/span>',r'class="source-version">v1\.0\.382<\/span>')
    rendered=replace_once(rendered,'assert.equal(descriptor.version, "1.0.381");','assert.equal(descriptor.version, "1.0.382");')
    rendered=replace_once(rendered,'assert.match(performanceSection, /重複処理/);','assert.match(performanceSection, /取り下げ済み/);')
    anchor='  assert.doesNotMatch(html, developmentPreviewMeta);'
    ui_checks='''
  const rollbackSection = html.match(/<section\\b[^>]*aria-labelledby="newx-rollback-heading"[^>]*>(.*?)<\\/section>/s)?.[1];
  assert.ok(rollbackSection, "NewX rollback section is rendered");
  assert.match(rollbackSection, /v381の変更を取り下げ/);
  assert.match(rollbackSection, /v380のコード/);
  assert.match(rollbackSection, /4つの設定/);
  assert.match(rollbackSection, /実機での復元後の体感は未確認/);
  assert.ok(rollbackSection.includes('href="/Hiro_Morphe_Patches_v1.0.382.mpp"'));
  assert.ok(rollbackSection.includes('href="/NewX_UI_Options_v1.1.2.mpp"'));
'''
    rendered=replace_once(rendered,anchor,anchor+ui_checks)
    writes[site/'tests/rendered-html.test.mjs']=rendered.encode()
    build_sh=(site/'scripts/build-verified.sh').read_text()
    for action in ['materialize','stage']:
        anchor=f'python3 "${{script_dir}}/stage-newxui381.py" {action}'
        require(anchor in build_sh,'Build anchor missing');build_sh=build_sh.replace(anchor,anchor+f'\npython3 "${{script_dir}}/stage-newxui382.py" {action}')
    anchor='TEST_BUILT_WORKER=1 MPP381_STAGED="${mpp_payload_dir}" node --test "${SITES_PROJECT_ROOT}/tests/delta381-mpp.test.mjs"'
    build_sh=replace_once(build_sh,anchor,anchor+'\nTEST_BUILT_WORKER=1 MPP382_STAGED="${mpp_payload_dir}" node --test "${SITES_PROJECT_ROOT}/tests/delta382-mpp.test.mjs"')
    writes[site/'scripts/build-verified.sh']=build_sh.encode()
    validator=(site/'scripts/validate_fast_mpp.py').read_text()
    anchor='    version = bundle["version"]\n'
    early='''    if version == "1.0.382":
        from validate_newxui382 import validate_release, reconstruct_release
        validate_changelog(changelog_path or bundle_path.with_name("CHANGELOG.md"), version, bundle["created_at"][:10])
        target = mpp_path or bundle_path.with_name("Hiro_Morphe_Patches_v1.0.382.mpp")
        if not target.exists(): target = bundle_path.parent / "_mpp" / "Hiro_Morphe_Patches_v1.0.382.mpp.bin"
        if target.exists():
            validate_release(bundle_path, target, bundle, validate_dex=validate_dex, dex_contract=dex_contract, dex_strings=dex_strings)
        else:
            raw = reconstruct_release(target)
            with tempfile.TemporaryDirectory(prefix="newxui382-validation-") as temp:
                recovered = Path(temp) / "Hiro_Morphe_Patches_v1.0.382.mpp"
                recovered.write_bytes(raw)
                validate_release(bundle_path, recovered, bundle, validate_dex=validate_dex, dex_contract=dex_contract, dex_strings=dex_strings)
        return
'''
    validator=replace_once(validator,anchor,anchor+early);writes[site/'scripts/validate_fast_mpp.py']=validator.encode()
    page=(site/'app/page.tsx').read_text()
    old='<p className="lead">Xの表示回数整形の重複処理を減らしました。既存の4つの表示設定と保存済みの設定値を保持します。</p>'
    new='<p className="lead">v381の変更を取り下げ、v380のコードへ戻しました。更新用の配布版はv382です。4つの表示設定は引き続き利用できます。</p>'
    page=replace_once(page,old,new)
    section='''      <section className="incident shell" aria-labelledby="newx-rollback-heading">
        <p className="kicker">X / 12.43.0-prod.01 / UI設定 v1.1.2 / Hiro v1.0.382</p>
        <h2 id="newx-rollback-heading">v381の変更を取り下げ、v380のコードへ復元</h2>
        <p>v381適用後にスクロールがさらに重くなったとの報告を受け、表示回数整形の変更を取り下げました。v380と、マニフェストの版情報を除くすべてのコード・リソースを一致させています。新たな最適化は加えていません。</p>
        <p>Managerで更新として受け取れるように配布版をv382としています。既存の4つの設定と保存済みの設定値は保持します。設定場所は<strong>Piko設定 → Hiroの表示設定</strong>です。</p>
        <p>Morphe Managerで<strong>v1.0.382</strong>へ更新し、元の未改造<strong>X 12.43.0-prod.01のAPKM一式</strong>へ、<strong>Piko 3.12.0-dev.1またはdev.2のNewX項目</strong>と<strong>「NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）」</strong>を適用し直してください。生成APKのインストールが必要です。</p>
        <p><a href="/Hiro_Morphe_Patches_v1.0.382.mpp">復元版の統合MPP v1.0.382</a> ／ <a href="/NewX_UI_Options_v1.1.2.mpp">UI設定だけの復元MPP v1.1.2</a> ／ <a href="/README_NewX_UI_Options_v1.1.2.md">導入方法と検証範囲</a></p>
        <p>v382のdev.2構成を再適用・APK再構築し、v380とのペイロード一致を確認しました。他構成の適用根拠はv380時点の検証記録です。実機での復元後の体感は未確認です。<a href="/QA_NewX_UI_Options_v1.1.2.json">復元検証記録</a> ／ <a href="/NewX_UI_Options_v1.1.2_sources_and_QA.zip">ソースと検証資料</a></p>
      </section>

'''
    anchor='      <section className="incident shell" aria-labelledby="newx-performance-heading">'
    page=replace_once(page,anchor,section+anchor)
    # Keep the historical entry and downloads while marking381 as withdrawn.
    page=replace_once(page,'<h2 id="newx-performance-heading">表示回数整形の重複処理を削減</h2>','<h2 id="newx-performance-heading">旧v381：取り下げ済みの整形処理変更</h2>\n        <p><strong>この変更はv382で取り下げました。上の復元版を使用してください。</strong></p>')
    writes[site/'app/page.tsx']=page.encode()
    card=(site/'app/source-card.tsx').read_text();writes[site/'app/source-card.tsx']=replace_once(card,'v1.0.381','v1.0.382').encode()
    ignore=(site/'.gitignore').read_text();writes[site/'.gitignore']=(ignore.rstrip()+'\n/public/'+COMBINED+'\n').encode()
    for path,raw in writes.items():
        if path.name in [COMBINED,SINGLE,APP_QA,BUILD_QA,SOURCE,README]:require(not path.exists()or path.read_bytes()==raw,'Existing release asset differs: '+str(path))
    report={'result':'READY','files':len(writes),'source_entries':len(source),'source_zip_bytes':len(source_zip),'artifacts':build['artifacts']}
    if args.check_only:print(json.dumps(report,ensure_ascii=False));return
    # Prepare/reconstruct the delta before changing the Site metadata or code.
    spec=importlib.util.spec_from_file_location('_prepare_ui382',HERE/'stage-newxui382.py');delta=importlib.util.module_from_spec(spec);spec.loader.exec_module(delta)
    with tempfile.TemporaryDirectory(prefix='newxui382-')as temporary:
        prepared=Path(temporary);report['delivery']=delta.prepare(site,prepared,root/'dist'/COMBINED)
        for path,raw in writes.items():path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(raw)
        destination=site/'scripts/mpp-release-data/382';destination.mkdir(parents=True,exist_ok=True)
        for path in prepared.iterdir():shutil.copyfile(path,destination/path.name)
    (public/COMBINED).write_bytes(artifacts[COMBINED])
    print(json.dumps(report,ensure_ascii=False))

if __name__=='__main__':main()
