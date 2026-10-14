import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Exact .62 runtime repair: consume a black-bar double tap without switching.
 * The full pending cancellation and nonblack native gesture branch survive.
 * No camera, Surface, recording, focus or renderer API is added.
 */
public final class TransformBlackTap1963 {
 public static final String TYPE="Lcom/hiro/ulike/BlackTap1925;";
 public static final String METHOD=TYPE+"->doubleTap(Lcom/bytedance/corecamera/ui/view/GestureBgLayout;Landroid/view/MotionEvent;)Z";
 public static final String CLASS_PIN="f54fed39490cec43431891ab3dc875eca58ed75a585f43c8ed0712359253872a";
 public static final String METHOD_PIN="42ff5e49278e209783b3ff1ad8ce95d5fd6b2a8deff1a95a44e41f5eac3fbd05";
 static final String BLACK_ENTRY=TYPE+"->live(Lcom/bytedance/corecamera/ui/view/GestureBgLayout;)Z";
 static final String DELEGATE="Lcom/bytedance/corecamera/ui/view/GestureBgLayout$d;->onDoubleTap(Landroid/view/MotionEvent;)V";
 private TransformBlackTap1963(){}
 static void require(boolean b,String why){MergePayloads.require(b,why);}
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static Method replace(Method m,MethodImplementation body){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body);}
 static ClassDef members(ClassDef c,Collection<Method> methods){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);}
 public static ClassDef patch(ClassDef before)throws Exception {
  require(before!=null&&before.getType().equals(TYPE),"Only exact BlackTap1925 class is reviewed");
  require(MergePayloads.classHash(before).equals(CLASS_PIN),"Unreviewed .62 BlackTap class");
  var methods=new ArrayList<Method>();int changed=0;
  for(Method method:before.getMethods()){
   if(!MergePayloads.id(method).equals(METHOD)){methods.add(method);continue;}
   require(MergePayloads.hash(method).equals(METHOD_PIN),"Unreviewed .62 double-tap body");
   var body=new MutableMethodImplementation(method.getImplementation());
   require(body.getRegisterCount()==11&&body.getTryBlocks().isEmpty(),"Pinned gesture register/handler ABI");
   int entry=-1,delegate=0,trueConstant=0,switches=0;
   for(int i=0;i<body.getInstructions().size();i++){
    Instruction op=body.getInstructions().get(i);
    if(ref(op).equals(BLACK_ENTRY)){
     require(entry<0&&op.getOpcode()==Opcode.INVOKE_STATIC&&op instanceof FiveRegisterInstruction,"One native black branch entry");
     var call=(FiveRegisterInstruction)op;require(call.getRegisterCount()==1&&call.getRegisterC()==9,"Pinned black branch owner v9");entry=i;
    }
    if(ref(op).equals(DELEGATE))delegate++;
    if(ref(op).equals("Li/o/a/b1/a/g/y;->switchCamera()V"))switches++;
    if(op.getOpcode()==Opcode.CONST_4&&op instanceof OneRegisterInstruction r&&r.getRegisterA()==2
      &&op instanceof NarrowLiteralInstruction literal&&literal.getNarrowLiteral()==1)trueConstant++;
   }
   require(entry>0&&delegate==1&&trueConstant==1&&switches==1,"Pinned nonblack delegate and handled-true branch");
   require(ref(body.getInstructions().get(entry-3)).equals(DELEGATE),"Nonblack callback remains before black branch");
   require(body.getInstructions().get(entry-2).getOpcode()==Opcode.RETURN
     &&((OneRegisterInstruction)body.getInstructions().get(entry-2)).getRegisterA()==2
     &&body.getInstructions().get(entry-1).getOpcode()==Opcode.RETURN
     &&((OneRegisterInstruction)body.getInstructions().get(entry-1)).getRegisterA()==1,
     "Nonblack native handled/unhandled results retained");
   // This label is the sole entry to all black-only camera work. Replacing its
   // first instruction preserves existing prefix branch labels/offsets. Remove
   // the now-unreachable tail so no switch or reopen path remains in the body.
   body.replaceInstruction(entry,new BuilderInstruction11x(Opcode.RETURN,2));
   while(body.getInstructions().size()>entry+1)body.removeInstruction(body.getInstructions().size()-1);
   // The removed camera tail's debug labels would all relocate to the new
   // method end. Drop debug positions for this changed method only; executable
   // prefix, registers, fields and every other method/debug table stay exact.
   methods.add(replace(method,new ImmutableMethodImplementation(body.getRegisterCount(),
       body.getInstructions(),body.getTryBlocks(),List.of())));changed++;
  }
  require(changed==1,"Exactly one black double-tap runtime method changed");
  return members(before,methods);
 }
 public static void verify(ClassDef before,ClassDef after)throws Exception {
  ClassDef expected=patch(before);
  require(after!=null&&after.getType().equals(TYPE),"Serialized BlackTap class retained");
  require(MergePayloads.classHash(expected).equals(MergePayloads.classHash(after)),"Exact black-only branch repair; other fields/methods/metadata unchanged");
  var oldMethods=MergePayloads.methods(List.of(before));var newMethods=MergePayloads.methods(List.of(after));
  require(oldMethods.keySet().equals(newMethods.keySet()),"BlackTap method inventory preserved");
  var changed=new TreeSet<String>();for(var row:oldMethods.entrySet())if(!MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(newMethods.get(row.getKey()))))changed.add(row.getKey());
  require(changed.equals(Set.of(METHOD)),"Only reviewed doubleTap body changed");
  int delegates=0;for(Instruction op:newMethods.get(METHOD).getImplementation().getInstructions()){
   String reference=ref(op);
   require(!reference.equals("Li/o/a/b1/a/g/y;->switchCamera()V")&&!reference.equals(BLACK_ENTRY),"Black double-tap performs no camera or recovery checks");
   if(reference.equals(DELEGATE))delegates++;
  }
  require(delegates==1,"Nonblack native gesture delegate preserved exactly once");
 }
}
