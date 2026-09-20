# CombatCoreSystems v1.4.4 — YAML・実装参照資料

この資料は配布ソースから機械的に収録しています。生成時は引き継ぎ書・ChatGPT-YAML-v1.4.4.mdも参照してください。運用中の秘密情報は含めないでください。

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

## 実装参照: config/DefinitionRegistry.java

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

        var setRoot = yaml.get("sets.yml").getConfigurationSection("sets");
        if (setRoot != null) for (String id : setRoot.getKeys(false)) for (String tier : List.of("two-piece", "four-piece")) {
            try { SetTrigger.parse(setRoot.getConfigurationSection(id + "." + tier + ".triggers"), buffs.keySet()); }
            catch (IllegalArgumentException ex) { errors.add("set " + id + ": " + ex.getMessage()); }
        }
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
                EnumMap<StatKey, Double> flat = parseStatMap(s.getConfigurationSection("modifiers.flat"), errors, "buff " + id);
                EnumMap<StatKey, Double> percent = parseStatMap(s.getConfigurationSection("modifiers.percent"), errors, "buff " + id);
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

## 実装参照: config/WeaponOptionsParser.java

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
        return new WeaponOptions.Ability(description(s), s.getBoolean("damage-enabled", true), cost,
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

## 実装参照: model/StatKey.java

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

## 実装参照: model/EquipmentRolls.java

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

## 実装参照: model/SetTrigger.java

```java
package com.github.saku0817.combatcoresystems.model;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

public record SetTrigger(String id, Event event, double hpBelow, long cooldownMillis, Target target, List<String> effects) {
    public enum Event { SKILL, ULTIMATE, HIT, TAKE_DAMAGE, HP_BELOW }
    public enum Target { SELF, OTHER }
    public static List<SetTrigger> parse(ConfigurationSection section, Set<String> knownBuffs) {
        if (section == null) return List.of();
        List<SetTrigger> result = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s == null) throw new IllegalArgumentException("Set trigger must be a map: " + id);
            Event event = Event.valueOf(s.getString("event", "").toUpperCase(Locale.ROOT));
            Target target = Target.valueOf(s.getString("target", "SELF").toUpperCase(Locale.ROOT));
            double hp = s.getDouble("hp-percent", 0.5), cooldown = s.getDouble("cooldown-seconds", 1);
            if (!Double.isFinite(hp) || hp < 0 || hp > 1 || !Double.isFinite(cooldown) || cooldown < 0 || cooldown > 86400)
                throw new IllegalArgumentException("Invalid set trigger HP/cooldown: " + id);
            if (event == Event.HP_BELOW && target != Target.SELF) throw new IllegalArgumentException("HP_BELOW requires SELF");
            List<String> effects = s.getStringList("effects");
            if (effects.isEmpty() || !knownBuffs.containsAll(effects)) throw new IllegalArgumentException("Unknown/empty buffs.yml effects: " + id);
            result.add(new SetTrigger(id, event, hp, Math.round(cooldown * 1000), target, List.copyOf(effects)));
        }
        return List.copyOf(result);
    }
}
```

## 実装参照: model/ItemInstance.java

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

## 実装参照: service/SetEffectService.java

```java
package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.event.AfterDamageEvent;
import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class SetEffectService implements Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final BuffService buffs;
    private DefinitionRegistry.Snapshot snapshot;
    private final Map<String, List<SetTrigger>> rules = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Set<String>> hpActive = new HashMap<>();
    public SetEffectService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, StatService stats, BuffService buffs) {
        this.plugin = plugin; this.definitions = definitions; this.players = players; this.stats = stats; this.buffs = buffs;
    }
    public static Map<String, Integer> counts(DefinitionRegistry.Snapshot snapshot, PlayerData data) {
        Map<String, Integer> counts = new HashMap<>();
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.RESONANCE)) {
            ItemInstance item = data.getEquipment().get(slot);
            EquipmentDefinition definition = item == null ? null : snapshot.equipment().get(item.getDefinitionId());
            if (definition != null && !definition.setId().isBlank()) counts.merge(definition.setId(), 1, Integer::sum);
        }
        return counts;
    }
    public void start() { Bukkit.getScheduler().runTaskTimer(plugin, () -> {
        refresh();
        if (rules.values().stream().flatMap(Collection::stream).noneMatch(r -> r.event() == SetTrigger.Event.HP_BELOW)) return;
        for (Player player : Bukkit.getOnlinePlayers()) fire(player, SetTrigger.Event.HP_BELOW, null);
    }, 5, 5); }
    private void refresh() {
        if (snapshot == definitions.snapshot()) return;
        snapshot = definitions.snapshot(); rules.clear(); hpActive.clear();
        var root = snapshot.config("sets.yml").getConfigurationSection("sets");
        if (root != null) for (String id : root.getKeys(false)) for (String tier : List.of("two-piece", "four-piece")) {
            try { rules.put(id + ":" + tier, SetTrigger.parse(root.getConfigurationSection(id + "." + tier + ".triggers"), snapshot.buffs().keySet())); }
            catch (IllegalArgumentException ignored) { /* Already reported by DefinitionRegistry; omit invalid initial definitions. */ }
        }
    }
    public void fire(Player player, SetTrigger.Event event, LivingEntity other) {
        refresh();
        PlayerData data = players.find(player.getUniqueId()).orElse(null);
        if (data == null || player.isDead()) return;
        Set<String> active = hpActive.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());
        Set<String> seen = new HashSet<>();
        for (var set : counts(snapshot, data).entrySet()) for (String tier : List.of("two-piece", "four-piece")) {
            if (set.getValue() < (tier.equals("two-piece") ? 2 : 4)) continue;
            String prefix = set.getKey() + ":" + tier;
            for (SetTrigger rule : rules.getOrDefault(prefix, List.of())) {
                if (rule.event() != event) continue;
                String key = prefix + ":" + rule.id();
                if (event == SetTrigger.Event.HP_BELOW) {
                    if (data.getHealth() / Math.max(1, stats.get(player, data).maxHp()) > rule.hpBelow()) continue;
                    seen.add(key);
                    if (!active.add(key)) continue;
                }
                LivingEntity target = rule.target() == SetTrigger.Target.SELF ? player : other;
                if (target == null || target.isDead()) continue;
                long now = System.currentTimeMillis();
                Map<String, Long> timers = cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
                if (timers.getOrDefault(key, 0L) > now) continue;
                timers.put(key, now + rule.cooldownMillis());
                for (String effect : rule.effects()) buffs.apply(target, effect, player.getUniqueId());
            }
        }
        if (event == SetTrigger.Event.HP_BELOW) active.retainAll(seen);
    }
    @EventHandler public void onDamage(AfterDamageEvent event) {
        if (!event.getResult().applied() || event.getResult().finalDamage() <= 0) return;
        refresh();
        if (rules.values().stream().flatMap(Collection::stream).noneMatch(rule -> rule.event() == SetTrigger.Event.HIT || rule.event() == SetTrigger.Event.TAKE_DAMAGE)) return;
        var request = event.getRequest();
        var attacker = request.attacker() == null ? null : Bukkit.getEntity(request.attacker());
        var target = Bukkit.getEntity(request.target());
        // DOT callbacks may run while BuffService iterates effects. Apply new effects on the next tick.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (attacker instanceof Player player && player.isOnline() && target instanceof LivingEntity other && !player.equals(other)) fire(player, SetTrigger.Event.HIT, other);
            if (target instanceof Player player && player.isOnline()) fire(player, SetTrigger.Event.TAKE_DAMAGE, attacker instanceof LivingEntity other ? other : null);
        });
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { cooldowns.remove(event.getPlayer().getUniqueId()); hpActive.remove(event.getPlayer().getUniqueId()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnvironmentDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent || event.getFinalDamage() <= 0 || !(event.getEntity() instanceof Player player)) return;
        Bukkit.getScheduler().runTask(plugin, () -> { if (player.isOnline()) fire(player, SetTrigger.Event.TAKE_DAMAGE, null); });
    }
}
```

## 実装参照: service/SkillService.java

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
