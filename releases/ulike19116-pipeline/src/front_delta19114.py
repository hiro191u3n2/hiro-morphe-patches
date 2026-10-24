"""Exact reviewed transform alias-contract delta, with unchanged arithmetic."""
from pathlib import Path
import hashlib,re
BASELINE_SHA256='db3eef06e6fa3fffc324255e856cdd871abd07f0eb8b078dc469275d0a99b780'
OLD_COMMENT='/* H28 processes two independent transform outputs in double lanes.'
NEW_COMMENT='''/* .114: values and temp are distinct Workspace arrays at every production
 * call site (including the residual exporter). Their non-overlap is explicit
 * so stores into temp cannot force reloads of the unchanged source values.
 * H28 processes two independent transform outputs in double lanes.'''
OLD_ARGUMENTS='double *values, double *temp, int n, int inverse'
NEW_ARGUMENTS='double *restrict values, double *restrict temp, int n, int inverse'
def restore_front19114(text,source=None):
    from source_inverse19115 import ROOT,restore as restore115
    historical=restore115(Path(source or ROOT),'native1955/single_noise1955.c',text.encode()).decode()
    return _restore114(historical)

def _restore114(text):
    """Historical alias proof only, after the complete reviewed .115 inverse."""
    if text.count(NEW_COMMENT)!=1 or text.count(NEW_ARGUMENTS)!=2:
        raise AssertionError('Missing exact transform alias delta')
    restored=text.replace(NEW_COMMENT,OLD_COMMENT).replace(NEW_ARGUMENTS,OLD_ARGUMENTS)
    if hashlib.sha256(restored.encode()).hexdigest()!=BASELINE_SHA256:
        raise AssertionError('Front arithmetic or non-reviewed source changed')
    return restored

def audit(source):
    source=Path(source);kernel=source/'native1955/single_noise1955.c'
    from source_inverse19115 import restore as restore115,MANIFEST_SHA256
    historical=restore115(source,'native1955/single_noise1955.c',kernel.read_bytes()).decode()
    _restore114(historical)
    expected=['w->coefficient[plane], w->temp, n, 0','w->coefficient[plane], w->temp, n, 1']
    files=['native1955/single_noise1955.c','native1960/residual_blocks1961.c']
    for name in files:
        text=(source/name).read_text()
        calls=re.findall(r'\btransform\(([^;{}]+)\);',text)
        if calls!=expected:raise AssertionError('Unreviewed transform callers: '+name)
    includes=[]
    for directory in source.glob('native[0-9]*'):
        if directory.is_dir():
            for f in directory.glob('*.c'):
                if re.search(r'#include\s+"[^"\n]*single_noise1955\.c"',f.read_text()):includes.append(f.relative_to(source).as_posix())
    if includes!=['native1960/residual_blocks1961.c']:
        raise AssertionError('Unreviewed native transform include translation unit')
    # Deliberately corrupt restored .114, so the original alias guard rejects
    # both controls instead of the new .115 full-source guard rejecting first.
    # The call-site and translation-unit audit above still reads current .115.
    mutations=[historical.replace(NEW_ARGUMENTS,OLD_ARGUMENTS,1),historical.replace(NEW_COMMENT,OLD_COMMENT,1)]
    for mutation in mutations:
        try:_restore114(mutation)
        except AssertionError:pass
        else:raise AssertionError('Corrupt alias contract accepted')
    return {'status':'passed','production_call_sites':4,'translation_units':files,
            'only_disjoint_workspace_arrays':True,'arithmetic_source_unchanged':True,
            'strict_restore_baseline_sha256':BASELINE_SHA256,'negative_controls':2,
            'arithmetic_source_scope':'restored114_source',
            'historical_negative_scope19115':'restored114_source',
            'current_production_callsites_audited19115':True,
            'current_front_source_inverse_verified19115':True,
            'complete_source_inverse_manifest_sha256':MANIFEST_SHA256}
