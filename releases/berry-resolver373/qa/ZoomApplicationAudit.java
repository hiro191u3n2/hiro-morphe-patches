import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Independent final-APK audit: exact zoom hook prefixes, unchanged original
 * control flow, unchanged original class definitions and scoped resource edits.
 * Usage: original.apk patched.apk report.json
 */
public final class ZoomApplicationAudit {
    static final DexFormatter F= DexFormatter.INSTANCE;
    static final String HELPER="Lhiro/berryzoom/OneHandZoom;";
    static final String TOUCH=HELPER+"->touch(Landroid/webkit/WebView;Landroid/view/MotionEvent;)Z";
    static final String BLOCK=HELPER+"->blocksParent(Landroid/view/ViewGroup;Landroid/webkit/WebView;Landroid/view/MotionEvent;)Z";
    // Use a supported serialization target for canonical comparisons. The
    // bundled dexlib predates a writer mapping for API 37.
    static final Opcodes OPS=Opcodes.forApi(26);
    static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
    static Map<String,ClassDef> load(String path)throws Exception {
        Map<String,ClassDef> out=new TreeMap<>();
        MultiDexContainer<? extends DexFile> dex=DexFileFactory.loadDexContainer(new File(path),OPS);
        for(String n:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(n).getDexFile().getClasses())need(out.put(c.getType(),c)==null,"duplicate class");
        return out;
    }
    static byte[] canonical(ClassDef c)throws Exception {
        MemoryDataStore out=new MemoryDataStore();try{DexPool.writeTo(out,new ImmutableDexFile(OPS,Collections.singleton(c)));return Arrays.copyOf(out.getBuffer(),out.getSize());}finally{out.close();}
    }
    static String desc(Method m){return F.getMethodDescriptor(m);}
    static String ref(Instruction i){return i instanceof ReferenceInstruction?F.getReference(((ReferenceInstruction)i).getReference()):"";}
    static List<Instruction> ins(Method m){List<Instruction> out=new ArrayList<>();for(Instruction i:m.getImplementation().getInstructions())out.add(i);return out;}
    static int[] args(Instruction i){
        if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction x=(FiveRegisterInstruction)i;return Arrays.copyOf(new int[]{x.getRegisterC(),x.getRegisterD(),x.getRegisterE(),x.getRegisterF(),x.getRegisterG()},x.getRegisterCount());}
        if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction x=(RegisterRangeInstruction)i;int[] a=new int[x.getRegisterCount()];for(int j=0;j<a.length;j++)a[j]=x.getStartRegister()+j;return a;}
        return new int[0];
    }
    static Map<Integer,Integer> addresses(List<Instruction> in){Map<Integer,Integer> map=new HashMap<>();int pc=0;for(int j=0;j<in.size();j++){map.put(pc,j);pc+=in.get(j).getCodeUnits();}map.put(pc,in.size());return map;}
    static int pc(List<Instruction> in,int index){int out=0;for(int j=0;j<index;j++)out+=in.get(j).getCodeUnits();return out;}
    static int target(List<Instruction> in,int index){Instruction i=in.get(index);Integer out=addresses(in).get(pc(in,index)+((OffsetInstruction)i).getCodeOffset());need(out!=null,"bad target");return out;}
    static String instruction(List<Instruction> in,int j,int prefix){
        Instruction i=in.get(j);String op=i.getOpcode().name();
        if(op.startsWith("GOTO"))op="GOTO";if(op.equals("CONST_STRING_JUMBO"))op="CONST_STRING";
        StringBuilder b=new StringBuilder(op);
        if(i instanceof OneRegisterInstruction)b.append("/a=").append(((OneRegisterInstruction)i).getRegisterA());
        if(i instanceof TwoRegisterInstruction)b.append("/b=").append(((TwoRegisterInstruction)i).getRegisterB());
        if(i instanceof ThreeRegisterInstruction)b.append("/c=").append(((ThreeRegisterInstruction)i).getRegisterC());
        if(i instanceof VariableRegisterInstruction)b.append("/args=").append(Arrays.toString(args(i)));
        if(i instanceof WideLiteralInstruction)b.append("/lit=").append(((WideLiteralInstruction)i).getWideLiteral());
        if(i instanceof ReferenceInstruction)b.append("/ref=").append(ref(i));
        if(i instanceof OffsetInstruction)b.append("/target=").append(target(in,j)-prefix);
        need(!(i instanceof PayloadInstruction)&&!(i instanceof DualReferenceInstruction),"unhandled hook-body instruction");
        return b.toString();
    }
    static void prefix(Method old,Method changed,boolean touch){
        List<Instruction> in=ins(changed),before=ins(old);int regs=old.getImplementation().getRegisterCount();
        need(regs==changed.getImplementation().getRegisterCount(),"register count changed");
        int invoke=touch?0:(regs<=16?2:4),length=invoke+5;
        need(in.size()==before.size()+length,"body length changed beyond prefix "+desc(old));
        need(ref(in.get(invoke)).equals(touch?TOUCH:BLOCK),"wrong injected invoke");
        need(Arrays.equals(args(in.get(invoke)),touch?new int[]{regs-2,regs-1}:(regs<=16?new int[]{regs-2,0,regs-1}:new int[]{0,1,2})),"wrong hook arguments");
        int result=touch?0:1;
        need(in.get(invoke+1).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)in.get(invoke+1)).getRegisterA()==result,"hook result destination");
        need(in.get(invoke+2).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)in.get(invoke+2)).getRegisterA()==result&&target(in,invoke+2)==length,"original branch target");
        need(in.get(invoke+3).getOpcode()==Opcode.CONST_4&&((OneRegisterInstruction)in.get(invoke+3)).getRegisterA()==0&&((NarrowLiteralInstruction)in.get(invoke+3)).getNarrowLiteral()==(touch?1:0),"hook return constant");
        need(in.get(invoke+4).getOpcode()==Opcode.RETURN&&((OneRegisterInstruction)in.get(invoke+4)).getRegisterA()==0,"hook return");
        if(!touch){
            int f1=regs<=16?0:1,f2=regs<=16?1:2;
            need(in.get(f1).getOpcode()==Opcode.IGET_OBJECT&&in.get(f2).getOpcode()==Opcode.IGET_OBJECT,"owned WebView field chain");
            FieldReference a=(FieldReference)((ReferenceInstruction)in.get(f1)).getReference(),b=(FieldReference)((ReferenceInstruction)in.get(f2)).getReference();
            need(a.getDefiningClass().equals(old.getDefiningClass())&&a.getType().equals(b.getDefiningClass()),"field chain types");
            if(regs<=16){
                need(((TwoRegisterInstruction)in.get(0)).getRegisterA()==0&&((TwoRegisterInstruction)in.get(0)).getRegisterB()==regs-2,"parent field receiver");
                need(((TwoRegisterInstruction)in.get(1)).getRegisterA()==0&&((TwoRegisterInstruction)in.get(1)).getRegisterB()==0,"WebView field receiver");
            }else{
                need(in.get(0).getOpcode()==Opcode.MOVE_OBJECT_FROM16&&((TwoRegisterInstruction)in.get(0)).getRegisterA()==0&&((TwoRegisterInstruction)in.get(0)).getRegisterB()==regs-2,"parent register copy");
                need(((TwoRegisterInstruction)in.get(1)).getRegisterA()==1&&((TwoRegisterInstruction)in.get(1)).getRegisterB()==0,"parent field receiver");
                need(((TwoRegisterInstruction)in.get(2)).getRegisterA()==1&&((TwoRegisterInstruction)in.get(2)).getRegisterB()==1,"WebView field receiver");
                need(in.get(3).getOpcode()==Opcode.MOVE_OBJECT_FROM16&&((TwoRegisterInstruction)in.get(3)).getRegisterA()==2&&((TwoRegisterInstruction)in.get(3)).getRegisterB()==regs-1,"event register copy");
            }
        }
        for(int j=0;j<before.size();j++)need(instruction(before,j,0).equals(instruction(in,j+length,length)),"original instruction changed "+desc(old)+" #"+j);
        need(old.getImplementation().getTryBlocks().isEmpty()&&changed.getImplementation().getTryBlocks().isEmpty(),"unexpected exception regions");
    }
    static Map<String,byte[]> resources(String path)throws Exception {Map<String,byte[]> out=new TreeMap<>();try(ZipFile z=new ZipFile(path)){for(Enumeration<? extends ZipEntry> it=z.entries();it.hasMoreElements();){ZipEntry e=it.nextElement();if(!e.isDirectory()&&!e.getName().matches("classes[0-9]*\\.dex")&&!e.getName().startsWith("META-INF/"))out.put(e.getName(),z.getInputStream(e).readAllBytes());}}return out;}
    public static void main(String[] a)throws Exception {
        Map<String,ClassDef> before=load(a[0]),after=load(a[1]);Map<String,Method> hooks=new TreeMap<>();
        for(ClassDef c:after.values())if(before.containsKey(c.getType()))for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())if(ref(i).equals(TOUCH)||ref(i).equals(BLOCK))need(hooks.put(ref(i),m)==null,"duplicate hook");
        need(hooks.size()==2,"missing zoom hook");
        Map<String,Method> originals=new HashMap<>();for(ClassDef c:before.values())for(Method m:c.getMethods())originals.put(desc(m),m);
        for(Map.Entry<String,Method> e:hooks.entrySet()){Method old=originals.get(desc(e.getValue()));need(old!=null,"hook target missing in original");prefix(old,e.getValue(),e.getKey().equals(TOUCH));}
        Set<String> targets=new HashSet<>();for(Method m:hooks.values())targets.add(desc(m));int preserved=0;
        for(ClassDef old:before.values()){
            ClassDef now=after.get(old.getType());need(now!=null,"original class removed");List<Method> normalized=new ArrayList<>();
            for(Method m:now.getMethods())normalized.add(targets.contains(desc(m))?originals.get(desc(m)):m);
            ClassDef clean=new ImmutableClassDef(now.getType(),now.getAccessFlags(),now.getSuperclass(),now.getInterfaces(),now.getSourceFile(),now.getAnnotations(),now.getFields(),normalized);
            need(Arrays.equals(canonical(old),canonical(clean)),"non-hook definition changed "+old.getType());preserved++;
        }
        int added=0;for(String t:after.keySet())if(!before.containsKey(t)){need(t.startsWith("Lhiro/berryzoom/"),"unexpected new class "+t);added++;}
        need(added>0&&after.containsKey(HELPER),"zoom runtime absent");
        Map<String,byte[]> r0=resources(a[0]),r1=resources(a[1]);need(r0.keySet().equals(r1.keySet()),"resource inventory changed");List<String> changes=new ArrayList<>();
        for(String name:r0.keySet())if(!Arrays.equals(r0.get(name),r1.get(name))){need(Arrays.asList("AndroidManifest.xml","resources.arsc","res/xml/pref_action.xml").contains(name),"unexpected resource edit "+name);changes.add(name);}
        need(changes.contains("res/xml/pref_action.xml"),"zoom preference was not changed");
        String result="{\"result\":\"PASS\",\"original_classes_preserved_except_two_verified_prefixes\":"+preserved+",\"zoom_runtime_classes_added\":"+added+",\"touch_target\":\""+desc(hooks.get(TOUCH))+"\",\"intercept_target\":\""+desc(hooks.get(BLOCK))+"\",\"resource_changes\":\""+changes+"\"}\n";
        Files.writeString(Path.of(a[2]),result);System.out.print(result);
    }
}
