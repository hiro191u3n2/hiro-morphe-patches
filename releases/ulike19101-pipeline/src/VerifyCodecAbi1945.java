import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** Pinned actual AndroidX ABI: supplied Handler uses one looper throughout;
 * output drain writes then releases; close posts two nested front tasks.
 * PrepTicket QUEUED abandon releases exactly once, RUNNING abandons then the
 * producer disposes/releases. Pin complete methods rather than loose symbols. */
public final class VerifyCodecAbi1945 {
    static final Map<String,String> PIN=Map.ofEntries(
        Map.entry("Landroidx/heifwriter/HeifWriter$Builder;->setHandler(Landroid/os/Handler;)Landroidx/heifwriter/HeifWriter$Builder;","72e91aa514190169800f74314a41196bf4bc272ae33e2da4f84f87f6d87b897d"),
        Map.entry("Landroidx/heifwriter/HeifWriter$Builder;->build()Landroidx/heifwriter/HeifWriter;","b364fd6b38a98ec187810bcfa5a31217d9b577e815d0a99e3ea781cc59dc0008"),
        Map.entry("Landroidx/heifwriter/HeifWriter;-><init>(Ljava/lang/String;Ljava/io/FileDescriptor;IIIZIIIILandroid/os/Handler;)V","8baee2e0af73928ea6e164e9ed0b0f7ed00de9bf360cebf22cdff1fcf0e595cb"),
        Map.entry("Landroidx/heifwriter/WriterBase;-><init>(IIIIZILandroid/os/Handler;Z)V","817c7d89d970c0258a859d01ca17c40559e24b752c80c6f69b65c8b79c4eb9df"),
        Map.entry("Landroidx/heifwriter/WriterBase;->close()V","c922f5a86612808e61816f925834e1ca58102573400f9b6619462380c6e9e188"),
        Map.entry("Landroidx/heifwriter/WriterBase;->closeInternal()V","fadcd8ac2605c9d8001aa050f9b729ddcf2d2070634e86c6a0a521c53f4a6f90"),
        Map.entry("Landroidx/heifwriter/WriterBase$1;->run()V","48c848d25b50c7d172838520605da6dd453cc34928cda273c2df3c4e7e19ca16"),
        Map.entry("Landroidx/heifwriter/HeifEncoder;-><init>(IIZIILandroid/os/Handler;Landroidx/heifwriter/EncoderBase$Callback;)V","1808bb9c03d279a2db8d851cdd4e923f8450b54e998fe247e29da791bdc65331"),
        Map.entry("Landroidx/heifwriter/EncoderBase;-><init>(Ljava/lang/String;IIZIILandroid/os/Handler;Landroidx/heifwriter/EncoderBase$Callback;Z)V","b69dd939ac3526c16995dc5b5a85cabde03237cfb20a93ca02e03389869ed7b7"),
        Map.entry("Landroidx/heifwriter/EncoderBase;->close()V","bf532916461884965b6f4bf17b2766fab259ce149fa895196de1830effd25c23"),
        Map.entry("Landroidx/heifwriter/EncoderBase;->stopInternal()V","85d5cb3d657ff124331ca86f9297fbe5f01a0921d90c94e6cd598e31377da100"),
        Map.entry("Landroidx/heifwriter/EncoderBase$2;->run()V","17b6c910833dc8a365c8ab59e9f676ca609b4c1bf78a7f492cbde9e4db63e7f3"),
        Map.entry("Landroidx/heifwriter/EncoderBase$EncoderCallback;->onOutputBufferAvailable(Landroid/media/MediaCodec;ILandroid/media/MediaCodec$BufferInfo;)V","82e902f27026ec0db0181fa85a6f6176534c1768bfd402d2a74fe851060297d7"),
        Map.entry("Landroidx/heifwriter/WriterBase$WriterCallback;->onDrainOutputBuffer(Landroidx/heifwriter/EncoderBase;Ljava/nio/ByteBuffer;)V","f4a70d0a974ebcca3d2bbe6a9ea1bad0fd8dc6097cbd8c0e9c519b51c0de15c4"),
        Map.entry("Lcom/hiro/ulike/PrepTicket186;-><init>(Lcom/hiro/ulike/PrepTicket186$Disposer;Ljava/lang/Runnable;)V","4a0127e3f31cf315d999287b7ddaeb3f78c7b44a4d4da7e52e9e974292eca5e6"),
        Map.entry("Lcom/hiro/ulike/PrepTicket186;->abandon()V","ebfab4a12a4e47c8a50d4d8c3f5a58ed29d20bb575338aa01aea2cc47e366169"),
        Map.entry("Lcom/hiro/ulike/PrepTicket186;->claim()Ljava/lang/Object;","0beee8a6fe0afb1f7299377d5f4cd9668adb87ff2b4144655872854b7ac8706a"),
        Map.entry("Lcom/hiro/ulike/PrepTicket186;->disposeAndRelease(Ljava/lang/Object;)V","e3662c2730de4f6851f6fe3114200b33d2cf28f8965e20059f60dc9055730327"),
        Map.entry("Lcom/hiro/ulike/PrepTicket186;->fail(Ljava/lang/Throwable;)V","d79902f54b59cb6f428fca84a45c341d05225b4557132b6edc9e809c25d5713b"),
        Map.entry("Lcom/hiro/ulike/PrepTicket186;->offer(Ljava/lang/Object;)V","a38055ffa13031668ed48dc7273e170dfae68a4fe5ddfdd3e618a68cb593bdb0"),
        Map.entry("Lcom/hiro/ulike/PrepTicket186;->recordFailureLocked(Ljava/lang/Throwable;)V","6e6fde0afbe2fd4e6afd160ee4e9b06ade84a7dee3213c20aa5793264e52707b"),
        Map.entry("Lcom/hiro/ulike/PrepTicket186;->releaseClaim()V","7f43bef4bec5dd6dae07e7a2d77bc3b908f0a93afa25756c858620264835060a"),
        Map.entry("Lcom/hiro/ulike/PrepTicket186;->releaseOnce()V","fb09c78b2c649916cef5b0e8f313f1ea74eaffa4c94bfd16b0d9a03c48924350"),
        Map.entry("Lcom/hiro/ulike/PrepTicket186;->start()Z","db2bf848e72820b11adbda61a1977053070e5e7d7579e2eb408aea314f8c0aa5")
    );
    static void need(boolean condition,String why){if(!condition)throw new IllegalStateException(why);}
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("APPLIED_APK_OR_RUNTIME_DEX");
        var all=MergePayloads.classes(args[0]);var methods=MergePayloads.methods(all.values());
        boolean applied=all.containsKey("Lcom/hiro/ulike/CodecDrain1945;");
        int checks=0;
        for(var entry:PIN.entrySet()){
            String id=entry.getKey(),expected=entry.getValue();
            if(applied&&id.equals(SaveSpeedHooks1945.ENCODER_CTOR))expected="d4b2e130c59647f8304fd79b134a6622494e05679adf6567c79713637db67022";
            if(applied&&id.equals(SaveSpeedHooks1945.STOP_INTERNAL))expected="8dc576bef5dd310112f7ad5cf2c9e31902f7d1ce6fd25a410591e60971024a99";
            Method actual=methods.get(id);need(actual!=null,"Missing codec ABI "+id);
            need(MergePayloads.hash(actual).equals(expected),"Unreviewed codec lifecycle ABI "+id);checks++;
        }
        Method set=methods.get("Landroidx/heifwriter/HeifWriter$Builder;->setHandler(Landroid/os/Handler;)Landroidx/heifwriter/HeifWriter$Builder;");
        need((set.getAccessFlags()&1)!=0,"setHandler must be public");checks++;
        for(var entry:all.entrySet())if(entry.getKey().startsWith("Landroidx/heifwriter/")){
            for(Method method:entry.getValue().getMethods())if(method.getImplementation()!=null)for(Instruction op:method.getImplementation().getInstructions())if(op instanceof ReferenceInstruction){
                String reference=((ReferenceInstruction)op).getReference().toString();
                need(!reference.equals("Landroid/os/HandlerThread;->quit()Z")&&!reference.equals("Landroid/os/HandlerThread;->quitSafely()Z"),"Shared callback looper must not be quit by AndroidX");checks++;
            }
        }
        System.out.println("PASS codec1945 pinned ABI checks="+checks+" applied="+applied+" shared_looper=true output_callback_unchanged=true direct_fd_preserved=true device_tested=false");
    }
}

