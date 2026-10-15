import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;
import com.android.tools.smali.dexlib2.immutable.value.ImmutableStringEncodedValue;
public final class RuntimeVersion19103Test {
 static final String V="1.9.106", P="Lcom/hiro/ulike/"; static int assertions;
 static ClassDef cls(String owner,String version,String method,String literal){
  List<ImmutableField> fields=version==null?List.of():List.of(new ImmutableField(owner,"VERSION","Ljava/lang/String;",0x19,new ImmutableStringEncodedValue(version),Set.of(),Set.of()));
  List<ImmutableMethod> methods=literal==null?List.of():List.of(new ImmutableMethod(owner,method,List.of(),"V",0x9,Set.of(),Set.of(),new ImmutableMethodImplementation(1,List.of(new ImmutableInstruction21c(Opcode.CONST_STRING,0,new ImmutableStringReference(literal)),new ImmutableInstruction10x(Opcode.RETURN_VOID)),List.of(),List.of())));
  return new ImmutableClassDef(owner,0x11,"Ljava/lang/Object;",List.of(),null,Set.of(),fields,methods);
 }
 static Map<String,ClassDef> base(){Map<String,ClassDef> values=new HashMap<>();for(String type:VerifyRuntimeVersion19101.ROOTS)values.put(type,cls(type,V,"unused",null));return values;}
 static void check(Map<String,ClassDef> values,boolean accepted){try{VerifyRuntimeVersion19101.verify(values,V);if(!accepted)throw new AssertionError("stale diagnostic accepted");}catch(IllegalStateException|IllegalArgumentException error){if(accepted)throw error;}assertions++;}
 public static void main(String[] a){
  check(base(),true);
  for(String root:VerifyRuntimeVersion19101.ROOTS){
   Map<String,ClassDef> values=base();values.remove(root);check(values,false);
   for(String bad:List.of("1.9.100","1.9.101","1.9.102","1.9.103","1.9.104","1.9.105")){values=base();values.put(root,cls(root,bad,"unused",null));check(values,false);}
   for(String suffix:List.of("","$Engine","$Storage","$CopyLogClick1993"))for(String literal:List.of("1.9.101","ULike v1.9.101 /","ULike 撮影ログ v1.9.101","version=1.9.101 api=36","1.9.103","ULike v1.9.103 /","ULike 撮影ログ v1.9.103","version=1.9.103 api=36")){
    values=base();String owner=root.substring(0,root.length()-1)+suffix+";";values.put(owner,cls(owner,suffix.isEmpty()?V:null,"offer",literal));check(values,false);
   }
  }
  Map<String,ClassDef> values=base();values.put(VerifyRuntimeVersion19101.OWNER,cls(VerifyRuntimeVersion19101.OWNER,V,"init","1.9.100"));check(values,true);
  values=base();values.put(VerifyRuntimeVersion19101.OWNER,cls(VerifyRuntimeVersion19101.OWNER,V,"init","ULike v1.9.100 /"));check(values,false);
  for(String root:VerifyRuntimeVersion19101.ROOTS){values=base();String owner=root.substring(0,root.length()-1)+"$Storage;";values.put(owner,cls(owner,null,"export","version="+V+" api=36"));check(values,true);}
  System.out.println("PASS diagnostic-version-family assertions="+assertions);
 }
}
