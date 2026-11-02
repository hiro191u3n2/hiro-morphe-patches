#!/usr/bin/env python3
"""Execute the unchanged session observer with dedicated scalar ownership doubles."""
import argparse
import json
from pathlib import Path
import re
import subprocess


def test(root,work,jdk=None):
    root,work=Path(root).resolve(),Path(work).resolve()
    folder=work/'camera-session1965'
    classes=folder/'classes';classes.mkdir(parents=True,exist_ok=True)
    fixtures=root/'tests1965/session-fixtures'
    files=[root/'CameraSession1965.java',root/'tests1965/Session1965Test.java',*sorted(fixtures.rglob('*.java'))]
    javac=str(Path(jdk)/'bin/javac') if jdk else 'javac'
    java=str(Path(jdk)/'bin/java') if jdk else 'java'
    compiled=subprocess.run([javac,'-source','8','-target','8','-encoding','UTF-8','-d',str(classes),*map(str,files)],
        stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    (folder/'compile.log').write_text(compiled.stdout)
    if compiled.returncode:raise AssertionError('Camera session compile failed: '+compiled.stdout)
    executed=subprocess.run([java,'-ea','-cp',str(classes),'com.hiro.ulike.Session1965Test'],
        stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=60)
    (folder/'execution.log').write_text(executed.stdout)
    if executed.returncode:raise AssertionError('Camera session regression failed: '+executed.stdout)
    count=re.search(r'^CAMERA_SESSION1965_ASSERTIONS=(\d+)$',executed.stdout,re.M)
    if not count:raise AssertionError('Actual session assertion count missing: '+executed.stdout)
    result={'status':'passed','assertions':int(count.group(1)),
        'scope':'Actual CameraSession1965 source; dedicated Android clock and retained ownership/state doubles; no frame inspection or physical camera execution.',
        'five_second_sampling_tests_passed':True,'bounded_weak_ownership_tests_passed':True,
        'scalar_privacy_tests_passed':True,'sampled_input_is_not_visible_success':True,
        'optional_observer_sink_faults_guarded':True,
        'physical_android_tested':False}
    (folder/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1])
    parser.add_argument('--work',type=Path,required=True);parser.add_argument('--jdk',type=Path)
    args=parser.parse_args();print(json.dumps(test(args.root,args.work,args.jdk),indent=2))
