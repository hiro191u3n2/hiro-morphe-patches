import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Read-only actual MainActivity ancestry and Android lifecycle/key entrypoints. */
public final class MainHierarchyAudit {
  public static void main(String[] args)throws Exception {
    Map<String,ClassDef> all=HomeBackAudit.read(args[0]);
    String type="Lcom/instagram/mainactivity/InstagramMainActivity;";Set<String> visited=new LinkedHashSet<>();
    Set<String> names=new HashSet<>(Arrays.asList("onCreate","onResume","onDestroy","onNewIntent","onUserInteraction","onUserLeaveHint","onBackPressed","dispatchKeyEvent"));
    while(type!=null&&visited.add(type)) {
      ClassDef c=all.get(type);if(c==null){System.out.println("PLATFORM_BOUNDARY "+type);break;}
      System.out.println("CLASS "+c.getType()+" extends "+c.getSuperclass());
      for(Method m:c.getMethods())if(names.contains(m.getName())) {
        System.out.println("  ENTRY "+m+" flags="+m.getAccessFlags()+" registers="+(m.getImplementation()==null?"none":m.getImplementation().getRegisterCount()));
        int index=0,pc=0;for(Instruction i:HomeBackAudit.instructions(m)) {
          MethodReference r=HomeBackAudit.ref(i);
          if(r!=null&&(r.getDefiningClass().startsWith("Lapp/hiro/oneback/")||r.getName().startsWith("hiroOneBack$")||names.contains(r.getName())))System.out.println("    i="+index+" pc="+pc+" "+i.getOpcode()+" "+r+" registers="+UploadIdentityAudit.args(i));
          index++;pc+=i.getCodeUnits();
        }
      }
      type=c.getSuperclass();
    }
  }
}
