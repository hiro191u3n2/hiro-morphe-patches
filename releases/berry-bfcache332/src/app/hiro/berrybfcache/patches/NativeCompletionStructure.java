package app.hiro.berrybfcache.patches;

import app.morphe.patcher.patch.BytecodePatchContext;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import kotlin.Unit;

/** Extracts only native completion bookkeeping; never replays page scripts or preview work. */
final class NativeCompletionStructure {
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private static final String WEB = "Landroid/webkit/WebView;", STR = "Ljava/lang/String;";
    final String holder, holderReady, holderCount, visible, activity, uiGetter,
            toolbarGetter, toolbarUpdate, progressGetter, progressComplete, progressField, refreshComplete;

    private NativeCompletionStructure(String[] r) {
        holder=r[0]; holderReady=r[1]; holderCount=r[2]; visible=r[3]; activity=r[4];
        uiGetter=r[5]; toolbarGetter=r[6]; toolbarUpdate=r[7]; progressGetter=r[8];
        progressComplete=r[9]; progressField=r[10]; refreshComplete=r[11];
    }

    static NativeCompletionStructure discover(BytecodePatchContext c, BfCacheRepairPatch.Profile p, Method finished) {
        Map<String,ClassDef> classes=new LinkedHashMap<>();
        c.classDefForEach(d->{classes.put(d.getType(),d);return Unit.INSTANCE;});
        return discover(classes,p,finished);
    }

    static NativeCompletionStructure discover(Map<String,ClassDef> classes, BfCacheRepairPatch.Profile p, Method finished) {
        List<Instruction> ins=instructions(finished);
        List<String[]> holders=new ArrayList<>(), uis=new ArrayList<>();
        for(int i=0;i+2<ins.size();i++) {
            Instruction a=ins.get(i),b=ins.get(i+1),d=ins.get(i+2);
            FieldReference hf=field(a),bf=field(b),df=field(d);
            if(a.getOpcode()!=Opcode.IGET_OBJECT||b.getOpcode()!=Opcode.IPUT_BOOLEAN||d.getOpcode()!=Opcode.IPUT
                    ||hf==null||bf==null||df==null||!hf.getDefiningClass().equals(p.tab)
                    ||!hf.getType().equals(bf.getDefiningClass())||!hf.getType().equals(df.getDefiningClass())
                    ||!bf.getType().equals("Z")||!df.getType().equals("I"))continue;
            int reg=((TwoRegisterInstruction)a).getRegisterA();
            if(!fieldSourceAt(finished,((TwoRegisterInstruction)a).getRegisterB(),i,p.client+"->"+p.clientTabField+":"+p.tab))continue;
            if(((TwoRegisterInstruction)b).getRegisterB()!=reg||((TwoRegisterInstruction)d).getRegisterB()!=reg
                    ||!constantAt(finished,((TwoRegisterInstruction)b).getRegisterA(),1,i+1)
                    ||!constantAt(finished,((TwoRegisterInstruction)d).getRegisterA(),0,i+2))continue;
            requirePublicField(classes,hf); requirePublicField(classes,bf); requirePublicField(classes,df);
            holders.add(new String[]{F.getFieldDescriptor(hf),F.getFieldDescriptor(bf),F.getFieldDescriptor(df)});
        }
        String[] h=unique(holders,"完了時の内部カウンターリセット");
        for(int i=0;i+11<ins.size();i++) {
            Instruction vis=ins.get(i), branch=ins.get(i+1);
            FieldReference vf=field(vis);
            if(vis.getOpcode()!=Opcode.IGET_BOOLEAN||vf==null||!vf.getDefiningClass().equals(p.tab)
                    ||!vf.getType().equals("Z")||branch.getOpcode()!=Opcode.IF_EQZ
                    ||((OneRegisterInstruction)branch).getRegisterA()!=((TwoRegisterInstruction)vis).getRegisterA()
                    ||branchTarget(ins,i+1)<i+12)continue;
            MethodReference group1=method(ins.get(i+2)),toolbar=method(ins.get(i+4)),update=method(ins.get(i+6)),
                    group2=method(ins.get(i+7)),progress=method(ins.get(i+9)),complete=method(ins.get(i+11));
            if(!virtual(ins.get(i+2))||!virtual(ins.get(i+4))||!virtual(ins.get(i+6))
                    ||!virtual(ins.get(i+7))||!virtual(ins.get(i+9))||!virtual(ins.get(i+11))
                    ||!objectGetter(group1)||!objectGetter(group2)||!same(group1,group2)
                    ||!objectGetter(toolbar)||!objectGetter(progress)||!signature(update,"V",p.tab)
                    ||!signature(complete,"V","Z")
                    ||!group1.getReturnType().equals(toolbar.getDefiningClass())
                    ||!group1.getReturnType().equals(progress.getDefiningClass())
                    ||!toolbar.getReturnType().equals(update.getDefiningClass())
                    ||!progress.getReturnType().equals(complete.getDefiningClass()))continue;
            int[] r2=args(ins.get(i+2)),r4=args(ins.get(i+4)),r6=args(ins.get(i+6)),
                    r7=args(ins.get(i+7)),r9=args(ins.get(i+9)),r11=args(ins.get(i+11));
            if(r2.length!=1||r4.length!=1||r6.length!=2||r7.length!=1||r9.length!=1||r11.length!=2
                    ||r2[0]!=r7[0]||r6[1]!=((TwoRegisterInstruction)vis).getRegisterB()
                    ||!moveResult(ins.get(i+3),r4[0])||!moveResult(ins.get(i+5),r6[0])
                    ||!moveResult(ins.get(i+8),r9[0])||!moveResult(ins.get(i+10),r11[0])
                    ||!constantAt(finished,r11[1],1,i+11))continue;
            Method cm=definition(classes,complete);
            if(!hasRef(cm,"Landroid/widget/ProgressBar;->getProgress()I")||!hasConstant(cm,100))continue;
            List<FieldReference> activityFields=new ArrayList<>();
            for(Field f:clazz(classes,p.client).getFields())if(f.getType().equals(group1.getDefiningClass())&&publicInstance(f))activityFields.add(f);
            FieldReference af=unique(activityFields,"クライアントのActivity参照");
            if(!fieldSourceAt(finished,((TwoRegisterInstruction)vis).getRegisterB(),i,p.client+"->"+p.clientTabField+":"+p.tab)
                    ||!fieldSourceAt(finished,r2[0],i+2,F.getFieldDescriptor(af)))continue;
            requirePublicField(classes,vf); requirePublicField(classes,af);
            for(MethodReference m:Arrays.asList(group1,toolbar,update,progress,complete))requirePublicMethod(classes,m);
            uis.add(new String[]{F.getFieldDescriptor(vf),F.getFieldDescriptor(af),F.getMethodDescriptor(group1),
                    F.getMethodDescriptor(toolbar),F.getMethodDescriptor(update),F.getMethodDescriptor(progress),F.getMethodDescriptor(complete)});
        }
        String[] u=unique(uis,"表示中タブのツールバー・進捗完了通知");

        List<FieldReference> progressFields=new ArrayList<>();
        for(ClassDef d:classes.values()) {
            if(!"Landroid/webkit/WebChromeClient;".equals(d.getSuperclass()))continue;
            boolean ownsTab=false;
            for(Field f:d.getFields())if(f.getType().equals(p.tab))ownsTab=true;
            if(!ownsTab)continue;
            for(Method m:d.getMethods())if(m.getName().equals("onProgressChanged")&&signature(m,"V",WEB,"I")) {
                List<Instruction> pi=instructions(m);
                for(int j=0;j+1<pi.size();j++) {
                    Instruction x=pi.get(j);FieldReference pf=field(x);
                    if(x.getOpcode()==Opcode.IPUT&&pf!=null&&pf.getDefiningClass().equals(p.tab)&&pf.getType().equals("I")
                            &&((TwoRegisterInstruction)x).getRegisterA()==m.getImplementation().getRegisterCount()-1
                            &&pi.get(j+1).getOpcode()==Opcode.RETURN_VOID&&hasConstant(m,100)
                            &&tabSourceAt(m,((TwoRegisterInstruction)x).getRegisterB(),j,p.tab))progressFields.add(pf);
                }
            }
        }
        FieldReference pf=unique(progressFields,"タブ内の読み込み進捗");requirePublicField(classes,pf);
        List<MethodReference> refresh=new ArrayList<>();
        for(Method m:clazz(classes,p.adapter).getMethods()) {
            if(!publicInstance(m)||!signature(m,"V"))continue;
            List<Instruction> ri=instructions(m);
            if(ri.size()!=5||ri.get(0).getOpcode()!=Opcode.IGET_OBJECT||ri.get(1).getOpcode()!=Opcode.IF_EQZ
                    ||ri.get(4).getOpcode()!=Opcode.RETURN_VOID||!virtual(ri.get(3)))continue;
            MethodReference r=method(ri.get(3));int[] ra=args(ri.get(3));
            FieldReference ownerField=field(ri.get(0));
            int receiver=((TwoRegisterInstruction)ri.get(0)).getRegisterA();
            if(r!=null&&r.getName().equals("setRefreshing")&&signature(r,"V","Z")&&ra.length==2
                    &&ownerField!=null&&ownerField.getDefiningClass().equals(p.adapter)
                    &&((TwoRegisterInstruction)ri.get(0)).getRegisterB()==m.getImplementation().getRegisterCount()-1
                    &&((OneRegisterInstruction)ri.get(1)).getRegisterA()==receiver&&branchTarget(ri,1)==4
                    &&ra[0]==receiver&&ra[1]!=receiver&&constantAt(m,ra[1],0,3)
                    &&assignable(classes,ownerField.getType(),r.getDefiningClass(),new HashSet<>())) {
                requirePublicMethod(classes,r);
                refresh.add(m);
            }
        }
        MethodReference rf=unique(refresh,"プル更新表示の完了処理");
        return new NativeCompletionStructure(new String[]{h[0],h[1],h[2],u[0],u[1],u[2],u[3],u[4],u[5],u[6],
                F.getFieldDescriptor(pf),F.getMethodDescriptor(rf)});
    }

    /** Four local registers plus this/WebView/URL; caller already verifies the owned WebView. */
    String code(BfCacheRepairPatch.Profile p) {
        return "iget-object v0, p0, "+p.client+"->"+p.clientTabField+":"+p.tab+"\n"
            +"iget-object v1, v0, "+p.tab+"->"+p.adapterField+":"+p.adapterInterface+"\n"
            +"instance-of v0, v1, "+p.adapter+"\nif-eqz v0, :done\ncheck-cast v1, "+p.adapter+"\n"
            +"iget-object v2, v1, "+p.adapter+"->"+p.webField+":"+p.webSubclass+"\nif-ne v2, p1, :done\n"
            +"iget-object v0, p0, "+p.client+"->"+p.clientTabField+":"+p.tab+"\n"
            +"const/4 v2, 0x1\ninvoke-virtual {v0, v2}, "+p.tab+"->"+p.completeSetter+"(I)V\n"
            +"iget-object v3, v0, "+holder+"\niput-boolean v2, v3, "+holderReady+"\n"
            +"const/4 v2, 0x0\niput v2, v3, "+holderCount+"\n"
            +"const/16 v2, 0x64\niput v2, v0, "+progressField+"\n"
            +"invoke-virtual {v1}, "+refreshComplete+"\n"
            +"invoke-static {p1, p2}, Lhiro/berry/TabPriority;->finished("+WEB+STR+")V\n"
            +"iget-boolean v2, v0, "+visible+"\nif-eqz v2, :done\n"
            +"iget-object v1, p0, "+activity+"\ninvoke-virtual {v1}, "+uiGetter+"\nmove-result-object v2\n"
            +"invoke-virtual {v2}, "+toolbarGetter+"\nmove-result-object v2\ninvoke-virtual {v2, v0}, "+toolbarUpdate+"\n"
            +"invoke-virtual {v1}, "+uiGetter+"\nmove-result-object v2\ninvoke-virtual {v2}, "+progressGetter+"\nmove-result-object v2\n"
            +"const/4 v3, 0x1\ninvoke-virtual {v2, v3}, "+progressComplete+"\n:done\nreturn-void";
    }

    private static boolean publicInstance(Field f){return(f.getAccessFlags()&9)==1;}
    private static boolean publicInstance(Method m){return(m.getAccessFlags()&9)==1;}
    private static boolean objectGetter(MethodReference r){return r!=null&&r.getReturnType().startsWith("L")&&r.getParameterTypes().isEmpty();}
    private static boolean signature(MethodReference r,String ret,String...p){if(r==null||!r.getReturnType().equals(ret)||r.getParameterTypes().size()!=p.length)return false;for(int i=0;i<p.length;i++)if(!p[i].contentEquals(r.getParameterTypes().get(i)))return false;return true;}
    private static boolean same(MethodReference a,MethodReference b){return a!=null&&b!=null&&F.getMethodDescriptor(a).equals(F.getMethodDescriptor(b));}
    private static boolean virtual(Instruction i){return i.getOpcode()==Opcode.INVOKE_VIRTUAL||i.getOpcode()==Opcode.INVOKE_VIRTUAL_RANGE;}
    private static int[] args(Instruction i){if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction a=(FiveRegisterInstruction)i;return Arrays.copyOf(new int[]{a.getRegisterC(),a.getRegisterD(),a.getRegisterE(),a.getRegisterF(),a.getRegisterG()},a.getRegisterCount());}if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction a=(RegisterRangeInstruction)i;int[]r=new int[a.getRegisterCount()];for(int j=0;j<r.length;j++)r[j]=a.getStartRegister()+j;return r;}return new int[0];}
    private static boolean moveResult(Instruction i,int reg){return i.getOpcode()==Opcode.MOVE_RESULT_OBJECT&&((OneRegisterInstruction)i).getRegisterA()==reg;}
    private static FieldReference field(Instruction i){if(!(i instanceof ReferenceInstruction))return null;Reference r=((ReferenceInstruction)i).getReference();return r instanceof FieldReference?(FieldReference)r:null;}
    private static MethodReference method(Instruction i){if(!(i instanceof ReferenceInstruction))return null;Reference r=((ReferenceInstruction)i).getReference();return r instanceof MethodReference?(MethodReference)r:null;}
    private static List<Instruction> instructions(Method m){List<Instruction>r=new ArrayList<>();if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())r.add(i);return r;}
    private static ClassDef clazz(Map<String,ClassDef> c,String t){ClassDef d=c.get(t);need(d!=null,"必要な型 "+t);return d;}
    private static Method definition(Map<String,ClassDef>c,MethodReference r){List<Method>a=new ArrayList<>();for(Method m:clazz(c,r.getDefiningClass()).getMethods())if(same(m,r))a.add(m);return unique(a,"必要なメソッド "+F.getMethodDescriptor(r));}
    private static void requirePublicMethod(Map<String,ClassDef>c,MethodReference r){need(publicInstance(definition(c,r)),"非公開メソッド "+F.getMethodDescriptor(r));}
    private static void requirePublicField(Map<String,ClassDef>c,FieldReference r){List<Field>a=new ArrayList<>();for(Field f:clazz(c,r.getDefiningClass()).getFields())if(F.getFieldDescriptor(f).equals(F.getFieldDescriptor(r)))a.add(f);need(publicInstance(unique(a,"必要なフィールド "+F.getFieldDescriptor(r))),"非公開フィールド");}
    private static boolean hasRef(Method m,String target){for(Instruction i:instructions(m))if(i instanceof ReferenceInstruction&&F.getReference(((ReferenceInstruction)i).getReference()).equals(target))return true;return false;}
    private static boolean hasConstant(Method m,int value){for(Instruction i:instructions(m))if(i instanceof NarrowLiteralInstruction&&i.getOpcode().name().startsWith("CONST_")&&((NarrowLiteralInstruction)i).getNarrowLiteral()==value)return true;return false;}
    private static int branchTarget(List<Instruction>ins,int index){if(!(ins.get(index)instanceof OffsetInstruction))return -1;int before=0;for(int i=0;i<index;i++)before+=ins.get(i).getCodeUnits();int target=before+((OffsetInstruction)ins.get(index)).getCodeOffset();int offset=0;for(int i=0;i<ins.size();i++){if(offset==target)return i;offset+=ins.get(i).getCodeUnits();}return -1;}
    private static <T>T unique(List<T>r,String purpose){need(r.size()==1,purpose+"：候補数 "+r.size());return r.get(0);}
    private static void need(boolean ok,String why){if(!ok)throw new IllegalStateException("BFCacheネイティブ完了検出："+why);}

    /** Constant must hold on every reachable control-flow path into the selected instruction. */
    private static boolean constantAt(Method m,int register,int value,int target){
        return provenAt(m,register,target,x->x instanceof NarrowLiteralInstruction&&x.getOpcode().name().startsWith("CONST_")&&((NarrowLiteralInstruction)x).getNarrowLiteral()==value);
    }
    private static boolean tabSourceAt(Method m,int register,int target,String tab){
        int self=selfRegister(m);
        return provenAt(m,register,target,x->x.getOpcode()==Opcode.IGET_OBJECT&&field(x)!=null
                &&field(x).getDefiningClass().equals(m.getDefiningClass())&&field(x).getType().equals(tab)
                &&((TwoRegisterInstruction)x).getRegisterB()==self);
    }
    private static boolean fieldSourceAt(Method m,int register,int target,String descriptor){
        int self=selfRegister(m);
        return provenAt(m,register,target,x->x.getOpcode()==Opcode.IGET_OBJECT&&field(x)!=null
                &&F.getFieldDescriptor(field(x)).equals(descriptor)&&((TwoRegisterInstruction)x).getRegisterB()==self);
    }
    private static int selfRegister(Method m){int words=1;for(CharSequence type:m.getParameterTypes())words+=(type.toString().equals("J")||type.toString().equals("D"))?2:1;return m.getImplementation().getRegisterCount()-words;}
    private static boolean assignable(Map<String,ClassDef> classes,String type,String target,Set<String>seen){
        if(type.equals(target))return true;if(!seen.add(type))return false;ClassDef c=classes.get(type);if(c==null)return false;
        if(c.getSuperclass()!=null&&assignable(classes,c.getSuperclass(),target,seen))return true;
        for(String i:c.getInterfaces())if(assignable(classes,i,target,seen))return true;return false;
    }
    private static boolean provenAt(Method m,int register,int target,java.util.function.Predicate<Instruction> writesExpected){
        if(!m.getImplementation().getTryBlocks().isEmpty())return false;
        List<Instruction>ins=instructions(m);Map<Integer,Integer>byOffset=new HashMap<>();int[]offs=new int[ins.size()];int off=0;
        for(int i=0;i<ins.size();i++){offs[i]=off;byOffset.put(off,i);off+=ins.get(i).getCodeUnits();}
        boolean[]seen=new boolean[ins.size()*2];ArrayDeque<Integer>q=new ArrayDeque<>();q.add(0);boolean reached=false;
        while(!q.isEmpty()){
            int state=q.remove(),i=state/2;boolean known=(state&1)!=0;if(i<0||i>=ins.size()||seen[state])continue;seen[state]=true;
            if(i==target){if(!known)return false;reached=true;}
            Instruction x=ins.get(i);if(writes(x,register))known=writesExpected.test(x);
            int bit=known?1:0;if(x.getOpcode().canContinue()&&i+1<ins.size())q.add((i+1)*2+bit);
            if(x instanceof OffsetInstruction){Integer t=byOffset.get(offs[i]+((OffsetInstruction)x).getCodeOffset());if(t==null)return false;
                if(x.getOpcode()==Opcode.PACKED_SWITCH||x.getOpcode()==Opcode.SPARSE_SWITCH){if(!(ins.get(t)instanceof SwitchPayload))return false;for(SwitchElement e:((SwitchPayload)ins.get(t)).getSwitchElements()){Integer dest=byOffset.get(offs[i]+e.getOffset());if(dest==null)return false;q.add(dest*2+bit);}}
                else if(x.getOpcode()!=Opcode.FILL_ARRAY_DATA)q.add(t*2+bit);
            }
        }
        return reached;
    }
    private static boolean writes(Instruction i,int r){return i instanceof OneRegisterInstruction&&(i.getOpcode().setsRegister()&&((OneRegisterInstruction)i).getRegisterA()==r||i.getOpcode().setsWideRegister()&&((OneRegisterInstruction)i).getRegisterA()+1==r);}
}
