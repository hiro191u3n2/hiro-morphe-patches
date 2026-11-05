import java.io.*;
import java.util.*;
import java.util.zip.*;
import app.hiro.amazonreviews.patches.AmazonReviewPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

public final class AmazonReviewAudit {
    static final String LOADER="Lapp/hiro/amazonreviews/patches/";
    static final String RUNTIME="Lapp/hiro/amazonreviews/";
    static long checks;
    static void check(boolean ok,String why) {checks++;if(!ok)throw new AssertionError(why);}
    static Map<String,ClassDef> read(String path) throws Exception {
        Map<String,ClassDef> out=new TreeMap<>();
        MultiDexContainer<? extends DexFile> dex=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());
        for(String name:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(name).getDexFile().getClasses())
            check(out.put(c.getType(),c)==null,"Duplicate class "+c.getType());
        return out;
    }
    static byte[] canonical(ClassDef c) throws Exception {
        MemoryDataStore store=new MemoryDataStore();
        try {DexPool.writeTo(store,new ImmutableDexFile(Opcodes.getDefault(),Collections.singleton(c)));
            return Arrays.copyOf(store.getBuffer(),store.getSize());}finally{store.close();}
    }
    /** Byte-identical DEX entries prove every enclosed class unchanged without reserializing it. */
    static Set<String> bytePreservedClasses(String original,String output)throws Exception{
        Set<String> out=new HashSet<>();
        MultiDexContainer<? extends DexFile> dex=DexFileFactory.loadDexContainer(new File(original),Opcodes.getDefault());
        try(ZipFile a=new ZipFile(original);ZipFile b=new ZipFile(output)){
            byte[] x=new byte[65536],y=new byte[65536];
            for(String name:dex.getDexEntryNames()){
                ZipEntry aa=a.getEntry(name),bb=b.getEntry(name);
                if(aa==null||bb==null||aa.getSize()!=bb.getSize())continue;
                boolean same=true;
                try(InputStream ai=a.getInputStream(aa);InputStream bi=b.getInputStream(bb)){
                    int ac,bc;
                    while((ac=readChunk(ai,x))>0){bc=readChunk(bi,y);if(ac!=bc||!Arrays.equals(Arrays.copyOf(x,ac),Arrays.copyOf(y,bc))){same=false;break;}}
                    if(same&&bi.read()!=-1)same=false;
                }
                if(same)for(ClassDef c:dex.getEntry(name).getDexFile().getClasses())out.add(c.getType());
            }
        }
        return out;
    }
    static int readChunk(InputStream in,byte[] bytes)throws IOException{
        int total=0,n;while(total<bytes.length&&(n=in.read(bytes,total,bytes.length-total))!=-1)total+=n;return total;
    }
    static Method copy(Method m,MethodImplementation body) {
        return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body);
    }
    static String sig(Method m){return m.getName()+m.getParameterTypes()+m.getReturnType();}
    static int target(Method m){for(int n=0;n<AmazonReviewPatch.TARGETS.length;n++)if(AmazonReviewPatch.matches(m,n))return n;return -1;}
    static void rejected(Runnable r,String why){boolean fail=false;try{r.run();}catch(IllegalStateException e){fail=true;}check(fail,why);}
    static void guards(String path) throws Exception {
        AmazonReviewPatch.validatePackage(AmazonReviewPatch.PACKAGE,AmazonReviewPatch.VERSION,AmazonReviewPatch.CODE);
        rejected(()->AmazonReviewPatch.validatePackage("other",AmazonReviewPatch.VERSION,AmazonReviewPatch.CODE),"Wrong package accepted");
        rejected(()->AmazonReviewPatch.validatePackage(AmazonReviewPatch.PACKAGE,"32.20",AmazonReviewPatch.CODE),"Wrong version accepted");
        rejected(()->AmazonReviewPatch.validatePackage(AmazonReviewPatch.PACKAGE,AmazonReviewPatch.VERSION,"0"),"Wrong code accepted");
        if(path!=null){
            Map<String,ClassDef> all=read(path);
            for(int n=0;n<AmazonReviewPatch.TARGETS.length;n++){
                final int id=n;
                Method original=AmazonReviewPatch.find(all.get(AmazonReviewPatch.TARGETS[n][0]),n);
                AmazonReviewPatch.validate(original,n);
                MutableMethod changed=new MutableMethod(original);
                changed.getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));
                rejected(()->AmazonReviewPatch.validate(changed,id),"Changed body accepted");
                MutableMethod patched=new MutableMethod(original);AmazonReviewPatch.apply(patched,n);
                rejected(()->AmazonReviewPatch.apply(patched,id),"Duplicate application accepted");
            }
        }
        System.out.println("PASS package/version/body guards and duplicate application rejection; assertions="+checks);
    }
    static void fingerprint(String path)throws Exception{
        Map<String,ClassDef> all=read(path);
        for(int n=0;n<AmazonReviewPatch.TARGETS.length;n++){
            Method m=AmazonReviewPatch.find(all.get(AmazonReviewPatch.TARGETS[n][0]),n);
            System.out.println(n+" "+AmazonReviewPatch.fingerprint(m)+" "+AmazonReviewPatch.fingerprint(new MutableMethod(m)));
        }
    }
    static void merge(String basePath,String addPath,String output)throws Exception{
        Map<String,ClassDef> base=read(basePath),add=read(addPath),all=new TreeMap<>();
        int replaced=0;
        for(ClassDef c:base.values())if(c.getType().startsWith(LOADER))replaced++;else all.put(c.getType(),c);
        check(replaced>0,"Previous review loader missing");
        check(!add.isEmpty(),"Empty addition");
        for(ClassDef c:add.values()){
            check(c.getType().startsWith(LOADER),"Addition outside namespace");
            check(all.put(c.getType(),c)==null,"Existing loader collision");
        }
        DexPool.writeTo(output,new ImmutableDexFile(Opcodes.getDefault(),all.values()));
        Map<String,ClassDef> after=read(output);check(all.keySet().equals(after.keySet()),"Loader inventory differs");
        for(ClassDef c:all.values())check(Arrays.equals(canonical(c),canonical(after.get(c.getType()))),"Loader class changed "+c.getType());
        System.out.println("PASS unrelated loader classes preserved="+(base.size()-replaced)+"; replaced="+replaced+"; new review classes="+add.size()+"; assertions="+checks);
    }
    static void audit(String originalPath,String outputPath,String runtimePath)throws Exception{
        Map<String,ClassDef> before=read(originalPath),after=read(outputPath),runtime=read(runtimePath);
        Set<String> bytePreserved=bytePreservedClasses(originalPath,outputPath);
        Set<String> expected=new TreeSet<>(before.keySet());expected.addAll(runtime.keySet());
        check(expected.equals(after.keySet()),"APK class inventory differs");
        for(ClassDef c:runtime.values()){
            check(c.getType().startsWith(RUNTIME)&&!c.getType().startsWith(LOADER),"Unexpected runtime class");
            check(Arrays.equals(canonical(c),canonical(after.get(c.getType()))),"Runtime differs "+c.getType());
        }
        int untouched=0,changed=0;
        for(ClassDef c:before.values()){
            if(bytePreserved.contains(c.getType())){untouched++;continue;}
            List<Method> methods=new ArrayList<>();boolean own=false;
            for(Method m:c.getMethods())if(target(m)>=0)own=true;
            if(!own){check(Arrays.equals(canonical(c),canonical(after.get(c.getType()))),"Unrelated class changed "+c.getType());untouched++;continue;}
            for(Method m:c.getMethods()){
                MutableMethod mm=new MutableMethod(m);int n=target(m);
                if(n>=0){AmazonReviewPatch.apply(mm,n);changed++;}
                methods.add(mm);
            }
            ClassDef wanted=new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);
            check(Arrays.equals(canonical(wanted),canonical(after.get(c.getType()))),"Target differs beyond approved hooks "+c.getType());
        }
        check(changed==6,"Wrong hook count");
        System.out.println("PASS APK exact class audit: unchanged="+untouched+" (byte-identical DEX classes="+bytePreserved.size()+"); methods hooked=6; inserted instructions=8; runtime classes="+runtime.size()+"; assertions="+checks+"; physical device not tested");
    }
    public static void main(String[] args)throws Exception{
        if(args[0].equals("fingerprint"))fingerprint(args[1]);
        else if(args[0].equals("guards"))guards(args.length>1?args[1]:null);
        else if(args[0].equals("merge"))merge(args[1],args[2],args[3]);
        else if(args[0].equals("audit"))audit(args[1],args[2],args[3]);
        else throw new IllegalArgumentException();
    }
}
