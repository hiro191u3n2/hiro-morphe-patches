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
import com.android.tools.smali.dexlib2.builder.BuilderInstruction;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Exact-version calendar month and widget colors; unrelated navigation and events remain native. */
public final class CalendarMonthColorsPatch {
    private CalendarMonthColorsPatch() {}
    public static final String NAME = "月表示とウィジェットの土曜を青・祝日を赤";
    public static final String OWNER = "Lvn0;";
    public static final String MONTH = "Lorg/fossify/calendar/views/MonthView;";
    public static final String DAYS = "Lit1;";
    public static final String RUNTIME = "Lapp/hiro/fossifycalendar/runtime/MonthColors;";
    public static final String CTX = "Landroid/content/Context;";
    public static final String OBJ = "Ljava/lang/Object;";
    public static final String VIEW = "Landroid/view/View;";
    public static final String PAINT = "Landroid/graphics/Paint;";
    public static final String FINGERPRINT = "2135cb03a99af2317c52ea2d41c787c79ba6c8f7c207e0df0b1e30228014bb51";
    public static final String CURRENT_FINGERPRINT = "bda90a64656132d22f1c8a9ccb2e48e85242b1fe2fc70f3bda093912a5f3913c";
    public static final Set<String> OWNERS = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(OWNER,MONTH,DAYS)));
    public static final Map<String,Set<String>> FINGERPRINTS;
    public static final List<String> PARAMS = Arrays.asList(CTX,"Ljava/lang/String;","Ljava/util/ArrayList;","Z","Lorg/joda/time/DateTime;");
    static {
        Map<String,Set<String>> hashes = new LinkedHashMap<String,Set<String>>();
        hashes.put(OWNER,new HashSet<String>(Arrays.asList(FINGERPRINT,CURRENT_FINGERPRINT)));
        // The same original methods serialize differently under the two verified Morphe SDKs.
        hashes.put(MONTH,new HashSet<String>(Arrays.asList("de0b29b6e42babe4073d477f1d2560e28b8a39b7cc3b4020213387e5862d98c4","837877066df0318671e7c30e87d4702f18f4beb32de2f03223d47b42d7e36d95")));
        hashes.put(DAYS,new HashSet<String>(Arrays.asList("e69818c772015486ffadd24ec892da8950750100341013ca2c447d1114cd3951","9d21683f3d8d4c9316edcbd03d3be3c57f4865862b5c53f9ed34db28db649629")));
        FINGERPRINTS=Collections.unmodifiableMap(hashes);
    }
    public static final BytecodePatch PATCH = new BytecodePatch(
        NAME,
        "カレンダー 1.12.1 (24) のアプリ内1か月表示とホームウィジェットで土曜日の曜日・日付を青、祝日の日付を赤にします。今日の丸印の文字はアプリ内では白、ウィジェットでは黒。祝日を優先し、前後月の薄さ・通常予定・印刷表示を維持。実機表示は未検証。",
        true,
        Collections.singleton(new Pair<String,Set<String>>("org.fossify.calendar",Collections.singleton("1.12.1"))),
        Collections.emptySet(),Collections.emptySet(),
        new Supplier<InputStream>() { public InputStream get() {
            InputStream in=CalendarMonthColorsPatch.class.getResourceAsStream("/extensions/fossify_calendar_colors.mpe");
            if(in==null)throw new IllegalStateException("Missing calendar colors extension");return in;
        }},
        new Function1<BytecodePatchContext,Unit>() { public Unit invoke(BytecodePatchContext c) {
            validatePackage(c.getPackageMetadata().getPackageName(),c.getPackageMetadata().getVersionName(),c.getPackageMetadata().getVersionCode());
            Map<String,Method> originals = new LinkedHashMap<String,Method>();
            for(String owner:OWNERS) originals.put(owner,ImmutableMethod.of(validateClass(c.classDefBy(owner))));
            for(String owner:OWNERS) apply(c.mutableClassDefBy(owner),originals.get(owner));
            return Unit.INSTANCE;
        }}, null);

    public static void validatePackage(String p,String v,String code) {
        if(!"org.fossify.calendar".equals(p)||!"1.12.1".equals(v)||!"24".equals(code))
            throw new IllegalStateException("Only verified Fossify Calendar 1.12.1 (24) is supported");
    }
    private static List<String> parameters(MethodReference m) {
        List<String> result=new ArrayList<String>();
        for(CharSequence p:m.getParameterTypes()) result.add(p.toString());
        return result;
    }
    public static boolean isTarget(Method m) {
        if(!"V".equals(m.getReturnType()))return false;
        if(OWNER.equals(m.getDefiningClass()))return "i".equals(m.getName())&&PARAMS.equals(parameters(m));
        if(MONTH.equals(m.getDefiningClass()))return "onDraw".equals(m.getName())&&Collections.singletonList("Landroid/graphics/Canvas;").equals(parameters(m));
        if(DAYS.equals(m.getDefiningClass()))return "a".equals(m.getName())&&Collections.singletonList("Z").equals(parameters(m));
        return false;
    }
    public static String fingerprint(Method m) {
        try {
            ImmutableClassDef w=new ImmutableClassDef(m.getDefiningClass(),1,"Ljava/lang/Object;",Collections.<String>emptyList(),null,Collections.emptySet(),Collections.emptyList(),Collections.singletonList(ImmutableMethod.of(m)));
            MemoryDataStore s=new MemoryDataStore();
            try {DexPool.writeTo(s,new ImmutableDexFile(Opcodes.forApi(29),Collections.singleton(w)));
                byte[] hash=MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(s.getBuffer(),s.getSize()));
                StringBuilder b=new StringBuilder();for(byte v:hash)b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();
            } finally {s.close();}
        } catch(Exception e){throw new IllegalStateException("Cannot verify calendar native method",e);}
    }
    private static boolean verifiedFingerprint(Method m) {
        Set<String> allowed=FINGERPRINTS.get(m.getDefiningClass());
        return allowed!=null&&allowed.contains(fingerprint(m));
    }
    public static Method validateClass(ClassDef c) {
        if(c==null||!OWNERS.contains(c.getType()))throw new IllegalStateException("Calendar color owner missing");
        int registers=OWNER.equals(c.getType())?56:MONTH.equals(c.getType())?34:22;
        int flags=OWNER.equals(c.getType())?1:17;
        Method found=null;
        for(Method m:c.getMethods())if(isTarget(m)){
            if(found!=null||m.getImplementation()==null||m.getImplementation().getRegisterCount()!=registers||m.getAccessFlags()!=flags)
                throw new IllegalStateException("Unexpected calendar declaration: "+c.getType());found=m;
        }
        if(found==null||!verifiedFingerprint(found))
            throw new IllegalStateException("Unverified or already modified calendar body: "+c.getType());
        return found;
    }
    private static MethodReference method(Instruction i) {
        if(!(i instanceof ReferenceInstruction))return null;
        Reference r=((ReferenceInstruction)i).getReference();return r instanceof MethodReference?(MethodReference)r:null;
    }
    private static boolean call(Instruction i,String owner,String name,String ret,String...params) {
        MethodReference m=method(i);
        return m!=null&&owner.equals(m.getDefiningClass())&&name.equals(m.getName())&&ret.equals(m.getReturnType())&&Arrays.asList(params).equals(parameters(m));
    }
    private static boolean regs(Instruction i,int...wanted) {
        if(!(i instanceof FiveRegisterInstruction))return false;
        FiveRegisterInstruction r=(FiveRegisterInstruction)i;
        int[] got={r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()};
        if(r.getRegisterCount()!=wanted.length)return false;
        for(int n=0;n<wanted.length;n++)if(got[n]!=wanted[n])return false;
        return true;
    }
    private static boolean dayStart(List<? extends Instruction> list,int i) {
        Instruction x=list.get(i);
        if(x.getOpcode()!=Opcode.NEW_INSTANCE||!(x instanceof OneRegisterInstruction)||((OneRegisterInstruction)x).getRegisterA()!=10||i+1>=list.size())return false;
        Reference r=((ReferenceInstruction)x).getReference();Instruction next=list.get(i+1);
        return r instanceof TypeReference&&"Ljava/lang/StringBuilder;".equals(((TypeReference)r).getType())&&next instanceof ReferenceInstruction&&((ReferenceInstruction)next).getReference() instanceof StringReference&&"day_".equals(((StringReference)((ReferenceInstruction)next).getReference()).getString());
    }
    private static boolean field(Instruction i,String owner,String name,String type,int a,int b) {
        if(i.getOpcode()!=Opcode.IGET_OBJECT||!(i instanceof TwoRegisterInstruction)||!(i instanceof ReferenceInstruction))return false;
        TwoRegisterInstruction r=(TwoRegisterInstruction)i;Reference f=((ReferenceInstruction)i).getReference();
        return r.getRegisterA()==a&&r.getRegisterB()==b&&f instanceof FieldReference&&owner.equals(((FieldReference)f).getDefiningClass())&&name.equals(((FieldReference)f).getName())&&type.equals(((FieldReference)f).getType());
    }
    private static BuilderInstruction invoke(String name,List<String> params,String ret,int...r) {
        int[] a=new int[5];System.arraycopy(r,0,a,0,r.length);
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC,r.length,a[0],a[1],a[2],a[3],a[4],new ImmutableMethodReference(RUNTIME,name,params,ret));
    }
    /** Replacing the original first preserves every native branch label at the hook entry. */
    private static void before(MutableMethodImplementation impl,int index,BuilderInstruction...code) {
        BuilderInstruction old=impl.getInstructions().get(index);
        impl.replaceInstruction(index,code[0]);
        for(int i=1;i<code.length;i++)impl.addInstruction(index+i,code[i]);
        impl.addInstruction(index+code.length,old);
    }
    private static int unique(int old,int index,String name) {
        if(old>=0)throw new IllegalStateException("Ambiguous "+name);return index;
    }
    public static int apply(MutableClass c,Method original) {
        if(c==null||!c.getType().equals(original.getDefiningClass())||!isTarget(original)||!verifiedFingerprint(original))throw new IllegalStateException("Verified original required");
        MutableMethod target=null;for(MutableMethod m:c.getMethods())if(isTarget(m))target=m;
        if(target==null)throw new IllegalStateException("Mutable calendar target missing");
        if(OWNER.equals(c.getType()))return widget(target.getImplementation());
        if(MONTH.equals(c.getType()))return month(target.getImplementation());
        return days(target.getImplementation());
    }
    private static int widget(MutableMethodImplementation impl) {
        List<Instruction> list=new ArrayList<Instruction>(impl.getInstructions());int header=-1,day=-1,today=-1;
        for(int i=0;i<list.size();i++){
            Instruction x=list.get(i);
            if(call(x,"Landroid/widget/RemoteViews;","setTextColor","V","I","I")&&regs(x,15,8,11))header=unique(header,i,"widget header");
            if(dayStart(list,i))day=unique(day,i,"widget date");
            if(call(x,"Lx11;","C","I","I")&&regs(x,2))today=unique(today,i,"widget today");
        }
        if(header<0||day<=header||today<=day||list.get(today+1).getOpcode()!=Opcode.MOVE_RESULT||((OneRegisterInstruction)list.get(today+1)).getRegisterA()!=7)throw new IllegalStateException("Widget anchors changed");
        impl.replaceInstruction(today,new BuilderInstruction21ih(Opcode.CONST_HIGH16,7,0xff000000));
        impl.replaceInstruction(today+1,new BuilderInstruction10x(Opcode.NOP));
        before(impl,day,new BuilderInstruction22x(Opcode.MOVE_OBJECT_FROM16,1,24),invoke("dateColor",Arrays.asList(CTX,OBJ,"I"),"I",0,1,2),new BuilderInstruction11x(Opcode.MOVE_RESULT,2));
        before(impl,header,invoke("weekdayColor",Arrays.asList("I","I","I"),"I",3,10,11),new BuilderInstruction11x(Opcode.MOVE_RESULT,11));
        // Shared monthly-day preparation already loaded the immutable calendar snapshot on this thread.
        return 2;
    }
    private static int month(MutableMethodImplementation impl) {
        List<Instruction> list=new ArrayList<Instruction>(impl.getInstructions());int header=-1,selection=-1,circle=-1,date=-1;
        for(int i=0;i<list.size();i++){
            Instruction x=list.get(i);
            if(field(x,MONTH,"D","Ljava/util/ArrayList;",3,0))header=unique(header,i,"month header");
            if(x.getOpcode()==Opcode.IF_EQZ&&x instanceof OneRegisterInstruction&&((OneRegisterInstruction)x).getRegisterA()==6)selection=unique(selection,i,"month selection");
            if(call(x,"Landroid/graphics/Canvas;","drawCircle","V","F","F","F",PAINT)&&regs(x,1,3,2,8,11))circle=unique(circle,i,"today circle");
            if(call(x,"Landroid/graphics/Canvas;","drawText","V","Ljava/lang/String;","F","F",PAINT)&&regs(x,1,14,3,9,15))date=unique(date,i,"month date");
        }
        if(header<0||selection<=header||circle<=selection||date<=circle)throw new IllegalStateException("Month view anchors changed");
        before(impl,date,invoke("appDatePaint",Arrays.asList(VIEW,OBJ,PAINT),"V",0,13,15));
        before(impl,circle,invoke("appCirclePaint",Arrays.asList(VIEW,OBJ,PAINT),"V",0,13,11));
        before(impl,selection,invoke("drawSelection",Arrays.asList(VIEW,OBJ,"Z"),"Z",0,13,6),new BuilderInstruction11x(Opcode.MOVE_RESULT,6));
        before(impl,header,invoke("appWeekdayPaint",Arrays.asList(VIEW,"I",PAINT),PAINT,0,2,5),new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,5));
        impl.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,32,1,new ImmutableMethodReference(RUNTIME,"appBeginRender",Collections.singletonList(VIEW),"V")));
        return 5;
    }
    private static int days(MutableMethodImplementation impl) {
        List<Instruction> list=new ArrayList<Instruction>(impl.getInstructions());int prepare=-1;
        for(int i=0;i<list.size();i++)if(call(list.get(i),"Lq50;","o","I",CTX,"Lorg/joda/time/DateTime;")&&regs(list.get(i),5,2))prepare=unique(prepare,i,"monthly metadata prepare");
        if(prepare<=0||!field(list.get(prepare-1),DAYS,"b",CTX,5,0))throw new IllegalStateException("Monthly data preparation changed");
        before(impl,prepare,invoke("beginRender",Collections.singletonList(CTX),"V",5));
        return 1;
    }
}
