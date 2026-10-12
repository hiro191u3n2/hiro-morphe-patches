package com.hiro.ulike;

import java.util.concurrent.CancellationException;

/** Executes the production uploader under a 32 MiB heap with 24.5MP sources.
 * The fake endpoints generate/check every pixel and only model I/O, not the
 * production scheduling, alpha scan, buffer reuse, or cancellation decisions. */
public final class Source1990Test {
    static int assertions,cases;static long transferred;static int maximum;
    static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    static int pixel(int index){return 0xff000000|((index*0x45d9f3b)^(index>>>7))&0xffffff;}
    static final class Endpoint implements GpuSource1990.Source,GpuSource1990.Sink,GpuSource1990.Check {
        final int width,height;int reads,writes,next,alpha=-1,fail=-1,cancelAfter=-1,readFail=-1;int[] identity;
        Endpoint(int w,int h){width=w;height=h;}
        public void read(int[] data,int first,int rows){
            if(reads++==readFail)throw new IllegalStateException("injected bitmap read failure");
            Source1990Test.check(first*width==next,"no overlap/skipped source rows");
            Source1990Test.check(data.length==GpuSource1990.capacity(width,height),"bounded allocation");
            if(identity==null)identity=data;else Source1990Test.check(identity==data,"one reused transfer buffer");
            maximum=Math.max(maximum,data.length);
            for(int i=0;i<width*rows;i++)data[i]=next+i==alpha?0x7f123456:pixel(next+i);
        }
        public boolean write(int first,int[] data,int count){
            Source1990Test.check(first==next,"contiguous SSBO destination offset");
            Source1990Test.check(count==Math.min(width*GpuSource1990.ROWS,width*height-next),"exact tail count");
            for(int i=0;i<count;i++)if(data[i]!=pixel(first+i))throw new AssertionError("pixel mismatch at "+(first+i));
            writes++;if(writes==fail)return false;
            next+=count;transferred+=count;return true;
        }
        public void check(){if(cancelAfter>=0&&writes>=cancelAfter)throw new CancellationException("injected capture");}
    }
    static void complete(int width,int height){
        Endpoint endpoint=new Endpoint(width,height);
        check(GpuSource1990.upload(width,height,endpoint,endpoint,endpoint),"complete image upload");
        check(endpoint.next==width*height,"all pixels reach native destination");
        check(endpoint.reads==endpoint.writes&&endpoint.writes==(height+31)/32,"one upload for each source band");cases++;
    }
    public static void main(String[] ignored){
        complete(1,1);complete(1,33);complete(17,65);complete(79,97);complete(3060,4080);complete(4080,3060);complete(4284,5712);
        for(int alpha:new int[]{0,17*64+16}){
            Endpoint endpoint=new Endpoint(17,65);endpoint.alpha=alpha;
            check(!GpuSource1990.upload(17,65,endpoint,endpoint,endpoint),"transparent pixel rejects entire output");
            check(endpoint.writes==alpha/(17*32),"no failed-alpha band upload");
        }
        Endpoint failed=new Endpoint(19,160);failed.fail=3;
        check(!GpuSource1990.upload(19,160,failed,failed,failed),"native transfer failure rejects");
        check(failed.reads==3&&failed.writes==3,"no work after failed transfer");
        for(int at:new int[]{0,1,2}){
            Endpoint stopped=new Endpoint(19,160);stopped.cancelAfter=at;
            try{GpuSource1990.upload(19,160,stopped,stopped,stopped);throw new AssertionError("cancellation swallowed");}
            catch(CancellationException expected){check(stopped.writes==at,"no transfer after cancellation");}
        }
        Endpoint broken=new Endpoint(19,160);broken.readFail=2;
        try{GpuSource1990.upload(19,160,broken,broken,broken);throw new AssertionError("bitmap failure swallowed");}
        catch(IllegalStateException expected){check(broken.writes==2,"no partial read accepted");}
        for(int[] invalid:new int[][]{{0,1},{1,0},{-1,2},{50000,50000}}){
            try{GpuSource1990.capacity(invalid[0],invalid[1]);throw new AssertionError("invalid dimension admitted");}
            catch(IllegalArgumentException expected){assertions++;}
        }
        check(4L*GpuSource1990.capacity(3060,4080)==391680L,"reported source Java bytes");
        check(4L*3060*4080-4L*GpuSource1990.capacity(3060,4080)==49547520L,"source Java peak reduction");
        check(GpuSource1990.workspaceBytes(3060,4080)==784896L,"Java plus native staging bound");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"all_pixels_verified\":"+transferred+",\"largest_java_buffer_bytes\":"+(4L*maximum)+",\"source_3060x4080_java_bytes\":391680,\"source_3060x4080_removed_java_bytes\":49547520,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}
