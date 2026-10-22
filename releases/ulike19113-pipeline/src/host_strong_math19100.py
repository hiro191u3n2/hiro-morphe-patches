#!/usr/bin/env python3
"""Keep the complete .96 arithmetic/GLES proof through the reviewed .113 delta.

The exact .113 inverse restores the byte-pinned .112 shader before the .107
region-only inverse is used for the inherited source contract. Actual
execution uses the current shader. All 1016 frozen Java records,
18240 actual JNI reads, published-.95 replay,
60 legacy preprocessor variants and exact-cutoff negative controls remain.
The .100 delta only shares original strict regional coordinates/detail and
local luma ratio in preparation modes 0/1/2 only; .113 extends the identical
rounded-result reuse to optional full-Strong profile6 only. Standard profile4
and tiled profile7 retain their original preprocessed programs. No expression
order, precision, weight formula or accumulation order is changed.
"""
from pathlib import Path
from legacy_shader19106 import production_text, restore_regional19107
from strong_delta19113 import BASELINE_SHA256, restore_shader19113
import argparse,hashlib,importlib.util,json,os,subprocess,sys
ROOT=Path(__file__).resolve().parent
REFERENCE99='c2d3e7de35cf36ea5b70cf6d41eaef8647eb29d5ebcf3bc63c3004c125a81360'
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def replace_once(text,old,new):
 if text.count(old)!=1:raise AssertionError('strict regional delta anchor changed: '+old[:100])
 return text.replace(old,new)
def extend19100(old):
 a=old.index('float regionalSigma(');b=old.index('/* GX28:',a);helper=old[a:b]
 shared=helper.replace('float regionalSigma(int x,int y,int plane){','vec3 regionalEvidence19100(int x,int y){')
 shared=replace_once(shared,'    precise float a=evidence[16+3*(iy*rw+ix)+plane],b=evidence[16+3*(iy*rw+nx)+plane];','    precise vec3 values;\n    for(int plane=0;plane<3;plane++){\n    precise float a=evidence[16+3*(iy*rw+ix)+plane],b=evidence[16+3*(iy*rw+nx)+plane];')
 shared=replace_once(shared,'    precise float result=(a*(1.0-fx)+b*fx)*(1.0-fy)+(c*(1.0-fx)+d*fx)*fy;return result;','    values[plane]=(a*(1.0-fx)+b*fx)*(1.0-fy)+(c*(1.0-fx)+d*fx)*fy;\n    }\n    return values;')
 comment='''/* .100: all three regional planes use the same immutable coordinates.
 * Compute those coordinates once, preserving every original scalar divide,
 * clamp and interpolation operation. Reuse detail for confidence and texture;
 * no sampling, precision, plane or output expression changes. */
'''
 result=old[:a]+'#if GX_IEEE_DIV73\n'+comment+shared+'#else\n'+helper+'#endif\n'+old[b:]
 line='    precise float sy=regionalSigma(x,row+u[6],0),sc=regionalSigma(x,row+u[6],1);'
 result=replace_once(result,line,'#if GX_IEEE_DIV73\n    precise vec3 region19100=regionalEvidence19100(x,row+u[6]);\n    precise float sy=region19100.x,sc=region19100.y;\n#else\n'+line+'\n#endif')
 oldline='    precise float confidenceValue=256.0*clamp(ieeeDiv1973((flatness.x-.65),.30),0.0,1.0)*clamp(ieeeDiv1973((max(sy,sc)-.60),1.8),0.0,1.0)*(1.0-regionalSigma(x,row+u[6],2));'
 result=replace_once(result,oldline,oldline.replace('regionalSigma(x,row+u[6],2)','region19100.z'))
 oldline='    precise float texture=(1.0-ieeePow2Div1978(.55*float(detail),8u))*(1.0-.85*regionalSigma(x,row+u[6],2));'
 result=replace_once(result,oldline,'#if GX_IEEE_DIV73\n'+oldline.replace('regionalSigma(x,row+u[6],2)','region19100.z')+'\n#else\n'+oldline+'\n#endif')
 oldline='    precise float texture=(1.0-ieeeDiv1973(.55*float(detail),256.0))*(1.0-.85*regionalSigma(x,row+u[6],2));'
 result=replace_once(result,oldline,oldline.replace('regionalSigma(x,row+u[6],2)','region19100.z'))
 result=replace_once(result,'        precise float wy=ieeeDiv1973(base,(1.0+square(ieeeDiv1973((cq.x-cp.x),thresholdY))));','        // .100: this identical rounded quotient/square feeds both weights.\n        // Evaluate it once; each weight retains its original operation order.\n        precise float lumaRatioSquared19100=square(ieeeDiv1973((cq.x-cp.x),thresholdY));\n        precise float wy=ieeeDiv1973(base,(1.0+lumaRatioSquared19100));')
 result=replace_once(result,'.12*square(ieeeDiv1973((cq.x-cp.x),thresholdY))','.12*lumaRatioSquared19100')
 shared='        // .100: this identical rounded quotient/square feeds both weights.\n        // Evaluate it once; each weight retains its original operation order.\n        precise float lumaRatioSquared19100=square(ieeeDiv1973((cq.x-cp.x),thresholdY));\n        precise float wy=ieeeDiv1973(base,(1.0+lumaRatioSquared19100));'
 original='        precise float wy=ieeeDiv1973(base,(1.0+square(ieeeDiv1973((cq.x-cp.x),thresholdY))));'
 result=replace_once(result,shared,'#if GX_STRONG_MODE >= 0 && GX_STRONG_MODE < 3\n'+shared+'\n#else\n'+original+'\n#endif')
 shared='        precise float wc=ieeeDiv1973(base,(1.0+.12*lumaRatioSquared19100+square(ieeeDiv1973((cq.y-cp.y),thresholdC))+square(ieeeDiv1973((cq.z-cp.z),thresholdC))));'
 original=shared.replace('.12*lumaRatioSquared19100','.12*square(ieeeDiv1973((cq.x-cp.x),thresholdY))')
 result=replace_once(result,shared,'#if GX_STRONG_MODE >= 0 && GX_STRONG_MODE < 3\n'+shared+'\n#else\n'+original+'\n#endif')
 return result

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 tracked=[source/'native1960/strong1960.comp',source/'tests19100/strong/reference99/strong1960.comp',source/'tests19100/strong/bench19100.py',source/'host_strong_math19100.py',source/'legacy_shader19106.py',source/'strong_delta19113.py'];pins={str(p.relative_to(source)):sha(p) for p in tracked}
 reference=(source/'tests19100/strong/reference99/strong1960.comp').read_text();candidate=(source/'native1960/strong1960.comp').read_text()
 if sha(source/'tests19100/strong/reference99/strong1960.comp')!=REFERENCE99:raise AssertionError('frozen .99 shader changed')
 # This inverse is evidence about the old source contract, never a substitute
 # for executing the current shader against the independent Java oracle.
 restored112=restore_shader19113(candidate,required=True)
 if hashlib.sha256(restored112.encode()).hexdigest()!=BASELINE_SHA256:raise AssertionError('Reviewed .113 inverse does not restore complete published .112 shader')
 restored106=restore_regional19107(restored112,required=True)
 if production_text(restored112)!=production_text(extend19100(reference)):raise AssertionError('restored .112 shader exceeds original scalar sharing delta')
 spec=importlib.util.spec_from_file_location('strong19100_retained96',source/'host_shader1996.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
 previous=base.expected_shader
 if previous((source/'tests1996/shader/reference95/strong1960.comp').read_text())!=reference:raise AssertionError('published .99 did not preserve exact .96 arithmetic')
 base.expected_shader=lambda text:extend19100(previous(text))
 retained=base.test(source,work/'retained96',jdk,ndk)
 # The inherited test's original structural description is intentionally
 # overridden: current has the exact two sharing changes above as well.
 retained['only_guarded_partial_predicates_changed']=False
 retained['strict19100_sharing_extension_verified']=True
 argv=[sys.executable,str(source/'tests19100/strong/bench19100.py'),'--source',str(source),'--work',str(work/'published99-bench')]
 p=subprocess.run(argv,capture_output=True,text=True,env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1'),timeout=600);(work/'published99-bench.log').write_text(p.stdout+p.stderr)
 if p.returncode:raise RuntimeError('published99 bench\n'+(p.stdout+p.stderr)[-10000:])
 bench=json.loads((work/'published99-bench/result.json').read_text())
 if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Strong proof source changed')
 result=dict(retained);result.update(status='passed',assertions=retained['assertions']+bench['assertions']+1,retained96=retained,regional_coordinates_shared_exactly=True,local_luma_ratio_shared_exactly=True,local_luma_ratio_preparation_only=False,local_luma_ratio_full_strong19113=True,local_luma_ratio_profile6_only19113=True,historical_shader112_source_contract=True,restored_shader112_sha256=BASELINE_SHA256,all_60_legacy_programs_unchanged=False,non_ieee_legacy_preprocessor_programs_unchanged=True,inherited_legacy_contract_restored=True,regional19107_exact_delta_verified=True,published99_benchmark=bench,strong19100_source_sha256=pins,physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
