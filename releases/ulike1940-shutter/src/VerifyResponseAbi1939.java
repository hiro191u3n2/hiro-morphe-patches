import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** Real original/applied APK inspection, independent of host runtime fixtures. */
public final class VerifyResponseAbi1939 {
 static final String BUTTON=ResponseHooks1939.BUTTON,H=ResponseHooks1939.HELPER;
 static int checks;
 static void need(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static Map<String,ClassDef> load(String name)throws Exception{
  Map<String,ClassDef> result=new TreeMap<>();var zip=DexFileFactory.loadDexContainer(new File(name),Opcodes.forApi(26));
  for(String entry:zip.getDexEntryNames())for(ClassDef c:zip.getEntry(entry).getDexFile().getClasses())need(result.put(c.getType(),c)==null,"Duplicate class "+c.getType());
  return result;
 }
 static Method method(Map<String,ClassDef> all,String id){
  ClassDef c=all.get(id.substring(0,id.indexOf("->")));need(c!=null,"Method owner "+id);
  for(Method m:c.getMethods())if(m.toString().equals(id))return m;
  throw new AssertionError("Missing method "+id);
 }
 static Field field(Map<String,ClassDef> all,String owner,String name,String type){
  ClassDef c=all.get(owner);need(c!=null,"Field owner "+owner);
  for(Field f:c.getFields())if(f.getName().equals(name)){need(f.getType().equals(type),"Field type "+f);return f;}
  throw new AssertionError("Missing field "+owner+"->"+name);
 }
 static String ref(Instruction op){return op instanceof ReferenceInstruction?((ReferenceInstruction)op).getReference().toString():"";}
 static int count(Method m,String id){int n=0;for(Instruction op:m.getImplementation().getInstructions())if(ref(op).equals(id))n++;return n;}
 static void publicMethod(Map<String,ClassDef> all,String id){Method m=method(all,id);need((m.getAccessFlags()&9)==1,"Public instance ABI "+id);}
 static void verify(Map<String,ClassDef> all){
  for(String name:List.of("q","f0"))field(all,BUTTON,name,"I");field(all,BUTTON,"J","Z");
  field(all,BUTTON,"L","J");field(all,BUTTON,"e0","Z");
  field(all,BUTTON,"N",BUTTON.substring(0,BUTTON.length()-1)+"$l;");
  String owner="Li/o/a/b1/a/w/b/a/b;",normal="Li/o/a/b1/a/w/b/a/b$a;",assist="Li/o/a/b1/a/w/b/c/q$b;";
  field(all,normal,"a",owner);field(all,assist,"a","Li/o/a/b1/a/w/b/c/q;");
  publicMethod(all,owner+"->P()Li/o/a/b1/a/g/e0;");
  need(owner.equals(all.get("Li/o/a/b1/a/w/b/c/q;").getSuperclass()),"Assist controller inherits same camera owner");
  publicMethod(all,"Li/o/a/b1/a/g/y;->D1()I");
  Method touch=method(all,ResponseHooks1939.TOUCH),down=method(all,ResponseHooks1939.DOWN);
  need(touch.getImplementation().getRegisterCount()==6&&down.getImplementation().getRegisterCount()==7,"Native register counts preserved");
  need(count(touch,"Landroid/view/View;->isEnabled()Z")==1,"Native enabled gate retained");
  need(count(touch,"Li/o/a/b1/a/t/f/h;->d()Z")==1,"Native permission gate retained");
  need(count(touch,BUTTON+"->K(FF)Z")==1,"Native touch target gate retained");
  need(count(touch,"Li/p/a/t/m;->b(J)Z")==1,"Original global duplicate guard retained");
  need(count(down,"Lcom/light/beauty/mc/preview/shutter/module/ShutterButton$l;->a()Z")==1,"Native listener rejection retained");
  need(count(down,"Lcom/light/beauty/mc/preview/shutter/module/ShutterButton$l;->b()V")==1,"Exactly one original still callback retained");
  need(count(down,"Lcom/light/beauty/mc/preview/shutter/module/ShutterButton$l;->d()V")==1,"Original video callback retained");
  Method up=method(all,BUTTON+"->h()V"),hold=method(all,"Lcom/light/beauty/mc/preview/shutter/module/ShutterButton$e;->run()V");
  need(count(up,"Lcom/light/beauty/mc/preview/shutter/module/ShutterButton$l;->b()V")==1,"Exactly one original photo-on-up call retained");
  boolean delay=false;for(Instruction op:down.getImplementation().getInstructions())if(op instanceof WideLiteralInstruction&&((WideLiteralInstruction)op).getWideLiteral()==300)delay=true;
  need(delay,"Native 300ms video hold remains");
  need(count(hold,"Lcom/light/beauty/mc/preview/shutter/module/ShutterButton$l;->d()V")==1,"Original hold runnable still owns video start");
  need(count(method(all,normal+"->b()V"),"Li/o/a/b1/a/g/e0;->X(IZ)Z")==1,"Main button invokes real original camera capture");
  need(count(method(all,assist+"->b()V"),"Li/o/a/b1/a/g/e0;->I()Z")==1,"Assist button invokes real original camera capture");
  boolean applied=all.containsKey(H);
  if(applied){
   need(count(touch,H+"->touchWindow(Ljava/lang/Object;)J")==1,"Touch response hook applied once");
   need(count(down,H+"->photoWindow(Ljava/lang/Object;)J")==1,"Photo response hook applied once");
   need(count(down,H+"->postHold(Landroid/view/View;Ljava/lang/Runnable;J)Z")==1,"Existing video hold scheduling bound to current press");
   Method ready=method(all,H+"->readyPhoto(Ljava/lang/Object;)Z");
   need(count(ready,"Lcom/hiro/ulike/AsyncSave1935;->readiness(Ljava/lang/Object;)I")==1,"Response observes actual native and save readiness");
   for(ClassDef c:all.values())if(c.getType().startsWith(H.substring(0,H.length()-1)))for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction op:m.getImplementation().getInstructions()){
    String target=ref(op);
    need(!target.contains("->post(")&&!target.contains("Ljava/lang/Thread;")&&!target.contains("Landroid/os/Handler;"),"Response adds no photo scheduler or worker");
    if(target.contains("->postDelayed("))need(m.getDefiningClass().equals(H)&&m.getName().equals("postHold")&&target.equals("Landroid/view/View;->postDelayed(Ljava/lang/Runnable;J)Z"),"Only existing native video hold may use View delayed callback");
    need(!target.contains("->X(IZ)")&&!target.contains("->I()Z")&&!target.contains("->capture(")&&!target.contains("->captureBurst(")&&!target.contains("->compress("),"Response cannot initiate capture or alter quality");
   }
  }else{
   for(String id:ResponseHooks1939.NATIVE){Method m=method(all,id);ResponseHooks1939.verify(m,ResponseHooks1939.repair(m));}
  }
  System.out.println("PASS response1939 native ABI checks="+checks+" applied="+applied+"; no device execution");
 }
 public static void main(String[] args)throws Exception{
  if(args.length<1||args.length>2)throw new IllegalArgumentException("STOCK_OR_APPLIED_APK [APPLIED_APK]");
  for(String arg:args){checks=0;Map<String,ClassDef> all=load(arg);checks=0;verify(all);}
 }
}
