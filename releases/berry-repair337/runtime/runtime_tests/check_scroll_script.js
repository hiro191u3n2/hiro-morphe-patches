const fs = require('fs');
const vm = require('vm');
const path = require('path');
const script = fs.readFileSync(path.join(process.argv[2], 'post_scroll.js'), 'utf8');
let assertions = 0;
function check(value, message) { assertions++; if (!value) throw Error(message); }
function denied(name) { return () => { throw Error('Forbidden page-content read or mutation: ' + name); }; }
function prohibit(o, fields) { for (const key of fields) Object.defineProperty(o, key, {get: denied(key), set: denied(key), configurable:true}); return o; }
function readonly(o) { return new Proxy(o, {set: denied('assignment'), deleteProperty: denied('delete'), defineProperty: denied('defineProperty')}); }
function sample({images=3, viewport=true, broken=false, marker=true, markerId='testmarker123', markerSerial=1, zeroRects=false, scrollingElement=true, code=script}={}) {
  let imageReads=0, rectReads=0, styleReads=0;
  function image(i) {
    imageReads++;
    return readonly(prohibit({nodeType:1, tagName:'IMG', complete:i%2===0,
      naturalWidth:i===2?0:100, naturalHeight:i===2?0:120,
      getBoundingClientRect(){rectReads++;if(zeroRects)return {left:0,top:0,right:0,bottom:0,width:0,height:0};return {left:10,top:i%3===0?50:2000,right:110,bottom:i%3===0?170:2120,width:100,height:120};}},
      ['src','currentSrc','alt','title','textContent','innerText','innerHTML','outerHTML','value','dataset']));
  }
  const imageList=new Proxy({length:images,item:i=>image(i)}, {get(o,k){if(/^\d+$/.test(String(k)))return image(Number(k));return o[k];},set:denied('image-list-write')});
  const root=readonly(prohibit({clientWidth:360,clientHeight:700,scrollWidth:360,scrollHeight:6000,
    getBoundingClientRect(){return {left:0,top:-1200,right:360,bottom:4800,width:360,height:6000};}},
    ['textContent','innerText','innerHTML','outerHTML','title']));
  const doc=readonly(prohibit({documentElement:root,body:root,scrollingElement:scrollingElement?root:null,images:imageList,
    querySelectorAll(selector){if(selector==='img')return imageList;throw Error('Unbounded or unsupported DOM query: '+selector);},
    createTreeWalker:denied('createTreeWalker'),createRange:denied('createRange'),createElement:denied('createElement'),
    addEventListener:denied('listener-write')},['URL','documentURI','title','cookie','referrer']));
  const vv=viewport?readonly({width:360,height:700,offsetLeft:0,offsetTop:0,pageLeft:0,pageTop:1200,scale:1}):undefined;
  const w={innerWidth:360,innerHeight:700,scrollX:0,scrollY:1200,pageXOffset:0,pageYOffset:1200,devicePixelRatio:3,
    visualViewport:vv,document:doc,
    __hiroBerryBFCacheRestoreV1:marker?readonly({id:markerId,serial:markerSerial,pending:true}):undefined,
    getComputedStyle(){styleReads++;return readonly({display:'block',visibility:'visible',opacity:'1'});},
    scrollTo:denied('scrollTo'),scrollBy:denied('scrollBy'),requestAnimationFrame:denied('requestAnimationFrame'),
    dispatchEvent:denied('dispatchEvent'),addEventListener:denied('addEventListener'),setTimeout:denied('setTimeout')};
  prohibit(w,['location']);const windowProxy=readonly(w);w.top=windowProxy;
  if(broken)Object.defineProperty(w,'innerWidth',{get(){return NaN;}});
  const context=vm.createContext({window:windowProxy,document:doc,
    getComputedStyle:w.getComputedStyle,performance:{now:()=>12.5},
    innerWidth:360,innerHeight:700,scrollX:0,scrollY:1200,devicePixelRatio:3,
    setTimeout:denied('setTimeout'),requestAnimationFrame:denied('requestAnimationFrame')});
  let result=vm.runInContext(code,context,{timeout:1000});
  if(typeof result==='string')result=JSON.parse(result);
  return {result,imageReads,rectReads,styleReads};
}
let s=sample();
check(Array.isArray(s.result)&&s.result.length===30,'probe returns the fixed 30-number schema');
check(s.result[29]===1,'actual scrolling element presence is explicit');
check(s.result[19]===3&&s.result[20]===3&&s.result[21]===1&&s.result[22]===1&&s.result[23]===1,'image statistics distinguish scanned, intersecting, complete and nonzero-natural-size images');
check(s.result.every(n=>typeof n==='number'&&Number.isFinite(n)),'probe exports finite numbers only');
check(s.result.includes(360)&&s.result.includes(700)&&s.result.includes(1200),'viewport and scroll values are represented');
check(s.imageReads>0&&s.rectReads>0,'visible image status is sampled');
check(s.imageReads<=128&&s.rectReads<=128,'image inspection has a fixed small cap');
const before=s.result.length;
s=sample({images:1000000});
check(s.imageReads<=128&&s.rectReads<=128,'million-image document still has bounded image work');
check(s.result.length===before,'page size does not change numeric schema');
check(s.result[19]===1000000&&s.result[20]===128&&s.result[25]===1,'scan cap is explicit and does not pretend every image was inspected');
s=sample({images:0,viewport:false});
check(Array.isArray(s.result)&&s.result.every(n=>typeof n==='number'&&Number.isFinite(n)),'missing visualViewport and no images are handled');
check(s.imageReads===0&&s.rectReads===0,'empty image collection does not walk nodes');
s=sample({zeroRects:true});
check(s.result[24]===3&&s.result[21]===0,'zero-size image rectangles are distinguished from intersecting images');
s=sample({scrollingElement:false});
check(s.result[29]===0&&s.result[9]===360&&s.result[10]===6000,'missing scrollingElement uses root dimensions and records fallback');
s=sample({marker:false});
check(s.result===null||Array.isArray(s.result),'absent marker is handled without creating page state');
const guarded=fs.readFileSync(path.join(process.argv[2],'post_scroll_guarded.js'),'utf8');
check(sample({code:guarded,markerId:'abcdefgh',markerSerial:7}).result!==null,'matching marker identity permits numeric sample');
check(sample({code:guarded,markerId:'abcdefgh',markerSerial:8}).result===null,'same document with newer restore serial rejects stale scope');
check(sample({code:guarded,markerId:'different',markerSerial:7}).result===null,'different document marker rejects sample');
check(sample({code:guarded,marker:false}).result===null,'required marker absence rejects without writing a marker');
check(!/\b(?:textContent|innerText|innerHTML|outerHTML|currentSrc)\b/.test(script),'script excludes page text and image address reads');
console.log(JSON.stringify({result:'PASS',assertions,scope:'Read-only numeric DOM probe in Node VM; bounded mock DOM, no browser rendering test'}));
