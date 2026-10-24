package com.hiro.ulike;
import java.lang.reflect.Field;
import java.util.Arrays;
/** Actual optional research wrapper; only its cached readiness bit is controlled.
 * No GPU availability, certificate, Session or JNI implementation is replaced. */
public final class RegionalCandidateIngress19115Test {
    static int assertions,cases;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Field field(String name)throws Exception{Field f=GpuNoise1960.class.getDeclaredField(name);f.setAccessible(true);return f;}
    public static void main(String[] ignored)throws Exception{
        Field loaded=field("loaded"),prepared=field("regionalPrepared19115");
        boolean previous=prepared.getBoolean(null);
        check(loaded.getInt(null)==0,"fresh JVM has not initialized JNI");
        GpuStrongDefault1990.Fixture fixture=GpuStrongDefault1990.fixture(0);
        float[] evidence=StrongNoise1958.gpuEvidence1960(fixture.model);
        int[][] maps=StrongNoise1958.gpuMaps1960(fixture.model);
        int[] invalid={0x80000000,0x80000001,0xbf800000,0xff7fffff,0x7f800000,0xff800000,0x7fc00001,0x7f800001,0xffc00001};
        try {
            for(boolean ready:new boolean[]{false,true}){
                prepared.setBoolean(null,ready);
                check(GpuNoise1960.regionalReady19115()==ready,"cached readiness branch selected");
                for(boolean required:new boolean[]{false,true})for(int bits:invalid){
                    evidence[16]=Float.intBitsToFloat(bits);int[] original=fixture.u.clone();
                    GpuStrong1960.Read1971 result=GpuStrong1960.ephemeralRegional19115(fixture.source,evidence,maps,fixture.policy,fixture.u,null,0,4,required);
                    String expected=!ready&&required?"regional_preparation_unavailable":"regional_input_unsupported19107";
                    check(result.result==null&&expected.equals(result.failure),"invalid evidence rejected on selected readiness/prepass branch");
                    check(loaded.getInt(null)==0,"rejected optional ingress does not reach session/JNI initialization");
                    check(Arrays.equals(original,fixture.u),"optional ingress preserves original uniforms");cases++;
                }
            }
        } finally {prepared.setBoolean(null,previous);}
        check(prepared.getBoolean(null)==previous,"controlled readiness restored");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"invalid_binary32_classes\":9,\"readiness_required_combinations\":4,\"invalid_cases\":"+cases+"}");
    }
}
