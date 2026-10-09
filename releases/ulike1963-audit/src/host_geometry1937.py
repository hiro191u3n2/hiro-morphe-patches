"""Exercise actual production geometry helper using independent native ownership fixtures."""
from pathlib import Path
import argparse,json,subprocess

def test(root,work,androidjar=None):
    root,work=Path(root).resolve(),Path(work).resolve()
    folder=work/'geometry1937-host';folder.mkdir(parents=True,exist_ok=True)
    sources={}
    for name in ('layout1937-stubs','geometry1937-compile','geometry1937-host'):
        for p in (root/name).rglob('*.java'):sources[str(p.relative_to(root/name))]=p
    # Only the legacy controller is needed from lifecycle stubs.
    sources={key:p for key,p in sources.items() if not str(p).startswith(str(root/'layout1937-stubs')) or key=='i/o/a/m/j/f.java'}
    sources['android/graphics/RectF.java']=root/'layout1937-host/android/graphics/RectF.java'
    classes=folder/'classes';classes.mkdir(parents=True,exist_ok=True)
    result=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',str(classes),str(root/'LayoutGeometry1937.java'),*map(str,sources.values())],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    (folder/'compile.txt').write_text(result.stdout);result.check_returncode()
    result=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.GeometryHost1937'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    (work/'host-geometry1937.txt').write_text(result.stdout);result.check_returncode()
    data=json.loads(result.stdout);data['assertions']=data.pop('checks');data['scope']='production Java geometry helper; modeled Android/native owners, no physical device';print(result.stdout,end='');return data

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=Path(__file__).resolve().parent);p.add_argument('--work',required=True,type=Path)
    a=p.parse_args();test(a.root,a.work)
