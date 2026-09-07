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
`releases/v1.0.0/` に生成され、同じJARが `backups/v1.0.0/` に必ず複製されます。

直接Mavenを使う場合は `mvn verify` です。ただし配布物の作成には、バックアップを保証する
リリーススクリプトを使用してください。

## 導入

生成された `CombatCoreSystems-1.0.0.jar` をPaperサーバーの `plugins` に配置します。
初回起動時に全設定ファイルとSQLiteデータベースが生成されます。

## バージョン管理

Conventional Commitsを推奨します。`main` へのpush/PRでGitHub Actionsがビルドとテストを行い、
`v*` タグではバージョン別JARをActions成果物として保存します。
