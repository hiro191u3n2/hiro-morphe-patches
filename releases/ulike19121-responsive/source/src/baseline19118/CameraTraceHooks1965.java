import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.iface.value.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.immutable.value.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Pinned prefix-only observers. Undo proves all original control flow remains. */
public final class CameraTraceHooks1965 {
 static final String P="Lcom/hiro/ulike/", O=P+"OpticalZoom;", T=P+"ProcessingTiming1947;", C=P+"CameraSession1965;", D=P+"CameraTrace1965;";
 static final String OLD_TITLE="直近の撮影・工程別処理時間（タップで更新）", TITLE="直近の撮影・工程別処理時間（更新・診断記録）";
 static final Map<String,String> PINS=new TreeMap<>();
 static {
  PINS.put(O+"->background(Ljava/lang/Object;)V","b724d16e62eda232b065309d4af83ad4fd5f8e9f3fa8a9a70b1eb9c815ad7b18");
  PINS.put(O+"->cameraError(Ljava/lang/Object;ILjava/lang/String;)V","16efb86bbf0b4f1893b2b62e28f7b06ee44d42c8ab93cc172e7dea4cb422d3d2");
  PINS.put(O+"->closing(Ljava/lang/Object;)V","bd86f82eb1033cae8457707fd26de823664ce34eb98fe44592b83f28b115d98a");
  PINS.put(O+"->foreground(Ljava/lang/Object;)V","cb343d1a9cae9b7fda342ba34e685a672bfe4de417cd258f03eeb57da416825a");
  PINS.put(O+"->init(Landroid/content/Context;)V","8f00f7e9391231532a5b0975d3f730faec8f5f7ed6e8158f33dc75007d8f817b");
  PINS.put(O+"->opened(Ljava/lang/Object;Ljava/lang/Object;)V","80db45129b7b390c55283c6edd85e855575af72c29befaa10a791149ea0c4255");
  PINS.put(O+"->prepared(Ljava/lang/Object;I)V","9162a7cf4251aafe1067451d44a6c83b6f7a7bb9aaf54862f7a184cf621db143");
  PINS.put(O+"->select(I)V","571e28f33918dcc5ef18507c66311375073e32258cdff9dc7484b6fdf89e0c29");
  PINS.put(O+"->track(Ljava/lang/Object;Landroid/content/Context;)V","e69acb30a0bb7ca84d629bdf2642dbeda29e641a062822dbc89252e1ffb3bd6e");
  PINS.put(T+"->init(Landroid/content/Context;)V","0a913da51e8a5fd3ae61022eb48cea34924c6748027216913ef941d7d4674591");
  PINS.put(T+"->install(Landroid/preference/PreferenceActivity;)V","1042cea20d62b5f14e376481ff26b4f59d665e56398ef5a4ed094f7205ebe5d6");
  PINS.put(P+"ProcessingTiming1947$1;->onPreferenceClick(Landroid/preference/Preference;)Z","d2f63de4c563ccc5970142846070ca27a661d4618de191651e3975429cfe4f9d");
 }
 static boolean owns(String type){return type.equals(O)||type.equals(T)||type.equals(P+"ProcessingTiming1947$1;");}
 static boolean prefix(Method m){return PINS.containsKey(MergePayloads.id(m))&&!m.getName().equals("install");}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static String version(String value,boolean inverse){String from=inverse?"1.9.65":"1.9.64",to=inverse?"1.9.64":"1.9.65";return value.equals(from)?to:value.startsWith("ULike v"+from)?"ULike v"+to+value.substring(("ULike v"+from).length()):value;}
 static ClassDef edit(ClassDef c,boolean inverse)throws Exception {
  MergePayloads.require(owns(c.getType()),"Reviewed observer class");var methods=new ArrayList<Method>();
  for(Method m:c.getMethods()){
   String id=MergePayloads.id(m);if(!inverse&&PINS.containsKey(id))MergePayloads.require(MergePayloads.hash(m).equals(PINS.get(id)),"Exact published .64 observation pin "+id);
   if(m.getImplementation()==null){methods.add(m);continue;}
   var b=new MutableMethodImplementation(m.getImplementation());
   if(prefix(m)){
    if(inverse){
     Instruction first=b.getInstructions().get(0);MergePayloads.require(first.getOpcode()==Opcode.INVOKE_STATIC_RANGE&&first instanceof ReferenceInstruction,"Observation prefix present");
     var params=new ArrayList<String>();for(CharSequence type:m.getParameterTypes())params.add(type.toString());String name=m.getName().equals("onPreferenceClick")?"open":m.getName();String owner=name.equals("open")?D:C;
     String ref=((ReferenceInstruction)first).getReference().toString();MergePayloads.require(ref.equals(new ImmutableMethodReference(owner,name,params,"V").toString()),"Exact scalar observer prefix");
     RegisterRangeInstruction call=(RegisterRangeInstruction)first;MergePayloads.require(call.getRegisterCount()==params.size()&&call.getStartRegister()==b.getRegisterCount()-params.size(),"Exact observer argument range");b.removeInstruction(0);
    }else{
     var params=new ArrayList<String>();for(CharSequence type:m.getParameterTypes())params.add(type.toString());
     for(String type:params)MergePayloads.require(!type.equals("J")&&!type.equals("D"),"Reviewed narrow observer arguments");
     String name=m.getName().equals("onPreferenceClick")?"open":m.getName();String owner=name.equals("open")?D:C;
     int count=params.size(),first=b.getRegisterCount()-count;
     b.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,first,count,new ImmutableMethodReference(owner,name,params,"V")));
    }
   }
   if(c.getType().equals(T))for(int i=0;i<b.getInstructions().size();i++){
    Instruction x=b.getInstructions().get(i);
    if(x instanceof ReferenceInstruction&&((ReferenceInstruction)x).getReference() instanceof StringReference){
     String value=((StringReference)((ReferenceInstruction)x).getReference()).getString(),updated=version(value,inverse);
     if(m.getName().equals("install")&&value.equals(inverse?TITLE:OLD_TITLE))updated=inverse?OLD_TITLE:TITLE;
     if(!value.equals(updated)){MergePayloads.require(x.getOpcode()==Opcode.CONST_STRING,"Pinned timing string opcode");b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(updated)));}
    }
   }
   methods.add(replace(m,b));
  }
  var fields=new ArrayList<Field>();
  for(Field f:c.getFields()){
   EncodedValue value=f.getInitialValue();if(c.getType().equals(T)&&f.getName().equals("VERSION")){
    MergePayloads.require(value instanceof StringEncodedValue&&((StringEncodedValue)value).getValue().equals(inverse?"1.9.65":"1.9.64"),"Timing version field pinned");
    value=new ImmutableStringEncodedValue(inverse?"1.9.64":"1.9.65");
    fields.add(new ImmutableField(f.getDefiningClass(),f.getName(),f.getType(),f.getAccessFlags(),value,f.getAnnotations(),f.getHiddenApiRestrictions()));
   }else fields.add(f);
  }
  return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),fields,methods);
 }
 static ClassDef patch(ClassDef c)throws Exception{ClassDef result=edit(c,false);verify(c,result);return result;}
 static String canonicalHash(ClassDef c)throws Exception{return MergePayloads.classHash(ImmutableClassDef.of(c));}
 static void verify(ClassDef before,ClassDef after)throws Exception{
  // Canonicalize annotation element ordering on both sides. Immutable wrappers
  // may reorder serialized annotation pools without changing values or code.
  MergePayloads.require(canonicalHash(before).equals(canonicalHash(edit(after,true))),"Inverse proves retained observer class/fields/code unchanged "+before.getType());
 }
}
