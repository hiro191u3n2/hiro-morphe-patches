(function () {
    'use strict';
    var VERSION = '1.0.8';
    var STATE = '__hiroReviewCountSort108';
    var STATUS = 'hiro-review-count-status', CONTROL = 'hiro-review-count-update', DEFER = 'data-hiro-review-deferred', DEFER_STYLE = 'hiro-review-deferred-style';
    var HOST = /^(?:(?:www|m|smile)\.)?amazon\.(?:co\.jp|com|co\.uk|de|fr|it|es|ca|com\.au|com\.br|com\.mx|in|nl|se|pl|com\.be|ie|sg|ae|sa|eg|com\.tr|co\.za)$/i;
    var NUMBER = '(?:[0-9]{1,3}(?:[, \\u00a0\\u202f][0-9]{3})+|[0-9]+(?:[.,][0-9]+)?)(?:万|千|[kKmM])?';
    var NAMED_COUNT = new RegExp('(?:^|[^0-9.,])(' + NUMBER + '(?:\\+)?)\\s*(?:個の評価|件の評価|件のレビュー|件のカスタマーレビュー|(?:(?:global|customer)\\s+)?(?:ratings?|reviews?)\\b)', 'i');

    function cleanNumber(text) {
        return String(text || '').replace(/[０-９]/g, function (x) { return String.fromCharCode(x.charCodeAt(0) - 65248); })
            .replace(/，/g, ',').replace(/．/g, '.').trim();
    }

    // Only use bare numbers from dedicated review-count elements, never product text.
    function parseCount(text, allowBare) {
        text = cleanNumber(text);
        // An explicit integer star prefix is not the first group of a count
        // such as "5つ星のうち4 123件の評価"; keep ordinary spaced counts valid.
        text = text.replace(/5つ星のうち\s*[0-5](?:[.,][0-9]+)?(?=$|[^0-9.,])/g, ' ');
        var named = NAMED_COUNT.exec(text);
        if (named) text = cleanNumber(named[1]);
        else if (!allowBare) return -1;
        text = text.replace(/^[（(]\s*/, '').replace(/\s*[)）]$/, '').replace(/\+$/, '').trim();
        if (/[\r\n\t\f\v]/.test(text)) return -1; // DOM text boundaries are not digit-group separators.
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
    var CANDIDATE = CARD + ', [data-asin]';
    var ROOTS = '.s-main-slot, #s-results-list-atf, #s-results-list, #s-result-list, .s-result-list, [data-component-type="s-search-results"]';
    var CAROUSEL = '.a-carousel-container, .a-carousel, .a-carousel-viewport, [data-a-carousel-options], [data-component-type="s-sponsored-carousel"], [data-component-type="s-searchgrid-carousel"], [data-component-type="sbv-video-single-product"], #rhf, #dp, #ppd';
    var REVIEW_BLOCK = '[data-cy="reviews-ratings"], [data-cy="reviews-block"]';
    var STAR = '.a-icon-star-small, .a-icon-star, .a-icon-star-mini, .a-icon-alt, [class*="a-star-"]';

    // Synthetic-layout fallback: actual app markup may omit the desktop result
    // classes and data-asin. Identity is held separately; never rewrite Amazon's
    // data-asin attributes. Only an explicit search route/native hint enables it.
    var MOBILE_EXCLUDE = CAROUSEL + ', aside, [role="complementary"], [role="dialog"], [aria-roledescription="carousel"], [data-a-carousel-options]';
    var inferredCards = new WeakMap(), inferredDocuments = new WeakMap();
    var PRODUCT_LINK = 'a[href*="/dp/"], a[href*="/gp/product/"], a[href*="/gp/aw/d/"]';
    function linkAsin(link) {
        var href = link && link.getAttribute('href');
        if (!href || /^(?:javascript|data):/i.test(href)) return null;
        // Relative links or trusted Amazon HTTPS links only. Ignore tracking
        // redirect parameters containing a product URL.
        if (/^(?:https?:)?\/\//i.test(href)) {
            var host = /^(?:https:)?\/\/([^/?#]+)(?=\/)/i.exec(href);
            if (!host || !HOST.test(host[1])) return null;
            href = href.slice(host[0].length);
        } else if (href.charAt(0) !== '/') return null;
        var match = /\/(?:dp|gp\/product|gp\/aw\/d)\/([A-Z0-9]{10})(?=([/?#]|$))/i.exec(href.split('?')[0].split('#')[0]);
        return match ? match[1].toUpperCase() : null;
    }
    function cardAsin(node) {
        var direct = node && node.getAttribute('data-asin');
        return direct && /^[A-Za-z0-9]{10}$/.test(direct) ? direct : inferredCards.get(node) || null;
    }
    function ratingEvidence(node) {
        if (node.querySelector(STAR)) return true;
        return Array.prototype.some.call(node.querySelectorAll('[aria-label]'), function (label) {
            var text = label.getAttribute('aria-label') || '';
            return parseCount(text, false) >= 0 || /(?:[0-5](?:[.,][0-9]+)?\s*(?:out of 5 stars?|stars?)|5つ星のうち\s*[0-5])/i.test(text);
        });
    }
    function mobileCountContext(node, card) {
        if (!inferredCards.has(card)) return false;
        for (var parent = node.parentElement, depth = 0; parent && parent !== card && depth < 2; parent = parent.parentElement, depth++) {
            if (parent.querySelector('h1,h2,h3,img,.a-price') || !belongs(parent, card)) return false;
            if (ratingEvidence(parent)) return true;
        }
        return false;
    }
    function discoverMobile(doc) {
        var previous = inferredDocuments.get(doc) || [];
        previous.forEach(function (node) { inferredCards.delete(node); });
        var cache = new WeakMap(), seen = new Set(), candidates = [], identities = new WeakMap();
        function discoveryHidden(node) { return hidden(node, cache) && !node.closest('[' + DEFER + '="' + VERSION + '"]'); }
        var links = Array.prototype.slice.call(doc.querySelectorAll(PRODUCT_LINK));
        if (links.length > 3000) { inferredDocuments.set(doc, []); return []; }
        function productIdentity(node) {
            if (identities.has(node)) return identities.get(node);
            var identity = null, conflict = false, title = false;
            var productLinks = Array.prototype.slice.call(node.querySelectorAll(PRODUCT_LINK));
            if (node.matches(PRODUCT_LINK)) productLinks.unshift(node);
            Array.prototype.forEach.call(productLinks, function (link) {
                if (discoveryHidden(link)) return;
                var asin = linkAsin(link);
                if (!asin) return;
                if (identity && identity !== asin) conflict = true; else identity = asin;
                if ((link.textContent || '').trim().length >= 3) title = true;
            });
            if (!title) title = Array.prototype.some.call(node.querySelectorAll('h1,h2,h3,[role="heading"]'), function (heading) { return !discoveryHidden(heading) && heading.textContent.trim().length >= 3; });
            var value = !conflict && identity && title && !!node.querySelector('img') ? identity : null;
            identities.set(node, value); return value;
        }
        function mobileWrapper(wrapper, child) {
            if (plainWrapper(wrapper, child)) return true;
            return Array.prototype.every.call(wrapper.childNodes, function (sibling) {
                if (sibling === child || sibling.nodeType === 8 || sibling.nodeType === 3 && !sibling.textContent.trim()) return true;
                if (sibling.nodeType !== 1) return false;
                if (discoveryHidden(sibling) || /^(SCRIPT|STYLE|TEMPLATE)$/.test(sibling.tagName)) return true;
                // Product-local controls are part of the row. Never carry an
                // unrelated heading, second product, or independent panel with it.
                return (sibling.matches('button,svg,[role="button"],input[type="checkbox"]')) &&
                    !sibling.querySelector('h1,h2,h3,' + PRODUCT_LINK) && !sibling.matches(PRODUCT_LINK);
            });
        }
        links.forEach(function (link) {
            if (!linkAsin(link) || discoveryHidden(link) || link.closest(MOBILE_EXCLUDE)) return;
            var chosen = null, unrated = null;
            // Include the anchor itself: some mobile variants make the entire
            // image/title/rating row one link instead of several inner links.
            for (var node = link, depth = 0; node && node !== doc.body && depth < 9; node = node.parentElement, depth++) {
                if (node.matches(ROOTS) || node.closest(MOBILE_EXCLUDE)) break;
                if (!productIdentity(node)) continue;
                if (!unrated) unrated = node;
                if (ratingEvidence(node)) { chosen = node; break; }
            }
            chosen = chosen || unrated;
            if (!chosen) return;
            var slot = chosen, asin = productIdentity(chosen);
            while (slot.parentElement && slot.parentElement !== doc.body && !slot.parentElement.matches(ROOTS) &&
                productIdentity(slot.parentElement) === asin && mobileWrapper(slot.parentElement, slot)) slot = slot.parentElement;
            if (!seen.has(slot)) { seen.add(slot); candidates.push({node: slot, asin: asin, parent: slot.parentElement}); }
        });
        // Cross-batch moves preserve complete cards. A formerly mobile root
        // can consequently contain canonical cards after sorting; recognize
        // repeated canonical peers there without requiring mobile image markup.
        Array.prototype.forEach.call(doc.querySelectorAll(CARD), function (card) {
            if (!structuralCard(card) || discoveryHidden(card) || card.closest(MOBILE_EXCLUDE) || !productEvidence(card)) return;
            for (var outer = card.parentElement; outer; outer = outer.parentElement) {
                if (structuralCard(outer)) return;
            }
            var slot = card;
            while (slot.parentElement && slot.parentElement !== doc.body && !slot.parentElement.matches(ROOTS) && plainWrapper(slot.parentElement, slot)) slot = slot.parentElement;
            if (!seen.has(slot)) { seen.add(slot); candidates.push({node: slot, asin: cardAsin(card), parent: slot.parentElement}); }
        });
        // Repeated sibling product slots are required. This prevents one hero,
        // product-detail block or an isolated recommendation becoming a list.
        var grouped = new Map();
        candidates.forEach(function (entry) {
            if (!grouped.has(entry.parent)) grouped.set(entry.parent, []);
            grouped.get(entry.parent).push(entry);
        });
        var roots = [], retained = [];
        function headedPanel(parent) {
            for (var panel = parent, depth = 0; panel && panel !== doc.body && depth < 4; panel = panel.parentElement, depth++) {
                if (panel.closest(MOBILE_EXCLUDE)) return true;
                var heading = Array.prototype.some.call(panel.children, function (child) {
                    if (child === parent || child.contains(parent) || discoveryHidden(child) || child.querySelector(PRODUCT_LINK) || productIdentity(child) || structuralCard(child) || seen.has(child)) return false;
                    var headings = child.matches('h1,h2,h3,[role="heading"]') ? [child] : child.querySelectorAll('h1,h2,h3,[role="heading"]');
                    return Array.prototype.some.call(headings, function (node) { return !/^(結果|検索結果|results|search results)$/i.test(node.textContent.trim()); });
                });
                if (heading) return true;
                if (panel.matches('main,[role="main"],#search') || panel.matches(ROOTS)) break;
            }
            return false;
        }
        grouped.forEach(function (entries, parent) {
            if (!parent || entries.length < 2 || new Set(entries.map(function (entry) { return entry.asin; })).size < 2 || headedPanel(parent)) return;
            var headed = Array.prototype.some.call(parent.children, function (child) {
                if (entries.some(function (entry) { return entry.node === child; }) || discoveryHidden(child) || productIdentity(child)) return false;
                var headings = child.matches('h1,h2,h3,[role="heading"]') ? [child] : child.querySelectorAll('h1,h2,h3,[role="heading"]');
                return Array.prototype.some.call(headings, function (heading) { return !/^(結果|検索結果|results|search results)$/i.test(heading.textContent.trim()); });
            });
            if (headed) return;
            // An unknown unrated product peer still participates as pending;
            // it must not remain mixed into the sorted known-count list.
            Array.prototype.forEach.call(parent.children, function (child) {
                if (discoveryHidden(child) || child.closest(MOBILE_EXCLUDE) || entries.some(function (entry) { return entry.node === child; })) return;
                var asin = productIdentity(child);
                if (asin) entries.push({node: child, asin: asin, parent: parent});
            });
            entries.forEach(function (entry) {
                var direct = entry.node.getAttribute('data-asin');
                if (!direct || !/^[A-Za-z0-9]{10}$/.test(direct)) inferredCards.set(entry.node, entry.asin);
                retained.push(entry.node);
            });
            roots.push(parent);
        });
        inferredDocuments.set(doc, retained);
        return roots;
    }

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

    function hidden(node, cache) {
        if (!node || node.nodeType !== 1) return false;
        if (cache && cache.has(node)) return cache.get(node);
        var invisible = node.getAttribute(DEFER) === VERSION || node.hasAttribute('hidden') ||
            !!(node.style && (node.style.display === 'none' || node.style.visibility === 'hidden' || node.style.visibility === 'collapse'));
        var view = node.ownerDocument && node.ownerDocument.defaultView;
        if (!invisible && view && typeof view.getComputedStyle === 'function') {
            var style = view.getComputedStyle(node);
            invisible = style.display === 'none' || style.visibility === 'hidden' || style.visibility === 'collapse';
        }
        invisible = invisible || hidden(node.parentElement, cache);
        if (cache) cache.set(node, invisible);
        return invisible;
    }

    function homePath(path) {
        path = String(path || '').replace(/^\/-\/[A-Za-z]{2}(?:_[A-Za-z]{2})?(?=\/)/, '');
        return /^\/gp\/(?:homepage\.html|aw\/(?:h|homepage)\.html|gw\/ajax\/mshop\.html)$/.test(path) || path === '' || path === '/' || /^\/ref=[^/]*\/?$/.test(path) || /^\/gp\/aw(?:\/|\/ref=[^/]*\/?)?$/.test(path);
    }

    function forbiddenPage(win) {
        var path = String(win.location.pathname || '').replace(/^\/-\/[A-Za-z]{2}(?:_[A-Za-z]{2})?(?=\/)/, '');
        if (/^\/(?:dp(?:\/|$)|gp\/(?:product|aw\/d|aw\/c|cart|buy|checkout|checkoutportal|your-account|your-orders|css)(?:\/|$)|hz\/(?:cart|checkout)(?:\/|$)|cart(?:\/|$)|checkout(?:\/|$)|ap(?:\/|$)|your-orders(?:\/|$))/.test(path)) return true;
        var doc = win.document;
        return !!doc && Array.prototype.some.call(doc.querySelectorAll('#dp, #ppd, #productTitle'), function (node) { return !hidden(node); });
    }
    function productEvidence(node) {
        return !!node.querySelector('h2, [data-cy="title-recipe"], .a-price, [data-cy="reviews-block"], [data-cy="reviews-ratings"], a[href*="/dp/"], a[href*="/gp/product/"], a[href*="customerReviews"]');
    }
    function isSearch(win, nativeHint) {
        if (!isAmazon(win) || forbiddenPage(win)) return false;
        if (searchPath(win.location.pathname)) return true;
        var doc = win.document;
        if (!doc) return false;
        // A verified native search navigation may retain a home/gateway URL.
        // It still needs a visible canonical list; a hint alone never ranks content.
        var cache = new WeakMap();
        function visibleList(root) {
            if (hidden(root, cache) || root.closest(CAROUSEL)) return false;
            return Array.prototype.some.call(root.querySelectorAll(CANDIDATE), function (card) {
                return !hidden(card, cache) && !card.closest(CAROUSEL) && (structuralCard(card) || productEvidence(card));
            });
        }
        if (nativeHint === true && (Array.prototype.some.call(doc.querySelectorAll(ROOTS), visibleList) || discoverMobile(doc).length > 0)) return true;
        // Without a native hint, retain the stronger visible search landmark proof.
        return Array.prototype.some.call(doc.querySelectorAll('#search'), function (search) {
            return !hidden(search, cache) && !search.closest('[aria-hidden="true"]') && Array.prototype.some.call(search.querySelectorAll(ROOTS), visibleList);
        });
    }

    function belongs(node, card) {
        if (!node || !card.contains(node) || node.closest(CAROUSEL)) return false;
        var asin = cardAsin(card);
        for (var p = node; p && p !== card; p = p.parentElement) {
            // Generic same-ASIN metadata wrappers do not own another product.
            // Valid native result boundaries retain the existing narrow fallback.
            if (p.matches && p.matches(CARD)) return false;
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
        if (mobileCountContext(node, card)) return true;
        var row = node.closest('.a-row');
        if (!row || !belongs(row, card)) return false;
        var stars = row.querySelectorAll(STAR);
        for (var i = 0; i < stars.length; i++) if (belongs(stars[i], card)) return true;
        return false;
    }

    function readCount(card, nestedFallback) {
        var i, n, exact = -1, approximate = -1;
        var reviewLink = 'a[href*="customerReviews"], a[href*="product-reviews/"]';
        var unrelated = 'h1, h2, h3, [data-cy="title-recipe"], .a-price';
        var visibility = new WeakMap();
        function countHidden(node) {
            if (!node || node.nodeType !== 1) return false;
            if (visibility.has(node)) return visibility.get(node);
            // aria-hidden suppresses duplicate accessibility announcements; it
            // does not hide a visually displayed count. Inspect actual display.
            var invisible = node.hasAttribute('hidden') || !!(node.style &&
                (node.style.display === 'none' || node.style.visibility === 'hidden' || node.style.visibility === 'collapse'));
            var view = node.ownerDocument && node.ownerDocument.defaultView;
            if (!invisible && view && typeof view.getComputedStyle === 'function') {
                var style = view.getComputedStyle(node);
                invisible = style.display === 'none' || style.visibility === 'hidden' || style.visibility === 'collapse';
            }
            invisible = invisible || countHidden(node.parentElement);
            visibility.set(node, invisible);
            return invisible;
        }
        function eligible(node) {
            return belongs(node, card) && !countHidden(node) && !node.closest(unrelated);
        }
        function hasStars(node) {
            if (node.querySelector(STAR) || /stars?|5つ星/i.test(node.getAttribute('aria-label') || '')) return true;
            var labels = node.querySelectorAll('[aria-label]');
            for (var i = 0; i < labels.length; i++) {
                if (eligible(labels[i]) && /stars?|5つ星/i.test(labels[i].getAttribute('aria-label') || '')) return true;
            }
            return false;
        }
        function remember(text, allowBare) {
            var value = parseCount(text, allowBare);
            if (value < 0) return value;
            var token = cleanNumber(text), named = NAMED_COUNT.exec(token);
            if (named) token = named[1];
            if (/[万千kKmM+]/.test(token)) {
                if (approximate < 0) approximate = value;
            } else if (exact < 0) exact = value;
            return value;
        }
        // Keep adjacent DOM text separate: a star score "4" followed by
        // "1234件の評価" must never become the count 41234. Skip hidden/foreign
        // descendants even when reading a whole review block or hyperlink.
        function ownedText(node) {
            if (node.nodeType === 3) return node.textContent;
            if (node.nodeType !== 1 || !eligible(node) || node.matches(STAR) || node.closest('.a-icon')) return '';
            var parts = [];
            for (var child = node.firstChild; child; child = child.nextSibling) {
                var text = ownedText(child);
                if (text.trim()) parts.push(text);
            }
            return parts.join('\n');
        }
        // Exact accessible labels take precedence over rounded visible counts.
        var labels = card.querySelectorAll('[aria-label], [data-review-count], [data-customer-review-count]');
        for (i = 0; i < labels.length; i++) {
            var label = labels[i];
            if (!eligible(label) || label.querySelector(unrelated)) continue;
            remember(label.getAttribute('aria-label'), false);
            if (label.hasAttribute('data-review-count')) remember(label.getAttribute('data-review-count'), true);
            if (label.hasAttribute('data-customer-review-count')) remember(label.getAttribute('data-customer-review-count'), true);
        }
        if (exact >= 0) return exact;
        var blocks = card.querySelectorAll(REVIEW_BLOCK);
        for (i = 0; i < blocks.length; i++) {
            if (!eligible(blocks[i])) continue;
            var leaves = blocks[i].querySelectorAll('*');
            for (var k = 0; k < leaves.length; k++) {
                var leaf = leaves[k];
                if (leaf.children.length || !eligible(leaf) || leaf.matches(STAR) || leaf.closest('.a-icon')) continue;
                remember(leaf.textContent, false);
            }
        }
        if (exact >= 0) return exact;
        // Compact mobile counts may be plain parenthesized spans beside stars.
        // Presentation-only spans still need a review block/link or star row.
        var compact = card.querySelectorAll('.s-underline-text, span.a-size-mini.puis-normal-weight-text, span' + (inferredCards.has(card) ? ', small, div' : ''));
        for (i = 0; i < compact.length; i++) {
            var countNode = compact[i];
            if (!eligible(countNode) || countNode.querySelector(unrelated) || !reviewContext(countNode, card) || countNode.matches(STAR) || countNode.querySelector(STAR) || countNode.closest('.a-icon')) continue;
            var enclosingLink = countNode.closest('a');
            if (enclosingLink && !enclosingLink.matches(reviewLink) && !countNode.closest(REVIEW_BLOCK) && !(inferredCards.has(card) && linkAsin(enclosingLink) === cardAsin(card) && mobileCountContext(countNode, card))) continue;
            var countText = ownedText(countNode), parenthesized = /^[（(].*[)）]$/.test(countText.trim());
            if (!countNode.matches('.s-underline-text, span.a-size-mini.puis-normal-weight-text') && !parenthesized) {
                var reviewBlock = countNode.closest(REVIEW_BLOCK);
                var bare = parseCount(countText, true);
                if (!reviewBlock || !eligible(reviewBlock) || !hasStars(reviewBlock) || countNode.children.length || bare <= 5) continue;
            }
            n = parseCount(countText, true);
            if (n >= 0 && n <= 5 && !parenthesized) {
                var countLink = countNode.closest(reviewLink);
                if (!countLink || hasStars(countLink)) continue;
            }
            if (n >= 0) remember(countText, true);
        }
        var links = card.querySelectorAll(reviewLink);
        for (i = 0; i < links.length; i++) {
            var link = links[i];
            if (!eligible(link)) continue;
            var hasStar = hasStars(link);
            var counts = link.querySelectorAll('.a-size-small, .a-size-base, .a-size-mini');
            for (var j = 0; j < counts.length; j++) {
                var span = counts[j];
                if (!eligible(span) || span.querySelector(unrelated) || span.matches(STAR) || span.querySelector(STAR) || span.closest('.a-icon')) continue;
                var spanText = ownedText(span);
                n = parseCount(spanText, true);
                // 0-5 can be an integer star score; larger validated counts are
                // unambiguous even without parentheses in a shared review link.
                if (hasStar && n <= 5 && !/^[（(].*[)）]$/.test(spanText.trim())) continue;
                if (n >= 0) remember(spanText, true);
            }
            if (!hasStar) remember(ownedText(link), true);
        }
        for (i = 0; i < blocks.length; i++) {
            if (eligible(blocks[i])) remember(ownedText(blocks[i]), false);
        }
        if (exact >= 0 || approximate >= 0) return exact >= 0 ? exact : approximate;
        // A sole immediate same-product result wrapper may carry the visible count.
        // Never override an outer value, choose among duplicates, or recurse past one wrapper.
        if (!nestedFallback) {
            var inner = Array.prototype.filter.call(card.querySelectorAll(CARD), function (node) {
                var parent = node.parentElement;
                while (parent && parent !== card && !structuralCard(parent)) parent = parent.parentElement;
                return parent === card && !countHidden(node) && !node.closest(CAROUSEL);
            });
            if (inner.length === 1 && structuralCard(inner[0]) && cardAsin(inner[0]) === cardAsin(card) && !isSponsored(inner[0])) {
                var foreign = false;
                for (var parent = inner[0].parentElement; parent && parent !== card; parent = parent.parentElement) {
                    if (parent.getAttribute('data-asin') && parent.getAttribute('data-asin') !== cardAsin(card)) foreign = true;
                }
                if (!foreign) return readCount(inner[0], true);
            }
        }
        return -1;
    }

    function structuralCard(node) {
        if (!node || node.nodeType !== 1 || !node.matches(CANDIDATE) && !inferredCards.has(node)) return false;
        var asin = cardAsin(node);
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
            if (node.nodeType === 1 && (/^(SCRIPT|STYLE|TEMPLATE)$/.test(node.tagName) || hidden(node))) continue;
            return false;
        }
        return true;
    }

    function presentation(node) {
        // Compatibility is determined by actual flow, not cosmetic parent tag
        // names/roles. A SECTION and DIV can hold identical vertically stacked
        // product rows, including append batches outside the first #search node.
        var view = node.ownerDocument && node.ownerDocument.defaultView, computed = null;
        if (view && typeof view.getComputedStyle === 'function') computed = view.getComputedStyle(node);
        var display = computed && computed.display || 'block';
        if (/^(block|flow-root|list-item)$/.test(display)) return 'block-flow';
        if (display === 'flex' || display === 'inline-flex') return 'flex:' + (computed.flexDirection || 'row') + ':' + (computed.flexWrap || 'nowrap');
        if (display === 'grid' || display === 'inline-grid') return 'grid:' + (computed.gridTemplateColumns || 'none') + ':' + (computed.gridAutoFlow || 'row');
        return display;
    }

    function chooseGroups(doc, includeSponsored, maximum, allowMobile) {
        var limit = maximum === 100 ? 100 : 1000;
        var mobileRoots = allowMobile ? discoverMobile(doc) : [];
        var mobileNodes = allowMobile ? inferredDocuments.get(doc) || [] : [];
        var roots = Array.prototype.slice.call(doc.querySelectorAll(ROOTS));
        mobileRoots.forEach(function (root) { if (roots.indexOf(root) < 0) roots.push(root); });
        var visibility = new WeakMap();
        var landmarks = Array.prototype.filter.call(doc.querySelectorAll('#search'), function (node) { return !hidden(node, visibility); });
        roots = roots.filter(function (root) {
            return !hidden(root, visibility) && !root.closest(CAROUSEL) && (allowMobile || !landmarks.length || landmarks.some(function (node) { return node.contains(root); }));
        });
        // Outer recognized lists already cover their nested row wrappers.
        roots = roots.filter(function (root) {
            return !roots.some(function (other) { return other !== root && other.contains(root); });
        });
        var rootCount = Math.min(100, roots.length), overflow = roots.length > 32; roots = roots.slice(0, 32);
        var found = [], seen = new Set(), unrecognized = false;
        roots.forEach(function (root) {
            if (seen.size >= limit) { if (root.querySelector(CANDIDATE)) overflow = true; return; }
            var candidates = Array.prototype.slice.call(root.querySelectorAll(CANDIDATE));
            mobileNodes.forEach(function (node) { if (root.contains(node) && candidates.indexOf(node) < 0) candidates.push(node); });
            var all = candidates.filter(function (card) {
                if (!structuralCard(card) || hidden(card, visibility) || card.closest(CAROUSEL) || card.closest('[' + DEFER + '="' + VERSION + '"]')) return false;
                for (var outer = card.parentElement; outer && outer !== root; outer = outer.parentElement) {
                    if (structuralCard(outer)) return false;
                }
                return true;
            });
            Array.prototype.forEach.call(root.querySelectorAll(CARD), function (node) {
                if (structuralCard(node) || hidden(node, visibility) || node.closest(CAROUSEL) || !productEvidence(node)) return;
                if (!all.some(function (card) { return node.contains(card); })) unrecognized = true;
            });
            if (all.length > limit - seen.size) overflow = true;
            all = all.slice(0, limit - seen.size);
            var counts = new WeakMap();
            all.forEach(function (card) {
                for (var p = card; p && root.contains(p); p = p.parentElement) {
                    counts.set(p, (counts.get(p) || 0) + 1);
                    if (p === root) break;
                }
            });
            var groups = [];
            all.forEach(function (card) {
                if (seen.has(card) || !includeSponsored && isSponsored(card)) return;
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
                    if (hidden(child, visibility) || group.slots.indexOf(child) >= 0 || structuralCard(child) && isSponsored(child) || /^(SCRIPT|STYLE|TEMPLATE)$/.test(child.tagName)) return false;
                    var headings = child.matches('h1, h2, h3, [role="heading"]') ? [child] : child.querySelectorAll('h1, h2, h3, [role="heading"]');
                    return Array.prototype.some.call(headings, function (heading) { return !/^(結果|検索結果|results|search results)$/i.test(heading.textContent.trim()); });
                });
                group.joinable = !divided && (group.parent === root || Array.prototype.every.call(group.parent.children, function (child) {
                    return hidden(child, visibility) || group.slots.indexOf(child) >= 0 || structuralCard(child) && isSponsored(child) || /^(SCRIPT|STYLE|TEMPLATE)$/.test(child.tagName);
                }));
                group.shape = presentation(group.parent);
                group.priority = divided ? 0 : group.root.matches('.s-main-slot, #s-results-list-atf, #s-results-list, #s-result-list') ? 3 : group.joinable ? 2 : 1;
                found.push(group);
            });
        });
        found.rootCount = rootCount;
        if (overflow) found.overflow = true;
        if (unrecognized) found.unrecognized = true;
        return found;
    }

    // Kept for diagnostics/tests of a single layout; sorting uses every compatible group.
    function chooseGroup(doc) {
        return chooseGroups(doc).sort(function (a, b) { return b.cards.length - a.cards.length; })[0] || null;
    }

    function install(win) {
        if (!win || !win.document || !win.MutationObserver || !isAmazon(win)) return null;
        if (win[STATE]) { win[STATE].schedule(); return win[STATE]; }
        function allowMobile() { return !nativeSuspended && isAmazon(win) && !forbiddenPage(win) && (state.searchHint || searchPath(win.location.pathname)); }
        var doc = win.document, IDLE = 320, TARGET = 100, BUDGET = 20000, STEP = 800, MAX_STEPS = 30, STALLS = 4, READY = 4000, QUIET = 800;
        var pending = 0, pendingAt = Infinity, running = false, disposed = false, dirty = false, generation = 0;
        var observer, lastActivity = -Infinity, touchActive = false, pointers = new Set(), activityEpoch = 0;
        var cache = new WeakMap(), objectIds = new WeakMap(), nextId = 1, nextOrdinal = 1;
        var lastKey = null, expectedScroll = null, writingScroll = false, restoreFrame = 0, restoreStyles = null, session = null, tokenAliases = new Map(), deferred = new Map(), passAnchor = null, changedCards = new Set(), broadChange = false, domIdentity = null, nextDomIdentity = 1, lastError = null, activationPending = false, nativeSuspended = false, suspendedNavigation = null;
        var state = {version: VERSION, active: false, searchHint: false, hintToken: null, phase: 'waiting',
            schedule: schedule, runNow: run, dispose: dispose, refresh: refresh, report: report,
            setNativeCheckpoint: function (version, route) {
                if (version !== VERSION || ['search', 'home', 'other'].indexOf(route) < 0) return false;
                state.nativeCheckpoint = {version: VERSION, route: route}; return true;
            },
            setSearchHint: function (value, token) {
                var hint = value === true, safeToken = typeof token === 'string' && /^[a-zA-Z0-9]{1,64}$/.test(token) ? token : null;
                // n0 is reserved by the native search fragment's onHidden/empty
                // query lifecycle. A stale /s URL must not reactivate home cards.
                if (!hint && safeToken === 'n0') {
                    nativeSuspended = true; suspendedNavigation = navigationIdentity();
                    state.searchHint = false; state.hintToken = null; state.active = false; activationPending = false;
                    observer.disconnect(); stopCollecting('native-hidden'); cancelTimer(); restoreDeferred(true); removeStatus(); observe();
                    dirty = false; return;
                }
                if (hint && /^n[0-9a-f]+$/i.test(safeToken || '') && safeToken !== 'n0') {
                    if (nativeSuspended) lastKey = null;
                    nativeSuspended = false; suspendedNavigation = null;
                } else if (nativeSuspended) {
                    if (navigationIdentity() === suspendedNavigation) return;
                    nativeSuspended = false; suspendedNavigation = null; lastKey = null;
                }

                if (hint && safeToken && !/^n[0-9a-f]+$/i.test(safeToken) && searchPath(win.location.pathname)) {
                    var canonical = searchKey();
                    // Native callback verifies this token against the current real search URL.
                    // Adopting that alias does not restart the same home-URL SPA cohort.
                    if (lastKey === 'native:' + safeToken) lastKey = canonical;
                    tokenAliases.set(safeToken, canonical);
                    if (tokenAliases.size > 32) tokenAliases.delete(tokenAliases.keys().next().value);
                }
                if (hint && safeToken && (lastKey === 'native:pending' || domIdentity)) {
                    lastKey = tokenAliases.get(safeToken) || 'native:' + safeToken; domIdentity = null;
                }
                if (state.searchHint !== hint || state.hintToken !== safeToken) {
                    state.searchHint = hint;
                    activationPending = hint && !state.active && !disposed && isAmazon(win) && !forbiddenPage(win);
                    if (activationPending) state.phase = 'waiting';
                    if (safeToken) state.hintToken = safeToken;
                    schedule();
                }
            }};
        win[STATE] = state;
        function errorName(error) {
            var name;
            try { name = error && error.name; } catch (ignored) {}
            return ['TypeError', 'ReferenceError', 'SyntaxError', 'RangeError', 'Error'].indexOf(name) >= 0 ? name : 'Error';
        }
        function report() {
            // Passive, fixed-schema observation. No identities, URLs, page text,
            // class names, exception messages, DOM writes or scheduled work escape.
            var result = {version: VERSION, phase: disposed ? 'disposed' : activationPending ? 'waiting' : !state.active ? 'inactive' : lastError ? 'error' : session && session.failed ? 'unreadable' : state.phase,
                roots: 0, cards: 0, known: 0, status: false, error: state.active ? lastError : null, limited: false};
            if (result.phase === 'finalizing') result.phase = 'ready';
            if (['inactive', 'waiting', 'collecting', 'ready', 'frozen', 'awaiting', 'unreadable', 'disposed', 'error'].indexOf(result.phase) < 0) result.phase = 'error';
            try {
                result.status = !!doc.getElementById(STATUS);
                if (disposed) return result;
                if (!isAmazon(win) || forbiddenPage(win)) { result.phase = 'inactive'; result.error = null; return result; }
                var groups = chooseGroups(doc, true, 100, allowMobile());
                result.roots = Math.min(100, groups.rootCount || 0); result.limited = !!groups.overflow;
                groups.forEach(function (group) {
                    group.cards.forEach(function (card) {
                        if (result.cards >= 100) { result.limited = true; return; }
                        result.cards++; if (readCount(card) >= 0) result.known++;
                    });
                });
            } catch (error) { result.phase = 'error'; result.error = errorName(error); }
            return result;
        }
        function now() { return win.performance && typeof win.performance.now === 'function' ? win.performance.now() : Date.now(); }
        function objectId(node) { if (!objectIds.has(node)) objectIds.set(node, nextId++); return objectIds.get(node); }
        function busy() { return touchActive || pointers.size > 0 || now() - lastActivity < IDLE; }
        function observe() {
            if (!disposed && doc.documentElement && isAmazon(win)) observer.observe(doc.documentElement, {
                childList: true, subtree: true, characterData: true, attributes: true,
                attributeFilter: ['aria-label', 'data-asin', 'data-component-type', 'data-review-count', 'data-customer-review-count', 'hidden', 'style', 'aria-hidden', 'class', 'href']
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
            if (session.phase !== 'ready') session.phase = 'finalizing'; session.interrupted = true; session.reason = reason;
            state.phase = session.phase; expectedScroll = null; generation++; cancelTimer(); dirty = true;
        }
        function activity(event) {
            var type = event.type;
            if (event.target && event.target.closest && event.target.closest('#' + CONTROL)) {
                if (type === 'pointerup' || type === 'pointercancel') pointers.delete(event.pointerId == null ? 0 : event.pointerId);
                if (type === 'touchend' || type === 'touchcancel') touchActive = !!event.touches && event.touches.length > 0;
                if (dirty && !touchActive && !pointers.size) queue(IDLE);
                return;
            }
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
            [STATUS, CONTROL].forEach(function (id) { var old = doc.getElementById(id); if (old && old.parentNode) old.parentNode.removeChild(old); });
        }
        function restoreDeferred(clear) {
            deferred.forEach(function (held, node) {
                if (node.getAttribute(DEFER) === VERSION) {
                    if (held.original == null) node.removeAttribute(DEFER); else node.setAttribute(DEFER, held.original);
                }
            });
            if (clear) deferred.clear();
        }
        function hideEntry(entry, reason) {
            var slot = entry.slot, held = deferred.get(slot);
            if (!held) { held = {original: slot.getAttribute(DEFER), asin: entry.asin, reason: reason}; deferred.set(slot, held); }
            held.reason = reason;
        }
        function applyDeferred() {
            var style = doc.getElementById(DEFER_STYLE);
            if (!style) { style = doc.createElement('style'); style.id = DEFER_STYLE; style.textContent = '[' + DEFER + '="' + VERSION + '"]{display:none!important}'; (doc.head || doc.documentElement).appendChild(style); }
            deferred.forEach(function (held, node) {
                if (!node.isConnected) {
                    if (node.getAttribute(DEFER) === VERSION) { if (held.original == null) node.removeAttribute(DEFER); else node.setAttribute(DEFER, held.original); }
                    deferred.delete(node); return;
                }
                if (node.getAttribute(DEFER) !== VERSION) node.setAttribute(DEFER, VERSION);
            });
        }
        function updateControl(groups) {
            var pendingCount = 0, excluded = 0;
            deferred.forEach(function (held, node) {
                if (!node.isConnected) {
                    if (node.getAttribute(DEFER) === VERSION) { if (held.original == null) node.removeAttribute(DEFER); else node.setAttribute(DEFER, held.original); }
                    deferred.delete(node); return;
                }
                if (held.reason === 'layout' || held.reason === 'sponsored') excluded++; else pendingCount++;
            });
            var control = doc.getElementById(CONTROL);
            if (!control) {
                control = doc.createElement('div'); control.id = CONTROL;
                control.style.cssText = 'padding:12px;background:#eef6f7;font-size:12px;line-height:1.5;';
                var summary = doc.createElement('span'); summary.setAttribute('data-hiro-summary', ''); control.appendChild(summary);
                var button = doc.createElement('button'); button.type = 'button'; button.textContent = '件数を再確認する';
                button.style.cssText = 'display:block;margin-top:8px;padding:8px 12px;'; button.addEventListener('click', refresh); control.appendChild(button);
            }
            var message = session.failed ? '件数を確認できたら自動更新します。未確認 ' + (session.failedCount || 0) + ' 件' : '追加・変更分は自動で並べ替えます。件数未確認 ' + pendingCount + ' 件';
            if (excluded) message += '／対象外の表示・広告 ' + excluded + ' 件';
            var textNode = control.querySelector('[data-hiro-summary]'); if (textNode.textContent !== message) textNode.textContent = message;
            var button = control.querySelector('button'); var disabled = session.phase === 'ready' || !pendingCount && !session.failed; if (button.disabled !== disabled) button.disabled = disabled;
            var roots = (groups || []).filter(function (g) { return g.priority > 0; }).map(function (g) { return g.root; });
            var tail = roots.length ? roots[roots.length - 1] : session && session.lastRoot;
            if (tail && tail.parentNode) { session.lastRoot = tail; if (tail.nextSibling !== control) tail.parentNode.insertBefore(control, tail.nextSibling); }
            else if (doc.body && control.parentNode !== doc.body) doc.body.appendChild(control);
        }
        function refresh() {
            if (disposed || !session || session.phase !== 'frozen' || !state.active) return;
            generation++; cancelTimer(); cache = new WeakMap(); nextOrdinal = 1;
            session.phase = 'ready'; session.readyUntil = now() + READY; session.readySignature = null; session.stableSince = now();
            session.manual = true; session.auto = false; session.interrupted = true;
            state.phase = 'ready'; var button = doc.querySelector('#' + CONTROL + ' button'); if (button) button.disabled = true; dirty = true; schedule();
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
            var fixed = kind === 'collecting' || kind === 'pending' || kind === 'unreadable' || kind === 'ready' && session && !session.manual;
            var css = 'box-sizing:border-box;padding:8px 12px;font-size:12px;line-height:1.5;color:#0f1111;background:#eef6f7;border-bottom:1px solid #d5d9d9;' +
                (fixed ? 'position:fixed;bottom:0;left:0;right:0;z-index:2147483646;pointer-events:none;' : '');
            if (label.getAttribute('data-hiro-style') !== kind) { label.style.cssText = css; label.setAttribute('data-hiro-style', kind); }
            var title = '最初に約100商品を最大20秒先読みし、取得済みの通常検索結果を評価件数順に並べます。追加・変更分も操作終了後に自動更新し、件数未確認の商品は判定でき次第追加します。';
            if (label.getAttribute('title') !== title) label.setAttribute('title', title);
            var anchor = statusAnchor(groups);
            if (fixed && doc.body) {
                if (label.parentNode !== doc.body) doc.body.appendChild(label);
            } else if (anchor && anchor.parentNode) {
                if (label.nextSibling !== anchor) anchor.parentNode.insertBefore(label, anchor);
            } else if (doc.body && label.parentNode !== doc.body) doc.body.insertBefore(label, doc.body.firstChild);
        }
        function canShowHint() {
            return state.searchHint && isAmazon(win) && !forbiddenPage(win);
        }
        function navigationIdentity() {
            var pairs = [];
            try {
                new URLSearchParams(win.location.search || '').forEach(function (value, key) {
                    if (/^(?:k|keyword|keywords|field-keywords|i|rh|bbn|node|me|emi)$/.test(key) || /^p_/.test(key)) pairs.push([key, value]);
                });
            } catch (ignored) {}
            pairs.sort(function (a, b) { return a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : a[1] < b[1] ? -1 : a[1] > b[1] ? 1 : 0; });
            return String(win.location.hostname || '') + String(win.location.pathname || '') + JSON.stringify(pairs);
        }
        function searchKey() {
            // n+hex identifies an actual native search submission, independent
            // of a stale WebView URL. URL-only notification tokens retain their
            // original canonical-query aliases. Pagination never resets ranks.
            var authoritative = state.searchHint && /^n[0-9a-f]+$/i.test(state.hintToken || '') ? state.hintToken : null;
            if (!searchPath(win.location.pathname)) return authoritative ? 'native:' + authoritative + '|filters:[]' : domIdentity || (state.hintToken ? tokenAliases.get(state.hintToken) || 'native:' + state.hintToken : lastKey || 'native:pending');
            var params;
            try { params = new URLSearchParams(win.location.search || ''); } catch (ignored) { return authoritative ? 'native:' + authoritative + '|filters:[]' : 'url:'; }
            var pairs = [];
            params.forEach(function (value, key) {
                if (/^(?:k|keyword|keywords|field-keywords|i|rh|bbn|node|me|emi)$/.test(key) || /^p_/.test(key)) {
                    if (!authoritative || !/^(?:k|keyword|keywords|field-keywords)$/.test(key)) pairs.push([key, value]);
                }
            });
            var canonical = JSON.stringify(pairs.sort(function (a, b) { return a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : a[1] < b[1] ? -1 : a[1] > b[1] ? 1 : 0; }));
            return authoritative ? 'native:' + authoritative + '|filters:' + canonical : 'url:' + canonical;
        }

        function resetResults(key) {
            if (key.indexOf('dom:') !== 0) domIdentity = null;
            lastError = null;
            restoreDeferred(true); removeStatus(); generation++; cancelTimer(); cache = new WeakMap(); nextOrdinal = 1; lastKey = key; expectedScroll = null;
            session = {phase: 'waiting', started: now(), deadline: now() + BUDGET, auto: false, interrupted: false,
                steps: 0, stalls: 0, lastCount: 0, nextStep: now(), ranks: new Map(), nodeRanks: new WeakMap(), nextRank: 0};
            state.phase = session.phase;
        }
        function countEntry(card, slot, bucket) {
            var asin = cardAsin(card), held = cache.get(card), value = readCount(card);
            if (!held || held.asin !== asin) held = {asin: asin, value: -1, ordinal: nextOrdinal++};
            held.value = value;
            cache.set(card, held);
            return {node: card, slot: slot, parent: slot.parentNode, count: held.value, ordinal: held.ordinal, bucket: bucket, asin: asin};
        }
        function inDocumentOrder(entries) {
            var selected = new Set(entries.map(function (entry) { return entry.node; })), positions = new Map(), index = 0;
            Array.prototype.forEach.call(doc.querySelectorAll('*'), function (node) { if (selected.has(node)) positions.set(node, index++); });
            return entries.sort(function (a, b) { return positions.get(a.node) - positions.get(b.node); });
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
        function validPlacements(plans) {
            return plans.every(function (plan) {
                return plan.ranked.every(function (entry, index) {
                    var parent = plan.entries[index].parent, tag = parent && parent.tagName, item = entry.slot.tagName;
                    // Combining ordinary block sections must not put DIV/A rows
                    // inside lists or move LI rows outside their required list.
                    if (tag === 'UL' || tag === 'OL') return item === 'LI';
                    if (item === 'LI') return false;
                    if (tag === 'TABLE') return /^(CAPTION|COLGROUP|THEAD|TBODY|TFOOT)$/.test(item);
                    if (/^(THEAD|TBODY|TFOOT)$/.test(tag)) return item === 'TR';
                    if (tag === 'TR') return item === 'TD' || item === 'TH';
                    if (tag === 'SELECT') return item === 'OPTION' || item === 'OPTGROUP';
                    return true;
                });
            });
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
                    var entry = counts ? countEntry(card, slot, key) : {node: card, slot: slot, parent: slot.parentNode, asin: cardAsin(card), bucket: key};
                    var style = typeof win.getComputedStyle === 'function' ? win.getComputedStyle(slot) : null;
                    entry.unsupported = !!(style && (/^(absolute|fixed)$/.test(style.position || '') || Number(style.order || 0) !== 0));
                    var parentStyle = typeof win.getComputedStyle === 'function' ? win.getComputedStyle(slot.parentElement) : null;
                    if (parentStyle && /reverse/.test(parentStyle.flexDirection || '')) entry.unsupported = true;
                    entry.sponsored = isSponsored(card); entry.priority = group.priority; entry.root = group.root; entries.push(entry);
                });
            });
            return inDocumentOrder(entries);
        }
        function uniqueCount(entries) { return new Set(entries.filter(function (e) { return !e.sponsored; }).map(function (e) { return e.asin; })).size; }
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
            session.nodeRanks.set(entry.node, {asin: entry.asin, rank: rank, count: entry.count});
            if (!session.ranks.has(entry.asin)) session.ranks.set(entry.asin, []);
            session.ranks.get(entry.asin).push({rank: rank, count: entry.count}); entry.rank = rank;
        }
        function selectBucket(entries) {
            var buckets = new Map();
            entries.forEach(function (entry) {
                if (entry.sponsored || entry.unsupported || entry.priority <= 0) return;
                var held = buckets.get(entry.bucket) || {key: entry.bucket, priority: entry.priority, size: 0}; held.size++; buckets.set(entry.bucket, held);
            });
            var choices = Array.from(buckets.values()).sort(function (a, b) { return b.priority - a.priority || b.size - a.size; });
            return choices.length ? choices[0].key : null;
        }
        function verifyVisible(entries) {
            var previous = Infinity, count = 0, visible = [];
            inDocumentOrder(entries.slice()).forEach(function (entry) {
                if (entry.slot.getAttribute(DEFER) === VERSION || hidden(entry.node)) return;
                var current = readCount(entry.node);
                if (current < 0 || current !== entry.count || current > previous) { hideEntry(entry, 'changed'); return; }
                previous = current; count++; visible.push(entry);
            });
            // DOM order alone is insufficient for positioned/CSS-reordered cards.
            // Check actual row/column positions only when every rectangle is usable
            // and distinct. Missing/collapsed geometry is not treated as evidence.
            var measured = visible.map(function (entry) {
                var rect = entry.slot.getBoundingClientRect && entry.slot.getBoundingClientRect();
                return rect && rect.width > 0 && rect.height > 0 && isFinite(rect.top) && isFinite(rect.left) ? {entry: entry, top: rect.top, left: rect.left} : null;
            });
            if (measured.length > 1 && measured.every(function (item) { return item !== null; })) {
                var direction = doc.documentElement.getAttribute('dir') || '';
                if (typeof win.getComputedStyle === 'function') direction = win.getComputedStyle(visible[0].root).direction || direction;
                measured.sort(function (a, b) { return Math.abs(a.top - b.top) > 2 ? a.top - b.top : (direction === 'rtl' ? b.left - a.left : a.left - b.left); });
                var distinct = measured.every(function (item, index) { return !index || Math.abs(item.top - measured[index - 1].top) > 2 || Math.abs(item.left - measured[index - 1].left) > 2; });
                if (distinct && measured.some(function (item, index) { return index > 0 && item.entry.count > measured[index - 1].entry.count; })) return -1;
            }
            return count;
        }
        function finishText(visible) {
            return visible ? '評価件数順・確認済み ' + visible + ' 商品・自動更新 v' + VERSION : '評価件数を確認できる商品がありません v' + VERSION;
        }
        function countSignature(entries) {
            return entries.map(function (entry) { return entry.asin + ':' + entry.count; }).sort().join(',');
        }
        function failOpen(groups, entries, message) {
            restoreDeferred(true); session.failed = true; session.failedCount = entries ? entries.length : 0;
            session.recoverable = !message && !!entries && entries.length > 0; session.failedSignature = countSignature(entries || []);
            session.phase = 'frozen'; state.phase = 'frozen'; cancelTimer();
            session.text = (message || '評価件数を確認できないため、そのまま表示') + ' v' + VERSION;
            showStatus(session.text, groups, 'unreadable'); updateControl(groups); dirty = false;
        }
        function finalize(groups) {
            var entries = entriesFor(groups, true);
            if (!entries.length) {
                session.phase = 'awaiting'; state.phase = 'awaiting'; session.interrupted = true; cancelTimer();
                showStatus('先読み終了：商品一覧の表示を待っています v' + VERSION, groups, 'awaiting'); dirty = false; return;
            }
            session.bucket = selectBucket(entries); session.ranks = new Map(); session.nodeRanks = new WeakMap(); session.nextRank = 0;
            // All original nodes remain in place or move as complete product slots. Own hiding is reversible.
            deferred.clear();
            var eligible = entries.filter(function (entry) {
                var reason = entry.sponsored ? 'sponsored' : entry.unsupported || entry.bucket !== session.bucket ? 'layout' : entry.count < 0 ? 'unknown' : null;
                if (reason) hideEntry(entry, reason); return !reason;
            });
            if (!eligible.length) { failOpen(groups, entries); return; }
            session.failed = false; lastError = null;
            var main = eligible.find(function (entry) { return entry.priority === 3; }) || eligible[0];
            session.rootReference = main.root;
            session.searchAsins = new Set(entries.filter(function (entry) { return !entry.sponsored && entry.bucket === session.bucket; }).map(function (entry) { return entry.asin; }));
            var plans = plansFor(eligible, function (a, b) { return b.count - a.count || a.ordinal - b.ordinal; });
            if (!validPlacements(plans)) { failOpen(groups, entries, '異なる一覧形式を安全に統合できないため、そのまま表示'); return; }
            plans.forEach(function (plan) { plan.ranked.forEach(function (entry) { assignRank(entry, session.nextRank++); }); });
            var returnTop = session.auto && !session.interrupted && session.scroller && !busy();
            // Pixel compensation preserves the visible product; only controlled
            // initial preload completion is allowed to return to the original top.
            protectAnchor(returnTop ? null : passAnchor, function () {
                applyMoves(plans); applyDeferred();
                var visible = verifyVisible(eligible); if (visible <= 0) { failOpen(groups, entries, visible < 0 ? '表示位置と件数順が一致しないため、そのまま表示' : null); return; }
                applyDeferred(); session.text = finishText(visible);
                session.phase = 'frozen'; state.phase = 'frozen';
                showStatus(session.text, groups, 'frozen'); updateControl(groups);
                if (returnTop) scrollToValue(session.scroller, session.origin);
                session.auto = false;
            });
            cancelTimer(); dirty = false;
        }
        function normalizeFrozen(groups, entries) {
            // Native search events can precede the asynchronous DOM replacement.
            // Immutable membership froze the old query, then permanently rejected
            // every real result. Rank the current readable cohort on every idle
            // update instead. This also handles overlap, virtualized replacement,
            // late pages and changed counts without starting another preload.
            session.auto = false; session.interrupted = true; session.manual = false;
            if (!entries.length) {
                restoreDeferred(true); session.phase = 'awaiting'; state.phase = 'awaiting';
                showStatus('商品一覧の更新を待っています v' + VERSION, groups, 'awaiting');
                dirty = false; return;
            }
            finalize(groups);
        }

        function beginReady() {
            if (session.phase !== 'ready') {
                session.phase = 'ready'; session.readyUntil = now() + READY; session.readySignature = null; session.stableSince = now();
            }
            state.phase = 'ready';
        }
        function ready(groups) {
            var entries = entriesFor(groups, true), signature = entries.map(function (e) { return objectId(e.node) + ':' + e.count; }).join(',');
            if (signature !== session.readySignature) { session.readySignature = signature; session.stableSince = now(); }
            var known = entries.filter(function (e) { return !e.sponsored && e.count >= 0; }).length;
            if (now() >= session.readyUntil || known === entries.filter(function (e) { return !e.sponsored; }).length && known > 0 && now() - session.stableSince >= QUIET) { finalize(groups); return; }
            if (session.manual) {
                entries.forEach(function (entry) {
                    var held = session.nodeRanks.get(entry.node);
                    if (!held || held.asin !== entry.asin || held.count !== entry.count) hideEntry(entry, 'pending');
                });
                applyDeferred();
            }
            var text = '評価件数を確認中 ' + known + ' / ' + entries.filter(function (e) { return !e.sponsored; }).length + ' 商品 v' + VERSION;
            var label = doc.getElementById(STATUS);
            if (!label || label.textContent !== text || label.getAttribute('data-hiro-state') !== 'ready') {
                protectAnchor(session.auto ? null : passAnchor, function () { showStatus(text, groups, 'ready'); });
            }
            queue(Math.min(250, session.readyUntil - now()));
        }
        function restoredDocument() {
            try {
                var nav = win.performance && win.performance.getEntriesByType && win.performance.getEntriesByType('navigation');
                return !!(nav && nav[0] && nav[0].type === 'back_forward');
            } catch (ignored) { return false; }
        }
        function collect(groups, entries) {
            var count = uniqueCount(entries), time = now();
            if (session.phase === 'awaiting') { if (!entries.length) { dirty = false; return; } session.phase = 'finalizing'; }
            if (session.phase === 'waiting') {
                if (!entries.length) {
                    showStatus('先読み中 0 / 約100商品・画面操作で終了 v' + VERSION, groups, 'collecting');
                    if (time < session.deadline) { queue(Math.min(STEP, session.deadline - time)); return; }
                    session.phase = 'finalizing';
                } else {
                    var scroller = getScroller(entries[0].slot);
                    session.scroller = scroller; session.origin = scrollPosition(scroller);
                    session.auto = isSearch(win, state.searchHint) &&
                        session.origin <= 1 && scrollPosition(doc.scrollingElement || doc.documentElement) <= 1 &&
                        typeof win.innerHeight === 'number' && win.innerHeight > 0 && typeof win.scrollTo === 'function' && !restoredDocument();
                    session.phase = session.auto ? 'collecting' : 'finalizing';
                    session.lastCount = count;
                }
            }
            if (session.phase === 'finalizing' || session.phase === 'ready') { beginReady(); ready(groups); return; }
            state.phase = session.phase;
            showStatus('先読み中 ' + count + ' / 約100商品・画面操作で終了 v' + VERSION, groups, session.phase);
            if (count >= TARGET || time >= session.deadline || session.steps >= MAX_STEPS) { beginReady(); ready(groups); return; }
            if (time < session.nextStep) { queue(Math.min(session.nextStep - time, session.deadline - time)); return; }
            if (count > session.lastCount) session.stalls = 0;
            else if (session.steps > 0) session.stalls++;
            session.lastCount = count;
            if (session.stalls >= STALLS) { beginReady(); ready(groups); return; }
            // Scroll only the current eligible result tail, never the page footer/recommendations.
            var eligible = entries.filter(function (e) { return getScroller(e.slot) === session.scroller; });
            var tail = eligible.length ? eligible[eligible.length - 1].slot : null;
            if (!tail || !tail.getBoundingClientRect) { beginReady(); ready(groups); return; }
            var rect = tail.getBoundingClientRect(), bounds = session.scroller === doc.documentElement || session.scroller === doc.scrollingElement || session.scroller === doc.body ?
                {top: 0, height: win.innerHeight} : session.scroller.getBoundingClientRect();
            if (!rect || !(rect.height > 0) || !bounds || !(bounds.height > 0)) { beginReady(); ready(groups); return; }
            var before = scrollPosition(session.scroller);
            scrollToValue(session.scroller, Math.max(before, before + rect.bottom - bounds.top - bounds.height * 0.75));
            session.steps++; session.nextStep = time + STEP;
            queue(Math.min(STEP, session.deadline - time));
        }
        function deferDuringInput() {
            if (!session || session.phase !== 'frozen' || session.failed) return;
            observer.disconnect();
            try {
                var groups = chooseGroups(doc, true, undefined, allowMobile());
                if (groups.overflow || groups.unrecognized) { failOpen([], [], groups.overflow ? '商品が多いため並び順を確認できません' : '一部の商品を認識できないため、並び順を確認できません'); return; }
                var entries = entriesFor(groups, false);
                entries.forEach(function (entry) {
                    var held = session.nodeRanks.get(entry.node), reason = entry.sponsored ? 'sponsored' : entry.unsupported || entry.bucket !== session.bucket ? 'layout' : null;
                    if (!reason && (!held || held.asin !== entry.asin)) reason = session.ranks.has(entry.asin) ? 'gesture' : 'new';
                    if (!reason && (broadChange || changedCards.has(entry.node)) && readCount(entry.node) !== held.count) reason = 'changed';
                    if (reason) hideEntry(entry, reason);
                });
                applyDeferred(); updateControl(groups); changedCards.clear(); broadChange = false;
            } catch (ignored) { lastError = errorName(ignored); restoreDeferred(true); try { failOpen([], []); } catch (ignoredStatus) {} }
            finally { observe(); }
        }
        function restoreGestureEntries() {
            deferred.forEach(function (held, node) {
                if (held.reason !== 'gesture') return;
                if (node.getAttribute(DEFER) === VERSION) { if (held.original == null) node.removeAttribute(DEFER); else node.setAttribute(DEFER, held.original); }
                deferred.delete(node);
            });
        }

        function run() {
            if (disposed || running) return;
            if (nativeSuspended) {
                if (navigationIdentity() === suspendedNavigation) { state.active = false; activationPending = false; dirty = false; return; }
                nativeSuspended = false; suspendedNavigation = null; state.hintToken = null; lastKey = null;
            }
            dirty = true;
            // Navigation/visibility must cancel automation even during a held touch.
            activationPending = false;
            var searching = isSearch(win, state.searchHint);
            state.active = searching || canShowHint();
            if (!state.active) { stopCollecting('navigation'); observer.disconnect(); restoreDeferred(true); removeStatus(); observe(); dirty = false; return; }
            if (doc.hidden || doc.visibilityState === 'hidden') { stopCollecting('hidden'); return; }
            var key = searchKey();
            if (lastKey !== key || !session) resetResults(key);
            if (!doc.getElementById(STATUS)) {
                observer.disconnect(); showStatus('評価件数順 v' + VERSION + '：操作が止まると確認します', [], 'pending'); observe();
            }
            if (busy()) { deferDuringInput(); if (!touchActive && !pointers.size) queue(Math.max(1, IDLE - (now() - lastActivity))); return; }
            running = true; observer.disconnect();
            try {
                var priorGroups = chooseGroups(doc, true, undefined, allowMobile());
                if (priorGroups.overflow || priorGroups.unrecognized) { failOpen([], [], priorGroups.overflow ? '商品が多いため並び順を確認できません' : '一部の商品を認識できないため、並び順を確認できません'); return; }
                var priorEntries = entriesFor(priorGroups, false);
                passAnchor = captureAnchor(priorEntries.filter(function (entry) {
                    return !entry.sponsored && readCount(entry.node) >= 0;
                }));
                restoreGestureEntries();
                if (session.phase === 'frozen' || session.manual) restoreDeferred(false);
                var groups = searching ? chooseGroups(doc, true, undefined, allowMobile()) : [];
                if (groups.overflow || groups.unrecognized) { failOpen([], [], groups.overflow ? '商品が多いため並び順を確認できません' : '一部の商品を認識できないため、並び順を確認できません'); return; }
                var entries = entriesFor(groups, session.phase === 'frozen');
                if (session.phase === 'frozen') normalizeFrozen(groups, entries);
                else collect(groups, entries);
            } catch (ignored) {
                lastError = errorName(ignored);
                // Fail open: stop automation permanently for this search, never retry scroll on a loop.
                cancelTimer(); session.phase = 'frozen'; state.phase = 'frozen'; session.interrupted = true;
                dirty = false;
                restoreDeferred(true);
                try { failOpen([], []); } catch (ignoredStatus) {}
            } finally { if (session && session.manual && session.phase !== 'frozen') applyDeferred(); changedCards.clear(); broadChange = false; running = false; observe(); }
        }
        observer = new win.MutationObserver(function (records) {
            function nearestCard(node) {
                for (var parent = node; parent; parent = parent.parentElement) {
                    if (parent.matches && (parent.matches(CANDIDATE) || inferredCards.has(parent))) return parent;
                }
                return null;
            }
            function markChanged(node) {
                var element = node && (node.nodeType === 1 ? node : node.parentElement), card = nearestCard(element), marked = false;
                for (var depth = 0; card && depth < 8 && changedCards.size < 1000; depth++) {
                    changedCards.add(card); marked = true; card = nearestCard(card.parentElement);
                }
                return marked;
            }
            var relevant = false; records.forEach(function (record) {
                var target = record.target.nodeType === 1 ? record.target : record.target.parentElement;
                if (target && (target.id === STATUS || target.id === CONTROL || target.id === DEFER_STYLE || target.closest('#' + STATUS + ', #' + CONTROL))) return;
                relevant = true;
                var marked = markChanged(target);
                Array.prototype.forEach.call(record.addedNodes || [], function (node) { marked = markChanged(node) || marked; });
                if (!marked && record.removedNodes && record.removedNodes.length) broadChange = true;
            });
            if (relevant && (state.active || isSearch(win, state.searchHint) || canShowHint())) schedule();
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
            restoreScrollStyles(); restoreDeferred(true); removeStatus();
            var style = doc.getElementById(DEFER_STYLE); if (style && style.parentNode) style.parentNode.removeChild(style);
            inputEvents.forEach(function (type) { win.removeEventListener(type, activity, {capture: true}); });
            doc.removeEventListener('visibilitychange', visibilityChanged);
            win.removeEventListener('popstate', navigated); win.removeEventListener('pageshow', navigated);
        }
        run(); return state;
    }

    if (typeof window !== 'undefined') install(window);
    else { try { if (typeof module === 'object' && module && module.exports) module.exports = {parseCount: parseCount, readCount: readCount, chooseGroup: chooseGroup, chooseGroups: chooseGroups, isAmazon: isAmazon, searchPath: searchPath, isSearch: isSearch, install: install}; } catch (ignoredExport) {} }
})();
