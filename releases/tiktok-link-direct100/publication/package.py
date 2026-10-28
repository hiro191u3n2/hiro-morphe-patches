#!/usr/bin/env python3
"""Package exact proofs and corresponding text source; do not distribute TikTok APKs."""
import argparse, json
from pathlib import Path
import publish as pub

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--dist',type=Path,required=True); p.add_argument('--base',type=Path,required=True)
    a = p.parse_args(); qa,real,artifacts = pub.validate_build(a.dist,a.base)
    cfg = json.loads((pub.HERE/'expected.json').read_text())
    pub.require(cfg['source_sha256'] == pub.source_hashes() and cfg['artifacts'] == artifacts
                and cfg['qa_sha256'] == pub.sha(qa) and cfg['real_apk_sha256'] == pub.sha(real), 'Reviewed source/output identity differs')
    (a.dist/pub.REAL_QA_NAME).write_bytes(real)
    pub.build.write_zip(a.dist/pub.SOURCE_ZIP,pub.expected_source_archive(qa,real))
    (a.dist/'RELEASE_NOTES.txt').write_bytes((pub.HERE/'RELEASE_NOTES.txt').read_bytes())
    (a.dist/'SHA256SUMS.txt').write_text(''.join(pub.sha_file(a.dist/name)+'  '+name+'\n' for name in pub.ASSETS if name != 'SHA256SUMS.txt'))
    print('PASS packaged seven exact assets, source, notices and real evidence; no APK')

if __name__ == '__main__': main()
