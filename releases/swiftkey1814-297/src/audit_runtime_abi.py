#!/usr/bin/env python3
"""Check runtime references against the archived target APK; never redistributes APK bytes."""
import argparse, hashlib, json, re
from pathlib import Path
from zipfile import ZipFile
from loguru import logger
from androguard.core.dex import DEX

def main():
    p=argparse.ArgumentParser(); p.add_argument('--apk', required=True,type=Path)
    p.add_argument('--extension',required=True,type=Path); p.add_argument('--output',required=True,type=Path)
    args=p.parse_args(); logger.remove(); classes={}
    with ZipFile(args.apk) as z:
        for name in z.namelist():
            if re.fullmatch(r'classes\d*\.dex',name):
                classes.update({c.get_name():c for c in DEX(z.read(name)).get_classes()})
    native=dict(classes)
    ext=DEX(args.extension.read_bytes()); ecs={c.get_name():c for c in ext.get_classes()}; classes.update(ecs)
    def lookup(owner,name,descriptor,isfield,seen=None):
        seen=set() if seen is None else seen
        if owner in seen or owner not in classes:return None
        seen.add(owner); c=classes[owner]
        for x in (c.get_fields() if isfield else c.get_methods()):
            if x.get_name()==name and x.get_descriptor().replace(' ','')==descriptor.replace(' ',''):return x,c
        for parent in [c.get_superclassname(),*c.get_interfaces()]:
            found=lookup(parent,name,descriptor,isfield,seen)
            if found:return found
        return None
    refs={}; unresolved=[]; inaccessible=[]; incompatible=[]
    for c in ecs.values():
        if not (c.get_name().startswith(('Lhiro/swiftkey/FlickSymbols', 'Lhiro/swiftkey/CandidateScroll')) or c.get_name() in ('Lhiro/swiftkey/KeySwap;', 'Lhiro/swiftkey/JapaneseGlide;')):continue
        for m in c.get_methods():
            for offset, ins in m.get_instructions_idx():
                for operand in ins.get_operands():
                    if len(operand)<3 or not isinstance(operand[2],str):continue
                    match=re.match(r'(L[^;]+;)->([^\s(]+)(.*)',operand[2])
                    if not match:continue
                    owner,name,descriptor=match.groups(); descriptor=descriptor.strip()
                    if owner.startswith(('Ljava/','Ljavax/','Landroid/')):continue
                    key=owner+'->'+name+descriptor
                    if key in refs:continue
                    found=lookup(owner,name,descriptor,not descriptor.startswith('('))
                    row={'reference':key,'resolved':bool(found)}; refs[key]=row
                    if not found:
                        # Android methods inherited by native View / Service subclasses
                        # are separately typechecked against the pinned Android API jar.
                        if name in ('getContext','getWidth','getHeight','getWindowToken','isShown','isAttachedToWindow','post','postDelayed','removeCallbacks','getCurrentInputConnection'):
                            row['android_inherited']=True
                        else:unresolved.append(key)
                        continue
                    member,defined=found; row.update(defined_owner=defined.get_name(),access=member.get_access_flags_string(),opcode=ins.get_name())
                    is_static=bool(member.get_access_flags()&8)
                    opcode_static=ins.get_name().startswith(('invoke-static','sget','sput'))
                    if is_static!=opcode_static:incompatible.append(key+' static/instance opcode mismatch')
                    if ins.get_name().startswith('invoke-interface') and not (defined.get_access_flags()&512):
                        incompatible.append(key+' invoked as interface but defining owner is a class')
                    same_package=c.get_name().rsplit('/',1)[0]==defined.get_name().rsplit('/',1)[0]
                    same_family=c.get_name().split('$')[0].rstrip(';')==defined.get_name().split('$')[0].rstrip(';')
                    public=bool(member.get_access_flags()&1) and bool(defined.get_access_flags()&1)
                    if not(public or same_package or same_family):inaccessible.append(key)
    report={'status':'PASSED' if not unresolved and not inaccessible and not incompatible else 'FAILED',
            'apk_sha256':hashlib.sha256(args.apk.read_bytes()).hexdigest(),
            'apk_provenance':'Previously modified archived SwiftKey Beta 9.13.16.4 v2 APK; not a clean application test',
            'extension_sha256':hashlib.sha256(args.extension.read_bytes()).hexdigest(),
            'native_and_extension_references':list(refs.values()),'unresolved':unresolved,'inaccessible':inaccessible,'incompatible_opcodes':incompatible,
            'device_tested':False,'original_apk_apply_tested':False}
    args.output.parent.mkdir(parents=True,exist_ok=True); args.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(report['status'],len(refs),'references; unresolved',unresolved,'inaccessible',inaccessible,'opcodes',incompatible)
    assert report['status']=='PASSED'

if __name__=='__main__':main()
