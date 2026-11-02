import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Four source-unavailable .80 runtime bodies receive only reviewed fences and
 * read-only completion markers. Removing those calls restores the entire DEX
 * canonical class byte-for-byte, including codec options, error handlers and publication. */
public final class SaveRuntimeHooks1981 {
 static final String P="Lcom/hiro/ulike/", BITMAP="Landroid/graphics/Bitmap;";
 static final Set<String> ROOTS=Set.of("SaveFd186","SaveQuality2","FaceRegions1934");
 static final Set<String> TYPES=Set.of(P+"SaveFd186;",P+"SaveFd186$Pending;",P+"SaveQuality2;",P+"FaceRegions1934;");
 static final String FD=P+"SaveFd186;->saveStageBefore1947("+BITMAP+"Ljava/io/File;I)Z";
 static final String FILE=P+"SaveQuality2;->saveFinalBefore1947("+BITMAP+"Ljava/io/File;Landroid/graphics/Bitmap$CompressFormat;I)Z";
 static final String PREP=P+"SaveFd186$Pending;->prepare()V";
 static final String FACE=P+"FaceRegions1934;->forBitmap("+BITMAP+"I)Lcom/hiro/ulike/FaceRegions1934$Mask;";
 static final String GATE=P+"CodecPreparation1981;->awaitCodec1981()V";
 static final String RELEASE=P+"EncoderTail1981;->release("+BITMAP+BITMAP+")V";
 static final String COMPLETE=P+"FaceResult1981;->completed1981()V";
 static final String EMPTY=P+"FaceResult1981;->completedEmpty1981(I)V";
 static final String OLD_PREP=P+"AsyncSave1935;->prepareCodec(Ljava/util/concurrent/ExecutorService;Ljava/lang/Runnable;)V";
 static final String NEW_PREP=P+"AsyncSave1935;->prepareStorage1981(Ljava/util/concurrent/ExecutorService;Ljava/lang/Runnable;)V";
 static boolean owned(String type){return TYPES.contains(type);}
 static String canonical(ClassDef c)throws Exception{return MergePayloads.classHash(ImmutableClassDef.of(c));}
 static void req(boolean yes,String reason){MergePayloads.require(yes,reason);}
 static String ref(Instruction x){return x instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static MethodReference method(String owner,String name,List<String> parameters){return new ImmutableMethodReference(P+owner+";",name,parameters,"V");}
 static BuilderInstruction35c call(MethodReference r,int...v){int[]a=new int[5];System.arraycopy(v,0,a,0,v.length);return new BuilderInstruction35c(Opcode.INVOKE_STATIC,v.length,a[0],a[1],a[2],a[3],a[4],r);}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef edit(ClassDef c,boolean inverse)throws Exception {
  var methods=new ArrayList<Method>();int hits=0;
  for(Method m:c.getMethods()){
   String id=MergePayloads.id(m);
   if(!Set.of(FD,FILE,PREP,FACE).contains(id)){methods.add(m);continue;}
   req(m.getImplementation()!=null,"Missing inherited body "+id);
   var b=new MutableMethodImplementation(m.getImplementation());int edits=0;
   if(inverse){
    for(int i=b.getInstructions().size()-1;i>=0;i--){Instruction x=b.getInstructions().get(i);String r=ref(x);
     if(Set.of(GATE,RELEASE,COMPLETE,EMPTY).contains(r)){req(x.getOpcode()==Opcode.INVOKE_STATIC,"Exact inserted fence opcode");b.removeInstruction(i);edits++;}
     else if(r.equals(NEW_PREP)){req(id.equals(FD)&&x instanceof FiveRegisterInstruction,"Storage preparation site");FiveRegisterInstruction f=(FiveRegisterInstruction)x;
      req(f.getRegisterCount()==2,"Storage argument count");b.replaceInstruction(i,call(method("AsyncSave1935","prepareCodec",List.of("Ljava/util/concurrent/ExecutorService;","Ljava/lang/Runnable;")),f.getRegisterC(),f.getRegisterD()));edits++;}
    }
   }else if(id.equals(PREP)){
    int builder=-1;
    for(int i=0;i<b.getInstructions().size();i++){Instruction x=b.getInstructions().get(i);req(!ref(x).equals(GATE),"Already patched storage gate");if(x.getOpcode()==Opcode.NEW_INSTANCE&&ref(x).equals("Landroidx/heifwriter/HeifWriter$Builder;")){req(builder<0,"Duplicate builder");builder=i;}}
    req(builder>0&&b.getInstructions().get(builder-1).getOpcode()==Opcode.IF_EQZ,"FD null check before codec phase");
    // The conditional's successful fallthrough reaches this fence; its failure
    // branch still goes directly to the original FD error and abort handling.
    b.addInstruction(builder,call(method("CodecPreparation1981","awaitCodec1981",List.of())));edits++;
   }else if(id.equals(FACE)){
    // Original normal returns: reliable upright raster, and shared zero/invalid
    // detector count. The latter marker itself accepts exactly zero only.
    int reliable=-1,empty=-1,count=-1;int address=0;
    for(int i=0;i<b.getInstructions().size();i++){Instruction x=b.getInstructions().get(i);
     if(ref(x).equals("Landroid/media/FaceDetector;->findFaces("+BITMAP+"[Landroid/media/FaceDetector$Face;)I")){
      req(b.getInstructions().get(i+1).getOpcode()==Opcode.MOVE_RESULT,"Detector result follows call");count=((OneRegisterInstruction)b.getInstructions().get(i+1)).getRegisterA();}
     if(address==0x1e9){req(x.getOpcode()==Opcode.RETURN_OBJECT,".80 reliable return anchor");reliable=i;}
     if(address==0x1fa){req(x.getOpcode()==Opcode.RETURN_OBJECT,".80 empty return anchor");empty=i;}
     address+=x.getCodeUnits();
    }
    req(reliable>=0&&empty>reliable&&count==4,"Exact published face completion anchors");
    // Replace then append to retain branch labels on the new marker.
    Instruction ret=b.getInstructions().get(empty);b.replaceInstruction(empty,call(method("FaceResult1981","completedEmpty1981",List.of("I")),count));b.addInstruction(empty+1,new BuilderInstruction11x(Opcode.RETURN_OBJECT,((OneRegisterInstruction)ret).getRegisterA()));edits++;
    ret=b.getInstructions().get(reliable);b.replaceInstruction(reliable,call(method("FaceResult1981","completed1981",List.of())));b.addInstruction(reliable+1,new BuilderInstruction11x(Opcode.RETURN_OBJECT,((OneRegisterInstruction)ret).getRegisterA()));edits++;
   }else{
    int finalBitmap=-1,writer=-1,close=-1;int prep=0,adds=0;
    for(int i=0;i<b.getInstructions().size();i++){Instruction x=b.getInstructions().get(i);String r=ref(x);
     if(r.equals(OLD_PREP)){
      req(id.equals(FD)&&x instanceof FiveRegisterInstruction,"Exact deferred preparation call");FiveRegisterInstruction f=(FiveRegisterInstruction)x;
      req(f.getRegisterCount()==2,"Preparation argument count");b.replaceInstruction(i,call(method("AsyncSave1935","prepareStorage1981",List.of("Ljava/util/concurrent/ExecutorService;","Ljava/lang/Runnable;")),f.getRegisterC(),f.getRegisterD()));prep++;edits++;}
     if(r.equals("Landroidx/heifwriter/HeifWriter;->addBitmap("+BITMAP+")V")){
      req(x instanceof FiveRegisterInstruction,"Encoder bitmap arguments");FiveRegisterInstruction f=(FiveRegisterInstruction)x;writer=f.getRegisterC();finalBitmap=f.getRegisterD();adds++;}
     if(close<0&&r.equals(P+"TimedIo1947;->close(Landroidx/heifwriter/HeifWriter;)V")){
      req(x instanceof FiveRegisterInstruction&&((FiveRegisterInstruction)x).getRegisterC()==writer,"Owned normal writer closes after addBitmap");close=i;}
    }
    req(close>=0&&adds==1&&prep==(id.equals(FD)?1:0),"Exact normal encode tail "+id);
    int pristine=b.getRegisterCount()-(id.equals(FD)?3:4);
    req(pristine==(id.equals(FD)?11:10)&&finalBitmap==(id.equals(FD)?3:13),"Pinned source/final register ownership");
    b.addInstruction(close+1,call(method("EncoderTail1981","release",List.of(BITMAP,BITMAP)),finalBitmap,pristine));edits++;
   }
   req(edits==(id.equals(PREP)||id.equals(FILE)?1:2),"Exact hook count "+id+" "+edits);hits++;
   methods.add(replace(m,b));
  }
  req(hits==1,"Exactly one reviewed body per patched class "+c.getType());
  return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);
 }
 static ClassDef patch(ClassDef c)throws Exception{
  req(owned(c.getType()),"Declared source-unavailable runtime class");ClassDef out=edit(c,false);
  req(canonical(c).equals(canonical(edit(out,true))),"Full class inverse restores .80 including cleanup and codec options "+c.getType());return out;
 }
 static void verify(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception{
  for(String type:TYPES){req(before.containsKey(type)&&after.containsKey(type),"Runtime hook class exists");
   req(canonical(before.get(type)).equals(canonical(edit(after.get(type),true))),"Serialized runtime hook inverse "+type);
   req(canonical(patch(before.get(type))).equals(canonical(after.get(type))),"Serialized fence positions and register identities "+type);}
 }
}
