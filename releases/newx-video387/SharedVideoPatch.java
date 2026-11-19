package app.hiro.newx.video;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import kotlin.Unit;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.BuilderInstruction;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference;

/** Use NewX's native FromId post destination for the two first-video deep link handlers. */
public final class SharedVideoPatch {
    static final String VERSION = "12.43.0-prod.01";
    static final String ROUTE = "Lcom/x/deeplink/route/b;";
    static final String DETAIL = "Lcom/x/navigation/bm;";
    static final String ACTION = "Lcom/x/navigation/hm;";
    static final String LIST = "Ljava/util/List;";
    static final String NAME = "NewX: 動画リンクで元の投稿を開く";
    static final ImmutableMethodReference CONSTRUCTOR = new ImmutableMethodReference(
        DETAIL, "<init>", Arrays.asList("J", "Z", ACTION, "I"), "V");
    static final ImmutableMethodReference LIST_OF = new ImmutableMethodReference(
        "Lkotlin/collections/CollectionsKt;", "listOf", Collections.singletonList("Ljava/lang/Object;"), LIST);

    public static final BytecodePatch PATCH = new BytecodePatch(NAME,
        "LINEなどの /video/1 共有リンクで、指定された投稿をXで開きます。投稿内の動画をタップして再生できます。",
        true, Collections.singletonList(new Compatibility("com.twitter.android","NewX",null,ApkFileType.APKM,
            "#000000",null,Collections.singletonList(new AppTarget(VERSION,
            Collections.singletonMap(SupportedAbi.ARM64_V8A,312430001),false,null,null)),FormFactor.MOBILE)),
        Collections.emptySet(),Collections.emptySet(),Collections.emptyList(),
        new kotlin.jvm.functions.Function1<BytecodePatchContext,Unit>() { public Unit invoke(BytecodePatchContext context) { return apply(context); } },null,null,"NewX");

    static void require(boolean value,String message) {
        if (!value) throw new IllegalStateException(NAME + " / " + message);
    }

    static String ref(Instruction insn) {
        return insn instanceof ReferenceInstruction ? ((ReferenceInstruction)insn).getReference().toString() : "";
    }

    static MutableMethod method(BytecodePatchContext context,String owner,String name,String... params) {
        for (MutableMethod method : context.mutableClassDefBy(owner).getMethods()) {
            if (name.equals(method.getName()) && LIST.equals(method.getReturnType())
                    && Arrays.asList(params).equals(method.getParameterTypes()) && method.getImplementation()!=null)
                return method;
        }
        throw new IllegalStateException("Expected shared-video handler: " + owner + "->" + name);
    }

    // Locate the parsed postId within the video branch of the multiplexed callback.
    static int insertion(MutableMethod method, String unbox) {
        List<? extends Instruction> code = method.getImplementation().getInstructions();
        int found = -1;
        for (int i=0;i+2<code.size();i++) {
            if (!ref(code.get(i)).equals("postId")) continue;
            for (int j=i+1;j<Math.min(i+22,code.size()-1);j++) {
                if (!ref(code.get(j)).equals(unbox) || code.get(j+1).getOpcode()!=Opcode.MOVE_RESULT_WIDE) continue;
                boolean video=false, parsed=false;
                for (int k=i;k<j;k++)
                    if (ref(code.get(k)).equals("Lkotlin/text/j;->O(ILjava/lang/String;)Ljava/lang/Long;")) parsed=true;
                for (int k=j+2;k<Math.min(j+120,code.size());k++) {
                    if (ref(code.get(k)).equals("video_deeplink")) {video=true;break;}
                    if (code.get(k).getOpcode()==Opcode.RETURN_OBJECT) break;
                }
                if (parsed && video) {
                    require(found==-1,"ambiguous video postId");
                    found=j+2;
                }
            }
        }
        require(found>=2,"video postId lookup changed");
        return found;
    }

    static void routeToPost(MutableMethod method,int offset) {
        int postId=((OneRegisterInstruction)method.getImplementation().getInstructions().get(offset-1)).getRegisterA();
        // Save both words before reusing v0-v5 for the native constructor's six register slots.
        List<BuilderInstruction> body=Arrays.asList(
            new BuilderInstruction22x(Opcode.MOVE_WIDE_FROM16,1,postId),
            new BuilderInstruction21c(Opcode.NEW_INSTANCE,0,new ImmutableTypeReference(DETAIL)),
            new BuilderInstruction11n(Opcode.CONST_4,3,0),
            new BuilderInstruction11n(Opcode.CONST_4,4,0),
            new BuilderInstruction11n(Opcode.CONST_4,5,4),
            new BuilderInstruction3rc(Opcode.INVOKE_DIRECT_RANGE,0,6,CONSTRUCTOR),
            new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,0,0,0,0,0,LIST_OF),
            new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,0),
            new BuilderInstruction11x(Opcode.RETURN_OBJECT,0));
        for (BuilderInstruction instruction:body) method.getImplementation().addInstruction(offset++,instruction);
    }

    static Unit apply(BytecodePatchContext context) {
        require(VERSION.equals(context.getPackageMetadata().getVersionName())
            && "com.twitter.android".equals(context.getPackageMetadata().getPackageName()),"X 12.43.0 required");
        boolean ctor=false;
        for (Method m:context.classDefBy(DETAIL).getDirectMethods()) if (CONSTRUCTOR.equals(m)) ctor=true;
        require(ctor,"post detail constructor changed");
        MutableMethod tab=method(context,"Lcom/x/video/tab/deeplink/c;","d",ROUTE,"Landroid/net/Uri;");
        MutableMethod detail=method(context,"Lcom/x/calling/deeplink/a;","a",ROUTE);
        int tabAt=insertion(tab,"Ljava/lang/Long;->longValue()J");
        int detailAt=insertion(detail,"Ljava/lang/Number;->longValue()J");
        routeToPost(tab,tabAt);
        routeToPost(detail,detailAt);
        return Unit.INSTANCE;
    }
}
