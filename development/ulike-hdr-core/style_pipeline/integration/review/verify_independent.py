#!/usr/bin/env python3
"""Independent SDK36 compilation and transaction/tile integration verification."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

if not __debug__:raise RuntimeError("Assertions must be enabled")
os.environ["ORT_DISABLE_TELEMETRY"]="1"
ROOT=Path(__file__).resolve().parents[1]
CORE=ROOT.parents[1]

def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def execute(cmd):
    p=subprocess.run(list(map(str,cmd)),text=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    if p.returncode:raise RuntimeError(p.stdout+"\n"+p.stderr)
    return p.stdout.strip()

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ("jdk","android-jar","ort-android-classes"):
        p.add_argument("--"+name,type=Path,required=True)
    p.add_argument("--report",type=Path);a=p.parse_args()
    sources=[]
    for relative in ("android_model_runtime/src","android_beauty_image/src","style_pipeline/src/main/java","style_pipeline/integration/src"):
        sources+=sorted((CORE/relative).rglob("*.java"))
    before={str(f.relative_to(CORE)):digest(f) for f in sources}
    with tempfile.TemporaryDirectory(prefix="ulike-independent-style-adapter-") as temp:
        execute([a.jdk/"javac","-source","8","-target","8","-bootclasspath",a.android_jar,"-cp",a.ort_android_classes,"-d",temp,*sources])
        execute([a.jdk/"javac","--release","8","-cp",temp+os.pathsep+str(a.ort_android_classes),"-d",temp,ROOT/"review/IndependentSink.java"])
        checks=json.loads(execute([a.jdk/"java","-cp",temp+os.pathsep+str(a.ort_android_classes),"com.hiro.ulike.style.integration.IndependentSink"]))
    assert before=={str(f.relative_to(CORE)):digest(f) for f in sources}
    author_real=json.loads((ROOT/"QA_REAL_MODEL_ADAPTER.json").read_text())
    assert author_real["status"]=="PASS" and author_real["analytic_fp32_output_bits_compared"]==1179648
    assert author_real["test_sha256"]==digest(ROOT/"test/com/hiro/ulike/style/integration/RealModelAdapterTest.java")
    assert author_real["adapter_sha256"]==digest(ROOT/"src/com/hiro/ulike/style/integration/PostNeuralStyleSink.java")
    author_summary={"execution":"author_executed; test_source_and_report_independently_reviewed","report_sha256":digest(ROOT/"QA_REAL_MODEL_ADAPTER.json"),"models":2,"analytic_fp32_output_bits_compared":author_real["analytic_fp32_output_bits_compared"],"synthetic_makeup_geometry":True,"same_frame_real_camera_test":False}
    report={"status":"PASS_INDEPENDENT_TRANSACTIONAL_STYLE_ADAPTER_REVIEW","sdk36_compile":True,"ort_runtime_initialized":False,"results":checks,"author_actual_model_adapter_validation":author_summary,"production_source_sha256":before,"reviewed_integration_file_sha256":{str(f.relative_to(ROOT)):digest(f) for f in sorted(ROOT.rglob("*")) if f.is_file() and f.name!="INDEPENDENT_REVIEW.json" and "__pycache__" not in str(f)},"reviewed_behavior":["exact owned frame object required at begin and provider return","resolved timestamp and oriented tile coordinates checked by verified stage","strict contiguous rows, finite unit domain and buffers validated before provider","only declared prefix of reused last input buffer read","incoming rows split to bounded array allocation volume","FP32->FP64 processing->FP32 conversion without integer photo buffer","all staged downstream/provider failures roll back once","original failures retain suppressed cleanup failure","completed output not aborted by parent cleanup"],"scope":{"synthetic_controlled_bindings":True,"actual_models_run_by_this_review":False,"actual_style_geometry_available":False,"android_device_execution":False,"hdr_preservation":False,"complete_style_equivalence":False,"downstream_atomicity_is_required_contract":True}}
    text=json.dumps(report,indent=2)+"\n"
    if a.report:a.report.write_text(text)
    else:print(text)

if __name__=="__main__":main()
