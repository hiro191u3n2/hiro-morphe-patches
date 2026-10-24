"""Production layout regression; prove the previous coordinator stalls with the same fixture."""
from pathlib import Path
import argparse,re,subprocess

def test(root,work,androidjar=None):
    root,work=Path(root).resolve(),Path(work).resolve()
    folder=work/'layout1937-host';folder.mkdir(parents=True,exist_ok=True)
    sources=sorted((root/'layout1937-host').rglob('*.java'))
    logs=[]
    for baseline in (True,False):
        name='baseline1936' if baseline else 'production1937'
        classes=folder/name;classes.mkdir(parents=True,exist_ok=True)
        helper=root/('layout1937-baseline/PreviewLayout1922.java' if baseline else 'PreviewLayout1922.java')
        result=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',str(classes),str(helper),*map(str,sources)],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
        (folder/(name+'-compile.txt')).write_text(result.stdout);result.check_returncode()
        result=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.LayoutHost1937',*(['baseline'] if baseline else [])],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
        (folder/(name+'.txt')).write_text(result.stdout);result.check_returncode();logs.append(result.stdout)
    if 'BASELINE1936_STALLED_MASK_REPRODUCED=811' not in logs[0]:raise RuntimeError('Previous defect not reproduced')
    match=re.search(r'^HOST_LAYOUT1937_ASSERTIONS=(\d+)$',logs[1],re.M)
    if not match:raise RuntimeError('Layout regression did not finish')
    combined=''.join(logs);(work/'host-layout1937.txt').write_text(combined);print(combined,end='')
    return {'status':'passed','assertions':int(match.group(1)),'baseline':'1.9.36 persistent 811px viewport reproduced','scope':'production Java coordinator; modeled Android animation/View, no physical device'}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=Path(__file__).resolve().parent);p.add_argument('--work',required=True,type=Path)
    a=p.parse_args();test(a.root,a.work)
