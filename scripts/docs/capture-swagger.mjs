#!/usr/bin/env node
// Attach to caller-owned Chrome; capture only the live rendered Swagger UI.
// @author Navaneeth Mysore G
// @author Claude
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';

// The script drives Chrome through the global WebSocket, which Node has only since version 22: fail clearly and
// at once, before any network use, instead of throwing a ReferenceError later outside the try block.
if (typeof WebSocket === 'undefined') {
  console.error(`capture-swagger.mjs needs Node >= 22 (a global WebSocket); this is Node ${process.versions.node}`);
  process.exit(2);
}

const [urlText, outputDir, portText, sourceArg] = process.argv.slice(2);
const sourceSHA = sourceArg || process.env.CAPTURE_SOURCE_SHA;
if (!urlText || !outputDir || !/^\d+$/.test(portText ?? '')) {
  throw new Error('Usage: node capture-swagger.mjs <local Swagger UI URL> <output directory> <Chrome debug port> [source SHA; or CAPTURE_SOURCE_SHA]');
}
if (!/^[a-fA-F0-9]{40}$/.test(sourceSHA ?? '')) throw new Error('A full 40-hex source SHA is required');
const url = new URL(urlText);
if (!['http:', 'https:'].includes(url.protocol) || !['localhost', '127.0.0.1', '[::1]'].includes(url.hostname)) {
  throw new Error('Swagger URL must be a live local HTTP(S) URL');
}
const origin = `http://127.0.0.1:${Number(portText)}`;
const getJSON = async (endpoint, options = {}) => {
  const response = await fetch(origin + endpoint, {...options, signal: AbortSignal.timeout(10000)});
  if (!response.ok) throw new Error(`${endpoint}: HTTP ${response.status}`);
  return response.json();
};
const version = await getJSON('/json/version');
// Keep only the two version strings: the full object carries the browser's DevTools WebSocket URL (a live
// control endpoint) and user-agent details, none of which belongs in a committed manifest.
const browser = {Browser: String(version.Browser ?? ''), 'Protocol-Version': String(version['Protocol-Version'] ?? '')};
// Error text carries no stack; absolute paths under the common roots are replaced (best effort; the runner's
// leak scan of the manifest is the control that matters).
const scrub = text => String(text).replace(/(file:\/\/)?\/(Users|home|private|var|tmp|opt|root)\/[^\s'"`)]*/g, '<path>');
const target = await getJSON('/json/new?about:blank', {method: 'PUT'});
// The control channel must be ws://127.0.0.1:<the chosen port>: a substituted local HTTP service could name another host.
let wsUrl = null;
try { wsUrl = new URL(String(target.webSocketDebuggerUrl)); } catch { /* rejected below */ }
if (!wsUrl || wsUrl.protocol !== 'ws:' || wsUrl.hostname !== '127.0.0.1' || wsUrl.port !== String(Number(portText))) {
  await getJSON(`/json/close/${target.id}`).catch(() => {});  // the throw below skips the finally: close the tab here
  throw new Error('CDP_WEBSOCKET_URL_REJECTED: the DevTools WebSocket URL is not ws://127.0.0.1:<the chosen port>');
}
const socket = new WebSocket(wsUrl.href);
const pending = new Map();
const problems = [];
let serial = 0;
const ready = new Promise((resolve, reject) => {
  const timer = setTimeout(() => reject(new Error('CDP connection timed out')), 10000);
  socket.addEventListener('open', () => {clearTimeout(timer); resolve();}, {once: true});
  socket.addEventListener('error', () => {clearTimeout(timer); reject(new Error('CDP connection failed'));}, {once: true});
});
ready.catch(() => {});  // the awaited copy below reports the failure; this avoids an early unhandled rejection
socket.addEventListener('message', event => {
  const message = JSON.parse(event.data);
  if (message.id) {
    const request = pending.get(message.id);
    if (!request) return;
    pending.delete(message.id);
    clearTimeout(request.timer);
    if (message.error) request.reject(new Error(JSON.stringify(message.error)));
    else request.resolve(message.result);
  } else if (message.method === 'Runtime.exceptionThrown') {
    problems.push({kind: 'javascript', details: message.params.exceptionDetails});
  } else if (message.method === 'Runtime.consoleAPICalled' && message.params.type === 'error') {
    problems.push({kind: 'console', details: message.params.args});
  } else if (message.method === 'Log.entryAdded' && message.params.entry.level === 'error') {
    problems.push({kind: 'browser', details: message.params.entry});
  } else if (message.method === 'Network.responseReceived' && message.params.response.status >= 400) {
    problems.push({kind: 'http', url: message.params.response.url, status: message.params.response.status});
  } else if (message.method === 'Network.loadingFailed' && !message.params.canceled) {
    problems.push({kind: 'network', details: message.params});
  }
});
socket.addEventListener('close', () => {
  for (const request of pending.values()) {clearTimeout(request.timer); request.reject(new Error('CDP closed'));}
  pending.clear();
});
function cdp(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = ++serial;
    const timer = setTimeout(() => {pending.delete(id); reject(new Error(`${method} timed out`));}, 20000);
    pending.set(id, {resolve, reject, timer});
    socket.send(JSON.stringify({id, method, params}));
  });
}
async function evaluate(expression) {
  const result = await cdp('Runtime.evaluate', {expression, awaitPromise: true, returnByValue: true});
  if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails));
  return result.result.value;
}
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
const stateExpression = `(() => {
  const visible = el => el.getBoundingClientRect().width > 0 && el.getBoundingClientRect().height > 0;
  const groups = [...document.querySelectorAll('.swagger-ui .opblock-tag-section')].filter(visible);
  const errors = [...document.querySelectorAll('.swagger-ui .errors-wrapper, .swagger-ui .error-wrapper')]
    .filter(visible).map(el => el.innerText.trim()).filter(Boolean);
  return {ready: document.readyState, errors, groups: groups.map(el => ({
    name: el.querySelector('.opblock-tag')?.getAttribute('data-tag') || el.querySelector('.opblock-tag')?.innerText.trim(),
    operations: el.querySelectorAll('.opblock').length,
    expanded: el.querySelector('.opblock-tag')?.getAttribute('aria-expanded'),
    height: el.getBoundingClientRect().height
  }))};
})()`;
await fs.mkdir(outputDir, {recursive: true});
const manifest = {requestedURL: url.href, startedAt: new Date().toISOString(),
  source_sha: sourceSHA.toLowerCase(), url: url.href, groups: [], images: [],
  browser, viewport: {width: 1280, height: 900, deviceScaleFactor: 1}, status: 'incomplete'};
try {
  await ready;
  for (const domain of ['Page', 'Runtime', 'Network', 'Log']) await cdp(`${domain}.enable`);
  await cdp('Emulation.setDeviceMetricsOverride', {...manifest.viewport, mobile: false});
  const navigation = await cdp('Page.navigate', {url: url.href});
  if (navigation.errorText) throw new Error(navigation.errorText);
  const deadline = Date.now() + 45000;
  let previous = '', stable = 0, state;
  while (Date.now() < deadline) {
    try { state = await evaluate(stateExpression); }
    catch (error) {
      if (!/context.*destroyed|Cannot find context|Inspected target navigated/i.test(String(error))) throw error;
      await pause(250); continue;
    }
    if (state.errors.length) throw new Error(`Swagger UI errors: ${state.errors.join('; ')}`);
    if (state.groups.length && state.ready === 'complete') {
      await evaluate(`(() => {
        for (const section of document.querySelectorAll('.swagger-ui .opblock-tag-section')) {
          const tag = section.querySelector('.opblock-tag');
          if (tag && (tag.getAttribute('aria-expanded') === 'false' || !section.classList.contains('is-open'))) tag.click();
        }
      })()`);
      const current = JSON.stringify(state.groups);
      stable = current === previous && state.groups.every(group => group.operations > 0 && group.expanded !== 'false') ? stable + 1 : 0;
      previous = current;
      if (stable >= 4) break;
    }
    await pause(500);
  }
  if (stable < 4) throw new Error('Swagger groups did not finish rendering within 45 seconds');
  await evaluate(`(() => { const style = document.createElement('style');
    style.textContent = '*, *::before, *::after { animation: none !important; transition: none !important; scroll-behavior: auto !important; }';
    document.head.appendChild(style); return document.fonts.ready.then(() => true); })()`);
  await pause(500);
  if (problems.length) throw new Error('Browser reported errors; see manifest');
  manifest.actualURL = await evaluate('location.href');
  manifest.url = manifest.actualURL;
  manifest.title = await evaluate('document.title');
  const actual = new URL(manifest.actualURL);
  if (!['localhost', '127.0.0.1', '[::1]'].includes(actual.hostname)) throw new Error('Navigation left local host');
  async function capture(filename, clip, metadata) {
    const result = await cdp('Page.captureScreenshot', {format: 'png', captureBeyondViewport: true,
      fromSurface: true, clip: {...clip, scale: 1}});
    const bytes = Buffer.from(result.data, 'base64');
    if (bytes.length < 24 || bytes.subarray(0, 8).toString('hex') !== '89504e470d0a1a0a') throw new Error('Invalid PNG');
    await fs.writeFile(path.join(outputDir, filename), bytes);
    manifest.images.push({file: filename, ...metadata, clip, width: bytes.readUInt32BE(16),
      height: bytes.readUInt32BE(20), bytes: bytes.length, sha256: crypto.createHash('sha256').update(bytes).digest('hex')});
  }
  await evaluate('window.scrollTo(0, 0)');
  const layout = await cdp('Page.getLayoutMetrics');
  const full = layout.cssContentSize;
  await capture('overview.png', {x: 0, y: 0, width: Math.ceil(full.width), height: Math.ceil(full.height)}, {kind: 'overview'});
  const groups = await evaluate(`(() => [...document.querySelectorAll('.swagger-ui .opblock-tag-section')]
    .filter(el => el.getBoundingClientRect().width > 0 && el.getBoundingClientRect().height > 0)
    .map((el, index) => ({index, name: el.querySelector('.opblock-tag')?.getAttribute('data-tag') || el.querySelector('.opblock-tag')?.innerText.trim(), operations: el.querySelectorAll('.opblock').length})))()`);
  manifest.groups = groups.map(group => group.name);
  for (const group of groups) {
    const clip = await evaluate(`(() => {
      const el = [...document.querySelectorAll('.swagger-ui .opblock-tag-section')]
        .filter(el => el.getBoundingClientRect().width > 0 && el.getBoundingClientRect().height > 0)[${group.index}];
      el.scrollIntoView({block: 'start'}); const r = el.getBoundingClientRect();
      const x = Math.max(0, Math.floor(r.left + scrollX)), y = Math.max(0, Math.floor(r.top + scrollY));
      return {x, y, width: Math.ceil(r.right + scrollX) - x, height: Math.ceil(r.bottom + scrollY) - y};
    })()`);
    await pause(100);
    const slug = (group.name || 'unnamed').normalize('NFKD').toLowerCase().replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 90) || 'group';
    await capture(`group-${String(group.index + 1).padStart(3, '0')}-${slug}.png`, clip,
      {kind: 'group', group: group.name, operationCount: group.operations});
  }
  const finalState = await evaluate(stateExpression);
  if (finalState.errors.length || problems.length) throw new Error('Errors appeared during capture');
  manifest.status = 'complete';
} catch (error) {
  manifest.error = scrub(error && error.message ? error.message : error);
  process.exitCode = 1;
} finally {
  manifest.finishedAt = new Date().toISOString();
  manifest.captured_at = manifest.finishedAt;
  manifest.browserErrors = problems;
  await fs.writeFile(path.join(outputDir, 'capture.json'), JSON.stringify(manifest, null, 2) + '\n');
  socket.close();
  await getJSON(`/json/close/${target.id}`).catch(() => {});
}
