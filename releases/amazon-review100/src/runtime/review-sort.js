(function () {
    'use strict';
    var VERSION = '1.0.0';
    var STATE = '__hiroReviewCountSort100';
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

    function isSearch(win) {
        var location = win.location;
        return location.protocol === 'https:' && HOST.test(location.hostname) && !/^\/s\/browse(?:\/|$)/.test(location.pathname) &&
            (/^\/s(?:\/|$)/.test(location.pathname) || /^\/gp\/aw\/s(?:\/|$)/.test(location.pathname) || location.pathname === '/gp/aw/search/s');
    }

    function readCount(card) {
        var labels = card.querySelectorAll('[data-cy="reviews-ratings"] [aria-label], [data-cy="reviews-block"] [aria-label], .a-icon-star-small[aria-label], a[aria-label][href*="customerReviews"], a[aria-label][href*="product-reviews"]');
        var i, n;
        for (i = 0; i < labels.length; i++) {
            if (labels[i].closest('[data-asin]') !== card) continue;
            n = parseCount(labels[i].getAttribute('aria-label'), false);
            if (n >= 0) return n;
        }
        var links = card.querySelectorAll('a[href*="customerReviews"], a[href*="product-reviews/"], [data-cy="reviews-block"] a.a-link-normal.s-underline-text.s-underline-link-text.s-link-style, [data-cy="reviews-ratings"] a.a-link-normal.s-underline-text.s-underline-link-text.s-link-style');
        for (i = 0; i < links.length; i++) {
            var link = links[i];
            if (link.closest('[data-asin]') !== card) continue;
            // Star-only anchors carry the same href; their text must fail the strict parser.
            n = parseCount(link.getAttribute('aria-label'), false);
            if (n >= 0) return n;
            if (link.querySelector('.a-icon-star-small, .a-icon-star, .a-icon-alt') || /stars?|5つ星/i.test(link.getAttribute('aria-label') || '')) continue;
            var counts = link.querySelectorAll('.s-underline-text, .a-size-small, .a-size-base');
            for (var j = 0; j < counts.length; j++) {
                if (counts[j].closest('.a-icon') || counts[j].querySelector('.a-icon-star-small, .a-icon-star, .a-icon-alt')) continue;
                n = parseCount(counts[j].textContent, true);
                if (n >= 0) return n;
            }
            n = parseCount(link.textContent, true);
            if (n >= 0) return n;
        }
        // New layouts expose a combined accessible label on a rating block.
        var blocks = card.querySelectorAll('[data-cy="reviews-ratings"], [data-cy="reviews-block"]');
        for (i = 0; i < blocks.length; i++) {
            if (blocks[i].closest('[data-asin]') !== card) continue;
            n = parseCount(blocks[i].getAttribute('aria-label'), false);
            if (n >= 0) return n;
            n = parseCount(blocks[i].textContent, false);
            if (n >= 0) return n;
        }
        return -1;
    }

    function isCard(node) {
        if (!node || node.nodeType !== 1) return false;
        var asin = node.getAttribute('data-asin');
        if (!asin || !/^[A-Za-z0-9]{10}$/.test(asin)) return false;
        if (!(node.getAttribute('data-component-type') === 's-search-result' || node.classList.contains('s-result-item') || node.tagName === 'LI')) return false;
        return !node.matches('.AdHolder, [data-component-type="sp-sponsored-result"]') &&
            !node.querySelector('[data-ad-feedback], [data-ad-feedback-label-id], .puis-sponsored-label-text, [cel_widget_id^="MAIN-FEATURED_ASINS_LIST-"]');
    }

    function chooseGroup(doc) {
        var selectors = ['.s-main-slot', '#s-results-list-atf', '#s-results-list', '#s-result-list', '.s-result-list', '[data-component-type="s-search-results"]'];
        for (var i = 0; i < selectors.length; i++) {
            var containers = doc.querySelectorAll(selectors[i]);
            for (var j = 0; j < containers.length; j++) {
                var parent = containers[j];
                if (parent.closest('.a-carousel-container, .a-carousel, [data-component-type="s-sponsored-carousel"], #rhf, #dp')) continue;
                var cards = Array.prototype.filter.call(parent.children, isCard);
                if (cards.length) return {parent: parent, cards: cards};
            }
        }
        return null;
    }

    function install(win) {
        if (!win || !win.document || !win.MutationObserver) return null;
        if (win[STATE]) { win[STATE].schedule(); return win[STATE]; }
        var doc = win.document;
        var pending = 0;
        var running = false;
        var disposed = false;
        var observer;
        var state = {version: VERSION, schedule: schedule, runNow: run, dispose: dispose};
        win[STATE] = state;

        function observe() {
            if (!disposed && doc.documentElement && isSearch(win)) observer.observe(doc.documentElement, {
                childList: true, subtree: true, characterData: true, attributes: true,
                attributeFilter: ['aria-label', 'data-asin', 'data-component-type']
            });
        }
        function schedule() {
            if (!disposed && !pending) pending = win.setTimeout(function () { pending = 0; run(); }, 80);
        }
        function removeStatus() {
            var old = doc.getElementById(STATUS);
            if (old && old.parentNode) old.parentNode.removeChild(old);
        }
        function run() {
            if (disposed || running) return;
            running = true;
            observer.disconnect();
            try {
                if (!isSearch(win)) { removeStatus(); return; }
                var group = chooseGroup(doc);
                if (!group) { removeStatus(); return; }
                var ranked = group.cards.map(function (node, index) { return {node: node, count: readCount(node), index: index}; });
                var known = ranked.filter(function (x) { return x.count >= 0; }).length;
                // With no recognized counts, don't imply success or alter the app's order.
                if (!known) { removeStatus(); return; }
                ranked.sort(function (a, b) { return b.count - a.count || a.index - b.index; });
                var changed = ranked.some(function (x, index) { return x.node !== group.cards[index]; });
                if (changed) {
                    var markers = group.cards.map(function (node) {
                        var marker = doc.createComment('hiro-review-slot');
                        group.parent.insertBefore(marker, node);
                        return marker;
                    });
                    for (var i = 0; i < ranked.length; i++) group.parent.insertBefore(ranked[i].node, markers[i]);
                    for (var m = 0; m < markers.length; m++) group.parent.removeChild(markers[m]);
                }
                var label = doc.getElementById(STATUS);
                if (!label) {
                    label = doc.createElement('div');
                    label.id = STATUS;
                    label.setAttribute('role', 'status');
                    label.setAttribute('aria-live', 'polite');
                    label.style.cssText = 'box-sizing:border-box;padding:8px 12px;font-size:12px;line-height:1.5;color:#0f1111;background:#eef6f7;border-bottom:1px solid #d5d9d9;';
                }
                var text = '評価件数順（読み込み済み ' + group.cards.length + ' 商品）';
                if (known !== group.cards.length) text += '・件数不明は末尾';
                if (label.textContent !== text) label.textContent = text;
                label.setAttribute('title', 'この検索画面で読み込まれた商品だけを評価件数の多い順に表示します。未読込の商品は含みません。');
                if (group.parent.parentNode && label.nextSibling !== group.parent) group.parent.parentNode.insertBefore(label, group.parent);
            } catch (ignored) {
                // Preserve navigation on unfamiliar layouts; a later DOM update may retry.
                removeStatus();
            } finally {
                running = false;
                observe();
            }
        }
        observer = new win.MutationObserver(schedule);
        var history = win.history;
        if (history) ['pushState', 'replaceState'].forEach(function (key) {
            var original = history[key];
            if (typeof original !== 'function') return;
            try { history[key] = function () { var result = original.apply(this, arguments); schedule(); return result; }; } catch (ignored) {}
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

    if (typeof module === 'object' && module.exports) module.exports = {parseCount: parseCount, readCount: readCount, chooseGroup: chooseGroup, isSearch: isSearch, install: install};
    if (typeof window !== 'undefined') install(window);
})();
