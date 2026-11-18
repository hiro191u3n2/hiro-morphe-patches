#!/usr/bin/env python3
"""Compile the independent NewX Japanese provider against actual Patcher2.4 APIs."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent
TRANSLATIONS = ROOT.parent / 'translations'
RESOURCE_FILES = (
    'res/values-ja/hiro_piko_ja.xml',
    'res/values/hiro_piko_ui.xml',
    'res/values-ja/hiro_piko_ui.xml',
)


def require(ok, why):
    if not ok:
        raise ValueError(why)


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def run(*args):
    result = subprocess.run([str(x) for x in args], text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    print(result.stdout, end='')
    result.check_returncode()
    return result.stdout


def android_text(text):
    # Outer Android quotes preserve whitespace and an initial @/? as literal text.
    return '"' + text.replace('\\', '\\\\').replace('"', '\\"').replace("'", "\\'").replace('\n', '\\n').replace('\t', '\\t').replace('\r', '\\u000d') + '"'


def xml_strings(rows, language):
    root = ET.Element('resources')
    for resource, values in sorted(rows.items()):
        node = ET.SubElement(root, 'string', name=resource, formatted='false')
        node.text = android_text(values[language])
    ET.indent(root, space='    ')
    return ET.tostring(root, encoding='utf-8', xml_declaration=True) + b'\n'


def archive(path, entries):
    with zipfile.ZipFile(path, 'w') as z:
        for name, raw in sorted(entries.items(), key=lambda item: (item[0] != 'META-INF/MANIFEST.MF', item[0] != 'classes.dex', item[0])):
            info = zipfile.ZipInfo(name, (2026, 11, 17, 0, 0, 0))
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            z.writestr(info, raw, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--tools', type=Path, required=True)
    parser.add_argument('--patcher', type=Path, required=True)
    parser.add_argument('--apk', type=Path, help='Already rebuilt Piko NewX APK for final-code host audit')
    parser.add_argument('--extension', type=Path, action='append', default=[], help='Unmodified upstream extension DEX archive; repeat for NewX/shared instead of --apk')
    parser.add_argument('--piko', type=Path, action='append', required=True, help='Official Piko MPP for real selected dependency-graph audit; repeat for every supported version')
    args = parser.parse_args()
    require(bool(args.apk) != bool(args.extension), 'Use either --apk or one or more --extension inputs')
    bytecode_inputs = [args.apk] if args.apk else args.extension
    inputs = [args.tools/'ecj.jar', args.tools/'d8.jar', args.patcher] + bytecode_inputs + args.piko
    for path in inputs:
        require(path.is_file(), 'Missing input: ' + str(path))
    targets = json.loads((TRANSLATIONS/'hardcoded-ja.json').read_bytes())
    require(isinstance(targets, list) and targets, 'Empty hardcoded translation mapping')
    helpers, sites = {}, set()
    for row in targets:
        require(all(isinstance(row[k], str) and row[k] for k in ('class','method','returnType','source','ja','resource','evidence')),
                'Invalid target string metadata')
        require(re.fullmatch(r'hiro_piko_newx_[a-z0-9_]+', row['resource']), 'Invalid helper resource name')
        require(type(row['expectedCount']) is int and row['expectedCount'] > 0, 'Invalid literal count')
        require(isinstance(row['parameterTypes'], list) and all(isinstance(t, str) for t in row['parameterTypes']), 'Invalid method parameters')
        key = (row['class'], row['method'], tuple(row['parameterTypes']), row['returnType'], row['source'])
        require(key not in sites, 'Duplicate literal target: ' + str(key)); sites.add(key)
        pair = {'source': row['source'], 'ja': row['ja']}
        require(row['resource'] not in helpers or helpers[row['resource']] == pair, 'Conflicting helper translation')
        helpers[row['resource']] = pair
    translated = (TRANSLATIONS/'newx-ja.xml').read_bytes()
    tree = ET.fromstring(translated)
    require(tree.tag == 'resources' and len(tree) == 485, 'Expected exactly485 NewX translations')
    original = ET.parse(TRANSLATIONS/'upstream-newx-strings.xml').getroot()
    expected_names = {node.attrib['name'] for node in original if node.attrib.get('translatable') != 'false'}
    require(all(node.tag == 'string' for node in tree) and {node.attrib['name'] for node in tree} == expected_names, 'Unexpected NewX resource')
    require(len({node.attrib['name'] for node in tree}) == 485, 'Duplicate NewX resource name')
    generated = ROOT/'build'
    for name in ('classes', 'testclasses', 'dex'):
        path = generated/name
        if path.exists(): shutil.rmtree(path)
        path.mkdir(parents=True)
    quote = lambda value: json.dumps(value, ensure_ascii=False)
    definitions = []
    for row in targets:
        parameters = 'new String[]{' + ','.join(map(quote, row['parameterTypes'])) + '}'
        definitions.append('new NewXJapanesePatch.Target(' + ','.join((quote(row['class']), quote(row['method']), parameters,
            quote(row['returnType']), quote(row['source']), quote(row['resource']), str(row['expectedCount']))) + ')')
    java = ('package app.hiro.newx.patches;\nfinal class NewXJapaneseData {\n'
            'static final String[] RESOURCE_PATHS = {' + ','.join(map(quote, RESOURCE_FILES)) + '};\n'
            'static NewXJapanesePatch.Target[] targets() { return new NewXJapanesePatch.Target[]{\n'
            + ',\n'.join(definitions) + '\n}; }\n}\n')
    data_source = generated/'NewXJapaneseData.java'; data_source.write_text(java)
    resources = dict(zip(RESOURCE_FILES, (translated, xml_strings(helpers, 'source'), xml_strings(helpers, 'ja'))))
    for name, raw in resources.items():
        path = generated/'resources'/name; path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(raw)
    compiler = ['java','-XX:-UsePerfData','-jar',args.tools/'ecj.jar','-8','-nowarn','-encoding','UTF-8']
    run(*compiler, '-cp', args.patcher, '-d', generated/'classes', ROOT/'NewXJapanesePatch.java', data_source)
    classpath = os.pathsep.join(map(str, (generated/'classes', args.patcher)))
    run(*compiler, '-cp', classpath, '-d', generated/'testclasses', ROOT/'NewXJapanesePatchAudit.java')
    audit = run('java','-XX:-UsePerfData','-Xmx2g','-cp',os.pathsep.join((str(generated/'testclasses'),classpath)),
                'NewXJapanesePatchAudit',os.pathsep.join(map(str,bytecode_inputs)),os.pathsep.join(map(str,args.piko)),generated/'audited-targets.dex')
    classes = {p.relative_to(generated/'classes').as_posix(): p.read_bytes() for p in (generated/'classes').rglob('*.class')}
    require(classes and all(n.startswith('app/hiro/newx/patches/') for n in classes), 'Unexpected provider/compile stub class')
    archive(generated/'provider.jar', classes)
    run('java','-XX:-UsePerfData','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26',
        '--classpath',args.patcher,'--output',generated/'dex',generated/'provider.jar')
    manifest = ('Manifest-Version: 1.0\r\nName: NewX Piko Japanese\r\nVersion: 1.0.1\r\n'
                'Description: Japanese Piko NewX UI with Piko dev.3 support\r\nTimestamp: 2026-11-18T00:00:00\r\n'
                'Patcher-Version: 2.4.0\r\nSource: remote\r\nAuthor: Hiro\r\nLicense: GPLv3\r\n\r\n').encode()
    entries = dict(classes, **{'classes.dex': (generated/'dex/classes.dex').read_bytes(), 'META-INF/MANIFEST.MF': manifest})
    entries.update({'newx-ja/' + name: raw for name, raw in resources.items()})
    for name in ('LICENSE','NOTICE'):
        entries['newx-ja/' + name] = (ROOT.parent/'upstream'/name).read_bytes()
    archive(ROOT/'component.mpp', entries)
    qa = {'result':'PASS','component_sha256':digest((ROOT/'component.mpp').read_bytes()),'target':'12.43.0-prod.01',
          'default':False,'newx_japanese_strings':485,'helper_strings':len(helpers),'literal_targets':len(targets),
          'literal_replacements':sum(row['expectedCount'] for row in targets),'runtime_extension_added':False,
          'host_audit':audit.strip(),'device_tested':False,
          'resource_sha256':{name:digest(raw) for name,raw in resources.items()},
          'bytecode_audit_input_kind':'rebuilt_apk' if args.apk else 'upstream_extensions',
          'input_sha256':{str(path):digest(path.read_bytes()) for path in bytecode_inputs + args.piko + [args.patcher]},
          'source_sha256':{path.name:digest(path.read_bytes()) for path in (ROOT/'NewXJapanesePatch.java',ROOT/'NewXJapanesePatchAudit.java',ROOT/'build.py',data_source,TRANSLATIONS/'hardcoded-ja.json',TRANSLATIONS/'newx-ja.xml')}}
    (ROOT/'QA.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:v for k,v in qa.items() if k in ('result','component_sha256','helper_strings','literal_replacements')},ensure_ascii=False))

if __name__ == '__main__':
    main()
