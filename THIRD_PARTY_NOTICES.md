# Third-Party Notices

CombatCoreSystems uses, bundles, or interfaces with the third-party software listed below. Each component remains subject to its own license; the CombatCoreSystems GPL license does not replace those terms. Full license texts for redistributed components and Kyori Adventure are stored in `third-party-licenses/` and packaged under `META-INF/third-party-licenses/` in the release JAR.

## Runtime-provided APIs (not bundled)

### Kyori Adventure

- Project: Adventure
- Upstream: https://github.com/PaperMC/adventure
- License: MIT
- Copyright: KyoriPowered and contributors
- Usage: Adventure text, MiniMessage, boss bars, and plain-text serialization are provided by the Paper runtime. Adventure is not shaded into the CombatCoreSystems JAR.
- License text: `third-party-licenses/KYORI-ADVENTURE-MIT.txt`

### Paper API

- Project: Paper
- Version used for compilation: 26.2 build 121 stable
- Upstream: https://github.com/PaperMC/Paper
- License: GPL-3.0 with separately MIT-licensed contributions as identified upstream
- Usage: Maven `provided` dependency; not bundled in the CombatCoreSystems JAR.

### PlaceholderAPI

- Project: PlaceholderAPI
- Version used for compilation: 2.11.6
- Upstream: https://github.com/PlaceholderAPI/PlaceholderAPI
- License: GPL-3.0
- Usage: Optional Maven `provided` dependency; not bundled in the CombatCoreSystems JAR.

## Components bundled in the release JAR

### Gson

- Version: 2.13.2
- Upstream: https://github.com/google/gson
- License: Apache-2.0
- License text: `third-party-licenses/APACHE-2.0.txt`

### SQLite JDBC

- Version: 3.50.3.0
- Upstream: https://github.com/xerial/sqlite-jdbc
- License: Apache-2.0; bundled SQLite components retain the upstream notices included in its license file
- License text: `third-party-licenses/SQLITE-JDBC-LICENSE.txt`

### MySQL Connector/J

- Version: 9.4.0
- Upstream: https://github.com/mysql/mysql-connector-j
- License: GPL-2.0 with the Universal FOSS Exception, Version 1.0
- License text: `third-party-licenses/MYSQL-CONNECTOR-J-LICENSE.txt`

### MariaDB Connector/J

- Version: 3.5.6
- Upstream: https://github.com/mariadb-corporation/mariadb-connector-j
- License: LGPL-2.1-or-later
- License text: `third-party-licenses/MARIADB-CONNECTOR-J-LGPL-2.1.txt`

## No endorsement

Third-party project names are used only to identify their software. No upstream project or contributor endorses CombatCoreSystems.
