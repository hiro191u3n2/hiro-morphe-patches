package com.hiro.ulike.geometry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
/** Original hostile protocol tests. No message here is an actual SDK observation. */
public final class ParserReview {
    static int checks;
    static final int ID=NativeMeshObservation.MESSAGE_ID;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    interface Action{void run();}
    static void rejects(Action a,String why){try{a.run();throw new AssertionError("accepted "+why);}catch(IllegalArgumentException expected){checks++;}}
    static NativeMeshObservation.Collector collector(Object owner){return new NativeMeshObservation.Collector(owner,17,Arrays.asList("AmazingFeature1"));}
    static void send(NativeMeshObservation.Collector c,int seq,String kind,int mesh,String data){c.accept(ID,17,seq,"G1|AmazingFeature1|"+kind+"|"+mesh+"|"+data);}
    static void first(NativeMeshObservation.Collector c){send(c,1,"BEGIN",-1,"1,1");send(c,2,"MESH",0,"6162,0,2,0:1");}
    static void valid(){
        Object owner=new Object();NativeMeshObservation.Collector c=collector(owner);
        check(!c.accept(1,0,0,null),"other protocol ignored without poisoning");first(c);
        send(c,3,"POS",0,"0,2;0.10000000149011612,-0,3.4028234663852886e+38,1.4012984643248171e-45");send(c,4,"END",-1,"3");
        NativeMeshObservation.Frame frame=c.finish(owner);check(frame.belongsTo(owner)&&!frame.belongsTo(new Object()),"exact analysis owner");
        check(!frame.productionGeometryVerified()&&!frame.legacyV2OpacityObserved(),"diagnostic only");
        NativeMeshObservation.Mesh mesh=frame.features.get(0).meshes.get(0);
        check(mesh.positionSemantic()==13&&mesh.positionComponents()==2&&mesh.vertexCount()==2&&!mesh.perFaceRoutingVerified(),"semantic and limits explicit");
        double[] positions=mesh.positions();int[] ids=mesh.declaredFaceIds();positions[0]=123;ids[0]=77;
        check(mesh.positions()[0]==(double)(float).1&&mesh.declaredFaceIds()[0]==0,"returned arrays cannot mutate owned observation");
        boolean immutable=false;try{frame.features.clear();}catch(UnsupportedOperationException e){immutable=true;}check(immutable,"immutable feature collection");
        rejects(()->c.finish(owner),"second finish");
    }
    static void malformed(){
        String[] coordinates={"NaN","Infinity","-Infinity","0.1","1e400","1e-999","0x1.0p0","1.0f","+1","01","1,2"," ","1e99999999999999999999999999999999"};
        for(String value:coordinates){Object o=new Object();NativeMeshObservation.Collector c=collector(o);first(c);
            rejects(()->send(c,3,"POS",0,"0,2;"+value+",0,0,0"),"invalid native float "+value);rejects(()->c.finish(o),"poisoned coordinates");}
        for(int fault=0;fault<12;fault++){
            final int f=fault;Object o=new Object();NativeMeshObservation.Collector c=collector(o);
            rejects(()->{
                if(f==0){c.accept(ID,18,1,"G1|AmazingFeature1|BEGIN|-1|1,1");return;}
                if(f==1){c.accept(ID,17,2,"G1|AmazingFeature1|BEGIN|-1|1,1");return;}
                if(f==2){c.accept(ID,17,1,"G1|AmazingFeature3|BEGIN|-1|1,1");return;}
                if(f==3){send(c,1,"BEGIN",-1,"1,33");return;}
                send(c,1,"BEGIN",-1,"1,1");
                if(f==4){send(c,2,"MESH",0,"6162,0,8193,0");return;}
                if(f==5){send(c,2,"MESH",0,"6162,1,2,0");return;}
                if(f==6){send(c,2,"MESH",0,"a,0,2,0");return;}
                if(f==7){send(c,2,"MESH",0,"6162,0,2,256");return;}
                send(c,2,"MESH",0,"6162,0,2,0");
                if(f==8){send(c,3,"POS",0,"1,1;0,0");return;}
                if(f==9){send(c,3,"POS",0,"0,2;0,0");return;}
                if(f==10){send(c,3,"END",-1,"2");return;}
                send(c,3,"ERROR",-1,"geometry_unavailable");
            },"hostile structure "+f);rejects(()->c.finish(o),"poisoned structure "+f);
        }
        Object o=new Object();NativeMeshObservation.Collector c=collector(o);first(c);send(c,3,"POS",0,"0,2;0,0,0,0");send(c,4,"END",-1,"3");
        rejects(()->c.finish(new Object()),"foreign completion owner");rejects(()->c.finish(o),"foreign completion poisons ledger");
    }
    static void packets(Path path)throws Exception {
        Object owner=new Object();NativeMeshObservation.Collector c=collector(owner);int count=0;
        for(String line:Files.readAllLines(path,StandardCharsets.UTF_8)){
            String[] a=line.split("\t",4);check(a.length==4,"review transport packet");
            check(c.accept(Integer.parseInt(a[0]),Long.parseLong(a[1]),Long.parseLong(a[2]),a[3]),"Lua emitted recognized packet");count++;
        }
        NativeMeshObservation.Frame f=c.finish(owner);check(f.features.size()==1,"complete Lua feature");
        int vertices=0;for(NativeMeshObservation.Mesh m:f.features.get(0).meshes){vertices+=m.vertexCount();for(double v:m.positions()){check(v==1.25||v==-2.5,"snapshot copied before callbacks mutated native vector");}}
        check(vertices==32768&&count==518,"maximum bounded Lua snapshot parsed");
        int[] expectedAliases={0,0,1,2};for(int i=0;i<4;i++)check(f.features.get(0).meshes.get(i).luaUserdataAliasOrdinal==expectedAliases[i],"Lua object alias retained without pointer claim");
    }
    static void three(Path path)throws Exception {
        Object owner=new Object();NativeMeshObservation.Collector c=new NativeMeshObservation.Collector(owner,17,Arrays.asList("AmazingFeature6"));
        for(String line:Files.readAllLines(path,StandardCharsets.UTF_8)){String[] a=line.split("\t",4);c.accept(Integer.parseInt(a[0]),Long.parseLong(a[1]),Long.parseLong(a[2]),a[3]);}
        NativeMeshObservation.Mesh mesh=c.finish(owner).features.get(0).meshes.get(0);
        check(mesh.positionSemantic()==0&&mesh.positionComponents()==3&&mesh.vertexCount()==2,"eyelash actual POSITION width three");
        check(Arrays.equals(mesh.positions(),new double[]{1.25,-2.5,3.75,1.25,-2.5,3.75}),"all three components copied");
        NativeMeshObservation.Collector wrong=new NativeMeshObservation.Collector(owner,17,Arrays.asList("AmazingFeature6"));
        wrong.accept(ID,17,1,"G1|AmazingFeature6|BEGIN|-1|2,1");wrong.accept(ID,17,2,"G1|AmazingFeature6|MESH|0|6162,0,2,0:1");
        rejects(()->wrong.accept(ID,17,3,"G1|AmazingFeature6|POS|0|0,2;1,2,3,4"),"two-component data for three-component feature");
        rejects(()->wrong.finish(owner),"wrong feature width poisons");
    }
    public static void main(String[] args)throws Exception {valid();malformed();packets(Paths.get(args[0]));three(Paths.get(args[0]).resolveSibling("three.tsv"));System.out.println("{\"checks\":"+checks+",\"native_execution\":false}");}
}
