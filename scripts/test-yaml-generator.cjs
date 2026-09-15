// Browser checks for the standalone generator. No server/network connection is required.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {chromium} = require(process.env.CCS_PLAYWRIGHT_MODULE || 'playwright');
async function main() {
    const browser = await chromium.launch(fs.existsSync(chromium.executablePath()) ? {headless:true, timeout:60000} : {channel:'msedge', headless:true, timeout:60000});
    try {
        const context = await browser.newContext({viewport:{width:1440,height:1000}, acceptDownloads:true});
        const page = await context.newPage(), errors=[], requests=[];
        page.on('pageerror', e=>errors.push(e.message));
        page.on('request', r=>{if(r.url().startsWith('http'))requests.push(r.url());});
        await page.goto(pathToFileURL(path.resolve('src/main/resources/web-editor.html')).href);
        await page.getByRole('heading',{name:'YAML生成',exact:true}).waitFor();
        fs.mkdirSync('server/v142-web-evidence/generated',{recursive:true});
        const types={weapon:'weapons',equipment:'equipment',set:'sets',heart:'divine-hearts',mob:'mobs',vanilla:'vanilla-mobs',boss:'bosses',buff:'buffs',reaction:'reactions'};
        for(const [type,root] of Object.entries(types)) {
            await page.locator('#gen-type').selectOption(type);
            if(type==='weapon') {
                await page.locator('#g-name').fill('<gold>引用符 " と : のある武器</gold>');
                await page.locator('#g-skill-enabled').check({force:true});
                await page.locator('#g-skill-reference').selectOption('HP');
                await page.locator('#g-lore').fill('<gray>1行目</gray>\n<gray>2行目</gray>');
            }
            if(type==='heart') await page.locator('#g-reaction-enabled').check({force:true});
            await page.locator('#generate').click({force:true});
            const text=await page.locator('#generated').inputValue(), parsed=await page.evaluate(()=>generatedDocument);
            assert(text.includes('data-version: 1'));
            assert.equal(parsed['data-version'],1);
            assert(parsed[root]['new_'+type]);
            if(type==='weapon') {
                assert.equal(parsed.weapons.new_weapon.skill.reference,'HP');
                assert.equal(parsed.weapons.new_weapon.lore.length,2);
                assert(parsed.weapons.new_weapon.name.includes('" と :'));
            }
            if(type==='heart') assert.equal(parsed['divine-hearts'].new_heart.rules['reaction-override'].damage.multiplier,2);
            const download=page.waitForEvent('download'); await page.locator('#gen-download').click({force:true});
            const result=await download;
            assert(result.suggestedFilename().endsWith('.yml'));
            await result.saveAs(path.resolve('server/v142-web-evidence/generated',type+'-'+result.suggestedFilename()));
            console.log('PASS: offline YAML generation and download: '+type);
        }
        await page.screenshot({path:'server/v142-web-evidence/generator.png',fullPage:true});
        const nested=await page.evaluate(()=>yamlText({'data-version':1,components:[{reference:'ATK',multiplier:2},{reference:'HP',multiplier:1}]}));
        fs.writeFileSync('server/v142-web-evidence/generated/nested.yml',nested);
        await page.getByRole('button',{name:'ダメージシミュレータ',exact:true}).click({force:true});
        assert.equal(await page.locator('#result').textContent(),'315');
        await page.locator('#critical').check({force:true});
        assert.equal(await page.locator('#result').textContent(),'473');
        await page.screenshot({path:'server/v142-web-evidence/simulator.png',fullPage:true});
        assert.deepEqual(errors,[]); assert.deepEqual(requests,[]);
        console.log('PASS: no external requests, no JavaScript errors, compound damage 315 and critical 473');
    } finally {await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
