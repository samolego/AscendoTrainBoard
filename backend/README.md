# AscendoTrainBoard Minimal Backend

Lightweight backend server for AscendoTrainBoard climbing problem tracking system. Designed to run on ESP32 or Raspberry Pi with minimal resource usage.

## Quick Start

### 1. Setup Data Directory

```bash
mkdir -p data sectors
```

### 2. Initialize Empty Data Files

```bash
echo "[]" > data/users.json
echo "[]" > data/problems.json
```

### 3. Setup Sectors (Optional)

For each sector (1-8), create directory structure:

```
sectors/
├── 1/
│   ├── metadata.json
│   └── wall.jpg
├── 2/
│   ├── metadata.json
│   └── wall.jpg
...
```

Example `sectors/1/metadata.json`:

```json
{
  "id": 1,
  "name": "Sector A",
  "image_filename": "wall.jpg",
  "holds": [
    [100, 150, 120, 170],
    [200, 175, 215, 190],
    [150, 300, 165, 320]
  ]
}
```

Holds format: `[start_x, start_y, end_x, end_y]` in pixels.

### 5. Run the Server

```bash
cargo run
```

Server starts on `http://0.0.0.0:3000` on debug and on port `80` on release build.

## API Endpoints

See [openapi specification](../openapi.yaml) for full details.

## Periodic Auto-Save

Server checks every **30 seconds** if data has changed. If modified, automatically saves to:
- `data/users.json`
- `data/problems.json`

## Admin Users

Users listed in `settings.json` under `admin_users` can:
- Edit/delete any user's problems
- All other permissions same as regular users

## Building for ESP32

```bash
# Install ESP32 Rust toolchain first
cargo build --release --target xtensa-esp32-espidf
```

## Development

Check code:
```bash
cargo check
```

Run with logging:
```bash
RUST_LOG=debug cargo run
```

Format code:
```bash
cargo fmt
```

## File Structure

```
backend/
├── src/
│   ├── main.rs       # App setup, routes, periodic save
│   ├── models.rs     # Data structures
│   ├── auth.rs       # SHA256 hashing + session tokens
│   ├── handlers.rs   # All API handlers
│   └── state.rs      # In-memory state + JSON persistence
├── data/
│   ├── settings.json
│   ├── users.json
│   └── problems.json
└── sectors/
    └── 1-8/
        ├── metadata.json
        └── image file
```
