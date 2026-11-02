package app.hiro.berry.patches;

import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;

/** Resolve the chooser as a linked view/binding/adapter/Intent-query graph. */
public final class GenericShareResolver {
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private static final String RV = "Landroidx/recyclerview/widget/RecyclerView;";
    private static final String OBJECT = "Ljava/lang/Object;";
    private static final String LIST = "Ljava/util/List;";
    private GenericShareResolver() {}

    public static void resolve(Collection<? extends ClassDef> classes, BerryProfiles.Profile p) {
        Map<String,ClassDef> byType = new LinkedHashMap<>();
        List<Method> callbacks = new ArrayList<>();
        for (ClassDef c : classes) {
            byType.put(c.getType(), c);
            for (Method m : c.getMethods()) if (body(m) && params(m, OBJECT) && OBJECT.equals(m.getReturnType())
                && hasString(m,"android.intent.action.VIEW") && count(m,"Ljava/util/List;->isEmpty()Z") == 1) callbacks.add(m);
        }
        List<Candidate> candidates = new ArrayList<>();
        for (ClassDef c : classes) for (Method view : c.getMethods()) {
            if (!body(view) || !"onViewCreated".equals(view.getName()) || !"V".equals(view.getReturnType())
                    || !params(view,"Landroid/view/View;","Landroid/os/Bundle;")) continue;
            List<MethodReference> setters = new ArrayList<>();
            List<MethodReference> getters = new ArrayList<>();
            for (Instruction i : view.getImplementation().getInstructions()) {
                Reference r = ref(i);
                if (!(r instanceof MethodReference)) continue;
                MethodReference m = (MethodReference)r;
                if (RV.equals(m.getDefiningClass()) && "setAdapter".equals(m.getName()) && "V".equals(m.getReturnType())
                        && m.getParameterTypes().size() == 1) uniqueAdd(setters,m);
                if (c.getType().equals(m.getDefiningClass()) && m.getParameterTypes().isEmpty()
                        && binding(byType.get(m.getReturnType()))) uniqueAdd(getters,m);
            }
            if (setters.size() != 1 || getters.size() != 1 || count(view,F.getMethodDescriptor(setters.get(0))) != 1) continue;
            MethodReference setter = setters.get(0), getter = getters.get(0);
            String adapterBase = setter.getParameterTypes().get(0).toString();
            List<Field> fields = new ArrayList<>();
            for (Field f : c.getFields()) if ((f.getAccessFlags() & 9) == 1 && assignable(byType,f.getType(),adapterBase,new HashSet<>())
                    && count(view,F.getFieldDescriptor(f)) > 0) fields.add(f);
            if (fields.size() != 1) continue;
            Field adapterField = fields.get(0);
            for (Method callback : callbacks) {
                Candidate candidate = candidate(byType,view,getter,setter,adapterField,callback);
                if (candidate != null) candidates.add(candidate);
            }
        }
        need(candidates.size() == 1, "共有先の画面・一覧・アダプター構造を一意に特定できません ("+candidates.size()+")");
        Candidate c = candidates.get(0);
        ClassDef binding = byType.get(c.getter.getReturnType());
        Field linear = oneField(binding,"Landroid/widget/LinearLayout;"), text = oneField(binding,"Landroid/widget/TextView;");
        Method getter = lookup(byType,c.getter), updater = lookup(byType,c.update);
        need(getter != null && updater != null, "共有先の実行部品の対象処理がありません");
        List<ClassDef> models = new ArrayList<>();
        for (ClassDef def : classes) if ((def.getAccessFlags() & 0x200) != 0
                && noArg(def,"Landroid/content/ComponentName;") != null && noArg(def,"Ljava/lang/CharSequence;") != null
                && noArg(def,"Landroid/graphics/drawable/Drawable;") != null) models.add(def);
        need(models.size()==1,"共有先のアプリ情報インターフェースを一意に特定できません");
        ClassDef model = models.get(0);
        List<ClassDef> activities = new ArrayList<>();
        for (ClassDef def : classes) if ((def.getAccessFlags() & 0x200)!=0 && noArg(def,"Landroid/content/pm/ActivityInfo;")!=null
                && assignable(byType,def.getType(),model.getType(),new HashSet<>())) activities.add(def);
        need(activities.size()==1,"共有先のActivity情報インターフェースを一意に特定できません");
        p.mapType("Lb03;", c.view.getDefiningClass());
        p.mapType("Lhc1;", binding.getType());
        p.mapType("Lz32;", c.adapterField.getType());
        p.mapType("Lfy;", c.update.getDefiningClass());
        p.mapType("Lal4;", c.setter.getParameterTypes().get(0).toString());
        p.mapType("Lmo0;", model.getType());
        p.mapType("Ln8;", activities.get(0).getType());
        p.mapType("Lj8;", c.callback.getDefiningClass());
        p.mapType("Ltp0;", c.query.getDefiningClass());
        p.mapType("Lyx0;", c.query.getParameterTypes().get(1).toString());
        p.mapMember("Lb03;->M0:Lz32;", F.getFieldDescriptor(c.adapterField));
        p.mapMember("Lhc1;->a:Landroid/widget/LinearLayout;",F.getFieldDescriptor(linear));
        p.mapMember("Lhc1;->b:Landroid/widget/TextView;",F.getFieldDescriptor(text));
        p.mapMember("Lmo0;->a()Landroid/content/ComponentName;",F.getMethodDescriptor(noArg(model,"Landroid/content/ComponentName;")));
        p.mapMember("Lmo0;->b()Ljava/lang/CharSequence;",F.getMethodDescriptor(noArg(model,"Ljava/lang/CharSequence;")));
        p.mapMember("Ltp0;->c(Landroid/content/Intent;Lyx0;)Ljava/lang/Object;",F.getMethodDescriptor(c.query));
        p.mapMember("Landroidx/recyclerview/widget/RecyclerView;->setAdapter(Lal4;)V",F.getMethodDescriptor(c.setter));
        bind(p,BerryProfiles.SHARE_VIEW,c.view);
        bind(p,BerryProfiles.SHARE_LIST,c.callback);
        bind(p,"Lb03;->X()Lhc1;",getter);
        bind(p,"Lfy;->O(Ljava/util/List;)V",updater);
        p.setShareRegisters(c.callback.getImplementation().getRegisterCount(),c.dialogRegister,c.returnRegister);
    }

    private static Candidate candidate(Map<String,ClassDef> classes, Method view, MethodReference getter,
            MethodReference setter, Field adapterField, Method callback) {
        List<Instruction> ins = new ArrayList<>();
        for (Instruction instruction : callback.getImplementation().getInstructions()) ins.add(instruction);
        int cast = -1, empty = -1, field = -1, update = -1, query = -1;
        MethodReference updater = null, queryRef = null;
        for (int k=0;k<ins.size();k++) {
            Instruction i=ins.get(k); Reference r=ref(i);
            if (i.getOpcode()==Opcode.CHECK_CAST && r instanceof TypeReference
                    && ((TypeReference)r).getType().equals(view.getDefiningClass())) {if(cast>=0)return null;cast=k;}
            if (r!=null && F.getReference(r).equals("Ljava/util/List;->isEmpty()Z")) {if(empty>=0)return null;empty=k;}
            if (r instanceof FieldReference && F.getFieldDescriptor((FieldReference)r).equals(F.getFieldDescriptor(adapterField))) {
                if(field>=0)return null; field=k;
            }
        }
        if (cast<0 || empty<=cast || field<=empty) return null;
        int dialog=regA(ins.get(cast));
        if (ins.get(field).getOpcode()!=Opcode.IGET_OBJECT || !(ins.get(field) instanceof TwoRegisterInstruction)
                || ((TwoRegisterInstruction)ins.get(field)).getRegisterB()!=dialog) return null;
        int adapter=regA(ins.get(field));
        int[] emptyArgs=args(ins.get(empty));
        if(emptyArgs==null || emptyArgs.length!=1)return null;
        for (int k=cast+1;k<empty;k++) {
            Reference r=ref(ins.get(k));
            if(r instanceof MethodReference) {
                MethodReference m=(MethodReference)r;
                if(m.getParameterTypes().size()==2 && m.getParameterTypes().get(0).toString().equals("Landroid/content/Intent;")
                        && OBJECT.equals(m.getReturnType()) && virtual(ins.get(k))) {
                    if(query>=0)return null;query=k;queryRef=m;
                }
            }
        }
        if(query<0)return null;
        for(int k=field+1;k<ins.size()-1;k++) {
            Reference r=ref(ins.get(k));
            if(r instanceof MethodReference) {
                MethodReference m=(MethodReference)r;int[] a=args(ins.get(k));
                if(params(m,LIST) && "V".equals(m.getReturnType()) && virtual(ins.get(k))
                        && assignable(classes,adapterField.getType(),m.getDefiningClass(),new HashSet<>())
                        && a!=null && a.length==2 && a[0]==adapter && a[1]==emptyArgs[0]
                        && ins.get(k+1).getOpcode()==Opcode.RETURN_OBJECT) {
                    if(update>=0)return null;update=k;updater=m;
                }
            }
            if(ins.get(k).getOpcode()==Opcode.RETURN_OBJECT)break;
        }
        if(update<0)return null;
        int returned=regA(ins.get(update+1));
        if(returned==dialog || returned==emptyArgs[0])return null;
        if(!noWrite(ins,cast+1,empty,dialog) || !noWrite(ins,empty,update+1,returned)
                || !noWrite(ins,field+1,update,adapter))return null;
        Candidate c=new Candidate();c.view=view;c.getter=getter;c.setter=setter;c.adapterField=adapterField;
        c.callback=callback;c.query=queryRef;c.update=updater;c.dialogRegister=dialog;c.returnRegister=returned;
        return c;
    }
    private static final class Candidate {
        Method view,callback; MethodReference getter,setter,query,update; Field adapterField;
        int dialogRegister,returnRegister;
    }
    private static void bind(BerryProfiles.Profile p,String logical,Method actual) {
        p.mapMember(logical,F.getMethodDescriptor(actual));p.trustMethod(logical,actual);
    }
    private static boolean binding(ClassDef c) {
        return c!=null && fieldCount(c,"Landroid/widget/LinearLayout;")==1 && fieldCount(c,"Landroid/widget/TextView;")==1 && fieldCount(c,RV)==1;
    }
    private static int fieldCount(ClassDef c,String type) {int n=0;for(Field f:c.getFields())if(type.equals(f.getType())&&(f.getAccessFlags()&9)==1)n++;return n;}
    private static Field oneField(ClassDef c,String type) {Field found=null;for(Field f:c.getFields())if(type.equals(f.getType())&&(f.getAccessFlags()&9)==1){need(found==null,"共有先の表示フィールドが重複しています");found=f;}need(found!=null,"共有先の表示フィールドがありません");return found;}
    private static Method noArg(ClassDef c,String returned) {Method found=null;for(Method m:c.getMethods())if(m.getParameterTypes().isEmpty()&&returned.equals(m.getReturnType())&&(m.getAccessFlags()&9)==1){if(found!=null)return null;found=m;}return found;}
    private static Method lookup(Map<String,ClassDef> classes,MethodReference r) {ClassDef c=classes.get(r.getDefiningClass());if(c==null)return null;for(Method m:c.getMethods())if(F.getMethodDescriptor(m).equals(F.getMethodDescriptor(r)))return m;return null;}
    private static void uniqueAdd(List<MethodReference> list,MethodReference m) {for(MethodReference x:list)if(F.getMethodDescriptor(x).equals(F.getMethodDescriptor(m)))return;list.add(m);}
    private static boolean body(Method m) {return m.getImplementation()!=null && (m.getAccessFlags()&8)==0;}
    private static boolean params(MethodReference m,String...p) {if(m.getParameterTypes().size()!=p.length)return false;for(int n=0;n<p.length;n++)if(!p[n].contentEquals(m.getParameterTypes().get(n)))return false;return true;}
    private static Reference ref(Instruction i) {return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference():null;}
    private static int count(Method m,String descriptor) {int n=0;for(Instruction i:m.getImplementation().getInstructions()){Reference r=ref(i);if(r!=null&&descriptor.equals(F.getReference(r)))n++;}return n;}
    private static boolean hasString(Method m,String value) {for(Instruction i:m.getImplementation().getInstructions()){Reference r=ref(i);if(r instanceof StringReference&&value.equals(((StringReference)r).getString()))return true;}return false;}
    private static boolean assignable(Map<String,ClassDef> classes,String from,String to,Set<String>seen) {if(from.equals(to))return true;if(!seen.add(from))return false;ClassDef c=classes.get(from);if(c==null)return false;for(String i:c.getInterfaces())if(assignable(classes,i,to,seen))return true;return c.getSuperclass()!=null&&assignable(classes,c.getSuperclass(),to,seen);}
    private static boolean virtual(Instruction i) {return i.getOpcode()==Opcode.INVOKE_VIRTUAL||i.getOpcode()==Opcode.INVOKE_VIRTUAL_RANGE;}
    private static int regA(Instruction i) {return i instanceof OneRegisterInstruction?((OneRegisterInstruction)i).getRegisterA():-1;}
    private static int[] args(Instruction i) {if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction f=(FiveRegisterInstruction)i;return Arrays.copyOf(new int[]{f.getRegisterC(),f.getRegisterD(),f.getRegisterE(),f.getRegisterF(),f.getRegisterG()},f.getRegisterCount());}if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction f=(RegisterRangeInstruction)i;int[]a=new int[f.getRegisterCount()];for(int n=0;n<a.length;n++)a[n]=f.getStartRegister()+n;return a;}return null;}
    private static boolean noWrite(List<? extends Instruction> ins,int from,int to,int reg) {for(int k=from;k<to;k++){Instruction i=ins.get(k);if(i.getOpcode().setsRegister()&&i instanceof OneRegisterInstruction){int a=regA(i);if(a==reg||i.getOpcode().setsWideRegister()&&a+1==reg)return false;}}return true;}
    private static void need(boolean b,String message) {BerryProfiles.require(b,message);}
}
