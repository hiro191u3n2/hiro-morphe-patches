#!/usr/bin/env python3
"""Host filesystem/concurrency/fallback tests; not an Android execution claim."""
import argparse, json, pathlib, subprocess
p=argparse.ArgumentParser()
p.add_argument('--tools', type=pathlib.Path, required=True)
p.add_argument('--work', type=pathlib.Path, required=True)
p.add_argument('--bundle', type=pathlib.Path)
a=p.parse_args()
src=pathlib.Path(__file__).resolve().parent
bundle=a.bundle or src.parents[1]/'js-build/rn_business.jsbundle'
if not bundle.is_file(): raise SystemExit('Missing reviewed fixture: '+str(bundle))
a.work.mkdir(parents=True,exist_ok=True)
classes=a.work/'classes';classes.mkdir(exist_ok=True)
sources=sorted(str(f) for folder in ('runtime','test-stubs','test') for f in (src/folder).rglob('*.java') if f.name=='MyPlanBundleGuardTest.java' or folder!='test')
subprocess.run(['javac','--release','8','-Xlint:-options','-d',str(classes),*sources],check=True)
actual=a.work/'android-api-classes';actual.mkdir(exist_ok=True)
subprocess.run(['javac','--release','8','-Xlint:-options','-cp',str(a.tools/'android.jar'),'-d',str(actual),*sorted(str(f) for f in (src/'runtime').rglob('*.java'))],check=True)
r=subprocess.run(['java','-cp',str(classes),'MyPlanBundleGuardTest',str(a.work/'cases'),str(bundle)],check=True,text=True,capture_output=True)
x=json.loads(r.stdout);x['compiled_against_real_android_api']=True
out=a.work/'HOST_BUNDLE_GUARD_QA.json';out.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(x,ensure_ascii=False,indent=2))
