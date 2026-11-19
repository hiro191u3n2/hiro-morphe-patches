package hiro;
import java.util.*;
import com.x.navigation.*;
public class Probe {
    static List tab(Map args){throw new AssertionError();}
    static List detail(Map args){throw new AssertionError();}
    public static void main(String[] args) {
        for(String value:new String[]{"1","4294967297","2012345678901234567","9223372036854775807","9223372036854775808","x",null}){
            Map<String,String> path=new HashMap<>();path.put("postId",value);
            for(List result:new List[]{tab(path),detail(path)}){
                Long expected=kotlin.text.j.O(10,value);
                if(expected==null){if(result!=null)throw new AssertionError("invalid ID");continue;}
                if(result.size()!=1)throw new AssertionError("destination count");
                bm destination=(bm)result.get(0);
                if(destination.b!=expected || destination.c || destination.d!=hm.None)throw new AssertionError("destination");
            }
        }
        System.out.println("PASS 14 compiled X route and PostDetail constructor cases on ART");
    }
}
