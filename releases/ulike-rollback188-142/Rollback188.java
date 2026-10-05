import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;

/** Replace the whole ULike loader namespace, not selected methods or strings. */
public final class Rollback188 {
  static final Opcodes OPS = Opcodes.forApi(26);
  static final String NS = "Lapp/hiro/ulike/patches/";
  static void check(boolean ok, String why) { if (!ok) throw new IllegalStateException(why); }
  static Map<String,ClassDef> load(String path) throws Exception {
    var result = new TreeMap<String,ClassDef>();
    var container = DexFileFactory.loadDexContainer(Path.of(path).toFile(), OPS);
    for (String name : container.getDexEntryNames())
      for (ClassDef c : container.getEntry(name).getDexFile().getClasses())
        check(result.put(c.getType(), c) == null, "Duplicate class: " + c.getType());
    return result;
  }
  static byte[] encode(Collection<ClassDef> classes) throws Exception {
    var pool = new DexPool(OPS);
    for (ClassDef c : classes) pool.internClass(ImmutableClassDef.of(c));
    var out = new MemoryDataStore();
    try { pool.writeTo(out); return out.getData(); } finally { out.close(); }
  }
  static String canonical(ClassDef c) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encode(List.of(c))));
  }
  public static void main(String[] args) throws Exception {
    check(args.length == 4, "BASE_DEX DONOR_DEX OUTPUT_DEX AUDIT_TSV");
    var base = load(args[0]); var donor = load(args[1]);
    check(base.size() == 243, "Unexpected baseline class count");
    check(donor.size() == 23, "Unexpected donor class count");
    var result = new TreeMap<>(base); var removed = new TreeSet<String>();
    for (String type : base.keySet()) if (type.startsWith(NS)) { result.remove(type); removed.add(type); }
    check(removed.size() == 23, "Unexpected old ULike namespace");
    for (var e : donor.entrySet()) {
      check(e.getKey().startsWith(NS), "Donor contains a non-ULike class: " + e.getKey());
      check(result.put(e.getKey(), e.getValue()) == null, "Cross-app collision: " + e.getKey());
    }
    check(removed.equals(donor.keySet()), "Unexpected loader class inventory change");
    Files.write(Path.of(args[2]), encode(result.values()));
    var actual = load(args[2]); check(actual.keySet().equals(result.keySet()), "Serialized class set differs");
    var audit = new ArrayList<String>(); int kept = 0, restored = 0;
    for (var e : actual.entrySet()) {
      ClassDef expected = e.getKey().startsWith(NS) ? donor.get(e.getKey()) : base.get(e.getKey());
      String hash = canonical(e.getValue());
      check(hash.equals(canonical(expected)), "Class semantics changed: " + e.getKey());
      if (e.getKey().startsWith(NS)) restored++; else kept++;
      audit.add(e.getKey() + "\t" + hash + "\t" + (e.getKey().startsWith(NS) ? "exact_donor_1.8.8" : "unchanged_baseline_1.0.141"));
    }
    check(kept == 220 && restored == 23, "Incomplete verification");
    Files.write(Path.of(args[3]), audit);
    System.out.println("PASS exact_donor_classes=" + restored + " unchanged_other_classes=" + kept);
  }
}
