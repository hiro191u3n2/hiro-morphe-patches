const fs = require('fs');
const vm = require('vm');
const path = require('path');
const dir = process.argv[2];
const script = fs.readFileSync(path.join(dir, 'document_start.js'), 'utf8');
const probe = fs.readFileSync(path.join(dir, 'probe.js'), 'utf8');
let assertions = 0;
function check(value, message) { assertions++; if (!value) throw Error(message); }
function page(main = true) {
  const handlers = {}, messages = [];
  const w = { addEventListener(name, callback) { (handlers[name] ||= []).push(callback); },
    hiroBerryBFCacheRestoreV1: { postMessage(value) { messages.push(value); } } };
  w.top = main ? w : {};
  const context = vm.createContext({window: w});
  return {w, handlers, messages, context,
    load() { vm.runInContext(script, context); },
    event(name, persisted) { for (const callback of handlers[name] || []) callback({persisted}); },
    probe() { return vm.runInContext(probe, context); }};
}
const p = page(); p.load(); p.load();
check(p.handlers.pageshow.length === 1 && p.handlers.pagehide.length === 1, 'single listener registration');
check(!Object.keys(p.w).includes('__hiroBerryBFCacheRestoreV1'), 'marker is non-enumerable');
p.event('pageshow', false);
check(p.messages.length === 0 && /\|0\|0$/.test(p.probe()), 'ordinary load is not a restore');
p.event('pagehide', true); p.event('pageshow', true);
check(p.messages.length === 1 && p.messages[0] === 'hiro-bfcache-restored-v1', 'restored page wakes native listener');
const first = p.probe();
check(/^B1\|[a-z0-9]{8,64}\|1\|1$/.test(first), 'probe has strict ASCII marker format');
check(p.probe() === first, 'probe does not consume or mutate restore marker');
p.event('pagehide', true); check(/\|1\|0$/.test(p.probe()), 'leaving page clears pending');
p.event('pageshow', true);
check(/\|2\|1$/.test(p.probe()) && p.messages.length === 2, 'subsequent restore advances serial');
p.event('pageshow', false); check(/\|2\|0$/.test(p.probe()), 'ordinary pageshow clears pending');
const sub = page(false); sub.load();
check(Object.keys(sub.handlers).length === 0 && sub.probe() === null, 'subframe has no listener or marker');
const q = page(); q.load(); q.event('pageshow', true);
check(q.probe().split('|')[1] !== first.split('|')[1], 'different document has a different identity');
const fail = page(); fail.w.hiroBerryBFCacheRestoreV1.postMessage = () => { throw Error('bridge unavailable'); };
fail.load(); fail.event('pageshow', true);
check(/\|1\|1$/.test(fail.probe()), 'bridge failure retains marker for commit-side query');
console.log(JSON.stringify({result:'PASS', assertions,
  scope:'Node event harness for the bundled document-start script; no browser or Android rendering test'}));
