"""Deterministic typed lowering of the binary64 policy body to GPU integer words.

No mathematical expression is evaluated here except encoding constant binary64
literals; all image-dependent arithmetic remains in the dispatched shader.
"""
from pathlib import Path
import re,struct

TOKEN=re.compile(r'\s+|//[^\n]*|/\*.*?\*/|(?:0x[0-9a-fA-F]+u?|\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?(?:LF|u)?|[A-Za-z_]\w*|\+\+|--|\+=|-=|\*=|/=|==|!=|<=|>=|&&|\|\||<<|>>|.',re.S)
TYPES={'D','int','uint','float','bool'}
PRIORITY={'=':1,'+=':1,'-=':1,'*=':1,'/=':1,'?':2,'||':3,'&&':4,'|':5,'^':6,'&':7,'==':8,'!=':8,'<':9,'<=':9,'>':9,'>=':9,'<<':10,'>>':10,'+':11,'-':11,'*':12,'/':12,'%':12}
RET={'floatBitsToInt':'int','roundD':'int','roundF':'int','luma':'int','validFace':'bool','acceptedFace':'bool','sine':'D','cosine':'D','ellipse':'D','faceSample':'int','noiseSigma':'float','smoothSample':'int'}

def literal(s):
    lo,hi=struct.unpack('<II',struct.pack('<d',float(s[:-2])))
    return 'uvec2(0x%08xu,0x%08xu)'%(lo,hi)

def constant(s):
    m=re.fullmatch(r'uvec2\(0x([0-9a-f]{8})u,0x([0-9a-f]{8})u\)',s)
    if m:return struct.unpack('<d',struct.pack('<II',int(m[1],16),int(m[2],16)))[0]
    if s.startswith('s64_neg(') and s.endswith(')'):
        value=constant(s[8:-1]);return None if value is None else -value
    return None

class Lower:
    def __init__(self,source):
        self.tokens=[x.group() for x in TOKEN.finditer(source) if not x.group().isspace() and not x.group().startswith(('//','/*'))]
        self.at=0;self.env={};self.returnType='void'
    def peek(self):return self.tokens[self.at] if self.at<len(self.tokens) else ''
    def take(self,expected=None):
        t=self.peek();self.at+=1
        if expected is not None:assert t==expected,(expected,t,self.tokens[max(0,self.at-12):self.at+12])
        return t
    def cast(self,v,typ):
        s,t=v
        if typ==t:return s
        if typ=='D':return ('s64_fromFloat' if t=='float' else 's64_fromUint' if t=='uint' else 's64_fromInt')+'('+s+')'
        if t=='D':return ('s64_toFloat' if typ=='float' else 's64_toInt')+'('+s+')'
        return typ+'('+s+')'
    def op(self,a,op,b):
        typ='D' if 'D' in (a[1],b[1]) else 'float' if 'float' in (a[1],b[1]) else a[1]
        if op in ('=','+=','-=','*=','/='):
            if op=='=':return a[0]+'='+self.cast(b,a[1]),a[1]
            value=self.op(a,op[0],b);return a[0]+'='+value[0],a[1]
        if typ=='D' and op in ('+','-','*','/','==','!=','<','<=','>','>='):
            # Compile-time scalar constants, never detector/image metadata.
            ca,cb=constant(a[0]),constant(b[0])
            if ca is not None and cb is not None and op in ('+','-','*','/'):
                v={'+':lambda:ca+cb,'-':lambda:ca-cb,'*':lambda:ca*cb,'/':lambda:ca/cb}[op]()
                lo,hi=struct.unpack('<II',struct.pack('<d',v))
                return 'uvec2(0x%08xu,0x%08xu)'%(lo,hi),'D'
            funcs={'+':'add','-':'sub','*':'mul','/':'div','==':'eq','<':'lt','<=':'le','>':'gt','>=':'ge','!=':'eq'}
            s='s64_'+funcs[op]+'('+self.cast(a,'D')+','+self.cast(b,'D')+')'
            if op=='!=':s='!'+s
            return s,'bool' if op in ('==','!=','<','<=','>','>=') else 'D'
        return '('+a[0]+op+b[0]+')','bool' if op in ('==','!=','<','<=','>','>=','&&','||') else typ
    def expr(self,minp=0):
        tok=self.take()
        if tok in ('-','+','!','~'):
            a=self.expr(13)
            left=('s64_neg('+a[0]+')' if a[1]=='D' and tok=='-' else '('+tok+a[0]+')',a[1])
        elif tok=='(':
            left=self.expr();self.take(')')
        elif tok.endswith('LF'):left=(literal(tok),'D')
        elif re.match(r'^(?:\d|\.)',tok):left=(tok,'float' if '.' in tok or 'e' in tok.lower() else 'uint' if tok.endswith('u') else 'int')
        else:left=(tok,self.env.get(tok,'bool' if tok in ('true','false') else 'int'))
        while True:
            tok=self.peek()
            if tok=='.':
                self.take();prop=self.take();left=(left[0]+'.'+prop,'D' if left[0]=='metadata' else 'float' if left[0] in ('grid','sigmaData') else 'uint' if left[0]=='source' else 'int');continue
            if tok=='[':
                self.take();index=self.expr();self.take(']');left=(left[0]+'['+index[0]+']',left[1]);continue
            if tok=='(':
                self.take();args=[]
                while self.peek()!=')':
                    args.append(self.expr())
                    if self.peek()!=',':break
                    self.take(',')
                self.take(')');name=left[0];typ=RET.get(name,args[0][1] if args else 'int')
                if name in TYPES and len(args)==1:
                    left=(self.cast(args[0],name) if name=='D' or args[0][1]=='D' else name+'('+args[0][0]+')',name);continue
                if name in ('min','max','clamp') and any(a[1]=='D' for a in args):
                    aa=[self.cast(a,'D') for a in args]
                    s='s64_'+name+'('+','.join(aa)+')' if name!='clamp' else 's64_min(s64_max('+aa[0]+','+aa[1]+'),'+aa[2]+')';left=(s,'D');continue
                if name in ('abs','sqrt','floor') and args[0][1]=='D':left=('s64_'+name+'('+args[0][0]+')','D');continue
                if name in ('sine','cosine','ellipse','roundD'):args=[(self.cast(a,'D'),'D') for a in args]
                left=(name+'('+','.join(a[0] for a in args)+')',typ);continue
            if tok in ('++','--'):
                self.take();left=(left[0]+tok,left[1]);continue
            if tok not in PRIORITY or PRIORITY[tok]<minp:break
            self.take();p=PRIORITY[tok]
            if tok=='?':
                a=self.expr();self.take(':');b=self.expr(p);typ='D' if 'D' in (a[1],b[1]) else a[1]
                left=('('+left[0]+'?'+self.cast(a,typ)+':'+self.cast(b,typ)+')',typ)
            else:left=self.op(left,tok,self.expr(p if p==1 else p+1))
        return left
    def declaration(self,terminator=';'):
        qualifiers=[]
        while self.peek() in ('precise','out','inout','in'):qualifiers.append(self.take())
        typ=self.take();assert typ in TYPES
        if typ=='D':qualifiers=[q for q in qualifiers if q!='precise']
        out=[]
        while True:
            name=self.take();self.env[name]=typ;s=name
            if self.peek()=='=':self.take();s+='='+self.cast(self.expr(),typ)
            out.append(s)
            if self.peek()!=',':break
            self.take(',')
        self.take(terminator)
        return ' '.join(qualifiers+['uvec2' if typ=='D' else typ])+' '+','.join(out)+terminator
    def statement(self):
        tok=self.peek()
        if tok=='{':
            self.take();out=[]
            while self.peek()!='}':out.append(self.statement())
            self.take('}');return '{\n'+'\n'.join(out)+'\n}'
        if tok=='if':
            self.take();self.take('(');s='if('+self.expr()[0]+')';self.take(')');s+=self.statement()
            if self.peek()=='else':self.take();s+='else '+self.statement()
            return s
        if tok=='for':
            self.take();self.take('(');init=self.declaration();cond=self.expr()[0];self.take(';');inc=self.expr()[0];self.take(')')
            return 'for('+init+cond+';'+inc+')'+self.statement()
        if tok=='return':
            self.take();s='return'
            if self.peek()!=';':s+=' '+self.cast(self.expr(),self.returnType)
            self.take(';');return s+';'
        if tok in ('continue','break'):self.take();self.take(';');return tok+';'
        if tok in TYPES or tok=='precise':return self.declaration()
        s=self.expr()[0];self.take(';');return s+';'
    def functions(self):
        out=[]
        while self.peek():
            self.env={'u':'int','f':'float'};typ=self.take();name=self.take();self.returnType=typ;self.take('(');params=[]
            while self.peek()!=')':
                q=[]
                while self.peek() in ('out','inout','in'):q.append(self.take())
                t=self.take();n=self.take();self.env[n]=t;params.append(' '.join(q+['uvec2' if t=='D' else t,n]))
                if self.peek()!=',':break
                self.take(',')
            self.take(')');out.append(('uvec2' if typ=='D' else typ)+' '+name+'('+','.join(params)+')'+self.statement())
        return '\n'.join(out)

def generate(root):
    root=Path(root);source=(root/'policy1965.comp').read_text();start=source.index('int roundD')
    header=source[:start].replace('#extension GL_EXT_shader_explicit_arithmetic_types_float64 : require\n','').replace('#define D float64_t','#define D uvec2').replace('readonly buffer Metadata {D values[];}','readonly buffer Metadata {uvec2 values[];}')
    # Library expansion is performed by the native shader packer, identically to
    # the original GLES fixture. No dialect adaptation occurs in software mode.
    header=header.replace('layout(local_size_x=64) in;','/* SOFT64_1965_LIBRARY */\nlayout(local_size_x=64) in;')
    (root/'policy1965_soft.comp').write_text(header+Lower(source[start:]).functions()+'\n')

if __name__=='__main__':generate(Path(__file__).resolve().parent)
