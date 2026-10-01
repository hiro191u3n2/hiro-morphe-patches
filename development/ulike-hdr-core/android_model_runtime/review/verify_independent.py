#!/usr/bin/env python3
"""Independent source-only Java compiler review; private models stay in --temp."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
from zipfile import ZipFile
import xml.etree.ElementTree as ET

if not __debug__:
    raise RuntimeError("Verification requires assertions; do not run with -O")

# ORT 1.30 Privacy.md distinguishes initialization from API-disabled events.
# Set before either Python import or any JVM subprocess can initialize ORT.
os.environ["ORT_DISABLE_TELEMETRY"]="1"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def invoke(command):
    return subprocess.run([str(x) for x in command],check=True,text=True,
                          stdout=subprocess.PIPE,stderr=subprocess.PIPE)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ("core","tools","baoman","goodlike","effect","bytenn","temp","report"):
        p.add_argument("--"+name,type=Path,required=True)
    a=p.parse_args()
    package=a.core/"android_model_runtime"
    source=package/"src/hiro/ulike/model"
    sources=sorted(source.glob("*.java"))
    hashes={str(path.relative_to(package)):digest(path) for path in sources}
    # Original decoded models may be written only to an external temporary tree.
    a.temp=a.temp.resolve();a.temp.mkdir(parents=True,exist_ok=True)
    assert not a.temp.is_relative_to(a.core.resolve())
    sdk_classes=a.temp/"sdk36_classes";host_classes=a.temp/"host_classes"
    sdk_classes.mkdir(exist_ok=True);host_classes.mkdir(exist_ok=True)
    jdk=a.tools/"jdk21/jdk-21.0.12.1+1/bin"
    sdk=a.tools/"android.jar";ort=a.tools/"onnxruntime-android-1.30.0-classes.jar"
    android_ns="{http://schemas.android.com/apk/res/android}"
    tools_ns="{http://schemas.android.com/tools}"
    with ZipFile(a.tools/"onnxruntime-android-1.30.0.aar") as archive:
        aar_manifest=archive.read("AndroidManifest.xml")
    providers=ET.fromstring(aar_manifest).findall("./application/provider")
    target="ai.onnxruntime.TelemetryInitializer"
    assert any(x.get(android_ns+"name")==target for x in providers)
    manifest=package/"AndroidManifest.xml"
    removals=ET.fromstring(manifest.read_bytes()).findall("./application/provider")
    assert any(x.get(android_ns+"name")==target and x.get(tools_ns+"node")=="remove"
               for x in removals)
    sdk_result=invoke([jdk/"javac","-source","8","-target","8","-bootclasspath",sdk,
                       "-cp",ort,"-d",sdk_classes,*sources])
    host_sources=[x for x in sources if x.name!="AndroidTensorEngine.java"]
    invoke([jdk/"javac","--release","8","-cp",ort,"-d",host_classes,*host_sources,
            package/"review/IndependentGuards.java"])
    sys.path[:0]=[str(a.core/"model_replay"),str(a.core/"model_inspect")]
    import numpy as np
    import onnx
    import onnxruntime
    # Verification must not send optional Microsoft runtime telemetry.
    onnxruntime.disable_telemetry_events()
    from onnx import numpy_helper,helper,TensorProto
    from baoman_replay import load_pinned,build_onnx,cpu_session
    from purity_replay import load_pinned_purity
    from bm_graph_metadata import load_baoman
    from legacy_graph_metadata import load_goodlike
    natural=load_baoman(a.baoman,a.bytenn)
    purity_graph,purity_weights,_=load_goodlike(a.goodlike,a.effect,a.bytenn)
    for name,graph,weights in (("baoman",natural["graph_text"].encode("ascii"),natural["weights"]),
                               ("purity",purity_graph,purity_weights)):
        (a.temp/(name+".graph")).write_bytes(graph)
        (a.temp/(name+".weights")).write_bytes(weights)
    guards=invoke([jdk/"java","-Xmx2g","-cp",str(host_classes)+":"+str(ort),
                   "hiro.ulike.model.IndependentGuards",a.baoman,a.goodlike,a.bytenn,a.effect,a.temp])
    assert "PASS independent_guard_checks=" in guards.stdout
    guard_count=int(guards.stdout.strip().split("=")[-1])
    cases=[]
    plans=(("baoman",load_pinned(a.baoman,a.bytenn)[0]),
           ("purity",load_pinned_purity(a.goodlike,a.effect,a.bytenn)[0]))
    for name,plan in plans:
        java=onnx.load(str(a.temp/(name+".java.onnx")))
        reference=build_onnx(plan)
        onnx.checker.check_model(java,full_check=True)
        assert java.ir_version==reference.ir_version
        assert java.opset_import==reference.opset_import
        assert java.graph.input==reference.graph.input and java.graph.output==reference.graph.output
        assert len(java.graph.node)==len(reference.graph.node)
        initializer_count=0
        ja={v.name:v for v in java.graph.initializer}
        pa={v.name:v for v in reference.graph.initializer}
        assert ja.keys()==pa.keys()
        for key in pa:
            x=numpy_helper.to_array(ja[key]);y=numpy_helper.to_array(pa[key])
            assert x.dtype==y.dtype and x.shape==y.shape
            assert x.tobytes()==y.tobytes()
            initializer_count+=1
        for jnode,pnode in zip(java.graph.node,reference.graph.node):
            assert (jnode.op_type,jnode.name,list(jnode.input),list(jnode.output))==(
                pnode.op_type,pnode.name,list(pnode.input),list(pnode.output))
            assert {v.name:v.SerializeToString() for v in jnode.attribute}=={
                v.name:v.SerializeToString() for v in pnode.attribute}
        # Expose every audited original operator boundary, preserving the
        # unchanged Java-generated graph and initializer data for execution.
        del java.graph.output[:]
        for layer in plan.layers:
            java.graph.output.append(helper.make_tensor_value_info(
                layer.node.output,TensorProto.FLOAT,list(layer.shape)))
        reference=build_onnx(plan,all_outputs=True)
        onnx.checker.check_model(java,full_check=True)
        js=cpu_session(java);ps=cpu_session(reference)
        yy,xx=np.mgrid[:256,:256]
        ramp=np.stack((xx/255,yy/255,(xx+yy)/510)).astype(np.float32)[None]*2-1
        rng=np.random.default_rng(170119)
        fixtures=(("zeros",np.zeros((1,3,256,256),np.float32)),
                  ("ramp",ramp),("random",rng.uniform(-1,1,(1,3,256,256)).astype(np.float32)))
        checks=[]
        for label,x in fixtures:
            actual=js.run(None,{"data":x});expected=ps.run(None,{"data":x})
            assert len(actual)==len(expected)==len(plan.layers)
            count=0;maxerror=0.
            for layer,jy,py in zip(plan.layers,actual,expected):
                assert jy.shape==py.shape==layer.shape
                assert jy.dtype==py.dtype==np.float32 and np.isfinite(jy).all()
                error=float(np.max(np.abs(jy-py)));maxerror=max(maxerror,error)
                assert np.array_equal(jy,py), (name,label,layer.node.output,error)
                count+=jy.size
            checks.append({"fixture":label,"compared_layers":len(actual),
                           "compared_values":count,"max_abs_error":maxerror,
                           "all_arrays_exactly_equal":True})
        cases.append({"model":name,"onnx_nodes":len(java.graph.node),
                      "exact_initializers":initializer_count,"cases":checks})
    assert hashes=={str(path.relative_to(package)):digest(path) for path in sources}
    report={"status":"PASS_INDEPENDENT_JAVA_COMPILER_REVIEW",
            "reviewed_source_sha256":hashes,
            "review_tools":{"android_sdk36_sha256":digest(sdk),"onnx_android_classes_sha256":digest(ort),
                            "onnx_python":onnx.__version__,"onnxruntime_python":onnxruntime.__version__,
                            "onnxruntime_privacy_md_sha256":digest(Path(onnxruntime.__file__).parent/"Privacy.md")},
            "sdk36_exact_source_compile":"PASS","guard_assertions":guard_count,
            "onnxruntime_telemetry":"ORT_DISABLE_TELEMETRY=1 before imports/JVM startup; API event disable also applied",
            "external_network_activity_verified":False,
            "android_manifest":{"sha256":digest(manifest),
                                "aar_manifest_sha256":hashlib.sha256(aar_manifest).hexdigest(),
                                "provider_removal_fragment_matches_original":True,
                                "actual_manifest_merge_verified":False},
            "structural_and_numeric_comparisons":cases,
            "limits":["Numerical execution uses host Python ONNX Runtime on Java-emitted graph bytes.",
                      "Disabling the telemetry API is not independent proof of network isolation.",
                      "This review does not execute the Java ORT wrapper or Android native library on a phone.",
                      "Input is prepared FP32 NCHW. No face detection, crop, complete style, HDR or app integration claim."],
            "original_model_data_published":False}
    a.report.write_text(json.dumps(report,indent=2)+"\n")
    print(json.dumps(report,indent=2))


if __name__=="__main__":main()
