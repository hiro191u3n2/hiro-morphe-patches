#!/usr/bin/env python3
"""v382 must preserve the complete v380 resource table and all non-DEX payloads."""
import argparse, hashlib, json, re, xml.etree.ElementTree as ET, zipfile
from pathlib import Path
import arsc as parser

def digest(data): return hashlib.sha256(data).hexdigest()

def inspect_strings(table, expected, cache_dir):
    # Reuse parsed results only for an identical table, expected values and parser.
    expected_json=json.dumps(sorted((k,c,v) for (k,c),v in expected.items()),ensure_ascii=False).encode()
    key=digest(table)+digest(expected_json)+digest(Path(parser.__file__).read_bytes())
    cache=cache_dir/('resources-v381-'+digest(key.encode())+'.json')
    if cache.exists():
        data=json.loads(cache.read_text())
        assert data['key']==key
        return data,True
    strings=parser.Arsc(table).entries
    checks=[];failures=[]
    for (name,config),value in expected.items():
        actual=strings.get(name,{}).get(config,{})
        ok=actual.get('value')==value
        checks.append({'name':name,'config':config,'passed':ok,'resource_id':actual.get('id')})
        if not ok:failures.append('Missing or incorrect UI value: '+name+'/'+config)
    for name,config in expected:
        if set(strings.get(name,{}))!={'','ja'}:failures.append('Unexpected UI configurations: '+name)
    data={'key':key,'string_configs':sum(len(v) for v in strings.values()),'checks':checks,'failures':failures}
    cache_dir.mkdir(parents=True,exist_ok=True)
    cache.write_text(json.dumps(data,ensure_ascii=False)+'\n')
    return data,False

def main():
    p=argparse.ArgumentParser();p.add_argument('baseline',type=Path);p.add_argument('apk',type=Path);p.add_argument('component',type=Path);p.add_argument('report',type=Path);a=p.parse_args()
    failures=[]
    with zipfile.ZipFile(a.baseline) as before, zipfile.ZipFile(a.apk) as after, zipfile.ZipFile(a.component) as component:
        old_table=before.read('resources.arsc');new_table=after.read('resources.arsc')
        same_table=old_table==new_table
        if not same_table:failures.append('Complete resource table changed')
        expected={}
        for config,path in [('', 'newx-ui/values.xml'),('ja','newx-ui/values-ja.xml')]:
            root=ET.fromstring('<resources>'+component.read(path).decode()+'</resources>')
            for node in root:expected[(node.attrib['name'],config)]=parser.android_string(node.text or '')
        if len(expected)!=20 or len({k for k,c in expected})!=10:failures.append('Expected exactly 20 UI string values across 10 names')
        inspected,cache_hit=inspect_strings(new_table,expected,Path(__file__).resolve().parent.parent.parent/'x-ui381'/'review'/'resource-cache')
        failures.extend(inspected['failures'])
        def payload_names(apk):
            return {x.filename for x in apk.infolist() if not x.is_dir() and not re.fullmatch(r'classes\d*\.dex',x.filename) and x.filename!='resources.arsc' and not x.filename.startswith('META-INF/')}
        old_names=payload_names(before);new_names=payload_names(after)
        missing=sorted(old_names-new_names);added=sorted(new_names-old_names)
        changed=sorted(n for n in old_names&new_names if before.read(n)!=after.read(n))
        if changed:failures.append('Existing non-DEX payloads changed: '+str(changed))
        if missing:failures.append('Existing non-DEX payloads missing: '+str(missing))
        if added:failures.append('Unexpected non-DEX payloads added: '+str(added))
    result={'result':'PASS' if not failures else 'FAIL','comparison':'v380 to v382 exact rollback','apk_sha256':digest(a.apk.read_bytes()),'resource_table_exact_identical':same_table,'resource_table_sha256':digest(new_table),'resource_parse_cache_hit':cache_hit,'existing_string_configs_identical':inspected['string_configs'] if same_table else 0,'existing_string_value_configs_identical':inspected['string_configs'] if same_table else 0,'name_lookup_ids_reassigned':{},'ui_string_checks':inspected['checks'],'added_string_checks':[],'updated_existing_string_checks':[],'existing_non_dex_payloads_identical':len(old_names)-len(missing)-len(changed),'changed_payloads':changed,'missing_payloads':missing,'added_payloads':added,'failures':failures,'physical_device_tested':False}
    a.report.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:result[k] for k in ['result','resource_table_exact_identical','existing_string_value_configs_identical','existing_non_dex_payloads_identical','failures']},ensure_ascii=False))
    raise SystemExit(bool(failures))

if __name__=='__main__':main()
