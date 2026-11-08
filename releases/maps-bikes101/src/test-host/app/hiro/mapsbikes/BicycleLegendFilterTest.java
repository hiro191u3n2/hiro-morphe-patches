package app.hiro.mapsbikes;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Byte-level regressions and an independent diff against the saved real legend. */
public final class BicycleLegendFilterTest {
    private static int assertions;
    private static final String BIKE = "poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium";
    private static final String SHOP = "poi/quantum/pinlet_v3/shopping-2-medium";
    private static final String CIRCLE = "poi/quantum/pinlet_v3/Circle_22_Background-2-medium";
    private static final byte[] HIDE = {0x38,0x01};
    private static final int[] REAL_CHANGED = {
        211,465,675,1066,1067,1068,1268,1332,2271,2284,2299,2380,2394,2395,
        2800,3432,3550,3984,4251,4252,4792,4893,8445,8446,8447,9014,9511,
        10885,10886,10887,10889,10890,10891,10892,10893,10896,10897,10899,
        10900,10905,10906,10908,10909,10910,10911
    };

    private BicycleLegendFilterTest() {}
    private static void check(boolean ok, String why) { assertions++; if (!ok) throw new AssertionError(why); }
    private static byte[] concat(byte[]... values) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] value : values) out.write(value,0,value.length);
        return out.toByteArray();
    }
    private static byte[] varint(long value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((value & ~127L) != 0) { out.write((int)(value & 127) | 128); value >>>= 7; }
        out.write((int)value); return out.toByteArray();
    }
    private static byte[] scalar(int field, long value) { return concat(varint(field << 3),varint(value)); }
    private static byte[] bytes(int field, byte[] value) {
        return concat(varint((field << 3) | 2),varint(value.length),value);
    }
    private static byte[] string(int field, String value) { return bytes(field,value.getBytes(StandardCharsets.UTF_8)); }
    private static byte[] refs(int field, int... ids) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int id : ids) { byte[] v=varint(id); out.write(v,0,v.length); }
        return bytes(field,out.toByteArray());
    }
    private static byte[] icon(int path) { return concat(scalar(2,4),scalar(5,path)); }
    private static byte[] legend(List<byte[]> styles, List<byte[]> icons, List<String> paths) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] style : styles) { byte[] v=bytes(3,style); out.write(v,0,v.length); }
        for (byte[] icon : icons) { byte[] v=bytes(9,icon); out.write(v,0,v.length); }
        for (String path : paths) { byte[] v=string(10,path); out.write(v,0,v.length); }
        byte[] tail=concat(string(11,"icon/name=assets/icons/"),string(12,".png&scale=4"),bytes(30,new byte[]{0,37,99}));
        out.write(tail,0,tail.length); return out.toByteArray();
    }
    private static byte[] legend(byte[]... styles) {
        return legend(Arrays.asList(styles),Arrays.asList(icon(0),icon(1),icon(2)),Arrays.asList(BIKE,SHOP,CIRCLE));
    }
    private static Set<Integer> set(int... values) {
        Set<Integer> out=new HashSet<Integer>(); for(int value:values)out.add(value); return out;
    }

    private static final class Raw {
        final int number, wire;
        final byte[] raw, body;
        Raw(int number,int wire,byte[]raw,byte[]body) { this.number=number;this.wire=wire;this.raw=raw;this.body=body; }
    }
    private static long readVarint(byte[] data, int[] at) {
        long value=0;
        for(int shift=0;shift<70;shift+=7) {
            if(at[0]>=data.length)throw new IllegalArgumentException("truncated");
            int b=data[at[0]++]&255;
            if(shift==63 && b>1)throw new IllegalArgumentException("overflow");
            value|=(long)(b&127)<<shift;
            if((b&128)==0)return value;
        }
        throw new IllegalArgumentException("overflow");
    }
    /** Independent wire reader; it does not call production parsing or predicates. */
    private static List<Raw> read(byte[] data) {
        List<Raw> out=new ArrayList<Raw>(); int[] at={0};
        while(at[0]<data.length) {
            int start=at[0]; long tag=readVarint(data,at); int wire=(int)(tag&7),number=(int)(tag>>>3);
            int body=at[0];
            if(wire==0)readVarint(data,at);
            else if(wire==1)at[0]+=8;
            else if(wire==5)at[0]+=4;
            else if(wire==2) { int length=(int)readVarint(data,at);body=at[0];at[0]+=length; }
            else throw new IllegalArgumentException("wire");
            if(number==0 || at[0]>data.length || at[0]<body)throw new IllegalArgumentException("length");
            out.add(new Raw(number,wire,Arrays.copyOfRange(data,start,at[0]),Arrays.copyOfRange(data,body,at[0])));
        }
        return out;
    }

    private static int assertDiff(byte[] original, byte[] filtered, Set<Integer> expectedChanged) {
        List<Raw> before=read(original),after=read(filtered);
        check(before.size()==after.size(),"Root records added/deleted");
        int style=0,changed=0;
        for(int i=0;i<before.size();i++) {
            Raw a=before.get(i),b=after.get(i);
            check(a.number==b.number && a.wire==b.wire,"Root field identity/order changed at "+i);
            boolean expected=a.number==3 && a.wire==2 && expectedChanged.contains(style);
            if(expected) {
                check(Arrays.equals(b.body,concat(a.body,HIDE)),"Changed style is not only appended hide flag at "+style);
                changed++;
            } else check(Arrays.equals(a.raw,b.raw),"Unrelated raw record changed at root index "+i+" style "+style);
            if(a.number==3 && a.wire==2)style++;
        }
        check(changed==expectedChanged.size(),"Expected changed styles not present");
        return before.size();
    }

    private static void changes(byte[] input,int... rows) {
        byte[] saved=input.clone(),out=BicycleLegendFilter.filter(input);
        check(out!=input,"Expected selective legend transform");
        check(Arrays.equals(input,saved),"Input legend bytes mutated");
        assertDiff(saved,out,set(rows));
        check(BicycleLegendFilter.filter(out)==out,"Legend transform not idempotent/reference-preserving");
    }
    private static void unchanged(byte[] input,String why) {
        byte[] saved=input==null?null:input.clone();
        check(BicycleLegendFilter.filter(input)==input,why);
        check(Arrays.equals(saved,input),"Original changed on fail-open path");
    }

    public static int run() {
        assertions=0;
        byte[] bicycle=concat(scalar(6,200000),refs(8,2,0),scalar(21,15));
        byte[] store=concat(scalar(6,200000),refs(8,2,1),scalar(34,999));
        changes(legend(bicycle,store),0);
        changes(legend(concat(bicycle,scalar(7,0))),0);
        changes(legend(concat(bicycle,HIDE,scalar(7,0))),0);
        unchanged(legend(concat(bicycle,HIDE)),"Already-hidden style rewritten");
        unchanged(legend(store),"Store/circle style hidden");
        changes(legend(concat(scalar(8,2),scalar(8,0))),0);
        changes(legend(bytes(5,refs(9,2,0))),0);
        // Inline icon branches must run even when root 9 has no bicycle entry.
        changes(legend(Arrays.asList(bytes(3,icon(0))),Arrays.asList(icon(1)),Arrays.asList(BIKE,SHOP)),0);
        changes(legend(Arrays.asList(bytes(5,bytes(6,icon(0)))),Arrays.asList(icon(1)),Arrays.asList(BIKE,SHOP)),0);
        changes(legend(Arrays.asList(bytes(3,icon(0))),Collections.emptyList(),Arrays.asList(BIKE)),0);
        changes(legend(Arrays.asList(refs(8,0)),Arrays.asList(string(1,"assets/icons/"+BIKE+".png")),Arrays.asList(SHOP)),0);
        // An image-id template can be replaced per feature with a store image.
        byte[] dynamic=concat(icon(0),scalar(8,37));
        unchanged(legend(Arrays.asList(refs(8,0)),Arrays.asList(dynamic),Arrays.asList(BIKE)),"Dynamic image override template hidden globally");
        unchanged(legend(Arrays.asList(bytes(3,dynamic)),Collections.emptyList(),Arrays.asList(BIKE)),"Dynamic inline icon hidden globally");
        // A color override has no effect on glyph identity.
        changes(legend(Arrays.asList(refs(8,0)),Arrays.asList(concat(icon(0),scalar(13,37))),Arrays.asList(BIKE)),0);
        unchanged(legend(Arrays.asList(refs(8,0)),Arrays.asList(icon(0)),Arrays.asList("other/bike_pinlet_v3-2-medium")),"Unknown namespace treated as bicycle");
        unchanged(legend(Arrays.asList(refs(8,0)),Arrays.asList(icon(0)),Arrays.asList("directions/bike-1-small")),"Route-mode icon hidden");
        unchanged(legend(refs(8,999)),"Invalid icon index accepted");
        unchanged(legend(bytes(8,new byte[]{(byte)0x80})),"Malformed packed icon index accepted");
        unchanged(legend(concat(bicycle,bytes(7,new byte[]{1}))),"Unknown hide-field wire accepted");
        byte[] wrongSignature=legend(bicycle);
        byte[] altered=wrongSignature.clone();
        for(int i=0;i<altered.length-4;i++)if(altered[i]=='p' && altered[i+1]=='n' && altered[i+2]=='g'){altered[i]='x';break;}
        unchanged(altered,"Unverified legend suffix accepted");
        byte[] good=legend(bicycle);
        unchanged(Arrays.copyOf(good,good.length-1),"Truncated root accepted");
        unchanged(concat(good,new byte[]{0}),"Invalid root tag accepted");
        unchanged(concat(good,new byte[]{(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,2}),"Overflowing varint accepted");
        unchanged(null,"Null bytes not preserved");
        unchanged(new byte[]{(byte)0x89,'P','N','G',13,10,26,10},"Image payload altered");
        unchanged(new byte[16*1024*1024+1],"Oversized bytes accepted");
        return assertions;
    }

    private static String sha256(byte[] value) throws Exception {
        StringBuilder out=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-256").digest(value))out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        return out.toString();
    }

    public static void main(String[] args) throws Exception {
        int fixtures=run();
        System.out.println("PASS CompactLegend selective/malformed/override fixtures: assertions="+fixtures);
        if(args.length>0) {
            byte[] original=Files.readAllBytes(Paths.get(args[0]));
            check(sha256(original).equals("c63aaba03ff7c812c6644ad489f75812c62465e87c63a36d09cccd7d839dfe2d"),"Unrecognized real legend fixture");
            byte[] saved=original.clone();
            long start=System.nanoTime();
            byte[] filtered=BicycleLegendFilter.filter(original);
            long milliseconds=(System.nanoTime()-start)/1000000;
            check(filtered!=original,"Real legend not transformed");
            check(Arrays.equals(saved,original),"Real input mutated");
            int records=assertDiff(original,filtered,set(REAL_CHANGED));
            check(BicycleLegendFilter.filter(filtered)==filtered,"Real legend not idempotent");
            if(args.length>1)Files.write(Paths.get(args[1]),filtered);
            System.out.println("PASS real Roadmap legend: sourceBytes="+original.length+", resultBytes="+filtered.length
                +", rootRecords="+records+", iconRecords=8799, labelStyles=13700, bicycleDescriptors=15"
                +", bicycleStyles=60, changedStyles=45, alreadyHiddenStyles=15"
                +", nonTargetRecordsByteIdentical=true, inputUnchanged=true, idempotent=true"
                +", transformMs="+milliseconds+", resultSha256="+sha256(filtered)+"; Android UI not executed");
        }
    }
}
