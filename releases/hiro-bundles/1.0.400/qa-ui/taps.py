import re, time, subprocess as sp, xml.etree.ElementTree as ET
from pathlib import Path
def adb(*args, timeout=60):
 r=sp.run(['adb',*args],timeout=timeout,capture_output=True,text=True)
 return r.stdout.strip()
def shell(*args):return adb('shell','-n',*args)
pkg='jp.ejimax.berrybrowser'
def activity():return shell('dumpsys activity activities | grep -m2 -E "topResumedActivity|mResumedActivity"')
def fixture():
 # Navigate back to Berry for each case. Force-stopping marks a freshly
 # installed receiver stopped, unlike a normally used Instagram install.
 shell('am','start','-a','android.intent.action.VIEW','-d','http://127.0.0.1:8787/','-p',pkg)
def nodes():
 adb('shell','uiautomator','dump','/data/local/tmp/bui.xml')
 data=adb('shell','cat','/data/local/tmp/bui.xml')
 try:return list(ET.fromstring(data[data.index('<?xml'):]).iter('node'))
 except (ValueError,ET.ParseError):return []
def tap(node):
 x,y,X,Y=map(int,re.findall(r'\d+',node.get('bounds')))
 shell('input','tap',str((x+X)//2),str((y+Y)//2))
# Exercise the installed application's launcher on first install before link tests.
print('installed', shell('pm','list','packages','--show-versioncode','com.instagram.android'),flush=True)
print('handler',shell('cmd','package','resolve-activity','--brief','-a','android.intent.action.VIEW','-c','android.intent.category.BROWSABLE','-d','https://www.instagram.com/p/QA_Berry12/','-p','com.instagram.android'),flush=True)
shell('monkey','-p','com.instagram.android','-c','android.intent.category.LAUNCHER','1')
time.sleep(4)
for label,path in [('Instagram Post','/p/QA_Berry12/'),('Instagram Profile','/instagram/')]:
 fixture()
 end=time.monotonic()+110
 while time.monotonic()<end:
  ns=nodes()
  found=next((n for n in ns if label in (n.get('text'),n.get('content-desc'))),None)
  if found is not None:break
  info=[n.get('text') or n.get('content-desc') or '' for n in ns]
  print('screen',info[:35],flush=True)
  consent=next((n for n in ns if n.get('text','').lower() in ('ok','continue','get started','next','start','allow','閉じる','次へ','開始','続行')),None)
  if consent is not None:tap(consent)
  time.sleep(2)
 else:raise SystemExit('Missing fixture link: '+label+' / '+activity())
 print('tap_node',found.attrib,'before',activity(),flush=True)
 adb('logcat','-c')
 tap(found)
 for i in range(35):
  logs=adb('logcat','-d','-s','ActivityTaskManager:I')
  launched=[l for l in logs.splitlines() if 'START u0' in l and 'com.instagram.android/' in l]
  fg=activity()
  if launched and 'com.instagram.android/' in fg:
   print(label,fg,launched[:3],flush=True);break
  time.sleep(1)
 else:
  ns=nodes()
  print('screen_after',[(n.get('text'),n.get('content-desc'),n.get('bounds')) for n in ns if n.get('text') or n.get('content-desc')][:65],flush=True)
  print('relevant_android_log', '\\n'.join(l for l in adb('logcat','-d','-t','1200').splitlines() if any(k in l for k in ('ActivityTaskManager', 'chromium', 'AndroidRuntime', 'Intent blocked', 'Permission Denial', 'Background activity', 'am_proc_start', 'instagr')))[-6000:],flush=True)
  raise SystemExit('No handoff: '+label+' '+str(launched)+' '+fg)
print('Both actual taps opened Instagram',flush=True)
