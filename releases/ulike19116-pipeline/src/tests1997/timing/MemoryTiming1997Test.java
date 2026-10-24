package com.hiro.ulike;

/** Diagnostics must preserve photo ownership and distinguish optional work. */
public final class MemoryTiming1997Test {
    static int assertions;
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static String render(ProcessingTiming1947.Trace t)throws Exception{return Timing1982Test.render(t);}
    public static void main(String[] args)throws Exception {
        long M=1024L*1024;
        ProcessingTiming1947.Trace t=ProcessingTiming1947.begin(new Object());
        check(!render(t).contains("前段の実行計画"),"unobserved plan is not invented");
        ProcessingTiming1947.frontPlan1997(t,4,192,0,110*M,165*M,145*M,12*M,1,4);
        ProcessingTiming1947.frontHalf1997(t,1,3121200);
        String text=render(t);
        check(text.contains("4ワーカー / 区間 192行 / 選択時のCPU空き枠 4"),"actual selected plan");
        check(text.contains("引継ぎ成功 / 3121200画素"),"committed half explicitly observed");
        check(text.contains("初回 110.0 MiB / 計画時 165.0 MiB"),"one shot budget snapshots");
        check(text.contains("整理を要求"),"trim status does not assert bytes freed");
        ProcessingTiming1947.frontHalf1997(t,1,0);
        check(t.frontHalfPixels1997==3121200,"inconsistent success rejected");
        ProcessingTiming1947.frontPlan1997(t,0,192,0,0,0,0,0,-1,4);
        check(t.frontWorkers1997==4,"invalid plan rejected");
        long[] m=new long[33];java.util.Arrays.fill(m,-1);
        m[0]=1;m[1]=220*M;m[2]=0;m[3]=1;m[4]=1;m[5]=2;m[6]=0;m[7]=900000;m[8]=3;
        m[9+11]=212*M;m[9+6]=55*M;m[9+8]=55*M;
        m[21+11]=267*M;m[21+6]=0;m[21+8]=0;
        ProcessingTiming1947.finishMemory1997(t,m);
        m[21+11]=0;
        text=render(t);
        check(text.contains("この撮影の補正メモリ再判定: 初期拒否 1回"),"foreground report separate from probe history");
        check(text.contains("再判定: 同じ上限内で成立"),"no relaxed cap claim");
        check(text.contains("整理・再判定後: 観測空き 267.0 MiB"),"report copied without retaining caller array");
        check(text.contains("GPU保持 未観測"),"missing owner snapshot is not zero");
        m[6]=1;ProcessingTiming1947.finishMemory1997(t,m);
        check(t.finishMemoryCount1997==1,"background observation cannot become foreground proof");
        ProcessingTiming1947.finishWork1992(t,ProcessingTiming1947.FINISH_PREPARATION1992,1000000,93*M);
        ProcessingTiming1947.finishWork1992(t,ProcessingTiming1947.FINISH_VALIDATION_COPY1997,2000000,47*M);
        text=render(t);
        check(text.contains("GPU補正準備・領域確保: 1回 / 1 ms / 論理量 93.0 MiB"),"optional copy does not inflate output allocations");
        check(text.contains("保存後の追加検証用コピー（保存公開前の取得）: 1回 / 2 ms / 論理量 47.0 MiB"),"optional copy has actual foreground position");
        ProcessingTiming1947.finishValidationSkipped1997(t,1);
        check(render(t).contains("今回は本番補正の容量不足を優先して見送り"),"deferred qualification explained");
        check(!t.noiseFirstMeasured1976&&!t.noiseModelMeasured1976,"diagnostics never invent stage timing");
        ProcessingTiming1947.finish(t,true);
        ProcessingTiming1947.frontHalf1997(t,3,0);m[6]=0;ProcessingTiming1947.finishMemory1997(t,m);
        check(t.frontHalfReason1997==1&&t.finishMemoryCount1997==1,"completed photo remains immutable");
        ProcessingTiming1947.Trace other=ProcessingTiming1947.begin(new Object());
        check(!render(other).contains("この撮影の補正メモリ再判定"),"other photo has no stale memory report");
        ProcessingTiming1947.frontHalf1997(other,3,0);
        check(render(other).contains("同じ並列数での予算不足（モデル側で必要な入力を生成）"),"half fallback preserves model work");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+"}");
    }
}
