(function () {
    'use strict';
    var VERSION = '1.0.2';
    var STATE = '__hiroReviewCountSort102';
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
        var doc = win.document, IDLE = 320;
        var pending = 0, running = false, disposed = false, dirty = false;
        var observer, lastActivity = -Infinity, touchActive = false, pointers = new Set(), activityEpoch = 0;
        var cache = new WeakMap(), objectIds = new WeakMap(), nextId = 1, nextOrdinal = 1;
        var lastKey = null, lastSignature = '', expectedScroll = null, restoreFrame = 0, restoreStyles = null;
        var state = {version: VERSION, active: false, searchHint: false, schedule: schedule, runNow: run, dispose: dispose,
            setSearchHint: function (value) { var hint = value === true; if (state.searchHint !== hint) { state.searchHint = hint; schedule(); } }};
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
        function queue(delay) {
            if (!disposed && !pending) pending = win.setTimeout(function () { pending = 0; run(); }, Math.max(0, delay));
        }
        function schedule() { dirty = true; queue(80); }
        function activity(event) {
            var type = event.type;
            if (type === 'scroll' && expectedScroll && now() < expectedScroll.until) {
                var actual = scrollPosition(expectedScroll.scroller);
                if (Math.abs(actual - expectedScroll.value) < 2) { expectedScroll = null; return; }
            }
            if (type === 'touchstart' || type === 'touchmove') touchActive = !event.touches || event.touches.length > 0;
            if (type === 'touchend' || type === 'touchcancel') touchActive = !!event.touches && event.touches.length > 0;
            if (type === 'pointerdown') pointers.add(event.pointerId == null ? 0 : event.pointerId);
            if (type === 'pointerup' || type === 'pointercancel') pointers.delete(event.pointerId == null ? 0 : event.pointerId);
            lastActivity = now(); activityEpoch++;
            if (dirty && !touchActive && !pointers.size) queue(IDLE);
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
                label.style.cssText = 'box-sizing:border-box;padding:8px 12px;font-size:12px;line-height:1.5;color:#0f1111;background:#eef6f7;border-bottom:1px solid #d5d9d9;';
            }
            if (label.textContent !== text) label.textContent = text;
            if (label.getAttribute('data-hiro-state') !== kind) label.setAttribute('data-hiro-state', kind);
            var title = '読み込み済みの同じ表示形式の商品を評価件数の多い順に表示します。スクロール操作中は移動を待ち、止まってから更新します。未読込の商品は含みません。';
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
            // Pagination/ref/telemetry parameters do not invalidate still-attached products.
            // Terms/filters exist only in memory and are never logged or passed to Java.
            var params;
            try { params = new URLSearchParams(win.location.search || ''); } catch (ignored) { return ''; }
            return ['k', 'keywords', 'field-keywords', 'i', 'rh', 'bbn', 'node', 'me', 'emi'].map(function (key) {
                return key + '=' + (params.get(key) || '');
            }).join('&');
        }
        function resetResults() { cache = new WeakMap(); nextOrdinal = 1; lastSignature = ''; }
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
        function run() {
            if (disposed || running) return;
            dirty = true;
            if (busy()) { if (!touchActive && !pointers.size) queue(Math.max(1, IDLE - (now() - lastActivity))); return; }
            running = true; observer.disconnect();
            try {
                var searching = isSearch(win), key = searchKey();
                if (key !== lastKey) { resetResults(); lastKey = key; }
                state.active = searching || canShowHint();
                if (!state.active) { removeStatus(); resetResults(); dirty = false; return; }
                var groups = searching ? chooseGroups(doc) : [];
                var entries = [];
                groups.forEach(function (group) {
                    var bucket = group.joinable ? 'shape:' + group.shape : 'parent:' + objectId(group.parent);
                    group.cards.forEach(function (card, index) { entries.push(countEntry(card, group.slots[index], bucket + '|scroll:' + objectId(getScroller(group.slots[index])))); });
                });
                entries.sort(inDocumentOrder);
                var buckets = new Map(); entries.forEach(function (entry) {
                    if (!buckets.has(entry.bucket)) buckets.set(entry.bucket, []); buckets.get(entry.bucket).push(entry);
                });
                var plans = []; buckets.forEach(function (items) {
                    var ranked = items.slice().sort(function (a, b) { return b.count - a.count || a.ordinal - b.ordinal; });
                    plans.push({entries: items, ranked: ranked, changed: ranked.some(function (item, index) { return item.slot !== items[index].slot; })});
                });
                var known = entries.filter(function (entry) { return entry.count >= 0; }).length;
                var kind = !entries.length ? 'waiting' : !known ? 'unreadable' : 'sorted';
                var text = kind === 'waiting' ? '評価件数順 v' + VERSION + '：商品一覧を確認中' :
                    kind === 'unreadable' ? '評価件数順 v' + VERSION + '：評価件数を取得できません（' + entries.length + ' 商品）' :
                    '評価件数順（読み込み済み ' + entries.length + ' 商品） v' + VERSION;
                if (known && known !== entries.length) text += '・件数不明は末尾';
                if (known && buckets.size > 1) text += '・表示形式ごと';
                var signature = kind + '|' + entries.map(function (e) { return objectId(e.node) + ':' + e.asin + ':' + e.count + ':' + e.bucket; }).join(',');
                var label = doc.getElementById(STATUS), anchorTarget = statusAnchor(groups);
                var badgeChanged = !label || label.textContent !== text || label.getAttribute('data-hiro-state') !== kind || (anchorTarget && label.nextSibling !== anchorTarget);
                var moves = known > 0 && plans.some(function (plan) { return plan.changed; });
                if (signature === lastSignature && !moves && !badgeChanged) { dirty = false; return; }
                if (busy()) { queue(IDLE); return; }
                if (moves || badgeChanged) {
                    var anchor = captureAnchor(entries);
                    protectAnchor(anchor, function () {
                        if (moves) applyMoves(plans);
                        if (badgeChanged) showStatus(text, groups, kind);
                    });
                }
                lastSignature = signature; dirty = false;
            } catch (ignored) {
                // A changed or detached result tree is retried only on another meaningful event.
                dirty = false;
                if (state.active) showStatus('評価件数順 v' + VERSION + '：この画面を読み取れません', [], 'unsupported');
            } finally { running = false; observe(); }
        }
        observer = new win.MutationObserver(function (records) {
            var relevant = records.some(function (record) {
                var target = record.target.nodeType === 1 ? record.target : record.target.parentElement;
                return !target || target.id !== STATUS && !target.closest('#' + STATUS);
            });
            if (relevant && (state.active || isSearch(win) || canShowHint())) schedule();
        });
        function navigated() { if (!searchPath(win.location.pathname)) state.searchHint = false; schedule(); }
        var history = win.history;
        if (history) ['pushState', 'replaceState'].forEach(function (key) {
            var original = history[key]; if (typeof original !== 'function') return;
            try { history[key] = function () { var result = original.apply(this, arguments); navigated(); return result; }; } catch (ignored) {}
        });
        var inputEvents = ['touchstart', 'touchmove', 'touchend', 'touchcancel', 'pointerdown', 'pointerup', 'pointercancel', 'wheel', 'scroll'];
        inputEvents.forEach(function (type) { win.addEventListener(type, activity, {passive: true, capture: true}); });
        win.addEventListener('popstate', navigated); win.addEventListener('pageshow', navigated);
        function dispose() {
            disposed = true; observer.disconnect();
            if (pending) win.clearTimeout(pending); pending = 0;
            if (restoreFrame && typeof win.cancelAnimationFrame === 'function') win.cancelAnimationFrame(restoreFrame);
            restoreScrollStyles(); removeStatus();
            inputEvents.forEach(function (type) { win.removeEventListener(type, activity, {capture: true}); });
            win.removeEventListener('popstate', navigated); win.removeEventListener('pageshow', navigated);
        }
        run(); return state;
    }

    if (typeof module === 'object' && module.exports) module.exports = {parseCount: parseCount, readCount: readCount, chooseGroup: chooseGroup, chooseGroups: chooseGroups, isAmazon: isAmazon, searchPath: searchPath, isSearch: isSearch, install: install};
    if (typeof window !== 'undefined') install(window);
})();
