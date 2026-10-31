package app.hiro.oneback.patches;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/**
 * Admission for TikTok's real Home navigation API. Method bodies were inspected
 * in the original global APK with SHA-256:
 * 47.4.3 af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b
 * Merely finding any Activity must never mark an unknown TikTok build supported.
 */
public final class TikTokNativeContract {
    private TikTokNativeContract() {}
    public static final String ABILITY="Lcom/bytedance/tiktok/homepage/mainfragment/HomeTabAbility;";
    public static final String ASSEM="Lcom/ss/android/ugc/aweme/main/assems/tabs/TabAbilityAssem;";
    public static final String HOX="Lcom/bytedance/hox/Hox;";
    public static final String MAIN="Lcom/ss/android/ugc/aweme/main/MainActivity;";
    public static final String STATE="Lcom/ss/android/ugc/aweme/homepage/api/interaction/ScrollSwitchStateManager;";
    public static final String STATE_BASE="Lcom/ss/android/ugc/aweme/homepage/api/interaction/BaseScrollSwitchStateManager;";
    public static final String PAGE="Lcom/ss/android/ugc/aweme/base/ui/CommonPageFragment;";
    public static final String PAGER_ITEM="Lcom/ss/android/ugc/aweme/homepage/api/interaction/CurrentPagerItem;";
    public static final String OUTER="Lcom/ss/android/ugc/aweme/base/HomePageViewPagerAbility;";
    public static final String OUTER_ASSEM="Lcom/ss/android/ugc/aweme/main/assems/ui/HomepageViewPagerAssem;";
    private static final String STRING="Ljava/lang/String;",BUNDLE="Landroid/os/Bundle;";
    private static final String FIELD="communication_gen_field_com_bytedance_tiktok_homepage_mainfragment_HomeTabAbility";
    private static final String OUTER_FIELD="communication_gen_field_com_ss_android_ugc_aweme_base_HomePageViewPagerAbility";
    public static final String[][] VERIFIED_4743={
        {ASSEM,"tB0",BUNDLE+","+STRING,"V","1c88cab028e4f3d992ae47bb8d57a88b3f465803ee9bba274293dd2a6e31c9a6"},
        {ASSEM,"tv2","",STRING,"e58fa8a3e69c7d7a81365acaa994716fbd913b3be9692fd8e3ec0a162ce22c62"},
        {ASSEM,"jI0","",HOX,"b2992a5839719508184a662bfa8ff6889c37e16566aba4b8366622c54a99ba18"},
        {ASSEM,"Cp","",HOX,"a4fb6afb4dc8913d5d7d54e2dee35bf947ca82c373aaa7c9fb86f5d500ac73f9"},
        {HOX,"w33",STRING,"Z","ce919be67c02837b65745588b6c575be240df6fe2b3d7db9605797ac866d4d47"},
        {HOX,"p33",STRING+",Z",STRING,"f03d91812ba11c734786e9106f8b45ed3f3fc545b2d1813d7f86ef5786cc5cd8"},
        {HOX,"F33",BUNDLE+","+STRING+",Z","V","2580faeb7a7eed0d5da64d77781b47ab5d5a2871db498dcc03935dfae56a552c"},
        {HOX,"o33","LX/05Lz;","LX/05I1;","ab4e4769874d121b1798f423376826996b66341b667fbfe7fa65a86f477ee723"},
        // HOME in Hox is the inner tab. These native outer-pager methods prove
        // page_feed rather than a retained profile/camera/discovery page.
        {STATE_BASE,"s33",STRING,"Z","41756410dd1278928911ed145a39b1c50c60ae39d9a77f9b30202e5f6154aefe"},
        {STATE_BASE,"i33","",STRING,"120fcc92febcb8afc77d2848bf8b3a036f5fcc281d3721952ff67b0dc6928529"},
        {STATE_BASE,"j33",STRING,PAGE,"69abe7a36a3569400ca3fb41a39aa1d9060857f7503a079b2ac335f1efcd0ac6"},
        {STATE_BASE,"p33","I",STRING,"2209ab9fa678dc94fc812df7470f508c8c9ecd7739bdac9acc7d3e140e35eb4c"},
        {STATE_BASE,"r33",STRING,"I","6678b4e1f5ecaccbf95596d2d150a7d268012a10cfd3cc0a1dfaff3b072912a1"},
        {STATE_BASE,"k33","I",PAGE,"58ca3b22eaeeb5372694fbc35d93d027197df9065d1c7db2832189ae87545182"},
        {PAGE,"LJLI","","Z","d9f3bc28bd409db361d2cff96da33e11195f919bc5619041ff5e468e540f7dd8"},
        {OUTER_ASSEM,"Ir1","Ljava/lang/Boolean;,Z","Z","a96959e54e878984f5320ae60783e5915fa792e2ad30d29c7fa27e75700fd106"},
        {"LX/06O9;","LJ","Ljava/lang/Boolean;,Z","Z","20414f35d1c01c223edac58a43a94728cfaad4fd4db01b3d02246bd488ba1583"},
        {PAGER_ITEM,"isRealBridgeSet","","Z","af321996ffe7ab7834c3f9bf6ec4d837dbca39751de60e18ce618162618b9ec8"},
        {PAGER_ITEM,"getValue","","Ljava/lang/Integer;","637f9c8c98cc9f8d64811b9e29ff0c95838650bb11964b7a0b62e09ea4ade19a"},
        {PAGER_ITEM,"setValue","I","V","176be7f75e63e2e114c3da961b9666a0b3c5c004529cc6b32ecefd52a63711d0"}
    };
    static void require(boolean condition,String message) {
        if(!condition)throw new IllegalStateException("TikTok OneBack native contract: "+message);
    }
    public static Method method(ClassDef clazz,String name,String encodedParameters,String returns) {
        if(clazz==null)return null;
        List<String> parameters=encodedParameters.isEmpty()?Collections.<String>emptyList():Arrays.asList(encodedParameters.split(","));
        Method found=null;
        for(Method m:clazz.getMethods()) {
            if(!name.equals(m.getName())||!returns.equals(m.getReturnType())||m.getParameterTypes().size()!=parameters.size())continue;
            boolean match=true;for(int i=0;i<parameters.size();i++)if(!parameters.get(i).contentEquals(m.getParameterTypes().get(i)))match=false;
            if(match){require(found==null,"duplicate method "+clazz.getType()+"->"+name);found=m;}
        }
        return found;
    }
    private static void publicInstance(Method method,String owner,String name,boolean body) {
        require(method!=null,"missing "+owner+"->"+name);
        require((method.getAccessFlags()&9)==1,"non-public/static "+owner+"->"+name);
        if(body)require(method.getImplementation()!=null,"missing body "+owner+"->"+name);
    }
    private static void publicField(ClassDef owner,String name,String type) {
        boolean found=false;
        if(owner!=null)for(Field field:owner.getFields())if(name.equals(field.getName())&&type.equals(field.getType())&&(field.getAccessFlags()&9)==1)found=true;
        require(found,"public instance field absent: "+(owner==null?"missing class":owner.getType())+"->"+name+":"+type);
    }
    private static void outer4743(Map<String,ClassDef> classes,ClassDef main) {
        ClassDef state=classes.get(STATE),base=classes.get(STATE_BASE),page=classes.get(PAGE);
        ClassDef ability=classes.get(OUTER),assem=classes.get(OUTER_ASSEM),item=classes.get(PAGER_ITEM);
        require(state!=null&&(state.getAccessFlags()&0x201)==1&&STATE_BASE.equals(state.getSuperclass()),"native ScrollSwitchStateManager hierarchy absent");
        require(base!=null&&(base.getAccessFlags()&0x201)==1,"native BaseScrollSwitchStateManager absent");
        require(page!=null&&(page.getAccessFlags()&0x201)==1,"native CommonPageFragment absent");
        require(item!=null&&(item.getAccessFlags()&0x201)==1,"native CurrentPagerItem absent");
        require(ability!=null&&(ability.getAccessFlags()&0x201)==0x201,"HomePageViewPagerAbility interface absent");
        require(assem!=null&&(assem.getAccessFlags()&0x201)==1&&assem.getInterfaces().contains(OUTER),"HomepageViewPagerAssem implementation absent");
        publicField(main,"stateManager",STATE);
        publicField(main,OUTER_FIELD,OUTER);
        publicInstance(method(ability,"Ir1","Ljava/lang/Boolean;,Z","Z"),OUTER,"Ir1",false);
        publicInstance(method(page,"LJLI","","Z"),PAGE,"LJLI",true);
    }
    /** Returns null for other applications; no other app's admission changes. */
    public static String verifyIfTikTok(String packageName,String version,Map<String,ClassDef> classes) {
        if(!"com.zhiliaoapp.musically".equals(packageName)&&!"com.ss.android.ugc.trill".equals(packageName))return null;
        OneBackPatch.validateTarget(packageName,version);
        ClassDef ability=classes.get(ABILITY),assem=classes.get(ASSEM),hox=classes.get(HOX),main=classes.get(MAIN);
        require(ability!=null&&(ability.getAccessFlags()&0x201)==0x201,"HomeTabAbility interface absent");
        require(assem!=null&&(assem.getAccessFlags()&1)!=0&&assem.getInterfaces().contains(ABILITY),"TabAbilityAssem implementation absent");
        require(hox!=null&&(hox.getAccessFlags()&1)!=0,"Hox absent");
        require(main!=null&&(main.getAccessFlags()&1)!=0,"MainActivity absent");
        boolean field=false;for(Field f:main.getFields())if(FIELD.equals(f.getName())&&ABILITY.equals(f.getType())&&(f.getAccessFlags()&9)==1)field=true;
        require(field,"MainActivity public HomeTabAbility binding absent");
        publicInstance(method(main,"getAbility","Ljava/lang/Class;","Ljava/lang/Object;"),MAIN,"getAbility",true);
        boolean v4743="47.4.3".equals(version);
        if(v4743)outer4743(classes,main);
        String select=v4743?"tB0":"Lx0",current=v4743?"tv2":"Mm2",
            tree=v4743?"jI0":"qE0",active=v4743?"w33":"fU2";
        String[][] contract={{select,BUNDLE+","+STRING,"V"},{current,"",STRING},{tree,"",HOX}};
        for(String[] row:contract) {
            publicInstance(method(ability,row[0],row[1],row[2]),ABILITY,row[0],false);
            publicInstance(method(assem,row[0],row[1],row[2]),ASSEM,row[0],true);
        }
        publicInstance(method(hox,active,STRING,"Z"),HOX,active,true);
        if(v4743)for(String[] row:verified(version)) {
            Method m=method(classes.get(row[0]),row[1],row[2],row[3]);
            require(m!=null&&row[4].equals(digest(m)),version+" method differs: "+row[0]+"->"+row[1]+". Refusing a silently ineffective Home route.");
        }
        return v4743?"4743":"4623";
    }
    public static String[][] verified(String version) {
        if("47.4.3".equals(version))return VERIFIED_4743;
        throw new IllegalArgumentException("No full native fingerprint profile for "+version);
    }
    public static Map<String,String> fingerprints(Map<String,ClassDef> classes) {
        return fingerprints("47.4.3",classes);
    }
    public static Map<String,String> fingerprints(String version,Map<String,ClassDef> classes) {
        Map<String,String> report=new LinkedHashMap<String,String>();
        for(String[] row:verified(version))report.put(row[0]+"->"+row[1]+"("+row[2]+")"+row[3],digest(method(classes.get(row[0]),row[1],row[2],row[3])));
        return report;
    }
    /** Normalize only DEX pool-dependent string/goto widths, retain all semantics. */
    public static String digest(Method method) {
        require(method!=null&&method.getImplementation()!=null,"cannot fingerprint missing native method");
        MethodImplementation body=method.getImplementation();
        StringBuilder text=new StringBuilder("flags="+method.getAccessFlags()+" registers="+body.getRegisterCount()+"\n");
        List<Instruction> instructions=new ArrayList<Instruction>();Map<Integer,Integer> indices=new HashMap<Integer,Integer>();int pc=0;
        for(Instruction i:body.getInstructions()){indices.put(pc,instructions.size());instructions.add(i);pc+=i.getCodeUnits();}indices.put(pc,instructions.size());pc=0;
        for(Instruction i:instructions) {
            Opcode op=i.getOpcode();text.append(op==Opcode.CONST_STRING_JUMBO?Opcode.CONST_STRING:op==Opcode.GOTO_16||op==Opcode.GOTO_32?Opcode.GOTO:op);
            if(i instanceof OneRegisterInstruction)text.append(" a=").append(((OneRegisterInstruction)i).getRegisterA());
            if(i instanceof TwoRegisterInstruction)text.append(" b=").append(((TwoRegisterInstruction)i).getRegisterB());
            if(i instanceof ThreeRegisterInstruction)text.append(" c=").append(((ThreeRegisterInstruction)i).getRegisterC());
            if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;text.append(" args=").append(r.getRegisterCount()).append(',').append(r.getRegisterC()).append(',').append(r.getRegisterD()).append(',').append(r.getRegisterE()).append(',').append(r.getRegisterF()).append(',').append(r.getRegisterG());}
            if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction r=(RegisterRangeInstruction)i;text.append(" range=").append(r.getStartRegister()).append(',').append(r.getRegisterCount());}
            if(i instanceof WideLiteralInstruction)text.append(" literal=").append(((WideLiteralInstruction)i).getWideLiteral());
            if(i instanceof OffsetInstruction)text.append(" target=").append(index(indices,pc+((OffsetInstruction)i).getCodeOffset()));
            if(i instanceof ReferenceInstruction)text.append(" ref=").append(((ReferenceInstruction)i).getReference());
            if(i instanceof DualReferenceInstruction)text.append(" ref2=").append(((DualReferenceInstruction)i).getReference2());
            require(!(i instanceof SwitchPayload)&&!(i instanceof com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload),"unexpected native payload");
            text.append('\n');pc+=i.getCodeUnits();
        }
        for(TryBlock<? extends ExceptionHandler> t:body.getTryBlocks()) {
            text.append("try=").append(index(indices,t.getStartCodeAddress())).append(':').append(index(indices,t.getStartCodeAddress()+t.getCodeUnitCount()));
            for(ExceptionHandler h:t.getExceptionHandlers())text.append(" catch=").append(h.getExceptionType()).append(':').append(index(indices,h.getHandlerCodeAddress()));
            text.append('\n');
        }
        try{StringBuilder result=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)))result.append(String.format(Locale.ROOT,"%02x",b&255));return result.toString();}
        catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static int index(Map<Integer,Integer> indices,int pc) {Integer found=indices.get(pc);require(found!=null,"invalid native branch/handler target");return found;}
}
