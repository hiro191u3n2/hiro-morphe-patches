import java.nio.file.*;import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
public final class VerifyUiBindings169 {
 public static void main(String[] a)throws Exception {
  var classes=MergePayloads.classes(a[0]);var methods=MergePayloads.methods(classes.values());ArrayList<String> rows=new ArrayList<>();
  String[] wanted={
   "Li/f/l/r/v$a;->onImageError(II)V","Li/f/l/r/s;->c(II)V","Li/f/l/r/s;->b(Landroid/graphics/Bitmap;[B)V","Li/f/l/r/s;->e()V",
   "Li/f/l/n/q/y/k;->c(Li/f/l/r/u;)V","Li/f/l/n/q/y/l;->c(Li/f/l/r/u;)V",
   "Li/o/a/b1/a/g/y$k;->a()V","Li/o/a/b1/a/g/y$k;->e(Li/o/a/b1/a/g/y;)V","Li/o/a/b1/a/g/y$k;->b()V","Li/o/a/b1/a/g/y$k;->c(Li/f/l/p/a;)V","Li/o/a/b1/a/g/y$k;->d()V",
   "Li/o/a/b1/a/g/y;->A0()Li/o/a/b1/a/e/l;","Li/o/a/b1/a/g/y;->R1(Z)V","Li/o/a/b1/a/g/y;->Q1(J)V","Li/o/a/b1/a/g/y;->P0()Li/o/a/b1/a/u/t;",
   "Li/o/a/b1/a/e/f;->c()Li/o/a/b1/a/b/e;","Li/o/a/b1/a/e/f;->t(Li/o/a/b1/a/e/f;)V",
   "Li/o/a/b1/a/b/c;->i()Li/o/a/b1/a/v/k;","Li/o/a/b1/a/b/c;->k()V","Li/o/a/b1/a/b/c;->l(ZILjava/lang/String;Ljava/lang/String;Lorg/json/JSONObject;)V",
   "Li/o/a/b1/a/v/h;->m(Ljava/lang/String;)V","Li/f/l/u/p;->h(Li/f/l/u/p;Ljava/lang/Object;ZILjava/lang/Object;)V",
   "Lcom/bytedance/strategy/persistence/entity/CapturedRecord;->setGenWidth(I)V","Lcom/bytedance/strategy/persistence/entity/CapturedRecord;->setGenHeight(I)V"};
  for(String id:wanted){Method m=methods.get(id);if(m==null||(m.getAccessFlags()&1)==0)throw new IllegalStateException("Missing public UI member "+id);rows.add(id+"\t"+MergePayloads.hash(m));}
  String[][] fields={{"Li/f/l/r/v$a;","c"},{"Li/f/l/r/s;","g","i","h","d"},{"Li/f/l/n/q/y/k;","b"},{"Li/f/l/n/q/y/l;","c"},{"Li/o/a/b1/a/g/y$k;","b","g","h","f","d"}};
  for(String[] group:fields){ClassDef c=classes.get(group[0]);for(int i=1;i<group.length;i++){boolean found=false;for(Field f:c.getFields())if(f.getName().equals(group[i])&&(f.getAccessFlags()&1)!=0){found=true;rows.add(DexFormatter.INSTANCE.getFieldDescriptor(f)+"\tpublic");}if(!found)throw new IllegalStateException("Missing public field "+group[0]+group[i]);}}
  Files.write(Path.of(a[1]),rows);System.out.println("PASS "+rows.size()+" exact original UI/callback members bound; this is static linkage evidence, not Android execution.");
 }
}
