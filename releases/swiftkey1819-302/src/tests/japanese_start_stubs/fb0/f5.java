package fb0;
import java.util.*;
public class f5 {
 public Iterable<?> b; public qj0.a current=qj0.a.OTHER; public int choices;
 public final Map<qj0.a,Set<?>> packs=new HashMap<qj0.a,Set<?>>();
 public Runnable onQuery,onSelect; public boolean queryThrows,selectThrows,reject;
 public f5(qj0.a... enabled){b=Arrays.asList(enabled);for(qj0.a a:enabled)packs.put(a,Collections.singleton(new Object()));}
 public qj0.a i(){return current;}
 public Set<?> k(qj0.a a){if(onQuery!=null)onQuery.run();if(queryThrows)throw new IllegalStateException();return packs.get(a);}
 public void q(qj0.a a){choices++;if(onSelect!=null)onSelect.run();if(selectThrows)throw new IllegalStateException();if(!reject)current=a;}
}
