from pathlib import Path
import re,subprocess

def test(root,work):
 classes=work/'shadow-host-classes';classes.mkdir()
 r=subprocess.run(['javac','--release','8','-d',str(classes),*[str(p) for p in sorted((root/'noise-stubs').rglob('*.java'))],str(root/'ShadowDetail1923.java'),str(root/'ShadowTests1923.java')],capture_output=True,text=True)
 (work/'shadow-javac.log').write_text(r.stdout+r.stderr);r.check_returncode()
 r=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.ShadowTests1923'],capture_output=True,text=True);(work/'host-tests1923.txt').write_text(r.stdout+r.stderr);print(r.stdout+r.stderr);r.check_returncode()
 return int(re.search(r'HOST_SHADOW_ASSERTIONS=(\d+)',r.stdout)[1])
