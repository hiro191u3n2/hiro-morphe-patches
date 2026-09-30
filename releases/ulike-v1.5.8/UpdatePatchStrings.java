import java.nio.file.*;import org.objectweb.asm.*;
/** Change only the patch description. Loader/targets/permissions remain unchanged. */
public final class UpdatePatchStrings {
 public static void main(String[] a)throws Exception{
  String description="v1.5.8：ノイズ低減・くっきり補正を強化。色ノイズと明暗ノイズの重みを分離し、広めの輪郭保護フィルタで平坦部を滑らかにします。くっきり補正は細部と周辺コントラストの2段階。両機能に最強を追加し、肌の質感を優先・輪郭の白フチを抑える・暗部ノイズを優先して除去を個別設定可能。以前のオン／オフ・強さを保持、新保護3項目は初期オン。新規利用時の画質2機能は初期オフ。保存写真の8bit sRGBのみ、入力解像度と画角を維持。HDR・広色域・メインスレッド・メモリ不足時は追加処理を見送り。肌の保護は色と輪郭による推定で顔認識ではありません。強い設定では細部の軟化や硬い輪郭になる場合があります。既存の望遠ワンタップ・白い倍率表示・スタイル保持・端の顔補正・24.5MP・HEIF保存等は維持。未改造ULike 5.6.2（740）専用。実機の画質・起動・設定画面・保存完了・所要時間は未検証。";
  ClassReader r=new ClassReader(Files.readAllBytes(Path.of(a[0])));ClassWriter w=new ClassWriter(0);int[] n={0};
  r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){return new MethodVisitor(Opcodes.ASM9,super.visitMethod(access,name,desc,sig,exceptions)){
   @Override public void visitLdcInsn(Object value){if(value instanceof String s&&s.startsWith("v1.5.7：")){value=description;n[0]++;}super.visitLdcInsn(value);}
  };}},0);
  if(n[0]!=1)throw new IllegalStateException("Expected one version description: "+n[0]);Path p=Path.of(a[1]);Files.createDirectories(p.getParent());Files.write(p,w.toByteArray());
 }
}
