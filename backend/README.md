# AscendoTrainBoard Minimal Backend

Lightweight backend server for AscendoTrainBoard climbing problem tracking system. Designed to run on ESP32 or Raspberry Pi with minimal resource usage.

## Features

- ✅ **Minimal dependencies** - Only essential crates for ESP32 compatibility
- ✅ **Simple authentication** - SHA256-based password hashing with random salt and session tokens
- ✅ **JSON persistence** - Periodic auto-save to prevent data loss
- ✅ **RESTful API** - Follows OpenAPI specification
- ✅ **Static sectors** - 8 climbing sectors with images and hold definitions

## Quick Start

### 1. Setup Data Directory

```bash
mkdir -p data sectors
```

### 2. Create Settings File

Create `data/settings.json`:

```json
{
  "ap_name": "AscendoTrainBoard",
  "ap_password": "your_wifi_password",
  "admin_users": ["admin"]
}
```

### 3. Initialize Empty Data Files

```bash
echo "[]" > data/users.json
echo "[]" > data/problems.json
```

### 4. Setup Sectors (Optional)

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
cargo run --release
```

Server starts on `http://0.0.0.0:3000`

## API Endpoints

### Authentication

- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login and get session token
- `POST /api/v1/auth/logout` - Logout

### Sectors (Read-only)

- `GET /api/v1/sectors` - List all sectors
- `GET /api/v1/sectors/{id}` - Get sector details
- `GET /api/v1/sectors/{id}/image` - Get sector wall image

### Problems

- `GET /api/v1/problems` - List problems (with filters)
- `GET /api/v1/problems/{id}` - Get problem details
- `POST /api/v1/problems` - Create problem (auth required)
- `PUT /api/v1/problems/{id}` - Update problem (auth required)
- `DELETE /api/v1/problems/{id}` - Delete problem (auth required)

### Grades/Ratings

- `GET /api/v1/problems/{id}/grades` - Get all grades for problem
- `POST /api/v1/problems/{id}/grades` - Submit/update grade (auth required)

## Authentication Flow

1. **Register/Login** to get a session token:

```bash
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'
```

Response:
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "username": "admin"
}
```

2. **Use token** in subsequent requests:

```bash
curl http://localhost:3000/api/v1/problems \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000"
```

## Data Files

### `data/settings.json` (Read-only)

Configuration loaded on startup. Not modified by server.

```json
{
  "ap_name": "AscendoTrainBoard",
  "ap_password": "wifi_pass",
  "admin_users": ["admin", "john"]
}
```

### `data/users.json`

User accounts with password hashes.

```json
[
  {
    "username": "john_doe",
    "password_hash": "64-character-hex-hash",
    "salt": "64-character-hex-salt"
  }
]
```

**Security notes:**
- Salt: 32 random bytes (256 bits of entropy) encoded as 64-character hex
- Password hash: SHA256(password + salt)
- Session tokens: 32 random bytes (256 bits) encoded as 64-character hex

### `data/problems.json`

Climbing problems with embedded grades.

```json
[
  {
    "id": 1,
    "name": "The Crimp Master",
    "description": "Technical crimps",
    "created_by": "john_doe",
    "grade": 6,
    "sector_id": 1,
    "hold_sequence": [[0, 1], [1, 2], [2, 3]],
    "grades": [
      {
        "username": "jane_smith",
        "grade": 7,
        "stars": 4,
        "created_at": "1736950800"
      }
    ],
    "created_at": "1736950800",
    "updated_at": "1736950800"
  }
]
```

## Periodic Auto-Save

Server checks every **30 seconds** if data has changed. If modified, automatically saves to:
- `data/users.json`
- `data/problems.json`

This prevents data loss while minimizing write operations (important for ESP32 flash longevity).

## Admin Users

Users listed in `settings.json` under `admin_users` can:
- Edit/delete any user's problems
- All other permissions same as regular users

## Resource Usage

Optimized for ESP32:
- **Binary size**: ~2-3 MB (release build)
- **RAM usage**: ~5-10 MB (depends on data size)
- **Dependencies**: 97 crates (minimal for web server)

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
minimal-backend/
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

## License

MIT