import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Version-pinned update of the published ULike 1.4.3 patch.
 * Retains INTERNET instead of deleting it. All resource/bytecode privacy hooks remain.
 * No app API, certificate, authorization, or content-entitlement checks are bypassed.
 */
public final class RestoreNetworkPermission {
  static final String OLD_NAME = "高画質撮影・質感を残す美肌・通信遮断";
  static final String NEW_NAME = "高画質撮影・質感美肌・素材通信を復旧";
  static final String DESCRIPTION = "v1.4.4：フィルター・スタイル等の素材取得を妨げていたINTERNET権限の削除を廃止します。既存の解析ログ・プッシュ・端末情報収集抑止は維持します。通信自体は可能になり、通信先を素材サーバーだけに限定する許可リスト方式ではありません。24.5MP切替、HEIF専用DCIM/Camera保存、高画質・質感美肌、原本保管、3:4の64dp下移動と上下黒背景は維持。未改造ULike 5.6.2（740）専用。実機での素材一覧・ダウンロード・適用は未確認です。";
  public static void main(String[] args) throws Exception {
    if (args.length != 2) throw new IllegalArgumentException("input.class output.class");
    ClassNode c = new ClassNode();
    new ClassReader(Files.readAllBytes(Path.of(args[0]))).accept(c, 0);
    if (!c.name.equals("app/hiro/ulike/patches/UlikeHqMaxPatch")) throw new IllegalStateException(c.name);
    int removals=0,names=0,descriptions=0,compat=0;
    for (FieldNode f:c.fields) if (f.value instanceof String && OLD_NAME.equals(f.value)) {f.value=NEW_NAME; names++;}
    for (MethodNode m:c.methods) {
      for (AbstractInsnNode n=m.instructions.getFirst(); n!=null;) {
        AbstractInsnNode next=n.getNext();
        if (m.name.equals("applyManifest") && n instanceof MethodInsnNode i && i.owner.equals("org/w3c/dom/Node") && i.name.equals("removeChild") && i.desc.equals("(Lorg/w3c/dom/Node;)Lorg/w3c/dom/Node;")) {
          // Preserve existing validated INTERNET node. Consume parent and child references,
          // and retain the original return-value stack shape for the following POP.
          InsnList keep = new InsnList();
          keep.add(new InsnNode(Opcodes.POP2));
          keep.add(new InsnNode(Opcodes.ACONST_NULL));
          m.instructions.insertBefore(n, keep);
          m.instructions.remove(n);
          removals++;
        }
        if (n instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
          if (s.equals(OLD_NAME)) {ldc.cst=NEW_NAME; names++;}
          else if (s.startsWith("24.5MP") && s.contains("INTERNET権限を削除")) {ldc.cst=DESCRIPTION; descriptions++;}
          else if (s.startsWith("ULike 5.6.2") && s.contains("通信を無効化")) {
            ldc.cst="ULike 5.6.2（740）の未改造APK専用。オンライン素材取得のため通信を許可し、既存の解析・プッシュ停止を維持します。"; compat++;
          }
        }
        n=next;
      }
    }
    if(removals!=1 || names!=2 || descriptions!=1 || compat!=1)
      throw new IllegalStateException("Unexpected contract: "+removals+","+names+","+descriptions+","+compat);
    ClassWriter cw=new ClassWriter(0); c.accept(cw);
    Path out=Path.of(args[1]); Files.createDirectories(out.getParent()); Files.write(out,cw.toByteArray());
    System.out.println("PASS: INTERNET deletion removed once; privacy hooks and app payload untouched; UI metadata updated.");
  }
}
