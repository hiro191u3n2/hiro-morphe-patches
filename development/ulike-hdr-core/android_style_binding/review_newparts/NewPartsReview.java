package com.hiro.ulike.binding;

import com.hiro.ulike.style.SampledMakeupPipeline;
import com.hiro.ulike.hdr.stillanalysis.StillMessageCollector;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Independent reviewer probes. No production implementation is copied here. */
public final class NewPartsReview {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private interface Run {void run()throws Exception;}
    private static void reject(Run work,String why)throws Exception{try{work.run();throw new AssertionError("accepted "+why);}catch(IllegalArgumentException|IOException expected){checks++;}}
    private static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static byte[] mutationTarget,mutationReplacement;
    public static final class MutationDigest extends MessageDigestSpi {
        private final MessageDigest delegate;
        public MutationDigest(){try{delegate=MessageDigest.getInstance("SHA-256","SUN");}catch(Exception e){throw new IllegalStateException(e);}}
        protected void engineUpdate(byte value){delegate.update(value);}
        protected void engineUpdate(byte[] value,int offset,int length){delegate.update(value,offset,length);}
        protected byte[] engineDigest(){byte[] result=delegate.digest();System.arraycopy(mutationReplacement,0,mutationTarget,0,mutationTarget.length);return result;}
        protected void engineReset(){delegate.reset();}
    }
    private static void png(Path fixtures)throws Exception {
        for(String line:Files.readAllLines(fixtures.resolve("cases.tsv"))){String[] fields=line.split("\t");byte[] raw=Files.readAllBytes(fixtures.resolve(fields[0]));
            if(fields[1].equals("reject")){reject(()->PinnedPngTexture.readPinned(raw,sha(raw)),fields[0]);continue;}
            byte[] expected=Files.readAllBytes(fixtures.resolve(fields[0]+".rgba"));PinnedPngTexture decoded=PinnedPngTexture.readPinned(raw,sha(raw));
            check(Arrays.equals(expected,decoded.copyStraightRgba()),"independently built PNG pixels "+fields[0]);
            byte[] returned=decoded.copyStraightRgba();returned[0]^=85;check(Arrays.equals(expected,decoded.copyStraightRgba()),"no mutable decoded alias");
            double[] sample=new double[4];
            for(int y=0;y<decoded.height;y++)for(int x=0;x<decoded.width;x++){
                decoded.sample((x+.5)/decoded.width,(y+.5)/decoded.height,PinnedPngTexture.Rows.FIRST_DECODED_ROW_AT_V_ZERO,false,sample);
                for(int c=0;c<4;c++)check(Math.abs(sample[c]-(expected[(y*decoded.width+x)*4+c]&255)/255.0)<2e-15,"literal texel center");
            }
        }
        byte[] source=Files.readAllBytes(fixtures.resolve("transparent-edge.png"));PinnedPngTexture edge=PinnedPngTexture.readPinned(source,sha(source));double[] value=new double[4];
        edge.sample(.5,.5,PinnedPngTexture.Rows.FIRST_DECODED_ROW_AT_V_ZERO,true,value);
        check(value[0]==0&&value[1]==0&&value[2]==.5&&value[3]==.5,"premultiply before interpolation suppresses transparent red");
        edge.sample(.5,.5,PinnedPngTexture.Rows.FIRST_DECODED_ROW_AT_V_ZERO,false,value);
        check(value[0]==.5&&value[2]==.5&&value[3]==.5,"straight interpolation is explicit separate mode");
        reject(()->edge.sample(Double.POSITIVE_INFINITY,0,PinnedPngTexture.Rows.FIRST_DECODED_ROW_AT_V_ZERO,true,value),"nonfinite sampler");
        mutationTarget=Files.readAllBytes(fixtures.resolve("mutation-a.png"));mutationReplacement=Files.readAllBytes(fixtures.resolve("mutation-b.png"));
        check(mutationTarget.length==mutationReplacement.length,"deterministic caller mutation fixture lengths");String pin=sha(mutationTarget);
        Provider provider=new Provider("ReviewMutation",1.0,"test-only digest completion mutation"){private static final long serialVersionUID=1L;};
        provider.put("MessageDigest.SHA-256",MutationDigest.class.getName());Security.insertProviderAt(provider,1);
        PinnedPngTexture stable;try{stable=PinnedPngTexture.readPinned(mutationTarget,pin);}finally{Security.removeProvider("ReviewMutation");}
        check(Arrays.equals(mutationTarget,mutationReplacement),"caller bytes really mutated after hash");
        check(Arrays.equals(stable.copyStraightRgba(),Files.readAllBytes(fixtures.resolve("mutation-a.png.rgba"))),"PNG pin and parse use the same immutable bytes");
    }
    private static final class SwitchPathFile extends File {
        private final String next;private final long originalLength;private int reads;
        SwitchPathFile(File first,File next){super(first.getPath());this.next=next.getPath();originalLength=first.length();}
        @Override public boolean isFile(){return true;}
        @Override public long length(){return originalLength;}
        @Override public String getPath(){return ++reads==1?super.getPath():next;}
    }
    private static void install(Path archive,Path altered,Path work)throws Exception{
        Files.createDirectories(work);SwitchPathFile input=new SwitchPathFile(archive.toFile(),altered.toFile());
        StyleObserverInstaller.Installation i=StyleObserverInstaller.install(input,ShotStyleSettings.NATURAL,work.toFile(),"snapshot",918);
        Path texture=i.directory.toPath().resolve("AmazingFeature1/image/blusher/blusher000.png");
        check(sha(Files.readAllBytes(texture)).equals("96e0155e5510c972ec3408bee132a9de657434471adffd5d07e68492b8acc30e"),"archive verification and extraction use identical private snapshot despite path change");
        check(i.copiedFiles==45&&i.nonce==918&&!i.completeStyleBinding&&!i.actualDeviceDeliveryVerified,"scope flags and actual file count");
        reject(()->StyleObserverInstaller.install(archive.toFile(),ShotStyleSettings.NATURAL,work.toFile(),"snapshot",919),"existing child");
        check(Files.exists(texture),"existing installation preserved");
        reject(()->StyleObserverInstaller.install(altered.toFile(),ShotStyleSettings.NATURAL,work.toFile(),"modified",918),"unpinned replacement archive");
        check(!Files.exists(work.resolve("modified")),"unpinned archive never installs");
    }
    private static void packet(StillMessageCollector c,String kind,int ordinal,String payload){c.accept(StillMessageCollector.MESSAGE_ID,76,ordinal,"S1|3dmakeup4|"+kind+"|"+(kind.equals("END")?-1:0)+"|"+payload);}
    private static ObservedStyleFrame observation(Object identity,double[] vertices,double[] matrix){
        StillMessageCollector c=new StillMessageCollector(76,ObservedStyleFrame.expectedFeatures(ShotStyleSettings.PURITY));c.submitted();
        for(String feature:ObservedStyleFrame.expectedFeatures(ShotStyleSettings.PURITY))if(!feature.equals("3dmakeup4"))c.accept(StillMessageCollector.MESSAGE_ID,76,1,"S1|"+feature+"|END|-1|0");
        StringJoiner values=new StringJoiner(",");for(double v:matrix)values.add(Double.toString(v));packet(c,"mvp",1,values.toString());int ordinal=2;
        for(int first=0;first<1427;first+=64){int count=Math.min(64,1427-first);values=new StringJoiner(",");for(int j=first*3;j<(first+count)*3;j++)values.add(Double.toString(vertices[j]));packet(c,"vertices",ordinal++,first+","+count+",1427;"+values);}
        packet(c,"uniform",ordinal++,"actual_face,intensity,0.625");packet(c,"END",ordinal,Integer.toString(ordinal-1));return ObservedStyleFrame.parse(c.finish(),identity,76,ShotStyleSettings.PURITY);
    }
    private static void doubles(DataOutputStream out,double[] data)throws Exception{out.writeInt(data.length);for(double v:data)out.writeDouble(v);}
    private static void raster(Path assets,Path output)throws Exception{
        AuthoredMesh mesh=AuthoredMesh.readPinned(Files.readAllBytes(assets.resolve("purity/3dmakeup4/mesh/Mesh1200wan.mesh")),"ff70ea10d239851697a6f9f0f48d25d1be0e9534d32fc4aa4d7abda551d5d0c0");
        PinnedPngTexture texture=PinnedPngTexture.readPinned(Files.readAllBytes(assets.resolve("purity/3dmakeup4/image/makeup3d_open.png")),"f91dbf4cc446b0db5d3af7eb6a314f0d706c087543357751011e2a04f48a5f47");
        double[] uv=mesh.copyUv(),vertices=new double[1427*3],matrix={.95,-.07,.1,.05,.04,1.05,-.04,-.03,0,0,.5,0,.22,-.13,.3,1};
        for(int i=0;i<1427;i++){double u=uv[i*2],v=uv[i*2+1];vertices[i*3]=u*1.8-.9;vertices[i*3+1]=v*1.7-.85;vertices[i*3+2]=.32*Math.sin(u*5)+.21*Math.cos(v*4);}
        Object id=new Object();ObservedStyleFrame observed=observation(id,vertices,matrix);
        Purity3dBinding.ShaderBase base=new Purity3dBinding.ShaderBase(){public Object frameIdentity(){return id;}public void sample(double u,double v,double[] rgb){rgb[0]=u;rgb[1]=v;rgb[2]=.25*u+.5*v;}};
        Purity3dBinding.RasterPolicy policy=Purity3dBinding.RasterPolicy.OPENGL_CLIP_TO_TOP_LEFT_NO_CULL_NO_DEPTH_LAST_TRIANGLE_SINGLE_CENTER;
        SampledMakeupPipeline.FrameTile tile=new SampledMakeupPipeline.FrameTile("review-perspective",918,96,72,0,0,96,72);
        SampledMakeupPipeline.ResolvedPass result=Purity3dBinding.resolve(observed,id,tile,0,"actual_face",mesh,texture,PinnedPngTexture.Rows.FIRST_DECODED_ROW_AT_V_ZERO,base,policy);
        check(result.intensity==.625&&result.opacity==1,"actual observed intensity retained");
        reject(()->Purity3dBinding.resolve(observed,new Object(),tile,0,"actual_face",mesh,texture,PinnedPngTexture.Rows.FIRST_DECODED_ROW_AT_V_ZERO,base,policy),"different frame");
        reject(()->Purity3dBinding.resolve(observed,id,tile,0,"invented_face",mesh,texture,PinnedPngTexture.Rows.FIRST_DECODED_ROW_AT_V_ZERO,base,policy),"unobserved material component");
        double[] bad=matrix.clone();bad[15]=-10;ObservedStyleFrame behind=observation(id,vertices,bad);
        reject(()->Purity3dBinding.resolve(behind,id,tile,0,"actual_face",mesh,texture,PinnedPngTexture.Rows.FIRST_DECODED_ROW_AT_V_ZERO,base,policy),"behind eye vertices");
        bad=matrix.clone();bad[11]=10;ObservedStyleFrame far=observation(id,vertices,bad);
        reject(()->Purity3dBinding.resolve(far,id,tile,0,"actual_face",mesh,texture,PinnedPngTexture.Rows.FIRST_DECODED_ROW_AT_V_ZERO,base,policy),"unsupported depth clipping");
        for(int y=0;y<72;y+=13){int height=Math.min(13,72-y);SampledMakeupPipeline.FrameTile part=new SampledMakeupPipeline.FrameTile("review-perspective",918,96,72,7,y,79,height);
            SampledMakeupPipeline.ResolvedPass chunk=Purity3dBinding.resolve(observed,id,part,0,"actual_face",mesh,texture,PinnedPngTexture.Rows.FIRST_DECODED_ROW_AT_V_ZERO,base,policy);
            for(int yy=0;yy<height;yy++)for(int x=0;x<79;x++){int small=yy*79+x,full=(yy+y)*96+x+7;check(chunk.coverage[small]==result.coverage[full],"tile coverage identity");for(int c=0;c<4;c++)check(chunk.rgba[small*4+c]==result.rgba[full*4+c],"tile sample identity");for(int c=0;c<3;c++)check(chunk.shaderBaseRgb[small*3+c]==result.shaderBaseRgb[full*3+c],"tile base identity");}
        }
        try(DataOutputStream out=new DataOutputStream(Files.newOutputStream(output))){out.writeInt(96);out.writeInt(72);doubles(out,vertices);doubles(out,matrix);doubles(out,uv);int[] indices=mesh.submeshes.get(0).copyTriangleIndices();out.writeInt(indices.length);for(int i:indices)out.writeInt(i);doubles(out,result.coverage);doubles(out,result.rgba);doubles(out,result.shaderBaseRgb);}
    }
    public static void main(String[] args)throws Exception{Path fixtures=Path.of(args[0]);png(fixtures);install(Path.of(args[2]),fixtures.resolve("altered.zip"),fixtures.resolve("install"));raster(Path.of(args[1]),fixtures.resolve("raster.bin"));System.out.println("{\"checks\":"+checks+",\"status\":\"PASS\"}");}
}
