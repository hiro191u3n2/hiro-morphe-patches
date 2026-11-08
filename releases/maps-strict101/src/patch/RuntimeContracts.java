package app.hiro.mapsstrict.patches;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload;
import com.android.tools.smali.dexlib2.iface.reference.*;
/** Frozen response/protobuf schema and code from both inspected APKs; never learned at patch time. */
public final class RuntimeContracts {
    private RuntimeContracts() {}
    private static final String[][] CLASSES = {
        {"Lcmzs;", "226b5641b2d2eec3df02a5a2f00f5bdcfcf077b23383cc0388ad06cdfc1301a2", "c9b3a84e2566ec47c49ec3309514906f04a017d1f39827e7238dea5af3e291bd"},
        {"Lcmzk;", "c76ebb205c08f5592dd0c5568ea7fbc88afb5c609f94255a333fe88f1a50348a", "d69037fc8bfd2c109a5212560b3b03c7eaf554cdbfc087ea34176451bf3ed215"},
        {"Lcnak;", "33ec1f6fe39a20173c68689442f029eca61a7904350535e43aff780be81c76b2", "b298843a3e5604fde6ac1d7d0acee323bc5cc55532e5129f8d3b4006e0e6a8fd"},
        {"Lcqgm;", "88b5b3ca1f657200651b841fb782800c084cbf7db35db51d227d2a9310205942", "0eab6532c96d51c5a7bad2bc967940ac3fcd375f2481abde2e6cf861d07c2420"},
        {"Lcqdn;", "8f50a321d72b085b5e95562277bba7eba9a6c46b5c5073f5d6677adfd89e3a00", "220a2d19431e69cf13e0c6b3a69257c014513442d7531573da086afeda44f7d8"},
        {"Lchaq;", "9053121e1b2f11293fd335bb4692e98a729d4e69c8d8bbd3565e3d402d170ee8", "1ae1ef3b87915c80caba075151ddc1a6a5cb57b97f050da8ec58624680b52a3c"},
        {"Lccfl;", "f2b8d3c567f4c41207c4eb6cccb6c5fdaa63959efec6062c03fa55fb5a6f6e82", "e76f4a31de80fb0f961f3cdb863b6e334cbc23c3efd8cab007c1d0f7a07a0612"},
        {"Lcjme;", "9f16eb0cb9b6530223a90fc24c869f593c6745c09c66e0a583450b103b749a79", "748ac3e51e13b30d6917b62bf2e6f25c03d4c6148554fe1768dcfff56927f438"},
        {"Lcbkh;", "c69172968c64e97e10618fc56dedfee832111a9f86dec0d2945b6e8f1027427e", "2fa20016813fada679886caf49031cccbcb6f10159926548278e75e5eee41c15"},
        {"Lcffu;", "699fa11884593025b93baaf6e4764c1e2c2ee62b66bf91e5847ea140d25d0672", "a0bad6bdbec5caf78c803fc72fc7d7ed0f1e742c9cd64d6e47a77041ffc0e811"},
        {"Lcfha;", "54d652db374391b998fc83987b7368948dde43da565675cd1db000bb06ca1440", "ab6c9c56bf9efefbbd55d633fd71edf98ceb7314758919163454e1150e0e1191"},
        {"Lcfgz;", "c47cb5a9a78fc6853613cfe136a9673d78ad939b79feb821b17eeac0bb98d95e", "043e801c0d1cd55262ac26d833e74528bb42f6f15fcae7c4c91b0595b3983717"},
        {"Lcfhn;", "5513b2ceb0663557fbd7a71d9a893d15f41d3cb3a0d84c89ae5475d58edd4877", "6fcadc2f291d7a21eef6bba0ee74a11e6c571576985e4b9967fe73bb2379b509"},
        {"Lcfhm;", "fed64344dda73d0fdb6df6761a60e82d0d568a1e64aa9e568d3d5a4523b69e33", "44cfbed6b3216becc9fdd0c257e33f96f88ad1d74fd6613a89778f56df0559aa"},
        {"Lcffs;", "bb22fa5003db33b33c21dc5d25c220edac76e689623743da8eb832186517bd4a", "24dfcbeb5e7e4ab3506bac3b81d0a35ebc5814cde9c49e73e0889d1abb95ef84"},
        {"Lcfmj;", "5f1ded2d91299cfb8ad2c939e5d17714e53c96a4bfe41856dc0a6f01b758a16f", "4bcbbfe233c62a8d5661ec635bc5561cd1e66fdf8ee8601b73514d171b6dfe68"},
        {"Lcfmi;", "7c0d7b22641c21a7478e2d1a36e172c3887895fdf9de37b362607e7a3a9826d3", "8bb20a4599a22b982204f6ae00d20af57ae47c604efe82058e71a99288dc9ef5"},
        // Native search pins, CID identity, selected wrappers, and preserved geometry.
        {"Lcbki;", "eca52c734767bc9e55fae97df52a2e84512a5e88af7479ae56257388754d3100", "dccfbefab27f9bc68611cf7e04935b98a19f8baf4b79c1b6774a7de4afffc279"},
        {"Lccpa;", "08ba20e013ea35e3738fe58eb454ab33cd1a89450b4ba49d7680dfab3f74f588", "1ef7d5942bf1043e6a57bae714bcb8df79aa5dee3e1ae3f12e5cb1805bb0de36"},
        {"Lckiu;", "1f569d6d1b9b85be3d119087e05fdf7a4cf3cef6e5312ee37a6c55fc9c394a48", "8ec393dd900e1f58986461da29543a80619eaa078fdf4e4587e6ae6c39e561ac"},
        {"Lccff;", "5d73b26b1b2435a475b47d441ddd85acf0a8c55c45bb012a5f0a60606a3a9450", "5ddfb1ca47609c78832b86160e3a2bb8af856a997b50b2b200baf27fc3d1a253"},
        {"Lcbkj;", "c3b8497eb53e1896ede3e96454b4475c8b35e8ed52e5197ad7b1d949bb836105", "15172ebe010f489bab298f773112915fe54bbd0c9476080eb6ebab0a6b492848"},
        {"Lcbke;", "284a951cec9d1ea8a76ace08587422885c9529e31e3504ba6d4ad5a487460815", "dc3e3ba52ef063a8e0a73c20ff3cf3d69d0f4b378c05ffa4265212be6184175b"},
        {"Lcbkg;", "fbaef36a46f2f7368541e834489cb5c34936092931d9a99970dff654dfe637e2", "4bbbfd39c8761b8a57c9ba6793c317a7af8b9afca60925245979c6ad9a363939"},
        {"Lccpb;", "d8667ce0df6a1d93d07bcdefade1fe84cdd57195a95255035e9f10b9b32c1bb7", "4cd418ecfbcd93512af7bba1f0c8727e8b148e2c9e9c68847170d6c4094df457"},
    };
    static String descriptor(MethodReference m) { return MapsStrictSearchPatch.descriptor(m); }
    static String hash(String s) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder b=new StringBuilder();
            for(byte x:digest)b.append(String.format(Locale.ROOT,"%02x",x&255));
            return b.toString();
        } catch(Exception e) { throw new IllegalStateException("Cannot validate response schema",e); }
    }
    public static void validate(MapsStrictSearchPatch.Lookup lookup) {
        for(String[] expected:CLASSES) {
            ClassDef c=lookup.get(expected[0]);
            MapsStrictSearchPatch.require(c!=null,"Missing response/protobuf class "+expected[0]);
            MapsStrictSearchPatch.require(expected[1].equals(hash(classApi(c))),"Response schema changed "+expected[0]);
            MapsStrictSearchPatch.require(expected[2].equals(hash(classBody(c))),"Response/protobuf code changed "+expected[0]);
        }
    }
  static String ref(Reference r){return r instanceof MethodReference?descriptor((MethodReference)r):r.toString();}
  static String body(Method m) {
    MethodImplementation code=m.getImplementation();if(code==null)return "abstract";
    StringBuilder b=new StringBuilder("registers=").append(code.getRegisterCount()).append('\n');
    int at=0;
    for(Instruction i:code.getInstructions()) {
      b.append(at).append(' ').append(i.getOpcode()).append(' ').append(i.getCodeUnits());
      if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction r=(RegisterRangeInstruction)i;b.append(" range=").append(r.getStartRegister()).append(',').append(r.getRegisterCount());}
      else if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;int[] v={r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()};b.append(" regs=");for(int j=0;j<r.getRegisterCount();j++)b.append(v[j]).append(',');}
      else if(i instanceof ThreeRegisterInstruction){ThreeRegisterInstruction r=(ThreeRegisterInstruction)i;b.append(" regs=").append(r.getRegisterA()).append(',').append(r.getRegisterB()).append(',').append(r.getRegisterC());}
      else if(i instanceof TwoRegisterInstruction){TwoRegisterInstruction r=(TwoRegisterInstruction)i;b.append(" regs=").append(r.getRegisterA()).append(',').append(r.getRegisterB());}
      else if(i instanceof OneRegisterInstruction)b.append(" reg=").append(((OneRegisterInstruction)i).getRegisterA());
      if(i instanceof ReferenceInstruction)b.append(" ref=").append(ref(((ReferenceInstruction)i).getReference()));
      if(i instanceof DualReferenceInstruction)b.append(" ref2=").append(ref(((DualReferenceInstruction)i).getReference2()));
      if(i instanceof WideLiteralInstruction)b.append(" lit=").append(((WideLiteralInstruction)i).getWideLiteral());
      if(i instanceof OffsetInstruction)b.append(" offset=").append(((OffsetInstruction)i).getCodeOffset());
      if(i instanceof SwitchPayload){b.append(" switches=");for(SwitchElement e:((SwitchPayload)i).getSwitchElements())b.append(e.getKey()).append(':').append(e.getOffset()).append(',');}
      if(i instanceof ArrayPayload){ArrayPayload a=(ArrayPayload)i;b.append(" array=").append(a.getElementWidth()).append(':').append(a.getArrayElements());}
      if(i instanceof VerificationErrorInstruction)b.append(" verification=").append(((VerificationErrorInstruction)i).getVerificationError());
      if(i instanceof InlineIndexInstruction)b.append(" inline=").append(((InlineIndexInstruction)i).getInlineIndex());
      if(i instanceof VtableIndexInstruction)b.append(" vtable=").append(((VtableIndexInstruction)i).getVtableIndex());
      if(i instanceof FieldOffsetInstruction)b.append(" fieldoffset=").append(((FieldOffsetInstruction)i).getFieldOffset());
      b.append('\n');at+=i.getCodeUnits();
    }
    for(TryBlock<? extends ExceptionHandler> t:code.getTryBlocks()){
      b.append("try ").append(t.getStartCodeAddress()).append(' ').append(t.getCodeUnitCount());
      for(ExceptionHandler h:t.getExceptionHandlers())b.append(' ').append(h.getExceptionType()).append(':').append(h.getHandlerCodeAddress());
      b.append('\n');
    }
    return b.toString();
  }
  static String classApi(ClassDef c) {
    StringBuilder b=new StringBuilder(c.getType()).append(' ').append(c.getAccessFlags()).append(' ').append(c.getSuperclass()).append(' ').append(c.getInterfaces()).append('\n');
    List<String> fields=new ArrayList<>();for(Field f:c.getFields())fields.add(f.toString()+" "+f.getAccessFlags()+" "+f.getInitialValue());Collections.sort(fields);for(String f:fields)b.append(f).append('\n');
    List<String> methods=new ArrayList<>();for(Method m:c.getMethods())methods.add(descriptor(m)+" "+m.getAccessFlags());Collections.sort(methods);for(String m:methods)b.append(m).append('\n');
    return b.toString();
  }
  static String classBody(ClassDef c) {
    StringBuilder b=new StringBuilder(classApi(c));
    List<Method> ms=new ArrayList<>();for(Method m:c.getMethods())ms.add(m);ms.sort(Comparator.comparing(RuntimeContracts::descriptor));
    for(Method m:ms)b.append(descriptor(m)).append('\n').append(body(m));
    return b.toString();
  }
}
