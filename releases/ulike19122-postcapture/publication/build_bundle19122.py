#!/usr/bin/env python3
"""Replace only ULike121 with the reviewed ULike122 in exact current bundle327."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import struct
import sys
import zlib

HERE=Path(__file__).resolve().parent
BASE_SHA='08fdc138369a972d7807ef684c021411183eb0d398297ca241d41ca5bddca8a7'
BASE_BYTES=48092622
REFERENCE_SHA='cfa0963fb2ec3e5207cff34b3a0d953a300ad5f69a64413e6e3b16d7e6b31fca'
SINGLE_SHA='674bddd1058f5b1ad72bda7f3798aae8b6f54c862a5dd19d3017bd0c9ce43c2a'
NAME='Hiro_Morphe_Patches_v1.0.328.mpp'
LOADER='app/hiro/ulike/patches/UlikeHqMaxPatch.class'
INSTALLER='app/hiro/ulike/patches/IntegrationPayload186.class'

def main(args):
    source=args.source/'src' if (args.source/'src').is_dir() else args.source
    sys.path.insert(0,str(source))
    import build1945 as common
    require,sha,run=common.require,common.sha,common.run
    os.environ['PATH']=str(args.jdk/'bin')+os.pathsep+os.environ.get('PATH','')
    identity=common.verify_jdk()
    require(sha((args.tools/'morphe.jar').read_bytes())==common.TOOL_PINS['morphe.jar'],'Morphe tool pin differs')
    require(args.baseline.stat().st_size==BASE_BYTES and sha(args.baseline.read_bytes())==BASE_SHA,'Bundle327 pin differs')
    require(sha(args.standalone.read_bytes())==SINGLE_SHA,'Reviewed standalone122 pin differs')
    reference=args.reference or source.parent/'baseline121/ULike_HQ_Texture_Online_v1.9.121.mpp'
    require(sha(reference.read_bytes())==REFERENCE_SHA,'Reference121 pin differs')
    require(not args.work.exists(),'Fresh work directory required')
    args.work.mkdir(parents=True);args.output.parent.mkdir(parents=True,exist_ok=True)
    before=common.archive(args.baseline);single=common.archive(args.standalone);old=common.archive(reference)
    require(common.headers(before['META-INF/MANIFEST.MF'])['Version']=='1.0.327','Baseline bundle version')
    require(common.headers(single['META-INF/MANIFEST.MF'])['Version']=='1.9.122','Standalone version')
    own=common.own
    owned=lambda data:{name:value for name,value in data.items() if own(name)}
    require(owned(before)==owned(old),'Current bundle does not carry exact public ULike121')
    require(set(owned(before))==set(owned(single)),'ULike asset inventory changed')
    require(single[INSTALLER]==before[INSTALLER],'Native installer changed')
    for name in ('ulike/methods.dex','ulike/methods.tsv'):
        require(single[name]==before[name],'Stock method payload changed: '+name)
    inputs=args.work/'inputs';inputs.mkdir()
    for name,data in [('bundle.dex',before['classes.dex']),('reference.dex',old['classes.dex']),('single.dex',single['classes.dex'])]:
        (inputs/name).write_bytes(data)
    for name,data in single.items():
        if name.startswith('app/hiro/ulike/patches/'):
            path=inputs/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
    classes=args.work/'classes';classes.mkdir()
    cp=os.pathsep.join(map(str,(args.tools/'morphe.jar',inputs,classes)))
    code=[source/'MergePayloads.java',HERE/'BundleLoaders19122.java']
    code_hashes={str(p.name):sha(p.read_bytes()) for p in code}
    run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,*code],args.work/'javac.log')
    for tag in ('current','repeat'):
        run(['java','-cp',cp,'BundleLoaders19122',inputs/'bundle.dex',inputs/'reference.dex',inputs/'single.dex',args.work/(tag+'.dex'),args.work/(tag+'.json')],args.work/(tag+'.log'))
    require((args.work/'current.dex').read_bytes()==(args.work/'repeat.dex').read_bytes(),'Nondeterministic loader merge')
    require((args.work/'current.json').read_bytes()==(args.work/'repeat.json').read_bytes(),'Nondeterministic loader evidence')
    loader_report=json.loads((args.work/'current.json').read_text())
    combined=dict(before);combined.update(owned(single));combined['classes.dex']=(args.work/'current.dex').read_bytes()
    fields=common.headers(before['META-INF/MANIFEST.MF'])
    fields.update(Version='1.0.328',Description='ULike 1.9.122: limit simultaneous save resource use, trim eligible idle caches after capture work completes, and observe post-save responsiveness. Explicit comparison controls retain normal image algorithms, resolution and certified paths by default. Automatic qualification remains disabled. Device responsiveness and device-specific root cause unverified.')
    combined['META-INF/MANIFEST.MF']=common.manifest(fields)
    expected={'META-INF/MANIFEST.MF','classes.dex','ulike/runtime.dex',LOADER}
    changed={name for name,value in before.items() if combined.get(name)!=value}
    require(set(before)==set(combined) and changed==expected,'Unexpected combined delta: '+repr(changed))
    unrelated={name:sha(value) for name,value in before.items() if not own(name) and name not in ('classes.dex','META-INF/MANIFEST.MF')}
    require(all(sha(combined[name])==value for name,value in unrelated.items()),'Unrelated app bytes changed')
    require(owned(combined)==owned(single),'Standalone and combined ULike assets differ')
    native={name:sha(value) for name,value in owned(single).items() if name.endswith('.so') or name=='ulike186/runtime/0000.bin'}
    require(len(native)==14 and all(sha(before[name])==value for name,value in native.items()),'Inherited native payloads changed')
    data=combined['classes.dex']
    require(data[:4]==b'dex\n' and struct.unpack_from('<I',data,32)[0]==len(data),'DEX header differs')
    require(data[12:32]==hashlib.sha1(data[32:]).digest() and struct.unpack_from('<I',data,8)[0]==zlib.adler32(data[12:])&0xffffffff,'DEX integrity failed')
    output=args.output
    require(output.name==NAME,'Exact combined artifact filename required')
    common.write_zip(output,combined);common.write_zip(args.work/'repeat.mpp',combined)
    require(output.read_bytes()==(args.work/'repeat.mpp').read_bytes(),'Nondeterministic combined MPP')
    listing=run(['java','-jar',args.tools/'morphe.jar','list-patches','--patches',output],args.work/'list-patches.log')
    require('v1.9.122' in listing and '高画質撮影・質感美肌・素材通信を復旧' in listing,'Combined ULike loader failed')
    require(code_hashes=={str(p.name):sha(p.read_bytes()) for p in code},'Builder sources changed during execution')
    result={
        'schema':'ulike19122-bundle-v1','status':'passed','bundle_version':'1.0.328','ulike_version':'1.9.122',
        'baseline_sha256':BASE_SHA,'baseline_bytes':BASE_BYTES,'standalone_sha256':SINGLE_SHA,
        'combined_sha256':sha(output.read_bytes()),'combined_bytes':output.stat().st_size,'combined_filename':NAME,
        'unrelated_entries_preserved':True,'unrelated_loader_classes_preserved':True,
        'unrelated_entry_count':len(unrelated),'unrelated_entry_sha256':unrelated,
        'loader_proof':loader_report,'loader_proof_sha256':sha((args.work/'current.json').read_bytes()),
        'native_payload_count':len(native),'native_payload_sha256':native,
        'exact_standalone_ulike_payloads':True,'native_installer_byte_identical':True,
        'stock_method_payloads_byte_identical':True,'resource_delta':sorted(changed),
        'deterministic_dex_verified':True,'deterministic_archive_verified':True,'morphe_loader_verified':True,
        'jdk_identity':identity,'tool_sha256':{'morphe.jar':common.TOOL_PINS['morphe.jar']},
        'builder_sha256':sha(Path(__file__).read_bytes()),'java_source_sha256':code_hashes,
        'physical_android_tested':False,'combined_original_apks_apply_tested':False,
    }
    report=args.report or output.with_name('bundle-report19122.json')
    report.parent.mkdir(parents=True,exist_ok=True)
    report.write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
    print(json.dumps({key:result[key] for key in ('status','combined_filename','combined_sha256','combined_bytes','unrelated_entry_count')},ensure_ascii=False))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('baseline','standalone','source','tools','jdk','work','output'):
        parser.add_argument('--'+name,type=Path,required=True)
    parser.add_argument('--reference',type=Path)
    parser.add_argument('--report',type=Path)
    arguments=parser.parse_args()
    for name,value in vars(arguments).items():
        if isinstance(value,Path):setattr(arguments,name,value.resolve())
    main(arguments)
