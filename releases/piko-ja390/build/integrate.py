"""Replace the NewX Japanese provider in the verified 1.0.389 bundle."""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import subprocess
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def build(base, component, tools, out):
    if digest(base) != 'ed49643607f33fbd9eac07bc327471e016ca65d4bc7e3067e50296a010939d43':
        raise ValueError('Expected the published 1.0.389 bundle')
    out.mkdir(parents=True, exist_ok=True)
    work = out / 'work'
    work.mkdir(exist_ok=True)
    with ZipFile(base) as old, ZipFile(component) as new:
        changes = {}
        for name in new.namelist():
            if name in ('classes.dex', 'META-INF/MANIFEST.MF'):
                continue
            if name in old.namelist() and old.read(name) == new.read(name):
                continue
            provider_class = name.startswith('app/hiro/newx/patches/NewXJapanesePatch') and name.endswith('.class') and name in old.namelist()
            if not provider_class and name != 'newx-ja/picker-grid-ja.xml':
                raise ValueError('Unexpected changed component file: ' + name)
            changes[name] = new.read(name)
        if not {'app/hiro/newx/patches/NewXJapanesePatch.class', 'newx-ja/picker-grid-ja.xml'} <= changes.keys():
            raise ValueError('Expected the provider and grid translations')
        for file, archive in [('base.dex', old), ('component.dex', new)]:
            (work / file).write_bytes(archive.read('classes.dex'))
        subprocess.run(['java','-jar',tools/'ecj.jar','-11','-cp',tools/'morphe.jar','-d',work,
                        Path(__file__).with_name('MergeJapanese.java')],check=True)
        subprocess.run(['java','-cp',f'{work}:{tools}/morphe.jar','MergeJapanese',work/'base.dex',
                        work/'component.dex',work/'merged.dex',out/'preservation.json'], check=True)
        changes['classes.dex'] = (work/'merged.dex').read_bytes()
        manifest = old.read('META-INF/MANIFEST.MF')
        changes['META-INF/MANIFEST.MF'] = manifest.replace(b'Version: 1.0.389',b'Version: 1.0.390').replace(
            b'2026-11-21T00:00:00',b'2026-11-21T05:20:00').replace(
            b'Instagram 456.0.0.53.78 Back support.',b'Japanese NewX media picker support for Piko 3.12.0-dev.5.')
        target = out/'Hiro_Morphe_Patches_v1.0.390.mpp'
        with ZipFile(target, 'w') as result, base.open('rb') as source:
            entries = old.infolist()
            for i, entry in enumerate(entries):
                info = copy.copy(entry)
                if info.filename in changes:
                    result.writestr(info,changes[info.filename])
                    continue
                end = entries[i+1].header_offset if i+1 < len(entries) else old.start_dir
                source.seek(info.header_offset)
                raw = source.read(end-info.header_offset)
                info.header_offset = result.fp.tell()
                result.fp.write(raw)
                result.filelist.append(info)
                result.NameToInfo[info.filename] = info
                result.start_dir = result.fp.tell()
            for name in changes.keys()-set(old.namelist()):
                result.writestr(copy.copy(new.getinfo(name)), changes[name])
        with ZipFile(target) as built:
            if built.testzip() or set(built.namelist()) != set(old.namelist())|set(changes):
                raise ValueError('Output ZIP inventory differs')
            for name in built.namelist():
                if built.read(name) != (changes[name] if name in changes else old.read(name)):
                    raise ValueError('Output differs: '+name)
        print(target, target.stat().st_size, digest(target))
if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for key in ('base','component','tools','out'):
        parser.add_argument('--'+key,required=True,type=Path)
    build(**vars(parser.parse_args()))
