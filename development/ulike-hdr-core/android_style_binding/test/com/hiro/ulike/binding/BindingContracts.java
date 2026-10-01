package com.hiro.ulike.binding;
import com.hiro.ulike.hdr.stillanalysis.StillMessageCollector;import java.util.*;import java.lang.reflect.*;import java.nio.file.*;import java.security.*;
public class BindingContracts {
 static int checks;static void check(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}interface Work{void run()throws Exception;}static void fails(Work w)throws Exception{try{w.run();}catch(Exception expected){checks++;return;}throw new AssertionError("accepted invalid binding");}
 static Class<?> observer;static Method begin,result,reset;
 static Object request(Object owner,String op,String[] paths,String[] keys,float[] values)throws Exception{return begin.invoke(null,owner,op,paths,paths==null?0:paths.length,null,0,null,keys,values,null,null);}
 static void success(Object owner,String op,String[] paths,String[] keys,float[] values)throws Exception{result.invoke(null,request(owner,op,paths,keys,values),0);}
 public static final class Choice {public Choice h(){return this;}public Choice g(){return this;}public Choice a(){return this;}public Map<Integer,Long>b(){return Collections.singletonMap(15,Long.parseLong(ShotStyleSettings.NATURAL));}}
 static void settings()throws Exception{
  observer=Class.forName("com.hiro.ulike.StyleSnapshot164");begin=observer.getDeclaredMethod("begin",Object.class,String.class,String[].class,int.class,String[].class,int.class,String[].class,String[].class,float[].class,Integer.class,Integer.class);begin.setAccessible(true);
  result=observer.getDeclaredMethod("result",Object.class,int.class);result.setAccessible(true);reset=observer.getDeclaredMethod("resetForTest");reset.setAccessible(true);reset.invoke(null);
  Object recorder=new Object(),shot=new Object();success(recorder,"set",new String[]{"/original/style","/original/shared:face_width:0.43"},null,null);
  success(recorder,"update",new String[]{"/original/style"},new String[]{"Internal_Makeup"},new float[]{0.8123457f});
  ShotStyleSettings.Snapshot s=ShotStyleSettings.freezeAtChoice(new Choice(),recorder,shot,19);
  check(s.recorderIdentity==recorder&&s.shotIdentity==shot&&s.shotEpoch==19&&s.requestedGraphComplete);check(!s.nativeExecutionConfirmed&&!s.nativeQueueBarrierPerformed);
  check(s.inlineParameters.size()==1&&s.inlineParameters.get(0).valueBits==Float.floatToRawIntBits(0.43f));check(s.uniqueObservedUpdate("Internal_Makeup").valueBits==Float.floatToRawIntBits(0.8123457f));
  fails(()->s.requestedNodes.add("other"));ShotStyleSettings.requireCurrent(s);
  Object pending=request(recorder,"update",new String[]{"/original/style"},new String[]{"Internal_Makeup"},new float[]{0.2f});
  fails(()->ShotStyleSettings.requireCurrent(s));fails(()->ShotStyleSettings.freeze(recorder,ShotStyleSettings.NATURAL,shot,20));result.invoke(null,pending,0);
  check(s.uniqueObservedUpdate("Internal_Makeup").valueBits==Float.floatToRawIntBits(0.8123457f));
  ShotStyleSettings.Snapshot next=ShotStyleSettings.freeze(recorder,ShotStyleSettings.NATURAL,shot,20);check(next.uniqueObservedUpdate("Internal_Makeup").value==0.2f);
  success(recorder,"update",new String[]{"/original/shared"},new String[]{"Internal_Makeup"},new float[]{0.6f});ShotStyleSettings.Snapshot ambiguous=ShotStyleSettings.freeze(recorder,ShotStyleSettings.NATURAL,shot,21);fails(()->ambiguous.uniqueObservedUpdate("Internal_Makeup"));
  fails(()->ShotStyleSettings.freeze(recorder,"unknown",shot,1));fails(()->ShotStyleSettings.freeze(new Object(),ShotStyleSettings.NATURAL,shot,1));
  Object overlap=request(recorder,"update",new String[]{"/original/style"},new String[]{"key"},new float[]{1});Object overlap2=request(recorder,"update",new String[]{"/original/style"},new String[]{"key"},new float[]{1});result.invoke(null,overlap,0);result.invoke(null,overlap2,0);fails(()->ShotStyleSettings.freeze(recorder,ShotStyleSettings.NATURAL,shot,22));
  reset.invoke(null);Object bad=new Object();success(bad,"set",new String[]{"/x:key:NaN"},null,null);fails(()->ShotStyleSettings.freeze(bad,ShotStyleSettings.NATURAL,shot,1));
 }
 static StillMessageCollector collector(String style){StillMessageCollector c=new StillMessageCollector(41,ObservedStyleFrame.expectedFeatures(style));c.submitted();return c;}
 static void packet(StillMessageCollector c,String feature,String kind,int face,int ordinal,String data){c.accept(StillMessageCollector.MESSAGE_ID,41,ordinal,"S1|"+feature+"|"+kind+"|"+face+"|"+data);}
 static void empty(StillMessageCollector c,String style,String except){for(String f:ObservedStyleFrame.expectedFeatures(style))if(!f.equals(except))packet(c,f,"END",-1,1,"0");}
 static String matrix="1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1";
 static void observations()throws Exception{
  Object frame=new Object();StillMessageCollector c=collector(ShotStyleSettings.NATURAL);empty(c,ShotStyleSettings.NATURAL,"AmazingFeature1");packet(c,"AmazingFeature1","uniform",0,1,"blush,intensity,0.8125");packet(c,"AmazingFeature1","uniform",0,2,"blush,intensity,0.3125");packet(c,"AmazingFeature1","END",-1,3,"2");StillMessageCollector.Snapshot snap=c.finish();
  ObservedStyleFrame f=ObservedStyleFrame.parse(snap,frame,41,ShotStyleSettings.NATURAL);check(f.frameIdentity==frame&&f.uniform("AmazingFeature1","blush",0,"intensity")==.3125);check(!f.completeStyleBinding&&!f.skinMaskPixelsAvailable&&!f.native2dGeometryAvailable);fails(()->f.uniform("AmazingFeature1","blush",1,"intensity"));fails(()->ObservedStyleFrame.parse(snap,frame,42,ShotStyleSettings.NATURAL));fails(()->ObservedStyleFrame.parse(snap,frame,41,ShotStyleSettings.PURITY));
  StillMessageCollector p=collector(ShotStyleSettings.PURITY);empty(p,ShotStyleSettings.PURITY,"3dmakeup4");packet(p,"3dmakeup4","mvp",0,1,matrix);int ordinal=2;
  for(int start=0;start<1427;start+=64){int count=Math.min(64,1427-start);StringBuilder values=new StringBuilder();for(int i=0;i<count;i++){if(i>0)values.append(',');values.append((start+i)/16.0).append(',').append((start+i)/32.0).append(',').append((start+i)/64.0);}packet(p,"3dmakeup4","vertices",0,ordinal++,start+","+count+",1427;"+values);}
  packet(p,"3dmakeup4","END",-1,ordinal,Integer.toString(ordinal-1));ObservedStyleFrame full=ObservedStyleFrame.parse(p.finish(),frame,41,ShotStyleSettings.PURITY);check(full.mesh3d(0).vertexCount==1427);double[] xyz=full.mesh3d(0).copyVertices();for(int i=0;i<1427;i++){check(xyz[i*3]==i/16.0);check(xyz[i*3+1]==i/32.0);check(xyz[i*3+2]==i/64.0);}xyz[0]=88;check(full.mesh3d(0).copyVertices()[0]==0);
  StillMessageCollector partial=collector(ShotStyleSettings.PURITY);empty(partial,ShotStyleSettings.PURITY,"3dmakeup4");packet(partial,"3dmakeup4","mvp",0,1,matrix);packet(partial,"3dmakeup4","END",-1,2,"1");fails(()->ObservedStyleFrame.parse(partial.finish(),frame,41,ShotStyleSettings.PURITY));
  for(String value:new String[]{"NaN","Infinity","1e9999","0x1.0p0"}){StillMessageCollector bad=collector(ShotStyleSettings.NATURAL);empty(bad,ShotStyleSettings.NATURAL,"AmazingFeature1");packet(bad,"AmazingFeature1","uniform",0,1,"blush,intensity,"+value);packet(bad,"AmazingFeature1","END",-1,2,"1");fails(()->ObservedStyleFrame.parse(bad.finish(),frame,41,ShotStyleSettings.NATURAL));}
 }
 static void meshes(String root)throws Exception{
  String[] files={"natural/AmazingFeature1/mesh/mask_faceuv22995_mesh.mesh","purity/3dmakeup4/mesh/Mesh1200wan.mesh","purity/AmazingFeature5/mesh/eye_part_faceu2988_mesh.mesh","purity/AmazingFeature6/mesh/jiemaoFaceU_V2_vwwo_1669694037.mesh"};int[] counts={1240,1427,870,1044},tris={1855,2304,1670,2004};
  for(int n=0;n<files.length;n++){byte[] b=Files.readAllBytes(Path.of(root,files[n]));String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));AuthoredMesh m=AuthoredMesh.readPinned(b,sha);check(m.vertexCount==counts[n]);int triangles=0;for(AuthoredMesh.Submesh s:m.submeshes)triangles+=s.triangleCount();check(triangles==tris[n]);double[] uv=m.copyUv();for(double v:uv)check(Double.isFinite(v));double before=uv[0];uv[0]=999;check(m.copyUv()[0]==before);byte[] changed=b.clone();changed[90]^=1;fails(()->AuthoredMesh.readPinned(changed,sha));}
 }
 public static void main(String[] args)throws Exception{settings();observations();meshes(args[0]);System.out.println("Binding contracts PASS "+checks);}
}
