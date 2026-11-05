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
const concealed=n=>!!n.closest('[data-hiro-review-deferred="1.0.6"], [hidden]') || n.style.display==='none';
const visibleNodes=doc=>productNodes(doc).filter(n=>!concealed(n));
const ids=doc=>visibleNodes(doc).map(n=>n.id);
const allIds=doc=>productNodes(doc).map(n=>n.id);
const descending=doc=>visibleNodes(doc).every((n,i,all)=>i===0||sorter.readCount(all[i-1])>=sorter.readCount(n));
const badge=f=>f.document.getElementById('hiro-review-count-status');
function layout(f,y=0) {
    const win=f.window; win.innerHeight=400; win.scrollY=y;win.scrollX=0;f.scrolls=[];
    Object.defineProperty(f.document.documentElement,'clientHeight',{configurable:true,get:()=>400});
    Object.defineProperty(f.document.documentElement,'scrollHeight',{configurable:true,get:()=>productNodes(f.document).length*100+700});
    f.rects=()=>productNodes(f.document).forEach(node=>{node.getBoundingClientRect=()=>{
        const index=visibleNodes(f.document).indexOf(node), label=badge(f),offset=label&&label.style.position!=='fixed'?24:0;
        if(index<0)return {top:0,bottom:0,height:0};
        return {top:index*100+offset-win.scrollY,bottom:(index+1)*100+offset-win.scrollY,height:100,width:300,left:0,right:300};
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
    const control=f=>f.document.getElementById('hiro-review-count-update');
    async function activate(f){const state=sorter.install(f.window);await settle(f);return state;}
    async function importNow(f){const button=control(f).querySelector('button');f.emit('pointerdown',{target:button,pointerId:1});f.emit('pointerup',{target:button,pointerId:1});button.dispatchEvent(new f.window.Event('click'));await settle(f);}

    // Current visible numeric values—not immutable ranks alone—must be descending.
    const f=fixture(`<main id="search"><div class="s-main-slot">${card(1,10)}${card(2,100)}${card(3,null)}</div></main>`),original=f.document.getElementById('p2');let clicks=0;original.addEventListener('click',()=>clicks++);
    const state=sorter.install(f.window);equal(state.phase,'ready','Every finalization route waits for count readiness');await settle(f);
    equal(state.phase,'frozen');equal(ids(f.document),['p2','p1'],'Only confirmed counts enter fixed list');check(concealed(f.document.getElementById('p3')));check(descending(f.document));
    original.dispatchEvent(new f.window.Event('click'));equal(clicks,1,'Original click handler survives sorting');check(badge(f).textContent.includes('確認済み 2 商品で固定'));check(control(f).textContent.includes('件数未確定 1 件'));
    await mutations();await settle(f);const monitor=track(f);f.document.querySelector('.s-main-slot').insertAdjacentHTML('beforeend',card(4,999999));await mutations();monitor.reset();await settle(f);
    equal(ids(f.document),['p2','p1'],'Late higher-count card is deferred, not appended visibly out of order');equal(monitor.insertions,0);check(concealed(f.document.getElementById('p4')));check(descending(f.document));
    f.document.querySelector('#p3 [data-cy]').innerHTML='<a href="#customerReviews"><span class="s-underline-text">99999</span></a>';await mutations();await settle(f);equal(ids(f.document),['p2','p1'],'Late hydration remains deferred until explicit update');
    await importNow(f);equal(ids(f.document),['p4','p3','p2','p1'],'Explicit button imports newly confirmed cards once');check(descending(f.document));equal(state.phase,'frozen');check(control(f).querySelector('button').disabled);
    countText(f,1,'9999999');await mutations();await settle(f);equal(ids(f.document),['p4','p3','p2'],'Changed frozen count cannot violate visible descending order');check(control(f).textContent.includes('件数未確定 1 件'));
    await importNow(f);equal(ids(f.document)[0],'p1');check(descending(f.document));
    monitor.reset();for(let i=0;i<5;i++){f.document.getElementById('p2').setAttribute('style',`opacity:${1-i/100}`);await mutations();await settle(f);}
    equal(monitor.insertions,0,'Cosmetic mutations cause no product moves');equal(monitor.badgeWrites,0,'Status writes remain idempotent');
    const ranks=ids(f.document);f.document.querySelector('.s-main-slot').innerHTML=card(2,100)+card(3,99999)+card(4,999999)+card(1,9999999);await mutations();await settle(f);equal(ids(f.document),ranks,'Same-ASIN native replacements reuse verified fixed ranks');
    f.document.querySelector('.s-main-slot').insertAdjacentHTML('afterbegin',card(5,99999999));await mutations();await settle(f);equal(ids(f.document),ranks,'Native prepend is deferred');
    const newCard=f.document.getElementById('p5');f.window.history.pushState({},'','/dp/'+asin(1));f.flush();equal(newCard.hasAttribute('data-hiro-review-deferred'),false,'Leaving search restores only own hiding');equal(badge(f),null);equal(control(f),null);
    f.window.history.replaceState({},'','/s?k=test&page=2&ref=abc');f.flush();check(concealed(newCard),'Same query return re-applies snapshot membership');equal(ids(f.document),ranks);
    f.window.history.replaceState({},'','/s?ref=new&k=test&page=3');f.flush();equal(ids(f.document),ranks,'Paging/tracking do not restart rank');
    f.window.history.replaceState({},'','/s?k=new');await settle(f);equal(ids(f.document)[0],'p5','New semantic query begins a new cohort');
    state.dispose();monitor.dispose();await mutations();equal(f.pending.size,0);check(!f.document.querySelector('[data-hiro-review-deferred]'));equal(f.document.getElementById('hiro-review-deferred-style'),null);

    const cosmetic=fixture(`<main id="search"><div class="s-main-slot"><div class="a-section">${card(1,100)}<aside hidden>old ad</aside></div>${card(2,90)}</div><div class="s-result-list">${card(3,10000).replace('s-result-item','s-result-item puis-card-container')}<div class="a-section">${card(4,9000)}</div></div></main>`),cos=await activate(cosmetic);
    equal(ids(cosmetic.document),['p3','p4','p1','p2'],'Cosmetic class/wrapper/hidden-inert differences do not partition ordinary batches');check(descending(cosmetic.document));cos.dispose();
    const sponsors=fixture(`<main id="search"><div class="s-main-slot">${card(1,10)}${card(9,999999,'ad').replace('class="s-result-item"','class="s-result-item AdHolder"')}${card(2,100)}</div></main>`),ads=await activate(sponsors);
    equal(ids(sponsors.document),['p2','p1']);check(concealed(sponsors.document.getElementById('ad')),'Visible sponsored slot cannot interrupt ranked list');check(control(sponsors).textContent.includes('対象外の表示・広告 1 件'));ads.dispose();check(!concealed(sponsors.document.getElementById('ad')),'Dispose restores sponsored original node');
    const sections=fixture(`<main id="search"><div class="s-main-slot">${card(1,10)}${card(2,20)}</div><div class="s-result-list"><h2>おすすめ</h2>${batch(3,9)}</div></main>`),secs=await activate(sections);
    equal(ids(sections.document),['p2','p1'],'Larger headed recommendation section cannot become main cohort');check(control(sections).textContent.includes('対象外の表示・広告 7 件'));secs.dispose();
    const outside=fixture(`<main id="search"><div class="s-main-slot">${card(1,10)}${card(2,20)}</div></main><aside class="a-carousel">${card(99,99999)}</aside>`),out=await activate(outside);check(!concealed(outside.document.getElementById('p99')),'Outside recommendation carousel untouched');out.dispose();
    const scrollers=fixture(`<main id="search"><section id="a"><div class="s-main-slot">${card(1,10)}${card(2,20)}</div></section><section id="b"><div class="s-result-list">${card(3,1000)}${card(4,2000)}</div></section></main>`);
    for(const id of ['a','b']){const el=scrollers.document.getElementById(id);Object.defineProperty(el,'scrollHeight',{value:1000});Object.defineProperty(el,'clientHeight',{value:200});el.scrollTop=0;}
    scrollers.window.getComputedStyle=el=>({overflowY:el.id==='a'||el.id==='b'?'auto':'visible'});const scs=await activate(scrollers);equal(ids(scrollers.document),['p2','p1'],'Independent scroll surface is deferred instead of silently mixing rankings');scs.dispose();
    const ordered=fixture(`<div class="s-main-slot">${card(1,10)}${card(2,20)}${card(3,99999)}</div>`);ordered.window.getComputedStyle=node=>({order:node.id==='p3'?'10':'0',position:'static'});const os=await activate(ordered);equal(ids(ordered.document),['p2','p1'],'CSS-order product cannot be falsely verified in DOM order');os.dispose();

    const unknown=fixture(`<div class="s-main-slot">${card(1,null)}${card(2,null)}</div>`),us=await activate(unknown);equal(ids(unknown.document),['p1','p2'],'Zero confirmed counts fail open');check(badge(unknown).textContent.includes('そのまま表示'));check(!badge(unknown).textContent.includes('で固定'));check(!control(unknown).querySelector('button').disabled);
    unknown.document.querySelector('#p1 [data-cy]').innerHTML='<a href="#customerReviews"><span class="s-underline-text">2</span></a>';unknown.document.querySelector('#p2 [data-cy]').innerHTML='<a href="#customerReviews"><span class="s-underline-text">99999</span></a>';await importNow(unknown);equal(ids(unknown.document),['p2','p1']);us.dispose();
    // A detached deferred node must never carry orphaned hiding into an app cache.
    const detached=fixture(`<div class="s-main-slot">${card(1,100)}${card(2,null)}</div>`),dts=await activate(detached),cachedNode=detached.document.getElementById('p2');check(concealed(cachedNode));cachedNode.remove();await mutations();await settle(detached);equal(cachedNode.hasAttribute('data-hiro-review-deferred'),false,'Pruning detached node restores own marker');
    detached.window.history.pushState({},'','/');detached.flush();detached.document.body.appendChild(cachedNode);check(!concealed(cachedNode),'Reattached cached node not permanently hidden');dts.dispose();
    const native=fixture(`<div class="s-main-slot">${card(1,100)}${card(2,null)}</div>`),ns=await activate(native),nativeNode=native.document.getElementById('p2');nativeNode.setAttribute('hidden','native');nativeNode.style.display='none';ns.dispose();equal(nativeNode.getAttribute('hidden'),'native');equal(nativeNode.style.display,'none','Native hiding changed during deferral is preserved');equal(nativeNode.hasAttribute('data-hiro-review-deferred'),false);

    const auto=layout(fixture(`<main id="search"><div class="s-main-slot">${batch(1,20)}</div></main>`));let loaded=20;
    auto.onScroll=()=>{if(auto.window.scrollY>0&&loaded<100){auto.document.querySelector('#search').insertAdjacentHTML('beforeend',`<div class="s-result-list">${batch(loaded+1,loaded+20)}</div>`);loaded+=20;auto.rects();}};
    const as=sorter.install(auto.window);equal(as.phase,'collecting');equal(ids(auto.document)[0],'p1','No sorting during native loader traversal');check(badge(auto).style.position==='fixed');await settle(auto);
    equal(as.phase,'frozen');equal(ids(auto.document)[0],'p100');equal(ids(auto.document).length,100);check(descending(auto.document));equal(auto.window.scrollY,0,'Controlled initial preload returns to top');equal(auto.scrolls.length,5,'Four tail-load scrolls and one return');
    auto.document.querySelector('#search').insertAdjacentHTML('beforeend',`<div class="s-result-list">${card(101,999999)}</div>`);auto.rects();const beforeScrolls=auto.scrolls.length;await mutations();await settle(auto);equal(ids(auto.document)[0],'p100');check(concealed(auto.document.getElementById('p101')));equal(auto.scrolls.length,beforeScrolls,'Frozen late arrivals do not restart automatic scrolling');
    await importNow(auto);equal(ids(auto.document)[0],'p101');equal(auto.scrolls.length,beforeScrolls,'Top-view manual update does not auto-scroll');check(descending(auto.document));as.dispose();
    const hydration=layout(fixture(`<div class="s-main-slot">${batch(1,99)}${card(100,null)}</div>`)),hys=sorter.install(hydration.window);equal(hys.phase,'ready');hydration.advance(900);equal(hys.phase,'ready','Missing counts do not freeze at old800ms deadline');
    hydration.document.querySelector('#p100 [data-cy]').innerHTML='<a href="#customerReviews"><span class="s-underline-text">999999</span></a>';await mutations();await settle(hydration);equal(ids(hydration.document)[0],'p100');check(descending(hydration.document));hys.dispose();
    const stall=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`)),ss=sorter.install(stall.window);await settle(stall);equal(ss.phase,'frozen');check(stall.now()-10000<=8000,'No-growth plus readiness bound');ss.dispose();
    const empty=layout(fixture('<main id="search"><div class="s-main-slot"></div></main>')),es=sorter.install(empty.window);await settle(empty);equal(es.phase,'awaiting');check(empty.now()-10000<=24000,'Empty initial list remains bounded');equal(empty.pending.size,0);empty.document.querySelector('.s-main-slot').innerHTML=card(1,10)+card(2,50);empty.rects();await mutations();await settle(empty);equal(ids(empty.document),['p2','p1']);equal(empty.scrolls.length,0);es.dispose();

    for(const type of ['touchstart','pointerdown','wheel','keydown']){
        const f=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`)),state=sorter.install(f.window),before=f.scrolls.length;
        const anchor=visibleNodes(f.document).find(n=>n.getBoundingClientRect().bottom>0),top=anchor.getBoundingClientRect().top;
        f.emit(type,{touches:[{}],pointerId:7});f.advance(type==='touchstart'||type==='pointerdown'?5000:319);equal(f.scrolls.length,before,type+' immediately stops preload');
        if(type==='touchstart')f.emit('touchend',{touches:[]});if(type==='pointerdown')f.emit('pointerup',{pointerId:7});await settle(f);equal(state.phase,'frozen');check(f.window.scrollY>0);equal(anchor.getBoundingClientRect().top,top,'Interrupted finalization preserves visible product pixel');state.dispose();
    }
    const cached=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`),550),cs=await activate(cached);check(cached.window.scrollY>0);check(cached.scrolls.length<=2,'Cached non-top permits only status/sort anchor compensation');
    cached.document.querySelector('.s-main-slot').insertAdjacentHTML('beforeend',card(21,99999));cached.rects();await mutations();await settle(cached);const anchor=visibleNodes(cached.document).find(n=>n.getBoundingClientRect().bottom>0),top=anchor.getBoundingClientRect().top;await importNow(cached);equal(anchor.getBoundingClientRect().top,top,'Manual import captures anchor before deferred nodes become measurable');check(cached.window.scrollY>0);cs.dispose();
    const bfc=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`));bfc.window.performance.getEntriesByType=()=>[{type:'back_forward'}];const bfs=await activate(bfc);equal(bfc.scrolls.length,0);bfs.dispose();
    const hidden=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`)),hs=sorter.install(hidden.window),hiddenCalls=hidden.scrolls.length;Object.defineProperty(hidden.document,'hidden',{value:true,configurable:true});hidden.document.dispatchEvent(new hidden.window.Event('visibilitychange'));hidden.advance(30000);equal(hidden.scrolls.length,hiddenCalls);equal(hidden.pending.size,0);Object.defineProperty(hidden.document,'hidden',{value:false,configurable:true});hidden.document.dispatchEvent(new hidden.window.Event('visibilitychange'));await settle(hidden);equal(hs.phase,'frozen');hs.dispose();

    const home=fixture(`<main id="search"><div class="s-main-slot">${card(1,10)}${card(2,100)}</div></main>`,'https://www.amazon.co.jp/'),homeState=sorter.install(home.window);check(!!badge(home),'Strong visible home search starts without a hint');homeState.setSearchHint(true,'a1');await settle(home);equal(ids(home.document),['p2','p1']);homeState.setSearchHint(false,null);home.flush();check(!!badge(home),'Strong visible search remains active without hint');homeState.setSearchHint(true,'a1');home.flush();equal(ids(home.document),['p2','p1']);homeState.dispose();
    for(const route of ['/dp/'+asin(1),'/hz/cart','/gp/aw/c','/your-orders/orders']){const f=layout(fixture(`<main id="search"><div class="s-main-slot">${batch(1,20)}</div></main>`,'https://www.amazon.co.jp'+route)),state=sorter.install(f.window);state.setSearchHint(true,'x');f.flush();equal(f.scrolls.length,0);equal(badge(f),null);check(!f.document.querySelector('[data-hiro-review-deferred]'));state.dispose();}
    const fault=fixture(`<div class="s-main-slot">${card(1,10)}${card(2,null)}</div>`),faultState=await activate(fault);const old=fault.document.querySelectorAll.bind(fault.document);fault.document.querySelectorAll=selector=>{if(selector.includes('.s-main-slot'))throw new Error('native tree change');return old(selector);};faultState.runNow();equal(fault.document.getElementById('p2').hasAttribute('data-hiro-review-deferred'),false,'Unexpected failure restores own hiding');check(badge(fault).textContent.includes('そのまま表示'));faultState.dispose();

    const held=layout(fixture(`<div class="s-main-slot">${card(1,10)}${card(2,20)}${card(3,100)}</div>`));held.window.performance.getEntriesByType=()=>[{type:'back_forward'}];const heldState=await activate(held),heldScrolls=held.scrolls.length;
    held.emit('touchstart',{touches:[{}]});held.document.querySelector('.s-main-slot').insertAdjacentHTML('beforeend',card(4,999999));held.rects();countText(held,1,'9999');countText(held,2,'8888');await mutations();held.advance(80);
    equal(ids(held.document),['p3'],'New arrivals and both changed-count mutations defer while touch is held');equal(held.scrolls.length,heldScrolls,'Held-input guard never sorts or scrolls');check(descending(held.document));
    held.emit('touchend',{touches:[]});await settle(held);held.emit('pointerdown',{pointerId:44,target:held.document.getElementById('p3')});held.emit('pointerup',{pointerId:44,target:control(held).querySelector('button')});
    await importNow(held);equal(ids(held.document),['p4','p1','p2','p3'],'Pointer ending over own control cannot leave busy stuck');heldState.dispose();

    const anchored=layout(fixture(`<div class="s-main-slot">${batch(1,20)}</div>`),550),ans=await activate(anchored);const kept=visibleNodes(anchored.document).find(n=>n.getBoundingClientRect().bottom>0),keptTop=kept.getBoundingClientRect().top;
    countText(anchored,20,'99999');await mutations();await settle(anchored);equal(kept.getBoundingClientRect().top,keptTop,'Hide-only change above the viewport preserves the saved product pixel');check(concealed(anchored.document.getElementById('p20')));ans.dispose();

    const flashing=fixture(`<div class="s-main-slot">${card(1,10)}${card(2,null)}</div>`),fls=await activate(flashing);flashing.document.querySelector('.s-main-slot').insertAdjacentHTML('beforeend',card(3,99999));await mutations();await settle(flashing);
    control(flashing).querySelector('button').dispatchEvent(new flashing.window.Event('click'));flashing.flush();equal(fls.phase,'ready');equal(ids(flashing.document),['p1'],'Manual readiness re-hides deferred slots before each paint');await settle(flashing);equal(ids(flashing.document),['p3','p1']);check(concealed(flashing.document.getElementById('p2')));fls.dispose();
    const vanished=fixture(`<div class="s-main-slot">${card(1,10)}${card(2,100)}</div>`),vns=sorter.install(vanished.window);vanished.document.querySelector('#p2 .s-underline-text').remove();await mutations();await settle(vanished);equal(ids(vanished.document),['p1'],'Finalization never reuses a stale cached count after current label disappears');vns.dispose();

    const aria=fixture(`<div class="s-main-slot">${card(1,10)}${card(2,99999).replace('id="p2"','id="p2" aria-hidden="true"')}${card(3,100)}</div>`),ars=await activate(aria);equal(ids(aria.document),['p2','p3','p1'],'Visually displayed aria-hidden card participates in descending verification');ars.dispose();
    const nested=fixture(`<div class="s-main-slot">${card(1,null).replace('<h2>商品1</h2>',card(1,99999,'inner'))}${card(2,100)}</div>`);equal(sorter.readCount(nested.document.getElementById('p1')),99999,'Sole visible same-ASIN inner result can supply missing outer count');
    nested.document.querySelector('#p1 > [data-cy]').innerHTML='<a href="#customerReviews"><span class="s-underline-text">1</span></a>';equal(sorter.readCount(nested.document.getElementById('p1')),1,'Own confirmed count always beats nested fallback');

    const tooManyRoots=fixture(`<main id="search">${Array.from({length:33},(_,i)=>`<div class="s-result-list">${card(i+1,i+1)}</div>`).join('')}</main>`),trs=sorter.install(tooManyRoots.window);check(badge(tooManyRoots).textContent.includes('商品が多いため並び順を確認できません'));equal(tooManyRoots.pending.size,0);equal(ids(tooManyRoots.document).length,33,'Overflow restores all original visible cards');trs.dispose();
    const tooManyCards=fixture(`<div class="s-main-slot">${batch(1,1001)}</div>`),tcs=sorter.install(tooManyCards.window);check(badge(tooManyCards).textContent.includes('商品が多いため並び順を確認できません'));check(!badge(tooManyCards).textContent.includes('で固定'));equal(tooManyCards.pending.size,0);equal(ids(tooManyCards.document).length,1001);tcs.dispose();

    for(const code of [10,13,9]) equal(sorter.parseCount('4'+String.fromCharCode(code)+'123',true),-1,'Separate DOM text segments are not numeric thousands groups');
    const segmented=fixture(`<div class="s-result-item" data-component-type="s-search-result" data-asin="${asin(1)}"><div data-cy="reviews-block"><a class="s-underline-text" href="#customerReviews"><svg aria-label="4 out of 5 stars"></svg><span class="a-size-base">4</span><span class="a-size-base">123</span></a></div></div>`);
    equal(sorter.readCount(segmented.document.querySelector('.s-result-item')),123,'SVG-star aggregate cannot merge integer score4 and count123 into4123');

    // v105: exact review-count values seen in the user video, through real browser-IIFE entry.
    const vm=require('node:vm'),fs=require('node:fs'),source=fs.readFileSync(require.resolve('../runtime/review-sort.js'),'utf8');
    for(const moduleValue of [null,Object.freeze({exports:{}}),Object.defineProperty({},'exports',{get(){throw new Error('site module getter');}})]){
        const f=fixture(`<main id="search"><div class="s-main-slot">${card(1,1313)}${card(2,34)}${card(3,'1.2万')}</div></main>`);
        vm.runInNewContext(source,{window:f.window,module:moduleValue,URLSearchParams});const state=f.window.__hiroReviewCountSort106;
        check(!!state,'Site CommonJS shim cannot stop browser installation');await settle(f);equal(ids(f.document),['p3','p1','p2'],'Video1313/34/12000 becomes12000/1313/34');state.dispose();
    }
    const moduleGetter=fixture(`<div class="s-main-slot">${card(1,1)}${card(2,100)}</div>`),context={window:moduleGetter.window,URLSearchParams};Object.defineProperty(context,'module',{get(){throw new Error('global module getter');}});
    vm.runInNewContext(source,context);check(!!moduleGetter.window.__hiroReviewCountSort106);await settle(moduleGetter);moduleGetter.window.__hiroReviewCountSort106.dispose();

    for(const route of ['/gp/homepage.html','/gp/aw/h.html','/gp/aw/homepage.html','/gp/gw/ajax/mshop.html','/-/en/gp/aw/homepage.html']){
        const f=fixture(`<main id="search"><div class="s-result-list" hidden>${card(99,999999)}</div><div class="s-main-slot">${card(1,1313)}${card(2,34)}${card(3,'1.2万')}</div></main>`,'https://www.amazon.co.jp'+route),state=sorter.install(f.window);
        check(sorter.isSearch(f.window),'Every visible root is inspected on '+route);check(!!badge(f));await settle(f);equal(ids(f.document),['p3','p1','p2']);state.dispose();
    }
    const noProof=fixture('<main id="search"><div class="s-main-slot"></div></main>','https://www.amazon.co.jp/gp/help/customer/display.html'),np=sorter.install(noProof.window);np.setSearchHint(true,'aa');noProof.flush();check(!!badge(noProof),'Verified native search hint exposes waiting status even before roots exist');check(!sorter.isSearch(noProof.window,true),'Hint without canonical DOM never ranks content');np.dispose();
    const visibleDetail=fixture(`<div id="productTitle">Visible product</div><main id="search"><div class="s-main-slot">${card(1,10)}${card(2,100)}</div></main>`),vds=sorter.install(visibleDetail.window);equal(badge(visibleDetail),null,'Visible product-detail document rejects stale search structure');vds.dispose();
    const cachedDetail=fixture(`<div id="productTitle" hidden>Cached product</div><main id="search"><div class="s-main-slot">${card(1,10)}${card(2,100)}</div></main>`,'https://www.amazon.co.jp/gp/homepage.html'),cds=await activate(cachedDetail);equal(ids(cachedDetail.document),['p2','p1']);cds.dispose();

    const genericCard=(n,c)=>card(n,c).replace('class="s-result-item"','class="puis-card-container"').replace(' data-component-type="s-search-result"','');
    const generic=fixture(`<main id="search"><div class="s-main-slot"><div data-component-type="s-search-result">${genericCard(1,1313)}</div><div class="s-result-item" data-asin="invalid">${genericCard(2,34)}</div>${genericCard(3,'1.2万')}</div></main>`),gs=await activate(generic);
    equal(Array.from(generic.document.querySelectorAll('.puis-card-container')).map(n=>n.id),['p3','p1','p2'],'Invalid outer shells do not discard valid generic inner products');check(badge(generic).textContent.includes('確認済み 3'));gs.dispose();
    const unmatched=fixture(`<div class="s-main-slot">${card(1,10)}<div class="s-result-item" data-component-type="s-search-result" id="unmatched"><h2>Unknown identity product</h2><div data-cy="reviews-block"><a href="#customerReviews">99999</a></div></div>${card(2,500)}</div>`),ums=sorter.install(unmatched.window);
    check(badge(unmatched).textContent.includes('一部の商品を認識できない'));check(!badge(unmatched).textContent.includes('で固定'));equal(unmatched.pending.size,0,'Unknown product is an explicit failure, never false global success');ums.dispose();
    const bare=fixture(`<div class="s-result-item" data-asin="${asin(1)}"><div data-cy="reviews-block"><i class="a-icon-star"><span class="a-icon-alt">4.5 out of5 stars</span></i><span class="a-size-small">4</span><span class="a-size-small">1,234</span></div></div>`);
    equal(sorter.readCount(bare.document.querySelector('.s-result-item')),1234,'Plain mobile count span is accepted with review-block/star evidence');bare.document.querySelector('i').remove();equal(sorter.readCount(bare.document.querySelector('.s-result-item')),-1,'Bare unlabelled numbers need supporting review evidence');

    const busyStart=fixture('<main id="search"><div class="s-main-slot"></div></main>','https://www.amazon.co.jp/gp/aw/h.html'),bst=sorter.install(busyStart.window);busyStart.emit('touchstart',{touches:[{}]});busyStart.document.querySelector('.s-main-slot').innerHTML=card(1,1313)+card(2,34)+card(3,'1.2万');await mutations();busyStart.advance(80);
    check(!!badge(busyStart));equal(badge(busyStart).style.position,'fixed');equal(badge(busyStart).parentNode,busyStart.document.body,'Initial busy banner avoids transformed search ancestors');check(!badge(busyStart).textContent.includes('で固定'));
    for(let i=0;i<80;i++){busyStart.emit('touchmove',{touches:[{}]});busyStart.advance(200);}equal(ids(busyStart.document),['p1','p2','p3']);check(badge(busyStart).textContent.includes('操作が止まると確認'));
    busyStart.emit('touchend',{touches:[]});await settle(busyStart);equal(ids(busyStart.document),['p3','p1','p2']);bst.dispose();
    const newBusy=fixture(`<div class="s-main-slot">${card(1,10)}${card(2,100)}</div>`),nbs=await activate(newBusy);newBusy.emit('touchstart',{touches:[{}]});newBusy.window.history.pushState({},'','/s?k=another');newBusy.flush();check(badge(newBusy).textContent.includes('操作が止まると確認'));equal(badge(newBusy).style.position,'fixed');check(!badge(newBusy).textContent.includes('確認済み'));equal(control(newBusy),null,'New query removes stale previous update controls');newBusy.emit('touchend',{touches:[]});await settle(newBusy);nbs.dispose();

    const recover=fixture(`<div class="s-main-slot">${card(1,1313)}${card(2,34)}${card(3,12000)}</div>`),rcs=await activate(recover);
    for(const n of recover.document.querySelectorAll('.s-underline-text'))n.textContent='';await mutations();await settle(recover);check(badge(recover).textContent.includes('そのまま表示'));equal(badge(recover).style.position,'fixed','Read failure remains observable');
    countText(recover,1,'131');countText(recover,2,'483');countText(recover,3,'2.4万');await mutations();await settle(recover);equal(ids(recover.document),['p3','p2','p1'],'Temporary total count loss recovers automatically to24000/483/131');check(descending(recover.document));rcs.dispose();
    const unchangedFailure=fixture(`<div class="s-main-slot">${card(1,null)}${card(2,null)}</div>`),ufs=await activate(unchangedFailure);
    for(let i=0;i<4;i++){unchangedFailure.document.querySelector('.s-main-slot').innerHTML=card(1,null)+card(2,null);await mutations();unchangedFailure.flush();equal(ufs.phase,'frozen','Same ASIN/count rerender does not restart failed readiness');equal(unchangedFailure.pending.size,0);}
    ufs.dispose();

    const homeAuto=layout(fixture(`<main id="search"><div class="s-main-slot">${batch(1,20)}</div></main>`,'https://www.amazon.co.jp/gp/aw/homepage.html'));let homeLoaded=20;
    homeAuto.onScroll=()=>{if(homeAuto.window.scrollY>0&&homeLoaded<100){homeAuto.document.querySelector('#search').insertAdjacentHTML('beforeend',`<div class="s-result-list">${batch(homeLoaded+1,homeLoaded+20)}</div>`);homeLoaded+=20;homeAuto.rects();}};
    const has=sorter.install(homeAuto.window);equal(has.phase,'collecting','Hintless strong home starts native preload');has.setSearchHint(true,'firstnative');await settle(homeAuto);equal(ids(homeAuto.document).length,100);equal(ids(homeAuto.document)[0],'p100');equal(homeAuto.scrolls.length,5,'Late first token adopts initial collection without restarting');has.dispose();

    async function homeCohort(){const f=fixture(`<main id="search"><div class="s-main-slot">${card(1,10)}${card(2,100)}</div></main>`,'https://www.amazon.co.jp/gp/homepage.html');return{f,state:await activate(f)};}
    function replaceRoot(f,html){f.document.querySelector('.s-main-slot').outerHTML='<div class="s-main-slot">'+html+'</div>';}
    const fresh=await homeCohort();fresh.state.setSearchHint(true,'oldnative');fresh.f.flush();fresh.state.setSearchHint(false,null);fresh.f.flush();replaceRoot(fresh.f,card(10,10)+card(11,1000));await mutations();await settle(fresh.f);equal(ids(fresh.f.document),['p11','p10'],'New disjoint top root starts new hintless cohort despite stale revoked token');fresh.state.setSearchHint(true,'newnative');fresh.f.flush();equal(fresh.state.phase,'frozen','Later authoritative token adopts inferred cohort');fresh.state.dispose();
    const same=await homeCohort();replaceRoot(same.f,card(1,10)+card(2,100));await mutations();same.f.flush();equal(same.state.phase,'frozen');equal(ids(same.f.document),['p2','p1'],'Same-ASIN root replacement preserves frozen order');same.state.dispose();
    const overlap=await homeCohort();replaceRoot(overlap.f,card(2,100)+card(3,99999));await mutations();overlap.f.flush();equal(overlap.state.phase,'frozen');equal(ids(overlap.f.document),['p2'],'Partial ASIN overlap does not infer a new search');overlap.state.dispose();
    const appended=await homeCohort();appended.f.document.querySelector('#search').insertAdjacentHTML('beforeend',`<div class="s-result-list">${card(3,99999)}</div>`);await mutations();appended.f.flush();equal(ids(appended.f.document),['p2','p1'],'New appended root is still a late batch');appended.state.dispose();
    const away=await homeCohort();away.f.window.scrollY=500;replaceRoot(away.f,card(3,10)+card(4,100));await mutations();away.f.flush();equal(away.state.phase,'frozen');equal(away.f.pending.size,0,'Non-top replacement does not infer a fresh search or preload');away.state.dispose();

    const visual=fixture(`<div class="s-main-slot">${card(1,1313)}${card(2,34)}${card(3,12000)}</div>`);Array.from(visual.document.querySelectorAll('[data-asin]')).forEach((n,i)=>{n.getBoundingClientRect=()=>({top:i*100,left:0,width:300,height:100,bottom:(i+1)*100,right:300});});const vis=await activate(visual);
    check(badge(visual).textContent.includes('表示位置と件数順が一致しない'));check(!badge(visual).textContent.includes('で固定'),'Measured visual inversion cannot claim sorted success');vis.dispose();
    const checkoutPortal=fixture(`<main id="search"><div class="s-main-slot">${card(1,10)}${card(2,100)}</div></main>`,'https://www.amazon.co.jp/gp/checkoutportal/enter-checkout.html'),cps=sorter.install(checkoutPortal.window);check(!sorter.isSearch(checkoutPortal.window),'APK checkout portal rejects stale strong search DOM');equal(badge(checkoutPortal),null);cps.dispose();

    // v106: the native callback receipt observes this exact runtime without changing it.
    const receiptKeys=['version','phase','roots','cards','known','status','error','limited'].sort();
    function validReceipt(value){
        equal(Object.keys(value).sort(),receiptKeys,'Fixed receipt schema');equal(value.version,'1.0.6');
        check(['inactive','waiting','collecting','ready','frozen','awaiting','unreadable','disposed','error'].includes(value.phase));
        for(const key of ['roots','cards','known'])check(Number.isInteger(value[key])&&value[key]>=0&&value[key]<=100,key+' is bounded');
        equal(typeof value.status,'boolean');equal(typeof value.limited,'boolean');
        check(value.error===null||['TypeError','ReferenceError','SyntaxError','RangeError','Error'].includes(value.error));
    }
    for(const route of ['/gp/aw/homepage.html','/gp/help/customer/display.html']){
        const f=fixture(`<div class="s-main-slot">${card(1,1313)}${card(2,34)}${card(3,'1.2万')}</div>`,'https://www.amazon.co.jp'+route),state=sorter.install(f.window);
        equal(state.report().phase,'inactive','A canonical list without #search needs verified native search context');equal(badge(f),null);
        state.setSearchHint(true,'bridge106');equal(state.report().phase,'waiting','The queued native activation has an honest synchronous receipt');
        equal(state.report().cards,3);check(sorter.isSearch(f.window,true),'Verified hint plus canonical list recognizes search without #search');
        f.flush();check(!!badge(f));await settle(f);equal(ids(f.document),['p3','p1','p2']);equal(state.report().phase,'frozen');validReceipt(state.report());state.dispose();
    }
    for(const route of ['/dp/'+asin(1),'/gp/checkoutportal/enter-checkout.html','/hz/cart']){
        const f=fixture(`<div class="s-main-slot">${card(1,34)}${card(2,12000)}</div>`,'https://www.amazon.co.jp'+route),state=sorter.install(f.window);
        state.setSearchHint(true,'bridge106');equal(state.report().phase,'inactive');equal(state.report().cards,0);f.flush();equal(badge(f),null);equal(ids(f.document),['p1','p2']);state.dispose();
    }
    const noRoot=fixture(`<section>${card(1,34)}${card(2,12000)}</section>`,'https://www.amazon.co.jp/gp/aw/homepage.html'),nrs=sorter.install(noRoot.window);
    nrs.setSearchHint(true,'unknown106');equal(nrs.report().phase,'waiting');noRoot.flush();check(!!badge(noRoot),'Missing canonical root is observable');equal(nrs.report().cards,0);equal(ids(noRoot.document),['p1','p2']);nrs.dispose();

    const passive=layout(fixture(`<div class="s-main-slot secret-private-class">${card(1,1313).replace('商品1','SECRET_PRODUCT_TITLE')}${card(2,null)}</div>`,'https://www.amazon.co.jp/s?k=SECRET_SEARCH_QUERY')),pas=sorter.install(passive.window);
    const htmlBefore=passive.document.documentElement.outerHTML,timersBefore=Array.from(passive.pending,([id,t])=>[id,t.due]),scrollBefore=passive.scrolls.slice(),phaseBefore=pas.phase;
    const diagnostic=pas.report();validReceipt(diagnostic);equal(diagnostic.cards,2);equal(diagnostic.known,1);equal(diagnostic.roots,1);
    for(let i=0;i<8;i++)equal(pas.report(),diagnostic,'Repeated passive receipt is stable');
    equal(passive.document.documentElement.outerHTML,htmlBefore,'Receipt causes no DOM writes');equal(Array.from(passive.pending,([id,t])=>[id,t.due]),timersBefore,'Receipt neither queues nor cancels work');equal(passive.scrolls,scrollBefore);equal(pas.phase,phaseBefore);
    for(const secret of ['SECRET_PRODUCT_TITLE','SECRET_SEARCH_QUERY',asin(1),'amazon.co.jp','secret-private-class'])check(!JSON.stringify(diagnostic).includes(secret),'No page identity in receipt');
    check(!pas.setNativeCheckpoint('SECRET_VERSION','search'));check(!pas.setNativeCheckpoint('1.0.6','SECRET_ROUTE'));equal(pas.nativeCheckpoint,undefined);
    check(pas.setNativeCheckpoint('1.0.6','search'));equal(pas.nativeCheckpoint,{version:'1.0.6',route:'search'});equal(pas.report(),diagnostic,'Checkpoint metadata cannot alter the passive report schema');
    equal(passive.document.documentElement.outerHTML,htmlBefore);equal(Array.from(passive.pending,([id,t])=>[id,t.due]),timersBefore);
    const originalQuery=passive.document.querySelectorAll.bind(passive.document);
    for(const exception of [new TypeError('SECRET_ERROR_MESSAGE'),Object.assign(new Error('SECRET_OTHER'),{name:'SECRET_ERROR_NAME'}),Object.defineProperty({},'name',{get(){throw new Error('SECRET_GETTER');}})]){
        passive.document.querySelectorAll=()=>{throw exception;};const failed=pas.report();validReceipt(failed);equal(failed.phase,'error');check(['TypeError','Error'].includes(failed.error));check(!JSON.stringify(failed).includes('SECRET'));equal(pas.phase,phaseBefore,'A report failure cannot change sorting state');
    }
    passive.document.querySelectorAll=originalQuery;pas.dispose();equal(pas.report().phase,'disposed');equal(pas.report().cards,0);equal(passive.pending.size,0);

    const bounded=fixture(`<div class="s-main-slot">${batch(1,101)}</div>`,'https://www.amazon.co.jp/'),bds=sorter.install(bounded.window),boundedReceipt=bds.report();validReceipt(boundedReceipt);equal(boundedReceipt.cards,100);equal(boundedReceipt.known,100);check(boundedReceipt.limited,'Receipt makes truncation explicit');equal(bounded.pending.size,0,'Inactive diagnostic does not start collection');bds.dispose();
    const rootOnly=fixture('<div class="s-main-slot"></div>','https://www.amazon.co.jp/'),ros=sorter.install(rootOnly.window);equal(ros.report().roots,1);equal(ros.report().cards,0);ros.dispose();

    console.log('PASS '+JSON.stringify({status:'passed',assertions,dom:'linkedom 0.18.12',synthetic_geometry_tested:true,real_browser_layout_tested:false,physical_device_tested:false}));
})().catch(error=>{console.error(error);process.exitCode=1;});
