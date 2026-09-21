# CombatCoreSystems v1.4.5 — GPT用YAML生成差分

この資料はv1.4.4までの引き継ぎ書に追加する差分。以下の新キーはv1.4.5以降で使用する。引き継ぎ書・完全参照資料・v1.4.4差分と併せてYAML生成GPTへ渡す。

## 弓の通常攻撃

発射物の移動速度・バニラの矢ダメージをATKに変換しない。近接と同じDamageServiceの通常攻撃計算を使い、攻撃クールダウンの代わりに弓のチャージ時間割合を使う。

- チャージ時間割合 `c = clamp(使用時間 / 20tick, 0, 1)`。
- 補正 `0.2 + 0.8 × c²`。50%チャージで40%、最大チャージで100%。
- Paperの発射イベントが返すforceは既に曲線変換済みなので、`c = sqrt(1 + 3 × clamp(force,0,1)) - 1` で戻す。
- 発射後の速度・距離・重力で補正を再計算しない。発射イベントにチャージ情報がない発射物は従来どおり100%扱い。
- 通常攻撃属性、防御・耐性・会心、攻撃力の既存基本式は維持する。

## 共通配置

次の場所に `triggers.<任意ID>` を設定できる。

```yaml
weapons:
  example:
    triggers: {}
    talent:
      hand: MAIN_HAND
      triggers: {}
    skill:
      triggers: {}
      # actionsはこの技の成功時に順番に実行
    ultimate:
      triggers: {}
equipment:
  example:
    triggers: {}
sets:
  example:
    two-piece:
      triggers: {}
    four-piece:
      triggers: {}
divine-hearts:
  example:
    triggers: {}
```

実際にはそれぞれ既存のweapons.yml、equipment.yml、sets.yml、divine_hearts.ymlへ分ける。天賦はhand条件を満たす間だけ有効。武器ルートは有効な手持ち武器、装備・神心は登録された装備、セットは必要部位数を満たす間だけ有効。同じ定義を複数所持しても同じ配置のトリガーは重複実行しない。

武器の `limit-breaks.0～5` にも同じキーを指定可能。Mapは累積マージ、actions等のListは丸ごと置換。変更したいトリガーと**同じ配置・同じID**へ差分を書く。ルートのトリガーはtalent内の同名キーでは上書きされない。

## Trigger

```yaml
triggers:
  on_hit:
    event: HIT
    target: SELF
    conditions: {damage-positive: true}
    actions:
      - type: APPLY_EFFECT
        effect: power_buff
        target: SELF
    cooldown-seconds: 0
    max-activations: 0
    activation-scope: GLOBAL
```

`max-activations: 0` は無制限。scopeはGLOBAL / COMBAT / LIFE。COMBATの回数は戦闘終了時、LIFEは死亡時に解除。状態はメモリ上のみで、再読み込み・ログアウト・サーバー再起動を越えて保存しない。

旧 `event/target/effects/cooldown-seconds` 形式とself-effects/target-effectsは維持する。旧effects形式の省略CTは1秒、新actions形式は0秒。effectsとactionsを両方書いた場合はactionsの後にeffectsを実行する。

### イベント

SKILL / ULTIMATE / HIT / TAKE_DAMAGE / HP_BELOW / BEFORE_HIT / NORMAL_ATTACK / HEAL / RECEIVE_HEAL / OVERHEAL / HP_ABOVE / BUFF_APPLIED / BUFF_REMOVED / STACK_CHANGED / STACK_REACHED / COMBAT_START / COMBAT_END / ENTER_FIELD / LEAVE_FIELD / TICK。

- BEFORE_HIT：対象と攻撃可否を確認後、ステータス取得・ダメージ計算より前。
- HIT：正のダメージ確定後。NORMAL_ATTACKはそのうち通常攻撃のみ。追加DAMAGEは通常攻撃として扱わない。
- HP_BELOW：HP割合が閾値以下、HP_ABOVE：閾値より上になった境界で一回。初回評価時に条件を満たす場合も発火。hp-percent省略時0.5。
- STACK_REACHED：conditions.stack.minを下から跨いだ場合のみ。
- HEAL：正の要求回復量。RECEIVE_HEALは受け手。OVERHEALはあふれた量が正のときだけ。
- HP・フィールド・TICKは5tick間隔。秒指定は実時間、監視の反映には最大5tick程度の遅延がある。
- skill/ultimate配置の攻撃・回復トリガーはその技由来の処理だけに適用する。

## 条件（同じMap内はAND）

| キー | 値 |
|---|---|
| min-hp-percent / max-hp-percent | 所有者HP割合、0～1 |
| requires-combat / requires-target | boolean |
| min-distance / max-distance | 対象への距離、0～1024 |
| damage-positive / heal-positive / overheal-positive | boolean |
| normal-attack-only / skill-only / ultimate-only | boolean |
| element | PHYSICAL / FIRE / WATER / WIND / THUNDER / MOON |
| critical | boolean |
| buff-present / buff-absent | 所有者のbuffs.yml定義ID |
| party-required | boolean |
| target-is-self / target-is-ally / target-is-enemy | boolean |
| inside-field / outside-field | field ID |
| stack | `{id: aim, scope: EVENT_TARGET, min: 1, max: 5}` |
| context-value | `{key: consumed_aim, min: 3, max: 5}` |

Actionにもconditionsを設定可能。conditions-anyは将来拡張用で今回の生成には使わない。技ルートの旧requires-target/max-distanceについては、旧版の空撃ち許可との互換動作を維持する。Trigger/Action内のrequires-targetは厳密に対象有無を判定する。

## 対象選択

SELF / SOURCE（所有者）、EVENT_TARGET / OTHER（イベント対象）、ATTACKER、VICTIM、NEAREST_ENEMY、NEAREST_ALLY、LOWEST_HP_PARTY_MEMBER、LOWEST_HP_PARTY_MEMBER_OR_SELF、ALL_PARTY_MEMBERS、ALL_PARTY_MEMBERS_AND_SELF、ENTITIES_IN_AREA、ALLIES_IN_AREA、ENEMIES_IN_AREA。

味方は自分と同一パーティー。敵の判定とDAMAGEは既存PvP・地域・パーティー保護を尊重。対象は同一ワールドの生存者。最寄り探索は64m以内。最低HPはHP/最大HP→距離→UUID順。OR_SELFはパーティーなしなら自分。ALL_PARTY_MEMBERSは自分を除き、AND_SELFは含める。areaを指定したActionは選択結果も範囲で絞る。

## スタック

```yaml
talent:
  hand: MAIN_HAND
  target-stack-policy:
    stack-id: aim
    max-targets: 3
    overflow: REMOVE_OLDEST
  triggers:
    aim_hit:
      event: NORMAL_ATTACK
      actions:
        - type: ADD_STACK
          id: aim
          scope: TARGET
          amount: 1
          max: 5
          duration: 0
          reapply: REFRESH
```

所有者UUID＋定義ID＋stack IDで分離。TARGETはさらに対象UUIDで分離。武器ルート・天賦・スキル・必殺技の間は同じ武器定義のスタックを共有する。別武器定義や別プレイヤーとは共有しない。

- ADD_STACK / SET_STACK：amount（省略1）、max（省略1、1以上）、duration（秒、0は無期限）、reapply。
- reapply：REFRESH（残り時間を更新）、EXTEND（延長）、IGNORE（既存期限を維持）。
- scope：SELF / TARGET / EVENT_TARGET / ATTACKER / VICTIM。SELF以外は対象別状態。
- CLEAR_STACK：scope ALL_TARGETSで対象別状態を全消去。SELF状態は含まない。
- CONSUME_STACK：amountだけ部分消費。不足時は存在分のみ。
- CLEAR_STACK/CONSUME_STACKのstore-resultは実際に消費した数。ADD/SETは操作後の層数。
- target-stack-policy：max-targets 0は無制限。REMOVE_OLDEST / REMOVE_NEWEST / REMOVE_LOWEST_STACK / REJECT_NEW。再付着でcreatedAtは変えない。REMOVE_NEWESTは新規対象を残さない。

```yaml
actions:
  - {type: CLEAR_STACK, id: aim, scope: ALL_TARGETS, store-result: consumed_aim}
  - type: APPLY_EFFECT
    effect: heavenly_eye_3
    conditions: {context-value: {key: consumed_aim, min: 3}}
```

## 攻撃中だけのステータス補正

```yaml
triggers:
  aim_bonus:
    event: BEFORE_HIT
    conditions: {stack: {id: aim, scope: EVENT_TARGET, min: 3}}
    actions:
      - type: MODIFY_EVENT_STATS
        target: SOURCE
        modifiers:
          percent: {ATK_PERCENT: 0.50, CRIT_RATE: 0.20}
          override: {CRIT_RATE: 1.0}
```

MODIFY_EVENT_STATSはBEFORE_HITでのみ有効。target SOURCE/SELFは攻撃者、EVENT_TARGET/VICTIM/OTHERは防御側の今回の計算用コピー。通常ステータスやPlayerDataには書き戻さない。

flat → percent → override。CRIT_RATE等の割合値は既存CCSと同じ加算比率（0.2は20ポイント）。ATK_PERCENTは今回のATK参照値への割合。overrideは加算ではなく最終値を固定し、同じ値への複数overrideは最後が優先する。

buffs.ymlにも `modifiers.override: {CRIT_RATE: 1.0}` を追加可能。最後に付与・再付与された有効Buffのoverrideが優先。ATK_FLAT / HP_FLAT / DEF_FLATのoverrideは最終ATK / 最大HP / DEF参照値を固定する。対応StatKeyは既存列挙値を使い、存在しないATK等のキーは使用しない。

## DAMAGE / HEAL

```yaml
actions:
  - type: DAMAGE
    target: EVENT_TARGET
    reference: ATK
    multiplier: 1.5
    attribute: THUNDER
    def-ignore: 0.5
  - type: DAMAGE
    target: EVENT_TARGET
    attribute: WATER
    components:
      - {reference: ATK, multiplier: 2.0, bonus-attribute: PHYSICAL}
      - {reference: HP, multiplier: 0.3, bonus-attribute: WATER}
  - type: HEAL
    target: LOWEST_HP_PARTY_MEMBER_OR_SELF
    reference: HP
    multiplier: 0.25
```

DAMAGEは必ず既存DamageServiceを通す。referenceはATK/HP/DEF、attributeはダメージ属性。componentsの意味は既存damage-componentsと同じ。def-ignoreは0～1で、この追加ダメージだけに加算し、通常攻撃には残さない。

HEALのreference：ATK / HP（発動者最大HP）/ DEF / FIXED / EVENT_DAMAGE / EVENT_HEAL / EVENT_EFFECTIVE_HEAL / EVENT_OVERHEAL。FIXEDはmultiplierが基礎回復量。すべて基礎回復量×(1+HEALING_POWER)。実回復量とあふれた量は最大HPとの差から求める。

skill/ultimate直下にactionsを設定すると成功時に一回実行。actionsを持つ新定義の通常の技ダメージは省略時無効にする。追加で従来の技ダメージも必要なら `damage-enabled: true` を明記する。actionsのない既存定義の省略値はtrueを維持。

## 動的補正

```yaml
actions:
  - type: APPLY_DYNAMIC_MODIFIER
    target: SELF
    stat: ATK_FLAT
    source: EVENT_OVERHEAL
    multiplier: 0.1
    duration: 10
    reapply: REFRESH
```

source：EVENT_DAMAGE / EVENT_HEAL / EVENT_EFFECTIVE_HEAL / EVENT_OVERHEAL / SOURCE_ATK / SOURCE_MAX_HP / SOURCE_DEF / TARGET_ATK / TARGET_MAX_HP / TARGET_DEF。付与時に数値を確定。同じ所有者・定義・配置・トリガー・Action位置の効果を再付与すると更新する。duration 0は無期限。ログアウト・リロードで消える一時効果。

## 範囲・フィールド

```yaml
actions:
  - type: CREATE_FIELD
    id: scorching_realm
    duration: 15
    area: {shape: SPHERE, radius: 5, origin: SELF}
    ally-effects: [fire_power]
    enemy-effects: [fire_resistance_down]
    effect-mode: WHILE_INSIDE
```

area.shape：SPHERE / CIRCLE / CYLINDER / BOX / FORWARD_BOX / CONE。

- radiusまたはrange：省略5。width/height/length：省略5。寸法0～128m。
- SPHEREは球。CIRCLE/CYLINDERは水平半径とheightで判定。
- BOXは中心対称、FORWARD_BOXは所有者の水平な向きを基準に前方length。
- CONEはrange（radiusでも可）、angle（省略90、0～360度）、height。
- origin：SELF / EVENT_TARGET / LOCATION（イベント位置）。生成したフィールドは生成地点固定で追従しない。
- target-filter：ENEMY / ALLY / ALL（省略）。
- 同じ所有者・定義・field IDの再生成は古いものを置換。
- ON_ENTER：進入時だけ通常Buff付与。REFRESH_WHILE_INSIDE：内部にいる間、通常Buffを5tickごとに再付与。
- WHILE_INSIDE：フィールド専用の付与元を保持し、退出・期限切れ時にその分だけ解除。他のフィールドや通常付与の同名Buffを巻き込まない。
- REMOVE_FIELDはidで同じ所有者・定義のフィールドを解除。

## 安全性・生成上の注意

未知のevent/action/selector/shape/scope/overflow/StatKey/attribute、未定義Buff、空actions/effects、負の時間・寸法、NaN/Infinity、不正def-ignore等はreloadエラー。現在の正常Snapshotは保持する。

追加攻撃は再びHITを発火できるが、同一chain内の同じ所有者・定義・配置・トリガーは一度のみ。ネスト上限16。固定武器ID・セットIDによるJava分岐は作らない。

生成する際は、参照Buffの定義をbuffs.ymlへ必ず一緒に出力する。説明文だけでは効果は実装されない。追加キーを使わない既存装備を勝手に書き換えない。
