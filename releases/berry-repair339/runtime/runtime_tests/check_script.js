const fs = require('fs');
const vm = require('vm');
const path = require('path');
const dir = process.argv[2];
const script = fs.readFileSync(path.join(dir, 'document_start.js'), 'utf8');
const probe = fs.readFileSync(path.join(dir, 'probe.js'), 'utf8');
let assertions = 0;
const URL = 'https://example.test/list';
function check(value, message) { assertions++; if (!value) throw Error(message); }
function page(main = true) {
  const handlers = {}, messages = [], options = {};
  const w = { location: {href: URL}, addEventListener(name, callback, capture) { (handlers[name] ||= []).push(callback); options[name]=capture; },
    hiroBerryBFCacheRestoreV1: { postMessage(value) { messages.push(value); } } };
  w.top = main ? w : {};
  const context = vm.createContext({window: w});
  return {w, handlers, messages, context, options,
    load() { vm.runInContext(script, context); },
    event(name, persisted) { for (const callback of handlers[name] || []) callback({persisted}); },
    probe() { return vm.runInContext(probe, context); }};
}
const fields = p => p.probe().split('|');
const p = page(); p.load(); p.load();
check(p.handlers.pageshow.length === 1 && p.handlers.pagehide.length === 1, 'single listener registration');
check(!Object.keys(p.w).includes('__hiroBerryBFCacheRestoreV1'), 'marker is non-enumerable');
check(p.options.pageshow === true, 'restore URL captured before later bubble handlers');
p.event('pageshow', false);
check(p.messages.length === 0 && fields(p)[2] === '0' && fields(p)[3] === '0' && fields(p)[4] === '', 'ordinary load is not a restore');
p.event('pagehide', true); p.event('pageshow', true);
check(p.messages.length === 1 && p.messages[0] === 'hiro-bfcache-restored-v1', 'restored page wakes native listener');
const first = p.probe();
check(/^B2\|[a-z0-9]{8,64}\|1\|1\|https%3A/.test(first), 'probe has B2 marker format');
check(fields(p)[4] === encodeURIComponent(URL) && fields(p)[5] === encodeURIComponent(URL), 'restore and current URLs initially agree');
check(p.probe() === first, 'probe does not consume or mutate restore marker');
p.w.location.href = URL + '?name=日本語|+"#part';
check(fields(p)[4] === encodeURIComponent(URL) && decodeURIComponent(fields(p)[5]) === p.w.location.href, 'replaceState changes current URL without changing restore anchor');
check(fields(p).length === 6, 'URL delimiters and Unicode cannot add record fields');
p.event('pagehide', true); check(fields(p)[2] === '1' && fields(p)[3] === '0' && fields(p)[4] === '', 'leaving page clears pending and restore URL');
p.event('pageshow', true);
check(fields(p)[2] === '2' && fields(p)[3] === '1' && p.messages.length === 2, 'subsequent restore advances serial');
check(decodeURIComponent(fields(p)[4]) === p.w.location.href, 'subsequent restore captures its own URL');
p.event('pageshow', false); check(fields(p)[2] === '2' && fields(p)[3] === '0' && fields(p)[4] === '', 'ordinary pageshow clears pending');
const sub = page(false); sub.load();
check(Object.keys(sub.handlers).length === 0 && sub.probe() === null, 'subframe has no listener or marker');
const q = page(); q.load(); q.event('pageshow', true);
check(q.probe().split('|')[1] !== first.split('|')[1], 'different document has a different identity');
const fail = page(); fail.w.hiroBerryBFCacheRestoreV1.postMessage = () => { throw Error('bridge unavailable'); };
fail.load(); fail.event('pageshow', true);
check(fields(fail)[3] === '1' && decodeURIComponent(fields(fail)[4]) === URL, 'bridge failure retains marker for commit-side query');
const huge = page(); huge.load(); huge.event('pageshow', true); huge.w.location.href = 'x'.repeat(8193);
check(huge.probe() === null, 'overlong current URL is rejected');
huge.event('pageshow',true); huge.w.location.href=URL;
check(huge.probe() === null, 'overlong stored restore URL is rejected');
console.log(JSON.stringify({result:'PASS', assertions,
  scope:'Node event harness for the bundled document-start script; no browser or Android rendering test'}));
