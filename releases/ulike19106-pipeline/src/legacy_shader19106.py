"""Remove only diagnostic preprocessor branches for inherited exact-source gates.

The actual shader remains the input to every JNI/GLES execution. This helper is
used solely for the historical source-delta contract, alongside the independent
published-.105 native selector / all-program preprocessor preservation proof.
"""
import re


def production_text(text):
    default='#ifndef GX_TRACE19106\n#define GX_TRACE19106 0\n#endif\n'
    if text.count(default)>1:raise AssertionError('Duplicate diagnostic default')
    text=text.replace(default,'',1)
    result=[];depth=0;keep=False;blocks=0
    for line in text.splitlines():
        directive=line.strip()
        if depth==0:
            if directive=='#if GX_TRACE19106':
                depth=1;keep=False;blocks+=1
            else:
                result.append(line)
            continue
        if re.match(r'^#\s*(if|ifdef|ifndef)\b',directive):
            depth+=1
            if keep:result.append(line)
        elif re.match(r'^#\s*endif\b',directive):
            depth-=1
            if depth and keep:result.append(line)
        elif depth==1 and directive=='#else':
            keep=True
        elif depth==1 and directive.startswith('#elif'):
            raise AssertionError('Unreviewed diagnostic conditional alternative')
        elif keep:
            result.append(line)
    if depth:raise AssertionError('Unclosed diagnostic preprocessor branch')
    # Insertion of diagnostic statements may leave empty lines. Every other
    # byte of production statements and historical comments remains exact.
    return '\n'.join(line for line in result if line.strip())+'\n'
