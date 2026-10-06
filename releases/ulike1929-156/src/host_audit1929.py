"""Execute the shipped 1929 Java helpers against deterministic Android/camera doubles.
This is not a physical camera test and does not simulate Android ART or Camera HAL.
"""
from pathlib import Path
import subprocess,shutil

def test(root,work):
    folder=work/'audit-tests';folder.mkdir();src=folder/'src';src.mkdir();classes=folder/'classes';classes.mkdir()
    for p in (root/'audit-stubs').rglob('*.java'):
        dest=src/p.relative_to(root/'audit-stubs');dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dest)
    for p in (root/'audit-host').rglob('*.java'):
        dest=src/p.relative_to(root/'audit-host');dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dest)
    for n in ['ProviderLifecycle1929','PreviewInputs1929','SessionFallback1929']:
        shutil.copyfile(root/(n+'.java'),src/('com/hiro/ulike/'+n+'.java'))
    cp=subprocess.run(['javac','--release','8','-encoding','UTF-8','-d',str(classes),*map(str,sorted(src.rglob('*.java')))],capture_output=True,text=True)
    (folder/'javac.log').write_text(cp.stdout+cp.stderr);cp.check_returncode()
    cp=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.AuditTest1929'],capture_output=True,text=True)
    (work/'host-audit1929.txt').write_text(cp.stdout+cp.stderr);cp.check_returncode();return int(cp.stdout.strip().split('=')[-1])
