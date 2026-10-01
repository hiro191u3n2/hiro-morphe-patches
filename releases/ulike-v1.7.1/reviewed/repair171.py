#!/usr/bin/env python3
"""Generate the narrow v1.7.1 hotfix from the exact public v1.7.0 source archive."""
from pathlib import Path
import shutil,json,hashlib,zipfile,argparse
SOURCE_SHA='da3387ccadb68fb6e86c8cb6c6b0d94baaf3d8db098c775e633396398f014e92'; APP='1.7.1'; BUNDLE='1.0.104'
PINS={'ULike_HQ_Texture_Online_v1.7.0.mpp':'26340855e287352e77dc0be27ff8eb6836a2e9b795eda3943edd2fc873680278','Hiro_Morphe_Patches_v1.0.103.mpp':'df9409136837bb8edb13dedc95ebc38711d95e2bd1c1a73d2b6770cf86f34807'}
def replace(text,old,new):
    assert text.count(old)==1, 'Expected one exact source anchor: '+old[:100]
    return text.replace(old,new,1)

def main():
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
    raw=a.source.read_bytes()
    assert hashlib.sha256(raw).hexdigest()==SOURCE_SHA,'Wrong baseline sources'
    root=a.out;root.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(a.source) as z:
        for n in z.namelist():
            assert not Path(n).is_absolute() and '..' not in Path(n).parts
            if n.startswith('qa170/') or n in ['SOURCE_FILES.json','expected-artifacts.json']:continue
            t=root/n;t.parent.mkdir(parents=True,exist_ok=True);t.write_bytes(z.read(n))
    (root/'qa170').mkdir(exist_ok=True)
    f=root/'src170/com/hiro/ulike/ManualLens170.java';s=f.read_text()
    start=s.index('    public static CaptureRequest.Builder createRequest(');end=s.index('    public static CaptureRequest build(',start)
    s=s[:start]+'''    // Failures in optional routing must use ULike's existing checked-camera-error
    // path, not terminate the camera thread with IllegalAccessError/RuntimeException.
    private static void reportFailure(OpticalZoom.Route r,String reason){
        try{OpticalZoom.fail(r,reason);}
        catch(RuntimeException|LinkageError error){status(reason+" / 復旧通知失敗: "+error.getClass().getSimpleName());}
    }
    private static CameraAccessException cameraFailure(OpticalZoom.Route r,String phase,Throwable error){
        String reason=phase+": "+error.getClass().getSimpleName();
        reportFailure(r,reason);
        if(error instanceof CameraAccessException)return (CameraAccessException)error;
        return new CameraAccessException(CameraAccessException.CAMERA_ERROR,reason,error);
    }
    public static CaptureRequest.Builder createRequest(CameraDevice d,int template)throws CameraAccessException{
        OpticalZoom.Route r=null;
        try{
            r=route(d);
            CaptureRequest.Builder b=null;boolean keys=false;
            if(r!=null&&r.physical()&&Build.VERSION.SDK_INT>=28){
                List<CaptureRequest.Key<?>> supported=r.logical.getAvailablePhysicalCameraRequestKeys();
                if(supported!=null&&!supported.isEmpty())try{
                    b=d.createCaptureRequest(template,Collections.singleton(r.physicalId));keys=true;
                }catch(RuntimeException|LinkageError optional){
                    // Only optional per-physical controls are abandoned. Fixed output
                    // binding is still required and is not misreported as successful.
                    status("物理レンズ追加制御を使わず再試行: "+optional.getClass().getSimpleName());
                }
            }
            if(b==null)b=d.createCaptureRequest(template);
            if(r!=null)synchronized(get("LOCK")){((Map<CaptureRequest.Builder,OpticalZoom.Route>)get("builders")).put(b,r);}
            synchronized(PHYSICAL_KEYS){PHYSICAL_KEYS.put(b,keys);}
            return b;
        }catch(CameraAccessException|ReflectiveOperationException|RuntimeException|LinkageError error){
            throw cameraFailure(r,"撮影リクエストの初期化失敗",error);
        }
    }
''' + s[end:]
    start=s.index('    public static void createSession(');end=s.index('    private static final class State',start)
    s=s[:start]+'''    public static void createSession(CameraDevice d,SessionConfiguration config)throws CameraAccessException{
        OpticalZoom.Route r=null;
        try{
            r=route(d);
            if(r==null){d.createCaptureSession(config);return;}
            if(r.physical()){
                if(r.failed||config.getSessionType()!=SessionConfiguration.SESSION_REGULAR||config.getInputConfiguration()!=null)
                    throw new IllegalArgumentException("This session cannot bind a fixed physical camera");
                if(config.getOutputConfigurations().isEmpty())throw new IllegalArgumentException("No lens outputs");
                for(OutputConfiguration out:config.getOutputConfigurations())out.setPhysicalCameraId(r.physicalId);
            }
            SessionConfiguration wrapped=new SessionConfiguration(config.getSessionType(),config.getOutputConfigurations(),config.getExecutor(),new State(r,config.getStateCallback()));
            if(config.getInputConfiguration()!=null)wrapped.setInputConfiguration(config.getInputConfiguration());
            if(config.getSessionParameters()!=null)wrapped.setSessionParameters(config.getSessionParameters());
            d.createCaptureSession(wrapped);
        }catch(CameraAccessException|ReflectiveOperationException|RuntimeException|LinkageError error){
            throw cameraFailure(r,"選択レンズのセッション初期化失敗",error);
        }
    }
''' + s[end:]
    s=replace(s,'if(yes("switching"))OpticalZoom.fail(route,label(requested)+"の固定レンズを開けません");','if(yes("switching"))reportFailure(route,label(requested)+"の固定レンズを開けません");')
    s=replace(s,'public void onConfigureFailed(CameraCaptureSession s){synchronized(PINNED){PINNED.remove(s);}OpticalZoom.fail(r,"選択レンズの出力構成が非対応です");if(next!=null)next.onConfigureFailed(s);}', 'public void onConfigureFailed(CameraCaptureSession s){r.configured=false;synchronized(PINNED){PINNED.remove(s);}reportFailure(r,"選択レンズの出力構成が非対応です");if(next!=null)next.onConfigureFailed(s);}')
    s=replace(s,'public void onClosed(CameraCaptureSession s){synchronized(PINNED){PINNED.remove(s);}if(next!=null)next.onClosed(s);}', 'public void onClosed(CameraCaptureSession s){r.configured=false;synchronized(PINNED){PINNED.remove(s);}if(next!=null)next.onClosed(s);}')
    f.write_text(s)
    (root/'Transform170.java').write_text(TRANSFORM)
    (root/'AuditAccess171.java').write_text(AUDIT)
    f=root/'host170/android/hardware/camera2/CameraAccessException.java'
    f.write_text('package android.hardware.camera2;public class CameraAccessException extends Exception{public static final int CAMERA_ERROR=3;public CameraAccessException(int i){super("camera "+i);}public CameraAccessException(int i,String m,Throwable e){super(m,e);}}\n')
    f=root/'host170/android/hardware/camera2/CameraDevice.java';s=f.read_text()
    s=replace(s,'public int ordinary,physical;','public int ordinary,physical;public RuntimeException sessionRuntime,requestRuntime;public LinkageError sessionLinkage,requestLinkage,optionalLinkage;')
    s=replace(s,'ordinary++;return new CaptureRequest.Builder();','ordinary++;if(requestRuntime!=null)throw requestRuntime;if(requestLinkage!=null)throw requestLinkage;return new CaptureRequest.Builder();')
    s=replace(s,'physical++;if(rejectOptional)','physical++;if(optionalLinkage!=null)throw optionalLinkage;if(rejectOptional)')
    s=replace(s,'if(failSession)throw new CameraAccessException(2);','if(sessionRuntime!=null)throw sessionRuntime;if(sessionLinkage!=null)throw sessionLinkage;if(failSession)throw new CameraAccessException(2);')
    f.write_text(s)
    f=root/'host170/com/hiro/ulike/OpticalZoom.java';s=f.read_text()
    s=replace(s,'static int starts,startedNominal,messages,failures;','static int starts,startedNominal,messages,failures;static LinkageError failureLinkage;')
    s=replace(s,'failures++;r.failed=true;status=text;','failures++;if(failureLinkage!=null)throw failureLinkage;if(r!=null)r.failed=true;status=text;')
    s=replace(s,'static void reset(){inventory=null;','static void reset(){failureLinkage=null;inventory=null;');f.write_text(s)
    f=root/'host170/com/hiro/ulike/ManualLens170Test.java';s=f.read_text()
    s=replace(s,'catch(IllegalArgumentException expected){rejected=true;}','catch(CameraAccessException expected){rejected=expected.getCause() instanceof IllegalArgumentException;}')
    s=replace(s,'catch(IllegalArgumentException e){rejected=true;}','catch(CameraAccessException e){rejected=e.getCause() instanceof IllegalArgumentException;}')
    s=replace(s,'System.out.println("PASS ManualLens170: "+checks+" assertions; mocked Camera2/session callbacks, NOT Galaxy execution.");',TESTS+'\n  System.out.println("PASS ManualLens171: "+checks+" assertions; mocked Camera2/session callbacks, NOT Galaxy execution.");')
    f.write_text(s)
    f=root/'build170.py';s=f.read_text()
    s=replace(s,"PINS={'ULike_HQ_Texture_Online_v1.6.9.mpp':'fb0427d8ff11433ccb653c2a6e15eaa5214b533aa8859d4c5d946cb8aeabf244','Hiro_Morphe_Patches_v1.0.102.mpp':'161b884f7dccdab8424054dec4f961e239148c7e2c17ac70231d2e9ab659a89b'}",'PINS='+repr(PINS))
    s=s.replace("a.input/'ULike_HQ_Texture_Online_v1.6.9.mpp'","a.input/'ULike_HQ_Texture_Online_v1.7.0.mpp'").replace("a.input/'Hiro_Morphe_Patches_v1.0.102.mpp'","a.input/'Hiro_Morphe_Patches_v1.0.103.mpp'")
    s=replace(s,"{'MainMacro168','MacroPolicy168','MacroUi168','LensFast167','ManualLens170'}","{'ManualLens170'}")
    s=replace(s,"assert len(helperclasses)>=13,'Missing manual routing helpers'","assert len(helperclasses)==3,'Unexpected manual helper inventory'")
    s=replace(s,"'VerifyHelperReferences.java','VerifyApplied.java'","'VerifyHelperReferences.java','VerifyApplied.java','AuditAccess171.java'")
    s=replace(s,"for cls,args,log in [('Transform170'","for cls,args,log in [('AuditAccess171',[b/'old-runtime.dex','3'],'ACCESS_BEFORE.txt'),('Transform170'")
    s=replace(s,"'HELPER_REFERENCES.txt')]:","'HELPER_REFERENCES.txt'),('AuditAccess171',[b/'runtime.dex','0'],'ACCESS_AFTER.txt')]:")
    s=replace(s,"'v1.6.9',b/'description.txt'","'v1.7.0',b/'description.txt'")
    # Generation and exact-byte verification are performed explicitly by the release
    # workflow; avoid running the predecessor's stale reproduction/QA path.
    s=replace(s,'a=p.parse_args();a.input=a.input.resolve();','a=p.parse_args();a.bootstrap=True;a.input=a.input.resolve();')
    cut=s.index(' if not a.bootstrap:\n  expected=');s=s[:cut]+"\nif __name__=='__main__':main()\n"
    s=s.replace('Reproduce v1.7.0 from exact v1.6.9','Reproduce v1.7.1 from exact v1.7.0')
    f.write_text(s)
    f=root/'package170.py';s=f.read_text().replace('fb0427d8ff11433ccb653c2a6e15eaa5214b533aa8859d4c5d946cb8aeabf244',PINS['ULike_HQ_Texture_Online_v1.7.0.mpp']).replace('161b884f7dccdab8424054dec4f961e239148c7e2c17ac70231d2e9ab659a89b',PINS['Hiro_Morphe_Patches_v1.0.103.mpp']);f.write_text(s)
    (root/'release170.json').write_text(json.dumps({'timestamp':'2026-10-02T06:45:00','patch_description':'v1.7.1 起動不具合対策：レンズ失敗処理のprivateアクセスを修正し、初期化例外を既存カメラエラー経路へ変換。手動1倍メイン広角・接写・3倍・5倍、距離自動切替OFF、美顔・HEIF保存を保持。Galaxy実機未検証。','manifest_description':'ULike startup linkage and camera error-path hotfix; manual lens routing retained. Device unverified.','standalone':{'name':'ULike HQ Texture Online','version':'1.7.1','filename':'ULike_HQ_Texture_Online_v1.7.1.mpp'},'integrated':{'name':'Hiro Morphe Patches','version':'1.0.104','filename':'Hiro_Morphe_Patches_v1.0.104.mpp'}},ensure_ascii=False,indent=2)+'\n')
    (root/'release_pipeline/publish170.py').write_text("#!/usr/bin/env python3\nimport publish\npublish.VERSION,publish.PREVIOUS_APP='1.7.1','1.7.0'\npublish.BUNDLE,publish.PREVIOUS='1.0.104','1.0.103'\npublish.TAG='ulike-v1.7.1'\npublish.UPLOAD_BRANCHES=publish.BRANCHES+('work/ulike-startup-v171',)\nif __name__=='__main__':publish.main()\n")
    for n in ['LOCAL_QA.json','README.md','RELEASE_NOTES.txt','CHANGELOG_SUMMARY.txt']:(root/'release_pipeline'/n).unlink(missing_ok=True)
    print('Generated v1.7.1 hotfix sources from exact v1.7.0 archive.')

TRANSFORM=r'''import java.nio.file.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.immutable.*;import com.android.tools.smali.dexlib2.immutable.reference.*;import com.android.tools.smali.dexlib2.builder.*;import com.android.tools.smali.dexlib2.builder.instruction.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;
public final class Transform170 {
 public static void main(String[] a)throws Exception{
  var old=MergePayloads.classes(a[0]);var all=new TreeMap<>(old);var helpers=MergePayloads.classes(a[1]);Path out=Path.of(a[2]);Files.createDirectories(out);
  for(ClassDef c:helpers.values()){
   MergePayloads.require(c.getType().equals("Lcom/hiro/ulike/ManualLens170;")||c.getType().startsWith("Lcom/hiro/ulike/ManualLens170$"),"Compile stub leaked: "+c.getType());all.put(c.getType(),c);
  }
  int access=0,version=0;String z="Lcom/hiro/ulike/OpticalZoom;",adv="Lcom/hiro/ulike/CaptureAdvanced3;";
  for(String type:List.of(z,adv)){
   ClassDef c=all.get(type);List<Method> methods=new ArrayList<>();
   for(Method m:c.getMethods()){
    int flags=m.getAccessFlags();var impl=m.getImplementation();
    if(type.equals(z)&&m.getName().equals("fail")){
     MergePayloads.require(flags==(AccessFlags.PRIVATE.getValue()|AccessFlags.STATIC.getValue()),"Unexpected failure visibility");flags=AccessFlags.STATIC.getValue();access++;
    }
    if(type.equals(adv)&&impl!=null){var b=new MutableMethodImplementation(impl);
     for(int n=0;n<b.getInstructions().size();n++){var ins=b.getInstructions().get(n);
      if(ins instanceof ReferenceInstruction ri&&ri.getReference() instanceof StringReference sr&&sr.getString().equals("ULike Capture v1.7.0\n")){
       int reg=((OneRegisterInstruction)ins).getRegisterA();var ref=new ImmutableStringReference("ULike Capture v1.7.1\n");b.replaceInstruction(n,ins.getOpcode()==Opcode.CONST_STRING_JUMBO?new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,reg,ref):new BuilderInstruction21c(Opcode.CONST_STRING,reg,ref));version++;
      }
     }impl=b;
    }
    methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),flags,m.getAnnotations(),m.getHiddenApiRestrictions(),impl));
   }
   all.put(type,new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
  }
  MergePayloads.require(access==1&&version==1,"Expected one exact access fix and version marker");
  MergePayloads.writeDex(out.resolve("runtime.dex"),all.values());var emitted=MergePayloads.classes(out.resolve("runtime.dex").toString());
  var before=MergePayloads.methods(old.values());var after=MergePayloads.methods(emitted.values());int kept=0,changed=0,added=0;List<String> audit=new ArrayList<>();
  for(var e:before.entrySet()){Method now=after.get(e.getKey());MergePayloads.require(now!=null,"Removed runtime method "+e.getKey());if(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(now))){kept++;}else{changed++;audit.add("changed\t"+e.getKey());}}
  for(String id:after.keySet())if(!before.containsKey(id)){added++;audit.add("added\t"+id);}
  for(var e:old.entrySet())if(!helpers.containsKey(e.getKey())&&!e.getKey().equals(z)&&!e.getKey().equals(adv))MergePayloads.require(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(emitted.get(e.getKey()))),"Unrelated class changed "+e.getKey());
  for(Method m:after.values())if(m.getDefiningClass().equals(z)&&!m.getName().equals("fail"))MergePayloads.require(MergePayloads.hash(before.get(MergePayloads.id(m))).equals(MergePayloads.hash(m)),"Manual entrypoint or other OpticalZoom code changed");
  audit.add("PASS runtime: preserved="+kept+", changed="+changed+", added="+added+", total="+after.size()+"; one private-to-package access fix; manual routing entrypoints preserved.");Files.write(out.resolve("runtime-audit.tsv"),audit);System.out.println(audit.get(audit.size()-1));
 }
}
'''
AUDIT=r'''import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;import com.android.tools.smali.dexlib2.formatter.DexFormatter;
public class AuditAccess171{
 static String id(MethodReference m){return DexFormatter.INSTANCE.getMethodDescriptor(m);}static String id(FieldReference f){return DexFormatter.INSTANCE.getFieldDescriptor(f);}
 public static void main(String[] args)throws Exception{
  var classes=MergePayloads.classes(args[0]);Map<String,Method> ms=new HashMap<>();Map<String,Field> fs=new HashMap<>();for(var c:classes.values()){for(var m:c.getMethods())ms.put(id(m),m);for(var f:c.getFields())fs.put(id(f),f);}
  int n=0;
  for(var c:classes.values())for(var m:c.getMethods()){if(m.getImplementation()==null)continue;for(var ins:m.getImplementation().getInstructions())if(ins instanceof ReferenceInstruction ri){var ref=ri.getReference();
   if(ref instanceof MethodReference mr){var t=ms.get(id(mr));if(t!=null&&AccessFlags.PRIVATE.isSet(t.getAccessFlags())&&!c.getType().equals(t.getDefiningClass())){System.out.println("ILLEGAL private method "+id(m)+" -> "+id(mr));n++;}}
   else if(ref instanceof FieldReference fr){var t=fs.get(id(fr));if(t!=null&&AccessFlags.PRIVATE.isSet(t.getAccessFlags())&&!c.getType().equals(t.getDefiningClass())){System.out.println("ILLEGAL private field "+id(m)+" -> "+id(fr));n++;}}
  }}
  System.out.println("PRIVATE_ACCESS_VIOLATIONS="+n);if(n!=Integer.parseInt(args[1]))throw new IllegalStateException("Unexpected private-member linkage audit result");
 }
}
'''
TESTS=r'''// Hotfix regressions: preserve the checked camera contract for failure paths.
  for(int kind=0;kind<4;kind++){
   o=setup();r=OpticalZoom.active;d=new CameraDevice("L");OpticalZoom.devices.put(d,r);
   Throwable injected=kind%2==0?new IllegalStateException("injected"):new NoSuchMethodError("injected");
   if(kind==0)d.sessionRuntime=(RuntimeException)injected;
   if(kind==1)d.sessionLinkage=(LinkageError)injected;
   if(kind==2)d.requestRuntime=(RuntimeException)injected;
   if(kind==3)d.requestLinkage=(LinkageError)injected;
   CameraAccessException caught=null;
   try{if(kind<2)ManualLens170.createSession(d,new SessionConfiguration(0,Arrays.asList(new OutputConfiguration()),Runnable::run,null));else ManualLens170.createRequest(d,1);}catch(CameraAccessException e){caught=e;}
   ok(caught!=null&&caught.getCause()==injected,"runtime/linkage errors become checked camera failures "+kind);
   ok(r.failed&&OpticalZoom.failures==1,"failure recovery notified once "+kind);
  }
  o=setup();r=OpticalZoom.active;d=new CameraDevice("L");OpticalZoom.devices.put(d,r);r.logical.physicalKeys.add(CaptureRequest.SCALER_CROP_REGION);d.optionalLinkage=new NoSuchMethodError("optional");b=ManualLens170.createRequest(d,1);
  ok(b!=null&&d.physical==1&&d.ordinary==1,"optional linkage failure uses ordinary request once");ok(!r.failed&&OpticalZoom.failures==0,"optional request fallback does not falsely fail the fixed outputs");
  o=setup();r=OpticalZoom.active;d=new CameraDevice("L");OpticalZoom.devices.put(d,r);d.failSession=true;CameraAccessException hardware=null;
  try{ManualLens170.createSession(d,new SessionConfiguration(0,Arrays.asList(new OutputConfiguration()),Runnable::run,null));}catch(CameraAccessException e){hardware=e;}
  ok(hardware!=null&&hardware.getCause()==null,"existing CameraAccessException identity/cause preserved");ok(OpticalZoom.failures==1,"hardware failure notified once");
  o=setup();r=OpticalZoom.active;d=new CameraDevice("L");OpticalZoom.devices.put(d,r);final int[] callbacks={0,0};
  CameraCaptureSession.StateCallback next=new CameraCaptureSession.StateCallback(){public void onConfigured(CameraCaptureSession x){callbacks[0]++;}public void onConfigureFailed(CameraCaptureSession x){callbacks[1]++;}};
  ManualLens170.createSession(d,new SessionConfiguration(0,Arrays.asList(new OutputConfiguration()),Runnable::run,next));s=new CameraCaptureSession(d);
  d.config.getStateCallback().onConfigured(s);ok(callbacks[0]==1&&r.configured,"configured callback is preserved");
  OpticalZoom.failureLinkage=new IllegalAccessError("injected error notification failure");d.config.getStateCallback().onConfigureFailed(s);
  ok(callbacks[1]==1&&!r.configured,"error callback survives an optional recovery-linkage failure");
  OpticalZoom.failureLinkage=null;d.config.getStateCallback().onClosed(s);ok(!r.configured,"closed session is not left configured");
  o=setup();r=OpticalZoom.active;d=new CameraDevice("L");OpticalZoom.devices.put(d,r);d.sessionRuntime=new IllegalArgumentException("unsupported");OpticalZoom.failureLinkage=new IllegalAccessError("injected");
  CameraAccessException protectedError=null;try{ManualLens170.createSession(d,new SessionConfiguration(0,Arrays.asList(new OutputConfiguration()),Runnable::run,null));}catch(CameraAccessException e){protectedError=e;}
  ok(protectedError!=null&&protectedError.getCause()==d.sessionRuntime,"notification failure does not replace original camera error");OpticalZoom.failureLinkage=null;'''
if __name__=='__main__':main()
