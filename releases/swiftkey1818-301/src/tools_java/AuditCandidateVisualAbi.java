import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.r8.org.objectweb.asm.ClassReader;
import com.android.tools.r8.org.objectweb.asm.tree.ClassNode;
import com.android.tools.r8.org.objectweb.asm.tree.FieldNode;
import com.android.tools.r8.org.objectweb.asm.tree.MethodNode;

/** Independent symbolic ABI verification of DEX against actual APK and Android API jar. */
public final class AuditCandidateVisualAbi implements AutoCloseable {
    private static final int PUBLIC = 1, PRIVATE = 2, PROTECTED = 4, STATIC = 8, INTERFACE = 0x200, ABSTRACT = 0x400;
    private final Map<String, Type> types = new HashMap<String, Type>();
    private final Set<String> missingTypes = new HashSet<String>();
    private final Map<String, ClassDef> extension = new TreeMap<String, ClassDef>();
    private final ZipFile android;
    private final Map<String, Map<String, Object>> references = new TreeMap<String, Map<String, Object>>();
    private final Map<String, Map<String, Object>> typeReferences = new TreeMap<String, Map<String, Object>>();
    private final Set<String> unresolved = new TreeSet<String>(), inaccessible = new TreeSet<String>(), incompatible = new TreeSet<String>();
    private final List<String> auditedClasses = new ArrayList<String>();
    private final List<Map<String,Object>> nativeHookEntryPoints = new ArrayList<Map<String,Object>>();
    private boolean nativeHookContractVerified = true;
    private static final String[] NATIVE_HOOK_ENTRY_POINTS = {
        "stabilize(Lrb0/j;Lib0/a;)Lib0/a;", "tryAppend(Lrb0/h;Z)Z", "snapshot(Lrb0/h;)V",
        "dispatchUpdate(Lrb0/j;Lfb0/u;Z)V",
        "scrollPanelToStart(Lrb0/k;Landroidx/recyclerview/widget/RecyclerView;I)V",
        "finishUpdate(Lrb0/j;)V",
        "rebaseScrollWatermark(Lrb0/l;Landroidx/recyclerview/widget/RecyclerView;II)V"
    };

    private static final class Member {
        String owner, name, descriptor; int access; boolean field;
        Member(String owner, String name, String descriptor, int access, boolean field) {
            this.owner=owner; this.name=name; this.descriptor=descriptor; this.access=access; this.field=field;
        }
    }
    private static final class Type {
        String name, parent, source; List<String> interfaces; int access;
        final Map<String, Member> methods = new HashMap<String, Member>(), fields = new HashMap<String, Member>();
        Type(String name, String parent, List<String> interfaces, int access, String source) {
            this.name=name; this.parent=parent; this.interfaces=interfaces; this.access=access; this.source=source;
        }
    }
    private static String descriptor(MethodReference method) {
        StringBuilder out = new StringBuilder("(");
        for (CharSequence p : method.getParameterTypes()) out.append(p);
        return out.append(')').append(method.getReturnType()).toString();
    }
    private static String name(String internal) { return internal == null ? null : "L"+internal+";"; }
    private static Type dexType(ClassDef c, String source) {
        Type out=new Type(c.getType(), c.getSuperclass(), new ArrayList<String>(c.getInterfaces()), c.getAccessFlags(),source);
        for (Method m:c.getMethods()) out.methods.put(m.getName()+descriptor(m),new Member(out.name,m.getName(),descriptor(m),m.getAccessFlags(),false));
        for (Field f:c.getFields()) out.fields.put(f.getName()+":"+f.getType(),new Member(out.name,f.getName(),f.getType(),f.getAccessFlags(),true));
        return out;
    }
    private AuditCandidateVisualAbi(Path apk, Path extensionPath, Path androidJar) throws Exception {
        android=new ZipFile(androidJar.toFile());
        MultiDexContainer<? extends DexFile> dex=DexFileFactory.loadDexContainer(apk.toFile(),Opcodes.forApi(26));
        for (String entry:dex.getDexEntryNames()) for (ClassDef c:dex.getEntry(entry).getDexFile().getClasses()) {
            if(types.put(c.getType(),dexType(c,"native_apk"))!=null) throw new IllegalStateException("Duplicate native type "+c.getType());
        }
        DexBackedDexFile ext=new DexBackedDexFile(Opcodes.forApi(26),java.nio.ByteBuffer.wrap(Files.readAllBytes(extensionPath)));
        for (ClassDef c:ext.getClasses()) {
            if(extension.put(c.getType(),c)!=null)throw new IllegalStateException("Duplicate extension type "+c.getType());
            types.put(c.getType(),dexType(c,"extension"));
        }
    }
    private Type type(String descriptor) throws IOException {
        if(descriptor==null)return null;
        Type known=types.get(descriptor); if(known!=null)return known;
        if(missingTypes.contains(descriptor))return null;
        if(descriptor.startsWith("[")) {
            Type array=new Type(descriptor,"Ljava/lang/Object;",Arrays.asList("Ljava/lang/Cloneable;","Ljava/io/Serializable;"),PUBLIC|0x10,"array");
            array.methods.put("clone()Ljava/lang/Object;",new Member(descriptor,"clone","()Ljava/lang/Object;",PUBLIC,false));
            types.put(descriptor,array); return array;
        }
        if(!descriptor.startsWith("L")||!descriptor.endsWith(";"))return null;
        ZipEntry entry=android.getEntry(descriptor.substring(1,descriptor.length()-1)+".class");
        if(entry==null){missingTypes.add(descriptor);return null;}
        ClassNode node=new ClassNode();
        try(InputStream input=android.getInputStream(entry)){new ClassReader(input).accept(node,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);}
        List<String> interfaces=new ArrayList<String>();for(String itf:node.interfaces)interfaces.add(name(itf));
        Type out=new Type(name(node.name),name(node.superName),interfaces,node.access,"android_api35");
        for(MethodNode method:node.methods)out.methods.put(method.name+method.desc,new Member(out.name,method.name,method.desc,method.access,false));
        for(FieldNode field:node.fields)out.fields.put(field.name+":"+field.desc,new Member(out.name,field.name,field.desc,field.access,true));
        types.put(descriptor,out);return out;
    }
    private Member lookup(String owner,String member,String descriptor,boolean field,Set<String> seen) throws IOException {
        if(owner==null||!seen.add(owner))return null;
        Type target=type(owner); if(target==null)return null;
        Member own=(field?target.fields:target.methods).get(member+(field?":":"")+descriptor);
        if(own!=null)return own;
        if(!field&&(member.equals("<init>")||member.equals("<clinit>")))return null;
        if(field) for(String itf:target.interfaces) {Member found=lookup(itf,member,descriptor,true,seen);if(found!=null)return found;}
        Member parent=lookup(target.parent,member,descriptor,field,seen);if(parent!=null)return parent;
        if(!field)for(String itf:target.interfaces){Member found=lookup(itf,member,descriptor,false,seen);if(found!=null)return found;}
        return null;
    }
    private boolean subtype(String child,String parent,Set<String> seen) throws IOException {
        if(child==null||!seen.add(child))return false;if(child.equals(parent))return true;
        Type c=type(child);if(c==null)return false;
        if(subtype(c.parent,parent,seen))return true;
        for(String itf:c.interfaces)if(subtype(itf,parent,seen))return true;
        return false;
    }
    private static String pkg(String owner){int slash=owner.lastIndexOf('/');return slash<0?"":owner.substring(0,slash);}
    private boolean accessible(String caller,Member member) throws IOException {
        if((member.access&PUBLIC)!=0)return true;
        if((member.access&PRIVATE)!=0)return caller.equals(member.owner);
        if(pkg(caller).equals(pkg(member.owner)))return true;
        return (member.access&PROTECTED)!=0&&subtype(caller,member.owner,new HashSet<String>());
    }
    private boolean classAccessible(String caller,String owner) throws IOException {
        if(owner.startsWith("["))return classAccessible(caller,owner.substring(1));
        if(owner.length()==1)return true;
        Type c=type(owner);return c!=null&&((c.access&PUBLIC)!=0||pkg(caller).equals(pkg(owner)));
    }
    private static int words(CharSequence type){return "J".contentEquals(type)||"D".contentEquals(type)?2:1;}
    private static String flags(int access){List<String> parts=new ArrayList<String>();if((access&PUBLIC)!=0)parts.add("public");if((access&PRIVATE)!=0)parts.add("private");if((access&PROTECTED)!=0)parts.add("protected");if((access&STATIC)!=0)parts.add("static");if((access&INTERFACE)!=0)parts.add("interface");if((access&ABSTRACT)!=0)parts.add("abstract");return String.join(" ",parts);}
    private void checkType(String caller,String use,String descriptor,String opcode) throws IOException {
        String component=descriptor;while(component.startsWith("["))component=component.substring(1);
        if(component.length()==1)return;
        Type target=type(component);
        String key=descriptor+" | "+opcode;
        Map<String,Object> row=typeReferences.get(key);
        if(row==null){row=new LinkedHashMap<String,Object>();row.put("reference",descriptor);row.put("opcode",opcode);row.put("resolved",target!=null);if(target!=null)row.put("resolution_source",target.source);typeReferences.put(key,row);}
        if(target==null)unresolved.add(use+" -> type "+descriptor);
        else if(!classAccessible(caller,descriptor))inaccessible.add(use+" -> type "+descriptor);
        if(target!=null&&opcode.equals("new-instance")&&(target.access&(INTERFACE|ABSTRACT))!=0)
            incompatible.add(use+" -> "+descriptor+" cannot instantiate interface/abstract type");
    }
    private void checkReference(String caller,String use,Instruction instruction,Reference ref) throws IOException {
        String opcode=instruction.getOpcode().name().toLowerCase(Locale.ROOT).replace('_','-');
        if(ref instanceof TypeReference){checkType(caller,use,((TypeReference)ref).getType(),opcode);return;}
        boolean field=ref instanceof FieldReference;
        if(!field&&!(ref instanceof MethodReference))return;
        String owner,member,desc;
        if(field){FieldReference f=(FieldReference)ref;owner=f.getDefiningClass();member=f.getName();desc=f.getType();}
        else{MethodReference m=(MethodReference)ref;owner=m.getDefiningClass();member=m.getName();desc=descriptor(m);}
        String key=owner+"->"+member+(field?":":"")+desc;
        Type symbolic=type(owner);
        Member found=lookup(owner,member,desc,field,new HashSet<String>());
        String entryKey=key+" | "+opcode;
        Map<String,Object> row=references.get(entryKey);
        if(row==null){
            row=new LinkedHashMap<String,Object>();row.put("reference",key);row.put("opcode",opcode);row.put("resolved",found!=null);
            row.put("callers",new TreeSet<String>());
            if(found!=null){row.put("defined_owner",found.owner);row.put("access",flags(found.access));row.put("resolution_source",type(found.owner).source);row.put("android_inherited",!owner.equals(found.owner)&&type(found.owner).source.equals("android_api35"));}
            references.put(entryKey,row);
        }
        @SuppressWarnings("unchecked") Set<String> callers=(Set<String>)row.get("callers");callers.add(use);
        if(found==null){unresolved.add(use+" -> "+key);return;}
        if(symbolic==null||!classAccessible(caller,owner)||!accessible(caller,found))inaccessible.add(use+" -> "+key);
        boolean isStatic=(found.access&STATIC)!=0;
        boolean opcodeStatic=opcode.startsWith("invoke-static")||opcode.startsWith("sget")||opcode.startsWith("sput");
        if(isStatic!=opcodeStatic)incompatible.add(use+" -> "+key+" static/instance opcode mismatch "+opcode);
        if(field){
            String expected;
            if(desc.startsWith("L")||desc.startsWith("["))expected="object";
            else if(desc.equals("J")||desc.equals("D"))expected="wide";
            else if(desc.equals("Z"))expected="boolean";
            else if(desc.equals("B"))expected="byte";
            else if(desc.equals("C"))expected="char";
            else if(desc.equals("S"))expected="short";
            else expected="";
            String suffix=opcode.length()>4?opcode.substring(5):"";
            if(!expected.equals(suffix))incompatible.add(use+" -> "+key+" field width/type mismatch "+opcode);
        }else{
            if(symbolic!=null&&opcode.startsWith("invoke-interface")&&(symbolic.access&INTERFACE)==0)incompatible.add(use+" -> "+key+" invoke-interface owner is a class");
            if(symbolic!=null&&opcode.startsWith("invoke-virtual")&&(symbolic.access&INTERFACE)!=0)incompatible.add(use+" -> "+key+" invoke-virtual owner is an interface");
            if(member.equals("<init>")&&(!opcode.startsWith("invoke-direct")||!owner.equals(found.owner)))incompatible.add(use+" -> "+key+" constructor invocation mismatch");
            if(opcode.startsWith("invoke-direct")&&!member.equals("<init>")&&(found.access&PRIVATE)==0)incompatible.add(use+" -> "+key+" nonprivate method invoked direct");
            if(opcode.startsWith("invoke-virtual")&&(found.access&PRIVATE)!=0)incompatible.add(use+" -> "+key+" private method invoked virtual");
            int count=isStatic?0:1;
            for(CharSequence parameter:((MethodReference)ref).getParameterTypes())count+=words(parameter);
            int encoded=instruction instanceof RegisterRangeInstruction?((RegisterRangeInstruction)instruction).getRegisterCount():instruction instanceof FiveRegisterInstruction?((FiveRegisterInstruction)instruction).getRegisterCount():-1;
            if(encoded!=count)incompatible.add(use+" -> "+key+" argument register width mismatch expected="+count+" actual="+encoded);
        }
    }
    private void audit() throws Exception {
        // The injected native calls are outside the extension being audited.
        // Check their public/static targets explicitly, not only calls made by
        // the helper itself, so a mismatched hook ABI cannot pass this audit.
        Type helper = type("Lhiro/swiftkey/CandidateScroll;");
        for (String signature : NATIVE_HOOK_ENTRY_POINTS) {
            Member target = helper == null ? null : helper.methods.get(signature);
            boolean valid = helper != null && (helper.access & PUBLIC) != 0 && target != null
                    && (target.access & (PUBLIC | STATIC)) == (PUBLIC | STATIC)
                    && (target.access & ABSTRACT) == 0;
            Map<String,Object> row = new LinkedHashMap<String,Object>();
            row.put("reference", "Lhiro/swiftkey/CandidateScroll;->" + signature);
            row.put("public_static_concrete", valid); nativeHookEntryPoints.add(row);
            if (!valid) {
                nativeHookContractVerified = false;
                incompatible.add("Injected native hook target unavailable/public-static mismatch: " + signature);
            }
        }
        for(ClassDef c:extension.values()){
            if(!c.getType().startsWith("Lhiro/swiftkey/CandidateScroll"))continue;
            auditedClasses.add(c.getType());
            if(c.getSuperclass()!=null){checkType(c.getType(),c.getType(),c.getSuperclass(),"extends");Type parent=type(c.getSuperclass());if(parent!=null&&(parent.access&INTERFACE)!=0)incompatible.add(c.getType()+" extends interface "+parent.name);}
            for(String itf:c.getInterfaces()){checkType(c.getType(),c.getType(),itf,"implements");Type target=type(itf);if(target!=null&&(target.access&INTERFACE)==0)incompatible.add(c.getType()+" implements class "+itf);}
            for(Method method:c.getMethods()){
                if(method.getImplementation()==null)continue;
                String use=c.getType()+"->"+method.getName()+descriptor(method);
                for(Instruction instruction:method.getImplementation().getInstructions()){
                    if(instruction instanceof ReferenceInstruction)checkReference(c.getType(),use,instruction,((ReferenceInstruction)instruction).getReference());
                    if(instruction instanceof DualReferenceInstruction)checkReference(c.getType(),use,instruction,((DualReferenceInstruction)instruction).getReference2());
                }
            }
        }
        if(auditedClasses.isEmpty())unresolved.add("No candidate-scroll extension classes were found");
    }
    private static String sha(Path path) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));}
    private static String json(Object value){
        if(value==null)return "null";if(value instanceof Boolean||value instanceof Number)return value.toString();
        if(value instanceof Map){StringBuilder b=new StringBuilder("{");boolean first=true;for(Map.Entry<?,?> e:((Map<?,?>)value).entrySet()){if(!first)b.append(',');first=false;b.append(json(e.getKey().toString())).append(':').append(json(e.getValue()));}return b.append('}').toString();}
        if(value instanceof Iterable){StringBuilder b=new StringBuilder("[");boolean first=true;for(Object e:(Iterable<?>)value){if(!first)b.append(',');first=false;b.append(json(e));}return b.append(']').toString();}
        StringBuilder b=new StringBuilder("\"");for(char c:value.toString().toCharArray()){if(c=='\\'||c=='\"')b.append('\\').append(c);else if(c=='\n')b.append("\\n");else if(c=='\r')b.append("\\r");else if(c=='\t')b.append("\\t");else if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}return b.append('"').toString();
    }
    public void close() throws IOException {android.close();}
    public static void main(String[] args) throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("Usage: AuditCandidateVisualAbi archive.apk extension.mpe android.jar output.json");
        Path apk=Path.of(args[0]),extension=Path.of(args[1]),android=Path.of(args[2]),output=Path.of(args[3]);
        try(AuditCandidateVisualAbi audit=new AuditCandidateVisualAbi(apk,extension,android)){
            audit.audit();boolean passed=audit.unresolved.isEmpty()&&audit.inaccessible.isEmpty()&&audit.incompatible.isEmpty();
            Map<String,Object> report=new LinkedHashMap<String,Object>();report.put("status",passed?"PASSED":"FAILED");
            report.put("scope","Independent symbolic native/member ABI verification, including inherited Android API35 members; not Android execution");
            report.put("apk_sha256",sha(apk));report.put("apk_provenance","Previously modified archived SwiftKey Beta 9.13.16.4 v2 APK; not a clean application test");
            report.put("extension_sha256",sha(extension));report.put("android_jar_sha256",sha(android));report.put("audited_classes",audit.auditedClasses);
            report.put("native_and_extension_references",audit.references.values());report.put("type_references",audit.typeReferences.values());
            report.put("native_hook_entrypoints",audit.nativeHookEntryPoints);
            report.put("native_hook_contract_verified",audit.nativeHookContractVerified);
            report.put("unresolved",audit.unresolved);report.put("inaccessible",audit.inaccessible);report.put("incompatible_opcodes",audit.incompatible);
            report.put("platform_members_checked",true);report.put("platform_inherited_members_resolved",true);report.put("each_caller_access_checked",true);
            report.put("device_tested",false);report.put("original_apk_apply_tested",false);
            Files.createDirectories(output.toAbsolutePath().getParent());Files.writeString(output,json(report)+"\n");
            System.out.println((passed?"PASSED":"FAILED")+" ABI classes="+audit.auditedClasses.size()+" member/opcode pairs="+audit.references.size()+" type/opcode pairs="+audit.typeReferences.size());
            if(!passed){System.out.println("unresolved="+audit.unresolved);System.out.println("inaccessible="+audit.inaccessible);System.out.println("incompatible_opcodes="+audit.incompatible);System.exit(1);}
        }
    }
}
