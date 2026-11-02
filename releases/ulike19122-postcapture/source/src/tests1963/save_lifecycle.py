#!/usr/bin/env python3
"""Focused production save regressions and inherited ordered-publication cases."""
from pathlib import Path
import argparse,importlib.util,json,subprocess

def test(root,work):
    root,work=Path(root).resolve(),Path(work).resolve()
    spec=importlib.util.spec_from_file_location('h33_current1963',root/'host_h33_1956.py')
    inherited=importlib.util.module_from_spec(spec);spec.loader.exec_module(inherited)
    old=inherited.test(root,work)
    folder=work/'host-h33-1956';classes=folder/'classes'
    subprocess.run(['javac','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-cp',str(classes),'-d',str(classes),str(root/'tests1963/SaveAudit1963.java')],check=True,capture_output=True,text=True)
    run=subprocess.run(['java','-Xmx256m','-cp',str(classes),'com.hiro.ulike.SaveAudit1963'],capture_output=True,text=True,timeout=45)
    (folder/'save1963.log').write_text(run.stdout+run.stderr)
    if run.returncode:raise RuntimeError(run.stdout+run.stderr)
    checks=int(run.stdout.split('checks=')[1].split()[0]);boundary=int(run.stdout.split('boundary_checks=')[1].split()[0])
    report={'status':'passed','assertions':checks+boundary+old['assertions'],'focused_assertions':checks,'focused_boundary_assertions':boundary,'inherited_assertions':old['assertions'],'preparation_failure_reopens_exact_shutter':True,'stale_failure_preserves_newer_capture':True,'failed_preparation_holds_exit_through_main_callback':True,'save_workers_survive_error_subclasses':True,'fifo_codec_publication_quality_ownership_preserved':True,'physical_android_tested':False}
    (work/'save1963-result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args()
    print(json.dumps(test(Path(__file__).resolve().parents[1],a.out),indent=2))
