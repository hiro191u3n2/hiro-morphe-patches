import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Exact hooks: real still-frame ownership, cancellation and output-identical resize. */
public final class Transform1933 {
    static final String P="Lcom/hiro/ulike/", B="Landroid/graphics/Bitmap;";
    static final String BURST=P+"BurstCapture1933;", SHOT=P+"ShotContext1932;";
    static final String HELPER="Lcom/hiro/ulike/(FusionPixels1933|BurstCapture1933|CapturePolicy1933|FastPixels1933|FastResize1933)(\\$[^;]+)?;";
    static final Set<String> ROOTS=Set.of(BURST,P+"FusionPixels1933;",P+"CapturePolicy1933;",P+"FastPixels1933;",P+"FastResize1933;");
    static final String Q0="Li/s/a/w/d0/a;->Q0(Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)V";
    static final String U0="Li/s/a/w/d0/a;->u0(Lcom/ss/android/ttvecamera/TECameraSettings$m;I)V";
    static final String STILL="Li/s/a/w/d0/a$g;->onImageAvailable(Landroid/media/ImageReader;)V";
    static final String RECEIVE=P+"CaptureYuv;->receive(Ljava/lang/Object;"+P+"CaptureYuv$State;Landroid/media/ImageReader;)V";
    static final String RELEASE=P+"CaptureYuv;->release(Ljava/lang/Object;)V";
    static final String FAILED=P+"CaptureYuv;->failed(Ljava/lang/Object;)V";
    static final String RESULT=P+"CaptureYuv;->result(Ljava/lang/Object;Landroid/hardware/camera2/CaptureResult;)V";
    static final String NORMALIZE=P+"QualityPipeline1932;->normalize("+B+"IZ)"+B;
    static final String OLD_RESIZE=P+"QualityPipeline1932;->resample("+B+"III)"+B;
    static final String NEW_RESIZE=P+"FastResize1933;->resample("+B+"III)"+B;
    static final String ALIAS=SHOT+"->receivedValues1933(Ljava/lang/Object;Ljava/lang/Object;JIILandroid/hardware/camera2/CaptureResult;)Z";
    static final String ACQUIRE="Landroid/media/ImageReader;->acquireNextImage()Landroid/media/Image;";
    static final String ACQUIRE_YUV=BURST+"->acquireYuv(Ljava/lang/Object;Ljava/lang/Object;Landroid/media/ImageReader;)Landroid/media/Image;";
    static final String ACQUIRE_STILL=BURST+"->acquireStill(Ljava/lang/Object;Landroid/media/ImageReader;)Landroid/media/Image;";
    static final String SEED_HASH="0859c613c32a4a1a3913adf8174a931d43b53d1e44a57101b8f239f529ad00b1";
    static final String OLD_DESCRIPTION="v1.9.32（v1.9.31基準）";
    static final String DESCRIPTION="v1.9.33（v1.9.32基準）候補47・50・60を統合。同露出の実フレーム合成、実測した動きに応じた夜景露出と枚数、画素一致を検証した高品質リサイズ高速化。既存のカメラ復帰修正と他アプリ保持。実機未確認。";
    static final Set<String> NATIVE=Set.of(Q0,U0,STILL);
    static final Set<String> RUNTIME=Set.of(RECEIVE,RELEASE,FAILED,RESULT,NORMALIZE);
    static final String[] PROTECTED={"FrontPreview1931","RearRestart1926","FacingMemory1928","RearLensUi1930","PreviewStart1927",
        "ProviderLifecycle1929","PreviewInputs1929","OpticalZoom","ManualLens170","ExitBusy1921"};

    static void req(boolean b,String message){MergePayloads.require(b,message);}
    static String id(Method m){return MergePayloads.id(m);}
    static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
    static Method replace(Method m,MethodImplementation body){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body);}
    static ClassDef members(ClassDef c,List<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
    static ImmutableMethodReference mr(String s){
        int arrow=s.indexOf("->"),open=s.indexOf('(',arrow),close=s.indexOf(')',open);req(arrow>0&&open>arrow&&close>open,"Method descriptor "+s);
        var args=new ArrayList<String>();for(int i=open+1;i<close;){int start=i;while(s.charAt(i)=='[')i++;if(s.charAt(i)=='L')i=s.indexOf(';',i)+1;else i++;req(i>start&&i<=close,"Parameter descriptor");args.add(s.substring(start,i));}
        return new ImmutableMethodReference(s.substring(0,arrow),s.substring(arrow+2,open),args,s.substring(close+1));
    }
    static BuilderInstruction call(String descriptor,int... registers){
        boolean contiguous=true;for(int i=1;i<registers.length;i++)contiguous&=registers[i]==registers[0]+i;
        if(contiguous)return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,registers[0],registers.length,mr(descriptor));
        req(registers.length<=5,"Too many explicit arguments");int[] v=new int[5];System.arraycopy(registers,0,v,0,registers.length);
        for(int r:registers)req(r>=0&&r<16,"Explicit register overflow");
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC,registers.length,v[0],v[1],v[2],v[3],v[4],mr(descriptor));
    }
    static BuilderInstruction redirect(Instruction instruction,String descriptor){
        if(instruction instanceof RegisterRangeInstruction r){req(instruction.getOpcode()==Opcode.INVOKE_STATIC_RANGE,"Expected static range call");return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,r.getStartRegister(),r.getRegisterCount(),mr(descriptor));}
        req(instruction instanceof FiveRegisterInstruction&&instruction.getOpcode()==Opcode.INVOKE_STATIC,"Expected static call");
        var r=(FiveRegisterInstruction)instruction;return new BuilderInstruction35c(Opcode.INVOKE_STATIC,r.getRegisterCount(),r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG(),mr(descriptor));
    }
    static void guard(MutableMethodImplementation body,int index,int scratch,String descriptor,int... registers){
        // Labels remain attached to the original continuation. False resumes the
        // complete original body; true means the helper owns/closes this image.
        Label continuation=body.newLabelForIndex(index);
        body.addInstruction(index,call(descriptor,registers));
        body.addInstruction(index+1,new BuilderInstruction11x(Opcode.MOVE_RESULT,scratch));
        body.addInstruction(index+2,new BuilderInstruction21t(Opcode.IF_EQZ,scratch,continuation));
        body.addInstruction(index+3,new BuilderInstruction10x(Opcode.RETURN_VOID));
    }
    static Method repair(Method method){
        String key=id(method);var body=new MutableMethodImplementation(method.getImplementation());int expected,owner;
        if(key.equals(Q0)){
            req(body.getRegisterCount()==15&&ref(body.getInstructions().get(0)).equals(P+"CaptureYuv;->received(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/CaptureResult;)V"),"Full-resolution entry ABI");
            guard(body,0,0,BURST+"->beginImage(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)Z",12,13,14);
        }else if(key.equals(U0)||key.equals(RELEASE)||key.equals(FAILED)){
            expected=key.equals(U0)?15:key.equals(RELEASE)?4:5;owner=expected-(key.equals(U0)?3:1);
            req(body.getRegisterCount()==expected,"Cancellation entry ABI "+key);body.addInstruction(0,call(BURST+"->canceled(Ljava/lang/Object;)V",owner));
        }else if(key.equals(RESULT)){
            req(body.getRegisterCount()==4,"Capture result observer ABI");
            body.addInstruction(0,call(BURST+"->observedResult(Ljava/lang/Object;Landroid/hardware/camera2/CaptureResult;)V",2,3));
        }else if(key.equals(STILL)||key.equals(RECEIVE)){
            expected=key.equals(STILL)?6:7;int image=expected-1,hit=-1;
            req(body.getRegisterCount()==expected,"Reader ABI "+key);
            for(int i=0;i<body.getInstructions().size();i++)if(ref(body.getInstructions().get(i)).equals(ACQUIRE)){req(hit==-1,"Duplicate acquire");hit=i;}
            req(hit>=0&&hit+2<body.getInstructions().size(),"Missing acquire result");Instruction move=body.getInstructions().get(hit+1);
            req(move.getOpcode()==Opcode.MOVE_RESULT_OBJECT&&((OneRegisterInstruction)move).getRegisterA()==image,"Reader result register");
            Instruction acquire=body.getInstructions().get(hit);
            req(acquire.getOpcode()==Opcode.INVOKE_VIRTUAL&&acquire instanceof FiveRegisterInstruction&&
                ((FiveRegisterInstruction)acquire).getRegisterCount()==1&&((FiveRegisterInstruction)acquire).getRegisterC()==image,"Original reader acquire ABI");
            body.replaceInstruction(hit,key.equals(STILL)?call(ACQUIRE_STILL,4,5):call(ACQUIRE_YUV,4,5,6));
            if(key.equals(STILL))guard(body,hit+2,0,BURST+"->stillImage(Ljava/lang/Object;Landroid/media/Image;)Z",4,5);
            else guard(body,hit+2,1,BURST+"->yuvImage(Ljava/lang/Object;Ljava/lang/Object;Landroid/media/Image;)Z",4,5,6);
        }else if(key.equals(NORMALIZE)){
            int hits=0;for(int i=0;i<body.getInstructions().size();i++)if(ref(body.getInstructions().get(i)).equals(OLD_RESIZE)){body.replaceInstruction(i,redirect(body.getInstructions().get(i),NEW_RESIZE));hits++;}
            req(hits==2,"Exactly two existing normalization resize calls");
        }else throw new IllegalStateException("Unreviewed hook "+key);
        return replace(method,body);
    }
    static Map<String,ClassDef> metadata(Map<String,ClassDef> old){
        var out=new TreeMap<String,ClassDef>();int count=0;
        for(var c:old.values()){
            var ms=new ArrayList<Method>();boolean changed=false;
            for(var m:c.getMethods()){
                if(m.getImplementation()==null){ms.add(m);continue;}var body=new MutableMethodImplementation(m.getImplementation());boolean hit=false;
                for(int i=0;i<body.getInstructions().size();i++){Instruction instruction=body.getInstructions().get(i);
                    if(instruction instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith(OLD_DESCRIPTION)){
                        req(c.getType().equals("Lapp/hiro/ulike/patches/UlikeHqMaxPatch;")&&instruction.getOpcode()==Opcode.CONST_STRING,"Only exact ULike metadata");
                        body.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)instruction).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));count++;hit=true;
                    }
                }ms.add(hit?replace(m,body):m);changed|=hit;
            }out.put(c.getType(),changed?members(c,ms):c);
        }req(count==1,"Exactly one ULike description per loader");return out;
    }
    static String jsonList(Collection<String> values){var a=new ArrayList<String>();for(String s:new TreeSet<>(values))a.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",a)+"]";}
    public static void main(String[] args)throws Exception{
        req(args.length==6,"BASE HELPER_DEX BUNDLE_DEX SEED OUT AUDIT");Path base=Path.of(args[0]),out=Path.of(args[4]);Files.createDirectories(out);
        var oldNativeClasses=MergePayloads.classes(base.resolve("ulike/methods.dex").toString());
        var oldNative=MergePayloads.methods(oldNativeClasses.values());var payload=new TreeMap<>(oldNative);
        var rows=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());req(oldNative.keySet().equals(rows.keySet()),"Baseline native inventory");
        for(var e:oldNative.entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Baseline native contract "+e.getKey());
        var seed=MergePayloads.methods(MergePayloads.classes(args[3]).values());req(seed.keySet().equals(Set.of(STILL)),"Only reviewed still reader seed");
        req(MergePayloads.hash(seed.get(STILL)).equals(SEED_HASH)&&!oldNative.containsKey(STILL),"Pinned stock still reader seed");
        var audit=new ArrayList<String>();
        for(String key:new TreeSet<>(NATIVE)){
            Method before=oldNative.getOrDefault(key,seed.get(key));req(before!=null,"Native target missing "+key);Method after=repair(before);payload.put(key,after);
            if(!oldNative.containsKey(key))rows.put(key,new String[]{key,MergePayloads.hash(before),MergePayloads.hash(after)});else rows.get(key)[2]=MergePayloads.hash(after);
            audit.add((oldNative.containsKey(key)?"NATIVE_CHANGED":"NATIVE_ADDED")+"\t"+key+"\t"+MergePayloads.hash(before)+"\t"+MergePayloads.hash(after));
        }
        int nativeKept=0;for(var e:oldNative.entrySet())if(!NATIVE.contains(e.getKey())){req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(payload.get(e.getKey()))),"Unrelated native changed");nativeKept++;}
        var helper=MergePayloads.classes(args[1]);var helperMethods=MergePayloads.methods(helper.values());
        var oldRuntime=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var runtime=new TreeMap<String,ClassDef>();var touched=new TreeSet<String>();int aliasCount=0;
        for(ClassDef c:oldRuntime.values()){
            var ms=new ArrayList<Method>();boolean changed=false;
            for(Method method:c.getMethods())if(RUNTIME.contains(id(method))){Method after=repair(method);ms.add(after);changed=true;touched.add(id(method));audit.add("RUNTIME\t"+id(method)+"\t"+MergePayloads.hash(method)+"\t"+MergePayloads.hash(after));}else ms.add(method);
            if(c.getType().equals(SHOT)){
                req(ms.stream().noneMatch(m->id(m).equals(ALIAS)),"Metadata alias collision");Method alias=helperMethods.get(ALIAS);req(alias!=null,"Missing exact metadata alias");ms.add(alias);aliasCount++;changed=true;
            }runtime.put(c.getType(),changed?members(c,ms):c);
        }
        req(touched.equals(RUNTIME)&&aliasCount==1,"Runtime hook coverage");
        var helperClasses=new TreeSet<String>();var roots=new TreeSet<String>();for(var e:helper.entrySet()){
            if(e.getKey().matches(HELPER)){req(!runtime.containsKey(e.getKey()),"Helper collision");runtime.put(e.getKey(),e.getValue());helperClasses.add(e.getKey());if(!e.getKey().contains("$"))roots.add(e.getKey());}
            else req(e.getKey().equals(SHOT),"Compile-only stub leaked "+e.getKey());
        }
        req(roots.equals(ROOTS),"Five production helper roots required");
        var afterMethods=MergePayloads.methods(runtime.values());int runtimeKept=0;
        for(var e:MergePayloads.methods(oldRuntime.values()).entrySet())if(!RUNTIME.contains(e.getKey())){req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(afterMethods.get(e.getKey()))),"Unrelated runtime changed "+e.getKey());runtimeKept++;}
        for(String name:PROTECTED){String type=P+name+";";req(oldRuntime.containsKey(type)&&MergePayloads.classHash(oldRuntime.get(type)).equals(MergePayloads.classHash(runtime.get(type))),"Protected startup/recovery class changed "+type);}
        var standalone=metadata(MergePayloads.classes(base.resolve("classes.dex").toString()));
        var oldBundle=MergePayloads.classes(args[2]);var bundle=metadata(oldBundle);int other=0;
        for(var e:oldBundle.entrySet())if(!e.getKey().startsWith(MergePayloads.PATCH_NS)){req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(bundle.get(e.getKey()))),"Other app loader changed");other++;}
        var nativeClasses=new TreeMap<String,ClassDef>();for(ClassDef holder:MergePayloads.holders(payload)){
            ClassDef previous=oldNativeClasses.get(holder.getType());var ms=new ArrayList<Method>();holder.getMethods().forEach(ms::add);
            nativeClasses.put(holder.getType(),previous==null?holder:ms.stream().anyMatch(m->NATIVE.contains(id(m)))?members(previous,ms):previous);
        }
        MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());MergePayloads.writeDex(out.resolve("methods.dex"),nativeClasses.values());
        MergePayloads.writeDex(out.resolve("loader.dex"),standalone.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
        var lines=new ArrayList<String>();for(var row:rows.values())lines.add(String.join("\t",row));Files.write(out.resolve("methods.tsv"),lines);
        for(var e:MergePayloads.methods(MergePayloads.classes(out.resolve("methods.dex").toString()).values()).entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Serialized native contract");
        Files.writeString(out.resolve("burst-inventory.json"),"{\n\"changed_runtime_methods\":"+jsonList(RUNTIME)+",\n\"changed_native_methods\":"+jsonList(NATIVE)+",\n\"new_helper_classes\":"+jsonList(helperClasses)+",\n\"new_runtime_aliases\":"+jsonList(Set.of(ALIAS))+",\n\"new_native_methods\":"+jsonList(Set.of(STILL))+",\n\"camera_startup_recovery_byte_identical\":true\n}\n");
        audit.add("PASS\texisting_native_retained="+nativeKept+"\texisting_runtime_retained="+runtimeKept+"\tnew_native=1\thelpers="+helperClasses.size()+"\tother_app_loaders_retained="+other);
        Files.write(Path.of(args[5]),audit);audit.forEach(System.out::println);
    }
}
