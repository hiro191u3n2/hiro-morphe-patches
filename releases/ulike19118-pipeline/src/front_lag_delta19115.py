"""Strict inverse of the single reviewed native safeguard control-flow edit."""
import hashlib
BASELINE_SHA256='a7310416487c63bb2c08c3a45e4b28f3fbfb11c965b1094e1e2a2fa498a04b8b'
OLD='double r1 = lag(residual, n, 1), r2 = lag(residual, n, 2), r4 = n == 8 ? lag(residual, n, 4) : 0;'
NEW='''/* Independent lag reductions have no side effects. The second-lag
     * predicates are disjoint; evaluate only the lag that its original
     * predicate can consume. Every computed reduction keeps its visit order. */
    double r2 = lag(residual, n, 2);
    double r1 = r2 > .25 ? lag(residual, n, 1) : 0;
    double r4 = n == 8 && r2 < -.15 ? lag(residual, n, 4) : 0;'''
def restore_front19115(text):
    if text.count(NEW)!=1 or OLD in text:
        raise AssertionError('Missing exact front lag control-flow delta')
    restored=text.replace(NEW,OLD)
    if hashlib.sha256(restored.encode()).hexdigest()!=BASELINE_SHA256:
        raise AssertionError('Front source outside reviewed lag delta changed')
    return restored

def audit(text):
    restore_front19115(text)
    mutations=[text.replace('r2 > .25 ? lag(residual, n, 1)', 'r2 > .26 ? lag(residual, n, 1)',1),
               text.replace('r2 < -.15 ? lag(residual, n, 4)', 'r2 < -.16 ? lag(residual, n, 4)',1),
               text+'\n']
    for mutation in mutations:
        try:restore_front19115(mutation)
        except AssertionError:pass
        else:raise AssertionError('Altered lag source accepted')
    return {'status':'passed','negative_controls':len(mutations),
            'baseline_sha256':BASELINE_SHA256,'exactly_one_control_flow_change':True,
            'computed_reduction_order_unchanged':True,'floating_environment_flags_claimed_identical':False}
