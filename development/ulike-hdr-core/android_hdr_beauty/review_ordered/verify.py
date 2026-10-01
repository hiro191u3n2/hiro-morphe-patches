#!/usr/bin/env python3
"""Independent ordered/zero-face properties; no model or phone execution."""
import argparse,hashlib,json,os,subprocess,tempfile
from pathlib import Path
HERE=Path(__file__).resolve().parent
CORE=HERE.parent.parent
os.environ['ORT_DISABLE_TELEMETRY']='1'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(args):
    p=subprocess.run(list(map(str,args)),capture_output=True,text=True)
    if p.returncode:raise RuntimeError(p.stdout+'\n'+p.stderr)
    return p.stdout.strip()
def main():
    p=argparse.ArgumentParser()
    for n in ('jdk-bin','android-jar','ort-classes'):p.add_argument('--'+n,type=Path,required=True)
    p.add_argument('--report',type=Path,default=HERE.parent/'INDEPENDENT_ORDERED_REVIEW.json');a=p.parse_args()
    production=[]
    for root in ('android_hdr_beauty/src','android_hdr_color/src','hdr_input/src/main','face_analysis/src/main','android_beauty_image/src','android_model_runtime/src','style_pipeline/src/main'):
        production+=sorted((CORE/root).rglob('*.java'))
    fixture=HERE.parent/'review/ProcessorReview.java'
    inventory=production+[fixture,HERE/'OrderedReview.java',Path(__file__).resolve()]
    before={str(f.relative_to(CORE)):sha(f) for f in inventory}
    with tempfile.TemporaryDirectory(prefix='ulike-ordered-independent-') as work:
        work=Path(work);classes=work/'classes';classes.mkdir()
        run([a.jdk_bin/'javac','--release','8','-cp',os.pathsep.join(map(str,(a.android_jar,a.ort_classes))),'-d',classes,*production,fixture,HERE/'OrderedReview.java'])
        result=json.loads(run([a.jdk_bin/'java','-Xmx128m','-cp',os.pathsep.join(map(str,(classes,a.ort_classes))),'hiro.ulike.beauty.OrderedReview']))
    assert before=={str(f.relative_to(CORE)):sha(f) for f in inventory},'source changed during review'
    old=json.loads((HERE.parent/'INDEPENDENT_REVIEW.json').read_text())
    for f,h in old['reviewed_file_sha256'].items():assert sha(CORE/f)==h,'existing numerical review stale: '+f
    report={'status':'PASS_INDEPENDENT_ORDERED_AND_ZERO_FACE_HDR_REVIEW','independent_properties':result,
      'manual_review':[
        'Exact rendition object and frame identity, raster, style, application-settings hash and complete explicit face order bind immutable snapshot.',
        'Zero observed faces still requires the complete remaining makeup/LUT graph and paired transaction; it is not an empty-graph shortcut.',
        '16-face hard limit, conservative retained primitive-array limit and cumulative image-pixels times face-count limit checked before staging and by pre-inference preflight.',
        'Budget excludes upstream ORT/preparation/native objects and must not be presented as a complete app heap guarantee.',
        'Observed order is caller-supplied evidence only. No detector array is promoted to native draw-order proof.',
        'Partial pair staging aborted on failure. Legacy single-face numerical processing remains unchanged.'
      ],
      'existing_review_rerun':{'report_sha256':sha(HERE.parent/'INDEPENDENT_REVIEW.json'),'appearance_math':old['appearance_math'],'owned_frame_processor':old['owned_frame_processor'],'sdk36_compile':old['sdk36_compile'],'d8_min26':old['d8_min26']},
      'reviewed_file_sha256':before,'actual_models_executed_in_this_review':False,'native_order_observed_on_device':False,
      'native_hdr_style_equivalence':False,'complete_app_integration_verified':False,'android_device_execution':False,
      'blocking_findings':[],'host_heap_limit_bytes':134217728}
    a.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'status':report['status'],'properties':result,'report':str(a.report)}))
if __name__=='__main__':main()
