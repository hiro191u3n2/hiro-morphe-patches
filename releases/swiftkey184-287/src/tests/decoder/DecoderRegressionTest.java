import hiro.swiftkey.JapaneseGlideDecoder;
import java.io.*;
import java.util.*;

/** Coordinates replay the archived target 9.13.16.4 layout resources/native arithmetic. */
public final class DecoderRegressionTest {
    static int checks;
    static final int[] CODES = new int[26];
    static { for (int i=0;i<26;i++) CODES[i]='a'+i; }
    static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    static final String[] NAMES={"standard","pc","split"};
    static final float[][] LAYOUTS={
        {0.100000003f,0.375f,0.600000024f,0.625f,0.399999991f,0.625f,0.299999997f,0.375f,0.250000007f,0.125f,0.399999991f,0.375f,0.5f,0.375f,0.600000024f,0.375f,0.75000006f,0.125f,0.700000048f,0.375f,0.800000072f,0.375f,0.900000095f,0.375f,0.800000072f,0.625f,0.700000048f,0.625f,0.850000083f,0.125f,0.950000107f,0.125f,0.0500000007f,0.125f,0.350000009f,0.125f,0.200000003f,0.375f,0.450000003f,0.125f,0.650000036f,0.125f,0.5f,0.625f,0.150000002f,0.125f,0.299999997f,0.625f,0.550000012f,0.125f,0.200000003f,0.625f}, // standard
        {0.0734999962f,0.375f,0.478499949f,0.625f,0.304499969f,0.625f,0.24749998f,0.375f,0.217499979f,0.125000003f,0.33449997f,0.375f,0.421499953f,0.375f,0.508499935f,0.375f,0.652499974f,0.125000003f,0.595499933f,0.375f,0.682499945f,0.375f,0.769499958f,0.375f,0.652499974f,0.625f,0.565499961f,0.625f,0.739499986f,0.125000003f,0.826499999f,0.125000003f,0.0434999962f,0.125000003f,0.304499969f,0.125000003f,0.160499986f,0.375f,0.391499951f,0.125000003f,0.565499961f,0.125000003f,0.391499951f,0.625f,0.130499985f,0.125000003f,0.217499979f,0.625f,0.478499949f,0.125000003f,0.130499985f,0.625f}, // pc
        {0.0659999996f,0.375f,0.735999972f,0.625f,0.263999991f,0.625f,0.197999991f,0.375f,0.164999999f,0.125f,0.263999991f,0.375f,0.329999983f,0.375f,0.735999972f,0.375f,0.835000038f,0.125f,0.802000016f,0.375f,0.868000031f,0.375f,0.934000015f,0.375f,0.868000031f,0.625f,0.802000016f,0.625f,0.901000023f,0.125f,0.967000008f,0.125f,0.0329999998f,0.125f,0.230999999f,0.125f,0.131999996f,0.375f,0.296999991f,0.125f,0.769000024f,0.125f,0.329999983f,0.625f,0.0989999995f,0.125f,0.197999991f,0.625f,0.702999979f,0.125f,0.131999992f,0.625f}, // split
    };
    static float[] path(String word,float[] centers,int steps,float drift,float jitter) {
        ArrayList<Float> p=new ArrayList<Float>();
        if(word.length()==1) return new float[]{centers[(word.charAt(0)-'a')*2],centers[(word.charAt(0)-'a')*2+1]};
        for(int i=0;i<word.length()-1;i++) {
            int a=word.charAt(i)-'a', b=word.charAt(i+1)-'a';
            for(int s=0;s<steps;s++) {
                float t=s/(float)steps;
                float noise=(float)Math.sin((i*steps+s)*2.1)*jitter;
                p.add(centers[2*a]*(1-t)+centers[2*b]*t+drift+noise);
                p.add(centers[2*a+1]*(1-t)+centers[2*b+1]*t+drift*.5f-noise*.5f);
            }
        }
        int last=word.charAt(word.length()-1)-'a';
        p.add(centers[last*2]+drift); p.add(centers[last*2+1]+drift*.5f);
        float[] r=new float[p.size()];for(int i=0;i<r.length;i++)r[i]=p.get(i);return r;
    }
    static boolean has(JapaneseGlideDecoder.Result[] candidates,String reading) {
        for(JapaneseGlideDecoder.Result r:candidates) if(r.hiragana.equals(reading)&&!r.fallback)return true;
        return false;
    }
    static String list(JapaneseGlideDecoder.Result[] candidates) {
        StringBuilder b=new StringBuilder();for(JapaneseGlideDecoder.Result r:candidates)b.append(r.romaji).append('=').append(r.hiragana).append(':').append(r.score).append(r.fallback?"?":"").append(',');return b.toString();
    }
    static final String[][] WORDS={
        {"arigatou","ありがとう"},{"ohayou","おはよう"},{"konnnichiha","こんにちは"},{"watashi","わたし"},{"nihonn","にほん"},
        {"toukyou","とうきょう"},{"tyoukyou","ちょうきょう"},{"neko","ねこ"},{"nekko","ねっこ"},{"kata","かた"},{"katta","かった"},
        {"gakou","がこう"},{"gakkou","がっこう"},{"saka","さか"},{"sakka","さっか"},{"kita","きた"},{"kiita","きいた"},{"kitta","きった"},
        {"hana","はな"},{"hanna","はんな"},{"sushi","すし"},{"susi","すし"},{"inu","いぬ"},{"yume","ゆめ"},{"kuruma","くるま"},
        {"densha","でんしゃ"},{"dennsha","でんしゃ"},{"sora","そら"},{"asa","あさ"},{"yoru","よる"},{"ai","あい"},{"ie","いえ"},{"ii","いい"},{"e","え"},
        {"koko","ここ"},{"haha","はは"},{"anata","あなた"},{"kore","これ"},{"koe","こえ"},{"kote","こて"},{"sore","それ"},{"are","あれ"},
        {"hai","はい"},{"iie","いいえ"},{"suki","すき"},{"taberu","たべる"},{"okashi","おかし"},{"okayu","おかゆ"},{"yokohama","よこはま"},
        {"ohayougozaimasu","おはようございます"},{"arigatougozaimasu","ありがとうございます"},
        {"kaisha","かいしゃ"},{"kaigi","かいぎ"},{"ginkou","ぎんこう"},{"hikouki","ひこうき"},{"sinnbunn","しんぶん"}
    };
    public static void main(String[] args) throws Exception {
        check(JapaneseGlideDecoder.decodeCandidates(new float[]{0,0},LAYOUTS[0],CODES).length==0,"unloaded safe");
        long start=System.nanoTime();
        try(InputStream in=new FileInputStream(args[0])) { JapaneseGlideDecoder.load(in); }
        check(JapaneseGlideDecoder.isReady(),"dictionary ready");
        System.out.printf(Locale.ROOT,"LOAD %.2f ms%n",(System.nanoTime()-start)/1e6);
        double worst=0,total=0;int calls=0;
        for(int layout=0;layout<LAYOUTS.length;layout++) for(int variant=0;variant<3;variant++) for(String[] word:WORDS) {
            float[] centers=LAYOUTS[layout];
            float[] trace=path(word[0],centers,variant==2?2:9,variant==1?.012f:0,variant==1?.007f:0);
            long before=System.nanoTime();
            JapaneseGlideDecoder.Result[] r=JapaneseGlideDecoder.decodeCandidates(trace,centers,CODES);
            double ms=(System.nanoTime()-before)/1e6;worst=Math.max(worst,ms);total+=ms;calls++;
            check(has(r,word[1]),NAMES[layout]+" variant="+variant+" word="+word[0]+" expected="+word[1]+" got="+list(r));
            check(r.length<=12,"bounded candidates");
            Set<String> seen=new HashSet<String>();
            for(JapaneseGlideDecoder.Result v:r) {check(seen.add(v.hiragana),"unique reading");check(v.scanned<=150000,"bounded rows");}
        }
        for(String word:new String[]{"neko","gakou","toukyou","kore"}) {
            float[] trace=path(word,LAYOUTS[0],9,0,0);
            check(JapaneseGlideDecoder.decode(trace,LAYOUTS[0],CODES)==null,"ambiguous not auto-picked "+word);
            String first=list(JapaneseGlideDecoder.decodeCandidates(trace,LAYOUTS[0],CODES));
            check(first.equals(list(JapaneseGlideDecoder.decodeCandidates(trace,LAYOUTS[0],CODES))),"deterministic "+word);
        }
        float[] c=LAYOUTS[0].clone();c[0]=Float.NaN;
        check(JapaneseGlideDecoder.decodeCandidates(path("neko",LAYOUTS[0],3,0,0),c,CODES).length==0,"bad geometry");
        check(JapaneseGlideDecoder.decodeCandidates(new float[]{Float.NaN,0},LAYOUTS[0],CODES).length==0,"bad trace");
        check(JapaneseGlideDecoder.decodeCandidates(new float[4098],LAYOUTS[0],CODES).length==0,"trace bound");
        check(JapaneseGlideDecoder.decodeCandidates(new float[]{0,0,1},LAYOUTS[0],CODES).length==0,"odd coordinates");
        float[] novel=path("komemame",LAYOUTS[0],9,0,0);
        JapaneseGlideDecoder.Result[] guesses=JapaneseGlideDecoder.decodeCandidates(novel,LAYOUTS[0],CODES);
        boolean fallback=false;
        for(JapaneseGlideDecoder.Result r:guesses) if(r.hiragana.equals("こめまめ")) fallback=r.fallback;
        check(fallback,"novel reading explicitly marked as a guess");
        check(JapaneseGlideDecoder.decode(novel,LAYOUTS[0],CODES)==null,"novel reading never automatically selected");
        float[] translated=LAYOUTS[0].clone();
        for(int i=0;i<translated.length;i++) translated[i]=translated[i]*800f+(i%2==0?120f:75f);
        check(has(JapaneseGlideDecoder.decodeCandidates(path("gakkou",translated,9,0,0),translated,CODES),"がっこう"),"translated and scaled coordinates");
        System.out.printf(Locale.ROOT,"TIMING calls=%d mean=%.3f ms worst=%.3f ms; host timings, not Android latency%n",calls,total/calls,worst);
        System.out.println("PASSED "+checks+" decoder assertions");
    }
}
