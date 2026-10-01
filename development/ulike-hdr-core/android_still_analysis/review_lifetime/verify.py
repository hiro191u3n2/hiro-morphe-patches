#!/usr/bin/env python3
"""Independent scoped lifetime review; does not install or enable patches."""
import hashlib,json,pathlib,subprocess,tempfile
ROOT=pathlib.Path(__file__).resolve().parent
MODULE=ROOT.parent
WORK=MODULE.parents[2]
TOOLS=WORK/'models168/tools';JDK=TOOLS/'jdk21/jdk-21.0.12.1+1/bin';APP=WORK/'complete_integration169';LIFE=APP/'lifecycle'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(args):
 p=subprocess.run([str(x) for x in args],capture_output=True,text=True)
 if p.returncode:raise RuntimeError(p.stdout+'\n'+p.stderr)
 return p.stdout.strip()
production=[MODULE/'src/main/java/com/hiro/ulike/hdr/stillanalysis'/name for name in ['RecorderAdmission.java','NativeLifetimeBoundary.java','NativeLifetimeHooks.java']]
with tempfile.TemporaryDirectory(prefix='ulike-independent-lifetime-') as temp:
 t=pathlib.Path(temp);classes=t/'classes';classes.mkdir();tools=t/'tools';tools.mkdir();delta=t/'delta'
 run([JDK/'javac','--release','17','-Xlint:all','-Werror','-d',classes,*production,ROOT/'NativeLifetimeReview.java'])
 host=run([JDK/'java','-cp',classes,'com.hiro.ulike.hdr.stillanalysis.NativeLifetimeReview'])
 dis=run([JDK/'javap','-c','-private','-cp',classes,'com.hiro.ulike.hdr.stillanalysis.NativeLifetimeHooks'])
 assert 'invokestatic' not in dis and 'NativeLifetimeBoundary.' not in dis and 'RecorderAdmission.' not in dis
 cp=':'.join(map(str,[TOOLS/'morphe-1.16.jar',APP/'tool_baseline',tools]))
 transforms=[APP/'integration/MergePayloads.java',LIFE/'PrepareNativeLifetime169.java',LIFE/'VerifyNativeLifetime169.java',ROOT/'IndependentNativeDexReview.java']
 run([JDK/'javac','-cp',cp,'-d',tools,*transforms])
 stock=WORK/'models168/inputs/base.apk';assert sha(stock)=='f51c64cc2ffedf2110f18816831e5c7cff5652980b42d6bb22a4a3f37316730d'
 generated=run([JDK/'java','-Xmx2g','-cp',cp,'PrepareNativeLifetime169',stock,delta])
 reconstruction=run([JDK/'java','-Xmx2g','-cp',cp,'VerifyNativeLifetime169',stock,delta/'native-lifetime-partial.dex'])
 dex=run([JDK/'java','-Xmx2g','-cp',cp,'IndependentNativeDexReview',stock,delta/'native-lifetime-partial.dex'])
 inspected=production+transforms+[ROOT/'NativeLifetimeReview.java',ROOT/'verify.py']
 out={'schema':'ulike-native-lifetime-independent-review-1','scope':'Original source review + isolated host ledger tests + reserialized private three-method delta inspection; no application install, coverage enable, ART or phone execution.',
 'host_checks':host,'disabled_hooks_have_no_static_calls':True,'private_generation':generated,'exact_original_reconstruction':reconstruction,'independent_dex_checks':dex,'stock_apk_sha256':sha(stock),'private_delta_sha256':sha(delta/'native-lifetime-partial.dex'),
 'source_sha256':{str(p.relative_to(WORK)):sha(p) for p in inspected},'helper_binary_sha256':{'MethodContract.class':sha(APP/'tool_baseline/app/hiro/ulike/patches/MethodContract.class')},
 'findings_fixed':[{'issue':'Runtime Errors during completion could remove TLS bookkeeping without quarantining pending native state.','resolution':'NativeLifetimeBoundary catches RuntimeException|Error, quarantines ticket before pop.'},{'issue':'Exceptional cleanup implicitly allocated an empty ThreadLocal deque, which could mask an original allocation Error; mismatched/missing cleanup was not quarantined.','resolution':'No initialValue allocation; allocation occurs before admission ticket at entry. Exceptional cleanup reads nullable TLS, quarantines before matching cleanup, and injected wrapper guards Throwable to preserve the original exception.'}],
 'verified_cases':['disabled hooks do not install admission or allocate TLS','nested separate invokers preserve tickets','same-invoker overlap and double init rejected before native work','Java handler cleared before actual native uninit does not release ledger early','failed native init with nonzero allocated handle retained until actual teardown','successful init with zero handle quarantined','native/getter exceptions and mismatched exceptional callbacks quarantine','cross-thread return cannot consume another thread ticket','all six original return sites retain exact status register','entry and exceptional handler excluded from added catch','v0 original throwable identity is retained and rethrown','all three original methods reconstruct exact original MethodContract SHA after removing only declared hooks'],
 'full_constructor_backend_worker_coverage':False,'actual_composer_restore_barrier':False,'hooks_installed':False,'hooks_enabled':False,'device_tested':False,'blocking_findings_within_reviewed_disabled_scope':[]}
 (ROOT/'INDEPENDENT_REVIEW.json').write_text(json.dumps(out,indent=2)+'\n')
 print(host);print(reconstruction);print(dex)
