import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Cache only reflection discovery in the exact retained .62 busy guard.
 * Every live Field.get/getBoolean, monitor, branch and exception handler stays
 * in place. Existing bounded ReflectionCache1945 owns metadata, never targets.
 */
public final class TransformExitBusy1963 {
 public static final String TYPE="Lcom/hiro/ulike/ExitBusy1921;";
 public static final String CAPTURE=TYPE+"->captureBusy(Z)Z";
 public static final String VALUE=TYPE+"->value(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;";
 public static final String CLASS_PIN="aef7ff489afcbcef70778f849b59d71c98247a53137fba50db6efd78e234b037";
 static final Map<String,String> METHOD_PINS=Map.of(
  CAPTURE,"dcb8f1175446fda5c4854054ec7d43ca561fd44ecd515915c1a145720cedd8f5",
  VALUE,"2654ff0a1648e464687759a803ca5cbb8c72a186b769362d1a142bea7404d42e");
 static final String CLASS_LOOKUP="Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;";
 static final String FIELD_LOOKUP="Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;";
 static final String CACHE="Lcom/hiro/ulike/ReflectionCache1945;";
 static final String TYPE_CACHE=CACHE+"->type(Ljava/lang/String;)Ljava/lang/Class;";
 static final String FIELD_CACHE=CACHE+"->declaredField(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;";
 private TransformExitBusy1963(){}
 static void require(boolean value,String why){MergePayloads.require(value,why);}
 static String ref(Instruction op){return op instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static Method replace(Method m,MethodImplementation body){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body);}
 static ClassDef members(ClassDef c,Collection<Method> methods){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);}
 static ClassDef transform(ClassDef before,boolean inverse)throws Exception {
  var methods=new ArrayList<Method>();int changed=0;
  for(Method method:before.getMethods()){
   String id=MergePayloads.id(method);if(!METHOD_PINS.containsKey(id)){methods.add(method);continue;}
   var body=new MutableMethodImplementation(method.getImplementation());int types=0,fields=0;
   for(int i=0;i<body.getInstructions().size();i++){
    Instruction op=body.getInstructions().get(i);String reference=ref(op);
    boolean type=reference.equals(inverse?TYPE_CACHE:CLASS_LOOKUP);
    boolean field=reference.equals(inverse?FIELD_CACHE:FIELD_LOOKUP);
    if(!type&&!field)continue;
    require(op instanceof FiveRegisterInstruction,"Pinned reflection discovery call format");
    var registers=(FiveRegisterInstruction)op;
    if(type){
     require(id.equals(VALUE)&&op.getOpcode()==Opcode.INVOKE_STATIC&&registers.getRegisterCount()==1,"Only one-string Class.forName uses the existing same-loader cache");
     var target=inverse?new ImmutableMethodReference("Ljava/lang/Class;","forName",List.of("Ljava/lang/String;"),"Ljava/lang/Class;"):
       new ImmutableMethodReference(CACHE,"type",List.of("Ljava/lang/String;"),"Ljava/lang/Class;");
     body.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,registers.getRegisterC(),0,0,0,0,target));types++;
    }else{
     require(op.getOpcode()==(inverse?Opcode.INVOKE_STATIC:Opcode.INVOKE_VIRTUAL)&&registers.getRegisterCount()==2,"Pinned exact-owner getDeclaredField discovery");
     var target=inverse?new ImmutableMethodReference("Ljava/lang/Class;","getDeclaredField",List.of("Ljava/lang/String;"),"Ljava/lang/reflect/Field;"):
       new ImmutableMethodReference(CACHE,"declaredField",List.of("Ljava/lang/Class;","Ljava/lang/String;"),"Ljava/lang/reflect/Field;");
     body.replaceInstruction(i,new BuilderInstruction35c(inverse?Opcode.INVOKE_VIRTUAL:Opcode.INVOKE_STATIC,
       2,registers.getRegisterC(),registers.getRegisterD(),0,0,0,target));fields++;
    }
   }
   require(types==(id.equals(VALUE)?1:0)&&fields==(id.equals(VALUE)?1:2),"Exact reflection discovery anchors only "+id);
   methods.add(replace(method,body));changed++;
  }
  require(changed==2,"Exactly two retained busy-guard methods contain discovery redirects");
  return members(before,methods);
 }
 public static ClassDef patch(ClassDef before)throws Exception {
  require(before!=null&&before.getType().equals(TYPE)&&MergePayloads.classHash(before).equals(CLASS_PIN),"Exact published .62 ExitBusy class required");
  var methods=MergePayloads.methods(List.of(before));for(var pin:METHOD_PINS.entrySet())require(methods.containsKey(pin.getKey())&&MergePayloads.hash(methods.get(pin.getKey())).equals(pin.getValue()),"Exact published busy-guard method pin "+pin.getKey());
  return transform(before,false);
 }
 public static void verify(ClassDef before,ClassDef after)throws Exception {
  ClassDef expected=patch(before);
  require(after!=null&&after.getType().equals(TYPE)&&MergePayloads.classHash(expected).equals(MergePayloads.classHash(after)),"Exact discovery-only busy-guard redirect and class preservation");
  require(MergePayloads.classHash(before).equals(MergePayloads.classHash(transform(after,true))),"Inverse preserves every instruction/register/branch/monitor/handler/live read");
  var old=MergePayloads.methods(List.of(before));var next=MergePayloads.methods(List.of(after));
  require(old.keySet().equals(next.keySet()),"ExitBusy method inventory preserved");
  var changed=new TreeSet<String>();for(var row:old.entrySet())if(!MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(next.get(row.getKey()))))changed.add(row.getKey());
  require(changed.equals(METHOD_PINS.keySet()),"Only two reviewed discovery-containing methods changed");
 }
}
