'use strict';
const assert = require('node:assert/strict');
const {parseHTML} = require('linkedom');
const sorter = require('../runtime/review-sort.js');
let assertions = 0;
function check(condition, message) {assert.ok(condition, message); assertions++;}
function equal(a, b, message) {assert.deepEqual(a, b, message); assertions++;}
function asin(n) {return 'B' + String(n).padStart(9, '0');}
function card(n, count, extra = '') {
    return `<div class="s-result-item" data-component-type="s-search-result" data-asin="${asin(n)}" id="p${n}">
      <h2><a href="/dp/${asin(n)}">商品 ${n} 9999個</a></h2><span class="a-price">￥98,765</span>
      <div data-cy="reviews-block"><a href="/dp/${asin(n)}/#customerReviews"><i class="a-icon-star-small"><span class="a-icon-alt">5つ星のうち4.5</span></i></a>
      ${count === null ? '' : `<a href="/dp/${asin(n)}/#customerReviews"><span class="a-size-small s-underline-text">${count}</span></a>`}</div>${extra}</div>`;
}
function fixture(body, url = 'https://www.amazon.co.jp/s?k=test') {
    const parsed = parseHTML(`<html><body>${body}</body></html>`);
    const listeners = new Map();
    const window = {document: parsed.document, MutationObserver: parsed.window.MutationObserver, Event: parsed.window.Event,
        addEventListener(type,fn) { if(!listeners.has(type)) listeners.set(type,new Set()); listeners.get(type).add(fn); },
        removeEventListener(type,fn) { if(listeners.has(type)) listeners.get(type).delete(fn); },
        dispatchEvent(event) { if(listeners.has(event.type)) Array.from(listeners.get(event.type)).forEach(fn=>fn(event)); return true; }};
    let address = new URL(url);
    Object.defineProperty(window, 'location', {value: address, writable: true});
    const pending = new Map(); let serial = 1, clock = 10000;
    window.performance = {now:()=>clock};
    window.setTimeout = function (fn,delay=0) {const id = serial++; pending.set(id, {fn,due:clock+delay}); return id;};
    window.clearTimeout = function (id) {pending.delete(id);};
    Object.defineProperty(window, 'history', {value: {
        pushState: function (_s, _t, to) {if (to) window.location = new URL(to, window.location.href); return 7;},
        replaceState: function (_s, _t, to) {if (to) window.location = new URL(to, window.location.href); return 8;}
    }, writable: true});
    function advance(ms) { clock+=ms; const todo=Array.from(pending.entries()).filter(([id,t])=>t.due<=clock); for(const [id,t] of todo) {pending.delete(id); t.fn();} }
    return {window, document: window.document, pending, advance, emit(type,props={}) {window.dispatchEvent(Object.assign({type},props));}, flush() {if(pending.size) advance(Math.max(...Array.from(pending.values()).map(t=>t.due))-clock);}};
}
function ids(doc) {return Array.from(doc.querySelector('.s-main-slot').children).filter(n=>n.id!=='hiro-review-count-status').map(n => n.id);}
async function mutations() {await Promise.resolve(); await new Promise(resolve => setImmediate(resolve));}

(async function () {
    const valid = [['1,234',1234],['(1,234)',1234],['（１２，３４５）',12345],['1.2万',12000],['1.2K',1200],['1,2K',1200],['2M',2000000],['1 234',1234],['1.234',1234],['0',0],['1,000+',1000],['4.5 out of 5 stars, 1,234 ratings',1234],['5つ星のうち4.5、12,345件の評価',12345],['5つ星のうち4.5, 12,345個の評価',12345],['5 1,234 ratings',1234],['5つ星のうち4.5 1,234個の評価',1234],['1.2万個の評価',12000]];
    for (const [input, expected] of valid) equal(sorter.parseCount(input, true), expected, input);
    for (const bad of ['4.5 out of 5 stars','5つ星のうち4.5','￥1,234','1,234円','4.5','9999 bought in past month','過去1か月で1000点以上購入されました','1,23','1.2.3','1 2','Infinite','-10','',null]) equal(sorter.parseCount(bad, true), -1, String(bad));
    equal(sorter.parseCount('1234', false), -1, 'Unknown labels cannot be bare counts');
    equal(sorter.parseCount('1,234 ratings', false), 1234);

    const f = fixture(`<main id="search"><div class="s-main-slot">${card(1,'12')}${card(2,'1,234')}<div class="s-result-item AdHolder" data-component-type="s-search-result" data-asin="${asin(3)}" id="sponsored">広告</div>${card(4,'1.2万')}${card(5,'1,234')}${card(6,null)}<div id="pagination">次へ</div></div></main>`);
    const original = f.document.getElementById('p2'); let clicks = 0;
    original.addEventListener('click', () => clicks++);
    const state = sorter.install(f.window);
    equal(ids(f.document), ['p4','p2','sponsored','p5','p1','p6','pagination'], 'Stable sort retains unrelated slots');
    check(original === f.document.getElementById('p2'), 'Original product nodes survive');
    original.dispatchEvent(new f.window.Event('click'));
    equal(clicks, 1, 'Native DOM event listener retained');
    equal(f.document.getElementById('hiro-review-count-status').textContent, '評価件数順（読み込み済み 5 商品） v1.0.2・件数不明は末尾');
    await mutations(); equal(f.pending.size, 0, 'Own sort creates no observer feedback timer');
    equal(sorter.install(f.window), state, 'Repeated native injection is idempotent');
    f.flush(); await mutations(); equal(f.pending.size, 0, 'Idempotent run produces no loop');

    const list = f.document.querySelector('.s-main-slot');
    list.insertAdjacentHTML('beforeend', card(7,'99,999'));
    await mutations(); equal(f.pending.size, 1, 'AJAX append debounces one sort');
    f.flush();
    equal(ids(f.document), ['p7','p4','sponsored','p2','p5','p1','pagination','p6'], 'New loaded card participates in product slots only');
    await mutations(); equal(f.pending.size, 0, 'AJAX sort settles');
    f.document.querySelector('#p1 .s-underline-text').textContent = '200,000';
    await mutations(); equal(f.pending.size, 1, 'Changed count schedules update');
    f.flush(); equal(ids(f.document)[0], 'p1', 'Updated review count changes order');
    await mutations(); equal(f.pending.size, 0);

    equal(f.window.history.pushState({},'', '/dp/' + asin(1)), 7, 'Preserve native history return');
    f.flush();
    equal(f.document.getElementById('hiro-review-count-status'), null, 'Remove label after SPA leaves search');
    const snapshot = ids(f.document);
    f.document.querySelector('#p2 .s-underline-text').textContent = '999,999';
    await mutations(); equal(f.pending.size, 0, 'No scanning on product page'); equal(ids(f.document), snapshot);
    f.window.history.replaceState({},'', '/s?k=again'); f.flush(); equal(ids(f.document)[0], 'p2', 'Resume after SPA re-enters search');
    state.dispose(); await mutations(); equal(f.pending.size, 0, 'Dispose clears timer');

    const noCounts = fixture(`<div class="s-main-slot">${card(1,null)}${card(2,null)}</div>`);
    const noCountState = sorter.install(noCounts.window);
    equal(ids(noCounts.document), ['p1','p2']); equal(noCounts.document.getElementById('hiro-review-count-status').getAttribute('data-hiro-state'), 'unreadable', 'Unknown counts remain visibly distinct from sorting success'); noCountState.dispose();
    const unsafe = fixture(`<div class="s-main-slot">${card(1,'2')}${card(2,'100')}</div>`, 'https://www.amazon.co.jp.attacker.example/s');
    const unsafeState = sorter.install(unsafe.window); equal(ids(unsafe.document), ['p1','p2'], 'Untrusted origin ignored'); equal(unsafeState, null, 'Untrusted origin cannot install bootstrap');
    const product = fixture(`<div id="dp"><div class="s-main-slot">${card(1,'2')}${card(2,'100')}</div></div>`);
    equal(sorter.chooseGroup(product.document), null, 'Recommendation containers ignored');
    const nested = fixture(`<div class="s-main-slot">${card(1,'12', `<div class="a-carousel">${card(90,'999,999')}</div>`)}${card(2,'30')}</div>`);
    const nestedState = sorter.install(nested.window); equal(ids(nested.document), ['p2','p1'], 'Nested carousel items never become main cards'); nestedState.dispose();
    const unrelated = fixture(`<div class="s-main-slot"><div data-asin="${asin(1)}" class="s-result-item" id="p1"><h2>100 ratings</h2><span class="a-price">1,234</span></div>${card(2,'3')}</div>`);
    equal(sorter.readCount(unrelated.document.getElementById('p1')), -1, 'Titles and prices ignored');
    const starAndCount = fixture(`<div class="s-result-item" data-asin="${asin(1)}"><a href="#customerReviews" aria-label="4 out of 5 stars"><span class="a-size-base">4</span><i class="a-icon-star"></i></a><a href="#customerReviews"><span class="s-underline-text">1,234</span></a></div>`);
    equal(sorter.readCount(starAndCount.document.querySelector('.s-result-item')), 1234, 'Integer star value never mistaken for count');
    const numericTitle = fixture(`<div class="s-result-item" data-asin="${asin(1)}"><a class="a-link-normal s-underline-text s-underline-link-text s-link-style" href="/dp/${asin(1)}">1984</a></div>`);
    equal(sorter.readCount(numericTitle.document.querySelector('.s-result-item')), -1, 'Generic numeric product title ignored');
    const accessible = fixture(`<div data-component-type="s-search-result" class="s-result-item" data-asin="${asin(1)}"><div data-cy="reviews-ratings" aria-label="5つ星のうち4.5、12,345件の評価">4.5</div></div>`);
    equal(sorter.readCount(accessible.document.querySelector('.s-result-item')), 12345, 'Combined accessible rating label');
    const mob = fixture(`<ul id="s-results-list-atf"><li data-asin="${asin(1)}"><a href="/product-reviews/${asin(1)}">(90)</a></li><li data-asin="${asin(2)}"><a href="/product-reviews/${asin(2)}">1.2K</a></li></ul>`, 'https://www.amazon.co.jp/gp/aw/s?k=x');
    const mobState = sorter.install(mob.window);
    equal(Array.from(mob.document.querySelector('ul').children).map(n => n.getAttribute('data-asin')), [asin(2),asin(1)], 'Mobile search list supported'); mobState.dispose();
    for (const url of ['https://www.amazon.co.jp/s', 'https://www.amazon.com/s/ref=abc', 'https://www.amazon.co.jp/gp/aw/search/s', 'https://m.amazon.co.jp/gp/aw/s/ref=x','https://www.amazon.co.jp/batteries/s','https://www.amazon.co.jp/-/en/s','https://www.amazon.co.jp/gp/search','https://www.amazon.co.jp/mn/search','https://www.amazon.co.jp/search-by-list/a']) check(sorter.isSearch({location:new URL(url)}), url);
    for (const url of ['http://www.amazon.co.jp/s','https://example.com/s','https://www.amazon.co.jp/suspicious','https://www.amazon.co.jp/s/browse','https://www.amazon.co.jp/dp/B000000001','https://amazon.co.jp.evil.test/s']) check(!sorter.isSearch({location:new URL(url)}), url);

    // The five reproduced v100 failures: compact count, shared-star link,
    // generic explicit label, same-ASIN metadata, and list wrappers.
    function rawCard(n, html) { return `<div class="s-result-item" data-component-type="s-search-result" data-asin="${asin(n)}" id="p${n}">${html}</div>`; }
    const variants = [
        '<div data-cy="reviews-block"><div class="a-row"><span class="a-size-small a-color-base">4.5</span><i class="a-icon a-icon-star-small a-star-small-4-5"></i><span class="a-size-mini puis-normal-weight-text">(1,234)</span></div></div>',
        `<div data-cy="reviews-block"><a href="/dp/${asin(1)}/#customerReviews"><span class="a-size-small a-color-base">4.5</span><i class="a-icon a-icon-star-small a-star-small-4-5"></i><span class="a-size-mini puis-normal-weight-text">(1,234)</span></a></div>`,
        '<div class="a-row"><span aria-label="1,234件の評価">(1,234)</span></div>',
        `<div data-asin="${asin(1)}"><a href="/dp/${asin(1)}/#customerReviews"><span class="s-underline-text">1,234</span></a></div>`
    ];
    for (let i = 0; i < variants.length; i++) {
        const x = fixture(`<div class="s-main-slot">${rawCard(1,variants[i])}${card(2,'20,000')}</div>`);
        equal(sorter.readCount(x.document.getElementById('p1')), 1234, 'v100 missing DOM form ' + i);
        const st = sorter.install(x.window);
        equal(ids(x.document), ['p2','p1'], 'v100 missing DOM form reorders ' + i);
        equal(x.document.getElementById('hiro-review-count-status').getAttribute('data-hiro-state'), 'sorted'); st.dispose();
    }
    const wrapped = fixture(`<div class="s-main-slot"><div id="row">${card(1,'20')}${card(2,'200')}</div></div>`);
    const wrappedState = sorter.install(wrapped.window);
    equal(Array.from(wrapped.document.getElementById('row').children).map(n=>n.id), ['p2','p1'], 'One grouping wrapper under main root');
    check(wrapped.document.getElementById('row').parentNode.classList.contains('s-main-slot'), 'List wrapper retained'); wrappedState.dispose();
    const slots = fixture(`<div class="s-main-slot"><div id="slot1">\n<div>${card(1,'20')}</div>\n</div><aside id="ad">広告枠</aside><section id="slot2">${card(2,'200')}</section></div>`);
    // Replace literal fixture escapes by actual ignorable text, as real HTML whitespace.
    for(const n of Array.from(slots.document.querySelector('#slot1').childNodes)) if(n.nodeType===3) n.textContent='\n'.replace('\\n','\n');
    const keptSlot = slots.document.getElementById('slot2'); let slotClicks=0; keptSlot.addEventListener('click',()=>slotClicks++);
    const slotsState = sorter.install(slots.window);
    equal(ids(slots.document), ['slot2','ad','slot1'], 'Safe single-product wrappers move whole and keep unrelated slots');
    keptSlot.dispatchEvent(new slots.window.Event('click')); equal(slotClicks,1); slotsState.dispose();
    const unsafeWrap = fixture(`<div class="s-main-slot"><section id="slot1"><h2 id="fixedheading">固定の見出し</h2>${card(1,'20')}</section><section id="slot2">${card(2,'200')}</section></div>`);
    const unsafeWrapState = sorter.install(unsafeWrap.window);
    equal(ids(unsafeWrap.document), ['slot1','slot2'], 'Wrapper with unrelated heading cannot move');
    equal(unsafeWrap.document.getElementById('fixedheading').parentNode.id,'slot1'); unsafeWrapState.dispose();
    const foreign = fixture(`<div class="s-main-slot">${rawCard(1,`<div data-asin="${asin(90)}"><span aria-label="999,999件の評価">999,999</span><a href="#customerReviews"><span class="s-underline-text">999,999</span></a></div>`)}${card(2,'20')}</div>`);
    equal(sorter.readCount(foreign.document.getElementById('p1')), -1, 'Foreign nested product counts excluded');
    const nestedReview = fixture(`<div class="s-main-slot">${rawCard(1,`<div class="a-carousel"><span aria-label="999,999件の評価">999,999</span></div>`)}${card(2,'20')}</div>`);
    equal(sorter.readCount(nestedReview.document.getElementById('p1')), -1, 'Carousel counts excluded');
    const barePrice = fixture(rawCard(1,'<div class="a-row"><span class="a-size-mini puis-normal-weight-text">1984</span></div>'));
    equal(sorter.readCount(barePrice.document.getElementById('p1')), -1, 'Bare compact style without rating context excluded');
    const intShared = fixture(rawCard(1,'<div data-cy="reviews-block"><a href="#customerReviews" aria-label="4 out of 5 stars"><span class="a-size-base">4</span><i class="a-icon-star"></i><span class="a-size-base">(1,234)</span></a></div>'));
    equal(sorter.readCount(intShared.document.getElementById('p1')),1234,'Integer star skipped in combined fallback');

    const linkedNumericTitle = fixture(rawCard(1,`<div class="a-row"><a href="/dp/${asin(1)}" class="s-underline-text"><h2>1984</h2></a><i class="a-icon-star"></i><a href="#customerReviews"><span class="s-underline-text">1,234</span></a></div>`));
    equal(sorter.readCount(linkedNumericTitle.document.getElementById('p1')),1234,'Numeric title anchor surrounding heading is not a review count');
    const plainNumericTitle = fixture(rawCard(1,`<div class="a-row"><a href="/dp/${asin(1)}" class="s-underline-text">1984</a><i class="a-icon-star"></i><a href="#customerReviews"><span class="s-underline-text">1,234</span></a></div>`));
    equal(sorter.readCount(plainNumericTitle.document.getElementById('p1')),1234,'Numeric non-review link in star row is not a count');

    const compactStar = fixture(rawCard(1,'<a href="#customerReviews" aria-label="4 out of 5 stars"><span class="s-underline-text">4</span><i class="a-icon-star"></i></a><a href="#customerReviews"><span class="s-underline-text">1,234</span></a>'));
    equal(sorter.readCount(compactStar.document.getElementById('p1')),1234,'Compact integer star cannot preempt later count');
    const compactStarRow = fixture(rawCard(1,'<div data-cy="reviews-block"><span class="a-size-mini puis-normal-weight-text">4</span><i class="a-icon-star"></i><span class="a-size-mini puis-normal-weight-text">(1,234)</span></div>'));
    equal(sorter.readCount(compactStarRow.document.getElementById('p1')),1234,'Compact integer star outside link skipped');
    const smallCount = fixture(rawCard(1,'<a href="#customerReviews" aria-label="4 out of 5 stars"><i class="a-icon-star"></i></a><a href="#customerReviews"><span class="s-underline-text">3</span></a>'));
    equal(sorter.readCount(smallCount.document.getElementById('p1')),3,'Separate count link may legitimately contain small integer');
    const groups = fixture(`<div class="s-result-list" id="outside">${card(91,'90')}${card(92,'100')}${card(93,'200')}${card(94,'1')}</div><main id="search"><div id="s-results-list-atf">${card(11,'9')}${card(12,'10')}</div><div class="s-main-slot">${card(1,'4')}${card(2,'500')}${card(3,'30')}</div></main>`);
    const groupState=sorter.install(groups.window);
    equal(ids(groups.document),['p12','p11','p1'],'Compatible main/ATF lists form one descending sequence');
    equal(Array.from(groups.document.getElementById('s-results-list-atf').children).map(n=>n.id),['p2','p3']);
    equal(Array.from(groups.document.getElementById('outside').children).map(n=>n.id),['p91','p92','p93','p94'],'Outside search landmark remains unchanged'); groupState.dispose();

    const emptySearch = fixture('<main id="search"></main>'); const emptyState=sorter.install(emptySearch.window);
    equal(emptySearch.document.getElementById('hiro-review-count-status').getAttribute('data-hiro-state'),'waiting','Activated empty search remains visible');
    check(emptySearch.document.getElementById('hiro-review-count-status').textContent.includes('1.0.2'),'Visible version marker');
    emptySearch.document.querySelector('#search').innerHTML=`<div class="s-main-slot">${card(1,'4')}${card(2,'500')}</div>`;
    await mutations(); emptySearch.flush(); equal(ids(emptySearch.document),['p2','p1'],'Late results sort without reinjection'); emptyState.dispose();

    const home=fixture('<main id="home">ホーム</main>','https://www.amazon.co.jp/'); const homeState=sorter.install(home.window);
    check(!!homeState,'Bootstrap installs on trusted home'); equal(home.document.getElementById('hiro-review-count-status'),null,'No search badge on home');
    home.document.getElementById('home').textContent='おすすめ商品を更新'; await mutations(); equal(home.pending.size,0,'Unrelated home updates do not sort');
    home.window.history.pushState({},'','/s?k=transition'); home.document.body.innerHTML=`<main id="search"><div class="s-main-slot">${card(1,'4')}${card(2,'500')}</div></main>`;
    await mutations(); home.flush(); equal(ids(home.document),['p2','p1'],'Home to same-document search activates');
    await mutations(); equal(home.pending.size,0,'Home transition settles'); homeState.dispose();

    const staleHome=fixture('<main id="home">ホーム</main>','https://www.amazon.co.jp/'); const staleState=sorter.install(staleHome.window);
    staleHome.document.body.innerHTML=`<main id="search"><div class="s-main-slot">${card(1,'4')}${card(2,'500')}</div></main>`;
    await mutations(); staleHome.flush(); equal(ids(staleHome.document),['p2','p1'],'Strong search DOM works while app URL still says home'); staleState.dispose();
    for(const attr of ['hidden','style','aria-hidden']) {
        const visibility = attr==='hidden' ? 'hidden' : attr==='style' ? 'style="display:none"' : 'aria-hidden="true"';
        const cached=fixture(`<main id="search" ${visibility}><div class="s-main-slot">${card(1,'4')}${card(2,'500')}</div></main>`,'https://www.amazon.co.jp/');
        const cachedState=sorter.install(cached.window);
        equal(cached.document.getElementById('hiro-review-count-status'),null,'Hidden cached search initially inactive '+attr);
        cached.document.getElementById('search').removeAttribute(attr);
        await mutations(); equal(cached.pending.size,1,'Visibility-only search transition schedules '+attr); cached.flush();
        equal(ids(cached.document),['p2','p1'],'Cached search activates after '+attr+' removal');
        await mutations(); equal(cached.pending.size,0,'Cached visibility activation settles '+attr); cachedState.dispose();
    }

    const recHome=fixture(`<div class="s-main-slot">${card(1,'4')}${card(2,'500')}</div>`,'https://www.amazon.co.jp/'); const recState=sorter.install(recHome.window);
    equal(ids(recHome.document),['p1','p2'],'Home recommendations without search landmark cannot sort');
    recState.setSearchHint(true); recHome.flush(); equal(ids(recHome.document),['p1','p2'],'Native boolean hint never permits sorting recommendations');
    equal(recHome.document.getElementById('hiro-review-count-status').getAttribute('data-hiro-state'),'waiting','Native hint only displays waiting state');
    recState.setSearchHint(false); recHome.flush(); equal(recHome.document.getElementById('hiro-review-count-status'),null); recState.dispose();
    for(const path of ['/dp/'+asin(1),'/gp/cart/view.html','/gp/buy/spc/handlers/display.html','/ap/signin','/hz/cart','/gp/aw/c','/your-orders/orders','/gp/your-account/order-history']) {
        const blocked=fixture(`<main id="search"><div class="s-main-slot">${card(1,'4')}${card(2,'500')}</div></main>`,'https://www.amazon.co.jp'+path);
        const bs=sorter.install(blocked.window); bs.setSearchHint(true); blocked.flush(); equal(ids(blocked.document),['p1','p2'],'No fallback sort in '+path); equal(blocked.document.getElementById('hiro-review-count-status'),null); bs.dispose();
    }
    const wrong=fixture(`<main id="search"><div class="s-main-slot">${card(1,'4')}${card(2,'500')}</div></main>`,'https://example.com/');
    equal(sorter.install(wrong.window),null,'Strong structure does not bypass host gate');


    // v102 regressions target the video symptom: a steady descending list must
    // not repeatedly move or change the viewport when lazy rendering updates it.
    function productIds(doc) {return Array.from(doc.querySelectorAll('#search [data-component-type="s-search-result"]')).map(n=>n.id);}
    function tracker(f) {
        let additions=0,badgeWrites=0;
        const obs=new f.window.MutationObserver(records=>records.forEach(r=>{
            if(r.type==='childList') for(const n of r.addedNodes) if(n.nodeType===1&&n.matches('[data-component-type="s-search-result"]')) additions++;
            const target=r.target.nodeType===1?r.target:r.target.parentElement;
            if(target&&(target.id==='hiro-review-count-status'||target.closest('#hiro-review-count-status'))) badgeWrites++;
        }));
        obs.observe(f.document.documentElement,{subtree:true,childList:true,attributes:true,characterData:true});
        return {reset(){additions=0;badgeWrites=0;},get additions(){return additions;},get badgeWrites(){return badgeWrites;},dispose(){obs.disconnect();}};
    }
    const paged=fixture(`<main id="search"><div class="s-main-slot" id="batch1">${card(1,'1000')}${card(2,'900')}<aside id="fixedwidget">固定広告枠</aside>${card(3,'800')}</div></main>`);
    const pagedState=sorter.install(paged.window); const pagedTrack=tracker(paged);
    paged.document.querySelector('#search').insertAdjacentHTML('beforeend',`<div class="s-result-list" id="batch2">${card(4,'5000')}${card(5,'950')}${card(6,'10')}</div>`);
    await mutations(); pagedTrack.reset(); paged.flush();
    equal(productIds(paged.document),['p4','p1','p5','p2','p3','p6'],'Append distinct compatible page group joins full descending sequence');
    equal(paged.document.getElementById('fixedwidget').parentNode.id,'batch1','Fixed widget keeps original group');
    equal(Array.from(paged.document.getElementById('batch1').children).map(n=>n.id),['p4','p1','fixedwidget','p5'],'Fixed widget keeps original slot');
    check(paged.document.getElementById('hiro-review-count-status').textContent.includes('6 商品'),'Badge counts all compatible loaded groups');
    await mutations(); pagedTrack.reset();
    for(let i=0;i<8;i++) {paged.document.getElementById('p1').style.borderColor=i%2?'red':'blue'; paged.document.getElementById('p1').classList.toggle('fixture-render-tick',!!(i%2)); await mutations(); paged.flush(); await mutations();}
    equal(pagedTrack.additions,0,'Style/class rendering churn never reparents an already ranked list'); equal(pagedTrack.badgeWrites,0,'Already correct badge receives zero DOM writes');
    const countSpan=paged.document.querySelector('#p4 .s-underline-text'); const countParent=countSpan.parentNode; countSpan.remove(); await mutations(); paged.flush(); await mutations();
    equal(productIds(paged.document),['p4','p1','p5','p2','p3','p6'],'Transient missing lazy count retains same node/ASIN last valid rank'); equal(pagedTrack.additions,0);
    countParent.appendChild(countSpan); await mutations(); paged.flush(); await mutations(); equal(pagedTrack.additions,0,'Restored lazy count cannot bounce product back');
    paged.window.history.replaceState({},'','/s?k=test&page=2&ref=sr_pg_2&qid=123'); paged.flush(); countSpan.remove(); await mutations(); paged.flush();
    equal(productIds(paged.document)[0],'p4','Pagination tracking does not invalidate known rating count');
    paged.document.getElementById('p4').setAttribute('data-asin',asin(404)); await mutations(); paged.flush();
    equal(productIds(paged.document).slice(-1),['p4'],'Reused node with different ASIN cannot inherit previous product count');
    countParent.appendChild(countSpan); countSpan.textContent='0'; await mutations(); paged.flush();
    equal(productIds(paged.document).slice(-1),['p4'],'Explicit valid zero replaces cached positive count');
    countSpan.textContent='5000'; await mutations(); paged.flush(); countSpan.remove();
    paged.window.history.replaceState({},'','/s?k=other-search'); await mutations(); paged.flush();
    equal(productIds(paged.document).slice(-1),['p4'],'New keyword invalidates old count cache'); pagedState.dispose(); pagedTrack.dispose();

    const headed=fixture(`<main id="search"><div class="s-result-list" id="primary"><h2>通常の商品</h2>${card(1,'200')}${card(2,'100')}</div><div class="s-result-list" id="related"><h2>関連する商品</h2>${card(3,'9000')}${card(4,'8000')}</div></main>`);
    const headedState=sorter.install(headed.window);
    equal(productIds(headed.document),['p1','p2','p3','p4'],'Distinct headed sections cannot exchange products'); headedState.dispose();
    const shapes=fixture(`<main id="search"><div class="s-result-list list-layout">${card(1,'200')}${card(2,'100')}</div><div class="s-result-list grid-layout">${card(3,'9000')}${card(4,'8000')}</div></main>`);
    const shapeState=sorter.install(shapes.window); equal(productIds(shapes.document),['p1','p2','p3','p4'],'Different presentation classes do not cross-sort'); shapeState.dispose();

    const gesture=fixture(`<main id="search"><div class="s-main-slot">${card(1,'100')}${card(2,'90')}${card(3,'80')}</div></main>`);
    const gestureState=sorter.install(gesture.window); const gt=tracker(gesture);
    gesture.emit('touchstart',{touches:[{}]}); gesture.emit('pointerdown',{pointerId:7});
    gesture.document.querySelector('#p3 .s-underline-text').textContent='5000'; await mutations(); gt.reset(); gesture.advance(100);
    equal(productIds(gesture.document),['p1','p2','p3'],'Touch prevents sort despite changed count');
    gesture.emit('touchmove',{touches:[{}]}); gesture.advance(1000); equal(gt.additions,0,'Long active touch cannot spin/move products');
    gesture.emit('touchend',{touches:[]}); gesture.emit('pointerup',{pointerId:7}); gesture.advance(160);
    gesture.emit('scroll'); gesture.advance(160); equal(gt.additions,0,'Momentum scroll extends quiet window');
    gesture.emit('scroll'); gesture.advance(319); equal(gt.additions,0,'No commit before full320ms idle'); gesture.advance(1); await mutations();
    equal(productIds(gesture.document),['p3','p1','p2'],'Deferred dirty order commits when motion actually stops');
    check(gt.additions>0,'Deferred sort is not starved'); gestureState.dispose(); gt.dispose();

    // Synthetic geometry is deliberately labeled: it verifies transaction math,
    // not Android/WebView rendering or native app-bar behavior.
    function geometry(f, startY=0) {
        f.window.innerHeight=300; f.window.scrollY=startY; f.window.scrollX=0; let corrections=0;
        Object.defineProperty(f.document,'scrollingElement',{configurable:true,value:f.document.documentElement});
        f.window.scrollTo=function(x,y){f.window.scrollY=Math.max(0,y);corrections++;f.emit('scroll');};
        f.installRects=function() { for(const n of f.document.querySelectorAll('[data-component-type="s-search-result"]')) n.getBoundingClientRect=function(){
            const list=Array.from(f.document.querySelectorAll('#search [data-component-type="s-search-result"]')); const top=list.indexOf(n)*100+(f.document.getElementById('hiro-review-count-status')?24:0)-f.window.scrollY;
            return {top,bottom:top+100,height:100,left:0,right:300,width:300};
        };}; f.installRects(); return {get corrections(){return corrections;}};
    }
    const initialTop=fixture(`<main id="search"><div class="s-main-slot">${card(1,'1')}${card(2,'3000')}${card(3,'2000')}${card(4,'1000')}</div></main>`); const topMetrics=geometry(initialTop);
    const topState=sorter.install(initialTop.window); equal(productIds(initialTop.document)[0],'p2','Initial top shows highest review count'); equal(initialTop.window.scrollY,0,'Initial sort must not scroll to old low-ranked first item'); equal(topMetrics.corrections,0); topState.dispose();
    const anchored=fixture(`<main id="search"><div class="s-main-slot" id="visiblebatch">${Array.from({length:10},(_,i)=>card(i+1,String(1000-i*50))).join('')}</div></main>`); const metrics=geometry(anchored); const anchoredState=sorter.install(anchored.window);
    anchored.window.scrollY=350; anchored.emit('scroll'); anchored.advance(320);
    const visible=anchored.document.getElementById('p4'), beforeTop=visible.getBoundingClientRect().top;
    anchored.document.querySelector('#search').insertAdjacentHTML('beforeend',`<div class="s-result-list">${card(11,'9000')}${card(12,'8000')}</div>`); anchored.installRects();
    await mutations(); anchored.flush();
    equal(productIds(anchored.document).slice(0,2),['p11','p12'],'New high-count batch enters correct global top');
    equal(visible.getBoundingClientRect().top,beforeTop,'Scrolled reader retains the same product pixel position'); equal(metrics.corrections,1,'One idle transaction applies one correction');
    const unchangedY=anchored.window.scrollY, anchoredTrack=tracker(anchored); await mutations(); anchored.flush(); await mutations(); anchoredTrack.reset();
    for(let i=0;i<5;i++){anchored.document.querySelector('#p1 .a-price').setAttribute('aria-label','image/layout refresh '+i);await mutations();anchored.flush();await mutations();}
    equal(anchored.window.scrollY,unchangedY,'Rendering churn never oscillates scroll position'); equal(metrics.corrections,1); equal(anchoredTrack.additions,0); equal(anchoredTrack.badgeWrites,0);
    // Only a tail swap: the visible stationary node must stay attached.
    let visibleInsertions=0; const oldInsert=visible.parentNode.insertBefore;
    visible.parentNode.insertBefore=function(node,before){if(node===visible)visibleInsertions++;return oldInsert.call(this,node,before);};
    anchored.document.querySelector('#p10 .s-underline-text').textContent='575'; await mutations(); anchored.flush();
    equal(visibleInsertions,0,'Unchanged visible position is not needlessly detached/reinserted'); anchoredState.dispose(); anchoredTrack.dispose();

    const independent=fixture(`<main id="search"><section id="scrollA"><div class="s-result-list">${card(1,'100')}${card(2,'90')}</div></section><section id="scrollB"><div class="s-result-list">${card(3,'9000')}${card(4,'8000')}</div></section></main>`);
    for(const id of ['scrollA','scrollB']) {const el=independent.document.getElementById(id); Object.defineProperty(el,'scrollHeight',{value:1000});Object.defineProperty(el,'clientHeight',{value:200});el.scrollTop=0;}
    independent.window.getComputedStyle=el=>({overflowY:el.id==='scrollA'||el.id==='scrollB'?'auto':'visible'});
    const independentState=sorter.install(independent.window); equal(productIds(independent.document),['p1','p2','p3','p4'],'Independent scrollers cannot exchange products'); independentState.dispose();

    console.log('PASS ' + JSON.stringify({status:'passed',assertions,dom:'linkedom 0.18.12',synthetic_geometry_tested:true,real_browser_layout_tested:false,physical_device_tested:false}));
})().catch(error => {console.error(error); process.exitCode = 1;});
