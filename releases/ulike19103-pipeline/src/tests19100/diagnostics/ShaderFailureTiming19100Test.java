package com.hiro.ulike;
public final class ShaderFailureTiming19100Test {
    static int assertions;
    static void check(boolean condition, String message) {
        assertions++; if (!condition) throw new AssertionError(message);
    }
    static String render(ProcessingTiming1947.Trace trace) throws Exception {
        return Timing1982Test.render(trace);
    }
    public static void main(String[] args) throws Exception {
        ProcessingTiming1947.Trace owner = ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.Trace other = ProcessingTiming1947.begin(new Object());
        check(!render(owner).contains("GPU候補作成の実記録:"), "unobserved driver failures remain absent");
        ProcessingTiming1947.pipelinePriorReason1989(owner, 1, 20, 1);
        check(owner.shaderFailures19100[0] == null, "empty cached result is not a fabricated failure");
        StringBuilder longFailure = new StringBuilder("fp64_extension_missing: line\n\t\u007f");
        for (int i = 0; i < 400; i++) longFailure.append('x');
        GpuNoise1960.failures[1] = longFailure.toString();
        ProcessingTiming1947.Scope scope = ProcessingTiming1947.enter(other);
        try {
            ProcessingTiming1947.pipelinePriorReason1989(owner, 1, 20, 1);
        } finally { ProcessingTiming1947.restore(scope); }
        check(owner.shaderFailures19100[0] != null, "explicit photo owns the unsupported single result");
        check(other.shaderFailures19100[0] == null, "thread-local or latest photo is never substituted");
        check(owner.shaderFailures19100[0].length() == 240, "driver text has a fixed bounded snapshot");
        check(owner.shaderFailures19100[0].indexOf('\n') < 0 && owner.shaderFailures19100[0].indexOf('\t') < 0 && owner.shaderFailures19100[0].indexOf(127) < 0, "control characters cannot inject diagnostic rows");
        check(render(owner).contains("GPU候補作成の実記録: 単写 / fp64_extension_missing:"), "render retains the actual failure classification");
        int observed = GpuNoise1960.observations;
        GpuNoise1960.failures[1] = "later unrelated failure";
        for (int i = 0; i < 16; i++) ProcessingTiming1947.pipelinePriorReason1989(owner, 1, 20, 1);
        check(GpuNoise1960.observations == observed, "repeat strips reuse the first scalar snapshot");
        check(!render(owner).contains("later unrelated failure"), "later program state cannot rewrite observed evidence");
        GpuNoise1960.failures[5] = "program_link_failed: residual linker result";
        ProcessingTiming1947.pipelineDetail1988(owner, 1, 0, 21, 1, 0);
        check(render(owner).contains("GPU候補作成の実記録: 残差 / program_link_failed:"), "final residual fallback records its own native failure");
        check(owner.shaderFailures19100[0].startsWith("fp64_extension_missing:"), "program results stay independent");
        observed = GpuNoise1960.observations;
        ProcessingTiming1947.pipelinePriorReason1989(null, 1, 20, 1);
        ProcessingTiming1947.pipelinePriorReason1989(other, 1, 20, 0);
        ProcessingTiming1947.pipelinePriorReason1989(other, 2, 20, 1);
        ProcessingTiming1947.pipelineDetail1988(other, 1, 1, 21, 1, 0);
        check(GpuNoise1960.observations == observed, "invalid or unrelated phases cannot query diagnostic evidence");
        GpuNoise1960.fail = true;
        ProcessingTiming1947.pipelinePriorReason1989(other, 1, 20, 1);
        check(other.shaderFailures19100[0] == null, "optional reflection failure cannot become image processing failure");
        GpuNoise1960.fail = false;
        ProcessingTiming1947.finish(owner, true);
        String frozen = render(owner);
        observed = GpuNoise1960.observations;
        ProcessingTiming1947.pipelinePriorReason1989(owner, 1, 20, 1);
        ProcessingTiming1947.pipelineDetail1988(owner, 1, 0, 21, 1, 0);
        check(observed == GpuNoise1960.observations && frozen.equals(render(owner)), "completed capture remains immutable and cannot issue observations");
        check(!frozen.contains("GPU採用 1回"), "compile evidence does not manufacture GPU adoption");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"scalar_failure_isolation\":true}");
    }
}
