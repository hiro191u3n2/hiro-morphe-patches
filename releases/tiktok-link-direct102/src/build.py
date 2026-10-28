#!/usr/bin/env python3
"""Deterministically upgrade only the existing TikTok link patch in bundle309.

This build checks loader preservation and metadata. Original-APK application
evidence is produced separately and required by publication, never invented here.
"""
from pathlib import Path, PurePosixPath
import argparse, hashlib, json, os, subprocess, zipfile

ROOT = Path(__file__).resolve().parent
VERSION, BASE_VERSION, BUNDLE_VERSION = '1.0.2', '1.0.309', '1.0.310'
BUILD_TIMESTAMP = '2026-10-28T06:18:24'
BASE_SHA = 'a4b9523f3ecc89899590f5fab4f7bb7622bdd5535e72c24373206a0fdc3e34c0'
BASE_BYTES = 19348490
BASE_URL = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/827e3eac3c04069714d34089804015c21166f195/downloads/Hiro_Morphe_Patches_v1.0.309.mpp'
SINGLE = 'TikTok_LinkDirect_v1.0.2.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.310.mpp'
QA = 'QA_TikTok_LinkDirect_v1.0.2.json'
MF = 'META-INF/MANIFEST.MF'
PREFIX = 'app/hiro/tiktoklink/patches/'
NOTICE_PREFIX = 'META-INF/hiro-tiktok-link-direct102/'
EXTENSION = 'extensions/hiro_tiktok_link_direct.mpe'
RUNTIME_PREFIX = 'app/hiro/tiktoklink/runtime/'
BASE_ONEBACK_EXTENSION = {'path':'extensions/oneback_exit.mpe','bytes':166028,
    'sha256':'43a0d988f1f11e0ccfd91a5a0562f386ea4a932d8d1419d5cf401b46b8865965'}
OWNED_BASE_JVM = {
    PREFIX+'FastRoutePatch$1.class':'a699a6426943ceccda57193bf1e4a7d4e6828178a00d8571348b054905d612cf',
    PREFIX+'FastRoutePatch$Plan.class':'24712660c4143d238fcdc9cc3727c038687fb3d95c9c591a80e439952720928d',
    PREFIX+'FastRoutePatch.class':'51d1346dd8fa99be5ea2ea7d40cdbc1b23f4db68732d842cccc80c1384a87119',
    PREFIX+'TikTokLinkDirectPatch$1.class':'f5e6209db798adf66a463810fbb765d5598f143056ae7108031d960bff4281fe',
    PREFIX+'TikTokLinkDirectPatch.class':'b3e76fc14cfb11c6fa186374c17fb0483fdbddfbfa462232220b46ae5cf0b0a3',
}
TOOLS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'morphe200.jar': 'deb5d931f3899b20a119637473d338c52585f0c561b5d8b2856b916fe90163b5',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}

def require(ok, message):
    if not ok: raise RuntimeError(message)

def sha(path):
    h = hashlib.sha256()
    with Path(path).open('rb') as f:
        for chunk in iter(lambda: f.read(1024*1024), b''): h.update(chunk)
    return h.hexdigest()

def sha_bytes(raw): return hashlib.sha256(raw).hexdigest()
def json_bytes(value): return (json.dumps(value, ensure_ascii=False, indent=2)+'\n').encode()

def checked_path(name):
    p = PurePosixPath(name)
    require(name and not p.is_absolute() and '..' not in p.parts and '\\' not in name
            and str(p) == name and not name.endswith('/'), 'Unsafe archive path '+name)

def archive(path):
    with zipfile.ZipFile(path) as z:
        require(len(z.namelist()) == len(set(z.namelist())), 'Duplicate ZIP entries')
        result = {}
        for name in z.namelist():
            checked_path(name.rstrip('/'))
            if not name.endswith('/'): result[name] = z.read(name)
        return result

def write_zip(path, entries):
    with zipfile.ZipFile(path, 'w') as z:
        for name, raw in sorted(entries.items()):
            checked_path(name)
            info = zipfile.ZipInfo(name, (2026,10,28,0,0,0))
            info.create_system = 3; info.external_attr = 0o100644 << 16
            z.writestr(info, raw, compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)

def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(rows, 'Orphan manifest continuation'); rows[-1] += line[1:]
        elif line: rows.append(line)
    values = [line.decode().split(': ', 1) for line in rows]
    require(all(len(row) == 2 for row in values), 'Malformed manifest')
    require(len(values) == len(dict(values)), 'Duplicate manifest keys')
    return dict(values)

def manifest(fields):
    rows = []
    for key, value in fields.items():
        require(isinstance(value, str) and not any(x in value for x in '\r\n\0'), 'Invalid manifest value')
        raw, prefix = (key+': '+value).encode(), b''
        while len(prefix)+len(raw) > 72:
            cut = 72-len(prefix)
            while raw[cut] & 192 == 128: cut -= 1
            rows.append(prefix+raw[:cut]); raw, prefix = raw[cut:], b' '
        rows.append(prefix+raw)
    result = b'\r\n'.join(rows)+b'\r\n\r\n'
    require(headers(result) == fields, 'Manifest roundtrip mismatch')
    return result

def source_ids(root):
    return {p.relative_to(root).as_posix(): sha(p) for p in sorted(root.rglob('*'))
            if p.is_file() and '__pycache__' not in p.parts and p.suffix not in ('.pyc','.class')}

def class_files(root): return {p.relative_to(root).as_posix():p.read_bytes() for p in sorted(root.rglob('*.class'))}

def validate_baseline(base):
    owned = {name for name in base if name.startswith(PREFIX)}
    require(owned == set(OWNED_BASE_JVM), 'Published own-loader inventory differs')
    for name,pin in OWNED_BASE_JVM.items():
        require(sha_bytes(base[name]) == pin, 'Published own-loader hash differs: '+name)
    require(not any(name.startswith(NOTICE_PREFIX) or name == EXTENSION for name in base),
            'New release notice or runtime already exists in baseline')
    oneback = base.get(BASE_ONEBACK_EXTENSION['path'])
    require(oneback is not None and len(oneback) == BASE_ONEBACK_EXTENSION['bytes']
            and sha_bytes(oneback) == BASE_ONEBACK_EXTENSION['sha256'], 'Published OneBack runtime identity differs')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('base','tools','work','dist'): parser.add_argument('--'+name, type=Path, required=True)
    args = parser.parse_args()
    for name in vars(args): setattr(args, name, getattr(args, name).resolve())
    require(sha(args.base) == BASE_SHA and args.base.stat().st_size == BASE_BYTES, 'Use exact published baseline309')
    for name, pin in TOOLS.items(): require(sha(args.tools/name) == pin, 'Tool identity differs: '+name)
    require(not args.work.exists(), 'Build work directory must be new')
    require(not args.dist.exists() or not any(args.dist.iterdir()), 'Dist directory must be empty')
    args.work.mkdir(parents=True); args.dist.mkdir(parents=True, exist_ok=True)
    sources = source_ids(ROOT)
    def run(command, name, timeout=300):
        print('RUN '+name, flush=True)
        with (args.work/name).open('w') as log:
            result = subprocess.run(list(map(str,command)), stdout=log, stderr=subprocess.STDOUT, timeout=timeout)
        value = (args.work/name).read_text()
        require(result.returncode == 0, name+' failed\n'+value[-12000:])
        return value.strip()
    require('Temurin-21.0.8+9' in run(['java','--version'],'java.txt'), 'Temurin 21.0.8+9 required')
    require(run(['javac','-version'],'javac.txt') == 'javac 21.0.8', 'javac 21.0.8 required')
    for name in ('patch','patch-dex','audit'): (args.work/name).mkdir()
    patch_sources = sorted((ROOT/'patch').glob('*.java'))
    require(patch_sources, 'No actual patch implementation found')
    run(['javac','--release','8','-encoding','UTF-8','-cp',args.tools/'morphe.jar','-d',args.work/'patch',*patch_sources], 'patch-javac.log')
    fresh = class_files(args.work/'patch')
    require(fresh and all(n.startswith(PREFIX) for n in fresh), 'New loaders must use dedicated namespace')
    write_zip(args.work/'patch.jar', fresh)
    run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26',
         '--lib',args.tools/'android.jar','--classpath',args.tools/'morphe.jar','--output',args.work/'patch-dex',args.work/'patch.jar'], 'loader-d8.log')
    require({p.name for p in (args.work/'patch-dex').iterdir()} == {'classes.dex'}, 'Unexpected multidex loader')
    auditcp = os.pathsep.join(map(str,[args.work/'audit',args.tools/'morphe.jar']))
    run(['javac','--release','8','-encoding','UTF-8','-cp',auditcp,'-d',args.work/'audit',
         ROOT/'test/BundleMergeAudit.java',ROOT/'test/MetadataAudit.java',ROOT/'test/RuntimeExtensionAudit.java'], 'audit-javac.log')
    base = archive(args.base)
    require(headers(base[MF])['Version'] == BASE_VERSION, 'Baseline manifest version differs')
    validate_baseline(base)
    (args.work/'baseline.dex').write_bytes(base['classes.dex'])
    merge = run(['java','-ea','-cp',auditcp,'BundleMergeAudit',args.work/'baseline.dex',
                 args.work/'patch-dex/classes.dex',args.work/'combined.dex',args.work/'single.dex'], 'merge-audit.txt')
    require(merge.startswith('PASS BundleMergeAudit'), 'Loader preservation audit failed')
    runtime_sources = sorted((ROOT/'runtime').rglob('*.java')) if (ROOT/'runtime').exists() else []
    require(runtime_sources, 'Dedicated shared-link runtime implementation required')
    for name in ('runtime','runtime-dex','runtime-host'): (args.work/name).mkdir()
    run(['javac','--release','8','-encoding','UTF-8','-cp',args.tools/'android.jar','-d',args.work/'runtime',*runtime_sources], 'runtime-javac.log')
    runtime_classes = class_files(args.work/'runtime')
    require(runtime_classes and all(n.startswith(RUNTIME_PREFIX) for n in runtime_classes), 'Runtime must use dedicated namespace only')
    write_zip(args.work/'runtime.jar',runtime_classes)
    run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',args.tools/'android.jar',
         '--output',args.work/'runtime-dex',args.work/'runtime.jar'], 'runtime-d8.log')
    require({p.name for p in (args.work/'runtime-dex').iterdir()} == {'classes.dex'}, 'Unexpected runtime multidex')
    runtime_audit = run(['java','-ea','-cp',auditcp,'RuntimeExtensionAudit',args.work/'runtime-dex/classes.dex',
                         args.work/'runtime.jar',args.base], 'runtime-extension-audit.txt')
    require(runtime_audit.startswith('PASS RuntimeExtensionAudit '), 'Serialized runtime namespace/inventory audit failed')
    host_sources = sorted((ROOT/'test-stubs').rglob('*.java')) + sorted((ROOT/'test-runtime').rglob('*.java'))
    require(host_sources and (ROOT/'test-stubs').is_dir() and (ROOT/'test-runtime').is_dir(), 'Independent runtime host tests required')
    host_compile_cp = os.pathsep.join(map(str,(args.work/'runtime',args.tools/'android.jar')))
    run(['javac','--release','8','-encoding','UTF-8','-cp',host_compile_cp,'-d',args.work/'runtime-host',*host_sources], 'runtime-host-javac.log')
    require(not set(class_files(args.work/'runtime-host')) & set(runtime_classes), 'Host fixtures must not replace production runtime classes')
    host_run_cp = os.pathsep.join(map(str,(args.work/'runtime-host',args.work/'runtime')))
    runtime_host = run(['java','-ea','-cp',host_run_cp,'app.hiro.tiktoklink.runtime.SharedLinkExitHostTest'], 'runtime-host-test.txt')
    require(runtime_host.startswith('PASS SharedLinkExitHostTest '), 'Actual compiled runtime host tests failed')
    fresh[EXTENSION] = (args.work/'runtime-dex/classes.dex').read_bytes()
    runtime_identity = {'path':EXTENSION,'bytes':len(fresh[EXTENSION]),'sha256':sha_bytes(fresh[EXTENSION])}
    for name in ('LICENSE','NOTICE'): fresh[NOTICE_PREFIX+name] = (ROOT.parent/name).read_bytes()
    old_fields = headers(base[MF]); combined_fields = dict(old_fields)
    combined_fields.update(Version=BUNDLE_VERSION, Timestamp=BUILD_TIMESTAMP,
        Description='Upgrade existing TikTok 47.4.3 shared-video link handling; preserve all unrelated patches and resources.')
    own_fields = {'Manifest-Version':'1.0','Name':'Hiro TikTok Link Direct','Version':VERSION,
        'Author':'Hiro contributors','Source':'https://github.com/hiro191u3n2/hiro-morphe-patches',
        'Description':'Direct shared-video link handling for original global TikTok 47.4.3.',
        'License':'GPL-3.0-only','Patcher-Version':'2.0.0','Timestamp':BUILD_TIMESTAMP}
    retained_base = {n:r for n,r in base.items() if n not in OWNED_BASE_JVM}
    combined = {**retained_base,**fresh,MF:manifest(combined_fields),'classes.dex':(args.work/'combined.dex').read_bytes()}
    single = {**fresh,MF:manifest(own_fields),'classes.dex':(args.work/'single.dex').read_bytes()}
    preserved = {n:r for n,r in base.items() if n not in (MF,'classes.dex') and n not in OWNED_BASE_JVM}
    require(all(combined.get(n) == raw for n,raw in preserved.items()), 'Unrelated resource or JVM loader changed')
    require(set(combined) == set(retained_base)|set(fresh), 'Aggregate entry inventory differs')
    for name, entries in ((SINGLE,single),(BUNDLE,combined)):
        write_zip(args.dist/name,entries); require(archive(args.dist/name) == entries, 'Archive roundtrip failed')
    metadata = {}
    for sdk in ('morphe.jar','morphe200.jar'):
        cp = os.pathsep.join(map(str,[args.work/'audit',args.tools/sdk]))
        result = run(['java','-ea','-cp',cp,'MetadataAudit',args.base,args.dist/SINGLE,args.dist/BUNDLE],sdk+'.metadata.txt')
        require(result.startswith('PASS MetadataAudit'), 'Metadata audit failed')
        metadata[sdk] = result
    require(source_ids(ROOT) == sources, 'Sources changed during build')
    proof = {'schema':'tiktok-link-direct-build-v3','version':VERSION,'bundle_version':BUNDLE_VERSION,
        'baseline_version':BASE_VERSION,'baseline_sha256':BASE_SHA,'target_package':'com.zhiliaoapp.musically',
        'target_version':'47.4.3','physical_android_tested':False,'sources':sources,'toolchain_sha256':TOOLS,
        'loader_preservation':merge,'metadata':metadata,'all_nonowned_loader_definitions_preserved':True,
        'all_nonowned_zip_entries_preserved_except_manifest_and_loader_dex':True,'preserved_entries':len(preserved),
        'replaced_baseline_jvm_sha256':OWNED_BASE_JVM,
        'changed_existing_entries':sorted(n for n in set(base)&set(combined) if base[n] != combined[n]),
        'removed_owned_entries':sorted(set(base)-set(combined)),
        'added_entries':sorted(set(combined)-set(base)),'runtime_added':True,
        'runtime_extension':runtime_identity,'runtime_classes':sorted(runtime_classes),
        'runtime_extension_audit':runtime_audit,'runtime_host_test':runtime_host,
        'baseline_oneback_extension':BASE_ONEBACK_EXTENSION,
        'artifacts':{name:{'bytes':(args.dist/name).stat().st_size,'sha256':sha(args.dist/name)} for name in (SINGLE,BUNDLE)}}
    (args.dist/QA).write_bytes(json_bytes(proof))
    print('PASS BUILD '+json.dumps(proof['artifacts']),flush=True)

if __name__ == '__main__': main()
