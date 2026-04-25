# World Generation Splicer

NeoForge 1.21.1 mod for surveying ore distribution in your world. Scans chunks, tracks per-dimension block stats, exports reports and graphs.

## Features

- Chunk scanner — on-load, radius, or full-world sweeps
- Per-dimension ore counts with Y-level distribution
- Exports to JSON, CSV, and XLSX
- Density and distribution graph rendering (PNG)
- Configurable target blocks, scan triggers, and permission gating

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.228+

## Commands

`/wgsplicer` (op-gated by default):

| Command | Description |
|---|---|
| `status` | Show scan state and collected dimensions |
| `scanchunk` | Scan the chunk you are standing in |
| `scanradius <r>` | Scan chunks within radius `r` |
| `scanall [r]` | Scan loaded world up to radius `r` (default 200) |
| `export` | Write JSON/CSV/XLSX + graphs to export folder |
| `reset` | Wipe collected stats |

## Config

Common config: `config/wgsplicer-common.toml`. Toggle scan mode, trigger, target blocks, export folder, and permission level.

## License

All Rights Reserved. See [LICENSE](LICENSE).
