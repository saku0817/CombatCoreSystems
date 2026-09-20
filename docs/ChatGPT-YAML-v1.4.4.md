# CombatCoreSystems v1.4.4 — YAML生成GPTへの変更点

対象はPaper 26.2 / Java 25。引き継ぎ書と参照資料を併用し、このファイルを旧バージョンの説明より優先してください。運用中のID・表示文・他の設定を保持し、ルートキーを重複させずに差分をマージします。

## 1. 装備の獲得時抽選 — equipment.yml

```yaml
data-version: 1
equipment:
  traveler_chest:
    name: '<blue>旅人のチェストプレート</blue>'
    material: DIAMOND_CHESTPLATE
    slot: CHEST
    rarity: 5
    max-level: 15
    main-stat-candidates:
      CRIT_RATE: {level-1: 0.05, max-level: 0.30, weight: 1}
      CRIT_DAMAGE: {level-1: 0.10, max-level: 0.60, weight: 1}
    substat-candidates:
      ATK_PERCENT: {value: 0.05, weight: 1}
      HP_FLAT: {value: 25, weight: 2}
      CRIT_RATE: {value: 0.05, weight: 1}
      CRIT_DAMAGE: {value: 0.10, weight: 1}
      DEF_PERCENT: {value: 0.05, weight: 1}
    initial-unlocked-substats: 0
    set: traveler
    lore: ['<gray>未知を歩く者の装備。</gray>']
```

- `main-stat-candidates`：StatKeyをキーに、`level-1`／`max-level`／`weight`を持つマップ。メイン1つを抽選。weight初期1、必ず正の有限値。
- `substat-candidates`：StatKeyをキーに、`value`／`weight`を持つマップ。レア度−1個（上限4、候補数が少なければその数）を重複なしで抽選。
- `value`省略時は固定値系25、それ以外0.05。メイン`level-1`も同じ既定値、`max-level`省略時はlevel-1と同じ。生成GPTは数値を明示すること。
- メインとサブの同一StatKeyは許可。サブ同士の重複は不可。重みは相対値であり、複数サブ抽選の各回で選択済みを候補から除外する。
- 割合は0.05=5%。初期開放数0なら抽選済みサブは未開放として表示。強化時の開放規則・増加量（固定25／割合0.05）は従来と同じ。
- 部位ごとのメイン制限：HEAD=HP/DEF、CHEST=会心率/会心ダメージ、LEGS=ATK、FEET=HP/ATK/DEF/会心、RESONANCE=属性ダメージ/耐性。固定値／割合のあるものは両方可。
- 既存の`main-stat`、`substats`、`initial-substats`も互換対応。候補マップがある場合は抽選を優先。固定例から抽選へ移す場合、不要なinitial-substatsとinitial-upgradesは削除を提案すること。
- 新規生成時だけ抽選し、選択したメイン種別と成長値・サブ種別と値を個体へ保存。強化・再読込・再ログインでは再抽選しない。
- 旧個体は以前と同様に定義のmain-statを参照する。旧個体を維持したい場合、そのmain-statを残したまま新しい候補マップを追加すること。
- 管理GUIでの編集は個体の上書きであり、YAML定義自体を書き換えない。

## 2. 条件付きセット効果 — sets.yml + buffs.yml

```yaml
# buffs.yml（既存buffsへ追記）
buffs:
  traveler_power:
    name: 旅人の鼓舞
    kind: BUFF
    target: SELF
    duration: 10
    max-stacks: 1
    reapply: REFRESH
    modifiers:
      percent: {ATK_PERCENT: 0.20}
  traveler_weakness:
    name: 旅人の反撃
    kind: DEBUFF
    target: ENEMY
    duration: 5
    max-stacks: 1
    reapply: REFRESH
    modifiers:
      percent: {ATK_PERCENT: -0.20}
```

```yaml
# sets.yml（既存setsへ追記）
sets:
  traveler:
    name: 旅人の軌跡
    two-piece:
      description: スキル発動時、攻撃力+20%を10秒。再発動待ち15秒。
      modifiers: {CRIT_DAMAGE: 0.16}
      triggers:
        skill_power:
          event: SKILL
          target: SELF
          effects: [traveler_power]
          cooldown-seconds: 15
    four-piece:
      description: 被弾時に攻撃者の攻撃力を低下。HP30%以下になった時に自身を強化。
      triggers:
        retaliation:
          event: TAKE_DAMAGE
          target: OTHER
          effects: [traveler_weakness]
          cooldown-seconds: 10
        emergency:
          event: HP_BELOW
          hp-percent: 0.30
          target: SELF
          effects: [traveler_power]
          cooldown-seconds: 30
```

`two-piece.triggers`と`four-piece.triggers`は任意ID→設定のマップ。常時の`modifiers`と併用可能。残響もセット数に含む（神心は含まない）。

| キー | 値・意味 |
| --- | --- |
| event | SKILL / ULTIMATE / HIT / TAKE_DAMAGE / HP_BELOW |
| target | SELF（自身、既定）/ OTHER（命中対象・被弾時の攻撃者） |
| effects | buffs.ymlで定義済みのIDの非空リスト。BUFF/DEBUFFいずれも指定可能 |
| cooldown-seconds | 再発動待ち0～86400秒。省略1秒。プレイヤー・セット・段階・トリガーごと |
| hp-percent | HP_BELOWの閾値0～1、既定0.5。0.3=30% |

SKILL/ULTIMATEは発動成功時（空撃ち含む）。HIT/TAKE_DAMAGEは実ダメージが正の時。通常攻撃・技・CCSの継続ダメージ・属性反応に対応。環境ダメージはTAKE_DAMAGEのSELFのみ（攻撃者がいないため）。命中・被弾による効果は安全のため次tickに適用し、その時点でも必要セット数を満たしている必要がある。

HP_BELOWは5tickごとに監視し、条件を満たしていない状態から満たした時に発動。低HPでセットを揃えた時も対象。低HPのまま毎回再発動はしない。HPが回復して条件を外れた後、再び閾値以下になり、待ち時間が終わっていれば再発動する。待ち時間中に閾値へ入った場合は、その滞在中に遅延発動はしない。HP_BELOWのtargetはSELFのみ。

セットを外した後は新規発動しないが、付与済み効果はbuffs.ymlのduration/permanent/reapplyに従う。再発動待ちはサーバー内のメモリ管理で、ログアウト／再起動で消去する。

空撃ちでOTHERの対象がいなければ効果を付与しない。スキル発動トリガーは技ダメージ計算より前に動くため、自己バフはその技のダメージにも影響する。

## 3. モブ・ボスの図鑑アイコン

`mobs.yml: mobs.ID.material`、`mobs.yml: vanilla-mobs.ID.material`、`bosses.yml: bosses.ID.material`へアイテムMaterial名を指定。

```yaml
material: ZOMBIE_HEAD # ボスならDRAGON_HEADなど
```

エンティティ種類を決める`entity-type`とは別。省略時は通常モブZOMBIE_HEAD、ボスDRAGON_HEAD。未発見は引き続きBARRIER。

## 4. 追加操作と表示

- `/ccsadmin menu`：チェスト形式の管理GUI。`combatcoresystems.admin.menu`が必要。個体編集には`combatcoresystems.admin.itemstats`、付与や各コマンドには従来の個別権限も必要。admin.*に含む。
- `/ccs status`：自分だけにチャット表示。戦闘中も可。`combatcoresystems.command.status`（一般権限）を追加。
- 管理コマンドの対象には`@s`、`@p`、`@a`、`@r`、`@e[type=player]`等を使用可能。セレクター結果はオンラインプレイヤーのみ。個体のGUI編集は1人のみ。
- GUI表示の追加キーは`gui.yml`の`admin.*`、`navigation.*`、`stats.source-line`、`stats.other-name`、`stats.chat-title`、`equipment.manage-equipped`、`equipment.unequip`、`encyclopedia.rarity-title`、`encyclopedia.all-rarities`。
- 武器・装備・神心の図鑑カテゴリにはレア度選択を挟む。rarityの定義方式は変更しない。
- 技の使用だけでは戦闘状態にしない。敵への正のダメージで戦闘に入る。属性反応名は白、ダメージ数値は付着属性の色。

## GPTへの確認事項

候補・数値・抽選重み、初期開放数、部位、セット数、発動イベント、対象、閾値、待ち時間、効果の寿命を決める。説明文だけでは効果は発動しない。buffs.yml→sets.yml→equipment.ymlの順で参照を揃え、存在しないIDを残さない。セット定義の名前やLoreではなく、実キーを出力すること。
