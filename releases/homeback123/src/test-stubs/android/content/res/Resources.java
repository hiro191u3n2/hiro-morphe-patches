package android.content.res;
public class Resources {public static final java.util.Map<Integer,String> IDS=new java.util.HashMap<Integer,String>();public String getResourceEntryName(int id){String s=IDS.get(id);if(s==null)throw new IllegalArgumentException();return s;}}
