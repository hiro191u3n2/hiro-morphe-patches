import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;

/** Audit actual foreground lifecycle hooks against their original method
 * bodies, including moved tab receivers and incoming branch/exception labels.
 * Compile alongside ZoomApplicationAudit. Args: original.apk output.apk report.json
 */
public final class ForegroundApplicationAudit {
    static final String WEB="Landroid/webkit/WebView;",BUNDLE="Landroid/os/Bundle;",PRIORITY="Lhiro/berry/TabPriority;";
    static final String RESTORE=PRIORITY+"->restorePending("+WEB+BUNDLE+")Z";
    static int checks;
    static void need(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    static String d(Method m){return ZoomApplicationAudit.desc(m);}
    static String r(Instruction i){return ZoomApplicationAudit.ref(i);}
    static List<Instruction> ins(Method m){return ZoomApplicationAudit.ins(m);}
    static int[] args(Instruction i){return ZoomApplicationAudit.args(i);}
    static int target(List<Instruction> in,int i){return ZoomApplicationAudit.target(in,i);}
    static int unique(List<Instruction> in,String ref){int found=-1;for(int j=0;j<in.size();j++)if(r(in.get(j)).equals(ref)){need(found<0,"duplicate reference "+ref);found=j;}need(found>=0,"missing reference "+ref);return found;}
    static Map<String,Method> methods(Map<String,ClassDef> cs){Map<String,Method> out=new TreeMap<>();for(ClassDef c:cs.values())for(Method m:c.getMethods())out.put(d(m),m);return out;}
    static Method hook(Map<String,ClassDef> old,Map<String,ClassDef> fresh,String ref){Method found=null;for(ClassDef c:fresh.values())if(old.containsKey(c.getType()))for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())if(r(i).equals(ref)){need(found==null,"duplicate app hook "+ref);found=m;}need(found!=null,"missing app hook "+ref);return found;}
    static String instruction(List<Instruction> in,int j,int[] normalized){String s=ZoomApplicationAudit.instruction(in,j,0);if(in.get(j) instanceof OffsetInstruction){int p=s.lastIndexOf("/target=");s=s.substring(0,p)+"/target="+normalized[target(in,j)];}return s;}
    static List<String> tries(Method m,int[] normalized){
        List<String> out=new ArrayList<>();Map<Integer,Integer> address=ZoomApplicationAudit.addresses(ins(m));
        for(TryBlock<? extends ExceptionHandler> b:m.getImplementation().getTryBlocks()){
            int start=normalized[address.get(b.getStartCodeAddress())],end=normalized[address.get(b.getStartCodeAddress()+b.getCodeUnitCount())];
            StringBuilder s=new StringBuilder().append(start).append(':').append(end);
            for(ExceptionHandler h:b.getExceptionHandlers())s.append('/').append(h.getExceptionType()).append('@').append(normalized[address.get(h.getHandlerCodeAddress())]);out.add(s.toString());
        }
        return out;
    }
    static void unchanged(Method old,Method fresh,Set<Integer> removed,Map<Integer,Instruction> replacements){
        List<Instruction> a=ins(old),b=ins(fresh);int[] am=new int[a.size()+1],bm=new int[b.size()+1];for(int j=0;j<am.length;j++)am[j]=j;
        int kept=0;for(int j=0;j<b.size();j++){bm[j]=kept;if(!removed.contains(j))kept++;}bm[b.size()]=kept;need(kept==a.size(),"instruction count differs "+d(old));
        need(old.getImplementation().getRegisterCount()==fresh.getImplementation().getRegisterCount(),"register count differs "+d(old));
        ImmutableMethod clean=new ImmutableMethod(fresh.getDefiningClass(),fresh.getName(),fresh.getParameters(),fresh.getReturnType(),fresh.getAccessFlags(),fresh.getAnnotations(),fresh.getHiddenApiRestrictions(),old.getImplementation());
        ClassDef ca=new ImmutableClassDef("Laudit;",1,"Ljava/lang/Object;",Collections.emptyList(),null,Collections.emptySet(),Collections.emptyList(),Collections.singleton(old));
        ClassDef cb=new ImmutableClassDef("Laudit;",1,"Ljava/lang/Object;",Collections.emptyList(),null,Collections.emptySet(),Collections.emptyList(),Collections.singleton(clean));
        try{need(Arrays.equals(ZoomApplicationAudit.canonical(ca),ZoomApplicationAudit.canonical(cb)),"method metadata changed "+d(old));}catch(Exception e){throw new IllegalStateException(e);}
        for(int j=0,k=0;j<b.size();j++)if(!removed.contains(j)){
            String actual=replacements.containsKey(j)?ZoomApplicationAudit.instruction(Collections.singletonList(replacements.get(j)),0,0):instruction(b,j,bm);
            need(instruction(a,k,am).equals(actual),"original instruction/flow changed "+d(old)+" #"+k);k++;
        }
        need(tries(old,am).equals(tries(fresh,bm)),"exception regions changed "+d(old));
    }
    static void existingField(Map<String,ClassDef> classes,FieldReference ref){ClassDef c=classes.get(ref.getDefiningClass());need(c!=null,"unknown field owner");int n=0;for(Field f:c.getFields())if(ZoomApplicationAudit.F.getFieldDescriptor(f).equals(ZoomApplicationAudit.F.getFieldDescriptor(ref))){need((f.getAccessFlags()&9)==1,"nonpublic/static injected field");n++;}need(n==1,"unknown injected field");}
    static String restore(Map<String,ClassDef> old,Map<String,ClassDef> fresh,Map<String,Method> before){
        Method out=hook(old,fresh,RESTORE),input=before.get(d(out));need(input!=null,"new restore target");List<Instruction> in=ins(out);int call=unique(in,RESTORE),prefix=call+5;
        int regs=input.getImplementation().getRegisterCount(),self=regs-2,bundle=regs-1;need(regs>=3,"restore lacks scratch local");
        Map<Integer,String> types=new HashMap<>();types.put(self,input.getDefiningClass());types.put(bundle,BUNDLE);List<String> fields=new ArrayList<>();
        for(int j=0;j<call;j++){
            Instruction x=in.get(j);need(x instanceof TwoRegisterInstruction,"unsupported receiver instruction");TwoRegisterInstruction v=(TwoRegisterInstruction)x;
            if(x.getOpcode()==Opcode.MOVE_OBJECT_FROM16){need(types.containsKey(v.getRegisterB()),"unknown copied receiver");types.put(v.getRegisterA(),types.get(v.getRegisterB()));}
            else{need(x.getOpcode()==Opcode.IGET_OBJECT,"receiver does more than field reads");FieldReference f=(FieldReference)((ReferenceInstruction)x).getReference();existingField(old,f);need(f.getDefiningClass().equals(types.get(v.getRegisterB())),"invalid injected iget receiver type");types.put(v.getRegisterA(),f.getType());fields.add(ZoomApplicationAudit.F.getFieldDescriptor(f));}
        }
        need(fields.size()==1||fields.size()==2,"unexpected restore field chain length");
        int[] passed=args(in.get(call));need(passed.length==2,"restore argument count");ClassDef web=old.get(types.get(passed[0]));need(web!=null&&WEB.equals(web.getSuperclass()),"restore receiver is not original WebView subclass");need(BUNDLE.equals(types.get(passed[1])),"restore bundle argument changed");
        List<Instruction> original=ins(input);for(String field:fields)unique(original,field);
        need(in.get(call).getOpcode()==Opcode.INVOKE_STATIC||in.get(call).getOpcode()==Opcode.INVOKE_STATIC_RANGE,"restore invoke not static");
        need(in.get(call+1).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)in.get(call+1)).getRegisterA()==0,"restore result register");
        need(in.get(call+2).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)in.get(call+2)).getRegisterA()==0&&target(in,call+2)==prefix,"restore original path target");
        need(in.get(call+3).getOpcode()==Opcode.CONST_4&&((OneRegisterInstruction)in.get(call+3)).getRegisterA()==0&&((NarrowLiteralInstruction)in.get(call+3)).getNarrowLiteral()==1,"restore success constant");
        need(in.get(call+4).getOpcode()==Opcode.RETURN&&((OneRegisterInstruction)in.get(call+4)).getRegisterA()==0,"restore early return");
        Set<Integer> removed=new HashSet<>();for(int j=0;j<prefix;j++)removed.add(j);unchanged(input,out,removed,Collections.emptyMap());
        System.out.println("PASS restore "+d(out)+" fields="+fields+" args="+Arrays.toString(passed));return d(out)+" fields="+fields;
    }
    static void noBypass(List<Instruction> in,Method method,int inserted,int anchor){
        for(int j=0;j<in.size();j++)if(in.get(j) instanceof OffsetInstruction)need(target(in,j)!=anchor,"incoming branch skips inserted before-hook "+d(method)+" #"+j);
        Map<Integer,Integer> addresses=ZoomApplicationAudit.addresses(in);for(TryBlock<? extends ExceptionHandler> b:method.getImplementation().getTryBlocks())for(ExceptionHandler h:b.getExceptionHandlers())need(addresses.get(h.getHandlerCodeAddress())!=anchor,"exception target skips inserted before-hook");
        need(inserted+1==anchor,"before-hook not immediately before anchor");
    }
    public static void main(String[] a)throws Exception{
        Map<String,ClassDef> old=ZoomApplicationAudit.load(a[0]),fresh=ZoomApplicationAudit.load(a[1]);Map<String,Method> before=methods(old);
        String restore=restore(old,fresh,before);List<String> audited=new ArrayList<>();
        String[][] specs={{"resumed","onResume","()V","after"},{"paused","onPause","()V","after"},{"stopped","stopLoading","()V","before"},{"destroyed","destroy","()V","before"},{"savePending","saveState","("+BUNDLE+")Landroid/webkit/WebBackForwardList;","before"},{"load","loadUrl","(Ljava/lang/String;)V","replace"}};
        for(String[] s:specs){
            String hookRef=PRIORITY+"->"+s[0]+"("+WEB+(s[0].equals("savePending")?BUNDLE:s[0].equals("load")?"Ljava/lang/String;":"")+")V",anchor=WEB+"->"+s[1]+s[2];
            Method out=hook(old,fresh,hookRef),input=before.get(d(out));need(input!=null,"missing original lifecycle target");List<Instruction> orig=ins(input),in=ins(out);int originalCall=unique(orig,anchor),added=unique(in,hookRef);need(Arrays.equals(args(orig.get(originalCall)),args(in.get(added))),"hook receiver/arguments differ "+s[0]);need(in.get(added).getOpcode()==Opcode.INVOKE_STATIC||in.get(added).getOpcode()==Opcode.INVOKE_STATIC_RANGE,"hook not static");
            if(s[3].equals("replace")){need(added==originalCall,"load replacement moved");unchanged(input,out,Collections.emptySet(),Collections.singletonMap(added,orig.get(originalCall)));}
            else{int platform=unique(in,anchor);if(s[3].equals("after"))need(platform+1==added,"after-hook moved");else noBypass(in,out,added,platform);unchanged(input,out,Collections.singleton(added),Collections.emptyMap());}
            audited.add(d(out)+" -> "+s[0]);System.out.println("PASS lifecycle "+d(out)+" -> "+s[0]);
        }
        String report="{\"result\":\"PASS\",\"checks\":"+checks+",\"restore\":\""+restore+"\",\"lifecycle_hooks\":\""+audited+"\",\"original_instructions_control_flow_try_regions_preserved\":true,\"before_hook_incoming_edges_checked\":true}\n";
        Files.writeString(Path.of(a[2]),report);System.out.print(report);
    }
}
