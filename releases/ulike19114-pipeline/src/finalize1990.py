#!/usr/bin/env python3
"""Bind fresh host/application evidence to exact .90 packages and archive reviewed sources."""
from pathlib import Path
import argparse,json
from build1990 import ROOT,VERSION,BUNDLE_VERSION,SINGLE,BUNDLE,BASE_PINS,source_pins,publication_pins,sha,require,json_bytes,write_zip
import application_evidence1990
QA_NAME='QA_ULike_v1.9.90.json'
SOURCE_ZIP='ULike_v1.9.90_sources_and_QA.zip'
SELECTED=['G1_PRE_CAPTURE_GPU_READINESS','G2_VALIDATED_DEFAULT_AND_OPTIONAL_TUNING','G3_REUSABLE_VERSIONED_QUALITY_PROOFS','G4_EXACT_GPU_NUMERICAL_REVIEW','G5_BOUNDED_GPU_IMAGE_TRANSPORT','G6_END_TO_END_PIPELINE_MEASUREMENT','G7_FIRST_SHOT_AND_REAL_SAVE_ACCEPTANCE']

def finalize(args):
 build=json.loads((args.dist/'build1990.json').read_text());host=json.loads(args.host_report.read_text());declaration=json.loads(args.declaration.read_text())
 require(build.get('schema')=='ulike1990-build-evidence-v1','Build report missing')
 require(host.get('status')=='passed' and host.get('physical_android_tested') is False,'Fresh host evidence missing or physical scope misreported')
 require(build['source_sha256']==source_pins()==declaration['reviewed_source_sha256'],'Sources changed after build')
 require(build['publication_sha256']==publication_pins()==declaration['reviewed_publication_sha256'],'Publisher changed after build')
 artifacts={n:{'sha256':sha((args.dist/n).read_bytes()),'bytes':(args.dist/n).stat().st_size} for n in (SINGLE,BUNDLE)}
 require(artifacts==build['artifacts'],'MPP bytes changed after audited build')
 application=application_evidence1990.read(ROOT,artifacts,report_path=args.application_report)
 qa={**build,'schema':'ulike1990-firstshot-release-qa-v1','status':'HOST_AND_ORIGINAL_APKS_VERIFIED_DEVICE_UNVERIFIED','selected_candidates':SELECTED,
     'host_tests_passed':True,'host_evidence':host,'host_report_sha256':sha(args.host_report.read_bytes()),
     'original_apk_apply_tested':True,'original_application':application,
     'device_tested':False,'physical_android_tested':False,'device_speed_measured':False,'device_quality_verified':False,
     'gpu_execution_on_physical_android':False,'first_shot_gpu_verified_on_physical_android':False,
     'samsung_gpu_failure_root_cause_confirmed':False,'all_processing_on_gpu':False,'capture_fusion_enabled':False,
     'unmeasured_acceptance_target':'Compare true shutter-to-published-file first and warm shots at unchanged image settings on SM-S948Q; host results do not establish target-device latency.'}
 (args.dist/QA_NAME).write_bytes(json_bytes(qa));(args.dist/'RELEASE_NOTES.txt').write_bytes(args.notes.read_bytes())
 source={'manifest.json':args.declaration.read_bytes(),QA_NAME:json_bytes(qa),'RELEASE_NOTES.txt':args.notes.read_bytes(),
         'evidence/host-audit1990.json':args.host_report.read_bytes(),'evidence/original-application1990.json':args.application_report.read_bytes(),
         'evidence/build1990.json':(args.dist/'build1990.json').read_bytes()}
 for name,digest in declaration['reviewed_source_sha256'].items():
  raw=(ROOT/name).read_bytes();require(sha(raw)==digest,'Source changed '+name);source['src/'+name]=raw
 publication=ROOT.parent/'publication'
 for p in sorted(publication.iterdir()):
  if p.is_file() and (p.name in ('publish1990.py','ulike1990-firstshot-publish.yml','README.md')):source['publication/'+p.name]=p.read_bytes()
 write_zip(args.dist/SOURCE_ZIP,source)
 names=(SINGLE,BUNDLE,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt')
 (args.dist/'SHA256SUMS.txt').write_text(''.join(sha((args.dist/n).read_bytes())+'  '+n+'\n' for n in sorted(names)))
 result={n:{'sha256':sha((args.dist/n).read_bytes()),'bytes':(args.dist/n).stat().st_size} for n in (*names,'SHA256SUMS.txt')}
 (args.dist/'release-artifacts1990.json').write_bytes(json_bytes(result));print(json.dumps(result,indent=2));return qa
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for name in ('dist','declaration','host-report','application-report','notes'):p.add_argument('--'+name,type=Path,required=True)
 finalize(p.parse_args())
