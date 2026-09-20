# CombatCoreSystems v1.4.4 — ChatGPT用YAML作成引き継ぎ書

v1.4.4の追加機能は `ChatGPT-YAML-v1.4.4.md` を併読してください。装備の抽選・セットの条件付き効果・図鑑アイコンについては同資料を優先します。

## 利用方法

このファイルと `ChatGPT-YAML-Reference.md`、変更対象の現在のymlをChatGPTのチャットへ添付してください。サーバーの認証トークン、DBパスワード、個人情報は添付前に除いてください。ソースの既定値と運用中の値は異なるため、運用中ファイルを優先して差分を作ります。

### そのまま使える依頼文

> CombatCoreSystems v1.4.4用の設定を作成してください。添付の引き継ぎ書・v1.4.4差分・YAMLリファレンスを仕様として使い、未実装のキーを推測で追加しないでください。表示文と実際の効果を別々に定義してください。変更対象以外のID・設定を保持し、ファイルごとの変更点、追加する定義、参照する他ファイル、実現できない部分を明示してください。不明なゲーム仕様は質問してください。YAMLはスペース2個でインデントし、ルートキーを重複させないでください。私が作りたい内容は次のとおりです：……

## ファイルとルート

| ファイル | 定義ルート | 内容 |
| --- | --- | --- |
| weapons.yml | weapons | 武器・天賦・スキル・必殺技・限界突破 |
| equipment.yml | equipment | 装備、メイン・サブステータス、初期Lv |
| sets.yml | sets | 装備2/4セット効果 |
| divine_hearts.yml | divine-hearts | 神心の表示、補正、通常属性と反応置換 |
| buffs.yml | buffs | バフ・デバフ。スキルからIDで参照 |
| mobs.yml | mobs / vanilla-mobs | 独自Mob／バニラMobの上書き・独自EXP |
| bosses.yml | bosses | Boss本体、技、フェーズ、ドロップ |
| reactions.yml | reactions | 属性反応の組合せ・成分・同一対象CT |
| levels.yml | player / materials / weapon-exp / equipment-exp | 成長と強化素材・必要EXP |
| config.yml | 各設定キー | 共通倍率、操作、色、HUD、ダメージチャット |
| messages.yml / gui.yml | 各表示キー | 文言・ツールチップ・GUI |

すべてのファイルは `data-version: 1` を使用します。既存の `equipments.yml` も読み込まれますが、新規作成は `equipment.yml` に統一してください。同名装備IDを両ファイルに作らないでください。IDは英数字・アンダースコア・ハイフンで作成し、異なるアイテム種別でも同じIDを避けます。

## 武器

- `name`, `material`, `rarity: 1..5`, `enchantment-glint`, `custom-model-data`, 複数行の`lore`。
- `category`: `MELEE` / `RANGED` / `UNCATEGORIZED`。`type`は旧別名で、両方書くと`type`優先。新規定義では`category`だけを使用。
- `base-atk.level-1`, `base-atk.level-100`: 武器Lv.1と100のATK。間は線形補間。
- `attribute-bonus.type`: `FIRE`, `WATER`, `WIND`, `THUNDER`, `MOON`, `PHYSICAL`。`value: 0.15`は+15%。
- `equip-level.min/max`: プレイヤーレベルによる装備効果の有効範囲（1～100）。
- `normal-attack.attribute`: 通常攻撃属性。`normal-attack.visual`: 演出。
- `talent`: 名前・説明・発動場所・倍率・常時補正。`skill`, `ultimate`: 別々の技。

基本ATK式は `(プレイヤー基礎ATK + 有効な両手のCCS武器ATK + バニラ手持ち武器換算ATK) × (1 + ATK_PERCENT) + ATK_FLAT`。ホットバー・インベントリにあるだけの武器ATKは加算されません。

### 天賦

```yaml
talent:
  name: '<gold>守りの光</gold>'
  description:
  - 'ホットバーに所持している間、最大HP+50%。'
  hand: HOT_BAR
  multiplier: 1.0
  modifiers:
    HP_PERCENT: 0.50
```

`hand`は`talent`の下に書きます。武器直下の`hand`キーはありません。

| hand | 発動する所持位置 |
| --- | --- |
| MAIN_HAND | 現在選択している利き手 |
| OFF_HAND | オフハンド |
| EITHER_HAND | どちらかの手 |
| HOT_BAR | ホットバー0～8（未選択も含む）。オフハンドは含まない |
| INVENTORY | 所持品0～35とオフハンド。防具スロット・カーソル・チェストは含まない |

天賦の補正値に`multiplier`を乗算します。装備可能レベルを満たす必要があります。異なる個体の天賦はそれぞれ加算します。手以外の天賦を有効にしても、スキル／必殺技は利き手武器から発動します。

### スキル・必殺技

- `name`, `description`（文字列または文字列リスト）。説明文を書くだけでは効果は発生しません。
- `reference`: `ATK` / `HP` / `DEF`。HPは最大HP。
- `multiplier`: 2.0=200%。`attribute`: 与えるダメージの属性。
- `cooldown-seconds`, `charges`, `radius`, `target`。`target: SELF`は自分、それ以外は敵向け判定。
- `damage-enabled: false`: ダメージなしの自己バフ技など。
- `cost.current-hp-percent: 0.30`: 現在HPの30%消費。0以上1未満。
- `conditions`: 対応キーはリファレンスを参照。`min-hp-percent: 0.30`は最大HPの30%以上を要求。
- `self-effects`, `target-effects`: `buffs.yml`に存在するIDのリスト。説明文や任意のスクリプトは入れない。
- `visual`: 演出。技・通常攻撃で使用可能。

```yaml
ultimate:
  name: '<red>明けの明星</red>'
  description: ['ATK200%＋炎補正を適用したATK100%の炎属性ダメージ。']
  reference: ATK
  attribute: FIRE
  cooldown-seconds: 80
  charges: 1
  target: ENEMY
  damage-components:
  - reference: ATK
    multiplier: 2.0
    bonus-attribute: PHYSICAL
  - reference: ATK
    multiplier: 1.0
    bonus-attribute: FIRE
  target-effects: [sunset]
```

`damage-components`がある場合、上位の`reference/multiplier`ではなく成分ごとの指定を使います。リスト全体を置換します。ATK100・炎補正+15%なら共通防御等の前で200+115=315です。防御・耐性の処理後に会心を適用します。最終ダメージはconfigの共通倍率・防御式にも依存し、常に315になるわけではありません。

`PHYSICAL`を`bonus-attribute`に指定すると属性ダメージ補正を乗せません。最終表示・属性付着は技の`attribute`を使用します。`damage-components: []`は不正です。

### 限界突破段階

`limit-breaks.0`～`.5`に変更したい項目だけ指定します。対応する差分は`base-atk`、`attribute-bonus`、`equip-level`、`talent`、`skill`、`ultimate`、`normal-attack`、`name`、`lore`、`rarity`、`category`です。material・custom-model-data・enchantment-glintを段階ごとに変更する機能ではありません。段階0→1→…の順に辞書を累積マージし、リストは丸ごと置換。未指定の値は前段階を継承します。各段階は起動／reload時に解析・検証されます。

```yaml
limit-breaks:
  1:
    base-atk: {level-1: 120, level-100: 650}
    attribute-bonus: {type: FIRE, value: 0.20}
    talent:
      modifiers: {HP_PERCENT: 0.60}
      description: ['最大HP+60%。']
    skill:
      cooldown-seconds: 25
      description: ['自己バフ。CT25秒。']
  2:
    ultimate:
      name: '<red>明けの明星・改</red>'
      damage-components:
      - {reference: ATK, multiplier: 2.5, bonus-attribute: PHYSICAL}
      - {reference: ATK, multiplier: 1.2, bonus-attribute: FIRE}
      description: ['ATK250%＋炎補正を適用したATK120%。']
```

実ATK、天賦、技、通常属性、武器Loreは同じ段階定義を使用。技の発動に加えて説明も明示的に更新してください。`atk-percent`という限界突破専用キーは未実装で警告のみです。代わりに`base-atk`で直接指定します。バフの強化は別IDをbuffs.ymlへ作り、段階の`self-effects/target-effects`を変更します。

## 装備・神心・バフ

装備は`slot`、`main-stat.type/level-1/max-level`、`max-level`、`set`、`initial-level`、`initial-substats`、`initial-upgrades`、`initial-unlocked-substats`を使用します。部位と使用可能メインステータスの組合せに制約があるため、リファレンスの例を基礎にします。初期値は新規作成品のみで、既存品を上書きしません。

神心はレベル強化対象ではありません。`talents`は名前と説明の表示用、`modifiers`と`rules`が実効果です。武器に通常属性があれば神心の通常属性変換より武器が優先。`rules.reaction-override`で反応を置換できます。詳しいキーは神心のコメント例を参照。

バフ／デバフは `kind: BUFF/DEBUFF`, `duration`（秒）, `max-stacks`, `modifiers.flat/percent`などを使用。`ATK_PERCENT: -0.30`はATK-30%。`DEF_IGNORED_WHEN_HIT: 0.40`は被攻撃時に防御力40%無視、攻撃者側の`DEF_IGNORE`とは別です。新しい効果名を命名するだけでは新しい処理は実装されません。

## 表示・演出

MiniMessageを使用。`<red>`, `<#ffaa00>`, `<bold>`, `<u>`, `<italic>`などのタグは正しく閉じます。未指定のLoreは白・斜体なし。意図的な`<italic>`は維持されます。レア度の色は`config.yml: rarity-colors`、属性色は`attribute-colors`。

演出は`particle/count/spread/sound/volume/pitch`。追加データ不要のParticleのみ。count 0～500、spread 0～16、volume 0～4、pitch 0～2。soundは`minecraft:...`形式。存在しない演出名は作らないでください。

## 操作・管理

- スキル: 手からのドロップ。成功経路ではアイテムを地面に落とさない。
- 必殺技: しゃがみ＋攻撃。通常攻撃との同時ヒットを抑止。
- `/ccs skill`、`/ccs ultimate`: 入力経路と技設定の切り分けにも使用。
- BEの選択スロット破棄補完は`controls.bedrock-selected-slot-drop-skill`。GeyserからBukkitイベントが来ない入力は検知できない。
- `/ccsadmin itemlevel <level> [player]`: 利き手の武器・装備のLvを設定。EXPは0にし、限界突破・サブステ抽選結果は維持。範囲外・レベルなしアイテムは拒否。
- 限界突破GUI: 対象を選び、別個体の同名武器を選び、確認する。消費素材は所持品0～35から選択。
- 強化GUI「全て選択」: 対応する実物・数値素材を選択。「この内容で強化」で初めて消費。最大Lvを超える余剰EXPは残らないのでプレビューを確認する。
- `config.yml: damage-chat.enabled: true`: 攻撃者本人のみにダメージをチャット表示。`messages.yml: damage-chat.format`の`<damage>`が属性色・会心・反応名付き表示に置換。

## 安全な反映手順

1. 現在の設定をバックアップし、検証用サーバーで試す。
2. 必要ファイルに定義を追加。ルート`weapons:`などを二重に書かず、既存IDを削除しない。
3. バフ・セットなど参照先も同時に追加。
4. `/ccsadmin reload`で検証し、エラー時はログの該当IDとキーを修正。
5. `/ccsadmin give item <player> <id> 1`で新規個体を確認。
6. 名前、Lore、実ATK、装備レベル条件、技コスト・CT・各段階の効果を確認。

YAML生成は設定作成であって、新しいゲーム処理を実装する機能ではありません。未対応の条件・効果は「プラグイン改修が必要」と回答させてください。運用DBや認証情報をChatGPT用資料に含めないでください。
