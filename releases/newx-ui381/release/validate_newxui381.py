#!/usr/bin/env python3
"""Verify v381 identities, prior payload preservation, source binding and QA."""
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import zipfile

VERSION = '1.0.381'
COMBINED = 'Hiro_Morphe_Patches_v1.0.381.mpp'
SINGLE = 'NewX_UI_Options_v1.1.1.mpp'
BUILD_QA = 'QA_build381.json'
APP_QA = 'QA_NewX_UI_Options_v1.1.1.json'
SOURCE = 'NewX_UI_Options_v1.1.1_sources_and_QA.zip'
README = 'README_NewX_UI_Options_v1.1.1.md'
BASE_SHA = '3672fa208fc53f791e8e5c34b1f401709fdb1f19a701e9c2018d6ce0b01a201a'
OLD_SINGLE = 'NewX_UI_Options_v1.1.0.mpp'
OLD_SINGLE_SHA = '17ca92ef30c3c582b665ac8118c0521b0db834c02519dcfd68317c265678b194'
CASES = {'dev1', 'dev2', 'dev2-clone', 'standalone'}

def require(ok, message):
    if not ok: raise ValueError('NewXUI381: '+message)

def digest(raw): return hashlib.sha256(raw).hexdigest()

def archive(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        names = z.namelist()
        require(len(names) == len(set(names)) and z.testzip() is None, 'invalid ZIP')
        require(all(not n.startswith('/') and '\\' not in n and '..' not in Path(n).parts for n in names), 'unsafe ZIP path')
        return {n:z.read(n) for n in names}

def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n',b'\n').splitlines():
        if line.startswith(b' '): rows[-1] += line[1:]
        elif line: rows.append(line)
    return dict(r.decode().split(': ',1) for r in rows)

def engine():
    root = Path(__file__).resolve().parents[1]
    spec = importlib.util.spec_from_file_location('_newxui381_storage',root/'scripts/stage-newxui381.py')
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    return root,module

def reconstruct_release(staged_path):
    root,storage = engine()
    recipe = json.loads(staged_path.with_name(COMBINED+'.delta381.json').read_bytes())
    return storage.engine(root).reconstruct(recipe, lambda name: (staged_path.parent/name).read_bytes())

def validate_release(bundle_path, mpp_path, bundle, *, validate_dex, dex_contract, dex_strings):
    root,storage = engine()
    require(bundle['version'] == VERSION and bundle['download_url'].endswith('/'+COMBINED), 'descriptor differs')
    require(bundle_path.with_name('source').read_bytes() == bundle_path.read_bytes(), 'descriptor alias differs')
    history = bundle_path.with_name('CHANGELOG.md').read_text().split('\n# ',1)[0]
    require('**X:**' in history and '表示回数' in history and '120Hz' in history and '12.43.0-prod.01' in history, 'X changelog missing')
    build_raw = bundle_path.with_name(BUILD_QA).read_bytes(); build = json.loads(build_raw)
    app_raw = bundle_path.with_name(APP_QA).read_bytes(); app = json.loads(app_raw)
    require(build.get('result') == 'PASS' and app.get('result') == 'PASS', 'QA not PASS')
    require(CASES <= set(app.get('application_tests',{})) and all(app['application_tests'][c].get('result') == 'PASS' for c in CASES), 'application cases missing/failed')
    _,base_raw = storage.previous(root).read_source(root,root/'scripts/mpp-release-data/380')
    require(digest(base_raw) == BASE_SHA, 'baseline380 differs')
    current_raw = mpp_path.read_bytes()
    single_path = bundle_path.with_name(SINGLE)
    if not single_path.exists(): single_path = bundle_path.parent/'_mpp'/(SINGLE+'.bin')
    single_raw = single_path.read_bytes()
    for name,raw in [(COMBINED,current_raw),(SINGLE,single_raw)]:
        expected = {'bytes':len(raw),'sha256':digest(raw)}
        require(build.get('artifacts',{}).get(name) == expected, 'build artifact differs: '+name)
        require(app.get('artifacts',{}).get(name) == expected, 'application artifact differs: '+name)
    old_single_path = bundle_path.with_name(OLD_SINGLE)
    if not old_single_path.exists(): old_single_path = bundle_path.parent/'_mpp'/(OLD_SINGLE+'.bin')
    old_single_raw = old_single_path.read_bytes()
    require(digest(old_single_raw) == OLD_SINGLE_SHA, 'previous UI component differs')
    base,current,single,old_single = map(archive,[base_raw,current_raw,single_raw,old_single_raw])
    reserved = {'META-INF/MANIFEST.MF','classes.dex'}
    old_ui = set(old_single)-reserved
    new_ui = set(single)-reserved
    require(old_ui <= set(base) and all(base[n] == old_single[n] for n in old_ui), 'baseline UI component differs')
    unchanged = set(base)-reserved-old_ui
    require(set(current) == unchanged|reserved|new_ui, 'unexpected removed or duplicate component payload')
    require(all(base[n] == current[n] for n in unchanged), 'non-UI application payload changed')
    require(all(single[n] == current[n] for n in new_ui), 'upgraded standalone payload differs')
    require(headers(current['META-INF/MANIFEST.MF'])['Version'] == VERSION and headers(single['META-INF/MANIFEST.MF'])['Version'] == '1.1.1', 'manifest versions differ')
    for label,items in [('combined',current),('standalone',single)]:
        validate_dex(items['classes.dex'],label+' loader')
        for name,data in items.items():
            if name.endswith('.mpe'): validate_dex(data,label+' extension '+name)
    old_types,_ = dex_contract(base['classes.dex']); new_types,_ = dex_contract(current['classes.dex'])
    previous_ui_types,_ = dex_contract(old_single['classes.dex'])
    single_types,_ = dex_contract(single['classes.dex'])
    non_ui_types = old_types-previous_ui_types
    require(previous_ui_types <= old_types and not non_ui_types & single_types, 'UI provider collision')
    require(new_types == non_ui_types|single_types, 'upgraded provider class set differs')
    require(all(t.startswith('Lapp/hiro/newxui/patches/') for t in single_types), 'unexpected UI provider namespace')
    preservation = build.get('dex_preservation',{})
    require(preservation.get('result') == 'PASS' and preservation.get('existing_non_ui_provider_classes_preserved') is True,
            'non-UI provider content preservation missing')
    require({'com.twitter.android','12.43.0-prod.01','3.12.0-dev.1','3.12.0-dev.2'} <= dex_strings(single['classes.dex']), 'target guard missing')
    source = archive(bundle_path.with_name(SOURCE).read_bytes())
    require(not any(Path(n).suffix.lower() in {'.apk','.apkm','.apks','.jar','.dex','.class','.keystore','.jks','.mpp','.mpe','.png','.mp4'} for n in source), 'non-source file in source ZIP')
    require(source[BUILD_QA] == build_raw and source[APP_QA] == app_raw, 'source QA binding differs')
    require(source[README] == bundle_path.with_name(README).read_bytes(), 'source README differs')
    for name,data in source.items():
        require((root/'scripts/newx-ui381/source'/name).read_bytes() == data, 'tracked source differs: '+name)
    print('Validated NewXUI381: tested combined/standalone identities, previous payloads, provider classes, extension DEX, four APK cases and source bindings. Runtime scope is recorded in QA.')
