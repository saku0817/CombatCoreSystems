// Integration check against an isolated local CCS server; configuration is restored in finally.
const assert = require('node:assert/strict');
const base = process.env.CCS_TEST_URL || 'http://127.0.0.1:18766';
assert(['127.0.0.1', 'localhost'].includes(new URL(base).hostname));
assert(process.env.CCS_TEST_TOKEN);
const headers = {Authorization: 'Basic ' + Buffer.from('test:' + process.env.CCS_TEST_TOKEN).toString('base64')};
async function call(route, body, tag) {
    return fetch(base + route, {signal: AbortSignal.timeout(30000), headers: {...headers, 'Content-Type':'text/yaml', ...(tag ? {'If-Match':tag} : {})},
        ...(body === undefined ? {} : {method:'POST', body})});
}
async function main() {
    const route = '/api/file?name=weapons.yml', first = await call(route), original = await first.text();
    assert.equal(first.status, 200);
    const document = await (await call('/api/document?name=weapons.yml', original)).json();
    const weapon = document.tree.weapons[Object.keys(document.tree.weapons)[0]];
    weapon['limit-breaks'] = {
        1: {'base-atk': {'level-1': 120, 'level-100':650}, talent:{hand:'HOT_BAR',modifiers:{HP_PERCENT:0.6}}, skill:{'cooldown-seconds':25}},
        2: {ultimate:{charges:3, description:['Stage 2']}},
        5: {talent:{hand:'INVENTORY'}}
    };
    let saved = false;
    try {
        assert.equal((await call(route, JSON.stringify(document.tree), first.headers.get('etag'))).status, 200); saved = true;
        const good = await call(route), goodText = await good.text();
        weapon['limit-breaks'][5].talent.hand = 'INVALID_HAND';
        assert.equal((await call(route, JSON.stringify(document.tree), good.headers.get('etag'))).status, 422);
        assert.equal(await (await call(route)).text(), goodText);
        console.log('PASS: full staged weapon reload, HOT_BAR/INVENTORY, invalid later stage rejected and rolled back');
    } finally {
        if (saved) {
            const latest = await call(route);
            assert.equal((await call(route, original, latest.headers.get('etag'))).status, 200);
            assert.equal(await (await call(route)).text(), original);
            console.log('PASS: original weapons.yml restored byte-for-byte');
        }
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
