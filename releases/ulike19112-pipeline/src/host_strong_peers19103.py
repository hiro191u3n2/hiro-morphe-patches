"""Narrow .102/.103/.106/.107 API compatibility for controlled Strong fixtures.

Only generated fixture copies gain these APIs. Production admission, shaders,
historical fixture files, test inputs and existing assertions remain untouched.
The deployed-probe default follows the fixture's non-benchmark/non-oracle state;
completed core readiness is a nonblocking read of the existing readiness flag.
"""

def once(body,old,new):
    if body.count(old)!=1:raise AssertionError('Strong fixture API anchor changed: '+old)
    return body.replace(old,new)

def transport(body):
    # The retained transport corpus uses an 8-pixel wide, 64-row model. Supply
    # its complete +0 regional planes so current production admission executes
    # normally; the fixture's synthetic pixels and assertions are unchanged.
    body=once(body,'static final class Model {int height=64;}',
              'static final class Model {int width=8,height=64;}')
    body=once(body,'static float[] gpuEvidence1960(Model model){return new float[16];}',
              'static float[] gpuEvidence1960(Model model){return new float[16+3*((model.width+63)/64)*((model.height+63)/64)];}')
    if 'class ResidentStages19104' in body:
        raise AssertionError('Unexpected post-save diagnostic peer in foreground fixture')
    # The retained foreground routing tests do not establish a post-save replay
    # context. Satisfy only the new diagnostic wrapper's compiler dependency;
    # accidentally entering that wrapper must fail immediately.
    body+='''\nfinal class ResidentStages19104 {
    static void checkFixed19105(){throw new AssertionError("post-save Strong diagnostic entered foreground fixture");}
}
'''
    body=once(body,'final class GpuResident1976 {','final class GpuResident1976 {\n    static boolean deployedProbe19102(){return benchmarking()&&!cpuOracle();}')
    method='''        Lease1971 reserveCapacity1983(int[] slots,long[] targets,long[] uploadBytes,long javaBytes){
            if(slots==null||targets==null||uploadBytes==null||slots.length!=targets.length||uploadBytes.length!=targets.length)
                throw new AssertionError("invalid explicit upload lease shape");
            for(int i=0;i<targets.length;i++)if(targets[i]<0||uploadBytes[i]<0||uploadBytes[i]>targets[i])
                throw new AssertionError("invalid explicit upload lease extent");
            return reserveCapacity1971(slots,targets,javaBytes);
        }
'''
    return once(body,'        Lease1971 reserveCapacity1971(int[] slots,long[] targets,long javaBytes){',method+'        Lease1971 reserveCapacity1971(int[] slots,long[] targets,long javaBytes){')

def startup(body):
    return once(body,'final class GpuStartup1990 {','final class GpuStartup1990 {static boolean coreDomainReady19102(){return GpuStrongDefault1990.ready;}')

def tuning_regional19107(body,source):
    # Retain the historical tuner transport while compiling the exact current
    # regional admission method into its GpuStrong peer. No permissive boolean
    # stub replaces the positive-finite input contract.
    body=once(body,'static float[] gpuEvidence1960(Model m){return new float[16];}',
              'static float[] gpuEvidence1960(Model m){return new float[16+3*((8+63)/64)*((m.height+63)/64)];}')
    production=(source/'GpuStrong1960.java').read_text()
    anchor='    static boolean regionalEvidence19107(float[] evidence,int width,int height){'
    if production.count(anchor)!=1:raise AssertionError('Regional admission method anchor changed')
    start=production.index(anchor);opening=production.index('{',start);depth=1;end=opening+1
    while depth and end<len(production):
        if production[end]=='{':depth+=1
        elif production[end]=='}':depth-=1
        end+=1
    if depth:raise AssertionError('Unclosed production regional admission method')
    return once(body,'final class GpuStrong1960 {',
                'final class GpuStrong1960 {\n'+production[start:end]+'\n')
