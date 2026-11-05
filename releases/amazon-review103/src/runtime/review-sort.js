(function () {
    'use strict';
    var VERSION = '1.0.3';
    var STATE = '__hiroReviewCountSort103';
    var STATUS = 'hiro-review-count-status';
    var HOST = /^(?:(?:www|m|smile)\.)?amazon\.(?:co\.jp|com|co\.uk|de|fr|it|es|ca|com\.au|com\.br|com\.mx|in|nl|se|pl|com\.be|ie|sg|ae|sa|eg|com\.tr|co\.za)$/i;
    var NUMBER = '(?:[0-9]{1,3}(?:[,\\u00a0\\u202f][0-9]{3})+|[0-9]+(?:[.,][0-9]+)?)(?:万|千|[kKmM])?';
    var NAMED_COUNT = new RegExp('(?:^|[^0-9.,])(' + NUMBER + ')\\s*(?:個の評価|件の評価|件のレビュー|件のカスタマーレビュー|ratings?\\b|reviews?\\b|global ratings?\\b)', 'i');

    function cleanNumber(text) {
        return String(text || '').replace(/[０-９]/g, function (x) { return String.fromCharCode(x.charCodeAt(0) - 65248); })
            .replace(/，/g, ',').replace(/．/g, '.').trim();
    }

    // Only use bare numbers from dedicated review-count elements, never product text.
    function parseCount(text, allowBare) {
        text = cleanNumber(text);
        var named = NAMED_COUNT.exec(text);
        if (named) text = cleanNumber(named[1]);
        else if (!allowBare) return -1;
        text = text.replace(/^[（(]\s*/, '').replace(/\s*[)）]$/, '').replace(/\+$/, '').trim();
        if (!/^[0-9][0-9,.\s\u00a0\u202f]*(?:万|千|[kKmM])?$/.test(text)) return -1;
        var multiplier = 1;
        var last = text.slice(-1).toLowerCase();
        if (last === '万') multiplier = 10000;
        else if (last === '千' || last === 'k') multiplier = 1000;
        else if (last === 'm') multiplier = 1000000;
        if (multiplier !== 1) text = text.slice(0, -1);
        if (/[\s\u00a0\u202f]/.test(text)) {
            if (!/^\d{1,3}(?:[\s\u00a0\u202f]\d{3})+$/.test(text)) return -1;
            text = text.replace(/[\s\u00a0\u202f]/g, '');
        }
        if (multiplier === 1) {
            if (/^\d{1,3}(?:,\d{3})+$/.test(text)) text = text.replace(/,/g, '');
            else if (/^\d{1,3}(?:\.\d{3})+$/.test(text)) text = text.replace(/\./g, '');
            else if (!/^\d+$/.test(text)) return -1;
        } else {
            if (/^\d+,\d{1,2}$/.test(text)) text = text.replace(',', '.');
            else if (!/^\d+(?:\.\d{1,3})?$/.test(text)) return -1;
        }
        var count = Number(text) * multiplier;
        return isFinite(count) && count >= 0 && count <= 1000000000 ? Math.round(count) : -1;
    }

    var CARD = '[data-component-type="s-search-result"], .s-result-item[data-asin], li[data-asin]';
    var ROOTS = '.s-main-slot, #s-results-list-atf, #s-results-list, #s-result-list, .s-result-list, [data-component-type="s-search-results"]';
    var CAROUSEL = '.a-carousel-container, .a-carousel, .a-carousel-viewport, [data-a-carousel-options], [data-component-type="s-sponsored-carousel"], [data-component-type="s-searchgrid-carousel"], [data-component-type="sbv-video-single-product"], #rhf, #dp, #ppd';
    var REVIEW_BLOCK = '[data-cy="reviews-ratings"], [data-cy="reviews-block"]';
    var STAR = '.a-icon-star-small, .a-icon-star, .a-icon-star-mini, .a-icon-alt, [class*="a-star-"]';

    function isAmazon(win) {
        var location = win && win.location;
        return !!location && location.protocol === 'https:' && HOST.test(location.hostname) &&
            (!location.port || location.port === '443') && !location.username && !location.password;
    }

    function searchPath(path) {
        path = String(path || '').replace(/^\/-\/[A-Za-z]{2}(?:_[A-Za-z]{2})?(?=\/)/, '');
        if (/^\/s\/browse(?:\/|$)/.test(path)) return false;
        return /^\/(?:[^/]+\/)?s(?:\/|$)/.test(path) || path === '/gp/search' ||
            path === '/gp/aw/search/s' || /^\/gp\/aw\/s(?:\/|$)/.test(path) ||
            path === '/mn/search' || /^\/search-by-list(?:\/|$)/.test(path);
    }

    function hidden(node) {
        for (var p = node; p && p.nodeType === 1; p = p.parentElement) {
            if (p.hasAttribute('hidden') || p.getAttribute('aria-hidden') === 'true' ||
                (p.style && (p.style.display === 'none' || p.style.visibility === 'hidden'))) return true;
        }
        return false;
    }

    function homePath(path) {
        path = String(path || '').replace(/^\/-\/[A-Za-z]{2}(?:_[A-Za-z]{2})?(?=\/)/, '');
        return path === '' || path === '/' || /^\/ref=[^/]*\/?$/.test(path) || /^\/gp\/aw(?:\/|\/ref=[^/]*\/?)?$/.test(path);
    }

    function isSearch(win) {
        if (!isAmazon(win)) return false;
        var path = String(win.location.pathname || '').replace(/^\/-\/[A-Za-z]{2}(?:_[A-Za-z]{2})?(?=\/)/, '');
        if (searchPath(path)) return true;
        // Some app transitions swap the main document before updating its URL.
        // Strong search structure may enable only home-style routes, never checkout/product screens.
        if (!homePath(path)) return false;
        var doc = win.document;
        if (!doc || doc.querySelector('#dp, #ppd, #productTitle')) return false;
        var search = doc.querySelector('#search');
        if (!search || hidden(search)) return false;
        var root = search.querySelector(ROOTS);
        return !!root && !hidden(root) && !!root.querySelector(CARD);
    }

    function belongs(node, card) {
        if (!node || !card.contains(node) || node.closest(CAROUSEL)) return false;
        if (node.closest(CARD) !== card) return false;
        var asin = card.getAttribute('data-asin');
        for (var p = node; p && p !== card; p = p.parentElement) {
            var nestedAsin = p.getAttribute && p.getAttribute('data-asin');
            if (nestedAsin && nestedAsin !== asin) return false;
        }
        return true;
    }

    function reviewContext(node, card) {
        var block = node.closest(REVIEW_BLOCK);
        if (block && belongs(block, card)) return true;
        var link = node.closest('a[href*="customerReviews"], a[href*="product-reviews/"]');
        if (link && belongs(link, card)) return true;
        var row = node.closest('.a-row');
        if (!row || !belongs(row, card)) return false;
        var stars = row.querySelectorAll(STAR);
        for (var i = 0; i < stars.length; i++) if (belongs(stars[i], card)) return true;
        return false;
    }

    function readCount(card) {
        var i, n;
        // Explicit count semantics are safe on spans too, not only review hyperlinks.
        var labels = card.querySelectorAll('[aria-label], [data-review-count], [data-customer-review-count]');
        for (i = 0; i < labels.length; i++) {
            var label = labels[i];
            if (!belongs(label, card) || label.closest('h1, h2, h3, [data-cy="title-recipe"], .a-price')) continue;
            n = parseCount(label.getAttribute('aria-label'), false);
            if (n >= 0) return n;
            if (label.hasAttribute('data-review-count')) n = parseCount(label.getAttribute('data-review-count'), true);
            else if (label.hasAttribute('data-customer-review-count')) n = parseCount(label.getAttribute('data-customer-review-count'), true);
            if (n >= 0) return n;
        }
        // Compact mobile layouts place the count beside stars with no dedicated link.
        var compact = card.querySelectorAll('.s-underline-text, span.a-size-mini.puis-normal-weight-text');
        for (i = 0; i < compact.length; i++) {
            var countNode = compact[i];
            if (!belongs(countNode, card) || !reviewContext(countNode, card) || countNode.matches(STAR) || countNode.querySelector(STAR) || countNode.closest('.a-icon, h1, h2, h3, [data-cy="title-recipe"], .a-price') || countNode.querySelector('h1, h2, h3, [data-cy="title-recipe"]')) continue;
            var enclosingLink = countNode.closest('a');
            if (enclosingLink && !enclosingLink.matches('a[href*="customerReviews"], a[href*="product-reviews/"]') && !countNode.closest(REVIEW_BLOCK)) continue;
            n = parseCount(countNode.textContent, true);
            if (n >= 0 && n <= 5 && !/^[（(].*[)）]$/.test(countNode.textContent.trim())) {
                var countLink = countNode.closest('a[href*="customerReviews"], a[href*="product-reviews/"]');
                if (!countLink || countLink.querySelector(STAR) || /stars?|5つ星/i.test(countLink.getAttribute('aria-label') || '')) continue;
            }
            if (n >= 0) return n;
        }
        var links = card.querySelectorAll('a[href*="customerReviews"], a[href*="product-reviews/"]');
        for (i = 0; i < links.length; i++) {
            var link = links[i];
            if (!belongs(link, card)) continue;
            var hasStar = !!link.querySelector(STAR) || /stars?|5つ星/i.test(link.getAttribute('aria-label') || '');
            var counts = link.querySelectorAll('.a-size-small, .a-size-base, .a-size-mini');
            for (var j = 0; j < counts.length; j++) {
                var span = counts[j];
                if (!belongs(span, card) || span.matches(STAR) || span.querySelector(STAR) || span.closest('.a-icon, .a-price')) continue;
                // A combined star/count link may expose integer star scores. Only its
                // parenthesized number is an unambiguous fallback; named/dedicated counts were handled above.
                if (hasStar && !/^[（(].*[)）]$/.test(span.textContent.trim())) continue;
                n = parseCount(span.textContent, true);
                if (n >= 0) return n;
            }
            if (!hasStar) {
                n = parseCount(link.textContent, true);
                if (n >= 0) return n;
            }
        }
        var blocks = card.querySelectorAll(REVIEW_BLOCK);
        for (i = 0; i < blocks.length; i++) {
            if (!belongs(blocks[i], card) || blocks[i].querySelector('[data-asin]')) continue;
            n = parseCount(blocks[i].textContent, false);
            if (n >= 0) return n;
        }
        return -1;
    }

    function structuralCard(node) {
        if (!node || node.nodeType !== 1 || !node.matches(CARD)) return false;
        var asin = node.getAttribute('data-asin');
        return !!asin && /^[A-Za-z0-9]{10}$/.test(asin);
    }

    function isSponsored(node) {
        return node.matches('.AdHolder, [data-component-type="sp-sponsored-result"]') ||
            !!node.querySelector('[data-ad-feedback], [data-ad-feedback-label-id], .puis-sponsored-label-text, [cel_widget_id^="MAIN-FEATURED_ASINS_LIST-"]');
    }

    // A wrapper may move with its sole card only if every sibling along the path
    // is whitespace, a comment, or inert markup. Other headings/widgets stay put.
    function plainWrapper(wrapper, child) {
        for (var node = wrapper.firstChild; node; node = node.nextSibling) {
            if (node === child || node.nodeType === 8) continue;
            if (node.nodeType === 3 && !node.textContent.trim()) continue;
            if (node.nodeType === 1 && /^(SCRIPT|STYLE|TEMPLATE)$/.test(node.tagName)) continue;
            return false;
        }
        return true;
    }

    function presentation(node) {
        var classes = Array.from(node.classList || []).filter(function (name) {
            // These identify the same search list at different pagination boundaries.
            return !/^(s-main-slot|s-result-list|s-search-results|celwidget)$/.test(name) && !/(?:^|[-_])(active|loading|loaded|hover|focus|selected|expanded|collapsed|tick|animating)(?:$|[-_])/.test(name);
        }).sort().join('.');
        var layout = '';
        var view = node.ownerDocument && node.ownerDocument.defaultView;
        if (view && typeof view.getComputedStyle === 'function') {
            var computed = view.getComputedStyle(node);
            layout = computed.display + ':' + computed.flexDirection + ':' + computed.gridTemplateColumns + ':' + computed.position;
        }
        return node.tagName + '[' + classes + ']' + layout;
    }

    function chooseGroups(doc) {
        var roots = Array.prototype.slice.call(doc.querySelectorAll(ROOTS));
        var searchLandmark = doc.querySelector('#search');
        if (searchLandmark && hidden(searchLandmark)) searchLandmark = null;
        roots = roots.filter(function (root) {
            return !hidden(root) && !root.closest(CAROUSEL) && (!searchLandmark || searchLandmark.contains(root));
        });
        // Outer recognized lists already cover their nested row wrappers.
        roots = roots.filter(function (root) {
            return !roots.some(function (other) { return other !== root && other.contains(root); });
        });
        var found = [], seen = new Set();
        roots.forEach(function (root) {
            var all = Array.prototype.filter.call(root.querySelectorAll(CARD), function (card) {
                if (!structuralCard(card) || hidden(card) || card.closest(CAROUSEL)) return false;
                var outer = card.parentElement && card.parentElement.closest(CARD);
                return !outer || !root.contains(outer);
            }).slice(0, 1000);
            var counts = new WeakMap();
            all.forEach(function (card) {
                for (var p = card; p && root.contains(p); p = p.parentElement) {
                    counts.set(p, (counts.get(p) || 0) + 1);
                    if (p === root) break;
                }
            });
            var groups = [];
            all.forEach(function (card) {
                if (seen.has(card) || isSponsored(card)) return;
                seen.add(card);
                var slot = card;
                while (slot.parentElement && slot.parentElement !== root && counts.get(slot.parentElement) === 1 && plainWrapper(slot.parentElement, slot)) slot = slot.parentElement;
                var parent = slot.parentElement;
                if (!parent || !root.contains(parent)) return;
                var group = groups.find(function (g) { return g.parent === parent; });
                if (!group) { group = {parent: parent, root: root, cards: [], slots: []}; groups.push(group); }
                group.cards.push(card); group.slots.push(slot);
            });
            groups.forEach(function (group) {
                // Never exchange products across a heading/extra widget inside a
                // nested section. Canonical result lists preserve their widget slots.
                var divided = Array.prototype.some.call(group.parent.children, function (child) {
                    if (group.slots.indexOf(child) >= 0 || structuralCard(child) && isSponsored(child) || /^(SCRIPT|STYLE|TEMPLATE)$/.test(child.tagName)) return false;
                    var headings = child.matches('h1, h2, h3, [role="heading"]') ? [child] : child.querySelectorAll('h1, h2, h3, [role="heading"]');
                    return Array.prototype.some.call(headings, function (heading) { return !/^(結果|検索結果|results|search results)$/i.test(heading.textContent.trim()); });
                });
                group.joinable = !divided && (group.parent === root || Array.prototype.every.call(group.parent.children, function (child) {
                    return group.slots.indexOf(child) >= 0 || structuralCard(child) && isSponsored(child) || /^(SCRIPT|STYLE|TEMPLATE)$/.test(child.tagName);
                }));
                var shapes = group.slots.map(function (slot, index) {
                    var chain = [], card = group.cards[index], node = slot;
                    while (node && node !== card) {
                        chain.push(presentation(node));
                        node = Array.prototype.find.call(node.children, function (child) { return child === card || child.contains(card); });
                    }
                    chain.push('CARD:' + presentation(card));
                    return chain.join('>');
                });
                group.shape = presentation(group.parent) + '|' + Array.from(new Set(shapes)).sort().join(',');
                found.push(group);
            });
        });
        return found;
    }

    // Kept for diagnostics/tests of a single layout; sorting uses every compatible group.
    function chooseGroup(doc) {
        return chooseGroups(doc).sort(function (a, b) { return b.cards.length - a.cards.length; })[0] || null;
    }

    function install(win) {
        if (!win || !win.document || !win.MutationObserver || !isAmazon(win)) return null;
        if (win[STATE]) { win[STATE].schedule(); return win[STATE]; }
        var doc = win.document, IDLE = 320, TARGET = 100, BUDGET = 20000, STEP = 800, MAX_STEPS = 30, STALLS = 4;
        var pending = 0, pendingAt = Infinity, running = false, disposed = false, dirty = false, generation = 0;
        var observer, lastActivity = -Infinity, touchActive = false, pointers = new Set(), activityEpoch = 0;
        var cache = new WeakMap(), objectIds = new WeakMap(), nextId = 1, nextOrdinal = 1;
        var lastKey = null, expectedScroll = null, writingScroll = false, restoreFrame = 0, restoreStyles = null, session = null, tokenAliases = new Map();
        var state = {version: VERSION, active: false, searchHint: false, hintToken: null, phase: 'waiting',
            schedule: schedule, runNow: run, dispose: dispose,
            setSearchHint: function (value, token) {
                var hint = value === true, safeToken = typeof token === 'string' && /^[a-zA-Z0-9]{1,64}$/.test(token) ? token : null;
                if (hint && safeToken && searchPath(win.location.pathname)) {
                    var canonical = searchKey();
                    // Native callback verifies this token against the current real search URL.
                    // Adopting that alias does not restart the same home-URL SPA cohort.
                    if (lastKey === 'native:' + safeToken) lastKey = canonical;
                    tokenAliases.set(safeToken, canonical);
                    if (tokenAliases.size > 32) tokenAliases.delete(tokenAliases.keys().next().value);
                }
                if (state.searchHint !== hint || state.hintToken !== safeToken) {
                    state.searchHint = hint;
                    if (safeToken) state.hintToken = safeToken;
                    schedule();
                }
            }};
        win[STATE] = state;
        function now() { return win.performance && typeof win.performance.now === 'function' ? win.performance.now() : Date.now(); }
        function objectId(node) { if (!objectIds.has(node)) objectIds.set(node, nextId++); return objectIds.get(node); }
        function busy() { return touchActive || pointers.size > 0 || now() - lastActivity < IDLE; }
        function observe() {
            if (!disposed && doc.documentElement && isAmazon(win)) observer.observe(doc.documentElement, {
                childList: true, subtree: true, characterData: true, attributes: true,
                attributeFilter: ['aria-label', 'data-asin', 'data-component-type', 'data-review-count', 'data-customer-review-count', 'hidden', 'style', 'aria-hidden', 'class']
            });
        }
        function cancelTimer() { if (pending) win.clearTimeout(pending); pending = 0; pendingAt = Infinity; }
        function queue(delay) {
            if (disposed) return;
            var due = now() + Math.max(0, delay);
            if (pending && pendingAt <= due) return;
            cancelTimer(); pendingAt = due;
            var stamp = generation;
            pending = win.setTimeout(function () { pending = 0; pendingAt = Infinity; if (stamp === generation) run(); }, Math.max(0, delay));
        }
        function schedule() { dirty = true; queue(80); }
        function stopCollecting(reason) {
            if (!session || session.phase === 'frozen') return;
            session.phase = 'finalizing'; session.interrupted = true; session.reason = reason;
            state.phase = session.phase; expectedScroll = null; generation++; cancelTimer(); dirty = true;
        }
        function activity(event) {
            var type = event.type;
            if (type === 'scroll' && writingScroll) return;
            if (type === 'scroll' && expectedScroll && now() < expectedScroll.until &&
                Math.abs(scrollPosition(expectedScroll.scroller) - expectedScroll.value) < 2) return;
            if (type === 'touchstart' || type === 'touchmove') touchActive = !event.touches || event.touches.length > 0;
            if (type === 'touchend' || type === 'touchcancel') touchActive = !!event.touches && event.touches.length > 0;
            if (type === 'pointerdown') pointers.add(event.pointerId == null ? 0 : event.pointerId);
            if (type === 'pointerup' || type === 'pointercancel') pointers.delete(event.pointerId == null ? 0 : event.pointerId);
            lastActivity = now(); activityEpoch++;
            stopCollecting('input');
            if (dirty && !touchActive && !pointers.size) queue(IDLE);
        }
        function visibilityChanged() {
            if (doc.hidden || doc.visibilityState === 'hidden') stopCollecting('hidden');
            else schedule();
        }
        function removeStatus() {
            var old = doc.getElementById(STATUS);
            if (old && old.parentNode) old.parentNode.removeChild(old);
        }
        function statusAnchor(groups) {
            // Stable placement at the start of the result area, never at a changing largest group.
            var root = groups && groups.length ? groups[0].root : doc.querySelector(ROOTS);
            return root && !hidden(root) && !root.closest(CAROUSEL) ? root : null;
        }
        function showStatus(text, groups, kind) {
            var label = doc.getElementById(STATUS);
            if (!label) {
                label = doc.createElement('div'); label.id = STATUS;
                label.setAttribute('role', 'status'); label.setAttribute('aria-live', 'polite');

            }
            if (label.textContent !== text) label.textContent = text;
            if (label.getAttribute('data-hiro-state') !== kind) label.setAttribute('data-hiro-state', kind);
            var css = 'box-sizing:border-box;padding:8px 12px;font-size:12px;line-height:1.5;color:#0f1111;background:#eef6f7;border-bottom:1px solid #d5d9d9;' +
                (kind === 'collecting' || kind === 'settling' ? 'position:fixed;bottom:0;left:0;right:0;z-index:2147483646;pointer-events:none;' : '');
            if (label.getAttribute('data-hiro-style') !== kind) { label.style.cssText = css; label.setAttribute('data-hiro-style', kind); }
            var title = '約100商品を上限20秒で先読みし、同じ表示形式ごとに評価件数順で一度だけ固定します。画面操作で先読みを終了します。追加商品は固定した商品の後ろに表示します。';
            if (label.getAttribute('title') !== title) label.setAttribute('title', title);
            var anchor = statusAnchor(groups);
            if (anchor && anchor.parentNode) {
                if (label.nextSibling !== anchor) anchor.parentNode.insertBefore(label, anchor);
            } else if (doc.body && label.parentNode !== doc.body) doc.body.insertBefore(label, doc.body.firstChild);
        }
        function canShowHint() {
            return state.searchHint && isAmazon(win) && homePath(win.location.pathname) && !doc.querySelector('#dp, #ppd, #productTitle');
        }
        function searchKey() {
            // Native opaque identity covers same-document app searches whose URL remains home.
            // Actual search criteria take precedence; pagination and telemetry never reset ranks.
            if (!searchPath(win.location.pathname)) return state.hintToken ? tokenAliases.get(state.hintToken) || 'native:' + state.hintToken : lastKey || 'native:pending';
            var params;
            try { params = new URLSearchParams(win.location.search || ''); } catch (ignored) { return 'url:'; }
            var pairs = [];
            params.forEach(function (value, key) {
                if (/^(?:k|keywords|field-keywords|i|rh|bbn|node|me|emi)$/.test(key) || /^p_/.test(key)) pairs.push([key, value]);
            });
            return 'url:' + JSON.stringify(pairs.sort(function (a, b) { return a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : a[1] < b[1] ? -1 : a[1] > b[1] ? 1 : 0; }));
        }
        function resetResults(key) {
            generation++; cancelTimer(); cache = new WeakMap(); nextOrdinal = 1; lastKey = key; expectedScroll = null;
            session = {phase: 'waiting', started: now(), deadline: now() + BUDGET, auto: false, interrupted: false,
                steps: 0, stalls: 0, lastCount: 0, nextStep: now(), ranks: new Map(), nodeRanks: new WeakMap(), nextRank: 0};
            state.phase = session.phase;
        }
        function countEntry(card, slot, bucket) {
            var asin = card.getAttribute('data-asin'), held = cache.get(card), value = readCount(card);
            if (!held || held.asin !== asin) held = {asin: asin, value: -1, ordinal: nextOrdinal++};
            if (value >= 0) held.value = value;
            cache.set(card, held);
            return {node: card, slot: slot, parent: slot.parentNode, count: held.value, ordinal: held.ordinal, bucket: bucket, asin: asin};
        }
        function inDocumentOrder(a, b) {
            if (a.slot === b.slot) return 0;
            return a.slot.compareDocumentPosition(b.slot) & 4 ? -1 : 1;
        }
        function getScroller(node) {
            for (var p = node.parentElement; p && p !== doc.body && p !== doc.documentElement; p = p.parentElement) {
                if (p.scrollHeight > p.clientHeight + 1 && p.clientHeight > 0 && typeof win.getComputedStyle === 'function') {
                    var overflow = win.getComputedStyle(p).overflowY;
                    if (overflow === 'auto' || overflow === 'scroll') return p;
                }
            }
            return doc.scrollingElement || doc.documentElement;
        }
        function scrollPosition(scroller) {
            if (scroller === doc.scrollingElement || scroller === doc.documentElement || scroller === doc.body) return Number(win.scrollY || win.pageYOffset || scroller.scrollTop || 0);
            return Number(scroller.scrollTop || 0);
        }
        function captureAnchor(entries) {
            if (busy() || typeof win.innerHeight !== 'number') return null;
            for (var i = 0; i < entries.length; i++) {
                var node = entries[i].slot;
                if (!node.getBoundingClientRect) continue;
                var rect = node.getBoundingClientRect(), scroller = getScroller(node);
                if (scrollPosition(scroller) <= 1) continue; // initial/top search must reveal the highest-ranked items
                var bounds = scroller !== doc.scrollingElement && scroller !== doc.documentElement && scroller !== doc.body && scroller.getBoundingClientRect ? scroller.getBoundingClientRect() : {top: 0, bottom: win.innerHeight};
                if (rect.height > 0 && rect.bottom > Math.max(0, bounds.top) && rect.top < Math.min(win.innerHeight, bounds.bottom)) {
                    return {node: node, top: rect.top, scroller: scroller, epoch: activityEpoch};
                }
            }
            return null;
        }
        function restoreScrollStyles() {
            if (restoreFrame && typeof win.cancelAnimationFrame === 'function') win.cancelAnimationFrame(restoreFrame);
            restoreFrame = 0;
            if (restoreStyles) { var undo = restoreStyles; restoreStyles = null; undo(); }
            restoreFrame = 0;
        }
        function protectAnchor(anchor, commit) {
            if (!anchor || !anchor.scroller || !anchor.scroller.style) { commit(); return; }
            // Keep this correction synchronous: no input event can interleave between
            // the last idle check, DOM transaction and pixel compensation.
            restoreScrollStyles();
            var style = anchor.scroller.style, oldAnchor = style.getPropertyValue('overflow-anchor'), oldPriority = style.getPropertyPriority ? style.getPropertyPriority('overflow-anchor') : '';
            var oldBehavior = style.getPropertyValue('scroll-behavior'), behaviorPriority = style.getPropertyPriority ? style.getPropertyPriority('scroll-behavior') : '';
            style.setProperty('overflow-anchor', 'none', 'important'); style.setProperty('scroll-behavior', 'auto', 'important');
            restoreStyles = function () {
                if (style.getPropertyValue('overflow-anchor') === 'none') { if (oldAnchor) style.setProperty('overflow-anchor', oldAnchor, oldPriority); else style.removeProperty('overflow-anchor'); }
                if (style.getPropertyValue('scroll-behavior') === 'auto') { if (oldBehavior) style.setProperty('scroll-behavior', oldBehavior, behaviorPriority); else style.removeProperty('scroll-behavior'); }
            };
            try {
                commit();
                if (!busy() && anchor.epoch === activityEpoch && anchor.node.isConnected && getScroller(anchor.node) === anchor.scroller) {
                    var delta = anchor.node.getBoundingClientRect().top - anchor.top;
                    if (Math.abs(delta) > 0.5 && isFinite(delta)) {
                        var before = scrollPosition(anchor.scroller);
                        expectedScroll = {scroller: anchor.scroller, value: before + delta, until: now() + 150};
                        if (anchor.scroller === doc.scrollingElement || anchor.scroller === doc.documentElement || anchor.scroller === doc.body) {
                            if (typeof win.scrollTo === 'function') win.scrollTo(Number(win.scrollX || 0), before + delta);
                        } else anchor.scroller.scrollTop = before + delta;
                    }
                }
            } finally {
                if (typeof win.requestAnimationFrame === 'function') restoreFrame = win.requestAnimationFrame(function () { observer.disconnect(); restoreScrollStyles(); observe(); });
                else restoreScrollStyles();
            }
        }
        function applyMoves(plans) {
            plans.forEach(function (plan) {
                if (!plan.changed) return;
                var markers = plan.entries.map(function (entry, index) {
                    if (entry.slot === plan.ranked[index].slot) return null;
                    var marker = doc.createComment('hiro-review-slot'); entry.parent.insertBefore(marker, entry.slot); return marker;
                });
                plan.ranked.forEach(function (entry, index) {
                    var marker = markers[index];
                    if (marker && entry.slot.nextSibling !== marker) marker.parentNode.insertBefore(entry.slot, marker);
                });
                markers.forEach(function (marker) { if (marker && marker.parentNode) marker.parentNode.removeChild(marker); });
            });
        }
        function entriesFor(groups, counts) {
            var entries = [];
            groups.forEach(function (group) {
                var bucket = group.joinable ? 'shape:' + group.shape : 'parent:' + objectId(group.parent);
                group.cards.forEach(function (card, index) {
                    var slot = group.slots[index], key = bucket + '|scroll:' + objectId(getScroller(slot));
                    entries.push(counts ? countEntry(card, slot, key) : {node: card, slot: slot, parent: slot.parentNode, asin: card.getAttribute('data-asin'), bucket: key});
                });
            });
            return entries.sort(inDocumentOrder);
        }
        function uniqueCount(entries) { return new Set(entries.map(function (e) { return e.asin; })).size; }
        function plansFor(entries, comparator) {
            var buckets = new Map();
            entries.forEach(function (e) { if (!buckets.has(e.bucket)) buckets.set(e.bucket, []); buckets.get(e.bucket).push(e); });
            var plans = [];
            buckets.forEach(function (items) {
                var ranked = items.slice().sort(comparator);
                plans.push({entries: items, ranked: ranked, changed: ranked.some(function (item, i) { return item.slot !== items[i].slot; })});
            });
            return plans;
        }
        function scrollToValue(scroller, value) {
            var current = scrollPosition(scroller);
            if (!isFinite(value) || Math.abs(value - current) < 1) return false;
            var record = {scroller: scroller, value: value, until: now() + 1600};
            expectedScroll = record;
            var style = scroller.style, old = style && style.getPropertyValue('scroll-behavior');
            var priority = style && style.getPropertyPriority ? style.getPropertyPriority('scroll-behavior') : '';
            if (style) style.setProperty('scroll-behavior', 'auto', 'important');
            writingScroll = true;
            try {
                if (scroller === doc.scrollingElement || scroller === doc.documentElement || scroller === doc.body) {
                    if (typeof win.scrollTo !== 'function') return false;
                    win.scrollTo(Number(win.scrollX || 0), value);
                } else scroller.scrollTop = value;
                // Browsers may clamp values and deliver coalesced scroll events later.
                record.value = scrollPosition(scroller);
            } finally {
                writingScroll = false;
                if (style && style.getPropertyValue('scroll-behavior') === 'auto') {
                    if (old) style.setProperty('scroll-behavior', old, priority); else style.removeProperty('scroll-behavior');
                }
            }
            return Math.abs(current - record.value) >= 1;
        }
        function assignRank(entry, rank) {
            session.nodeRanks.set(entry.node, {asin: entry.asin, rank: rank});
            if (!session.ranks.has(entry.asin)) session.ranks.set(entry.asin, []);
            if (session.ranks.get(entry.asin).indexOf(rank) < 0) session.ranks.get(entry.asin).push(rank);
            entry.rank = rank;
        }
        function finishText(plans, entries) {
            var known = entries.filter(function (e) { return e.count >= 0; }).length;
            var text = known ? '評価件数順・先読み ' + uniqueCount(entries) + ' 商品で固定' :
                entries.length ? '評価件数を取得できません（' + uniqueCount(entries) + ' 商品で固定）' : '先読み終了：商品一覧を確認できません';
            if (known && known !== entries.length) text += '・件数不明は末尾';
            if (known && plans.length > 1) text += '・表示形式ごと';
            return text + ' v' + VERSION + '・追加分は末尾';
        }
        function finalize(groups) {
            // This is the only review-count ranking in a search session.
            var entries = entriesFor(groups, true), plans = plansFor(entries, function (a, b) { return b.count - a.count || a.ordinal - b.ordinal; });
            if (!entries.length) {
                session.phase = 'awaiting'; state.phase = 'awaiting'; session.interrupted = true; cancelTimer();
                showStatus('先読み終了：商品一覧の表示を待っています v' + VERSION, groups, 'awaiting'); dirty = false; return;
            }
            plans.forEach(function (plan) { plan.ranked.forEach(function (entry) { assignRank(entry, session.nextRank++); }); });
            session.text = finishText(plans, entries);
            var returnTop = session.auto && !session.interrupted && session.scroller && !busy();
            protectAnchor(returnTop ? null : captureAnchor(entries), function () {
                applyMoves(plans); showStatus(session.text, groups, 'frozen');
                if (returnTop) scrollToValue(session.scroller, session.origin);
            });
            session.phase = 'frozen'; state.phase = 'frozen'; cancelTimer(); dirty = false;
        }
        function normalizeFrozen(groups, entries) {
            // Never inspect rating/count nodes here. Native replacements inherit the ASIN rank;
            // first-seen newcomers get immutable ranks after every already-known product.
            var used = new Set();
            entries.forEach(function (entry) {
                var held = session.nodeRanks.get(entry.node);
                if (held && held.asin === entry.asin) { entry.rank = held.rank; used.add(held.rank); }
            });
            entries.forEach(function (entry) {
                if (entry.rank != null) return;
                var ranks = session.ranks.get(entry.asin) || [], rank = ranks.find(function (value) { return !used.has(value); });
                if (rank == null) rank = session.nextRank++;
                assignRank(entry, rank); used.add(rank);
            });
            var plans = plansFor(entries, function (a, b) { return a.rank - b.rank; });
            var label = doc.getElementById(STATUS), target = statusAnchor(groups);
            var badgeChanged = !label || label.textContent !== session.text || label.getAttribute('data-hiro-state') !== 'frozen' || (target && label.nextSibling !== target);
            if (plans.some(function (plan) { return plan.changed; }) || badgeChanged) protectAnchor(captureAnchor(entries), function () {
                applyMoves(plans); if (badgeChanged) showStatus(session.text, groups, 'frozen');
            });
            dirty = false;
        }
        function restoredDocument() {
            try {
                var nav = win.performance && win.performance.getEntriesByType && win.performance.getEntriesByType('navigation');
                return !!(nav && nav[0] && nav[0].type === 'back_forward');
            } catch (ignored) { return false; }
        }
        function collect(groups, entries) {
            var count = uniqueCount(entries), time = now();
            if (session.phase === 'awaiting') session.phase = 'finalizing';
            if (session.phase === 'waiting') {
                if (!entries.length) {
                    showStatus('先読み中 0 / 約100商品・画面操作で終了 v' + VERSION, groups, 'collecting');
                    if (time < session.deadline) { queue(Math.min(STEP, session.deadline - time)); return; }
                    session.phase = 'finalizing';
                } else {
                    var scroller = getScroller(entries[0].slot);
                    session.scroller = scroller; session.origin = scrollPosition(scroller);
                    session.auto = (searchPath(win.location.pathname) || state.searchHint && isSearch(win)) &&
                        session.origin <= 1 && scrollPosition(doc.scrollingElement || doc.documentElement) <= 1 &&
                        typeof win.innerHeight === 'number' && win.innerHeight > 0 && typeof win.scrollTo === 'function' && !restoredDocument();
                    session.phase = session.auto ? 'collecting' : 'finalizing';
                    session.lastCount = count;
                }
            }
            if (session.phase === 'finalizing') { finalize(groups); return; }
            if (count >= TARGET && session.phase !== 'settling') { session.phase = 'settling'; session.settleUntil = Math.min(time + STEP, session.deadline); }
            state.phase = session.phase;
            showStatus('先読み中 ' + count + ' / 約100商品・画面操作で終了 v' + VERSION, groups, session.phase);
            if (time >= session.deadline || session.steps >= MAX_STEPS || session.phase === 'settling' && time >= session.settleUntil) { finalize(groups); return; }
            if (session.phase === 'settling') { queue(session.settleUntil - time); return; }
            if (time < session.nextStep) { queue(Math.min(session.nextStep - time, session.deadline - time)); return; }
            if (count > session.lastCount) session.stalls = 0;
            else if (session.steps > 0) session.stalls++;
            session.lastCount = count;
            if (session.stalls >= STALLS) { finalize(groups); return; }
            // Scroll only the current eligible result tail, never the page footer/recommendations.
            var eligible = entries.filter(function (e) { return getScroller(e.slot) === session.scroller; });
            var tail = eligible.length ? eligible[eligible.length - 1].slot : null;
            if (!tail || !tail.getBoundingClientRect) { finalize(groups); return; }
            var rect = tail.getBoundingClientRect(), bounds = session.scroller === doc.documentElement || session.scroller === doc.scrollingElement || session.scroller === doc.body ?
                {top: 0, height: win.innerHeight} : session.scroller.getBoundingClientRect();
            if (!rect || !(rect.height > 0) || !bounds || !(bounds.height > 0)) { finalize(groups); return; }
            var before = scrollPosition(session.scroller);
            scrollToValue(session.scroller, Math.max(before, before + rect.bottom - bounds.top - bounds.height * 0.75));
            session.steps++; session.nextStep = time + STEP;
            queue(Math.min(STEP, session.deadline - time));
        }
        function run() {
            if (disposed || running) return;
            dirty = true;
            // Navigation/visibility must cancel automation even during a held touch.
            var definitive = isAmazon(win) && searchPath(win.location.pathname);
            var searching = definitive || state.searchHint && isSearch(win);
            state.active = searching || canShowHint();
            if (!state.active) { stopCollecting('navigation'); observer.disconnect(); removeStatus(); observe(); dirty = false; return; }
            if (doc.hidden || doc.visibilityState === 'hidden') { stopCollecting('hidden'); return; }
            var key = searchKey();
            if (lastKey !== key || !session) resetResults(key);
            if (busy()) { if (!touchActive && !pointers.size) queue(Math.max(1, IDLE - (now() - lastActivity))); return; }
            running = true; observer.disconnect();
            try {
                var groups = searching ? chooseGroups(doc) : [], entries = entriesFor(groups, false);
                if (session.phase === 'frozen') normalizeFrozen(groups, entries);
                else collect(groups, entries);
            } catch (ignored) {
                // Fail open: stop automation permanently for this search, never retry scroll on a loop.
                cancelTimer(); session.phase = 'frozen'; state.phase = 'frozen'; session.interrupted = true;
                session.text = '先読みを終了しました v' + VERSION;
                dirty = false;
                try { showStatus(session.text, [], 'frozen'); } catch (ignoredStatus) {}
            } finally { running = false; observe(); }
        }
        observer = new win.MutationObserver(function (records) {
            var relevant = records.some(function (record) {
                var target = record.target.nodeType === 1 ? record.target : record.target.parentElement;
                return !target || target.id !== STATUS && !target.closest('#' + STATUS);
            });
            if (relevant && (state.active || isSearch(win) || canShowHint())) schedule();
        });
        function navigated(event) { if (!searchPath(win.location.pathname)) state.searchHint = false; if (event && event.type === 'pageshow' && event.persisted) stopCollecting('restore'); schedule(); }
        var history = win.history;
        if (history) ['pushState', 'replaceState'].forEach(function (key) {
            var original = history[key]; if (typeof original !== 'function') return;
            try { history[key] = function () { var result = original.apply(this, arguments); navigated(); return result; }; } catch (ignored) {}
        });
        var inputEvents = ['touchstart', 'touchmove', 'touchend', 'touchcancel', 'pointerdown', 'pointerup', 'pointercancel', 'wheel', 'scroll', 'keydown'];
        inputEvents.forEach(function (type) { win.addEventListener(type, activity, {passive: true, capture: true}); });
        doc.addEventListener('visibilitychange', visibilityChanged);
        win.addEventListener('popstate', navigated); win.addEventListener('pageshow', navigated);
        function dispose() {
            disposed = true; observer.disconnect();
            generation++; cancelTimer();
            if (restoreFrame && typeof win.cancelAnimationFrame === 'function') win.cancelAnimationFrame(restoreFrame);
            restoreScrollStyles(); removeStatus();
            inputEvents.forEach(function (type) { win.removeEventListener(type, activity, {capture: true}); });
            doc.removeEventListener('visibilitychange', visibilityChanged);
            win.removeEventListener('popstate', navigated); win.removeEventListener('pageshow', navigated);
        }
        run(); return state;
    }

    if (typeof module === 'object' && module.exports) module.exports = {parseCount: parseCount, readCount: readCount, chooseGroup: chooseGroup, chooseGroups: chooseGroups, isAmazon: isAmazon, searchPath: searchPath, isSearch: isSearch, install: install};
    if (typeof window !== 'undefined') install(window);
})();
