package app.hiro.mapsstrict.patches;

import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.*;

/** Accept only the exact published v1.0.0 runtime and its two known native hooks. */
public final class UpgradeContracts {
    private UpgradeContracts() {}
    public static final String REFERENCE = "/contracts/maps_strict_search_v1.0.0.dex";
    public static final String PREFIX = "Lapp/hiro/mapsstrict/";

    public static Map<String,ClassDef> resource(String name) {
        try (InputStream raw=MapsStrictSearchPatch.class.getResourceAsStream(name)) {
            MapsStrictSearchPatch.require(raw!=null,"Missing verified runtime resource " + name);
            DexFile dex=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),new BufferedInputStream(raw));
            Map<String,ClassDef> result=new TreeMap<String,ClassDef>();
            for(ClassDef c:dex.getClasses()) {
                MapsStrictSearchPatch.require(c.getType().startsWith(PREFIX),"Unexpected runtime namespace");
                MapsStrictSearchPatch.require(result.put(c.getType(),c)==null,"Duplicate runtime class");
            }
            MapsStrictSearchPatch.require(!result.isEmpty(),"Empty runtime resource");
            return result;
        } catch(Exception e) { throw new IllegalStateException("Cannot load verified Maps runtime",e); }
    }

    public static void validateLegacyRuntime(MapsStrictSearchPatch.Lookup lookup, Collection<ClassDef> installed) {
        Map<String,ClassDef> expected=resource(REFERENCE);
        Set<String> actual=new TreeSet<String>();
        for(ClassDef c:installed) if(c.getType().startsWith(PREFIX))actual.add(c.getType());
        MapsStrictSearchPatch.require(actual.equals(expected.keySet()),"Unknown or partial search filter installation");
        for(ClassDef reference:expected.values()) {
            ClassDef current=lookup.get(reference.getType());
            MapsStrictSearchPatch.require(current!=null
                &&RuntimeContracts.classBody(current).equals(RuntimeContracts.classBody(reference)),
                "Only published strict-search v1.0.0 can be upgraded: " + reference.getType());
        }
    }

    public static void validateInstalledNative(final MapsStrictSearchPatch.Lookup lookup) {
        final MutableClass request=new MutableClass(lookup.get(MapsStrictSearchPatch.OWNER));
        final MutableClass model=new MutableClass(lookup.get(MapsStrictSearchPatch.MODEL));
        MutableMethod f=(MutableMethod)MapsStrictSearchPatch.find(request,MapsStrictSearchPatch.TARGET);
        int at=MapsStrictSearchPatch.insertionIndex(f);
        List<? extends Instruction> code=f.getImplementation().getInstructions();
        checkField(code.get(at),1,0,"Lcqgi;->e:Ljava/lang/String;");
        checkField(code.get(at+1),2,9,"Lavds;->g:Lavdv;");
        checkCall(code.get(at+2),3,10,1,2,MapsStrictSearchPatch.FILTER);
        Instruction result=code.get(at+3),cast=code.get(at+4);
        MapsStrictSearchPatch.require(result.getOpcode()==Opcode.MOVE_RESULT_OBJECT
            &&((OneRegisterInstruction)result).getRegisterA()==10,"Unknown installed response hook");
        MapsStrictSearchPatch.require(cast.getOpcode()==Opcode.CHECK_CAST
            &&((OneRegisterInstruction)cast).getRegisterA()==10
            &&((ReferenceInstruction)cast).getReference().toString().equals("Lcqgm;"),"Unknown installed response cast");
        for(int i=0;i<5;i++)f.getImplementation().removeInstruction(at);
        MutableMethod merge=(MutableMethod)MapsStrictSearchPatch.find(model,NativeContracts.MERGE);
        int mergeAt=MapsStrictSearchPatch.mergeInsertionIndex(merge)-1;
        checkCall(merge.getImplementation().getInstructions().get(mergeAt),2,9,10,0,MapsStrictSearchPatch.RECONCILE);
        merge.getImplementation().removeInstruction(mergeAt);
        int markers=0;
        for(Iterator<MutableField> fields=model.getFields().iterator();fields.hasNext();) {
            MutableField field=fields.next();
            if(field.getName().equals(MapsStrictSearchPatch.MARKER)) {
                MapsStrictSearchPatch.require(field.getType().equals("Ljava/lang/Object;")
                    &&field.getAccessFlags()==MapsStrictSearchPatch.MARKER_FLAGS&&field.getInitialValue()==null,
                    "Unknown installed search success marker");
                fields.remove();markers++;
            }
        }
        MapsStrictSearchPatch.require(markers==1,"Missing installed search success marker");
        NativeContracts.validate(new MapsStrictSearchPatch.Lookup(){public ClassDef get(String type){
            if(type.equals(MapsStrictSearchPatch.OWNER))return request;
            if(type.equals(MapsStrictSearchPatch.MODEL))return model;
            return lookup.get(type);
        }});
    }

    private static void checkField(Instruction instruction,int a,int b,String field) {
        MapsStrictSearchPatch.require(instruction.getOpcode()==Opcode.IGET_OBJECT
            &&((TwoRegisterInstruction)instruction).getRegisterA()==a
            &&((TwoRegisterInstruction)instruction).getRegisterB()==b
            &&((ReferenceInstruction)instruction).getReference().toString().equals(field),"Unknown installed field hook");
    }
    private static void checkCall(Instruction instruction,int count,int c,int d,int e,String method) {
        MapsStrictSearchPatch.require(instruction.getOpcode()==Opcode.INVOKE_STATIC,"Unknown installed invocation opcode");
        FiveRegisterInstruction registers=(FiveRegisterInstruction)instruction;
        MapsStrictSearchPatch.require(registers.getRegisterCount()==count&&registers.getRegisterC()==c
            &&registers.getRegisterD()==d&&(count<3||registers.getRegisterE()==e)
            &&MapsStrictSearchPatch.descriptor((MethodReference)((ReferenceInstruction)instruction).getReference()).equals(method),
            "Unknown installed hook call");
    }

    public static void replaceRuntime(BytecodePatchContext context) {
        // The patcher's extension merger does not replace existing method bodies.
        // Explicit MutableClass replacements are needed for STRIP_FAST to remove
        // the old implementation from its original DEX and install the new one.
        for(ClassDef type:resource(MapsStrictSearchPatch.EXTENSION).values())
            context.getPatchClasses$morphe_patcher().addClass$morphe_patcher(new MutableClass(type));
    }
}
