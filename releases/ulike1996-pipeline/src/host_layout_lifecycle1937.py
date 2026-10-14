"""Execute production lifecycle guards and generation-bound task wrappers with native ABI fixtures."""
from pathlib import Path
import argparse,re,subprocess

def test(root,work,androidjar=None):
    root,work=Path(root).resolve(),Path(work).resolve()
    folder=work/'layout-lifecycle1937-host';classes=folder/'classes';classes.mkdir(parents=True,exist_ok=True)
    sources=sorted((root/'layout1937-host').rglob('*.java'))+sorted((root/'layout-lifecycle1937-host').rglob('*.java'))
    result=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',str(classes),str(root/'PreviewLayout1922.java'),str(root/'LayoutLifecycle1937.java'),*map(str,sources)],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    (folder/'compile.txt').write_text(result.stdout);result.check_returncode()
    result=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.LayoutLifecycleHost1937'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    (work/'host-layout-lifecycle1937.txt').write_text(result.stdout);result.check_returncode()
    match=re.search(r'^HOST_LAYOUT_LIFECYCLE1937_ASSERTIONS=(\d+)$',result.stdout,re.M)
    if not match:raise RuntimeError('Lifecycle regressions did not complete')
    print(result.stdout,end='')
    return {'status':'passed','assertions':int(match.group(1)),'scope':'production lifecycle and core coordinator; modeled native callbacks/View/Handler, no physical device'}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=Path(__file__).resolve().parent);p.add_argument('--work',required=True,type=Path)
    a=p.parse_args();test(a.root,a.work)
