#!/usr/bin/env python3
"""Freeze completed local SDK build identities before CI rebuilds. No remote writes."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,re,zipfile
p=argparse.ArgumentParser();p.add_argument('--dist',type=Path,required=True);a=p.parse_args()
root=Path(__file__).resolve().parent
s=importlib.util.spec_from_file_location('release_publication',root/'publish.py');pub=importlib.util.module_from_spec(s);s.loader.exec_module(pub)
source=root.parent/'src'
qa_path=a.dist/'QA_HomeBack_v1.1.11.json';qa_raw=qa_path.read_bytes();qa=json.loads(qa_raw)
assert (qa['patch_version'],qa['bundle_version'],qa['baseline_version'])==('1.1.11','1.0.250','1.0.249')
pub.validate_regressions(qa.get('regressions'))
with zipfile.ZipFile(a.dist/pub.SINGLE) as z:runtime=z.read(pub.RUNTIME_EXTENSION)
assert pub.checks.fresh_evidence(runtime,root.parent)==qa['fresh_provided_apk_static_audit_evidence']
pub.checks.pipeline_result(qa,runtime,qa['artifacts'])
assert qa['validation_sources']==pub.checks.source_digests(root.parent)
assert qa['physical_android_tested'] is False and qa['third_party_apk_tested'] is False
assert qa['provided_apk_inputs_already_patched'] is True and qa['historical_provided_apk_static_audit_evidence']
match=re.fullmatch(r'SUCCESS: (\d+) cases, (\d+) assertions',qa['host'])
assert match and int(match[1])>=315 and int(match[2])>=1476
assert qa['host_counts']=={'cases':int(match[1]),'assertions':int(match[2])}
assert len(qa['synthetic'])==10 and all(row['result']=='PASS' for row in qa['synthetic'])
assert qa['sources']=={str(x.relative_to(source)):hashlib.sha256(x.read_bytes()).hexdigest() for x in sorted(source.rglob('*')) if x.is_file() and x.suffix in ('.java','.py')},'Sources changed since local build'
for name,row in qa['artifacts'].items():
 raw=(a.dist/name).read_bytes();assert row=={'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()}
for name,row in qa['historical_provided_apk_static_audit_evidence'].items():
 path=root.parent/name;raw=path.read_bytes();assert row=={'sha256':hashlib.sha256(raw).hexdigest(),'report':json.loads(raw)}
cfg=json.loads((root/'manifest-template.json').read_text())
audit_sources={str(x.relative_to(root.parent)):hashlib.sha256(x.read_bytes()).hexdigest() for folder in (root.parent/'evidence',root.parent/'publication/audit') for x in sorted(folder.rglob('*')) if x.is_file() and x.suffix in ('.java','.py')}
assert audit_sources,'APK audit sources missing'
cfg.update(freeze_state='frozen',local_expected_mpp=qa['artifacts'],local_expected_qa_sha256=hashlib.sha256(qa_raw).hexdigest(),local_expected_host=qa['host'],local_expected_regressions=qa['regressions'],local_expected_audit_sources=audit_sources)
(root/'manifest-template.json').write_text(json.dumps(cfg,ensure_ascii=False,indent=2)+'\n')
cfg.update(expected_mpp=qa['artifacts'],expected_qa_sha256=hashlib.sha256(qa_raw).hexdigest())
(root/'manifest.json').write_text(json.dumps(cfg,ensure_ascii=False,indent=2)+'\n')
print('PASS frozen local SDK artifacts, QA, source identity and provided-APK audit evidence')
