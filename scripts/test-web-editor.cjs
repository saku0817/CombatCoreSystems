// Run against an isolated local smoke server, never a production configuration.
// CCS_TEST_TOKEN is required. CCS_PLAYWRIGHT_MODULE optionally enables browser checks.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const base = process.env.CCS_TEST_URL || 'http://127.0.0.1:18765';
assert(['127.0.0.1', 'localhost', '[::1]'].includes(new URL(base).hostname), 'Only loopback test servers are allowed');
const token = process.env.CCS_TEST_TOKEN;
assert(token, 'CCS_TEST_TOKEN is required');
const auth = { Authorization: 'Basic ' + Buffer.from('ccs:' + token).toString('base64') };
async function call(route, body, etag) {
    return fetch(base + route, body === undefined ? {headers: auth} : {
        method: 'POST', headers: {...auth, 'Content-Type': 'text/yaml', ...(etag ? {'If-Match': etag} : {})}, body
    });
}
async function main() {
    assert.equal((await fetch(base + '/api/summary')).status, 401);
    assert.equal((await call('/api/summary')).status, 200);
    assert.equal((await call('/api/file?name=../outside.yml')).status, 400);
    const route = '/api/file?name=buffs.yml';
    const first = await call(route), original = await first.text(), originalTag = first.headers.get('etag');
    assert(originalTag);
    assert.equal((await call(route, 'data-version: 1\nbuffs: [')).status, 422);
    const edit = await call('/api/document?name=buffs.yml&newId=ccs_v142_http_test', original);
    assert.equal(edit.status, 200);
    const proposed = await edit.json();
    assert.equal(proposed.tree.buffs.ccs_v142_http_test.duration, 10);
    assert.equal(proposed.tree.buffs.ccs_v142_http_test.modifiers.flat.ATK_PERCENT, .1);
    assert.equal((await call(route, proposed.yaml)).status, 428);
    const invalidDefinition = proposed.yaml.replace(/^([ \t]*)kind: BUFF[ \t]*$/m, '$1kind: INVALID_KIND');
    assert.notEqual(invalidDefinition, proposed.yaml);
    assert.equal((await call(route, invalidDefinition, originalTag)).status, 422);
    assert.equal(await (await call(route)).text(), original, 'Invalid definitions must be restored byte-for-byte');
    let saved = false;
    try {
        const result = await call(route, proposed.yaml, originalTag);
        assert.equal(result.status, 200); saved = true;
        assert((await result.json()).backup);
        assert.equal((await call(route, original, originalTag)).status, 409, 'Stale writers must be rejected');
    } finally {
        if (saved) {
            const latest = await call(route);
            assert.equal((await call(route, original, latest.headers.get('etag'))).status, 200);
            assert.equal(await (await call(route)).text(), original);
        }
    }
    console.log('PASS: authentication, allowlist, YAML validation, templates, required ETag, rollback, successful save, concurrent edit protection, restoration');
    if (!process.env.CCS_PLAYWRIGHT_MODULE) return;
    const {chromium} = require(process.env.CCS_PLAYWRIGHT_MODULE);
    const browser = await chromium.launch(fs.existsSync(chromium.executablePath()) ? {headless: true} : {channel: 'msedge', headless: true});
    try {
        const context = await browser.newContext({httpCredentials: {username: 'ccs', password: token}, viewport: {width: 1440, height: 1000}});
        const page = await context.newPage(), errors = [];
        page.on('pageerror', e => errors.push(e.message));
        await page.goto(base); await page.getByText('接続中', {exact: false}).waitFor();
        fs.mkdirSync('server/v142-web-evidence', {recursive: true});
        await page.screenshot({path: 'server/v142-web-evidence/dashboard.png', fullPage: true});
        await page.getByRole('button', {name: '武器', exact: true}).click({force:true});
        await page.locator('#live').getByText('ナビゲーター', {exact: true}).waitFor();
        await page.screenshot({path: 'server/v142-web-evidence/weapon.png', fullPage: true});
        await page.getByRole('button', {name: 'ダメージシミュレータ', exact: true}).click({force:true});
        await page.locator('#global').fill('2');
        assert.equal(await page.locator('#result').textContent(), '315');
        await page.locator('#critical').check({force:true});
        assert.equal(await page.locator('#result').textContent(), '473');
        await page.screenshot({path: 'server/v142-web-evidence/simulator.png', fullPage: true});
        assert.deepEqual(errors, []);
        console.log('PASS: browser dashboard, weapon/talent preview, compound 315 damage, critical 473 damage, no JavaScript errors');
    } finally { await browser.close(); }
}
main().catch(error => {console.error(error); process.exitCode = 1;});
