package app.hiro.oneback.patches;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/**
 * Admission for TikTok's real Home navigation API. 47.5.3 method bodies were
 * inspected in the original APK with SHA-256
 * 6670ca3f93bfea379658cad3c27f05d3a380b23a0d82569cce684b7453e8ff3e.
 * Merely finding any Activity must never mark an unknown TikTok build supported.
 */
public final class TikTokNativeContract {
    private TikTokNativeContract() {}
    public static final String ABILITY="Lcom/bytedance/tiktok/homepage/mainfragment/HomeTabAbility;";
    public static final String ASSEM="Lcom/ss/android/ugc/aweme/main/assems/tabs/TabAbilityAssem;";
    public static final String HOX="Lcom/bytedance/hox/Hox;";
    public static final String MAIN="Lcom/ss/android/ugc/aweme/main/MainActivity;";
    private static final String STRING="Ljava/lang/String;",BUNDLE="Landroid/os/Bundle;";
    private static final String FIELD="communication_gen_field_com_bytedance_tiktok_homepage_mainfragment_HomeTabAbility";
    public static final String[][] VERIFIED_4753={
        {ASSEM,"IA0",BUNDLE+","+STRING,"V","28cc492dc6282bc7ecde6b889c4181a8791279cb35217569c4f7c73f782e2c4e"},
        {ASSEM,"Nu2","",STRING,"533dd67b73a857ecfc182ddb4cff658689c53266e4bc90bdd7910de90e3fceff"},
        {ASSEM,"EH0","",HOX,"b2992a5839719508184a662bfa8ff6889c37e16566aba4b8366622c54a99ba18"},
        {ASSEM,"Cp","",HOX,"79b3ccd5cb48ce98eb8e33a1d55e82f3eb253828c3b32b9235b8fc19b36ca42c"},
        {HOX,"i33",STRING,"Z","c8138056ff9d28b8069cfe1aa1e7ad1362567b3fcdc82b20d62cdfe3a77c18c9"},
        {HOX,"b33",STRING+",Z",STRING,"6159b4f6101ddd9b5cbad89534a3eb1a3664e8d3dc7fc37a2e8c650e41af542b"},
        {HOX,"r33",BUNDLE+","+STRING+",Z","V","23292cf07ddb70d87538fba2c383c52cb6f7d34b7c0965d488f592b6635c5daa"},
        {HOX,"a33","LX/05Fw;","LX/05Dq;","20f46b6f08abfc25cbb80b29ad8684ebb12b9a7847522e8c23f35c4b4b2aa6d6"}
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
    /** Returns null for other applications; no other app's admission changes. */
    public static String verifyIfTikTok(String packageName,String version,Map<String,ClassDef> classes) {
        if(!"com.zhiliaoapp.musically".equals(packageName)&&!"com.ss.android.ugc.trill".equals(packageName))return null;
        require("47.5.3".equals(version)||"46.2.3".equals(version),
            "unsupported TikTok "+version+"; verified native routes: 47.5.3 / legacy 46.2.3. Use the matching original APK.");
        require(!"47.5.3".equals(version)||"com.zhiliaoapp.musically".equals(packageName),
            "47.5.3 is verified for com.zhiliaoapp.musically only; no original trill 47.5.3 APK has been audited.");
        ClassDef ability=classes.get(ABILITY),assem=classes.get(ASSEM),hox=classes.get(HOX),main=classes.get(MAIN);
        require(ability!=null&&(ability.getAccessFlags()&0x201)==0x201,"HomeTabAbility interface absent");
        require(assem!=null&&(assem.getAccessFlags()&1)!=0&&assem.getInterfaces().contains(ABILITY),"TabAbilityAssem implementation absent");
        require(hox!=null&&(hox.getAccessFlags()&1)!=0,"Hox absent");
        require(main!=null&&(main.getAccessFlags()&1)!=0,"MainActivity absent");
        boolean field=false;for(Field f:main.getFields())if(FIELD.equals(f.getName())&&ABILITY.equals(f.getType())&&(f.getAccessFlags()&9)==1)field=true;
        require(field,"MainActivity public HomeTabAbility binding absent");
        publicInstance(method(main,"getAbility","Ljava/lang/Class;","Ljava/lang/Object;"),MAIN,"getAbility",true);
        boolean modern="47.5.3".equals(version);
        String select=modern?"IA0":"Lx0",current=modern?"Nu2":"Mm2",tree=modern?"EH0":"qE0",active=modern?"i33":"fU2";
        String[][] contract={{select,BUNDLE+","+STRING,"V"},{current,"",STRING},{tree,"",HOX}};
        for(String[] row:contract) {
            publicInstance(method(ability,row[0],row[1],row[2]),ABILITY,row[0],false);
            publicInstance(method(assem,row[0],row[1],row[2]),ASSEM,row[0],true);
        }
        publicInstance(method(hox,active,STRING,"Z"),HOX,active,true);
        if(modern)for(String[] row:VERIFIED_4753) {
            Method m=method(classes.get(row[0]),row[1],row[2],row[3]);
            require(m!=null&&row[4].equals(digest(m)),"47.5.3 method differs: "+row[0]+"->"+row[1]+". Refusing a silently ineffective Home route.");
        }
        return modern?"4753":"4623";
    }
    public static Map<String,String> fingerprints(Map<String,ClassDef> classes) {
        Map<String,String> report=new LinkedHashMap<String,String>();
        for(String[] row:VERIFIED_4753)report.put(row[0]+"->"+row[1]+"("+row[2]+")"+row[3],digest(method(classes.get(row[0]),row[1],row[2],row[3])));
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
