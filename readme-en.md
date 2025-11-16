# AscendoTrainBoard

AscendoTrainBoard is a self-hosted climbing boulder tracking application, used in our climbing club. Users can submit custom climbing problems (boulders), view them on sector images with hold sequences, and provide grades and ratings. The app promotes community-driven problem creation and grading, making it easy to manage and share climbing routes.

## Features

- **User Authentication**: Basic registration, login and session tokens.
- **Problem Submission**: Create, edit, and delete climbing problems with hold sequences on predefined sectors.
- **Grading System**: Users can suggest grades and rate problems (1-5 stars), with averages calculated automatically.
- **Sector Management**: Static sectors with images and hold definitions, discovered dynamically from the filesystem.
- **Cross-Platform App**: Kotlin Multiplatform app for Android and Web, providing a seamless experience.
- **Lightweight Backend**: Rust-based server optimized for low-resource devices like ESP32 or Raspberry Pi.
- **Self-Hosted**: Run your own instance for privacy and control.
- **RESTful API**: Full API documented in OpenAPI spec for easy integration.
- **Admin Functions**: Admin users can manage all problems and users.

## Architecture

AscendoTrainBoard consists of two main components:

### Backend
- **Language**: Rust
- **Purpose**: Provides the REST API, handles authentication, data persistence, and serves sector images.
- **Deployment**: Designed for ESP32 or Raspberry Pi, with minimal resource usage (binary ~3-4 MB).
- **Data Storage**: JSON files for users, problems, and settings; periodic auto-save to prevent data loss.
- **Security**: SHA256 password hashing with salt, bearer token authentication, rate limiting.
    - note: you'll need to use an https certificate additionally to ensure the traffic is safe

### Frontend App
- **Language**: Kotlin Multiplatform
- **Targets**: Android and Web (using Compose Multiplatform).
- **Purpose**: User interface for browsing sectors, viewing/creating problems, and submitting grades.
- **API Integration**: Uses generated Kotlin models from the OpenAPI spec.

## Getting Started

### Prerequisites
- Rust toolchain (for backend)
- Kotlin Multiplatform setup (for app)
- Node.js and npm (for OpenAPI generator)

### Setting Up the Backend
1. Clone the repository and navigate to the `backend` directory.
2. Follow the setup instructions in [backend/README.md](backend/README.md):
   - Create data directories and initial JSON files.
   - Configure settings (e.g., admin users).
   - Optionally set up sectors with images and metadata.
3. Build and run the server:
   ```bash
   cargo run
   ```
   The server starts on `http://0.0.0.0:3000`.

### Setting Up the App
1. Navigate to the `app` directory.
2. Generate Kotlin models from the OpenAPI spec (see below).
3. Follow build instructions in [app/README.md](app/README.md) for Android or Web.

### Generating Kotlin Models
To generate Kotlin data models from the OpenAPI specification:
```bash
npx @openapitools/openapi-generator-cli generate \
  -i openapi.yaml \
  -g kotlin \
  -o app/composeApp/ \
  --global-property models \
  --package-name io.github.samolego.ascendo_trainboard.api.generated \
  --additional-properties=library=multiplatform,useCoroutines=true,dateLibrary=string
```

### Building the App
- **Android**: `./gradlew :composeApp:assembleDebug`
- **Web**: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` (for development)

## API Documentation
The full API is documented in [openapi.yaml](openapi.yaml).
It includes endpoints for authentication, sectors, problems, grades, and admin functions.

## Development
- **Backend**: Use `cargo check`, `cargo fmt`, and `RUST_LOG=debug cargo run` for development.
- **App**: Use IntelliJ IDEA or Android Studio for Kotlin development.
- **Contributing**: Ensure code follows best practices; run tests if available.

## Privacy
AscendoTrainBoard can be used anonymously for browsing. Account creation is optional. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for full details.
