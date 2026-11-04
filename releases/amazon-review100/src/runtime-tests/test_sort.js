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
    const window = {document: parsed.document, MutationObserver: parsed.window.MutationObserver, Event: parsed.window.Event, addEventListener: parsed.window.addEventListener.bind(parsed.window), removeEventListener: parsed.window.removeEventListener.bind(parsed.window)};
    let address = new URL(url);
    Object.defineProperty(window, 'location', {value: address, writable: true});
    const pending = new Map(); let serial = 1;
    window.setTimeout = function (fn) {const id = serial++; pending.set(id, fn); return id;};
    window.clearTimeout = function (id) {pending.delete(id);};
    Object.defineProperty(window, 'history', {value: {
        pushState: function (_s, _t, to) {if (to) window.location = new URL(to, window.location.href); return 7;},
        replaceState: function (_s, _t, to) {if (to) window.location = new URL(to, window.location.href); return 8;}
    }, writable: true});
    return {window, document: window.document, pending, flush() {const todo = Array.from(pending.values()); pending.clear(); todo.forEach(fn => fn());}};
}
function ids(doc) {return Array.from(doc.querySelector('.s-main-slot').children).map(n => n.id);}
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
    equal(f.document.getElementById('hiro-review-count-status').textContent, '評価件数順（読み込み済み 5 商品）・件数不明は末尾');
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
    equal(ids(noCounts.document), ['p1','p2']); equal(noCounts.document.getElementById('hiro-review-count-status'), null, 'No false successful status'); noCountState.dispose();
    const unsafe = fixture(`<div class="s-main-slot">${card(1,'2')}${card(2,'100')}</div>`, 'https://www.amazon.co.jp.attacker.example/s');
    const unsafeState = sorter.install(unsafe.window); equal(ids(unsafe.document), ['p1','p2'], 'Untrusted origin ignored'); unsafeState.dispose();
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
    for (const url of ['https://www.amazon.co.jp/s', 'https://www.amazon.com/s/ref=abc', 'https://www.amazon.co.jp/gp/aw/search/s', 'https://m.amazon.co.jp/gp/aw/s/ref=x']) check(sorter.isSearch({location:new URL(url)}), url);
    for (const url of ['http://www.amazon.co.jp/s','https://example.com/s','https://www.amazon.co.jp/suspicious','https://www.amazon.co.jp/s/browse','https://www.amazon.co.jp/dp/B000000001','https://amazon.co.jp.evil.test/s']) check(!sorter.isSearch({location:new URL(url)}), url);
    console.log('PASS ' + JSON.stringify({status:'passed',assertions,dom:'linkedom 0.18.12',physical_device_tested:false}));
})().catch(error => {console.error(error); process.exitCode = 1;});
