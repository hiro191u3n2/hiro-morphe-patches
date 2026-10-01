package com.hiro.ulike.geometry;
import java.nio.file.*;
import java.util.*;
public final class DrawContracts {
 static int checks;
 static void check(boolean b){checks++;if(!b)throw new AssertionError();}
 static void bad(Runnable r){checks++;try{r.run();throw new AssertionError("accepted malformed observation");}catch(IllegalArgumentException expected){}}
 static List<String> fixture(){return Arrays.asList("BEGIN|-1|1,1","MESH|0|66616365,3,2,2,0:1:2:3:4","VTX|0|0,3;0,0,0,0,1,0,1,0,0,1,0,1","SUB|0|0,0,4,3,6","IDX|0|0,0,3;0,1,2","SUB|0|1,1,5,0,3","END|-1|6");}
 static NativeDrawObservation.Collector collector(Object owner){return new NativeDrawObservation.Collector(owner,314,Arrays.asList("AmazingFeature1"));}
 static NativeDrawObservation.Frame parse(Object owner,List<String> fixture){NativeDrawObservation.Collector c=collector(owner);int n=0;for(String p:fixture)c.accept(NativeDrawObservation.MESSAGE_ID,314,++n,"D1|AmazingFeature1|"+p);return c.finish(owner);}
 public static void main(String[] args)throws Exception {
  Object owner=new Object();NativeDrawObservation.Frame frame=parse(owner,fixture());NativeDrawObservation.Mesh m=frame.features.get(0).meshes.get(0);
  check(frame.belongsTo(owner) && !frame.belongsTo(new Object()));check(!frame.productionGeometryVerified() && !frame.finalUniformMatricesObserved() && !frame.legacyV2OpacityObserved());
  check(m.vertexCount()==3 && m.positionComponents()==2 && m.positionSemantic()==13 && m.uvSemantic()==6);check(m.generatedRendererMaterialCount==2);
  check(m.submeshes.size()==2 && m.submeshes.get(0).requestedIndexCount()==3 && m.submeshes.get(0).storedIndexCount==6);check(m.submeshes.get(1).requestedIndexCount()==0 && m.submeshes.get(1).indices32);
  check(!m.perFaceRoutingVerified() && !m.materialToSubmeshMappingVerified());
  m.positions()[0]=999;m.uv()[0]=999;m.declaredFaceIds()[0]=999;m.submeshes.get(0).indices()[0]=999;
  check(m.positions()[0]==0 && m.uv()[0]==0 && m.declaredFaceIds()[0]==0 && m.submeshes.get(0).indices()[0]==0);
  NativeMeshObservation.Collector g=new NativeMeshObservation.Collector(owner,314,Arrays.asList("AmazingFeature1"));String[] gp={"BEGIN|-1|1,1","MESH|0|66616365,0,3,0:1:2:3:4","POS|0|0,3;0,0,1,0,0,1","END|-1|3"};
  for(int i=0;i<gp.length;i++)g.accept(NativeMeshObservation.MESSAGE_ID,314,i+1,"G1|AmazingFeature1|"+gp[i]);NativeMeshObservation.Frame gf=g.finish(owner);frame.requireSamePositions(gf);checks++;
  bad(()->frame.requireSamePositions(null));
  for(int i=0;i<fixture().size();i++){final int j=i;bad(()->{List<String> broken=new ArrayList<String>(fixture());broken.remove(j);parse(owner,broken);});}
  String[][] mutations={{"MESH|0|66616365,3,2,2,0:1:2:3:4","MESH|0|66616365,3,2,-2,0"},{"SUB|0|0,0,4,3,6","SUB|0|0,0,4,7,6"},{"SUB|0|0,0,4,3,6","SUB|0|0,2,4,3,6"},{"SUB|0|0,0,4,3,6","SUB|0|0,0,4,3,65537"},{"IDX|0|0,0,3;0,1,2","IDX|0|0,0,3;0,1,3"},{"IDX|0|0,0,3;0,1,2","IDX|0|1,0,3;0,1,2"},{"IDX|0|0,0,3;0,1,2","IDX|0|0,1,3;0,1,2"},{"VTX|0|0,3;0,0,0,0,1,0,1,0,0,1,0,1","VTX|0|0,3;1e-999,0,0,0,1,0,1,0,0,1,0,1"}};
  for(String[] change:mutations)bad(()->{List<String> broken=new ArrayList<String>(fixture());broken.set(broken.indexOf(change[0]),change[1]);parse(owner,broken);});
  for(String value:new String[]{"NaN","Infinity","1e400","0x1.0p0","0.1","1e-999","-1e-999"})bad(()->{List<String> broken=new ArrayList<String>(fixture());broken.set(2,"VTX|0|0,3;"+value+",0,0,0,1,0,1,0,0,1,0,1");parse(owner,broken);});
  NativeDrawObservation.Collector c=collector(owner);check(!c.accept(NativeMeshObservation.MESSAGE_ID,0,0,null));bad(()->c.accept(NativeDrawObservation.MESSAGE_ID,999,1,"D1|AmazingFeature1|BEGIN|-1|0,0"));bad(()->c.accept(NativeDrawObservation.MESSAGE_ID,314,1,"D1|AmazingFeature1|BEGIN|-1|0,0"));
  NativeDrawObservation.Collector done=collector(owner);done.accept(NativeDrawObservation.MESSAGE_ID,314,1,"D1|AmazingFeature1|BEGIN|-1|0,0");done.accept(NativeDrawObservation.MESSAGE_ID,314,2,"D1|AmazingFeature1|END|-1|1");check(done.finish(owner).features.get(0).meshes.isEmpty());bad(()->done.finish(owner));
  NativeDrawObservation.Collector wrong=collector(owner);bad(()->wrong.finish(new Object()));bad(()->wrong.finish(owner));
  List<String> lua=Files.readAllLines(Paths.get(args[0]));NativeDrawObservation.Collector lc=collector(owner);int n=0;for(String line:lua){lc.accept(NativeDrawObservation.MESSAGE_ID,314,++n,line);checks++;}
  NativeDrawObservation.Mesh lm=lc.finish(owner).features.get(0).meshes.get(0);check(lm.vertexCount()==1240 && lm.submeshes.size()==2 && lm.submeshes.get(0).requestedIndexCount()==1239);check(lm.uv()[0]==0 && lm.uv()[1]==0.0625);check(lm.submeshes.get(0).indices()[1238]==1238);
  for(int i=0;i<1240;i++){check(lm.positions()[i*2]==i/16.0);check(lm.uv()[i*2]==i/16.0);}System.out.println("draw contracts "+checks);
 }
}
