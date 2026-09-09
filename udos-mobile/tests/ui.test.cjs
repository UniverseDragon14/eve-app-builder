const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const assets = path.resolve(__dirname, '../app/src/main/assets');
const source = readFileSync(path.join(assets, 'udos.js'), 'utf8');
const html = readFileSync(path.join(assets, 'udos.html'), 'utf8');

function page(withBridge = true) {
  const elements = Object.fromEntries(['q', 'out', 'sheet', 'sheetBody'].map(id =>
    [id, { value: '', innerHTML: '', innerText: '', textContent: '', style: {} }]));
  const calls = [];
  const sandbox = { document: { getElementById: id => elements[id] } };
  sandbox.window = sandbox;
  if (withBridge) {
    sandbox.UDOS = Object.fromEntries(['command', 'listen', 'wake', 'sleep', 'speak',
      'openCamera', 'openSettings', 'openApps', 'openSite', 'voiceStatus', 'setVoiceLanguage']
      .map(method => [method, (...args) => calls.push([method, ...args])]));
  }
  vm.createContext(sandbox);
  vm.runInContext(source, sandbox);
  return { sandbox, elements, calls };
}

test('the page loads one script and every static button handler compiles', () => {
  const scripts = [...html.matchAll(/<script\b[^>]*>/g)];
  assert.equal(scripts.length, 1);
  assert.match(scripts[0][0], /src="udos\.js"/);
  for (const match of html.matchAll(/on(?:click|keydown)="([^"]*)"/g)) {
    new vm.Script(match[1]);
  }
});

test('Voice, Wake and Sleep invoke the installed bridge once each', () => {
  const { sandbox, calls } = page();
  sandbox.cmd('voice');
  sandbox.cmd('wake');
  sandbox.cmd('sleep');
  assert.deepEqual(calls, [['listen'], ['wake'], ['sleep']]);
});

test('Send forwards a complete command instead of just echoing it', () => {
  const { sandbox, elements, calls } = page();
  elements.q.value = 'Hey Nova, open camera';
  sandbox.send();
  assert.deepEqual(calls, [['command', 'Hey Nova, open camera']]);
  assert.equal(elements.q.value, '');
});

test('typed content is escaped before rendering; it never becomes HTML', () => {
  const { sandbox, elements, calls } = page();
  sandbox.cmd('<img src=x onerror=UDOS.openCamera()>');
  assert.doesNotMatch(elements.out.innerHTML, /<img/);
  assert.match(elements.out.innerHTML, /&lt;img/);
  assert.equal(calls.length, 1);
  assert.equal(calls[0][0], 'command');
});

test('read response sends the visible response text to TTS', () => {
  const { sandbox, elements, calls } = page();
  elements.out.innerText = 'Camera is ready';
  sandbox.readResponse();
  assert.deepEqual(calls, [['speak', 'Camera is ready']]);
});

test('microphone availability comes from the device, with no hard-coded diagnosis', () => {
  const { sandbox, calls } = page();
  sandbox.voiceSetup();
  sandbox.cmd('voice check');
  assert.deepEqual(calls, [['listen'], ['voiceStatus']]);
});

test('missing bridge produces an accurate result and no fake permission claim', () => {
  const { sandbox, elements } = page(false);
  sandbox.voiceSetup();
  assert.match(elements.out.innerHTML, /installed UDOS Mobile/);
  assert.doesNotMatch(elements.out.innerHTML, /Permission OK|No speech recognition service/);
});

test('settings retains a working Android settings button and language controls', () => {
  const { sandbox, elements, calls } = page();
  sandbox.settings();
  assert.equal(elements.sheet.style.display, 'flex');
  const handlers = [...elements.sheetBody.innerHTML.matchAll(/onclick="([^"]+)"/g)].map(x => x[1]);
  for (const handler of handlers) new vm.Script(handler);
  vm.runInContext(handlers.find(x => x.includes("openSettings")), sandbox);
  vm.runInContext(handlers.find(x => x.includes("ta-IN")), sandbox);
  assert.deepEqual(calls, [['openSettings'], ['setVoiceLanguage', 'ta-IN']]);
});

test('ordinary status and phone commands do not contact Pi', () => {
  const { sandbox, calls } = page();
  sandbox.cmd('status');
  sandbox.cmd('camera');
  sandbox.cmd('settings');
  assert.deepEqual(calls, [['command', 'status'], ['command', 'camera'], ['command', 'settings']]);
});

test('oversized input and unlisted bridge methods are rejected', () => {
  const { sandbox, calls } = page();
  sandbox.cmd('x'.repeat(1001));
  assert.equal(sandbox.callNative('exec', 'anything'), false);
  assert.deepEqual(calls, []);
});
