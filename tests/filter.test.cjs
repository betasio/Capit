const { test } = require('node:test');
const assert = require('node:assert/strict');
const { classify } = require('../app/src/main/assets/instagram-filter.js');
test('blocks Reel, Reels, Explore and profile Reels routes', () => {
  for (const path of ['/reel/abc/', '/reels/', '/explore/', '/friend/reels/', '/%72eel/abc/'])
    assert.equal(classify(path), 'blocked', path);
});
test('preserves DMs, Stories, normal posts, profiles, login and challenges', () => {
  for (const [path, result] of [['/direct/inbox/', 'messages'], ['/direct/t/123/', 'messages'],
    ['/stories/friend/123/', 'stories'], ['/p/abc/', 'allowed'], ['/friend/', 'allowed'],
    ['/accounts/login/', 'allowed'], ['/challenge/', 'allowed']]) assert.equal(classify(path), result);
});
test('distinguishes unfiltered home from experimental Following route', () => {
  assert.equal(classify('/'), 'home');
  assert.equal(classify('/?variant=following'), 'following');
  assert.equal(classify('/?variant=somethingelse'), 'home');
});
test('does not mistake foreign hosts or unsafe schemes for Instagram', () => {
  for (const url of ['https://instagram.com.evil.test/reel/x', 'https://evilinstagram.com/',
    'http://www.instagram.com/', 'javascript:alert(1)', 'intent://instagram/', 'https://evil.test/'])
    assert.equal(classify(url), 'external', url);
});
test('resolves absolute and relative Instagram URLs correctly', () => {
  assert.equal(classify('https://www.instagram.com/reel/123/?x=y'), 'blocked');
  assert.equal(classify('https://m.instagram.com/direct/inbox/'), 'messages');
  assert.equal(classify('../reels/', 'https://www.instagram.com/friend/'), 'blocked');
  assert.equal(classify('/reelish/'), 'allowed');
});
