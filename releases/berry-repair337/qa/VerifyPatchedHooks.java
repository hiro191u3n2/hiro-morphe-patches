import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;

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
    add("Lhiro/berrybfcache/RestoreCoordinator;","shouldSkipLoading",2);
    add("Lhiro/berrybfcache/NativeRestoreTracker;","install",1);
  }
  static final String WEB="Landroid/webkit/WebView;", STR="Ljava/lang/String;", COORD="Lhiro/berrybfcache/RestoreCoordinator;";
  static final String TAIL=COORD+"->startedBodyFinished("+WEB+STR+")V", GUARD=COORD+"->shouldSkipLoading("+WEB+")Z";
  static final DexFormatter F=DexFormatter.INSTANCE;
  static final String POST_SCROLL="Lhiro/berrybfcache/PostScrollDiagnostics;";
  static final String SCRIPT_SHA="38345ede2cc0a0691e43654f225839c456b96801995c4e39ee667a71cbadfd76";
  static void need(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
  static Map<String,ClassDef> load(Path apk)throws IOException {
    Map<String,ClassDef> all=new TreeMap<>();
    try(ZipFile z=new ZipFile(apk.toFile())){for(Enumeration<? extends ZipEntry> es=z.entries();es.hasMoreElements();){
      ZipEntry e=es.nextElement();if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
      DexBackedDexFile d=DexBackedDexFile.fromInputStream(Opcodes.forApi(26),new BufferedInputStream(z.getInputStream(e)));
      for(ClassDef c:d.getClasses())need(all.put(c.getType(),c)==null,"Duplicate class");
    }}return all;
  }
  static List<Instruction> instructions(Method m){List<Instruction> r=new ArrayList<>();for(Instruction i:m.getImplementation().getInstructions())r.add(i);return r;}
  static String ref(Instruction i){return i instanceof ReferenceInstruction?F.getReference(((ReferenceInstruction)i).getReference()):"";}
  static int[] args(Instruction i){
    if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction x=(FiveRegisterInstruction)i;return Arrays.copyOf(new int[]{x.getRegisterC(),x.getRegisterD(),x.getRegisterE(),x.getRegisterF(),x.getRegisterG()},x.getRegisterCount());}
    if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction x=(RegisterRangeInstruction)i;int[]r=new int[x.getRegisterCount()];for(int j=0;j<r.length;j++)r[j]=x.getStartRegister()+j;return r;}return new int[0];
  }
  static Method uniqueMethod(Map<String,ClassDef> all,String owner,String name){
    Method found=null;for(ClassDef c:all.values())if(owner==null? !helper(c.getType()):owner.equals(c.getType()))for(Method m:c.getMethods())if(m.getName().equals(name)){need(found==null,"Duplicate method "+name);found=m;}
    need(found!=null,"Missing method "+name);return found;
  }
  static int uniqueRef(Method m,String descriptor){int found=-1;List<Instruction> in=instructions(m);for(int j=0;j<in.size();j++)if(ref(in.get(j)).equals(descriptor)){need(found==-1,"Duplicate reference "+descriptor);found=j;}need(found>=0,"Missing reference "+descriptor);return found;}
  static int target(List<Instruction> in,int index){int here=0;for(int j=0;j<index;j++)here+=in.get(j).getCodeUnits();int dest=here+((OffsetInstruction)in.get(index)).getCodeOffset(),offset=0;for(int j=0;j<in.size();j++){if(offset==dest)return j;offset+=in.get(j).getCodeUnits();}throw new IllegalStateException("Invalid branch");}
  static boolean constant(Instruction i,int reg,int value){return i instanceof NarrowLiteralInstruction&&i.getOpcode().name().startsWith("CONST_")&&((OneRegisterInstruction)i).getRegisterA()==reg&&((NarrowLiteralInstruction)i).getNarrowLiteral()==value;}
  static void sig(Method m,String ret,int registers,String...params){need(m.getReturnType().equals(ret)&&m.getParameterTypes().equals(Arrays.asList(params))&&m.getImplementation().getRegisterCount()==registers,"Method signature/register mismatch: "+m);}
  static boolean reg1(Instruction i,int a){return i instanceof OneRegisterInstruction&&((OneRegisterInstruction)i).getRegisterA()==a;}
  static boolean reg2(Instruction i,int a,int b){return reg1(i,a)&&i instanceof TwoRegisterInstruction&&((TwoRegisterInstruction)i).getRegisterB()==b;}
  static void invoke(Instruction i,String descriptor,int...registers){need(ref(i).equals(descriptor)&&Arrays.equals(args(i),registers),"Unexpected invoke/arguments "+ref(i));}
  /** Compare actual native instructions, operands and branch destinations with the previous release.
   * The only normalized changes are two guarded call targets and completion calls inserted at returns.
   */
  static String canonical(Method m,boolean removeTail,Map<String,String> rewrite){
    List<Instruction> in=instructions(m);boolean[]omit=new boolean[in.size()];int tails=0,returns=0;
    for(int j=0;j<in.size();j++){
      if(ref(in.get(j)).equals(TAIL)){need(removeTail,"Unexpected baseline tail hook");omit[j]=true;tails++;
        need(j+1<in.size()&&in.get(j+1).getOpcode()==Opcode.RETURN_VOID,"Tail is not followed by return");
        invoke(in.get(j),TAIL,m.getImplementation().getRegisterCount()-3,m.getImplementation().getRegisterCount()-2);
      }
      if(in.get(j).getOpcode()==Opcode.RETURN_VOID){returns++;if(removeTail)need(j>0&&omit[j-1],"Return bypasses completion hook");}
      if(removeTail&&in.get(j)instanceof OffsetInstruction){int t=target(in,j);need(in.get(t).getOpcode()!=Opcode.RETURN_VOID,"Branch bypasses tail hook");}
    }
    if(removeTail)need(tails>0&&tails==returns,"Tail/return count differs");
    int[]retained=new int[in.size()+1];int n=0;for(int j=0;j<in.size();j++){retained[j]=n;if(!omit[j])n++;}retained[in.size()]=n;
    StringBuilder b=new StringBuilder(F.getMethodDescriptor(m)).append('/').append(m.getAccessFlags()).append('/').append(m.getImplementation().getRegisterCount()).append('\n');
    for(int j=0;j<in.size();j++){
      Instruction i=in.get(j);if(omit[j])continue;String op=i.getOpcode().name();if(op.startsWith("GOTO"))op="GOTO";if(op.equals("CONST_STRING_JUMBO"))op="CONST_STRING";b.append(op);
      if(i instanceof OneRegisterInstruction)b.append(" A=").append(((OneRegisterInstruction)i).getRegisterA());
      if(i instanceof TwoRegisterInstruction)b.append(" B=").append(((TwoRegisterInstruction)i).getRegisterB());
      if(i instanceof ThreeRegisterInstruction)b.append(" C=").append(((ThreeRegisterInstruction)i).getRegisterC());
      if(i instanceof VariableRegisterInstruction)b.append(" args=").append(Arrays.toString(args(i)));
      if(i instanceof WideLiteralInstruction)b.append(" lit=").append(((WideLiteralInstruction)i).getWideLiteral());
      if(i instanceof ReferenceInstruction)b.append(" ref=").append(rewrite.getOrDefault(ref(i),ref(i)));
      if(i instanceof OffsetInstruction)b.append(" target=").append(retained[target(in,j)]);
      need(!(i instanceof PayloadInstruction)&&!(i instanceof DualReferenceInstruction),"Unsupported comparison instruction");b.append('\n');
    }
    need(m.getImplementation().getTryBlocks().isEmpty(),"Unexpected onPageStarted try blocks");return b.toString();
  }
  static void auditNative(Map<String,ClassDef> all,Path baseline)throws IOException {
    Method complete=uniqueMethod(all,null,"hiroCompleteBfcache"),tabStart=uniqueMethod(all,null,"hiroTabStartedBfcache"),loading=uniqueMethod(all,null,"hiroSetLoadingBfcache"),install=uniqueMethod(all,null,"hiroInstallNativeBfcache");
    String client=complete.getDefiningClass();Method started=uniqueMethod(all,client,"onPageStarted"),finished=uniqueMethod(all,client,"onPageFinished");
    sig(complete,"Z",7,WEB,STR);sig(tabStart,"V",2,WEB);sig(loading,"V",4,"I");sig(install,"V",8);
    need((tabStart.getAccessFlags()&8)!=0,"TabPriority guard must be static");
    Method contract=uniqueMethod(all,"Lhiro/berrybfcache/CompletionTarget;","hiroCompleteBfcache");need(contract.getReturnType().equals("Z"),"Completion target must return boolean");
    List<Instruction> bridge=instructions(tabStart);need(bridge.size()==5,"Unexpected TabPriority guard body");invoke(bridge.get(0),GUARD,1);
    need(bridge.get(1).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)bridge.get(1)).getRegisterA()==0&&bridge.get(2).getOpcode()==Opcode.IF_NEZ&&reg1(bridge.get(2),0)&&target(bridge,2)==4,"Invalid TabPriority guard flow");
    invoke(bridge.get(3),"Lhiro/berry/TabPriority;->started("+WEB+")V",1);
    List<Instruction> li=instructions(loading);need(li.size()==12,"Unexpected loading guard body");
    need(constant(li.get(0),1,3)&&li.get(1).getOpcode()==Opcode.IF_NE&&target(li,1)==10&&Arrays.equals(new int[]{((TwoRegisterInstruction)li.get(1)).getRegisterA(),((TwoRegisterInstruction)li.get(1)).getRegisterB()},new int[]{3,1}),"Loading guard is not restricted to state 3");
    need(li.get(2).getOpcode()==Opcode.IGET_OBJECT&&li.get(3).getOpcode()==Opcode.INSTANCE_OF&&li.get(4).getOpcode()==Opcode.IF_EQZ&&target(li,4)==10&&li.get(5).getOpcode()==Opcode.CHECK_CAST&&li.get(6).getOpcode()==Opcode.IGET_OBJECT,"Missing owned WebView lookup");
    need(reg2(li.get(2),0,2)&&reg2(li.get(3),1,0)&&reg1(li.get(4),1)&&reg1(li.get(5),0)&&reg2(li.get(6),0,0),"Unsafe loading guard operands");
    invoke(li.get(7),GUARD,0);need(li.get(8).getOpcode()==Opcode.MOVE_RESULT&&reg1(li.get(8),0)&&li.get(9).getOpcode()==Opcode.IF_NEZ&&reg1(li.get(9),0)&&target(li,9)==11,"Invalid loading guard flow");
    // Smali has no instruction for labels: the normal setter follows the guard branch.
    String originalSetter="";for(Instruction i:li)if(i.getOpcode()==Opcode.INVOKE_VIRTUAL){need(originalSetter.isEmpty(),"Duplicate setter in wrapper");originalSetter=ref(i);invoke(i,originalSetter,2,3);}
    need(!originalSetter.isEmpty(),"Original setter missing");
    List<Instruction> ni=instructions(install);need(ni.size()==9,"Unexpected native installer length");
    need(ni.get(0).getOpcode()==Opcode.IGET_OBJECT&&((TwoRegisterInstruction)ni.get(0)).getRegisterA()==0&&((TwoRegisterInstruction)ni.get(0)).getRegisterB()==7,"Native installer overlaps this register");
    for(int j=1;j<=5;j++)need((ni.get(j).getOpcode()==Opcode.CONST_STRING||ni.get(j).getOpcode()==Opcode.CONST_STRING_JUMBO)&&((OneRegisterInstruction)ni.get(j)).getRegisterA()==j,"Native installer argument register mismatch");
    need(constant(ni.get(6),6,1),"Original fixture should allow Page peer creation after app-consumer audit");
    invoke(ni.get(7),"Lhiro/berrybfcache/NativeRestoreTracker;->install("+WEB+STR+STR+STR+STR+STR+"Z)V",0,1,2,3,4,5,6);
    need(ni.get(8).getOpcode()==Opcode.RETURN_VOID,"Native install return missing");
    List<Instruction> ci=instructions(complete);need(ci.get(3).getOpcode()==Opcode.IF_EQZ&&ci.get(6).getOpcode()==Opcode.IF_NE,"Ownership guards missing");
    need(ci.get(0).getOpcode()==Opcode.IGET_OBJECT&&reg2(ci.get(0),0,4)&&ci.get(1).getOpcode()==Opcode.IGET_OBJECT&&reg2(ci.get(1),1,0)&&ci.get(2).getOpcode()==Opcode.INSTANCE_OF&&reg2(ci.get(2),0,1)&&reg1(ci.get(3),0)&&ci.get(4).getOpcode()==Opcode.CHECK_CAST&&reg1(ci.get(4),1)&&ci.get(5).getOpcode()==Opcode.IGET_OBJECT&&reg2(ci.get(5),2,1)&&reg2(ci.get(6),2,5),"Completion ownership guard does not use this/WebView parameters");
    need(ref(li.get(2)).equals(ref(ci.get(1)))&&ref(li.get(3)).equals(ref(ci.get(2)))&&ref(li.get(6)).equals(ref(ci.get(5)))&&ref(ni.get(0)).equals(ref(ci.get(5))),"Loading guard/installer uses a different adapter or WebView");
    int failure=target(ci,3),success=ci.size()-4;need(target(ci,6)==failure&&failure==ci.size()-2&&constant(ci.get(failure),0,0)&&ci.get(failure+1).getOpcode()==Opcode.RETURN&&((OneRegisterInstruction)ci.get(failure+1)).getRegisterA()==0,"Ownership failure must return false");
    need(constant(ci.get(success),0,1)&&ci.get(success+1).getOpcode()==Opcode.RETURN&&((OneRegisterInstruction)ci.get(success+1)).getRegisterA()==0,"Completion success must return true");
    int conditional=0;for(int j=7;j<ci.size();j++)if(ci.get(j)instanceof OffsetInstruction){conditional++;need(ci.get(j).getOpcode()==Opcode.IF_EQZ&&target(ci,j)==success,"Unexpected completion exit");}need(conditional==1,"Hidden-tab completion must return true");
    // URL/title copies must use exactly the native getter -> metadata field assignments.
    for(String getter:Arrays.asList("getUrl","getTitle")){
      String[] expected=metadataPair(finished,getter),actual=metadataPair(complete,getter);
      need(Arrays.equals(expected,actual),"Native metadata assignment differs for "+getter);
    }
    Map<String,String> rewrite=new HashMap<>();rewrite.put(F.getMethodDescriptor(tabStart),"Lhiro/berry/TabPriority;->started("+WEB+")V");rewrite.put(F.getMethodDescriptor(loading),originalSetter);
    uniqueRef(started,F.getMethodDescriptor(tabStart));uniqueRef(started,F.getMethodDescriptor(loading));
    int webParameter=started.getImplementation().getRegisterCount()-3,urlParameter=webParameter+1;
    for(Instruction i:instructions(started))if(i instanceof OneRegisterInstruction&&(i.getOpcode().setsRegister()||i.getOpcode().setsWideRegister())){
      int written=((OneRegisterInstruction)i).getRegisterA();
      need(written!=webParameter&&written!=urlParameter&&(!i.getOpcode().setsWideRegister()||written+1!=webParameter&&written+1!=urlParameter),"Native start overwrites WebView/URL parameters used by completion tail");
    }
    Method old=uniqueMethod(load(baseline),client,"onPageStarted");String before=canonical(old,false,Collections.emptyMap()),after=canonical(started,true,rewrite);
    need(before.equals(after),"Original onPageStarted body/operands/branches changed\nOLD\n"+before+"\nNEW\n"+after);
    for(ClassDef c:all.values())need(!c.getType().contains("RestoreDiagnostics"),"Legacy unpublished diagnostic runtime is present");
    need(all.containsKey("Lhiro/berrybfcache/PresentationRecovery;"),"Presentation recovery runtime absent");
    need(all.containsKey("Lhiro/berrybfcache/RenderDiagnostics;"),"Render diagnostics runtime absent");
    boolean starts=false,cancels=false,records=false;
    for(ClassDef c:all.values())if(c.getType().startsWith("Lhiro/berrybfcache/"))for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:instructions(m)) {
      String r=ref(i);
      if(c.getType().equals(COORD)&&r.startsWith("Lhiro/berrybfcache/PresentationRecovery;->start("))starts=true;
      if(c.getType().equals(COORD)&&r.startsWith("Lhiro/berrybfcache/PresentationRecovery;->cancel("))cancels=true;
      if(r.startsWith("Lhiro/berrybfcache/RenderDiagnostics;->record("))records=true;
    }
    need(starts&&cancels,"Coordinator does not start/cancel presentation recovery");
    need(records,"No rendering diagnostic phase records");
  }
  static String[] metadataPair(Method m,String name){
    String[] found=null;List<Instruction> in=instructions(m);
    for(int j=0;j+2<in.size();j++)if(in.get(j)instanceof ReferenceInstruction&&((ReferenceInstruction)in.get(j)).getReference()instanceof MethodReference){
      MethodReference r=(MethodReference)((ReferenceInstruction)in.get(j)).getReference();if(!r.getName().equals(name))continue;
      if(in.get(j).getOpcode()!=Opcode.INVOKE_INTERFACE&&in.get(j).getOpcode()!=Opcode.INVOKE_INTERFACE_RANGE)continue;
      need(in.get(j+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT,"Metadata result missing");int value=((OneRegisterInstruction)in.get(j+1)).getRegisterA(),put=j+2;
      if(in.get(put).getOpcode()==Opcode.IGET_OBJECT)put++;
      need(in.get(put).getOpcode()==Opcode.IPUT_OBJECT&&((TwoRegisterInstruction)in.get(put)).getRegisterA()==value,"Metadata copy missing");
      need(found==null,"Duplicate native metadata getter "+name);found=new String[]{F.getMethodDescriptor(r),ref(in.get(put))};
    }need(found!=null,"Native metadata missing "+name);return found;
  }
  static boolean methodExists(Map<String,ClassDef> all,MethodReference m,String owner,Set<String> seen){
    if(platform(owner))return true;if(!seen.add(owner))return false;ClassDef c=all.get(owner);if(c==null)return false;
    for(Method x:c.getMethods())if(x.getName().equals(m.getName())&&x.getReturnType().equals(m.getReturnType())&&x.getParameterTypes().equals(m.getParameterTypes()))return true;
    if(c.getSuperclass()!=null&&methodExists(all,m,c.getSuperclass(),seen))return true;
    for(String it:c.getInterfaces())if(methodExists(all,m,it,seen))return true;
    return false;
  }
  static boolean reaches(String from,String wanted,Map<String,Set<String>> calls,Set<String> seen){
    if(from.equals(wanted))return true;if(!seen.add(from))return false;
    for(String next:calls.getOrDefault(from,Collections.emptySet()))if(reaches(next,wanted,calls,seen))return true;
    return false;
  }
  static void auditDiagnosticCopy(Map<String,ClassDef> all){
    String diagnostic="Lhiro/berrybfcache/RenderDiagnostics;";
    Method copy=uniqueMethod(all,diagnostic,"copy");String copyDescriptor=F.getMethodDescriptor(copy);
    need((copy.getAccessFlags()&2)!=0,"Diagnostic copy must be private");
    Map<String,Set<String>> calls=new HashMap<>();List<String> clicks=new ArrayList<>();int writes=0;
    for(ClassDef c:all.values())if(c.getType().startsWith("Lhiro/berrybfcache/"))for(Method m:c.getMethods())if(m.getImplementation()!=null){
      String source=F.getMethodDescriptor(m);Set<String> targets=calls.computeIfAbsent(source,k->new HashSet<>());
      if(m.getName().equals("onClick"))clicks.add(source);
      for(Instruction i:instructions(m))if(i instanceof ReferenceInstruction&&((ReferenceInstruction)i).getReference()instanceof MethodReference){
        MethodReference r=(MethodReference)((ReferenceInstruction)i).getReference();targets.add(F.getMethodDescriptor(r));
        if(r.getDefiningClass().equals("Landroid/content/ClipboardManager;")&&r.getName().equals("setPrimaryClip")){
          writes++;need(source.equals(copyDescriptor),"Clipboard write outside explicit diagnostic copy");
        }
        if(c.getType().startsWith("Lhiro/berrybfcache/RenderDiagnostics")||c.getType().startsWith("Lhiro/berrybfcache/PostScrollDiagnostics"))
          need(!r.getName().equals("getUrl")&&!r.getName().equals("getTitle"),"Diagnostic runtime reads URL/title");
      }
    }
    need(writes==1,"Unexpected clipboard write count");
    boolean clickCanCopy=false;for(String click:clicks)if(reaches(click,copyDescriptor,calls,new HashSet<>()))clickCanCopy=true;
    need(clickCanCopy,"No explicit button handler reaches diagnostic copy");
    for(String name:Arrays.asList("record","begin","cancel","show","report")){
      int found=0;for(Method m:all.get(diagnostic).getMethods())if(m.getName().equals(name)) {
        found++;need(!reaches(F.getMethodDescriptor(m),copyDescriptor,calls,new HashSet<>()),"Automatic diagnostic path can copy without button: "+name);
      }need(found>0,"Missing diagnostic API "+name);
    }
  }
  static String sha256(String text)throws Exception{
    byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();
  }
  static String diagnosticTarget(Map<String,ClassDef> all,MethodReference reference){
    String descriptor=F.getMethodDescriptor(reference),name=reference.getName();
    if(!reference.getDefiningClass().equals(POST_SCROLL)||!(name.equals("-$$Nest$smscriptFor")||name.equals("-$$Nest$smparse")))return descriptor;
    Method bridge=uniqueMethod(all,POST_SCROLL,name);List<Instruction> in=instructions(bridge);
    need((bridge.getAccessFlags()&8)!=0&&bridge.getParameterTypes().equals(Arrays.asList(STR))&&bridge.getReturnType().equals(STR)&&in.size()==3,"Unexpected diagnostic nest bridge");
    String target=POST_SCROLL+"->"+name.substring("-$$Nest$sm".length())+"("+STR+")"+STR;
    int input=bridge.getImplementation().getRegisterCount()-1;invoke(in.get(0),target,input);
    need(in.get(1).getOpcode()==Opcode.MOVE_RESULT_OBJECT&&in.get(2).getOpcode()==Opcode.RETURN_OBJECT
        &&((OneRegisterInstruction)in.get(1)).getRegisterA()==((OneRegisterInstruction)in.get(2)).getRegisterA(),"Diagnostic nest bridge does not return direct result");
    return target;
  }
  static void auditNumericDiagnostics(Map<String,ClassDef> all)throws Exception{
    need(all.containsKey(POST_SCROLL),"Post-scroll diagnostics absent");
    String scriptField="Lhiro/berrybfcache/PostScrollDiagnostics$State;->script:Ljava/lang/String;";
    int evaluations=0,literals=0,scriptWrites=0,parserCalls=0;
    boolean parserBound=false;
    Set<String> mutations=new HashSet<>(Arrays.asList("reload","loadUrl","loadData","loadDataWithBaseURL","scrollTo","scrollBy", "zoomBy","zoomIn","zoomOut","setInitialScale","setScaleX","setScaleY","setTranslationX","setTranslationY","setLayerType","setVisibility","invalidate","postInvalidate","postInvalidateOnAnimation","requestLayout","clearHistory","clearCache","destroy","onPause","onResume"));
    for(ClassDef c:all.values())if(c.getType().startsWith("Lhiro/berrybfcache/RenderDiagnostics")||c.getType().startsWith("Lhiro/berrybfcache/PostScrollDiagnostics"))for(Method m:c.getMethods())if(m.getImplementation()!=null){
      List<Instruction> in=instructions(m);
      for(int j=0;j<in.size();j++){
        Instruction i=in.get(j);if(!(i instanceof ReferenceInstruction))continue;
        Reference r=((ReferenceInstruction)i).getReference();
        if(r instanceof StringReference){String value=((StringReference)r).getString();if(value.startsWith("/*BerryPostScrollNumericV1*/")){
          literals++;need(c.getType().equals(POST_SCROLL)&&m.getName().equals("scriptFor")&&sha256(value).equals(SCRIPT_SHA),"Numeric script differs from reviewed read-only literal");
        }}
        if(r instanceof FieldReference&&ref(i).equals(scriptField)&&i.getOpcode()==Opcode.IPUT_OBJECT){
          scriptWrites++;need(c.getType().equals("Lhiro/berrybfcache/PostScrollDiagnostics$State;")&&m.getName().equals("<init>"),"Diagnostic script can change after construction");
          need(j>=2&&in.get(j-1).getOpcode()==Opcode.MOVE_RESULT_OBJECT&&in.get(j-2)instanceof ReferenceInstruction&&((ReferenceInstruction)in.get(j-2)).getReference()instanceof MethodReference
            &&diagnosticTarget(all,(MethodReference)((ReferenceInstruction)in.get(j-2)).getReference()).equals(POST_SCROLL+"->scriptFor("+STR+")"+STR),"Diagnostic script is not produced by guarded literal builder");
          need(((OneRegisterInstruction)in.get(j-1)).getRegisterA()==((TwoRegisterInstruction)i).getRegisterA(),"Diagnostic script result register differs");
        }
        if(!(r instanceof MethodReference))continue;
        MethodReference mr=(MethodReference)r;String descriptor=F.getMethodDescriptor(mr);
        if(c.getType().startsWith("Lhiro/berrybfcache/PostScrollDiagnostics"))need(!mutations.contains(mr.getName()),"Post-scroll observation mutates browsing/rendering: "+descriptor);
        if(mr.getName().equals("evaluateJavascript")){
          evaluations++;need(c.getType().equals(POST_SCROLL)&&m.getName().equals("sampleNow")&&descriptor.equals(WEB+"->evaluateJavascript("+STR+"Landroid/webkit/ValueCallback;)V"),"Unexpected diagnostic evaluation site");
          int reg=args(i)[1],writer=-1,before=j;
          for(int moves=0;moves<8;moves++){
            writer=-1;
            for(int k=before-1;k>=0;k--){Instruction previous=in.get(k);if(previous.getOpcode().setsRegister()&&previous instanceof OneRegisterInstruction&&((OneRegisterInstruction)previous).getRegisterA()==reg){writer=k;break;}}
            if(writer>=0&&in.get(writer).getOpcode().name().startsWith("MOVE_OBJECT")){
              reg=((TwoRegisterInstruction)in.get(writer)).getRegisterB();before=writer;
            }else break;
          }
          need(writer>=0&&in.get(writer).getOpcode()==Opcode.IGET_OBJECT&&ref(in.get(writer)).equals(scriptField),"Evaluation is not fed from the fixed guarded script field");
          for(int k=writer+1;k<j;k++)need(!(in.get(k)instanceof OffsetInstruction),"Evaluation script register crosses an unreviewed branch");
        }
        if(diagnosticTarget(all,mr).equals(POST_SCROLL+"->parse("+STR+")"+STR)&&!m.getName().equals("-$$Nest$smparse")){
          parserCalls++;need(c.getType().startsWith("Lhiro/berrybfcache/PostScrollDiagnostics$")&&m.getName().equals("onReceiveValue"),"Numeric parser called outside value callback");
        }
      }
    }
    Method parse=uniqueMethod(all,POST_SCROLL,"parse");for(Instruction i:instructions(parse))if(i instanceof NarrowLiteralInstruction&&((NarrowLiteralInstruction)i).getNarrowLiteral()==4096)parserBound=true;
    need(evaluations==1&&literals==1&&scriptWrites==1&&parserCalls==1&&parserBound,"Numeric observation pipeline inventory differs");
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
    need(a.length==2,"Expected patched APK and baseline332 APK");
    auditNative(all,Path.of(a[1]));
    auditDiagnosticCopy(all);
    auditNumericDiagnostics(all);
    Map<String,Integer> found=new TreeMap<>();List<Method> hooked=new ArrayList<>();
    for(ClassDef c:all.values())if(!helper(c.getType()))for(Method m:c.getMethods())if(m.getImplementation()!=null){
      boolean isHooked=false;
      for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction){Reference ref=((ReferenceInstruction)i).getReference();if(ref instanceof MethodReference){MethodReference mr=(MethodReference)ref;String key=mr.getDefiningClass()+"->"+mr.getName();if(required.containsKey(key)){found.merge(key,1,Integer::sum);isHooked=true;}}}
      if(isHooked||m.getName().startsWith("hiro")||m.getName().equals("onPageStarted")&&m.getDefiningClass().equals(uniqueMethod(all,null,"hiroCompleteBfcache").getDefiningClass()))hooked.add(m);
    }
    for(Map.Entry<String,Integer> e:required.entrySet())if(!e.getValue().equals(found.get(e.getKey())))throw new IllegalStateException("Hook count differs: "+e.getKey()+" expected="+e.getValue()+" actual="+found.get(e.getKey()));
    int refs=0;
    for(Method m:hooked)checkEncoding(m);
    // Every generated native helper keeps all incoming parameter registers intact.
    for(Method m:hooked)if(Arrays.asList("hiroCompleteBfcache","hiroInstallBfcache","hiroInstallNativeBfcache","hiroStartedBfcache","hiroTabStartedBfcache","hiroSetLoadingBfcache").contains(m.getName())){
      int words=(m.getAccessFlags()&8)==0?1:0;for(CharSequence t:m.getParameterTypes())words+=(t.toString().equals("J")||t.toString().equals("D"))?2:1;
      int first=m.getImplementation().getRegisterCount()-words;
      for(Instruction i:instructions(m))if(i.getOpcode().setsRegister()&&i instanceof OneRegisterInstruction)
        need(((OneRegisterInstruction)i).getRegisterA()<first,"Generated helper overwrites incoming parameters: "+m);
    }
    for(Method m:hooked)for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction){Reference r=((ReferenceInstruction)i).getReference();
      if(r instanceof MethodReference){MethodReference mr=(MethodReference)r;if(!methodExists(all,mr,mr.getDefiningClass(),new HashSet<>()))throw new IllegalStateException("Unresolved method from hook: "+mr);refs++;}
      else if(r instanceof FieldReference){FieldReference fr=(FieldReference)r;if(!fieldExists(all,fr,fr.getDefiningClass(),new HashSet<>()))throw new IllegalStateException("Unresolved field from hook: "+fr);refs++;}
    }
    int runtimeMethods=0,runtimeRefs=0;
    for(ClassDef c:all.values())if(c.getType().startsWith("Lhiro/berrybfcache/"))for(Method m:c.getMethods())if(m.getImplementation()!=null) {
      checkEncoding(m);runtimeMethods++;
      for(Instruction i:instructions(m))if(i instanceof ReferenceInstruction) {
        Reference r=((ReferenceInstruction)i).getReference();
        if(r instanceof MethodReference){MethodReference mr=(MethodReference)r;need(methodExists(all,mr,mr.getDefiningClass(),new HashSet<>()),"Unresolved runtime method: "+mr);runtimeRefs++;}
        else if(r instanceof FieldReference){FieldReference fr=(FieldReference)r;need(fieldExists(all,fr,fr.getDefiningClass(),new HashSet<>()),"Unresolved runtime field: "+fr);runtimeRefs++;}
      }
    }
    need(runtimeMethods>0&&runtimeRefs>0,"No runtime references checked");
    if(!all.containsKey("Lapp/hiro/berry/runtime/BerryQueueInit;"))throw new IllegalStateException("Queue provider missing");
    String ascii=new String(manifest,java.nio.charset.StandardCharsets.ISO_8859_1).replace("\0","");
    if(!ascii.contains("app.hiro.berry.runtime.BerryQueueInit")||!ascii.contains("jp.ejimax.berrybrowser.hiro.messagequeue"))throw new IllegalStateException("Queue manifest provider absent");
    System.out.println("{\"result\":\"PASS_ACTUAL_HOOKS\",\"expected_hook_kinds\":"+required.size()+",\"application_methods_with_hooks\":"+hooked.size()+",\"resolved_method_and_field_references\":"+refs+",\"register_bounds_invoke_words_and_branches_valid\":true,\"queue_provider_and_manifest_present\":true,\"native_start_body_preserved_from_332\":true,\"guarded_loading_and_tail_hooks_valid\":true,\"tail_webview_url_parameters_preserved\":true,\"completion_boolean_and_native_metadata_valid\":true,\"native_installer_registers_valid\":true,\"render_recovery_runtime_present\":true,\"render_diagnostics_runtime_present\":true,\"legacy_diagnostic_runtime_absent\":true,\"all_new_runtime_references_valid\":true,\"diagnostic_copy_requires_explicit_click\":true,\"diagnostic_url_title_text_reads_absent\":true,\"diagnostic_numeric_javascript_allowlisted\":true,\"diagnostic_scroll_sampler_has_no_rendering_mutators\":true,\"runtime_methods_checked\":"+runtimeMethods+",\"runtime_method_and_field_references_checked\":"+runtimeRefs+"}");
  }
}
