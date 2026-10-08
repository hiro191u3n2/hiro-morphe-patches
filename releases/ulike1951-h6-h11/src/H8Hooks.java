import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** H8's higher-level two-pass bridge. Both original method bodies are kept
 * byte-for-byte under package-access aliases, including their exceptions. */
public final class H8Hooks {
 static final String OWNER="Lcom/hiro/ulike/DetailSerial186;";
 static final String HELPER="Lcom/hiro/ulike/CorePixels1950;";
 static final String FILTER=OWNER+"->filter(Lcom/hiro/ulike/DetailPixels$Work;IIIIIIZZZ)V";
 static final String FILTER_ALIAS=OWNER+"->filterBeforeH8(Lcom/hiro/ulike/DetailPixels$Work;IIIIIIZZZ)V";
 static final String STAGE=OWNER+"->stage(Lcom/hiro/ulike/DetailSerial186$Context;III)V";
 static final String STAGE_ALIAS=OWNER+"->stageBeforeH8(Lcom/hiro/ulike/DetailSerial186$Context;III)V";
 static final Set<String> changedIds=Set.of(FILTER,STAGE),newIds=Set.of(FILTER_ALIAS,STAGE_ALIAS);
 public static Set<String> changedIds(){return changedIds;}
 public static Set<String> newIds(){return newIds;}
 static void require(boolean b,String why){if(!b)throw new IllegalStateException(why);}
 static List<String> arguments(Method m){var result=new ArrayList<String>();m.getParameters().forEach(p->result.add(p.getType()));return result;}
 static Method method(Method m,String name,int flags,MethodImplementation body){return new ImmutableMethod(m.getDefiningClass(),name,m.getParameters(),m.getReturnType(),flags,m.getAnnotations(),m.getHiddenApiRestrictions(),body);}
 static Method original(Method m){return method(m,m.getName().equals("filter")?"filterBeforeH8":"stageBeforeH8",m.getAccessFlags()&~AccessFlags.PRIVATE.getValue(),m.getImplementation());}
 static Method bridge(Method m){
  boolean filter=MergePayloads.id(m).equals(FILTER);
  require(filter||MergePayloads.id(m).equals(STAGE),"reviewed H8 method signature");
  require(m.getImplementation()!=null&&m.getImplementation().getRegisterCount()==(filter?27:4),"pinned H8 register shape");
  int count=filter?10:4;
  var body=new MutableMethodImplementation(count);
  body.addInstruction(new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,0,count,new ImmutableMethodReference(HELPER,filter?"filter":"stage",arguments(m),"V")));
  body.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
  return method(m,m.getName(),m.getAccessFlags(),body);
 }
 static Set<String> apply(Map<String,ClassDef> runtime,Map<String,ClassDef> original){
  ClassDef current=runtime.get(OWNER),old=original.get(OWNER);require(current!=null&&old!=null,"H8 target exists");
  var before=MergePayloads.methods(original.values());
  for(String id:changedIds)require(before.containsKey(id),"reviewed original exists: "+id);
  var methods=new ArrayList<Method>();Set<String>hits=new HashSet<>();
  for(Method m:current.getMethods()){
   String id=MergePayloads.id(m);
   require(!newIds.contains(id),"fresh H8 alias required");
   if(changedIds.contains(id)){methods.add(bridge(m));methods.add(original(m));hits.add(id);}
   else methods.add(m);
  }
  require(hits.equals(changedIds),"exactly two H8 interception points");
  runtime.put(OWNER,new ImmutableClassDef(current.getType(),current.getAccessFlags(),current.getSuperclass(),current.getInterfaces(),current.getSourceFile(),current.getAnnotations(),current.getFields(),methods));
  return changedIds;
 }
 static void verify(Map<String,ClassDef> before,Map<String,ClassDef> after){
  var a=MergePayloads.methods(before.values()),b=MergePayloads.methods(after.values());
  for(String id:changedIds){Method original=a.get(id),replacement=b.get(id);require(original!=null&&replacement!=null,"H8 method preserved");
   String alias=id.equals(FILTER)?FILTER_ALIAS:STAGE_ALIAS;
   require(MergePayloads.hash(bridge(original)).equals(MergePayloads.hash(replacement)),"H8 bridge exact "+id);
   require(MergePayloads.hash(original(original)).equals(MergePayloads.hash(b.get(alias))),"H8 original method exact "+alias);
  }
 }
}
