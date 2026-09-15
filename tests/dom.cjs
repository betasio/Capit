// Synthetic Instagram fixtures, not a live-account compatibility test.
const { chromium } = require('playwright');
const fs = require('node:fs');
const assert = require('node:assert/strict');
(async () => {
  const browser = await chromium.launch({headless:true});
  try {
    const page = await browser.newPage();
    await page.route('https://www.instagram.com/**', route => route.fulfill({contentType:'text/html', body:`
      <html><head></head><body><nav><a id="reels" href="/reels/">Reels</a><a id="inbox" href="/direct/inbox/">Messages</a></nav>
      <div id="stories"><a href="/stories/friend/1/">Story</a></div>
      <article id="photo"><span>Friend photo</span><a href="/p/1/">Photo</a></article>
      <article id="reel"><a href="/reel/1/">Reel</a></article>
      <article id="suggested"><span>Suggested for you</span></article>
      <section id="thread"><p>Hello there</p><a id="shared" href="/reel/2/">Shared Reel</a></section>
      </body></html>`}));
    await page.addInitScript({content:fs.readFileSync('app/src/main/assets/instagram-filter.js','utf8')});
    await page.goto('https://www.instagram.com/');
    assert.equal(await page.locator('#photo').isVisible(), false);
    assert.equal(await page.locator('#stories').isVisible(), true);
    assert.equal(await page.locator('#reels').isVisible(), false);
    await page.goto('https://www.instagram.com/?variant=following');
    assert.equal(await page.locator('#photo').isVisible(), true);
    assert.equal(await page.locator('#suggested').isVisible(), false);
    assert.equal(await page.locator('#reel').isVisible(), false);
    await page.evaluate(() => {
      const card = document.createElement('article'); card.id='late'; card.innerHTML='<a href="/reel/late/">Late Reel</a>';
      document.body.append(card);
    });
    await page.waitForFunction(() => document.querySelector('#late').hasAttribute('data-capit-hidden'));
    assert.equal(await page.locator('#late').isVisible(), false);
    await page.evaluate(() => history.pushState({}, '', '/reels/'));
    assert.equal(new URL(page.url()).pathname, '/');
    await page.goto('https://www.instagram.com/direct/inbox/');
    assert.equal(await page.locator('#thread').isVisible(), true);
    assert.equal(await page.locator('#shared').isVisible(), false);
    await page.goto('https://www.instagram.com/reel/direct/');
    assert.equal(await page.locator('#capit-blocked').isVisible(), true);
    assert.equal(await page.locator('#photo').isVisible(), false);
    console.log('DOM fixtures passed: home, Stories, Following, Reels, suggestions, dynamic cards, SPA navigation, DMs, direct routes.');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode=1; });
