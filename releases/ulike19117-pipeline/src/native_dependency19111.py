"""One reviewed diagnostic exclusion; restore the complete historical manifest.

The numerical/native inventory and fingerprint algorithm remain unchanged.
Only the new bounded timing observer is classified outside numerical identity.
"""
import json

NAME='ResidentTiming19111.java'
REASON='Bounded post-save resident timing observer; cannot alter numerical policies or grant quality or speed admission.'
DELTA={'excluded_runtime_java':{NAME:REASON}}
LINE=('    '+json.dumps(NAME)+': '+json.dumps(REASON)+',\n').encode()
ANCHOR=b'    "StrongTrace19106.java": '

def restore(current,original):
    if type(current) is not bytes or type(original) is not bytes:
        raise AssertionError('Exact dependency manifest bytes required')
    if NAME.encode() in original or original.count(ANCHOR)!=1:
        raise AssertionError('Frozen manifest diagnostic baseline differs')
    expected=original.replace(ANCHOR,LINE+ANCHOR,1)
    if current!=expected or current.count(LINE)!=1:
        raise AssertionError('Unreviewed .111 dependency classification delta')
    restored=current.replace(LINE,b'',1)
    if restored!=original:
        raise AssertionError('Dependency inverse differs from complete frozen manifest')
    return restored

def negative_controls(current,original):
    if restore(current,original)!=original:raise AssertionError('Manifest inverse failed')
    assertions=1
    mutations=[original,current+LINE,current.replace(LINE,b'',1),current.replace(LINE,LINE+LINE,1),
        current.replace(NAME.encode(),b'Unreviewed111.java',1),current.replace(REASON.encode(),b'Numerical processing.',1),
        current.replace(b'"numerical_java": [',b'"numerical_java": ["ResidentTiming19111.java",',1),
        current.replace(ANCHOR,b'    "ChangedExisting19106.java": ',1),
        current+b'\n',current.replace(b'"native_dependencies": [',b'"native_dependencies": ["unreviewed.c",',1)]
    for raw in mutations:
        if raw==current:raise AssertionError('Manifest mutation did not change bytes')
        try:restore(raw,original)
        except AssertionError:assertions+=1
        else:raise AssertionError('Malformed dependency manifest accepted')
    return assertions
