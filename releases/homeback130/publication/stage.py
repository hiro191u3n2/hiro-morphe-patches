#!/usr/bin/env python3
"""Download only pinned public build tools and the immutable published baseline."""
import argparse, io, urllib.request, zipfile
from pathlib import Path
import publish as pub

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--root',type=Path,required=True);a=p.parse_args()
    a.root.mkdir(parents=True,exist_ok=True);tools=a.root/'tools';tools.mkdir(exist_ok=True)
    rows={
        'morphe.jar':('https://github.com/MorpheApp/morphe-desktop/releases/download/v1.16.0/morphe-desktop-1.16.0-all.jar',None,None),
        'morphe22.jar':('https://github.com/MorpheApp/morphe-desktop/releases/download/v1.19.0-dev.19/morphe-desktop-1.19.0-dev.19-all.jar',None,None),
        'morphe200.jar':('https://github.com/MorpheApp/morphe-desktop/releases/download/v1.19.0/morphe-desktop-1.19.0-all.jar',None,None),
        'android.jar':('https://dl.google.com/android/repository/platform-35_r02.zip','0988cacad01b38a18a47bac14a0695f246bc76c1b06c0eeb8eb0dc825ab0c8e0','android-35/android.jar'),
        'd8.jar':('https://dl.google.com/android/repository/build-tools_r35_linux.zip','bd3a4966912eb8b30ed0d00b0cda6b6543b949d5ffe00bea54c04c81e1561d88','android-15/lib/d8.jar')}
    for name,(url,pin,entry) in rows.items():
        raw=pub.fetch(url)
        if pin:pub.require(pub.sha(raw)==pin,'Tool archive pin differs: '+name)
        if entry:
            with zipfile.ZipFile(io.BytesIO(raw)) as z:raw=z.read(entry)
        pub.require(pub.sha(raw)==pub.build.TOOLS[name],'Tool pin differs: '+name)
        (tools/name).write_bytes(raw);print('PIN VERIFIED '+name,flush=True)
    raw=pub.fetch(pub.BASE_URL);pub.require(pub.sha(raw)==pub.BASE_SHA256,'Baseline pin differs')
    (a.root/'baseline308.mpp').write_bytes(raw);print('PIN VERIFIED baseline308',flush=True)

if __name__ == '__main__':main()
