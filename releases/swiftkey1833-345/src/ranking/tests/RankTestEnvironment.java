package hiro.swiftkey;
import java.text.Normalizer;
import java.util.Locale;

/** Retained reading() model. Only known test romaji uses a tiny native encoder stand-in. */
final class RankTestEnvironment {
    static String reading(String input) {
        if (input == null || input.length()==0 || input.length()>128) return null;
        input=Normalizer.normalize(input,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        boolean latin=true;
        for (int i=0;i<input.length();i++) {
            char c=input.charAt(i);
            if (!((c>='a' && c<='z') || c=='\'' || c=='-')) { latin=false; break; }
        }
        if (latin) {
            if (input.equals("hashi")) input="はし";
            else if (input.equals("kikan")) input="きかん";
            else if (input.equals("ai")) input="あい";
            else return null;
        }
        if (input==null || input.length()<1 || input.length()>32) return null;
        StringBuilder result=new StringBuilder(input.length());
        for (int i=0;i<input.length();i++) {
            char c=input.charAt(i);
            if (c>=0x30a1 && c<=0x30f6) c=(char)(c-0x60);
            if (!(c>=0x3041 && c<=0x3096) && c!=0x30fc) return null;
            result.append(c);
        }
        return result.toString();
    }
}
