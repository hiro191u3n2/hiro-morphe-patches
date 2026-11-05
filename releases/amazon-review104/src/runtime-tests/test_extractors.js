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

    // v104 extraction regressions are synthetic source-level fixtures, not a
    // claim that these fragments were captured from the user's current device.
    for (const [text, count] of [
        ['12 345 ratings', 12345], ['12\u00a0345 reviews', 12345], ['12\u202f345 ratings', 12345],
        ['1,234 customer reviews', 1234], ['4.5 out of 5 stars. 1,234 customer reviews', 1234],
        ['1.2K+ ratings', 1200], ['1,234+ ratings', 1234], ['1.2万+個の評価', 12000]
    ]) equal(sorter.parseCount(text, false), count, 'Named grouping/abbreviation: ' + text);
    equal(sorter.parseCount('5 1,234 ratings', false), 1234, 'Leading separate star score is not joined with grouped count');
    equal(sorter.parseCount('5つ星のうち4 123件の評価', false), 123, 'Explicit Japanese integer-star prefix cannot become a count digit group');
    equal(sorter.parseCount('5つ星のうち4 12 345件の評価', false), 12345, 'Explicit integer-star prefix preserves a spaced full count');
    equal(sorter.parseCount('4.5 out of 5 stars', false), -1, 'Named grammar still rejects stars alone');
    equal(sorter.parseCount('1K+ bought in past month', true), -1, 'Approximate sales remain unrelated to ratings');
    for (const score of ['4', '4.5']) {
        equal(read(`<div data-cy="reviews-block"><i class="a-icon-star"><span class="a-icon-alt">5つ星のうち${score}</span></i><span>1234件の評価</span></div>`), 1234, 'Adjacent star and named count cannot concatenate: ' + score);
        equal(read(`<div data-cy="reviews-block"><span>5つ星のうち${score}</span><span>123件の評価</span></div>`), 123, 'Read named leaf before ambiguous aggregate spacing: ' + score);
    }
    equal(read('<div data-cy="reviews-block"><i class="a-icon-star"><span class="a-icon-alt">5つ星のうち4</span></i>1234件の評価</div>'), 1234, 'Direct count text excludes star-icon subtree');
    for (const wrapper of ['data-cy="reviews-block"', 'class="a-row"']) {
        equal(read(`<div ${wrapper}><span class="a-size-small">4.5</span><i class="a-icon-star"></i><span class="a-size-small">(1,234)</span></div>`), 1234, 'Plain parenthesized count with review context: ' + wrapper);
    }
    equal(read('<div class="a-row"><i class="a-icon-star" aria-hidden="true"></i><span>(1,234)</span></div>'), 1234, 'Decorative star icon still supplies review-row context');
    equal(read('<span class="a-size-small">(1,234)</span>'), -1, 'Parentheses without review context remain insufficient');
    equal(read(`<div class="a-row"><i class="a-icon-star"></i><a href="/dp/${asin(1)}"><span>(1984)</span></a></div>`), -1, 'Parenthesized numeric product link is not a review count');
    equal(read('<a href="#customerReviews" aria-label="4.5 out of 5 stars"><span class="a-size-small">4.5</span><i class="a-icon-star"></i><span class="a-size-base">1,234</span></a>'), 1234, 'Shared review hyperlink accepts unparenthesized count above star range');
    equal(read('<a href="#customerReviews" aria-label="4 out of 5 stars"><span class="a-size-base">4</span><i class="a-icon-star"></i></a>'), -1, 'Shared link integer star alone stays unknown');
    equal(read('<a href="#customerReviews" aria-label="4 out of 5 stars"><span class="a-size-base">4</span><i class="a-icon-star"></i><span class="a-size-base">(3)</span></a>'), 3, 'Parenthesized small count in shared link remains valid');
    for (const attr of ['hidden', 'style="display:none"', 'style="visibility:hidden"']) {
        equal(read(`<span ${attr} aria-label="999,999 ratings"></span><a href="#customerReviews"><span class="s-underline-text">1,234</span></a>`), 1234, 'Hidden label cannot override visible count: ' + attr);
        equal(read(`<div ${attr}><a href="#customerReviews"><span class="s-underline-text">999,999</span></a></div><a href="#customerReviews"><span class="s-underline-text">1,234</span></a>`), 1234, 'Hidden compact ancestor cannot override visible count: ' + attr);
    }
    equal(read('<div data-cy="reviews-block"><span class="s-underline-text" aria-hidden="true">1,234</span></div>'), 1234, 'aria-hidden alone does not hide a visually displayed count');
    equal(read('<span aria-label="12,345 ratings"></span><div data-cy="reviews-block"><span class="s-underline-text" aria-hidden="true">1.2万</span></div>'), 12345, 'Exact accessible count takes priority over visible rounded accessibility duplicate');
    equal(read('<div data-cy="reviews-block"><span hidden>999999 ratings</span> <span>1234 ratings</span></div>'), 1234, 'Aggregate fallback ignores hidden named text');
    equal(read('<div data-cy="reviews-block"><span hidden>999999 ratings</span> 1234 ratings</div>'), 1234, 'Hidden descendant excluded when count is direct block text');
    equal(read('<div data-cy="reviews-block"><span class="a-price">999999 ratings</span> 1234 ratings</div>'), 1234, 'Aggregate fallback ignores price subtree');
    equal(read('<div data-cy="reviews-block"><span class="a-icon-alt">999999 ratings</span> 1234 ratings</div>'), 1234, 'Aggregate fallback ignores star subtree even with misleading count wording');
    equal(read(`<div data-cy="reviews-block"><div data-asin="${asin(90)}"><h2>別の商品</h2><span>999999 ratings</span></div>1234 ratings</div>`), 1234, 'Foreign nested product text excluded while own direct count remains readable');
    equal(read('<div data-cy="reviews-block"><div class="a-carousel"><span>999999 ratings</span></div>1234 ratings</div>'), 1234, 'Carousel text excluded from aggregate fallback');
    equal(read('<span aria-label="1.2K ratings"></span><span aria-label="1,234 ratings"></span>'), 1234, 'Exact accessible label wins over earlier rounded accessible label');
    equal(read('<span aria-label="1.2K ratings"></span><a href="#customerReviews"><span class="s-underline-text">1,234</span></a>'), 1234, 'Exact dedicated count wins over rounded label');
    equal(read('<span aria-label="1,234 ratings"></span><a href="#customerReviews"><span class="s-underline-text">1.2K</span></a>'), 1234, 'Exact accessible label wins over rounded visible count');
    equal(read('<span aria-label="1,234+ ratings"></span><span data-review-count="1,237"></span>'), 1237, 'Exact count attribute wins over approximate plus-label');
    equal(read('<div data-cy="reviews-block"><span class="s-underline-text">1.2K</span><span>1,234 ratings</span></div>'), 1234, 'Exact named leaf wins over rounded compact count');
    equal(read(`<div data-cy="reviews-block"><div data-asin="${asin(1)}"><span>1,234 ratings</span></div></div>`), 1234, 'Same-ASIN generic metadata permits named text fallback');
    equal(read(`<div class="s-result-item" data-asin="${asin(1)}"><span aria-label="999999 ratings"></span></div><a href="#customerReviews"><span class="s-underline-text">1,234</span></a>`), 1234, 'A nested result component cannot override outer count even with the same ASIN');
    equal(read(`<div class="s-result-item" data-asin="${asin(90)}"><span aria-label="999999 ratings"></span></div><span aria-label="1234 ratings"></span>`), 1234, 'Nested foreign card label still rejected');
    equal(read('<div data-component-type="s-search-result"><span aria-label="999999 ratings"></span></div>'), -1, 'Nested result with missing identity cannot inherit outer product');
    equal(read(`<div data-asin="${asin(90)}"><div class="s-result-item" data-asin="${asin(1)}"><span aria-label="999999 ratings"></span></div></div>`), -1, 'Foreign intermediate product cannot be bypassed by matching inner ASIN');
    equal(read(`<a href="/dp/${asin(1)}" aria-label="1984 ratings"><h2>1984</h2></a>`), -1, 'Label on product-title wrapper is not a rating count');

    for (const className of ['css-none', 'css-invisible']) {
        const css = fixture(rawCard(1, `<div class="${className}"><span aria-label="999999 ratings"></span></div><a href="#customerReviews"><span class="s-underline-text">1,234</span></a>`));
        const view = css.document.defaultView, oldStyle = view.getComputedStyle;
        try {
            view.getComputedStyle = node => ({display: node.classList.contains('css-none') ? 'none' : 'block', visibility: node.classList.contains('css-invisible') ? 'hidden' : 'visible'});
            equal(sorter.readCount(css.document.getElementById('p1')), 1234, 'Computed CSS hidden ancestor excluded: ' + className);
        } finally { if (oldStyle) view.getComputedStyle = oldStyle; else delete view.getComputedStyle; }
    }
    equal(read('<span class="aok-offscreen" aria-label="12,345 ratings"></span><a href="#customerReviews">1.2万</a>'), 12345, 'Offscreen accessible exact count remains usable');

    equal(read('<div data-cy="reviews-block"><span>4</span><span>123</span><span>件の評価</span></div>'), 123, 'Separate star/count/unit nodes preserve numeric token boundaries');
    equal(read('<div data-cy="reviews-block"><span>4.5</span><span>1234</span><span>ratings</span></div>'), 1234, 'Separate decimal-star/count/unit nodes preserve numeric token boundaries');
    for (const countClass of ['a-size-base', 's-underline-text']) {
        equal(read(`<a href="#customerReviews"><span class="${countClass}">4</span><svg aria-label="4 out of 5 stars" role="img"></svg><span class="${countClass}">120</span></a>`), 120, 'Accessible SVG star does not expose integer score as count: ' + countClass);
    }
    equal(read('<a href="#customerReviews"><span class="a-size-base">4</span><svg aria-label="5つ星のうち4" role="img"></svg></a>'), -1, 'Japanese accessible SVG star alone stays unknown');

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
    const shapeView = shapes.document.defaultView, priorStyle = shapeView.getComputedStyle;
    try {
        shapeView.getComputedStyle = node => ({display: node.classList.contains('grid-layout') ? 'grid' : 'block', flexDirection: 'row', gridTemplateColumns: node.classList.contains('grid-layout') ? '100px 100px' : 'none'});
        const shapeGroups = sorter.chooseGroups(shapes.document);
        check(shapeGroups[0].shape !== shapeGroups[1].shape, 'Actual block versus grid parent flow remains incompatible');
    } finally { if (priorStyle) shapeView.getComputedStyle = priorStyle; else delete shapeView.getComputedStyle; }
    const cosmetic = fixture(`<main id="search"><div class="s-result-list first-page cosmetic-a">${card(1, '200')}${card(2, '100')}</div><div class="s-result-list next-page cosmetic-b"><div class="neutral"><section>${card(3, '9000')}</section><aside hidden>非表示の補助情報</aside></div><div class="neutral">${card(4, '8000')}</div></div></main>`);
    const cosmeticGroups = sorter.chooseGroups(cosmetic.document);
    equal(cosmeticGroups.length, 2, 'Cosmetic classes and safe wrapper depth retain both result groups');
    equal(cosmeticGroups[0].shape, cosmeticGroups[1].shape, 'Cosmetic classes and neutral single-card wrappers share the same parent flow');
    check(cosmeticGroups.every(group => group.joinable), 'Hidden inert sibling does not partition result groups');
    equal(cosmeticGroups[1].slots.map(node => node.className), ['neutral', 'neutral'], 'Safe wrappers include hidden inert siblings without splitting the list');
    const hiddenSibling = fixture(`<main id="search"><div class="s-result-list"><div id="row-with-hidden">${card(1, '2')}${card(2, '1')}<aside hidden><h2>非表示の旧見出し</h2></aside></div></div><div class="s-result-list">${card(3, '4')}${card(4, '3')}</div></main>`);
    const hiddenSiblingGroups = sorter.chooseGroups(hiddenSibling.document);
    check(hiddenSiblingGroups.every(group => group.joinable), 'Hidden stale heading within a row does not create a distinct visible section');
    equal(hiddenSiblingGroups[0].shape, hiddenSiblingGroups[1].shape, 'Nested row with only hidden extra content matches the same block flow');
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
