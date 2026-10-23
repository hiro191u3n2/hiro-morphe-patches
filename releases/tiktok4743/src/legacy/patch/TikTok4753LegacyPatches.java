/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only
 * Native contracts audited against original global TikTok APKs:
 * 47.4.3 af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b
 * 47.5.3 6670ca3f93bfea379658cad3c27f05d3a380b23a0d82569cce684b7453e8ff3e.
 * Structural anchors are checked before any writes. Missing contracts fail patching.
 */
package app.hiro.tiktok4753.patches;

import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import app.morphe.patcher.util.smali.ExternalLabel;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;

/** Reimplemented native hooks; independent of upstream Settings and extension load order. */
public final class TikTok4753LegacyPatches {
    public static final String RUNTIME = "Lapp/hiro/tiktok4753/runtime/LegacyHooks;";
    private static final String VIEW = "Landroid/view/View;";
    private static final String AWEME = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;";
    private static final String PRIORITY = "Lcom/ss/android/ugc/feed/platform/componentmanager/data/FeedComponentPriorityData;";
    private static final String BUBBLE = "Lcom/ss/android/ugc/aweme/base/ui/assem/FeedSkylightBubbleAssem;";
    private static final String LEFT = "Lcom/ss/android/ugc/feed/platform/cell/interact/info/InteractInfoAreaComponent;";
    private static final String UPVOTE = "Lcom/ss/android/ugc/aweme/upvote/detail/whitebar/UpvoteVideoAssemNew;";
    private static final InstructionExtensions EDIT = InstructionExtensions.INSTANCE;
    /** Each name below was resolved from the corresponding APK's native instructions.
     * Package/version selects the table; each action still checks its structural anchors. */
    private static final class Contract {
        final String leftId,anchor,button,progress,seek,reason,inverse,view,dragExperiment,protectedVideo;
        final String previewGate,previewScene,previewCheck,previewAvailability,storyToggle,storyScene;
        Contract(boolean v4743) {
            leftId=v4743?"p1":"E1";
            anchor=v4743?"LX/07HS;":"LX/07Qm;";
            button=v4743?"LX/07Oc;":"LX/07Qi;";
            progress=v4743?"LX/06tY;":"LX/06Xo;";
            seek=v4743?"LX/06qw;":"LX/06WL;";
            reason=v4743?"LJI":"LJII";
            inverse=v4743?"LX/09X8;":"LX/09jc;";
            view=v4743?"LX/06oF;":"LX/06V9;";
            dragExperiment=v4743?"LX/09X3;":"LX/0B1p;";
            protectedVideo=v4743?"LX/09MK;":"LX/09Hp;";
            previewGate=v4743?"LJII":"LJIIIIZZ";
            previewScene=v4743?"LJIILJJIL":"LJIILL";
            previewCheck=v4743?"LJIJI":"LJIJJ";
            previewAvailability=v4743?"LX/0F2U;":"LX/0P1m;";
            storyToggle=v4743?"ar":"br";
            storyScene=v4743?"Yq":"Vq";
        }
    }
    private static final Contract CONTRACT_4743 = new Contract(true), CONTRACT_4753 = new Contract(false);
    private static Contract contract(BytecodePatchContext c) {
        validate(c);
        return "47.4.3".equals(c.getPackageMetadata().getVersionName())?CONTRACT_4743:CONTRACT_4753;
    }
    private TikTok4753LegacyPatches() {}
    interface Action { void run(BytecodePatchContext c); }
    public static Set<Pair<String, Set<String>>> compatibility() {
        return Collections.singleton(new Pair<String, Set<String>>("com.zhiliaoapp.musically", new LinkedHashSet<String>(Arrays.asList("47.4.3","47.5.3"))));
    }
    public static void validate(BytecodePatchContext c) {
        require("com.zhiliaoapp.musically".equals(c.getPackageMetadata().getPackageName()) &&
            Arrays.asList("47.4.3","47.5.3").contains(c.getPackageMetadata().getVersionName()), "requires the original global TikTok 47.4.3 or 47.5.3");
    }
    private static final BytecodePatch RUNTIME_PATCH = new BytecodePatch(null, null, true,
        compatibility(), Collections.emptySet(), Collections.emptySet(), new Supplier<InputStream>() {
            public InputStream get() { InputStream value=TikTok4753LegacyPatches.class.getResourceAsStream("/extensions/hiro_tiktok4753.mpe");
                require(value!=null,"missing independent runtime");return value; }
        }, new Function1<BytecodePatchContext,Unit>() { public Unit invoke(BytecodePatchContext c) {validate(c);return Unit.INSTANCE;} }, null);
    private static BytecodePatch patch(String name,String description,final Action action) {
        return new BytecodePatch(name,description,true,compatibility(),Collections.<Patch<?>>singleton(RUNTIME_PATCH),Collections.emptySet(),null,
            new Function1<BytecodePatchContext,Unit>() {public Unit invoke(BytecodePatchContext c){validate(c);return Unit.INSTANCE;}},
            new Function1<BytecodePatchContext,Unit>() {public Unit invoke(BytecodePatchContext c){validate(c);action.run(c);return Unit.INSTANCE;}});
    }
    public static final BytecodePatch HIDE_TEXT = patch("フィード左下の文字・再投稿欄を非表示（TikTok 47.4.3・47.5.3）",
        "投稿者名・投稿日・説明・翻訳・楽曲・サブスク案内・再投稿を含む左下情報欄を非表示。各対応版の実際の左側コンテナを対象にし、再表示にも対応します。右側ボタン・上部タブ・下部ナビは対象外です。", new Action(){public void run(BytecodePatchContext c){hideText(c);}});
    public static final BytecodePatch HIDE_PROMOTIONS = patch("フィード下部のCapCut・エフェクト誘導枠を完全遮断（TikTok 47.4.3・47.5.3）",
        "場所・施設・予約・CapCut・エフェクト等のアンカー枠、下部誘導ボタン、再投稿欄を非表示。各対応版の旧形式と新しいアンカー生成経路の両方を抑止します。",new Action(){public void run(BytecodePatchContext c){hidePromotions(c);}});
    public static final BytecodePatch SHOW_SEEKBAR = patch("動画のシークバーを常時表示（TikTok 47.4.3・47.5.3）",
        "通常動画のシークバーを表示。各対応版の表示判定・ドラッグ制限・非表示実験・表示更新の早期終了を修正します。LIVE・画像投稿等の元の除外条件は保持します。",new Action(){public void run(BytecodePatchContext c){showSeekbar(c);}});
    public static final BytecodePatch SHOW_THUMBNAIL = patch("シーク中の動画プレビューサムネイルを表示（TikTok 47.4.3・47.5.3）",
        "シーク中のプレビューにかかる実験・シーン制限を解除します。TikTokから実際のプレビュー画像が提供された動画だけが対象です。",new Action(){public void run(BytecodePatchContext c){showThumbnail(c);}});
    public static final BytecodePatch HIDE_STORIES = patch("フォロー中のストーリーズ件数を非表示（TikTok 47.4.3・47.5.3）",
        "フォロー中の上部ストーリーズ件数を枠ごと非表示。各対応版の実際のシーン判定と表示・アニメーション開始前を制御し、他のシーンを保持します。",new Action(){public void run(BytecodePatchContext c){hideStories(c);}});

    static void require(boolean yes,String message){if(!yes)throw new IllegalStateException("TikTok 47.4.3/47.5.3 native contract: "+message);}
    static List<String> params(MethodReference m){List<String> p=new ArrayList<>();for(CharSequence x:m.getParameterTypes())p.add(x.toString());return p;}
    static List<Instruction> body(Method m){require(m.getImplementation()!=null,"no code "+m);List<Instruction>b=new ArrayList<>();for(Instruction i:m.getImplementation().getInstructions())b.add(i);return b;}
    static Reference ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference():null;}
    static boolean string(Method m,String value){for(Instruction i:body(m)){Reference r=ref(i);if(r instanceof StringReference&&value.equals(((StringReference)r).getString()))return true;}return false;}
    static boolean calls(Method m,String owner,String name){for(Instruction i:body(m)){Reference r=ref(i);if(r instanceof MethodReference){MethodReference x=(MethodReference)r;if(x.getDefiningClass().equals(owner)&&x.getName().equals(name))return true;}}return false;}
    static boolean field(Method m,String owner,String name){for(Instruction i:body(m)){Reference r=ref(i);if(r instanceof FieldReference){FieldReference x=(FieldReference)r;if(x.getDefiningClass().equals(owner)&&x.getName().equals(name))return true;}}return false;}
    static MutableMethod method(BytecodePatchContext c,String owner,String name,String ret,String... p){
        MutableClass cl=c.mutableClassDefByOrNull(owner);require(cl!=null,"missing class "+owner);MutableMethod match=null;
        for(MutableMethod m:cl.getMethods())if(m.getName().equals(name)&&m.getReturnType().equals(ret)&&params(m).equals(Arrays.asList(p))){require(match==null,"ambiguous "+owner+name);match=m;}
        require(match!=null&&match.getImplementation()!=null,"missing "+owner+"->"+name+Arrays.toString(p)+ret);return match;
    }
    static void locals(Method m,int needed){int p=(m.getAccessFlags()&8)==0?1:0;for(CharSequence x:m.getParameterTypes())p+=x.toString().equals("J")||x.toString().equals("D")?2:1;require(m.getImplementation().getRegisterCount()-p>=needed,"locals "+m);}
    static void note(String feature,Method m){System.out.println("HIRO4753 "+feature+" "+m);}
    static void prepend(MutableMethod m,String code){EDIT.addInstructions(m,0,code);}
    public static boolean registerIsPreserved(Method m,int register){
        for(Instruction i:body(m))if(i.getOpcode().setsRegister()&&i instanceof OneRegisterInstruction){
            int written=((OneRegisterInstruction)i).getRegisterA();
            if(written==register||(i.getOpcode().setsWideRegister()&&written+1==register))return false;
        }
        return true;
    }
    static MethodReference rootAccessor(Method m){
        MethodReference found=null;
        for(Instruction i:body(m)){Reference r=ref(i);if(r instanceof MethodReference){MethodReference x=(MethodReference)r;
            if(x.getName().equals("getContainerView")&&x.getReturnType().equals(VIEW)&&params(x).isEmpty()){
                require(found==null||found.equals(x),"ambiguous container accessor "+m);found=x;
            }
        }}
        require(found!=null,"missing bound root accessor "+m);return found;
    }
    static int hideRootAtReturns(MutableMethod m,MethodReference viewCall,String followingScene){
        boolean followingOnly=followingScene!=null;
        locals(m,followingOnly?2:1);
        require((m.getAccessFlags()&8)==0&&params(m).equals(Collections.singletonList(VIEW)),"unexpected view lifecycle signature "+m);
        int self=m.getImplementation().getRegisterCount()-2;
        require(registerIsPreserved(m,self),"this register overwritten before view lifecycle returns "+m);
        List<Integer> at=new ArrayList<>();List<Instruction>b=body(m);for(int i=0;i<b.size();i++)if(b.get(i).getOpcode()==Opcode.RETURN_VOID)at.add(i);
        require(!at.isEmpty(),"no return for target view "+m);Collections.reverse(at);
        for(int index:at){
            // Replace the return itself so existing branch labels also enter the hook.
            // Locals are dead here; p0 was proven unchanged through the whole native body.
            MethodReference first=followingOnly?new ImmutableMethodReference(BUBBLE,followingScene,Collections.<String>emptyList(),"I"):viewCall;
            m.getImplementation().replaceInstruction(index,new BuilderInstruction3rc(Opcode.INVOKE_VIRTUAL_RANGE,self,1,first));
            String code=followingOnly?
                "move-result v0\nconst/4 v1, 0x2\nnop\ninvoke-virtual/range {p0 .. p0}, "+viewCall+"\n":"";
            code+="move-result-object v0\ninvoke-static {v0}, "+RUNTIME+"->hide(Ljava/lang/Object;)V\n";
            EDIT.addInstructions(m,index+1,code+"return-void");
            if(followingOnly){
                // A label assembled inside a snippet inserted at a nonzero offset can
                // retain that snippet's address. Bind the branch to this method's real
                // return after insertion so other scenes cannot jump back into setup.
                Label nativeReturn=m.getImplementation().newLabelForIndex(index+7);
                m.getImplementation().replaceInstruction(index+3,new BuilderInstruction22t(Opcode.IF_NE,0,1,nativeReturn));
            }
        }
        return at.size();
    }
    public static void hideText(BytecodePatchContext c){
        Contract t=contract(c);
        MutableMethod marker=method(c,LEFT,t.leftId,"Ljava/lang/String;");
        MutableMethod create=method(c,LEFT,"onViewCreated","V",VIEW);
        MutableMethod bind=method(c,UPVOTE,"xa","Z","Ljava/lang/Object;");
        require(string(marker,"left_container"),"information root no longer left_container");
        require(calls(create,"Lcom/bytedance/assem/arch/reused/ReusedUIAssem;","getContainerView"),"left root accessor changed");
        locals(bind,1);
        int count=hideRootAtReturns(create,rootAccessor(create),null);
        prepend(bind,"const/4 v0, 0x0\nreturn v0");
        note("left-root returns="+count,create);note("repost",bind);
    }
    public static void hidePromotions(BytecodePatchContext c){
        Contract t=contract(c);
        MutableMethod anchor=method(c,t.anchor,"LIZIZ","Ljava/lang/String;",t.anchor,PRIORITY,"Ljava/util/List;");
        MutableMethod button=method(c,t.button,"LIZJ","Ljava/util/Set;","Ljava/lang/String;","Ljava/lang/String;",PRIORITY);
        MutableMethod repost=method(c,UPVOTE,"xa","Z","Ljava/lang/Object;");
        require(field(anchor,PRIORITY,"priorityList")&&calls(anchor,"Lcom/ss/android/ugc/aweme/feed/model/AnchorCommonStruct;","getComponentKey"),"legacy anchor selection changed");
        require(string(button,"bottom_button")&&field(button,PRIORITY,"containerMap"),"bottom button container changed");
        final List<Method> modern=new ArrayList<>();
        c.classDefForEach(new Function1<ClassDef,Unit>(){public Unit invoke(ClassDef cl){for(Method m:cl.getMethods())if(isModernAnchorFactory(m))modern.add(m);return Unit.INSTANCE;}});
        require(modern.size()==1,"expected one modern anchor renderer, found "+modern.size());
        Method modernMethod=modern.get(0);MutableMethod factory=method(c,modernMethod.getDefiningClass(),modernMethod.getName(),modernMethod.getReturnType(),params(modernMethod).toArray(new String[0]));
        for(MutableMethod m:Arrays.asList(anchor,button,repost,factory))locals(m,1);
        prepend(anchor,"const/4 v0, 0x0\nreturn-object v0");
        prepend(button,"new-instance v0, Ljava/util/LinkedHashSet;\ninvoke-direct {v0}, Ljava/util/LinkedHashSet;-><init>()V\nreturn-object v0");
        prepend(factory,"new-instance v0, Ljava/util/ArrayList;\ninvoke-direct {v0}, Ljava/util/ArrayList;-><init>()V\nreturn-object v0");
        prepend(repost,"const/4 v0, 0x0\nreturn v0");
        for(MutableMethod m:Arrays.asList(anchor,button,factory,repost))note("promotions",m);
    }
    public static boolean isModernAnchorFactory(Method m){
        if(m.getImplementation()==null||(m.getAccessFlags()&8)==0||!m.getReturnType().equals("Ljava/util/List;"))return false;
        List<String> p=params(m);if(p.size()!=4||!p.get(0).equals(AWEME)||!p.get(1).equals("Z"))return false;
        int anchors=0;boolean key=false,copy=false,owner=false;
        for(Instruction i:body(m)){Reference r=ref(i);if(r instanceof MethodReference){MethodReference x=(MethodReference)r;
            if(x.getDefiningClass().equals(AWEME)&&x.getName().equals("getAnchors"))anchors++;
            if(x.getDefiningClass().equals("Lcom/ss/android/ugc/aweme/feed/model/AnchorCommonStruct;")&&x.getName().equals("getComponentKey"))key=true;
            if(x.getDefiningClass().equals("Ljava/util/ArrayList;")&&x.getName().equals("<init>")&&params(x).equals(Collections.singletonList("Ljava/util/Collection;")))copy=true;
        }else if(r instanceof FieldReference&&((FieldReference)r).getDefiningClass().equals("Lcom/ss/android/ugc/aweme/fcpanchor/access/VideoFeedMultiTagAssem;"))owner=true;}
        return anchors==1&&key&&copy&&owner;
    }
    public static void showSeekbar(BytecodePatchContext c){
        Contract t=contract(c);
        MutableMethod predicate=method(c,t.progress,"LIZJ","Z",AWEME);
        MutableMethod drag=method(c,t.progress,"LIZIZ","Z",AWEME);
        MutableMethod reason=method(c,t.seek,t.reason,"Z",AWEME);
        MutableMethod inverse=method(c,t.inverse,"LIZ","Z",AWEME);
        MutableMethod setter=method(c,t.view,"setSeekBarShowType","V","I");
        require(field(predicate,"Lcom/ss/android/ugc/aweme/feed/model/VideoControl;","showProgressBar"),"progress predicate lost its server flag");
        require(calls(drag,"Lcom/ss/android/ugc/aweme/feed/model/AwemeExtKt;","isPhotoMode")&&calls(drag,"Lcom/ss/android/ugc/aweme/feed/model/AwemeSaaSExtKt;","isLive"),"drag gate lost LIVE/photo exclusions");
        require(string(reason,"can not show seekbar, state: 6, hit inverse expriment")&&calls(reason,inverse.getDefiningClass(),inverse.getName()),"inverse experiment call changed");
        require(string(setter,"seekbar show type change, change to:")&&field(setter,t.view,"LLJJJIL"),"show type store changed");
        List<Integer> drafts=new ArrayList<>();List<Instruction> db=body(drag);
        for(int i=0;i<db.size();i++){Reference r=ref(db.get(i));if(db.get(i).getOpcode()==Opcode.IGET&&r instanceof FieldReference&&((FieldReference)r).getDefiningClass().equals("Lcom/ss/android/ugc/aweme/feed/model/VideoControl;")&&((FieldReference)r).getName().equals("draftProgressBar"))drafts.add(i);}
        require(drafts.size()==1,"draggable server flag count="+drafts.size());
        int experiment=-1;
        for(int i=0;i<db.size();i++){Reference r=ref(db.get(i));if(db.get(i).getOpcode()==Opcode.SGET_OBJECT&&r instanceof FieldReference&&((FieldReference)r).getDefiningClass().equals(t.dragExperiment)&&((FieldReference)r).getName().equals("LIZ")){require(experiment<0,"ambiguous drag experiment");experiment=i;}}
        require(experiment>2&&experiment<drafts.get(0)&&db.get(experiment-1).getOpcode()==Opcode.IF_NEZ,"drag experiment boundary changed");
        Reference lastExclusion=ref(db.get(experiment-3));
        require(lastExclusion instanceof MethodReference&&((MethodReference)lastExclusion).getDefiningClass().equals(t.protectedVideo)&&((MethodReference)lastExclusion).getName().equals("LJFF"),"protected-video exclusion moved");
        locals(predicate,1);locals(inverse,1);locals(setter,1);
        int self=setter.getImplementation().getRegisterCount()-2;
        require(self<=15,"show setter this register exceeds iput format");
        prepend(predicate,"if-eqz p0, :hiro_no_video\nconst/4 v0, 0x1\nreturn v0\n:hiro_no_video\nconst/4 v0, 0x0\nreturn v0");
        prepend(inverse,"const/4 v0, 0x1\nreturn v0");
        // The original experiment's enabled branch already permits null VideoControl.
        // Enter that behavior after all native unsupported/protected-video exclusions.
        // Do not force the entire predicate true: its earlier false branches remain live.
        EDIT.addInstructions(drag,experiment,"const/4 v0, 0x1\nreturn v0");
        // A previously stored zero makes another hidden -> zero rewrite compare equal and
        // skip the entire visible branch. Invalidate only zero requests at entry.
        EDIT.addInstructionsWithLabels(setter,0,
            "invoke-static/range {p1 .. p1}, "+RUNTIME+"->seekbarType(I)I\nmove-result p1\nif-nez p1, :hiro_original\nconst/4 v0, -0x1\niput v0, p0, "+t.view+"->LLJJJIL:I\n",
            new ExternalLabel[]{new ExternalLabel("hiro_original",setter.getImplementation().getInstructions().get(0))});
        for(MutableMethod m:Arrays.asList(predicate,drag,inverse,setter))note("seekbar",m);
    }
    public static void showThumbnail(BytecodePatchContext c){
        Contract t=contract(c);
        MutableMethod gate=method(c,t.seek,t.previewGate,"Z");
        MutableMethod loaderCheck=method(c,t.seek,t.previewCheck,"Z");
        require(field(gate,t.seek,"LLLL")&&calls(gate,t.seek,t.previewScene),"thumbnail experiment/scene gate changed");
        require(calls(loaderCheck,t.seek,t.previewGate)&&calls(loaderCheck,t.previewAvailability,"LIZ"),"real thumbnail availability guard changed");
        locals(gate,1);prepend(gate,"const/4 v0, 0x1\nreturn v0");note("preview gate (native loader check retained)",gate);
    }
    public static void hideStories(BytecodePatchContext c){
        Contract t=contract(c);
        MutableMethod create=method(c,BUBBLE,"onViewCreated","V",VIEW);
        MutableMethod toggle=method(c,BUBBLE,t.storyToggle,"V","Z","Z");
        MutableMethod scene=method(c,BUBBLE,t.storyScene,"I");
        MutableMethod clear=null;for(MutableMethod m:c.mutableClassDefBy(BUBBLE).getMethods())if(m.getName().equals("onClearModeEvent"))clear=m;
        require(clear!=null&&calls(clear,BUBBLE,t.storyToggle),"story clear-mode no longer calls the toggle");
        require(calls(toggle,BUBBLE,t.storyScene)&&calls(toggle,"Landroid/animation/AnimatorSet;","start"),"story scene/animation changed");
        List<Instruction> cb=body(create);int afterSuper=-1;
        for(int i=0;i<cb.size();i++){Reference r=ref(cb.get(i));if((cb.get(i).getOpcode()==Opcode.INVOKE_SUPER||cb.get(i).getOpcode()==Opcode.INVOKE_SUPER_RANGE)&&r instanceof MethodReference&&((MethodReference)r).getName().equals("onViewCreated")){require(afterSuper<0,"multiple story superclass calls");afterSuper=i+1;}}
        require(afterSuper>0,"missing original story superclass binding");locals(create,2);locals(toggle,2);
        MethodReference viewCall=rootAccessor(toggle);
        String guard="invoke-virtual/range {p0 .. p0}, "+BUBBLE+"->"+t.storyScene+"()I\nmove-result v0\nconst/4 v1, 0x2\nif-ne v0, v1, :hiro_story_original\n";
        int exits=hideRootAtReturns(create,rootAccessor(create),t.storyScene);
        EDIT.addInstructionsWithLabels(toggle,0,guard+"invoke-virtual/range {p0 .. p0}, "+viewCall+"\nmove-result-object v0\ninvoke-static {v0}, "+RUNTIME+"->hide(Ljava/lang/Object;)V\nreturn-void\n",
            new ExternalLabel[]{new ExternalLabel("hiro_story_original",toggle.getImplementation().getInstructions().get(0))});
        note("following stories create exits="+exits+" (native initialization retained)",create);note("following stories show/animation",toggle);note("following scene",scene);
    }
}
