package com.hiro.ulike.geometry;

import java.nio.file.*;
import java.util.*;

/** Independent host parser tests. A parsed diagnostic is never a GPU draw receipt. */
public final class DrawReview {
    static int checks;static final int ID=NativeDrawObservation.MESSAGE_ID,NONCE=901;
    static void ok(boolean value){checks++;if(!value)throw new AssertionError("check="+checks);}
    interface Action{void run();}
    static void bad(Action action){try{action.run();throw new AssertionError("malformed accepted");}catch(IllegalArgumentException expected){checks++;}}
    static NativeDrawObservation.Collector collector(Object owner,String...features){return new NativeDrawObservation.Collector(owner,NONCE,Arrays.asList(features));}
    static List<String> small(String feature){
        boolean three=feature.equals("AmazingFeature6");return Arrays.asList(
            "D1|"+feature+"|BEGIN|-1|2,1","D1|"+feature+"|MESH|0|0061ff,3,2,-1,7:3",
            "D1|"+feature+"|VTX|0|0,3;"+(three?"-0,1.25,-2.5,0,1,3,4,5,0.25,0.5,6,7,8,1,0":"-0,1.25,0,1,3,4,0.25,0.5,6,7,1,0"),
            "D1|"+feature+"|SUB|0|0,1,4,3,65536","D1|"+feature+"|IDX|0|0,0,3;2,0,1",
            "D1|"+feature+"|SUB|0|1,0,5,0,65536","D1|"+feature+"|END|-1|6");
    }
    static NativeDrawObservation.Frame parse(Object owner,String feature,List<String> packets){
        NativeDrawObservation.Collector c=collector(owner,feature);int sequence=0;
        for(String packet:packets)ok(c.accept(ID,NONCE,++sequence,packet));return c.finish(owner);
    }
    static void basic(){
        Object owner=new Object();for(String feature:Arrays.asList("AmazingFeature1","AmazingFeature3","AmazingFeature5","AmazingFeature6","AmazingFeature7")){
            NativeDrawObservation.Frame frame=parse(owner,feature,small(feature));NativeDrawObservation.Mesh mesh=frame.features.get(0).meshes.get(0);
            ok(frame.belongsTo(owner)&&!frame.belongsTo(new Object()));ok(!frame.productionGeometryVerified()&&!frame.finalUniformMatricesObserved()&&!frame.legacyV2OpacityObserved());
            ok(!mesh.perFaceRoutingVerified()&&!mesh.materialToSubmeshMappingVerified());
            ok(mesh.positionSemantic()==(feature.equals("AmazingFeature6")?0:13)&&mesh.uvSemantic()==6);
            ok(mesh.generatedRendererMaterialCount==-1&&mesh.vertexCount()==3);
            ok(mesh.submeshes.get(0).storedIndexCount==65536&&mesh.submeshes.get(0).requestedIndexCount()==3);
            ok(Arrays.equals(mesh.submeshes.get(0).indices(),new int[]{2,0,1}));ok(mesh.submeshes.get(1).indices().length==0);
            int[] indices=mesh.submeshes.get(0).indices(),ids=mesh.declaredFaceIds();double[] pos=mesh.positions(),uv=mesh.uv();
            indices[0]=9;ids[0]=9;pos[0]=9;uv[0]=9;
            ok(mesh.submeshes.get(0).indices()[0]==2&&mesh.declaredFaceIds()[0]==7&&mesh.positions()[0]==0&&mesh.uv()[0]==0);
            try{frame.features.clear();throw new AssertionError("mutable features");}catch(UnsupportedOperationException expected){checks++;}
            try{mesh.submeshes.clear();throw new AssertionError("mutable submeshes");}catch(UnsupportedOperationException expected){checks++;}
            for(int drop=0;drop<7;drop++){final int selected=drop;bad(()->{List<String> broken=new ArrayList<>(small(feature));broken.remove(selected);parse(owner,feature,broken);});}
            for(int repeat=0;repeat<7;repeat++){final int selected=repeat;bad(()->{List<String> broken=new ArrayList<>(small(feature));broken.add(selected,broken.get(selected));parse(owner,feature,broken);});}
            for(String invalid:Arrays.asList("1e-999","-1e-999","NaN","Infinity","0.1","+0",".5","1.","00","0x0.1p0","1e100","1e-45")){
                bad(()->{List<String> broken=new ArrayList<>(small(feature));broken.set(2,broken.get(2).replace("-0,",invalid+","));parse(owner,feature,broken);});
            }
        }
        NativeDrawObservation.Collector zero=collector(owner,"AmazingFeature1");ok(!zero.accept(NativeMeshObservation.MESSAGE_ID,0,0,null));
        zero.accept(ID,NONCE,1,"D1|AmazingFeature1|BEGIN|-1|0,0");zero.accept(ID,NONCE,2,"D1|AmazingFeature1|END|-1|1");
        ok(zero.finish(owner).features.get(0).meshes.isEmpty());bad(()->zero.finish(owner));
        bad(()->collector(owner,"AmazingFeature0"));bad(()->collector(owner,"AmazingFeature1","AmazingFeature1"));
        NativeDrawObservation.Collector poisoned=collector(owner,"AmazingFeature1");
        bad(()->poisoned.accept(ID,NONCE+1,1,"D1|AmazingFeature1|BEGIN|-1|0,0"));
        bad(()->poisoned.accept(ID,NONCE,1,"D1|AmazingFeature1|BEGIN|-1|0,0"));
        NativeDrawObservation.Collector wrong=collector(owner,"AmazingFeature1");bad(()->wrong.finish(new Object()));bad(()->wrong.finish(owner));
        for(String packet:Arrays.asList("D1|AmazingFeature1|BEGIN|-1|0,33","D1|AmazingFeature1|BEGIN|-1|11,0","D1|AmazingFeature1|BEGIN|-1|0,0|extra",
                "D1|AmazingFeature1|BEGIN|-1|0,0\n","D1|AmazingFeature1|ERROR|-1|unavailable","G1|AmazingFeature1|BEGIN|-1|0,0")){
            bad(()->collector(owner,"AmazingFeature1").accept(ID,NONCE,1,packet));
        }
        for(long ordinal:new long[]{-1,0,2,8193,Long.MAX_VALUE})bad(()->collector(owner,"AmazingFeature1").accept(ID,NONCE,ordinal,"D1|AmazingFeature1|BEGIN|-1|0,0"));
    }
    static void exactCorrelation(){
        Object owner=new Object();NativeDrawObservation.Frame frame=parse(owner,"AmazingFeature1",small("AmazingFeature1"));
        String[] values={"BEGIN|-1|2,1","MESH|0|0061ff,0,3,7:3","POS|0|0,3;-0,1.25,3,4,6,7","END|-1|3"};
        NativeMeshObservation.Collector match=new NativeMeshObservation.Collector(owner,NONCE,Arrays.asList("AmazingFeature1"));
        for(int i=0;i<values.length;i++)match.accept(NativeMeshObservation.MESSAGE_ID,NONCE,i+1,"G1|AmazingFeature1|"+values[i]);
        frame.requireSamePositions(match.finish(owner));checks++;
        for(int mode=0;mode<5;mode++){
            final int m=mode;bad(()->{
                Object other=m==0?new Object():owner;int nonce=m==1?NONCE+1:NONCE;
                NativeMeshObservation.Collector changed=new NativeMeshObservation.Collector(other,nonce,Arrays.asList("AmazingFeature1"));
                String[] copy=values.clone();if(m==2)copy[0]="BEGIN|-1|1,1";if(m==3)copy[1]="MESH|0|0061ff,0,3,3:7";if(m==4)copy[2]="POS|0|0,3;0,1.25,3,4,6,7";
                for(int i=0;i<copy.length;i++)changed.accept(NativeMeshObservation.MESSAGE_ID,nonce,i+1,"G1|AmazingFeature1|"+copy[i]);
                frame.requireSamePositions(changed.finish(other));
            });
        }
    }
    static void luaPackets(Path path)throws Exception{
        Object owner=new Object();List<String> lines=Files.readAllLines(path);NativeDrawObservation.Collector c=collector(owner,"AmazingFeature1");int n=0;
        for(String line:lines){ok(c.accept(ID,NONCE,++n,line));}
        NativeDrawObservation.Frame frame=c.finish(owner);ok(frame.features.get(0).meshes.size()==4);
        int vertexCount=0,indexCount=0;
        for(int component=0;component<4;component++){
            NativeDrawObservation.Mesh mesh=frame.features.get(0).meshes.get(component);double[] position=mesh.positions(),uv=mesh.uv();
            ok(mesh.generatedRendererMaterialCount==2&&mesh.submeshes.size()==1);
            for(int i=0;i<8192;i++){
                ok(position[2*i]==i/16.0&&position[2*i+1]==-i/32.0);ok(uv[2*i]==(i%2)&&uv[2*i+1]==.5);vertexCount++;
            }
            int[] indices=mesh.submeshes.get(0).indices();ok(indices.length==65536);
            for(int i=0;i<indices.length;i++){ok(indices[i]==i%8192);indexCount++;}
        }
        ok(vertexCount==32768&&indexCount==262144);
        // A single out-of-range index invalidates the entire observation and poisons reuse.
        NativeDrawObservation.Collector invalid=collector(owner,"AmazingFeature1");boolean mutated=false;int sequence=0;
        for(String line:lines){
            if(!mutated&&line.contains("|IDX|")){
                String broken=line.replaceFirst(";0,",";8192,");final int ordinal=++sequence;
                bad(()->invalid.accept(ID,NONCE,ordinal,broken));mutated=true;break;
            }
            invalid.accept(ID,NONCE,++sequence,line);
        }
        ok(mutated);bad(()->invalid.finish(owner));
    }
    public static void main(String[] args)throws Exception{basic();exactCorrelation();luaPackets(Paths.get(args[0]));System.out.println("{\"checks\":"+checks+",\"vertices\":32768,\"indices\":262144,\"native_execution\":false}");}
}
