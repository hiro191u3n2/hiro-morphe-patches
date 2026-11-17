package app.hiro.newx.patches;

import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.patch.Compatibility;
import app.morphe.patcher.patch.AppTarget;
import app.morphe.patcher.patch.ApkFileType;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.BuilderInstruction;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.rewriter.*;

/** Additive NewX port: shared DM/Grok/user timestamps remain byte-for-byte unchanged. */
public final class NewXAbsoluteTimePatch {
    public static final String VERSION = "12.43.0-prod.01";
    public static final String NAME = "NewX: ポストの日時を絶対時間・24時間表記にする";
    public static final String BYLINE = "Lcom/x/ui/common/user/y;";
    public static final String LAMBDA = "Lcom/x/ui/common/user/u0;";
    public static final String CLONE = "Lcom/x/ui/common/user/u0HiroAbsolute1243;";
    public static final String POST = "Lcom/x/urt/items/post/q1;";
    public static final String DETAIL = "Lcom/x/urt/items/post/a2;";
    public static final String ACCESS = "Lcom/x/urt/items/post/accessibility/b;";
    public static final String UTIL = "Lcom/x/android/utils/i2;";
    public static final String RUNTIME = "Lapp/hiro/newx/runtime/NewXAbsoluteTime;";
    public static final String METHOD = "hiroAbsolutePostByline1243";
    private static final String INSTANT = "Lkotlin/time/Instant;";
    private static final String STR = "Ljava/lang/String;";
    public static final List<String> TARGETS = Arrays.asList(BYLINE,LAMBDA,POST,DETAIL,ACCESS,UTIL,INSTANT);

    public static final BytecodePatch PATCH = app.morphe.patcher.patch.PatchKt.bytecodePatch(
        NAME,
        "X 12.43.0-prod.01: ポストの投稿日時を yyyy/MM/dd HH:mm に変更。端末のタイムゾーンで24時間表記。投稿専用の描画経路を分離し、DMなどの共通日時処理を維持。",
        true, builder -> {
            builder.compatibleWith(new Compatibility("com.twitter.android","NewX",null,ApkFileType.APKM,"#000000",null,Collections.singletonList(new AppTarget(VERSION,Collections.emptyMap(),false,null,null))));
            builder.extendWith(() -> {
                InputStream in=NewXAbsoluteTimePatch.class.getResourceAsStream("/extensions/newx_absolute_time.mpe");
                if(in==null)throw new IllegalStateException("Missing NewX timestamp extension");return in;
            });
            builder.execute(c -> {
                if (!"com.twitter.android".equals(c.getPackageMetadata().getPackageName()) || !VERSION.equals(c.getPackageMetadata().getVersionName()))
                    throw new IllegalStateException("NewX absolute time requires verified X " + VERSION);
                apply(new Editor() {
                    public ClassDef get(String type) { return c.classDefByOrNull(type); }
                    public MutableClass mutable(String type) { return c.mutableClassDefBy(type); }
                    public void add(ClassDef value) { c.getPatchClasses$morphe_patcher().addClass$morphe_patcher(value); }
                });
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });

    public interface Editor { ClassDef get(String type); MutableClass mutable(String type); void add(ClassDef value); }
    private static void require(boolean yes,String why) { if(!yes)throw new IllegalStateException("NewX timestamp: " + why); }
    private static Method find(ClassDef c,String name) {
        require(c!=null,"missing class"); Method found=null;
        for(Method m:c.getMethods()) if(m.getName().equals(name)) { require(found==null,"ambiguous " + name);found=m; }
        require(found!=null && found.getImplementation()!=null,"missing method " + c.getType()+"->"+name); return found;
    }
    private static boolean signature(MethodReference m,String... types) {
        if(!STR.equals(m.getReturnType())||m.getParameterTypes().size()!=types.length)return false;
        for(int i=0;i<types.length;i++)if(!types[i].contentEquals(m.getParameterTypes().get(i)))return false;
        return true;
    }
    private static MethodReference ref(Instruction i) {
        if(!(i instanceof ReferenceInstruction))return null;
        Reference r=((ReferenceInstruction)i).getReference();return r instanceof MethodReference?(MethodReference)r:null;
    }
    private static int count(Method m,String owner,String name) {
        int n=0;for(Instruction i:m.getImplementation().getInstructions()) { MethodReference r=ref(i);if(r!=null&&r.getDefiningClass().equals(owner)&&r.getName().equals(name))n++; }return n;
    }
    private static int directTypes(Method m,String type) {
        int n=0;for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction&&((ReferenceInstruction)i).getReference() instanceof TypeReference&&((TypeReference)((ReferenceInstruction)i).getReference()).getType().equals(type))n++;return n;
    }
    public static void apply(Editor e) {
        require(e.get(CLONE)==null,"already patched restart lambda");
        for(Method m:e.get(BYLINE).getMethods())require(!m.getName().equals(METHOD),"already patched byline");
        Method original=find(e.get(BYLINE),"r");
        require(original.getParameterTypes().size()==15&&original.getParameterTypes().get(4).toString().equals(INSTANT)&&"V".equals(original.getReturnType()),"byline signature changed");
        require(count(original,UTIL,"i")==1&&count(original,LAMBDA,"<init>")==1&&directTypes(original,LAMBDA)==1,"byline structure changed");
        Method restart=find(e.get(LAMBDA),"invoke");
        require(count(restart,BYLINE,"r")==1,"Compose restart path changed");
        Method post=find(e.get(POST),"invoke");
        require(count(post,BYLINE,"r")==1,"post byline caller changed");
        require(count(find(e.get(DETAIL),"b"),UTIL,"g")==1&&count(find(e.get(DETAIL),"g"),UTIL,"g")==1,"detail call sites changed");
        require(count(find(e.get(ACCESS),"a"),UTIL,"d")==1,"accessibility call changed");
        require(signature(find(e.get(UTIL),"i"),INSTANT,"Landroid/content/res/Resources;","Lcom/x/clock/c;"),"relative utility signature changed");
        Method epoch=find(e.get(INSTANT),"e");
        require(epoch.getParameterTypes().isEmpty()&&"J".equals(epoch.getReturnType()),"Instant epoch signature changed");
        // Rewrite only a copy of the post byline and its remembered Compose restart lambda.
        DexRewriter rw=new DexRewriter(new RewriterModule() {
            @Override public Rewriter<String> getTypeRewriter(Rewriters r) { return new Rewriter<String>() { public String rewrite(String x) { return LAMBDA.equals(x)?CLONE:x; }}; }
            @Override public Rewriter<MethodReference> getMethodReferenceRewriter(final Rewriters r) {
                return new Rewriter<MethodReference>() { public MethodReference rewrite(MethodReference m) {
                    String owner=r.getTypeRewriter().rewrite(m.getDefiningClass());
                    String name=BYLINE.equals(m.getDefiningClass())&&"r".equals(m.getName())?METHOD:m.getName();
                    return new ImmutableMethodReference(owner,name,m.getParameterTypes(),m.getReturnType());
                }};
            }
        });
        MutableMethod copied=new MutableMethod(ImmutableMethod.of(rw.getMethodRewriter().rewrite(original)));
        require(METHOD.equals(copied.getName()),"clone rename failed");
        require(redirectUtility(copied,"i","post")==1,"cloned byline hook count");
        ClassDef copiedLambda=ImmutableClassDef.of(rw.getClassDefRewriter().rewrite(e.get(LAMBDA)));
        require(copiedLambda.getType().equals(CLONE),"restart lambda clone failed");
        // All validation and clone creation completed before app mutation.
        e.add(copiedLambda);
        e.mutable(BYLINE).getMethods().add(copied);
        require(renameCall(findMutable(e.mutable(POST),"invoke"),BYLINE,"r",METHOD)==1,"post redirect count");
        require(redirectUtility(findMutable(e.mutable(DETAIL),"b"),"g","detail")==1,"detail b hook");
        require(redirectUtility(findMutable(e.mutable(DETAIL),"g"),"g","detail")==1,"detail g hook");
        require(redirectUtility(findMutable(e.mutable(ACCESS),"a"),"d","accessible")==1,"accessible hook");
    }
    private static MutableMethod findMutable(MutableClass c,String n){return (MutableMethod)find(c,n);}
    private static int renameCall(MutableMethod method,String owner,String oldName,String newName){
        int count=0;List<BuilderInstruction> code=method.getImplementation().getInstructions();
        for(int i=0;i<code.size();i++){Instruction old=code.get(i);MethodReference r=ref(old);if(r!=null&&r.getDefiningClass().equals(owner)&&r.getName().equals(oldName)){
            method.getImplementation().replaceInstruction(i,redirect(old,new ImmutableMethodReference(owner,newName,r.getParameterTypes(),r.getReturnType())));count++;
        }}return count;
    }
    private static int redirectUtility(MutableMethod method,String oldName,String newName){
        int count=0;List<BuilderInstruction> code=method.getImplementation().getInstructions();
        for(int i=0;i<code.size();i++){Instruction old=code.get(i);MethodReference r=ref(old);if(r!=null&&r.getDefiningClass().equals(UTIL)&&r.getName().equals(oldName)){
            require(STR.equals(r.getReturnType()),"utility return type changed");
            method.getImplementation().replaceInstruction(i,redirect(old,new ImmutableMethodReference(RUNTIME,newName,r.getParameterTypes(),r.getReturnType())));count++;
        }}return count;
    }
    private static BuilderInstruction redirect(Instruction old,MethodReference to){
        require(old.getOpcode()==Opcode.INVOKE_STATIC||old.getOpcode()==Opcode.INVOKE_STATIC_RANGE,"non-static invoke");
        if(old instanceof FiveRegisterInstruction){FiveRegisterInstruction x=(FiveRegisterInstruction)old;return new BuilderInstruction35c(Opcode.INVOKE_STATIC,x.getRegisterCount(),x.getRegisterC(),x.getRegisterD(),x.getRegisterE(),x.getRegisterF(),x.getRegisterG(),to);}
        if(old instanceof RegisterRangeInstruction){RegisterRangeInstruction x=(RegisterRangeInstruction)old;return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,x.getStartRegister(),x.getRegisterCount(),to);}
        throw new IllegalStateException("Unexpected call encoding");
    }
}
