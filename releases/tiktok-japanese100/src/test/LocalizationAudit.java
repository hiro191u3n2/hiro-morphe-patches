/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.*;import java.nio.file.*;import java.util.*;
import app.hiro.tiktokjapanese.patches.TikTokJapanesePatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Checks the actual published Metra DEX, not an imitation of its registry. */
public final class LocalizationAudit {
    static final String TARGET=TikTokJapanesePatch.TARGET;
    static final String HELPER="Lapp/hiro/tiktokjapanese/runtime/";
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Map<String,ClassDef> read(String path)throws Exception {
        Map<String,ClassDef> result=new TreeMap<>();
        MultiDexContainer<? extends DexFile> container=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());
        for(String entry:container.getDexEntryNames())for(ClassDef c:container.getEntry(entry).getDexFile().getClasses())
            check(result.put(c.getType(),c)==null,"duplicate class: "+c.getType());
        return result;
    }
    static byte[] canonical(Collection<? extends ClassDef> classes)throws Exception {
        MemoryDataStore store=new MemoryDataStore();
        try{DexPool.writeTo(store,new ImmutableDexFile(Opcodes.getDefault(),classes));return Arrays.copyOf(store.getBuffer(),store.getSize());}
        finally{store.close();}
    }
    static void write(String path,Map<String,ClassDef> classes)throws Exception {Files.write(Paths.get(path),canonical(classes.values()));}
    static void merge(String[] args)throws Exception {
        Map<String,ClassDef> base=read(args[1]),extra=read(args[2]),merged=new TreeMap<>(base);
        int added=0;
        for(ClassDef c:extra.values()) {
            ClassDef old=merged.get(c.getType());
            if(old!=null) {
                check(args[0].equals("merge-fixture"),"additive merge collided: "+c.getType());
                check(Arrays.equals(canonical(Collections.singleton(old)),canonical(Collections.singleton(c))),"published shared duplicate differs: "+c.getType());
            } else {merged.put(c.getType(),c);added++;}
        }
        write(args[3],merged);Map<String,ClassDef> reread=read(args[3]);
        Map<String,ClassDef> kept=new TreeMap<>(reread);kept.keySet().retainAll(base.keySet());
        check(Arrays.equals(canonical(base.values()),canonical(kept.values())),"old loader definitions changed");
        System.out.println("PASS merge: preserved="+base.size()+", added="+added);
    }
    static ClassDef restored(ClassDef after) {
        List<Method> methods=new ArrayList<>();int saved=0,wrapper=0;
        for(Method m:after.getMethods()) {
            if(m.getName().equals("of")){wrapper++;continue;}
            if(m.getName().equals(TikTokJapanesePatch.ORIGINAL)) {
                saved++;m=new ImmutableMethod(m.getDefiningClass(),"of",m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),m.getImplementation());
            }
            methods.add(m);
        }
        check(saved==1&&wrapper==1,"one saved original and one wrapper");
        return new ImmutableClassDef(after.getType(),after.getAccessFlags(),after.getSuperclass(),after.getInterfaces(),after.getSourceFile(),after.getAnnotations(),after.getFields(),methods);
    }
    static void audit(String[] args)throws Exception {
        Map<String,ClassDef> before=read(args[1]),after=read(args[2]);
        check(before.containsKey(TARGET)&&after.containsKey(TARGET),"actual registry present");
        ClassDef fixed=after.get(TARGET);
        check(Arrays.equals(canonical(Collections.singleton(before.get(TARGET))),canonical(Collections.singleton(restored(fixed)))),"original language method or registry changed");
        MutableClass already=new MutableClass(fixed);check(!TikTokJapanesePatch.apply(already),"wrapper must be recognized without duplicate injection");
        before.remove(TARGET);after.remove(TARGET);int helpers=0;
        for(Iterator<String> it=after.keySet().iterator();it.hasNext();)if(it.next().startsWith(HELPER)){it.remove();helpers++;}
        check(helpers==2,"exact dictionary/helper class inventory");
        check(before.keySet().equals(after.keySet()),"unrelated runtime classes added or removed");
        check(Arrays.equals(canonical(before.values()),canonical(after.values())),"unrelated runtime bytecode changed");
        System.out.println("PASS actual Metra DEX: preserved="+before.size()+"; original language dispatcher preserved; Japanese wrapper present; repeat apply safe");
    }
    static void selftest(String path)throws Exception {
        Map<String,ClassDef> classes=read(path);ClassDef original=classes.get(TARGET);check(original!=null,"published registry exists");
        MutableClass changed=new MutableClass(original);check(TikTokJapanesePatch.apply(changed),"first apply");
        check(!TikTokJapanesePatch.apply(changed),"idempotent apply");
        check(Arrays.equals(canonical(Collections.singleton(original)),canonical(Collections.singleton(restored(changed)))),"original registry retained exactly");
        for(String name:new String[]{"com.instagram.android","com.twitter.android","wrong"}) {
            boolean rejected=false;try{TikTokJapanesePatch.validatePackage(name);}catch(IllegalStateException expected){rejected=true;}
            check(rejected,"wrong app rejected");
        }
        MutableClass absent=new MutableClass(new ImmutableClassDef(TARGET,1,"Ljava/lang/Object;",Collections.emptyList(),null,Collections.emptySet(),Collections.emptyList(),Collections.emptyList()));
        boolean rejected=false;try{TikTokJapanesePatch.apply(absent);}catch(IllegalStateException expected){rejected=true;}
        check(rejected,"unsupported API rejected");
        System.out.println("PASS registry selftest: real upstream API; idempotence; exact original preservation; wrong-app and unsupported-API rejection; assertions="+assertions);
    }
    public static void main(String[] args)throws Exception {
        if(args.length==4&&(args[0].equals("merge")||args[0].equals("merge-fixture"))){merge(args);return;}
        if(args.length==3&&args[0].equals("audit")){audit(args);return;}
        if(args.length==2&&args[0].equals("selftest")){selftest(args[1]);return;}
        if(args.length==2&&args[0].equals("empty")) {
            Map<String,ClassDef> classes=new TreeMap<>();String type="Lfixture/Unrelated;";
            classes.put(type,new ImmutableClassDef(type,1,"Ljava/lang/Object;",Collections.emptyList(),null,Collections.emptySet(),Collections.emptyList(),Collections.emptyList()));
            write(args[1],classes);return;
        }
        throw new IllegalArgumentException("merge BASE NEW OUT | audit ORIGINAL PATCHED | selftest UPSTREAM | empty OUT");
    }
}
