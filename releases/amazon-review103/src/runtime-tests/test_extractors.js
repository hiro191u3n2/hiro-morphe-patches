'use strict';

// Pure extraction/scope regressions retained from v100-v102. These use a parsed
// DOM only: no runtime installation, mutation scheduling, browser or layout claim.
const assert = require('node:assert/strict');
const {parseHTML} = require('linkedom');

async function run(sorterOverride) {
    const sorter = sorterOverride || require('../runtime/review-sort.js');
    let assertions = 0;
    function check(condition, message) { assert.ok(condition, message); assertions++; }
    function equal(actual, expected, message) { assert.deepEqual(actual, expected, message); assertions++; }
    function asin(n) { return 'B' + String(n).padStart(9, '0'); }
    function rawCard(n, html, attrs = '') {
        return `<div class="s-result-item" data-component-type="s-search-result" data-asin="${asin(n)}" id="p${n}" ${attrs}>${html}</div>`;
    }
    function card(n, count, extra = '') {
        return rawCard(n, `<h2><a href="/dp/${asin(n)}">商品 ${n} 9999個</a></h2><span class="a-price">￥98,765</span>
          <div data-cy="reviews-block"><a href="/dp/${asin(n)}/#customerReviews"><i class="a-icon-star-small"><span class="a-icon-alt">5つ星のうち4.5</span></i></a>
          ${count === null ? '' : `<a href="/dp/${asin(n)}/#customerReviews"><span class="a-size-small s-underline-text">${count}</span></a>`}</div>${extra}`);
    }
    function fixture(body, url = 'https://www.amazon.co.jp/s?k=test') {
        const parsed = parseHTML(`<html><body>${body}</body></html>`);
        return {document: parsed.document, location: new URL(url)};
    }
    function read(html) {
        const f = fixture(rawCard(1, html));
        return sorter.readCount(f.document.getElementById('p1'));
    }
    function groupIds(groups) { return groups.map(group => group.cards.map(node => node.id)); }

    const valid = [
        ['1,234', 1234], ['(1,234)', 1234], ['（１２，３４５）', 12345],
        ['1.2万', 12000], ['1.2K', 1200], ['1,2K', 1200], ['2M', 2000000],
        ['1 234', 1234], ['1.234', 1234], ['0', 0], ['1,000+', 1000],
        ['4.5 out of 5 stars, 1,234 ratings', 1234], ['5つ星のうち4.5、12,345件の評価', 12345],
        ['5つ星のうち4.5, 12,345個の評価', 12345], ['5 1,234 ratings', 1234],
        ['5つ星のうち4.5 1,234個の評価', 1234], ['1.2万個の評価', 12000]
    ];
    for (const [input, expected] of valid) equal(sorter.parseCount(input, true), expected, 'Count format: ' + input);
    for (const bad of ['4.5 out of 5 stars', '5つ星のうち4.5', '￥1,234', '1,234円', '4.5', '9999 bought in past month', '過去1か月で1000点以上購入されました', '1,23', '1.2.3', '1 2', 'Infinite', '-10', '', null]) {
        equal(sorter.parseCount(bad, true), -1, 'Reject unrelated/invalid count: ' + String(bad));
    }
    equal(sorter.parseCount('1234', false), -1, 'Generic labels cannot supply bare numbers');
    equal(sorter.parseCount('1,234 ratings', false), 1234, 'Explicit count semantics permit named labels');

    const standard = fixture(`<div class="s-main-slot">${card(1, '1,234')}${card(2, null)}</div>`);
    equal(sorter.readCount(standard.document.getElementById('p1')), 1234, 'Traditional separate review hyperlink');
    equal(sorter.readCount(standard.document.getElementById('p2')), -1, 'No count stays unknown');
    equal(read('<h2>100 ratings</h2><span class="a-price">1,234</span>'), -1, 'Titles and prices ignored');
    equal(read('<a href="#customerReviews" aria-label="4 out of 5 stars"><span class="a-size-base">4</span><i class="a-icon-star"></i></a><a href="#customerReviews"><span class="s-underline-text">1,234</span></a>'), 1234, 'Integer star score cannot preempt count');
    equal(read(`<a class="a-link-normal s-underline-text s-underline-link-text s-link-style" href="/dp/${asin(1)}">1984</a>`), -1, 'Generic numeric product link ignored');
    equal(read('<div data-cy="reviews-ratings" aria-label="5つ星のうち4.5、12,345件の評価">4.5</div>'), 12345, 'Combined accessible rating label');

    // These four actual source omissions were independently reproduced in v100.
    const variants = [
        '<div data-cy="reviews-block"><div class="a-row"><span class="a-size-small a-color-base">4.5</span><i class="a-icon a-icon-star-small a-star-small-4-5"></i><span class="a-size-mini puis-normal-weight-text">(1,234)</span></div></div>',
        `<div data-cy="reviews-block"><a href="/dp/${asin(1)}/#customerReviews"><span class="a-size-small a-color-base">4.5</span><i class="a-icon a-icon-star-small a-star-small-4-5"></i><span class="a-size-mini puis-normal-weight-text">(1,234)</span></a></div>`,
        '<div class="a-row"><span aria-label="1,234件の評価">(1,234)</span></div>',
        `<div data-asin="${asin(1)}"><a href="/dp/${asin(1)}/#customerReviews"><span class="s-underline-text">1,234</span></a></div>`
    ];
    for (let i = 0; i < variants.length; i++) equal(read(variants[i]), 1234, 'v100 missing count DOM form ' + i);
    equal(read(`<div data-asin="${asin(90)}"><span aria-label="999,999件の評価">999,999</span><a href="#customerReviews"><span class="s-underline-text">999,999</span></a></div>`), -1, 'Foreign nested product count excluded');
    equal(read('<div class="a-carousel"><span aria-label="999,999件の評価">999,999</span></div>'), -1, 'Carousel count excluded');
    equal(read('<div class="a-row"><span class="a-size-mini puis-normal-weight-text">1984</span></div>'), -1, 'Compact style without rating context is insufficient');
    equal(read('<div data-cy="reviews-block"><a href="#customerReviews" aria-label="4 out of 5 stars"><span class="a-size-base">4</span><i class="a-icon-star"></i><span class="a-size-base">(1,234)</span></a></div>'), 1234, 'Combined link permits parenthesized count but rejects integer star');
    equal(read(`<div class="a-row"><a href="/dp/${asin(1)}" class="s-underline-text"><h2>1984</h2></a><i class="a-icon-star"></i><a href="#customerReviews"><span class="s-underline-text">1,234</span></a></div>`), 1234, 'Link containing a numeric heading is not a count');
    equal(read(`<div class="a-row"><a href="/dp/${asin(1)}" class="s-underline-text">1984</a><i class="a-icon-star"></i><a href="#customerReviews"><span class="s-underline-text">1,234</span></a></div>`), 1234, 'Numeric non-review link in a star row is not a count');
    equal(read('<a href="#customerReviews" aria-label="4 out of 5 stars"><span class="s-underline-text">4</span><i class="a-icon-star"></i></a><a href="#customerReviews"><span class="s-underline-text">1,234</span></a>'), 1234, 'Compact integer star cannot preempt later count');
    equal(read('<div data-cy="reviews-block"><span class="a-size-mini puis-normal-weight-text">4</span><i class="a-icon-star"></i><span class="a-size-mini puis-normal-weight-text">(1,234)</span></div>'), 1234, 'Compact integer star outside hyperlink skipped');
    equal(read('<a href="#customerReviews" aria-label="4 out of 5 stars"><i class="a-icon-star"></i></a><a href="#customerReviews"><span class="s-underline-text">3</span></a>'), 3, 'Separate count hyperlink can legitimately contain a small integer');
    equal(read('<span data-review-count="0"></span>'), 0, 'Explicit zero count retained');
    equal(read('<span data-customer-review-count="2,345"></span>'), 2345, 'Explicit customer count attribute');
    equal(read('<div class="a-price"><span data-review-count="9999"></span></div>'), -1, 'Price subtree excluded even with count-looking metadata');

    const acceptedUrls = ['https://www.amazon.co.jp/s', 'https://www.amazon.com/s/ref=abc', 'https://www.amazon.co.jp/gp/aw/search/s', 'https://m.amazon.co.jp/gp/aw/s/ref=x', 'https://www.amazon.co.jp/batteries/s', 'https://www.amazon.co.jp/-/en/s', 'https://www.amazon.co.jp/gp/search', 'https://www.amazon.co.jp/mn/search', 'https://www.amazon.co.jp/search-by-list/a'];
    for (const url of acceptedUrls) check(sorter.isSearch({location: new URL(url)}), 'Known search URL: ' + url);
    const rejectedUrls = ['http://www.amazon.co.jp/s', 'https://example.com/s', 'https://www.amazon.co.jp/suspicious', 'https://www.amazon.co.jp/s/browse', 'https://www.amazon.co.jp/dp/B000000001', 'https://amazon.co.jp.evil.test/s'];
    for (const url of rejectedUrls) check(!sorter.isSearch({location: new URL(url)}), 'Non-search or untrusted URL: ' + url);
    for (const url of ['https://amazon.co.jp/', 'https://www.amazon.co.jp:443/', 'https://smile.amazon.com/', 'https://m.amazon.co.uk/']) check(sorter.isAmazon({location: new URL(url)}), 'Trusted marketplace origin: ' + url);
    for (const url of ['http://www.amazon.co.jp/', 'https://www.amazon.co.jp:8443/', 'https://user@www.amazon.co.jp/', 'https://www.amazon.co.jp.attacker.example/', 'https://amazon.example/', 'file:///s', 'javascript:alert(1)']) check(!sorter.isAmazon({location: new URL(url)}), 'Reject untrusted origin: ' + url);

    const searchMarkup = `<main id="search"><div class="s-main-slot">${card(1, '4')}${card(2, '500')}</div></main>`;
    for (const homePath of ['/', '/ref=nav_logo', '/gp/aw', '/gp/aw/', '/-/en/']) check(sorter.isSearch(fixture(searchMarkup, 'https://www.amazon.co.jp' + homePath)), 'Strong search DOM permits home transition: ' + homePath);
    check(!sorter.isSearch(fixture(`<div class="s-main-slot">${card(1, '4')}</div>`, 'https://www.amazon.co.jp/')), 'Home recommendations lack search landmark');
    check(!sorter.isSearch(fixture('<main id="search"><div class="s-main-slot"></div></main>', 'https://www.amazon.co.jp/')), 'Empty root cannot imply home search');
    for (const attr of ['hidden', 'style="display:none"', 'aria-hidden="true"']) check(!sorter.isSearch(fixture(`<main id="search" ${attr}><div class="s-main-slot">${card(1, '4')}</div></main>`, 'https://www.amazon.co.jp/')), 'Hidden cached search does not activate: ' + attr);
    for (const marker of ['dp', 'ppd', 'productTitle']) check(!sorter.isSearch(fixture(`<div id="${marker}"></div>` + searchMarkup, 'https://www.amazon.co.jp/')), 'Product marker blocks stale search fallback: ' + marker);
    for (const blockedPath of ['/dp/' + asin(1), '/gp/cart/view.html', '/gp/buy/spc/handlers/display.html', '/ap/signin', '/hz/cart', '/gp/aw/c', '/your-orders/orders', '/gp/your-account/order-history']) {
        check(!sorter.isSearch(fixture(searchMarkup, 'https://www.amazon.co.jp' + blockedPath)), 'Search DOM cannot override product/cart/account path: ' + blockedPath);
    }
    check(!sorter.isSearch(fixture(searchMarkup, 'https://example.com/')), 'Strong structure cannot bypass host restriction');

    const basic = fixture(`<main id="search"><div class="s-main-slot">${card(1, '12')}${card(2, '1,234')}<div class="s-result-item AdHolder" data-component-type="s-search-result" data-asin="${asin(3)}" id="sponsored">広告</div>${card(4, '1.2万')}<div id="pagination">次へ</div></div></main>`);
    const before = basic.document.body.innerHTML;
    const basicGroups = sorter.chooseGroups(basic.document);
    equal(groupIds(basicGroups), [['p1', 'p2', 'p4']], 'Select organic primary cards in existing order');
    equal(basicGroups[0].slots.map(node => node.id), ['p1', 'p2', 'p4'], 'Nonproduct/ad slots excluded');
    equal(basic.document.body.innerHTML, before, 'Pure grouping cannot mutate the document');
    equal(sorter.chooseGroup(basic.document).cards.length, 3, 'Single-group diagnostic identifies selected group');
    const product = fixture(`<div id="dp"><div class="s-main-slot">${card(1, '2')}${card(2, '100')}</div></div>`);
    equal(sorter.chooseGroup(product.document), null, 'Product recommendations are not a result group');
    const nested = fixture(`<div class="s-main-slot">${card(1, '12', `<div class="a-carousel">${card(90, '999,999')}</div>`)}${card(2, '30')}</div>`);
    equal(groupIds(sorter.chooseGroups(nested.document)), [['p1', 'p2']], 'Nested carousel cards never become primary results');
    const nestedCard = fixture(`<div class="s-main-slot">${card(1, '12', card(90, '999,999'))}${card(2, '30')}</div>`);
    equal(groupIds(sorter.chooseGroups(nestedCard.document)), [['p1', 'p2']], 'Nested result-card component is not double-counted');
    const invalid = fixture(`<div class="s-main-slot"><div class="s-result-item" data-asin="bad" id="invalid"></div>${card(1, '12')}</div>`);
    equal(groupIds(sorter.chooseGroups(invalid.document)), [['p1']], 'Invalid ASIN excluded from primary results');
    const mobile = fixture(`<ul id="s-results-list-atf"><li data-asin="${asin(1)}" id="p1"><a href="/product-reviews/${asin(1)}">(90)</a></li><li data-asin="${asin(2)}" id="p2"><a href="/product-reviews/${asin(2)}">1.2K</a></li></ul>`);
    equal(groupIds(sorter.chooseGroups(mobile.document)), [['p1', 'p2']], 'Legacy mobile list cards remain supported');
    equal(sorter.readCount(mobile.document.getElementById('p1')), 90, 'Legacy mobile direct review text');
    equal(sorter.readCount(mobile.document.getElementById('p2')), 1200, 'Legacy mobile abbreviated count');

    const wrapped = fixture(`<div class="s-main-slot"><div id="row">${card(1, '20')}${card(2, '200')}</div></div>`);
    const wrappedGroups = sorter.chooseGroups(wrapped.document);
    equal(groupIds(wrappedGroups), [['p1', 'p2']], 'Grouping wrapper below recognized root is supported');
    equal(wrappedGroups[0].parent.id, 'row', 'Cards retain actual grouping parent');
    const slots = fixture(`<div class="s-main-slot"><div id="slot1">\n<div>${card(1, '20')}</div>\n</div><aside id="ad">固定の広告</aside><section id="slot2">${card(2, '200')}</section></div>`);
    const slotGroups = sorter.chooseGroups(slots.document);
    equal(groupIds(slotGroups), [['p1', 'p2']], 'Single-card wrappers can share a primary group');
    equal(slotGroups[0].slots.map(node => node.id), ['slot1', 'slot2'], 'Safe complete wrapper selected as each movable slot');
    equal(slots.document.getElementById('ad').parentNode.className, 's-main-slot', 'Grouping leaves unrelated slot untouched');
    const unsafeWrap = fixture(`<div class="s-main-slot"><section id="slot1"><h2 id="fixedheading">固定の見出し</h2>${card(1, '20')}</section><section id="slot2">${card(2, '200')}</section></div>`);
    const unsafeGroups = sorter.chooseGroups(unsafeWrap.document);
    equal(unsafeGroups.find(group => group.cards.some(node => node.id === 'p1')).slots.map(node => node.id), ['p1'], 'Wrapper with unrelated heading is never selected for moving');
    equal(unsafeWrap.document.getElementById('fixedheading').parentNode.id, 'slot1', 'Grouping preserves fixed heading ownership');

    const pages = fixture(`<div class="s-result-list" id="outside">${card(91, '90')}${card(92, '100')}</div><main id="search"><div id="s-results-list-atf">${card(11, '9')}${card(12, '10')}</div><div class="s-main-slot">${card(1, '4')}${card(2, '500')}${card(3, '30')}</div></main>`);
    const pageGroups = sorter.chooseGroups(pages.document);
    equal(groupIds(pageGroups), [['p11', 'p12'], ['p1', 'p2', 'p3']], 'All loaded groups inside search landmark are collected');
    check(pageGroups.every(group => group.joinable), 'Compatible unheaded pagination groups are joinable');
    equal(pageGroups[0].shape, pageGroups[1].shape, 'ATF/main semantic container names do not break layout compatibility');
    equal(sorter.chooseGroup(pages.document).cards.map(node => node.id), ['p1', 'p2', 'p3'], 'Largest-group diagnostic retains compatibility');
    const nestedRoots = fixture(`<main id="search"><div class="s-main-slot"><div class="s-result-list">${card(1, '2')}${card(2, '1')}</div></div></main>`);
    equal(groupIds(sorter.chooseGroups(nestedRoots.document)), [['p1', 'p2']], 'Nested recognized root is counted only once');
    const headed = fixture(`<main id="search"><div class="s-result-list"><h2>通常の商品</h2>${card(1, '200')}${card(2, '100')}</div><div class="s-result-list"><h2>関連する商品</h2>${card(3, '9000')}${card(4, '8000')}</div></main>`);
    check(sorter.chooseGroups(headed.document).every(group => !group.joinable), 'Distinct section headings prevent cross-section exchange');
    const canonicalHeading = fixture(`<div class="s-main-slot"><h2>検索結果</h2>${card(1, '200')}${card(2, '100')}</div>`);
    check(sorter.chooseGroups(canonicalHeading.document)[0].joinable, 'Canonical search-results heading does not split one list');
    const shapes = fixture(`<main id="search"><div class="s-result-list list-layout">${card(1, '200')}${card(2, '100')}</div><div class="s-result-list grid-layout">${card(3, '9000')}${card(4, '8000')}</div></main>`);
    const shapeGroups = sorter.chooseGroups(shapes.document);
    check(shapeGroups[0].shape !== shapeGroups[1].shape, 'Different presentation classes remain incompatible');
    const hiddenRoot = fixture(`<main id="search"><div class="s-result-list" hidden>${card(91, '900')}</div><div class="s-main-slot">${card(1, '2')}${card(2, '1')}</div></main>`);
    equal(groupIds(sorter.chooseGroups(hiddenRoot.document)), [['p1', 'p2']], 'Hidden cached root excluded from extraction');
    const hiddenCard = fixture(`<div class="s-main-slot">${rawCard(91, '', 'hidden')}${card(1, '2')}${card(2, '1')}</div>`);
    equal(groupIds(sorter.chooseGroups(hiddenCard.document)), [['p1', 'p2']], 'Hidden cached card excluded from extraction');

    return assertions;
}

module.exports = {run};
if (require.main === module) {
    run().then(assertions => console.log('PASS ' + JSON.stringify({status:'passed', assertions, suite:'pure_extractors', dom:'linkedom 0.18.12', runtime_installed:false, real_browser_layout_tested:false}))).catch(error => {console.error(error); process.exitCode = 1;});
}
