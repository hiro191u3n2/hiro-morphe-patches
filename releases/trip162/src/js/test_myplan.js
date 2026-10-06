'use strict';
// Executes the exact indexed modules. Native Text/layout, localization and
// unrelated app widgets are explicit host stubs; this is not an Android render.
const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const crypto = require('crypto');

const [baselinePath, patchedPath, reportPath] = process.argv.slice(2);
assert(baselinePath && patchedPath, 'Usage: node test_myplan.js baseline.jsbundle patched.jsbundle [report.json]');
const sha = raw => crypto.createHash('sha256').update(raw).digest('hex');
const baseline = fs.readFileSync(baselinePath), patched = fs.readFileSync(patchedPath);
assert.equal(sha(baseline), 'fde4c6689131987b3f0b355ee7c0f836f93757c7cffa6632debc3b3046c5fba0');

function splitBundle(buffer) {
  const tail = /\n(\d{10}),(\d{10}),(\d{10}),(\d{10})$/.exec(buffer.toString('latin1'));
  assert(tail);
  const [moduleOffset, moduleSize, resourceOffset, resourceSize] = tail.slice(1).map(Number);
  const index = buffer.subarray(moduleOffset, moduleOffset + moduleSize).toString();
  const modules = new Map([...index.matchAll(/M(\d+)A,(\d+),(\d+);/g)]
    .map(m => [+m[1], buffer.subarray(+m[2], +m[2] + +m[3]).toString()]));
  const offsets = [...buffer.subarray(resourceOffset, resourceOffset + resourceSize).toString()
    .matchAll(/,(\d+),\d+;/g)].map(m => +m[1]);
  new vm.Script(buffer.subarray(0, Math.min(...offsets)).toString(), {filename: 'all-crn-modules.js'});
  return modules;
}
const beforeModules = splitBundle(baseline), afterModules = splitBundle(patched);
assert.equal(beforeModules.size, 395);
assert.equal(afterModules.size, beforeModules.size);
const changed = [...afterModules].filter(([id, code]) => code !== beforeModules.get(id)).map(([id]) => id).sort();
assert.deepEqual(changed, [666761, 667189]);
for (const id of [666817, 666790, 666755, 666756, 666726, 666788, 666762, 667264])
  assert.equal(afterModules.get(id), beforeModules.get(id), `Unrelated renderer ${id} changed`);

const jsx = (type, props, key) => ({type, props: props || {}, key});
const JSX = {jsx, jsxs: jsx, Fragment: 'Fragment'};
const noop = () => {};
const React = {Fragment: 'Fragment', memo: f => f, useMemo: f => f(),
  useState: value => [value, noop], useRef: current => ({current}), useEffect: noop,
  useCallback: f => f, forwardRef: f => props => f(props, null)};
const wrapped = value => ({__esModule: true, default: value});
const babel = value => value && value.__esModule ? value : {default: value};
const enumProxy = new Proxy({}, {get: (_, key) => key});

function environment(modules) {
  const state = {theme: 'light', rtl: false, fontScale: 1, a11y: false,
    dateStrings: new Map(), dateCalls: [], durationCalls: [], imageLoads: 0,
    cmtFetches: 0, urlCalls: [], telemetry: [], durationPrefix: ''};
  class L10nDateFormatter {
    constructor(seconds, offset) { this.seconds = seconds; state.dateCalls.push([seconds, offset]); }
    hmString() { return state.dateStrings.get(this.seconds * 1000) || ''; }
    getStandardTimeString() { return `fixture-date-${this.seconds}`; }
  }
  class L10nDurationFormatter {
    constructor(seconds) { this.seconds = seconds; state.durationCalls.push(seconds); }
    convertToString() {
      return state.durationPrefix + `${Math.floor(this.seconds / 3600)}時間${Math.floor(this.seconds % 3600 / 60)}分`;
    }
  }
  const NativeKit = {L10nDateFormatter, L10nDurationFormatter};
  const theme = {useThemeColor: () => ({theme: state.theme, getIBUColor: (color, alpha) => color})};
  const styles = {IBUFont: {getFontStyle: size => ({
    fontSize: size === 'SubHead_20' ? 20 : size === 'Body_15' ? 15 : 12,
  })}};
  const Image = props => { state.imageLoads++; return jsx('Image', props); };
  const AdaptiveText = props => jsx('AdaptiveText', props);
  const stylesProxy = new Proxy({}, {get: (_, name) => String(name)});
  const mocks = new Map([
    [1, babel], [12, {...React, ...wrapped(React)}], [14, {Dimensions: {addEventListener: noop}}],
    [18, value => Array.from(value)], [42, value => value],
    [131, (object, keys) => Object.fromEntries(Object.entries(object).filter(([key]) => !keys.includes(key)))],
    [230, JSX], [376, () => noop],
    [333353, {CRNText: 'CRNText', ...styles}],
    [666692, wrapped('View')], [666693, wrapped('View')],
    [666698, wrapped({isAndroid: () => true})], [666703, wrapped({isRTL: () => state.rtl})],
    [666717, wrapped({})], [666718, () => ({})], [666719, value => value],
    [666720, () => stylesProxy], [666716, (...names) => names.filter(Boolean).join(' ')],
    [666721, theme], [666722, {scheduleHeightManager: {updateHeight: noop}}],
    [666724, {createExpV2: (key, data) => ({key, data}), sendUBT: (...args) => state.telemetry.push(args),
      UBT_KEYS_CLK: enumProxy, UBT_KEYS_EXP: enumProxy}],
    [666725, {MODULE_TYPE: 'fixture', checkBookingType: () => 'flight', UBT_KEYS_CLK: enumProxy, UBT_KEYS_EXP: enumProxy}],
    [666727, NativeKit], [666728, {}], [666729, {}],
    [667300, {checkIsCustomSchedule: () => false, checkIsNeedShowCountDown: () => false, RTLStyle: {}}],
    [666738, {tools: {openURL: url => state.urlCalls.push(url)}}],
    [666742, {setA11yProps: value => value}], [666754, wrapped(AdaptiveText)],
    [666757, {IBUFontSizeEnum: enumProxy, IBUColorEnum: enumProxy, IBUFontWeightEnum: enumProxy}],
    [666758, (...items) => Object.assign({}, ...items)],
    [666759, {getShark: (key, args) => `${key}${args ? ':' + args.join(',') : ''}`, SHARK_KEYS: enumProxy}],
    [666763, wrapped({})], [666769, wrapped(props => jsx('ReservationCard',
      {...props, children: [props.extra, props.noticeInfo, props.children]}))],
    [666773, wrapped(Image)], [666775, wrapped('Icon')], [666786, wrapped({})],
    [666783, {useAccessibility: () => ({isA11yMode: state.a11y, fontScale: state.fontScale})}],
    [666785, wrapped('Gradient')], [666794, value => value], [666797, wrapped(() => null)],
    [666818, wrapped(() => null)], [666828, wrapped(Image)], [666833, wrapped('RelatedRoutes')],
    [667097, 'fixture-airline-image'], [667190, 'fixture-city-placeholder'],
    [637, {IBUCMTModel: {getCMTDataV2() { state.cmtFetches++; }}}],
  ]);
  for (const id of [666764, 666820, 666825, 666826, 666784, 666827, 666831, 666832, 667231, 667379])
    mocks.set(id, wrapped(props => jsx('OtherReservation', props)));
  const selected = new Set([666761, 666762, 666755, 666756, 666726, 666790, 666788, 666817, 667189, 667264]);
  const cache = new Map();
  function load(id) {
    if (mocks.has(id)) return mocks.get(id);
    if (cache.has(id)) return cache.get(id);
    assert(selected.has(id), `Unexpected module dependency: ${id}`);
    let definition;
    vm.runInNewContext(modules.get(id), {__d: (factory, number, deps) => {
      assert.equal(number, id); definition = {factory, deps};
    }, __i: noop}, {filename: `${id}.js`});
    assert(definition);
    const m = {exports: {}};
    cache.set(id, m.exports);
    definition.factory({}, load, load, load, m, m.exports, definition.deps);
    cache.set(id, m.exports);
    return m.exports;
  }
  mocks.set(666787, {get Module12Hour() { return load(666790).default; },
    get CrossDayBadge() { return load(666788).default; },
    get getAcrossDays() { return load(666788).getAcrossDays; },
    EstimatedTime: () => { throw new Error('Old center-arrow duration renderer was invoked'); },
    TextCarousel: () => jsx('TextCarousel', {}),
    handleDeeplink: url => state.urlCalls.push(url)});
  return {state, load};
}

function expand(node) {
  if (Array.isArray(node)) return node.flatMap(x => {
    const value = expand(x);
    return value == null ? [] : Array.isArray(value) ? value : [value];
  });
  if (node == null || node === false) return null;
  if (typeof node !== 'object') return node;
  if (typeof node.type === 'function') return expand(node.type(node.props));
  return {...node, props: {...node.props, children: expand(node.props.children)}};
}
function text(node) {
  if (node == null || node === false) return '';
  if (Array.isArray(node)) return node.map(text).join('');
  if (typeof node !== 'object') return String(node);
  return text(node.props.children);
}
function walk(node, output = []) {
  if (Array.isArray(node)) node.forEach(item => walk(item, output));
  else if (node && typeof node === 'object') { output.push(node); walk(node.props.children, output); }
  return output;
}

const old = environment(beforeModules), current = environment(afterModules);
const banner = {title: 'Fixture Cityへの旅行', deeplink: 'fixture://trip',
  imageUrl: 'fixture://scenic.png', cityBriefInfo: {imageUrl: 'fixture://fallback.png'}, index: 0};
const oldBanner = expand(old.load(667189).CmtTitle(banner));
assert(oldBanner && oldBanner.props.style.height === 120);
assert(text(oldBanner).includes(banner.title));
assert.equal(old.state.imageLoads, 1, 'Baseline must actually construct the scenic image');
for (const theme of ['light', 'dark']) for (const rtl of [false, true]) for (const title of ['', banner.title]) {
  current.state.theme = theme; current.state.rtl = rtl;
  assert.equal(current.load(667189).CmtTitle({...banner, title}), null);
}
assert.equal(current.state.imageLoads, 0);
assert.equal(current.state.urlCalls.length, 0);
assert.equal(current.state.telemetry.length, 0);
// All sibling timeline components execute to the same host tree as baseline.
for (const theme of ['light', 'dark']) for (const rtl of [false, true]) {
  for (const env of [old, current]) { env.state.theme = theme; env.state.rtl = rtl; }
  for (const [name, props] of [['CityTitle', {title: 'Tokyo → Fixture City', isFirstCity: true}],
    ['TimeTitle', {title: '10月22日（木）'}], ['TimelineLine', {style: {top: 20}}], ['TimelineEndDot', {}]])
    assert.deepEqual(JSON.parse(JSON.stringify(expand(current.load(667189)[name](props)))),
      JSON.parse(JSON.stringify(expand(old.load(667189)[name](props)))), name);
}

const dep = Date.UTC(2026, 9, 22, 2, 30), arr = Date.UTC(2026, 9, 22, 4, 50);
const retDep = dep + 86400000, retArr = arr + 86400000;
const details = {travelBeginTime: dep, travelEndTime: arr, flightDuration: 140,
  fromAirportName: 'Airport A T3', toAirportName: 'Airport B', pnrNo: 'TEST-A',
  pnrDeepLink: 'fixture://pnr-a', airLine: {flightNo: 'TEST101', iconUrl: 'fixture://airline'}};
const actions = [{text: 'オンラインチェックイン', deeplink: 'fixture://checkin'},
  {text: '運航状況', deeplink: 'fixture://status'}];
const outbound = {orderId: 'fixture-outbound', scheduleNo: 'fixture-one', scheduleType: 1,
  productLine: 'flight', detailDeeplink: 'fixture://booking-a', flightDetail: details, operateButtons: actions};
const inbound = {...outbound, orderId: 'fixture-inbound', scheduleNo: 'fixture-two',
  flightDetail: {...details, travelBeginTime: retDep, travelEndTime: retArr,
    flightDuration: 110, pnrNo: 'TEST-B', airLine: {flightNo: 'TEST102'}}};
const schedule = {data: {canMove: true, multiCmtTimelineItineraries: [{
  cmtId: 'fixture-trip', ...banner, title: banner.title, cityIdList: [1],
  timelineItineraries: [
    {title: 'Tokyo → Fixture City', cityBriefInfo: {cityId: 1}, itineraries: [{time: '10月22日（木）', itinerary: outbound}]},
    {title: 'Fixture City → Tokyo', cityBriefInfo: {cityId: 2}, itineraries: [{time: '10月23日（金）', itinerary: inbound}]},
  ],
}], moreOrderButton: {description: '全ての予約を表示', deeplink: 'fixture://all-bookings'}}, refreshKey: 1};
let scenarioCount = 0;
for (const theme of ['light', 'dark']) for (const rtl of [false, true])
  for (const fontScale of [1, 1.3, 2]) for (const a11y of [false, true]) {
    Object.assign(current.state, {theme, rtl, fontScale, a11y});
    current.state.dateStrings = new Map([[dep, '11:30'], [arr, '13:50'], [retDep, '20:45'], [retArr, '22:30']]);
    const root = current.load(666761).default(schedule), tree = expand(root), nodes = walk(tree);
    const allText = text(tree);
    assert(!allText.includes(banner.title));
    for (const value of ['11:30', '13:50', '20:45', '22:30', '2時間20分', '1時間50分',
      'TEST-A', 'TEST-B', 'Tokyo → Fixture City', 'Fixture City → Tokyo', '10月22日（木）',
      '10月23日（金）', '全ての予約を表示']) assert(allText.includes(value), value);
    const cards = nodes.filter(node => node.type === 'ReservationCard');
    assert.deepEqual(cards.map(node => node.props.data.orderId), ['fixture-outbound', 'fixture-inbound']);
    assert.deepEqual(cards.map(node => node.props.title), ['TEST101', 'TEST102']);
    cards.forEach(card => assert.deepEqual(card.props.actions, actions));
    const clocks = nodes.filter(node => node.type === 'CRNText' && node.props.style.fontSize === 20);
    assert.equal(clocks.length, 4);
    for (const clock of clocks) {
      assert.equal(clock.props.style.flexShrink, 0);
      assert.equal(clock.props.style.paddingRight, 4);
      assert(!('width' in clock.props.style));
      assert(!('maxWidth' in clock.props.style));
      assert.equal(clock.props.maxFontSizeMultiplier, 1.3);
    }
    for (const value of ['2時間20分', '1時間50分']) {
      const duration = nodes.find(node => node.type === 'CRNText' && text(node) === value);
      assert(duration);
      assert.equal(duration.props.style.width, '100%');
      assert.equal(duration.props.numberOfLines, undefined);
      assert.equal(duration.props.maxFontSizeMultiplier, 1.3);
      assert(!('maxWidth' in duration.props.style));
      assert(!('height' in duration.props.style));
    }
    assert.equal(nodes.filter(node => node.props.style && node.props.style.flexWrap === 'wrap').length, 2);
    assert(!nodes.some(node => node.props.style && node.props.style.height === 120));
    // The first actual group keeps CityTitle's20px gap without old extra22px.
    assert.equal(root.props.children[0][0].props.style.marginTop, 0);
    scenarioCount++;
  }
assert.equal(current.state.imageLoads, 0, 'Scenic placeholder and remote image never mount');
assert.equal(current.state.cmtFetches, 0, 'Prior preparation-card suppression is retained');
assert(current.state.durationCalls.includes(140 * 60));
assert(current.state.durationCalls.includes(110 * 60));
assert(current.state.dateCalls.some(([seconds, offset]) => seconds === dep / 1000 && offset === 28800));

// A real flight renderer exercises localization and exceptional data contracts.
function flight(detail) {
  return expand(current.load(666817).default({data: {...outbound, flightDetail: detail}, index: 1}));
}
current.state.dateStrings = new Map([[dep, '11:30 AM'], [arr, '1:50 PM']]);
assert(text(flight(details)).includes('11:30AM1:50PM2時間20分'));
const next = arr + 86400000;
current.state.dateStrings.set(next, '13:50');
current.state.rtl = false;
assert(text(flight({...details, travelEndTime: next})).includes('+1'));
current.state.rtl = true;
assert(text(flight({...details, travelEndTime: next})).includes('1+'));
current.state.rtl = false;
const missing = flight({...details, travelBeginTime: 0});
assert.equal(walk(missing).filter(node => node.type === 'CRNText' && node.props.style.fontSize === 20).length, 0);
const noDuration = flight({...details, flightDuration: 0});
assert.equal(walk(noDuration).filter(node => node.type === 'CRNText' && node.props.style.width === '100%').length, 0);
current.state.durationPrefix = '非常に長いローカライズされた所要時間：';
const long = flight({...details, flightDuration: 12599}), expected = current.state.durationPrefix + '209時間59分';
assert(text(long).includes(expected));
const longDuration = walk(long).find(node => node.type === 'CRNText' && text(node) === expected);
assert.equal(longDuration.props.numberOfLines, undefined);
assert.equal(longDuration.props.style.width, '100%');

// Both trip groups retain timeline content and only the intended intergroup gap.
const many = {...schedule, data: {...schedule.data, multiCmtTimelineItineraries:
  [schedule.data.multiCmtTimelineItineraries[0], {...schedule.data.multiCmtTimelineItineraries[0], cmtId: 'fixture-trip-2'}]}};
const manyRoot = current.load(666761).default(many);
assert.deepEqual(Array.from(manyRoot.props.children[0], node => node.props.style.marginTop), [0, 8]);
assert.equal(walk(expand(manyRoot)).filter(node => node.type === 'ReservationCard').length, 4);

const report = {
  schema: 'trip-myplan-js-render-v1.10.15', passed: true,
  baseline_sha256: sha(baseline), patched_sha256: sha(patched),
  parsed_indexed_modules: afterModules.size, changed_modules: changed,
  full_timeline_scenarios: scenarioCount,
  source_modules_executed: [666761, 666762, 666817, 666790, 666755, 666756, 666726, 666788, 667189, 667264],
  checks: ['baseline scenic120px card constructs image; patched returns null',
    'city/date/timeline siblings equal baseline', 'two actual flight cards retain data/title/PNR/actions',
    '24/12h time values preserved, nonshrinking clocks without width cap',
    'duration has its own full-width multiline row and original minute conversion',
    'light/dark, RTL, accessibility mode, font scales1/1.3/2',
    'cross-day badges both directions, missing time, zero and long duration',
    'first/intergroup margin0/8 plus original CityTitle20',
    'multiple trips retain all four reservation cards',
    'all old time/layout modules unchanged and no old arrow renderer invoked'],
  native_date_timezone_contract_preserved: true,
  android_view_measurement_tested: false,
  limitations: 'Host JSX/tree/style-contract execution with native Text/localization/widget stubs. Device glyph measurement and runtime bundle selection are separate validations.',
};
if (reportPath) fs.writeFileSync(reportPath, JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify(report, null, 2));
