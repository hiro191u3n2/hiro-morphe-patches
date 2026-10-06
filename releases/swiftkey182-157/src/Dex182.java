import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.smali.Smali;
import com.android.tools.smali.smali.SmaliOptions;

/** DEX assembly/merge through the unmodified Morphe toolchain. */
public final class Dex182 {
    static final String CURSOR="Lhiro/swiftkey/CursorLatinBoundary;";
    static final String MATCHER="Lhiro/swiftkey/UnicodeLatinToken;";
    static void require(boolean yes,String message) {if(!yes)throw new IllegalStateException(message);}
    static DexFile load(String path)throws Exception {return DexFileFactory.loadDexFile(new File(path),Opcodes.forApi(26));}
    static TreeMap<String,ClassDef> classes(DexFile dex){
        TreeMap<String,ClassDef> result=new TreeMap<>();
        for(ClassDef c:dex.getClasses())require(result.put(c.getType(),c)==null,"Duplicate "+c.getType());
        return result;
    }
    static void write(Map<String,ClassDef> classes,String path)throws Exception {
        DexPool pool=new DexPool(Opcodes.forApi(26));
        for(ClassDef c:classes.values())pool.internClass(c);
        FileDataStore out=new FileDataStore(new File(path));
        try{pool.writeTo(out);}finally{out.close();}
    }
    static void internalReference(String type,Set<String> all,Set<String> selected){
        require(!all.contains(type)||selected.contains(type),"Omitted internal patcher dependency: "+type);
    }
    public static void main(String[] args)throws Exception {
        if(args[0].equals("merge")){
            TreeMap<String,ClassDef> original=classes(load(args[1]));
            TreeMap<String,ClassDef> cursor=classes(load(args[2]));
            TreeMap<String,ClassDef> helper=classes(load(args[3]));
            require(original.size()==36 && original.containsKey(CURSOR),"SwiftKey extension baseline differs");
            require(!original.containsKey(MATCHER),"Already patched");
            require(cursor.size()==1 && cursor.containsKey(CURSOR),"Unexpected replacement classes");
            require(helper.size()==1 && helper.containsKey(MATCHER),"Unexpected production helper classes");
            original.put(CURSOR,cursor.get(CURSOR));original.put(MATCHER,helper.get(MATCHER));
            write(original,args[4]);
            require(classes(load(args[4])).size()==37,"Unexpected final extension class count");
            System.out.println("PASS extension: 36 original classes, one replacement, one new production class");
        }else if(args[0].equals("assemble")){
            SmaliOptions options=new SmaliOptions();options.apiLevel=26;options.jobs=1;options.verboseErrors=true;
            options.outputDexFile=args[2];
            require(Smali.assemble(options,args[1]),"Smali assembly failed");
        }else if(args[0].equals("single")){
            TreeMap<String,ClassDef> all=classes(load(args[1]));
            TreeMap<String,ClassDef> selected=new TreeMap<>();
            for(ClassDef c:all.values())if(c.getType().startsWith("Lapp/hiro/swiftkey/"))selected.put(c.getType(),c);
            require(selected.size()==15,"Expected 15 SwiftKey patcher classes");
            for(ClassDef c:selected.values()){
                internalReference(c.getSuperclass(),all.keySet(),selected.keySet());
                for(String t:c.getInterfaces())internalReference(t,all.keySet(),selected.keySet());
                for(Field f:c.getFields())internalReference(f.getType(),all.keySet(),selected.keySet());
                for(Method m:c.getMethods()){
                    internalReference(m.getReturnType(),all.keySet(),selected.keySet());
                    for(CharSequence t:m.getParameterTypes())internalReference(t.toString(),all.keySet(),selected.keySet());
                    if(m.getImplementation()==null)continue;
                    for(Instruction ins:m.getImplementation().getInstructions())if(ins instanceof ReferenceInstruction){
                        Reference ref=((ReferenceInstruction)ins).getReference();
                        if(ref instanceof TypeReference)internalReference(((TypeReference)ref).getType(),all.keySet(),selected.keySet());
                        if(ref instanceof MethodReference)internalReference(((MethodReference)ref).getDefiningClass(),all.keySet(),selected.keySet());
                        if(ref instanceof FieldReference)internalReference(((FieldReference)ref).getDefiningClass(),all.keySet(),selected.keySet());
                    }
                }
            }
            write(selected,args[2]);
            require(classes(load(args[2])).size()==15,"Single patcher count mismatch");
            System.out.println("PASS single: all 15 current SwiftKey patcher classes; no omitted internal dependencies");
        }else throw new IllegalArgumentException("Unknown operation "+args[0]);
    }
}
