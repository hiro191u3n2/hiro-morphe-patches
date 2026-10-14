import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import kotlin.Pair;
import app.hiro.oneback.patches.OneBackPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.iface.debug.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.debug.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Independent DEX scope, control-flow, ABI, manifest, and bundle-preservation audit. */
public final class DexAudit {
    private static final String RUNTIME="Lapp/hiro/oneback/runtime/OneBackExit;";
    private static final String PREFIX="Lapp/hiro/oneback/", HELPER="hiroOneBack$";
    private static final String ACTIVITY="Landroid/app/Activity;", DIALOG="Landroid/app/Dialog;";
    private static final String VIEW="Landroid/view/View;", KEY_TYPE="Landroid/view/KeyEvent;";
    private static final String CREATE="onCreate(Landroid/os/Bundle;)V", RESUME="onResume()V", DESTROY="onDestroy()V";
    private static final String BACK="onBackPressed()V", KEY="dispatchKeyEvent("+KEY_TYPE+")Z";
    private static final String PREIME="onKeyPreIme(I"+KEY_TYPE+")Z", DISPATCH_PREIME="dispatchKeyEventPreIme("+KEY_TYPE+")Z";
    private static final String NS="http://schemas.android.com/apk/res/android";
    private static final Set<String> PACKAGES=set("com.ss.android.ugc.trill","com.zhiliaoapp.musically","com.instagram.android","com.twitter.android","ctrip.english");
    private static final Set<String> ACTIVITY_BASES=set(ACTIVITY,"Landroid/app/ListActivity;","Landroid/app/NativeActivity;","Landroid/app/ActivityGroup;","Landroid/app/TabActivity;","Landroid/preference/PreferenceActivity;","Landroid/accounts/AccountAuthenticatorActivity;","Landroid/app/AliasActivity;");
    private static final Set<String> DIALOG_BASES=set(DIALOG,"Landroid/app/AlertDialog;","Landroid/app/ProgressDialog;","Landroid/app/DatePickerDialog;","Landroid/app/TimePickerDialog;","Landroid/app/Presentation;");
    private static final Set<String> TEXT_BASES=set("Landroid/widget/TextView;","Landroid/widget/EditText;","Landroid/widget/AutoCompleteTextView;","Landroid/widget/MultiAutoCompleteTextView;","Landroid/widget/Button;","Landroid/widget/CheckedTextView;","Landroid/widget/CompoundButton;","Landroid/widget/ToggleButton;","Landroid/widget/CheckBox;","Landroid/widget/RadioButton;","Landroid/widget/Switch;");
    private static int assertions;
    private static final List<String> tests=new ArrayList<String>();
    private static final Map<String,Long> normalizations=new TreeMap<String,Long>();
    private static Map<String,ClassDef> hierarchy;
    private static Set<String> set(String... values){return new LinkedHashSet<String>(Arrays.asList(values));}
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);assertions++;}
    private interface Action{void run()throws Exception;}
    private static void rejected(Action action,String reason)throws Exception{
        boolean fail=false;try{action.run();}catch(IllegalStateException|IllegalArgumentException expected){fail=true;}
        check(fail,reason);
    }
    private static Map<String,ClassDef> classes(String file)throws Exception{
        Map<String,ClassDef> result=new TreeMap<String,ClassDef>();
        MultiDexContainer<? extends DexFile> container=DexFileFactory.loadDexContainer(new File(file),Opcodes.getDefault());
        for(String entry:container.getDexEntryNames())for(ClassDef c:container.getEntry(entry).getDexFile().getClasses())
            check(result.put(c.getType(),c)==null,"duplicate class "+c.getType());
        return result;
    }
    private static byte[] canonical(ClassDef c)throws Exception{
        check(c!=null,"missing class");MemoryDataStore out=new MemoryDataStore();
        try{DexPool.writeTo(out,new ImmutableDexFile(Opcodes.getDefault(),Collections.singleton(c)));
            return Arrays.copyOf(out.getBuffer(),out.getSize());}finally{out.close();}
    }
    private static byte[] canonical(Method m)throws Exception{
        return canonical(new ImmutableClassDef(m.getDefiningClass(),1,"Ljava/lang/Object;",Collections.<String>emptyList(),null,
                Collections.<Annotation>emptySet(),Collections.<Field>emptyList(),Collections.singleton(m)));
    }
    private static ClassDef withoutMethods(ClassDef c){
        return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),Collections.<Method>emptyList());
    }
    private static Method copy(Method metadata,String name,int flags,MethodImplementation body){
        return new ImmutableMethod(metadata.getDefiningClass(),name,metadata.getParameters(),metadata.getReturnType(),flags,metadata.getAnnotations(),metadata.getHiddenApiRestrictions(),body);
    }
    private static String key(MethodReference m){StringBuilder b=new StringBuilder(m.getName()).append('(');
        for(CharSequence p:m.getParameterTypes())b.append(p);return b.append(')').append(m.getReturnType()).toString();}
    private static Map<String,Method> methods(ClassDef c){Map<String,Method> result=new TreeMap<String,Method>();
        for(Method m:c.getMethods())check(result.put(key(m),m)==null,"duplicate method "+c.getType()+key(m));return result;}
    private static List<Instruction> instructions(Method m){List<Instruction> result=new ArrayList<Instruction>();
        if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())result.add(i);return result;}
    private static MethodReference reference(Instruction i){if(!(i instanceof ReferenceInstruction))return null;
        Reference r=((ReferenceInstruction)i).getReference();return r instanceof MethodReference?(MethodReference)r:null;}
    private static MethodReference ref(String owner,String name,String result,String...params){return new ImmutableMethodReference(owner,name,Arrays.asList(params),result);}
    private static int[] registers(Instruction i){
        if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;int[] all={r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()};return Arrays.copyOf(all,r.getRegisterCount());}
        if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction r=(RegisterRangeInstruction)i;int[] result=new int[r.getRegisterCount()];for(int n=0;n<result.length;n++)result[n]=r.getStartRegister()+n;return result;}
        throw new AssertionError("unexpected call encoding "+i.getOpcode());
    }
    private static int self(Method m){int words=1;for(CharSequence p:m.getParameterTypes())words+=("J".contentEquals(p)||"D".contentEquals(p))?2:1;
        return m.getImplementation().getRegisterCount()-words;}
    private static void requireCall(Instruction i,Opcode normal,MethodReference wanted,int...args){
        Opcode range=normal==Opcode.INVOKE_STATIC?Opcode.INVOKE_STATIC_RANGE:normal==Opcode.INVOKE_DIRECT?Opcode.INVOKE_DIRECT_RANGE:normal==Opcode.INVOKE_SUPER?Opcode.INVOKE_SUPER_RANGE:Opcode.INVOKE_VIRTUAL_RANGE;
        check(i.getOpcode()==normal||i.getOpcode()==range,"wrong invoke kind for "+wanted+": "+i.getOpcode());
        check(wanted.equals(reference(i)),"wrong call target; expected "+wanted+", got "+reference(i));
        check(Arrays.equals(args,registers(i)),"wrong register arguments for "+wanted);
    }
    private static int address(List<? extends Instruction> list,int index){int result=0;for(int n=0;n<index;n++)result+=list.get(n).getCodeUnits();return result;}
    private static int branch(List<? extends Instruction> list,int index){int target=address(list,index)+((OffsetInstruction)list.get(index)).getCodeOffset();
        for(int n=0;n<list.size();n++)if(address(list,n)==target)return n;return -1;}
    private static boolean subtype(String type,Set<String> roots){Set<String> seen=new HashSet<String>();
        while(type!=null&&seen.add(type)){if(roots.contains(type))return true;ClassDef c=hierarchy.get(type);if(c==null)return false;type=c.getSuperclass();}return false;}
    private static boolean view(String type){Set<String> seen=new HashSet<String>();
        while(type!=null&&seen.add(type)){if(VIEW.equals(type)||TEXT_BASES.contains(type)||"Landroid/view/ViewGroup;".equals(type)||"Landroid/webkit/WebView;".equals(type)||(type.startsWith("Landroid/widget/")&&type.endsWith("Layout;")))return true;
            ClassDef c=hierarchy.get(type);if(c==null)return false;type=c.getSuperclass();}return false;}
    private static boolean instance(Method m){return (m.getAccessFlags()&(0x8|0x2))==0;}
    private static MethodReference runtime(String name){
        switch(name){
            case "install":case "onResume":case "onDestroy":case "back":return ref(RUNTIME,name,"V",ACTIVITY);
            case "key":return ref(RUNTIME,name,"Z",ACTIVITY,KEY_TYPE);
            case "dialogShown":case "dialogBack":return ref(RUNTIME,name,"V",DIALOG);
            case "dialogKey":return ref(RUNTIME,name,"Z",DIALOG,KEY_TYPE);
            case "keyFromView":return ref(RUNTIME,name,"Z",VIEW,KEY_TYPE);
            default:throw new AssertionError("unreviewed public runtime ABI "+name);
        }
    }
    private static boolean virtual(Instruction i){Opcode op=i.getOpcode();return op==Opcode.INVOKE_VIRTUAL||op==Opcode.INVOKE_VIRTUAL_RANGE||op==Opcode.INVOKE_SUPER||op==Opcode.INVOKE_SUPER_RANGE||op==Opcode.INVOKE_INTERFACE||op==Opcode.INVOKE_INTERFACE_RANGE;}
    private static boolean dialogVoid(Instruction i){MethodReference r=reference(i);return virtual(i)&&r!=null&&"show()V".equals(key(r))&&subtype(r.getDefiningClass(),DIALOG_BASES);}
    private static boolean dialogValue(Instruction i){MethodReference r=reference(i);return virtual(i)&&r!=null&&"show".equals(r.getName())&&r.getParameterTypes().isEmpty()&&subtype(r.getReturnType(),DIALOG_BASES);}
    private static String lifecycle(String k){return CREATE.equals(k)?"install":RESUME.equals(k)?"onResume":DESTROY.equals(k)?"onDestroy":null;}
    private static String guardHook(String type,String k){
        if(KEY.equals(k)&&subtype(type,ACTIVITY_BASES))return "key";
        if(KEY.equals(k)&&subtype(type,DIALOG_BASES))return "dialogKey";
        if((PREIME.equals(k)||DISPATCH_PREIME.equals(k))&&view(type))return "keyFromView";return null;
    }

    private static void normalization(String name){normalizations.put(name,normalizations.containsKey(name)?normalizations.get(name)+1:1);}
    private static boolean gotoWidth(Opcode opcode){return opcode==Opcode.GOTO||opcode==Opcode.GOTO_16||opcode==Opcode.GOTO_32;}
    private static final class Layout {
        final List<Instruction> code;final int[] addresses;final Map<Integer,Integer> boundary=new HashMap<Integer,Integer>();final Map<Integer,Integer> paddingLineAliases=new HashMap<Integer,Integer>();final int alignmentNops;
        Layout(Method method){this(method,false);}
        Layout(Method method,boolean alignment){List<Instruction> raw=instructions(method);int[] rawAddresses=new int[raw.size()+1];Map<Integer,Integer> rawIndex=new HashMap<Integer,Integer>();int address=0;
            for(int n=0;n<raw.size();n++){rawAddresses[n]=address;rawIndex.put(address,n);address+=raw.get(n).getCodeUnits();}rawAddresses[raw.size()]=address;
            Set<Integer> targets=new HashSet<Integer>();for(int n=0;n<raw.size();n++){Instruction i=raw.get(n);if(i instanceof OffsetInstruction){int destination=rawAddresses[n]+((OffsetInstruction)i).getCodeOffset();targets.add(destination);
                if(i.getOpcode()==Opcode.PACKED_SWITCH||i.getOpcode()==Opcode.SPARSE_SWITCH){Integer p=rawIndex.get(destination);if(p!=null&&raw.get(p) instanceof SwitchPayload)for(SwitchElement e:((SwitchPayload)raw.get(p)).getSwitchElements())targets.add(rawAddresses[n]+e.getOffset());}}}
            Set<Integer> forbiddenPadding=new HashSet<Integer>(targets);for(DebugItem d:method.getImplementation().getDebugItems())if(!(d instanceof LineNumber))forbiddenPadding.add(d.getCodeAddress());
            for(TryBlock<? extends ExceptionHandler> t:method.getImplementation().getTryBlocks())for(ExceptionHandler h:t.getExceptionHandlers())forbiddenPadding.add(h.getHandlerCodeAddress());
            code=new ArrayList<Instruction>();List<Integer> keptAddresses=new ArrayList<Integer>();int removed=0;
            for(int n=0;n<raw.size();n++){boolean terminal=n>0&&(raw.get(n-1).getOpcode()==Opcode.RETURN_VOID||raw.get(n-1).getOpcode()==Opcode.RETURN||raw.get(n-1).getOpcode()==Opcode.RETURN_WIDE||raw.get(n-1).getOpcode()==Opcode.RETURN_OBJECT||raw.get(n-1).getOpcode()==Opcode.THROW||raw.get(n-1).getOpcode()==Opcode.GOTO||raw.get(n-1).getOpcode()==Opcode.GOTO_16||raw.get(n-1).getOpcode()==Opcode.GOTO_32);
                boolean padding=terminal&&raw.get(n).getOpcode()==Opcode.NOP&&n+1<raw.size()&&raw.get(n+1) instanceof PayloadInstruction&&(rawAddresses[n]&1)==1&&(rawAddresses[n+1]&1)==0&&!forbiddenPadding.contains(rawAddresses[n]);
                if(padding)for(TryBlock<? extends ExceptionHandler> t:method.getImplementation().getTryBlocks())if(rawAddresses[n]>=t.getStartCodeAddress()&&rawAddresses[n]<t.getStartCodeAddress()+t.getCodeUnitCount()){padding=false;break;}
                if(padding)paddingLineAliases.put(rawAddresses[n],code.size()+(alignment?0:1));boundary.put(rawAddresses[n],code.size());if(padding&&alignment){removed++;continue;}code.add(raw.get(n));keptAddresses.add(rawAddresses[n]);}
            alignmentNops=removed;addresses=new int[code.size()+1];for(int n=0;n<code.size();n++)addresses[n]=keptAddresses.get(n);addresses[code.size()]=address;boundary.put(address,code.size());}
        int exact(int address,String label){Integer found=boundary.get(address);check(found!=null,label+" is not an instruction boundary: "+address);return found;}
        int floor(int address,String label){check(address>=0&&address<=addresses[addresses.length-1],label+" debug location outside method: "+address);int found=Arrays.binarySearch(addresses,address);return found>=0?found:-found-2;}
        boolean paddingLine(DebugItem item){return item instanceof LineNumber&&paddingLineAliases.containsKey(item.getCodeAddress());}
        int debugAnchor(DebugItem item,String label){return paddingLine(item)?paddingLineAliases.get(item.getCodeAddress()):floor(item.getCodeAddress(),label);}
        int debugOffset(DebugItem item,int anchor){return paddingLine(item)?0:item.getCodeAddress()-addresses[anchor];}
        List<Integer> switchOrigins(int payload){List<Integer> result=new ArrayList<Integer>();for(int n=0;n<code.size();n++){
            Opcode op=code.get(n).getOpcode();if(op==Opcode.PACKED_SWITCH||op==Opcode.SPARSE_SWITCH)if(exact(addresses[n]+((OffsetInstruction)code.get(n)).getCodeOffset(),"switch payload")==payload)result.add(n);}return result;}
    }
    private static final class TryShape {
        final int start,end,rawStart,rawEnd;final List<Object> handlers=new ArrayList<Object>();final List<Integer> rawHandlers=new ArrayList<Integer>();
        TryShape(int a,int b,int ra,int rb){start=a;end=b;rawStart=ra;rawEnd=rb;}
    }
    private static List<TryShape> tries(Method method,Layout layout){List<TryShape> result=new ArrayList<TryShape>();
        for(TryBlock<? extends ExceptionHandler> t:method.getImplementation().getTryBlocks()){int a=t.getStartCodeAddress(),b=a+t.getCodeUnitCount(),start=layout.exact(a,"try start"),end=layout.exact(b,"try end");
            TryShape shape=result.isEmpty()?null:result.get(result.size()-1);if(shape==null||shape.rawStart!=a||shape.rawEnd!=b){shape=new TryShape(start,end,a,b);result.add(shape);}
            for(ExceptionHandler h:t.getExceptionHandlers()){shape.handlers.add(Arrays.asList(h.getExceptionType(),layout.exact(h.getHandlerCodeAddress(),"catch handler")));shape.rawHandlers.add(h.getHandlerCodeAddress());}}
        return result;}
    /** All encoded operands are represented, using instruction identities only for relocated PCs. */
    private static List<Object> operands(Layout layout,int index){Instruction i=layout.code.get(index);List<Object> out=new ArrayList<Object>();
        if(i instanceof OneRegisterInstruction)out.add(Arrays.asList("A",((OneRegisterInstruction)i).getRegisterA()));
        if(i instanceof TwoRegisterInstruction)out.add(Arrays.asList("B",((TwoRegisterInstruction)i).getRegisterB()));
        if(i instanceof ThreeRegisterInstruction)out.add(Arrays.asList("C",((ThreeRegisterInstruction)i).getRegisterC()));
        if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;out.add(Arrays.asList("invoke",r.getRegisterCount(),r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()));}
        if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction r=(RegisterRangeInstruction)i;out.add(Arrays.asList("range",r.getStartRegister(),r.getRegisterCount()));}
        if(i instanceof WideLiteralInstruction)out.add(Arrays.asList("literal",((WideLiteralInstruction)i).getWideLiteral()));
        if(i instanceof ReferenceInstruction){ReferenceInstruction r=(ReferenceInstruction)i;out.add(Arrays.asList("reference",r.getReferenceType(),ImmutableReferenceFactory.of(r.getReferenceType(),r.getReference())));}
        if(i instanceof DualReferenceInstruction){DualReferenceInstruction r=(DualReferenceInstruction)i;out.add(Arrays.asList("reference2",r.getReferenceType2(),ImmutableReferenceFactory.of(r.getReferenceType2(),r.getReference2())));}
        if(i instanceof OffsetInstruction)out.add(Arrays.asList("target",layout.exact(layout.addresses[index]+((OffsetInstruction)i).getCodeOffset(),"branch/payload target")));
        if(i instanceof FieldOffsetInstruction)out.add(Arrays.asList("fieldOffset",((FieldOffsetInstruction)i).getFieldOffset()));
        if(i instanceof InlineIndexInstruction)out.add(Arrays.asList("inlineIndex",((InlineIndexInstruction)i).getInlineIndex()));
        if(i instanceof VtableIndexInstruction)out.add(Arrays.asList("vtableIndex",((VtableIndexInstruction)i).getVtableIndex()));
        if(i instanceof VerificationErrorInstruction)out.add(Arrays.asList("verificationError",((VerificationErrorInstruction)i).getVerificationError()));
        if(i instanceof ArrayPayload){ArrayPayload p=(ArrayPayload)i;List<Long> values=new ArrayList<Long>();for(Number n:p.getArrayElements())values.add(n.longValue());out.add(Arrays.asList("array",p.getElementWidth(),values));}
        if(i instanceof SwitchPayload){List<Integer> origins=layout.switchOrigins(index);List<Object> cases=new ArrayList<Object>();for(SwitchElement e:((SwitchPayload)i).getSwitchElements()){
            List<Object> targets=new ArrayList<Object>();if(origins.isEmpty())targets.add(Arrays.asList("unreferencedOffset",e.getOffset()));
            for(int origin:origins)targets.add(Arrays.asList(origin,layout.exact(layout.addresses[origin]+e.getOffset(),"switch case target")));cases.add(Arrays.asList(e.getKey(),targets));}out.add(Arrays.asList("switch",cases));}
        // Fail rather than overlooking operands if a future dexlib format is introduced.
        String format=i.getOpcode().format.name();check(Arrays.asList("Format10t","Format10x","Format11n","Format11x","Format12x","Format20bc","Format20t","Format21c","Format21ih","Format21lh","Format21s","Format21t","Format22b","Format22c","Format22cs","Format22s","Format22t","Format22x","Format23x","Format30t","Format31c","Format31i","Format31t","Format32x","Format35c","Format35mi","Format35ms","Format3rc","Format3rmi","Format3rms","Format45cc","Format4rcc","Format51l","ArrayPayload","PackedSwitchPayload","SparseSwitchPayload").contains(format),"unsupported operand format "+format);
        return out;
    }
    private static List<Object> debugValue(DebugItem item){List<Object> value=new ArrayList<Object>();value.add(item.getDebugItemType());
        if(item instanceof LineNumber)value.add(((LineNumber)item).getLineNumber());
        else if(item instanceof StartLocal){StartLocal local=(StartLocal)item;value.addAll(Arrays.asList(local.getRegister(),local.getName(),local.getType(),local.getSignature()));}
        // End/restart encode only the register; names/types are supplied by the ordered start event.
        else if(item instanceof EndLocal)value.add(((EndLocal)item).getRegister());
        else if(item instanceof RestartLocal)value.add(((RestartLocal)item).getRegister());
        else if(item instanceof SetSourceFile)value.add(((SetSourceFile)item).getSourceFile());
        else check(item instanceof PrologueEnd||item instanceof EpilogueBegin,"unknown debug event kind "+item.getClass().getName());return value;
    }
    /**
     * The two independently observed encoding changes are explicit: string versus jumbo width,
     * and dexlib moving a debug event inside an instruction to that instruction's start.
     * Only unreachable odd-address padding directly between a terminal and an aligned payload
     * can be omitted; it cannot be a target or protected instruction. Line events on such
     * padding may move forward to the same payload, retaining their values and order.
     * GOTO/GOTO_16/GOTO_32 may select a different width, with the exact target retained.
     * No other opcode change, instruction addition/removal, changed operand,
     * changed target, changed catch interval/handler, or dropped/reordered debug event is accepted.
     */
    private static void semanticMethod(Method expected,Method actual,boolean emitted,String stage)throws Exception{
        String label=expected.toString();check(Arrays.equals(canonical(copy(expected,expected.getName(),expected.getAccessFlags(),null)),canonical(copy(actual,actual.getName(),actual.getAccessFlags(),null))),"semantic method metadata changed "+label);
        check((expected.getImplementation()==null)==(actual.getImplementation()==null),"implementation presence changed "+label);if(expected.getImplementation()==null)return;
        check(expected.getImplementation().getRegisterCount()==actual.getImplementation().getRegisterCount(),"register count changed "+label);
        Layout before=new Layout(expected,emitted),after=new Layout(actual,emitted);check(before.code.size()==after.code.size(),"instruction count changed "+label+": "+before.code.size()+" -> "+after.code.size());
        for(int n=after.alignmentNops;n<before.alignmentNops;n++)normalization(stage+"_unreachable_alignment_nops_removed");for(int n=before.alignmentNops;n<after.alignmentNops;n++)normalization(stage+"_unreachable_alignment_nops_added");
        for(int n=0;n<before.code.size();n++){Instruction a=before.code.get(n),b=after.code.get(n);Opcode ao=a.getOpcode(),bo=b.getOpcode();
            if(ao!=bo){if(gotoWidth(ao)&&gotoWidth(bo))normalization(stage+(b.getCodeUnits()>a.getCodeUnits()?"_goto_width_widened":"_goto_width_narrowed"));
                else{check(emitted&&((ao==Opcode.CONST_STRING&&bo==Opcode.CONST_STRING_JUMBO)||(ao==Opcode.CONST_STRING_JUMBO&&bo==Opcode.CONST_STRING)),"unapproved opcode change "+label+" instruction["+n+"]: "+ao+" -> "+bo);normalization(stage+(bo==Opcode.CONST_STRING_JUMBO?"_const_string_widened":"_const_string_narrowed"));}}
            List<Object> av=operands(before,n),bv=operands(after,n);check(av.equals(bv),"instruction operand/target changed "+label+" instruction["+n+"]: "+av+" -> "+bv);
            if(a instanceof OffsetInstruction&&((OffsetInstruction)a).getCodeOffset()!=((OffsetInstruction)b).getCodeOffset())normalization(stage+"_branch_offsets_relocated");
        }
        List<TryShape> beforeTries=tries(expected,before),afterTries=tries(actual,after);check(beforeTries.size()==afterTries.size(),"try-block count changed after exact-same-range grouping "+label);
        if(expected.getImplementation().getTryBlocks().size()!=actual.getImplementation().getTryBlocks().size())normalization(stage+"_same_range_try_handler_groups_reformatted");
        for(int n=0;n<beforeTries.size();n++){TryShape a=beforeTries.get(n),b=afterTries.get(n);check(a.start==b.start&&a.end==b.end,"try protected instruction range changed "+label);check(a.handlers.equals(b.handlers),"exception type/order/handler changed "+label);
            if(a.rawStart!=b.rawStart||a.rawEnd!=b.rawEnd)normalization(stage+"_try_boundaries_relocated");for(int k=0;k<a.rawHandlers.size();k++)if(!a.rawHandlers.get(k).equals(b.rawHandlers.get(k)))normalization(stage+"_handler_addresses_relocated");
        }
        List<DebugItem> bd=new ArrayList<DebugItem>(),ad=new ArrayList<DebugItem>();for(DebugItem d:expected.getImplementation().getDebugItems())bd.add(d);for(DebugItem d:actual.getImplementation().getDebugItems())ad.add(d);
        check(bd.size()==ad.size(),"debug event count changed "+label);
        for(int n=0;n<bd.size();n++){DebugItem a=bd.get(n),b=ad.get(n);check(debugValue(a).equals(debugValue(b)),"debug event value/order changed "+label+" event["+n+"]");
            boolean ap=before.paddingLine(a),bp=after.paddingLine(b);check(!bp||ap,"debug line moved backward from payload to padding "+label+" event["+n+"]");
            int ai=before.debugAnchor(a,label),bi=after.debugAnchor(b,label);check(ai==bi,"debug event changed owning instruction "+label+" event["+n+"]");int ax=before.debugOffset(a,ai),bx=after.debugOffset(b,bi);
            check(ax==bx||(ax>0&&bx==0),"debug event moved other than observed instruction-start floor "+label+" event["+n+"]");
            if(ap&&!bp)normalization(stage+"_debug_lines_from_padding_to_payload");
            if(ax!=bx)normalization(stage+"_debug_events_floored");if(a.getCodeAddress()!=b.getCodeAddress())normalization(stage+"_debug_addresses_relocated");
        }
        normalization(stage+"_methods_encoding_equivalent");
    }
    private static void semanticClass(ClassDef expected,ClassDef actual,String stage)throws Exception{
        check(Arrays.equals(canonical(withoutMethods(expected)),canonical(withoutMethods(actual))),"emitted class metadata/fields changed "+expected.getType());Map<String,Method> a=methods(expected),b=methods(actual);check(a.keySet().equals(b.keySet()),"emitted class method inventory changed "+expected.getType());
        for(String key:a.keySet())if(!Arrays.equals(canonical(a.get(key)),canonical(b.get(key))))semanticMethod(a.get(key),b.get(key),true,stage);normalization(stage+"_classes_encoding_equivalent");
    }
    private static Method normalizationFixture(boolean jumbo){
        MutableMethodImplementation code=body(1,
            jumbo?new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,0,new ImmutableStringReference("type")):new BuilderInstruction21c(Opcode.CONST_STRING,0,new ImmutableStringReference("type")),
            new BuilderInstruction11n(Opcode.CONST_4,0,1),new BuilderInstruction10x(Opcode.NOP),new BuilderInstruction11x(Opcode.MOVE_EXCEPTION,0),new BuilderInstruction11n(Opcode.CONST_4,0,0),new BuilderInstruction11x(Opcode.RETURN,0));
        code.replaceInstruction(2,new BuilderInstruction10t(Opcode.GOTO,code.newLabelForIndex(5)));code.addCatch("Ljava/lang/Throwable;",code.newLabelForIndex(0),code.newLabelForIndex(1),code.newLabelForIndex(3));
        int afterString=jumbo?3:2;List<DebugItem> debug=Arrays.<DebugItem>asList(new ImmutableLineNumber(0,10),new ImmutableStartLocal(0,0,"text","Ljava/lang/String;",null),new ImmutableLineNumber(jumbo?0:1,11),new ImmutableLineNumber(afterString,12),new ImmutableEndLocal(afterString,0));
        return method("Lfixture/Reencoded;","sample",Collections.<String>emptyList(),"Z",1|8,new ImmutableMethodImplementation(1,code.getInstructions(),code.getTryBlocks(),debug));
    }
    private static Method paddingFixture(boolean jumbo){MutableMethodImplementation code=body(1,new BuilderInstruction11n(Opcode.CONST_4,0,0),new BuilderInstruction10x(Opcode.NOP),
            jumbo?new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,0,new ImmutableStringReference("view")):new BuilderInstruction21c(Opcode.CONST_STRING,0,new ImmutableStringReference("view")),new BuilderInstruction11x(Opcode.RETURN_OBJECT,0));
        if(!jumbo)code.addInstruction(new BuilderInstruction10x(Opcode.NOP));int payload=jumbo?4:5;code.addInstruction(new BuilderPackedSwitchPayload(0,Collections.singletonList(code.newLabelForIndex(2))));
        code.replaceInstruction(1,new BuilderInstruction31t(Opcode.PACKED_SWITCH,0,code.newLabelForIndex(payload)));return method("Lfixture/Reencoded;","payload",Collections.<String>emptyList(),"Ljava/lang/Object;",1|8,code);}
    private static void auditRejected(Action action,String label)throws Exception{boolean fail=false;Map<String,Long> snapshot=new TreeMap<String,Long>(normalizations);
        try{action.run();}catch(AssertionError expected){fail=true;}finally{normalizations.clear();normalizations.putAll(snapshot);}check(fail,"semantic audit accepted "+label);}
    private static void normalizationTests()throws Exception{
        final Method original=normalizationFixture(false),encoded=normalizationFixture(true);semanticMethod(original,encoded,true,"selftest_normalization");
        auditRejected(()->semanticMethod(original,encoded,false,"selftest_rejected"),"unapproved pre-emission string-width change");
        final MutableMethod wrongString=new MutableMethod(encoded);wrongString.getImplementation().replaceInstruction(0,new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,0,new ImmutableStringReference("wrong")));
        auditRejected(()->semanticMethod(original,wrongString,true,"selftest_rejected"),"changed resolved string");
        final MutableMethod wrongLiteral=new MutableMethod(encoded);wrongLiteral.getImplementation().replaceInstruction(1,new BuilderInstruction11n(Opcode.CONST_4,0,2));auditRejected(()->semanticMethod(original,wrongLiteral,true,"selftest_rejected"),"changed literal");
        final MutableMethod wrongBranch=new MutableMethod(encoded);wrongBranch.getImplementation().replaceInstruction(2,new BuilderInstruction10t(Opcode.GOTO,wrongBranch.getImplementation().newLabelForIndex(4)));auditRejected(()->semanticMethod(original,wrongBranch,true,"selftest_rejected"),"changed branch target");
        final MutableMethod addedInstruction=new MutableMethod(encoded);addedInstruction.getImplementation().addInstruction(1,new BuilderInstruction10x(Opcode.NOP));auditRejected(()->semanticMethod(original,addedInstruction,true,"selftest_rejected"),"inserted instruction");
        List<DebugItem> changedDebug=new ArrayList<DebugItem>();for(DebugItem d:encoded.getImplementation().getDebugItems())changedDebug.add(d);changedDebug.set(3,new ImmutableLineNumber(3,99));
        final Method wrongLine=copy(encoded,encoded.getName(),encoded.getAccessFlags(),new ImmutableMethodImplementation(1,encoded.getImplementation().getInstructions(),encoded.getImplementation().getTryBlocks(),changedDebug));auditRejected(()->semanticMethod(original,wrongLine,true,"selftest_rejected"),"changed debug line value");
        List<DebugItem> changedLocal=new ArrayList<DebugItem>();for(DebugItem d:encoded.getImplementation().getDebugItems())changedLocal.add(d);changedLocal.set(1,new ImmutableStartLocal(0,0,"different","Ljava/lang/String;",null));
        final Method wrongLocal=copy(encoded,encoded.getName(),encoded.getAccessFlags(),new ImmutableMethodImplementation(1,encoded.getImplementation().getInstructions(),encoded.getImplementation().getTryBlocks(),changedLocal));auditRejected(()->semanticMethod(original,wrongLocal,true,"selftest_rejected"),"changed local-variable metadata");
        List<DebugItem> movedDebug=new ArrayList<DebugItem>();for(DebugItem d:encoded.getImplementation().getDebugItems())movedDebug.add(d);movedDebug.set(0,new ImmutableLineNumber(1,10));
        final Method wrongPosition=copy(encoded,encoded.getName(),encoded.getAccessFlags(),new ImmutableMethodImplementation(1,encoded.getImplementation().getInstructions(),encoded.getImplementation().getTryBlocks(),movedDebug));auditRejected(()->semanticMethod(original,wrongPosition,true,"selftest_rejected"),"debug instruction-start event moved into its instruction");
        List<DebugItem> reordered=new ArrayList<DebugItem>();for(DebugItem d:encoded.getImplementation().getDebugItems())reordered.add(d);Collections.swap(reordered,0,1);
        final Method wrongOrder=copy(encoded,encoded.getName(),encoded.getAccessFlags(),new ImmutableMethodImplementation(1,encoded.getImplementation().getInstructions(),encoded.getImplementation().getTryBlocks(),reordered));auditRejected(()->semanticMethod(original,wrongOrder,true,"selftest_rejected"),"reordered debug event");
        final Method noCatch=copy(encoded,encoded.getName(),encoded.getAccessFlags(),new ImmutableMethodImplementation(1,encoded.getImplementation().getInstructions(),Collections.<TryBlock<? extends ExceptionHandler>>emptyList(),encoded.getImplementation().getDebugItems()));auditRejected(()->semanticMethod(original,noCatch,true,"selftest_rejected"),"removed exception handler");
        List<ImmutableExceptionHandler> catches=Arrays.asList(new ImmutableExceptionHandler("Ljava/lang/InterruptedException;",4),new ImmutableExceptionHandler("Ljava/util/concurrent/ExecutionException;",4));
        final Method multi=copy(original,original.getName(),original.getAccessFlags(),new ImmutableMethodImplementation(1,original.getImplementation().getInstructions(),Collections.singletonList(new ImmutableTryBlock(0,2,catches)),original.getImplementation().getDebugItems()));
        final MutableMethod split=new MutableMethod(multi);check(multi.getImplementation().getTryBlocks().size()==1&&split.getImplementation().getTryBlocks().size()==2,"dexlib same-range catch-group fixture");semanticMethod(multi,split,false,"selftest_same_range_try");
        final Method wrongCatchOrder=copy(multi,multi.getName(),multi.getAccessFlags(),new ImmutableMethodImplementation(1,multi.getImplementation().getInstructions(),Collections.singletonList(new ImmutableTryBlock(0,2,Arrays.asList(catches.get(1),catches.get(0)))),multi.getImplementation().getDebugItems()));
        auditRejected(()->semanticMethod(multi,wrongCatchOrder,false,"selftest_rejected"),"reordered catch handlers");
        final Method wrongCatchRange=copy(multi,multi.getName(),multi.getAccessFlags(),new ImmutableMethodImplementation(1,multi.getImplementation().getInstructions(),Collections.singletonList(new ImmutableTryBlock(0,3,catches)),multi.getImplementation().getDebugItems()));
        auditRejected(()->semanticMethod(multi,wrongCatchRange,false,"selftest_rejected"),"changed protected instruction range");
        final Method padded=paddingFixture(false),unpadded=paddingFixture(true);check(instructions(padded).size()==6&&instructions(unpadded).size()==5,"real payload padding-width fixture");semanticMethod(padded,unpadded,true,"selftest_alignment");
        final Method debugOnPadding=copy(padded,padded.getName(),padded.getAccessFlags(),new ImmutableMethodImplementation(1,padded.getImplementation().getInstructions(),padded.getImplementation().getTryBlocks(),Collections.singletonList(new ImmutableLineNumber(7,100))));
        auditRejected(()->semanticMethod(debugOnPadding,unpadded,true,"selftest_rejected"),"omitted padding debug line");
        final Method debugOnPayload=copy(unpadded,unpadded.getName(),unpadded.getAccessFlags(),new ImmutableMethodImplementation(1,unpadded.getImplementation().getInstructions(),unpadded.getImplementation().getTryBlocks(),Collections.singletonList(new ImmutableLineNumber(8,100))));semanticMethod(debugOnPadding,debugOnPayload,true,"selftest_padding_debug");
        final Method debugOnExecutable=copy(unpadded,unpadded.getName(),unpadded.getAccessFlags(),new ImmutableMethodImplementation(1,unpadded.getImplementation().getInstructions(),unpadded.getImplementation().getTryBlocks(),Collections.singletonList(new ImmutableLineNumber(7,100))));auditRejected(()->semanticMethod(debugOnPadding,debugOnExecutable,true,"selftest_rejected"),"padding debug line moved onto an executable return");
        final Method lineAtPayloadWithPad=copy(padded,padded.getName(),padded.getAccessFlags(),new ImmutableMethodImplementation(1,padded.getImplementation().getInstructions(),padded.getImplementation().getTryBlocks(),Collections.singletonList(new ImmutableLineNumber(8,100))));auditRejected(()->semanticMethod(lineAtPayloadWithPad,debugOnPadding,true,"selftest_rejected"),"payload debug line moved backward onto padding");
        final MutableMethod targetedPadding=new MutableMethod(padded);targetedPadding.getImplementation().replaceInstruction(5,new BuilderPackedSwitchPayload(0,Collections.singletonList(targetedPadding.getImplementation().newLabelForIndex(4))));
        auditRejected(()->semanticMethod(targetedPadding,unpadded,true,"selftest_rejected"),"padding used as a switch-case target");
        for(Opcode width:Arrays.asList(Opcode.GOTO_16,Opcode.GOTO_32)){MutableMethod widened=new MutableMethod(original);MutableMethodImplementation code=widened.getImplementation();code.replaceInstruction(2,width==Opcode.GOTO_16?new BuilderInstruction20t(width,code.newLabelForIndex(5)):new BuilderInstruction30t(width,code.newLabelForIndex(5)));semanticMethod(original,widened,true,"selftest_goto_width");}
        final MutableMethod wrongWideTarget=new MutableMethod(original);wrongWideTarget.getImplementation().replaceInstruction(2,new BuilderInstruction20t(Opcode.GOTO_16,wrongWideTarget.getImplementation().newLabelForIndex(4)));auditRejected(()->semanticMethod(original,wrongWideTarget,true,"selftest_rejected"),"wide GOTO with a different target");
        final MutableMethod changedCondition=new MutableMethod(original);changedCondition.getImplementation().replaceInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,0,changedCondition.getImplementation().newLabelForIndex(5)));auditRejected(()->semanticMethod(original,changedCondition,true,"selftest_rejected"),"unconditional GOTO changed to a conditional branch");
        tests.add("encoding-normalization regression: exact string/jumbo and debug floor with relocated catch PCs accepted; string/literal/branch/instruction/debug line/local/order/position/catch changes rejected");
        tests.add("try-group/payload-padding regression: exact ordered same-range handler grouping and unreachable unreferenced alignment pad accepted; handler reordering/range changes and referenced/debug-bearing padding rejected");
        tests.add("GOTO width regression: GOTO/GOTO_16/GOTO_32 target-preserving encoding accepted; different target or conditional opcode rejected");
        tests.add("padding line-event regression: non-executable unreferenced padding line may move only to its same payload; dropping it, moving to executable code, or moving backward rejected");
    }

    /** Strip only approved single-call insertions, then compare complete serialized original body. */
    private static void preserved(Method original,Method changed,boolean helper)throws Exception{
        check(changed!=null,"missing preserved body "+original);
        if(original.getImplementation()==null){check(Arrays.equals(canonical(original),canonical(changed)),"abstract/non-code method changed "+original);return;}
        check(changed.getImplementation()!=null,"lost original implementation "+original);
        check(original.getImplementation().getRegisterCount()==changed.getImplementation().getRegisterCount(),"original register count changed "+original);
        if(helper){int flags=(original.getAccessFlags()&~(1|4|16|0x400))|2|0x1000;
            check(changed.getAccessFlags()==flags,"helper visibility/flags differ "+changed);
            check(changed.getName().equals(HELPER+original.getName()),"helper name differs");}
        else check(original.getAccessFlags()==changed.getAccessFlags(),"method access changed "+original);
        MutableMethod restored=new MutableMethod(changed);List<BuilderInstruction> list=restored.getImplementation().getInstructions();
        int lifecycleCount=0,showCount=0;
        for(int n=list.size()-1;n>=0;n--){Instruction i=list.get(n);MethodReference r=reference(i);if(r==null||!RUNTIME.equals(r.getDefiningClass()))continue;
            String hook=r.getName();check(r.equals(runtime(hook)),"injected ABI mismatch "+original);
            if("dialogShown".equals(hook)){
                check(n>=1,"dialog hook before show");int receiver;
                if(dialogVoid(list.get(n-1)))receiver=registers(list.get(n-1))[0];
                else{check(n>=2&&list.get(n-1).getOpcode()==Opcode.MOVE_RESULT_OBJECT&&dialogValue(list.get(n-2)),"dialog hook not immediately after its shown object "+original);receiver=((OneRegisterInstruction)list.get(n-1)).getRegisterA();}
                requireCall(i,Opcode.INVOKE_STATIC,runtime("dialogShown"),receiver);showCount++;
            }else{
                check(!helper&&subtype(original.getDefiningClass(),ACTIVITY_BASES)&&instance(original)&&hook.equals(lifecycle(key(original)))&&n==0,"lifecycle hook escaped exact method entry "+original);
                requireCall(i,Opcode.INVOKE_STATIC,runtime(hook),self(original));lifecycleCount++;
            }
            restored.getImplementation().removeInstruction(n);
        }
        Method normal=copy(original,original.getName(),original.getAccessFlags(),restored.getImplementation());
        // Metadata must also survive body extraction and helper relocation.
        Method metadata=copy(changed,original.getName(),original.getAccessFlags(),null);
        check(Arrays.equals(canonical(copy(original,original.getName(),original.getAccessFlags(),null)),canonical(metadata)),"method metadata changed "+original);
        if(!Arrays.equals(canonical(original),canonical(normal)))semanticMethod(original,normal,false,"restored");
        if(!helper&&subtype(original.getDefiningClass(),ACTIVITY_BASES)&&instance(original)&&lifecycle(key(original))!=null)check(lifecycleCount==1,"missing lifecycle entry hook "+original);
        int expectedShows=0;List<Instruction> old=instructions(original);
        for(int n=0;n<old.size();n++)if(dialogVoid(old.get(n))||(dialogValue(old.get(n))&&n+1<old.size()&&old.get(n+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT))expectedShows++;
        check(showCount==expectedShows,"not all materialized Dialog.show sites hooked "+original);
    }
    private static void backAudit(Method m,String hook){List<Instruction> list=instructions(m);check(list.size()==2,"Back must only close app and return "+m);
        requireCall(list.get(0),Opcode.INVOKE_STATIC,runtime(hook),self(m));check(list.get(1).getOpcode()==Opcode.RETURN_VOID,"Back must return");}
    private static void lifecycleAudit(ClassDef original,Method m){List<Instruction> list=instructions(m);String hook=lifecycle(key(m));check(hook!=null&&list.size()==3,"unexpected lifecycle bridge "+m);
        int receiver=self(m);requireCall(list.get(0),Opcode.INVOKE_STATIC,runtime(hook),receiver);int[] args=new int[m.getParameterTypes().size()+1];for(int n=0;n<args.length;n++)args[n]=receiver+n;
        requireCall(list.get(1),Opcode.INVOKE_SUPER,new ImmutableMethodReference(original.getSuperclass(),m.getName(),m.getParameterTypes(),"V"),args);
        check(list.get(2).getOpcode()==Opcode.RETURN_VOID,"lifecycle bridge must return");}
    private static void guardAudit(ClassDef original,Method m,String hook,Method old){
        List<Instruction> list=instructions(m);check(list.size()==7,"guard instruction count "+m);int receiver=self(m),argWords=m.getParameterTypes().size()+1;
        check(receiver>=1,"guard requires separate result register");
        requireCall(list.get(0),Opcode.INVOKE_STATIC,runtime(hook),receiver,receiver+argWords-1);
        check(list.get(1).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)list.get(1)).getRegisterA()==0,"guard result register");
        check(list.get(2).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)list.get(2)).getRegisterA()==0&&branch(list,2)==4,"false must dispatch original path");
        check(list.get(3).getOpcode()==Opcode.RETURN&&((OneRegisterInstruction)list.get(3)).getRegisterA()==0,"consumed Back must return true before original path");
        int[] args=new int[argWords];for(int n=0;n<args.length;n++)args[n]=receiver+n;
        MethodReference fallback=new ImmutableMethodReference(old==null?original.getSuperclass():m.getDefiningClass(),old==null?m.getName():HELPER+m.getName(),m.getParameterTypes(),m.getReturnType());
        requireCall(list.get(4),old==null?Opcode.INVOKE_SUPER:Opcode.INVOKE_DIRECT,fallback,args);
        check(list.get(5).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)list.get(5)).getRegisterA()==0,"original result fetched");
        check(list.get(6).getOpcode()==Opcode.RETURN&&((OneRegisterInstruction)list.get(6)).getRegisterA()==0,"non-Back preserves original bool result");
        check(m.getImplementation().getTryBlocks().isEmpty(),"guard must not swallow original dispatch exceptions");
    }
    private static void classAudit(ClassDef original,ClassDef patched)throws Exception{
        check(Arrays.equals(canonical(withoutMethods(original)),canonical(withoutMethods(patched))),"class metadata or fields modified "+original.getType());
        Map<String,Method> before=methods(original),after=methods(patched);Set<String> consumed=new HashSet<String>();String type=original.getType();
        boolean activity=subtype(type,ACTIVITY_BASES),dialog=subtype(type,DIALOG_BASES),boundary=!hierarchy.containsKey(original.getSuperclass());
        for(Method old:before.values()){
            String k=key(old);Method changed=after.get(k);check(changed!=null,"lost method "+old);consumed.add(k);
            if(old.getImplementation()==null||!instance(old)){preserved(old,changed,false);continue;}
            if(BACK.equals(k)&&(activity||dialog)){
                check(Arrays.equals(canonical(copy(old,old.getName(),old.getAccessFlags(),null)),canonical(copy(changed,changed.getName(),changed.getAccessFlags(),null))),"Back method metadata changed");
                backAudit(changed,activity?"back":"dialogBack");continue;
            }
            String hook=guardHook(type,k);
            if(hook!=null){String helper=HELPER+k;Method body=after.get(helper);check(body!=null,"original key method helper missing "+old);consumed.add(helper);
                check(Arrays.equals(canonical(copy(old,old.getName(),old.getAccessFlags(),null)),canonical(copy(changed,changed.getName(),changed.getAccessFlags(),null))),"key wrapper metadata changed");
                guardAudit(original,changed,hook,old);preserved(old,body,true);
            }else preserved(old,changed,false);
        }
        for(Map.Entry<String,Method> item:after.entrySet())if(!consumed.contains(item.getKey())){
            String k=item.getKey();Method m=item.getValue();
            check(!before.containsKey(k)&&boundary,"unexpected descendant/helper addition "+m);
            if(activity&&lifecycle(k)!=null)lifecycleAudit(original,m);
            else if(BACK.equals(k)&&(activity||dialog))backAudit(m,activity?"back":"dialogBack");
            else if(KEY.equals(k)&&(activity||dialog))guardAudit(original,m,activity?"key":"dialogKey",null);
            else if(PREIME.equals(k)&&subtype(type,TEXT_BASES))guardAudit(original,m,"keyFromView",null);
            else throw new AssertionError("unexpected added method "+m);
        }
        if(boundary){List<String> required=activity?Arrays.asList(CREATE,RESUME,DESTROY,BACK,KEY):dialog?Arrays.asList(BACK,KEY):subtype(type,TEXT_BASES)?Collections.singletonList(PREIME):Collections.<String>emptyList();
            for(String k:required)check(after.containsKey(k),"missing framework-boundary hook "+type+k);}
    }

    private static final class Applied implements OneBackPatch.Editor{
        final Map<String,ClassDef> original;final Map<String,MutableClass> changed=new TreeMap<String,MutableClass>();Map<String,Integer> counts;
        Applied(Map<String,ClassDef> source){original=source;}
        public MutableClass mutable(String type){MutableClass c=changed.get(type);if(c==null){check(original.containsKey(type),"editor requested unknown class");c=new MutableClass(original.get(type));changed.put(type,c);}return c;}
        Map<String,ClassDef> result(){Map<String,ClassDef> out=new TreeMap<String,ClassDef>(original);out.putAll(changed);return out;}
        void apply(){counts=OneBackPatch.applyAll(original,this);}
    }
    private static void auditTransform(Map<String,ClassDef> original,Map<String,ClassDef> patched)throws Exception{
        hierarchy=original;for(String type:original.keySet()){
            check(patched.containsKey(type),"lost class "+type);
            if(original.get(type)==patched.get(type))continue;
            if(type.startsWith(PREFIX))check(Arrays.equals(canonical(original.get(type)),canonical(patched.get(type))),"existing OneBack runtime changed by transform");
            else classAudit(original.get(type),patched.get(type));
        }
    }

    private static void merge(String basePath,String additionPath,String outputPath)throws Exception{
        Map<String,ClassDef> base=classes(basePath),addition=classes(additionPath),merged=new TreeMap<String,ClassDef>(base);
        check(!base.isEmpty()&&!addition.isEmpty(),"empty loader input");
        for(ClassDef c:addition.values()){
            check(c.getType().startsWith(PREFIX),"new loader escaped OneBack namespace "+c.getType());
            check(!merged.containsKey(c.getType()),"loader collision; existing class must never be replaced "+c.getType());merged.put(c.getType(),c);
        }
        DexPool.writeTo(outputPath,new ImmutableDexFile(Opcodes.getDefault(),merged.values()));Map<String,ClassDef> read=classes(outputPath);
        check(read.keySet().equals(merged.keySet()),"merged loader class inventory mismatch");
        for(ClassDef c:merged.values())check(Arrays.equals(canonical(c),canonical(read.get(c.getType()))),"merged loader class was altered "+c.getType());
        System.out.println("PASS OneBack union merge: all "+base.size()+" baseline classes canonically unchanged; added="+addition.size()+"; total="+read.size()+"; assertions="+assertions);
    }

    private static ImmutableMethod method(String owner,String name,List<String> params,String result,int flags,MethodImplementation code){
        List<ImmutableMethodParameter> p=new ArrayList<ImmutableMethodParameter>();for(String s:params)p.add(new ImmutableMethodParameter(s,Collections.<Annotation>emptySet(),null));
        return new ImmutableMethod(owner,name,p,result,flags,Collections.<Annotation>emptySet(),Collections.emptySet(),code);
    }
    private static ClassDef clazz(String type,String parent,int flags,Method...methods){return new ImmutableClassDef(type,flags,parent,Collections.<String>emptyList(),"Fixture.java",Collections.<Annotation>emptySet(),Collections.<Field>emptyList(),Arrays.asList(methods));}
    private static MutableMethodImplementation body(int registers,BuilderInstruction...instructions){MutableMethodImplementation c=new MutableMethodImplementation(registers);for(BuilderInstruction i:instructions)c.addInstruction(i);return c;}
    private static BuilderInstruction invoke(Opcode opcode,MethodReference target,int...args){boolean small=args.length<=5;for(int r:args)if(r>15)small=false;
        if(small){int[] r=new int[5];System.arraycopy(args,0,r,0,args.length);return new BuilderInstruction35c(opcode,args.length,r[0],r[1],r[2],r[3],r[4],target);}
        for(int n=1;n<args.length;n++)check(args[n]==args[0]+n,"fixture range contiguous");
        Opcode range=opcode==Opcode.INVOKE_STATIC?Opcode.INVOKE_STATIC_RANGE:opcode==Opcode.INVOKE_SUPER?Opcode.INVOKE_SUPER_RANGE:Opcode.INVOKE_VIRTUAL_RANGE;
        return new BuilderInstruction3rc(range,args[0],args.length,target);
    }
    private static Method returnVoid(String owner,String name,int flags){return method(owner,name,Collections.<String>emptyList(),"V",flags,body(1,new BuilderInstruction10x(Opcode.RETURN_VOID)));}
    private static Method boolMethod(String owner,String name,List<String>params,int flags,int registers){return method(owner,name,params,"Z",flags,body(registers,new BuilderInstruction11n(Opcode.CONST_4,0,1),new BuilderInstruction11x(Opcode.RETURN,0)));}
    private static Map<String,ClassDef> fixtures(){
        Map<String,ClassDef> all=new TreeMap<String,ClassDef>();String b="Lfixture/BaseActivity;",child="Lfixture/ChildActivity;",empty="Lfixture/EmptyActivity;";
        MutableMethodImplementation keyBody=body(260,invoke(Opcode.INVOKE_SUPER,ref(ACTIVITY,"dispatchKeyEvent","Z",KEY_TYPE),258,259),new BuilderInstruction11x(Opcode.MOVE_RESULT,0),new BuilderInstruction10x(Opcode.NOP),new BuilderInstruction11n(Opcode.CONST_4,0,1),new BuilderInstruction11x(Opcode.RETURN,0),new BuilderInstruction11n(Opcode.CONST_4,0,0),new BuilderInstruction11x(Opcode.RETURN,0));
        keyBody.replaceInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,0,keyBody.newLabelForIndex(5)));
        all.put(b,clazz(b,ACTIVITY,1,
            method(b,"onCreate",Collections.singletonList("Landroid/os/Bundle;"),"V",4,body(2,invoke(Opcode.INVOKE_SUPER,ref(ACTIVITY,"onCreate","V","Landroid/os/Bundle;"),0,1),new BuilderInstruction10x(Opcode.RETURN_VOID))),
            method(b,"onResume",Collections.<String>emptyList(),"V",4,body(301,invoke(Opcode.INVOKE_SUPER,ref(ACTIVITY,"onResume","V"),300),new BuilderInstruction10x(Opcode.RETURN_VOID))),
            returnVoid(b,"onBackPressed",1|16),method(b,"dispatchKeyEvent",Collections.singletonList(KEY_TYPE),"Z",1,keyBody),returnVoid(b,"unrelated",1)));
        all.put(child,clazz(child,b,1,
            method(child,"onCreate",Collections.singletonList("Landroid/os/Bundle;"),"V",4,body(2,invoke(Opcode.INVOKE_SUPER,ref(b,"onCreate","V","Landroid/os/Bundle;"),0,1),new BuilderInstruction10x(Opcode.RETURN_VOID))),
            returnVoid(child,"onDestroy",4),boolMethod(child,"dispatchKeyEvent",Collections.singletonList(KEY_TYPE),1,2)));
        all.put(empty,clazz(empty,ACTIVITY,1));
        String abs="Lfixture/AbstractActivity;";
        all.put(abs,clazz(abs,ACTIVITY,1|0x400,method(abs,"dispatchKeyEvent",Collections.singletonList(KEY_TYPE),"Z",1|0x400,null)));
        String nativeActivity="Lfixture/NativeActivity;";all.put(nativeActivity,clazz(nativeActivity,"Landroid/app/NativeActivity;",1));
        String dialog="Lfixture/CustomDialog;",dialogChild="Lfixture/ChildDialog;";
        all.put(dialog,clazz(dialog,DIALOG,1,returnVoid(dialog,"onBackPressed",1),boolMethod(dialog,"dispatchKeyEvent",Collections.singletonList(KEY_TYPE),1|16,2)));
        all.put(dialogChild,clazz(dialogChild,dialog,1));String dialogEmpty="Lfixture/EmptyDialog;";all.put(dialogEmpty,clazz(dialogEmpty,"Landroid/app/AlertDialog;",1));
        String text="Lfixture/EditText;",textChild="Lfixture/ChildEditText;";
        all.put(text,clazz(text,"Landroid/widget/EditText;",1,boolMethod(text,"dispatchKeyEventPreIme",Collections.singletonList(KEY_TYPE),1|16,260)));
        all.put(textChild,clazz(textChild,text,1,boolMethod(textChild,"onKeyPreIme",Arrays.asList("I",KEY_TYPE),1,3)));
        String customView="Lfixture/CustomView;";
        all.put(customView,clazz(customView,VIEW,1,boolMethod(customView,"onKeyPreIme",Arrays.asList("I",KEY_TYPE),1,300),boolMethod(customView,"dispatchKeyEventPreIme",Collections.singletonList(KEY_TYPE),1,2)));
        String stranger="Lfixture/Unrelated;";all.put(stranger,clazz(stranger,"Ljava/lang/Object;",1,returnVoid(stranger,"onBackPressed",1),boolMethod(stranger,"dispatchKeyEvent",Collections.singletonList(KEY_TYPE),1,2),returnVoid(stranger,"onResume",1),returnVoid(stranger,"show",1)));
        String caller="Lfixture/DialogCaller;";List<Method> showMethods=new ArrayList<Method>();
        showMethods.add(method(caller,"showHigh",Collections.singletonList(DIALOG),"V",1|8,body(301,invoke(Opcode.INVOKE_VIRTUAL,ref(DIALOG,"show","V"),300),new BuilderInstruction10x(Opcode.RETURN_VOID))));
        showMethods.add(method(caller,"showBuilder",Collections.singletonList("Landroid/app/AlertDialog$Builder;"),"V",1|8,body(18,invoke(Opcode.INVOKE_VIRTUAL,ref("Landroid/app/AlertDialog$Builder;","show","Landroid/app/AlertDialog;"),17),new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,16),new BuilderInstruction10x(Opcode.RETURN_VOID))));
        showMethods.add(method(caller,"showIgnored",Collections.singletonList("Landroid/app/AlertDialog$Builder;"),"V",1|8,body(1,invoke(Opcode.INVOKE_VIRTUAL,ref("Landroid/app/AlertDialog$Builder;","show","Landroid/app/AlertDialog;"),0),new BuilderInstruction10x(Opcode.RETURN_VOID))));
        showMethods.add(method(caller,"unrelatedShow",Collections.singletonList(stranger),"V",1|8,body(1,invoke(Opcode.INVOKE_VIRTUAL,ref(stranger,"show","V"),0),new BuilderInstruction10x(Opcode.RETURN_VOID))));
        all.put(caller,clazz(caller,"Ljava/lang/Object;",1,showMethods.toArray(new Method[0])));
        // Extension classes are already merged by the patcher and must remain outside hook selection.
        String extension="Lapp/hiro/oneback/runtime/AlreadyLoaded;";all.put(extension,clazz(extension,ACTIVITY,1,returnVoid(extension,"onResume",1)));
        return all;
    }

    private static String xml(Document doc)throws Exception{Transformer t=TransformerFactory.newInstance().newTransformer();t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");StringWriter out=new StringWriter();t.transform(new DOMSource(doc),new StreamResult(out));return out.toString();}
    private static Document parse(String xml)throws Exception{return parse(xml,true);}
    private static Document parse(String xml,boolean namespaceAware)throws Exception{DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(namespaceAware);f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));}
    private static void manifestTests()throws Exception{
        for(String pkg:PACKAGES){String source="<manifest xmlns:android=\""+NS+"\" package=\""+pkg+"\"><uses-sdk android:targetSdkVersion=\"36\"/><uses-permission android:name=\"android.permission.CAMERA\"/><application android:label=\"Fixture\" android:enableOnBackInvokedCallback=\"false\"><activity android:name=\".One\" android:exported=\"true\" android:enableOnBackInvokedCallback=\"false\"/><activity android:name=\".Two\" android:excludeFromRecents=\"false\"/><activity-alias android:name=\".Alias\" android:targetActivity=\".One\"/><service android:name=\".Sync\" android:enabled=\"true\"/></application></manifest>";
            Document original=parse(source),changed=parse(source);String before=xml(original);int count=OneBackPatch.patchManifest(changed,pkg);check(count==3,"manifest hook attribute count");
            Element app=(Element)changed.getElementsByTagName("application").item(0);check("true".equals(app.getAttributeNS(NS,"enableOnBackInvokedCallback")),"application opts into modern Back");
            NodeList activities=changed.getElementsByTagName("activity");for(int n=0;n<activities.getLength();n++)check("true".equals(((Element)activities.item(n)).getAttributeNS(NS,"enableOnBackInvokedCallback")),"every Activity overrides opt-out");
            app.setAttributeNS(NS,"android:enableOnBackInvokedCallback","false");((Element)activities.item(0)).setAttributeNS(NS,"android:enableOnBackInvokedCallback","false");((Element)activities.item(1)).removeAttributeNS(NS,"enableOnBackInvokedCallback");
            check(before.equals(xml(changed)),"manifest changed anything beyond callback opt-in");
            check(!((Element)changed.getElementsByTagName("activity-alias").item(0)).hasAttributeNS(NS,"enableOnBackInvokedCallback"),"alias must not receive unsupported attribute");
            // Morphe Document uses a namespace-unaware DOM. Serialize and parse again so an
            // old false qualified-name attribute cannot silently win over a new namespaced one.
            Document legacy=parse(source,false);check(OneBackPatch.patchManifest(legacy,pkg)==3,"legacy DOM callback count");Document reloaded=parse(xml(legacy));
            check("true".equals(((Element)reloaded.getElementsByTagName("application").item(0)).getAttributeNS(NS,"enableOnBackInvokedCallback")),"Morphe namespace-unaware DOM must overwrite existing application false");
            NodeList reloadedActivities=reloaded.getElementsByTagName("activity");for(int n=0;n<reloadedActivities.getLength();n++)check("true".equals(((Element)reloadedActivities.item(n)).getAttributeNS(NS,"enableOnBackInvokedCallback")),"Morphe namespace-unaware DOM must overwrite existing Activity false");
            final Document mismatch=parse(source);String snapshot=xml(mismatch);rejected(()->OneBackPatch.patchManifest(mismatch,"other.package"),"unsupported manifest package accepted");check(snapshot.equals(xml(mismatch)),"unsupported package partially modified manifest");
        }
        final Document mismatch=parse("<manifest package=\"wrong\"><application/></manifest>");String mismatchBefore=xml(mismatch);rejected(()->OneBackPatch.patchManifest(mismatch,"ctrip.english"),"package mismatch accepted");check(mismatchBefore.equals(xml(mismatch)),"package mismatch mutated manifest");
        for(String bad:Arrays.asList("<manifest package=\"ctrip.english\"/>","<manifest package=\"ctrip.english\"><application/><application/></manifest>")){
            final Document d=parse(bad);String before=xml(d);rejected(()->OneBackPatch.patchManifest(d,"ctrip.english"),"invalid application count accepted");check(before.equals(xml(d)),"invalid manifest partially edited");}
        tests.add("manifest: namespace-aware and Morphe namespace-unaware DOM serialization; exact callback opt-in; aliases/permissions/targetSdk/recents/service preserved; atomic rejection");
    }
    private static Object get(Object target,String method)throws Exception{return target.getClass().getMethod(method).invoke(target);}
    @SuppressWarnings("unchecked") private static void metadataTests()throws Exception{
        check(PACKAGES.equals(OneBackPatch.PACKAGES),"compatibility package set must be exact");
        for(String pkg:PACKAGES)OneBackPatch.validatePackage(pkg);
        for(final String pkg:Arrays.asList("other.package","com.twitter.android.beta","",null))rejected(()->OneBackPatch.validatePackage(pkg),"unsupported package accepted");
        Object patch=OneBackPatch.PATCH;check(OneBackPatch.NAME.equals(get(patch,"getName")),"patch display name");check(Boolean.TRUE.equals(get(patch,"getUse")),"OneBack must default enabled");
        Set<String> actual=new LinkedHashSet<String>();Object compatibility=get(patch,"getCompatiblePackages");check(compatibility instanceof Set,"compatible packages shape");
        for(Object value:(Set<?>)compatibility){check(value instanceof Pair,"compatible package entry");Pair<?,?> pair=(Pair<?,?>)value;actual.add((String)pair.getFirst());check(pair.getSecond()==null,"patch must not impose a version list");}
        check(PACKAGES.equals(actual),"patch metadata package set");Object dependencies=get(patch,"getDependencies");check(dependencies instanceof Set&&((Set<?>)dependencies).size()==1,"exactly one manifest resource dependency");
        Object resource=((Set<?>)dependencies).iterator().next();check(resource.getClass().getSimpleName().equals("ResourcePatch"),"manifest dependency is a resource patch");check(get(resource,"getName")==null,"resource dependency must be hidden");check(Boolean.FALSE.equals(get(resource,"getUse")),"hidden dependency not separately enabled");
        tests.add("metadata: default enabled, five exact packages, unrestricted versions, one hidden resource dependency");
    }
    private static void rejectAtomic(final Map<String,ClassDef> input,String reason)throws Exception{
        final Applied applied=new Applied(input);rejected(()->applied.apply(),reason);check(applied.changed.isEmpty(),"rejected plan acquired mutable proxies: "+reason);
    }
    private static void rejectionTests(Map<String,ClassDef> original,Applied applied)throws Exception{
        rejectAtomic(applied.result(),"reapplying an existing helper-bearing patch must fail");
        Map<String,ClassDef> collision=new TreeMap<String,ClassDef>(original);String t="Lzzzz/Collision;";collision.put(t,clazz(t,"Ljava/lang/Object;",1,returnVoid(t,HELPER+"unrelated",1)));rejectAtomic(collision,"late helper name collision must fail before editing");
        Map<String,ClassDef> noActivity=new TreeMap<String,ClassDef>();String t2="Lfixture/OnlyView;";noActivity.put(t2,clazz(t2,VIEW,1));rejectAtomic(noActivity,"no Activity must fail before editing");
        for(String signature:Arrays.asList(BACK,KEY,CREATE,RESUME,DESTROY,PREIME,DISPATCH_PREIME)){
            Map<String,ClassDef> nativeCase=new TreeMap<String,ClassDef>(original);String type="Lzzzz/NativeCallback;",name=signature.substring(0,signature.indexOf('('));
            List<String> params=signature.equals(KEY)||signature.equals(DISPATCH_PREIME)?Collections.singletonList(KEY_TYPE):signature.equals(PREIME)?Arrays.asList("I",KEY_TYPE):signature.equals(CREATE)?Collections.singletonList("Landroid/os/Bundle;"):Collections.<String>emptyList();
            String parent=signature.equals(PREIME)||signature.equals(DISPATCH_PREIME)?VIEW:ACTIVITY;
            nativeCase.put(type,clazz(type,parent,1,method(type,name,params,signature.endsWith("Z")?"Z":"V",1|0x100,null)));rejectAtomic(nativeCase,"native targeted callback must reject safely: "+signature);
        }
        tests.add("atomic preflight: duplicate application, late helper collision, missing Activity, seven native callback signatures");
    }

    private static void saveReport(String path,Map<String,Object> extra)throws Exception{
        Map<String,Object> report=new LinkedHashMap<String,Object>();report.put("result","PASS");report.put("blocking_findings",Collections.emptyList());report.put("assertions",assertions);report.put("tests",new ArrayList<String>(tests));report.putAll(extra);
        report.put("encoding_normalizations",new TreeMap<String,Long>(normalizations));String json=json(report);if(path!=null){Path p=Paths.get(path);if(p.toAbsolutePath().getParent()!=null)Files.createDirectories(p.toAbsolutePath().getParent());Files.write(p,(json+"\n").getBytes(StandardCharsets.UTF_8));}System.out.println("PASS OneBack DEX audit: assertions="+assertions+"; tests="+tests.size());
    }
    private static String quote(String s){StringBuilder b=new StringBuilder("\"");for(int n=0;n<s.length();n++){char c=s.charAt(n);if(c=='\\'||c=='\"')b.append('\\').append(c);else if(c=='\n')b.append("\\n");else if(c=='\r')b.append("\\r");else if(c=='\t')b.append("\\t");else if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}return b.append('"').toString();}
    private static String json(Object value){if(value==null)return"null";if(value instanceof String)return quote((String)value);if(value instanceof Number||value instanceof Boolean)return value.toString();
        StringBuilder b=new StringBuilder();if(value instanceof Map){b.append('{');boolean first=true;for(Map.Entry<?,?> e:((Map<?,?>)value).entrySet()){if(!first)b.append(',');first=false;b.append(quote(String.valueOf(e.getKey()))).append(':').append(json(e.getValue()));}return b.append('}').toString();}
        if(value instanceof Iterable){b.append('[');boolean first=true;for(Object o:(Iterable<?>)value){if(!first)b.append(',');first=false;b.append(json(o));}return b.append(']').toString();}return quote(String.valueOf(value));}
    private static void selftest(String report,String originalPath,String patchedPath)throws Exception{
        metadataTests();manifestTests();normalizationTests();Map<String,ClassDef> original=fixtures();Applied applied=new Applied(original);applied.apply();auditTransform(original,applied.result());
        check(!methods(applied.result().get("Lfixture/ChildActivity;")).containsKey(BACK),"child must inherit final Back without illegal override");
        check(!methods(applied.result().get("Lfixture/ChildDialog;")).containsKey(KEY),"child must inherit final Dialog key method");
        check(!methods(applied.result().get("Lfixture/ChildEditText;")).containsKey(DISPATCH_PREIME),"child must inherit final pre-IME dispatcher");
        check(applied.counts.get("dialog_show_hooks")==1&&applied.counts.get("dialog_builder_hooks")==1,"fixture direct/Builder Dialog hook coverage");
        tests.add("Activity hierarchy: zero-local/high-register lifecycle and original dispatch bodies, final inheritance, abstract override preservation, framework boundary bridges");
        tests.add("View hierarchy: existing onKeyPreIme/dispatchKeyEventPreIme, high registers, text boundary bridge and inherited final method");
        tests.add("Dialog hierarchy/show sites: high-register virtual show, Builder materialized result, unrelated/discarded result unchanged");
        tests.add("independent scope audit: original bodies restored byte-for-byte, original flags/annotations/fields/class metadata preserved");
        rejectionTests(original,applied);
        if(originalPath!=null){DexPool.writeTo(originalPath,new ImmutableDexFile(Opcodes.getDefault(),original.values()));DexPool.writeTo(patchedPath,new ImmutableDexFile(Opcodes.getDefault(),applied.result().values()));auditTransform(classes(originalPath),classes(patchedPath));tests.add("roundtrip: emitted original and patched DEX reopen with exactly audited code and hierarchy");}
        Map<String,Object> extra=new LinkedHashMap<String,Object>();extra.put("counts",applied.counts);extra.put("fixture_classes",original.size());extra.put("changed_classes",applied.changed.size());extra.put("physical_device_tested",false);saveReport(report,extra);
    }
    private static void appAudit(String originalPath,String patchedPath,String report)throws Exception{
        Map<String,ClassDef> original=classes(originalPath),patched=classes(patchedPath);Applied expected=new Applied(original);expected.apply();Map<String,ClassDef> reviewed=expected.result();auditTransform(original,reviewed);int preserved=0,changed=0,added=0,hooks=0,internalRefs=0;
        int checked=0;for(String type:original.keySet()){check(patched.containsKey(type),"emitted application lost class "+type);ClassDef target=reviewed.get(type);byte[] targetBytes=canonical(target);if(!Arrays.equals(targetBytes,canonical(patched.get(type))))semanticClass(target,patched.get(type),"emitted");
            if(original.get(type)==target||Arrays.equals(canonical(original.get(type)),targetBytes))preserved++;else changed++;if(++checked%25000==0)System.out.println("AUDIT emitted classes="+checked+"/"+original.size());}
        for(ClassDef c:patched.values()){
            if(!original.containsKey(c.getType())){check(c.getType().startsWith("Lapp/hiro/oneback/runtime/OneBackExit"),"unexpected added application class "+c.getType());added++;}
            for(Method m:c.getMethods())for(Instruction i:instructions(m)){MethodReference r=reference(i);if(r!=null&&r.getDefiningClass().startsWith("Lapp/hiro/oneback/runtime/")){
                ClassDef target=patched.get(r.getDefiningClass());check(target!=null&&methods(target).containsKey(key(r)),"unresolved OneBack runtime ABI "+r);internalRefs++;
                if(!c.getType().startsWith("Lapp/hiro/oneback/runtime/")){check(RUNTIME.equals(r.getDefiningClass())&&r.equals(runtime(r.getName())),"application injected private or unknown runtime ABI "+r);hooks++;}
            }}
        }
        check(patched.containsKey(RUNTIME)&&added>0&&hooks>0,"runtime payload/hook missing from emitted application");
        tests.add("emitted application: every original class equals reviewed transform, allowing only verified const-string/jumbo reencoding and exact debug instruction-start floor; instruction operands/targets, catches, and all debug event values/order retained; added classes limited to OneBackExit runtime");
        tests.add("emitted application: every OneBack runtime invocation resolves, only nine public hook signatures cross application boundary");
        Map<String,Object> extra=new LinkedHashMap<String,Object>();extra.put("counts",expected.counts);extra.put("original_classes",original.size());extra.put("unchanged_classes",preserved);extra.put("changed_classes",changed);extra.put("runtime_classes_added",added);extra.put("public_hooks",hooks);extra.put("resolved_runtime_calls",internalRefs);extra.put("physical_device_tested",false);saveReport(report,extra);
    }
    public static void main(String[]args)throws Exception{
        if(args.length==4&&"merge".equals(args[0])){merge(args[1],args[2],args[3]);return;}
        if((args.length==1||args.length==2||args.length==4)&&"selftest".equals(args[0])){selftest(args.length>=2?args[1]:null,args.length==4?args[2]:null,args.length==4?args[3]:null);return;}
        if(args.length==4&&"audit-app".equals(args[0])){appAudit(args[1],args[2],args[3]);return;}
        throw new IllegalArgumentException("DexAudit merge BASEDEX NEWDEX OUTDEX | selftest [REPORT_JSON [ORIGINAL_DEX PATCHED_DEX]] | audit-app ORIGINAL.apk PATCHED.apk REPORT_JSON");
    }
}
