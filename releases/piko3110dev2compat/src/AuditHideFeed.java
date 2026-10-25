import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Independent inspection of emitted app DEX; never executes Instagram. */
public class AuditHideFeed {
  static void require(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
  static MethodReference ref(Instruction i) {
    if (i instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m) return m;
    return null;
  }
  static boolean string(Instruction i,String s) {
    return i instanceof ReferenceInstruction r && r.getReference() instanceof StringReference t && t.getString().equals(s);
  }
  public static void main(String[] args) throws Exception {
    int hooks=0,footers=0,loads=0,models=0;
    try(ZipFile z=new ZipFile(args[0])) {
      for(ZipEntry e: Collections.list(z.entries())) {
        if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
        DexBackedDexFile d=new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(z.getInputStream(e).readAllBytes()));
        for(ClassDef c:d.getClasses())for(Method m:c.getMethods()) {
          if(m.getImplementation()==null)continue;
          List<Instruction> ins=new ArrayList<>();m.getImplementation().getInstructions().forEach(ins::add);
          Map<Integer,Integer> at=new HashMap<>();int[] pc=new int[ins.size()];int off=0;
          for(int i=0;i<ins.size();i++){pc[i]=off;at.put(off,i);off+=ins.get(i).getCodeUnits();}
          boolean footer=ins.stream().anyMatch(i->string(i,"SHIMMER")) && ins.stream().anyMatch(i->string(i,"WELCOME_CARD")) && ins.stream().anyMatch(i->string(i,"syncUpdateState"));
          boolean model=ins.stream().anyMatch(i->string(i,"MainfeedAdapter.buildModels"));
          for(int i=0;i<ins.size();i++) {
            MethodReference r=ref(ins.get(i));
            if(r==null || !r.getName().equals("hideFeedPosts") || !r.getReturnType().equals("Z"))continue;
            hooks++;
            require(i+2<ins.size(),"Truncated hideFeedPosts hook");
            require(ins.get(i+1).getOpcode()==Opcode.MOVE_RESULT,"Missing result");
            Instruction branch=ins.get(i+2);
            require(branch instanceof OffsetInstruction,"Missing hideFeedPosts branch");
            Integer t=at.get(pc[i+2]+((OffsetInstruction)branch).getCodeOffset());
            require(t!=null,"Invalid branch address");
            if(footer) {
              require(branch.getOpcode()==Opcode.IF_NEZ,"Footer hide branch must be IF_NEZ");
              MethodReference dest=ref(ins.get(t));
              require(dest!=null && dest.getDefiningClass().equals("Lcom/facebook/systrace/Systrace;") && dest.getParameterTypes().equals(List.of("J")) && dest.getReturnType().equals("Z"),"Footer branch must target original Systrace call");
              MethodReference fall=ref(ins.get(i+3));
              require(fall!=null && fall.getParameterTypes().isEmpty() && fall.getReturnType().equals("Z"),"Disabled setting must retain original footer call");
              boolean snapshot=false;
              for(int j=0;j<i;j++){MethodReference a=ref(ins.get(j));if(a!=null&&a.getName().equals("<init>")&&a.getParameterTypes().size()==13)snapshot=true;}
              require(snapshot,"Snapshot construction must precede footer hook");
              footers++;
            } else if(model) {
              require(branch.getOpcode()==Opcode.IF_EQZ,"Model normal branch must be IF_EQZ");
              require(ins.get(i+3).getOpcode()==Opcode.CONST_16,"Hidden model count must be zeroed");
              require(ins.get(i+3) instanceof WideLiteralInstruction w && w.getWideLiteral()==0,"Model count must be exactly zero");
              require(t==i+4,"Normal model path must skip zeroing");models++;
            } else {
              require(i==0 && branch.getOpcode()==Opcode.IF_EQZ,"Load-more hook must start method");
              require(ins.get(i+3).getOpcode()==Opcode.RETURN_VOID,"Hidden load-more must return");
              require(t==i+4 && ins.get(t).getOpcode()==Opcode.IGET_OBJECT,"Normal load-more must retain original first instruction");loads++;
            }
          }
        }
      }
    }
    require(hooks==3 && footers==1 && models==1 && loads==1,"Expected exactly three complete hooks: "+hooks+","+footers+","+models+","+loads);
    System.out.println("PASS: 3 hideFeedPosts hooks; live footer branch reaches original Systrace, normal footer and snapshot preserved, model count and load-more guards intact.");
  }
}
