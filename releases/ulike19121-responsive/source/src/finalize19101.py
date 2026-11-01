#!/usr/bin/env python3
"""Bind fresh host/application evidence to exact .119 packages and archive reviewed sources."""
from pathlib import Path
import argparse,json
from build19101 import ROOT,VERSION,BUNDLE_VERSION,SINGLE,BUNDLE,BASE_PINS,source_pins,publication_pins,sha,require,json_bytes,write_zip
import application_evidence19101
QA_NAME='QA_ULike_v1.9.119.json'
SOURCE_ZIP='ULike_v1.9.119_sources_and_QA.zip'
BASELINE_REPORT='evidence/immutable118-host-audit.json'
BASELINE_REPORT_SHA256='59d8a40b0a588291e879545823cba9a197bdd593752336cf0652f5289faa4ddc'
BASELINE_REPORT_BYTES=1260766

def historical_baseline_report(release_root,host):
 raw=(release_root/BASELINE_REPORT).read_bytes()
 require(len(raw)==BASELINE_REPORT_BYTES and sha(raw)==BASELINE_REPORT_SHA256,'Pinned historical baseline report differs')
 require(host.get('baseline_host_report_sha256')==BASELINE_REPORT_SHA256 and json.loads(raw)==host.get('baseline_host_report'),'Historical input differs from embedded executed baseline evidence')
 return raw

SELECTED=['NOISE119_STRONG_QUOTIENT_SHARING_EXACT_DIVIDER','NOISE119_PROTECTED_FRONT_BLOCK_SKIP','NOISE119_IMMUTABLE_LEGACY_ROUTE_AND_SEPARATE_CANDIDATE_PROOF']

def finalize(args):
 build=json.loads((args.dist/'build19101.json').read_text());host=json.loads(args.host_report.read_text());declaration=json.loads(args.declaration.read_text())
 require(build.get('schema')=='ulike19101-build-evidence-v1','Build report missing')
 require(host.get('status')=='passed' and host.get('physical_android_tested') is False,'Fresh host evidence missing or physical scope misreported')
 require(build['source_sha256']==source_pins()==declaration['reviewed_source_sha256'],'Sources changed after build')
 require(build['publication_sha256']==publication_pins()==declaration['reviewed_publication_sha256'],'Publisher changed after build')
 artifacts={n:{'sha256':sha((args.dist/n).read_bytes()),'bytes':(args.dist/n).stat().st_size} for n in (SINGLE,BUNDLE)}
 require(artifacts==build['artifacts'],'MPP bytes changed after audited build')
 application=application_evidence19101.read(ROOT,artifacts,report_path=args.application_report)
 historical=historical_baseline_report(ROOT.parent,host)
 qa={**build,'schema':'ulike19101-pipeline-release-qa-v1','status':'HOST_AND_ORIGINAL_APKS_VERIFIED_DEVICE_UNVERIFIED','selected_candidates':SELECTED,
     'regional_positive_proofs_scope':'research_only_not_distributed_adoption',
     'host_tests_passed':True,'host_evidence':host,'host_report_sha256':sha(args.host_report.read_bytes()),
     'original_apk_apply_tested':True,'original_application':application,
     'device_tested':False,'physical_android_tested':False,'device_speed_measured':False,'device_quality_verified':False,
     'gpu_execution_on_physical_android':False,'first_shot_gpu_verified_on_physical_android':False,
     'device_capture_variance_improvement_verified':False,
     'samsung_gpu_failure_root_cause_confirmed':False,'all_processing_on_gpu':False,'capture_fusion_enabled':False,
     'unmeasured_acceptance_target':'Target under 2000 ms from observed recorder capture entry to published file at unchanged image settings on SM-S948Q, with first and warm shots measured separately. UI touch time is reported only when the exact hooked synchronous UI stack binds to this capture; asynchronous or unrelated UI events remain unmeasured. Host results do not establish target-device latency.'}
 (args.dist/QA_NAME).write_bytes(json_bytes(qa));(args.dist/'RELEASE_NOTES.txt').write_bytes(args.notes.read_bytes())
 source={'DESIGN_REVIEW.md':(ROOT.parent/'DESIGN_REVIEW.md').read_bytes(),'production-roots19115.json':(ROOT.parent/'production-roots19115.json').read_bytes(),'manifest.json':args.declaration.read_bytes(),QA_NAME:json_bytes(qa),'RELEASE_NOTES.txt':args.notes.read_bytes(),
         'evidence/host-audit19101.json':args.host_report.read_bytes(),'evidence/original-application19101.json':args.application_report.read_bytes(),
         'evidence/build19101.json':(args.dist/'build19101.json').read_bytes(),BASELINE_REPORT:historical}
 for name,digest in declaration['reviewed_source_sha256'].items():
  raw=(ROOT/name).read_bytes();require(sha(raw)==digest,'Source changed '+name);source['src/'+name]=raw
 publication=ROOT.parent/'publication'
 for p in sorted(publication.iterdir()):
  if p.is_file() and (p.name in ('publish19101.py','ulike19101-pipeline-publish.yml','README.md')):source['publication/'+p.name]=p.read_bytes()
 write_zip(args.dist/SOURCE_ZIP,source)
 names=(SINGLE,BUNDLE,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt')
 (args.dist/'SHA256SUMS.txt').write_text(''.join(sha((args.dist/n).read_bytes())+'  '+n+'\n' for n in sorted(names)))
 result={n:{'sha256':sha((args.dist/n).read_bytes()),'bytes':(args.dist/n).stat().st_size} for n in (*names,'SHA256SUMS.txt')}
 (args.dist/'release-artifacts19101.json').write_bytes(json_bytes(result));print(json.dumps(result,indent=2));return qa
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for name in ('dist','declaration','host-report','application-report','notes'):p.add_argument('--'+name,type=Path,required=True)
 finalize(p.parse_args())
