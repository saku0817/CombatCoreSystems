# CombatCoreSystems

Paper 26.2 / Java 25 向けのARPG・オープンワールドRPG基盤プラグインです。

実装基準は [docs/CombatCoreSystems v1.0.0 最終実装仕様書.md](docs/CombatCoreSystems%20v1.0.0%20最終実装仕様書.md) です。

## 必要環境

- Paper 26.2
- Java 25
- PlaceholderAPI（任意）
- Floodgate（任意、存在時のみBedrock判定に利用）

## ビルド

Windows PowerShellで `scripts/build-release.ps1` を実行します。テスト済みJARは
`releases/v<version>/` に生成され、同じJARが `backups/v<version>/` に必ず複製されます。

直接Mavenを使う場合は `mvn verify` です。ただし配布物の作成には、バックアップを保証する
リリーススクリプトを使用してください。

## 導入

生成された `CombatCoreSystems-<version>.jar` をPaperサーバーの `plugins` に配置します。
初回起動時に全設定ファイルとSQLiteデータベースが生成されます。

## 管理コマンド

- `/ccsadmin edit force on [username]`: 対象を強制的に戦闘状態にします。
- `/ccsadmin edit force off [username]`: 対象の戦闘状態を解除します。
- `username` を省略した場合は、実行したプレイヤー自身が対象です。コンソールからは省略できません。

## バージョン管理

Conventional Commitsを推奨します。`main` へのpush/PRでGitHub Actionsがビルドとテストを行い、
`v*` タグではバージョン別JARをActions成果物として保存します。

通常の変更は作業用ブランチで行い、確認後に `main` へ統合します。リリース時は `pom.xml` と
`CHANGELOG.md` のバージョンを更新し、リリーススクリプトを実行してから `v<version>` タグを付けます。
