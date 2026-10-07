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
public final class VerifyBurst1933 {
    static final String PREFIX="Lcom/hiro/ulike/", B="Landroid/graphics/Bitmap;";
    static final String BURST=PREFIX+"BurstCapture1933;", SHOT=PREFIX+"ShotContext1932;";
    static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
    static final String HELPER="Lcom/hiro/ulike/(FusionPixels1933|BurstCapture1933|CapturePolicy1933|FastPixels1933|FastResize1933)(\\$[^;]+)?;";
    static final Set<String> ROOTS=Set.of(BURST,PREFIX+"FusionPixels1933;",PREFIX+"CapturePolicy1933;",PREFIX+"FastPixels1933;",PREFIX+"FastResize1933;");
    static final String Q0="Li/s/a/w/d0/a;->Q0(Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)V";
    static final String U0="Li/s/a/w/d0/a;->u0(Lcom/ss/android/ttvecamera/TECameraSettings$m;I)V";
    static final String STILL="Li/s/a/w/d0/a$g;->onImageAvailable(Landroid/media/ImageReader;)V";
    static final String RECEIVE=PREFIX+"CaptureYuv;->receive(Ljava/lang/Object;"+PREFIX+"CaptureYuv$State;Landroid/media/ImageReader;)V";
    static final String RELEASE=PREFIX+"CaptureYuv;->release(Ljava/lang/Object;)V";
    static final String FAILED=PREFIX+"CaptureYuv;->failed(Ljava/lang/Object;)V";
    static final String RESULT=PREFIX+"CaptureYuv;->result(Ljava/lang/Object;Landroid/hardware/camera2/CaptureResult;)V";
    static final String NORMALIZE=PREFIX+"QualityPipeline1932;->normalize("+B+"IZ)"+B;
    static final String OLD_RESIZE=PREFIX+"QualityPipeline1932;->resample("+B+"III)"+B;
    static final String NEW_RESIZE=PREFIX+"FastResize1933;->resample("+B+"III)"+B;
    static final String ALIAS=SHOT+"->receivedValues1933(Ljava/lang/Object;Ljava/lang/Object;JIILandroid/hardware/camera2/CaptureResult;)Z";
    static final String ACQUIRE="Landroid/media/ImageReader;->acquireNextImage()Landroid/media/Image;";
    static final String ACQUIRE_YUV=BURST+"->acquireYuv(Ljava/lang/Object;Ljava/lang/Object;Landroid/media/ImageReader;)Landroid/media/Image;";
    static final String ACQUIRE_STILL=BURST+"->acquireStill(Ljava/lang/Object;Landroid/media/ImageReader;)Landroid/media/Image;";
    static final String SEED_HASH="0859c613c32a4a1a3913adf8174a931d43b53d1e44a57101b8f239f529ad00b1";
    static final Set<String> NATIVE=Set.of(Q0,U0,STILL), RUNTIME=Set.of(RECEIVE,RELEASE,FAILED,RESULT,NORMALIZE), ALIASES=Set.of(ALIAS);
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
        String key=id(after);req(after.getImplementation()!=null,"Changed method lacks body");
        var code=new MutableMethodImplementation(after.getImplementation());
        if(before!=null)req(before.getImplementation().getRegisterCount()==code.getRegisterCount(),"Register ABI changed "+key);
        if(key.equals(NORMALIZE)){
            req(before!=null,"Resize redirect requires baseline");MethodReference previous=null;int oldCount=0,hits=0;
            for(Instruction instruction:instructions(before))if(ref(instruction).equals(OLD_RESIZE)){previous=(MethodReference)((ReferenceInstruction)instruction).getReference();oldCount++;}
            req(oldCount==2,"Expected two original resize calls");
            for(int i=0;i<code.getInstructions().size();i++)if(ref(code.getInstructions().get(i)).equals(NEW_RESIZE)){
                code.replaceInstruction(i,originalCall(code.getInstructions().get(i),previous));hits++;
            }req(hits==2,"Expected two fast resize redirects");
        }else if(key.equals(U0)||key.equals(RELEASE)||key.equals(FAILED)){
            String call=BURST+"->canceled(Ljava/lang/Object;)V";int owner=key.equals(U0)?12:key.equals(RELEASE)?3:4;
            req(code.getRegisterCount()==(key.equals(U0)?15:key.equals(RELEASE)?4:5),"Cancel register ABI");
            req(unique(code,call,key)==0,"Cancel must precede all original operations");hook(code.getInstructions().get(0),call,new int[]{owner},key);code.removeInstruction(0);
        }else if(key.equals(RESULT)){
            String call=BURST+"->observedResult(Ljava/lang/Object;Landroid/hardware/camera2/CaptureResult;)V";
            req(code.getRegisterCount()==4&&unique(code,call,key)==0,"Result observer ABI/position");
            hook(code.getInstructions().get(0),call,new int[]{2,3},key);code.removeInstruction(0);
        }else if(key.equals(Q0)||key.equals(STILL)||key.equals(RECEIVE)){
            String call=BURST+(key.equals(Q0)?"->beginImage(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)Z":
                key.equals(STILL)?"->stillImage(Ljava/lang/Object;Landroid/media/Image;)Z":"->yuvImage(Ljava/lang/Object;Ljava/lang/Object;Landroid/media/Image;)Z");
            int[] arguments=key.equals(Q0)?new int[]{12,13,14}:key.equals(STILL)?new int[]{4,5}:new int[]{4,5,6};
            int scratch=key.equals(RECEIVE)?1:0;int expected=key.equals(Q0)?15:key.equals(STILL)?6:7;
            req(code.getRegisterCount()==expected,"Ownership hook register ABI "+key);
            int index=unique(code,call,key);hook(code.getInstructions().get(index),call,arguments,key);
            if(key.equals(Q0))req(index==0,"beginImage must precede old metadata association");
            else {
                String acquire=key.equals(STILL)?ACQUIRE_STILL:ACQUIRE_YUV;
                req(index>=2&&unique(code,acquire,key)==index-2,"Hook must immediately follow actual full-resolution acquisition");
                hook(code.getInstructions().get(index-2),acquire,key.equals(STILL)?new int[]{4,5}:new int[]{4,5,6},key);
                Instruction move=code.getInstructions().get(index-1);req(move.getOpcode()==Opcode.MOVE_RESULT_OBJECT&&((OneRegisterInstruction)move).getRegisterA()==expected-1,"Acquired Image argument differs");
            }
            req(index+4<code.getInstructions().size(),"Ownership guard truncated");
            Instruction move=code.getInstructions().get(index+1),branch=code.getInstructions().get(index+2),ret=code.getInstructions().get(index+3);
            req(move.getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)move).getRegisterA()==scratch,"Ownership result register differs");
            req(branch.getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)branch).getRegisterA()==scratch,"Ownership branch polarity or register differs");
            req(((OffsetInstruction)branch).getCodeOffset()==branch.getCodeUnits()+ret.getCodeUnits(),"False must resume exactly at original continuation");
            req(ret.getOpcode()==Opcode.RETURN_VOID,"Owned Image must bypass original delivery");
            for(int i=0;i<4;i++)code.removeInstruction(index);
            if(!key.equals(Q0))code.replaceInstruction(index-2,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,1,expected-1,0,0,0,0,
                new ImmutableMethodReference("Landroid/media/ImageReader;","acquireNextImage",List.of(),"Landroid/media/Image;")));
        }else throw new IllegalStateException("No independent reversal for "+key);
        Method restored=body(after,code);
        if(before!=null)req(MergePayloads.hash(before).equals(MergePayloads.hash(restored)),"Original body, branches, or try ranges changed after reversing approved hook "+key);
        return restored;
    }

    static void metadataAlias(Method method){
        req(method!=null&&method.getAccessFlags()==9&&method.getReturnType().equals("Z"),"Metadata alias must be public static boolean");
        req(id(method).equals(ALIAS)&&method.getImplementation()!=null,"Exact metadata alias signature");
        Set<String> references=new TreeSet<>();for(Instruction instruction:instructions(method))references.add(ref(instruction));
        req(references.contains("Landroid/hardware/camera2/CaptureResult;->SENSOR_TIMESTAMP:Landroid/hardware/camera2/CaptureResult$Key;"),"Alias must validate actual sensor timestamp");
        req(references.contains(SHOT+"->selectResult(Landroid/hardware/camera2/CaptureResult;Ljava/lang/String;)Landroid/hardware/camera2/CaptureResult;"),"Alias must resolve the selected physical sensor");
        req(references.contains(SHOT+"->field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"),"Alias must validate actual owner callback");
        req(references.contains(SHOT+"->bind(Ljava/lang/Object;"+PREFIX+"ShotContext1932$Shot;)V"),"Alias must bind existing shot record");
        for(String reference:references)req(!reference.startsWith("Landroid/media/Image;"),"Alias must not retain a closed Image");
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
                        string.getString().startsWith("v1.9.32（v1.9.31基準）")){
                    originalDescription=string.getString();previousHits++;
                }
                req(previousHits==1&&originalDescription!=null,"Original ULike description is not unique");
                int hits=0;
                for(int i=0;i<code.getInstructions().size();i++){
                    Instruction instruction=code.getInstructions().get(i);
                    if(instruction instanceof ReferenceInstruction reference&&reference.getReference() instanceof StringReference string&&
                            string.getString().equals(Transform1933.DESCRIPTION)){
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
        req(args.length==2||args.length==3,"BASE EMITTED [STOCK_APK]");Path baseline=Path.of(args[0]),emitted=Path.of(args[1]);
        req(Transform1933.NATIVE.equals(NATIVE)&&Transform1933.RUNTIME.equals(RUNTIME)&&Transform1933.HELPER.equals(HELPER)&&Transform1933.ALIAS.equals(ALIAS),"Transform differs from independently reviewed boundaries");
        var oldNativeClasses=MergePayloads.classes(baseline.resolve("ulike/methods.dex").toString());var nextNativeClasses=MergePayloads.classes(emitted.resolve("methods.dex").toString());
        var oldNative=MergePayloads.methods(oldNativeClasses.values());var nextNative=MergePayloads.methods(nextNativeClasses.values());
        var oldRows=MergePayloads.contracts(baseline.resolve("ulike/methods.tsv").toString());var rows=MergePayloads.contracts(emitted.resolve("methods.tsv").toString());
        req(oldNative.keySet().equals(oldRows.keySet())&&nextNative.keySet().equals(rows.keySet()),"Native contract inventories differ");
        req(nextNative.keySet().containsAll(oldNative.keySet()),"Existing native method removed");
        var addedNative=new TreeSet<>(nextNative.keySet());addedNative.removeAll(oldNative.keySet());req(addedNative.equals(Set.of(STILL)),"Only the actual still reader callback may be newly patched");
        var expectedClasses=new TreeSet<>(oldNativeClasses.keySet());expectedClasses.add(STILL.substring(0,STILL.indexOf("->")));
        req(nextNativeClasses.keySet().equals(expectedClasses),"Unexpected native holder classes");req(rows.get(STILL)[1].equals(SEED_HASH),"New hook original hash is not independently pinned");
        var changedNative=new TreeSet<String>();var restoredNative=new TreeMap<String,Method>();int nativeKept=0;
        for(var entry:nextNative.entrySet()){
            String key=entry.getKey();Method before=oldNative.get(key),after=entry.getValue();req(MergePayloads.hash(after).equals(rows.get(key)[2]),"Native emitted hash differs "+key);
            if(before!=null){methodHeader(before,after);req(MergePayloads.hash(before).equals(oldRows.get(key)[2]),"Baseline native hash differs");req(rows.get(key)[1].equals(oldRows.get(key)[1]),"Original APK contract overwritten");}
            if(before==null||!MergePayloads.hash(before).equals(MergePayloads.hash(after))){
                req(NATIVE.contains(key),"Unexpected native method change "+key);changedNative.add(key);Method restored=undo(before,after);
                if(before==null)req(MergePayloads.hash(restored).equals(SEED_HASH),"New still callback body changed beyond ownership guard");restoredNative.put(key,restored);
            }else{nativeKept++;restoredNative.put(key,after);}
        }req(changedNative.equals(NATIVE),"Observed native change set differs");
        for(var entry:oldNativeClasses.entrySet()){
            ClassDef next=nextNativeClasses.get(entry.getKey());classShell(entry.getValue(),next);var restored=new ArrayList<Method>();
            for(Method method:next.getMethods())if(!addedNative.contains(id(method)))restored.add(restoredNative.get(id(method)));
            req(canonicalClassHash(entry.getValue()).equals(canonicalClassHash(methods(next,restored))),"Native class shell or original bodies changed "+entry.getKey());
        }
        var oldRuntime=MergePayloads.classes(baseline.resolve("ulike/runtime.dex").toString());var nextRuntime=MergePayloads.classes(emitted.resolve("runtime.dex").toString());
        req(nextRuntime.keySet().containsAll(oldRuntime.keySet()),"Runtime class removed");var helpers=new TreeSet<>(nextRuntime.keySet());helpers.removeAll(oldRuntime.keySet());var roots=new TreeSet<String>();
        for(String helper:helpers){req(helper.matches(HELPER),"Unexpected helper/test/stub class "+helper);if(!helper.contains("$"))roots.add(helper);}req(roots.equals(ROOTS),"Exactly five production helper roots required");
        var oldMethods=MergePayloads.methods(oldRuntime.values());var newMethods=MergePayloads.methods(nextRuntime.values());req(newMethods.keySet().containsAll(oldMethods.keySet()),"Existing runtime method removed");
        var newExisting=new TreeSet<String>();for(String key:newMethods.keySet())if(!oldMethods.containsKey(key)&&!helpers.contains(newMethods.get(key).getDefiningClass()))newExisting.add(key);
        req(newExisting.equals(ALIASES),"Unreviewed member in an existing runtime class");metadataAlias(newMethods.get(ALIAS));
        var changedRuntime=new TreeSet<String>();var restoredRuntime=new TreeMap<String,Method>();int runtimeKept=0;
        for(var entry:oldMethods.entrySet()){
            String key=entry.getKey();Method before=entry.getValue(),after=newMethods.get(key);methodHeader(before,after);
            if(!MergePayloads.hash(before).equals(MergePayloads.hash(after))){req(RUNTIME.contains(key),"Unexpected runtime change "+key);changedRuntime.add(key);restoredRuntime.put(key,undo(before,after));}
            else{runtimeKept++;restoredRuntime.put(key,after);}
        }req(changedRuntime.equals(RUNTIME),"Observed runtime change set differs");
        for(var entry:oldRuntime.entrySet()){
            ClassDef next=nextRuntime.get(entry.getKey());classShell(entry.getValue(),next);var restored=new ArrayList<Method>();
            for(Method method:next.getMethods())if(!ALIASES.contains(id(method)))restored.add(restoredRuntime.get(id(method)));
            req(canonicalClassHash(entry.getValue()).equals(canonicalClassHash(methods(next,restored))),"Runtime class shell or original methods changed "+entry.getKey());
        }
        int protectedClasses=0;
        for(String name:List.of("FrontPreview1931","RearRestart1926","FacingMemory1928","RearLensUi1930","PreviewStart1927","ProviderLifecycle1929","PreviewInputs1929","OpticalZoom","ManualLens170","ExitBusy1921")){
            String root=PREFIX+name+";";req(oldRuntime.containsKey(root),"Missing protected root "+root);
            for(var entry:oldRuntime.entrySet())if(entry.getKey().equals(root)||entry.getKey().startsWith(PREFIX+name+"$")){
                req(MergePayloads.classHash(entry.getValue()).equals(MergePayloads.classHash(nextRuntime.get(entry.getKey()))),"Protected startup/recovery bytes differ "+entry.getKey());protectedClasses++;
            }
        }
        int standaloneOther=loader(baseline.resolve("classes.dex"),emitted.resolve("loader.dex"));req(standaloneOther==0,"Standalone contains another app");
        int bundleOther=loader(baseline.resolve("bundle.dex"),emitted.resolve("bundle-loader.dex"));req(bundleOther>0,"Integrated loader missing other applications");
        String report=Files.readString(emitted.resolve("burst-inventory.json"));
        req(inventory(report,"changed_runtime_methods").equals(changedRuntime),"Runtime inventory does not describe bytes");
        req(inventory(report,"changed_native_methods").equals(changedNative),"Native inventory does not describe bytes");
        req(inventory(report,"new_helper_classes").equals(helpers),"Helper inventory does not describe bytes");
        req(inventory(report,"new_runtime_aliases").equals(ALIASES),"Alias inventory does not describe bytes");
        req(inventory(report,"new_native_methods").equals(addedNative),"New native inventory does not describe bytes");
        req(report.contains("\"camera_startup_recovery_byte_identical\":true"),"Missing proven startup/recovery retention");
        int stockContracts=0;if(args.length==3){
            var stock=MergePayloads.methods(MergePayloads.classes(args[2]).values());for(var entry:rows.entrySet()){
                Method method=stock.get(entry.getKey());req(method!=null&&MergePayloads.hash(method).equals(entry.getValue()[1]),"Original APK method contract differs "+entry.getKey());stockContracts++;
            }req(MergePayloads.hash(stock.get(STILL)).equals(MergePayloads.hash(restoredNative.get(STILL))),"Still callback reversal differs from actual APK");
        }
        System.out.println("PASS independent burst DEX verification: assertions="+assertions+", runtime_changed="+changedRuntime.size()+", runtime_retained="+runtimeKept+
            ", native_changed="+changedNative.size()+", native_retained="+nativeKept+", new_native="+addedNative.size()+", helpers="+helpers.size()+", aliases=1, protected_startup_recovery_classes="+protectedClasses+
            ", other_app_loader_classes="+bundleOther+", original_APK_contracts="+stockContracts+". Device untested.");
    }
}
