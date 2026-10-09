#!/usr/bin/env python3
"""Bind actual host/package evidence and exact reproducible sources into assets."""
from pathlib import Path
import argparse,importlib.util,json,zipfile,io
from build1956 import *
from validate1956 import host_checks,SOURCE_GROUPS
QA_NAME=f'QA_ULike_v{VERSION}.json';SOURCE_ZIP=f'ULike_v{VERSION}_sources_and_QA.zip'
PUBLISHER_FILES=['publish1956.py','README.md','ulike1956-h28-h33-publish.yml']

def eligible(p,rel):return not p.is_symlink() and not any(part in ('__pycache__','tmp','build','dist','classes') or part.startswith('.') for part in rel.parts) and p.suffix.lower() in ('.java','.py','.c','.h','.comp','.sh','.txt','.json','.md','.dex','.tsv')
def source_zip(path,items):
 with zipfile.ZipFile(path,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for name,data in sorted(items.items()):
   info=zipfile.ZipInfo(name,(2026,10,8,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;z.writestr(info,data)
def release_notes(qa):
 r=qa['host_quality_result']
 return f'''ULike v{VERSION} / Hiro Morphe Patches v{BUNDLE_VERSION}
ULike v{BASE_VERSION} / 総合版 v{BASE_BUNDLE_VERSION} を基準にH28～H33を反映。

H28：NR1～NR4のDCT演算と主ノイズ除去の整数演算を、画素一致を条件にまとめて計算。
H29：従来CPU主ノイズ除去の横・縦処理を連結し、解析と受け渡しを削減。
H30：従来残留ノイズ処理で出力が変わらない画素の集計を事前判定で省略。
H31：実測で選んだGPU処理行数を、同じGPU・ドライバ・処理条件で再利用。
H32：従来GPU主ノイズ除去の作業領域を上限付きで再利用し、保持量を撮影メモリ予算へ反映。
H33：圧縮器が実際に解放された後の保存確認と次の圧縮を並行化。保存公開と完了通知の順序を維持。

NR1～NR4の単写ノイズ低減と合成なしを維持。解像度、美顔・補正、保存形式、圧縮品質は維持。
現在の通常単写保存経路はNR1～NR4を使用するため、H29・H30・H32の従来ノイズ処理の効果はその経路が動く場合に限る。
変更前後の計算結果、失敗時の戻し処理、保存順序、メモリ上限、JNI・ソフトウェアGPUのホスト検証：{r['assertions']}件成功。
ホスト検証は実機の速度向上や撮影成功を保証しない。Galaxy実機の動作・画質・速度は未確認。元APKSへの今回の適用は未実施。
合成・ノイズ・補正・圧縮・保存の区間計測は一部が重なるため、合計はシャッターから保存完了までの時間とは一致しない。

Morphe Managerでパッチソースを更新後、未改造ULike 5.6.2（740）へ再適用し、生成アプリを更新インストールしてください。
'''

def finalize(args):
 declaration=json.loads((args.source.parent/'manifest.json').read_text());qa_path=args.dist/QA_NAME;qa=json.loads(qa_path.read_text());result=host_checks(qa,args.source)
 spec=importlib.util.spec_from_file_location('publisher1956',args.source.parent/'publication/publish1956.py');pub=importlib.util.module_from_spec(spec);spec.loader.exec_module(pub);pub.configure(declaration)
 evbytes=args.validation.read_bytes();ev=json.loads(evbytes);artifacts={n:{'sha256':sha((args.dist/n).read_bytes()),'bytes':(args.dist/n).stat().st_size} for n in [SINGLE,BUNDLE]}
 require(ev.get('schema')=='ulike1956-desktop-validation-v1' and ev.get('status')=='passed' and ev.get('artifacts')==qa['artifacts']==artifacts and ev['host_quality_result_sha256']==qa['host_quality_result_sha256'] and ev['host_assertions']==result['assertions'] and ev['host_reports']==result['reports'] and ev['source_groups']=={k:qa[k] for k in SOURCE_GROUPS},'Fresh validation/build evidence differs')
 require(ev['serialized_dex_preservation_verified'] and ev['save_publication_hooks_inverse_verified'] and ev['single_image_capture_preserved'],'Required inverse evidence')
 for k,v in pub.REQUIRED_QA.items():actual=pub.lookup_qa(qa,k);require(type(actual) is type(v) and actual==v,'Required QA '+k)
 sources={'src/'+p.relative_to(args.source).as_posix():p.read_bytes() for p in sorted(args.source.rglob('*')) if p.is_file() and eligible(p,p.relative_to(args.source))};sources['manifest.json']=json_bytes(declaration)
 for name in PUBLISHER_FILES:sources['publication/'+name]=(args.source.parent/'publication'/name).read_bytes()
 qa.update(desktop_evidence_sha256=sha(evbytes),desktop_validation=ev,source_sha256={n:sha(b) for n,b in sources.items()});qa_path.write_bytes(json_bytes(qa));(args.dist/'RELEASE_NOTES.txt').write_text(release_notes(qa))
 package={**sources,'README.md':sources['publication/README.md'],'evidence/validation.json':evbytes}
 for n in [QA_NAME,'host-regression1956-result.json','native-builds1956.json','optimization-inventory1956.json','emitted-audit.tsv','helper-references.txt','emitted.log','metadata.log','native-installer-metadata.log','native-rows1956.tsv','RELEASE_NOTES.txt']:package[n]=(args.dist/n).read_bytes()
 require(package['helper-references.txt'].startswith(b'PASS helper references in ') and b'serialized_dex_verified=true' in package['emitted.log'] and b'all nine existing rows' in package['native-installer-metadata.log'],'Fresh diagnostic proof')
 source_zip(args.dist/SOURCE_ZIP,package);names=[SINGLE,BUNDLE,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt'];(args.dist/'SHA256SUMS.txt').write_text(''.join(sha((args.dist/n).read_bytes())+'  '+n+'\n' for n in names))
 contract=dict(pub.REQUIRED_QA)
 for key in ['changed_runtime_methods','changed_native_methods','new_helper_classes','new_runtime_aliases','new_native_methods','new_jni_methods','new_runtime_methods','removed_runtime_methods','replaced_helper_roots']:contract[key]=qa[key]
 contract.update({'host_quality_result.assertions':result['assertions'],'host_quality_result':result})
 generated={**declaration,'schema':'ulike1956-publication-v1','qa_required_values':contract,'required_source_paths':sorted(n for n in sources if n.startswith('src/')),'native_payloads':ev['native_payloads'],'unchanged_native_payloads':ev['unchanged_native_payloads'],'artifacts':{n:{'bytes':(args.dist/n).stat().st_size,'sha256':sha((args.dist/n).read_bytes())} for n in names+['SHA256SUMS.txt']}}
 args.manifest.parent.mkdir(parents=True,exist_ok=True);args.manifest.write_bytes(json_bytes(generated));pub.load_expected(args.manifest);print('PASS finalized source-pinned H28-H33 artifacts and publication manifest');return generated
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['dist','source','validation','manifest']:p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();finalize(a)
