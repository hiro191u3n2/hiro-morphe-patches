#!/usr/bin/env python3
"""Build the scoped Instagram external-profile Back fix with preservation gates.

Builds never claim physical device operation. Publication separately requires
both final MPPs to be applied to the original Instagram 452 APK and audited.
"""
from pathlib import Path, PurePosixPath
import argparse, hashlib, json, os, re, subprocess, zipfile

ROOT = Path(__file__).resolve().parent
VERSION, BASE_VERSION, BUNDLE_VERSION = '1.1.23', '1.0.308', '1.0.309'
BUILD_TIMESTAMP = '2026-10-28T05:30:00'
BASE_SHA = 'a7f9fd4c88c2f794fade005df7bd327f40dccedcdfee9f012271da9cfb568e34'
BASE_URL = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/49940825b57afdf8054bfe6c2d414670d15cd8fb/downloads/Hiro_Morphe_Patches_v1.0.308.mpp'
SINGLE = 'OneBack_Exit_NoRecents_v1.1.23.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.309.mpp'
QA = 'QA_HomeBack_v1.1.23.json'
MF = 'META-INF/MANIFEST.MF'
PREFIX = 'app/hiro/oneback/patches/'
NOTICE_PREFIX = 'META-INF/hiro-homeback130/'
EXTENSION = 'extensions/oneback_exit.mpe'
BASE_RUNTIME_SHA = '9ded0e61c6935515c7c6d101f6f2518310816902a9b46d5765efc30baf31dca0'
TOOLS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'morphe22.jar': 'fe4bc92815bb14976150e4ec83e4985932b46d1d47edec75d37604481572e864',
    'morphe200.jar': 'deb5d931f3899b20a119637473d338c52585f0c561b5d8b2856b916fe90163b5',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}
REQUIRED_HOST_TESTS = {
    'Back116Regression','Back117InstagramTest','Back117TikTokTest','Back118InstagramGuardTest',
    'Back118InstagramTest','Back119InstagramTest','Back119TwitterTest','Back120InstagramTest',
    'Back120MainDispatchTest','Back121InstagramActivityLayersTest','Back121InstagramCurrentTest',
    'Back121InstagramDispatchTest','Back121InstagramScopeTest','Back122InstagramDispatcherTest',
    'Back123InstagramTabBackTest','Back123NativeReelsTest','Back124TwitterTest','Back125TwitterKeyTest',
    'Back125TwitterSystemTest','Back125TwitterTerminalTest','HomeBackHostTest',
    'Back126InstagramProfileTest','Back127Instagram452Test','Back128InstagramProfileTest',
    'Back4743TikTokTest','Back4743NativeBackTest','Back4753RejectedTest',
    'Back130InstagramProfileLinkTest',
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
            if p.is_file() and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.dex')}

def class_files(root): return {p.relative_to(root).as_posix():p.read_bytes() for p in sorted(root.rglob('*.class'))}


def owned_entry(name):
    return name.startswith(PREFIX) or name == EXTENSION

def validate_baseline(base):
    require(headers(base[MF])['Version'] == BASE_VERSION, 'Baseline manifest version differs')
    require(PREFIX+'OneBackPatch.class' in base and sum(n.startswith(PREFIX) for n in base) == 15,
            'Published OneBack loader inventory differs')
    require(sha_bytes(base[EXTENSION]) == BASE_RUNTIME_SHA, 'Published OneBack runtime differs')
    require(not any(n.startswith(NOTICE_PREFIX) for n in base), 'Release notice already exists')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('base','tools','work','dist'): parser.add_argument('--'+name, type=Path, required=True)
    args = parser.parse_args()
    for name in vars(args): setattr(args, name, getattr(args, name).resolve())
    require(sha(args.base) == BASE_SHA, 'Use exact published baseline308')
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
    for name in ('patch','patch-dex','runtime','runtime-dex','host','host4743','host4753','audit','fixtures'):
        (args.work/name).mkdir()
    run(['javac','--release','8','-encoding','UTF-8','-cp',args.tools/'morphe.jar','-d',args.work/'patch',
         *sorted((ROOT/'patch').glob('*.java'))], 'patch-javac.log')
    run(['javac','--release','8','-encoding','UTF-8','-cp',args.tools/'android.jar','-d',args.work/'runtime',
         *sorted((ROOT/'runtime').glob('*.java'))], 'runtime-javac.log')
    modern = {p.stem for p in (ROOT/'test-runtime').glob('Back*.java')
              if p.stem.startswith(('Back4743','Back4753')) and 'public static void main' in p.read_text()}
    hostcp = os.pathsep.join(map(str,[args.work/'host',args.work/'runtime',args.tools/'android.jar']))
    classic = [p for p in sorted((ROOT/'test-runtime').glob('*.java')) if p.stem not in modern]
    run(['javac','--release','8','-encoding','UTF-8','-cp',hostcp,'-d',args.work/'host',
         *sorted((ROOT/'test-stubs').rglob('*.java')),*classic], 'host-javac.log')
    host = {}
    for source in classic:
        if 'public static void main' not in source.read_text(): continue
        value = run(['java','-ea','-cp',hostcp,'app.hiro.oneback.runtime.'+source.stem],source.stem+'.txt')
        require('PASS' in value or 'SUCCESS:' in value or re.search(r'REGRESSION: [0-9]+ cases, 0 failed',value),
                'Host success marker missing: '+source.stem)
        host[source.stem] = value.splitlines()[-1]
    for abi in ('4743','4753'):
        names = sorted(n for n in modern if n.startswith('Back'+abi))
        folder = args.work/('host'+abi)
        cp = os.pathsep.join(map(str,[folder,args.work/'host',args.work/'runtime',args.tools/'android.jar']))
        run(['javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',folder,
             *sorted((ROOT/('test-stubs'+abi)).rglob('*.java')),
             *[ROOT/'test-runtime'/(n+'.java') for n in names]], 'host'+abi+'-javac.log')
        for name in names:
            value = run(['java','-ea','-cp',cp,'app.hiro.oneback.runtime.'+name],name+'.txt')
            require(value.splitlines()[-1].startswith('PASS '+name), 'Native ABI regression failed: '+name)
            host[name] = value.splitlines()[-1]
    require(REQUIRED_HOST_TESTS <= set(host), 'Required existing/new regression missing')
    auditcp = os.pathsep.join(map(str,[args.work/'audit',args.work/'patch',args.tools/'morphe.jar']))
    run(['javac','-encoding','UTF-8','-cp',auditcp,'-d',args.work/'audit',
         *sorted((ROOT/'test-dex').glob('*.java')),*sorted((ROOT/'publication-audit').glob('*.java'))], 'audit-javac.log')
    structure = run(['java','-ea','-cp',auditcp,'HomeBackAudit','selftest',args.work/'fixtures'],'structure.txt')
    require(structure.splitlines()[-1].startswith('PASS selftest:'), 'Native wrapper structure audit failed')
    for kind, minimum in (('patch','26'),('runtime','29')):
        write_zip(args.work/(kind+'.jar'),class_files(args.work/kind))
        run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api',minimum,
             '--lib',args.tools/'android.jar','--classpath',args.tools/('morphe.jar' if kind=='patch' else 'android.jar'),
             '--output',args.work/(kind+'-dex'),args.work/(kind+'.jar')],kind+'-d8.log')
        require({p.name for p in (args.work/(kind+'-dex')).iterdir()} == {'classes.dex'}, 'Unexpected multidex '+kind)
    base = archive(args.base); validate_baseline(base)
    (args.work/'baseline-loader.dex').write_bytes(base['classes.dex'])
    (args.work/'baseline-runtime.dex').write_bytes(base[EXTENSION])
    merge = run(['java','-ea','-cp',auditcp,'Bundle130Audit','merge',args.work/'baseline-loader.dex',
                 args.work/'patch-dex/classes.dex',args.work/'combined.dex',args.work/'single.dex'],'loader-preservation.txt')
    runtime = run(['java','-ea','-cp',auditcp,'Bundle130Audit','runtime',args.work/'baseline-runtime.dex',
                   args.work/'runtime-dex/classes.dex',args.work/'verified-runtime.dex'],'runtime-preservation.txt')
    fresh = class_files(args.work/'patch')
    require(fresh and all(n.startswith(PREFIX) for n in fresh), 'Unexpected patch namespace')
    fresh[EXTENSION] = (args.work/'verified-runtime.dex').read_bytes()
    assets = {p.relative_to(ROOT/'assets').as_posix():p.read_bytes() for p in sorted((ROOT/'assets').rglob('*')) if p.is_file()}
    prior_assets = {n:r for n,r in base.items() if re.fullmatch(r'upgrades/oneback-runtime[0-9]+\.dex',n)}
    require(assets == prior_assets and len(assets) == 7, 'Existing runtime upgrade assets must remain exact')
    fresh.update(assets)
    for name in ('LICENSE','NOTICE'): fresh[NOTICE_PREFIX+name] = (ROOT.parent/name).read_bytes()
    fields = headers(base[MF]); fields.update(Version=BUNDLE_VERSION,Timestamp=BUILD_TIMESTAMP,
        Description='Instagram 452 external-profile Back exit; profile child navigation and unrelated patches preserved.')
    single_fields = {'Manifest-Version':'1.0','Name':'OneBack Exit No Recents','Version':VERSION,
        'Author':'Hiro contributors','Source':'https://github.com/hiro191u3n2/hiro-morphe-patches',
        'Description':'Instagram external profile entry Back exit with preserved native child navigation.',
        'License':'GPL-3.0-only with inherited notices','Patcher-Version':'2.0.0','Timestamp':BUILD_TIMESTAMP}
    retained = {n:r for n,r in base.items() if not owned_entry(n)}
    combined = {**retained,**fresh,MF:manifest(fields),'classes.dex':(args.work/'combined.dex').read_bytes()}
    single = {**fresh,MF:manifest(single_fields),'classes.dex':(args.work/'single.dex').read_bytes()}
    preserved = {n:r for n,r in retained.items() if n not in (MF,'classes.dex')}
    require(all(combined.get(n) == raw for n,raw in preserved.items()), 'Unrelated archive entry changed')
    require(set(combined) == set(retained)|set(fresh), 'Archive entry inventory differs')
    for name,entries in ((SINGLE,single),(BUNDLE,combined)):
        write_zip(args.dist/name,entries); require(archive(args.dist/name) == entries, 'Archive roundtrip failed')
    metadata = {}
    for sdk in ('morphe.jar','morphe22.jar','morphe200.jar'):
        cp = os.pathsep.join(map(str,[args.work/'audit',args.tools/sdk]))
        value = run(['java','-ea','-cp',cp,'Metadata130Audit',args.base,args.dist/SINGLE,args.dist/BUNDLE],sdk+'.metadata.txt')
        require(value.startswith('PASS Metadata130Audit'), 'Actual loader metadata audit failed')
        metadata[sdk] = value
    require(source_ids(ROOT) == sources, 'Sources changed during build')
    proof = {'schema':'homeback130-build-v1','version':VERSION,'bundle_version':BUNDLE_VERSION,
        'baseline_version':BASE_VERSION,'baseline_sha256':BASE_SHA,'target_package':'com.instagram.android',
        'target_version':'452.1.0.53.64','physical_android_tested':False,'sources':sources,'toolchain_sha256':TOOLS,
        'host':host,'dex_structure':structure.splitlines()[-1],'loader_preservation':merge,'runtime_preservation':runtime,
        'runtime_sha256':sha_bytes(fresh[EXTENSION]),'metadata':metadata,
        'all_nonowned_loader_definitions_preserved':True,'all_nonowned_zip_entries_preserved_except_manifest_and_loader_dex':True,
        'preserved_entries':len(preserved),'changed_existing_entries':sorted(n for n in set(base)&set(combined) if base[n] != combined[n]),
        'added_entries':sorted(set(combined)-set(base)),'removed_owned_entries':sorted(set(base)-set(combined)),
        'artifacts':{name:{'bytes':(args.dist/name).stat().st_size,'sha256':sha(args.dist/name)} for name in (SINGLE,BUNDLE)}}
    (args.dist/QA).write_bytes(json_bytes(proof))
    print('PASS BUILD '+json.dumps(proof['artifacts']),flush=True)

if __name__ == '__main__': main()
