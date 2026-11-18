"""Run the compiled RestoreCoordinator document-start script in a Chromium DOM.

Usage: python check_listing_browser.py document_start.js [--chromium BINARY] [--report FILE]

The fixture serves local HTML through request interception. The explicit restore
sequence supplies ordered, trusted-shaped pagehide and pageshow notifications.
Chromium performs reloads, sets navigation entry keys, restores scroll, maintains
form inputs, and executes all JS. This is separate from an Android BFCache test.
"""

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path

from playwright.sync_api import sync_playwright

LIST = "https://news.yahoo.co.jp/categories/entertainment?from=tab#section"
ARTICLE = "https://news.yahoo.co.jp/articles/example"
ORIGIN = "https://news.yahoo.co.jp"
FIXTURE = """<!doctype html><meta charset=utf-8>
<title>一覧</title><body style="margin:0;min-height:4900px">
<h1 id="heading">ニュース一覧</h1><a id="article" href="/articles/example">記事を開く</a>
<form><input id="title" name="title"><textarea id="body"></textarea>
<input id="choice" type="checkbox"><select id="selection">
<option>one</option><option>two</option></select></form>
<div id="editor" contenteditable>original</div>
<p style="margin-top:1500px">下部の一覧</p>"""
HOOK = """
(() => {
  const nativeAdd = window.addEventListener.bind(window);
  const handlers = {pagehide: [], pageshow: []};
  const register = window.addEventListener;
  window.addEventListener = function(name, callback, options) {
    if (this === window && name in handlers) handlers[name].push(callback);
    return register.call(this, name, callback, options);
  };
  Object.defineProperty(window,'__fixtureLifecycle',{value:(name,trusted=true,persisted=true)=>{
    for(const callback of handlers[name]){
      callback.call(window,{type:name,isTrusted:trusted,persisted,target:window,currentTarget:window});
    }
  }});
  nativeAdd('error', e=>console.log('fixture error: '+e.message));
})();
"""


def require(ok, message):
    if not ok:
        raise AssertionError(message)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("script", type=Path)
    parser.add_argument("--chromium")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    production = args.script.read_text()
    results = []
    errors = []
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(
            executable_path=args.chromium, headless=True,
            args=["--no-sandbox", "--disable-dev-shm-usage"],
            ignore_default_args=["--disable-back-forward-cache"],
        )
        version = browser.version

        def run(name, url, expect_reload, setup=None, events=None,
                test_forward=False, assert_content=None, before_init="", fixture=None):
            context = browser.new_context(viewport={"width":400,"height":800})
            requests = Counter()

            def serve(route):
                if route.request.is_navigation_request():
                    requests[route.request.url.split("#",1)[0]] += 1
                route.fulfill(status=200, content_type="text/html", body=fixture(requests,route.request) if fixture else FIXTURE)

            context.route("**/*", serve)
            context.add_init_script(script=HOOK + ";\n" + before_init + ";\n" + production)
            page = context.new_page()
            page.on("pageerror", lambda err: errors.append(str(err)))
            page.goto(url)
            page.wait_for_load_state("load")
            key = lambda: page.evaluate("navigation.currentEntry.key")
            history_length = lambda: page.evaluate("history.length")
            if test_forward:
                page.evaluate("history.replaceState({owner:'fixture',category:2},'')")
                page.locator("#article").click()
                require(page.url == ARTICLE, "article navigation failed")
                page.go_back()
                page.wait_for_load_state("load")
                require(page.url == url, "history returned to wrong list")
            before_key, before_len = key(), history_length()
            before_urls = page.evaluate("navigation.entries().map(e=>[e.key,e.url])")
            page.evaluate("scrollTo(0,713)")
            page.wait_for_function("Math.abs(scrollY-713)<=1")
            if setup:
                setup(context, page)
            request_key = url.split("#",1)[0]
            before_count = requests[request_key]
            if events is None:
                events = """
                    window.__fixtureLifecycle('pagehide');
                    window.__fixtureLifecycle('pageshow');
                """
            page.evaluate("() => {"+events+"}")
            if expect_reload:
                page.wait_for_function(
                    "performance.getEntriesByType('navigation')[0].type === 'reload'",
                    timeout=5000)
                page.wait_for_load_state("load")
            page.wait_for_timeout(180)
            difference = requests[request_key] - before_count
            require(difference == expect_reload, f"{name}: wanted {expect_reload} reload, got {difference}")
            require(page.url == url, f"{name}: changed URL (query/fragment)")
            require(key() == before_key and history_length() == before_len, f"{name}: changed history entry or length")
            require(before_urls == page.evaluate("navigation.entries().map(e=>[e.key,e.url])"), f"{name}: changed history stack")
            if setup is None:
                require(abs(page.evaluate("scrollY") - 713) <= 1, f"{name}: scroll position lost")
            if assert_content:
                require(page.evaluate(assert_content), f"{name}: form draft changed")
            if test_forward:
                require(page.evaluate("history.state?.owner==='fixture' && history.state?.category===2"), "page-owned history.state changed")
                page.go_forward()
                require(page.url == ARTICLE, f"{name}: forward history lost")
            results.append({"scenario":name,"reload_requests":difference,"history_preserved":True})
            context.close()

        def draft(selector, value):
            def fill(_, page):
                page.locator(selector).fill(value)
                page.locator(selector).blur()
            return fill

        run("category-query-fragment-and-forward",LIST,1,test_forward=True)
        run("news-list-root",ORIGIN+"/",1)
        run("other-category",ORIGIN+"/categories/sports",1)
        run("article",ARTICLE,0)
        run("pickup",ORIGIN+"/pickup/1234",0)
        run("nested-category-path",ORIGIN+"/categories/entertainment/details",0)
        run("unrelated-host","https://example.test/categories/entertainment",0)
        run("lookalike-host","https://news.yahoo.co.jp.example.test/categories/entertainment",0)
        run("ordinary-load",LIST,0,events="window.__fixtureLifecycle('pageshow',true,false)")
        run("no-preceding-pagehide",LIST,0,events="window.__fixtureLifecycle('pageshow')")
        run("untrusted-pageshow",LIST,0,events="window.__fixtureLifecycle('pagehide');window.__fixtureLifecycle('pageshow',false)")
        run("offline",LIST,0,setup=lambda context,page: context.set_offline(True))
        run("hidden",LIST,0,setup=lambda _,p:p.evaluate("""() => {
            Object.defineProperty(document,'visibilityState',{get:()=> 'hidden'});
            Object.defineProperty(document,'hidden',{get:()=>true});
        }"""))
        run("focused-input",LIST,0,setup=lambda _,p:p.locator("#title").focus())
        run("edited-then-blurred-text",LIST,0,setup=draft("#title","保存前の記事名"),
            assert_content="document.querySelector('#title').value==='保存前の記事名'")
        run("edited-then-blurred-contenteditable",LIST,0,setup=draft("#editor","保存前の本文"),
            assert_content="document.querySelector('#editor').textContent==='保存前の本文'")
        run("checkbox-edit",LIST,0,setup=lambda _,p:(p.locator("#choice").check(),p.locator("#choice").blur()),
            assert_content="document.querySelector('#choice').checked")
        run("select-edit",LIST,0,setup=lambda _,p:(p.locator("#selection").select_option(label="two"),p.locator("#selection").blur()),
            assert_content="document.querySelector('#selection').value==='two'")
        run("non-persisted-pagehide",LIST,0,events="""
            window.__fixtureLifecycle('pagehide',true,false);window.__fixtureLifecycle('pageshow');
        """)
        run("native-callback-only",LIST,1,events="""
            window.__fixtureLifecycle('pagehide');
            window.__hiroBerryBFCacheRestoreV1.recoverListing(location.href);
        """)
        run("native-callback-stale-url",LIST,0,events="""
            window.__fixtureLifecycle('pagehide');
            window.__hiroBerryBFCacheRestoreV1.recoverListing(location.origin+'/categories/sports');
        """)
        run("native-callback-no-cached-document",LIST,0,events="""
            window.__hiroBerryBFCacheRestoreV1.recoverListing(location.href);
        """)
        run("native-and-dom-callback",LIST,1,events="""
            window.__fixtureLifecycle('pagehide');window.__hiroBerryBFCacheRestoreV1.recoverListing(location.href);
            window.__fixtureLifecycle('pageshow');
        """)
        run("private-storage-unavailable-with-scrolled-list",LIST,0,before_init="""
            Object.defineProperty(window,'sessionStorage',{get(){throw new Error('storage denied')}});
        """)
        def late_content(counts, request):
            if counts[LIST.split('#')[0]] < 2:
                return FIXTURE
            return """<!doctype html><title>loading list</title><body id="shell"><h1>News</h1>
                <script>setTimeout(()=>{document.body.style.minHeight='4900px'},85)</script>"""
        run("async-list-restores-scroll",LIST,1,fixture=late_content)
        run("duplicate-return-notification",LIST,1,events="""
            window.__fixtureLifecycle('pagehide');window.__fixtureLifecycle('pageshow');window.__fixtureLifecycle('pageshow');
        """)
        require(not errors, f"JS page errors: {errors}")
        browser.close()
    report = {"result":"PASS", "chromium_version":version,
              "document_start_sha256":hashlib.sha256(args.script.read_bytes()).hexdigest(),
              "restore_signal":"controlled lifecycle notifications",
              "actual_browser_operations":["reload","forward and back","entry key and stack","scroll and editable controls","offline"],
              "scenarios":results, "page_errors":errors}
    content = json.dumps(report,ensure_ascii=False,indent=2)+"\n"
    if args.report:
        args.report.write_text(content)
    print(content,end="")


if __name__ == "__main__":
    main()
