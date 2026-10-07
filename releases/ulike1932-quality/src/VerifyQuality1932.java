import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Independently undo each approved hook, then require the complete old bodies and class shells. */
public final class VerifyQuality1932 {
    static final String PREFIX="Lcom/hiro/ulike/";
    static final String PIPE=PREFIX+"QualityPipeline1932;";
    static final String SHOT=PREFIX+"ShotContext1932;";
    static final String STATE=PREFIX+"ChromaPipeline186$State;";
    static final String SETTINGS=PREFIX+"PhotoDetail$Settings;";
    static final String SETTINGS_FIELD=PREFIX+"PhotoDetail;->settings:"+SETTINGS;
    static final String WATERMARK="Li/p/a/t/e;->d(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;IDD)Landroid/graphics/Bitmap;";
    static final String WATERMARK_HASH="ff8ddd52a708366663435721f0c8ef1bc9ac3d6098f2ace2cc1e5db355eaff4a";
    static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
    static final String HELPER="Lcom/hiro/ulike/(QualityPipeline1932|QualityShadow1932|QualityPixels1932|ShotContext1932)(\\$[^;]+)?;";
    static final Set<String> ROOTS=Set.of(PIPE,SHOT,PREFIX+"QualityPixels1932;",PREFIX+"QualityShadow1932;");
    static final Map<String,String> ALIASES=Map.of(
        PREFIX+"PhotoDetail;->snapshot1932()"+SETTINGS,SETTINGS_FIELD,
        PREFIX+"ChromaPipeline177;->enabled1932()Z",PREFIX+"ChromaPipeline177;->enabled:Z");
    static int assertions;

    static void req(boolean value,String message){assertions++;if(!value)throw new IllegalStateException(message);}
    static String id(Method method){return MergePayloads.id(method);}
    static String ref(Instruction instruction){return instruction instanceof ReferenceInstruction value?value.getReference().toString():"";}
    static Method body(Method method,MethodImplementation implementation){
        return new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),
            method.getAccessFlags(),method.getAnnotations(),method.getHiddenApiRestrictions(),implementation);
    }
    static ClassDef methods(ClassDef type,Collection<Method> methods){
        return new ImmutableClassDef(type.getType(),type.getAccessFlags(),type.getSuperclass(),type.getInterfaces(),
            type.getSourceFile(),type.getAnnotations(),type.getFields(),methods);
    }
    static String canonicalClassHash(ClassDef type)throws Exception{
        // ImmutableClassDef sorts annotation sets before DexPool interns them.
        // Normalize both operands; direct-vs-immutable byte order differs even
        // for an untouched class containing multiple method annotations.
        return MergePayloads.classHash(ImmutableClassDef.of(type));
    }
    static void classShell(ClassDef before,ClassDef after){
        String key=before.getType();
        req(key.equals(after.getType())&&before.getAccessFlags()==after.getAccessFlags()&&
            Objects.equals(before.getSuperclass(),after.getSuperclass())&&before.getInterfaces().equals(after.getInterfaces())&&
            Objects.equals(before.getSourceFile(),after.getSourceFile())&&before.getAnnotations().equals(after.getAnnotations()),
            "Class header or annotations changed "+key);
        var fields=new TreeMap<String,Field>();for(Field field:before.getFields())fields.put(field.toString(),field);
        var current=new TreeMap<String,Field>();for(Field field:after.getFields())current.put(field.toString(),field);
        req(fields.keySet().equals(current.keySet()),"Runtime/native field set changed "+key);
        for(var entry:fields.entrySet()){
            Field previous=entry.getValue(),next=current.get(entry.getKey());
            req(previous.getAccessFlags()==next.getAccessFlags()&&Objects.equals(previous.getInitialValue(),next.getInitialValue())&&
                previous.getAnnotations().equals(next.getAnnotations())&&previous.getHiddenApiRestrictions().equals(next.getHiddenApiRestrictions()),
                "Field metadata changed "+entry.getKey());
        }
    }
    static void methodHeader(Method before,Method after){
        String key=id(before);
        req(key.equals(id(after))&&before.getAccessFlags()==after.getAccessFlags()&&before.getAnnotations().equals(after.getAnnotations())&&
            before.getHiddenApiRestrictions().equals(after.getHiddenApiRestrictions()),"Method metadata changed "+key);
        req(before.getParameters().size()==after.getParameters().size(),"Method parameter count changed "+key);
        for(int i=0;i<before.getParameters().size();i++){
            var previous=before.getParameters().get(i);var next=after.getParameters().get(i);
            req(previous.getType().equals(next.getType())&&Objects.equals(previous.getName(),next.getName())&&previous.getAnnotations().equals(next.getAnnotations()),
                "Method parameter metadata changed "+key);
        }
    }
    static int parameterWords(Method method){
        int count=(method.getAccessFlags()&AccessFlags.STATIC.getValue())==0?1:0;
        for(CharSequence type:method.getParameterTypes())count+=type.toString().equals("J")||type.toString().equals("D")?2:1;
        return count;
    }
    static List<Instruction> instructions(Method method){
        var result=new ArrayList<Instruction>();
        if(method.getImplementation()!=null)method.getImplementation().getInstructions().forEach(result::add);
        return result;
    }
    static int[] registers(Instruction instruction){
        req(instruction.getOpcode()==Opcode.INVOKE_STATIC||instruction.getOpcode()==Opcode.INVOKE_STATIC_RANGE,
            "Approved hook must be a static invocation");
        if(instruction instanceof RegisterRangeInstruction range){
            int[] result=new int[range.getRegisterCount()];
            for(int i=0;i<result.length;i++)result[i]=range.getStartRegister()+i;
            return result;
        }
        req(instruction instanceof FiveRegisterInstruction,"Unexpected hook register format");
        var values=(FiveRegisterInstruction)instruction;
        return Arrays.copyOf(new int[]{values.getRegisterC(),values.getRegisterD(),values.getRegisterE(),values.getRegisterF(),values.getRegisterG()},values.getRegisterCount());
    }
    static void hook(Instruction instruction,String expected,int[] arguments,String method){
        req(ref(instruction).equals(expected),"Wrong inserted helper reference in "+method);
        req(Arrays.equals(registers(instruction),arguments),"Wrong inserted helper arguments in "+method);
    }
    static int unique(MutableMethodImplementation code,String reference,String method){
        int found=-1;
        for(int i=0;i<code.getInstructions().size();i++)if(ref(code.getInstructions().get(i)).equals(reference)){
            req(found==-1,"Duplicate approved reference in "+method);found=i;
        }
        req(found>=0,"Missing approved reference in "+method);return found;
    }
    static BuilderInstruction originalCall(Instruction current,MethodReference original){
        if(current instanceof RegisterRangeInstruction range){
            req(current.getOpcode()==Opcode.INVOKE_STATIC_RANGE,"Redirect changed invocation kind");
            return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,range.getStartRegister(),range.getRegisterCount(),original);
        }
        req(current instanceof FiveRegisterInstruction&&current.getOpcode()==Opcode.INVOKE_STATIC,"Redirect changed invocation kind");
        var values=(FiveRegisterInstruction)current;
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC,values.getRegisterCount(),values.getRegisterC(),values.getRegisterD(),values.getRegisterE(),values.getRegisterF(),values.getRegisterG(),original);
    }

    static Method undo(Method before,Method after){
        String key=id(after);req(after.getImplementation()!=null,"Changed method lacks code "+key);
        var code=new MutableMethodImplementation(after.getImplementation());
        if(before!=null)req(before.getImplementation()!=null&&before.getImplementation().getRegisterCount()==code.getRegisterCount(),
            "Unexpected register count change "+key);
        if(Transform1932.REDIRECTS.containsKey(key)){
            req(before!=null,"Redirect lacks baseline "+key);
            String[] pair=Transform1932.REDIRECTS.get(key);MethodReference original=null;int count=0;
            for(Instruction instruction:instructions(before))if(ref(instruction).equals(pair[0])){
                req(instruction instanceof ReferenceInstruction&&((ReferenceInstruction)instruction).getReference() instanceof MethodReference,
                    "Redirect anchor is not a method");
                original=(MethodReference)((ReferenceInstruction)instruction).getReference();count++;
            }
            req(count==1&&original!=null,"Original redirect anchor is not unique "+key);
            int index=unique(code,pair[1],key);
            code.replaceInstruction(index,originalCall(code.getInstructions().get(index),original));
        }else if(Transform1932.ENTRY.containsKey(key)){
            var expected=Transform1932.ENTRY.get(key);
            req(code.getRegisterCount()==expected.originalRegisters,"Entry register ABI differs "+key);
            req(unique(code,expected.reference,key)==0,"Entry hook is not first "+key);
            hook(code.getInstructions().get(0),expected.reference,expected.registers,key);
            code.removeInstruction(0);
        }else if(Transform1932.OBSERVE.containsKey(key)){
            var expected=Transform1932.OBSERVE.get(key);
            req(code.getRegisterCount()==expected.hook.originalRegisters,"Observer register ABI differs "+key);
            int index=unique(code,expected.hook.reference,key);
            req(index>=2,"Observer missing captured result "+key);
            hook(code.getInstructions().get(index),expected.hook.reference,expected.hook.registers,key);
            req(ref(code.getInstructions().get(index-2)).equals(expected.anchor),"Observer moved away from native result "+key);
            Instruction result=code.getInstructions().get(index-1);
            req(result.getOpcode()==expected.resultOpcode&&result instanceof OneRegisterInstruction&&
                ((OneRegisterInstruction)result).getRegisterA()==expected.resultRegister,"Observer result register differs "+key);
            req(unique(code,expected.anchor,key)==index-2,"Observer anchor is not unique "+key);
            code.removeInstruction(index);
        }else if(Transform1932.INVALIDATE.contains(key)){
            String call=SHOT+"->invalidateBeauty(Ljava/lang/Object;)V";
            req((after.getAccessFlags()&AccessFlags.STATIC.getValue())==0,"Composer invalidation must receive the recorder instance");
            req(unique(code,call,key)==0,"Composer invalidation is not first "+key);
            hook(code.getInstructions().get(0),call,new int[]{code.getRegisterCount()-parameterWords(after)},key);
            code.removeInstruction(0);
        }else if(key.equals(Transform1932.LEGACY_SETTINGS)){
            String call=PIPE+"->settingsForLegacy("+SETTINGS+")"+SETTINGS;
            int index=unique(code,call,key);
            req(index>0&&index+1<code.getInstructions().size(),"Legacy override is incomplete");
            Instruction read=code.getInstructions().get(index-1),result=code.getInstructions().get(index+1);
            req(read.getOpcode()==Opcode.SGET_OBJECT&&ref(read).equals(SETTINGS_FIELD),"Legacy override must follow actual settings read");
            int register=((OneRegisterInstruction)read).getRegisterA();
            hook(code.getInstructions().get(index),call,new int[]{register},key);
            req(result.getOpcode()==Opcode.MOVE_RESULT_OBJECT&&((OneRegisterInstruction)result).getRegisterA()==register,
                "Legacy settings result must replace the same register");
            code.removeInstruction(index+1);code.removeInstruction(index);
        }else if(key.equals(Transform1932.STATE_CTOR)){
            String call=PIPE+"->stateCreated("+STATE+")V";
            int index=unique(code,call,key);
            req(index+1<code.getInstructions().size()&&code.getInstructions().get(index+1).getOpcode()==Opcode.RETURN_VOID,
                "State snapshot hook must be immediately before constructor return");
            hook(code.getInstructions().get(index),call,new int[]{code.getRegisterCount()-parameterWords(after)},key);
            req(after.getName().equals("<init>"),"State hook must target a constructor");
            code.removeInstruction(index);
        }else throw new IllegalStateException("No independent reversal for "+key);
        Method restored=body(after,code);
        if(before!=null)req(MergePayloads.hash(before).equals(MergePayloads.hash(restored)),"Unapproved original-body change after reversing hook "+key);
        return restored;
    }

    static void alias(Method method,String field){
        req(method!=null,"Missing alias "+field);
        req((method.getAccessFlags()&(AccessFlags.PUBLIC.getValue()|AccessFlags.STATIC.getValue()))==9,"Alias must be public static");
        req(method.getParameterTypes().isEmpty(),"Alias must not take arguments");
        req(method.getImplementation()!=null&&method.getImplementation().getRegisterCount()==1,"Alias register count");
        List<Instruction> code=instructions(method);req(code.size()==2,"Alias must only read the private field and return it");
        boolean object=!method.getReturnType().equals("Z");
        req(code.get(0).getOpcode()==(object?Opcode.SGET_OBJECT:Opcode.SGET_BOOLEAN)&&ref(code.get(0)).equals(field),"Alias reads wrong field");
        req(code.get(1).getOpcode()==(object?Opcode.RETURN_OBJECT:Opcode.RETURN),"Alias has wrong return type");
        req(((OneRegisterInstruction)code.get(0)).getRegisterA()==0&&((OneRegisterInstruction)code.get(1)).getRegisterA()==0,
            "Alias must return its field value directly");
    }

    static int loader(Path baseline,Path emitted)throws Exception{
        var old=MergePayloads.classes(baseline.toString());var next=MergePayloads.classes(emitted.toString());
        req(old.keySet().equals(next.keySet()),"Patch-loader class inventory changed");
        int metadata=0,other=0;
        for(var entry:old.entrySet()){
            String type=entry.getKey();ClassDef current=next.get(type);
            if(!type.equals(LOADER)){
                req(MergePayloads.classHash(entry.getValue()).equals(MergePayloads.classHash(current)),"Unrelated loader class changed "+type);
                if(!type.startsWith("Lapp/hiro/ulike/patches/"))other++;
                continue;
            }
            var originalMethods=MergePayloads.methods(List.of(entry.getValue()));
            var currentMethods=MergePayloads.methods(List.of(current));
            req(originalMethods.keySet().equals(currentMethods.keySet()),"ULike loader method set changed");
            var restoredMethods=new ArrayList<Method>();
            classShell(entry.getValue(),current);
            for(Method method:current.getMethods()){
                Method previous=originalMethods.get(id(method));
                methodHeader(previous,method);
                if(MergePayloads.hash(previous).equals(MergePayloads.hash(method))){restoredMethods.add(method);continue;}
                var code=new MutableMethodImplementation(method.getImplementation());
                String originalDescription=null;int previousHits=0;
                for(Instruction instruction:instructions(previous))if(instruction instanceof ReferenceInstruction reference&&reference.getReference() instanceof StringReference string&&
                        string.getString().startsWith("v1.9.31（v1.9.30基準）")){
                    originalDescription=string.getString();previousHits++;
                }
                req(previousHits==1&&originalDescription!=null,"Original ULike description is not unique");
                int hits=0;
                for(int i=0;i<code.getInstructions().size();i++){
                    Instruction instruction=code.getInstructions().get(i);
                    if(instruction instanceof ReferenceInstruction reference&&reference.getReference() instanceof StringReference string&&
                            string.getString().equals(Transform1932.DESCRIPTION)){
                        req(instruction.getOpcode()==Opcode.CONST_STRING,"Unexpected metadata opcode");
                        code.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)instruction).getRegisterA(),
                            new ImmutableStringReference(originalDescription)));hits++;
                    }
                }
                req(hits==1,"Exactly one ULike description may change");metadata+=hits;
                Method restored=body(method,code);
                req(MergePayloads.hash(previous).equals(MergePayloads.hash(restored)),"ULike loader changed beyond metadata");
                restoredMethods.add(restored);
            }
            req(canonicalClassHash(entry.getValue()).equals(canonicalClassHash(methods(current,restoredMethods))),
                "ULike loader class metadata/fields changed");
        }
        req(metadata==1,"Exactly one ULike metadata string must be updated per loader DEX");return other;
    }

    static Set<String> inventory(String json,String key){
        Matcher matcher=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*\\[([^]]*)]",Pattern.DOTALL).matcher(json);
        req(matcher.find(),"Missing emitted inventory field "+key);String body=matcher.group(1);req(!matcher.find(),"Duplicate inventory key "+key);
        var out=new TreeSet<String>();
        Matcher quoted=Pattern.compile("\\\"([^\\\"]*)\\\"").matcher(body);
        int end=0;
        while(quoted.find()){
            req(body.substring(end,quoted.start()).matches("[\\s,]*"),"Malformed inventory "+key);
            req(out.add(quoted.group(1)),"Duplicate inventory row "+key);end=quoted.end();
        }
        req(body.substring(end).matches("[\\s,]*"),"Malformed trailing inventory "+key);return out;
    }

    public static void main(String[] args)throws Exception{
        req(args.length==2||args.length==3,"BASE EMITTED [STOCK_APK]");
        Path baseline=Path.of(args[0]),emitted=Path.of(args[1]);
        req(Transform1932.ALIASES.equals(ALIASES)&&Transform1932.HELPER.equals(HELPER),"Reviewed helper and alias boundaries differ");
        var oldNativeClasses=MergePayloads.classes(baseline.resolve("ulike/methods.dex").toString());
        var newNativeClasses=MergePayloads.classes(emitted.resolve("methods.dex").toString());
        var oldNative=MergePayloads.methods(oldNativeClasses.values());var nextNative=MergePayloads.methods(newNativeClasses.values());
        var oldRows=MergePayloads.contracts(baseline.resolve("ulike/methods.tsv").toString());
        var rows=MergePayloads.contracts(emitted.resolve("methods.tsv").toString());
        req(oldNative.keySet().equals(oldRows.keySet())&&nextNative.keySet().equals(rows.keySet()),"Native contract inventory mismatch");
        req(nextNative.keySet().containsAll(oldNative.keySet()),"Existing native method removed");
        var addedNative=new TreeSet<>(nextNative.keySet());addedNative.removeAll(oldNative.keySet());
        req(addedNative.equals(Set.of(WATERMARK)),"Only the reviewed original watermark helper may become newly patched");
        var expectedNativeClasses=new TreeSet<>(oldNativeClasses.keySet());
        expectedNativeClasses.add(WATERMARK.substring(0,WATERMARK.indexOf("->")));
        req(newNativeClasses.keySet().equals(expectedNativeClasses),"Unexpected native holder class added or removed");
        req(rows.get(WATERMARK)[1].equals(WATERMARK_HASH),"Watermark original hash differs from independently pinned stock method");
        var changedNative=new TreeSet<String>();var restoredNative=new TreeMap<String,Method>();int nativeKept=0;
        for(var entry:nextNative.entrySet()){
            String key=entry.getKey();Method before=oldNative.get(key),after=entry.getValue();
            req(MergePayloads.hash(after).equals(rows.get(key)[2]),"Emitted native contract hash differs "+key);
            if(before!=null){
                methodHeader(before,after);
                req(MergePayloads.hash(before).equals(oldRows.get(key)[2]),"Baseline native contract differs "+key);
                req(rows.get(key)[1].equals(oldRows.get(key)[1]),"Original APK contract overwritten "+key);
            }
            if(before==null||!MergePayloads.hash(before).equals(MergePayloads.hash(after))){
                req(Transform1932.NATIVE.contains(key),"Unexpected native method change "+key);changedNative.add(key);
                Method restored=undo(before,after);
                if(before==null)req(MergePayloads.hash(restored).equals(WATERMARK_HASH),"New watermark hook changed original processing");
                restoredNative.put(key,restored);
            }else{nativeKept++;restoredNative.put(key,after);}
        }
        req(changedNative.equals(Transform1932.NATIVE),"Native reviewed change set differs from observed changes");
        for(var entry:oldNativeClasses.entrySet()){
            ClassDef current=newNativeClasses.get(entry.getKey());req(current!=null,"Native holder class removed");
            classShell(entry.getValue(),current);
            boolean touched=false;for(Method method:current.getMethods())touched|=changedNative.contains(id(method));
            if(!touched){
                req(MergePayloads.classHash(entry.getValue()).equals(MergePayloads.classHash(current)),"Untouched native holder bytes changed "+entry.getKey());
                continue;
            }
            var restored=new ArrayList<Method>();
            for(Method method:current.getMethods())if(!addedNative.contains(id(method)))restored.add(restoredNative.get(id(method)));
            req(canonicalClassHash(entry.getValue()).equals(canonicalClassHash(methods(current,restored))),"Native holder metadata changed "+entry.getKey());
        }
        var oldRuntime=MergePayloads.classes(baseline.resolve("ulike/runtime.dex").toString());
        var nextRuntime=MergePayloads.classes(emitted.resolve("runtime.dex").toString());
        req(nextRuntime.keySet().containsAll(oldRuntime.keySet()),"Existing runtime class removed");
        var helpers=new TreeSet<>(nextRuntime.keySet());helpers.removeAll(oldRuntime.keySet());var roots=new TreeSet<String>();
        for(String helper:helpers){req(helper.matches(HELPER),"Unexpected helper or test class "+helper);if(!helper.contains("$"))roots.add(helper);}
        req(roots.equals(ROOTS),"Exactly four new production helper roots are required");
        var oldMethods=MergePayloads.methods(oldRuntime.values());var newMethods=MergePayloads.methods(nextRuntime.values());
        req(newMethods.keySet().containsAll(oldMethods.keySet()),"Runtime method removed");
        var newExisting=new TreeSet<String>();
        for(String key:newMethods.keySet())if(!oldMethods.containsKey(key)&&!helpers.contains(newMethods.get(key).getDefiningClass()))newExisting.add(key);
        req(newExisting.equals(ALIASES.keySet()),"Unexpected new member in an existing runtime class");
        for(var entry:ALIASES.entrySet())alias(newMethods.get(entry.getKey()),entry.getValue());
        var changedRuntime=new TreeSet<String>();var restoredRuntime=new TreeMap<String,Method>();int runtimeKept=0;
        for(var entry:oldMethods.entrySet()){
            String key=entry.getKey();Method before=entry.getValue(),after=newMethods.get(key);
            methodHeader(before,after);
            if(!MergePayloads.hash(before).equals(MergePayloads.hash(after))){
                req(Transform1932.RUNTIME.contains(key),"Unexpected runtime change "+key);changedRuntime.add(key);
                restoredRuntime.put(key,undo(before,after));
            }else{runtimeKept++;restoredRuntime.put(key,after);}
        }
        req(changedRuntime.equals(Transform1932.RUNTIME),"Runtime reviewed change set differs from observed changes");
        for(var entry:oldRuntime.entrySet()){
            ClassDef current=nextRuntime.get(entry.getKey());var restored=new ArrayList<Method>();
            classShell(entry.getValue(),current);
            boolean touched=false;for(Method method:current.getMethods())touched|=changedRuntime.contains(id(method))||ALIASES.containsKey(id(method));
            if(!touched){
                req(MergePayloads.classHash(entry.getValue()).equals(MergePayloads.classHash(current)),"Untouched runtime class bytes changed "+entry.getKey());
                continue;
            }
            for(Method method:current.getMethods())if(!ALIASES.containsKey(id(method)))restored.add(restoredRuntime.get(id(method)));
            req(canonicalClassHash(entry.getValue()).equals(canonicalClassHash(methods(current,restored))),
                "Runtime class shell, fields, annotations or original methods changed "+entry.getKey());
        }
        for(String name:List.of("FrontPreview1931","RearRestart1926","FacingMemory1928","RearLensUi1930","PreviewStart1927",
                "ProviderLifecycle1929","PreviewInputs1929","OpticalZoom","ManualLens170","ExitBusy1921")){
            String type=PREFIX+name+";";
            req(oldRuntime.containsKey(type)&&MergePayloads.classHash(oldRuntime.get(type)).equals(MergePayloads.classHash(nextRuntime.get(type))),
                "Existing camera lifecycle/UI class changed "+type);
        }
        int standaloneOther=loader(baseline.resolve("classes.dex"),emitted.resolve("loader.dex"));
        req(standaloneOther==0,"Standalone loader contains another app");
        int bundleOther=loader(baseline.resolve("bundle.dex"),emitted.resolve("bundle-loader.dex"));
        req(bundleOther>0,"Integrated loader verification lacks other applications");
        String report=Files.readString(emitted.resolve("quality-inventory.json"));
        req(inventory(report,"changed_runtime_methods").equals(changedRuntime),"Runtime inventory does not describe emitted bytes");
        req(inventory(report,"changed_native_methods").equals(changedNative),"Native inventory does not describe emitted bytes");
        req(inventory(report,"new_helper_classes").equals(helpers),"Helper inventory does not describe emitted bytes");
        req(inventory(report,"new_runtime_aliases").equals(ALIASES.keySet()),"Alias inventory does not describe emitted bytes");
        req(inventory(report,"new_native_methods").equals(addedNative),"New native inventory does not describe emitted bytes");
        int stockContracts=0;
        if(args.length==3){
            var stock=MergePayloads.methods(MergePayloads.classes(args[2]).values());
            for(var entry:rows.entrySet()){
                Method method=stock.get(entry.getKey());req(method!=null,"Original APK method missing "+entry.getKey());
                req(MergePayloads.hash(method).equals(entry.getValue()[1]),"Original APK hash contract differs "+entry.getKey());stockContracts++;
            }
            req(MergePayloads.hash(stock.get(WATERMARK)).equals(MergePayloads.hash(restoredNative.get(WATERMARK))),"Watermark reversal differs from full original APK");
        }
        System.out.println("PASS independent quality DEX verification: assertions="+assertions+", runtime_changed="+changedRuntime.size()+
            ", runtime_retained="+runtimeKept+", native_changed="+changedNative.size()+", native_retained="+nativeKept+
            ", new_native="+addedNative.size()+", helpers="+helpers.size()+", aliases="+ALIASES.size()+
            ", other_app_loader_classes="+bundleOther+", original_APK_contracts="+stockContracts+". Camera lifecycle retained; device untested.");
    }
}
