// Headless logic regression test; does not replace browser rendering checks.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const html=fs.readFileSync('src/main/resources/web-editor.html','utf8');
const script=html.match(/<script>([\s\S]*?)<\/script>/)[1].replace('navigate(offline?1:0).catch(error);','');
const nodes=new Map();
const node=id=>{
  if(!nodes.has(id))nodes.set(id,{value:'',textContent:'',disabled:false,readOnly:false,append(){},
    set innerHTML(value){this.html=value;const option=value.match(/<option value="([^"]+)"/);if(option)this.value=option[1];},get innerHTML(){return this.html||'';}});
  return nodes.get(id);
};
const context=vm.createContext({document:{getElementById:node,createElement:()=>node('button'),querySelector:()=>node('aside')},window:{addEventListener(){}},location:{protocol:'file:'},console});
vm.runInContext(script,context);
const set=(id,value)=>node(id).value=String(value);
set('gen-type','equipment');set('g-id','test_gear');set('g-name','テスト装備');set('g-material','IRON_CHESTPLATE');set('g-lore','');set('g-set','');
let checks=0;
for(const slot of ['HEAD','CHEST','LEGS','FEET','RESONANCE'])for(const rarity of [3,4,5]){
  set('g-slot',slot);set('g-rarity',rarity);set('g-initial-level',1);
  vm.runInContext('equipmentFieldsChanged();generateYaml()',context);
  const gear=JSON.parse(vm.runInContext('JSON.stringify(generatedDocument.equipment.test_gear)',context));
  assert.equal(gear.slot,slot);assert.equal(gear['max-level'],{3:9,4:12,5:15}[rarity]);
  assert.equal(gear['main-stat'],undefined);assert.equal(Object.keys(gear['substat-candidates']).length,8);
  assert(vm.runInContext('equipmentMains[document.getElementById("g-slot").value].includes(document.getElementById("g-main").value)',context));
  assert.equal(node('g-max-level').readOnly,true);checks++;
}
set('g-initial-level',16);assert.throws(()=>vm.runInContext('generateYaml()',context),/初期レベル/);checks++;
set('g-initial-level',1);set('g-main','DEF_IGNORE');assert.throws(()=>vm.runInContext('generateYaml()',context),/部位/);checks++;
console.log('PASS v1.4.6 generator: '+checks+' cases; all slot/rarity combinations and invalid inputs');
