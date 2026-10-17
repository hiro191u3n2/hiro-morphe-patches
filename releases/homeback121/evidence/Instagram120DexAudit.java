import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import app.hiro.oneback.patches.OneBackPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Applies only the new native main-dispatch hook in memory to the supplied APK.
 * The input APK remains byte-for-byte unchanged; no device result is inferred. */
public final class Instagram120DexAudit {
    static int assertions;
    static void check(boolean value,String message) {assertions++;if(!value)throw new AssertionError(message);}
    static String sha(byte[] data)throws Exception {byte[] digest=MessageDigest.getInstance("SHA-256").digest(data);StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format("%02x",b&255));return out.toString();}
    static String sha(Path path)throws Exception {try(InputStream in=Files.newInputStream(path)){MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] b=new byte[1024*1024];for(int n;(n=in.read(b))>=0;)d.update(b,0,n);StringBuilder out=new StringBuilder();for(byte v:d.digest())out.append(String.format("%02x",v&255));return out.toString();}}
    static String resourceSha(String path)throws Exception {try(InputStream in=OneBackPatch.class.getResourceAsStream(path)){if(in==null)throw new AssertionError("missing compiled patch resource");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];for(int n;(n=in.read(b))>=0;)out.write(b,0,n);return sha(out.toByteArray());}}
    static Method method(ClassDef c,OneBackPatch.InstagramMainBoundary b) {for(Method m:c.getMethods())if(m.getName().equals(b.methodName)&&m.getReturnType().equals("V")&&m.getParameterTypes().equals(Collections.singletonList(b.parameterType)))return m;throw new AssertionError("missing main dispatcher");}
    static int target(List<Instruction> ins,int i) {return HomeBackAudit.addr(ins,i)+((OffsetInstruction)ins.get(i)).getCodeOffset();}
    static ImmutableMethod methodWithCode(Method before,MethodImplementation code) {return new ImmutableMethod(before.getDefiningClass(),before.getName(),before.getParameters(),before.getReturnType(),before.getAccessFlags(),before.getAnnotations(),before.getHiddenApiRestrictions(),code);}
    static BuilderInstruction changedCall(Instruction old,int receiver,String owner) {
        MethodReference ref=HomeBackAudit.ref(old);FiveRegisterInstruction r=(FiveRegisterInstruction)old;
        return new BuilderInstruction35c(old.getOpcode(),r.getRegisterCount(),receiver,r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG(),new ImmutableMethodReference(owner==null?ref.getDefiningClass():owner,ref.getName(),ref.getParameterTypes(),ref.getReturnType()));
    }
    static String quote(String s) {return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
    static void append(StringBuilder json,String key,String value) {if(json.length()>2)json.append(",\n");json.append("  ").append(quote(key)).append(": ").append(value);}
    static Field nativeField(ClassDef owner,String name,String type) {
        if(owner!=null)for(Field f:owner.getFields())if(name.equals(f.getName())&&type.equals(f.getType())&&(f.getAccessFlags()&9)==1)return f;
        return null;
    }
    static void nativeLayers(Map<String,ClassDef> classes) {
        ClassDef reels=classes.get("LX/8Zv;"),popup=classes.get("LX/21x;"),layer=classes.get("LX/16z;");
        check(reels!=null&&popup!=null&&layer!=null,"exact native Reels guard types exist");
        check(nativeField(reels,"A1Y","LX/21x;")!=null&&nativeField(reels,"A0L","LX/0wS;")!=null,"read-only adapter fields retain exact public instance types");
        check(nativeField(popup,"A05","Z")!=null&&nativeField(layer,"A15","Z")!=null,"read-only adapter native activity flags are public instance booleans");
        check((layer.getAccessFlags()&17)==17&&"LX/0wS;".equals(layer.getSuperclass()),"native layer is exact public final subclass of field type");
        check(OneBackPatch.instagramReelsBoundary(reels,classes)>=0,"existing safe native Reels boundary still matches");
        Method back=null;for(Method m:reels.getMethods())if("onBackPressed".equals(m.getName())&&m.getParameterTypes().isEmpty()&&"Z".equals(m.getReturnType()))back=m;
        check(back!=null,"native Reels Back handler found");List<Instruction> code=HomeBackAudit.instructions(back);
        String[] fields={"LX/8Zv;->A1Y:LX/21x;","LX/21x;->A05:Z","LX/8Zv;->A0L:LX/0wS;","LX/16z;->A15:Z"};int[] positions={0,3,7,11};
        for(int i=0;i<positions.length;i++)check(fields[i].equals(((ReferenceInstruction)code.get(positions[i])).getReference().toString()),"read-only adapter exactly mirrors original native guard field #"+i);
        check("LX/16z;".equals(((TypeReference)((ReferenceInstruction)code.get(10)).getReference()).getType()),"native layer guard uses exact expected cast");
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("APK REPORT_JSON LOG_TXT PATCH_SOURCE AUDIT_SOURCE");
        Path apk=Paths.get(args[0]);String inputHash=sha(apk);
        Map<String,ClassDef> classes=HomeBackAudit.read(args[0]);nativeLayers(classes);int matches=0,unchanged=0,negative=0,nativeCount=0;
        String matchedClass="",matchedMethod="";OneBackPatch.InstagramMainBoundary boundary=null;
        for(ClassDef c:classes.values()) {
            OneBackPatch.InstagramMainBoundary b=OneBackPatch.instagramMainReelsBoundary(c,classes);
            if(b==null)continue;matches++;boundary=b;matchedClass=c.getType();matchedMethod=b.methodName+"("+b.parameterType+")V";
            Method before=method(c,b);List<Instruction> old=HomeBackAudit.instructions(before);nativeCount=old.size();
            check(b.instruction==62&&b.scratch==0,"exact actual APK boundary and dead scratch register");
            check(Arrays.equals(b.incomingBranches,new int[]{37,48}),"all native branches entering dispatcher identified");
            check(HomeBackAudit.addr(old,b.instruction)==126,"actual pre-AndroidX PC is 126");
            MutableClass out=new MutableClass(c);Map<String,Integer> counts=new LinkedHashMap<String,Integer>();
            OneBackPatch.instagramMainReelsHook(out,b,counts);
            Method after=method(out,b);List<Instruction> ins=HomeBackAudit.instructions(after);
            check(Integer.valueOf(1).equals(counts.get("instagram_main_reels_dispatch")),"one targeted insertion");
            check(ins.size()==old.size()+4&&after.getImplementation().getRegisterCount()==before.getImplementation().getRegisterCount(),"four instructions and no register shift");
            MethodReference call=HomeBackAudit.ref(ins.get(62));
            check(call!=null&&OneBackPatch.RUNTIME.equals(call.getDefiningClass())&&"tryInstagramMainReelsBack".equals(call.getName())&&call.getParameterTypes().equals(Collections.singletonList(OneBackPatch.ACTIVITY))&&"Z".equals(call.getReturnType()),"Activity runtime ABI at actual main dispatch boundary");
            check(((FiveRegisterInstruction)ins.get(62)).getRegisterCount()==1&&((FiveRegisterInstruction)ins.get(62)).getRegisterC()==before.getImplementation().getRegisterCount()-2,"exact Activity receiver passed");
            check(ins.get(63).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)ins.get(63)).getRegisterA()==b.scratch,"only dead scratch receives guard result");
            check(target(ins,64)==HomeBackAudit.addr(ins,66),"false verdict resumes exact original AndroidX dispatcher acquisition");
            check(ins.get(65).getOpcode()==Opcode.RETURN_VOID,"true verdict consumes only current native Back");
            for(int i:new int[]{24,30,61})check(ins.get(i).getOpcode()==Opcode.RETURN_VOID&&i<b.instruction,"native panel/layer/dialog consumed returns remain before new hook");
            Map<Integer,Integer> indexes=new HashMap<Integer,Integer>();for(int i=0;i<=old.size();i++)indexes.put(HomeBackAudit.addr(old,i),i);
            for(int i=0;i<old.size();i++)if(old.get(i) instanceof OffsetInstruction) {
                Integer targetIndex=indexes.get(target(old,i));check(targetIndex!=null,"native branch points to instruction boundary");
                int newIndex=i<62?i:i+4,newTarget=targetIndex<62?targetIndex:targetIndex==62?62:targetIndex+4;
                check(target(ins,newIndex)==HomeBackAudit.addr(ins,newTarget),"every native branch preserves target, including entries now reaching new guard: "+i);
            }
            MutableMethodImplementation restore=new MutableMethodImplementation(after.getImplementation());
            for(int i=0;i<4;i++)restore.removeInstruction(62);
            for(int i:b.incomingBranches) {OneRegisterInstruction branch=(OneRegisterInstruction)restore.getInstructions().get(i);restore.replaceInstruction(i,new BuilderInstruction21t(Opcode.IF_EQZ,branch.getRegisterA(),restore.newLabelForIndex(62)));}
            check(Arrays.equals(HomeBackAudit.methodBytes(before),HomeBackAudit.methodBytes(methodWithCode(before,restore))),"removing four inserted instructions restores entire native method byte-for-byte including branches/debug/try");
            Map<String,Method> afterMethods=HomeBackAudit.methods(out);
            for(Method m:c.getMethods())if(!HomeBackAudit.key(m).equals(HomeBackAudit.key(before))) {check(Arrays.equals(HomeBackAudit.methodBytes(m),HomeBackAudit.methodBytes(afterMethods.get(HomeBackAudit.key(m)))),"unrelated method remains byte exact");unchanged++;}
            byte[] emitted=HomeBackAudit.canonical(out);ClassDef parsed=new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(emitted)).getClasses().iterator().next();
            check(Arrays.equals(emitted,HomeBackAudit.canonical(parsed)),"hook and every branch survive actual DEX serialization");
            List<Instruction> serialized=HomeBackAudit.instructions(method(parsed,b));
            for(int i:b.incomingBranches)check(target(serialized,i)==HomeBackAudit.addr(serialized,62),"serialized native no-layer branch reaches new hook");
            for(int mutation=0;mutation<12;mutation++) {
                MutableClass changed=new MutableClass(c);MutableMethod bad=(MutableMethod)method(changed,b);MutableMethodImplementation code=bad.getImplementation();
                int yes=((OneRegisterInstruction)old.get(1)).getRegisterA(),element=((OneRegisterInstruction)old.get(50)).getRegisterA();
                if(mutation==0||mutation==1) {int i=b.incomingBranches[mutation];code.replaceInstruction(i,new BuilderInstruction21t(Opcode.IF_EQZ,b.scratch,code.newLabelForIndex(63)));}
                if(mutation==2)code.replaceInstruction(30,new BuilderInstruction10x(Opcode.NOP));
                if(mutation==3) {FieldReference f=(FieldReference)((ReferenceInstruction)old.get(19)).getReference();code.replaceInstruction(19,new BuilderInstruction22c(Opcode.IGET_OBJECT,b.scratch,bad.getImplementation().getRegisterCount()-2,new ImmutableFieldReference("Lunrelated/Activity;",f.getName(),f.getType())));}
                if(mutation==4)code.replaceInstruction(63,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,yes));
                if(mutation==5)code.replaceInstruction(62,changedCall(old.get(62),bad.getImplementation().getRegisterCount()-1,null));
                if(mutation==6)code.replaceInstruction(58,new BuilderInstruction11x(Opcode.MOVE_RESULT,yes));
                if(mutation==7)code.replaceInstruction(59,new BuilderInstruction22t(Opcode.IF_NE,b.scratch,element,code.newLabelForIndex(46)));
                if(mutation==8)code.replaceInstruction(23,new BuilderInstruction22t(Opcode.IF_NE,b.scratch,yes,code.newLabelForIndex(62)));
                if(mutation==9)code.replaceInstruction(80,new BuilderInstruction21t(Opcode.IF_NEZ,b.scratch,code.newLabelForIndex(62)));
                if(mutation==10)code.replaceInstruction(60,changedCall(old.get(60),element,"Lunrelated/Dialog;"));
                if(mutation==11)code.replaceInstruction(72,changedCall(old.get(72),bad.getImplementation().getRegisterCount()-2,"Lunrelated/ComponentActivity;"));
                boolean refused=false;try {refused=OneBackPatch.instagramMainReelsBoundary(changed,classes)==null;}catch(IllegalStateException expected){refused=true;}
                check(refused,"changed native guard/edge/receiver/reference fails closed #"+mutation);negative++;
            }
        }
        check(matches==1,"exactly one native main Reels dispatch match in actual APK");
        check(inputHash.equals(sha(apk)),"input APK never modified");
        String log="MATCH "+matchedClass+"->"+matchedMethod+" scratch=v"+boundary.scratch+" boundary_instruction="+boundary.instruction+" native_pc=126 native_instructions="+nativeCount+" incoming_branches=[37, 48] unchanged_other_methods="+unchanged+"\n"
            +"PASS Instagram120DexAudit: "+matches+" actual main Back dispatcher, "+assertions+" assertions, "+negative+" unsafe mutations rejected; all native overlay guards and other methods preserved; APK unchanged; physical Android not tested\n";
        Files.write(Paths.get(args[2]),log.getBytes(StandardCharsets.UTF_8));System.out.print(log);
        StringBuilder json=new StringBuilder("{\n");
        append(json,"schema",quote("instagram-main-reels-dispatch-v120"));append(json,"result",quote("PASS"));append(json,"input_apk",quote(apk.getFileName().toString()));
        append(json,"apk_sha256",quote(inputHash));append(json,"apk_bytes",Long.toString(Files.size(apk)));append(json,"input_apk_modified","false");
        append(json,"full_apk_rebuild_tested","false");append(json,"physical_android_tested","false");append(json,"matched_class",quote(matchedClass));append(json,"matched_method",quote(matchedMethod));
        append(json,"boundary_instruction",Integer.toString(boundary.instruction));append(json,"native_pc","126");append(json,"scratch_register",Integer.toString(boundary.scratch));
        append(json,"incoming_branches","[37,48]");append(json,"insertion_count","1");append(json,"native_instruction_count",Integer.toString(nativeCount));
        append(json,"unchanged_other_methods",Integer.toString(unchanged));append(json,"assertions",Integer.toString(assertions));append(json,"restored_native_body_exact","true");
        append(json,"negative_mutations_rejected",Integer.toString(negative));append(json,"dex_serialization_verified","true");
        append(json,"native_fragment_guard_adapter_verified","true");
        append(json,"native_fragment_guard_source_sha256",quote(sha(Paths.get(args[3]).getParent().getParent().resolve("runtime/InstagramNativeLayers.java"))));
        append(json,"patch_source_sha256",quote(sha(Paths.get(args[3]))));append(json,"patch_class_sha256",quote(resourceSha("/app/hiro/oneback/patches/OneBackPatch.class")));
        append(json,"audit_source_sha256",quote(sha(Paths.get(args[4]))));append(json,"log_sha256",quote(sha(log.getBytes(StandardCharsets.UTF_8))));
        json.append("\n}\n");Files.write(Paths.get(args[1]),json.toString().getBytes(StandardCharsets.UTF_8));
    }
}
