import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import java.nio.file.*;
import java.util.*;

/** Audit the two final 3.84.21 APKs. Compile with the previous release's
 * ZoomApplicationAudit and ForegroundApplicationAudit. Use equal patch selections.
 * Arguments are previous.apk candidate.apk result.json.
 */
public final class NavigationPreviewAudit {
    static final String CLIENT = "La90;";
    static final String POST = "Landroid/view/View;->post(Ljava/lang/Runnable;)Z";
    static final String BRIDGE = "Le80;->hiroPostAfterPageFinished(Landroid/view/View;Ljava/lang/Runnable;)V";
    static final String DELAYED = "Landroid/view/View;->postDelayed(Ljava/lang/Runnable;J)Z";
    static final String CAPTURE = "Loz0;->c(Lh26;)Landroid/graphics/Bitmap;";
    static final String DIRTY = "Lh26;->T:Z";
    static void require(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }
    static Method get(Map<String,ClassDef> defs, String owner, String name) {
        Method found = null;
        for (Method m : defs.get(owner).getMethods()) {
            if (!m.getName().equals(name)) continue;
            require(found == null, owner + name + " ambiguous"); found = m;
        }
        require(found != null,owner + name + " missing");
        return found;
    }
    static int reference(Method method, String descriptor, boolean required) {
        int found = -1;
        List<Instruction> code = ZoomApplicationAudit.ins(method);
        for (int i = 0; i < code.size(); i++) {
            if (!ZoomApplicationAudit.ref(code.get(i)).equals(descriptor)) continue;
            require(found < 0, "Ambiguous " + descriptor + " in " + method); found = i;
        }
        require((found >= 0) == required, descriptor + " in " + method); return found;
    }
    static void verifyCase(Method method, int selection, String expected, boolean expectCapture) {
        List<Instruction> code = ZoomApplicationAudit.ins(method);
        int branch = -1;
        for (int i = 0; i < code.size(); i++) if (code.get(i).getOpcode() == Opcode.PACKED_SWITCH) {
            require(branch < 0, "Multiple switch sites " + method); branch = i;
        }
        require(branch >= 0,"Missing selector switch");
        Map<Integer,Integer> offsets = ZoomApplicationAudit.addresses(code);
        int entry = branch+1;
        SwitchPayload payload = (SwitchPayload) code.get(ZoomApplicationAudit.target(code,branch));
        for (SwitchElement item : payload.getSwitchElements()) if (item.getKey() == selection)
            entry = offsets.get(ZoomApplicationAudit.pc(code,branch)+item.getOffset());
        Set<Integer> pending = new HashSet<>(), seen = new HashSet<>();
        pending.add(entry); boolean called = false, captured = false;
        while (!pending.isEmpty()) {
            int pc = pending.iterator().next(); pending.remove(pc); if (!seen.add(pc)) continue;
            Instruction ins = code.get(pc);
            String ref = ZoomApplicationAudit.ref(ins);
            called |= ref.equals(expected); captured |= ref.equals(CAPTURE);
            if (ins.getOpcode().canContinue()) pending.add(pc+1);
            if (ins instanceof OffsetInstruction) pending.add(ZoomApplicationAudit.target(code,pc));
        }
        require(called && captured == expectCapture,"Unexpected path for "+method+" selector "+selection);
    }
    static void verifyFocusPaths(Map<String,ClassDef> defs) {
        Method finished = get(defs,CLIENT,"onPageFinished");
        int delayed = reference(finished,DELAYED,true);
        List<Instruction> code = ZoomApplicationAudit.ins(finished);
        require(ZoomApplicationAudit.ref(code.get(delayed-1)).startsWith("Lv80;-><init>("),"Other delayed task");
        // 3.84.21 identifies the connected notification with selector 1.
        int selector = ZoomApplicationAudit.args(code.get(delayed-1))[3];
        for (int p=delayed-2; p>=0; p--) {
            Instruction ins = code.get(p);
            if (!(ins instanceof OneRegisterInstruction) || !ins.getOpcode().setsRegister()
                    || ((OneRegisterInstruction)ins).getRegisterA()!=selector) continue;
            require(ins instanceof NarrowLiteralInstruction && ((NarrowLiteralInstruction)ins).getNarrowLiteral()==1,
                    "Delayed task selector changed"); selector=-1; break;
        }
        require(selector==-1,"Missing delayed task selector initialization");
        int dirty = reference(finished,DIRTY,true);
        require(code.get(dirty).getOpcode()==Opcode.IPUT_BOOLEAN,"Page finish no longer marks thumbnail dirty");
        Method runnable = get(defs,"Le80;","run");
        verifyCase(runnable,1,CAPTURE,true);
        verifyCase(get(defs,"Lv80;","run"),1,
                "Ljp/ejimax/berrybrowser/browser_impl/BrowserActivity;->K0(Lh26;Ljava/lang/String;)V",false);
        verifyCase(get(defs,"Lv80;","run"),0,CAPTURE,true);
        reference(get(defs,"Lhm6;","C"),CAPTURE,true);
        reference(get(defs,"Loz0;","b"),"Lh26;->m(Landroid/graphics/Canvas;)V",true);
        reference(get(defs,"Loz0;","c"),"Lh26;->m(Landroid/graphics/Canvas;)V",true);
        reference(get(defs,"Lh26;","m"),"Landroid/view/View;->draw(Landroid/graphics/Canvas;)V",true);
        reference(get(defs,"Lfq5;","h"),POST,true);
    }
    static int runBridge(Method bridge, boolean preview, int selector) {
        List<Instruction> code = ZoomApplicationAudit.ins(bridge);
        require(bridge.getAccessFlags()==9 && bridge.getImplementation().getRegisterCount()==4
                && bridge.getImplementation().getTryBlocks().isEmpty(),"Guard signature or exception behavior changed");
        int pc=0, calls=0, steps=0; Object[] regs={null,null,"frame",preview ? "preview" : "other"};
        while (++steps <= 20) {
            Instruction ins=code.get(pc);
            int a=ins instanceof OneRegisterInstruction ? ((OneRegisterInstruction)ins).getRegisterA() : -1;
            int b=ins instanceof TwoRegisterInstruction ? ((TwoRegisterInstruction)ins).getRegisterB() : -1;
            int next=pc+1;
            switch(ins.getOpcode()) {
                case INSTANCE_OF:
                    require(ZoomApplicationAudit.ref(ins).equals("Le80;"),"Wrong tested class");
                    regs[a]="preview".equals(regs[b]) ? 1 : 0; break;
                case IF_EQZ: if ((Integer)regs[a]==0) next=ZoomApplicationAudit.target(code,pc); break;
                case MOVE_OBJECT: regs[a]=regs[b];break;
                case CHECK_CAST:
                    require("preview".equals(regs[a]) && ZoomApplicationAudit.ref(ins).equals("Le80;"),"Unsafe cast"); break;
                case IGET_BYTE:
                    require("preview".equals(regs[b])&&ZoomApplicationAudit.ref(ins).equals("Le80;->o:B"),"Wrong discriminator");
                    regs[a]=selector; break;
                case IF_LTZ: if ((Integer)regs[a]<0) next=ZoomApplicationAudit.target(code,pc);break;
                case CONST_4: regs[a]=((NarrowLiteralInstruction)ins).getNarrowLiteral();break;
                case IF_GT: if ((Integer)regs[a]>(Integer)regs[b]) next=ZoomApplicationAudit.target(code,pc);break;
                case INVOKE_VIRTUAL:
                    require(ZoomApplicationAudit.ref(ins).equals(POST)&&Arrays.equals(ZoomApplicationAudit.args(ins),new int[]{2,3}),
                            "Guard forwards the wrong invocation/arguments");
                    require("frame".equals(regs[2])&&(preview?"preview":"other").equals(regs[3]),"Guard clobbered forwarded arguments");
                    calls++; break;
                case RETURN_VOID: return calls;
                default: throw new AssertionError("Unexpected guard opcode " + ins.getOpcode());
            }
            pc=next;
        }
        throw new AssertionError("Guard did not terminate");
    }
    public static void main(String[] args) throws Exception {
        Map<String,ClassDef> before=ZoomApplicationAudit.load(args[0]);
        Map<String,ClassDef> after=ZoomApplicationAudit.load(args[1]);
        require(before.keySet().equals(after.keySet()),"Class inventory changed");
        Method oldFinish=get(before,CLIENT,"onPageFinished"), newFinish=get(after,CLIENT,"onPageFinished");
        int changed = reference(oldFinish,POST,true);
        Method bridge=get(after,"Le80;","hiroPostAfterPageFinished");
        require(ZoomApplicationAudit.desc(bridge).equals(BRIDGE),"Unexpected wrapper signature");
        for (int key=Byte.MIN_VALUE; key<=Byte.MAX_VALUE; key++)
            require(runBridge(bridge,true,key)==((key==0||key==1)?0:1),"Wrong guard result for "+key);
        require(runBridge(bridge,false,0)==1,"Unrelated runnable dropped");
        int wrapped=reference(newFinish,BRIDGE,true);
        List<Instruction> oldCode=ZoomApplicationAudit.ins(oldFinish), newCode=ZoomApplicationAudit.ins(newFinish);
        require(changed==wrapped && oldCode.get(changed).getOpcode()==Opcode.INVOKE_VIRTUAL
                && (newCode.get(wrapped).getOpcode()==Opcode.INVOKE_STATIC||newCode.get(wrapped).getOpcode()==Opcode.INVOKE_STATIC_RANGE)
                && Arrays.equals(ZoomApplicationAudit.args(oldCode.get(changed)),ZoomApplicationAudit.args(newCode.get(wrapped))),
                "Guard call moved or different registers supplied");
        require(!newCode.get(wrapped+1).getOpcode().name.startsWith("move-result"),"post result in use");
        reference(newFinish,POST,false);
        // Substitute the original invocation. The shared verifier compares registers,
        // operands, branch destinations, method metadata, and exception regions.
        ForegroundApplicationAudit.unchanged(oldFinish,newFinish,Set.of(),Map.of(wrapped,oldCode.get(changed)));
        verifyFocusPaths(after);
        int methods=0;
        for (ClassDef orig : before.values()) {
            ClassDef candidate=after.get(orig.getType());
            List<Method> methodsToCompare = new ArrayList<>();
            for (Method method : candidate.getMethods()) {
                if (ZoomApplicationAudit.desc(method).equals(BRIDGE)) continue;
                methods++;
                methodsToCompare.add(ZoomApplicationAudit.desc(method).equals(ZoomApplicationAudit.desc(newFinish)) ? oldFinish : method);
            }
            ClassDef normalized = new ImmutableClassDef(candidate.getType(),candidate.getAccessFlags(),candidate.getSuperclass(),
                    candidate.getInterfaces(),candidate.getSourceFile(),candidate.getAnnotations(),candidate.getFields(),methodsToCompare);
            require(Arrays.equals(ZoomApplicationAudit.canonical(orig),ZoomApplicationAudit.canonical(normalized)),
                    "Unexpected DEX change in " + orig.getType());
        }
        Map<String,byte[]> origResources=ZoomApplicationAudit.resources(args[0]), resources=ZoomApplicationAudit.resources(args[1]);
        require(origResources.keySet().equals(resources.keySet()),"APK resource inventory changed");
        for(String key : resources.keySet()) require(Arrays.equals(origResources.get(key),resources.get(key)),"Resource changed: "+key);
        String report=String.format(Locale.ROOT,
                "{\"result\":\"PASS\",\"app_version\":\"3.84.21\",\"class_count\":%d,\"method_count\":%d,\"resource_count\":%d,"
                + "\"replaced_instructions\":1,\"new_methods\":1,\"guard_inputs_tested\":257,\"only_changed_method\":\"%s\",\"guarded_invocation\":\"%s\","
                + "\"thumbnail_dirty_marker_retained\":true,\"tab_detach_and_manual_capture_retained\":true,"
                + "\"selector_1_thumbnail_retained\":true,\"second_delayed_task_is_connected_notification\":true}%n",
                before.size(),methods,resources.size(),ZoomApplicationAudit.desc(newFinish),POST);
        Files.writeString(Path.of(args[2]),report);System.out.print(report);
    }
}
