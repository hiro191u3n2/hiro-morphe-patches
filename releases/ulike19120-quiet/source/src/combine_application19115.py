#!/usr/bin/env python3
"""Bind two independently applied final MPP reports without packaging private APKs."""
from pathlib import Path
import argparse,json
import application_evidence19101
from build19101 import SINGLE,BUNDLE,ROOT

def combine(standalone,bundle,build,output):
    artifacts=json.loads(Path(build).read_text())['artifacts']
    first=Path(standalone).read_bytes();second=Path(bundle).read_bytes()
    application_evidence19101._read_raw(ROOT,artifacts,first,SINGLE)
    application_evidence19101._read_raw(ROOT,artifacts,second,BUNDLE)
    report=json.loads(first)
    if 'bundle_application' in report:raise AssertionError('Already-combined application report')
    report['bundle_application']=json.loads(second)
    Path(output).parent.mkdir(parents=True,exist_ok=True)
    Path(output).write_text(json.dumps(report,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
    application_evidence19101.read(ROOT,artifacts,Path(output))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('standalone','bundle','build','output'):p.add_argument('--'+name,required=True,type=Path)
    a=p.parse_args();combine(a.standalone,a.bundle,a.build,a.output)
