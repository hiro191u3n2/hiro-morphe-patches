package app.hiro.berry.patches;

import app.hiro.berry.patches.BerryProfiles.Profile;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Field;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;

/** Resolve app-owned names by typed relationships and Android API call sites.
 * No version string, obfuscated name, resource ID, or arbitrary first match is used.
 * All resolution runs on an immutable pre-patch snapshot. Captured hashes then
 * protect those selected methods from intervening changes during patch execution.
 */
public final class GenericBerryResolver {
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private static final String WEB = "Landroid/webkit/WebView;";
    private static final String MOTION = "Landroid/view/MotionEvent;";
    private static final String BUNDLE = "Landroid/os/Bundle;";
    private static final String BITMAP = "Landroid/graphics/Bitmap;";
    private static final String FRAME = "Landroid/widget/FrameLayout;";
    private static final String ACTIVITY = "Ljp/ejimax/berrybrowser/browser_impl/BrowserActivity;";
    private final List<ClassDef> classes;
    private final Map<String, ClassDef> byType = new LinkedHashMap<>();
    private final Profile profile;
    private ClassDef web, controller, intercept;
    private Field webField, controllerField;
    private boolean foregroundReady, foregroundResolving, shareReady, shareResolving;

    private GenericBerryResolver(Collection<? extends ClassDef> input) {
        classes = new ArrayList<>(input);
        for (ClassDef c : classes) {
            require(byType.put(c.getType(), c) == null, "APK に型が重複しています: " + c.getType());
        }
        profile = new Profile("automatic-structure", -1, -1, -1);
        profile.automatic = this;
    }

    public static Profile resolve(String packageName, Collection<? extends ClassDef> classes) {
        require(BerryProfiles.PACKAGE.equals(packageName), "Berry Browser の元 APK を選択してください");
        GenericBerryResolver result = new GenericBerryResolver(classes);
        result.resolveCore();
        return result.profile;
    }

    void ensure(String logical) {
        if (logical.startsWith("Ltt5;") || logical.startsWith("Lc70;") || logical.startsWith("Lg60;")) {
            if (!foregroundReady && !foregroundResolving) {
                foregroundResolving = true;
                try { resolveForeground(); foregroundReady = true; }
                finally { foregroundResolving = false; }
            }
        }
        if (shareLogical(logical) && !shareReady && !shareResolving) {
            shareResolving = true;
            try { GenericShareResolver.resolve(classes, profile); shareReady = true; }
            finally { shareResolving = false; }
        }
    }

    private static boolean shareLogical(String value) {
        for (String t : new String[]{"Lb03;", "Lhc1;", "Ln8;", "Lmo0;", "Lz32;", "Lfy;", "Lal4;", "Ltp0;", "Lyx0;", "Lj8;"})
            if (value.contains(t)) return true;
        return false;
    }

    private void resolveCore() {
        List<Core> found = new ArrayList<>();
        for (ClassDef candidate : classes) {
            if (!WEB.equals(candidate.getSuperclass())) continue;
            Method touch = named(candidate, "onTouchEvent", "Z", MOTION);
            Method scroll = named(candidate, "onScrollChanged", "V", "I", "I", "I", "I");
            if (!concrete(touch) || !concrete(scroll)) continue;
            if (countRef(touch, WEB + "->onTouchEvent(" + MOTION + ")Z") != 1
                || countRef(scroll, WEB + "->onScrollChanged(IIII)V") != 1) continue;
            for (ClassDef holder : classes) {
                List<Field> fields = fields(holder, candidate.getType());
                if (fields.size() != 1) continue;
                Field wf = fields.get(0);
                if (!publicInstance(wf.getAccessFlags())) continue;
                if (!coreMethodsPresent(holder, wf)) continue;
                for (ClassDef parent : classes) {
                    Method input = named(parent, "onInterceptTouchEvent", "Z", MOTION);
                    if (!concrete(input) || !viewGroup(parent.getType(), new HashSet<>())) continue;
                    List<Field> owns = fields(parent, holder.getType());
                    if (owns.size() != 1 || !publicInstance(owns.get(0).getAccessFlags())) continue;
                    if (countRef(input, fd(owns.get(0))) < 1 || countRef(input, fd(wf)) < 1) continue;
                    if (countRef(input, "Landroid/view/GestureDetector;->onTouchEvent(" + MOTION + ")Z") != 1) continue;
                    found.add(new Core(candidate, holder, parent, wf, owns.get(0), touch, scroll, input));
                }
            }
        }
        require(found.size() == 1, "WebView とタッチ処理を一意に自動検出できませんでした（候補 " + found.size() + "）");
        Core c = found.get(0);
        web = c.web; controller = c.controller; intercept = c.intercept;
        webField = c.webField; controllerField = c.controllerField;
        profile.mapType("Lcx;", web.getType());
        profile.mapType("Lc41;", controller.getType());
        profile.mapType("Lz31;", intercept.getType());
        profile.mapMember("Lc41;->q:Lcx;", fd(webField));
        profile.mapMember("Lz31;->h0:Lc41;", fd(controllerField));
        profile.trustMethod(BerryProfiles.TOUCH, c.touch);
        profile.trustMethod(BerryProfiles.INTERCEPT, c.input);
        profile.trustMethod("Lcx;->onScrollChanged(IIII)V", c.scroll);
        for (String[] row : controllerAnchors()) {
            Method m = uniqueAnchored(controller, row[1], row[2], tail(row, 3), fd(webField));
            profile.trustMethod(row[0], m);
        }
    }

    private static final class Core {
        final ClassDef web, controller, intercept;
        final Field webField, controllerField;
        final Method touch, scroll, input;
        Core(ClassDef w, ClassDef c, ClassDef i, Field wf, Field cf, Method t, Method s, Method in) {
            web=w; controller=c; intercept=i; webField=wf; controllerField=cf; touch=t; scroll=s; input=in;
        }
    }

    private static String[][] controllerAnchors() {
        return new String[][] {
            {"Lc41;->S(Ljava/lang/String;)V", WEB+"->loadUrl(Ljava/lang/String;)V", "V", "Ljava/lang/String;"},
            {"Lc41;->V()V", WEB+"->onResume()V", "V"},
            {"Lc41;->t0()V", WEB+"->onPause()V", "V"},
            {"Lc41;->m()V", WEB+"->stopLoading()V", "V"},
            {"Lc41;->destroy()V", WEB+"->destroy()V", "V"},
            {"Lc41;->r("+BUNDLE+")V", WEB+"->saveState("+BUNDLE+")Landroid/webkit/WebBackForwardList;", "V", BUNDLE},
            {"Lc41;->h("+BUNDLE+")Z", WEB+"->restoreState("+BUNDLE+")Landroid/webkit/WebBackForwardList;", "Z", BUNDLE}
        };
    }

    private boolean coreMethodsPresent(ClassDef owner, Field field) {
        for (String[] row : controllerAnchors()) {
            if (anchored(owner, row[1], row[2], tail(row, 3), fd(field)).size() != 1) return false;
        }
        return true;
    }

    private void resolveForeground() {
        List<Client> clients = new ArrayList<>();
        for (ClassDef c : classes) {
            if (!"Landroid/webkit/WebViewClient;".equals(c.getSuperclass())) continue;
            Method start = named(c, "onPageStarted", "V", WEB, "Ljava/lang/String;", BITMAP);
            Method finish = named(c, "onPageFinished", "V", WEB, "Ljava/lang/String;");
            if (!concrete(start) || !concrete(finish)) continue;
            if (fields(c, ACTIVITY).size() != 1) continue;
            for (Field cf : c.getFields()) {
                if (!publicInstance(cf.getAccessFlags()) || countRef(start, fd(cf)) < 1 || countRef(finish, fd(cf)) < 1) continue;
                ClassDef tab = byType.get(cf.getType());
                if (tab == null || !sharesControllerInterface(tab)) continue;
                List<Field> frames = fields(tab, FRAME);
                if (frames.size() != 1 || !publicInstance(frames.get(0).getAccessFlags())) continue;
                if (countRef(start, fd(frames.get(0))) < 1 || countRef(finish, fd(frames.get(0))) < 1) continue;
                clients.add(new Client(c, tab, frames.get(0), start, finish));
            }
        }
        require(clients.size() == 1, "ページ完了通知とタブ構造を一意に自動検出できませんでした（候補 " + clients.size() + "）");
        Client client = clients.get(0);
        List<Preview> previews = new ArrayList<>();
        for (ClassDef runnable : classes) {
            if (!runnable.getInterfaces().contains("Ljava/lang/Runnable;")) continue;
            List<Field> tabs = fields(runnable, client.tab.getType());
            List<Field> bytes = fields(runnable, "B");
            if (tabs.size() != 1 || bytes.size() != 1 || fields(runnable, ACTIVITY).size() != 1) continue;
            if (!publicInstance(tabs.get(0).getAccessFlags()) || !publicInstance(bytes.get(0).getAccessFlags())) continue;
            Method run = named(runnable, "run", "V");
            if (!concrete(run) || countRef(run, fd(tabs.get(0))) != 1 || countRef(run, fd(bytes.get(0))) != 1) continue;
            if (!previewCases(run, bytes.get(0), client.tab.getType())) continue;
            previews.add(new Preview(runnable, tabs.get(0), bytes.get(0), run));
        }
        require(previews.size() == 1, "プレビュー生成処理を一意に自動検出できませんでした（候補 " + previews.size() + "）");
        Preview preview = previews.get(0);
        profile.mapType("Lc70;", client.client.getType());
        profile.mapType("Ltt5;", client.tab.getType());
        profile.mapType("Lg60;", preview.owner.getType());
        profile.mapMember("Lg60;->m:Ltt5;", fd(preview.tab));
        profile.mapMember("Lg60;->l:B", fd(preview.selector));
        profile.mapMember("Ltt5;->q:"+FRAME, fd(client.frame));
        profile.trustMethod("Lc70;->onPageStarted("+WEB+"Ljava/lang/String;"+BITMAP+")V", client.start);
        profile.trustMethod("Lc70;->onPageFinished("+WEB+"Ljava/lang/String;)V", client.finish);
        profile.trustMethod("Lg60;->run()V", preview.run);
    }

    private boolean sharesControllerInterface(ClassDef tab) {
        for (Field f : tab.getFields()) {
            for (String implemented : controller.getInterfaces()) if (f.getType().equals(implemented)) return true;
        }
        return false;
    }
    private static final class Client {
        final ClassDef client, tab; final Field frame; final Method start, finish;
        Client(ClassDef c, ClassDef t, Field f, Method s, Method e) { client=c;tab=t;frame=f;start=s;finish=e; }
    }
    private static final class Preview {
        final ClassDef owner; final Field tab, selector; final Method run;
        Preview(ClassDef o, Field t, Field s, Method r){owner=o;tab=t;selector=s;run=r;}
    }

    /** PreviewScheduler defers selectors0/1, so prove both switch cases really
     * generate/store tab bitmaps; selectors for unrelated work stay untouched. */
    private static boolean previewCases(Method method, Field selector, String tab) {
        Map<Integer, Instruction> at = new LinkedHashMap<>();
        int address=0;
        for (Instruction instruction : method.getImplementation().getInstructions()) {
            at.put(address,instruction); address += instruction.getCodeUnits();
        }
        Instruction first = at.get(0);
        if (first == null || first.getOpcode() != Opcode.IGET_BYTE || !(first instanceof ReferenceInstruction)
            || !fd(selector).equals(F.getReference(((ReferenceInstruction)first).getReference()))) return false;
        int selectorRegister=((OneRegisterInstruction)first).getRegisterA();
        int switchAddress=-1; SwitchPayload payload=null;
        for (Map.Entry<Integer,Instruction> e:at.entrySet()) {
            Instruction i=e.getValue();
            if ((i.getOpcode()==Opcode.PACKED_SWITCH || i.getOpcode()==Opcode.SPARSE_SWITCH)
                && ((OneRegisterInstruction)i).getRegisterA()==selectorRegister) {
                if (switchAddress>=0) return false;
                Instruction p=at.get(e.getKey()+((OffsetInstruction)i).getCodeOffset());
                if (!(p instanceof SwitchPayload)) return false;
                switchAddress=e.getKey();payload=(SwitchPayload)p;
            }
        }
        if (payload==null) return false;
        Map<Integer,Integer> cases=new HashMap<>();
        for (SwitchElement e:payload.getSwitchElements()) cases.put(e.getKey(),switchAddress+e.getOffset());
        for (int key:new int[]{0,1}) {
            if (!cases.containsKey(key) || !bitmapCase(at,cases.get(key),tab)) return false;
        }
        return true;
    }

    private static boolean bitmapCase(Map<Integer,Instruction> code,int start,String tab) {
        Deque<Integer> pending=new ArrayDeque<>();Set<Integer> seen=new HashSet<>();pending.add(start);
        boolean generate=false,store=false;
        while(!pending.isEmpty()) {
            int pc=pending.removeFirst();if(!seen.add(pc))continue;Instruction i=code.get(pc);if(i==null)return false;
            if(i instanceof ReferenceInstruction) {
                Reference r=((ReferenceInstruction)i).getReference();
                if(r instanceof MethodReference) {
                    MethodReference m=(MethodReference)r;
                    if(BITMAP.equals(m.getReturnType()) && params(m).equals(Collections.singletonList(tab)))generate=true;
                    if("V".equals(m.getReturnType()) && params(m).equals(Arrays.asList("I",BITMAP)))store=true;
                }
            }
            String op=i.getOpcode().name;
            if(op.startsWith("return") || op.equals("throw"))continue;
            if(i instanceof SwitchPayload || i.getOpcode()==Opcode.PACKED_SWITCH || i.getOpcode()==Opcode.SPARSE_SWITCH)return false;
            if(i instanceof OffsetInstruction) {
                pending.add(pc+((OffsetInstruction)i).getCodeOffset());
                if(op.startsWith("goto"))continue;
            }
            pending.add(pc+i.getCodeUnits());
        }
        return generate && store;
    }

    private boolean viewGroup(String type,Set<String> seen) {
        if (Arrays.asList("Landroid/view/ViewGroup;", "Landroid/widget/FrameLayout;", "Landroid/widget/LinearLayout;", "Landroid/widget/RelativeLayout;", "Landroid/widget/ScrollView;", "Landroid/widget/HorizontalScrollView;", "Landroid/widget/AbsoluteLayout;").contains(type)) return true;
        if(!seen.add(type))return false;
        ClassDef c=byType.get(type);
        return c!=null && c.getSuperclass()!=null && viewGroup(c.getSuperclass(),seen);
    }
    private static boolean concrete(Method m){return m!=null&&m.getImplementation()!=null&&(m.getAccessFlags()&8)==0;}
    private static boolean publicInstance(int flags){return(flags&9)==1;}
    private static List<String> params(MethodReference m){List<String> r=new ArrayList<>();for(CharSequence p:m.getParameterTypes())r.add(p.toString());return r;}
    private static String fd(FieldReference f){return F.getFieldDescriptor(f);}
    private static String[] tail(String[] a,int from){return Arrays.copyOfRange(a,from,a.length);}
    private static List<Field> fields(ClassDef owner,String type){List<Field> r=new ArrayList<>();for(Field f:owner.getFields())if((f.getAccessFlags()&8)==0&&type.equals(f.getType()))r.add(f);return r;}
    private static Method named(ClassDef owner,String name,String result,String... parameters) {
        Method found=null;
        for(Method m:owner.getMethods())if(name.equals(m.getName())&&result.equals(m.getReturnType())&&params(m).equals(Arrays.asList(parameters))) {
            if(found!=null)return null;found=m;
        }
        return found;
    }
    private static List<Method> anchored(ClassDef owner,String anchor,String result,String[] parameters,String field) {
        List<Method> found=new ArrayList<>();
        for(Method m:owner.getMethods())if(concrete(m)&&publicInstance(m.getAccessFlags())&&result.equals(m.getReturnType())
            &&params(m).equals(Arrays.asList(parameters))&&countRef(m,anchor)==1&&countRef(m,field)==1)found.add(m);
        return found;
    }
    private static Method uniqueAnchored(ClassDef owner,String anchor,String result,String[] parameters,String field) {
        List<Method> found=anchored(owner,anchor,result,parameters,field);
        require(found.size()==1,"Android 呼び出しを一意に自動検出できません: "+anchor);
        return found.get(0);
    }
    private static int countRef(Method method,String descriptor) {
        int count=0;if(method==null||method.getImplementation()==null)return 0;
        for(Instruction i:method.getImplementation().getInstructions())if(i instanceof ReferenceInstruction&&descriptor.equals(F.getReference(((ReferenceInstruction)i).getReference())))count++;
        return count;
    }
    private static void require(boolean value,String message){BerryProfiles.require(value,message);}
}
