// Run only against the isolated local smoke server. Every modified file is restored byte-for-byte.
const assert = require('node:assert/strict');
const base = process.env.CCS_TEST_URL || 'http://127.0.0.1:18766';
assert(['127.0.0.1', 'localhost'].includes(new URL(base).hostname));
assert(process.env.CCS_TEST_TOKEN);
const auth = 'Basic ' + Buffer.from('test:' + process.env.CCS_TEST_TOKEN).toString('base64');
async function request(name, body, etag, document = false) {
  return fetch(base + (document ? '/api/document?name=' : '/api/file?name=') + name, {
    signal: AbortSignal.timeout(30000), headers: {Authorization: auth, 'Content-Type': 'text/yaml', ...(etag ? {'If-Match': etag} : {})},
    ...(body === undefined ? {} : {method: 'POST', body})
  });
}
async function read(name) { const response = await request(name); assert.equal(response.status, 200); return {text: await response.text(), etag: response.headers.get('etag')}; }
async function write(name, tree, status = 200) {
  const before = await read(name); const response = await request(name, JSON.stringify(tree), before.etag);
  assert.equal(response.status, status, await response.text());
  if (status !== 200) assert.equal((await read(name)).text, before.text, 'Rejected definition must not change the file');
}
async function tree(name, text) { const response = await request(name, text, undefined, true); assert.equal(response.status, 200); return (await response.json()).tree; }
async function main() {
  const originals = new Map();
  for (const name of ['sets.yml', 'equipment.yml', 'mobs.yml']) originals.set(name, await read(name));
  const buffsFile = await read('buffs.yml'); const buffs = await tree('buffs.yml', buffsFile.text);
  const effect = Object.keys(buffs.buffs)[0]; assert(effect);
  try {
    const sets = await tree('sets.yml', originals.get('sets.yml').text);
    sets.sets ||= {}; assert(!sets.sets.ccs_smoke_v144);
    sets.sets.ccs_smoke_v144 = {name:'test', 'two-piece':{triggers:{skill:{event:'SKILL', effects:[effect]}, low:{event:'HP_BELOW','hp-percent':0.3,effects:[effect]}}}};
    await write('sets.yml', sets);
    const equipment = await tree('equipment.yml', originals.get('equipment.yml').text);
    equipment.equipment ||= {}; assert(!equipment.equipment.ccs_smoke_v144);
    equipment.equipment.ccs_smoke_v144 = {name:'test', material:'DIAMOND_CHESTPLATE', slot:'CHEST', rarity:5, set:'ccs_smoke_v144',
      'main-stat-candidates':{CRIT_RATE:{'level-1':.05,'max-level':.3,weight:1},CRIT_DAMAGE:{'level-1':.1,'max-level':.6,weight:2}},
      'substat-candidates':{CRIT_RATE:{value:.05},CRIT_DAMAGE:{value:.1},ATK_PERCENT:{value:.05},HP_FLAT:{value:25}}};
    await write('equipment.yml', equipment);
    const invalid = structuredClone(equipment); invalid.equipment.ccs_smoke_v144['main-stat-candidates'].CRIT_RATE.weight = 0;
    await write('equipment.yml', invalid, 422);
    const duplicate = structuredClone(equipment); duplicate.equipment.ccs_smoke_v144['substat-candidates'].crit_rate = {value:.1};
    await write('equipment.yml', duplicate, 422);
    const badTrigger = structuredClone(sets); badTrigger.sets.ccs_smoke_v144['two-piece'].triggers.skill.effects = ['missing_v144_buff'];
    await write('sets.yml', badTrigger, 422);
    const badRatio = structuredClone(sets); badRatio.sets.ccs_smoke_v144['two-piece'].triggers.low['hp-percent'] = 30;
    await write('sets.yml', badRatio, 422);
    const mobs = await tree('mobs.yml', originals.get('mobs.yml').text);
    mobs.mobs ||= {}; assert(!mobs.mobs.ccs_smoke_v144);
    mobs.mobs.ccs_smoke_v144 = {name:'test', 'entity-type':'PIG',material:'PIG_SPAWN_EGG',level:{min:1,max:1},stats:{hp:{min:20,max:20},atk:{min:2,max:2},def:{min:0,max:0}}};
    await write('mobs.yml', mobs);
    mobs.mobs.ccs_smoke_v144.material = 'NOT_A_MATERIAL'; await write('mobs.yml', mobs, 422);
    console.log('PASS: v1.4.4 equipment pools, set triggers, icons, invalid definitions and rollback');
  } finally {
    for (const name of ['mobs.yml', 'equipment.yml', 'sets.yml']) {
      const current = await read(name); const original = originals.get(name).text;
      const response = await request(name, original, current.etag); assert.equal(response.status, 200, await response.text());
      assert.equal((await read(name)).text, original);
    }
    console.log('PASS: every smoke configuration restored byte-for-byte');
  }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
