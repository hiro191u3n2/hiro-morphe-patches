package com.hiro.ulike.geometry;
import java.util.*;
import java.nio.file.*;
public final class ObservationContracts {
 static int checks;
 static void ok(boolean b){checks++;if(!b)throw new AssertionError();}
 static void bad(Runnable r){checks++;try{r.run();throw new AssertionError("accepted malformed input");}catch(IllegalArgumentException good){}}
 static Object id=new Object();
 static NativeMeshObservation.Collector make(){return new NativeMeshObservation.Collector(id,77,Arrays.asList("AmazingFeature1"));}
 static void put(NativeMeshObservation.Collector c,int seq,String s){c.accept(NativeMeshObservation.MESSAGE_ID,77,seq,"G1|AmazingFeature1|"+s);}
 static NativeMeshObservation.Collector one(){NativeMeshObservation.Collector c=make();put(c,1,"BEGIN|-1|1,1");put(c,2,"MESH|0|66616365,0,2,0:1:2:3:4");put(c,3,"POS|0|0,2;0,0.5,1,-0");put(c,4,"END|-1|3");return c;}
 public static void main(String[] args)throws Exception {
  NativeMeshObservation.Collector c=one();NativeMeshObservation.Frame f=c.finish(id);
  ok(f.belongsTo(id)&&!f.belongsTo(new Object())&&!f.productionGeometryVerified()&&!f.legacyV2OpacityObserved());
  NativeMeshObservation.Mesh m=f.features.get(0).meshes.get(0);
  ok(m.vertexCount()==2&&m.positionSemantic()==13&&m.positionComponents()==2&&!m.perFaceRoutingVerified());
  double[] xy=m.positions();xy[0]=999;ok(m.positions()[0]==0);int[] ids=m.declaredFaceIds();ids[0]=255;ok(m.declaredFaceIds()[0]==0);
  ok(Double.doubleToRawLongBits(m.positions()[3])==Double.doubleToRawLongBits(-0d));
  bad(()->c.finish(id));bad(()->one().finish(new Object()));bad(()->make().finish(id));
  ok(!make().accept(55,77,1,"ignored"));
  bad(()->make().accept(NativeMeshObservation.MESSAGE_ID,78,1,"G1|AmazingFeature1|BEGIN|-1|0,0"));
  NativeMeshObservation.Collector empty=make();put(empty,1,"BEGIN|-1|0,0");put(empty,2,"END|-1|1");ok(empty.finish(id).features.get(0).meshes.isEmpty());
  for(String s:Arrays.asList("NaN","Infinity","1e999","1e-999","0.1","0x1p0","1f"," 1","+1","01")) {
   NativeMeshObservation.Collector q=make();put(q,1,"BEGIN|-1|1,1");put(q,2,"MESH|0|61,0,1,0");bad(()->put(q,3,"POS|0|0,1;"+s+",0"));bad(()->q.finish(id));
  }
  for(String s:Arrays.asList("MESH|1|61,0,1,0","MESH|0|61,0,8193,0","MESH|0|zz,0,1,0","MESH|0|61,1,1,0","MESH|0|61,0,1,256","MESH|0|61,0,1,0:")) {
   NativeMeshObservation.Collector q=make();put(q,1,"BEGIN|-1|1,1");bad(()->put(q,2,s));
  }
  for(String s:Arrays.asList("POS|0|1,1;0,0","POS|0|0,2;0,0","POS|0|0,1;0,0,0","END|-1|2","MESH|1|62,1,1,0","ERROR|-1|geometry_unavailable","vertices|0|0,1;0,0")) {
   NativeMeshObservation.Collector q=make();put(q,1,"BEGIN|-1|1,1");put(q,2,"MESH|0|61,0,1,0");bad(()->put(q,3,s));
  }
  NativeMeshObservation.Collector q=make();put(q,1,"BEGIN|-1|0,0");bad(()->put(q,3,"END|-1|1"));
  bad(()->new NativeMeshObservation.Collector(id,77,Arrays.asList("AmazingFeature1","AmazingFeature1")));
  NativeMeshObservation.Collector multi=new NativeMeshObservation.Collector(id,77,Arrays.asList("AmazingFeature1","AmazingFeature3"));
  multi.accept(NativeMeshObservation.MESSAGE_ID,77,1,"G1|AmazingFeature1|BEGIN|-1|0,0");multi.accept(NativeMeshObservation.MESSAGE_ID,77,2,"G1|AmazingFeature1|END|-1|1");
  multi.accept(NativeMeshObservation.MESSAGE_ID,77,1,"G1|AmazingFeature3|BEGIN|-1|1,0");multi.accept(NativeMeshObservation.MESSAGE_ID,77,2,"G1|AmazingFeature3|END|-1|1");bad(()->multi.finish(id));
  NativeMeshObservation.Collector eyelash=new NativeMeshObservation.Collector(id,77,Arrays.asList("AmazingFeature6"));
  String[] ep={"BEGIN|-1|1,1","MESH|0|61,0,1,0","POS|0|0,1;0.5,0.25,-1","END|-1|3"};
  for(int i=0;i<ep.length;i++)eyelash.accept(NativeMeshObservation.MESSAGE_ID,77,i+1,"G1|AmazingFeature6|"+ep[i]);
  NativeMeshObservation.Mesh em=eyelash.finish(id).features.get(0).meshes.get(0);ok(em.positionSemantic()==0 && em.positionComponents()==3 && em.vertexCount()==1 && em.positions()[2]==-1);
  if(args.length>0){
   NativeMeshObservation.Collector fromLua=new NativeMeshObservation.Collector(id,314,Arrays.asList("AmazingFeature1"));int seq=0;
   for(String line:Files.readAllLines(Paths.get(args[0])))fromLua.accept(NativeMeshObservation.MESSAGE_ID,314,++seq,line);
   NativeMeshObservation.Mesh lm=fromLua.finish(id).features.get(0).meshes.get(0);ok(lm.vertexCount()==1240);
   for(int i=0;i<2480;i++)ok(lm.positions()[i]==i/16.0);
  }
  System.out.println("PASS "+checks+" owned geometry/parser checks");
 }
}
