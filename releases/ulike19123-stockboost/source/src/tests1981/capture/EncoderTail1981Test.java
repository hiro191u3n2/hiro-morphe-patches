package com.hiro.ulike;

import android.graphics.Bitmap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class EncoderTail1981Test {
    static int assertions;
    static synchronized void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    static void waitIdle()throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while((!SaveQueue1935.idle1953()||ExitJobs185.count()!=0)&&System.nanoTime()<until)Thread.yield();check(SaveQueue1935.idle1953()&&ExitJobs185.count()==0,"real Async photo and exit guard drain");}
    static void scenario(final int mode)throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<Throwable>();AtomicInteger receipts=new AtomicInteger();
        i.o.a.b1.a.b.f.a manager=new i.o.a.b1.a.b.f.a(new i.o.a.b1.a.b.f.a.c(){public void a(){}public void b(boolean ok,int n,String p,String e){if(ok)receipts.incrementAndGet();}});
        Bitmap source=new Bitmap(new int[]{101+mode,102,103,104});ShotContext1932.bind(source,500+mode);i.f.l.n.s.a.b().image=source;
        HostRouter1981.encoder=(controller,input,rotation,direction)->{
            try {
                Bitmap finished=mode==0?input:input.copy(input.getConfig(),true);
                AsyncSave1935.encoding(finished);long before=SaveQueue1935.nativeBytes();
                check(!input.isRecycled()&&!finished.isRecycled(),"input and final survive through hardware use");
                if(mode==0){AsyncSave1935.hardwareClosed1956();EncoderTail1981.release(finished,input);check(!input.isRecycled()&&SaveQueue1935.nativeBytes()==before,"pristine encoder identity never recycled early");}
                else {
                    if(mode==2)QualityPipeline1932.leased=finished;
                    if(mode==3)QualityPipeline1932.fail=true;
                    AsyncSave1935.hardwareClosed1956();EncoderTail1981.release(finished,input);
                    if(mode==2||mode==3){check(!finished.isRecycled()&&SaveQueue1935.nativeBytes()==before,"proof lease or notification failure preserves live image accounting");QualityPipeline1932.leased=null;QualityPipeline1932.fail=false;EncoderTail1981.release(finished,input);}
                    check(finished.isRecycled()&&!input.isRecycled(),"after actual close only distinct final pixels released");
                    check(SaveQueue1935.nativeBytes()==input.getAllocationByteCount(),"only exact recycled final allocation removed from tail cost");
                    int recycled=finished.recycleCalls;EncoderTail1981.release(finished,input);check(finished.recycleCalls==recycled,"duplicate release remains idempotent");
                }
                check(AsyncSave1935.snapshotForPhoto1981(input).id==500+mode,"same-photo immutable metadata remains available through tail");
                return "tail-"+mode;
            }catch(Throwable error){failure.set(error);throw error;}finally{done.countDown();}
        };
        AsyncSave1935.submitAuto(manager,0,0);check(done.await(4,TimeUnit.SECONDS),"actual Async encoder-tail hook reached");waitIdle();
        check(failure.get()==null,"encoder tail scenario has no resource/identity failure: "+failure.get());check(receipts.get()==1,"exact original success receipt delivered");check(!source.isRecycled(),"SDK source remains outside queue disposal ownership");
        HostRouter1981.encoder=null;
    }
    public static void main(String[]args)throws Exception{for(int mode=0;mode<4;mode++)scenario(mode);System.out.println("PASS EncoderTail1981 assertions="+assertions);}
}
