"""Production output-renderer recovery in deterministic Android/SDK queue doubles."""
from pathlib import Path
import shutil
import subprocess

def test(root,work,androidjar=None):
    root,work=Path(root),Path(work)
    folder=work/'renderer1938-tests'
    folder.mkdir(parents=True,exist_ok=True)
    src,classes=folder/'src',folder/'classes'
    src.mkdir();classes.mkdir()
    for source in (root/'renderer1938-host').rglob('*.java'):
        dest=src/source.relative_to(root/'renderer1938-host')
        dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,dest)
    shutil.copyfile(root/'RenderStartup1938.java',src/'com/hiro/ulike/RenderStartup1938.java')
    for name in ('Scheduling1944','SpeedWorkers1935'):
        shutil.copyfile(root/(name+'.java'),src/('com/hiro/ulike/'+name+'.java'))
    command=['javac','-source','8','-target','8','-encoding','UTF-8','-d',str(classes)]+list(map(str,sorted(src.rglob('*.java'))))
    result=subprocess.run(command,capture_output=True,text=True)
    (folder/'javac.log').write_text(result.stdout+result.stderr);result.check_returncode()
    result=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.RenderTest1938'],capture_output=True,text=True)
    (work/'host-renderer1938.txt').write_text(result.stdout+result.stderr);result.check_returncode()
    return {'status':'passed','assertions':int(result.stdout.strip().split('=')[-1]),'device_tested':False}

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--work',type=Path,required=True);args=p.parse_args()
    args.work.mkdir(parents=True,exist_ok=True);print(test(Path(__file__).resolve().parent,args.work))
