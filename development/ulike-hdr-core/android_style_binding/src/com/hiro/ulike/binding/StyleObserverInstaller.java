package com.hiro.ulike.binding;
import java.io.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;import java.util.zip.*;

/** Android-compatible private one-still installer. Reads only caller-owned
 * pinned archives. Our own observation appendices are bundled; vendor scripts,
 * textures, model bytes and executable libraries are not bundled.
 * A NEW owned SDK instance must load this fresh directory for exactly one still
 * nonce. Active preview and repeated submissions must never use this copy.
 */
public final class StyleObserverInstaller {
 private StyleObserverInstaller(){}
 public static final class Installation {
  public final File directory;public final int nonce,copiedFiles;public final String styleId;
  public final Set<String> expectedFeatures;
  public final boolean actualDeviceDeliveryVerified=false,completeStyleBinding=false;
  private Installation(File f,int n,int count,String style){directory=f;nonce=n;copiedFiles=count;styleId=style;expectedFeatures=ObservedStyleFrame.expectedFeatures(style);}
 }
 private static void require(boolean b,String s){if(!b)throw new IllegalArgumentException(s);}
 private static String hex(byte[] hash){StringBuilder s=new StringBuilder();for(byte b:hash){s.append(Character.forDigit((b&255)>>>4,16));s.append(Character.forDigit(b&15,16));}return s.toString();}
 private static String sha(byte[] bytes)throws Exception{return hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
 private static byte[] ownedArchive(File archive)throws IOException{long size=archive.length();require(size>=0&&size<=32L*1024*1024,"archive size budget");byte[] snapshot=new byte[(int)size];try(DataInputStream in=new DataInputStream(new FileInputStream(archive))){in.readFully(snapshot);require(in.read()==-1,"archive size changed while reading");}return snapshot;}
 private static byte[] entryBytes(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int total=0,n;while((n=in.read(chunk))!=-1){require(n<=4*1024*1024-total,"entry read budget");out.write(chunk,0,n);total+=n;}return out.toByteArray();}
 private static byte[] bounded(InputStream in,int expected)throws IOException{require(expected>=0&&expected<=4*1024*1024,"entry length budget");byte[] out=new byte[expected];int pos=0;while(pos<out.length){int n=in.read(out,pos,out.length-pos);if(n<0)throw new EOFException();if(n==0)continue;pos+=n;}require(in.read()==-1,"entry length changed");return out;}
 private static void write(File f,byte[] bytes)throws IOException{try(OutputStream out=new FileOutputStream(f)){out.write(bytes);}}
 private static void removeCreated(File f)throws IOException{if(f.isDirectory()){File[] children=f.listFiles();if(children==null)throw new IOException("cannot list private observer cleanup");for(File child:children)removeCreated(child);}if(!f.delete())throw new IOException("cannot remove private observer staging");}
 /** privateParent must be the caller app's private storage directory. The new
  * child must not exist. This module never deletes or patches existing effects.
  */
 public static Installation install(File archive,String styleId,File privateParent,String newChildName,int nonce)throws Exception{
  require(archive!=null&&archive.isFile()&&archive.length()<=32L*1024*1024&&privateParent!=null&&privateParent.isDirectory()&&newChildName!=null&&newChildName.matches("[A-Za-z0-9_-]{1,64}")&&nonce>0,"bounded archive, private parent and new positive nonce required");
  boolean natural=ShotStyleSettings.NATURAL.equals(styleId);require(natural||ShotStyleSettings.PURITY.equals(styleId),"unsupported style");
  String expected=natural?"5b50343dcedc7e9aadd218626e621ad683cd67372d225024f118b47f493546f0":"cd5da20fefe1f9bd594e4e0d055d6320392fa54fb5df9d34b8f159c791c5b117";
  byte[] archiveSnapshot=ownedArchive(archive);require(sha(archiveSnapshot).equals(expected),"style archive SHA-256 mismatch");
  File parent=privateParent.getCanonicalFile(),output=new File(parent,newChildName);require(!output.exists()&&output.getCanonicalFile().getParentFile().equals(parent),"new private observer child required");
  require(output.mkdir(),"cannot create private observer child");int copied=0,total=0;
  try{
   try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(archiveSnapshot))){Set<String> names=new HashSet<>();int all=0;ZipEntry entry;
    while((entry=zip.getNextEntry())!=null){String name=entry.getName();require(++all<=10000&&names.add(name),"archive member count/duplicate");if(!name.startsWith("materials/016/"))continue;
     String relative=name.substring(14);require(!relative.isEmpty()&&!relative.startsWith("/")&&!relative.contains("\\")&&!entry.isDirectory(),"selected member path");for(String part:relative.split("/",-1))require(!part.isEmpty()&&!part.equals(".")&&!part.equals(".."),"selected member path segment");
     require(entry.getSize()<=4*1024*1024&&copied<512,"selected entry budget");
     File destination=new File(output,relative);require(destination.getCanonicalPath().startsWith(output.getCanonicalPath()+File.separator),"selected member escape");File folder=destination.getParentFile();require(folder.isDirectory()||folder.mkdirs(),"private member directory");byte[] bytes=entryBytes(zip);require(total+bytes.length<=32L*1024*1024,"selected total byte budget");write(destination,bytes);copied++;total+=bytes.length;
    }
   }
   require(copied==(natural?45:143),"selected archive member count changed");
   for(String[] pin:natural?NATURAL:PURITY){File script=new File(output,pin[0]);byte[] bytes;try(InputStream in=new FileInputStream(script)){bytes=bounded(in,(int)script.length());}require(sha(bytes).equals(pin[3]),"selected script SHA-256 mismatch");String source=new String(bytes,StandardCharsets.UTF_8),marker="exports."+pin[1]+" = "+pin[1];int at=source.indexOf(marker);require(at>=0&&source.indexOf(marker,at+marker.length())<0,"script insertion boundary");String feature=pin[0].substring(0,pin[0].indexOf('/'));String updated=source.substring(0,at)+appendix(pin[2],pin[1],feature,nonce)+"\n"+source.substring(at);write(script,updated.getBytes(StandardCharsets.UTF_8));}
   return new Installation(output,nonce,copied,styleId);
  }catch(Exception|Error failure){try{removeCreated(output);}catch(Exception cleanup){failure.addSuppressed(cleanup);}throw failure;}
 }
 private static final String[][] NATURAL={
  {"AmazingFeature1/lua/EffectFaceMakeupSystemScript.lua","EffectFaceMakeupSystemScript","uniform","2744310a943847735bba3a1c807dcc59cb71ed800d1870a6ba1c227432369df4"},
  {"AmazingFeature0/lua/SeekModeScript.lua","SeekModeScript","natural_neural","370dfeec7dee30b418d4b5b5d554604d97e8d28becdf1bd176f551badb474983"},
  {"AmazingFeature2/ComposerUpdate.lua","ComposerUpdate","lut","8dd28466018ef89e2e73f514e00131787afda8bc184c961c03feef8b96f83136"},
 };
 private static final String[][] PURITY={
  {"AmazingFeature1/lua/EffectFaceMakeupSystemScript.lua","EffectFaceMakeupSystemScript","uniform","caef5ae79e2cb94304beb2a22eed450b882b8724bfb3625b4d70554655475d4b"},
  {"AmazingFeature3/lua/EffectFaceMakeupSystemScript.lua","EffectFaceMakeupSystemScript","uniform","caef5ae79e2cb94304beb2a22eed450b882b8724bfb3625b4d70554655475d4b"},
  {"AmazingFeature5/lua/EffectFaceMakeupSystemScript.lua","EffectFaceMakeupSystemScript","uniform","caef5ae79e2cb94304beb2a22eed450b882b8724bfb3625b4d70554655475d4b"},
  {"AmazingFeature6/lua/EffectFaceMakeupSystemScript.lua","EffectFaceMakeupSystemScript","uniform","72e71f471eb030784cb78c2538a32bedb8c40067b674230aa7abadf2c012a4be"},
  {"AmazingFeature7/lua/EffectFaceMakeupSystemScript.lua","EffectFaceMakeupSystemScript","uniform","caef5ae79e2cb94304beb2a22eed450b882b8724bfb3625b4d70554655475d4b"},
  {"3dmakeup4/lua/Face3DSystem.lua","Face3DSystem","mesh3d","f3eb6e2ffeaaebe9e88bdb711e4bee3d4abac3ec02254113150696c9d78e017d"},
  {"AmazingFeature0/lua/LaughGanScript.lua","LaughGanScript","purity_neural","e40999fd1a8828f8073830adaa6c5074363ad2a76099f18c5ffc5f7405722b82"},
  {"AmazingFeature8/ComposerUpdate.lua","ComposerUpdate","lut","8dd28466018ef89e2e73f514e00131787afda8bc184c961c03feef8b96f83136"},
  {"AmazingFeature9/ComposerUpdate.lua","ComposerUpdate","lut","8dd28466018ef89e2e73f514e00131787afda8bc184c961c03feef8b96f83136"},
 };
 private static String appendix(String kind,String klass,String feature,int nonce){String template;switch(kind){
 case "uniform":template=
  "\n" +
  "-- BEGIN Hiro same-still observation appendix; this is not vendor code.\n" +
  "local __hiro_nonce = HIRONONCE\n" +
  "local __hiro_feature = \"HIROFEATURE\"\n" +
  "local __hiro_sequence = 0\n" +
  "local __hiro_done = false\n" +
  "local __hiro_active = false\n" +
  "local function __hiro_number(value)\n" +
  "    if type(value) ~= \"number\" or value ~= value or value == math.huge or value == -math.huge then\n" +
  "        error(\"nonfinite observation\")\n" +
  "    end\n" +
  "    return string.format(\"%.17g\", value)\n" +
  "end\n" +
  "local function __hiro_safe(value)\n" +
  "    if type(value) ~= \"string\" or #value > 128 or string.find(value, \"[^%w_%-]\") then\n" +
  "        error(\"invalid component identifier\")\n" +
  "    end\n" +
  "    return value\n" +
  "end\n" +
  "local function __hiro_emit(kind, face, payload)\n" +
  "    if #payload > 7000 then error(\"observation packet too large\") end\n" +
  "    __hiro_sequence = __hiro_sequence + 1\n" +
  "    Amaz.MessageCenter.sendMessage(1431065345, __hiro_nonce, __hiro_sequence,\n" +
  "        \"S1|\" .. __hiro_feature .. \"|\" .. kind .. \"|\" .. tostring(face) .. \"|\" .. payload)\n" +
  "end\n" +
  "local function __hiro_uniform(component, face, key, value)\n" +
  "    __hiro_emit(\"uniform\", face, __hiro_safe(component) .. \",\" .. key .. \",\" .. __hiro_number(value))\n" +
  "end\n" +
  "local function __hiro_matrix(face, m)\n" +
  "    local numbers = {}\n" +
  "    for row=0,3 do\n" +
  "        local v = m:GetRow(row)\n" +
  "        table.insert(numbers,__hiro_number(v.x));table.insert(numbers,__hiro_number(v.y))\n" +
  "        table.insert(numbers,__hiro_number(v.z));table.insert(numbers,__hiro_number(v.w))\n" +
  "    end\n" +
  "    __hiro_emit(\"mvp\",face,table.concat(numbers,\",\"))\n" +
  "end\n" +
  "local __hiro_original_update = HIROCLASS.onUpdate\n" +
  "\n" +
  "local __hiro_original_opacity = HIROCLASS._setOpacity\n" +
  "function HIROCLASS:_setOpacity(component,face,opacity)\n" +
  "    __hiro_original_opacity(self,component,face,opacity)\n" +
  "    if __hiro_active then\n" +
  "        __hiro_uniform(component.entity.name,face,\"intensity\",opacity)\n" +
  "    end\n" +
  "end\n" +
  "\n" +
  "function HIROCLASS:onUpdate(context,deltaTime)\n" +
  "    if __hiro_done then\n" +
  "        if __hiro_original_update then __hiro_original_update(self,context,deltaTime) end\n" +
  "        return\n" +
  "    end\n" +
  "    __hiro_active=true\n" +
  "    if __hiro_original_update then __hiro_original_update(self,context,deltaTime) end\n" +
  "    __hiro_active=false\n" +
  "    __hiro_done=true\n" +
  "    local ok = pcall(function()\n" +
  "\n" +
  "        -- These are post-setFaceUniform values. They are not a GL readback.\n" +
  "        -- A 2D native FaceMakeup vertex/segmentation observer is still required.\n" +
  "\n" +
  "    end)\n" +
  "    if ok then\n" +
  "        __hiro_emit(\"END\",-1,tostring(__hiro_sequence))\n" +
  "    else\n" +
  "        __hiro_emit(\"ERROR\",-1,\"observation_failed\")\n" +
  "    end\n" +
  "end\n" +
  "-- END Hiro same-still observation appendix.\n";break;
 case "mesh3d":template=
  "\n" +
  "-- BEGIN Hiro same-still observation appendix; this is not vendor code.\n" +
  "local __hiro_nonce = HIRONONCE\n" +
  "local __hiro_feature = \"HIROFEATURE\"\n" +
  "local __hiro_sequence = 0\n" +
  "local __hiro_done = false\n" +
  "local __hiro_active = false\n" +
  "local function __hiro_number(value)\n" +
  "    if type(value) ~= \"number\" or value ~= value or value == math.huge or value == -math.huge then\n" +
  "        error(\"nonfinite observation\")\n" +
  "    end\n" +
  "    return string.format(\"%.17g\", value)\n" +
  "end\n" +
  "local function __hiro_safe(value)\n" +
  "    if type(value) ~= \"string\" or #value > 128 or string.find(value, \"[^%w_%-]\") then\n" +
  "        error(\"invalid component identifier\")\n" +
  "    end\n" +
  "    return value\n" +
  "end\n" +
  "local function __hiro_emit(kind, face, payload)\n" +
  "    if #payload > 7000 then error(\"observation packet too large\") end\n" +
  "    __hiro_sequence = __hiro_sequence + 1\n" +
  "    Amaz.MessageCenter.sendMessage(1431065345, __hiro_nonce, __hiro_sequence,\n" +
  "        \"S1|\" .. __hiro_feature .. \"|\" .. kind .. \"|\" .. tostring(face) .. \"|\" .. payload)\n" +
  "end\n" +
  "local function __hiro_uniform(component, face, key, value)\n" +
  "    __hiro_emit(\"uniform\", face, __hiro_safe(component) .. \",\" .. key .. \",\" .. __hiro_number(value))\n" +
  "end\n" +
  "local function __hiro_matrix(face, m)\n" +
  "    local numbers = {}\n" +
  "    for row=0,3 do\n" +
  "        local v = m:GetRow(row)\n" +
  "        table.insert(numbers,__hiro_number(v.x));table.insert(numbers,__hiro_number(v.y))\n" +
  "        table.insert(numbers,__hiro_number(v.z));table.insert(numbers,__hiro_number(v.w))\n" +
  "    end\n" +
  "    __hiro_emit(\"mvp\",face,table.concat(numbers,\",\"))\n" +
  "end\n" +
  "local __hiro_original_update = HIROCLASS.onUpdate\n" +
  "\n" +
  "function HIROCLASS:onUpdate(context,deltaTime)\n" +
  "    if __hiro_done then\n" +
  "        if __hiro_original_update then __hiro_original_update(self,context,deltaTime) end\n" +
  "        return\n" +
  "    end\n" +
  "    __hiro_active=true\n" +
  "    if __hiro_original_update then __hiro_original_update(self,context,deltaTime) end\n" +
  "    __hiro_active=false\n" +
  "    __hiro_done=true\n" +
  "    local ok = pcall(function()\n" +
  "\n" +
  "        local result = Amaz.Algorithm.getAEAlgorithmResult()\n" +
  "        local count = math.min(#self.faceEntity,result:getFaceCount())\n" +
  "        for slot=1,count do\n" +
  "            if self.faceEntity[slot].visible then\n" +
  "                local info = result:getFaceMeshInfo(slot-1)\n" +
  "                if info == nil then error(\"missing same-still face mesh\") end\n" +
  "                local vertices = info.vertexes\n" +
  "                local total = vertices:size()\n" +
  "                if total ~= 1427 then error(\"unexpected pinned 3D mesh vertex count\") end\n" +
  "                __hiro_matrix(slot-1,info.mvp)\n" +
  "                for first=0,total-1,64 do\n" +
  "                    local numbers = {};local amount=math.min(64,total-first)\n" +
  "                    for i=first,first+amount-1 do\n" +
  "                        local v=vertices:get(i)\n" +
  "                        table.insert(numbers,__hiro_number(v.x));table.insert(numbers,__hiro_number(v.y));table.insert(numbers,__hiro_number(v.z))\n" +
  "                    end\n" +
  "                    __hiro_emit(\"vertices\",slot-1,tostring(first)..\",\"..tostring(amount)..\",\"..tostring(total)..\";\"..table.concat(numbers,\",\"))\n" +
  "                end\n" +
  "                local material=self.faceComp[slot].sharedMaterials:get(0)\n" +
  "                __hiro_uniform(self.faceEntity[slot].name,slot-1,\"intensity\",material.properties:getFloat(\"intensity\"))\n" +
  "            end\n" +
  "        end\n" +
  "\n" +
  "    end)\n" +
  "    if ok then\n" +
  "        __hiro_emit(\"END\",-1,tostring(__hiro_sequence))\n" +
  "    else\n" +
  "        __hiro_emit(\"ERROR\",-1,\"observation_failed\")\n" +
  "    end\n" +
  "end\n" +
  "-- END Hiro same-still observation appendix.\n";break;
 case "natural_neural":template=
  "\n" +
  "-- BEGIN Hiro same-still observation appendix; this is not vendor code.\n" +
  "local __hiro_nonce = HIRONONCE\n" +
  "local __hiro_feature = \"HIROFEATURE\"\n" +
  "local __hiro_sequence = 0\n" +
  "local __hiro_done = false\n" +
  "local __hiro_active = false\n" +
  "local function __hiro_number(value)\n" +
  "    if type(value) ~= \"number\" or value ~= value or value == math.huge or value == -math.huge then\n" +
  "        error(\"nonfinite observation\")\n" +
  "    end\n" +
  "    return string.format(\"%.17g\", value)\n" +
  "end\n" +
  "local function __hiro_safe(value)\n" +
  "    if type(value) ~= \"string\" or #value > 128 or string.find(value, \"[^%w_%-]\") then\n" +
  "        error(\"invalid component identifier\")\n" +
  "    end\n" +
  "    return value\n" +
  "end\n" +
  "local function __hiro_emit(kind, face, payload)\n" +
  "    if #payload > 7000 then error(\"observation packet too large\") end\n" +
  "    __hiro_sequence = __hiro_sequence + 1\n" +
  "    Amaz.MessageCenter.sendMessage(1431065345, __hiro_nonce, __hiro_sequence,\n" +
  "        \"S1|\" .. __hiro_feature .. \"|\" .. kind .. \"|\" .. tostring(face) .. \"|\" .. payload)\n" +
  "end\n" +
  "local function __hiro_uniform(component, face, key, value)\n" +
  "    __hiro_emit(\"uniform\", face, __hiro_safe(component) .. \",\" .. key .. \",\" .. __hiro_number(value))\n" +
  "end\n" +
  "local function __hiro_matrix(face, m)\n" +
  "    local numbers = {}\n" +
  "    for row=0,3 do\n" +
  "        local v = m:GetRow(row)\n" +
  "        table.insert(numbers,__hiro_number(v.x));table.insert(numbers,__hiro_number(v.y))\n" +
  "        table.insert(numbers,__hiro_number(v.z));table.insert(numbers,__hiro_number(v.w))\n" +
  "    end\n" +
  "    __hiro_emit(\"mvp\",face,table.concat(numbers,\",\"))\n" +
  "end\n" +
  "local __hiro_original_update = HIROCLASS.onUpdate\n" +
  "\n" +
  "function HIROCLASS:onUpdate(context,deltaTime)\n" +
  "    if __hiro_done then\n" +
  "        if __hiro_original_update then __hiro_original_update(self,context,deltaTime) end\n" +
  "        return\n" +
  "    end\n" +
  "    __hiro_active=true\n" +
  "    if __hiro_original_update then __hiro_original_update(self,context,deltaTime) end\n" +
  "    __hiro_active=false\n" +
  "    __hiro_done=true\n" +
  "    local ok = pcall(function()\n" +
  "__hiro_uniform(\"Entity\",-1,\"intensity\",self.pass4Material.properties:getFloat(\"intensity\"))\n" +
  "\n" +
  "    end)\n" +
  "    if ok then\n" +
  "        __hiro_emit(\"END\",-1,tostring(__hiro_sequence))\n" +
  "    else\n" +
  "        __hiro_emit(\"ERROR\",-1,\"observation_failed\")\n" +
  "    end\n" +
  "end\n" +
  "-- END Hiro same-still observation appendix.\n";break;
 case "purity_neural":template=
  "\n" +
  "-- BEGIN Hiro same-still observation appendix; this is not vendor code.\n" +
  "local __hiro_nonce = HIRONONCE\n" +
  "local __hiro_feature = \"HIROFEATURE\"\n" +
  "local __hiro_sequence = 0\n" +
  "local __hiro_done = false\n" +
  "local __hiro_active = false\n" +
  "local function __hiro_number(value)\n" +
  "    if type(value) ~= \"number\" or value ~= value or value == math.huge or value == -math.huge then\n" +
  "        error(\"nonfinite observation\")\n" +
  "    end\n" +
  "    return string.format(\"%.17g\", value)\n" +
  "end\n" +
  "local function __hiro_safe(value)\n" +
  "    if type(value) ~= \"string\" or #value > 128 or string.find(value, \"[^%w_%-]\") then\n" +
  "        error(\"invalid component identifier\")\n" +
  "    end\n" +
  "    return value\n" +
  "end\n" +
  "local function __hiro_emit(kind, face, payload)\n" +
  "    if #payload > 7000 then error(\"observation packet too large\") end\n" +
  "    __hiro_sequence = __hiro_sequence + 1\n" +
  "    Amaz.MessageCenter.sendMessage(1431065345, __hiro_nonce, __hiro_sequence,\n" +
  "        \"S1|\" .. __hiro_feature .. \"|\" .. kind .. \"|\" .. tostring(face) .. \"|\" .. payload)\n" +
  "end\n" +
  "local function __hiro_uniform(component, face, key, value)\n" +
  "    __hiro_emit(\"uniform\", face, __hiro_safe(component) .. \",\" .. key .. \",\" .. __hiro_number(value))\n" +
  "end\n" +
  "local function __hiro_matrix(face, m)\n" +
  "    local numbers = {}\n" +
  "    for row=0,3 do\n" +
  "        local v = m:GetRow(row)\n" +
  "        table.insert(numbers,__hiro_number(v.x));table.insert(numbers,__hiro_number(v.y))\n" +
  "        table.insert(numbers,__hiro_number(v.z));table.insert(numbers,__hiro_number(v.w))\n" +
  "    end\n" +
  "    __hiro_emit(\"mvp\",face,table.concat(numbers,\",\"))\n" +
  "end\n" +
  "local __hiro_original_update = HIROCLASS.onUpdate\n" +
  "\n" +
  "function HIROCLASS:onUpdate(context,deltaTime)\n" +
  "    if __hiro_done then\n" +
  "        if __hiro_original_update then __hiro_original_update(self,context,deltaTime) end\n" +
  "        return\n" +
  "    end\n" +
  "    __hiro_active=true\n" +
  "    if __hiro_original_update then __hiro_original_update(self,context,deltaTime) end\n" +
  "    __hiro_active=false\n" +
  "    __hiro_done=true\n" +
  "    local ok = pcall(function()\n" +
  "__hiro_uniform(\"LaughGan_main\",-1,\"intensity\",self.MeshRenderer.material.properties:getFloat(\"intensity\"))\n" +
  "\n" +
  "    end)\n" +
  "    if ok then\n" +
  "        __hiro_emit(\"END\",-1,tostring(__hiro_sequence))\n" +
  "    else\n" +
  "        __hiro_emit(\"ERROR\",-1,\"observation_failed\")\n" +
  "    end\n" +
  "end\n" +
  "-- END Hiro same-still observation appendix.\n";break;
 case "lut":template=
  "\n" +
  "-- BEGIN Hiro same-still observation appendix; this is not vendor code.\n" +
  "local __hiro_nonce = HIRONONCE\n" +
  "local __hiro_feature = \"HIROFEATURE\"\n" +
  "local __hiro_sequence = 0\n" +
  "local __hiro_done = false\n" +
  "local __hiro_active = false\n" +
  "local function __hiro_number(value)\n" +
  "    if type(value) ~= \"number\" or value ~= value or value == math.huge or value == -math.huge then\n" +
  "        error(\"nonfinite observation\")\n" +
  "    end\n" +
  "    return string.format(\"%.17g\", value)\n" +
  "end\n" +
  "local function __hiro_safe(value)\n" +
  "    if type(value) ~= \"string\" or #value > 128 or string.find(value, \"[^%w_%-]\") then\n" +
  "        error(\"invalid component identifier\")\n" +
  "    end\n" +
  "    return value\n" +
  "end\n" +
  "local function __hiro_emit(kind, face, payload)\n" +
  "    if #payload > 7000 then error(\"observation packet too large\") end\n" +
  "    __hiro_sequence = __hiro_sequence + 1\n" +
  "    Amaz.MessageCenter.sendMessage(1431065345, __hiro_nonce, __hiro_sequence,\n" +
  "        \"S1|\" .. __hiro_feature .. \"|\" .. kind .. \"|\" .. tostring(face) .. \"|\" .. payload)\n" +
  "end\n" +
  "local function __hiro_uniform(component, face, key, value)\n" +
  "    __hiro_emit(\"uniform\", face, __hiro_safe(component) .. \",\" .. key .. \",\" .. __hiro_number(value))\n" +
  "end\n" +
  "local function __hiro_matrix(face, m)\n" +
  "    local numbers = {}\n" +
  "    for row=0,3 do\n" +
  "        local v = m:GetRow(row)\n" +
  "        table.insert(numbers,__hiro_number(v.x));table.insert(numbers,__hiro_number(v.y))\n" +
  "        table.insert(numbers,__hiro_number(v.z));table.insert(numbers,__hiro_number(v.w))\n" +
  "    end\n" +
  "    __hiro_emit(\"mvp\",face,table.concat(numbers,\",\"))\n" +
  "end\n" +
  "local __hiro_original_update = HIROCLASS.onUpdate\n" +
  "\n" +
  "function HIROCLASS:onUpdate(context,deltaTime)\n" +
  "    if __hiro_done then\n" +
  "        if __hiro_original_update then __hiro_original_update(self,context,deltaTime) end\n" +
  "        return\n" +
  "    end\n" +
  "    __hiro_active=true\n" +
  "    if __hiro_original_update then __hiro_original_update(self,context,deltaTime) end\n" +
  "    __hiro_active=false\n" +
  "    __hiro_done=true\n" +
  "    local ok = pcall(function()\n" +
  "\n" +
  "        for i=1,#self.filterComponent do\n" +
  "            local component=self.filterComponent[i]\n" +
  "            __hiro_uniform(component.entity.name,-1,\"uniAlpha\",component.sharedMaterials:get(0).properties:getFloat(\"uniAlpha\"))\n" +
  "        end\n" +
  "\n" +
  "    end)\n" +
  "    if ok then\n" +
  "        __hiro_emit(\"END\",-1,tostring(__hiro_sequence))\n" +
  "    else\n" +
  "        __hiro_emit(\"ERROR\",-1,\"observation_failed\")\n" +
  "    end\n" +
  "end\n" +
  "-- END Hiro same-still observation appendix.\n";break;
 default:throw new IllegalArgumentException("observer kind");}return template.replace("HIRONONCE",Integer.toString(nonce)).replace("HIROFEATURE",feature).replace("HIROCLASS",klass);}
}
