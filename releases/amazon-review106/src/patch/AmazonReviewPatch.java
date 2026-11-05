package app.hiro.amazonreviews.patches;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Six narrowly guarded hooks, including in-document updates and cached fragment display. */
public final class AmazonReviewPatch {
    private AmazonReviewPatch() {}
    public static final String NAME = "商品検索を評価件数の多い順に表示";
    public static final String PACKAGE = "com.amazon.mShop.android.shopping";
    public static final String VERSION = "32.19.0.100";
    public static final String CODE = "1243250306";
    public static final String RUNTIME = "Lapp/hiro/amazonreviews/ReviewCountRuntime;";
    public static final String EXTENSION = "/extensions/amazon_review_sort.mpe";
    public static final String[][] TARGETS = {
        {"Lcom/amazon/mShop/web/MShopWebView;", "loadUrl", "Ljava/lang/String;", "Z"},
        {"Lcom/amazon/mobile/mash/MASHWebView;", "loadUrl", "Ljava/lang/String;", "Ljava/util/Map;", "Z"},
        {"Lcom/amazon/mShop/web/MShopWebViewClient;", "onPageCommitVisible", "Landroid/webkit/WebView;", "Ljava/lang/String;"},
        {"Lcom/amazon/mShop/web/MShopWebViewClient;", "onPageFinished", "Landroid/webkit/WebView;", "Ljava/lang/String;"},
        {"Lcom/amazon/mShop/web/MShopWebViewClient;", "doUpdateVisitedHistory", "Landroid/webkit/WebView;", "Ljava/lang/String;", "Z"},
        {"Lcom/amazon/mShop/web/InteractionWebFragment;", "onFragmentPostShown"}
    };
    // The two pinned SDKs serialize the same original method slightly differently.
    public static final String[][] HASHES = {
        {"62a5b124468fa2ae323ffe267e3c87fd3d9ee43ba00763b521c21c6536651361", "417c010af9dc52436c3cf64924b2a169ade62e575ab2cb1caaaa76659ece9834"},
        {"16d131eeb4a76046bccbfef3821c3522a8bf32502e5cfc3a765416d467a3faca", "ea421b3f56c550351c6a02e5364d0a1b1d145fdb4c9b8d02650bfd1a792ae45c"},
        {"5efaa0cd6d95f82732b88c0256ae32d7ed5be4e646baa3db104fd68a173db22d"},
        {"1836b3d9464c4f4f8d7fae1638c05b9a1558f5d511d451646e6d39109350c37b", "b3da008c02ca8010eb17bd5ff64c5ced76d0422c7ea52dca6845b73b44653646"},
        {"46da67545458bc2a25b542394c33eea6ed46037035b7311d3e2947d3d3cccde4"}, {"cb774c0b75ff4fcb680fa401e0a539764f7fdc51f8662180def30631da0a1671"}
    };
    public static Set<Pair<String,Set<String>>> compatibility() {
        return Collections.singleton(new Pair<String,Set<String>>(PACKAGE, Collections.singleton(VERSION)));
    }
    private static final BytecodePatch GUARD = new BytecodePatch(null, null, false, compatibility(),
        Collections.<Patch<?>>emptySet(), Collections.emptySet(), null,
        new Function1<BytecodePatchContext,Unit>() { public Unit invoke(BytecodePatchContext c) {
            validatePackage(c.getPackageMetadata().getPackageName(), c.getPackageMetadata().getVersionName(), c.getPackageMetadata().getVersionCode());
            require(c.classDefByOrNull(RUNTIME) == null, "Review sort runtime already installed");
            for (int n=0;n<TARGETS.length;n++) validate(find(c.classDefBy(TARGETS[n][0]), n), n);
            return Unit.INSTANCE;
        }}, null);
    public static final BytecodePatch PATCH = new BytecodePatch(NAME,
        "Amazon 32.19.0.100 添付改造版用。読込通知とJS応答診断を追加。100商品を目安に先読みし、評価件数を取得できた商品を多い順に並べて固定します。後着分や件数未取得分は分け、操作ボタンで追加分を含めて並べ直せます。取得数は検索結果や通信状況により異なります。星のみの評価を含み、全検索結果の全件順位は保証しません。v1.0.0〜v1.0.5適用済みAPKへの重ね掛け不可。元の添付APKから適用してください。動画で順序不成立を確認していますが原因は未確定で、v1.0.6の実機表示は未確認。",
        true, compatibility(), Collections.<Patch<?>>singleton(GUARD), Collections.emptySet(),
        new Supplier<InputStream>() { public InputStream get() {
            InputStream in=AmazonReviewPatch.class.getResourceAsStream(EXTENSION);
            require(in != null, "Missing review sort runtime"); return in;
        }}, new Function1<BytecodePatchContext,Unit>() { public Unit invoke(BytecodePatchContext c) {
            for (int n=0;n<TARGETS.length;n++) {
                MutableClass cl=c.mutableClassDefBy(TARGETS[n][0]);
                for (MutableMethod m:cl.getMethods()) if (matches(m,n)) apply(m,n);
            }
            return Unit.INSTANCE;
        }}, null);
    public static void require(boolean ok,String why) { if(!ok) throw new IllegalStateException(why); }
    public static void validatePackage(String p,String v,String code) {
        require(PACKAGE.equals(p) && VERSION.equals(v) && CODE.equals(code), "Requires supplied Amazon 32.19.0.100 (1243250306)");
    }
    public static boolean matches(Method m,int n) {
        String[] t=TARGETS[n];
        return t[0].equals(m.getDefiningClass()) && t[1].equals(m.getName()) && "V".equals(m.getReturnType())
            && m.getParameterTypes().equals(Arrays.asList(Arrays.copyOfRange(t,2,t.length)));
    }
    public static Method find(ClassDef c,int n) {
        require(c != null,"Missing target class"); Method out=null;
        for(Method m:c.getMethods()) if(matches(m,n)) { require(out==null,"Duplicate target"); out=m; }
        require(out!=null,"Missing target method "+n); return out;
    }
    public static String fingerprint(Method m) {
        try {
            MethodImplementation body=m.getImplementation();
            require(body!=null,"Missing body");
            ImmutableMethodImplementation normalized=new ImmutableMethodImplementation(body.getRegisterCount(),body.getInstructions(),body.getTryBlocks(),Collections.emptyList());
            ImmutableMethod method=new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),normalized);
            ImmutableClassDef wrapper=new ImmutableClassDef(m.getDefiningClass(),1,"Ljava/lang/Object;",Collections.emptyList(),null,Collections.emptySet(),Collections.emptyList(),Collections.singleton(method));
            MemoryDataStore store=new MemoryDataStore();
            try {
                DexPool.writeTo(store,new ImmutableDexFile(Opcodes.forApi(29),Collections.singleton(wrapper)));
                byte[] hash=MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(store.getBuffer(),store.getSize()));
                StringBuilder s=new StringBuilder(); for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255)); return s.toString();
            } finally {store.close();}
        } catch(Exception e) {throw new IllegalStateException("Fingerprint failed",e);}
    }
    public static void validate(Method m,int n) {
        require(matches(m,n) && m.getAccessFlags()==(n==5?4:1) && m.getImplementation()!=null,"Target declaration differs "+n);
        require(Arrays.asList(HASHES[n]).contains(fingerprint(m)),"Unverified or already modified Amazon method "+n);
    }
    public static void apply(MutableMethod m,int n) {
        validate(m,n);
        MutableMethodImplementation body=m.getImplementation();
        int p1=n==5?0:body.getRegisterCount()-(TARGETS[n].length-1)+1;
        if(n<2) {
            ImmutableMethodReference call=new ImmutableMethodReference(RUNTIME,"normalizeUrl",Collections.singletonList("Ljava/lang/String;"),"Ljava/lang/String;");
            body.addInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,p1,0,0,0,0,call));
            body.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,p1));
        } else {
            ImmutableMethodReference call=new ImmutableMethodReference(RUNTIME,"inject",Arrays.asList("Landroid/webkit/WebView;","Ljava/lang/String;"),"V");
            // Finished starts with original ad-block + price-chart injections (3 instructions).
            // Commit-visible must be hooked before its parameters are reused as local registers.
            // Fragment-shown obtains the WebView and navigation URL in v0/v1 and runs
            // its existing price chart injection first. Its safe insertion boundary is 5.
            int index=n==3?3:n==5?5:0;
            body.addInstruction(index,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,p1,p1+1,0,0,0,call));
        }
    }
}
