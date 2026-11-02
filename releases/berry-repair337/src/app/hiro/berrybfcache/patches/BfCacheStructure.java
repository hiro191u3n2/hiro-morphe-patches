package app.hiro.berrybfcache.patches;

import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.function.Predicate;

/** Resolve the BFCache integration from typed platform call sites, not app versions/names. */
final class BfCacheStructure {
    private static final String WEB = "Landroid/webkit/WebView;", STR = "Ljava/lang/String;";
    private static final String SET = "Ljava/util/Set;";
    private static final String BOUNDARY = "Lorg/chromium/support_lib_boundary/WebViewProviderBoundaryInterface;";
    private static final String ADD_LISTENER = BOUNDARY + "->addWebMessageListener(" + STR + "[Ljava/lang/String;Ljava/lang/reflect/InvocationHandler;)V";
    private static final String ADD_SCRIPT = BOUNDARY + "->addDocumentStartJavaScript(" + STR + "[Ljava/lang/String;)Ljava/lang/reflect/InvocationHandler;";
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private BfCacheStructure() {}

    static BfCacheRepairPatch.Profile discover(Map<String, ClassDef> classes,
            String client, String adapter, String tab, String web) {
        ClassDef cc = clazz(classes, client), ac = clazz(classes, adapter), tc = clazz(classes, tab);
        need("Landroid/webkit/WebViewClient;".equals(cc.getSuperclass()), "WebViewClientの継承構造");
        need(WEB.equals(clazz(classes, web).getSuperclass()), "WebViewの継承構造");
        Field clientTab = unique(fields(cc, f -> tab.equals(f.getType()) && instancePublic(f)), "クライアントからタブへの参照");
        Field adapterWeb = unique(fields(ac, f -> web.equals(f.getType()) && instancePublic(f)), "アダプターからWebViewへの参照");
        Field tabAdapter = unique(fields(tc, f -> instancePublic(f) && classes.containsKey(f.getType())
                && (classes.get(f.getType()).getAccessFlags() & 0x200) != 0
                && assignable(classes, adapter, f.getType(), new HashSet<>())), "タブからWebViewアダプターへの参照");
        String iface = tabAdapter.getType();

        Method listener = unique(methods(ac, m -> instancePublic(m) && m.getReturnType().equals("Z")
                && m.getParameterTypes().size() == 3 && m.getParameterTypes().get(0).toString().equals(STR)
                && m.getParameterTypes().get(1).toString().equals(SET)
                && hasString(m, "WEB_MESSAGE_LISTENER") && countRef(m, ADD_LISTENER) == 1), "WebMessageListener設定処理");
        Method document = unique(methods(ac, m -> instancePublic(m) && m.getReturnType().startsWith("L")
                && parameters(m, STR, STR, SET) && hasString(m, "DOCUMENT_START_SCRIPT")
                && countRef(m, ADD_SCRIPT) == 1), "document-start設定処理");
        MethodReference feature = unique(methodRefs(listener, r -> signature(r, "Z", STR)
                && invokedStatic(listener, F.getMethodDescriptor(r)) && containsMethodRef(document, r)
                && invokedStatic(document, F.getMethodDescriptor(r))), "共有するWebView機能判定");
        Method featureDef = method(classes, F.getMethodDescriptor(feature));
        need((featureDef.getAccessFlags() & 9) == 9, "機能判定処理の公開static属性");
        MethodReference factory = unique(methodRefs(listener, r -> parameters(r, WEB)
                && r.getReturnType().startsWith("L") && invokedStatic(listener, F.getMethodDescriptor(r))
                && containsMethodRef(document, r) && invokedStatic(document, F.getMethodDescriptor(r))
                && hasBoundaryField(classes, r.getReturnType())), "共有するWebView provider取得処理");
        Method factoryDef = method(classes, F.getMethodDescriptor(factory));
        need((factoryDef.getAccessFlags() & 9) == 9, "provider取得処理の公開static属性");
        String provider = factory.getReturnType();
        Field providerBoundary = unique(fields(clazz(classes, provider), f -> BOUNDARY.equals(f.getType())
                && instancePublic(f) && countRef(listener, F.getFieldDescriptor(f)) == 1
                && countRef(document, F.getFieldDescriptor(f)) == 1), "providerのboundary参照");
        need(countRef(listener, F.getFieldDescriptor(adapterWeb)) == 1 && countRef(document, F.getFieldDescriptor(adapterWeb)) == 1,
                "両設定処理が同じWebViewを使用すること");

        Method started = method(classes, client + "->onPageStarted(" + WEB + STR + "Landroid/graphics/Bitmap;)V");
        Method finished = method(classes, client + "->onPageFinished(" + WEB + STR + ")V");
        need(countRef(started, F.getFieldDescriptor(clientTab)) >= 1 && countRef(finished, F.getFieldDescriptor(clientTab)) >= 1,
                "ページ開始・完了処理のタブ参照");
        List<MethodReference> setterCandidates = methodRefs(started, r -> r.getDefiningClass().equals(tab)
                && signature(r, "V", "I") && containsMethodRef(finished, r)
                && calledWithConstant(started, r, 3) && calledWithConstant(finished, r, 1));
        MethodReference setter = unique(setterCandidates, "開始3・完了1の読み込み状態更新処理");
        Method setterDef = method(classes, F.getMethodDescriptor(setter));
        need(instancePublic(setterDef), "状態更新処理の公開属性");
        List<Instruction> si = instructions(setterDef);
        need(!si.isEmpty() && si.get(0).getOpcode() == Opcode.IPUT
                && ((TwoRegisterInstruction)si.get(0)).getRegisterA() == setterDef.getImplementation().getRegisterCount()-1
                && ((TwoRegisterInstruction)si.get(0)).getRegisterB() == setterDef.getImplementation().getRegisterCount()-2
                && ((FieldReference)((ReferenceInstruction)si.get(0)).getReference()).getDefiningClass().equals(tab),
                "状態更新処理が引数をタブへ保存すること");

        List<Method> setupCandidates = new ArrayList<>();
        for (ClassDef c : classes.values()) for (Method m : c.getMethods()) {
            if (!instancePublic(m) || !signature(m, "V", tab) || m.getImplementation()==null) continue;
            if (countType(m, client, Opcode.NEW_INSTANCE) != 1 || countRef(m, F.getFieldDescriptor(tabAdapter)) < 1) continue;
            List<MethodReference> constructors = methodRefs(m, r -> r.getDefiningClass().equals(client)
                    && r.getName().equals("<init>") && r.getParameterTypes().contains(tab));
            List<MethodReference> setters = methodRefs(m, r -> r.getDefiningClass().equals(tab)
                    && signature(r, "V", "Landroid/webkit/WebViewClient;"));
            if (constructors.size()!=1 || setters.size()!=1) continue;
            Method tabSetter = method(classes, F.getMethodDescriptor(setters.get(0)));
            if (countRef(tabSetter, F.getFieldDescriptor(tabAdapter)) != 1) continue;
            List<MethodReference> forwarding = methodRefs(tabSetter, r -> r.getDefiningClass().equals(iface)
                    && signature(r, "V", "Landroid/webkit/WebViewClient;"));
            if (forwarding.size()!=1) continue;
            Method adapterSetter = method(classes, adapter + "->" + forwarding.get(0).getName() + "(Landroid/webkit/WebViewClient;)V");
            if (countRef(adapterSetter, WEB+"->setWebViewClient(Landroid/webkit/WebViewClient;)V") != 1
                    || countRef(adapterSetter, F.getFieldDescriptor(adapterWeb)) != 1) continue;
            need(m.getImplementation().getRegisterCount()-2 >= 2, "設定処理の作業レジスタ数");
            setupCandidates.add(m);
        }
        Method setup = unique(setupCandidates, "WebViewClientを接続するタブ設定処理");
        BfCacheRepairPatch.Profile result = new BfCacheRepairPatch.Profile(client, adapter, tab,
                setup.getDefiningClass(), setup.getName(), tabAdapter.getName(), iface,
                adapterWeb.getName(), web, setter.getName(), feature.getDefiningClass(), factory.getDefiningClass(),
                provider, listener.getName(), document.getName(), document.getReturnType(),
                ExactBody.sha256(setup), ExactBody.sha256(started), ExactBody.sha256(setterDef),
                ExactBody.sha256(listener), ExactBody.sha256(document));
        result.clientTabField=clientTab.getName(); result.providerBoundaryField=providerBoundary.getName();
        result.featureMethod=feature.getName(); result.factoryMethod=factory.getName();
        result.listenerCallbackType=listener.getParameterTypes().get(2).toString();
        return result;
    }

    private static boolean hasBoundaryField(Map<String,ClassDef> classes,String type) {
        ClassDef c=classes.get(type); if(c==null)return false;
        return fields(c,f->BOUNDARY.equals(f.getType())&&instancePublic(f)).size()==1;
    }
    private static ClassDef clazz(Map<String,ClassDef> c,String type){ClassDef r=c.get(type);need(r!=null,"必要な型 "+type);return r;}
    private static Method method(Map<String,ClassDef> c,String descriptor){String owner=descriptor.substring(0,descriptor.indexOf("->"));return unique(methods(clazz(c,owner),m->F.getMethodDescriptor(m).equals(descriptor)),"必要な処理 "+descriptor);}
    private static List<Method> methods(ClassDef c,Predicate<Method> p){List<Method> r=new ArrayList<>();for(Method m:c.getMethods())if(p.test(m))r.add(m);return r;}
    private static List<Field> fields(ClassDef c,Predicate<Field> p){List<Field> r=new ArrayList<>();for(Field f:c.getFields())if(p.test(f))r.add(f);return r;}
    private static boolean instancePublic(Method m){return(m.getAccessFlags()&9)==1;}
    private static boolean instancePublic(Field f){return(f.getAccessFlags()&9)==1;}
    private static boolean parameters(MethodReference m,String...types){if(m.getParameterTypes().size()!=types.length)return false;for(int i=0;i<types.length;i++)if(!types[i].contentEquals(m.getParameterTypes().get(i)))return false;return true;}
    private static boolean signature(MethodReference m,String ret,String...types){return ret.equals(m.getReturnType())&&parameters(m,types);}
    private static List<Instruction> instructions(Method m){List<Instruction>r=new ArrayList<>();if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())r.add(i);return r;}
    private static String reference(Instruction i){return i instanceof ReferenceInstruction?F.getReference(((ReferenceInstruction)i).getReference()):"";}
    private static int countRef(Method m,String r){int n=0;for(Instruction i:instructions(m))if(reference(i).equals(r))n++;return n;}
    private static int countType(Method m,String type,Opcode opcode){int n=0;for(Instruction i:instructions(m))if(i.getOpcode()==opcode&&reference(i).equals(type))n++;return n;}
    private static boolean hasString(Method m,String text){for(Instruction i:instructions(m))if(i instanceof ReferenceInstruction&&((ReferenceInstruction)i).getReference() instanceof StringReference&&text.equals(((StringReference)((ReferenceInstruction)i).getReference()).getString()))return true;return false;}
    private static List<MethodReference> methodRefs(Method m,Predicate<MethodReference> p){Map<String,MethodReference>r=new LinkedHashMap<>();for(Instruction i:instructions(m))if(i instanceof ReferenceInstruction&&((ReferenceInstruction)i).getReference() instanceof MethodReference){MethodReference mr=(MethodReference)((ReferenceInstruction)i).getReference();if(p.test(mr))r.put(F.getMethodDescriptor(mr),mr);}return new ArrayList<>(r.values());}
    private static boolean containsMethodRef(Method m,MethodReference ref){return countRef(m,F.getMethodDescriptor(ref))>=1;}
    private static boolean invokedStatic(Method m,String ref){for(Instruction i:instructions(m))if((i.getOpcode()==Opcode.INVOKE_STATIC||i.getOpcode()==Opcode.INVOKE_STATIC_RANGE)&&reference(i).equals(ref))return true;return false;}
    private static boolean assignable(Map<String,ClassDef> c,String from,String to,Set<String> seen){if(from.equals(to))return true;if(!seen.add(from))return false;ClassDef d=c.get(from);if(d==null)return false;for(String i:d.getInterfaces())if(assignable(c,i,to,seen))return true;return d.getSuperclass()!=null&&assignable(c,d.getSuperclass(),to,seen);}
    private static <T>T unique(List<T> list,String purpose){need(list.size()==1,purpose+"：候補数 "+list.size());return list.get(0);}
    private static void need(boolean ok,String reason){if(!ok)throw new IllegalStateException("BFCache自動検出："+reason);}

    /** Require the state literal on every path reaching the call, including back edges. */
    static int loadingCallIndex(Method method, String descriptor) {
        List<Instruction> ins=instructions(method); int found=-1;
        for(int k=0;k<ins.size();k++) {
            Instruction call=ins.get(k); if(!reference(call).equals(descriptor))continue;
            int register;
            if(call instanceof FiveRegisterInstruction && ((FiveRegisterInstruction)call).getRegisterCount()==2)
                register=((FiveRegisterInstruction)call).getRegisterD();
            else if(call instanceof RegisterRangeInstruction && ((RegisterRangeInstruction)call).getRegisterCount()==2)
                register=((RegisterRangeInstruction)call).getStartRegister()+1;
            else continue;
            if(constantAtCall(method,ins,register,3,k)) { if(found>=0)return -1; found=k; }
        }
        return found;
    }
    private static boolean calledWithConstant(Method method,MethodReference target,int value){
        List<Instruction> ins=instructions(method);String descriptor=F.getMethodDescriptor(target);
        for(int k=0;k<ins.size();k++) {
            Instruction call=ins.get(k);if(!reference(call).equals(descriptor))continue;
            int register;
            if(call instanceof FiveRegisterInstruction&&((FiveRegisterInstruction)call).getRegisterCount()==2)register=((FiveRegisterInstruction)call).getRegisterD();
            else if(call instanceof RegisterRangeInstruction&&((RegisterRangeInstruction)call).getRegisterCount()==2)register=((RegisterRangeInstruction)call).getStartRegister()+1;
            else continue;
            if(constantAtCall(method,ins,register,value,k))return true;
        }
        return false;
    }
    private static boolean writes(Instruction i,int register){return i.getOpcode().setsRegister()&&i instanceof OneRegisterInstruction&&((OneRegisterInstruction)i).getRegisterA()==register
            ||i.getOpcode().setsWideRegister()&&i instanceof OneRegisterInstruction&&((OneRegisterInstruction)i).getRegisterA()+1==register;}
    private static boolean constantAtCall(Method method,List<Instruction> ins,int register,int value,int call){
        if(!method.getImplementation().getTryBlocks().isEmpty())return false;
        Map<Integer,Integer> offsetIndex=new HashMap<>();int[] offsets=new int[ins.size()];int off=0;
        for(int i=0;i<ins.size();i++){offsets[i]=off;offsetIndex.put(off,i);off+=ins.get(i).getCodeUnits();}
        boolean[] seen=new boolean[ins.size()*2];ArrayDeque<Integer>q=new ArrayDeque<>();q.add(0);boolean reached=false;
        while(!q.isEmpty()){int state=q.remove(),i=state/2;boolean known=(state&1)!=0;
            if(i<0||i>=ins.size()||seen[state])continue;seen[state]=true;
            if(i==call){if(!known)return false;reached=true;}
            Instruction x=ins.get(i);
            if(writes(x,register))known=x instanceof NarrowLiteralInstruction
                    && x.getOpcode().name().startsWith("CONST_")
                    && ((NarrowLiteralInstruction)x).getNarrowLiteral()==value;
            int bit=known?1:0;
            if(x.getOpcode().canContinue()&&i+1<ins.size())q.add((i+1)*2+bit);
            if(x instanceof OffsetInstruction){Integer t=offsetIndex.get(offsets[i]+((OffsetInstruction)x).getCodeOffset());if(t==null)return false;
                if(x.getOpcode()==Opcode.PACKED_SWITCH||x.getOpcode()==Opcode.SPARSE_SWITCH){if(!(ins.get(t)instanceof SwitchPayload))return false;for(SwitchElement e:((SwitchPayload)ins.get(t)).getSwitchElements()){Integer dest=offsetIndex.get(offsets[i]+e.getOffset());if(dest==null)return false;q.add(dest*2+bit);}}
                else if(x.getOpcode()!=Opcode.FILL_ARRAY_DATA)q.add(t*2+bit);
            }
        }
        return reached;
    }
}
