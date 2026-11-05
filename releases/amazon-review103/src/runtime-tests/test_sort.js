'use strict';
const assert = require('node:assert/strict');
const {parseHTML} = require('linkedom');
const sorter = require('../runtime/review-sort.js');
let assertions = 0;
function check(value,message) { assert.ok(value,message); assertions++; }
function equal(actual,expected,message) { assert.deepEqual(actual,expected,message); assertions++; }
function asin(n) {return 'B'+String(n).padStart(9,'0');}
function card(n,count=n,id='p'+n) {return `<div class="s-result-item" data-component-type="s-search-result" data-asin="${asin(n)}" id="${id}"><h2>商品${n}</h2><div data-cy="reviews-block">${count===null?'':`<a href="#customerReviews"><span class="s-underline-text">${count}</span></a>`}</div></div>`;}
function batch(from,to) {return Array.from({length:to-from+1},(_,i)=>card(from+i)).join('');}
function fixture(body,url='https://www.amazon.co.jp/s?k=test') {
    const parsed=parseHTML(`<html><body>${body}</body></html>`), listeners=new Map(), pending=new Map();
    let clock=10000, serial=1;
    const win={document:parsed.document,MutationObserver:parsed.window.MutationObserver,Event:parsed.window.Event,location:new URL(url),
        performance:{now:()=>clock},addEventListener(type,fn){if(!listeners.has(type))listeners.set(type,new Set());listeners.get(type).add(fn);},
        removeEventListener(type,fn){listeners.get(type)?.delete(fn);},
        dispatchEvent(event){Array.from(listeners.get(event.type)||[]).forEach(fn=>fn(event));return true;},
        setTimeout(fn,delay=0){const id=serial++;pending.set(id,{fn,due:clock+delay});return id;},clearTimeout(id){pending.delete(id);}};
    win.history={pushState(_s,_t,to){if(to)win.location=new URL(to,win.location.href);return 7;},replaceState(_s,_t,to){if(to)win.location=new URL(to,win.location.href);return 8;}};
    const f={window:win,document:win.document,pending,emit(type,props={}){win.dispatchEvent(Object.assign({type},props));},
        advance(ms){clock+=ms;for(const [id,t]of Array.from(pending)){if(t.due<=clock&&pending.has(id)){pending.delete(id);t.fn();}}},
        flush(){if(pending.size)f.advance(Math.max(0,Math.min(...Array.from(pending.values()).map(t=>t.due))-clock));},
        now:()=>clock};
    return f;
}
const productNodes=doc=>Array.from(doc.querySelectorAll('[data-component-type="s-search-result"]')).filter(n=>!n.classList.contains('AdHolder'));
const ids=doc=>productNodes(doc).map(n=>n.id);
const badge=f=>f.document.getElementById('hiro-review-count-status');
function layout(f,y=0) {
    const win=f.window; win.innerHeight=400; win.scrollY=y;win.scrollX=0;f.scrolls=[];
    Object.defineProperty(f.document.documentElement,'clientHeight',{configurable:true,get:()=>400});
    Object.defineProperty(f.document.documentElement,'scrollHeight',{configurable:true,get:()=>productNodes(f.document).length*100+700});
    f.rects=()=>productNodes(f.document).forEach(node=>{node.getBoundingClientRect=()=>{
        const index=productNodes(f.document).indexOf(node), label=badge(f),offset=label&&label.getAttribute('data-hiro-state')==='frozen'?24:0;
        return {top:index*100+offset-win.scrollY,bottom:(index+1)*100+offset-win.scrollY,height:100};
    };});
    win.scrollTo=(x,value)=>{f.scrolls.push(value);win.scrollY=Math.max(0,Math.min(value,f.document.documentElement.scrollHeight-win.innerHeight));f.emit('scroll');if(f.onScroll)f.onScroll();};
    f.rects();return f;
}
async function mutations(){await Promise.resolve();await new Promise(resolve=>setImmediate(resolve));}
async function settle(f,limit=80){for(let i=0;i<limit&&f.pending.size;i++){f.flush();await mutations();}check(!f.pending.size,'Bounded timers settle');}
function countText(f,n,text){f.document.querySelector('#p'+n+' .s-underline-text').textContent=text;}
function track(f){let insertions=0,badgeWrites=0;const old=f.document.documentElement.constructor.prototype.insertBefore;
    const proto=f.document.documentElement.constructor.prototype;
    proto.insertBefore=function(node,before){if(node.nodeType===1&&node.matches?.('[data-component-type="s-search-result"]'))insertions++;return old.call(this,node,before);};
    const observer=new f.window.MutationObserver(records=>{badgeWrites+=records.filter(r=>r.target.id==='hiro-review-count-status'||r.target.parentElement?.id==='hiro-review-count-status').length;});observer.observe(f.document,{subtree:true,childList:true,attributes:true,characterData:true});
    return {get insertions(){return insertions;},get badgeWrites(){return badgeWrites;},reset(){insertions=0;badgeWrites=0;},dispose(){proto.insertBefore=old;observer.disconnect();}};
}
(async()=>{
    assertions+=await require('./test_extractors.js').run();
    // Missing geometry means passive one-time ranking, not synthetic automatic scrolling.
    const f=fixture(`<main id="search"><div class="s-main-slot">${card(1,10)}${card(2,100)}${card(3,null)}</div></main>`);
    const original=f.document.getElementById('p2');let clicks=0;original.addEventListener('click',()=>clicks++);
    const state=sorter.install(f.window);
    equal(state.phase,'frozen');equal(ids(f.document),['p2','p1','p3']);original.dispatchEvent(new f.window.Event('click'));equal(clicks,1,'DOM handlers survive once-sort');
    check(badge(f).textContent.includes('先読み 3 商品で固定'));check(badge(f).textContent.includes('件数不明は末尾'));equal(badge(f).style.position,undefined,'Frozen badge is normal flow');
    await mutations();await settle(f);const monitor=track(f);
    f.document.querySelector('.s-main-slot').insertAdjacentHTML('beforeend',card(4,999999));await mutations();monitor.reset();await settle(f);
    equal(ids(f.document),['p2','p1','p3','p4'],'Late appended high count stays at end');equal(monitor.insertions,0,'Correct native append needs no movement');
    countText(f,1,'9999999');await mutations();await settle(f);equal(ids(f.document),['p2','p1','p3','p4'],'Changed counts never rerank frozen products');
    // Count hydration is not even read after freezing.
    const cardOne=f.document.getElementById('p1'), oldQuery=cardOne.querySelectorAll;
    cardOne.querySelectorAll=function(selector){if(selector.includes('aria-label'))throw new Error('Frozen count extraction forbidden');return oldQuery.call(this,selector);};
    state.runNow();check(badge(f).textContent.includes('先読み 3 商品で固定'),'Frozen normalization bypasses count extraction');cardOne.querySelectorAll=oldQuery;
    f.document.querySelector('.s-main-slot').insertAdjacentHTML('afterbegin',card(5,888888));await mutations();await settle(f);equal(ids(f.document),['p2','p1','p3','p4','p5'],'True native prepend placed after fixed cohort');
    f.document.querySelector('.s-main-slot').innerHTML=card(5,1)+card(1,999999)+card(4,999999)+card(3,999999)+card(2,0);await mutations();await settle(f);
    equal(ids(f.document),['p2','p1','p3','p4','p5'],'Whole-root same-ASIN replacements reuse frozen rank');
    monitor.reset();for(let i=0;i<5;i++){f.document.getElementById('p1').setAttribute('style',`opacity:${1-i/100}`);await mutations();await settle(f);}
    equal(monitor.insertions,0,'Rendering churn does not move frozen nodes');equal(monitor.badgeWrites,0,'Frozen badge is idempotent');
    const frozen=ids(f.document);f.window.history.pushState({},'','/dp/'+asin(1));f.flush();equal(badge(f),null,'Product navigation removes badge');
    countText(f,1,'999999999');f.window.history.replaceState({},'','/s?k=test&page=2&ref=abc');f.flush();equal(ids(f.document),frozen,'Same-query return from product retains frozen ranks');
    f.window.history.replaceState({},'','/s?ref=def&page=3&k=test');f.flush();equal(ids(f.document),frozen,'Page/ref/order changes do not reset');
    f.window.history.replaceState({},'','/s?k=other');f.flush();equal(ids(f.document)[0],'p1','Real new keyword performs one new sort');
    equal(sorter.install(f.window),state,'Repeated bootstrap returns existing state');state.dispose();monitor.dispose();await mutations();equal(f.pending.size,0);

    // Duplicate ASIN occurrences get separate frozen positions; replacement fallback remains safe.
    const dup=fixture(`<div class="s-main-slot">${card(1,100)}${card(2,50)}${card(1,null,'duplicate')}${card(3,10)}</div>`);const ds=sorter.install(dup.window);
    const dupOrder=ids(dup.document);equal(dupOrder,['p1','p2','p3','duplicate']);ds.runNow();equal(ids(dup.document),dupOrder,'Duplicate ASIN with unknown count never shifts after freeze');
    dup.document.querySelector('.s-main-slot').innerHTML=card(1,null,'duplicate-new')+card(3,999)+card(2,999)+card(1,0,'first-new');ds.runNow();equal(ids(dup.document),['duplicate-new','p2','p3','first-new'],'Replacement occurrences fill original ASIN rank slots');ds.dispose();

    // Realistic synthetic geometry: existing loader appends only after result tail scrolling.
    const auto=layout(fixture(`<main id="search"><div class="s-main-slot">${batch(1,20)}</div></main>`));let loaded=20;
    auto.onScroll=()=>{if(auto.window.scrollY>0&&loaded<100){auto.document.querySelector('#search').insertAdjacentHTML('beforeend',`<div class="s-result-list">${batch(loaded+1,loaded+20)}</div>`);loaded+=20;auto.rects();}};
    const as=sorter.install(auto.window);equal(as.phase,'collecting');equal(ids(auto.document)[0],'p1','Collecting never reorders cards');equal(badge(auto).style.position,'fixed','Collecting progress stays visible');
    await mutations();for(let i=0;i<3;i++){auto.advance(800);await mutations();equal(ids(auto.document)[0],'p1','Append collection keeps native sequence');}
    auto.advance(800);await mutations();equal(as.phase,'settling','Target waits briefly for review labels');countText(auto,1,'999999');
    auto.advance(800);await mutations();await settle(auto);equal(as.phase,'frozen');equal(ids(auto.document)[0],'p1','Settling window includes final count hydration');equal(ids(auto.document)[1],'p100');equal(auto.window.scrollY,0,'Normal controlled preload returns to original top');
    equal(auto.scrolls.length,5,'Four native-loader scrolls plus one top restore');equal(badge(auto).style.position,undefined,'Fixed progress removed on completion');check(badge(auto).textContent.includes('100 商品で固定'));
    const beforeAuto=auto.scrolls.length;auto.document.querySelector('#search').insertAdjacentHTML('beforeend',`<div class="s-result-list">${card(101,99999999)}</div>`);auto.rects();await mutations();await settle(auto);equal(ids(auto.document).at(-1),'p101');equal(auto.scrolls.length,beforeAuto,'Late append neither preload nor return-to-top');as.dispose();

    const noGrowth=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`));const ng=sorter.install(noGrowth.window);await settle(noGrowth);equal(ng.phase,'frozen');check(noGrowth.now()-10000<=4000,'Stalled loader finishes early');check(noGrowth.scrolls.length<=5);check(badge(noGrowth).textContent.includes('20 商品で固定'));ng.dispose();
    const budget=layout(fixture(`<div class="s-main-slot">${batch(1,2)}</div>`));let n=2;budget.onScroll=()=>{if(budget.window.scrollY>0&&n<90){budget.document.querySelector('.s-main-slot').insertAdjacentHTML('beforeend',card(++n));budget.rects();}};
    const bs=sorter.install(budget.window);for(let i=0;i<200&&bs.phase!=='frozen';i++){budget.advance(100);await mutations();}
    equal(bs.phase,'frozen','Constant growth cannot extend20s deadline');check(budget.now()-10000<=20000);check(budget.scrolls.length<=31,'Hard step budget');bs.dispose();

    // Any input cancels auto work immediately, then one idle finalization preserves reader position.
    for(const type of ['touchstart','pointerdown','wheel','keydown']){
        const interrupted=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`));const is=sorter.install(interrupted.window), before=interrupted.scrolls.length;
        const anchor=productNodes(interrupted.document).find(node=>node.getBoundingClientRect().bottom>0), top=anchor.getBoundingClientRect().top;
        interrupted.emit(type,{touches:[{}],pointerId:7});interrupted.advance(type==='touchstart'||type==='pointerdown'?5000:319);equal(interrupted.scrolls.length,before,`${type} stops all pending preload scrolls`);
        if(type==='touchstart')interrupted.emit('touchend',{touches:[]});if(type==='pointerdown')interrupted.emit('pointerup',{pointerId:7});
        interrupted.advance(320);await mutations();await settle(interrupted);equal(is.phase,'frozen');check(interrupted.window.scrollY>0,'Interrupted reader never forced to top');equal(anchor.getBoundingClientRect().top,top,'Interrupted finalization preserves visible product');is.dispose();
    }
    const hidden=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`)), hs=sorter.install(hidden.window), hiddenBefore=hidden.scrolls.length;
    Object.defineProperty(hidden.document,'hidden',{configurable:true,value:true});hidden.document.dispatchEvent(new hidden.window.Event('visibilitychange'));hidden.advance(30000);equal(hidden.scrolls.length,hiddenBefore);equal(hidden.pending.size,0,'Hidden stops outstanding work');
    Object.defineProperty(hidden.document,'hidden',{value:false,configurable:true});hidden.document.dispatchEvent(new hidden.window.Event('visibilitychange'));await settle(hidden);equal(hs.phase,'frozen');check(hidden.window.scrollY>0);hs.dispose();

    const cached=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`),550),cs=sorter.install(cached.window);equal(cs.phase,'frozen');check(cached.window.scrollY>0,'Non-top cached view stays non-top');check(cached.scrolls.length<=1,'Non-top never starts loader');cs.dispose();
    const bfc=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`));bfc.window.performance.getEntriesByType=()=>[{type:'back_forward'}];const bcs=sorter.install(bfc.window);equal(bcs.phase,'frozen');equal(bfc.scrolls.length,0,'Fresh restored document at top skips preload');bcs.dispose();
    const initialTop=layout(fixture(`<div class="s-main-slot">${batch(1,120)}</div>`)), its=sorter.install(initialTop.window);equal(its.phase,'settling');initialTop.advance(800);equal(initialTop.window.scrollY,0,'Initial top does not preserve old low-ranked card');equal(ids(initialTop.document)[0],'p120');its.dispose();

    const empty=layout(fixture('<main id="search"><div class="s-main-slot"></div></main>')),es=sorter.install(empty.window);empty.advance(20000);equal(es.phase,'awaiting');equal(empty.pending.size,0,'No initial products ends polling at deadline');
    empty.document.querySelector('.s-main-slot').innerHTML=card(1,10)+card(2,50);empty.rects();await mutations();await settle(empty);equal(es.phase,'frozen');equal(ids(empty.document),['p2','p1'],'Delayed first result batch still sorts once');equal(empty.scrolls.length,0);es.dispose();

    const home=layout(fixture(`<main id="search"><div class="s-main-slot">${batch(1,20)}</div></main>`,'https://www.amazon.co.jp/')),homeState=sorter.install(home.window);equal(home.scrolls.length,0);equal(badge(home),null,'Home DOM fallback alone does not initiate work');
    homeState.setSearchHint(true,'a1');home.flush();equal(homeState.phase,'collecting','Native search hint enables verified home-URL SPA');homeState.setSearchHint(true,'a1');const steps=home.scrolls.length;home.flush();equal(home.scrolls.length,steps,'Repeated identical hint does not duplicate step');
    home.emit('wheel');home.advance(320);await settle(home);const homeOrder=ids(home.document);countText(home,1,'999999');homeState.setSearchHint(false,null);home.flush();equal(badge(home),null);homeState.setSearchHint(true,'a1');home.flush();equal(ids(home.document),homeOrder,'Same opaque token restores frozen home-URL search');
    homeState.setSearchHint(true,'b2');home.flush();equal(ids(home.document)[0],'p1','New opaque query token resets ranking');homeState.dispose();
    for(const route of ['/dp/'+asin(1),'/hz/cart','/gp/aw/c','/your-orders/orders']){const safe=layout(fixture(`<main id="search"><div class="s-main-slot">${batch(1,20)}</div></main>`,'https://www.amazon.co.jp'+route));const ss=sorter.install(safe.window);ss.setSearchHint(true,'x');safe.flush();equal(safe.scrolls.length,0,route+' cannot auto scroll');equal(badge(safe),null);ss.dispose();}

    const alias=fixture(`<main id="search"><div class="s-main-slot">${card(1,10)}${card(2,100)}</div></main>`), als=sorter.install(alias.window);
    als.setSearchHint(true,'a11');alias.flush();countText(alias,1,'99999');alias.window.location=new URL('https://www.amazon.co.jp/');als.setSearchHint(true,'a11');als.runNow();equal(ids(alias.document),['p2','p1'],'Same native token bridges actual URL to home-URL SPA without resetting freeze');
    als.setSearchHint(true,'b22');alias.flush();equal(ids(alias.document),['p1','p2'],'Genuinely different native token resets home SPA');
    countText(alias,2,'999999');alias.window.location=new URL('https://www.amazon.co.jp/s?k=second');als.setSearchHint(true,'b22');alias.flush();equal(ids(alias.document),['p1','p2'],'Current search URL adopts existing verified native identity without another sort');als.dispose();
    const collision=fixture(`<div class="s-main-slot">${card(1,10)}${card(2,20)}</div>`,'https://www.amazon.co.jp/s?k=foo%26rh%3Dx'), cos=sorter.install(collision.window);
    countText(collision,1,'9999');collision.window.history.replaceState({},'','/s?k=foo&rh=x');collision.flush();equal(ids(collision.document),['p1','p2'],'Escaped delimiters cannot collide with a real filter identity');cos.dispose();

    // No cross-heading or cross-presentation moves, even after late prepends.
    const partitions=fixture(`<main id="search"><div class="s-result-list"><h2>Section A</h2>${card(1,10)}${card(2,20)}</div><div class="s-result-list"><h2>Section B</h2>${card(3,1000)}${card(4,2000)}</div></main>`);const ps=sorter.install(partitions.window);equal(ids(partitions.document),['p2','p1','p4','p3']);check(badge(partitions).textContent.includes('表示形式ごと'));ps.dispose();
    const scrollers=fixture(`<main id="search"><section id="a"><div class="s-result-list">${card(1,10)}${card(2,20)}</div></section><section id="b"><div class="s-result-list">${card(3,1000)}${card(4,2000)}</div></section></main>`);
    for(const id of ['a','b']){const el=scrollers.document.getElementById(id);Object.defineProperty(el,'scrollHeight',{value:1000});Object.defineProperty(el,'clientHeight',{value:200});el.scrollTop=0;}
    scrollers.window.getComputedStyle=el=>({overflowY:el.id==='a'||el.id==='b'?'auto':'visible'});const scs=sorter.install(scrollers.window);
    equal(ids(scrollers.document),['p2','p1','p4','p3'],'Separate scroll containers never exchange products');scs.dispose();
    const ads=fixture(`<div class="s-main-slot">${card(1,10)}${card(2,100)}${card(1,999999,'ad').replace('class="s-result-item"','class="s-result-item AdHolder"')}</div>`),adsState=sorter.install(ads.window);
    equal(ids(ads.document),['p2','p1']);equal(ads.document.querySelector('.s-main-slot').lastElementChild.id,'ad','Duplicate-ASIN ad is excluded and retains its slot');check(badge(ads).textContent.includes('2 商品で固定'));adsState.dispose();
    const lateLabels=fixture(`<div class="s-main-slot">${card(1,null)}${card(2,null)}</div>`),lls=sorter.install(lateLabels.window);check(badge(lateLabels).textContent.includes('取得できません'));
    lateLabels.document.querySelector('#p1 [data-cy]').innerHTML='<a href="#customerReviews"><span class="s-underline-text">1</span></a>';lateLabels.document.querySelector('#p2 [data-cy]').innerHTML='<a href="#customerReviews"><span class="s-underline-text">999999</span></a>';
    await mutations();await settle(lateLabels);equal(ids(lateLabels.document),['p1','p2'],'Late label hydration cannot silently rerank frozen cohort');lls.dispose();

    const fail=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`));fail.window.scrollTo=()=>{throw new Error('Destroyed scroll surface');};const fs=sorter.install(fail.window);equal(fs.phase,'frozen');equal(fail.pending.size,0,'Scroll error cannot leave a retry loop');fs.dispose();
    const smooth=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`));smooth.document.documentElement.style.setProperty('scroll-behavior','smooth');const originalScroll=smooth.window.scrollTo;let sawAuto=false;
    smooth.window.scrollTo=(x,y)=>{sawAuto=smooth.document.documentElement.style.getPropertyValue('scroll-behavior')==='auto';originalScroll(x,y);};const sms=sorter.install(smooth.window);check(sawAuto,'Preload overrides CSS smooth scrolling during write');equal(smooth.document.documentElement.style.getPropertyValue('scroll-behavior'),'smooth','Page scroll CSS restored');sms.dispose();await mutations();equal(smooth.pending.size,0,'Dispose invalidates all pending work');
    console.log('PASS '+JSON.stringify({status:'passed',assertions,dom:'linkedom 0.18.12',synthetic_geometry_tested:true,real_browser_layout_tested:false,physical_device_tested:false}));
})().catch(error=>{console.error(error);process.exitCode=1;});
