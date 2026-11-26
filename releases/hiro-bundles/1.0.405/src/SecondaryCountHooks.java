package app.hiro.newxui.patches;

import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import java.util.*;

/** Independent native counters which bypass the shared X count formatter. */
final class SecondaryCountHooks {
    private static final String RUNTIME = "Lapp/hiro/newxui/ViewCountFormat;";
    private enum Site {
        CREATOR("Lcom/x/creator/subscriptions/i1;", "c", "2609612d309d5ea5358c7a08c4daddd435cc07d70d754307f803935eb5475503",0,4,0,"J"),
        PROMOTION_REACH("Lcom/x/quickpromote/c0;","o","9b91c51bf9e40461eba96510f87dbe25fad3cfe86708d90ca4a73a45906496c1",0,6,0,"I"),
        PROMOTION_TOTAL("Lcom/x/quickpromote/x1;","a","76ba5c2be3443dec6d40f4c434225c4ee99a770d721522173965ea9371ea45c5",0,4,0,"J"),
        SPORTS("Lcom/google/android/gms/internal/mlkit_vision_common/k8;","b","ca806723aaa724540073677ccee9a2118531d2f717263e1837e0e702c63fc971",0,2,0,"J"),
        SPACE("Lcom/x/spaces/notification/c;","b","a1900a679ed1ade8a1d176fe0c0c34c4427ce9e48aae81eb51af982ad016de63",132,3,1,"I"),
        CAMERA("Lcom/x/camera/capture/a4;","f","ccdbab34a3eb102db77ab9d8c8351816171b025e0e208a326ee0364baa71965f",599,9,11,"J"),
        STOCK_CARD("Lai/x/grok/cards/j6;","a","a894dff1fc3abee55b4cf86f504b4ce0f5668b66db8e8f6d433cbcc765553f7b",6,0,2,"D");

        final String type,name,fingerprint,parameter;
        final int insert,raw,temp;
        Site(String type,String name,String fingerprint,int insert,int raw,int temp,String parameter) {
            this.type=type;this.name=name;this.fingerprint=fingerprint;this.insert=insert;
            this.raw=raw;this.temp=temp;this.parameter=parameter;
        }

        Method find(ClassDef cls) {
            for (Method m:cls.getMethods()) {
                if (name.equals(m.getName()) && m.getImplementation()!=null
                        && fingerprint.equals(ChatRecommendationsHook.fingerprint(ChatRecommendationsHook.code(m)))) return m;
            }
            throw new IllegalStateException("Xの件数表示を確認できません: "+type+"->"+name);
        }

        void apply(MutableMethodImplementation body) {
            int pos=insert;
            if (this==STOCK_CARD) {
                // Use the native unscaled double branch, retaining the currency, sign and decimals.
                Label unscaled=body.newLabelForIndex(30);
                body.addInstruction(pos++,new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,
                        new ImmutableMethodReference(UiOptionsPatch.OPTIONS,"fullViewCounts",Collections.emptyList(),"Z")));
                body.addInstruction(pos++,new BuilderInstruction11x(Opcode.MOVE_RESULT,temp));
                body.addInstruction(pos,new BuilderInstruction21t(Opcode.IF_NEZ,temp,unscaled));
                return;
            }

            Label original=body.newLabelForIndex(insert);
            Label resume=this==SPACE ? body.newLabelForIndex(161) : this==CAMERA ? body.newLabelForIndex(651) : null;
            boolean wide="J".equals(parameter);
            body.addInstruction(pos++,new BuilderInstruction35c(Opcode.INVOKE_STATIC,wide?2:1,raw,wide?raw+1:0,0,0,0,
                    new ImmutableMethodReference(RUNTIME,"groupIfEnabled",Collections.singletonList(parameter),"Ljava/lang/String;")));
            body.addInstruction(pos++,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,temp));
            body.addInstruction(pos++,new BuilderInstruction21t(Opcode.IF_EQZ,temp,original));
            if (resume==null) {
                body.addInstruction(pos,new BuilderInstruction11x(Opcode.RETURN_OBJECT,temp));
            } else {
                body.addInstruction(pos++,new BuilderInstruction12x(Opcode.MOVE_OBJECT,this==SPACE?9:6,temp));
                body.addInstruction(pos,new BuilderInstruction30t(Opcode.GOTO_32,resume));
            }
        }
    }

    static void apply(BytecodePatchContext context) {
        for (Site site:Site.values()) {
            MutableMethod method=(MutableMethod)site.find(context.mutableClassDefBy(site.type));
            site.apply(method.getImplementation());
        }
    }
}
