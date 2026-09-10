# CombatCoreSystems

Paper 26.2 / Java 25 向けの、PvE・PvP対応ARPG／オープンワールドRPG基盤プラグインです。

現在のバージョン: **v1.3.0**

## 主な機能

- Mob討伐で得る独自経験値と独自レベル、基礎・上級ステータス、転生、5プリセットのスキルツリー
- 5属性と全10組の属性反応、会心、防御・耐性を含むダメージ計算
- ホットバー自動認識式の近接／遠距離武器、スキル、必殺技、限界突破、装備、セット効果、神の心
- カスタムMob・Boss、設定可能なバニラMob、リージョン、スポーン、図鑑、HUD、GUI
- 最大4人のパーティー、PvP設定、10秒間の戦闘状態
- SQLite／YAML／JSON／MySQL／MariaDB、定期保存・バックアップ・復元
- PlaceholderAPI連携と、他プラグイン向け公開API・イベント

詳しい使い方、設定例、API例は [GitHub Wiki](https://github.com/saku0817/CombatCoreSystems/wiki) を参照してください。実装基準は [最終実装仕様書](docs/CombatCoreSystems%20v1.0.0%20最終実装仕様書.md) です。

## 必要環境

| 項目 | 内容 |
|---|---|
| サーバー | Paper 26.2 |
| Java | 25 |
| PlaceholderAPI | 任意 |
| Floodgate | 任意（存在時のみBedrock判定に利用） |

## 導入

1. `CombatCoreSystems-<version>.jar` をPaperサーバーの `plugins` に配置します。
2. サーバーを起動し、`plugins/CombatCoreSystems/` に設定・データファイルを生成します。
3. 必要に応じて設定を編集し、`/ccsadmin reload` で再読込します。

Minecraft標準の経験値・レベルとCCSの経験値・レベルは独立しています。Mobごとの`custom-exp`と`drop-custom-exp`は`mobs.yml`、必要経験値式とバニラ武器ATK変換値は`levels.yml`で変更できます。

CCS武器は`weapons.yml`の`type: MELEE|RANGED`で種別を指定します。ホットバー内では同じ種別を1本まで保持でき、余分な武器はメインインベントリ、空きがなければ地面へ移動します。メイン武器の手動登録は不要です。強化GUIでは3等級の素材使用数、強化前後、上昇幅を確認でき、実物素材と数値素材、上級・下級素材を相互変換できます。

## コマンド

| コマンド | 用途 |
|---|---|
| `/ccs menu` | メインメニュー |
| `/ccs help` | ヘルプ |
| `/ccs open party` | パーティーGUI（招待・承認・退出・管理） |
| `/ccs open party ...` | パーティー管理サブコマンド |
| `/ccs open encyclopedia` | 図鑑と詳細画面 |
| `/ccs open equipments` | 装備・解除と装備状態の確認 |
| `/ccs open enhancement` | 素材数をボタンで指定して強化・変換 |
| `/ccs skill`・`/ccs ultimate` | 手持ちCCS武器のスキル・必殺技（戦闘中可） |
| `/ccsadmin debug on 10m` | 10分間の攻撃ログ記録 |
| `/ccsadmin debug status` | debug状態と予約の確認 |
| `/ccsadmin reload` | 設定の再読込 |
| `/ccsadmin save` | 全データの保存 |
| `/ccsadmin backup ...` | バックアップ／復元 |
| `/ccsadmin edit force on\|off [username]` | 戦闘状態の強制変更。名前省略時は自分 |

権限や全サブコマンドは [コマンドと権限](https://github.com/saku0817/CombatCoreSystems/wiki/Commands-and-Permissions) に掲載しています。

v1.3.0の操作、設定例、移行時の注意は [v1.3.0ガイド](docs/v1.3.0.md) を参照してください。GUIの増減や変換は通常タップで操作できます。未登録のバニラMobの報酬は `mobs.yml` の `vanilla-defaults` で設定します。

## ビルドとリリース

通常の検証は `mvn clean verify` で行います。配布JARは、必ず次のリリーススクリプトで生成してください。

```powershell
.\scripts\build-release.ps1
```

検証済みJARが `releases/v<version>/`、同一内容のバックアップが `backups/v<version>/` に生成されます。`main` へのpush／PRと `v*` タグはGitHub Actionsでも検証されます。

## バージョン管理

作業ブランチで変更し、確認後に `main` へ統合します。リリース時は `pom.xml` と `CHANGELOG.md` を更新し、上記スクリプトでJARとバックアップを生成してから `v<version>` タグを付けます。コミットメッセージはConventional Commits形式を推奨します。

## ライセンス

CombatCoreSystemsは **GNU General Public License v3.0 or later**（`GPL-3.0-or-later`）で公開しています。詳細は [LICENSE](LICENSE) を参照してください。

Kyori Adventureを含む外部ソフトウェアには、それぞれのライセンスが引き続き適用されます。著作権表示、利用形態、ライセンス全文は [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) と [third-party-licenses](third-party-licenses) を参照してください。
