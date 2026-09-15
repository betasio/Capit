(function () {
  'use strict';
  const isInstagram = host => host === 'instagram.com' || host.endsWith('.instagram.com');
  function classify(raw, base = 'https://www.instagram.com/') {
    try {
      const u = new URL(raw, base);
      if (u.protocol !== 'https:' || !isInstagram(u.hostname)) return 'external';
      const path = decodeURIComponent(u.pathname).toLowerCase();
      if (/^\/(reel|reels|explore)(\/|$)/.test(path) || /^\/[^/]+\/reels(\/|$)/.test(path)) return 'blocked';
      if (path === '/') return u.searchParams.get('variant') === 'following' ? 'following' : 'home';
      if (/^\/direct(\/|$)/.test(path)) return 'messages';
      if (/^\/stories(\/|$)/.test(path)) return 'stories';
      return 'allowed';
    } catch (_) { return 'external'; }
  }
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = { classify };
    return;
  }
  if (!isInstagram(location.hostname) || window.top !== window || window.__capitInstalled) return;
  window.__capitInstalled = true;
  const css = `
    [data-capit-hidden] { display:none !important; }
    html[data-capit-route="home"] article { display:none !important; }
    html[data-capit-route="blocked"] body > :not(#capit-blocked) { display:none !important; }
    #capit-blocked { position:fixed; inset:0; z-index:2147483647; background:#f5f4ee; color:#183c30;
      padding:80px 28px; font:18px/1.6 system-ui; }
    #capit-blocked a { color:#216b52; }
  `;
  let scheduled = false;
  function setRoute() {
    const route = classify(location.href);
    document.documentElement?.setAttribute('data-capit-route', route);
    return route;
  }
  function apply() {
    scheduled = false;
    if (!document.documentElement) return;
    const route = setRoute();
    if (!document.getElementById('capit-style')) {
      const style = document.createElement('style');
      style.id = 'capit-style'; style.textContent = css;
      (document.head || document.documentElement).appendChild(style);
    }
    let blocked = document.getElementById('capit-blocked');
    if (route === 'blocked' && document.body) {
      if (!blocked) {
        blocked = document.createElement('section'); blocked.id = 'capit-blocked';
        const title = document.createElement('h1'); title.textContent = 'A little less scrolling.';
        const text = document.createElement('p'); text.textContent = 'Reels and Explore are hidden in Capit.';
        const link = document.createElement('a'); link.href = '/direct/inbox/'; link.textContent = 'Open messages';
        blocked.append(title, text, link); document.body.appendChild(blocked);
      }
      document.querySelectorAll('video').forEach(v => v.pause());
    } else if (blocked) blocked.remove();
    // Re-evaluate recycled nodes when Instagram changes a card's content.
    document.querySelectorAll('[data-capit-hidden]').forEach(el => el.removeAttribute('data-capit-hidden'));
    document.querySelectorAll('a[href]').forEach(a => {
      if (classify(a.href) !== 'blocked') return;
      // Never hide an entire message thread or Story because it contains a shared Reel link.
      const card = (route === 'home' || route === 'following' || route === 'allowed') ? a.closest('article') : null;
      (card || a).setAttribute('data-capit-hidden', '');
      if (card) card.querySelectorAll('video').forEach(v => v.pause());
    });
    if (route === 'following' || route === 'home') {
      document.querySelectorAll('article').forEach(card => {
        const markers = [...card.querySelectorAll('span')].some(s =>
          /^(Suggested for you|Suggested posts|Sponsored)$/i.test(s.textContent.trim()));
        if (route === 'home' || markers) {
          card.setAttribute('data-capit-hidden', '');
          card.querySelectorAll('video').forEach(v => v.pause());
        }
      });
    }
  }
  function schedule() {
    if (scheduled) return;
    scheduled = true; requestAnimationFrame(apply);
  }
  // Avoid observing attributes changed by our own filtering.
  new MutationObserver(schedule).observe(document, { subtree:true, childList:true, characterData:true, attributes:true, attributeFilter:['href'] });
  document.addEventListener('click', event => {
    const a = event.target.closest?.('a[href]');
    if (a && classify(a.href) === 'blocked') {
      event.preventDefault(); event.stopImmediatePropagation();
    }
  }, true);
  for (const method of ['pushState', 'replaceState']) {
    const original = history[method];
    history[method] = function (state, unused, url) {
      if (url != null && classify(url, location.href) === 'blocked') return;
      const result = original.apply(this, arguments);
      setRoute(); schedule(); return result;
    };
  }
  window.addEventListener('popstate', () => { setRoute(); schedule(); });
  document.addEventListener('DOMContentLoaded', apply, { once:true });
  setRoute(); apply();
})();
