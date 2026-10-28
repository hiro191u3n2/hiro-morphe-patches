import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.json.JSONArray;
import com.reandroid.json.JSONObject;

/** Compare emitted resource meaning, including the prior published patch output.
 * Morphe can repack the table and normalize some resource entry names. Only the
 * already-published baseline's exact names are accepted; all IDs/values/configs
 * and string/style pools must also agree with the original APK.
 */
public final class InstagramResourcesAudit {
    private static void check(boolean value,String why) {
        if(!value)throw new AssertionError(why);
    }
    private static TableBlock load(String path)throws Exception {
        try(ZipFile zip=new ZipFile(path);
            InputStream in=zip.getInputStream(zip.getEntry("resources.arsc"))) {
            return TableBlock.load(in);
        }
    }
    private static void bytes(Block a,Block b,String label) {
        check(Arrays.equals(a.getBytes(),b.getBytes()),label+" bytes changed");
    }
    private static boolean same(Object a,Object b) {
        if(a instanceof JSONObject&&b instanceof JSONObject) {
            JSONObject x=(JSONObject)a,y=(JSONObject)b;
            if(!x.keySet().equals(y.keySet()))return false;
            for(String key:x.keySet())if(!same(x.get(key),y.get(key)))return false;
            return true;
        }
        if(a instanceof JSONArray&&b instanceof JSONArray) {
            JSONArray x=(JSONArray)a,y=(JSONArray)b;
            if(x.length()!=y.length())return false;
            for(int i=0;i<x.length();i++)if(!same(x.get(i),y.get(i)))return false;
            return true;
        }
        return Objects.equals(a,b);
    }
    private static void pools(TableBlock a,TableBlock b) {
        check(a.size()==b.size(),"package count");
        bytes(a.getStringPool(),b.getStringPool(),"global string and style pool");
        for(int i=0;i<a.size();i++) {
            PackageBlock x=a.get(i),y=b.get(i);
            check(x.getId()==y.getId()&&x.getName().equals(y.getName()),"package identity");
            check(x.getHeaderBlock().getTypeIdOffsetItem().get()
                    ==y.getHeaderBlock().getTypeIdOffsetItem().get(),"type ID offset");
            bytes(x.getTypeStringPool(),y.getTypeStringPool(),"type string pool");
            bytes(x.getLibraryBlock(),y.getLibraryBlock(),"library table");
            bytes(x.getOverlayableList(),y.getOverlayableList(),"overlay metadata");
            bytes(x.getStagedAliasList(),y.getStagedAliasList(),"staged aliases");
        }
    }
    private static JSONObject canonical(PackageBlock block) {
        JSONObject json=block.toJson(true);
        // Resource type/configuration chunks and entry records are keyed, not
        // sequential values. Sort only these keyed collections; retain every
        // value-array ordering and reject duplicate IDs/configurations.
        JSONArray specs=json.getJSONArray("specs");
        for(Object object:specs) {
            JSONArray types=((JSONObject)object).getJSONArray("types");
            Set<String> configs=new HashSet<>();
            for(Object typeObject:types) {
                JSONObject type=(JSONObject)typeObject;
                check(configs.add(type.getInt("id")+"/"+type.get("config").toString()),
                        "duplicate resource type/configuration");
                JSONArray entries=type.getJSONArray("entries");Set<Integer> ids=new HashSet<>();
                for(Object entry:entries)check(ids.add(((JSONObject)entry).getInt("id")),"duplicate resource entry ID");
                entries.sort((a,b)->Integer.compare(((JSONObject)a).getInt("id"),((JSONObject)b).getInt("id")));
            }
            types.sort((a,b)->{
                JSONObject x=(JSONObject)a,y=(JSONObject)b;
                int cmp=Integer.compare(x.getInt("id"),y.getInt("id"));
                return cmp!=0?cmp:x.get("config").toString().compareTo(y.get("config").toString());
            });
        }
        return json;
    }
    public static void main(String[] args)throws Exception {
        check(args.length==3,"ORIGINAL_APK BASELINE_PATCHED_APK CURRENT_PATCHED_APK");
        TableBlock original=load(args[0]),baseline=load(args[1]),current=load(args[2]);
        check(original.size()==1,"pinned Instagram package inventory");
        pools(original,baseline);pools(original,current);
        int entries=0,normalized=0,strings=0,drawables=0;
        for(int p=0;p<original.size();p++) {
            JSONObject before=canonical(original.get(p));
            JSONObject prior=canonical(baseline.get(p));
            JSONObject after=canonical(current.get(p));
            check(same(prior,after),"resource semantics differ from published baseline");
            int packageId=before.getInt("package_id");
            JSONArray oldSpecs=before.getJSONArray("specs"),newSpecs=after.getJSONArray("specs");
            check(oldSpecs.length()==newSpecs.length(),"resource spec count");
            for(int s=0;s<oldSpecs.length();s++) {
                JSONArray oldTypes=oldSpecs.getJSONObject(s).getJSONArray("types");
                JSONArray newTypes=newSpecs.getJSONObject(s).getJSONArray("types");
                check(oldTypes.length()==newTypes.length(),"resource configuration count");
                for(int t=0;t<oldTypes.length();t++) {
                    JSONObject oldType=oldTypes.getJSONObject(t),newType=newTypes.getJSONObject(t);
                    int typeId=oldType.getInt("id");String type=oldType.getString("name");
                    JSONArray oldEntries=oldType.getJSONArray("entries"),newEntries=newType.getJSONArray("entries");
                    check(oldEntries.length()==newEntries.length(),"resource entry count");
                    for(int e=0;e<oldEntries.length();e++) {
                        JSONObject oldEntry=oldEntries.getJSONObject(e),newEntry=newEntries.getJSONObject(e);
                        entries++;
                        String oldName=oldEntry.getString("entry_name"),newName=newEntry.getString("entry_name");
                        if(oldName.equals(newName))continue;
                        int id=(packageId<<24)|(typeId<<16)|oldEntry.getInt("id");
                        check(("string".equals(type)||"drawable".equals(type))
                                &&newName.equals(String.format(Locale.ROOT,"%s_0x%08x",type,id)),
                                "unexpected resource name normalization: "+type+"/"+Integer.toHexString(id)+"/"+newName);
                        if("string".equals(type))strings++;else drawables++;
                        normalized++;newEntry.put("entry_name",oldName);
                    }
                }
            }
            check(same(before,after),"resource ID/type/flag/configuration/value changed");
        }
        check(entries==57543&&normalized==18503&&strings==18389&&drawables==114,
                "pinned original/baseline normalization inventory");
        System.out.println("PASS InstagramResourcesAudit entries="+entries+
                "; IDs/types/flags/configurations/values and string/style pools retained"+
                "; baseline semantics identical; existing SDK name normalization="+normalized+
                " (string="+strings+", drawable="+drawables+"); id names unchanged");
    }
}
