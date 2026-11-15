package app.hiro.berrybfcache.patches;

import app.hiro.berry.patches.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import java.io.File;
import java.util.*;
import java.util.function.Function;

/** Focused completion detector and receiver-ownership tests on real APKs,
 * using the actual full structural BFCache profile discovery. */
public final class NativeDirectGetterTest {
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private static Map<String,ClassDef> original;
    private static BfCacheRepairPatch.Profile profile;
    private static Method finished;
    private static int checks;
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("APK platform|adapter");
        original=new LinkedHashMap<>();
        MultiDexContainer<? extends DexFile> dex=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.forApi(37));
        for(String name:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(name).getDexFile().getClasses())original.put(c.getType(),c);
        BerryProfiles.Profile core=GenericBerryResolver.resolve("jp.ejimax.berrybrowser",original.values());
        String client=core.type("Lc70;"),adapter=core.type("Lc41;"),tab=core.type("Ltt5;"),web=core.type("Lcx;");
        List<Field> af=new ArrayList<>(),wf=new ArrayList<>(),cf=new ArrayList<>();
        for(Field f:original.get(tab).getFields())if(f.getType().equals(adapter)||original.get(adapter).getInterfaces().contains(f.getType()))af.add(f);
        for(Field f:original.get(adapter).getFields())if(f.getType().equals(web))wf.add(f);
        for(Field f:original.get(client).getFields())if(f.getType().equals(tab))cf.add(f);
        need(af.size()==1&&wf.size()==1&&cf.size()==1,"unique native ownership profile");
        profile=BfCacheStructure.discover(original,client,adapter,tab,web);
        need(profile.adapterField.equals(af.get(0).getName())&&profile.adapterInterface.equals(af.get(0).getType())
            &&profile.webField.equals(wf.get(0).getName())&&profile.clientTabField.equals(cf.get(0).getName()),
            "full BFCache profile matches independently resolved ownership");
        for(Method m:original.get(client).getMethods())if(m.getName().equals("onPageFinished"))finished=m;
        NativeCompletionStructure nativeBody=NativeCompletionStructure.discover(original,profile,finished);
        boolean platform=args[1].equals("platform");
        need(nativeBody.platformUrlGetter==platform&&nativeBody.platformTitleGetter==platform,"expected getter invocation kind");
        String body=nativeBody.code(profile),call=platform?"invoke-virtual {p1}, ":"invoke-interface {v1}, ";
        need(body.contains(call+nativeBody.urlGetter)&&body.contains(call+nativeBody.titleGetter),"generated getters use the verified receiver and opcode");
        need(body.indexOf("if-ne v2, p1, :failed")<body.indexOf(call+nativeBody.urlGetter),"owned WebView equality guard precedes direct getters");
        int foreign=finished.getImplementation().getRegisterCount()-2;
        reject(mutate(nativeBody.urlGetter,i->foreignCall(i,foreign)),"URL getter foreign receiver");
        reject(mutate(nativeBody.titleGetter,i->foreignCall(i,foreign)),"title getter foreign receiver");
        reject(mutate(F.getFieldDescriptor(af.get(0)),i->foreignField(i,foreign)),"adapter load foreign tab");
        reject(mutate(nativeBody.metadata,i->foreignField(i,foreign)),"metadata load foreign tab");
        reject(privateField(nativeBody.metadataUrl),"private URL metadata field");
        reject(privateField(nativeBody.metadataTitle),"private title metadata field");
        reject(privateField(nativeBody.metadata),"private metadata owner field");
        if(platform){
            reject(mutate(F.getFieldDescriptor(wf.get(0)),i->foreignField(i,foreign)),"WebView load foreign adapter");
            reject(mutate(nativeBody.urlGetter,i->{FiveRegisterInstruction r=(FiveRegisterInstruction)i;
                return new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE,r.getRegisterCount(),r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG(),((ReferenceInstruction)i).getReference());}),"platform getter invoked as interface");
            reject(mutate(nativeBody.urlGetter,i->{FiveRegisterInstruction r=(FiveRegisterInstruction)i;
                return new ImmutableInstruction35c(i.getOpcode(),r.getRegisterCount(),r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG(),
                    new ImmutableMethodReference(web,"getUrl",Collections.emptyList(),"Ljava/lang/String;"));}),"getter is not exact platform method");
        }
        System.out.println("PASS native completion ownership checks="+checks+" mode="+args[1]);
    }
    private static Instruction foreignCall(Instruction i,int foreign){FiveRegisterInstruction r=(FiveRegisterInstruction)i;return new ImmutableInstruction35c(i.getOpcode(),r.getRegisterCount(),foreign,r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG(),((ReferenceInstruction)i).getReference());}
    private static Instruction foreignField(Instruction i,int foreign){TwoRegisterInstruction r=(TwoRegisterInstruction)i;return new ImmutableInstruction22c(i.getOpcode(),r.getRegisterA(),foreign,((ReferenceInstruction)i).getReference());}
    private static Map<String,ClassDef> mutate(String target,Function<Instruction,Instruction> transform){
        List<Instruction> code=new ArrayList<>();int changed=0;
        for(Instruction i:finished.getImplementation().getInstructions())if(i instanceof ReferenceInstruction&&F.getReference(((ReferenceInstruction)i).getReference()).equals(target)){code.add(transform.apply(i));changed++;}else code.add(i);
        if(changed!=1)throw new AssertionError("expected one mutation for "+target+": "+changed);
        MethodImplementation impl=finished.getImplementation();
        Method replacement=new ImmutableMethod(finished.getDefiningClass(),finished.getName(),finished.getParameters(),finished.getReturnType(),finished.getAccessFlags(),finished.getAnnotations(),finished.getHiddenApiRestrictions(),new ImmutableMethodImplementation(impl.getRegisterCount(),code,impl.getTryBlocks(),impl.getDebugItems()));
        ClassDef owner=original.get(profile.client);List<Method> methods=new ArrayList<>();for(Method m:owner.getMethods())methods.add(F.getMethodDescriptor(m).equals(F.getMethodDescriptor(finished))?replacement:m);
        Map<String,ClassDef> result=new LinkedHashMap<>(original);result.put(owner.getType(),copy(owner,owner.getFields(),methods));return result;
    }
    private static Map<String,ClassDef> privateField(String target){
        String type=target.substring(0,target.indexOf("->"));ClassDef owner=original.get(type);List<Field> fields=new ArrayList<>();int changed=0;
        for(Field f:owner.getFields())if(F.getFieldDescriptor(f).equals(target)){fields.add(new ImmutableField(f.getDefiningClass(),f.getName(),f.getType(),(f.getAccessFlags()&~1)|2,f.getInitialValue(),f.getAnnotations(),f.getHiddenApiRestrictions()));changed++;}else fields.add(f);
        if(changed!=1)throw new AssertionError("expected one private-field mutation");
        Map<String,ClassDef> result=new LinkedHashMap<>(original);result.put(type,copy(owner,fields,owner.getMethods()));return result;
    }
    private static ClassDef copy(ClassDef c,Iterable<? extends Field>f,Iterable<? extends Method>m){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),f,m);}
    private static void reject(Map<String,ClassDef>classes,String label){Method m=null;for(Method candidate:classes.get(profile.client).getMethods())if(F.getMethodDescriptor(candidate).equals(F.getMethodDescriptor(finished)))m=candidate;
        try{NativeCompletionStructure.discover(classes,profile,m);}catch(IllegalStateException expected){need(true,"rejected "+label);return;}throw new AssertionError("accepted "+label);}
    private static void need(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
}
