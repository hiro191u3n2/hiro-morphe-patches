package app.hiro.berryshare.patches;
import app.hiro.berry.patches.BerryProfiles;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.immutable.*;
import java.util.*;
public class ShareAnchorTest {
 static MutableMethod method(String owner,String name,String returned,int registers,String...params){
  List<ImmutableMethodParameter> ps=new ArrayList<>();for(String p:params)ps.add(new ImmutableMethodParameter(p,Collections.emptySet(),null));
  return new MutableMethod(new ImmutableMethod(owner,name,ps,returned,1,Collections.emptySet(),Collections.emptySet(),new ImmutableMethodImplementation(registers,Collections.emptyList(),Collections.emptyList(),Collections.emptyList())));
 }
 static BerryProfiles.Profile profile()throws Exception{var ctor=BerryProfiles.Profile.class.getDeclaredConstructor(String.class,int.class,int.class,int.class);ctor.setAccessible(true);return ctor.newInstance("anchor-fixture",40,6,5);}
 static Map<String,MutableMethod> fixture(boolean badSelf,boolean badList){
  MutableMethod view=method("Lb03;","onViewCreated","V",40,"Landroid/view/View;","Landroid/os/Bundle;");
  InstructionExtensions.INSTANCE.addInstructions(view,0,
   "move-object/from16 v3, p0\nconst/16 p0, 0\n"+(badSelf?"const/4 v3, 0\n":"")+
   "iget-object v1, v3, Lb03;->M0:Lz32;\ninvoke-virtual {v0, v1}, Landroidx/recyclerview/widget/RecyclerView;->setAdapter(Lal4;)V\nreturn-void");
  MutableMethod list=method("Lj8;","q","Ljava/lang/Object;",40,"Ljava/lang/Object;");
  InstructionExtensions.INSTANCE.addInstructions(list,0,
   "check-cast v6, Lb03;\ninvoke-virtual/range {v20 .. v22}, Ltp0;->c(Landroid/content/Intent;Lyx0;)Ljava/lang/Object;\n"+
   "invoke-interface/range {v16 .. v16}, Ljava/util/List;->isEmpty()Z\n"+
   "iget-object v15, v6, Lb03;->M0:Lz32;\n"+
   (badList?"invoke-virtual {v15, v0}, Lfy;->O(Ljava/util/List;)V\n":"invoke-virtual/range {v15 .. v16}, Lfy;->O(Ljava/util/List;)V\n")+
   "return-object v5");
  Map<String,MutableMethod> m=new LinkedHashMap<>();m.put(BerryProfiles.SHARE_VIEW,view);m.put(BerryProfiles.SHARE_LIST,list);return m;
 }
 public static void main(String[]args)throws Exception{
  BerryProfiles.Profile p=profile();BrowserVisibilityPatch.Anchors a=BrowserVisibilityPatch.anchors(fixture(false,false),p);
  if(a.viewDialogRegister!=3||a.dialogRegister!=6||a.listRegister!=16||a.returnRegister!=5)throw new AssertionError();
  var call=BrowserVisibilityPatch.class.getDeclaredMethod("updateCall",BrowserVisibilityPatch.Anchors.class,int.class,BerryProfiles.Profile.class);call.setAccessible(true);
  String code=(String)call.invoke(null,a,40,p);if(!code.contains("move-object/from16 v1, v16")||!code.contains("invoke-static {v0, v1}"))throw new AssertionError(code);
  for(int n=0;n<2;n++){boolean rejected=false;try{BrowserVisibilityPatch.anchors(fixture(n==0,n==1),p);}catch(IllegalStateException expected){rejected=true;}if(!rejected)throw new AssertionError("accepted malformed dataflow "+n);}
  System.out.println("PASS: shifted p0 alias, high list register, range invokes, and two invalid dataflow rejections");
 }
}
