#!/usr/bin/env python3
"""Run incremental v380 -> v381 preservation audits on completed APK cases."""
from pathlib import Path
import argparse,json,shutil,subprocess

ROOT=Path(__file__).resolve().parent.parent
REVIEW=ROOT/'review'
BASE=ROOT.parent/'x-ui380'/'verification'

def main():
    p=argparse.ArgumentParser();p.add_argument('cases',nargs='*',default=['dev1','dev2','dev2-clone','standalone']);a=p.parse_args()
    cp=f'{ROOT}/tools/morphe.jar:{ROOT}/build/provider'
    # Only the audit belongs here. Load hook classes from the exact final provider build.
    shutil.rmtree(REVIEW/'classes'/'app',ignore_errors=True)
    subprocess.run(['java','-jar',str(ROOT/'tools/ecj.jar'),'-17','-nowarn','-cp',cp,'-d',str(REVIEW/'classes'),str(REVIEW/'CompletedUiApkAudit.java')],check=True)
    for case in a.cases:
        assert case in ['dev1','dev2','dev2-clone','standalone']
        old=BASE/(case+'.apk');new=ROOT/'verification'/(case+'.apk')
        assert old.is_file()and new.is_file(),f'Missing completed pair: {case}'
        commands=[
            ('apk-preservation',['java','-XX:-UsePerfData','-Xmx3g','-Dhiro.audit.cache='+str(ROOT.parent/'x-ui380'/'review'/'cache'),'-cp',str(REVIEW/'classes')+':'+cp,'CompletedUiApkAudit',str(old),str(new),str(ROOT/'build/runtime-dex/classes.dex'),str(REVIEW/(case+'-apk-preservation.json'))]),
            ('resources',['python3',str(REVIEW/'audit_resources.py'),str(old),str(new),str(ROOT/'component.mpp'),str(REVIEW/(case+'-resources.json'))])
        ]
        for name,command in commands:
            with (REVIEW/(case+'-'+name+'.log')).open('w')as log:subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,check=True)
            result=json.loads((REVIEW/(case+'-'+name+'.json')).read_text());assert result['result']=='PASS'
            print(case,name,'PASS',flush=True)

if __name__=='__main__':main()
