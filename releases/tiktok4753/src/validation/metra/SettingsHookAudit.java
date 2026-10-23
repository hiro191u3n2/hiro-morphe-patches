import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import app.morphe.patches.tiktok.misc.settings.*;
public final class SettingsHookAudit {
 static Map<String,ClassDef> load(String apk, Set<String> wanted) throws Exception {
  Map<String,ClassDef> result=new HashMap<>();
  try(var z=new ZipFile(apk)) {var es=z.entries();while(es.hasMoreElements()) {var e=es.nextElement();if(!e.getName().matches("classes(?:[0-9]+)?\\.dex"))continue;
   var d=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),new BufferedInputStream(z.getInputStream(e)));
   for(var c:d.getClasses())if(wanted==null||wanted.contains(c.getType()))result.put(c.getType(),c);
  }} return result;
 }
 static Method method(ClassDef c,String name) {Method found=null;for(var m:c.getMethods())if(m.getName().equals(name)){if(found!=null)throw new IllegalStateException("Ambiguous "+name);found=m;}if(found==null)throw new IllegalStateException("Missing "+name);return found;}
 static List<Instruction> insns(Method m) {List<Instruction>a=new ArrayList<>();if(m.getImplementation()!=null)for(var i:m.getImplementation().getInstructions())a.add(i);return a;}
 static boolean refContains(Method m,String str){for(var i:insns(m))if(i instanceof ReferenceInstruction r&&r.getReference().toString().contains(str))return true;return false;}
 static boolean classString(ClassDef c,String str){for(var m:c.getMethods())for(var i:insns(m))if(i instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().equals(str))return true;return false;}
 static boolean calls(Method m,String owner,String name){for(var i:insns(m))if(i instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference x&&x.getDefiningClass().equals(owner)&&x.getName().equals(name))return true;return false;}
 static boolean settingsBackReachable(ClassDef activity){
  var entry=method(activity,"onBackPressed");
  String settings="Lapp/morphe/extension/tiktok/settings/TikTokActivityHook;";
  if(calls(entry,settings,"handleBackPressed"))return true;
  // OneBack moves the original native callback into a private helper. Verify
  // the emitted guarded fallback rather than searching unrelated methods.
  var code=insns(entry);String runtime="Lapp/hiro/oneback/runtime/OneBackExit;";
  if(code.size()!=12||code.get(0).getOpcode()!=Opcode.INVOKE_STATIC
    ||!(code.get(0) instanceof ReferenceInstruction first)
    ||!(first.getReference() instanceof MethodReference firstCall)
    ||!firstCall.getDefiningClass().equals(runtime)||!firstCall.getName().equals("tryBack")
    ||code.get(1).getOpcode()!=Opcode.MOVE_RESULT||code.get(2).getOpcode()!=Opcode.IF_EQZ
    ||code.get(3).getOpcode()!=Opcode.RETURN_VOID||code.get(5).getOpcode()!=Opcode.INVOKE_DIRECT)return false;
  int branchAddress=code.get(0).getCodeUnits()+code.get(1).getCodeUnits();
  int fallbackAddress=branchAddress+code.get(2).getCodeUnits()+code.get(3).getCodeUnits();
  if(branchAddress+((OffsetInstruction)code.get(2)).getCodeOffset()!=fallbackAddress)return false;
  var target=(MethodReference)((ReferenceInstruction)code.get(5)).getReference();
  if(!target.getDefiningClass().equals(activity.getType())||!target.getName().equals("hiroOneBack$onBackPressed")
     ||!target.getParameterTypes().isEmpty()||!target.getReturnType().equals("V"))return false;
  var helper=method(activity,target.getName());
  if((helper.getAccessFlags()&2)==0||!calls(entry,runtime,"enterBack")||!calls(entry,runtime,"afterNative")
     ||!calls(entry,runtime,"leaveNative")||entry.getImplementation().getTryBlocks().isEmpty())return false;
  return calls(helper,settings,"handleBackPressed");
 }
 static void demand(boolean b,String n){if(!b)throw new IllegalStateException(n);System.out.println("PASS "+n);}
 static void show(Method m){System.out.println("METHOD "+m);for(var i:insns(m)){if(i instanceof ReferenceInstruction r)System.out.println("  "+i.getOpcode()+" "+r.getReference());else if(i instanceof NarrowLiteralInstruction n)System.out.println("  "+i.getOpcode()+" 0x"+Integer.toHexString(n.getNarrowLiteral()));}}
 public static void main(String[]a)throws Exception{
  var original=load(a[0],null);System.out.println("native_classes="+original.size());
  String vm="Lcom/ss/android/ugc/aweme/setting/ui/rvmpcompose/group/support/cells/OpenDebugCellVM;";
  String fragment="Lcom/ss/android/ugc/aweme/setting/ui/rvmpcompose/SettingsComposeRvmpFragment;";
  String activity="Lcom/bytedance/ies/ugc/aweme/commercialize/compliance/personalization/AdPersonalizationActivity;";
  var stateMethod=method(original.get(vm),"defaultState");String state=SettingsPatchKt.openDebugStateClass(stateMethod);System.out.println("OpenDebugState="+state);
  List<Method> compose=new ArrayList<>();for(var c:original.values())for(var m:c.getMethods())if(SettingsPatchKt.isOpenDebugRowComposeMethod(m,state))compose.add(m);
  demand(compose.size()==1,"one OpenDebug row compose in real original APK");var row=compose.get(0);System.out.println("OpenDebugRow="+row);
  var wrapper=SettingsPatchKt.openDebugClickWrapper(row,state);String wrapperClass=wrapper.getClassName(),wrapperMethod=wrapper.getInvokeName();System.out.println("OpenDebugClick="+wrapperClass+"->"+wrapperMethod);
  String function=SettingsPatchKt.openDebugFunction2LambdaClass(stateMethod);System.out.println("OpenDebugFunction2="+function);
  var top=ForYouLongPressKt.findTopTabBind(original::get);demand(top.getSite()!=null,"For You top tab exact structural match: "+top.getMiss());var topSite=top.getSite();System.out.println("ForYouBind="+topSite.getMethod());System.out.println("ForYouTagRead="+topSite.getTagRead());
  Set<String>wanted=new HashSet<>(List.of(vm,state,fragment,activity,row.getDefiningClass(),wrapperClass,function,topSite.getMethod().getDefiningClass(),"Lapp/morphe/extension/tiktok/settings/L10nTranslations;","Lapp/hiro/tiktokjapanese/runtime/JapaneseTranslations;"));
  String renderer="Lcom/ss/android/ugc/aweme/commercialize/hybrid/impl/web/AdWebMoreActionService;";List<Method>renders=new ArrayList<>();for(var m:original.get(renderer).getMethods())if(m.getReturnType().equals("V")&&m.getParameterTypes().toString().equals("[Lcom/bytedance/hybrid/spark/SparkContext;, Landroid/content/Context;, Lcom/bytedance/tux/icon/TuxIconView;]"))renders.add(m);
  demand(renders.size()==1,"settings icon renderer");var iconMethod=renders.get(0);List<ClassDef>keys=new ArrayList<>();for(var c:original.values())if(classString(c,"ad_browser_settings"))keys.add(c);
  int icon=SettingsPatchKt.resolveSettingsIconResourceId(iconMethod,keys);System.out.println("nativeSettingsGear=0x"+Integer.toHexString(icon));
  original=null;System.gc();var patched=load(a[1],wanted);
  var act=patched.get(activity);demand(refContains(method(act,"onCreate"),"TikTokActivityHook;->initialize"),"native activity onCreate routes to settings initialize");demand(settingsBackReachable(act),"native activity Back retains Settings handler directly or through verified OneBack fallback");
  var click=method(patched.get(wrapperClass),wrapperMethod);demand(refContains(click,"morphe_settings"),"compose click carries settings action");demand(refContains(click,"startActivity"),"compose click opens target activity");
  var frag=patched.get(fragment);boolean atFront=false;for(var m:frag.getMethods())if(refContains(m,"OPEN_DEBUG")&&refContains(m,"ArrayList;->remove")&&refContains(m,"ArrayList;->add")){atFront=true;System.out.println("settingsRows="+m);}demand(atFront,"visible settings rows move OpenDebug to front");
  var rowPatched=method(patched.get(row.getDefiningClass()),row.getName());demand(refContains(rowPatched,"Metra"),"visible row title set to Metra");
  var bind=method(patched.get(topSite.getMethod().getDefiningClass()),topSite.getMethod().getName());demand(refContains(bind,"ForYouTabSettingsShortcut;->onTopTab"),"For You bind hands native view/tag to shortcut");
  var registry=patched.get("Lapp/morphe/extension/tiktok/settings/L10nTranslations;");demand(refContains(method(registry,"of"),"JapaneseTranslations;->forLanguage"),"real output localization dispatcher invokes Japanese table");demand(patched.containsKey("Lapp/hiro/tiktokjapanese/runtime/JapaneseTranslations;"),"Japanese runtime present");
  System.out.println("NATIVE_SETTINGS_HOOK_AUDIT_PASSED");
 }
}
