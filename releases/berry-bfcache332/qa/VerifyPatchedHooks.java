import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Independently confirm real hook calls in application methods, not just loaded helper classes. */
public final class VerifyPatchedHooks {
  static boolean helper(String t){return t.startsWith("Lhiro/")||t.startsWith("LHiroBerryBrowserChooser")||t.startsWith("Lapp/hiro/");}
  static boolean platform(String t){return t.startsWith("Landroid/")||t.startsWith("Ljava/")||t.startsWith("Ljavax/")||t.startsWith("Ldalvik/");}
  static final Map<String,Integer> required=new LinkedHashMap<>();
  static void add(String owner,String name,int count){required.put(owner+"->"+name,count);}
  static {
    add("Lhiro/berryzoom/OneHandZoom;","touch",1);add("Lhiro/berryzoom/OneHandZoom;","blocksParent",1);
    for(String name:Arrays.asList("load","resumed","paused","stopped","destroyed","savePending","restorePending","started","scroll","touch"))add("Lhiro/berry/TabPriority;",name,1);
    add("Lhiro/berry/TabPriority;","finished",2);
    for(String name:Arrays.asList("defer","scroll","touch"))add("Lhiro/berry/PreviewScheduler;",name,1);
    add("LHiroBerryBrowserChooser;","attach",1);add("LHiroBerryBrowserChooser;","update",1);
    for(String name:Arrays.asList("started","committed","finished","listenerName","listener","script"))add("Lhiro/berrybfcache/RestoreCoordinator;",name,1);
    add("Lhiro/berrybfcache/NativeCompletion;","<init>",2);
  }
  static boolean methodExists(Map<String,ClassDef> all,MethodReference m,String owner,Set<String> seen){
    if(platform(owner))return true;if(!seen.add(owner))return false;ClassDef c=all.get(owner);if(c==null)return false;
    for(Method x:c.getMethods())if(x.getName().equals(m.getName())&&x.getReturnType().equals(m.getReturnType())&&x.getParameterTypes().equals(m.getParameterTypes()))return true;
    if(c.getSuperclass()!=null&&methodExists(all,m,c.getSuperclass(),seen))return true;
    for(String it:c.getInterfaces())if(methodExists(all,m,it,seen))return true;
    return false;
  }
  static boolean fieldExists(Map<String,ClassDef> all,FieldReference f,String owner,Set<String> seen){
    if(platform(owner))return true;if(!seen.add(owner))return false;ClassDef c=all.get(owner);if(c==null)return false;
    for(Field x:c.getFields())if(x.getName().equals(f.getName())&&x.getType().equals(f.getType()))return true;
    return c.getSuperclass()!=null&&fieldExists(all,f,c.getSuperclass(),seen);
  }
  static void register(int r,int count,Method m){
    if(r<0||r>=count)throw new IllegalStateException("Out-of-range register v"+r+" of "+count+" in "+m);
  }
  static void checkEncoding(Method m){
    int count=m.getImplementation().getRegisterCount(),offset=0;
    Map<Integer,Instruction> offsets=new TreeMap<>();
    for(Instruction i:m.getImplementation().getInstructions()){
      offsets.put(offset,i);offset+=i.getCodeUnits();
      if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction r=(RegisterRangeInstruction)i;if(r.getRegisterCount()>0){register(r.getStartRegister(),count,m);register(r.getStartRegister()+r.getRegisterCount()-1,count,m);}}
      else if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;int[] rs={r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()};if(r.getRegisterCount()>5)throw new IllegalStateException("Invalid invoke count");for(int x=0;x<r.getRegisterCount();x++)register(rs[x],count,m);}
      else if(i instanceof ThreeRegisterInstruction){ThreeRegisterInstruction r=(ThreeRegisterInstruction)i;register(r.getRegisterA(),count,m);register(r.getRegisterB(),count,m);register(r.getRegisterC(),count,m);}
      else if(i instanceof TwoRegisterInstruction){TwoRegisterInstruction r=(TwoRegisterInstruction)i;register(r.getRegisterA(),count,m);register(r.getRegisterB(),count,m);}
      else if(i instanceof OneRegisterInstruction)register(((OneRegisterInstruction)i).getRegisterA(),count,m);
      if(i.getOpcode().name.startsWith("invoke-")&&i instanceof ReferenceInstruction){Reference ref=((ReferenceInstruction)i).getReference();if(ref instanceof MethodReference){MethodReference r=(MethodReference)ref;int words=i.getOpcode().name.startsWith("invoke-static")?0:1;for(CharSequence p:r.getParameterTypes())words+=(p.toString().equals("J")||p.toString().equals("D"))?2:1;int actual=i instanceof RegisterRangeInstruction?((RegisterRangeInstruction)i).getRegisterCount():((FiveRegisterInstruction)i).getRegisterCount();if(words!=actual)throw new IllegalStateException("Invoke word count differs in "+m+": "+r);}}
    }
    for(Map.Entry<Integer,Instruction> e:offsets.entrySet())if(e.getValue() instanceof OffsetInstruction){int target=e.getKey()+((OffsetInstruction)e.getValue()).getCodeOffset();if(!offsets.containsKey(target))throw new IllegalStateException("Invalid branch target in "+m);}
  }
  public static void main(String[] a)throws Exception {
    Map<String,ClassDef> all=new TreeMap<>();byte[] manifest;
    try(ZipFile z=new ZipFile(a[0])){
      manifest=z.getInputStream(z.getEntry("AndroidManifest.xml")).readAllBytes();
      for(Enumeration<? extends ZipEntry> es=z.entries();es.hasMoreElements();){ZipEntry e=es.nextElement();if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
        DexBackedDexFile d=DexBackedDexFile.fromInputStream(Opcodes.forApi(26),new BufferedInputStream(z.getInputStream(e)));
        for(ClassDef c:d.getClasses())if(all.put(c.getType(),c)!=null)throw new IllegalStateException("Duplicate class");
      }
    }
    Map<String,Integer> found=new TreeMap<>();List<Method> hooked=new ArrayList<>();
    for(ClassDef c:all.values())if(!helper(c.getType()))for(Method m:c.getMethods())if(m.getImplementation()!=null){
      boolean isHooked=false;
      for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction){Reference ref=((ReferenceInstruction)i).getReference();if(ref instanceof MethodReference){MethodReference mr=(MethodReference)ref;String key=mr.getDefiningClass()+"->"+mr.getName();if(required.containsKey(key)){found.merge(key,1,Integer::sum);isHooked=true;}}}
      if(isHooked)hooked.add(m);
    }
    for(Map.Entry<String,Integer> e:required.entrySet())if(!e.getValue().equals(found.get(e.getKey())))throw new IllegalStateException("Hook count differs: "+e.getKey()+" expected="+e.getValue()+" actual="+found.get(e.getKey()));
    int refs=0;
    for(Method m:hooked)checkEncoding(m);
    for(Method m:hooked)for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction){Reference r=((ReferenceInstruction)i).getReference();
      if(r instanceof MethodReference){MethodReference mr=(MethodReference)r;if(!methodExists(all,mr,mr.getDefiningClass(),new HashSet<>()))throw new IllegalStateException("Unresolved method from hook: "+mr);refs++;}
      else if(r instanceof FieldReference){FieldReference fr=(FieldReference)r;if(!fieldExists(all,fr,fr.getDefiningClass(),new HashSet<>()))throw new IllegalStateException("Unresolved field from hook: "+fr);refs++;}
    }
    if(!all.containsKey("Lapp/hiro/berry/runtime/BerryQueueInit;"))throw new IllegalStateException("Queue provider missing");
    String ascii=new String(manifest,java.nio.charset.StandardCharsets.ISO_8859_1).replace("\0","");
    if(!ascii.contains("app.hiro.berry.runtime.BerryQueueInit")||!ascii.contains("jp.ejimax.berrybrowser.hiro.messagequeue"))throw new IllegalStateException("Queue manifest provider absent");
    System.out.println("{\"result\":\"PASS_ACTUAL_HOOKS\",\"expected_hook_kinds\":"+required.size()+",\"application_methods_with_hooks\":"+hooked.size()+",\"resolved_method_and_field_references\":"+refs+",\"register_bounds_invoke_words_and_branches_valid\":true,\"queue_provider_and_manifest_present\":true}");
  }
}
