/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
package app.hiro.tiktoklink.patches;

import java.util.*;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;

/** A gate after the native detail's key/comment/child consumers, before navigation. */
public final class SharedBackPatch {
    public static final String OWNER="Lcom/ss/android/ugc/aweme/detail/ui/saf/DetailSafRootFragment;";
    public static final String RUNTIME="Lapp/hiro/tiktoklink/runtime/SharedLinkExit;";
    public static final String BACK_SHA="9b028b0cd5e80952f7f4afe0fe64a7b25ae01998ba5abe9608505709d162752e";
    public static final String RETURN_SHA="ba55adb411b9317a36ad80d5c542f93427c731c1ec0955c27a697a3153fdb7d2";
    public static final String RESUME_SHA="fa6ef575dfa37943e1e4c02a1985dc82a372754a9cf200eae495b858d681c2a9";
    public static final String DESTROY_SHA="777f0dffad1cfc65e331a9e2a9a16fda4111a07df8d6fa87affd59f17b330367";
    public static final String ROUTE_SHA="0a38d713d2bd9b38b710c663f4b32e44bdf27723733a84d32576fd236aade1a8";
    private static final String OBJECT="Ljava/lang/Object;",INTENT="Landroid/content/Intent;",URI="Landroid/net/Uri;";
    private SharedBackPatch() {}
    private static void require(boolean ok,String why) { TikTokLinkDirectPatch.require(ok,why); }
    private static MutableMethod pin(BytecodePatchContext c,String name,String result,String sha,int registers) {
        MutableMethod m=TikTokLinkDirectPatch.method(c,OWNER,name,result);
        require(sha.equals(FastRoutePatch.fingerprint(m)),"native shared-Back full-method fingerprint differs: "+name);
        require(m.getImplementation().getRegisterCount()==registers,"native shared-Back register layout differs: "+name);
        return m;
    }
    private static void runtime(BytecodePatchContext c,String name,String result,String...parameters) {
        Method m=TikTokLinkDirectPatch.method(c,RUNTIME,name,result,parameters);
        require((m.getAccessFlags()&9)==9,"missing public static shared-link runtime method: "+name);
    }
    private static int at(List<Instruction> body,int address) {
        int pc=0;for(int i=0;i<body.size();i++){if(pc==address)return i;pc+=body.get(i).getCodeUnits();}
        throw new IllegalStateException("missing shared-Back native address "+address);
    }
    public static Plan prepare(BytecodePatchContext c) {
        runtime(c,"prepare","V",INTENT,URI);runtime(c,"transfer","V",INTENT,INTENT);
        runtime(c,"isPrepared","Z",INTENT);runtime(c,"bind","V",OBJECT);runtime(c,"forget","V",OBJECT);
        runtime(c,"tryExit","Z",OBJECT);runtime(c,"suppressNativeReturn","Z",OBJECT);
        MutableMethod back=pin(c,"onBackPressed","Z",BACK_SHA,7);
        MutableMethod returning=pin(c,"bY","V",RETURN_SHA,25);
        MutableMethod resume=pin(c,"onResume","V",RESUME_SHA,12);
        MutableMethod destroy=pin(c,"onDestroy","V",DESTROY_SHA,15);
        pin(c,"eY",INTENT,ROUTE_SHA,2);
        List<Instruction> code=TikTokLinkDirectPatch.code(back);
        int gate=at(code,0x00b3);Instruction target=code.get(gate);
        require(gate==94&&target.getOpcode()==Opcode.INVOKE_INTERFACE
                &&"LX/0yWM;->LJIILJJIL()V".equals(TikTokLinkDirectPatch.ref(target))
                &&((FiveRegisterInstruction)target).getRegisterCount()==1
                &&((FiveRegisterInstruction)target).getRegisterC()==0,"native child navigation boundary changed");
        require(code.get(93).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)code.get(93)).getRegisterA()==0
                &&((OffsetInstruction)code.get(93)).getCodeOffset()==-37,"native valid-child fallback changed");
        require(code.get(95).getOpcode()==Opcode.GOTO&&((OffsetInstruction)code.get(95)).getCodeOffset()==-42
                &&code.get(at(code,0x008c)).getOpcode()==Opcode.CONST_4
                &&((NarrowLiteralInstruction)code.get(at(code,0x008c))).getNarrowLiteral()==1,
                "native navigation consumed result changed");
        List<Instruction> life=TikTokLinkDirectPatch.code(resume);
        require(life.size()==109&&life.get(108).getOpcode()==Opcode.RETURN_VOID,"native resume completion changed");
        // Both lifecycle methods are Fragment methods; OneBack's Activity wrappers
        // cannot rename them. Preserve all original instructions and switch alignment.
        return new Plan(back,returning,resume,destroy,gate);
    }
    private static BuilderInstruction invoke(String name,String result,int receiver) {
        ImmutableMethodReference ref=new ImmutableMethodReference(RUNTIME,name,Collections.singletonList(OBJECT),result);
        if(receiver<16)return new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,receiver,0,0,0,0,ref);
        return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,receiver,1,ref);
    }
    public static final class Plan {
        private final MutableMethod back,returning,resume,destroy;private final int gate;
        private Plan(MutableMethod b,MutableMethod r,MutableMethod s,MutableMethod d,int g) {
            back=b;returning=r;resume=s;destroy=d;gate=g;
        }
        public void apply() {
            MutableMethodImplementation b=back.getImplementation();
            Label nativeCall=b.newLabelForIndex(gate);
            b.addInstruction(gate,invoke("tryExit","Z",6));
            b.addInstruction(gate+1,new BuilderInstruction11x(Opcode.MOVE_RESULT,1));
            b.addInstruction(gate+2,new BuilderInstruction21t(Opcode.IF_EQZ,1,nativeCall));
            b.addInstruction(gate+3,new BuilderInstruction11x(Opcode.RETURN,3));

            b=returning.getImplementation();Label nativeReturn=b.newLabelForIndex(0);
            b.addInstruction(0,invoke("suppressNativeReturn","Z",24));
            b.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
            b.addInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,0,nativeReturn));
            b.addInstruction(3,new BuilderInstruction10x(Opcode.RETURN_VOID));

            // Existing branches target the final RETURN. Replace that instruction
            // so every arrival executes bind; inserting before it would be skipped.
            b=resume.getImplementation();b.replaceInstruction(108,invoke("bind","V",11));
            b.addInstruction(109,new BuilderInstruction10x(Opcode.RETURN_VOID));
            b=destroy.getImplementation();b.addInstruction(0,invoke("forget","V",14));
            b.addInstruction(1,new BuilderInstruction10x(Opcode.NOP)); // Four code units retain switch-payload alignment.
            System.out.println("HIRO_LINK_DIRECT shared-detail Back gate after native consumers; lifecycle-bound issued session; no additional Back callback");
        }
    }
}
