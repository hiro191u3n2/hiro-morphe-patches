package app.hiro.newx.patches;

import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.Compatibility;
import app.morphe.patcher.patch.AppTarget;
import app.morphe.patcher.patch.ApkFileType;
import app.morphe.patcher.patch.SupportedAbi;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

public final class NewXBackPatch {
    public static final String NAME="NewX: 戻る1回で終了・履歴削除";
    public static final String VERSION="12.43.0-prod.01";
    public static final String MAIN="Lcom/x/android/main/MainActivity;";
    public static final String ROOT="Lcom/x/android/main/ea;";
    public static final String CALLBACK="Lcom/x/android/main/u9;";
    public static final String RUNTIME="Lapp/hiro/newx/back/RootExit;";
    public static final String U_SHA="9bbdb9efbe085935ca3c78a14221c9e3551c0c85dd44b6288332cefd93b7fcd6", T_SHA="bdd5f70bf4ba3261e577d8bfb3c6c6ccccf7eabc1a904b5ea9064133cc59bf67", CALLBACK_SHA="ce632beaf68192ca3f39c35b27a7d05302ae9e54b0094558d0f7b634bc9d11cd";
    public static final BytecodePatch PATCH = new BytecodePatch(NAME,
        "X 12.43.0: ホーム・検索・通知・DMの最上位で戻る1回で終了し履歴から削除。終了前にホームを選択。詳細画面・ダイアログ・キーボードは元の戻る処理を保持。",
        true, Collections.singletonList(new Compatibility("com.twitter.android","NewX",null,ApkFileType.APKM,"#000000",null,Collections.singletonList(new AppTarget(VERSION,Collections.singletonMap(SupportedAbi.ARM64_V8A,312430001),false,null,null)))),
        Collections.emptySet(),Collections.emptySet(),Collections.singletonList(new Supplier<Iterable<Supplier<InputStream>>>() { public Iterable<Supplier<InputStream>> get(){return Collections.singletonList(new Supplier<InputStream>() {
            public InputStream get(){InputStream s=NewXBackPatch.class.getResourceAsStream("/extensions/newx_back_exit.mpe");if(s==null)throw new IllegalStateException("Missing NewX back runtime");return s;}
        });}}),new Function1<BytecodePatchContext,Unit>() {
            public Unit invoke(BytecodePatchContext c){
                if(!"com.twitter.android".equals(c.getPackageMetadata().getPackageName())||!VERSION.equals(c.getPackageMetadata().getVersionName()))throw new IllegalStateException("NewX back: verified X 12.43.0-prod.01 required");
                ClassDef root=c.classDefBy(ROOT), callback=c.classDefBy(CALLBACK), main=c.classDefBy(MAIN);
                verify(root,callback,main);
                apply(c.mutableClassDefBy(ROOT),c.mutableClassDefBy(CALLBACK),c.mutableClassDefBy(MAIN));
                return Unit.INSTANCE;
            }
        },null,null,"NewX");
    public static Method method(ClassDef c,String name,String ret){Method out=null;for(Method m:c.getMethods())if(m.getName().equals(name)&&m.getReturnType().equals(ret)&&m.getParameterTypes().isEmpty()){if(out!=null)throw new IllegalStateException("Ambiguous "+name);out=m;}if(out==null||out.getImplementation()==null)throw new IllegalStateException("Missing "+c.getType()+" "+name);return out;}
    public static List<Instruction> code(Method m){List<Instruction> a=new ArrayList<Instruction>();for(Instruction i:m.getImplementation().getInstructions())a.add(i);return a;}
    static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    public static int callbackStart(Method m){List<Instruction> a=code(m);int marker=-1;for(int i=0;i<a.size();i++)if(ref(a.get(i)).equals("exit_confirmation")){if(marker>=0)throw new IllegalStateException("Ambiguous root callback");marker=i;}if(marker<0)throw new IllegalStateException("Root confirmation marker missing");for(int i=marker;i>=0;i--)if(a.get(i).getOpcode()==Opcode.CHECK_CAST&&ref(a.get(i)).equals(ROOT))return i;throw new IllegalStateException("Root callback receiver missing");}
    public static List<Instruction> callbackCode(Method m){List<Instruction>a=code(m);int start=callbackStart(m),end=start;while(end<a.size()&&a.get(end).getOpcode()!=Opcode.RETURN_OBJECT)end++;if(end==a.size())throw new IllegalStateException("Callback end missing");return a.subList(start,end+1);}
    public static String fingerprint(List<? extends Instruction> list){try{StringBuilder b=new StringBuilder();for(Instruction i:list){b.append(i.getOpcode().name()).append(':');if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;b.append(r.getRegisterCount()).append(',').append(r.getRegisterC()).append(',').append(r.getRegisterD()).append(',').append(r.getRegisterE()).append(',').append(r.getRegisterF()).append(',').append(r.getRegisterG());}else if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction r=(RegisterRangeInstruction)i;b.append(r.getStartRegister()).append(',').append(r.getRegisterCount());}else if(i instanceof ThreeRegisterInstruction){ThreeRegisterInstruction r=(ThreeRegisterInstruction)i;b.append(r.getRegisterA()).append(',').append(r.getRegisterB()).append(',').append(r.getRegisterC());}else if(i instanceof TwoRegisterInstruction){TwoRegisterInstruction r=(TwoRegisterInstruction)i;b.append(r.getRegisterA()).append(',').append(r.getRegisterB());}else if(i instanceof OneRegisterInstruction)b.append(((OneRegisterInstruction)i).getRegisterA());b.append(':').append(ref(i));if(i instanceof WideLiteralInstruction)b.append(':').append(((WideLiteralInstruction)i).getWideLiteral());if(i instanceof OffsetInstruction)b.append(':').append(((OffsetInstruction)i).getCodeOffset());b.append('\n');}byte[] digest=MessageDigest.getInstance("SHA-256").digest(b.toString().getBytes(StandardCharsets.UTF_8));StringBuilder h=new StringBuilder();for(byte x:digest)h.append(String.format("%02x",x&255));return h.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    public static void verify(ClassDef root,ClassDef callback,ClassDef main){
        if(!ROOT.equals(root.getType())||!CALLBACK.equals(callback.getType())||!MAIN.equals(main.getType()))throw new IllegalStateException("Wrong NewX classes");
        Method u=method(root,"u","Z"),t=method(root,"t","Z"),invoke=method(callback,"invoke","Ljava/lang/Object;"),start=method(main,"onStart","V");
        if(!U_SHA.equals(fingerprint(code(u)))||!T_SHA.equals(fingerprint(code(t)))||!CALLBACK_SHA.equals(fingerprint(callbackCode(invoke))))throw new IllegalStateException("NewX native back boundary changed or already patched");
        boolean superStart=false;for(Instruction i:code(start)){if(ref(i).startsWith(RUNTIME))throw new IllegalStateException("NewX lifecycle already patched");if(i.getOpcode()==Opcode.INVOKE_SUPER&&ref(i).endsWith("->onStart()V"))superStart=true;}
        if(!superStart)throw new IllegalStateException("Native onStart super call missing");
        if(t.getImplementation().getRegisterCount()!=2||invoke.getImplementation().getRegisterCount()!=27)throw new IllegalStateException("Unexpected root registers");
        int idx=callbackStart(invoke);Instruction cast=code(invoke).get(idx);if(((OneRegisterInstruction)cast).getRegisterA()!=0)throw new IllegalStateException("Unexpected callback receiver");
    }
    static MutableMethod mutable(MutableClass c,String name,String ret){return (MutableMethod)method(c,name,ret);}
    static ImmutableMethodReference runtime(String n,List<String> p,String r){return new ImmutableMethodReference(RUNTIME,n,p,r);}
    public static void apply(MutableClass root,MutableClass callback,MutableClass main){
        MutableMethod t=mutable(root,"t","Z");MutableMethodImplementation a=t.getImplementation();Label originalT=a.newLabelForIndex(0);
        a.addInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,1,0,0,0,0,runtime("enabled",Collections.singletonList("Ljava/lang/Object;"),"Z")));
        a.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));a.addInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,0,originalT));a.addInstruction(3,new BuilderInstruction11x(Opcode.RETURN,0));
        MutableMethod invoke=mutable(callback,"invoke","Ljava/lang/Object;");int insert=callbackStart(invoke)+1;MutableMethodImplementation b=invoke.getImplementation();Label fallback=b.newLabelForIndex(insert);
        b.addInstruction(insert++,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,0,0,0,0,0,runtime("rootBack",Collections.singletonList("Ljava/lang/Object;"),"Z")));
        b.addInstruction(insert++,new BuilderInstruction11x(Opcode.MOVE_RESULT,1));
        b.addInstruction(insert++,new BuilderInstruction21t(Opcode.IF_EQZ,1,fallback));
        b.addInstruction(insert,new BuilderInstruction11x(Opcode.RETURN_OBJECT,10));
        MutableMethod start=mutable(main,"onStart","V");int receiver=start.getImplementation().getRegisterCount()-1;
        start.getImplementation().addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,receiver,1,runtime("bind",Collections.singletonList("Landroid/app/Activity;"),"V")));
    }
}
