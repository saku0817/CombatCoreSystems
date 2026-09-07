# CombatCoreSystems v1.0.0 最終実装仕様書

## 0. 文書目的

本書は、Minecraft Paper向けARPG / オープンワールドサーバー用プラグイン **CombatCoreSystems（CCS）** の実装仕様を定義する。

実装担当のCodeXは本書を最優先仕様として扱い、ゲームシステムに影響する仕様を独自判断で変更・追加しないこと。

内部クラス名、DBテーブル名、キャッシュ方式等の純粋な実装詳細については、本仕様と矛盾しない範囲で適切に設計してよい。

---

# 1. プロジェクト基本情報

- プラグイン名：CombatCoreSystems
- 略称：CCS
- バージョン：1.0.0
- 対象：Paper 26.2
- 用途：ARPG / オープンワールドRPGサーバー
- PvE：対応
- PvP：対応
- Java Edition：対応
- Bedrock Edition：Geyser / Floodgate環境を考慮
- PlaceholderAPI：任意連携

---

# 2. plugin.yml

```yaml
name: CombatCoreSystems
version: 1.0.0
main: com.github.saku0817.combatcoresystems.CombatCoreSystems
api-version: '26.2'
description: "ARPG,オープンワールドサーバー向けに必要な要素を追加するプラグインです。"
authors:
  - s3_q3x

softdepend:
  - PlaceholderAPI

commands:
  ccs:
    description: CombatCoreSystemsの一般コマンドです。
    usage: /ccs

  ccsadmin:
    description: CombatCoreSystemsの管理者コマンドです。
    usage: /ccsadmin
```

Floodgateは `softdepend` へ含めない。

Floodgateが存在する環境ではBedrock判定等に利用できるようにするが、存在しない場合でもCCSは正常起動すること。

---

# 3. 基本設計思想

## 3.1 ARPG設計

原神等のオープンワールド型アクションRPGを参考モデルとする。

ただし完全再現ではなくMinecraftの操作体系・Entity・Attribute・Inventory等へ適応させる。

## 3.2 Minecraft標準機能優先

Minecraft / Paperですでに利用可能な仕組みは可能な限り再利用する。

例：

- Health
- max_health
- attack_damage
- attack_speed
- armor
- Projectile
- Knockback
- Attack Cooldown
- Potion Effect
- Inventory
- Entity AI
- Death / Respawn

独自処理はCCS固有機能に必要な範囲へ限定する。

---

# 4. Kyori Adventure / MiniMessage

表示テキストは原則 **Kyori Adventure Component** を使用する。

対象：

- Chat
- ActionBar
- BossBar
- Sidebar
- GUI
- アイテム名
- Lore
- 通知
- 属性反応
- デバッグ表示

YAML上の装飾文字列には **MiniMessage** を基本採用する。

すべてのCCS管理アイテムで、

- 表示名
- 複数行Lore
- フレーバーテキスト
- 色
- 装飾

を設定可能とする。

---

# 5. プレイヤーレベル

最大：

**Lv100**

BasicステータスはLvに応じて直線成長する。

---

# 6. Basicステータス

| ステータス | Lv1 | Lv100 |
|---|---:|---:|
| HP | 20 | 3000 |
| ATK | 2 | 200 |
| DEF | 0 | 100 |

共通式：

\[
Stat(L)=Start+(End-Start)\times\frac{L-1}{99}
\]

---

# 7. Advancedステータス

| ステータス | 初期値 |
|---|---:|
| 会心ダメージ | 50% |
| 会心率 | 5% |
| 治癒力 | 0% |
| クールタイム | 0% |
| 攻撃速度 | 4 |
| 炎属性ダメージ | 0% |
| 炎属性耐性 | 0% |
| 水属性ダメージ | 0% |
| 水属性耐性 | 0% |
| 風属性ダメージ | 0% |
| 風属性耐性 | 0% |
| 雷属性ダメージ | 0% |
| 雷属性耐性 | 0% |
| 月属性ダメージ | 0% |
| 月属性耐性 | 0% |

通常LvアップではAdvancedは成長しない。

主な入手元：

- 装備
- スキルツリー
- バフ
- 神心
- その他特殊効果

## 上限

- 会心率ステータス自体：上限なし
- 実際の会心判定：100%上限
- 属性耐性：最大100%
- 属性耐性：負数可
- クールタイム：最大100%
- その他：原則グローバル上限なし

---

# 8. Minecraft Attribute連携

## HP

Minecraft `max_health` を使用。

## ATK

Minecraft `attack_damage` を利用可能な範囲で使用。

## DEF

Minecraft `armor` をCCS DEFへ変換。

```text
Armor 1 = CCS DEF 1
```

Minecraft標準Armor軽減とCCS DEF軽減を二重適用しない。

## 攻撃速度

Minecraft `attack_speed` を利用。

---

# 9. HP・死亡・回復

- 現在HPはMinecraft Health
- HP下限0
- HP0で標準死亡
- 独自ダウンなし
- 蘇生なし
- リスポーン時HP100%
- 自然回復なし
- オーバーヒールなし

## 回復式

参照：

- HP
- ATK
- DEF

\[
BaseHeal=ReferenceStat\timesMultiplier
\]

\[
FinalHeal=BaseHeal\times(1+HealingPower)
\]

戦闘中も通常通り回復可能。

---

# 10. ATK計算

\[
ATK=
(PlayerBaseATK+WeaponBaseATK)
\times(1+ATK\%)
+FlatATK
\]

---

# 11. 基礎ダメージ

各攻撃・スキルは、

- HP
- ATK
- DEF

のいずれかを参照可能。

\[
BaseDamage=ReferenceStat\timesMultiplier
\]

標準倍率：

**1.0**

---

# 12. DEF計算

\[
EffectiveDEF=
DEF\times(1-DEFDown)\times(1-DEFIgnore)
\]

防御係数：

\[
DefenseCoefficient=
\frac{AttackerLv+100}
{(AttackerLv+100)+(DefenderLv+100)+EffectiveDEF}
\]

\[
DamageAfterDEF=
DamageBeforeDEF\times DefenseCoefficient
\]

- DEF低下：割合
- DEF無視：割合
- 独立した「貫通ステータス」は作らない

---

# 13. 会心

判定時の実効会心率は最大100%。

\[
CriticalDamage=
PreCriticalDamage\times(1+CriticalDamageStat)
\]

初期50%なら1.5倍。

会心判定は攻撃ヒット時。

---

# 14. 通常ダメージ処理順

基本順：

```text
参照ステータス
↓
倍率
↓
防御
↓
属性
↓
会心
↓
四捨五入
↓
最終ダメージ
```

- 最終ダメージ下限0
- 最終表示は整数
- PvE / PvP共通式

---

# 15. 固定ダメージ

固定ダメージは以下をすべて無視する。

- DEF
- DEF低下
- DEF無視
- 属性ダメージ
- 属性耐性
- 属性耐性低下
- 属性耐性無視
- 会心
- その他通常ダメージ補正

指定値をそのままHPから減算する特殊ダメージとして扱う。

---

# 16. 通常攻撃

## 近接

Minecraft標準左クリック攻撃。

## 遠距離

弓・クロスボウ・トライデント等の標準挙動を流用。

## 共通

- 倍率1.0
- 標準Attack Speed
- 標準Attack Cooldown
- 標準Sweep
- 標準Knockback
- 空振りでも攻撃CT消費
- 標準射程を基本とする
- 必要なら独自射程補正可能
- 通常攻撃は基本無属性

スキル・神心等によって通常攻撃を属性化できる。

---

# 17. 武器システム

## 17.1 カテゴリ

### 近距離

例：

- 剣
- 斧
- その他近接指定武器

### 遠距離

例：

- 弓
- クロスボウ
- トライデント
- その他遠距離指定武器

各武器定義に `melee` / `ranged` 相当のカテゴリを持たせる。

Materialだけへの完全依存は避ける。

## 17.2 レベル

Lv1～100。

## 17.3 レアリティ

★1～★5。

レアリティだけを理由とするグローバル性能倍率は設けない。

## 17.4 メインステータス

2枠固定。

1. 攻撃力
2. 属性攻撃力

属性攻撃力の属性は武器ごとに、

- 炎
- 水
- 風
- 雷
- 月

から指定可能。

武器そのものが属性化されるわけではない。

属性耐性は武器メインステータスに使用不可。

## 17.5 その他

- サブステータスなし
- 固有効果あり
- スキル1あり
- 必殺技あり
- 基本不破壊
- Vanilla Enchantment不可
- CustomModelData対応
- 装備可能Lvの最低・最大条件設定可能
- YAML定義

---

# 18. 武器ATK成長

各武器に、

- Lv1基礎ATK
- Lv100基礎ATK

を設定。

その間はプレイヤーステータス同様に直線成長。

\[
WeaponATK(L)=Start+(End-Start)\frac{L-1}{99}
\]

---

# 19. 武器限界突破

最大：

**5**

状態：

- 0
- 1
- 2
- 3
- 4
- 5

同じ武器を重ねることで進行。

武器Lvとは独立。

各突破段階で個別に、

- 通常攻撃倍率
- スキル倍率
- 必殺技倍率
- 固有効果
- CT
- チャージ
- 持続時間
- 範囲
- Hit数
- 追加攻撃
- 発動条件
- 挙動そのもの

を変更可能。

単純な%強化に限定しない。

---

# 20. 武器必要EXP

Lvが高いほど必要EXPが増加。

標準方式：

**二次増加**

\[
RequiredEXP(L)=Base+Growth\times(L-1)^2
\]

- `Base` はYAML設定可能
- `Growth` はYAML設定可能
- レアリティによる必要EXP差なし

必要ならレベルごとの固定テーブル方式へ差し替え可能。

---

# 21. スキル

各武器：

- 通常攻撃
- スキル1
- 必殺技

## 標準入力

スキル1：

```text
Sneak + Attack
```

必殺技：

```text
Sneak + Use
```

## 仕様

- CT個別設定
- 倍率個別設定
- 初期倍率1.0
- HP / ATK / DEF参照
- 属性設定可能
- 単体 / AoE
- Self / Enemy / Ally
- 詠唱時間なし
- 発動後基本中断なし
- チャージ数設定可能
- 使用条件設定可能

使用条件例：

- HP
- 対象存在
- 距離
- Buff
- Debuff
- Combat State

独立したSP / MP / Energyは作らない。

---

# 22. クールタイム補正

\[
FinalCooldown=
BaseCooldown\times(1-Cooltime)
\]

Cooltime最大100%。

---

# 23. 属性

5属性：

- 炎
- 水
- 風
- 雷
- 月

## 標準カラー

| 属性 | HEX |
|---|---|
| 炎 | `#ff0000` |
| 水 | `#0000cd` |
| 風 | `#3cb371` |
| 雷 | `#4b0082` |
| 月 | `#6495ed` |

設定から変更可能。

適用対象：

- GUI
- ActionBar
- TextDisplay
- Lore
- 属性表示
- 反応表示
- デバッグ
- 表示用Placeholder

---

# 24. 属性ダメージ・耐性

属性ダメージ：

\[
Damage\times(1+ElementDamage)
\]

耐性：

\[
Damage\times(1-FinalResistance)
\]

属性耐性は負値可、最大100%。

---

# 25. 属性耐性低下・無視

順序：

```text
基礎耐性
↓
耐性低下
↓
耐性無視
↓
最終耐性
```

\[
R_1=BaseResistance-ResistanceDown
\]

\[
FinalResistance=
R_1\times(1-ResistanceIgnore)
\]

---

# 26. 属性無効

属性無効が設定された場合：

- 対応属性ダメージ0
- 対応属性付着不可
- その属性を必要とする属性反応不可

---

# 27. 属性付着

標準持続時間：

**5秒**

属性ごとに独立管理。

同じ対象へ複数属性を同時保持可能。

例：

```text
Fire  2.8秒
Water 4.6秒
Moon  1.2秒
```

同属性を再付着した場合：

- 反応しない
- その属性の時間を更新

---

# 28. 複数属性時の反応優先順位

新しい属性が付着した場合、既存付着のうち、

> **最も新しく付着した属性**

とのみ反応判定する。

例：

```text
炎 → 水 → 雷
```

雷付着時：

```text
水 × 雷 → 感電
```

反応成立後：

- 既存側の水を消費
- 新規側の雷も残らない
- 炎は維持

---

# 29. 属性反応

| 組み合わせ | 反応 | 効果 | CT |
|---|---|---|---:|
| 炎×水 | 蒸発 | 炎50% + 水50% | 2.0秒 |
| 炎×風 | 炎風 | 半径2mへ炎100% + 風30%、少し浮上 | 1.5秒 |
| 炎×雷 | 天火 | 炎50% + 雷50%×3Hit | 2.5秒 |
| 炎×月 | 月燃焼 | 炎50% + 月100%、炎耐性-20% 2秒 | 3.0秒 |
| 水×風 | 風解 | 半径2mへ水100% + 風30%、少し浮上 | 1.5秒 |
| 水×雷 | 感電 | 水50% + 雷50%×3Hit | 2.5秒 |
| 水×月 | 月水天 | 水100% + 月50%、水耐性-20% 2秒 | 3.0秒 |
| 風×雷 | 雷飄 | 半径2mへ雷100% + 風30%、少し浮上 | 1.5秒 |
| 風×月 | 月風蝕 | 風150% + 月50%、風耐性-20% 2秒、少し浮上 | 3.0秒 |
| 雷×月 | 月天雷 | 雷30%×3Hit + 月150%、雷耐性-20% 2秒 | 3.0秒 |

1m ≒ 1Minecraft block。

反応CTは対象ごと・反応種類ごとに管理。

---

# 30. 属性反応ダメージ

反応を発生させた攻撃と同じ参照ステータスを使用。

ATK参照攻撃：

```text
ReactionReference = ATK
```

HP参照ならHP。

DEF参照ならDEF。

各属性部分：

\[
Component=
ReferenceStat
\times ReactionMultiplier
\times(1+ElementDamage)
\times(1-FinalResistance)
\]

複数属性部分は個別計算後に合算。

属性反応ダメージは**会心可能**。

---

# 31. Buff / Debuff

## Buff

- Self
- Ally

## Debuff

- Enemy

## 補正

- 固定値
- %

## 持続

- 秒数
- 永続

## Stack

対応。

最大Stackは効果ごとに設定。

同名Buff/Debuffを別個体として重複させない。

再付与方式：

- 時間更新
- Stack追加
- 上書き
- Custom

解除条件：

- 時間切れ
- 死亡
- Logout
- Skill
- 特殊条件
- 永続

専用Debuff Resistanceステータスは作らない。

---

# 32. DoT / HoT / 行動制御

## DoT

設定可能：

- 参照ステータス
- 倍率
- 属性
- Tick間隔
- 持続
- Stack
- 会心可否
- 固定ダメージ

## HoT

通常回復式を使用。

## 行動制御

Minecraft標準機能を優先して、

- Slow
- Immobilize
- Levitation
- Knockback
- Pull

等に対応。

属性付着はBuff/Debuffシステムとは独立管理。

---

# 33. 装備スロット

- 頭
- 胴
- 脚
- 足
- 残響
- 神心

各1枠。

---

# 34. 装備レアリティ

装備は：

- ★3
- ★4
- ★5

のみ。

★1 / ★2は存在しない。

## レアリティ別上限

| レアリティ | 最大Lv | 最大開放サブステ |
|---|---:|---:|
| ★3 | 9 | 2 |
| ★4 | 12 | 3 |
| ★5 | 15 | 4 |

レアリティだけによる固定ステータス倍率はない。

ただし最大Lv・最大開放サブステ数は異なる。

---

# 35. レアリティカラー

| ★ | 色 |
|---|---|
| ★1 | `#ffffff` |
| ★2 | `#55ff55` |
| ★3 | `#5555ff` |
| ★4 | `#aa00aa` |
| ★5 | `#ffaa00` |

設定変更可能。

---

# 36. 部位別メインステータス

## 頭

- HP固定
- HP%
- DEF固定
- DEF%

## 胴

- 会心ダメージ
- 会心率

## 脚

- ATK固定
- ATK%

## 足

- ATK固定
- ATK%
- 会心ダメージ
- 会心率
- HP固定
- HP%
- DEF固定
- DEF%

## 残響

- 炎/水/風/雷/月 属性攻撃力
- 炎/水/風/雷/月 属性耐性

---

# 37. メインステータス成長

各装備ごとに、

- Lv1値
- 最大Lv値

を定義。

その間は直線成長。

\[
MainStat(L)=
Start+
(End-Start)\times
\frac{L-1}{MaxLevel-1}
\]

---

# 38. サブステータス

候補：

- ATK固定
- ATK%
- HP固定
- HP%
- DEF固定
- DEF%
- 会心率
- 会心ダメージ

最大4枠。

同じサブステを複数枠に持たせない。

メインと同種類のサブステは1枠まで共存可能。

---

# 39. サブステ初期値・強化値

割合系：

**5%**

固定値系：

**25**

強化1回ごと：

- 割合系 +5%
- 固定値系 +25

---

# 40. サブステ生成

装備入手時に全候補枠の、

- 種類
- 値

を決定。

未開放でも内容は閲覧可能。

強化対象は完全ランダム。

未開放サブステも強化抽選対象。

同一ステータスへ複数回連続強化可能。

---

# 41. レアリティ別サブステ進行

## ★3

- Lv3：1個開放 + 1回強化
- Lv6：1個開放 + 1回強化
- Lv9：強化のみ

最大2個開放、3回強化。

## ★4

- Lv3：1個開放 + 1回強化
- Lv6：1個開放 + 1回強化
- Lv9：1個開放 + 1回強化
- Lv12：強化のみ

最大3個開放、4回強化。

## ★5

- Lv3：1個開放 + 1回強化
- Lv6：1個開放 + 1回強化
- Lv9：1個開放 + 1回強化
- Lv12：1個開放 + 1回強化
- Lv15：強化のみ

最大4個開放、5回強化。

---

# 42. 装備セット

対象：

- 頭
- 胴
- 脚
- 足

残響は対象外。

対応：

- 2セット
- 4セット

4セット成立時：

```text
2セット効果 + 4セット効果
```

を同時発動。

---

# 43. Minecraftインベントリとの装備同期

頭・胴・脚・足はMinecraft標準Armor Slotから直接装備可能。

CCS装備GUIと**双方向同期**する。

```text
Minecraft Armor Slot
↕
CCS Equipment GUI
```

別々の装備状態を持たない。

バニラ防具の場合はArmor値をCCS DEFへ1:1変換。

## 戦闘中

以下を禁止：

- Armor Slot着脱
- Shift Clickによる着脱
- Drag
- Slot交換
- CCS GUIでの変更

戦闘中は装備状態を固定。

残響・神心はCCS GUI専用。

---

# 44. 残響

- 1枠
- 通常装備扱い
- レアリティ★3～★5
- 最大Lvはレアリティ依存
- サブステあり
- セット対象外
- 属性攻撃 / 属性耐性をメインに設定可能

---

# 45. 神心

特殊装備。

- 1枠
- 固定レアリティ
- Lvなし
- 限界突破なし
- サブステ成長なし
- 入手時点で完成

単純なステータス装備ではなく、戦闘ルール変更可能。

例：

- 通常攻撃属性化
- Skill属性変更
- Ultimate属性変更
- 倍率変更
- CT変更
- AoE化
- 追加攻撃
- 属性反応強化
- 回復
- Barrier
- Stat変換
- その他特殊処理

---

# 46. 装備必要EXP

武器と同様に二次増加。

\[
RequiredEXP(L)=Base+Growth\times(L-1)^2
\]

- Base / Growth YAML変更可能
- レアリティ差なし
- 固定EXPテーブルへの切替可能

---

# 47. 強化素材システム

完全に3系統へ分離。

1. プレイヤー
2. 武器
3. 装備

各3ランク：

- 下級
- 中級
- 上級

標準EXP：

- 下級：100
- 中級：1,000
- 上級：10,000

YAMLから変更可能。

---

# 48. 初期強化素材

## プレイヤー

| ID | 名前 | Material | EXP |
|---|---|---|---:|
| `player_exp_low` | 初級修練書 | PAPER | 100 |
| `player_exp_mid` | 中級修練書 | BOOK | 1,000 |
| `player_exp_high` | 上級修練書 | ENCHANTED_BOOK | 10,000 |

## 武器

| ID | 名前 | Material | EXP |
|---|---|---|---:|
| `weapon_exp_low` | 粗製強化鉱 | RAW_IRON | 100 |
| `weapon_exp_mid` | 精製強化鉱 | IRON_INGOT | 1,000 |
| `weapon_exp_high` | 高純度強化鉱 | DIAMOND | 10,000 |

## 装備

| ID | 名前 | Material | EXP |
|---|---|---|---:|
| `equipment_exp_low` | 微光の繊維 | STRING | 100 |
| `equipment_exp_mid` | 輝光の繊維 | GLOWSTONE_DUST | 1,000 |
| `equipment_exp_high` | 星光の繊維 | PRISMARINE_CRYSTALS | 10,000 |

---

# 49. 強化素材利用

- 専用GUI
- 複数個一括使用
- 自動選択
- 一括投入
- 余剰EXPは次Lvへ繰越
- 最大Lv到達後の余剰は破棄
- 強化失敗なし
- 金銭等の追加コストなし
- Material変更可能
- CustomModelData対応
- Lore対応
- 管理者Give対応

---

# 50. プレイヤーEXP

Minecraft標準EXP獲得を使用。

必要EXP：

**Minecraft標準必要EXPの5倍**

Lv100で余剰破棄。

死亡EXPペナルティなし。

Lv差補正なし。

強化素材からもプレイヤーEXPを取得可能。

---

# 51. 新生回帰

実行条件：

**Lv100**

追加コスト：

なし。

GUI確認：

1回。

実行後：

- Lv1
- EXP初期状態
- 現在HPを新しい最大HPまで全回復

永久補正1回につき：

- HP +100
- ATK +20
- DEF +10

回数上限なし。

\[
HP=LevelHP+100R
\]

\[
ATK=LevelATK+20R
\]

\[
DEF=LevelDEF+10R
\]

維持：

- 武器
- 武器Lv
- 限界突破
- 装備
- 装備Lv
- 神心
- スキルツリー
- ポイント
- その他育成データ

戦闘中実行不可。

---

# 52. スキルツリー

- 複数ツリー
- 初期から利用可能
- Inventory GUI
- YAML定義
- 分岐あり
- 前提ノードあり
- 排他分岐可能
- ノード複数段階強化可能

## ポイント

Lvアップ1回：

**1pt**

初回Lv1→100：

最大99pt。

新生回帰後、すでにポイント付与済みのレベル到達では再取得不可。

ポイント獲得履歴を保存。

最大所持数：

原則なし。

通常Lv育成由来は最大99pt。

管理者編集可能。

---

# 53. スキルツリーノード

分類：

1. Basic
2. Advanced
3. Physical
4. Special

## Basic

- HP
- ATK
- DEF

## Advanced

- 会心
- 治癒
- CT
- 攻撃速度
- 属性関連等

## Physical

例：

- 移動速度
- ジャンプ
- Knockback Resistance
- 落下軽減
- Swim Speed
- Mining Speed

## Special

汎用Passive。

禁止：

- 特定武器専用強化
- 特定神心専用強化
- Skill倍率変更
- Skill範囲変更
- Skill属性変更

---

# 54. スキルツリー条件

取得条件として、

- 前提ノード
- Lv
- 新生回帰回数
- その他指定条件

を設定可能。

ノードごとに必要ポイント個別設定。

段階ごとのコスト・効果量も個別設定可能。

---

# 55. スキルツリープリセット

最大：

**5**

切替：

**24時間CT**

リセット：

- 無料
- 無制限
- 全ポイント返却
- 24時間CT

プリセット切替も24時間CT対象とし、リセットCT回避に利用できないようにする。

---

# 56. Mob PvE

- Vanilla Mobも基本CCS対象
- 個別除外可能
- Mob Lvあり
- Lv上限なし
- HP = max_health
- ATK = attack_damage
- DEF = armorをCCS DEF変換
- 属性系Advanced保持可能
- Native Element設定可能
- Element Immunity可能
- CCS Skill可能
- Mob→PlayerもCCS式
- Player→MobもCCS式
- Mob→MobもCCS式

外部Mobプラグインとの専用連携は実装しない。

---

# 57. Mob Lv決定方式

Mob定義ごとに選択可能：

- 固定Lv
- 範囲ランダム
- ワールド基準
- リージョン基準
- 周辺プレイヤーLv基準

各Mobに、

- Min Lv
- Max Lv

設定可能。

ワールド / リージョンにもLv帯設定可能。

HP / ATK / DEFは、

- Lv成長
- 固定設定

の両方に対応。

成長式はMobごとに設定可能。

---

# 58. Boss

Bossは通常Mobと別定義。

Lv：

- 固定
- 範囲指定

を基本とする。

対応：

- 特殊Stat
- Native Element
- Immunity
- Skill
- Phase
- Phase別Stat
- Phase別Skill
- Phase条件
- 特殊Drop
- Respawn

---

# 59. Mob / Boss Lv表示

名前等へLv表示可能。

ON/OFF設定。

表示形式変更可能。

例：

```text
Lv.42 ゾンビ
```

---

# 60. Mob AI

Minecraft標準AIを基本使用。

標準側：

- Move
- Pathfinding
- Target
- Chase
- Normal Attack

CCS：

- 特殊Skill
- Element
- Boss Phase
- 特殊条件

CCSが全Mobの独自AIを毎Tick完全制御しない。

---

# 61. リージョン自動スポーン

CCS独自Region内へMob/Bossを自動スポーン可能。

設定可能：

- Region ID
- Mob/Boss ID
- 同時存在上限
- Spawn Interval
- 1回のSpawn数
- Lv決定方式
- Lv範囲
- Respawn CT
- 必要なSpawn条件
- Despawn設定

Region内の安全な地点を選択してSpawn。

全Entityの毎回全走査は避ける。

---

# 62. 自動スポーンMobのDespawn

- Region周辺にPlayer不在 → 60秒後Despawn
- Playerが戻れば待機解除
- Combat中MobはDespawnしない
- Bossは標準では自動Despawnしない
- CCS自動Spawn個体は内部タグで識別
- Despawn時Dropなし
- Despawn時EXPなし
- 再Spawn時は原則新規個体
- Regionごとに設定上書き可能

Chunk Unload時：

- 永続対象なら安全に保存
- 非永続対象なら安全に破棄

---

# 63. PvP

デフォルトON。

Server設定から変更可能。

PvP条件：

> 両方のPlayerがPvP ONの場合のみ成立。

- PvEと同一ダメージ式
- 標準Knockback
- 標準Invulnerability Frame
- 回復通常
- Buff/Debuff共通
- Element Reaction共通
- Lv差補正なし
- 装備差補正なし
- 専用Duelなし
- Self Attack Damageなし
- Self Buff/Heal可能
- 同Party Friendly Fire無効

---

# 64. CCS Region

WorldGuard風の独自Region管理機能。

形状：

**Cuboidのみ**

## Y座標

任意。

Y指定なし：

```text
X/Z指定範囲の全Y
```

として扱う。

## 優先順位

- World設定よりRegion設定優先
- Region重複時はより具体的・狭いRegionを優先
- 境界座標はRegion内

1Worldに複数Region登録可能。

各Regionは一意ID。

`regions.yml` で管理。

WorldGuard等との専用連携は行わない。

---

# 65. Region選択

WorldGuard風に2点選択できる管理機能を用意。

例：

```text
/ccsadmin region wand
```

- Pos1
- Pos2
- Create
- Delete
- Info
- List
- Flag

全高Regionも作成可能。

WandのMaterial等は設定可能とする。

---

# 66. Party

CCSに簡易Party Systemを内蔵。

最大人数：

**4人**

招待有効時間：

**60秒**

機能：

- Create
- Invite
- Accept
- Decline
- Leave
- Kick
- Leader Transfer
- Disband
- Member List
- Party Chat
- GUI

招待は複数同時受信可能。

---

# 67. Party Leader

Leader可能操作：

- Invite
- Kick
- Disband
- Leader Transfer

Leader離脱時：

1. Online残存メンバーを優先
2. その中で参加順が最も古いPlayer
3. Onlineが1人もいない場合はOfflineを含め最古参加者

へ自動移譲。

---

# 68. Party永続化

Offlineメンバーを保持。

Server Restart後も復元。

1人だけになっても自動解散しない。

最後の1人が明示的に解体した場合にParty削除。

保存：

- Party ID
- Leader UUID
- Member UUID
- Join Order
- 必要な管理情報

Partyの戦闘効果は基本的に、

**同Party Friendly Fire無効**

のみ。

---

# 69. 戦闘状態

開始：

- 攻撃がHit
- 被弾

空振りだけでは開始しない。

解除：

最後の有効戦闘イベントから**10秒**。

新しい攻撃・被弾で再度10秒。

Death：

即解除。

Respawn時非戦闘。

---

# 70. 戦闘中武器固定

登録：

- 近距離武器1
- 遠距離武器1

戦闘中、この登録2種類の間は切替可能。

他の近距離 / 遠距離武器へ変更不可。

複数武器をInventoryに持っていても、

- ATK
- Passive
- Skill
- Element Bonus

等を重複適用しない。

---

# 71. Combat開始時の武器判定

自分から攻撃して開始：

最初に使用した有効武器。

被弾から開始：

最後に使用していた有効登録武器。

---

# 72. 戦闘中装備制限

変更禁止：

- 頭
- 胴
- 脚
- 足
- 残響
- 神心
- 登録外武器

通常Inventoryからの装備操作も防止。

---

# 73. Combat Logout Penalty

違反回数：

| 回数 | 処理 |
|---:|---|
| 1 | 警告 |
| 2 | 警告 |
| 3 | 警告 |
| 4 | Player Lv -1 |

4回目後：

```text
Violation = 0
```

Lv下限1。

新生回帰回数・永久補正には影響なし。

---

# 74. Sidebar HUD

標準表示：

```text
(設定したサーバー名)
(ユーザー名)

Lv.80
次のレベルまで、あと○○

HP 2450 / 3000
```

内容・順序・文章・装飾は設定可能。

Sidebar自体を：

- Server全体でOFF
- Player個別でOFF

可能。

TAB等の外部HUDプラグイン利用を前提として共存可能にする。

---

# 75. ActionBar

戦闘時標準例：

```text
⚔ 戦闘中 8.4秒 | スキル: あと3.2秒 | 必殺技: 発動可能 ⚔
```

属性付着例：

```text
🔥 炎 3.4秒 | 🌙 月 4.1秒
```

表示：

- 内容
- 順序
- 区切り
- 表記
- 色
- Ready文言

等を設定可能。

---

# 76. HUD更新

値変更：

**即時**

時間系：

**5Tickごと**

例：

- Combat残り時間
- Skill CT
- Buff残り時間
- Element残り時間

変化なしの場合は不要な再描画を避ける。

---

# 77. BossBar

常時HUD用途には使わない。

主に：

- Boss HP
- Boss特殊ギミック

へ使用。

---

# 78. Damage / Heal TextDisplay

方式：

**TextDisplay**

表示位置：

対象Entity頭上付近。

表示時間：

**0.8秒**

少し上昇しながら消える。

## 通常

```text
124
```

## Critical

通常より大きく、

```text
CRIT 286
```

または設定文。

## Heal

```text
+450
```

## Reaction

```text
感電 320
```

## DoT / HoT

通常より小さく、短時間表示。

## 上限

1Playerあたり同時最大：

**20個**

Player / Mob / Boss共通。

個別ON/OFF可能。

---

# 79. 総合メニュー

```text
/ccs menu
```

から開く。

Inventory GUI。

主要項目：

- Stats
- Equipment
- Skill Tree
- Rebirth
- Party
- Enhancement
- Encyclopedia
- Settings

各GUIから総合メニューへ戻れる。

戦闘中は一般 `/ccs` コマンド禁止のためMenuも利用不可。

---

# 80. Equipment GUI

表示対象：

- Melee Weapon
- Ranged Weapon
- Head
- Chest
- Legs
- Feet
- Resonance
- Divine Heart

Head/Chest/Legs/FeetはMinecraft Inventoryと同期。

---

# 81. Stats GUI

表示：

- Player Lv
- EXP
- Rebirth
- Basic
- Advanced
- Element Stats

---

# 82. Skill Tree GUI

- Inventory GUI
- 54Slotを基本
- Page対応
- Tree切替
- Node表示
- Preset
- Reset
- Main Menuへ戻る

---

# 83. Rebirth GUI

表示：

- 現在Lv
- Rebirth回数
- 現在の永久補正
- 次回補正
- 実行条件

実行前に1回確認GUI。

---

# 84. Party GUI

最大4人を一覧表示。

表示：

- Player Name
- Leader
- Online / Offline
- Lv
- HP
- Join Order

Leader：

- Kick
- Transfer
- Invite
- Disband

Invite：

Online Player一覧から選択可能。

受信Invite一覧も表示。

Party Chat切替対応。

---

# 85. 図鑑

コマンド：

```text
/ccs encyclopedia
```

または総合メニュー。

Inventory GUI。

標準カテゴリ：

- 武器
- 装備
- 神心
- 強化素材
- 敵Mob
- 敵Boss

YAMLから新規カテゴリ追加可能。

---

# 86. 図鑑公開方式

## 最初から全公開

- 武器
- 装備
- 神心
- 強化素材
- その他追加カテゴリ

## 発見型

- Mob
- Boss

未発見：

```text
？？？
```

---

# 87. Mob / Boss図鑑アンロック

アンロック条件：

- Playerが対象を攻撃
- または対象からPlayerが攻撃される

どちらか成立した時点。

定義ID単位。

UUID個体単位ではない。

アンロック後永続保存。

初回登録通知可能。

---

# 88. 未発見図鑑情報

未発見Mob/Bossでは、

- Name
- ID
- Lv
- HP
- ATK
- DEF
- Element
- Resistance
- Skill
- Drop
- Region
- Phase

等を非表示。

一覧には `？？？` として存在する。

---

# 89. 図鑑GUI

トップ：

**54Slot**

カテゴリ一覧：

**54Slot**

1ページ：

最大45項目程度。

下段：

- Previous
- Next
- Back
- Search
- Filter

詳細GUI：

**54Slot**

情報量が多ければPage分割。

すべてMain Menuへ戻れる。

図鑑アイコン：

`KNOWLEDGE_BOOK`

---

# 90. 図鑑並び替え

標準：

1. レアリティ
2. IDまたは設定順

GUIから並び順変更可能。

---

# 91. 図鑑検索

名前検索。

Anvil GUIは使用せず**Chat入力**。

Java / Bedrock共通。

処理：

```text
検索ボタン
↓
GUIを一時閉じる
↓
Chatへ入力
↓
図鑑GUI再表示
```

未発見Mob/Bossは実名検索にHitしない。

GUIを完全に閉じた場合は検索状態をReset。

---

# 92. 図鑑Filter

対応：

- レアリティ
- 武器カテゴリ
- 装備部位
- Mob/Boss属性
- 発見済み / 未発見

複数Filter同時使用可能。

検索との併用可能。

---

# 93. 図鑑詳細：武器

- Name
- ID
- Rarity
- Material
- Melee / Ranged
- Lv1 Base ATK
- Lv100 Base ATK
- Element Damage Type
- Element Damage
- Passive
- Skill
- Ultimate
- Limit Break 0～5
- Equip Lv条件
- Lore

---

# 94. 図鑑詳細：装備

- Name
- ID
- Rarity
- Slot
- Max Lv
- Main Stat
- Lv1 Main Value
- Max Lv Main Value
- Substat候補
- Set ID
- 2Set
- 4Set
- Lore

---

# 95. 図鑑詳細：神心

- Name
- ID
- Rarity
- Effect概要
- Rule Change
- Trigger
- Stat Modifier
- Lore

---

# 96. 図鑑詳細：素材

- Name
- ID
- 系統
- Rank
- EXP
- Material
- Lore

---

# 97. 図鑑詳細：Mob

- Name
- ID
- Lv方式
- Lv範囲
- HP
- ATK
- DEF
- Element
- Resistance
- Immunity
- Skill
- Drop
- EXP
- World
- Region
- Description

---

# 98. 図鑑詳細：Boss

Mob項目に加え：

- Boss Ability
- Phase
- Phase Condition
- Special Skill
- Special Drop
- Spawn概要
- Respawn CT

---

# 99. 図鑑Custom Category

`encyclopedia.yml` から、

- ID
- Name
- Icon
- Description
- Lore
- 任意Fields

を設定可能。

標準では最初から公開。

---

# 100. Java / Bedrock操作互換

Floodgate APIを利用可能な場合はBedrock Playerを識別。

CCS本体はFloodgateなしでも起動。

Java限定必須操作を作らない。

Skill：

- Sneak + Attack
- Sneak + Use

Inventory GUI：

Java / Bedrock共通。

Controller専用補助なし。

Touch専用UIなし。

---

# 101. 保存方式

対応候補：

- YAML
- JSON
- SQLite
- MySQL
- MariaDB

デフォルト：

**SQLite**

主キー：

**UUID**

---

# 102. 保存対象

Player：

- Lv
- EXP
- Rebirth
- HP
- Weapon
- Weapon Lv
- Limit Break
- Equipment
- Equipment Lv
- Substats
- Resonance
- Divine Heart
- Skill Tree
- Skill Points
- Presets
- Point Grant History
- PvP
- HUD
- Controls
- Combat Logout Count
- Buff/Debuff
- Element Attachment
- Encyclopedia
- Party関係

Mob/Boss：

必要な永続個体状態。

Combat State自体は保存しない。

---

# 103. Logout中の時間

Logout中：

- Buff/Debuff Timer停止
- Element Attachment Timer停止

再Login後、残り時間から再開。

---

# 104. Auto Save

**20分ごと**

通常のLvUp・装備変更等では即時保存しない。

`/ccsadmin save`：

オンラインおよびメモリ保持中のデータを即時保存。

Shutdown：

未保存データを保存。

---

# 105. Async I/O

DB / File I/Oは原則非同期。

ただしBukkit/Paper Entity・Inventory等をAsync Threadから直接危険に操作しない。

基本：

```text
Main ThreadでSnapshot
↓
DTO / Serializable Data
↓
Async I/O
```

---

# 106. DB障害

- 未保存データをMemory保持
- 30秒ごと再試行
- 時間上限なし
- 復旧後再保存
- 保存失敗をConsoleへ通知
- 権限保持Adminにも通知

---

# 107. Backup

自動：

**12時間ごと**

保持：

**7世代**

保存先：

```text
plugins/CombatCoreSystems/backups/
```

7世代超過：

最古から削除。

命名：

```text
cssbackup_yyyy-MM-dd_HH-mm-ss
```

対象：

- Player Data
- Party
- Mob/Boss persistent data
- CCS Config/YAML definitions

---

# 108. Backup方式

SQLite：

整合性を維持したSnapshot。

YAML / JSON：

Data Directoryをまとめて保存。

MySQL / MariaDB：

CCS独自Export形式を用意。

手動：

```text
/ccsadmin backup create
```

復元：

```text
/ccsadmin backup restore <id>
```

Restore前には現状態を自動Backup。

---

# 109. data-version

各永続データに整数型 `data-version` を保持。

例：

```yaml
data-version: 1
```

Plugin Versionとは分離。

旧Version：

```text
v1 → v2 → v3
```

のように段階的Migration。

Migration前に自動Backup。

保存データVersionがPlugin対応Versionより新しい場合：

- 読込停止
- 上書き禁止
- ERROR
- Admin通知
- 元データ保持

---

# 110. YAML構成

```text
plugins/CombatCoreSystems/
├─ config.yml
├─ messages.yml
├─ storage.yml
├─ levels.yml
├─ reactions.yml
├─ buffs.yml
├─ sets.yml
├─ gui.yml
├─ weapons.yml
├─ equipment.yml
├─ divine_hearts.yml
├─ skill_trees.yml
├─ mobs.yml
├─ bosses.yml
├─ regions.yml
├─ spawns.yml
└─ encyclopedia.yml
```

---

# 111. YAML用途

## config.yml
Global settings。

## messages.yml
MiniMessage対応メッセージ。

## storage.yml
Storage / DB。

## levels.yml
Player Lv / EXP / Rebirth / growth。

## reactions.yml
Element reactions。

## buffs.yml
Buff / Debuff / DoT / HoT。

## sets.yml
Equipment set。

## gui.yml
GUI / HUD layout。

## weapons.yml
Weapons + Skill + Ultimate。

## equipment.yml
Head / Chest / Legs / Feet / Resonance。

## divine_hearts.yml
Divine Heart。

## skill_trees.yml
Skill Tree。

## mobs.yml
Mob definitions。

## bosses.yml
Boss definitions。

## regions.yml
CCS Region。

## spawns.yml
Auto Spawn。

## encyclopedia.yml
Encyclopedia category / layout / custom entries。

`skills.yml` は作らない。

`rarities.yml` は作らない。

主要定義はすべて一意ID。

必要なら個別ファイル分割にも対応可能。

---

# 112. Reload

```text
/ccsadmin reload
```

Reload可能：

- messages
- levels
- reactions
- buffs
- sets
- gui
- weapons
- equipment
- divine hearts
- skill trees
- mobs
- bosses
- regions
- spawns
- encyclopedia
- HUD
- element colors
- rarity colors
- Placeholder表示
- PvP regions

Reload時：

1. Load
2. Syntax validation
3. ID validation
4. Reference validation
5. Value validation
6. Successful definitionsのみ反映

既存正常状態を壊さない。

---

# 113. Restart必須設定

主に：

- Storage backend変更
- SQLite ↔ MySQL/MariaDB
- DB connection情報
- plugin.yml
- Dependency structure
- Data schema基幹変更
- API互換性に関わる変更

---

# 114. 一般コマンド

```text
/ccs
/ccs help
/ccs menu

/ccs open stats
/ccs open equipments
/ccs open skilltree
/ccs open rebirth
/ccs open party

/ccs encyclopedia

/ccs setting pvp on
/ccs setting pvp off
/ccs setting hud
/ccs setting controls
```

---

# 115. Partyコマンド

```text
/ccs party create
/ccs party invite <player>
/ccs party accept <player>
/ccs party decline <player>
/ccs party leave
/ccs party kick <player>
/ccs party leader <player>
/ccs party disband
/ccs party list
/ccs party chat <message>
```

---

# 116. Debugコマンド

```text
/ccs debug on
/ccs debug on <duration>
/ccs debug off
/ccs debug schedule <開始まで> [継続時間]
/ccs debug status
/ccs debug cancel
```

`/ccsadmin debug` は使用しない。

`/ccs debug` はAdminまたは専用Permission保持者のみ。

---

# 117. Admin Player編集

```text
/ccsadmin edit level <player> set <value>
/ccsadmin edit level <player> add <value>
/ccsadmin edit level <player> remove <value>

/ccsadmin edit exp <player> set <value>
/ccsadmin edit exp <player> add <value>
/ccsadmin edit exp <player> remove <value>

/ccsadmin edit buff add <player> <id>
/ccsadmin edit buff remove <player> <id>

/ccsadmin edit element add <player> <id>
/ccsadmin edit element remove <player> <id>

/ccsadmin edit force on <player>
/ccsadmin edit force off <player>
```

Offline Player編集対応。

---

# 118. Admin Give

```text
/ccsadmin give item <player> <id> <amount>
```

対象：

- Weapon
- Equipment
- Divine Heart
- Enhancement Material
- その他CCS Item

---

# 119. Admin Mob/Boss

```text
/ccsadmin spawn mob <x> <y> <z> <id>
/ccsadmin spawn boss <x> <y> <z> <id>
```

相対座標：

```text
~ ~ ~
```

対応。

---

# 120. Admin Region

例：

```text
/ccsadmin region wand
/ccsadmin region pos1
/ccsadmin region pos2
/ccsadmin region create <id>
/ccsadmin region create <id> --full-height
/ccsadmin region delete <id>
/ccsadmin region info <id>
/ccsadmin region list
/ccsadmin region flag <id> pvp allow
/ccsadmin region flag <id> pvp deny
```

WorldGuard風に分かりやすく操作できること。

---

# 121. Admin Backup

```text
/ccsadmin backup create
/ccsadmin backup restore <id>
/ccsadmin backup list
```

---

# 122. Admin Encyclopedia

```text
/ccsadmin encyclopedia unlock <player> mob <id>
/ccsadmin encyclopedia unlock <player> boss <id>

/ccsadmin encyclopedia reset <player> mob <id>
/ccsadmin encyclopedia reset <player> boss <id>

/ccsadmin encyclopedia unlock <player> all
/ccsadmin encyclopedia reset <player> all
```

---

# 123. System Admin

```text
/ccsadmin reload
/ccsadmin save
```

---

# 124. Combat中コマンド制限

Combat State中：

**一般 `/ccs` コマンドをすべて禁止。**

Admin commandは制限対象外。

---

# 125. Tab Completion

以下を補完：

- Command
- Subcommand
- Player
- Item ID
- Mob ID
- Boss ID
- Region ID
- Buff ID
- Element
- set/add/remove
- on/off
- その他候補値

---

# 126. Permission

一般：

```text
combatcoresystems.command.*
combatcoresystems.command.help
combatcoresystems.command.menu
combatcoresystems.command.open.stats
combatcoresystems.command.open.equipments
combatcoresystems.command.open.skilltree
combatcoresystems.command.open.rebirth
combatcoresystems.command.open.party
combatcoresystems.command.encyclopedia
combatcoresystems.command.setting.pvp
combatcoresystems.command.setting.hud
combatcoresystems.command.setting.controls
combatcoresystems.command.party
```

一般機能は原則 `default: true`。

---

# 127. Debug Permission

```text
combatcoresystems.command.debug
combatcoresystems.debug.*
combatcoresystems.debug.player
combatcoresystems.debug.mob
combatcoresystems.debug.damage
combatcoresystems.debug.critical
combatcoresystems.debug.element
combatcoresystems.debug.reaction
combatcoresystems.debug.buff
combatcoresystems.debug.skill
combatcoresystems.debug.combat
combatcoresystems.debug.storage
combatcoresystems.debug.performance
```

原則OPまたは明示付与。

---

# 128. Admin Permission

```text
combatcoresystems.admin.*
combatcoresystems.admin.level
combatcoresystems.admin.exp
combatcoresystems.admin.give
combatcoresystems.admin.spawn
combatcoresystems.admin.buff
combatcoresystems.admin.element
combatcoresystems.admin.force
combatcoresystems.admin.region
combatcoresystems.admin.backup
combatcoresystems.admin.encyclopedia
combatcoresystems.admin.reload
combatcoresystems.admin.save
combatcoresystems.admin.storage.alert
```

原則 `default: op`。

---

# 129. PlaceholderAPI

PlaceholderAPIは任意依存。

未導入：

- CCS正常起動
- Placeholder連携だけ無効

名前空間：

```text
%ccs_...%
```

取得時は可能な限りCache済み値を返し、その場で全Statを再計算しない。

---

# 130. Placeholder値形式

生値：

数値のみ。

表示用：

設定可能な文言・装飾を返せる。

Color付きとRawを分離。

小数：

用途別。

基本：

- Integer
- または小数1桁

---

# 131. Placeholder基本一覧

例：

```text
%ccs_player_name%

%ccs_level%
%ccs_exp%
%ccs_exp_required%
%ccs_exp_remaining%
%ccs_rebirth_count%
%ccs_skill_points%

%ccs_hp%
%ccs_max_hp%
%ccs_atk%
%ccs_def%

%ccs_crit_rate%
%ccs_crit_damage%
%ccs_healing_power%
%ccs_cooltime%
%ccs_attack_speed%

%ccs_fire_damage%
%ccs_fire_resistance%
%ccs_water_damage%
%ccs_water_resistance%
%ccs_wind_damage%
%ccs_wind_resistance%
%ccs_thunder_damage%
%ccs_thunder_resistance%
%ccs_moon_damage%
%ccs_moon_resistance%

%ccs_combat%
%ccs_combat_display%
%ccs_combat_remaining%

%ccs_skill_ready%
%ccs_skill_status%
%ccs_skill_status_display%
%ccs_skill_cooldown%
%ccs_skill_charges%

%ccs_ultimate_ready%
%ccs_ultimate_status%
%ccs_ultimate_status_display%
%ccs_ultimate_cooldown%
%ccs_ultimate_charges%

%ccs_melee_weapon%
%ccs_melee_weapon_level%
%ccs_melee_weapon_limitbreak%

%ccs_ranged_weapon%
%ccs_ranged_weapon_level%
%ccs_ranged_weapon_limitbreak%

%ccs_head%
%ccs_chest%
%ccs_legs%
%ccs_feet%
%ccs_resonance%
%ccs_divine_heart%

%ccs_element_count%
%ccs_elements%
%ccs_elements_display%

%ccs_buff_count%
%ccs_debuff_count%
%ccs_buffs%
%ccs_debuffs%

%ccs_party%
%ccs_party_id%
%ccs_party_leader%
%ccs_party_size%
%ccs_party_max_size%
%ccs_party_is_leader%

%ccs_pvp%
%ccs_pvp_display%
```

---

# 132. Buff ID Placeholder

必要な主要効果について動的ID型も対応可能。

```text
%ccs_buff_<id>_active%
%ccs_buff_<id>_stacks%
%ccs_buff_<id>_remaining%
```

---

# 133. Encyclopedia Placeholder

```text
%ccs_encyclopedia_mob_discovered%
%ccs_encyclopedia_mob_total%
%ccs_encyclopedia_boss_discovered%
%ccs_encyclopedia_boss_total%
%ccs_encyclopedia_discovered%
%ccs_encyclopedia_total%

%ccs_encyclopedia_progress%
%ccs_encyclopedia_mob_progress%
%ccs_encyclopedia_boss_progress%
```

現在の一覧は**v1基本Placeholder一覧**。

今後追加可能。

公開済みPlaceholderの名前変更・削除は極力避ける。

---

# 134. Public API

API root：

```text
com.github.saku0817.combatcoresystems.api.v1
```

破壊的変更時のみ：

```text
api.v2
```

等を追加。

内部Manager等を直接公開せず、Interface / DTOを中心にする。

---

# 135. Public API分野

- Player
- Combat
- Damage
- Heal
- Element
- Reaction
- Buff
- Debuff
- Weapon
- Equipment
- Divine Heart
- Skill
- Level
- Rebirth
- Party
- Mob
- Boss
- Encyclopedia
- Events

---

# 136. Damage API

外部PluginからCCS式でDamage発生可能。

指定可能：

- Attacker
- Target
- Reference Stat
- Multiplier
- Element
- Crit可否
- Fixed Damage
- Source
- その他必要Context

---

# 137. Equipment API

取得・変更：

- Melee Weapon
- Ranged Weapon
- Weapon Lv
- Limit Break
- Head
- Chest
- Legs
- Feet
- Resonance
- Divine Heart
- Set

ただし通常CCSルールを経由する。

Combat装備Lockを無視して直接内部状態を書き換えない。

---

# 138. Party API

可能：

- Create
- Invite
- Join
- Leave
- Kick
- Transfer
- Disband
- Member取得
- Same Party判定
- Party ID取得

---

# 139. Encyclopedia API

可能：

- Discovered判定
- Unlock
- Reset
- Discovered一覧
- Count
- Total Count

---

# 140. Custom Events

公開候補：

- BeforeDamage
- AfterDamage
- Critical
- Heal
- ElementAttach
- ElementReaction
- BuffApply
- BuffRemove
- DebuffApply
- DebuffRemove
- CombatStart
- CombatEnd
- LevelUp
- Rebirth
- WeaponChange
- EquipmentChange
- SkillActivate
- UltimateActivate
- MobDeath
- BossDeath
- BossPhaseChange
- EncyclopediaUnlock
- EncyclopediaReset

用途に応じてCancelable / Mutable。

---

# 141. Debug Mode

ON/OFF可能。

実行者単位。

再起動時：

**OFFへ戻す**

予約も再起動後保持しない。

---

# 142. Debug Schedule

```text
/ccs debug on
/ccs debug on 10m
/ccs debug off

/ccs debug schedule <開始まで> [継続時間]

/ccs debug status
/ccs debug cancel
```

1Userにつき予約1件。

---

# 143. Debug Target

対応：

- `self`
- `player <name>`
- `mob <UUID>`
- `boss <UUID>`

---

# 144. Debug Category

最低限：

- all
- player
- damage
- critical
- element
- reaction
- buff
- skill
- weapon
- equipment
- combat
- mob
- boss
- level
- skilltree
- storage
- performance

将来追加可能。

---

# 145. Debug内容

確認可能：

## Player
- Base
- Modifiers
- Final

## Damage
- 各計算過程

## Critical
- Crit Rate
- Random
- Result

## Element
- Attachment
- Remaining
- Resistance

## Reaction
- Name
- CT
- Calculation

## Buff/Debuff
- ID
- Stack
- Remaining
- Source

## Skill
- CT
- Charge
- Conditions
- Availability

## Weapon
- Registered Weapon
- Lv
- Limit Break
- Active Effects

## Equipment
- Main
- Sub
- Set
- Divine Heart

## Combat
- State
- Remaining
- Locked Weapons

## Mob/Boss
- ID
- Lv
- HP
- ATK
- DEF
- Element
- Phase

## EXP
- Current
- Required
- Rebirth

## Skill Tree
- Points
- Nodes
- Preset

## Storage
- Last Save
- Backend
- Pending Save

## Performance
- TPS
- CCS処理時間
- Entity Count
- 各管理件数

---

# 146. Debug GUI

54Slot Inventory GUI。

基本閲覧専用。

値編集はAdmin Command側。

---

# 147. Debug Logs

保存先：

```text
plugins/CombatCoreSystems/logs/debug/
```

出力：

- Player
- Console
- File

日次Rotation。

---

# 148. Performance基本方針

- 毎Tick処理最小化
- 1 / 5 / 20Tickを用途別に利用
- 全Entity走査回避
- Stat / Equipment / ConfigをCache
- Stat再計算は変更時
- Element Attachmentは対象別管理
- Buff/Debuffは共通Scheduler等
- Skill CTは終了時刻方式
- DB/File I/O Async
- GUIは開いているPlayerだけ
- HUD時間系は5Tick
- Boss Phase判定頻度制御
- 1Tick処理件数に設定可能な上限

---

# 149. Cooldown実装

Skillごとに大量のScheduled Taskを生成する方式を避ける。

原則：

```text
CooldownEndTimestamp
```

を保持し、現在時刻との比較で判定。

---

# 150. Exception Handling

1件の例外でCCS全体を停止させない。

適切なSystem境界で捕捉。

ログ：

- INFO
- WARN
- ERROR
- DEBUG

---

# 151. YAML Validation

起動 / Reload時に、

- Syntax
- Duplicate ID
- Missing ID
- Reference
- Number Range
- Material
- Element
- Set
- Reaction
- Skill Tree
- Mob
- Boss
- Region
- Spawn

等を検証。

---

# 152. 不正定義

可能な限り：

- 該当定義のみ無効
- 他正常定義は継続

基幹設定が完全破損している場合のみPlugin起動失敗を許可。

欠損ID：

- WARN / ERROR
- その処理のみ安全に中止
- Server継続

---

# 153. Damage Display負荷対策

同時TextDisplay上限あり。

古い表示の削除等で負荷を制御。

DoT等の大量表示時も過剰Entity生成を防止。

---

# 154. External Plugin共存

PlaceholderAPI：

任意。

TAB等：

共存前提。

WorldGuard：

専用連携なし。

MythicMobs等：

専用連携なし。

Floodgate：

利用可能ならBedrock識別。

CCS自身は外部連携Pluginへ必須依存しない。

---

# 155. CodeX実装時の必須ルール

1. 本仕様をゲーム仕様のSingle Source of Truthとして扱う。
2. 確定仕様を独自に変更しない。
3. Minecraft標準機能を再利用可能なら優先する。
4. Paper APIを基準にする。
5. Adventure Componentを基本にする。
6. YAML表示はMiniMessage対応。
7. Java / Bedrock双方で成立しない必須操作を追加しない。
8. UUIDをPlayer識別の基準とする。
9. DB/File I/Oは原則Async。
10. Bukkit/Paper Entity APIを危険にAsync操作しない。
11. 全Entity毎Tick走査を避ける。
12. Statを毎Tick再計算しない。
13. Cacheを利用する。
14. Skill CTを大量Task方式にしない。
15. Public APIと内部実装を分離する。
16. Item / Weapon / Equipment / Mob / Boss等は一意IDを持つ。
17. Weaponの効果をInventory所持だけで重複適用しない。
18. Combat中の武器・装備Lockを必ず維持する。
19. Vanilla ArmorとCCS DEFの二重軽減を防ぐ。
20. 不正YAML1件で全Pluginを可能な限り停止させない。
21. Reload時は「読込→検証→適用」の順にする。
22. 保存Migration前にBackupする。
23. 未対応の新しいdata-versionを上書きしない。
24. Placeholder取得のために高コスト再計算を毎回実行しない。
25. Auto Spawn管理で毎回World全Entityを走査しない。
26. Encyclopedia未発見Mob/Bossの実名を検索等から漏らさない。
27. Combat中は一般 `/ccs` を使用不可にする。
28. Admin機能はPermissionを必ず確認する。
29. GUIのSlot・Icon等は可能な限り `gui.yml` から調整可能にする。
30. 本仕様にないゲームルール上の判断が必要になった場合、勝手に補完せず仕様確認対象とする。

---

# 156. 実装側で自由に決めてよい範囲

以下は本仕様を変えない限りCodeX側で適切に決定可能。

- Java Class名
- Manager / Service分割
- Internal DTO名
- DB Table名
- Column名
- Index設計
- Cache実装
- Scheduler内部構造
- YAML Keyの細かな命名
- GUI Decor Pane
- GUI Slotの微調整
- Region Wandの具体Material / CustomModelData
- TextDisplay補間方法
- Log内部Format
- Auto Spawn安全地点探索Algorithm
- Unit Test構成
- Package内部構造

ただしPublic APIは、

```text
com.github.saku0817.combatcoresystems.api.v1
```

を基準とする。

---

# 157. 完成条件

CombatCoreSystems v1.0.0は、少なくとも本書に定義された以下の主要Subsystemが相互に矛盾なく動作することを完成条件とする。

- Player Stats
- Damage
- Healing
- Weapon
- Skill
- Ultimate
- Element
- Reaction
- Buff / Debuff
- Equipment
- Resonance
- Divine Heart
- Enhancement
- Player Level
- Rebirth
- Skill Tree
- Mob
- Boss
- Spawn
- Region
- PvP
- Party
- Combat State
- HUD
- GUI
- PlaceholderAPI
- Encyclopedia
- Storage
- Backup
- Migration
- Public API
- Debug
- Performance / Exception Handling

以上を **CombatCoreSystems v1.0.0 最終実装仕様** とする。