#!/usr/bin/env python3
"""Inspect compiled resource table: preserve all baseline strings and verify 16 additions."""
import argparse, hashlib, json, re, xml.etree.ElementTree as ET, zipfile
from pathlib import Path
import arsc as parser

def main():
    p=argparse.ArgumentParser();p.add_argument('baseline',type=Path);p.add_argument('apk',type=Path);p.add_argument('component',type=Path);p.add_argument('report',type=Path);a=p.parse_args()
    failures=[]
    name_lookups=set(json.loads((Path(__file__).parent/'name_lookup_resources.json').read_text()))
    relocated={}
    with zipfile.ZipFile(a.baseline) as before, zipfile.ZipFile(a.apk) as after, zipfile.ZipFile(a.component) as component:
        old=parser.Arsc(before.read('resources.arsc')).entries
        new=parser.Arsc(after.read('resources.arsc')).entries
        same=0
        for key, configs in old.items():
            for config, entry in configs.items():
                current=new.get(key,{}).get(config,{})
                if {k:v for k,v in current.items() if k!='id'} != {k:v for k,v in entry.items() if k!='id'}:
                    failures.append('Existing resource value changed: '+key+'/'+config)
                else:
                    same+=1
                    if current.get('id')!=entry.get('id'):
                        # The 176 v1.0.378 helper strings are all loaded by exact NAME
                        # through ResourceUtils.getStringOrThrow(String). Adding eight
                        # earlier-sorted names reallocates their IDs; no native ID moves.
                        if key not in name_lookups or int(current['id'],16)-int(entry['id'],16)!=8:
                            failures.append('Unexpected existing resource ID change: '+key+'/'+config)
                        relocated[key]={'before':entry['id'],'after':current.get('id')}
        checks=[]
        expected_names=set()
        for config,path in [('', 'newx-ui/values.xml'),('ja','newx-ui/values-ja.xml')]:
            root=ET.fromstring('<resources>'+component.read(path).decode()+'</resources>')
            for node in root:
                key=node.attrib['name'];expected=parser.android_string(node.text or '')
                expected_names.add(key)
                actual=new.get(key,{}).get(config,{})
                ok=actual.get('value')==expected
                checks.append({'name':key,'config':config,'passed':ok,'resource_id':actual.get('id')})
                if not ok:failures.append('Missing or incorrect addition: '+key+'/'+config)
        if set(new)-set(old)!=expected_names:failures.append('Unexpected new resource names: '+str(sorted((set(new)-set(old))^expected_names)))
        for name in expected_names:
            if set(new.get(name,{}))!={'','ja'}:failures.append('Unexpected new resource configurations: '+name)
        payloads=0;changed_payloads=[];missing=[]
        for entry in before.infolist():
            n=entry.filename
            if entry.is_dir() or re.fullmatch(r'classes\d*\.dex',n) or n=='resources.arsc' or n.startswith('META-INF/'):continue
            if n not in after.namelist():missing.append(n)
            elif before.read(n)==after.read(n):payloads+=1
            else:changed_payloads.append(n)
        if changed_payloads:failures.append('Existing non-DEX payloads changed: '+str(changed_payloads))
        if missing:failures.append('Existing non-DEX payloads missing: '+str(missing))
    result={'result':'PASS' if not failures else 'FAIL','apk_sha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'existing_string_configs_identical':same,'existing_string_value_configs_identical':same,'name_lookup_ids_reassigned':relocated,'added_string_checks':checks,'existing_non_dex_payloads_identical':payloads,'changed_payloads':changed_payloads,'missing_payloads':missing,'failures':failures,'physical_device_tested':False}
    a.report.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:result[k] for k in ['result','existing_string_value_configs_identical','existing_non_dex_payloads_identical','failures']},ensure_ascii=False))
    raise SystemExit(bool(failures))
if __name__=='__main__':main()
