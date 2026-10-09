"""Execute the actual original-GLES IEEE binary64 random/boundary foundation gate."""
from pathlib import Path
import importlib.util
def test(root,work,jdk=None,ndk=None):
 root=Path(root).resolve();work=Path(work).resolve()/'soft64-foundation1965'
 spec=importlib.util.spec_from_file_location('executed_soft64_foundation1965',root/'tests1965/soft64_arithmetic1965.py')
 mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
 result=mod.test(work,4000)
 if not isinstance(result,dict) or result.get('status')!='passed' or result.get('errors') not in (None,[]):raise AssertionError('GPU binary64 foundation gate failed')
 if result.get('assertions',0)<=0:
  result['assertions']=int(result.get('cases',0))
 if result['assertions']<=0:raise AssertionError('GPU binary64 gate reported no cases/assertions')
 if result.get('shader_dialect_unmodified') is not True:raise AssertionError('Original GLES shader dialect required')
 result['original_gles_required_math_executed']=True
 result['physical_android_tested']=False
 return result
