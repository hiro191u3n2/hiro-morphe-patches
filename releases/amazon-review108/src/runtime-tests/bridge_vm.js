'use strict';
// Execute the exact script captured from Java, using one browser-like global.
// This tests the bridge contract; linkedom and synthetic geometry are not an Android WebView.
const assert = require('node:assert/strict');
const vm = require('node:vm');
const readline = require('node:readline');
const {parseHTML} = require('linkedom');

const scenario = process.argv[2];
assert.ok(['normal', 'home-no-search', 'unknown-no-search', 'cart', 'product', 'js-error', 'native-queryless', 'native-tracking', 'native-cached-return'].includes(scenario), 'Known bridge scenario');
const forbidden = scenario === 'cart' || scenario === 'product';
const nativeScenario = scenario.startsWith('native-');
const noSearchLandmark = nativeScenario || scenario === 'home-no-search' || scenario === 'unknown-no-search';
const secret = 'PRIVATE_TEST_NEVER_EXPORT';
const cards = [34, 12000].map((count, index) =>
    `<article class="s-result-item" data-component-type="s-search-result" data-asin="B00000000${index + 1}"><h2>${secret} product</h2><div data-cy="reviews-block"><span class="s-underline-text">(${count})</span></div></article>`).join('');
const content = `<div class="s-main-slot">${cards}</div>`;
const {document, window: dom} = parseHTML(`<html><head></head><body>${noSearchLandmark ? content : `<main id="search">${content}</main>`}</body></html>`);
const paths = {'native-queryless': '/gp/homepage.html', 'native-tracking': '/gp/homepage.html', 'native-cached-return': '/gp/homepage.html', 'home-no-search': '/gp/homepage.html', 'unknown-no-search': '/mobile/entry/', cart: '/gp/cart/view.html', product: '/dp/B000000001'};
const listeners = new Map(), timers = new Map();
let timerId = 0, clock = 10000, executions = 0;
const win = {
    document,
    location: new URL('https://www.amazon.co.jp' + (paths[scenario] || `/s?k=${secret}`)),
    URL, URLSearchParams,
    MutationObserver: scenario === 'js-error' ? function () { throw new TypeError('PRIVATE_EXCEPTION_NEVER_EXPORT'); } : dom.MutationObserver,
    performance: {now: () => clock, getEntriesByType: () => []},
    innerHeight: 480, innerWidth: 360, scrollX: 0, scrollY: 240,
    addEventListener(type, callback) { if (!listeners.has(type)) listeners.set(type, new Set()); listeners.get(type).add(callback); },
    removeEventListener(type, callback) { listeners.get(type)?.delete(callback); },
    setTimeout(callback, delay = 0) { const id = ++timerId; timers.set(id, {callback, due: clock + delay}); return id; },
    clearTimeout(id) { timers.delete(id); },
    getComputedStyle(node) {
        return {display: node.style.display || 'block', visibility: node.style.visibility || 'visible',
            position: node.style.position || 'static', overflowY: 'visible', flexDirection: 'row',
            gridTemplateColumns: 'none', order: '0', direction: 'ltr'};
    },
    scrollTo() { throw new Error('The immediate receipt must not begin scrolling in this scrolled fixture'); }
};
win.window = win; win.self = win; win.globalThis = win;
win.history = {pushState() {}, replaceState() {}};
Object.defineProperty(document, 'scrollingElement', {configurable: true, value: document.documentElement});
const context = vm.createContext(win);
// V8 exposes a contextified Window proxy to code evaluated in the context.
// Bind the DOM back to that exact object, rather than the outside sandbox object.
Object.defineProperty(document, 'defaultView', {configurable: true, value: vm.runInContext('globalThis', context)});
assert.equal(vm.runInContext('window === globalThis && self === window && document.defaultView === window', context), true, 'One Window global');
function execute(script, elapsed) {
assert.ok(script.length > 0 && script.length < 256000, 'Actual bounded Java-generated script');
if (!executions++) assert.ok(script.length > 1000, 'Initial evaluation contains actual runtime payload');
assert.ok(Number.isFinite(elapsed) && elapsed >= 0 && elapsed <= 60000);
if (scenario === 'native-tracking' && executions === 2) win.location = new URL('https://www.amazon.co.jp/gp/homepage.html?ref=tracking#fragment');
const until = clock + elapsed;
let steps = 0;
while (timers.size) {
    const next = [...timers.entries()].sort((a, b) => a[1].due - b[1].due)[0];
    if (next[1].due > until) break;
    assert.ok(++steps <= 200, 'Bounded JavaScript timer progression');
    timers.delete(next[0]); clock = next[1].due; next[1].callback();
}
clock = until;
if (scenario === 'native-cached-return' && executions === 4) {
    const before = [...document.querySelectorAll('.s-underline-text')].map(node => Number(node.textContent.replace(/[()]/g, '')));
    assert.deepEqual(before, [34,12000], 'Revoked same-URL home recommendations remain untouched before native onShown');
    assert.equal(document.getElementById('hiro-review-count-status'), null);
}
const result = vm.runInContext(script, context, {timeout: 3000});
assert.equal(typeof result, 'string', 'Native wrapper returns JSON.stringify receipt');
assert.ok(result.length <= 4096, 'Bounded receipt');
assert.ok(!result.includes(secret) && !result.includes('PRIVATE_EXCEPTION') && !result.includes('https:') && !result.includes('B00000000'), 'No page identity or exception message in receipt');
const receipt = JSON.parse(result);
assert.equal(receipt.v, '1.0.8');
assert.ok(['search', 'home', 'other'].includes(receipt.route));
if (scenario === 'js-error') {
    assert.equal(receipt.stage, 'js-error');
    assert.equal(receipt.error, 'TypeError');
} else {
    assert.equal(receipt.stage, 'ack');
    assert.equal(receipt.report.version, '1.0.8');
    assert.equal(receipt.report.cards, forbidden ? 0 : 2);
    assert.equal(receipt.report.known, forbidden ? 0 : 2);
    assert.equal(receipt.report.roots, forbidden ? 0 : 1);
    for (const key of ['cards', 'known', 'roots']) assert.ok(Number.isInteger(receipt.report[key]) && receipt.report[key] >= 0 && receipt.report[key] <= 100);
    assert.equal(typeof receipt.report.status, 'boolean');
    assert.equal(typeof receipt.report.limited, 'boolean');
    assert.equal(receipt.report.error, null);
    const order = [...document.querySelectorAll('.s-main-slot article')].map(node => Number(node.querySelector('.s-underline-text').textContent.replace(/[()]/g, '')));
    if (forbidden) {
        assert.equal(receipt.report.phase, 'inactive');
        assert.equal(receipt.report.status, false);
        assert.deepEqual(order, [34, 12000], 'Stale canonical search DOM on forbidden routes remains untouched');
    } else if (scenario === 'native-cached-return' && executions === 3) {
        assert.equal(receipt.report.phase, 'inactive', 'Actual native onHidden revokes the persistent runtime');
        assert.equal(receipt.report.status, false);
        // Same document now contains an unrelated home cohort. It must stay in
        // original order until the actual native onShown explicitly resumes.
        document.querySelector('.s-main-slot').innerHTML = cards;
    } else if (scenario === 'native-cached-return' && executions === 4) {
        assert.equal(receipt.report.phase, 'waiting', 'Actual onShown queues cached-document reactivation');
        assert.deepEqual(order, [34,12000]);
    } else if (executions >= 2) {
        assert.equal(receipt.report.phase, 'frozen', 'Scheduled JavaScript actually reaches sorted state before follow-up native hook');
        assert.equal(receipt.report.status, true);
        assert.deepEqual(order, [12000, 34], 'Actual injected runtime sorts the existing product nodes');
    }
}
// Chromium serializes the JS return value as JSON before calling ValueCallback<String>.
// Since the wrapper returned a string, the native callback receives an outer JSON string.
return JSON.stringify(result);
}
// One process and one Window persist through every native retry/checkpoint in a case.
// Wire input is elapsed milliseconds, a tab, and base64 of the exact captured script.
const input = readline.createInterface({input: process.stdin, crlfDelay: Infinity});
input.on('line', line => {
    try {
        const separator = line.indexOf('\t');
        assert.ok(separator > 0, 'Bridge wire request');
        const elapsed = Number(line.slice(0, separator));
        const script = Buffer.from(line.slice(separator + 1), 'base64').toString('utf8');
        process.stdout.write(execute(script, elapsed) + '\n');
    } catch (error) {
        process.stderr.write(String(error.stack || error) + '\n');
        process.exitCode = 1; input.close();
    }
});
