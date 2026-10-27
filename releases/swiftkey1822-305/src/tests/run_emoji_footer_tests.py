#!/usr/bin/env python3
"""Execute unchanged EmojiFooter production Java against isolated JVM view/native fakes.

This verifies helper topology, geometry, guards, theme lookup and native-event dispatch.
It does not execute Android ConstraintLayout, real touch timers, or the native input engine.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--source-dir',type=Path,default=Path(__file__).resolve().parent.parent)
    parser.add_argument('--work',type=Path,required=True)
    args=parser.parse_args()
    root=args.source_dir.resolve()
    work=args.work.resolve()
    work.mkdir(parents=True,exist_ok=True)
    classes=work/'classes'
    if classes.exists():shutil.rmtree(classes)
    classes.mkdir()
    production=root/'src/hiro/swiftkey/EmojiFooter.java'
    harness=root/'tests/host/EmojiFooterHostTest.java'
    stubs=sorted((root/'tests/emoji_footer_host_stubs').rglob('*.java'))
    if not production.is_file() or not harness.is_file() or not stubs:
        raise RuntimeError('Missing production helper, harness or isolated host fakes')
    inputs=[production,harness,*stubs]
    compile_result=subprocess.run(['java','com.sun.tools.javac.Main','--release','8','-g:none','-encoding','UTF-8','-d',str(classes),*map(str,inputs)],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    (work/'compile.log').write_text(compile_result.stdout)
    print(compile_result.stdout,end='')
    if compile_result.returncode:raise RuntimeError('Emoji footer host compilation failed')
    result=subprocess.run(['java','-cp',str(classes),'hiro.swiftkey.EmojiFooterHostTest'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    (work/'host.log').write_text(result.stdout)
    print(result.stdout,end='')
    match=re.search(r'PASS EmojiFooter scenarios=(\d+) assertions=(\d+)',result.stdout)
    if result.returncode or not match:raise RuntimeError('Emoji footer host behavior checks failed')
    cases,assertions=map(int,match.groups())
    report={
        'status':'PASSED','scope':'Unchanged production EmojiFooter Java with isolated JVM Android/native fakes',
        'scenarios':cases,'assertions':assertions,
        'production_source_sha256':sha(production),
        'device_tested':False,'android_instrumentation_tested':False,'original_apk_apply_tested':False,
        'native_view_identity_preserved':True,'native_listener_identity_preserved':True,
        'native_delete_repeat_actor_identity_preserved':True,
        'native_category_selection_and_scroll_state_preserved':True,
        'layout_order_verified':True,'emoji_scope_verified':True,'repeated_install_idempotent':True,
        'narrow_width_verified':True,'physical_left_to_right_under_rtl_verified':True,
        'native_enter_dispatch_verified':True,'failure_guards_verified':True,
        'transaction_rollback_verified':True,'theme_following_verified':True,
        'enter_accessibility_verified':True,
        'input_engine_editor_action_execution_tested':False,
        'native_delete_repeat_timing_executed':False,
        'android_constraintlayout_solver_executed':False,
        'test_sources':{p.relative_to(root).as_posix():sha(p) for p in sorted([*inputs,Path(__file__).resolve()])},
        'limitations':[
            'Host fakes model view parentage, measurement, listener dispatch and draw calls; not Android rendering.',
            'Native Enter sender is a recording fake. Exact native action routing is reviewed separately against APK bytecode.',
            'Preserved native DeleteKeyButton actor/listener identity is verified; its actual repeat timer is not executed.',
            'No handset touch, screen reader, native IME composition or clean-original-APK application was executed.'
        ]
    }
    (work/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print('PASS report.json unchanged production helper behavior verified')


if __name__=='__main__':main()
