# CombatCoreSystems v1.4.5 — YAML・実装参照資料

この資料は配布ソースから機械的に収録しています。生成時は引き継ぎ書・ChatGPT-YAML-v1.4.5.mdも参照してください。運用中の秘密情報は含めないでください。

## bosses.yml

```yaml
data-version: 1
# v1.4.4: bosses.ID.material: DRAGON_HEAD で図鑑アイコンを指定。
# Boss追加例: bosses.dragon: {name: "<red>Dragon</red>", entity-type: ENDER_DRAGON, level: {min: 100, max: 100}, custom-exp: 10000}
bosses: {}
```

## buffs.yml

```yaml
data-version: 1
# v1.4.5: modifiers.override: {CRIT_RATE: 1.0} で最終会心率を100%へ固定。
# 複数overrideは最後に付与/再付与された有効Buffが優先。割合の加算とは異なります。
# Buff追加例: buffs.power: {kind: BUFF, target: SELF, duration: 10, modifiers: {percent: {ATK_PERCENT: 0.2}}}
# 武器の self-effects / target-effects からIDで参照します。効果の数値はここで管理。
buffs:
  departure_crit:
    name: 出航
    kind: BUFF
    target: SELF
    duration: 30
    max-stacks: 1
    reapply: REFRESH
    modifiers:
      flat:
        CRIT_DAMAGE: 0.48
  sunset:
    name: 落日
    kind: DEBUFF
    target: ENEMY
    duration: 3
    max-stacks: 1
    reapply: REFRESH
    modifiers:
      percent:
        ATK_PERCENT: -0.3
        # 対象が攻撃を受ける際、その時点の有効防御力の40%を無視します。
        DEF_IGNORED_WHEN_HIT: 0.4
```

## config.yml

```yaml
data-version: 1
# レンタルサーバー向け管理画面。外部公開時はホスト側のHTTPSリバースプロキシとFWを使用してください。
web-editor:
  enabled: false
  bind: 127.0.0.1
  port: 8765
  threads: 2
  # 16文字以上に変更するまで起動しません。GitHub等へ実トークンをコミットしないでください。
  token: CHANGE_ME
controls:
  # 対象がいなくてもCT・コスト・自己効果を適用して発動成功にする。
  allow-empty-cast: true
  # BE等で不発を調べる際だけtrueにし、ドロップイベントの分類をサーバーログで確認します。
  debug-inputs: false
  # HUDの常時表示で発動失敗理由が消えないよう、自分のチャットにも表示。
  failure-chat: true
  # Geyserが選択中ホットバースロットの破棄をInventoryClickへ変換した場合のBE専用補完。
  # BEで選択中スロットをインベントリ画面から破棄した場合もスキル扱い。他スロットの破棄は通常通り。
  bedrock-selected-slot-drop-skill: true
  # 手持ちCCS武器をドロップしようとすると、落とさずにスキルを発動。インベントリからの破棄は除外。
  drop-skill: true
  # しゃがみ＋攻撃で必殺技。通常攻撃はキャンセル。コマンドは引き続き併用可能。
  sneak-attack-ultimate: true
# 基本設定例: combat-duration-seconds: 15 とすると戦闘状態が15秒続きます。
server-name: "<gold>CombatCoreSystems</gold>"
pvp-enabled: true
combat-duration-seconds: 10
attribute-attachment-seconds: 5
autosave-minutes: 20
backup-hours: 12
backup-retention: 7
# 最終ダメージ全体へ掛ける倍率です。1.0で旧v1.2.0相当、標準値2.0でDEF 0の対象に概ねATK相当を与えます。
damage:
  global-multiplier: 2.0
text-display:
  enabled: true
  # 超過分も含む元の総ダメージ値を通常表示形式で表示します。
  show-overdamage: true
  duration-seconds: 0.8
  max-per-player: 20
hud:
  # 視線の先にいるMobのHPを表示。遮蔽物越しの対象は表示しません。
  target-bossbar-enabled: true
  target-range: 24.0
  sidebar-enabled: true
  actionbar-enabled: true
  update-ticks: 5
  ready-sound:
    skill:
      enabled: true
      sound: minecraft:block.note_block.pling
      volume: 0.8
      pitch: 1.2
    ultimate:
      enabled: true
      sound: minecraft:block.amethyst_block.chime
      volume: 0.8
      pitch: 0.8
region-wand:
  material: WOODEN_AXE
damage-chat:
  # true: 与えたダメージを攻撃した本人のチャットだけに表示。TextDisplayとは独立。
  enabled: false
performance:
  max-operations-per-tick: 250
attribute-colors:
  FIRE: "#ff0000"
  WATER: "#0000cd"
  WIND: "#3cb371"
  THUNDER: "#4b0082"
  MOON: "#6495ed"
rarity-colors:
  1: "#ffffff"
  2: "#55ff55"
  3: "#5555ff"
  4: "#aa00aa"
  5: "#ffaa00"
```

## divine_hearts.yml

```yaml
data-version: 1
# v1.4.5: divine-hearts.ID.triggers に武器/セットと共通のTriggerを追加可能。
# 例: on_skill: {event: SKILL, actions: [{type: APPLY_EFFECT, effect: fire_power}]}
# 参照するfire_power等はbuffs.ymlへ別途定義してください。
# 各神の心内に enchantment-glint: true / false を指定できます（省略時false）。
# 以下は divine-hearts: {} を置き換える例です。神の心はレベル・限界突破を持ちません。
# divine-hearts:
#   divine-heart-flame:
#     name: "<red>炎の神心</red>"
#     material: NETHER_STAR
#     rarity: 4
#     lore:
#       - "<gray>炎を宿した心</gray>"
#     modifiers:
#       FIRE_DAMAGE: 0.20
#     rules:
#       normal-attack-attribute: FIRE
#       skill:
#         attribute: FIRE
#         multiplier: 1.15
#         radius-add: 1.0
#       ultimate:
#         attribute: FIRE
#         multiplier: 1.25
#         radius-add: 2.0
# modifiers だけでも装備可能。rules は任意です。
# 装備GUIの「神の心」枠→所持品の神心を選択。実物は所持品に残り、1個体を登録します。
# 実物を手放すと装備登録が解除されます。戦闘中は着脱できません。
divine-hearts: {}
# 完全な天賦・属性反応置換例:
# divine-hearts:
#   sun_god_heart:
#     name: '<red><bold>火神の神心</bold></red>'
#     material: NETHER_STAR
#     rarity: 5
#     enchantment-glint: true
#     modifiers: {FIRE_DAMAGE: 0.50}
#     talents:
#       solar_blessing:
#         name: 太陽の祝福
#         color: red
#         description:
#           - '<#ff0000>火属性</#ff0000>ダメージ <yellow>+50%</yellow>'
#           - '通常攻撃が<#ff0000>火属性</#ff0000>攻撃に変更される。（武器指定がある場合は武器を優先）'
#           - '<#ff0000>火属性</#ff0000>による属性反応を<red><u><b>「烈日」</b></u></red>へ変更する。'
#       blazing_sun:
#         name: 烈日
#         color: red
#         description:
#           - '反応時、半径3mに攻撃力200%の火属性ダメージ。火属性耐性-20%、10秒。反応CT15秒。'
#     rules:
#       normal-attack-attribute: FIRE
#       reaction-override:
#         source-attribute: FIRE
#         id: blazing_sun
#         name: 烈日
#         radius: 3.0
#         cooldown-seconds: 15.0
#         damage: {reference: ATK, multiplier: 2.0, attribute: FIRE}
#         resistance-down: {attribute: FIRE, amount: 0.20, duration-seconds: 10.0}
#     lore: ['<gray>太陽のように光り輝いている。</gray>']
```

## encyclopedia.yml

```yaml
data-version: 1
# 図鑑カテゴリ例: categories.custom: {name: "<yellow>特殊</yellow>", icon: BOOK, discovery: PUBLIC}
categories:
  weapons:
    name: "<gold>武器</gold>"
    icon: IRON_SWORD
    discovery: PUBLIC
  equipment:
    name: "<blue>装備</blue>"
    icon: DIAMOND_CHESTPLATE
    discovery: PUBLIC
  divine_hearts:
    name: "<light_purple>神心</light_purple>"
    icon: NETHER_STAR
    discovery: PUBLIC
  materials:
    name: "<green>強化素材</green>"
    icon: EXPERIENCE_BOTTLE
    discovery: PUBLIC
  mobs:
    name: "<red>敵Mob</red>"
    icon: ZOMBIE_HEAD
    discovery: ENCOUNTER
  bosses:
    name: "<dark_red>敵Boss</dark_red>"
    icon: DRAGON_HEAD
    discovery: ENCOUNTER
custom-entries: {}
```

## equipment.yml

```yaml
data-version: 1
# v1.4.5: equipment.ID.triggers に共通Triggerを追加可能。
# 例: heal_power: {event: OVERHEAL, actions: [{type: APPLY_DYNAMIC_MODIFIER,
#       stat: ATK_FLAT, source: EVENT_OVERHEAL, multiplier: 0.1, duration: 10}]}
# 各装備内に enchantment-glint: true / false を指定できます（省略時false）。
# 正式名は equipment.yml。equipments.yml の equipment: / equipments: も互換読込します。
# 同じIDを両方に定義しないでください。以下は equipment: {} を置き換える例です。
# equipment:
#   iron_head:
#     name: "<gray>鉄の兜</gray>"
#     material: IRON_HELMET
#     slot: HEAD
#     rarity: 3
#     max-level: 9
#     main-stat:
#       type: DEF_FLAT
#       level-1: 5
#       max-level: 20
#     substats:
#       - HP_PERCENT
#       - CRIT_RATE
#     lore:
#       - "<gray>守りを固める兜</gray>"
#   flame_resonance:
#     name: "<red>炎の残響</red>"
#     material: BLAZE_POWDER
#     slot: RESONANCE
#     rarity: 4
#     max-level: 12
#     main-stat:
#       type: FIRE_DAMAGE
#       level-1: 0.05
#       max-level: 0.20
#     substats: [ATK_PERCENT, CRIT_RATE]
# 割合は 0.20 = 20%。表示は日本語、設定キーは英語のままです。
# HEAD: HP/DEF、CHEST: 会心、LEGS: ATK、FEET: HP/ATK/DEF/会心、RESONANCE: 属性ダメージ/耐性。
# 装備GUIの空の部位をタップして候補を選びます。戦闘中は変更できません。
equipment: {}
# v1.4.4 抽選例（equipment.ID 配下。割合は0.05=5%。獲得時に選び個体へ保存）:
#     main-stat-candidates:
#       CRIT_RATE: {level-1: 0.05, max-level: 0.30, weight: 1}
#       CRIT_DAMAGE: {level-1: 0.10, max-level: 0.60, weight: 1}
#     substat-candidates:
#       ATK_PERCENT: {value: 0.05, weight: 1}
#       HP_FLAT: {value: 25, weight: 2}
#       CRIT_RATE: {value: 0.05, weight: 1}
#       CRIT_DAMAGE: {value: 0.10, weight: 1}
#       DEF_PERCENT: {value: 0.05, weight: 1}
#     initial-unlocked-substats: 0
# main-stat-candidates はメイン1個、substat-candidates は最大 rarity-1 個（上限4）を重複なしで抽選。
# メインとサブの重複は許可。weight は正の相対抽選重み。部位別メイン制限は従来どおり。
# サブ抽選指定時は initial-substats より優先。省略時は従来の固定例／substatsを互換読込。
# 既存個体は再抽選しません。新形式の main-stat は個体にレベル1と最大レベルの値を保存。
# 強化時のサブステ増加量は従来どおり固定値25／割合0.05です。
# 完全なツールチップ・固定サブステータス例:
# equipment:
#   fictional_traveler_chest:
#     name: '<blue><bold>虚構の旅人のチェストプレート</bold></blue>'
#     material: DIAMOND_CHESTPLATE
#     slot: CHEST
#     rarity: 5
#     max-level: 25
#     initial-level: 9 # 作成時のレベル。省略時1。既存アイテムには適用しません。
#     main-stat: {type: CRIT_DAMAGE, level-1: 0.60, max-level: 0.60}
#     substats: [ATK_PERCENT, HP_FLAT, CRIT_RATE, CRIT_DAMAGE]
#     initial-substats: {ATK_PERCENT: 0.10, HP_FLAT: 50, CRIT_RATE: 0.05, CRIT_DAMAGE: 0.05}
#     initial-upgrades: {ATK_PERCENT: 1, HP_FLAT: 1, CRIT_RATE: 0, CRIT_DAMAGE: 0}
#     initial-unlocked-substats: 3
#     set: fictional_traveler
#     lore:
#       - '<gray>「真実だけを辿れば、いつか世界の果てへ着けると思っていた。」</gray>'
#       - '<gray>旅人は幾つもの星を渡り、幾つもの物語を見届けた。</gray>'
#       - '<gray>「歴史とは、起きた出来事の集積ではない。」</gray>'
#       - '<gray>だから旅人は歩き続ける。</gray>'
#       - '<gray>虚構と現実の境界がとうに失われた道を、その足跡を残して...</gray>'
```

## gui.yml

```yaml
data-version: 1
# GUI用アイコン全体のエンチャント光。実物のCCSアイテムは各定義を参照します。
enchantment-glint: false
stats:
  other-name: '<white>その他の補正</white>'
  source-line: '<white><source>：<stat> <yellow><value></yellow></white>'
  chat-title: '<gold>自分のステータス</gold>'
  title: "<dark_aqua>✦ ステータス ✦</dark_aqua>"
  border-material: GRAY_STAINED_GLASS_PANE
  health-name: "<red>♥ 体力</red>"
  attack-name: "<gold>⚔ 攻撃力</gold>"
  defense-name: "<aqua>◆ 防御力</aqua>"
  critical-name: "<light_purple>✧ 会心</light_purple>"
# GUI表示例: name: "<aqua>表示名</aqua>"。MiniMessage形式を使用できます。
main-menu:
  title: "<dark_gray>CombatCoreSystems</dark_gray>"
  size: 54
  entries:
    stats:
      slot: 10
      icon: PLAYER_HEAD
      name: "<aqua>ステータス</aqua>"
    equipment:
      slot: 12
      icon: DIAMOND_CHESTPLATE
      name: "<gold>装備</gold>"
    skilltree:
      slot: 14
      icon: OAK_SAPLING
      name: "<green>スキルツリー</green>"
    rebirth:
      slot: 16
      icon: NETHER_STAR
      name: "<light_purple>新生回帰</light_purple>"
    party:
      slot: 29
      icon: TOTEM_OF_UNDYING
      name: "<yellow>Party</yellow>"
    enhancement:
      slot: 31
      icon: ANVIL
      name: "<blue>強化</blue>"
    encyclopedia:
      slot: 33
      icon: KNOWLEDGE_BOOK
      name: "<aqua>図鑑</aqua>"
    settings:
      slot: 40
      icon: COMPARATOR
      name: "<gray>設定</gray>"
enhancement:
  select-all: "<green>全て選択</green>"
  select-all-lore: "<gray>対応する実物・数値素材を全て選択します。確定するまで消費しません。</gray>"
  clear-selection: "<yellow>選択を解除</yellow>"
  duplicate-title: "<light_purple>消費する同名武器を選択</light_purple>"
  duplicate-confirm-title: "<red>この武器を消費しますか？</red>"
  duplicate-confirm: "<green>右の武器を1本消費して、左の武器を限界突破</green>"
  no-duplicate: "<red>消費可能な同名武器がありません</red>"
  failure-reasons:
    target_not_weapon: "対象の武器が見つかりません。選び直してください。"
    limit_break_maximum: "限界突破段階が最大です。"
    invalid_material: "対象武器自身は素材にできません。"
    materials_changed: "選択した素材が移動・変更されました。選び直してください。"
    select_duplicate_weapon: "GUIで消費する同名武器を選んでください。"
  title: "<blue>強化</blue>"
  player:
    name: "<green>プレイヤーレベルアップ</green>"
    lore: "<gray>3種類の素材を選んでレベルアップ</gray>"
  weapon:
    name: "<aqua>武器レベルアップ＆限界突破</aqua>"
    lore: "<gray>所持武器から選択</gray>"
  equipment:
    name: "<gold>装備レベルアップ</gold>"
    lore: "<gray>所持・装備中の装備から選択</gray>"
  conversion:
    name: "<light_purple>変換</light_purple>"
    lore: "<gray>実物素材・数値素材・等級を相互変換</gray>"
  weapon-list-title: "<aqua>武器を選択</aqua>"
  equipment-list-title: "<gold>装備を選択</gold>"
  detail-title: "<blue>新強化GUI</blue>"
  conversion-title: "<light_purple>強化素材の変換</light_purple>"
  preview-name: "<white>強化プレビュー</white>"
  player-target: "<yellow>プレイヤー Lv.<level></yellow>"
  preview-before: "<gray>強化前: Lv.<level> EXP <exp></gray>"
  preview-after: "<green>強化後: Lv.<level> EXP <exp></green>"
  preview-gain: "<yellow>上昇幅: Lv. +<levels> / EXP +<exp></yellow>"
  material-count: "<gray>選択: <selected> / 所持: <available></gray>"
  material-exp: "<gray>獲得EXP: <exp></gray>"
  tap-controls: "<yellow>上下のボタンで使用数を変更</yellow>"
  add-one: "<green>1個増やす</green>"
  remove-one: "<red>1個減らす</red>"
  limit-break: "<light_purple>限界突破</light_purple>"
  limit-break-lore: "<gray>同じ武器を1本消費</gray>"
  confirm: "<green>この内容で強化</green>"
  success: "<green>強化しました。Lv.<level></green>"
  limit-break-success: "<green>限界突破しました。</green>"
  failure: "<red>強化できません: <reason></red>"
  conversion-failed: "<red>変換に必要な素材または空きがありません。</red>"
  conversion-count: "<gray>実物: <physical> / 数値: <virtual></gray>"
  conversion-select: "<yellow>タップして変換方法を選択</yellow>"
  deposit: "<green>実物1個 → 数値1個</green>"
  withdraw: "<green>数値1個 → 実物1個</green>"
  upgrade-tier: "<yellow>数値10個 → 上級1個</yellow>"
  downgrade-tier: "<yellow>数値1個 → 下級10個</yellow>"
equipment:
  manage-equipped: '<green>装備中 — タップで交換・解除</green>'
  unequip: '<yellow>この部位を外す</yellow>'
  equipped: "<green>装備中 — タップで解除</green>"
  failed: "<red>装備を変更できません。戦闘状態・所持品の空き・設定を確認してください。</red>"
  select: "<green>所持品から装備を選ぶ</green>"
  list-title: "<gold>タップして装備</gold>"
encyclopedia:
  rarity-title: '<gold>レア度を選択</gold>'
  all-rarities: '<white>全レア度</white>'
  detail-title: "<aqua>図鑑・詳細</aqua>"
party:
  leave: "<red>退出</red>"
  invite: "<green>プレイヤーを招待</green>"
  disband: "<red>パーティを解散</red>"
  invitations: "<yellow>届いた招待</yellow>"
  actions-title: "<yellow>パーティ操作</yellow>"
  kick: "<red>メンバーを除名</red>"
  transfer: "<yellow>リーダーを移譲</yellow>"
  accept: "<green>招待を承認</green>"
  decline: "<red>招待を辞退</red>"
  confirm: "<green>実行する</green>"
  invited: "<yellow>招待が届きました。/ccs open party で確認できます。</yellow>"
hud:
  # 複数スタックの技は戦闘外でも残数を表示します。
  ability-actionbar: '<yellow>スキル: <skill_status></yellow> <gray>|</gray> <gold>必殺技: <ultimate_status></gold>'
  ability-ready: 発動可能
  ability-undefined: 未設定
  ability-cooldown: 'あと<seconds>秒'
  ability-stacks: '<remaining>/<maximum> <status>'
  target-health: "<hp> / <max_hp>"
  # sidebar-linesで使用可能: <server>, <player>, <level>, <exp>, <required_exp>, <exp_remaining>, <hp>, <max_hp>, <atk>, <def>
  sidebar-title: "<gold><server></gold>"
  sidebar-lines:
  - "<white><player></white>"
  - ""
  - "<yellow>Lv.<level></yellow>"
  - "<gray>次のレベルまで、あと <exp_remaining></gray>"
  - ""
  - "<red>HP <hp> / <max_hp></red>"
  # 使用可能: <combat_remaining>, <skill_status>, <ultimate_status>
  combat-actionbar: "<red>⚔ 戦闘中 <combat_remaining>秒</red> <gray>|</gray> <yellow>スキル: <skill_status></yellow> <gray>|</gray> <gold>必殺技: <ultimate_status></gold>"
  attribute-actionbar: "<attributes>"

# v1.4.4: Java/BE共通の単一タップ操作。
navigation:
  previous: '<yellow>前のページ</yellow>'
  next: '<yellow>次のページ</yellow>'
  back: '<yellow>戻る</yellow>'
  search: '<aqua>検索</aqua>'
  search-hint: '<gray>チャットへ名前を入力</gray>'
admin:
  title: '<dark_red>CCS 管理メニュー</dark_red>'
  target: '<yellow>対象：<target></yellow>'
  target-hint: '<white>名前またはセレクターを入力（初期値 @s）</white>'
  target-prompt: '対象の名前／セレクターを入力してください。'
  items: '<green>独自アイテムを取り出す</green>'
  item-stats: '<gold>利き手の装備ステータスを編集</gold>'
  item-stats-hint: '<white>メイン・サブステを個体単位で編集。YAML定義は変更しません。</white>'
  commands: '<aqua>管理コマンド操作</aqua>'
  close: '<yellow>閉じる</yellow>'
  back: '<yellow>戻る</yellow>'
  previous: '<yellow>前のページ</yellow>'
  next: '<yellow>次のページ</yellow>'
  denied: '<red>権限がありません。</red>'
  edit-title: '<gold>装備個体の編集</gold>'
  choose-stat: '<gold>ステータスを選択</gold>'
  main-stat: '<yellow>メインステを変更</yellow>'
  main-values: 'Lv.1の値 最大Lvの値 を空白区切りで入力（例 0.05 0.60）'
  add-substat: '<green>サブステを追加</green>'
  sub-value: '値を入力（5%なら0.05）。既存のサブステとの重複はできません。'
  unlocked: '<aqua>開放済みサブステ数</aqua>'
  sub-edit-hint: '<white>値 強化回数 を入力。deleteでこのサブステを削除。</white>'
  updated: '<green>装備個体を更新しました。</green>'
  input-cancel: 'cancelで中止。対象に@s等を使用できます。'
  confirm-title: '<red>実行内容を確認</red>'
  confirm-summary: '<white>実行する操作</white>'
  confirm: '<green>確定して実行</green>'
  cancel: '<yellow>中止</yellow>'
  # 例: command-names.edit-level: プレイヤーレベル変更
  command-names: {}
```

## levels.yml

```yaml
data-version: 1
# materials.<ID>.enchantment-glint: true / false で素材の光を指定（省略時false）。
# 独自レベル設定例: mode は QUADRATIC / LINEAR / FIXED / TABLE を指定できます。
player:
  max-level: 100
  # Minecraftのハート表示を常に1行（10個）に縮尺表示します。
  minecraft-health-scale: 20.0
  # Minecraft内部の上限を超えるHPは仮想HPとして保持し、この値までの実HPへ比例変換します。
  minecraft-max-health: 1024.0
  hp:
    start: 20.0
    end: 3000.0
  atk:
    start: 2.0
    end: 200.0
  def:
    start: 0.0
    end: 100.0
  required-exp:
    mode: QUADRATIC
    base: 100
    growth: 25
  # TABLE使用例:
  # values: {1: 100, 2: 150, 3: 225}
  # バニラ武器の攻撃力をCCS ATKへ加算します。値と倍率は自由に変更できます。
  vanilla-weapons:
    enabled: true
    conversion-multiplier: 1.0
    attack-values:
      WOODEN_SWORD: 4.0
      GOLDEN_SWORD: 4.0
      STONE_SWORD: 5.0
      IRON_SWORD: 6.0
      DIAMOND_SWORD: 7.0
      NETHERITE_SWORD: 8.0
      WOODEN_AXE: 7.0
      GOLDEN_AXE: 7.0
      STONE_AXE: 9.0
      IRON_AXE: 9.0
      DIAMOND_AXE: 9.0
      NETHERITE_AXE: 10.0
      TRIDENT: 9.0
      MACE: 6.0
      BOW: 6.0
      CROSSBOW: 9.0
  rebirth:
    hp: 100.0
    atk: 20.0
    def: 10.0
weapon-exp:
  mode: QUADRATIC
  base: 100
  growth: 25
equipment-exp:
  mode: QUADRATIC
  base: 100
  growth: 25
materials:
  # 素材追加例: custom_book: {name: "<green>修練書</green>", material: BOOK, exp: 250, type: PLAYER}
  player_exp_low:
    name: "<white>初級修練書</white>"
    material: PAPER
    exp: 100
    type: PLAYER
  player_exp_mid:
    name: "<aqua>中級修練書</aqua>"
    material: BOOK
    exp: 1000
    type: PLAYER
  player_exp_high:
    name: "<gold>上級修練書</gold>"
    material: ENCHANTED_BOOK
    exp: 10000
    type: PLAYER
  weapon_exp_low:
    name: "<white>粗製強化鉱</white>"
    material: RAW_IRON
    exp: 100
    type: WEAPON
  weapon_exp_mid:
    name: "<aqua>精製強化鉱</aqua>"
    material: IRON_INGOT
    exp: 1000
    type: WEAPON
  weapon_exp_high:
    name: "<gold>高純度強化鉱</gold>"
    material: DIAMOND
    exp: 10000
    type: WEAPON
  equipment_exp_low:
    name: "<white>微光の繊維</white>"
    material: STRING
    exp: 100
    type: EQUIPMENT
  equipment_exp_mid:
    name: "<aqua>輝光の繊維</aqua>"
    material: GLOWSTONE_DUST
    exp: 1000
    type: EQUIPMENT
  equipment_exp_high:
    name: "<gold>星光の繊維</gold>"
    material: PRISMARINE_CRYSTALS
    exp: 10000
    type: EQUIPMENT
```

## messages.yml

```yaml
data-version: 1
# 空文字にすると発動通知を省略できます（天賦は所持条件を満たした時だけ通知）。
ability-announcement:
  skill: "<aqua>スキル発動：<name></aqua>"
  ultimate: "<gold>必殺技発動：<name></gold>"
  talent: "<green>天賦発動：<name></green>"
material-tooltip:
  usage: "<gray>用途：<yellow><target>強化用</yellow></gray>"
  targets:
    PLAYER: プレイヤー
    WEAPON: 武器
    EQUIPMENT: 装備
equipment-tooltip:
  slot: '<white>装備部位：<u><slot></u></white>'
  level: '<white>Lv.<yellow><level></yellow> / <yellow><max_level></yellow></white>'
  main-stat: '<yellow>➽ <stat> +<value></yellow>'
  substat-open: '<white>・<stat> <yellow>+<value></yellow> <aqua>[+<upgrades>]</aqua></white>'
  substat-locked: '<gray>・<stat> +<value> [未開放]</gray>'
  set-title: '<yellow><u>「<set>」</u></yellow> <white>シリーズ</white>'
  set-active: '<green>・<pieces>セット <description> [発動中]</green>'
  set-inactive: '<gray>・<pieces>セット <description> [未発動]</gray>'
  slots:
    HEAD: ヘルメット
    CHEST: チェストプレート
    LEGS: レギンス
    FEET: ブーツ
    RESONANCE: 残響
divine-heart-tooltip:
  slot: '<white>装備部位：<u>神心</u></white>'
  talent-title: '<yellow>➽ <talent_color><u><b>「<name>」</b></u></talent_color></yellow>'
# v1.4.0 武器ツールチップ。headerは行の追加・削除・並べ替えが可能です。
weapon-tooltip:
  # description未指定の既存武器に表示する要約です。
  ability-summary: '<white><reference> × <multiplier> / <attribute> / CT <cooldown>秒 / <charges>スタック</white>'
  categories:
    MELEE: 近接
    RANGED: 遠距離
    UNCATEGORIZED: 未指定
  colors:
    FIRE: '#ff0000'
    WATER: '#55aaff'
    WIND: '#55ffaa'
    THUNDER: '#cc88ff'
    MOON: '#ddddff'
    PHYSICAL: '#ffffff'
  header:
    - '<white>カテゴリ：<u><category></u></white>'
    - "<hover:show_text:'<white>次のレベルまであと <yellow><exp_remaining></yellow></white>'><white>Lv.<yellow><level></yellow> / <yellow>100</yellow></white></hover>"
    - '<white>限界突破段階：<yellow><break></yellow> / <yellow>5</yellow></white>'
    - '<white>武器攻撃力: <yellow><attack></yellow></white>'
    - '<attribute_color><attribute></attribute_color><white>ダメージ <yellow><bonus>%</yellow></white>'
    - '<white>装備可能レベル: <yellow><min_level></yellow> ~ <yellow><max_level></yellow></white>'
  talent-title: '<#adff2f>➽ 天賦 <u><b><name></b></u></#adff2f>'
  skill-title: '<#adff2f>➽ スキル <u><b><name></b></u></#adff2f>'
  ultimate-title: '<#adff2f>➽ 必殺技 <u><b><name></b></u></#adff2f>'
# 表示名のみを変更します。設定キー・保存データは変更しません。
# 例: FIRE_DAMAGE: "炎属性ダメージ"
display-names: {}
skill-failure:
  loading: "<yellow>プレイヤーデータを読み込み中です。</yellow>"
  weapon: "<red>スキルを設定したCCS武器を手に持ってください。</red>"
  unknown-weapon: "<red>この武器の設定が読み込まれていません。</red>"
  equip-level: "<red>武器の装備可能レベルを満たしていません。</red>"
  undefined: "<red>この武器には使用する技が設定されていません。</red>"
  conditions: "<red>技の発動条件を満たしていません。体力・対象・距離・戦闘状態を確認してください。</red>"
  target: "<red>対象が見つかりません。対象に照準を合わせてください。</red>"
item-lore:
  # 神心ルール・セット効果。利用可能: <label>, <key>, <value>
  effect: "<gray><label>: <key> = <value></gray>"
# MiniMessage形式で全表示文を編集できます。例: <red>赤文字</red>
prefix: "<dark_gray>[<gold>CCS</gold>]</dark_gray> "
no-permission: "<red>権限がありません。</red>"
player-only: "<red>プレイヤーのみ実行できます。</red>"
combat-command-blocked: "<red>戦闘中は一般CCSコマンドを使用できません。</red>"
reloaded: "<green>設定を再読み込みしました。</green>"
reload-failed: "<red>設定の検証に失敗したため、現在の正常な設定を維持しました。</red>"
saved: "<green>データを保存しました。</green>"
backup-created: "<green>バックアップを作成しました: <id></green>"
exp-gained: "<aqua>独自EXP +<amount>（<current>/<required>）</aqua>"
level-up: "<green>Lv.<old_level> → Lv.<level>！ HP +<hp_gain> / ATK +<atk_gain> / DEF +<def_gain></green>"
weapon-audit-moved: "<yellow>同カテゴリの武器はホットバー/オフハンドに1本だけ置けます。余分な武器を移動しました。</yellow>"
rebirth-complete: "<light_purple>新生回帰を行いました。回数: <count></light_purple>"
party-friendly-fire: "<yellow>同じPartyのメンバーには攻撃できません。</yellow>"
encyclopedia-unlock: "<aqua>図鑑に新しい対象を登録しました: <name></aqua>"
attribute:
  attached: "<gray><attribute>属性が付与されています。残り: <seconds>秒</gray>"
  reaction: "<aqua><reaction> <damage></aqua>"
damage-display:
  normal: "<white><damage></white>"
  critical: "<gold>CRIT <damage></gold>"
  reaction: "<aqua><reaction> <damage></aqua>"
  # v1.2.1以降、オーバーダメージもnormal/critical/reactionと同じ形式で総ダメージを表示します。
  overdamage: "<white><damage></white>"
  heal: "<green>+<heal></green>"
command:
  itemlevel: "<green><player> の手持ちアイテムをLv.<level>にしました（残りEXPは0）。</green>"
  updated: "<green>設定を更新しました。</green>"
  force: "<green><player> の戦闘状態を <state> にしました。</green>"
rebirth-failure:
  level: "<red>必要レベルに達していません。新生回帰にはLv.<required>が必要です（現在Lv.<level>）。</red>"
  combat: "<red>戦闘中は新生回帰できません。</red>"
damage-chat:
  # <damage>には属性色・会心・反応名を含むダメージ表示が入ります。
  format: "<gray>[ダメージ]</gray> <damage>"
```

## mobs.yml

```yaml
data-version: 1
# v1.4.4: mobs.ID.material: ZOMBIE_HEAD で図鑑アイコンを指定（vanilla-mobs.ID.materialも同様）。
virtual-health:
  # Minecraft内部へ同期する表示用HPの上限。CCS上のMob HP自体には上限を設けません。
  physical-cap: 1024
# 個別定義のないバニラMobの報酬。個別定義のcustom-exp/drop-custom-expが優先されます。
vanilla-defaults:
  custom-exp: 5
  drop-custom-exp: true
# CCS独自Mob追加例:
# mobs:
#   flame_zombie:
#     name: "<red>炎のゾンビ</red>"
#     entity-type: ZOMBIE
#     level: {min: 1, max: 10}
#     stats:
#       hp: {min: 30.0, max: 120.0}
#       atk: {min: 4.0, max: 15.0}
#       def: {min: 0.0, max: 10.0}
#     native-attribute: FIRE
#     custom-exp: 25
#     drop-custom-exp: true
#     show-level: true
mobs: {}

# バニラMob編集例。未登録のバニラMobもMinecraft armorを0として扱います。
# 同じentity-typeは1定義だけ登録してください。
# vanilla-mobs:
#   zombie:
#     name: "<green>ゾンビ</green>"
#     entity-type: ZOMBIE
#     level: {min: 1, max: 5}
#     stats:
#       hp: {min: 20.0, max: 40.0}
#       atk: {min: 3.0, max: 6.0}
#       def: {min: 0.0, max: 0.0}
#     native-attribute: PHYSICAL
#     custom-exp: 10
#     drop-custom-exp: true
#     show-level: true
vanilla-mobs: {}
```

## reactions.yml

```yaml
data-version: 1
# 属性反応追加例: attributesに2属性、damage-componentsに各倍率、cooldownに同一対象CT秒数を指定します。
reactions:
  vaporize:
    name: "蒸発"
    attributes:
    - FIRE
    - WATER
    radius: 0.0
    cooldown: 2.0
    damage-components:
      FIRE: 0.5
      WATER: 0.5
  fire_wind:
    name: "炎風"
    attributes:
    - FIRE
    - WIND
    radius: 2.0
    cooldown: 1.5
    levitation: 0.35
    damage-components:
      FIRE: 1.0
      WIND: 0.3
  sky_fire:
    name: "天火"
    attributes:
    - FIRE
    - THUNDER
    radius: 0.0
    cooldown: 2.5
    hits: 3
    multi-hit-attribute: THUNDER
    damage-components:
      FIRE: 0.5
      THUNDER: 0.5
  moon_burn:
    name: "月燃焼"
    attributes:
    - FIRE
    - MOON
    radius: 0.0
    cooldown: 3.0
    resistance-down:
      attribute: FIRE
      amount: 0.2
      duration: 2.0
    damage-components:
      FIRE: 0.5
      MOON: 1.0
  wind_release:
    name: "風解"
    attributes:
    - WATER
    - WIND
    radius: 2.0
    cooldown: 1.5
    levitation: 0.35
    damage-components:
      WATER: 1.0
      WIND: 0.3
  electro_charged:
    name: "感電"
    attributes:
    - WATER
    - THUNDER
    radius: 0.0
    cooldown: 2.5
    hits: 3
    multi-hit-attribute: THUNDER
    damage-components:
      WATER: 0.5
      THUNDER: 0.5
  moon_water:
    name: "月水天"
    attributes:
    - WATER
    - MOON
    radius: 0.0
    cooldown: 3.0
    resistance-down:
      attribute: WATER
      amount: 0.2
      duration: 2.0
    damage-components:
      WATER: 1.0
      MOON: 0.5
  thunder_drift:
    name: "雷飄"
    attributes:
    - WIND
    - THUNDER
    radius: 2.0
    cooldown: 1.5
    levitation: 0.35
    damage-components:
      THUNDER: 1.0
      WIND: 0.3
  moon_wind:
    name: "月風蝕"
    attributes:
    - WIND
    - MOON
    radius: 0.0
    cooldown: 3.0
    levitation: 0.35
    resistance-down:
      attribute: WIND
      amount: 0.2
      duration: 2.0
    damage-components:
      WIND: 1.5
      MOON: 0.5
  moon_thunder:
    name: "月天雷"
    attributes:
    - THUNDER
    - MOON
    radius: 0.0
    cooldown: 3.0
    hits: 3
    multi-hit-attribute: THUNDER
    resistance-down:
      attribute: THUNDER
      amount: 0.2
      duration: 2.0
    damage-components:
      THUNDER: 0.3
      MOON: 1.5
```

## regions.yml

```yaml
data-version: 1
# Region追加例: regions.field: {world: world, full-height: true, pos1: {x: 0, z: 0}, pos2: {x: 100, z: 100}, flags: {pvp: allow}}
regions: {}
```

## sets.yml

```yaml
data-version: 1
# v1.4.5: two-piece/four-piece.triggers は武器と共通のconditions/actionsに対応。
# 例: guard: {event: TAKE_DAMAGE, max-activations: 3, activation-scope: COMBAT,
#             actions: [{type: APPLY_EFFECT, effect: guard_def, target: SELF}]}
# guard_defはbuffs.ymlで定義してください。旧effects形式もそのまま使用可能。
# セット追加例: sets.warrior.two-piece.modifiers.ATK_PERCENT: 0.15
sets: {}
# v1.4.4: two-piece/four-piece配下に triggers を追加可能。効果本体はbuffs.ymlに定義。
#     two-piece:
#       triggers:
#         on_skill:
#           event: SKILL # SKILL / ULTIMATE / HIT / TAKE_DAMAGE / HP_BELOW
#           target: SELF # SELF=自身 / OTHER=命中対象・被弾時の攻撃者。HP_BELOWはSELFのみ
#           effects: [attack_up] # buffs.ymlで定義済みのID。未定義なら再読込を拒否
#           cooldown-seconds: 10 # 初期1秒、0～86400秒
#         emergency:
#           event: HP_BELOW
#           hp-percent: 0.30 # 30%。HP条件は初めて満たした時に発動（5tickごと判定）
#           target: SELF
#           effects: [emergency_shield]
#           cooldown-seconds: 30
# 残響もセット数に含みます。外すと新たな発動は止まりますが付与済み効果はbuffs.ymlの寿命に従います。
# 実装例（equipment.ymlのfictional_traveler_chestと組み合わせる）:
# sets:
#   fictional_traveler:
#     name: 虚構辿る旅人の軌跡
#     two-piece:
#       description: 会心ダメージ+16%
#       modifiers: {CRIT_DAMAGE: 0.16}
#     four-piece:
#       description: 敵にダメージを与える時、敵の防御力を10%無視する。
#       modifiers: {DEF_IGNORE: 0.10}
```

## skill_trees.yml

```yaml
data-version: 1
# スキルツリー追加例: trees.combat.name: "戦闘ツリー"
trees: {}
```

## spawns.yml

```yaml
data-version: 1
# 自動Spawn例: spawns.field_zombie: {enabled: true, region: field, definition: flame_zombie, boss: false, amount: 1, max-alive: 5, interval-seconds: 30}
spawns: {}
```

## storage.yml

```yaml
data-version: 1
# MySQL使用例: backendを MYSQL に変更し、下の接続情報を設定してください。
backend: SQLITE
sqlite:
  file: data.db
mysql:
  host: localhost
  port: 3306
  database: combatcoresystems
  username: root
  password: ""
  use-ssl: false
```

## weapons.yml

```yaml
data-version: 1
# v1.4.5: 武器/talent/skill/ultimate の triggers から共通Actionを実行できます。
# 例（武器IDの下）:
# triggers:
#   mark:
#     event: NORMAL_ATTACK
#     actions: [{type: ADD_STACK, id: aim, scope: TARGET, amount: 1, max: 5, duration: 10}]
#   focused:
#     event: BEFORE_HIT
#     conditions: {stack: {id: aim, scope: EVENT_TARGET, min: 3}}
#     actions: [{type: MODIFY_EVENT_STATS, modifiers: {percent: {ATK_PERCENT: 0.5}}}]
# skill.actions / ultimate.actions は技の成功時に実行。actionsがある新定義は省略時damage-enabled=false。
# 旧技ダメージも併用する場合だけdamage-enabled: true。詳細はChatGPT-YAML-v1.4.5.md。
# v1.4.3: talent.hand は MAIN_HAND / OFF_HAND / EITHER_HAND / HOT_BAR / INVENTORY。
# HOT_BAR=0～8、INVENTORY=0～35とオフハンド。これらは天賦の発動場所で、基礎ATKは手持ちだけ。
# limit-breaks は0～5。指定キーだけ前段階へ累積上書きし、説明も実効果も同じ段階を使用します。
# 例（武器IDの下へ追加）:
# limit-breaks:
#   1:
#     base-atk: {level-1: 120, level-100: 600}
#     attribute-bonus: {type: FIRE, value: 0.20}
#     talent:
#       hand: HOT_BAR
#       modifiers: {HP_PERCENT: 0.60}
#       description: ['ホットバーにある間、最大HP+60%。']
#     skill:
#       name: '<red>強化された出航</red>'
#       cooldown-seconds: 25
#       description: ['CT25秒。効果はself-effectsが参照するbuffs.ymlで設定。']
#   2:
#     ultimate:
#       multiplier: 3
#       description: ['ATK300%の攻撃。damage-componentsを定義した技では成分リストが優先。']
# 各武器内に enchantment-glint: true / false を指定できます（省略時false）。
# category/type: MELEE、RANGED、UNCATEGORIZED（同時所持制限なし）。type指定時はtypeを優先。
# 下記は動作する実装例。既存サーバーでは buffs.yml の2つの定義も一緒に追加してください。
# 倍率・割合: 0.3=30%、2.0=200%。descriptionは説明のみ、効果は別キーで設定します。
weapons:
  guiding_star:
    name: '<red><bold>夜を照らす導きの星</bold></red>'
    material: DIAMOND_SWORD
    category: MELEE
    rarity: 5
    base-atk:
      level-1: 100
      level-100: 100
    attribute-bonus:
      type: FIRE
      value: 0.15
    equip-level:
      min: 1
      max: 100
    normal-attack:
      attribute: FIRE
      visual:
        particle: FLAME
        count: 12
        spread: 0.4
    talent:
      name: '<red>ナビゲーター</red>'
      description:
        - 'この武器を利き手に所持しているとき、常に自身の最大HP<yellow><u>+50%</u></yellow>。'
      # MAIN_HAND / OFF_HAND / EITHER_HAND。multiplierはmodifiers全体に掛けます。
      hand: MAIN_HAND
      multiplier: 1.0
      modifiers:
        HP_PERCENT: 0.5
    skill:
      id: departure
      name: '<red>出航</red>'
      description:
        - '現在の<yellow><u>HP30%</u></yellow>を消費し、自身に会心ダメージ<yellow><u>+48%</u></yellow>のバフを<yellow><u>30</u></yellow>秒間付与する。'
        - '現在のHPが最大HPの<yellow><u>30%</u></yellow>を下回っている場合は発動できない。CT<yellow><u>30</u></yellow>秒。'
      target: SELF
      damage-enabled: false
      reference: ATK
      multiplier: 1.0
      cooldown-seconds: 30
      charges: 1
      conditions:
        min-hp-percent: 0.3
      cost:
        current-hp-percent: 0.3
      self-effects: [departure_crit]
      visual:
        particle: ENCHANT
        count: 40
        spread: 0.8
        sound: minecraft:entity.player.levelup
        volume: 0.7
        pitch: 1.2
    ultimate:
      id: morning_star
      name: '<red>今宵、明けの明星が墜ちる</red>'
      description:
        - '攻撃した敵単体に<yellow><u>攻撃力200%</u></yellow>＋<yellow><u>攻撃力100%に炎属性補正を適用した値</u></yellow>の炎属性ダメージを与える。'
        - 'デバフ「落日」を<yellow><u>3</u></yellow>秒間付与する。CT<yellow><u>80</u></yellow>秒。'
      target: ENEMY
      radius: 0
      attribute: FIRE
      reference: ATK
      # damage-components指定時は、各項の合計にこのmultiplierを掛けます。
      multiplier: 1.0
      damage-components:
        - reference: ATK
          multiplier: 2.0
          # PHYSICAL = この項には属性ダメージ補正を掛けない。
          bonus-attribute: PHYSICAL
        - reference: ATK
          multiplier: 1.0
          bonus-attribute: FIRE
      cooldown-seconds: 80
      charges: 1
      conditions:
        requires-target: true
        max-distance: 16
      target-effects: [sunset]
      visual:
        particle: FLAME
        count: 80
        spread: 1.0
        sound: minecraft:entity.blaze.shoot
        volume: 1.0
        pitch: 0.8
    lore:
      - '<gray>「開拓の旅は、今この瞬間から始まる」</gray>'
      - '<gray>列車は広大な宇宙を駆け抜け、ナビゲーターと仲間たちが肩を並べて進んだ旅の奇跡は、揺るがぬ強固なレールとなる。</gray>'
      - '<gray>「一緒に、群星が照らす未来へ向かいましょう！」</gray>'
```

## DefinitionRegistry.java

```java
package com.github.saku0817.combatcoresystems.config;

import com.github.saku0817.combatcoresystems.model.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public final class DefinitionRegistry {
    public static final List<String> FILES = List.of(
            "config.yml", "messages.yml", "storage.yml", "levels.yml", "reactions.yml", "buffs.yml",
            "sets.yml", "gui.yml", "weapons.yml", "equipment.yml", "divine_hearts.yml", "skill_trees.yml",
            "mobs.yml", "bosses.yml", "regions.yml", "spawns.yml", "encyclopedia.yml"
    );

    private final JavaPlugin plugin;
    private final AtomicReference<Snapshot> current = new AtomicReference<>();

    public DefinitionRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void createDefaults() {
        for (String file : FILES) {
            File target = new File(plugin.getDataFolder(), file);
            if (!target.exists()) plugin.saveResource(file, false);
            else mergeMissingDefaults(file, target);
        }
    }

    private void mergeMissingDefaults(String name, File target) {
        try (var stream = plugin.getResource(name)) {
            if (stream == null) return;
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            YamlConfiguration current = new YamlConfiguration();
            // The convenience loader swallows syntax errors and returns an empty document.
            // Never use it before a migration that writes to the user's file.
            current.load(target);
            boolean changed = mergeMissing(current, defaults);
            if (changed) {
                File backup = new File(plugin.getDataFolder(), "backups/config-migrations/" + System.currentTimeMillis() + "-" + name);
                backup.getParentFile().mkdirs();
                java.nio.file.Files.copy(target.toPath(), backup.toPath());
                java.nio.file.Path temporary = java.nio.file.Files.createTempFile(target.toPath().getParent(), "ccs-migration-", ".yml");
                try {
                    current.save(temporary.toFile());
                    try { java.nio.file.Files.move(temporary, target.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
                    catch (java.nio.file.AtomicMoveNotSupportedException ex) { java.nio.file.Files.move(temporary, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
                } finally { java.nio.file.Files.deleteIfExists(temporary); }
                plugin.getLogger().info("Added new default settings to " + name + " (backup: " + backup.getPath() + ")");
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Could not merge new defaults into " + name + "; existing file was kept", ex);
        }
    }

    static boolean mergeMissing(ConfigurationSection target, ConfigurationSection defaults) {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            if (!target.contains(key)) {
                target.set(key, defaults.get(key));
                target.setComments(key, defaults.getComments(key));
                target.setInlineComments(key, defaults.getInlineComments(key));
                changed = true;
            } else if (target.isConfigurationSection(key) && defaults.isConfigurationSection(key)
                    && !(target.getCurrentPath().isEmpty() && Set.of("weapons", "equipment", "divine-hearts", "sets", "mobs", "vanilla-mobs", "bosses", "buffs", "reactions", "regions", "spawns", "skill-trees").contains(key))) {
                changed |= mergeMissing(target.getConfigurationSection(key), defaults.getConfigurationSection(key));
            }
            // A pre-existing scalar/list is intentional; don't replace it with a section.
        }
        return changed;
    }

    public boolean loadInitial() {
        createDefaults();
        LoadResult result = loadCandidate();
        logIssues(result);
        if (!result.fatalErrors().isEmpty()) return false;
        current.set(result.snapshot());
        return true;
    }

    public boolean reloadSafely() {
        return reloadSafely(true);
    }

    public boolean reloadSafely(boolean migrate) {
        if (migrate) createDefaults();
        LoadResult result = loadCandidate();
        logIssues(result);
        if (!result.errors().isEmpty() || !result.fatalErrors().isEmpty()) return false;
        current.set(result.snapshot());
        return true;
    }

    public Snapshot snapshot() {
        Snapshot snapshot = current.get();
        if (snapshot == null) throw new IllegalStateException("Definitions are not loaded");
        return snapshot;
    }

    private LoadResult loadCandidate() {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> fatal = new ArrayList<>();
        Map<String, YamlConfiguration> yaml = new LinkedHashMap<>();
        for (String name : FILES) {
            File file = new File(plugin.getDataFolder(), name);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (config.getKeys(false).isEmpty() && file.length() > 0) {
                if (name.equals("config.yml") || name.equals("storage.yml")) fatal.add(name + " could not be parsed");
                else errors.add(name + " could not be parsed");
            }
            int version = config.getInt("data-version", -1);
            if (version > PlayerData.DATA_VERSION) fatal.add(name + " has unsupported data-version " + version);
            if (version < 1) errors.add(name + " is missing data-version");
            yaml.put(name, config);
        }

        // Accept the commonly used plural filename without replacing an existing definition.
        File pluralEquipment = new File(plugin.getDataFolder(), "equipments.yml");
        if (pluralEquipment.isFile()) {
            YamlConfiguration alias = YamlConfiguration.loadConfiguration(pluralEquipment);
            ConfigurationSection root = alias.getConfigurationSection("equipment");
            if (root == null) root = alias.getConfigurationSection("equipments");
            if (root == null) errors.add("equipments.yml must contain an equipment section");
            else for (String id : root.getKeys(false)) {
                String path = "equipment." + id;
                if (yaml.get("equipment.yml").contains(path)) errors.add("Duplicate equipment ID in equipment.yml and equipments.yml: " + id);
                else yaml.get("equipment.yml").set(path, root.getConfigurationSection(id));
            }
        }

        Map<String, ReactionDefinition> reactions = parseReactions(yaml.get("reactions.yml"), errors);
        Map<String, WeaponDefinition> weapons = parseWeapons(yaml.get("weapons.yml"), errors, warnings);
        Map<String, List<WeaponDefinition>> weaponStages = parseWeaponStages(yaml.get("weapons.yml"), weapons, errors, warnings);
        Map<String, EquipmentDefinition> equipment = parseEquipment(yaml.get("equipment.yml"), errors, warnings);
        validateDivineHearts(yaml.get("divine_hearts.yml"), errors);
        Map<String, MobDefinition> mobs = parseMobs(yaml.get("mobs.yml"), "mobs", false, false, errors);
        Map<String, MobDefinition> vanillaMobs = parseMobs(yaml.get("mobs.yml"), "vanilla-mobs", false, true, errors);
        Map<String, MobDefinition> bosses = parseMobs(yaml.get("bosses.yml"), "bosses", true, false, errors);
        Map<String, BuffDefinition> buffs = parseBuffs(yaml.get("buffs.yml"), errors);
        for (WeaponDefinition weapon : weaponStages.values().stream().flatMap(List::stream).toList()) {
            for (var ability : Arrays.asList(weapon.skill(), weapon.ultimate())) {
                if (ability == null) continue;
                for (String id : java.util.stream.Stream.concat(ability.options().selfEffects().stream(), ability.options().targetEffects().stream()).toList())
                    if (!buffs.containsKey(id)) errors.add("weapon " + weapon.id() + " references missing buff/debuff " + id);
            }
        }
        Map<String, RegionDefinition> regions = parseRegions(yaml.get("regions.yml"), errors);

        for (EquipmentDefinition definition : equipment.values()) {
            if (!definition.setId().isBlank() && !yaml.get("sets.yml").contains("sets." + definition.setId())) {
                errors.add("equipment " + definition.id() + " references missing set " + definition.setId());
            }
        }

        TriggerCatalog.compile(yaml,buffs.keySet(),errors);
        Snapshot snapshot = new Snapshot(Map.copyOf(yaml), reactions, weapons, equipment, buffs, mobs, vanillaMobs, bosses, regions, weaponStages);
        return new LoadResult(snapshot, warnings, errors, fatal);
    }

    private void validateDivineHearts(YamlConfiguration yaml, List<String> errors) {
        ConfigurationSection root = yaml.getConfigurationSection("divine-hearts");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            String path = "divine-hearts." + id + ".rules.reaction-override";
            if (!yaml.isConfigurationSection(path)) continue;
            for (String attributePath : List.of("source-attribute", "damage.attribute"))
                if (Element.parse(yaml.getString(path + "." + attributePath)).isEmpty()) errors.add("divine heart " + id + " has invalid " + attributePath);
            if (yaml.isConfigurationSection(path + ".resistance-down") && Element.parse(yaml.getString(path + ".resistance-down.attribute")).isEmpty())
                errors.add("divine heart " + id + " has invalid resistance-down.attribute");
            try { ReferenceStat.valueOf(yaml.getString(path + ".damage.reference", "ATK").toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { errors.add("divine heart " + id + " has invalid damage.reference"); }
            for (String number : List.of("radius", "cooldown-seconds", "damage.multiplier", "resistance-down.amount", "resistance-down.duration-seconds"))
                if (!Double.isFinite(yaml.getDouble(path + "." + number)) || yaml.getDouble(path + "." + number) < 0) errors.add("divine heart " + id + " has invalid " + number);
        }
    }

    private Map<String, ReactionDefinition> parseReactions(YamlConfiguration yaml, List<String> errors) {
        Map<String, ReactionDefinition> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("reactions");
        if (root == null) return Map.of();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            List<String> rawElements = list(section, "attributes", "elements");
            if (rawElements.size() != 2) { errors.add("reaction " + id + " must have exactly two attributes"); continue; }
            Optional<Element> first = Element.parse(rawElements.get(0));
            Optional<Element> second = Element.parse(rawElements.get(1));
            if (first.isEmpty() || second.isEmpty() || first.get() == Element.PHYSICAL || second.get() == Element.PHYSICAL) {
                errors.add("reaction " + id + " has an invalid attribute"); continue;
            }
            EnumMap<Element, Double> components = new EnumMap<>(Element.class);
            ConfigurationSection componentSection = section(section, "damage-components", "components");
            if (componentSection != null) {
                for (String key : componentSection.getKeys(false)) Element.parse(key)
                        .ifPresent(element -> components.put(element, componentSection.getDouble(key)));
            }
            ConfigurationSection down = section.getConfigurationSection("resistance-down");
            Element downElement = down == null ? null : Element.parse(value(down, "attribute", "element")).orElse(null);
            result.put(id, new ReactionDefinition(id, section.getString("name", id), first.get(), second.get(),
                    Math.max(0, section.getDouble("radius", 0)), Math.max(0, section.getDouble("cooldown", 0)),
                    Math.max(1, section.getInt("hits", 1)), Element.parse(value(section, "multi-hit-attribute", "multi-hit-element")).orElse(null),
                    Math.max(0, section.getDouble("levitation", 0)), components,
                    downElement, down == null ? 0 : down.getDouble("amount"), down == null ? 0 : down.getDouble("duration")));
        }
        return Map.copyOf(result);
    }

    private Map<String, WeaponDefinition> parseWeapons(YamlConfiguration yaml, List<String> errors, List<String> warnings) {
        Map<String, WeaponDefinition> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("weapons");
        if (root == null) return Map.of();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            Material material = Material.matchMaterial(s.getString("material", ""));
            if (material == null) { errors.add("weapon " + id + " has invalid material"); continue; }
            WeaponDefinition.Category category;
            String categoryName = s.contains("type") ? s.getString("type", "") : s.getString("category", "");
            try { category = WeaponDefinition.Category.valueOf(categoryName.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { errors.add("weapon " + id + " has invalid type (MELEE, RANGED or UNCATEGORIZED)"); continue; }
            int rarity = s.getInt("rarity", 1);
            if (rarity < 1 || rarity > 5) { errors.add("weapon " + id + " rarity must be 1..5"); continue; }
            String bonusType = value(s, "attribute-bonus.type", "element-bonus.type");
            if (bonusType != null) bonusType = bonusType.toUpperCase(Locale.ROOT).replace('-', '_').replaceFirst("_DAMAGE$", "");
            Element element = Element.parse(bonusType).orElse(Element.PHYSICAL);
            ConfigurationSection breaks = s.getConfigurationSection("limit-breaks");
            if (breaks != null) for (String stage : breaks.getKeys(false)) {
                if (breaks.contains(stage + ".atk-percent")) warnings.add("weapon " + id + " limit-breaks." + stage + ".atk-percent is unsupported; no ATK bonus is applied");
            }
            int min = Math.max(1, s.getInt("equip-level.min", 1));
            int max = Math.min(100, s.getInt("equip-level.max", 100));
            if (min > max) { errors.add("weapon " + id + " equip level range is invalid"); continue; }
            try { result.put(id, new WeaponDefinition(id, s.getString("name", id), material.name(), category, rarity,
                    s.getDouble("base-atk.level-1"), s.getDouble("base-atk.level-100"), element,
                    decimal(s, "attribute-bonus.value", "element-bonus.value"), min, max,
                    s.contains("custom-model-data") ? s.getInt("custom-model-data") : null, s.getStringList("lore"),
                    parseSkill(id + ":skill", s.getConfigurationSection("skill"), warnings),
                    parseSkill(id + ":ultimate", s.getConfigurationSection("ultimate"), warnings), readLimitBreaks(s.getConfigurationSection("limit-breaks")), WeaponOptionsParser.weapon(s)));
            } catch (IllegalArgumentException ex) { errors.add("weapon " + id + ": " + ex.getMessage()); }
        }
        return Map.copyOf(result);
    }

    private Map<String, List<WeaponDefinition>> parseWeaponStages(YamlConfiguration source, Map<String, WeaponDefinition> base,
                                                                List<String> errors, List<String> warnings) {
        Map<String, List<WeaponDefinition>> result = new LinkedHashMap<>();
        for (String id : base.keySet()) {
            var raw = source.getConfigurationSection("weapons." + id);
            var effective = new YamlConfiguration();
            overlay(effective.createSection("weapons." + id), raw);
            effective.set("weapons." + id + ".limit-breaks", null);
            List<WeaponDefinition> stages = new ArrayList<>();
            for (int stage = 0; stage <= 5; stage++) {
                var patch = raw.getConfigurationSection("limit-breaks." + stage);
                if (patch != null) overlay(effective.getConfigurationSection("weapons." + id), patch);
                WeaponDefinition value = parseWeapons(effective, errors, warnings).get(id);
                if (value == null) { errors.add("weapon " + id + " has invalid limit-break stage " + stage); break; }
                stages.add(value);
            }
            if (stages.size() == 6) result.put(id, List.copyOf(stages));
        }
        return Map.copyOf(result);
    }

    static void overlay(ConfigurationSection target, ConfigurationSection patch) {
        for (String key : patch.getKeys(false)) {
            var child = patch.getConfigurationSection(key);
            if (child == null) target.set(key, patch.get(key));
            else {
                var existing = target.getConfigurationSection(key);
                overlay(existing == null ? target.createSection(key) : existing, child);
            }
        }
        // Legacy cooldown spelling must override an inherited canonical spelling as well.
        if (patch.contains("cooldown") && !patch.contains("cooldown-seconds")) target.set("cooldown-seconds", patch.get("cooldown"));
    }

    private WeaponDefinition.SkillDefinition parseSkill(String fallbackId, ConfigurationSection s, List<String> warnings) {
        if (s == null) return null;
        ReferenceStat stat;
        try { stat = ReferenceStat.valueOf(s.getString("reference", "ATK").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { warnings.add(fallbackId + " uses ATK because reference is invalid"); stat = ReferenceStat.ATK; }
        return new WeaponDefinition.SkillDefinition(s.getString("id", fallbackId), s.getString("name", fallbackId), stat,
                s.getDouble("multiplier", 1), Element.parse(value(s, "attribute", "element")).orElse(Element.PHYSICAL),
                Math.max(0, s.getDouble("cooldown-seconds", s.getDouble("cooldown", 0))), Math.max(1, s.getInt("charges", 1)),
                Math.max(0, s.getDouble("radius", 0)), s.getString("target", "ENEMY"),
                s.getConfigurationSection("conditions") == null ? Map.of() : Map.copyOf(s.getConfigurationSection("conditions").getValues(false)), WeaponOptionsParser.ability(s));
    }

    private Map<Integer, Map<String, Object>> readLimitBreaks(ConfigurationSection root) {
        if (root == null) return Map.of();
        Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
        for (String key : root.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                ConfigurationSection section = root.getConfigurationSection(key);
                if (level >= 0 && level <= 5 && section != null) result.put(level, Map.copyOf(section.getValues(true)));
            } catch (NumberFormatException ignored) {}
        }
        return Map.copyOf(result);
    }

    private Map<String, EquipmentDefinition> parseEquipment(YamlConfiguration yaml, List<String> errors, List<String> warnings) {
        Map<String, EquipmentDefinition> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("equipment");
        if (root == null) return Map.of();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            Material material = Material.matchMaterial(s.getString("material", ""));
            if (material == null) { errors.add("equipment " + id + " has invalid material"); continue; }
            EquipmentSlot slot;
            StatKey main;
            try {
                slot = EquipmentSlot.valueOf(s.getString("slot", "").toUpperCase(Locale.ROOT));
                var mainPool = EquipmentRolls.candidates(s.getConfigurationSection("main-stat-candidates"), true);
                if (s.contains("main-stat-candidates") && mainPool.isEmpty()) throw new IllegalArgumentException("Empty main-stat-candidates");
                main = s.contains("main-stat.type") || mainPool.isEmpty() ? StatKey.valueOf(s.getString("main-stat.type", "").toUpperCase(Locale.ROOT)) : mainPool.getFirst().key();
                for (var candidate : mainPool) if (!mainAllowed(slot, candidate.key())) throw new IllegalArgumentException("Main stat not allowed for slot");
                var subPool = EquipmentRolls.candidates(s.getConfigurationSection("substat-candidates"), false);
                if (s.contains("substat-candidates") && subPool.isEmpty()) throw new IllegalArgumentException("Empty substat-candidates");
            } catch (IllegalArgumentException ex) { errors.add("equipment " + id + " has invalid slot or main stat"); continue; }
            if (!mainAllowed(slot, main)) { errors.add("equipment " + id + " main stat is not allowed for its slot"); continue; }
            int rarity = s.getInt("rarity");
            int expectedMax = rarity == 3 ? 9 : rarity == 4 ? 12 : rarity == 5 ? 15 : -1;
            if (expectedMax < 0) { errors.add("equipment " + id + " rarity must be 3..5"); continue; }
            int maxLevel = s.getInt("max-level", expectedMax);
            if (maxLevel < 1 || maxLevel > 100) { errors.add("equipment " + id + " max-level must be 1..100"); continue; }
            if (maxLevel != expectedMax) warnings.add("equipment " + id + " max-level differs from rarity standard");
            List<StatKey> candidates = new ArrayList<>();
            ConfigurationSection initialStats = s.getConfigurationSection("initial-substats");
            if (initialStats != null) {
                parseStatMap(initialStats, errors, "equipment " + id + " initial-substats");
                if (initialStats.getKeys(false).size() > 4) errors.add("equipment " + id + " initial-substats must have at most four entries");
                int unlocked = s.getInt("initial-unlocked-substats", 0);
                if (unlocked < 0 || unlocked > initialStats.getKeys(false).size()) errors.add("equipment " + id + " has invalid initial-unlocked-substats");
            }
            for (String raw : s.getStringList("substats")) {
                try { StatKey key = StatKey.valueOf(raw.toUpperCase(Locale.ROOT)); if (!candidates.contains(key)) candidates.add(key); }
                catch (IllegalArgumentException ex) { errors.add("equipment " + id + " contains invalid substat " + raw); }
            }
            var mainPool = EquipmentRolls.candidates(s.getConfigurationSection("main-stat-candidates"), true);
            double first = s.getDouble("main-stat.level-1", mainPool.isEmpty() ? 0 : mainPool.getFirst().first());
            double last = s.getDouble("main-stat.max-level", mainPool.isEmpty() ? 0 : mainPool.getFirst().last());
            if (!Double.isFinite(first) || !Double.isFinite(last)) { errors.add("equipment " + id + " has non-finite main stat"); continue; }
            result.put(id, new EquipmentDefinition(id, s.getString("name", id), material.name(), slot, rarity, maxLevel,
                    main, first, last, List.copyOf(candidates),
                    s.getString("set", ""), s.contains("custom-model-data") ? s.getInt("custom-model-data") : null,
                    s.getStringList("lore")));
        }
        return Map.copyOf(result);
    }

    public static boolean mainAllowed(EquipmentSlot slot, StatKey key) {
        return switch (slot) {
            case HEAD -> Set.of(StatKey.HP_FLAT, StatKey.HP_PERCENT, StatKey.DEF_FLAT, StatKey.DEF_PERCENT).contains(key);
            case CHEST -> Set.of(StatKey.CRIT_RATE, StatKey.CRIT_DAMAGE).contains(key);
            case LEGS -> Set.of(StatKey.ATK_FLAT, StatKey.ATK_PERCENT).contains(key);
            case FEET -> Set.of(StatKey.ATK_FLAT, StatKey.ATK_PERCENT, StatKey.CRIT_RATE, StatKey.CRIT_DAMAGE,
                    StatKey.HP_FLAT, StatKey.HP_PERCENT, StatKey.DEF_FLAT, StatKey.DEF_PERCENT).contains(key);
            case RESONANCE -> key.name().endsWith("_DAMAGE") || key.name().endsWith("_RESISTANCE");
            default -> false;
        };
    }

    private Map<String, MobDefinition> parseMobs(YamlConfiguration yaml, String rootName, boolean boss, boolean vanilla, List<String> errors) {
        Map<String, MobDefinition> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection(rootName);
        if (root == null) return Map.of();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            if (s.contains("material") && (Material.matchMaterial(s.getString("material", "")) == null || Material.matchMaterial(s.getString("material", "")).isAir())) {
                errors.add(rootName + "." + id + ".material must be a non-air item material"); continue;
            }
            try { EntityType.valueOf(s.getString("entity-type", "").toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { errors.add((boss ? "boss " : "mob ") + id + " has invalid entity type"); continue; }
            if (!EntityType.valueOf(s.getString("entity-type").toUpperCase(Locale.ROOT)).isAlive()) {
                errors.add(rootName + "." + id + " must use a living entity type"); continue;
            }
            boolean invalidStats = false;
            for (String key : List.of("stats.hp.min", "stats.hp.max", "stats.atk.min", "stats.atk.max", "stats.def.min", "stats.def.max")) {
                if (s.contains(key) && (!Double.isFinite(s.getDouble(key)) || (key.startsWith("stats.hp") ? s.getDouble(key) <= 0 : s.getDouble(key) < 0))) {
                    errors.add(rootName + "." + id + "." + key + " must be finite and " + (key.startsWith("stats.hp") ? "positive" : "nonnegative")); invalidStats = true;
                }
            }
            if (invalidStats) continue;
            int min = Math.max(1, s.getInt("level.min", 1));
            int max = Math.max(min, s.getInt("level.max", min));
            Set<Element> immunity = EnumSet.noneOf(Element.class);
            for (String raw : s.getStringList("immunity")) Element.parse(raw).ifPresent(immunity::add);
            EnumMap<Element, Double> resistance = new EnumMap<>(Element.class);
            ConfigurationSection rs = s.getConfigurationSection("resistance");
            if (rs != null) for (String key : rs.getKeys(false)) Element.parse(key).ifPresent(e -> resistance.put(e, rs.getDouble(key)));
            result.put(id, new MobDefinition(id, s.getString("name", id), s.getString("entity-type"), boss, min, max,
                    s.getDouble("stats.hp.min", 20), s.getDouble("stats.hp.max", 20), s.getDouble("stats.atk.min", 2),
                    s.getDouble("stats.atk.max", 2), s.getDouble("stats.def.min", 0), s.getDouble("stats.def.max", 0),
                    Element.parse(value(s, "native-attribute", "native-element")).orElse(Element.PHYSICAL), Set.copyOf(immunity), Map.copyOf(resistance),
                    Math.max(0, s.contains("custom-exp") ? s.getLong("custom-exp") : s.getLong("exp", 0)),
                    s.getBoolean("drop-custom-exp", true), s.getBoolean("show-level", true), vanilla));
        }
        return Map.copyOf(result);
    }

    private Map<String, BuffDefinition> parseBuffs(YamlConfiguration yaml, List<String> errors) {
        Map<String, BuffDefinition> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("buffs");
        if (root == null) return Map.of();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                BuffDefinition.Kind kind = BuffDefinition.Kind.valueOf(s.getString("kind", "BUFF").toUpperCase(Locale.ROOT));
                BuffDefinition.Target target = BuffDefinition.Target.valueOf(s.getString("target", kind == BuffDefinition.Kind.BUFF ? "SELF" : "ENEMY").toUpperCase(Locale.ROOT));
                BuffDefinition.Reapply reapply = BuffDefinition.Reapply.valueOf(s.getString("reapply", "REFRESH").toUpperCase(Locale.ROOT));
                var buffOptions=com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition.map(s);
                com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition.range(buffOptions,"duration",0,0,Double.MAX_VALUE);
                com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition.integer(buffOptions,"max-stacks",1,1);
                EnumMap<StatKey, Double> flat = parseStatMap(s.getConfigurationSection("modifiers.flat"), errors, "buff " + id);
                EnumMap<StatKey, Double> percent = parseStatMap(s.getConfigurationSection("modifiers.percent"), errors, "buff " + id);
                if (s.contains("modifiers.override")) com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition.modifiers(
                        Map.of("override",com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition.map(s.getConfigurationSection("modifiers.override"))));
                BuffDefinition.TickEffect tick = parseTickEffect(s.getConfigurationSection("tick-effect"), errors, id);
                result.put(id, new BuffDefinition(id, kind, target, Math.max(0, s.getDouble("duration", 0)),
                        s.getBoolean("permanent"), Math.max(1, s.getInt("max-stacks", 1)), reapply, Map.copyOf(flat), Map.copyOf(percent), tick));
            } catch (IllegalArgumentException ex) { errors.add("buff " + id + " contains an invalid enum value"); }
        }
        return Map.copyOf(result);
    }

    private EnumMap<StatKey, Double> parseStatMap(ConfigurationSection section, List<String> errors, String owner) {
        EnumMap<StatKey, Double> result = new EnumMap<>(StatKey.class);
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            try { result.put(StatKey.valueOf(key.toUpperCase(Locale.ROOT)), section.getDouble(key)); }
            catch (IllegalArgumentException ex) { errors.add(owner + " has invalid stat " + key); }
        }
        return result;
    }

    private BuffDefinition.TickEffect parseTickEffect(ConfigurationSection section, List<String> errors, String id) {
        if (section == null) return null;
        try {
            return new BuffDefinition.TickEffect(section.getBoolean("healing"), section.getBoolean("fixed"),
                    ReferenceStat.valueOf(section.getString("reference", "ATK").toUpperCase(Locale.ROOT)),
                    section.getDouble("multiplier", 1), Element.parse(value(section, "attribute", "element")).orElse(Element.PHYSICAL),
                    Math.max(0.05, section.getDouble("interval", 1)), section.getBoolean("critical"));
        } catch (IllegalArgumentException ex) {
            errors.add("buff " + id + " has invalid tick-effect");
            return null;
        }
    }

    private Map<String, RegionDefinition> parseRegions(YamlConfiguration yaml, List<String> errors) {
        Map<String, RegionDefinition> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("regions");
        if (root == null) return Map.of();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null || s.getString("world") == null) { errors.add("region " + id + " is missing world"); continue; }
            int x1 = s.getInt("pos1.x"), z1 = s.getInt("pos1.z"), x2 = s.getInt("pos2.x"), z2 = s.getInt("pos2.z");
            boolean full = s.getBoolean("full-height", false);
            Integer y1 = full ? null : Math.min(s.getInt("pos1.y"), s.getInt("pos2.y"));
            Integer y2 = full ? null : Math.max(s.getInt("pos1.y"), s.getInt("pos2.y"));
            Map<String, String> flags = new LinkedHashMap<>();
            ConfigurationSection fs = s.getConfigurationSection("flags");
            if (fs != null) for (String key : fs.getKeys(false)) flags.put(key, fs.getString(key, ""));
            result.put(id, new RegionDefinition(id, s.getString("world"), Math.min(x1, x2), y1, Math.min(z1, z2),
                    Math.max(x1, x2), y2, Math.max(z1, z2), Map.copyOf(flags)));
        }
        return Map.copyOf(result);
    }

    private List<String> list(ConfigurationSection section, String preferred, String legacy) {
        return section.contains(preferred) ? section.getStringList(preferred) : section.getStringList(legacy);
    }
    private ConfigurationSection section(ConfigurationSection section, String preferred, String legacy) {
        ConfigurationSection result = section.getConfigurationSection(preferred);
        return result == null ? section.getConfigurationSection(legacy) : result;
    }
    private String value(ConfigurationSection section, String preferred, String legacy) {
        return section.contains(preferred) ? section.getString(preferred) : section.getString(legacy);
    }
    private double decimal(ConfigurationSection section, String preferred, String legacy) {
        return section.contains(preferred) ? section.getDouble(preferred) : section.getDouble(legacy);
    }

    private void logIssues(LoadResult result) {
        result.warnings().forEach(message -> plugin.getLogger().warning(message));
        result.errors().forEach(message -> plugin.getLogger().severe(message));
        result.fatalErrors().forEach(message -> plugin.getLogger().log(Level.SEVERE, message));
    }

    public record Snapshot(Map<String, YamlConfiguration> yaml, Map<String, ReactionDefinition> reactions,
                           Map<String, WeaponDefinition> weapons, Map<String, EquipmentDefinition> equipment,
                           Map<String, BuffDefinition> buffs, Map<String, MobDefinition> mobs, Map<String, MobDefinition> vanillaMobs,
                           Map<String, MobDefinition> bosses, Map<String, RegionDefinition> regions,
                           Map<String, List<WeaponDefinition>> weaponStages) {
        public YamlConfiguration config(String file) { return yaml.get(file); }
        public Snapshot(Map<String, YamlConfiguration> yaml, Map<String, ReactionDefinition> reactions,
                        Map<String, WeaponDefinition> weapons, Map<String, EquipmentDefinition> equipment,
                        Map<String, BuffDefinition> buffs, Map<String, MobDefinition> mobs, Map<String, MobDefinition> vanillaMobs,
                        Map<String, MobDefinition> bosses, Map<String, RegionDefinition> regions) {
            this(yaml, reactions, weapons, equipment, buffs, mobs, vanillaMobs, bosses, regions, Map.of());
        }
        public WeaponDefinition weapon(ItemInstance instance) {
            if (instance == null) return null;
            var stages = weaponStages.get(instance.getDefinitionId());
            return stages == null ? weapons.get(instance.getDefinitionId()) : stages.get(Math.clamp(instance.getLimitBreak(), 0, 5));
        }
    }

    private record LoadResult(Snapshot snapshot, List<String> warnings, List<String> errors, List<String> fatalErrors) {}
}
```

## WeaponOptionsParser.java

```java
package com.github.saku0817.combatcoresystems.config;

import com.github.saku0817.combatcoresystems.model.*;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Strict optional fields: invalid combat definitions fail reload instead of failing on use. */
final class WeaponOptionsParser {
    private WeaponOptionsParser() {}
    static WeaponOptions weapon(ConfigurationSection s) {
        var t = s.getConfigurationSection("talent");
        WeaponOptions.Talent talent = null;
        if (t != null) {
            Map<StatKey, Double> modifiers = new EnumMap<>(StatKey.class);
            var values = t.getConfigurationSection("modifiers");
            if (values != null) for (String key : values.getKeys(false))
                modifiers.put(StatKey.valueOf(key.toUpperCase(Locale.ROOT)), finite(values, key, 0));
            talent = new WeaponOptions.Talent(t.getString("name", "天賦"), description(t),
                    WeaponOptions.Hand.valueOf(t.getString("hand", "MAIN_HAND").toUpperCase(Locale.ROOT)),
                    nonnegative(t, "multiplier", 1), Map.copyOf(modifiers));
        }
        return new WeaponOptions(element(s.getString("normal-attack.attribute", "PHYSICAL")), talent,
                visual(s.getConfigurationSection("normal-attack.visual")));
    }
    static WeaponOptions.Ability ability(ConfigurationSection s) {
        nonnegative(s, "multiplier", 1);
        nonnegative(s, "cooldown-seconds", s.getDouble("cooldown", 0));
        nonnegative(s, "radius", 0);
        double hpCondition = nonnegative(s, "conditions.min-hp-percent", 0);
        if (hpCondition > 1) throw new IllegalArgumentException("conditions.min-hp-percent must be 0..1");
        nonnegative(s, "conditions.max-distance", 0);
        List<WeaponOptions.Component> components = new ArrayList<>();
        for (Map<?, ?> raw : s.getMapList("damage-components")) {
            var c = new org.bukkit.configuration.MemoryConfiguration();
            raw.forEach((key, value) -> c.set(String.valueOf(key), value));
            components.add(new WeaponOptions.Component(ReferenceStat.valueOf(c.getString("reference", "ATK").toUpperCase(Locale.ROOT)),
                    nonnegative(c, "multiplier", 1), element(c.getString("bonus-attribute", "PHYSICAL"))));
        }
        if (s.contains("damage-components") && (!s.isList("damage-components") || components.isEmpty()))
            throw new IllegalArgumentException("damage-components must be a nonempty list");
        double cost = nonnegative(s, "cost.current-hp-percent", 0);
        if (cost >= 1) throw new IllegalArgumentException("cost.current-hp-percent must be less than 1");
        return new WeaponOptions.Ability(description(s), s.getBoolean("damage-enabled", !s.contains("actions")), cost,
                List.copyOf(components), s.getStringList("self-effects"), s.getStringList("target-effects"),
                visual(s.getConfigurationSection("visual")));
    }
    private static List<String> description(ConfigurationSection s) {
        return s.isList("description") ? List.copyOf(s.getStringList("description"))
                : s.contains("description") ? List.of(s.getString("description", "")) : List.of();
    }
    private static Element element(String value) {
        return Element.parse(value).orElseThrow(() -> new IllegalArgumentException("invalid attribute: " + value));
    }
    private static double finite(ConfigurationSection s, String key, double fallback) {
        if (s.contains(key) && !(s.get(key) instanceof Number)) throw new IllegalArgumentException(key + " must be numeric");
        double value = s.getDouble(key, fallback);
        if (!Double.isFinite(value)) throw new IllegalArgumentException(key + " must be finite");
        return value;
    }
    private static double nonnegative(ConfigurationSection s, String key, double fallback) {
        double value = finite(s, key, fallback);
        if (value < 0) throw new IllegalArgumentException(key + " must be nonnegative");
        return value;
    }
    private static WeaponOptions.Visual visual(ConfigurationSection s) {
        if (s == null) return WeaponOptions.Visual.NONE;
        String particle = s.getString("particle", "").toUpperCase(Locale.ROOT);
        if (!particle.isBlank() && Particle.valueOf(particle).getDataType() != Void.class)
            throw new IllegalArgumentException("visual particle requires additional data; use a data-free particle");
        String sound = s.getString("sound", "");
        if (!sound.isBlank() && !sound.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"))
            throw new IllegalArgumentException("visual.sound must be a namespaced sound key");
        int count = s.getInt("count", 20);
        double spread = nonnegative(s, "spread", .5), volume = nonnegative(s, "volume", 1), pitch = nonnegative(s, "pitch", 1);
        if (count < 0 || count > 500 || spread > 16 || volume > 4 || pitch > 2)
            throw new IllegalArgumentException("visual exceeds limits: count 0..500, spread 0..16, volume 0..4, pitch 0..2");
        return new WeaponOptions.Visual(particle, count, spread, sound, (float) volume, (float) pitch);
    }
}
```

## TriggerCatalog.java

```java
package com.github.saku0817.combatcoresystems.config;

import com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

/** Compilation is performed during candidate validation, not on each event. */
public final class TriggerCatalog {
    public record Source(String definition, String placement, List<TriggerDefinition> triggers,
                         Map<String,Object> options) {}
    private TriggerCatalog() {}
    public static Map<String,List<Source>> compile(Map<String,YamlConfiguration> files, Set<String> buffs, List<String> errors) {
        Map<String,List<Source>> out = new LinkedHashMap<>();
        var weapons = files.get("weapons.yml").getConfigurationSection("weapons");
        if (weapons != null) for (String id : weapons.getKeys(false)) {
            var raw = weapons.getConfigurationSection(id);
            if (raw == null) continue;
            var effective = new YamlConfiguration(); DefinitionRegistry.overlay(effective,raw);
            effective.set("limit-breaks",null);
            for (int stage = 0; stage <= 5; stage++) {
                var patch = raw.getConfigurationSection("limit-breaks." + stage);
                if (patch != null) DefinitionRegistry.overlay(effective,patch);
                List<Source> sources = new ArrayList<>();
                for (String placement : List.of("weapon","talent","skill","ultimate")) {
                    var section = placement.equals("weapon") ? effective : effective.getConfigurationSection(placement);
                    compileSource(sources,"weapon:" + id,placement,section,buffs,errors);
                }
                out.put("weapon:" + id + ":" + stage,List.copyOf(sources));
            }
        }
        for (String root : List.of("sets","equipment","divine-hearts")) {
            String file = root.equals("divine-hearts") ? "divine_hearts.yml" : root + ".yml";
            var section = files.get(file).getConfigurationSection(root);
            if (section == null) continue;
            for (String id : section.getKeys(false)) {
                List<Source> sources = new ArrayList<>();
                if (root.equals("sets")) {
                    for (String tier : List.of("two-piece","four-piece"))
                        compileSource(sources,"set:" + id,tier,section.getConfigurationSection(id + "." + tier),buffs,errors);
                } else compileSource(sources,root + ":" + id,"root",section.getConfigurationSection(id),buffs,errors);
                out.put(root + ":" + id,List.copyOf(sources));
            }
        }
        return Map.copyOf(out);
    }
    private static void compileSource(List<Source> out,String definition,String placement,ConfigurationSection section,Set<String> buffs,List<String> errors) {
        if (section == null) return;
        try {
            var options = TriggerDefinition.map(section);
            var triggers = TriggerDefinition.parse(section.getConfigurationSection("triggers"),buffs);
            TriggerDefinition.policy(TriggerDefinition.child(options,"target-stack-policy"));
            if (options.containsKey("area")) TriggerDefinition.area(TriggerDefinition.child(options,"area"));
            TriggerDefinition.actions(options,buffs);
            if (placement.equals("skill") || placement.equals("ultimate")) TriggerDefinition.validateConditions(TriggerDefinition.child(options,"conditions"),buffs);
            if (!triggers.isEmpty() || options.containsKey("actions")) out.add(new Source(definition,placement,triggers,options));
        } catch (IllegalArgumentException ex) { errors.add(definition + "." + placement + ": " + ex.getMessage()); }
    }
}
```

## StatKey.java

```java
package com.github.saku0817.combatcoresystems.model;

public enum StatKey {
    HP_FLAT, HP_PERCENT, ATK_FLAT, ATK_PERCENT, DEF_FLAT, DEF_PERCENT,
    CRIT_RATE, CRIT_DAMAGE, HEALING_POWER, COOLDOWN, ATTACK_SPEED,
    FIRE_DAMAGE, FIRE_RESISTANCE, WATER_DAMAGE, WATER_RESISTANCE,
    WIND_DAMAGE, WIND_RESISTANCE, THUNDER_DAMAGE, THUNDER_RESISTANCE,
    MOON_DAMAGE, MOON_RESISTANCE, DEF_DOWN, DEF_IGNORE, DEF_IGNORED_WHEN_HIT,
    FIRE_RESISTANCE_DOWN,
    WATER_RESISTANCE_DOWN,
    WIND_RESISTANCE_DOWN,
    THUNDER_RESISTANCE_DOWN,
    MOON_RESISTANCE_DOWN
}
```

## EquipmentRolls.java

```java
package com.github.saku0817.combatcoresystems.model;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
import java.util.random.RandomGenerator;

/** Weighted draws are performed once on acquisition; selected values are persisted in ItemInstance. */
public final class EquipmentRolls {
    private EquipmentRolls() {}
    public record Candidate(StatKey key, double first, double last, double weight) {}
    public static List<Candidate> candidates(ConfigurationSection section, boolean main) {
        if (section == null) return List.of();
        List<Candidate> result = new ArrayList<>();
        for (String raw : section.getKeys(false)) {
            StatKey key = StatKey.valueOf(raw.toUpperCase(Locale.ROOT));
            if (result.stream().anyMatch(c -> c.key() == key)) throw new IllegalArgumentException("Duplicate stat: " + raw);
            ConfigurationSection value = section.getConfigurationSection(raw);
            if (value == null) throw new IllegalArgumentException("Stat candidate must be a map: " + raw);
            double first = value.getDouble(main ? "level-1" : "value", key.name().endsWith("_FLAT") ? 25 : 0.05);
            double last = main ? value.getDouble("max-level", first) : first;
            double weight = value.getDouble("weight", 1);
            if (!Double.isFinite(first) || !Double.isFinite(last) || !Double.isFinite(weight) || weight <= 0)
                throw new IllegalArgumentException("Invalid candidate value/weight: " + raw);
            result.add(new Candidate(key, first, last, weight));
        }
        double total = result.stream().mapToDouble(Candidate::weight).sum();
        if (!Double.isFinite(total)) throw new IllegalArgumentException("Candidate weights overflow");
        return List.copyOf(result);
    }
    public static List<Candidate> draw(List<Candidate> candidates, int count, RandomGenerator random) {
        List<Candidate> pool = new ArrayList<>(candidates), result = new ArrayList<>();
        while (!pool.isEmpty() && result.size() < count) {
            double pick = random.nextDouble() * pool.stream().mapToDouble(Candidate::weight).sum();
            int index = 0;
            while (index < pool.size() - 1 && (pick -= pool.get(index).weight()) >= 0) index++;
            Candidate chosen = pool.remove(index); result.add(chosen);
            pool.removeIf(candidate -> candidate.key() == chosen.key());
        }
        return result;
    }
}
```

## ItemInstance.java

```java
package com.github.saku0817.combatcoresystems.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ItemInstance {
    private String instanceId = UUID.randomUUID().toString();
    private String definitionId = "";
    private int level = 1;
    private long exp;
    private int limitBreak;
    private Map<String, Double> substats = new LinkedHashMap<>();
    private Map<String, Integer> substatUpgrades = new LinkedHashMap<>();
    private int unlockedSubstats;
    private StatKey mainStat;
    private double mainAtLevel1;
    private double mainAtMaxLevel;

    public StatKey mainStat(EquipmentDefinition definition) { return mainStat == null ? definition.mainStat() : mainStat; }
    public double mainValue(EquipmentDefinition definition) {
        return com.github.saku0817.combatcoresystems.util.CoreMath.linear(mainStat == null ? definition.mainAtLevel1() : mainAtLevel1,
                mainStat == null ? definition.mainAtMaxLevel() : mainAtMaxLevel, level, definition.maxLevel());
    }
    public void setMainStat(StatKey key, double first, double last) {
        if (key == null || !Double.isFinite(first) || !Double.isFinite(last)) throw new IllegalArgumentException("Invalid main stat");
        mainStat = key; mainAtLevel1 = first; mainAtMaxLevel = last;
    }

    public String getInstanceId() { return instanceId; }
    public String getDefinitionId() { return definitionId; }
    public int getLevel() { return level; }
    public long getExp() { return exp; }
    public int getLimitBreak() { return limitBreak; }
    public Map<String, Double> getSubstats() { return substats; }
    public Map<String, Integer> getSubstatUpgrades() { return substatUpgrades; }
    public int getUnlockedSubstats() { return unlockedSubstats; }
    public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }
    public void setLevel(int level) { this.level = Math.max(1, Math.min(100, level)); }
    public void setExp(long exp) { this.exp = Math.max(0L, exp); }
    public void setLimitBreak(int limitBreak) { this.limitBreak = Math.max(0, Math.min(5, limitBreak)); }
    public void setUnlockedSubstats(int count) { this.unlockedSubstats = Math.max(0, Math.min(4, count)); }
}
```

## Area.java

```java
package com.github.saku0817.combatcoresystems.model.trigger;

/** Geometry in local coordinates: x=right, y=up, z=forward. */
public record Area(Shape shape, double radius, double width, double height, double length, double angle, Origin origin) {
    public enum Shape { CIRCLE, SPHERE, BOX, FORWARD_BOX, CONE, CYLINDER }
    public enum Origin { SELF, EVENT_TARGET, LOCATION }
    public Area {
        for (double n : new double[]{radius, width, height, length, angle})
            if (!Double.isFinite(n) || n < 0) throw new IllegalArgumentException("Invalid area dimension");
        if (angle > 360) throw new IllegalArgumentException("Area angle exceeds 360");
    }
    public boolean contains(double x, double y, double z) {
        return switch (shape) {
            case SPHERE -> x*x+y*y+z*z <= radius*radius;
            case CIRCLE, CYLINDER -> x*x+z*z <= radius*radius && Math.abs(y) <= height/2;
            case BOX -> Math.abs(x) <= width/2 && Math.abs(y) <= height/2 && Math.abs(z) <= length/2;
            case FORWARD_BOX -> Math.abs(x) <= width/2 && Math.abs(y) <= height/2 && z >= 0 && z <= length;
            case CONE -> x*x+z*z <= radius*radius && Math.abs(y) <= height/2
                    && (x*x+z*z == 0 || z / Math.sqrt(x*x+z*z) >= Math.cos(Math.toRadians(angle/2)) - 1e-12);
        };
    }
    public double searchRadius() {
        return switch(shape) {
            case SPHERE -> radius;
            case CIRCLE, CYLINDER, CONE -> Math.sqrt(radius*radius+height*height/4);
            case BOX -> Math.sqrt(width*width+height*height+length*length)/2;
            case FORWARD_BOX -> Math.sqrt(width*width/4+height*height/4+length*length);
        };
    }
}
```

## EventContext.java

```java
package com.github.saku0817.combatcoresystems.model.trigger;

import com.github.saku0817.combatcoresystems.model.Element;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import java.util.*;

public final class EventContext {
    public final TriggerChain chain;
    public final TriggerEvent event;
    public LivingEntity source, target, attacker, victim, healer, healed;
    public Player player;
    public String weapon = "", ability = "", combatId = "";
    public double damage, finalDamage, healAmount, requestedHeal, effectiveHeal, overheal;
    public Element attribute = Element.PHYSICAL;
    public boolean critical, reaction, normalAttack, skill, ultimate;
    public Location location;
    public final Map<String, Object> values = new LinkedHashMap<>();
    public final EventModifier sourceModifiers = new EventModifier();
    public final EventModifier targetModifiers = new EventModifier();
    public EventContext(TriggerEvent event, TriggerChain chain, LivingEntity source, LivingEntity target) {
        this.event = Objects.requireNonNull(event); this.chain = chain == null ? new TriggerChain() : chain;
        this.source = source; this.target = target; this.attacker = source; this.victim = target;
        this.player = source instanceof Player p ? p : null;
        this.location = target != null ? target.getLocation() : source == null ? null : source.getLocation();
    }
    public EventContext child(TriggerEvent event, LivingEntity source, LivingEntity target) {
        EventContext result = new EventContext(event, chain, source, target);
        result.weapon = weapon; result.ability = ability; result.combatId = combatId;
        result.damage = damage; result.finalDamage = finalDamage;
        result.healAmount = healAmount; result.requestedHeal=requestedHeal; result.effectiveHeal = effectiveHeal; result.overheal = overheal;
        result.attribute = attribute; result.critical = critical; result.reaction = reaction;
        result.normalAttack = normalAttack; result.skill = skill; result.ultimate = ultimate;
        result.healer = healer; result.healed = healed; result.values.putAll(values);
        return result;
    }
}
```

## EventModifier.java

```java
package com.github.saku0817.combatcoresystems.model.trigger;

import com.github.saku0817.combatcoresystems.model.StatKey;
import java.util.EnumMap;
import java.util.Map;

/** Per-calculation modifiers. Never mutates cached PlayerStats. Last override wins. */
public final class EventModifier {
    private final Map<StatKey, Double> flat = new EnumMap<>(StatKey.class);
    private final Map<StatKey, Double> percent = new EnumMap<>(StatKey.class);
    private final Map<StatKey, Double> override = new EnumMap<>(StatKey.class);
    public void add(String mode, StatKey key, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite modifier");
        switch (mode) {
            case "flat" -> flat.merge(key, value, Double::sum);
            case "percent" -> percent.merge(key, value, Double::sum);
            case "override" -> override.put(key, value);
            default -> throw new IllegalArgumentException("Unknown modifier mode: " + mode);
        }
    }
    public double advanced(StatKey key, double base) {
        // CCS advanced stats are additive ratios, including modifiers.percent.CRIT_RATE.
        return override.getOrDefault(key, base + flat.getOrDefault(key, 0.0) + percent.getOrDefault(key, 0.0));
    }
    public double primary(StatKey flatKey, StatKey percentKey, double base) {
        double ratio=override.getOrDefault(percentKey,flat.getOrDefault(percentKey, 0.0) + percent.getOrDefault(percentKey, 0.0) + percent.getOrDefault(flatKey, 0.0));
        double value = (base + flat.getOrDefault(flatKey, 0.0)) * (1+ratio);
        return override.getOrDefault(flatKey, value);
    }
}
```

## TriggerChain.java

```java
package com.github.saku0817.combatcoresystems.model.trigger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Shared by synchronous damage/heal/stack child events. */
public final class TriggerChain {
    private final UUID id = UUID.randomUUID();
    private final Set<String> executed = new HashSet<>();
    private int depth;
    public UUID id() { return id; }
    public int depth() { return depth; }
    public boolean enter(UUID owner, String source, String trigger) {
        if (depth >= 16 || !executed.add(owner + ":" + source + ":" + trigger)) return false;
        depth++;
        return true;
    }
    public void leave() {
        if (depth <= 0) throw new IllegalStateException("Unbalanced trigger chain");
        depth--;
    }
}
```

## TriggerDefinition.java

```java
package com.github.saku0817.combatcoresystems.model.trigger;

import com.github.saku0817.combatcoresystems.model.Element;
import com.github.saku0817.combatcoresystems.model.StatKey;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Immutable validated configuration. Bukkit sections never escape the compilation step. */
public record TriggerDefinition(String id, TriggerEvent event, String target, Map<String,Object> conditions,
                                List<Map<String,Object>> actions, long cooldown, int maxActivations,
                                String activationScope, double hpPercent) {
    public static final Set<String> SELECTORS = Set.of("SELF", "SOURCE", "OTHER", "EVENT_TARGET", "ATTACKER", "VICTIM",
            "NEAREST_ENEMY", "NEAREST_ALLY", "LOWEST_HP_PARTY_MEMBER", "LOWEST_HP_PARTY_MEMBER_OR_SELF",
            "ALL_PARTY_MEMBERS", "ALL_PARTY_MEMBERS_AND_SELF", "ENTITIES_IN_AREA", "ALLIES_IN_AREA", "ENEMIES_IN_AREA");
    public static final Set<String> SCOPES = Set.of("SELF", "TARGET", "EVENT_TARGET", "ATTACKER", "VICTIM", "ALL_TARGETS");
    private static final Set<String> ACTIONS = Set.of("APPLY_EFFECT", "REMOVE_EFFECT", "ADD_STACK", "SET_STACK", "CLEAR_STACK",
            "CONSUME_STACK", "DAMAGE", "HEAL", "MODIFY_EVENT_STATS", "CREATE_FIELD", "REMOVE_FIELD", "APPLY_DYNAMIC_MODIFIER");
    private static final Set<String> CONDITIONS = Set.of("min-hp-percent", "max-hp-percent", "requires-combat", "requires-target",
            "min-distance", "max-distance", "damage-positive", "heal-positive", "overheal-positive", "normal-attack-only",
            "skill-only", "ultimate-only", "element", "critical", "buff-present", "buff-absent", "party-required",
            "target-is-self", "target-is-ally", "target-is-enemy", "inside-field", "outside-field", "stack", "context-value");
    public static Map<String,Object> map(Object value) {
        Map<?,?> raw;
        if (value instanceof ConfigurationSection section) raw = section.getValues(false);
        else if (value instanceof Map<?,?> m) raw = m;
        else throw new IllegalArgumentException("Expected map");
        Map<String,Object> out = new LinkedHashMap<>();
        raw.forEach((k,v) -> out.put(String.valueOf(k), freeze(v)));
        return Collections.unmodifiableMap(out);
    }
    private static Object freeze(Object value) {
        if (value instanceof Map<?,?> || value instanceof ConfigurationSection) return map(value);
        if (value instanceof List<?> list) return list.stream().map(TriggerDefinition::freeze).toList();
        if (value instanceof Number n && !Double.isFinite(n.doubleValue())) throw new IllegalArgumentException("Non-finite number");
        return value;
    }
    public static String text(Map<String,Object> m, String key, String fallback) {
        Object value = m.get(key); return value == null ? fallback : String.valueOf(value);
    }
    public static double number(Map<String,Object> m, String key, double fallback) {
        if (!m.containsKey(key)) return fallback;
        if (!(m.get(key) instanceof Number n) || !Double.isFinite(n.doubleValue())) throw new IllegalArgumentException("Invalid number: " + key);
        return n.doubleValue();
    }
    public static double range(Map<String,Object> m, String key, double fallback, double min, double max) {
        double n = number(m,key,fallback);
        if (n < min || n > max) throw new IllegalArgumentException("Out of range: " + key);
        return n;
    }
    public static int integer(Map<String,Object> m, String key, int fallback, int min) {
        double n = range(m,key,fallback,min,Integer.MAX_VALUE);
        if (n != Math.rint(n)) throw new IllegalArgumentException("Expected integer: " + key);
        return (int)n;
    }
    public static Map<String,Object> child(Map<String,Object> m, String key) { return m.containsKey(key) ? map(m.get(key)) : Map.of(); }
    public static String choice(Map<String,Object> m, String key, String fallback, Set<String> choices) {
        String v = text(m,key,fallback).toUpperCase(Locale.ROOT);
        if (!choices.contains(v)) throw new IllegalArgumentException("Unknown " + key + ": " + v);
        return v;
    }
    public static List<String> strings(Map<String,Object> m, String key) {
        if (!m.containsKey(key)) return List.of();
        if (!(m.get(key) instanceof List<?> list) || list.isEmpty() || list.stream().anyMatch(v -> !(v instanceof String s) || s.isBlank()))
            throw new IllegalArgumentException("Expected nonempty string list: " + key);
        return list.stream().map(String::valueOf).toList();
    }
    public static List<TriggerDefinition> parse(ConfigurationSection section, Set<String> buffs) {
        if (section == null) return List.of();
        List<TriggerDefinition> out = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            Map<String,Object> m = map(section.get(id));
            TriggerEvent event = TriggerEvent.valueOf(text(m,"event", "").toUpperCase(Locale.ROOT));
            String target = choice(m,"target","SELF",SELECTORS);
            Map<String,Object> conditions = new LinkedHashMap<>(child(m,"conditions")); validateConditions(conditions,buffs);
            if (m.containsKey("effects") && target.equals("OTHER")) conditions.put("requires-target",true);
            List<Map<String,Object>> actions = new ArrayList<>(actions(m,buffs));
            for (String effect : strings(m,"effects")) {
                effect(effect,buffs); actions.add(Map.of("type","APPLY_EFFECT","effect",effect,"target",target));
            }
            if (actions.isEmpty()) throw new IllegalArgumentException("Trigger has no actions: " + id);
            out.add(new TriggerDefinition(id,event,target,Map.copyOf(conditions),List.copyOf(actions),
                    Math.round(range(m,"cooldown-seconds",m.containsKey("actions") ? 0 : 1,0,86400)*1000),
                    integer(m,"max-activations",0,0),choice(m,"activation-scope","GLOBAL",Set.of("GLOBAL","COMBAT","LIFE")),
                    range(m,"hp-percent",.5,0,1)));
        }
        return List.copyOf(out);
    }
    public static void validateConditions(Map<String,Object> m, Set<String> buffs) {
        for (String key : m.keySet()) {
            if (!CONDITIONS.contains(key)) throw new IllegalArgumentException("Unknown condition: " + key);
            switch (key) {
                case "min-hp-percent", "max-hp-percent" -> range(m,key,0,0,1);
                case "min-distance", "max-distance" -> range(m,key,0,0,1024);
                case "element" -> attribute(text(m,key,""));
                case "buff-present", "buff-absent" -> effect(text(m,key,""),buffs);
                case "inside-field", "outside-field" -> required(m,key);
                case "stack", "context-value" -> {
                    var c = child(m,key); required(c,key.equals("stack") ? "id" : "key");
                    if (key.equals("stack")) choice(c,"scope","SELF",SCOPES);
                    if (c.containsKey("min")) number(c,"min",0);
                    if (c.containsKey("max")) number(c,"max",0);
                    if (number(c,"min",-Double.MAX_VALUE) > number(c,"max",Double.MAX_VALUE)) throw new IllegalArgumentException("Reversed condition bounds");
                }
                default -> { if (!(m.get(key) instanceof Boolean)) throw new IllegalArgumentException("Expected boolean: " + key); }
            }
        }
    }
    private static void required(Map<String,Object> m,String key) { if (text(m,key,"").isBlank()) throw new IllegalArgumentException("Missing " + key); }
    private static void effect(String id,Set<String> buffs) { if (!buffs.contains(id)) throw new IllegalArgumentException("Unknown buff: " + id); }
    private static void attribute(String name) { if (Element.parse(name).isEmpty()) throw new IllegalArgumentException("Unknown attribute: " + name); }
    public static Area area(Map<String,Object> m) {
        return new Area(Area.Shape.valueOf(text(m,"shape","SPHERE").toUpperCase(Locale.ROOT)),
                range(m,m.containsKey("range") ? "range" : "radius",5,0,128),
                range(m,"width",5,0,128),range(m,"height",5,0,128),range(m,"length",5,0,128),range(m,"angle",90,0,360),
                Area.Origin.valueOf(text(m,"origin","SELF").toUpperCase(Locale.ROOT)));
    }
    public static void modifiers(Map<String,Object> m) {
        if (m.isEmpty()) throw new IllegalArgumentException("Empty modifiers");
        for (var entry : m.entrySet()) {
            if (!Set.of("flat","percent","override").contains(entry.getKey())) throw new IllegalArgumentException("Unknown modifier mode: " + entry.getKey());
            var values = map(entry.getValue());
            for (String stat : values.keySet()) { StatKey.valueOf(stat); number(values,stat,0); }
        }
    }
    public static List<Map<String,Object>> actions(Map<String,Object> m, Set<String> buffs) {
        if (!m.containsKey("actions")) return List.of();
        if (!(m.get("actions") instanceof List<?> list) || list.isEmpty()) throw new IllegalArgumentException("Empty/invalid actions");
        List<Map<String,Object>> out = new ArrayList<>();
        for (Object raw : list) {
            Map<String,Object> a = map(raw);
            String type = choice(a,"type","",ACTIONS);
            choice(a,"target","SELF",SELECTORS); validateConditions(child(a,"conditions"),buffs);
            if (a.containsKey("area")) area(child(a,"area"));
            choice(a,"target-filter","ALL",Set.of("ALL","ALLY","ENEMY"));
            range(a,"duration",0,0,86400); range(a,"multiplier",1,0,1e9);
            switch (type) {
                case "APPLY_EFFECT", "REMOVE_EFFECT" -> effect(text(a,"effect",""),buffs);
                case "ADD_STACK", "SET_STACK", "CLEAR_STACK", "CONSUME_STACK" -> {
                    required(a,"id"); choice(a,"scope","SELF",SCOPES);
                    if (text(a,"scope","SELF").equalsIgnoreCase("ALL_TARGETS") && !type.equals("CLEAR_STACK")) throw new IllegalArgumentException("ALL_TARGETS action scope requires CLEAR_STACK");
                    integer(a,"amount",1,0); integer(a,"max",1,1);
                    choice(a,"reapply","REFRESH",Set.of("REFRESH","EXTEND","IGNORE"));
                }
                case "MODIFY_EVENT_STATS" -> modifiers(child(a,"modifiers"));
                case "DAMAGE", "HEAL" -> {
                    choice(a,"reference","ATK", type.equals("DAMAGE") ? Set.of("ATK","HP","DEF") :
                            Set.of("ATK","HP","DEF","FIXED","EVENT_DAMAGE","EVENT_HEAL","EVENT_EFFECTIVE_HEAL","EVENT_OVERHEAL"));
                    attribute(text(a,"attribute","PHYSICAL")); range(a,"def-ignore",0,0,1);
                    if (a.containsKey("components")) {
                        if (!(a.get("components") instanceof List<?> components) || components.isEmpty()) throw new IllegalArgumentException("Empty components");
                        for (Object component : components) {
                            var c = map(component); choice(c,"reference","ATK",Set.of("ATK","HP","DEF"));
                            range(c,"multiplier",1,0,1e9); attribute(text(c,"bonus-attribute","PHYSICAL"));
                        }
                    }
                }
                case "CREATE_FIELD" -> {
                    required(a,"id"); area(child(a,"area"));
                    choice(a,"effect-mode","WHILE_INSIDE",Set.of("ON_ENTER","REFRESH_WHILE_INSIDE","WHILE_INSIDE"));
                    for (String key : List.of("ally-effects","enemy-effects")) for (String id : strings(a,key)) effect(id,buffs);
                }
                case "REMOVE_FIELD" -> required(a,"id");
                case "APPLY_DYNAMIC_MODIFIER" -> {
                    StatKey.valueOf(text(a,"stat",""));
                    choice(a,"source","",Set.of("EVENT_DAMAGE","EVENT_HEAL","EVENT_EFFECTIVE_HEAL","EVENT_OVERHEAL",
                            "SOURCE_ATK","SOURCE_MAX_HP","SOURCE_DEF","TARGET_ATK","TARGET_MAX_HP","TARGET_DEF"));
                    choice(a,"reapply","REFRESH",Set.of("REFRESH","EXTEND","IGNORE"));
                }
            }
            Map<String,Object> normalized=new LinkedHashMap<>(a);
            for (String key : List.of("type","target","scope","reapply","reference","attribute","target-filter","effect-mode","source"))
                if (normalized.containsKey(key)) normalized.put(key,text(a,key,"").toUpperCase(Locale.ROOT));
            out.add(Collections.unmodifiableMap(normalized));
        }
        return List.copyOf(out);
    }
    public static void policy(Map<String,Object> m) {
        if (m.isEmpty()) return;
        required(m,"stack-id"); integer(m,"max-targets",0,0);
        choice(m,"overflow","REMOVE_OLDEST",Set.of("REMOVE_OLDEST","REMOVE_NEWEST","REMOVE_LOWEST_STACK","REJECT_NEW"));
    }
}
```

## TriggerEvent.java

```java
package com.github.saku0817.combatcoresystems.model.trigger;

public enum TriggerEvent {
    SKILL, ULTIMATE, HIT, TAKE_DAMAGE, HP_BELOW, BEFORE_HIT, NORMAL_ATTACK,
    HEAL, RECEIVE_HEAL, OVERHEAL, HP_ABOVE, BUFF_APPLIED, BUFF_REMOVED,
    STACK_CHANGED, STACK_REACHED, COMBAT_START, COMBAT_END, ENTER_FIELD, LEAVE_FIELD, TICK
}
```

## TriggerService.java

```java
package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest;
import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageResult;
import com.github.saku0817.combatcoresystems.api.v1.event.AfterDamageEvent;
import com.github.saku0817.combatcoresystems.api.v1.event.CombatStateEvent;
import com.github.saku0817.combatcoresystems.config.*;
import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.model.trigger.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.function.Supplier;
import static com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition.*;

/** Shared synchronous event engine. Sources are compiled once per validated snapshot. */
public final class TriggerService implements Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final ItemService items;
    private final StatService stats;
    private final CombatStateService combat;
    private final DamageService damage;
    private final HealService healing;
    private final BuffService buffs;
    private final TargetSelectorService selectors;
    private final StackService stacks=new StackService();
    private final DynamicEffectService dynamic=new DynamicEffectService();
    private final FieldService fields;
    private DefinitionRegistry.Snapshot snapshot;
    private Map<String,List<TriggerCatalog.Source>> catalog=Map.of();
    private final Map<String,Long> cooldowns=new HashMap<>();
    private final Map<String,Integer> activations=new HashMap<>();
    private final Map<String,Boolean> hpStates=new HashMap<>();
    private final Map<UUID,String> combats=new HashMap<>();
    private EventContext current;
    private double nextDefIgnore;
    private boolean resetting;
    private boolean scanInventory,scanHotbar,monitors;
    public TriggerService(JavaPlugin plugin,DefinitionRegistry definitions,PlayerDataService players,ItemService items,
                          StatService stats,CombatStateService combat,DamageService damage,HealService healing,BuffService buffs,
                          PartyService parties,MobService mobs) {
        this.plugin=plugin; this.definitions=definitions; this.players=players; this.items=items; this.stats=stats;
        this.combat=combat; this.damage=damage; this.healing=healing; this.buffs=buffs;
        selectors=new TargetSelectorService(parties,damage,players,stats,mobs);
        fields=new FieldService(selectors,buffs,this::emit);
        stacks.onChange(this::stackChanged);
    }
    public DynamicEffectService dynamic() { return dynamic; }
    public EventContext current() { return current; }
    public void start() { refresh(); Bukkit.getScheduler().runTaskTimer(plugin,this::tick,5,5); }
    private void refresh() {
        if (snapshot==definitions.snapshot()) return;
        snapshot=definitions.snapshot();
        List<String> errors=new ArrayList<>();
        Map<String,org.bukkit.configuration.file.YamlConfiguration> files=new HashMap<>();
        for (String file : List.of("weapons.yml","equipment.yml","sets.yml","divine_hearts.yml")) files.put(file,snapshot.config(file));
        catalog=TriggerCatalog.compile(files,snapshot.buffs().keySet(),errors);
        var all=catalog.values().stream().flatMap(Collection::stream).toList();
        scanInventory=all.stream().anyMatch(s -> s.placement().equals("talent") && text(s.options(),"hand","MAIN_HAND").equalsIgnoreCase("INVENTORY"));
        scanHotbar=all.stream().anyMatch(s -> s.placement().equals("talent") && text(s.options(),"hand","MAIN_HAND").equalsIgnoreCase("HOT_BAR"));
        monitors=all.stream().flatMap(s -> s.triggers().stream()).anyMatch(t -> Set.of(TriggerEvent.HP_BELOW,TriggerEvent.HP_ABOVE,TriggerEvent.TICK).contains(t.event()));
        resetting=true;
        try { fields.reset(); stacks.reset(); dynamic.reset(); cooldowns.clear(); activations.clear(); hpStates.clear(); }
        finally { resetting=false; }
        errors.forEach(plugin.getLogger()::warning);
    }
    private List<TriggerCatalog.Source> sources(Player player) {
        refresh(); PlayerData data=players.find(player.getUniqueId()).orElse(null); if (data==null) return List.of();
        List<TriggerCatalog.Source> result=new ArrayList<>(); Set<String> seen=new HashSet<>();
        int selected=player.getInventory().getHeldItemSlot();
        for (int slot=0;slot<=40;slot++) {
            if (slot>=36 && slot<=39) continue;
            if (slot!=selected && slot!=40 && !scanInventory && !(slot<=8 && scanHotbar)) continue;
            var stack=player.getInventory().getItem(slot);
            if (!snapshot.weapons().containsKey(items.id(stack).orElse(""))) continue;
            ItemInstance item=items.instance(stack).orElse(null);
            WeaponDefinition weapon=item==null ? null : snapshot.weapon(item);
            if (weapon==null || !weapon.canEquip(data.getLevel())) continue;
            for (var source : catalog.getOrDefault("weapon:"+weapon.id()+":"+Math.clamp(item.getLimitBreak(),0,5),List.of())) {
                boolean active=slot==selected || slot==40;
                if (source.placement().equals("talent")) {
                    WeaponOptions.Hand hand=WeaponOptions.Hand.valueOf(text(source.options(),"hand","MAIN_HAND").toUpperCase(Locale.ROOT));
                    active=hand.includes(slot,selected);
                }
                if (active && seen.add(source.definition()+":"+source.placement())) result.add(source);
            }
        }
        for (ItemInstance item : data.getEquipment().values()) {
            String root=snapshot.equipment().containsKey(item.getDefinitionId()) ? "equipment:" : "divine-hearts:";
            if (seen.add(root+item.getDefinitionId())) result.addAll(catalog.getOrDefault(root+item.getDefinitionId(),List.of()));
        }
        for (var set : SetEffectService.counts(snapshot,data).entrySet()) for (var source : catalog.getOrDefault("sets:"+set.getKey(),List.of()))
            if (set.getValue()>=(source.placement().equals("two-piece") ? 2 : 4)) result.add(source);
        return result;
    }
    public void emit(EventContext context) {
        if (resetting) return;
        if (!(context.source instanceof Player player) || !player.isOnline() || players.find(player.getUniqueId()).isEmpty()) return;
        EventContext prior=current; current=context;
        try {
            context.player=player; context.combatId=combats.getOrDefault(player.getUniqueId(),"");
            for (var source : sources(player)) for (var trigger : source.triggers()) {
                if (trigger.event()!=context.event) continue;
                if ((context.event==TriggerEvent.SKILL || context.event==TriggerEvent.ULTIMATE)
                        && source.definition().startsWith("weapon:") && !context.weapon.isBlank() && !source.definition().equals("weapon:"+context.weapon)) continue;
                if (source.placement().equals("skill") && context.event==TriggerEvent.ULTIMATE
                        || source.placement().equals("ultimate") && context.event==TriggerEvent.SKILL) continue;
                if (Set.of(TriggerEvent.BEFORE_HIT,TriggerEvent.HIT,TriggerEvent.NORMAL_ATTACK,TriggerEvent.HEAL,TriggerEvent.OVERHEAL).contains(context.event)) {
                    if (source.placement().equals("skill") && !context.skill || source.placement().equals("ultimate") && !context.ultimate) continue;
                }
                if (context.values.containsKey("stackSource") && !source.definition().equals(context.values.get("stackSource"))) continue;
                String key=player.getUniqueId()+":"+source.definition()+":"+source.placement()+":"+trigger.id();
                if (context.event==TriggerEvent.HP_BELOW || context.event==TriggerEvent.HP_ABOVE) {
                    double ratio=selectors.health(player)/Math.max(1,selectors.maxHealth(player));
                    boolean active=context.event==TriggerEvent.HP_BELOW ? ratio<=trigger.hpPercent() : ratio>trigger.hpPercent();
                    Boolean before=hpStates.put(key,active);
                    if (!active || Boolean.TRUE.equals(before)) continue;
                }
                if (context.event==TriggerEvent.STACK_REACHED) {
                    var condition=child(trigger.conditions(),"stack");
                    if (!Objects.equals(text(condition,"id",""),context.values.get("stackId"))) continue;
                    double threshold=number(condition,"min",1);
                    if (number(context.values,"oldStacks",0)>=threshold || number(context.values,"newStacks",0)<threshold) continue;
                }
                if (!conditions(trigger.conditions(),player,source.definition(),context)) continue;
                long now=System.currentTimeMillis();
                if (cooldowns.getOrDefault(key,0L)>now) continue;
                String counter=key+":"+trigger.activationScope()+(trigger.activationScope().equals("COMBAT") ? ":"+context.combatId : "");
                if (trigger.activationScope().equals("COMBAT") && !combat.inCombat(player.getUniqueId()) && context.event!=TriggerEvent.COMBAT_END) continue;
                if (trigger.maxActivations()>0 && activations.getOrDefault(counter,0)>=trigger.maxActivations()) continue;
                if (!context.chain.enter(player.getUniqueId(),source.definition(),source.placement()+":"+trigger.id())) {
                    if (context.chain.depth()>=16) plugin.getLogger().warning("Trigger chain depth limit reached: "+context.chain.id());
                    continue;
                }
                cooldowns.put(key,now+trigger.cooldown());
                if (trigger.maxActivations()>0) activations.merge(counter,1,Integer::sum);
                try { actions(player,source,trigger.actions(),context,trigger.target(),trigger.id()); }
                finally { context.chain.leave(); }
            }
        } finally { current=prior; }
    }
    public boolean conditions(Map<String,Object> c,Player owner,String definition,EventContext context) {
        double hp=selectors.health(owner)/Math.max(1,selectors.maxHealth(owner)); LivingEntity target=context.target;
        for (String key : c.keySet()) {
            boolean actual;
            switch(key) {
                case "min-hp-percent" -> { if (hp<number(c,key,0)) return false; continue; }
                case "max-hp-percent" -> { if (hp>number(c,key,1)) return false; continue; }
                case "min-distance", "max-distance" -> {
                    if (target==null || !owner.getWorld().equals(target.getWorld())) return false;
                    double distance=owner.getLocation().distance(target.getLocation());
                    if (key.equals("min-distance") ? distance<number(c,key,0) : distance>number(c,key,0)) return false; continue;
                }
                case "element" -> { if (context.attribute!=Element.parse(text(c,key,"")).orElse(null)) return false; continue; }
                case "buff-present" -> { if (!buffs.has(owner,text(c,key,""))) return false; continue; }
                case "buff-absent" -> { if (buffs.has(owner,text(c,key,""))) return false; continue; }
                case "inside-field" -> { if (!fields.inside(owner,text(c,key,""))) return false; continue; }
                case "outside-field" -> { if (fields.inside(owner,text(c,key,""))) return false; continue; }
                case "stack", "context-value" -> {
                    var condition=child(c,key); double value;
                    if (key.equals("context-value")) {
                        Object raw=context.values.get(text(condition,"key","")); if (!(raw instanceof Number n)) return false; value=n.doubleValue();
                    } else {
                        String scope=text(condition,"scope","SELF").toUpperCase(Locale.ROOT), id=text(condition,"id","");
                        StackService.Key stack=stackKey(owner,definition,id,scope,context);
                        value=scope.equals("ALL_TARGETS") ? stacks.total(owner.getUniqueId(),definition,id) : stack==null ? 0 : stacks.count(stack);
                    }
                    if (value<number(condition,"min",-Double.MAX_VALUE) || value>number(condition,"max",Double.MAX_VALUE)) return false; continue;
                }
                case "requires-combat" -> actual=combat.inCombat(owner.getUniqueId());
                case "requires-target" -> actual=target!=null && !target.isDead();
                case "damage-positive" -> actual=context.finalDamage>0;
                case "heal-positive" -> actual=context.healAmount>0;
                case "overheal-positive" -> actual=context.overheal>0;
                case "normal-attack-only" -> actual=context.normalAttack;
                case "skill-only" -> actual=context.skill;
                case "ultimate-only" -> actual=context.ultimate;
                case "critical" -> actual=context.critical;
                case "party-required" -> actual=selectors.hasParty(owner);
                case "target-is-self" -> actual=owner.equals(target);
                case "target-is-ally" -> actual=target!=null && selectors.ally(owner,target);
                case "target-is-enemy" -> actual=target!=null && selectors.enemy(owner,target);
                default -> throw new IllegalArgumentException("Unknown condition "+key);
            }
            if (actual!=Boolean.TRUE.equals(c.get(key))) return false;
        }
        return true;
    }
    private StackService.Key stackKey(Player owner,String source,String id,String scope,EventContext context) {
        LivingEntity target=switch(scope) { case "TARGET","EVENT_TARGET" -> context.target; case "ATTACKER" -> context.attacker; case "VICTIM" -> context.victim; default -> owner; };
        if (target==null) return null;
        return new StackService.Key(owner.getUniqueId(),source,id,scope.equals("SELF") ? null : target.getUniqueId());
    }
    private void actions(Player owner,TriggerCatalog.Source source,List<Map<String,Object>> actions,EventContext context,String fallbackTarget,String triggerId) {
        int index=0;
        for (var action : actions) {
            String actionKey=source.definition()+":"+source.placement()+":"+triggerId+":"+(index++);
            if (!conditions(child(action,"conditions"),owner,source.definition(),context)) continue;
            String type=text(action,"type","").toUpperCase(Locale.ROOT); double result=0;
            String scope=text(action,"scope","SELF").toUpperCase(Locale.ROOT),id=text(action,"id","");
            if (Set.of("ADD_STACK","SET_STACK","CLEAR_STACK","CONSUME_STACK").contains(type)) {
                StackService.Key key=stackKey(owner,source.definition(),id,scope,context);
                if (type.equals("CLEAR_STACK") && scope.equals("ALL_TARGETS")) result=stacks.clearTargets(owner.getUniqueId(),source.definition(),id);
                else if (key!=null) {
                    var p=child(source.options(),"target-stack-policy");
                    var policy=new StackService.Policy(text(p,"stack-id","").equals(id) ? integer(p,"max-targets",0,0) : 0,
                            StackService.Overflow.valueOf(text(p,"overflow","REMOVE_OLDEST").toUpperCase(Locale.ROOT)));
                    int amount=integer(action,"amount",1,0), max=integer(action,"max",1,1);
                    long duration=Math.round(number(action,"duration",0)*1000);
                    var reapply=StackService.Reapply.valueOf(text(action,"reapply","REFRESH"));
                    result=switch(type) {
                        case "ADD_STACK" -> stacks.add(key,amount,max,duration,reapply,policy);
                        case "SET_STACK" -> stacks.set(key,amount,max,duration,reapply,policy);
                        case "CLEAR_STACK" -> stacks.clear(key);
                        default -> stacks.consume(key,amount);
                    };
                }
            } else if (type.equals("CREATE_FIELD")) fields.create(owner,source.definition(),action,context);
            else if (type.equals("REMOVE_FIELD")) fields.remove(owner.getUniqueId(),source.definition(),id);
            else if (type.equals("MODIFY_EVENT_STATS")) {
                if (context.event!=TriggerEvent.BEFORE_HIT) continue;
                EventModifier modifier=Set.of("EVENT_TARGET","VICTIM","OTHER").contains(text(action,"target","SOURCE")) ? context.targetModifiers : context.sourceModifiers;
                child(action,"modifiers").forEach((mode,values) -> map(values).forEach((stat,value) -> modifier.add(mode,StatKey.valueOf(stat),((Number)value).doubleValue())));
            } else for (LivingEntity target : selectors.select(text(action,"target",fallbackTarget).toUpperCase(Locale.ROOT),owner,context,action)) {
                switch(type) {
                    case "APPLY_EFFECT" -> { if (buffs.apply(target,text(action,"effect",""),owner.getUniqueId())) result++; }
                    case "REMOVE_EFFECT" -> { if (buffs.remove(target,text(action,"effect",""))) result++; }
                    case "DAMAGE" -> {
                        List<WeaponOptions.Component> components=new ArrayList<>();
                        if (action.get("components") instanceof List<?> values) for (Object raw : values) {
                            var c=map(raw); components.add(new WeaponOptions.Component(ReferenceStat.valueOf(text(c,"reference","ATK").toUpperCase(Locale.ROOT)),number(c,"multiplier",1),Element.parse(text(c,"bonus-attribute","PHYSICAL")).orElseThrow()));
                        }
                        double prior=nextDefIgnore; nextDefIgnore=number(action,"def-ignore",0);
                        try { result+=damage.apply(new DamageRequest(owner.getUniqueId(),target.getUniqueId(),ReferenceStat.valueOf(text(action,"reference","ATK")),
                                number(action,"multiplier",1),Element.parse(text(action,"attribute","PHYSICAL")).orElseThrow(),true,false,0,"trigger:"+actionKey,components)).finalDamage(); }
                        finally { nextDefIgnore=prior; }
                    }
                    case "HEAL" -> result+=healing.healAmount(owner,target,reference(text(action,"reference","HP"),owner,target,context)*number(action,"multiplier",1),false);
                    case "APPLY_DYNAMIC_MODIFIER" -> {
                        double value=reference(text(action,"source",""),owner,target,context)*number(action,"multiplier",1);
                        dynamic.apply(target.getUniqueId(),owner.getUniqueId()+":"+actionKey,StatKey.valueOf(text(action,"stat","")),value,
                                Math.round(number(action,"duration",0)*1000),text(action,"reapply","REFRESH")); stats.invalidate(target.getUniqueId()); result=value;
                    }
                }
            }
            if (action.containsKey("store-result")) context.values.put(text(action,"store-result",""),result);
        }
    }
    private double reference(String key,LivingEntity owner,LivingEntity target,EventContext context) {
        return switch(key) {
            case "FIXED" -> 1;
            case "EVENT_DAMAGE" -> context.finalDamage;
            case "EVENT_HEAL" -> context.healAmount;
            case "EVENT_EFFECTIVE_HEAL" -> context.effectiveHeal;
            case "EVENT_OVERHEAL" -> context.overheal;
            case "TARGET_MAX_HP" -> selectors.maxHealth(target);
            case "TARGET_ATK" -> healing.value(target,ReferenceStat.ATK);
            case "TARGET_DEF" -> healing.value(target,ReferenceStat.DEF);
            case "HP","SOURCE_MAX_HP" -> selectors.maxHealth(owner);
            case "DEF","SOURCE_DEF" -> healing.value(owner,ReferenceStat.DEF);
            default -> healing.value(owner,ReferenceStat.ATK);
        };
    }
    public DamageResult damage(DamageRequest request,Supplier<DamageResult> operation) {
        LivingEntity attacker=request.attacker()!=null && Bukkit.getEntity(request.attacker()) instanceof LivingEntity e ? e : null;
        LivingEntity target=Bukkit.getEntity(request.target()) instanceof LivingEntity e ? e : null;
        EventContext event=new EventContext(TriggerEvent.BEFORE_HIT,current==null ? null : current.chain,attacker,target);
        if (attacker instanceof Player p) event.weapon=items.id(p.getInventory().getItemInMainHand()).orElse("");
        if (request.source().startsWith("skill:") || request.source().startsWith("ultimate:")) event.ability=request.source().substring(request.source().indexOf(':')+1);
        event.attribute=request.element(); event.normalAttack=request.source().equals("normal_attack");
        event.skill=request.source().startsWith("skill:") || request.source().startsWith("trigger:") && current!=null && current.skill;
        event.ultimate=request.source().startsWith("ultimate:") || request.source().startsWith("trigger:") && current!=null && current.ultimate;
        event.reaction=request.source().startsWith("reaction:"); event.sourceModifiers.add("flat",StatKey.DEF_IGNORE,nextDefIgnore);
        double priorIgnore=nextDefIgnore; nextDefIgnore=0;
        EventContext prior=current; current=event;
        try { return operation.get(); }
        finally { current=prior; nextDefIgnore=priorIgnore; }
    }
    public void beforeHit() { if (current!=null) emit(current); }
    @EventHandler public void onDamage(AfterDamageEvent event) {
        if (!event.getResult().applied() || event.getResult().finalDamage()<=0) return;
        var request=event.getRequest();
        LivingEntity attacker=request.attacker()!=null && Bukkit.getEntity(request.attacker()) instanceof LivingEntity e ? e : null;
        LivingEntity victim=Bukkit.getEntity(request.target()) instanceof LivingEntity e ? e : null;
        EventContext hit=current==null ? new EventContext(TriggerEvent.HIT,null,attacker,victim) : current.child(TriggerEvent.HIT,attacker,victim);
        hit.finalDamage=event.getResult().finalDamage(); hit.damage=hit.finalDamage; hit.critical=event.getResult().critical();
        hit.attribute=request.element(); hit.reaction=request.source().startsWith("reaction:"); hit.normalAttack=request.source().equals("normal_attack");
        hit.skill|=request.source().startsWith("skill:"); hit.ultimate|=request.source().startsWith("ultimate:");
        emit(hit);
        if (hit.normalAttack) emit(hit.child(TriggerEvent.NORMAL_ATTACK,attacker,victim));
        EventContext received=hit.child(TriggerEvent.TAKE_DAMAGE,victim,attacker); received.attacker=attacker; received.victim=victim; emit(received);
    }
    public void cast(Player owner,LivingEntity target,String weapon,int stage,boolean ultimate) {
        EventContext event=new EventContext(ultimate ? TriggerEvent.ULTIMATE : TriggerEvent.SKILL,current==null ? null : current.chain,owner,target);
        event.weapon=weapon; event.skill=!ultimate; event.ultimate=ultimate;
        emit(event);
        for (var source : sources(owner)) if (source.definition().equals("weapon:"+weapon) && source.placement().equals(ultimate ? "ultimate" : "skill")) {
            EventContext prior=current; current=event;
            try {
                var direct=new ArrayList<Map<String,Object>>();
                for (var action : TriggerDefinition.actions(source.options(),snapshot.buffs().keySet())) {
                    var inherited=new LinkedHashMap<>(action);
                    if (!inherited.containsKey("area") && source.options().containsKey("area")) inherited.put("area",source.options().get("area"));
                    direct.add(inherited);
                }
                actions(owner,source,direct,event,"SELF","direct");
            }
            finally { current=prior; }
        }
    }
    public boolean canCast(Player owner,LivingEntity target,String weapon,boolean ultimate,Map<String,Object> raw) {
        Map<String,Object> additional=new LinkedHashMap<>(map(raw));
        // v1.4.4 intentionally lets empty casts bypass target/distance requirements.
        for (String legacy : List.of("min-hp-percent","max-distance","requires-combat","requires-target")) additional.remove(legacy);
        EventContext event=new EventContext(ultimate ? TriggerEvent.ULTIMATE : TriggerEvent.SKILL,null,owner,target);
        event.skill=!ultimate; event.ultimate=ultimate;
        return conditions(additional,owner,"weapon:"+weapon,event);
    }
    public void healed(LivingEntity source,LivingEntity target,double requested,double effective,double overheal) {
        EventContext event=current==null ? new EventContext(TriggerEvent.HEAL,null,source,target) : current.child(TriggerEvent.HEAL,source,target);
        event.healer=source; event.healed=target; event.healAmount=requested; event.requestedHeal=requested; event.effectiveHeal=effective; event.overheal=overheal;
        emit(event); emit(event.child(TriggerEvent.RECEIVE_HEAL,target,source));
        if (overheal>0) emit(event.child(TriggerEvent.OVERHEAL,source,target));
    }
    public void buffEvent(LivingEntity target,String id,boolean added) {
        EventContext event=new EventContext(added ? TriggerEvent.BUFF_APPLIED : TriggerEvent.BUFF_REMOVED,current==null ? null : current.chain,target,target);
        event.values.put("buffId",id); emit(event);
    }
    private void stackChanged(StackService.Change change) {
        Player owner=Bukkit.getPlayer(change.key().owner());
        if (owner==null && current!=null && current.source instanceof Player p && p.getUniqueId().equals(change.key().owner())) owner=p;
        if (owner==null) return;
        LivingEntity target=change.key().target()!=null && Bukkit.getEntity(change.key().target()) instanceof LivingEntity e ? e : null;
        if (target==null && current!=null && current.target!=null && current.target.getUniqueId().equals(change.key().target())) target=current.target;
        EventContext event=new EventContext(TriggerEvent.STACK_CHANGED,current==null ? null : current.chain,owner,target);
        event.values.put("stackSource",change.key().source()); event.values.put("stackId",change.key().id());
        event.values.put("oldStacks",change.oldStacks()); event.values.put("newStacks",change.newStacks());
        event.values.put("stackDelta",change.newStacks()-change.oldStacks()); event.values.put("stackTarget",change.key().target());
        emit(event); if (change.newStacks()>change.oldStacks()) emit(event.child(TriggerEvent.STACK_REACHED,owner,target));
    }
    private void tick() {
        refresh(); stacks.expire(); fields.tick();
        if (!monitors) { long now=System.currentTimeMillis(); cooldowns.values().removeIf(end -> end<=now); return; }
        for (Player player : Bukkit.getOnlinePlayers()) {
            List<TriggerCatalog.Source> active=sources(player);
            Set<String> hpKeys=new HashSet<>();
            String prefix=player.getUniqueId()+":";
            for (var source : active) for (var trigger : source.triggers())
                if (trigger.event()==TriggerEvent.HP_BELOW || trigger.event()==TriggerEvent.HP_ABOVE)
                    hpKeys.add(prefix+source.definition()+":"+source.placement()+":"+trigger.id());
            hpStates.keySet().removeIf(key -> key.startsWith(prefix) && !hpKeys.contains(key));
            for (TriggerEvent event : List.of(TriggerEvent.HP_BELOW,TriggerEvent.HP_ABOVE,TriggerEvent.TICK))
                if (active.stream().anyMatch(s -> s.triggers().stream().anyMatch(t -> t.event()==event))) emit(new EventContext(event,null,player,null));
        }
        long now=System.currentTimeMillis(); cooldowns.values().removeIf(end -> end<=now);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onEnvironmentDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent || event.getFinalDamage()<=0 || !(event.getEntity() instanceof Player player)) return;
        Bukkit.getScheduler().runTask(plugin,() -> {
            if (!player.isOnline()) return;
            EventContext context=new EventContext(TriggerEvent.TAKE_DAMAGE,null,player,null);
            context.attacker=null; context.victim=player; context.damage=event.getFinalDamage(); context.finalDamage=event.getFinalDamage(); emit(context);
        });
    }
    @EventHandler public void onCombat(CombatStateEvent event) {
        // Accessors are resolved against the stable CCS API below.
        if (!(Bukkit.getEntity(event.getEntityId()) instanceof Player player)) return;
        boolean active=event.isEntering();
        String ending=combats.getOrDefault(player.getUniqueId(),"");
        if (active) combats.put(player.getUniqueId(),UUID.randomUUID().toString());
        emit(new EventContext(active ? TriggerEvent.COMBAT_START : TriggerEvent.COMBAT_END,current==null ? null : current.chain,player,null));
        if (!active) {
            combats.remove(player.getUniqueId(),ending);
            activations.keySet().removeIf(k -> k.startsWith(player.getUniqueId()+":") && k.endsWith(":COMBAT:"+ending));
        }
    }
    private void forget(UUID owner,boolean death) {
        stacks.forget(owner); fields.forget(owner); dynamic.forget(owner);
        String prefix=owner+":"; hpStates.keySet().removeIf(k -> k.startsWith(prefix)); cooldowns.keySet().removeIf(k -> k.startsWith(prefix));
        activations.keySet().removeIf(k -> k.startsWith(prefix) && (!death || k.endsWith(":LIFE") || k.contains(":COMBAT:")));
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { forget(event.getPlayer().getUniqueId(),false); combats.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void onDeath(PlayerDeathEvent event) { forget(event.getEntity().getUniqueId(),true); }
}
```

## StackService.java

```java
package com.github.saku0817.combatcoresystems.service;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Transient owner/definition-isolated stacks; server-thread access only. */
public final class StackService {
    public enum Reapply { REFRESH, EXTEND, IGNORE }
    public enum Overflow { REMOVE_OLDEST, REMOVE_NEWEST, REMOVE_LOWEST_STACK, REJECT_NEW }
    public record Key(UUID owner, String source, String id, UUID target) {}
    public record State(long createdAt, long updatedAt, int count, long expiresAt) {}
    public record Change(Key key, int oldStacks, int newStacks) {}
    public record Policy(int maxTargets, Overflow overflow) {
        public Policy { if (maxTargets < 0) throw new IllegalArgumentException("negative max-targets"); Objects.requireNonNull(overflow); }
    }
    private final Map<Key, State> states = new LinkedHashMap<>();
    private final LongSupplier clock;
    private Consumer<Change> listener = ignored -> {};
    public StackService() { this(System::currentTimeMillis); }
    public StackService(LongSupplier clock) { this.clock = clock; }
    public void onChange(Consumer<Change> listener) { this.listener = Objects.requireNonNull(listener); }
    public State state(Key key) { expire(key); return states.get(key); }
    public int count(Key key) { State state = state(key); return state == null ? 0 : state.count(); }
    public long total(UUID owner, String source, String id) {
        expire();
        return states.entrySet().stream().filter(e -> e.getKey().target()!=null && matches(e.getKey(), owner, source, id))
                .mapToLong(e -> e.getValue().count()).sum();
    }
    private static boolean matches(Key key, UUID owner, String source, String id) {
        return key.owner().equals(owner) && key.source().equals(source) && key.id().equals(id);
    }
    public int add(Key key, int amount, int max, long durationMillis, Reapply reapply, Policy policy) {
        if (amount < 0) throw new IllegalArgumentException("negative stack amount");
        return set(key, (int) Math.min(max, (long) count(key) + amount), max, durationMillis, reapply, policy);
    }
    public int set(Key key, int amount, int max, long durationMillis, Reapply reapply, Policy policy) {
        if (max < 1 || amount < 0 || durationMillis < 0) throw new IllegalArgumentException("invalid stack bounds");
        expire(key);
        State old = states.get(key);
        int before = old == null ? 0 : old.count(), after = Math.min(max, amount);
        if (after == 0) { remove(key); return 0; }
        if (old == null && key.target() != null && policy != null && policy.maxTargets() > 0) {
            List<Map.Entry<Key, State>> targets = states.entrySet().stream()
                    .filter(e -> e.getKey().target() != null && matches(e.getKey(), key.owner(), key.source(), key.id())).toList();
            if (targets.size() >= policy.maxTargets()) {
                if (policy.overflow() == Overflow.REJECT_NEW || policy.overflow() == Overflow.REMOVE_NEWEST) return 0;
                Comparator<Map.Entry<Key, State>> order = Comparator.comparingLong(e -> e.getValue().createdAt());
                if (policy.overflow() == Overflow.REMOVE_LOWEST_STACK)
                    order = Comparator.<Map.Entry<Key, State>>comparingInt(e -> e.getValue().count()).thenComparing(order);
                for (var entry : targets.stream().sorted(order).limit(targets.size()-policy.maxTargets()+1L).toList()) remove(entry.getKey());
            }
        }
        long now = clock.getAsLong();
        long expires = durationMillis == 0 ? Long.MAX_VALUE : saturatingAdd(now, durationMillis);
        if (old != null && reapply == Reapply.IGNORE) expires = old.expiresAt();
        if (old != null && reapply == Reapply.EXTEND) expires = durationMillis == 0 ? Long.MAX_VALUE : saturatingAdd(old.expiresAt(), durationMillis);
        states.put(key, new State(old == null ? now : old.createdAt(), now, after, expires));
        if (before != after) listener.accept(new Change(key, before, after));
        return after;
    }
    private static long saturatingAdd(long a, long b) { return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
    public int consume(Key key, int amount) {
        if (amount < 0) throw new IllegalArgumentException("negative consumption");
        State state = state(key);
        if (state == null) return 0;
        int used = Math.min(amount, state.count());
        if (used == state.count()) remove(key);
        else if (used > 0) {
            states.put(key, new State(state.createdAt(), clock.getAsLong(), state.count() - used, state.expiresAt()));
            listener.accept(new Change(key, state.count(), state.count() - used));
        }
        return used;
    }
    public int clear(Key key) { expire(key); return remove(key); }
    public long clearTargets(UUID owner, String source, String id) {
        expire(); long total = 0;
        for (Key key : List.copyOf(states.keySet())) if (key.target() != null && matches(key, owner, source, id)) total += remove(key);
        return total;
    }
    private int remove(Key key) {
        State old = states.remove(key);
        if (old == null) return 0;
        listener.accept(new Change(key, old.count(), 0)); return old.count();
    }
    private void expire(Key key) { State state = states.get(key); if (state != null && state.expiresAt() <= clock.getAsLong()) remove(key); }
    public void expire() { for (Key key : List.copyOf(states.keySet())) expire(key); }
    public void forget(UUID entity) { states.keySet().removeIf(key -> key.owner().equals(entity) || entity.equals(key.target())); }
    public void reset() { states.clear(); }
}
```

## TargetSelectorService.java

```java
package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.trigger.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.*;

public final class TargetSelectorService {
    private final PartyService parties;
    private final DamageService damage;
    private final PlayerDataService players;
    private final StatService stats;
    private final MobService mobs;
    public TargetSelectorService(PartyService parties,DamageService damage,PlayerDataService players,StatService stats,MobService mobs) {
        this.parties=parties; this.damage=damage; this.players=players; this.stats=stats; this.mobs=mobs;
    }
    public double health(LivingEntity entity) {
        return entity instanceof Player p ? players.find(p.getUniqueId()).map(d->d.getHealth()).orElse(0.0) : mobs.health(entity);
    }
    public double maxHealth(LivingEntity entity) {
        return entity instanceof Player p ? players.find(p.getUniqueId()).map(d->stats.get(p,d).maxHp()).orElse(1.0) : mobs.maxHealth(entity);
    }
    public boolean ally(LivingEntity owner,LivingEntity target) {
        return owner.equals(target) || owner instanceof Player && target instanceof Player && parties.sameParty(owner.getUniqueId(),target.getUniqueId());
    }
    public boolean enemy(LivingEntity owner,LivingEntity target) { return !ally(owner,target) && damage.canAffect(owner,target); }
    public boolean hasParty(LivingEntity owner) { return parties.findByPlayer(owner.getUniqueId()).isPresent(); }
    private List<LivingEntity> party(LivingEntity owner,boolean includeSelf) {
        List<LivingEntity> result=new ArrayList<>();
        parties.findByPlayer(owner.getUniqueId()).ifPresent(p -> p.getMembers().forEach(member -> {
            Player player=Bukkit.getPlayer(member.asUuid());
            if (player != null && !player.isDead() && player.getWorld().equals(owner.getWorld()) && (includeSelf || !player.equals(owner))) result.add(player);
        }));
        if (includeSelf && !result.contains(owner)) result.add(owner);
        return result;
    }
    public Location origin(LivingEntity owner,EventContext context,Area area) {
        return switch(area.origin()) {
            case SELF -> owner.getLocation();
            case EVENT_TARGET -> context.target == null ? null : context.target.getLocation();
            case LOCATION -> context.location == null ? null : context.location.clone();
        };
    }
    public List<LivingEntity> inArea(Location origin,Area area) {
        if (origin == null || origin.getWorld() == null) return List.of();
        return origin.getWorld().getNearbyLivingEntities(origin,area.searchRadius(),e -> !e.isDead() && contains(origin,area,e.getLocation())).stream()
                .sorted(Comparator.comparing(e -> e.getUniqueId().toString())).toList();
    }
    public boolean contains(Location origin,Area area,Location point) {
        if (!origin.getWorld().equals(point.getWorld())) return false;
        Vector delta=point.toVector().subtract(origin.toVector());
        Vector forward=origin.getDirection().setY(0);
        if (forward.lengthSquared()<1e-10) forward=new Vector(0,0,1); else forward.normalize();
        Vector right=new Vector(forward.getZ(),0,-forward.getX());
        return area.contains(delta.dot(right),delta.getY(),delta.dot(forward));
    }
    public List<LivingEntity> select(String selector,LivingEntity owner,EventContext context,Map<String,Object> options) {
        List<LivingEntity> out=new ArrayList<>();
        switch(selector) {
            case "SELF", "SOURCE" -> out.add(owner);
            case "EVENT_TARGET", "OTHER" -> { if (context.target!=null) out.add(context.target); }
            case "ATTACKER" -> { if (context.attacker!=null) out.add(context.attacker); }
            case "VICTIM" -> { if (context.victim!=null) out.add(context.victim); }
            case "ALL_PARTY_MEMBERS", "ALL_PARTY_MEMBERS_AND_SELF" -> out.addAll(party(owner,selector.endsWith("AND_SELF")));
            case "LOWEST_HP_PARTY_MEMBER", "LOWEST_HP_PARTY_MEMBER_OR_SELF" -> {
                List<LivingEntity> members=party(owner,true);
                if (!hasParty(owner) && !selector.endsWith("OR_SELF")) members.clear();
                members.stream().min(Comparator.<LivingEntity>comparingDouble(e -> health(e)/Math.max(1,maxHealth(e)))
                        .thenComparingDouble(e -> e.getLocation().distanceSquared(owner.getLocation())).thenComparing(e -> e.getUniqueId().toString())).ifPresent(out::add);
            }
            case "NEAREST_ENEMY", "NEAREST_ALLY" -> owner.getWorld().getNearbyLivingEntities(owner.getLocation(),64,
                    e -> !e.equals(owner) && !e.isDead() && (selector.endsWith("ENEMY") ? enemy(owner,e) : ally(owner,e))).stream()
                    .min(Comparator.<LivingEntity>comparingDouble(e -> e.getLocation().distanceSquared(owner.getLocation())).thenComparing(e -> e.getUniqueId().toString())).ifPresent(out::add);
            case "ENTITIES_IN_AREA", "ALLIES_IN_AREA", "ENEMIES_IN_AREA" -> {
                Area area=TriggerDefinition.area(TriggerDefinition.child(options,"area"));
                out.addAll(inArea(origin(owner,context,area),area));
                if (selector.equals("ALLIES_IN_AREA")) out.removeIf(e -> !ally(owner,e));
                if (selector.equals("ENEMIES_IN_AREA")) out.removeIf(e -> !enemy(owner,e));
            }
            default -> throw new IllegalArgumentException("Unknown selector: " + selector);
        }
        String filter=TriggerDefinition.text(options,"target-filter","ALL");
        out.removeIf(e -> e.isDead() || !e.getWorld().equals(owner.getWorld()) || filter.equals("ENEMY") && !enemy(owner,e) || filter.equals("ALLY") && !ally(owner,e));
        if (options.containsKey("area")) {
            Area area=TriggerDefinition.area(TriggerDefinition.child(options,"area"));
            Location origin=origin(owner,context,area);
            out.removeIf(e -> origin==null || !contains(origin,area,e.getLocation()));
        }
        return List.copyOf(out);
    }
}
```

## FieldService.java

```java
package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.trigger.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import java.util.*;
import java.util.function.Consumer;
import static com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition.*;

public final class FieldService {
    private record Key(UUID owner,String source,String id) {}
    private static final class Field {
        final Key key; final Location origin; final Area area; final long expires; final Map<String,Object> options;
        final Map<UUID,List<String>> inside=new LinkedHashMap<>();
        Field(Key key,Location origin,Area area,long expires,Map<String,Object> options) {
            this.key=key; this.origin=origin.clone(); this.area=area; this.expires=expires; this.options=options;
        }
    }
    private final Map<Key,Field> fields=new LinkedHashMap<>();
    private final TargetSelectorService selectors;
    private final BuffService buffs;
    private final Consumer<EventContext> events;
    public FieldService(TargetSelectorService selectors,BuffService buffs,Consumer<EventContext> events) { this.selectors=selectors; this.buffs=buffs; this.events=events; }
    public void create(LivingEntity owner,String source,Map<String,Object> options,EventContext context) {
        String id=text(options,"id",""); remove(owner.getUniqueId(),source,id);
        Area area=area(child(options,"area")); Location origin=selectors.origin(owner,context,area);
        if (origin==null) return;
        long duration=Math.round(number(options,"duration",0)*1000);
        Key key=new Key(owner.getUniqueId(),source,id);
        fields.put(key,new Field(key,origin,area,duration==0 ? Long.MAX_VALUE : System.currentTimeMillis()+duration,options));
    }
    public boolean inside(LivingEntity entity,String id) {
        return fields.values().stream().anyMatch(f -> f.key.id().equals(id) && f.expires>System.currentTimeMillis() && selectors.contains(f.origin,f.area,entity.getLocation()));
    }
    private String lease(Field field,String effect) { return "field:"+field.key+":"+effect; }
    private void leave(Field field,UUID target,List<String> effects,LivingEntity owner) {
        effects.forEach(id -> buffs.release(target,lease(field,id)));
        if (Bukkit.getEntity(target) instanceof LivingEntity entity) {
            EventContext event=new EventContext(TriggerEvent.LEAVE_FIELD,null,entity,owner); event.values.put("fieldId",field.key.id()); events.accept(event);
        }
    }
    public void remove(UUID owner,String source,String id) {
        Field old=fields.remove(new Key(owner,source,id)); if (old==null) return;
        LivingEntity entity=Bukkit.getEntity(owner) instanceof LivingEntity e ? e : null;
        old.inside.forEach((target,effects) -> leave(old,target,effects,entity));
    }
    public void tick() {
        for (Field field : List.copyOf(fields.values())) {
            LivingEntity owner=Bukkit.getEntity(field.key.owner()) instanceof LivingEntity e ? e : null;
            if (owner==null || owner.isDead() || field.expires<=System.currentTimeMillis()) { remove(field.key.owner(),field.key.source(),field.key.id()); continue; }
            Set<UUID> seen=new HashSet<>();
            String mode=text(field.options,"effect-mode","WHILE_INSIDE");
            for (LivingEntity target : selectors.inArea(field.origin,field.area)) {
                String filter=text(field.options,"target-filter","ALL");
                if (filter.equals("ENEMY") && !selectors.enemy(owner,target) || filter.equals("ALLY") && !selectors.ally(owner,target)) continue;
                List<String> effects=selectors.ally(owner,target) ? strings(field.options,"ally-effects")
                        : selectors.enemy(owner,target) ? strings(field.options,"enemy-effects") : List.of();
                UUID uuid=target.getUniqueId(); seen.add(uuid);
                List<String> old=field.inside.put(uuid,effects);
                if (old!=null && !old.equals(effects)) old.forEach(id -> buffs.release(uuid,lease(field,id)));
                if (old==null) {
                    EventContext event=new EventContext(TriggerEvent.ENTER_FIELD,null,target,owner); event.values.put("fieldId",field.key.id()); events.accept(event);
                }
                if (fields.get(field.key)!=field) break;
                if (mode.equals("WHILE_INSIDE")) effects.forEach(id -> buffs.acquire(target,lease(field,id),id,owner.getUniqueId()));
                else if (mode.equals("REFRESH_WHILE_INSIDE")) effects.forEach(id -> buffs.refresh(target,id,owner.getUniqueId()));
                else if (old==null) effects.forEach(id -> buffs.apply(target,id,owner.getUniqueId()));
            }
            for (UUID uuid : List.copyOf(field.inside.keySet())) if (!seen.contains(uuid)) leave(field,uuid,field.inside.remove(uuid),owner);
        }
    }
    public void forget(UUID owner) {
        for (Key key : List.copyOf(fields.keySet())) if (key.owner().equals(owner)) remove(key.owner(),key.source(),key.id());
    }
    public void reset() { for (Key key : List.copyOf(fields.keySet())) remove(key.owner(),key.source(),key.id()); }
}
```

## DynamicEffectService.java

```java
package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.StatKey;
import java.util.*;

public final class DynamicEffectService {
    private record Entry(StatKey stat,double value,long expires) {}
    private final Map<UUID,Map<String,Entry>> effects=new HashMap<>();
    public void apply(UUID target,String key,StatKey stat,double value,long duration,String reapply) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite dynamic modifier");
        var entries=effects.computeIfAbsent(target,ignored -> new LinkedHashMap<>());
        Entry old=entries.get(key); long now=System.currentTimeMillis();
        if (old!=null && old.expires()>now && reapply.equals("IGNORE")) return;
        long expires=duration==0 ? Long.MAX_VALUE : now+duration;
        if (old!=null && old.expires()>now && reapply.equals("EXTEND")) expires=old.expires()==Long.MAX_VALUE || duration==0 ? Long.MAX_VALUE : old.expires()+duration;
        entries.put(key,new Entry(stat,value,expires));
    }
    public Map<StatKey,Double> modifiers(UUID target) {
        var entries=effects.get(target); if (entries==null) return Map.of();
        entries.values().removeIf(e -> e.expires()<=System.currentTimeMillis());
        if (entries.isEmpty()) { effects.remove(target); return Map.of(); }
        Map<StatKey,Double> result=new EnumMap<>(StatKey.class);
        entries.values().forEach(e -> result.merge(e.stat(),e.value(),Double::sum));
        return result;
    }
    public void forget(UUID target) { effects.remove(target); }
    public void reset() { effects.clear(); }
}
```

## HealService.java

```java
package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.model.PlayerStats;
import com.github.saku0817.combatcoresystems.model.ReferenceStat;
import com.github.saku0817.combatcoresystems.model.StatKey;
import com.github.saku0817.combatcoresystems.util.CoreMath;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class HealService {
    private final PlayerDataService players;
    private final StatService stats;
    private final DamageDisplayService displays;
    private final LevelService levels;
    private final MobService mobs;
    private TriggerService triggers;
    public void bindTriggers(TriggerService triggers) { this.triggers=triggers; }

    public HealService(PlayerDataService players, StatService stats, DamageDisplayService displays, LevelService levels, MobService mobs) {
        this.players = players; this.stats = stats; this.displays = displays; this.levels = levels; this.mobs = mobs;
    }

    public long heal(LivingEntity source, LivingEntity target, ReferenceStat reference, double multiplier, boolean small) {
        double hp = value(source, ReferenceStat.HP), atk = value(source, ReferenceStat.ATK), def = value(source, ReferenceStat.DEF);
        double referenceValue = switch (reference) { case HP -> hp; case ATK -> atk; case DEF -> def; };
        return healAmount(source,target,referenceValue*multiplier,small);
    }

    public long healAmount(LivingEntity source,LivingEntity target,double baseAmount,boolean small) {
        if (target==null || target.isDead() || !Double.isFinite(baseAmount) || baseAmount<=0) return 0;
        if (source==null) source=target;
        double healingPower = source instanceof Player p ? stats.get(p, players.require(p)).value(StatKey.HEALING_POWER) : 0;
        long amount = Math.round(Math.max(0,baseAmount*(1+healingPower)));
        if (amount<=0) return 0;
        double current=target instanceof Player p ? players.require(p).getHealth() : mobs.health(target);
        double maximum=value(target,ReferenceStat.HP);
        double effective=Math.min(amount,Math.max(0,maximum-current));
        if (target instanceof Player player) {
            PlayerData data = players.require(player); levels.setVirtualHealth(player, data, data.getHealth() + amount);
        } else {
            mobs.setHealth(target, mobs.health(target) + amount);
        }
        displays.heal(source == null ? target.getUniqueId() : source.getUniqueId(), target, amount, small);
        if (triggers!=null) triggers.healed(source,target,amount,effective,amount-effective);
        return amount;
    }

    public double value(LivingEntity source, ReferenceStat stat) {
        if (source instanceof Player player) {
            PlayerStats value = stats.get(player, players.require(player));
            return switch (stat) { case HP -> value.maxHp(); case ATK -> value.atk(); case DEF -> value.def(); };
        }
        if (stat == ReferenceStat.HP) return mobs.maxHealth(source);
        if (stat == ReferenceStat.DEF) return mobs.definition(source).map(definition -> mobs.defense(source,definition,mobs.level(source))).orElse(0.0);
        Attribute attribute = switch (stat) { case HP -> Attribute.MAX_HEALTH; case ATK -> Attribute.ATTACK_DAMAGE; case DEF -> Attribute.ARMOR; };
        return source.getAttribute(attribute) == null ? 0 : source.getAttribute(attribute).getValue();
    }
}
```

## BuffService.java

```java
package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest;
import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.BuffDefinition;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.model.TimedEffect;
import com.github.saku0817.combatcoresystems.model.StatKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BuffService {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final DamageService damage;
    private final HealService healing;
    private final Map<String, Long> nextTicks = new ConcurrentHashMap<>();
    private final Map<UUID, List<TimedEffect>> entityEffects = new HashMap<>();
    private final Map<UUID, Map<String, TimedEffect>> ownedEffects = new HashMap<>();
    private long lastOrder;
    private long order() { return lastOrder=Math.max(System.currentTimeMillis()*1000,lastOrder+1); }
    private java.util.function.BiConsumer<LivingEntity, String> applied = (target,id) -> {};
    private java.util.function.BiConsumer<LivingEntity, String> removed = (target,id) -> {};
    public void bindEvents(java.util.function.BiConsumer<LivingEntity,String> applied, java.util.function.BiConsumer<LivingEntity,String> removed) {
        this.applied=applied; this.removed=removed;
    }
    public List<TimedEffect> effects(LivingEntity target) {
        List<TimedEffect> result=new ArrayList<>();
        if (target instanceof Player p) players.find(p.getUniqueId()).ifPresent(data -> { result.addAll(data.getBuffs()); result.addAll(data.getDebuffs()); });
        else result.addAll(entityEffects.getOrDefault(target.getUniqueId(),List.of()));
        result.addAll(ownedEffects.getOrDefault(target.getUniqueId(),Map.of()).values());
        result.sort(Comparator.comparingLong(TimedEffect::getAppliedOrder));
        return List.copyOf(result);
    }
    public boolean has(LivingEntity target,String id) { return effects(target).stream().anyMatch(e -> e.getId().equals(id) && (e.isPermanent() || e.getRemainingMillis()>0)); }
    public void acquire(LivingEntity target,String lease,String id,UUID source) {
        if (!definitions.snapshot().buffs().containsKey(id) || target.isDead()) return;
        var values=ownedEffects.computeIfAbsent(target.getUniqueId(),ignored -> new LinkedHashMap<>());
        TimedEffect effect=new TimedEffect(id,source,1,0,true); effect.setAppliedOrder(order());
        if (values.putIfAbsent(lease,effect)==null) {
            stats.invalidate(target.getUniqueId()); applied.accept(target,id);
        }
    }
    public void release(UUID target,String lease) {
        var values=ownedEffects.get(target); if (values==null) return;
        TimedEffect old=values.remove(lease);
        if (values.isEmpty()) ownedEffects.remove(target);
        if (old!=null) { stats.invalidate(target); if (Bukkit.getEntity(target) instanceof LivingEntity entity) removed.accept(entity,old.getId()); }
    }
    public boolean remove(LivingEntity target,String id) {
        if (target instanceof Player p) return remove(p,id);
        boolean changed=entityEffects.getOrDefault(target.getUniqueId(),new ArrayList<>()).removeIf(e -> e.getId().equals(id));
        if (changed) { stats.invalidate(target.getUniqueId()); removed.accept(target,id); }
        return changed;
    }
    public void refresh(LivingEntity target,String id,UUID source) {
        BuffDefinition definition=definitions.snapshot().buffs().get(id);
        if (definition==null || target.isDead()) return;
        List<TimedEffect> effects;
        if (target instanceof Player player) {
            PlayerData data=players.require(player);
            effects=definition.kind()==BuffDefinition.Kind.BUFF ? data.getBuffs() : data.getDebuffs();
        } else effects=entityEffects.getOrDefault(target.getUniqueId(),List.of());
        TimedEffect existing=effects.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
        if (existing==null) { apply(target,id,source); return; }
        existing.setRemainingMillis((long)(definition.durationSeconds()*1000)); existing.setAppliedOrder(order());
        stats.invalidate(target.getUniqueId()); applied.accept(target,id);
    }

    public BuffService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, StatService stats,
                       DamageService damage, HealService healing) {
        this.plugin = plugin; this.definitions = definitions; this.players = players; this.stats = stats;
        this.damage = damage; this.healing = healing;
    }

    public void start() { Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L); }

    public boolean apply(Player target, String id, UUID source) {
        return apply((LivingEntity) target, id, source);
    }

    public boolean apply(LivingEntity target, String id, UUID source) {
        BuffDefinition definition = definitions.snapshot().buffs().get(id);
        if (definition == null || target.isDead()) return false;
        List<TimedEffect> effects;
        if (target instanceof Player player) {
            PlayerData data = players.require(player);
            effects = definition.kind() == BuffDefinition.Kind.BUFF ? data.getBuffs() : data.getDebuffs();
        } else effects = entityEffects.computeIfAbsent(target.getUniqueId(), ignored -> new ArrayList<>());
        TimedEffect existing = effects.stream().filter(effect -> effect.getId().equals(id)).findFirst().orElse(null);
        long duration = (long) (definition.durationSeconds() * 1000);
        if (existing == null) { existing=new TimedEffect(id, source, 1, duration, definition.permanent()); effects.add(existing); }
        else switch (definition.reapply()) {
            case REFRESH -> existing.setRemainingMillis(duration);
            case STACK -> { existing.setStacks(Math.min(definition.maxStacks(), existing.getStacks() + 1)); existing.setRemainingMillis(duration); }
            case OVERWRITE -> { existing.setStacks(1); existing.setRemainingMillis(duration); }
            case CUSTOM -> { return false; }
        }
        existing.setAppliedOrder(order());
        stats.invalidate(target.getUniqueId());
        applied.accept(target,id);
        return true;
    }

    public double modifier(LivingEntity target, StatKey key) {
        List<TimedEffect> effects = effects(target);
        double result = 0;
        for (TimedEffect effect : effects) {
            BuffDefinition definition = definitions.snapshot().buffs().get(effect.getId());
            if (definition != null && (effect.isPermanent() || effect.getRemainingMillis() > 0))
                result += (definition.flatModifiers().getOrDefault(key, 0.0) + definition.percentModifiers().getOrDefault(key, 0.0)) * effect.getStacks();
        }
        return result;
    }

    public boolean remove(Player target, String id) {
        PlayerData data = players.require(target);
        boolean removed = data.getBuffs().removeIf(e -> e.getId().equals(id)) | data.getDebuffs().removeIf(e -> e.getId().equals(id));
        if (removed) { stats.invalidate(target.getUniqueId()); this.removed.accept(target,id); }
        return removed;
    }

    public void onDeath(Player player) {
        PlayerData data = players.require(player);
        data.getBuffs().removeIf(effect -> !effect.isPermanent());
        data.getDebuffs().removeIf(effect -> !effect.isPermanent());
        stats.invalidate(player.getUniqueId());
    }

    private void tick() {
        long elapsed = 250;
        for (var entry : List.copyOf(entityEffects.entrySet())) {
            Entity raw = Bukkit.getEntity(entry.getKey());
            if (!(raw instanceof LivingEntity living) || !living.isValid() || living.isDead()) {
                String prefix = entry.getKey() + ":"; nextTicks.keySet().removeIf(key -> key.startsWith(prefix));
                entityEffects.remove(entry.getKey()); continue;
            }
            updateList(living, entry.getValue(), elapsed);
            if (entry.getValue().isEmpty()) entityEffects.remove(entry.getKey());
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = players.find(player.getUniqueId()).orElse(null);
            if (data == null) continue;
            boolean changed = updateList(player, data.getBuffs(), elapsed);
            changed |= updateList(player, data.getDebuffs(), elapsed);
            if (changed) stats.invalidate(player.getUniqueId());
        }
        for (var entry : List.copyOf(ownedEffects.entrySet())) {
            if (!(Bukkit.getEntity(entry.getKey()) instanceof LivingEntity target) || target.isDead()) continue;
            for (TimedEffect effect : List.copyOf(entry.getValue().values())) {
                BuffDefinition definition=definitions.snapshot().buffs().get(effect.getId());
                if (definition!=null && definition.tickEffect()!=null) runTickEffect(target,effect,definition);
            }
        }
    }

    private boolean updateList(LivingEntity target, List<TimedEffect> effects, long elapsed) {
        boolean changed = false;
        for (TimedEffect effect : List.copyOf(effects)) {
            if (!effects.contains(effect)) continue;
            BuffDefinition definition = definitions.snapshot().buffs().get(effect.getId());
            if (definition == null) { nextTicks.remove(target.getUniqueId() + ":" + effect.getId()); effects.remove(effect); changed = true; removed.accept(target,effect.getId()); continue; }
            if (!effect.isPermanent()) {
                effect.setRemainingMillis(Math.max(0, effect.getRemainingMillis() - elapsed));
                if (effect.getRemainingMillis() == 0) { nextTicks.remove(target.getUniqueId() + ":" + effect.getId()); effects.remove(effect); changed = true; removed.accept(target,effect.getId()); continue; }
            }
            if (definition.tickEffect() != null) runTickEffect(target, effect, definition);
        }
        return changed;
    }

    private void runTickEffect(LivingEntity target, TimedEffect effect, BuffDefinition definition) {
        String key = target.getUniqueId() + ":" + effect.getId();
        long now = System.currentTimeMillis();
        if (nextTicks.getOrDefault(key, 0L) > now) return;
        BuffDefinition.TickEffect tick = definition.tickEffect();
        nextTicks.put(key, now + (long) (tick.intervalSeconds() * 1000));
        LivingEntity source = target;
        try {
            Entity resolved = effect.getSource().isBlank() ? null : Bukkit.getEntity(UUID.fromString(effect.getSource()));
            if (resolved instanceof LivingEntity living) source = living;
        } catch (IllegalArgumentException ignored) {}
        if (tick.healing()) healing.heal(source, target, tick.referenceStat(), tick.multiplier() * effect.getStacks(), true);
        else damage.apply(new DamageRequest(source.getUniqueId(), target.getUniqueId(), tick.referenceStat(),
                tick.multiplier() * effect.getStacks(), tick.element(), tick.critical(), tick.fixed(),
                tick.fixed() ? tick.multiplier() * effect.getStacks() : 0, "effect:" + effect.getId()));
    }
}
```

## DamageService.java

```java
package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.damage.DamageApi;
import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest;
import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageResult;
import com.github.saku0817.combatcoresystems.api.v1.event.AfterDamageEvent;
import com.github.saku0817.combatcoresystems.api.v1.event.BeforeDamageEvent;
import com.github.saku0817.combatcoresystems.api.v1.event.ElementReactionEvent;
import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.util.CoreMath;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class DamageService implements DamageApi, Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final CombatStateService combat;
    private final ElementService elements;
    private final MobService mobs;
    private final PartyService parties;
    private final DamageDisplayService displays;
    private final RegionService regions;
    private final LevelService levels;
    private final ItemService items;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey projectileWeaponKey;
    private final NamespacedKey projectileStageKey;
    private final Map<UUID, AttackCharge> attackCharges = new HashMap<>();
    private final Map<String, Long> heartReactionCooldowns = new HashMap<>();
    private long nextHeartCooldownCleanup;
    private BuffService buffs;
    private TriggerService triggers;
    public void bindTriggers(TriggerService triggers) { this.triggers=triggers; }
    public void bindBuffs(BuffService buffs) { this.buffs = buffs; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBeforeAttack(io.papermc.paper.event.player.PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        attackCharges.put(player.getUniqueId(), new AttackCharge(Bukkit.getCurrentTick(), player.getAttackCooldown()));
    }

    @EventHandler public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) { attackCharges.remove(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBow(org.bukkit.event.entity.EntityShootBowEvent event) {
        event.getProjectile().getPersistentDataContainer().set(new NamespacedKey(plugin, "shot_force"), PersistentDataType.DOUBLE, (double) event.getForce());
        var data=event.getProjectile().getPersistentDataContainer();
        data.remove(projectileWeaponKey); data.remove(projectileStageKey);
        ItemInstance instance=items.instance(event.getBow()).orElse(null);
        if (instance!=null) {
            data.set(projectileWeaponKey,PersistentDataType.STRING,instance.getDefinitionId());
            data.set(projectileStageKey,PersistentDataType.INTEGER,instance.getLimitBreak());
        }
    }

    private record AttackCharge(int tick, double value) {}

    public DamageService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, StatService stats,
                         CombatStateService combat, ElementService elements, MobService mobs, PartyService parties,
                         DamageDisplayService displays, RegionService regions, LevelService levels, ItemService items) {
        this.items = items;
        this.plugin = plugin;
        this.definitions = definitions;
        this.players = players;
        this.stats = stats;
        this.combat = combat;
        this.elements = elements;
        this.mobs = mobs;
        this.parties = parties;
        this.displays = displays;
        this.regions = regions;
        this.levels = levels;
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.projectileWeaponKey = new NamespacedKey(plugin, "projectile_weapon");
        this.projectileStageKey = new NamespacedKey(plugin, "projectile_weapon_stage");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        if (event.getEntity().getPersistentDataContainer().has(new NamespacedKey(plugin,"shot_force"))) return;
        String weapon = player.getInventory().getItemInMainHand().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (weapon != null) event.getEntity().getPersistentDataContainer().set(projectileWeaponKey, PersistentDataType.STRING, weapon);
        ItemInstance instance = items.instance(player.getInventory().getItemInMainHand()).orElse(null);
        if (instance != null) event.getEntity().getPersistentDataContainer().set(projectileStageKey, PersistentDataType.INTEGER, instance.getLimitBreak());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        LivingEntity attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.equals(target)) { if (attacker != null) event.setCancelled(true); return; }
        if (!allowed(attacker, target)) { event.setCancelled(true); return; }

        String weaponId = weaponId(event.getDamager(), attacker);
        combat.touch(attacker, weaponId);
        combat.touch(target, "");
        if (attacker instanceof Player player) stats.invalidate(player.getUniqueId());

        double multiplier = 1.0;
        if (attacker instanceof Player player && !(event.getDamager() instanceof Projectile)) {
            AttackCharge charge = attackCharges.get(player.getUniqueId());
            double cooled = charge != null && charge.tick() == Bukkit.getCurrentTick() ? charge.value() : player.getAttackCooldown();
            multiplier = attackMultiplier(cooled);
        } else if (event.getDamager() instanceof Projectile projectile) {
            multiplier = bowAttackMultiplier(projectile.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "shot_force"), PersistentDataType.DOUBLE, 1.0));
        }
        Element element = mobs.definition(attacker).map(MobDefinition::nativeElement).orElse(Element.PHYSICAL);
        if (attacker instanceof Player player) {
            WeaponDefinition weapon = attackWeapon(player, event.getDamager());
            if (weapon != null && weapon.canEquip(players.require(player).getLevel())) element = weapon.options().normalElement();
            PlayerData data = players.require(player); ItemInstance heart = data.getEquipment().get(EquipmentSlot.DIVINE_HEART);
            if (heart != null) {
                var heartConfig = definitions.snapshot().config("divine_hearts.yml");
                String path = "divine-hearts." + heart.getDefinitionId() + ".rules.";
                if (weapon == null || weapon.options().normalElement() == Element.PHYSICAL)
                    element = Element.parse(heartConfig.contains(path + "normal-attack-attribute")
                            ? heartConfig.getString(path + "normal-attack-attribute") : heartConfig.getString(path + "normal-attack-element")).orElse(element);
            }
        }
        DamageRequest request = new DamageRequest(attacker.getUniqueId(), target.getUniqueId(), ReferenceStat.ATK,
                multiplier, element, true, false, 0, "normal_attack");
        event.setCancelled(true);
        DamageResult result = apply(request);
        if (!result.applied()) return;
        WeaponDefinition activeWeapon = attacker instanceof Player owner ? attackWeapon(owner, event.getDamager()) : null;
        if (attacker instanceof Player player && activeWeapon != null && activeWeapon.canEquip(players.require(player).getLevel()))
            WeaponVisuals.play(target, activeWeapon.options().visual());
        event.setCancelled(true);
        if (result.finalDamage() > 0) applyStandardKnockback(attacker, target);
    }

    public boolean canAffect(LivingEntity attacker, LivingEntity target) { return !target.isDead() && allowed(attacker, target); }

    private WeaponDefinition attackWeapon(Player player, Entity damager) {
        if (damager instanceof Projectile projectile) {
            String id = projectile.getPersistentDataContainer().get(projectileWeaponKey, PersistentDataType.STRING);
            if (id == null) return null;
            int stage = projectile.getPersistentDataContainer().getOrDefault(projectileStageKey, PersistentDataType.INTEGER, 0);
            var variants = definitions.snapshot().weaponStages().get(id);
            return variants == null ? definitions.snapshot().weapons().get(id) : variants.get(Math.clamp(stage, 0, 5));
        }
        return definitions.snapshot().weapon(items.instance(player.getInventory().getItemInMainHand()).orElse(null));
    }

    private boolean allowed(LivingEntity attacker, LivingEntity target) {
        if (attacker instanceof Player first && target instanceof Player second) {
            boolean global = definitions.snapshot().config("config.yml").getBoolean("pvp-enabled", true);
            String regional = regions.flag(target.getLocation(), "pvp").orElse("");
            boolean areaAllows = regional.equalsIgnoreCase("allow") || (regional.isBlank() && global);
            if (!areaAllows) return false;
            PlayerData firstData = players.find(first.getUniqueId()).orElse(null);
            PlayerData secondData = players.find(second.getUniqueId()).orElse(null);
            if (firstData == null || secondData == null || !firstData.isPvpEnabled() || !secondData.isPvpEnabled()) return false;
            return !parties.sameParty(first.getUniqueId(), second.getUniqueId());
        }
        return true;
    }

    static double attackMultiplier(double cooled) {
        double clamped = Math.max(0, Math.min(1, cooled));
        return 0.2 + clamped * clamped * 0.8;
    }

    static double bowAttackMultiplier(double force) {
        // Vanilla force=(draw^2+2*draw)/3. Recover draw time, never projectile velocity.
        double draw = Math.sqrt(1 + 3 * Math.clamp(Double.isFinite(force) ? force : 0, 0, 1)) - 1;
        return attackMultiplier(draw);
    }

    private LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof LivingEntity living) return living;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) return living;
        return null;
    }

    private String weaponId(Entity damageEntity, LivingEntity attacker) {
        if (damageEntity instanceof Projectile projectile) {
            String tagged = projectile.getPersistentDataContainer().get(projectileWeaponKey, PersistentDataType.STRING);
            if (tagged != null) return tagged;
        }
        if (attacker instanceof Player player) {
            String id = player.getInventory().getItemInMainHand().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
            return id == null ? "" : id;
        }
        return "";
    }

    @Override public DamageResult apply(DamageRequest request) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Damage API must be called from the server thread");
        return triggers==null ? applyInternal(request) : triggers.damage(request,() -> applyInternal(request));
    }

    private DamageResult applyInternal(DamageRequest request) {
        Entity rawTarget = Bukkit.getEntity(request.target());
        if (!(rawTarget instanceof LivingEntity target) || target.isDead()) return DamageResult.failed(request.source(), "target_not_available");
        LivingEntity attacker = null;
        if (request.attacker() != null) {
            Entity rawAttacker = Bukkit.getEntity(request.attacker());
            if (!(rawAttacker instanceof LivingEntity living)) return DamageResult.failed(request.source(), "attacker_not_available");
            attacker = living;
        }
        BeforeDamageEvent before = new BeforeDamageEvent(request);
        Bukkit.getPluginManager().callEvent(before);
        if (before.isCancelled()) return DamageResult.failed(request.source(), "cancelled");
        if (attacker!=null && !allowed(attacker,target)) return DamageResult.failed(request.source(),"not_allowed");
        if (triggers!=null) triggers.beforeHit();
        if (target.isDead()) return DamageResult.failed(request.source(),"target_not_available");

        DamageResult result = calculate(request, attacker, target);
        if (!result.applied()) return result;
        if (attacker != null && !allowed(attacker, target)) return DamageResult.failed(request.source(), "not_allowed");
        if (entersCombat(result.finalDamage(), attacker == null ? null : attacker.getUniqueId(), target.getUniqueId())) {
            combat.touch(attacker, weaponId(attacker, attacker));
            combat.touch(target, "");
        }
        if (result.finalDamage() > 0 && attacker instanceof Player player) target.setKiller(player);
        long overdamage = overdamage(target, result.finalDamage());
        subtractHealth(target, result.finalDamage());
        UUID owner = attacker == null ? target.getUniqueId() : attacker.getUniqueId();
        displays.damage(owner, target, result.finalDamage(), result.critical(), null, false, overdamage, request.element(), null);
        Bukkit.getPluginManager().callEvent(new AfterDamageEvent(request, result));

        if (!request.fixedDamage() && request.element() != Element.PHYSICAL && !mobs.immune(target, request.element()) && attacker != null && !target.isDead()) {
            var config = definitions.snapshot().config("config.yml");
            double duration = config.contains("attribute-attachment-seconds")
                    ? config.getDouble("attribute-attachment-seconds", 5)
                    : config.getDouble("element-attachment-seconds", 5);
            LivingEntity reactionAttacker = attacker;
            elements.attach(target, request.element(), duration).ifPresent(trigger -> applyReaction(reactionAttacker, target, request.referenceStat(), trigger));
        }
        return result;
    }

    private DamageResult calculate(DamageRequest request, LivingEntity attacker, LivingEntity target) {
        if (request.fixedDamage()) {
            return new DamageResult(true, CoreMath.roundedDamage(request.fixedAmount()), false, request.element(),
                    request.fixedAmount(), 1, 0, request.source(), "");
        }
        if (attacker == null) return DamageResult.failed(request.source(), "attacker_required");
        CombatantStats source = combatant(attacker);
        CombatantStats defender = combatant(target);
        if (triggers!=null && triggers.current()!=null) {
            source=modified(source,triggers.current().sourceModifiers);
            defender=modified(defender,triggers.current().targetModifiers);
        }
        double reference = switch (request.referenceStat()) { case HP -> source.hp; case ATK -> source.atk; case DEF -> source.def; };
        double base = Math.max(0, reference * request.multiplier());
        if (!request.components().isEmpty()) {
            base=0;
            for (var component : request.components()) base+=request.multiplier()*componentAmount(component,source.hp,source.atk,source.def,source.elementDamage(component.bonusElement()));
        }
        double outgoingIgnored = source.details instanceof PlayerStats value ? value.value(StatKey.DEF_IGNORE)
                : buffs == null ? 0 : buffs.modifier(attacker, StatKey.DEF_IGNORE);
        double ignored = outgoingIgnored + (defender.details instanceof PlayerStats value ? value.value(StatKey.DEF_IGNORED_WHEN_HIT)
                : buffs == null ? 0 : buffs.modifier(target, StatKey.DEF_IGNORED_WHEN_HIT));
        double effectiveDef = CoreMath.effectiveDefense(defender.def, defender.defDown) * (1 - Math.clamp(ignored, 0, 1));
        double defenseCoefficient = CoreMath.defenseCoefficient(source.level, defender.level, effectiveDef);
        double damage = base * defenseCoefficient;
        damage *= Math.max(0, definitions.snapshot().config("config.yml").getDouble("damage.global-multiplier", 2.0));
        double resistance = 0;
        if (request.element() != Element.PHYSICAL) {
            if (mobs.immune(target, request.element())) damage = 0;
            else {
                resistance = CoreMath.finalResistance(defender.resistance(request.element()),
                        defender.resistanceDown(request.element()) + elements.resistanceDown(target.getUniqueId(), request.element()));
                damage *= (request.components().isEmpty() ? 1 + source.elementDamage(request.element()) : 1) * (1 - resistance);
            }
        }
        boolean critical = request.canCritical() && ThreadLocalRandom.current().nextDouble() < Math.min(1, source.critRate);
        if (critical) damage *= 1 + source.critDamage;
        return new DamageResult(true, CoreMath.roundedDamage(damage), critical, request.element(), base,
                defenseCoefficient, resistance, request.source(), "");
    }

    static boolean entersCombat(long damage, UUID attacker, UUID target) { return damage > 0 && attacker != null && !attacker.equals(target); }

    private CombatantStats modified(CombatantStats original,com.github.saku0817.combatcoresystems.model.trigger.EventModifier modifier) {
        Map<StatKey,Double> values=new EnumMap<>(StatKey.class);
        for (StatKey key : StatKey.values()) {
            double base=original.details instanceof PlayerStats p ? p.value(key) : 0;
            values.put(key,modifier.advanced(key,base));
        }
        for (Element element : Element.values()) if (element!=Element.PHYSICAL) {
            StatKey resistance=StatKey.valueOf(element.name()+"_RESISTANCE");
            values.put(resistance,modifier.advanced(resistance,original.resistance(element)));
        }
        double hp=modifier.primary(StatKey.HP_FLAT,StatKey.HP_PERCENT,original.hp);
        double atk=modifier.primary(StatKey.ATK_FLAT,StatKey.ATK_PERCENT,original.atk);
        double def=modifier.primary(StatKey.DEF_FLAT,StatKey.DEF_PERCENT,original.def);
        return new CombatantStats(original.level,hp,atk,def,modifier.advanced(StatKey.CRIT_RATE,original.critRate),
                modifier.advanced(StatKey.CRIT_DAMAGE,original.critDamage),modifier.advanced(StatKey.DEF_DOWN,original.defDown),
                new PlayerStats(original.level,hp,atk,def,values));
    }

    static double componentAmount(WeaponOptions.Component component, double hp, double atk, double def, double bonus) {
        double reference = switch (component.reference()) { case HP -> hp; case ATK -> atk; case DEF -> def; };
        return Math.max(0, reference * component.multiplier() * (component.bonusElement() == Element.PHYSICAL ? 1 : Math.max(0, 1 + bonus)));
    }

    private void applyReaction(LivingEntity attacker, LivingEntity central, ReferenceStat referenceStat, ElementService.ReactionTrigger trigger) {
        referenceStat = divineReactionReference(attacker, trigger, referenceStat);
        trigger = divineReaction(attacker, central, trigger);
        if (trigger == null) return;
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(central);
        if (trigger.definition().radius() > 0) {
            central.getWorld().getNearbyLivingEntities(central.getLocation(), trigger.definition().radius(),
                    entity -> !entity.equals(attacker) && !entity.equals(central)).forEach(targets::add);
        }
        CombatantStats source = combatant(attacker);
        double reference = switch (referenceStat) { case HP -> source.hp; case ATK -> source.atk; case DEF -> source.def; };
        for (LivingEntity target : targets) {
            if (target.isDead() || !allowed(attacker, target)) continue;
            CombatantStats defender = combatant(target);
            double total = 0;
            for (Map.Entry<Element, Double> component : trigger.definition().components().entrySet()) {
                if (mobs.immune(target, component.getKey())) continue;
                double resistance = CoreMath.finalResistance(defender.resistance(component.getKey()),
                        defender.resistanceDown(component.getKey()) + elements.resistanceDown(target.getUniqueId(), component.getKey()));
                int hits = component.getKey() == trigger.definition().multiHitElement() ? trigger.definition().hits() : 1;
                total += reference * component.getValue() * (1 + source.elementDamage(component.getKey())) * (1 - resistance) * hits;
            }
            boolean critical = ThreadLocalRandom.current().nextDouble() < Math.min(1, source.critRate);
            if (critical) total *= 1 + source.critDamage;
            total *= Math.max(0, definitions.snapshot().config("config.yml").getDouble("damage.global-multiplier", 2.0));
            long rounded = CoreMath.roundedDamage(total);
            if (!allowed(attacker, target)) continue;
            if (rounded > 0 && attacker instanceof Player player) target.setKiller(player);
            long overdamage = overdamage(target, rounded);
            subtractHealth(target, rounded);
            displays.damage(attacker.getUniqueId(), target, rounded, critical, trigger.definition().name(), false, overdamage, trigger.incoming(), null);
            if (entersCombat(rounded, attacker.getUniqueId(), target.getUniqueId())) {
                combat.touch(attacker, weaponId(attacker, attacker)); combat.touch(target, "");
            }
            Bukkit.getPluginManager().callEvent(new AfterDamageEvent(
                    new DamageRequest(attacker.getUniqueId(), target.getUniqueId(), referenceStat, 1, trigger.incoming(), false, true, rounded, "reaction:" + trigger.definition().id()),
                    new DamageResult(true, rounded, critical, trigger.incoming(), total, 1, 0, "reaction:" + trigger.definition().id(), "")));
            if (trigger.definition().levitation() > 0) target.setVelocity(target.getVelocity().setY(trigger.definition().levitation()));
            if (trigger.definition().resistanceDownElement() != null) elements.applyResistanceDown(target.getUniqueId(),
                    trigger.definition().resistanceDownElement(), trigger.definition().resistanceDown(), trigger.definition().resistanceDownSeconds());
            Bukkit.getPluginManager().callEvent(new ElementReactionEvent(target.getUniqueId(), trigger.definition().id(),
                    trigger.existing(), trigger.incoming(), rounded));
        }
    }

    private ReferenceStat divineReactionReference(LivingEntity attacker, ElementService.ReactionTrigger trigger, ReferenceStat fallback) {
        if (!(attacker instanceof Player player)) return fallback;
        ItemInstance heart = players.require(player).getEquipment().get(EquipmentSlot.DIVINE_HEART);
        if (heart == null) return fallback;
        var config = definitions.snapshot().config("divine_hearts.yml");
        String root = "divine-hearts." + heart.getDefinitionId() + ".rules.reaction-override";
        Element source = Element.parse(config.getString(root + ".source-attribute")).orElse(null);
        if (source != trigger.incoming()) return fallback;
        try { return ReferenceStat.valueOf(config.getString(root + ".damage.reference", fallback.name()).toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return fallback; }
    }

    private ElementService.ReactionTrigger divineReaction(LivingEntity attacker, LivingEntity target, ElementService.ReactionTrigger original) {
        if (!(attacker instanceof Player player)) return original;
        ItemInstance heart = players.require(player).getEquipment().get(EquipmentSlot.DIVINE_HEART);
        if (heart == null) return original;
        var config = definitions.snapshot().config("divine_hearts.yml");
        String root = "divine-hearts." + heart.getDefinitionId() + ".rules.reaction-override";
        if (!config.isConfigurationSection(root)) return original;
        Element source = Element.parse(config.getString(root + ".source-attribute")).orElse(null);
        if (source == null || original.incoming() != source) return original;
        String id = config.getString(root + ".id", heart.getDefinitionId() + "_reaction");
        long now = System.currentTimeMillis();
        String cooldownKey = target.getUniqueId() + ":" + id;
        if (now >= nextHeartCooldownCleanup) {
            heartReactionCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
            nextHeartCooldownCleanup = now + 1000;
        }
        if (heartReactionCooldowns.getOrDefault(cooldownKey, 0L) > now) return null;
        double cooldown = Math.max(0, config.getDouble(root + ".cooldown-seconds", 0));
        heartReactionCooldowns.put(cooldownKey, now + (long) (cooldown * 1000));
        Element damageElement = Element.parse(config.getString(root + ".damage.attribute", source.name())).orElse(source);
        Element downElement = Element.parse(config.getString(root + ".resistance-down.attribute", damageElement.name())).orElse(damageElement);
        ReactionDefinition replacement = new ReactionDefinition(id, config.getString(root + ".name", id), original.existing(), original.incoming(),
                Math.max(0, config.getDouble(root + ".radius", 0)), cooldown, 1, null, 0,
                Map.of(damageElement, Math.max(0, config.getDouble(root + ".damage.multiplier", 1))), downElement,
                Math.max(0, config.getDouble(root + ".resistance-down.amount", 0)), Math.max(0, config.getDouble(root + ".resistance-down.duration-seconds", 0)));
        return new ElementService.ReactionTrigger(replacement, original.existing(), original.incoming());
    }

    private CombatantStats combatant(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerData data = players.require(player);
            PlayerStats value = stats.get(player, data);
            return new CombatantStats(value.level(), value.maxHp(), value.atk(), value.def(), value.value(StatKey.CRIT_RATE),
                    value.value(StatKey.CRIT_DAMAGE), value.value(StatKey.DEF_DOWN), value);
        }
        MobDefinition definition = mobs.definition(entity).orElse(null);
        int level = definition == null ? 1 : mobs.level(entity);
        double hp = mobs.maxHealth(entity);
        double atk = attribute(entity, Attribute.ATTACK_DAMAGE, 2);
        double def = definition == null ? 0 : mobs.defense(entity, definition, level);
        if (buffs != null) {
            atk = CoreMath.attack(atk, 0, buffs.modifier(entity, StatKey.ATK_PERCENT), buffs.modifier(entity, StatKey.ATK_FLAT));
            def = Math.max(0, def * (1 + buffs.modifier(entity, StatKey.DEF_PERCENT)) + buffs.modifier(entity, StatKey.DEF_FLAT));
        }
        if (triggers==null) return new CombatantStats(level,hp,atk,def,0.05,0.5,buffs==null ? 0 : buffs.modifier(entity,StatKey.DEF_DOWN),definition);
        Map<StatKey,Double> modifiers=new EnumMap<>(StatKey.class);
        for (StatKey key : StatKey.values()) modifiers.put(key,buffs==null ? 0 : buffs.modifier(entity,key));
        Map<StatKey,Double> dynamic=triggers.dynamic().modifiers(entity.getUniqueId());
        dynamic.forEach((key,value) -> modifiers.merge(key,value,Double::sum));
        atk=atk*(1+dynamic.getOrDefault(StatKey.ATK_PERCENT,0.0))+dynamic.getOrDefault(StatKey.ATK_FLAT,0.0);
        def=def*(1+dynamic.getOrDefault(StatKey.DEF_PERCENT,0.0))+dynamic.getOrDefault(StatKey.DEF_FLAT,0.0);
        hp=hp*(1+dynamic.getOrDefault(StatKey.HP_PERCENT,0.0))+dynamic.getOrDefault(StatKey.HP_FLAT,0.0);
        modifiers.merge(StatKey.CRIT_RATE,.05,Double::sum); modifiers.merge(StatKey.CRIT_DAMAGE,.5,Double::sum);
        if (definition!=null) definition.resistances().forEach((element,value) -> {
            if (element!=Element.PHYSICAL) modifiers.merge(StatKey.valueOf(element.name()+"_RESISTANCE"),value,Double::sum);
        });
        if (buffs!=null) for (TimedEffect effect : buffs.effects(entity)) {
            var overrides=definitions.snapshot().config("buffs.yml").getConfigurationSection("buffs."+effect.getId()+".modifiers.override");
            if (overrides!=null) for (String key : overrides.getKeys(false)) {
                double value=overrides.getDouble(key);
                switch(StatKey.valueOf(key)) { case HP_FLAT -> hp=value; case ATK_FLAT -> atk=value; case DEF_FLAT -> def=value; default -> modifiers.put(StatKey.valueOf(key),value); }
            }
        }
        PlayerStats details=new PlayerStats(level,hp,atk,def,modifiers);
        return new CombatantStats(level,hp,atk,def,details.value(StatKey.CRIT_RATE),details.value(StatKey.CRIT_DAMAGE),details.value(StatKey.DEF_DOWN),details);
    }

    private double attribute(LivingEntity entity, Attribute attribute, double fallback) {
        var instance = entity.getAttribute(attribute);
        return instance == null ? fallback : instance.getValue();
    }

    private void subtractHealth(LivingEntity target, long damage) {
        if (damage <= 0 || target.isDead()) return;
        target.setLastDamage(damage);
        target.setNoDamageTicks(target.getMaximumNoDamageTicks());
        if (target instanceof Player player) {
            PlayerData data = players.require(player);
            levels.setVirtualHealth(player, data, data.getHealth() - damage);
        } else mobs.setHealth(target, mobs.health(target) - damage);
    }

    private long overdamage(LivingEntity target, long damage) {
        if (!definitions.snapshot().config("config.yml").getBoolean("text-display.show-overdamage", true)) return 0;
        double current = target instanceof Player player ? players.require(player).getHealth() : mobs.health(target);
        return CoreMath.overdamage(damage, current);
    }

    private void applyStandardKnockback(LivingEntity attacker, LivingEntity target) {
        Vector direction = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).setY(0);
        if (direction.lengthSquared() == 0) return;
        target.setVelocity(target.getVelocity().multiply(0.5).add(direction.normalize().multiply(0.4)).setY(0.2));
    }

    private record CombatantStats(int level, double hp, double atk, double def, double critRate, double critDamage,
                                  double defDown, Object details) {
        double elementDamage(Element element) {
            if (details instanceof PlayerStats player) return player.elementDamage(element);
            return 0;
        }
        double resistance(Element element) {
            if (details instanceof PlayerStats player) return player.resistance(element);
            if (details instanceof MobDefinition mob) return mob.resistances().getOrDefault(element, 0.0);
            return 0;
        }
        double resistanceDown(Element element) { return details instanceof PlayerStats player ? player.resistanceDown(element) : 0; }
    }
}
```

## SkillService.java

```java
package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest;
import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.util.CoreMath;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillService implements Listener {
    private SetEffectService setEffects;
    private TriggerService triggers;
    public void bindTriggers(TriggerService value) { triggers=value; }
    public void bindSetEffects(SetEffectService value) { setEffects = value; }
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final CombatStateService combat;
    private final DamageService damage;
    private final ItemService items;
    private final JavaPlugin plugin;
    private BuffService buffs;
    private LevelService levels;
    private DebugService debug;
    public void bindDebug(DebugService debug) { this.debug = debug; }
    private final Map<UUID, Integer> inventoryDrops = new HashMap<>();
    private final Map<UUID, Map<String, AbilityState>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Boolean, Integer>> inputTicks = new HashMap<>();
    private final MiniMessage mini = MiniMessage.miniMessage();

    public SkillService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, StatService stats,
                        CombatStateService combat, DamageService damage, ItemService items) {
        this.definitions = definitions; this.players = players; this.stats = stats; this.combat = combat; this.damage = damage; this.items = items;
        this.plugin = plugin;
    }

    public void bindEffects(BuffService buffs, LevelService levels) { this.buffs = buffs; this.levels = levels; }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSneakAttack(io.papermc.paper.event.player.PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking() || !(event.getAttacked() instanceof LivingEntity target)) return;
        if (input(player, target, true)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSneakUse(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (!event.getPlayer().isSneaking() || (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)) return;
        LivingEntity target = rayTarget(event.getPlayer());
        if (input(event.getPlayer(), target, true)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrop(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getAction().name().startsWith("DROP_")) {
            if (event.getWhoClicked() instanceof Player player && isBedrock(player)
                    && definitions.snapshot().config("config.yml").getBoolean("controls.drop-skill", true)
                    && definitions.snapshot().config("config.yml").getBoolean("controls.bedrock-selected-slot-drop-skill", true)
                    && !event.isCancelled()
                    && event.getClickedInventory() == player.getInventory() && event.getSlot() == player.getInventory().getHeldItemSlot()) {
                WeaponDefinition weapon = definitions.snapshot().weapon(items.instance(event.getCurrentItem()).orElse(null));
                if (weapon != null && weapon.skill() != null && player.hasPermission("combatcoresystems.command.skill")) {
                    event.setCancelled(true);
                    int selected = player.getInventory().getHeldItemSlot();
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline() && player.getInventory().getHeldItemSlot() == selected
                                && items.id(player.getInventory().getItemInMainHand()).orElse("").equals(weapon.id())) activate(player, false);
                    });
                    trace("bedrock selected-slot fallback", player, "action=" + event.getAction() + " slot=" + event.getSlot());
                    return;
                }
            }
            markInventoryDrop(event.getWhoClicked().getUniqueId());
            if (event.getWhoClicked() instanceof Player player) trace("inventory drop", player, "action=" + event.getAction() + " slot=" + event.getSlot());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void traceInventory(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !inputDebugEnabled(player)) return;
        trace("inventory click", player, "click=" + event.getClick() + " action=" + event.getAction() + " slot=" + event.getSlot() + " rawSlot=" + event.getRawSlot() + " cancelled=" + event.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void traceDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!inputDebugEnabled(player)) return;
        trace("drop", player, "cancelled=" + event.isCancelled()
                + " inventoryDrop=" + inventoryDrops.containsKey(player.getUniqueId()) + " view=" + player.getOpenInventory().getType()
                + " permission=" + player.hasPermission("combatcoresystems.command.skill")
                + " weapon=" + items.id(event.getItemDrop().getItemStack()).orElse("none"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void traceInteraction(PlayerInteractEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND && inputDebugEnabled(event.getPlayer()))
            trace("interact", event.getPlayer(), "action=" + event.getAction() + " cancelled=" + event.isCancelled());
    }

    private void trace(String kind, Player player, String details) {
        if (debug != null && debug.enabled(player.getUniqueId())) debug.log(player.getUniqueId(), "skill-input", player.getName() + " " + kind + " " + details);
        else if (definitions.snapshot().config("config.yml").getBoolean("controls.debug-inputs", false))
            plugin.getLogger().info("[skill-input] " + player.getName() + " " + kind + " " + details);
    }

    private boolean inputDebugEnabled(Player player) {
        return definitions.snapshot().config("config.yml").getBoolean("controls.debug-inputs", false) || debug != null && debug.enabled(player.getUniqueId());
    }

    private boolean isBedrock(Player player) {
        if (org.bukkit.Bukkit.getPluginManager().getPlugin("floodgate") == null) return false;
        try {
            Class<?> type = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = type.getMethod("getInstance").invoke(null);
            return Boolean.TRUE.equals(type.getMethod("isFloodgatePlayer", UUID.class).invoke(api, player.getUniqueId()));
        } catch (ReflectiveOperationException ex) { return false; }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        // Only a real cursor item can become a close-time inventory drop.
        // Geyser may close a view immediately before a normal hand-drop packet.
        var cursor = event.getPlayer().getItemOnCursor();
        if (!cursor.getType().isAir()) markInventoryDrop(event.getPlayer().getUniqueId());
    }

    private void markInventoryDrop(UUID id) {
        int tick = org.bukkit.Bukkit.getCurrentTick();
        inventoryDrops.put(id, tick);
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> inventoryDrops.remove(id, tick));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHandDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!definitions.snapshot().config("config.yml").getBoolean("controls.drop-skill", true)
                || inventoryDrops.containsKey(player.getUniqueId())
                || !isHandDropView(player.getOpenInventory().getType().name())
                || !player.hasPermission("combatcoresystems.command.skill")) {
            trace("drop skipped", player, "view=" + player.getOpenInventory().getType() + " inventoryDrop=" + inventoryDrops.containsKey(player.getUniqueId())
                    + " enabled=" + definitions.snapshot().config("config.yml").getBoolean("controls.drop-skill", true)
                    + " permission=" + player.hasPermission("combatcoresystems.command.skill"));
            return;
        }
        ItemInstance dropped = items.instance(event.getItemDrop().getItemStack()).orElse(null);
        WeaponDefinition weapon = dropped == null ? null : definitions.snapshot().weapon(dropped);
        if (weapon == null || weapon.skill() == null) return;
        int slot = player.getInventory().getHeldItemSlot();
        event.setCancelled(true);
        // Cancellation restores the removed item after dispatch; never cast using an empty hand.
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (player.getInventory().getHeldItemSlot() != slot) {
                fail(player, "hand-changed", "ドロップ操作中に持ち替えたため、スキルを中止しました。");
                return;
            }
            ItemInstance restored = items.instance(player.getInventory().getItemInMainHand()).orElse(null);
            if (restored != null && restored.getInstanceId().equals(dropped.getInstanceId())) activate(player, false);
            else fail(player, "hand-restore", "武器の手持ち復元を確認できませんでした。持ち直してから再試行してください。");
        });
    }

    static boolean isHandDropView(String type) {
        return "CRAFTING".equals(type) || "CREATIVE".equals(type);
    }

    private boolean input(Player player, LivingEntity target, boolean ultimate) {
        if (!definitions.snapshot().config("config.yml").getBoolean("controls.sneak-attack-ultimate", true)
                || !player.hasPermission("combatcoresystems.command." + (ultimate ? "ultimate" : "skill"))
                || items.id(player.getInventory().getItemInMainHand()).isEmpty()) return false;
        Map<Boolean, Integer> ticks = inputTicks.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        int tick = org.bukkit.Bukkit.getCurrentTick();
        WeaponDefinition weapon = definitions.snapshot().weapon(items.instance(player.getInventory().getItemInMainHand()).orElse(null));
        if (weapon == null || weapon.ultimate() == null) return false;
        if (Objects.equals(ticks.put(ultimate, tick), tick)) return true;
        activate(player, target, ultimate);
        return true; // An attempted ultimate never leaks a simultaneous normal hit, even on cooldown.
    }

    @EventHandler public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) { inputTicks.remove(event.getPlayer().getUniqueId()); inventoryDrops.remove(event.getPlayer().getUniqueId()); }

    public boolean activate(Player player, LivingEntity target, boolean ultimate) {
        trace("cast attempt", player, "kind=" + (ultimate ? "ultimate" : "skill") + " target=" + (target == null ? "none" : target.getType()));
        PlayerData data = players.find(player.getUniqueId()).orElse(null);
        if (data == null) return fail(player, "loading", "プレイヤーデータを読み込み中です。");
        String weaponId = items.id(player.getInventory().getItemInMainHand()).orElse("");
        if (weaponId.isBlank()) return false;
        WeaponDefinition weapon = definitions.snapshot().weapon(items.instance(player.getInventory().getItemInMainHand()).orElse(null));
        if (weapon == null) return fail(player, "unknown-weapon", "この武器の設定が読み込まれていません。");
        if (!weapon.canEquip(data.getLevel())) return fail(player, "equip-level", "武器の装備可能レベルを満たしていません。");
        WeaponDefinition.SkillDefinition ability = ultimate ? weapon.ultimate() : weapon.skill();
        if (ability == null) return fail(player, "undefined", "この武器には使用する技が設定されていません。");
        if (!conditionsMet(player, target, ability.conditions())) return fail(player, "conditions", "技の発動条件を満たしていません。体力・対象・距離・戦闘状態を確認してください。");
        if (triggers!=null && !triggers.canCast(player,target,weaponId,ultimate,ability.conditions())) return fail(player,"conditions","技の発動条件を満たしていません。");
        if (java.util.stream.Stream.concat(ability.options().selfEffects().stream(), ability.options().targetEffects().stream())
                .anyMatch(id -> !definitions.snapshot().buffs().containsKey(id))) return fail(player, "effect", "参照先のバフ・デバフ設定が見つかりません。");
        if (target != null && !ability.target().equalsIgnoreCase("SELF") && !damage.canAffect(player, target))
            return fail(player, "target", "この対象には技を使用できません。");

        ItemInstance instance = items.instance(player.getInventory().getItemInMainHand()).orElse(null);
        int limitBreak = instance == null ? 0 : instance.getLimitBreak();
        double baseCooldown = override(weapon, limitBreak, (ultimate ? "ultimate" : "skill") + ".cooldown", ability.cooldownSeconds());
        int maxCharges = Math.max(1, (int) override(weapon, limitBreak, (ultimate ? "ultimate" : "skill") + ".charges", ability.charges()));
        String cooldownId = weaponId + ":" + (ultimate ? "ultimate" : "skill");
        AbilityState state = cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(cooldownId, ignored -> new AbilityState(maxCharges));
        double adjustedCooldown = CoreMath.cooldownSeconds(baseCooldown, stats.get(player, data).value(StatKey.COOLDOWN));
        state.refresh(maxCharges, adjustedCooldown);
        double multiplier = override(weapon, limitBreak, (ultimate ? "ultimate" : "skill") + ".multiplier", ability.multiplier());
        double radius = override(weapon, limitBreak, (ultimate ? "ultimate" : "skill") + ".radius", ability.radius());
        Element element = ability.element();
        String attributePath = (ultimate ? "ultimate" : "skill") + ".attribute";
        Object overrideElement = cumulativeOverride(weapon, limitBreak, attributePath);
        if (overrideElement == null) overrideElement = cumulativeOverride(weapon, limitBreak, (ultimate ? "ultimate" : "skill") + ".element");
        if (overrideElement != null) element = Element.parse(String.valueOf(overrideElement)).orElse(element);
        ItemInstance heart = data.getEquipment().get(EquipmentSlot.DIVINE_HEART);
        if (heart != null) {
            String path = "divine-hearts." + heart.getDefinitionId() + ".rules." + (ultimate ? "ultimate" : "skill");
            var divine = definitions.snapshot().config("divine_hearts.yml");
            if (divine.contains(path + ".attribute")) element = Element.parse(divine.getString(path + ".attribute")).orElse(element);
            else if (divine.contains(path + ".element")) element = Element.parse(divine.getString(path + ".element")).orElse(element);
            multiplier *= divine.getDouble(path + ".multiplier", 1.0);
            radius += divine.getDouble(path + ".radius-add", 0.0);
        }

        List<LivingEntity> targets = new ArrayList<>();
        if (target != null) targets.add(target);
        if (radius > 0) {
            player.getWorld().getNearbyLivingEntities(target == null ? player.getLocation() : target.getLocation(), radius,
                    entity -> !entity.equals(player)).forEach(entity -> { if (!targets.contains(entity)) targets.add(entity); });
        }
        if (ability.target().equalsIgnoreCase("SELF")) { targets.clear(); targets.add(player); }
        else targets.removeIf(current -> !damage.canAffect(player, current));
        boolean emptyCast = targets.isEmpty() && definitions.snapshot().config("config.yml").getBoolean("controls.allow-empty-cast", true);
        if (targets.isEmpty() && !emptyCast) return fail(player, "target", "対象が見つかりません。対象に照準を合わせてください。");
        if (!state.consume(maxCharges, adjustedCooldown)) {
            player.sendActionBar(mini.deserialize(definitions.snapshot().config("messages.yml").getString("skill-failure.cooldown", "<red>あと <seconds>秒</red>")
                    .replace("<seconds>", String.format(Locale.ROOT, "%.1f", state.remainingSeconds()))));
            return true;
        }
        // Enter combat only after a successful hit, not merely on casting.
        String announcement = definitions.snapshot().config("messages.yml").getString(
                "ability-announcement." + (ultimate ? "ultimate" : "skill"), "<aqua>発動：<name></aqua>");
        if (!announcement.isBlank()) player.sendMessage(mini.deserialize(announcement.replace("<name>", ability.name())));
        if (ability.options().currentHpCost() > 0 && levels != null)
            levels.setVirtualHealth(player, data, data.getHealth() * (1 - ability.options().currentHpCost()));
        if (buffs != null) for (String id : ability.options().selfEffects()) buffs.apply(player, id, player.getUniqueId());
        stats.invalidate(player.getUniqueId());
        trace("cast success", player, "weapon=" + weaponId + " kind=" + (ultimate ? "ultimate" : "skill") + " empty=" + emptyCast);
        if (setEffects != null) setEffects.fire(player, ultimate ? com.github.saku0817.combatcoresystems.model.SetTrigger.Event.ULTIMATE
                : com.github.saku0817.combatcoresystems.model.SetTrigger.Event.SKILL, target != null && damage.canAffect(player, target) ? target : null);
        if (triggers!=null) triggers.cast(player,target,weaponId,limitBreak,ultimate);
        if (emptyCast) WeaponVisuals.play(player, ability.options().visual());
        Element activeElement = element;
        for (LivingEntity current : targets) {
            if (!current.equals(player) && !damage.canAffect(player, current)) continue;
            boolean applied = !ability.options().damageEnabled() || damage.apply(new DamageRequest(player.getUniqueId(), current.getUniqueId(), ability.referenceStat(), multiplier,
                    activeElement, true, false, 0, ultimate ? "ultimate:" + ability.id() : "skill:" + ability.id(), ability.options().components())).applied();
            if (applied) {
                if (buffs != null) for (String id : ability.options().targetEffects()) buffs.apply(current, id, player.getUniqueId());
                WeaponVisuals.play(current, ability.options().visual());
            }
        }
        return true;
    }

    private boolean conditionsMet(Player player, LivingEntity target, Map<String, Object> conditions) {
        PlayerData data = players.require(player);
        PlayerStats currentStats = stats.get(player, data);
        double hpRatio = data.getHealth() / Math.max(1, currentStats.maxHp());
        if (conditions.containsKey("min-hp-percent") && hpRatio < number(conditions.get("min-hp-percent"))) return false;
        if (Boolean.TRUE.equals(conditions.get("requires-target")) && target == null
                && !definitions.snapshot().config("config.yml").getBoolean("controls.allow-empty-cast", true)) return false;
        if (conditions.containsKey("max-distance") && (target == null
                ? !definitions.snapshot().config("config.yml").getBoolean("controls.allow-empty-cast", true)
                : target.getWorld() != player.getWorld() || target.getLocation().distanceSquared(player.getLocation()) > Math.pow(number(conditions.get("max-distance")), 2))) return false;
        return !Boolean.TRUE.equals(conditions.get("requires-combat")) || combat.inCombat(player.getUniqueId());
    }

    public boolean activate(Player player, boolean ultimate) {
        if (items.id(player.getInventory().getItemInMainHand()).isEmpty()) return fail(player, "weapon", "スキルを設定したCCS武器を手に持ってください。");
        return activate(player, rayTarget(player), ultimate);
    }

    private boolean fail(Player player, String key, String fallback) {
        trace("cast rejected", player, "reason=" + key);
        var message = mini.deserialize(definitions.snapshot().config("messages.yml").getString("skill-failure." + key, "<red>" + fallback + "</red>"));
        player.sendActionBar(message);
        if (definitions.snapshot().config("config.yml").getBoolean("controls.failure-chat", true)) player.sendMessage(message);
        return false;
    }

    private LivingEntity rayTarget(Player player) {
        RayTraceResult ray = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 16,
                0.5, entity -> entity instanceof LivingEntity && !entity.equals(player) && player.hasLineOfSight(entity));
        Entity hit = ray == null ? null : ray.getHitEntity();
        return hit instanceof LivingEntity living ? living : null;
    }

    private double override(WeaponDefinition weapon, int limitBreak, String key, double fallback) {
        Object value = cumulativeOverride(weapon, limitBreak, key);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private Object cumulativeOverride(WeaponDefinition weapon, int limitBreak, String key) {
        Object result = null;
        for (int level = 0; level <= limitBreak; level++) {
            Map<String, Object> values = weapon.limitBreaks().get(level);
            if (values != null && values.containsKey(key)) result = values.get(key);
            if (values != null && key.endsWith(".cooldown") && values.containsKey(key + "-seconds")) result = values.get(key + "-seconds");
        }
        return result;
    }

    private double number(Object value) { return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value)); }

    public Status status(UUID player, boolean ultimate) {
        Player online = org.bukkit.Bukkit.getPlayer(player);
        ItemInstance held = online == null ? null : items.instance(online.getInventory().getItemInMainHand()).orElse(null);
        WeaponDefinition weapon = held == null ? null : definitions.snapshot().weapon(held);
        var ability = weapon == null ? null : ultimate ? weapon.ultimate() : weapon.skill();
        if (ability == null) return new Status(false, 0, 0, 0);
        String kind = ultimate ? "ultimate" : "skill";
        int maximum = Math.max(1, (int) override(weapon, held.getLimitBreak(), kind + ".charges", ability.charges()));
        AbilityState state = cooldowns.getOrDefault(player, Map.of()).get(weapon.id() + ":" + kind);
        if (state == null) return new Status(true, 0, maximum, maximum, weapon.id() + ":" + kind);
        double cooldown = CoreMath.cooldownSeconds(override(weapon, held.getLimitBreak(), kind + ".cooldown", ability.cooldownSeconds()), stats.get(online, players.require(online)).value(StatKey.COOLDOWN));
        state.refresh(maximum, cooldown);
        return new Status(state.charges > 0, state.remainingSeconds(), state.charges, maximum, weapon.id() + ":" + kind);
    }

    public record Status(boolean ready, double remainingSeconds, int charges, int maximumCharges, String abilityKey) {
        public Status(boolean ready, double remainingSeconds, int charges, int maximumCharges) { this(ready, remainingSeconds, charges, maximumCharges, ""); }
    }

    private static final class AbilityState {
        private int charges;
        private int maximumCharges;
        private double cooldownSeconds;
        private long nextChargeAt;
        AbilityState(int maximum) { this.charges = maximum; this.maximumCharges = maximum; }
        void refresh(int maximum, double cooldown) {
            maximumCharges = maximum; cooldownSeconds = cooldown;
            long now = System.currentTimeMillis();
            while (charges < maximumCharges && nextChargeAt > 0 && now >= nextChargeAt) {
                charges++;
                nextChargeAt = charges < maximumCharges ? nextChargeAt + (long) (cooldown * 1000) : 0;
            }
            charges = Math.min(charges, maximumCharges);
        }
        boolean consume(int maximum, double cooldown) {
            refresh(maximum, cooldown);
            if (charges <= 0) return false;
            charges--;
            if (charges < maximum && nextChargeAt == 0) nextChargeAt = System.currentTimeMillis() + (long) (cooldown * 1000);
            if (cooldown <= 0) refresh(maximum, cooldown);
            return true;
        }
        double remainingSeconds() { return charges > 0 || nextChargeAt == 0 ? 0 : Math.max(0, (nextChargeAt - System.currentTimeMillis()) / 1000.0); }
    }
}
```

## StatService.java

```java
package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.util.CoreMath;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class StatService implements Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final CombatStateService combat;
    private final ItemService items;
    private final NamespacedKey itemIdKey;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cacheTicks = new HashMap<>();
    private DefinitionRegistry.Snapshot cachedDefinitions;
    private DefinitionRegistry.Snapshot talentDefinitions;
    private Set<WeaponOptions.Hand> talentHands = Set.of();
    private final Map<UUID, Set<String>> activeTalents = new HashMap<>();
    private BuffService buffs;
    private DynamicEffectService dynamic;
    public void bindEffects(BuffService buffs,DynamicEffectService dynamic) { this.buffs=buffs; this.dynamic=dynamic; }

    public StatService(JavaPlugin plugin, DefinitionRegistry definitions, CombatStateService combat, ItemService items) {
        this.plugin = plugin;
        this.definitions = definitions;
        this.combat = combat;
        this.items = items;
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
    }

    public PlayerStats get(Player player, PlayerData data) {
        if (cachedDefinitions != definitions.snapshot()) { cache.clear(); cacheTicks.clear(); cachedDefinitions = definitions.snapshot(); }
        // Share repeated HUD/damage queries within one tick, never cache external changes indefinitely.
        PlayerStats value = cache.get(player.getUniqueId());
        return value != null && Objects.equals(cacheTicks.get(player.getUniqueId()), org.bukkit.Bukkit.getCurrentTick())
                ? value : recalculate(player, data);
    }

    public PlayerStats recalculate(Player player, PlayerData data) {
        PlayerStats stats = calculate(player, data, false);
        synchronizeAttackAttribute(player, stats.atk());
        cache.put(player.getUniqueId(), stats);
        cacheTicks.put(player.getUniqueId(), org.bukkit.Bukkit.getCurrentTick());
        return stats;
    }

    public void invalidate(UUID uuid) { cache.remove(uuid); }

    @EventHandler public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
        cacheTicks.remove(event.getPlayer().getUniqueId());
        activeTalents.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler public void onHeldItem(PlayerItemHeldEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> invalidate(event.getPlayer().getUniqueId()));
    }

    @EventHandler public void onSwapHand(PlayerSwapHandItemsEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> invalidate(event.getPlayer().getUniqueId()));
    }

    public PlayerStats describe(Player player, PlayerData data) { return calculate(player, data, true); }

    private PlayerStats calculate(Player player, PlayerData data, boolean detailed) {
        if (talentDefinitions != definitions.snapshot()) {
            talentDefinitions = definitions.snapshot();
            talentHands = java.util.stream.Stream.concat(talentDefinitions.weapons().values().stream(),
                            talentDefinitions.weaponStages().values().stream().flatMap(List::stream))
                    .map(weapon -> weapon.options().talent()).filter(Objects::nonNull)
                    .map(WeaponOptions.Talent::hand).collect(java.util.stream.Collectors.toSet());
        }
        YamlConfiguration levels = definitions.snapshot().config("levels.yml");
        int level = data.getLevel();
        int rebirth = data.getRebirthCount();
        double baseHp = CoreMath.linear(levels.getDouble("player.hp.start", 20), levels.getDouble("player.hp.end", 3000), level, 100)
                + levels.getDouble("player.rebirth.hp", 100) * rebirth;
        double playerBaseAtk = CoreMath.linear(levels.getDouble("player.atk.start", 2), levels.getDouble("player.atk.end", 200), level, 100)
                + levels.getDouble("player.rebirth.atk", 20) * rebirth;
        double baseDef = CoreMath.linear(levels.getDouble("player.def.start", 0), levels.getDouble("player.def.end", 100), level, 100)
                + levels.getDouble("player.rebirth.def", 10) * rebirth;

        EnumMap<StatKey, Double> modifiers = defaults();
        Map<String, Map<StatKey, Double>> sources = new LinkedHashMap<>();
        if (detailed) sources.put("基礎値", Map.of(StatKey.HP_FLAT, baseHp, StatKey.ATK_FLAT, playerBaseAtk, StatKey.DEF_FLAT, baseDef,
                StatKey.CRIT_RATE, modifiers.get(StatKey.CRIT_RATE), StatKey.CRIT_DAMAGE, modifiers.get(StatKey.CRIT_DAMAGE), StatKey.ATTACK_SPEED, modifiers.get(StatKey.ATTACK_SPEED)));
        double weaponAtk = vanillaWeaponAttack(player, levels);
        if (detailed && weaponAtk != 0) sources.put("バニラ武器から", Map.of(StatKey.ATK_FLAT, weaponAtk));
        Set<String> currentTalents = new HashSet<>();
        Set<String> previousTalents = activeTalents.getOrDefault(player.getUniqueId(), Set.of());
        Set<String> counted = new HashSet<>();
        int selected = player.getInventory().getHeldItemSlot();
        for (int slot = 0; slot <= 40; slot++) {
            if (slot >= 36 && slot <= 39) continue;
            if (slot != selected && slot != 40 && !talentHands.contains(WeaponOptions.Hand.INVENTORY)
                    && !(slot <= 8 && talentHands.contains(WeaponOptions.Hand.HOT_BAR))) continue;
            ItemStack stack = player.getInventory().getItem(slot);
            if (!definitions.snapshot().weapons().containsKey(items.id(stack).orElse(""))) continue;
            ItemInstance instance = items.instance(stack).orElse(null);
            WeaponDefinition weapon = instance == null ? null : definitions.snapshot().weapon(instance);
            if (weapon == null) continue;
            if (!counted.add(instance.getInstanceId())) continue;
            if (!weapon.canEquip(level)) continue;
            if (slot == selected || slot == 40) {
                weaponAtk += weapon.attackFor(level, instance.getLevel());
                if (detailed) sources.computeIfAbsent("武器：" + weapon.name(), ignored -> new EnumMap<>(StatKey.class)).merge(StatKey.ATK_FLAT, weapon.attackFor(level, instance.getLevel()), Double::sum);
                if (weapon.bonusElement() != Element.PHYSICAL) add(modifiers, damageKey(weapon.bonusElement()), weapon.elementBonus());
                if (detailed && weapon.bonusElement() != Element.PHYSICAL) sources.get("武器：" + weapon.name()).merge(damageKey(weapon.bonusElement()), weapon.elementBonus(), Double::sum);
            }
            WeaponOptions.Talent talent = weapon.options().talent();
            if (talent != null && talent.hand().includes(slot, selected)) {
                EnumMap<StatKey, Double> beforeTalent = detailed ? new EnumMap<>(modifiers) : null;
                talent.modifiers().forEach((key, value) -> add(modifiers, key, value * talent.multiplier()));
                recordDelta(sources, "天賦：" + talent.name(), beforeTalent, modifiers);
                String activation = instance.getInstanceId() + ":" + talent.name();
                currentTalents.add(activation);
                if (!previousTalents.contains(activation)) {
                    String message = definitions.snapshot().config("messages.yml").getString("ability-announcement.talent", "<green>天賦発動：<name></green>");
                    if (!message.isBlank()) player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(message.replace("<name>", talent.name())));
                }
            }
        }
        activeTalents.put(player.getUniqueId(), currentTalents);
        EnumMap<StatKey, Double> beforeEquipment = detailed ? new EnumMap<>(modifiers) : null;
        for (Map.Entry<EquipmentSlot, ItemInstance> equipped : data.getEquipment().entrySet()) {
            ItemInstance item = equipped.getValue();
            EquipmentDefinition equipment = definitions.snapshot().equipment().get(item.getDefinitionId());
            if (equipment != null) {
                add(modifiers, item.mainStat(equipment), item.mainValue(equipment));
                item.getSubstats().entrySet().stream().limit(item.getUnlockedSubstats()).forEach(entry -> {
                    String key = entry.getKey(); double value = entry.getValue();
                    try { add(modifiers, StatKey.valueOf(key), value); } catch (IllegalArgumentException ignored) {}
                });
            }
        }
        recordDelta(sources, "装備から", beforeEquipment, modifiers);
        List<TimedEffect> activeEffects=buffs==null ? java.util.stream.Stream.concat(data.getBuffs().stream(), data.getDebuffs().stream()).toList() : buffs.effects(player);
        for (TimedEffect effect : activeEffects) {
            EnumMap<StatKey, Double> before = detailed ? new EnumMap<>(modifiers) : null;
            applyEffect(effect, modifiers);
            recordDelta(sources, "バフ・デバフ：" + definitions.snapshot().config("buffs.yml").getString("buffs." + effect.getId() + ".name", effect.getId()), before, modifiers);
        }
        EnumMap<StatKey, Double> beforeTree = detailed ? new EnumMap<>(modifiers) : null;
        applySkillTree(data, modifiers); recordDelta(sources, "スキルツリーから", beforeTree, modifiers);
        for (var set : SetEffectService.counts(definitions.snapshot(), data).entrySet()) {
            EnumMap<StatKey, Double> before = detailed ? new EnumMap<>(modifiers) : null;
            var config = definitions.snapshot().config("sets.yml");
            if (set.getValue() >= 2) applyModifierSection(config.getConfigurationSection("sets." + set.getKey() + ".two-piece.modifiers"), modifiers);
            if (set.getValue() >= 4) applyModifierSection(config.getConfigurationSection("sets." + set.getKey() + ".four-piece.modifiers"), modifiers);
            recordDelta(sources, "セット効果：" + config.getString("sets." + set.getKey() + ".name", set.getKey()), before, modifiers);
        }
        EnumMap<StatKey, Double> beforeHeart = detailed ? new EnumMap<>(modifiers) : null;
        applyDivineHeart(data, modifiers); recordDelta(sources, "神心から", beforeHeart, modifiers);

        double vanillaArmor = 0;
        if (dynamic!=null) {
            var before=detailed ? new EnumMap<>(modifiers) : null;
            dynamic.modifiers(player.getUniqueId()).forEach((key,value) -> add(modifiers,key,value));
            recordDelta(sources,"動的効果から",before,modifiers);
        }
        Map<StatKey,Double> finalOverrides=new EnumMap<>(StatKey.class);
        for (TimedEffect effect : activeEffects) {
            if (!effect.isPermanent() && effect.getRemainingMillis()<=0) continue;
            var overrides=definitions.snapshot().config("buffs.yml").getConfigurationSection("buffs."+effect.getId()+".modifiers.override");
            if (overrides==null) continue;
            for (String key : overrides.getKeys(false)) {
                StatKey stat=StatKey.valueOf(key); double value=overrides.getDouble(key);
                finalOverrides.put(stat,value);
            }
        }
        var beforeOverrides=detailed ? new EnumMap<>(modifiers) : null;
        finalOverrides.forEach((key,value) -> { if (!Set.of(StatKey.HP_FLAT,StatKey.ATK_FLAT,StatKey.DEF_FLAT).contains(key)) modifiers.put(key,value); });
        recordDelta(sources,"固定値補正（バフ・デバフ）",beforeOverrides,modifiers);
        AttributeInstance armor = player.getAttribute(Attribute.ARMOR);
        if (armor != null) vanillaArmor = armor.getValue();
        if (detailed && vanillaArmor != 0) sources.put("バニラ防具から", Map.of(StatKey.DEF_FLAT, vanillaArmor));
        double hp = finalOverrides.getOrDefault(StatKey.HP_FLAT,baseHp * (1 + modifiers.get(StatKey.HP_PERCENT)) + modifiers.get(StatKey.HP_FLAT));
        double atk = finalOverrides.getOrDefault(StatKey.ATK_FLAT,CoreMath.attack(playerBaseAtk, weaponAtk, modifiers.get(StatKey.ATK_PERCENT), modifiers.get(StatKey.ATK_FLAT)));
        double def = finalOverrides.getOrDefault(StatKey.DEF_FLAT,(baseDef + vanillaArmor) * (1 + modifiers.get(StatKey.DEF_PERCENT)) + modifiers.get(StatKey.DEF_FLAT));
        if (detailed) {
            var original=Map.of(StatKey.HP_FLAT,baseHp*(1+modifiers.get(StatKey.HP_PERCENT))+modifiers.get(StatKey.HP_FLAT),
                    StatKey.ATK_FLAT,CoreMath.attack(playerBaseAtk,weaponAtk,modifiers.get(StatKey.ATK_PERCENT),modifiers.get(StatKey.ATK_FLAT)),
                    StatKey.DEF_FLAT,(baseDef+vanillaArmor)*(1+modifiers.get(StatKey.DEF_PERCENT))+modifiers.get(StatKey.DEF_FLAT));
            for (var key : original.keySet()) if (finalOverrides.containsKey(key))
                sources.computeIfAbsent("固定値補正（バフ・デバフ）",ignored -> new EnumMap<>(StatKey.class)).put(key,finalOverrides.get(key)-original.get(key));
        }
        return new PlayerStats(level, hp, atk, def, modifiers).withSources(sources);
    }

    private void recordDelta(Map<String, Map<StatKey, Double>> sources, String label, Map<StatKey, Double> before, Map<StatKey, Double> after) {
        if (before == null) return;
        for (StatKey key : StatKey.values()) {
            double delta = after.getOrDefault(key, 0.0) - before.getOrDefault(key, 0.0);
            if (Math.abs(delta) > 1e-12) sources.computeIfAbsent(label, ignored -> new EnumMap<>(StatKey.class)).merge(key, delta, Double::sum);
        }
    }

    private double vanillaWeaponAttack(Player player, YamlConfiguration levels) {
        if (!levels.getBoolean("player.vanilla-weapons.enabled", true)) return 0;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) return 0;
        if (held.hasItemMeta()) {
            String itemId = held.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
            if (itemId != null && definitions.snapshot().weapons().containsKey(itemId)) return 0;
        }
        double value = levels.getDouble("player.vanilla-weapons.attack-values." + held.getType().name(), 0);
        return Math.max(0, value * levels.getDouble("player.vanilla-weapons.conversion-multiplier", 1));
    }

    /** Native held-item modifiers must not be added a second time to CCS's final ATK. */
    static void synchronizeAttackAttribute(Player player, double finalAttack) {
        AttributeInstance attribute = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attribute == null) return;
        double add = 0, scalar = 0, product = 1;
        for (org.bukkit.attribute.AttributeModifier modifier : attribute.getModifiers()) {
            switch (modifier.getOperation()) {
                case ADD_NUMBER -> add += modifier.getAmount();
                case ADD_SCALAR -> scalar += modifier.getAmount();
                case MULTIPLY_SCALAR_1 -> product *= 1 + modifier.getAmount();
            }
        }
        double factor = (1 + scalar) * product;
        if (factor > 0 && Double.isFinite(factor)) {
            double base = CoreMath.nativeAttackBase(finalAttack, add, scalar, product);
            if (Double.compare(attribute.getBaseValue(), base) != 0) attribute.setBaseValue(base);
        }
    }

    private EnumMap<StatKey, Double> defaults() {
        EnumMap<StatKey, Double> values = new EnumMap<>(StatKey.class);
        for (StatKey key : StatKey.values()) values.put(key, 0.0);
        values.put(StatKey.CRIT_RATE, 0.05);
        values.put(StatKey.CRIT_DAMAGE, 0.50);
        values.put(StatKey.ATTACK_SPEED, 4.0);
        return values;
    }

    private void applyEffects(PlayerData data, EnumMap<StatKey, Double> modifiers) {
        for (TimedEffect effect : data.getBuffs()) applyEffect(effect, modifiers);
        for (TimedEffect effect : data.getDebuffs()) applyEffect(effect, modifiers);
    }

    private void applyEffect(TimedEffect effect, EnumMap<StatKey, Double> modifiers) {
        BuffDefinition definition = definitions.snapshot().buffs().get(effect.getId());
        if (definition == null) return;
        definition.flatModifiers().forEach((key, value) -> add(modifiers, key, value * effect.getStacks()));
        definition.percentModifiers().forEach((key, value) -> add(modifiers, key, value * effect.getStacks()));
    }

    private void applySkillTree(PlayerData data, EnumMap<StatKey, Double> modifiers) {
        var trees = definitions.snapshot().config("skill_trees.yml").getConfigurationSection("trees");
        if (trees == null) return;
        for (String tree : trees.getKeys(false)) {
            for (var node : data.getSkillNodes().entrySet()) {
                var section = trees.getConfigurationSection(tree + ".nodes." + node.getKey());
                if (section == null) continue;
                for (int rank = 1; rank <= node.getValue(); rank++) applyModifierSection(section.getConfigurationSection("ranks." + rank + ".modifiers"), modifiers);
            }
        }
    }

    private void applySetBonuses(PlayerData data, EnumMap<StatKey, Double> modifiers) {
        Map<String, Integer> counts = new HashMap<>();
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.RESONANCE)) {
            ItemInstance item = data.getEquipment().get(slot);
            EquipmentDefinition definition = item == null ? null : definitions.snapshot().equipment().get(item.getDefinitionId());
            if (definition != null && !definition.setId().isBlank()) counts.merge(definition.setId(), 1, Integer::sum);
        }
        var sets = definitions.snapshot().config("sets.yml");
        counts.forEach((id, count) -> {
            if (count >= 2) applyModifierSection(sets.getConfigurationSection("sets." + id + ".two-piece.modifiers"), modifiers);
            if (count >= 4) applyModifierSection(sets.getConfigurationSection("sets." + id + ".four-piece.modifiers"), modifiers);
        });
    }

    private void applyDivineHeart(PlayerData data, EnumMap<StatKey, Double> modifiers) {
        ItemInstance heart = data.getEquipment().get(EquipmentSlot.DIVINE_HEART);
        if (heart != null) applyModifierSection(definitions.snapshot().config("divine_hearts.yml")
                .getConfigurationSection("divine-hearts." + heart.getDefinitionId() + ".modifiers"), modifiers);
    }

    private void applyModifierSection(org.bukkit.configuration.ConfigurationSection section, EnumMap<StatKey, Double> modifiers) {
        if (section == null) return;
        for (String raw : section.getKeys(false)) {
            try { add(modifiers, StatKey.valueOf(raw.toUpperCase(java.util.Locale.ROOT)), section.getDouble(raw)); }
            catch (IllegalArgumentException ignored) {}
        }
    }

    private void add(EnumMap<StatKey, Double> values, StatKey key, double value) { values.merge(key, value, Double::sum); }
    private StatKey damageKey(Element element) { return StatKey.valueOf(element.name() + "_DAMAGE"); }
}
```
