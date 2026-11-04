(function () {
    'use strict';
    var VERSION = '1.0.1';
    var STATE = '__hiroReviewCountSort101';
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

    function chooseGroup(doc) {
        var roots = doc.querySelectorAll(ROOTS);
        var searchLandmark = doc.querySelector('#search');
        if (searchLandmark && hidden(searchLandmark)) searchLandmark = null;
        var best = null;
        for (var r = 0; r < roots.length; r++) {
            var root = roots[r];
            if (hidden(root) || root.closest(CAROUSEL) || (searchLandmark && !searchLandmark.contains(root))) continue;
            var all = Array.prototype.filter.call(root.querySelectorAll(CARD), function (card) {
                if (!structuralCard(card) || hidden(card) || card.closest(CAROUSEL)) return false;
                var outer = card.parentElement && card.parentElement.closest(CARD);
                return !outer || !root.contains(outer);
            }).slice(0, 500);
            if (!all.length) continue;
            var counts = new WeakMap();
            all.forEach(function (card) {
                for (var p = card; p && root.contains(p); p = p.parentElement) {
                    counts.set(p, (counts.get(p) || 0) + 1);
                    if (p === root) break;
                }
            });
            var groups = [];
            all.forEach(function (card) {
                if (isSponsored(card)) return;
                var slot = card;
                while (slot.parentElement && slot.parentElement !== root && counts.get(slot.parentElement) === 1 && plainWrapper(slot.parentElement, slot)) slot = slot.parentElement;
                var parent = slot.parentElement;
                if (!parent || !root.contains(parent)) return;
                var group = null;
                for (var g = 0; g < groups.length; g++) if (groups[g].parent === parent) group = groups[g];
                if (!group) { group = {parent: parent, root: root, cards: [], slots: []}; groups.push(group); }
                group.cards.push(card);
                group.slots.push(slot);
            });
            for (var g = 0; g < groups.length; g++) {
                if (!best || groups[g].cards.length > best.cards.length) best = groups[g];
            }
            // Evaluate every recognized root; a short ATF list must not hide a larger main list.
        }
        return best;
    }

    function install(win) {
        if (!win || !win.document || !win.MutationObserver || !isAmazon(win)) return null;
        if (win[STATE]) { win[STATE].schedule(); return win[STATE]; }
        var doc = win.document;
        var pending = 0;
        var running = false;
        var disposed = false;
        var observer;
        var state = {version: VERSION, active: false, searchHint: false, schedule: schedule, runNow: run, dispose: dispose,
            setSearchHint: function (value) { state.searchHint = value === true; schedule(); }};
        win[STATE] = state;

        function observe() {
            if (!disposed && doc.documentElement && isAmazon(win)) observer.observe(doc.documentElement, {
                childList: true, subtree: true, characterData: true, attributes: true,
                attributeFilter: ['aria-label', 'data-asin', 'data-component-type', 'data-review-count', 'data-customer-review-count', 'hidden', 'style', 'aria-hidden', 'class']
            });
        }
        function schedule() {
            if (!disposed && !pending) pending = win.setTimeout(function () { pending = 0; run(); }, 80);
        }
        function removeStatus() {
            var old = doc.getElementById(STATUS);
            if (old && old.parentNode) old.parentNode.removeChild(old);
        }
        function showStatus(text, group, kind) {
            var label = doc.getElementById(STATUS);
            if (!label) {
                label = doc.createElement('div');
                label.id = STATUS;
                label.setAttribute('role', 'status');
                label.setAttribute('aria-live', 'polite');
                label.style.cssText = 'box-sizing:border-box;padding:8px 12px;font-size:12px;line-height:1.5;color:#0f1111;background:#eef6f7;border-bottom:1px solid #d5d9d9;';
            }
            if (label.textContent !== text) label.textContent = text;
            label.setAttribute('data-hiro-state', kind);
            label.setAttribute('title', 'この検索画面で読み込まれた同じ一覧の商品を評価件数の多い順に表示します。未読込の商品は含みません。');
            var anchor = group ? group.parent : doc.querySelector(ROOTS);
            if (anchor && (hidden(anchor) || anchor.closest(CAROUSEL))) anchor = null;
            if (anchor && anchor.parentNode) {
                if (label.nextSibling !== anchor) anchor.parentNode.insertBefore(label, anchor);
            } else if (doc.body && label.parentNode !== doc.body) {
                doc.body.insertBefore(label, doc.body.firstChild);
            }
        }
        function canShowHint() {
            // The native callback passes only a boolean, never URLs or search terms.
            // A hint may explain a delayed search, but cannot enable sorting.
            var path = String(win.location.pathname || '').replace(/^\/-\/[A-Za-z]{2}(?:_[A-Za-z]{2})?(?=\/)/, '');
            return state.searchHint && isAmazon(win) && homePath(path) && !doc.querySelector('#dp, #ppd, #productTitle');
        }
        function run() {
            if (disposed || running) return;
            running = true;
            observer.disconnect();
            try {
                var searching = isSearch(win);
                state.active = searching || canShowHint();
                if (!state.active) { removeStatus(); return; }
                if (!searching) { showStatus('評価件数順 v' + VERSION + '：商品一覧を確認中', null, 'waiting'); return; }
                var group = chooseGroup(doc);
                if (!group) { showStatus('評価件数順 v' + VERSION + '：商品一覧を確認中', null, 'waiting'); return; }
                var ranked = group.cards.map(function (node, index) { return {node: node, slot: group.slots[index], count: readCount(node), index: index}; });
                var known = ranked.filter(function (x) { return x.count >= 0; }).length;
                if (!known) {
                    showStatus('評価件数順 v' + VERSION + '：評価件数を取得できません（' + group.cards.length + ' 商品）', group, 'unreadable');
                    return;
                }
                ranked.sort(function (a, b) { return b.count - a.count || a.index - b.index; });
                var changed = ranked.some(function (x, index) { return x.slot !== group.slots[index]; });
                if (changed) {
                    var markers = group.slots.map(function (node) {
                        var marker = doc.createComment('hiro-review-slot');
                        group.parent.insertBefore(marker, node);
                        return marker;
                    });
                    for (var i = 0; i < ranked.length; i++) group.parent.insertBefore(ranked[i].slot, markers[i]);
                    for (var m = 0; m < markers.length; m++) group.parent.removeChild(markers[m]);
                }
                var text = '評価件数順（読み込み済み ' + group.cards.length + ' 商品） v' + VERSION;
                if (known !== group.cards.length) text += '・件数不明は末尾';
                showStatus(text, group, 'sorted');
            } catch (ignored) {
                if (state.active) showStatus('評価件数順 v' + VERSION + '：この画面を読み取れません', null, 'unsupported');
            } finally {
                running = false;
                observe();
            }
        }
        observer = new win.MutationObserver(function () {
            // Remain installed on the initial home document. A DOM-only transition
            // can then start sorting even when no onPageFinished event is fired.
            if (state.active || isSearch(win) || canShowHint()) schedule();
        });
        var history = win.history;
        if (history) ['pushState', 'replaceState'].forEach(function (key) {
            var original = history[key];
            if (typeof original !== 'function') return;
            try { history[key] = function () {
                var result = original.apply(this, arguments);
                if (!searchPath(win.location.pathname)) state.searchHint = false;
                schedule(); return result;
            }; } catch (ignored) {}
        });
        win.addEventListener('popstate', schedule);
        win.addEventListener('pageshow', schedule);
        function dispose() {
            disposed = true;
            observer.disconnect();
            if (pending) win.clearTimeout(pending);
            pending = 0;
            removeStatus();
            win.removeEventListener('popstate', schedule);
            win.removeEventListener('pageshow', schedule);
        }
        run();
        return state;
    }

    if (typeof module === 'object' && module.exports) module.exports = {parseCount: parseCount, readCount: readCount, chooseGroup: chooseGroup, isAmazon: isAmazon, searchPath: searchPath, isSearch: isSearch, install: install};
    if (typeof window !== 'undefined') install(window);
})();
