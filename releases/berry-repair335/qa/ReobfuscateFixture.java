import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.rewriter.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;

/** Private synthetic patching fixture. No claim that this unsigned renamed APK runs on Android. */
public final class ReobfuscateFixture {
  static final Map<String,ClassDef> classes=new TreeMap<>();
  static final Map<String,String> types=new TreeMap<>(), methods=new TreeMap<>(), fields=new TreeMap<>();
  static final Set<String> fieldOwners=new HashSet<>(Arrays.asList("Lc41;","Lg60;","Ltt5;","Lz31;","Lb03;","Lhc1;","Lc70;","Lyp6;"));
  static final Map<String,Set<String>> methodNames=new HashMap<>();
  static {
    methodNames.put("Lc41;",new HashSet<>(Arrays.asList("S","V","t0","m","r","h","w0","v")));
    methodNames.put("Lb03;",new HashSet<>(Arrays.asList("X")));
    methodNames.put("Lfy;",new HashSet<>(Arrays.asList("O")));
    methodNames.put("Lj8;",new HashSet<>(Arrays.asList("q")));
    methodNames.put("Ltp0;",new HashSet<>(Arrays.asList("c")));
    methodNames.put("Lmo0;",new HashSet<>(Arrays.asList("a","b")));
    methodNames.put("Lip6;",new HashSet<>(Arrays.asList("b")));
    methodNames.put("Lso6;",new HashSet<>(Arrays.asList("d")));
    methodNames.put("Ltt5;",new HashSet<>(Arrays.asList("T0")));
    methodNames.put("Lsm2;",new HashSet<>(Arrays.asList("v")));
  }
  static String methodKey(MethodReference m){return m.getDefiningClass()+"->"+m.getName()+"("+join(m.getParameterTypes())+")"+m.getReturnType();}
  static String fieldKey(FieldReference f){return f.getDefiningClass()+"->"+f.getName()+":"+f.getType();}
  static String join(List<? extends CharSequence> values){StringBuilder s=new StringBuilder();for(CharSequence v:values)s.append(v);return s.toString();}
  static String type(String t){int n=0;while(t.charAt(n)=='[')n++;String base=t.substring(n);return t.substring(0,n)+types.getOrDefault(base,base);}
  static List<String> params(List<? extends CharSequence> ps){List<String> r=new ArrayList<>();for(CharSequence p:ps)r.add(type(p.toString()));return r;}
  static String methodName(String owner,String name,List<? extends CharSequence> ps,String ret,Set<String> seen){
    String key=owner+"->"+name+"("+join(ps)+")"+ret;
    if(methods.containsKey(key))return methods.get(key);
    if(!seen.add(owner))return name;
    ClassDef c=classes.get(owner);if(c==null)return name;
    List<String> parents=new ArrayList<>(c.getInterfaces());if(c.getSuperclass()!=null)parents.add(c.getSuperclass());
    for(String p:parents){String v=methodName(p,name,ps,ret,seen);if(!v.equals(name))return v;}
    return name;
  }
  static String methodName(MethodReference m){return methodName(m.getDefiningClass(),m.getName(),m.getParameterTypes(),m.getReturnType(),new HashSet<>());}
  static String fieldName(String owner,String name,String t,Set<String> seen){
    String key=owner+"->"+name+":"+t;if(fields.containsKey(key))return fields.get(key);
    if(!seen.add(owner))return name;ClassDef c=classes.get(owner);if(c==null||c.getSuperclass()==null)return name;
    return fieldName(c.getSuperclass(),name,t,seen);
  }
  static String fieldName(FieldReference f){return fieldName(f.getDefiningClass(),f.getName(),f.getType(),new HashSet<>());}
  public static void main(String[] a)throws Exception{
    Path input=Path.of(a[0]),output=Path.of(a[1]),report=Path.of(a[2]);
    Map<String,byte[]> original=new LinkedHashMap<>();
    Map<String,DexBackedDexFile> dexes=new TreeMap<>();
    try(ZipFile z=new ZipFile(input.toFile())){
      for(Enumeration<? extends ZipEntry> e=z.entries();e.hasMoreElements();){ZipEntry n=e.nextElement();if(n.isDirectory())continue;
        byte[] b=z.getInputStream(n).readAllBytes();original.put(n.getName(),b);
        if(n.getName().matches("classes[0-9]*\\.dex")){
          DexBackedDexFile d=DexBackedDexFile.fromInputStream(Opcodes.forApi(26),new BufferedInputStream(new ByteArrayInputStream(b)));dexes.put(n.getName(),d);
          for(ClassDef c:d.getClasses()){if(classes.put(c.getType(),c)!=null)throw new IllegalStateException("Duplicate class");}
        }
      }
    }
    byte[] xml=original.get("AndroidManifest.xml"),oldVersion="3.83.74".getBytes(StandardCharsets.UTF_16LE),newVersion="9.99.99".getBytes(StandardCharsets.UTF_16LE);
    int changedVersion=0;for(int at=0;at<=xml.length-oldVersion.length;at++)if(Arrays.equals(Arrays.copyOfRange(xml,at,at+oldVersion.length),oldVersion)){System.arraycopy(newVersion,0,xml,at,newVersion.length);changedVersion++;}
    if(changedVersion!=1)throw new IllegalStateException("Expected one original versionName");
    int i=0;for(ClassDef c:classes.values())if(c.getType().matches("L[^/;]+;"))types.put(c.getType(),String.format("Lqa/renamed/Type%05d;",i++));
    i=0;for(ClassDef c:classes.values()){
      for(Field f:c.getFields())if(fieldOwners.contains(c.getType()))fields.put(fieldKey(f),String.format("renamedField%04d",i++));
      for(Method m:c.getMethods())if(methodNames.getOrDefault(c.getType(),Collections.emptySet()).contains(m.getName()))methods.put(methodKey(m),String.format("renamedMethod%04d",i++));
    }
    if(!types.containsKey("Lcx;")||!types.containsKey("Lc41;")||methods.size()<10||fields.size()<10)throw new IllegalStateException("Incomplete renaming inputs");
    DexRewriter rw=new DexRewriter(new RewriterModule(){
      @Override public Rewriter<String> getTypeRewriter(Rewriters r){return ReobfuscateFixture::type;}
      @Override public Rewriter<MethodReference> getMethodReferenceRewriter(Rewriters r){return m->new ImmutableMethodReference(type(m.getDefiningClass()),methodName(m),params(m.getParameterTypes()),type(m.getReturnType()));}
      @Override public Rewriter<FieldReference> getFieldReferenceRewriter(Rewriters r){return f->new ImmutableFieldReference(type(f.getDefiningClass()),fieldName(f),type(f.getType()));}
      @Override public Rewriter<Method> getMethodRewriter(Rewriters r){
        MethodRewriter normal=new MethodRewriter(r);return m->{Method n=normal.rewrite(m);return new ImmutableMethod(n.getDefiningClass(),methodName(m),n.getParameters(),n.getReturnType(),n.getAccessFlags(),n.getAnnotations(),n.getHiddenApiRestrictions(),n.getImplementation());};
      }
      @Override public Rewriter<Field> getFieldRewriter(Rewriters r){
        FieldRewriter normal=new FieldRewriter(r);return f->{Field n=normal.rewrite(f);return new ImmutableField(n.getDefiningClass(),fieldName(f),n.getType(),n.getAccessFlags(),n.getInitialValue(),n.getAnnotations(),n.getHiddenApiRestrictions());};
      }
    });
    Path tmp=Files.createTempDirectory(output.toAbsolutePath().getParent(),"rename-");
    for(Map.Entry<String,DexBackedDexFile> e:dexes.entrySet()){
      Path target=tmp.resolve(e.getKey());DexPool.writeTo(target.toString(),rw.getDexFileRewriter().rewrite(e.getValue()));original.put(e.getKey(),Files.readAllBytes(target));
    }
    try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(output))){
      for(Map.Entry<String,byte[]> e:original.entrySet()){
        if(e.getKey().startsWith("META-INF/")&&(e.getKey().endsWith(".RSA")||e.getKey().endsWith(".SF")||e.getKey().endsWith(".MF")))continue;
        ZipEntry ze=new ZipEntry(e.getKey());ze.setTime(0);z.putNextEntry(ze);z.write(e.getValue());z.closeEntry();
      }
    }
    StringBuilder j=new StringBuilder("{\n  \"synthetic_only\": true,\n  \"synthetic_version_name\": \"9.99.99\",\n  \"device_runtime_tested\": false,\n  \"renamed_classes\": "+types.size()+",\n  \"renamed_fields\": "+fields.size()+",\n  \"renamed_methods\": "+methods.size()+",\n  \"types\": {");
    boolean first=true;for(Map.Entry<String,String> e:types.entrySet()){if(!first)j.append(',');first=false;j.append('\n').append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append('"');}
    j.append("\n  },\n  \"members\": {");first=true;Map<String,String> members=new TreeMap<>(methods);members.putAll(fields);for(Map.Entry<String,String> e:members.entrySet()){if(!first)j.append(',');first=false;j.append('\n').append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append('"');}j.append("\n  }\n}\n");Files.writeString(report,j);
    for(Path p:Files.newDirectoryStream(tmp))Files.delete(p);Files.delete(tmp);
    System.out.println("PASS renamed "+types.size()+" classes, "+fields.size()+" fields, "+methods.size()+" methods");
  }
}
