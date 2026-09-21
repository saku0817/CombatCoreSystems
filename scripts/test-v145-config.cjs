// Isolated loopback smoke server only. Restore every changed document byte-for-byte.
const assert = require('node:assert/strict');
const base = process.env.CCS_TEST_URL || 'http://127.0.0.1:18767';
assert(['127.0.0.1','localhost'].includes(new URL(base).hostname));
assert(process.env.CCS_TEST_TOKEN);
const auth = 'Basic ' + Buffer.from('test:' + process.env.CCS_TEST_TOKEN).toString('base64');
async function request(name, body, etag, document = false) {
  return fetch(base + (document ? '/api/document?name=' : '/api/file?name=') + name, {
    signal: AbortSignal.timeout(30000), headers: {Authorization: auth, 'Content-Type':'text/yaml', ...(etag ? {'If-Match':etag} : {})},
    ...(body === undefined ? {} : {method:'POST',body})
  });
}
async function read(name) { const r=await request(name); assert.equal(r.status,200); return {text:await r.text(),etag:r.headers.get('etag')}; }
async function tree(name,text) { const r=await request(name,text,undefined,true); assert.equal(r.status,200); return (await r.json()).tree; }
async function write(name,document,status=200) {
  const before=await read(name), r=await request(name,JSON.stringify(document),before.etag);
  assert.equal(r.status,status,await r.text());
  if (status!==200) assert.equal((await read(name)).text,before.text);
}
async function main() {
  const original=await read('weapons.yml'); const defs=await tree('weapons.yml',original.text);
  const buffs=await tree('buffs.yml',(await read('buffs.yml')).text), effect=Object.keys(buffs.buffs)[0]; assert(effect);
  const id=Object.keys(defs.weapons)[0]; assert(id);
  try {
    defs.weapons[id].triggers={smoke:{event:'BEFORE_HIT',conditions:{stack:{id:'aim',scope:'EVENT_TARGET',min:1}},actions:[
      {type:'MODIFY_EVENT_STATS',modifiers:{override:{CRIT_RATE:1}}},
      {type:'DAMAGE',target:'EVENT_TARGET',reference:'ATK',multiplier:1.5,attribute:'THUNDER','def-ignore':.5}
    ]}};
    defs.weapons[id].talent ||= {};
    defs.weapons[id].talent['target-stack-policy']={'stack-id':'aim','max-targets':3,overflow:'REMOVE_OLDEST'};
    defs.weapons[id].talent.triggers={mark:{event:'NORMAL_ATTACK',actions:[{type:'ADD_STACK',id:'aim',scope:'TARGET',max:5}]}};
    defs.weapons[id].skill ||= {};
    defs.weapons[id].skill.actions=[{type:'HEAL',target:'LOWEST_HP_PARTY_MEMBER_OR_SELF',reference:'HP',multiplier:.25}];
    defs.weapons[id].ultimate ||= {};
    defs.weapons[id].ultimate.actions=[{type:'CREATE_FIELD',id:'smoke',duration:5,area:{shape:'FORWARD_BOX',width:5,height:5,length:5},'ally-effects':[effect],'effect-mode':'WHILE_INSIDE'}];
    await write('weapons.yml',defs);
    for (const mutation of [
      w=>w.triggers.smoke.event='NO_EVENT',
      w=>w.triggers.smoke.actions[0].type='NO_ACTION',
      w=>w.triggers.smoke.actions[1].target='NO_SELECTOR',
      w=>w.triggers.smoke.actions[1]['def-ignore']=1.01,
      w=>w.triggers.smoke.actions[1]['def-ignore']=-.1,
      w=>w.triggers.smoke.actions[0].modifiers={override:{NO_STAT:1}},
      w=>w.triggers.smoke.actions[0].modifiers={unknown:{CRIT_RATE:1}},
      w=>w.triggers.smoke.conditions.stack.scope='NO_SCOPE',
      w=>w.talent['target-stack-policy'].overflow='NO_OVERFLOW',
      w=>w.triggers.smoke.actions=[],
      w=>w.ultimate.actions[0].area.shape='NO_SHAPE',
      w=>w.ultimate.actions[0].area.width=-1,
      w=>w.ultimate.actions[0]['ally-effects']=['ccs_missing_v145'],
      w=>w.ultimate.actions[0].duration=-1
    ]) {
      const bad=structuredClone(defs); mutation(bad.weapons[id]); await write('weapons.yml',bad,422);
    }
    console.log('PASS: v1.4.5 generic actions, stack policy, field definition, 14 invalid settings and rollback');
  } finally {
    const current=await read('weapons.yml'); const r=await request('weapons.yml',original.text,current.etag);
    assert.equal(r.status,200,await r.text()); assert.equal((await read('weapons.yml')).text,original.text);
    console.log('PASS: original weapons.yml restored byte-for-byte');
  }
}
main().catch(error=>{ console.error(error); process.exitCode=1; });
