#!/usr/bin/env python3
"""Independent literal-shader and analytic LUT review; supplied assets stay private."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

if not __debug__:
    raise RuntimeError("Review requires enabled Python assertions")
ROOT=Path(__file__).resolve().parents[1]

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def execute(command):
    p=subprocess.run(list(map(str,command)),stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
    if p.returncode:raise RuntimeError(p.stdout+"\n"+p.stderr)
    return p.stdout.strip()

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ("jdk","android-jar","natural-zip","purity-zip"):
        p.add_argument("--"+name,type=Path,required=True)
    p.add_argument("--report",type=Path)
    a=p.parse_args();sources=sorted((ROOT/"src/main/java").rglob("*.java"));test=ROOT/"review/IndependentSamples.java"
    before={str(f.relative_to(ROOT)):sha(f) for f in sources}
    with tempfile.TemporaryDirectory(prefix="ulike-independent-style-") as temp:
        execute([a.jdk/"javac","-source","8","-target","8","-bootclasspath",a.android_jar,"-d",temp,*sources])
        execute([a.jdk/"javac","--release","8","-cp",temp,"-d",temp,test,*sorted((ROOT/"src/test/java").rglob("*.java"))])
        numbers=json.loads(execute([a.jdk/"java","-Xmx256m","-cp",temp,"com.hiro.ulike.style.IndependentSamples"]))
        author_contracts=json.loads(execute([a.jdk/"java","-cp",temp,"com.hiro.ulike.style.PipelineTest"]))
        actual_report=Path(temp)/"actual.json"
        execute([sys.executable,ROOT/"verify_actual_assets.py",a.natural_zip,a.purity_zip,"--java",a.jdk/"java","--classes",temp,"--output",actual_report])
        actual_rerun=json.loads(actual_report.read_text())
        assert actual_rerun["status"]=="PASS" and actual_rerun["scalar_comparisons"]==43008
    author=json.loads((ROOT/"QA_ACTUAL_ASSETS.json").read_text())
    pinned={"natural":"5b50343dcedc7e9aadd218626e621ad683cd67372d225024f118b47f493546f0","purity":"cd5da20fefe1f9bd594e4e0d055d6320392fa54fb5df9d34b8f159c791c5b117"}
    assets={};checks=0
    with zipfile.ZipFile(a.natural_zip) as natural,zipfile.ZipFile(a.purity_zip) as purity:
        archives={"natural":natural,"purity":purity}
        assert sha(a.natural_zip)==pinned["natural"] and sha(a.purity_zip)==pinned["purity"]
        for record in author["passes"]:
            name="natural" if record["pass"].startswith("NATURAL") else "purity"
            for member,expected in record["source_sha256"].items():
                got=hashlib.sha256(archives[name].read(member)).hexdigest();assert got==expected
                assets[name+":"+member]=got;checks+=1
        for group,record in author["lut_assets"].items():
            name="natural" if group=="natural" else "purity"
            for member,expected in record["source_sha256"].items():
                got=hashlib.sha256(archives[name].read(member)).hexdigest();assert got==expected
                assets[name+":"+member]=got;checks+=1
        # Independently inspect branch/source facts that found real implementation
        # mistakes during review, instead of inferring from enum labels.
        d3=purity.read("materials/016/3dmakeup4/xshader/makeup.frag").decode()
        assert "uniform float opacity" not in d3 and "meVal.a * intensity" in d3
        assert "share://input.texture".encode() in purity.read("materials/016/3dmakeup4/material/face.material")
        blush=purity.read("materials/016/FaceMakeupV2_byExport2/makeup/blusher.frag").decode()
        assert "vec4 src = texture2D(videoImageTexture, texCoord)" in blush
        lash=purity.read("materials/016/AmazingFeature6/xshader/jiemao_faceu_e3ffcd1e800b950f0e616295904d1451.frag").decode()
        main=lash[lash.rindex("void main"):]
        assert "opacity" not in main and "weight = 0.0" in main
        assert b"AMAZING_USE_BLENDMODE_MUTIPLAY" in purity.read("materials/016/AmazingFeature6/material/jiemaoFaceU_msqo_1669694037.material")
        # Five mask-enabled materials explicitly carry the enabled USE_SEG value.
        for feature,material in [("AmazingFeature1","lips_keypoint_faceu2996_MATERIAL"),("AmazingFeature5","eye_part_faceu2987_MATERIAL"),("AmazingFeature5","eye_part_faceu2988_MATERIAL"),("AmazingFeature7","eye_part_faceu2991_MATERIAL"),("AmazingFeature7","eye_part_faceu2992_MATERIAL")]:
            data=purity.read("materials/016/"+feature+"/material/"+material+".material")
            assert b"USE_SEG\xdc\x9b\xee\xd1\0\0\0\0\x01\0\0\x001" in data
        assert purity.read("materials/016/AmazingFeature9/image/filter.png").startswith(b"\xff\xd8\xff")
    after={str(f.relative_to(ROOT)):sha(f) for f in sources};assert before==after
    report={"status":"PASS_INDEPENDENT_SAMPLED_STYLE_COMPONENT_REVIEW","reviewed_file_sha256":{str(f.relative_to(ROOT)):sha(f) for f in sorted(ROOT.rglob("*")) if f.is_file() and f.name!="INDEPENDENT_REVIEW.json" and "__pycache__" not in str(f) and "integration" not in f.relative_to(ROOT).parts},"java_android_sdk36_compile":True,"host_java_results":numbers,"author_contracts_rerun":author_contracts,"actual_asset_rerun":{"scalar_comparisons":actual_rerun["scalar_comparisons"],"max_abs_error":actual_rerun["max_abs_error"],"fixtures":len(actual_rerun["actual_asset_results"])},"asset_hash_comparisons":checks,"distinct_asset_sha256":assets,"review_fixes":["PURITY_3D ignores nonexistent opacity input","PURITY_BLUSHER requires explicitly supplied video-image RGB separate from destination","PURITY_3D requires explicitly supplied share-input RGB and opaque-fragment replacement even at zero intensity"],"scope":{"same_frame_rasterized_geometry_supplied_externally":True,"resolved_uniforms_supplied_externally":True,"original_source_routing_not_guessed":True,"original_gpu_parity":False,"whole_style_rendered_on_android":False,"hdr_domain_supported":False,"original_asset_jpeg_loss_reversible":False}}
    encoded=json.dumps(report,indent=2)+"\n"
    if a.report:a.report.write_text(encoded)
    else:print(encoded)

if __name__=="__main__":main()
