package app.hiro.fossifycalendar.patches;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Version-pinned monthly home-screen widget color correction. */
public final class CalendarMonthColorsPatch {
    private CalendarMonthColorsPatch() {}
    public static final String NAME = "月間ウィジェットの土曜を青・祝日を赤";
    public static final String OWNER = "Lvn0;";
    public static final String RUNTIME = "Lapp/hiro/fossifycalendar/runtime/MonthColors;";
    public static final String FINGERPRINT = "2135cb03a99af2317c52ea2d41c787c79ba6c8f7c207e0df0b1e30228014bb51";
    // dexlib serialization differs between the two verified Morphe SDKs for the same original method.
    public static final String CURRENT_FINGERPRINT = "bda90a64656132d22f1c8a9ccb2e48e85242b1fe2fc70f3bda093912a5f3913c";
    public static final String CTX = "Landroid/content/Context;";
    public static final String OBJ = "Ljava/lang/Object;";
    public static final List<String> PARAMS = Arrays.asList(CTX,"Ljava/lang/String;","Ljava/util/ArrayList;","Z","Lorg/joda/time/DateTime;");
    public static final BytecodePatch PATCH = new BytecodePatch(
        NAME,
        "カレンダー 1.12.1 (24) の1か月ホームウィジェットで土曜日の曜日・日付を青、祝日の日付を赤にします。祝日を優先し、前後月の薄さ・通常予定・今日の丸印を維持。実機表示は未検証。",
        true,
        Collections.singleton(new Pair<String,Set<String>>("org.fossify.calendar",Collections.singleton("1.12.1"))),
        Collections.emptySet(),Collections.emptySet(),
        new Supplier<InputStream>() { public InputStream get() {
            InputStream in=CalendarMonthColorsPatch.class.getResourceAsStream("/extensions/fossify_calendar_colors.mpe");
            if(in==null)throw new IllegalStateException("Missing calendar colors extension");return in;
        }},
        new Function1<BytecodePatchContext,Unit>() { public Unit invoke(BytecodePatchContext c) {
            validatePackage(c.getPackageMetadata().getPackageName(),c.getPackageMetadata().getVersionName(),c.getPackageMetadata().getVersionCode());
            Method original=validateClass(c.classDefBy(OWNER));
            apply(c.mutableClassDefBy(OWNER),ImmutableMethod.of(original));return Unit.INSTANCE;
        }}, null);

    public static void validatePackage(String p,String v,String code) {
        if(!"org.fossify.calendar".equals(p)||!"1.12.1".equals(v)||!"24".equals(code))
            throw new IllegalStateException("Only verified Fossify Calendar 1.12.1 (24) is supported");
    }
    public static boolean isTarget(Method m) {
        if (!"i".equals(m.getName()) || !"V".equals(m.getReturnType()) || m.getParameterTypes().size() != PARAMS.size()) return false;
        for (int i = 0; i < PARAMS.size(); i++) if (!PARAMS.get(i).equals(m.getParameterTypes().get(i).toString())) return false;
        return true;
    }
    public static String fingerprint(Method m) {
        try {
            ImmutableClassDef w=new ImmutableClassDef(m.getDefiningClass(),1,"Ljava/lang/Object;",Collections.<String>emptyList(),null,Collections.emptySet(),Collections.emptyList(),Collections.singletonList(ImmutableMethod.of(m)));
            MemoryDataStore s=new MemoryDataStore();
            try {DexPool.writeTo(s,new ImmutableDexFile(Opcodes.forApi(29),Collections.singleton(w)));
                byte[] hash=MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(s.getBuffer(),s.getSize()));
                StringBuilder b=new StringBuilder();for(byte v:hash)b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();
            } finally {s.close();}
        } catch(Exception e){throw new IllegalStateException("Cannot verify monthly widget method",e);}
    }
    private static boolean verifiedFingerprint(Method m) {
        String value = fingerprint(m);
        return FINGERPRINT.equals(value) || CURRENT_FINGERPRINT.equals(value);
    }
    public static Method validateClass(ClassDef c) {
        if(c==null||!OWNER.equals(c.getType()))throw new IllegalStateException("Monthly widget callback missing");
        Method found=null;
        for(Method m:c.getMethods())if(isTarget(m)){
            if(found!=null||m.getImplementation()==null||m.getImplementation().getRegisterCount()!=56||m.getAccessFlags()!=1)
                throw new IllegalStateException("Unexpected monthly widget declaration");found=m;
        }
        if(found==null||!verifiedFingerprint(found))
            throw new IllegalStateException("Unverified or already modified monthly widget body");
        return found;
    }
    private static MethodReference method(Instruction i) {
        if(!(i instanceof ReferenceInstruction))return null;
        Reference r=((ReferenceInstruction)i).getReference();return r instanceof MethodReference?(MethodReference)r:null;
    }
    private static boolean header(Instruction i) {
        MethodReference m=method(i);
        if(m==null||!"Landroid/widget/RemoteViews;".equals(m.getDefiningClass())||!"setTextColor".equals(m.getName())||!(i instanceof FiveRegisterInstruction))return false;
        FiveRegisterInstruction r=(FiveRegisterInstruction)i;
        return r.getRegisterCount()==3&&r.getRegisterC()==15&&r.getRegisterD()==8&&r.getRegisterE()==11;
    }
    private static boolean dayStart(List<? extends Instruction> list,int i) {
        Instruction x=list.get(i);
        if(x.getOpcode()!=Opcode.NEW_INSTANCE||!(x instanceof OneRegisterInstruction)||((OneRegisterInstruction)x).getRegisterA()!=10||i+1>=list.size())return false;
        Reference r=((ReferenceInstruction)x).getReference();
        Instruction next=list.get(i+1);
        return r instanceof TypeReference&&"Ljava/lang/StringBuilder;".equals(((TypeReference)r).getType())&&next instanceof ReferenceInstruction&&((ReferenceInstruction)next).getReference() instanceof StringReference&&"day_".equals(((StringReference)((ReferenceInstruction)next).getReference()).getString());
    }
    public static int apply(MutableClass c,Method original) {
        if(c==null||!OWNER.equals(c.getType())||!isTarget(original)||!verifiedFingerprint(original))throw new IllegalStateException("Verified original required");
        MutableMethod target=null;
        for(MutableMethod m:c.getMethods())if(isTarget(m))target=m;
        if(target==null)throw new IllegalStateException("Mutable callback missing");
        MutableMethodImplementation impl=target.getImplementation();
        List<Instruction> list=new ArrayList<Instruction>(impl.getInstructions());
        int h=-1,d=-1;
        for(int i=0;i<list.size();i++){
            if(header(list.get(i))){if(h!=-1)throw new IllegalStateException("Ambiguous weekday color");h=i;}
            if(dayStart(list,i)){if(d!=-1)throw new IllegalStateException("Ambiguous date color");d=i;}
        }
        if(h<0||d<=h)throw new IllegalStateException("Monthly widget anchors changed");
        // Replace the labelled original instruction first, so every native branch enters the hook.
        com.android.tools.smali.dexlib2.builder.BuilderInstruction old=impl.getInstructions().get(d);
        impl.replaceInstruction(d,new BuilderInstruction22x(Opcode.MOVE_OBJECT_FROM16,1,24));
        impl.addInstruction(d+1,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,0,1,2,0,0,new ImmutableMethodReference(RUNTIME,"dateColor",Arrays.asList(CTX,OBJ,"I"),"I")));
        impl.addInstruction(d+2,new BuilderInstruction11x(Opcode.MOVE_RESULT,2));
        impl.addInstruction(d+3,old);
        old=impl.getInstructions().get(h);
        impl.replaceInstruction(h,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,3,10,11,0,0,new ImmutableMethodReference(RUNTIME,"weekdayColor",Arrays.asList("I","I","I"),"I")));
        impl.addInstruction(h+1,new BuilderInstruction11x(Opcode.MOVE_RESULT,11));
        impl.addInstruction(h+2,old);
        impl.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,51,1,new ImmutableMethodReference(RUNTIME,"beginRender",Collections.singletonList(CTX),"V")));
        return 3;
    }
}
