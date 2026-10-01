# CombatCoreSystems v1.4.6 — YAML生成GPT用差分

この資料はv1.4.6の追加・変更点です。v1.4.5へ新キーを適用しないでください。設定の全体像は引き継ぎ書とYAML・実装参照資料を併読してください。

## 装備の成長・移行

- 最大Lv/サブステ枠: ★3=9/2、★4=12/3、★5=15/4。旧`max-level`よりこの上限を優先。
- Lv1開放数は0。Lv3/6/9/12で1個ずつ開放（最大枠まで）。
- Lv3/6/9/12/15で開放とは別に1回強化。未開放を含む、その個体のサブステのどれか1個が候補。同じ種類への繰返し強化が可能。
- 全サブステ種類は取得時に決め、個体に保存する。サブ同士の重複禁止、メインとの重複許可。
- 強化先の履歴も個体に保存。itemlevelで下げて戻しても再抽選しない。itemlevelは開放数・強化履歴を指定Lvに対応する状態へ戻すため、管理者が個別編集した強化数も再計算する。
- 既存個体も移行対象。上限超過Lvを下げ、有効な種類を保持して不適合種類を再抽選。値と強化履歴を新表で再計算。移行済み情報を保存して繰返し再抽選しない。
- 未読込チェスト等のアイテムをワールド全走査しない。CCSで初めて読み取った時点で移行。

|部位|選択・抽選できるメイン|
|---|---|
|HEAD|HP_FLAT, HP_PERCENT, DEF_FLAT, DEF_PERCENT|
|CHEST|ATK_FLAT, ATK_PERCENT|
|LEGS|CRIT_RATE, CRIT_DAMAGE|
|FEET|HP_PERCENT, DEF_PERCENT, ATK_PERCENT, HEALING_POWER|
|RESONANCE|各5属性の`*_DAMAGE`, `*_RESISTANCE`|

サブ候補は`HP_FLAT, HP_PERCENT, DEF_FLAT, DEF_PERCENT, ATK_FLAT, ATK_PERCENT, CRIT_RATE, CRIT_DAMAGE`の8種のみ。属性ダメージ・属性耐性・治癒力はサブにならない。

メインとサブの数値はユーザー指定のv1.4.6表を使用。9.3%は0.093、11.7%は0.117。`main-stat.level-1/max-level`や候補の`value`等の旧任意値は新成長表を上書きしない。`weight`は候補抽選に使用する。

```yaml
equipment:
  example_chest:
    name: '<blue>旅人の胴鎧</blue>'
    material: DIAMOND_CHESTPLATE
    slot: CHEST
    rarity: 5
    main-stat-candidates:
      ATK_FLAT: {weight: 1}
      ATK_PERCENT: {weight: 1}
    substat-candidates:
      ATK_PERCENT: {weight: 1}
      HP_FLAT: {weight: 1}
      CRIT_RATE: {weight: 1}
      CRIT_DAMAGE: {weight: 1}
      DEF_PERCENT: {weight: 1}
    set: traveler
    lore: ['<gray>旅に使われた胴鎧。</gray>']
```

## 超過経験値の返却

素材によるプレイヤー/武器/装備の強化が最大Lvへ到達した時の超過分を、同じ用途の素材へ変換する。100EXP未満を切り捨て、大きな素材から返す。100の倍数のEXPを持つ素材を使用し、100EXP素材を残すこと。

返却先は既存の数値上の素材所持数。インベントリ満杯でも散逸しない。変換GUIで実物化可能。`messages.yml: enhancement-overflow-refund`で表示文を編集できる。変数は`<materials>`。

## 自己スタックのアクションバー

```yaml
# gui.yml のhudに追加（スタックIDは実際にActionで定義したIDと一致させる）
hud:
  self-stacks-enabled: true
  self-stack: '<aqua><name>: <count></aqua>'
  self-stack-names:
    electric_corrosion: '電蝕'
    fire_seed: '火種'
```

`SELF`スコープの現在の累積数を表示する。対象別スタックは含まない。期限切れ・0個は表示しない。IDは例であり、特定武器へのハードコードではない。

## 名称・入力・管理

- 表示名「神心/神の心」は「追憶」、「回復力」は「治癒力」。互換キー`divine_hearts.yml`、`divine-hearts`、`DIVINE_HEART`、`HEALING_POWER`は維持。
- 治癒力は既存の割合上級ステータス`HEALING_POWER`を使用。自然レベル上昇項目は追加しない。
- `controls.bedrock-selected-slot-drop-skill`は廃止・無視。インベントリ由来のドロップをスキルへ変換しない。手からのドロップは既存の発動経路を維持。
- 管理アイテム一覧は武器/装備（セット別）/追憶/強化素材。サブステの種類・強化数は選択GUIを使用。
- `gui.yml`の`admin.categories.*`, `admin.sub-editor-title`, `admin.sub-select-hint`, `admin.sub-upgrade-choice`, `admin.sub-replace`で表示を変更可能。回数選択文の変数は`<count>`, `<value>`。

## 戦闘補正

`buffs.yml`の各バフに`combat-modifiers`または`context-modifiers`を記述できる。恒久ステータスを書き換えず、その攻撃・回復の計算だけに適用する。

```yaml
combat-modifiers:
  damage-dealt: {percent: 0.20}
  skill:
    damage-dealt: {percent: 0.50}
    healing-dealt: {percent: 0.50}
  normal-attack:
    element: {override: THUNDER}
    crit-rate: {override: 1.0}
```

同一カテゴリのpercentは加算、全体補正と種類別補正は乗算。上例ではスキル与ダメージは1.2×1.5倍。回復は基礎回復×(1+治癒力)×与回復補正×被回復補正。攻撃側の`damage-dealt`と防御側の`damage-taken`は別の層として乗算する。通常の属性耐性とは別。

同じバフ内・同priorityでは全体設定→種類別設定の順。Actionの会心指定はバフより後に適用。最終属性がEventで変わった場合、属性scopeと属性別被ダメージ補正は変更後の属性で評価する。

種類別カテゴリは`normal-attack`, `skill`, `ultimate`, `talent`, `trigger`, `buff`, `debuff`, `dot`, `field`, `reaction`, `mob-attack`, `environment`。属性別の被ダメージには`fire: {damage-taken: {percent: 0.2}}`などを使用。

`scope`は`source-kind`（大文字列挙名、単数または配列）、`source-id`（完全一致）、`element`、`tags`（すべて一致）を指定できる。`priority`は整数、小さい順に適用。同一層のoverrideは後勝ち。

通常の継続効果は`DOT`/`buff:効果ID`、フィールド滞在中だけ保持する継続効果は`FIELD`/`field:フィールドID`。スキルActionは`SKILL`/`skill:スキルのid`が既定（id省略時は定義識別子）。Actionの`source-kind`/`source-id`で明示指定できる。Conditionのsource-idも接頭辞を含め完全一致で指定する。

DAMAGE/HEAL Actionには`modifiers`、`source-kind`、`source-id`、`tags`を指定できる。会心は`critical.mode: DEFAULT / DISABLED / ENABLED / FORCED`、`critical.rate: {override: 0.5}`、`critical.damage: {percent: 0.5}`。会心のpercent/flatは割合への加算（0.5=50ポイント）、恒久ステータスには反映しない。

`BEFORE_DAMAGE`は`BEFORE_HIT`、`AFTER_DAMAGE`は`HIT`、`AFTER_HEAL`は`HEAL`の別名。`BEFORE_HEAL`も使用可能。`MODIFY_EVENT`はBEFORE_HIT/BEFORE_HEAL内だけで有効。

## 移動Action

```yaml
actions:
  - type: DASH
    target: SELF
    direction: FORWARD
    distance: 5
    duration: 0.25
    collision: STOP
    hit-policy: ONCE_PER_TARGET
    path-area: {shape: BOX, width: 1.5, height: 2.5}
    path-actions:
      - type: DAMAGE
        target: EVENT_TARGET
        reference: ATK
        multiplier: 1.0
        attribute: PHYSICAL
```

種類はMOVE/DASH/LEAP/KNOCKBACK/PULL/TELEPORT。衝突方式は現時点でSTOPのみ。距離は最大64m、時間は最大10秒、MOVE/DASHは最大8m/tick。未読込チャンクを強制ロードしない。`path-actions`は移動経路命中時、`end-actions`は正常終了または衝突停止時に実行。死亡・退出・ワールド移動・再読込による取消ではend-actionsを実行しない。移動イベントはMOVE_START/MOVE_TICK/MOVE_HIT/MOVE_END。

## 限界突破の説明表示

天賦・スキル・必殺技の説明を突破0段階と比較し、変更・追加された文字だけ黄色にする。名称や変更のない文字の元の色、太字などは保持する。

## Webエディタの変更

装備の生成フォームは部位ごとのメイン候補だけを表示。最大Lvはレア度から自動決定し、数値の自由指定は廃止。`main-stat-candidates`と8種類の`substat-candidates`を生成する。重みや候補の絞り込みは生成後のYAMLで編集可能。プレビューには旧任意成長値を表示せず「共通成長表」と候補を表示する。

## 検証範囲

Java単体試験95件と隔離Paperサーバー試験134項目を確認。隔離試験はMobと模擬プレイヤーを使い、移動時のMob位置更新を補助しているため、Java/BEクライアントによる実際の操作・描画は別途確認が必要です。
