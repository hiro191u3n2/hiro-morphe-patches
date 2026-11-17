import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.hiro.newx.patches.NewXBackPatch;

public class BackDexTest {
 static void require(boolean c,String m){if(!c)throw new AssertionError(m);}
 public static void main(String[] a)throws Exception{
  Map<String,ClassDef> classes=new HashMap<String,ClassDef>();
  for(File f:new File(a[0]).listFiles())if(f.getName().endsWith(".dex"))for(ClassDef c:DexFileFactory.loadDexFile(f,Opcodes.getDefault()).getClasses())classes.put(c.getType(),c);
  ClassDef r=classes.get(NewXBackPatch.ROOT), cb=classes.get(NewXBackPatch.CALLBACK), m=classes.get(NewXBackPatch.MAIN);
  String u=NewXBackPatch.fingerprint(NewXBackPatch.code(NewXBackPatch.method(r,"u","Z"))),t=NewXBackPatch.fingerprint(NewXBackPatch.code(NewXBackPatch.method(r,"t","Z"))),b=NewXBackPatch.fingerprint(NewXBackPatch.callbackCode(NewXBackPatch.method(cb,"invoke","Ljava/lang/Object;")));
  System.out.println("U_SHA="+u+"\nT_SHA="+t+"\nCALLBACK_SHA="+b);
  if(a.length>1&&a[1].equals("fingerprints"))return;
  NewXBackPatch.verify(r,cb,m);
  MutableClass mr=new MutableClass(r),mc=new MutableClass(cb),mm=new MutableClass(m);
  NewXBackPatch.apply(mr,mc,mm);
  List<Instruction> before=NewXBackPatch.code(NewXBackPatch.method(r,"t","Z")),after=NewXBackPatch.code(NewXBackPatch.method(mr,"t","Z"));
  require(after.size()==before.size()+4,"root enabled hook size");
  require(after.get(2).getOpcode()==Opcode.IF_EQZ,"native t fallback");
  require(((OffsetInstruction)after.get(2)).getCodeOffset()==3,"t fallback targets original instruction");
  require(NewXBackPatch.fingerprint(before).equals(NewXBackPatch.fingerprint(after.subList(4,after.size()))),"native t tail unchanged");
  List<Instruction> ci=NewXBackPatch.code(NewXBackPatch.method(mc,"invoke","Ljava/lang/Object;"));int pos=NewXBackPatch.callbackStart(NewXBackPatch.method(cb,"invoke","Ljava/lang/Object;"))+1;
  require(ci.get(pos).getOpcode()==Opcode.INVOKE_STATIC,"root callback hook");
  require(ci.get(pos+2).getOpcode()==Opcode.IF_EQZ,"native callback fallback");
  require(((OffsetInstruction)ci.get(pos+2)).getCodeOffset()==3,"callback fallback target");
  require(ci.get(pos+3).getOpcode()==Opcode.RETURN_OBJECT,"handled callback returns Unit register");
  require(NewXBackPatch.code(NewXBackPatch.method(mm,"onStart","V")).get(0).getOpcode()==Opcode.INVOKE_STATIC_RANGE,"Activity bind hook");
  boolean rejected=false;try{NewXBackPatch.verify(mr,mc,mm);}catch(IllegalStateException expected){rejected=true;}require(rejected,"duplicate application rejected");
  require(NewXBackPatch.fingerprint(NewXBackPatch.code(NewXBackPatch.method(mr,"u","Z"))).equals(u),"native child stack predicate unchanged");
  int changed=0;for(ClassDef[] pair:new ClassDef[][]{{r,mr},{cb,mc},{m,mm}}){for(Method old:pair[0].getMethods()){if(old.getImplementation()==null)continue;Method newer=null;for(Method n:pair[1].getMethods())if(n.toString().equals(old.toString())){newer=n;break;}require(newer!=null,"method retained");if(!NewXBackPatch.fingerprint(NewXBackPatch.code(old)).equals(NewXBackPatch.fingerprint(NewXBackPatch.code(newer))))changed++;}}
  require(changed==3,"exactly 3 scoped methods changed: "+changed);
  System.out.println("PASS BackDexTest: stock native guards; three scoped methods; preserved native fallback; duplicate rejection");
 }
}
