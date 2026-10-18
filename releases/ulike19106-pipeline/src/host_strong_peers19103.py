"""Narrow .102/.103/.106 API compatibility for controlled Strong fixtures.

Only generated fixture copies gain these APIs. Production admission, shaders,
historical fixture files, test inputs and existing assertions remain untouched.
The deployed-probe default follows the fixture's non-benchmark/non-oracle state;
completed core readiness is a nonblocking read of the existing readiness flag.
"""

def once(body,old,new):
    if body.count(old)!=1:raise AssertionError('Strong fixture API anchor changed: '+old)
    return body.replace(old,new)

def transport(body):
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
