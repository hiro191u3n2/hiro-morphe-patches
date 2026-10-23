#!/usr/bin/env python3
"""Validate a complete Japanese catalog and generate bounded Java initializer chunks."""
import collections,csv,json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parent
FORMAT=re.compile(r'%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?(?:[tT][a-zA-Z]|[a-zA-Z%])')
EXPECTED=2181
def validate():
    english=list(csv.DictReader((ROOT/'l10n/en.csv').open(encoding='utf-8',newline='')))
    japanese=list(csv.DictReader((ROOT/'l10n/ja.csv').open(encoding='utf-8',newline='')))
    assert len(english)==len(japanese)==EXPECTED,'Catalog entry count differs'
    assert [r['source'] for r in english]==[r['source'] for r in japanese],'Keys or order differ'
    assert len({r['source'] for r in japanese})==EXPECTED,'Duplicate English keys'
    retained=[]
    for r in japanese:
        a,b=r['source'],r['target']
        assert b and b.strip(),'Empty translation: '+a
        x,y=FORMAT.findall(a),FORMAT.findall(b)
        if all('$' in v or v=='%%' for v in x):
            assert collections.Counter(x)==collections.Counter(y),'Numbered placeholders differ: '+a
        else: assert x==y,'Ordered placeholders differ: '+a
        assert a.count('\\n')==b.count('\\n'),'Line break count differs: '+a
        for token_pattern in (r'\{[A-Za-z_][A-Za-z0-9_]*\}',r'https?://[^\s<>"\)]+'):
            assert collections.Counter(re.findall(token_pattern,a))==collections.Counter(re.findall(token_pattern,b)),'Literal tokens differ: '+a
        assert '\x00' not in b and '\r' not in b,'Invalid control character'
        if a==b:retained.append(a)
    return japanese,retained
def literal(s):
    return json.dumps(s.replace('\\n','\n'),ensure_ascii=False)
def generate(out):
    rows,retained=validate();out=Path(out);out.mkdir(parents=True,exist_ok=True)
    header='''/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only
 * Japanese translation of the icysymmetra / Hushfeed English settings catalog.
 * Derived catalog and upstream notices are supplied in LICENSE and NOTICE.
 * Generated from src/l10n/ja.csv; edit the CSV, not this file.
 */
package app.hiro.tiktokjapanese.runtime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
public final class JapaneseTranslations {
    private JapaneseTranslations() {}
    public static Map<String,String> forLanguage(String tag) {
        return "ja".equals(tag) ? Holder.TABLE : null;
    }
    private static final class Holder {
        static final Map<String,String> TABLE = create();
    }
    private static Map<String,String> create() {
        Map<String,String> map = new LinkedHashMap<String,String>(3000);
'''
    chunks=[rows[i:i+60] for i in range(0,len(rows),60)]
    text=header+''.join('        fill%d(map);\n'%i for i in range(len(chunks)))
    text+='        return Collections.unmodifiableMap(map);\n    }\n'
    for i,chunk in enumerate(chunks):
        text+='    private static void fill%d(Map<String,String> map) {\n'%i
        text+=''.join('        map.put(%s, %s);\n'%(literal(r['source']),literal(r['target'])) for r in chunk)
        text+='    }\n'
    text+='}\n';(out/'JapaneseTranslations.java').write_text(text,encoding='utf-8')
    return {'entries':len(rows),'initializer_chunks':len(chunks),'retained_technical_or_brand_entries':retained,'placeholder_integrity':True,'newline_integrity':True}
if __name__=='__main__':
    import sys
    print(json.dumps(generate(sys.argv[1]),ensure_ascii=False,indent=2))
