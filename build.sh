#!/bin/bash
set -euo pipefail

ALLOW_PORT_80=true

# ----------------------------
# Build backend (Rust)
# ----------------------------
echo "Building backend for Raspberry Pi 5 (aarch64)..."
(
    cd backend
    cargo build --release --target aarch64-unknown-linux-gnu
)

# ----------------------------
# Build frontend (Kotlin Multiplatform WASM)
# ----------------------------
echo "Building frontend..."
(
    cd app
    ./gradlew wasmJsBrowserDistribution --build-cache --configuration-cache
)

echo "Build completed successfully!"

# ----------------------------
# Prepare dist directory
# ----------------------------
mkdir -p build/

# ----------------------------
# Extract versionName
# ----------------------------
version=$(grep -oP '(?<=^versionName=).+' app/composeApp/gradle.properties || true)
if [[ -z "$version" ]]; then
    echo "Warning: Could not extract version. Using 'dev'."
    version="dev"
fi

dist="build/$version"
mkdir -p "$dist"

# ----------------------------
# Copy backend binary
# ----------------------------
backend_bin="backend/target/aarch64-unknown-linux-gnu/release/trainboard_service"

if [[ -f "$backend_bin" ]]; then
    cp "$backend_bin" "$dist/"
else
    echo "Warning: Backend binary not found at $backend_bin"
fi

# ----------------------------
# Copy frontend WASM distribution
# ----------------------------
frontend_dist="app/composeApp/build/dist/wasmJs/productionExecutable"
if [[ -d "$frontend_dist" ]]; then
    cp -r "$frontend_dist" "$dist/page"
else
    echo "Warning: Frontend dist not found at $frontend_dist"
fi

# ----------------------------
# Enable CAP_NET_BIND_SERVICE (default ON)
# ----------------------------
if $ALLOW_PORT_80; then
    echo "Granting permission to bind to port 80..."
    if command -v setcap >/dev/null 2>&1; then
        sudo setcap 'cap_net_bind_service=+ep' "$dist/trainboard_service" \
            || echo "Warning: Failed to set capabilities (requires sudo)"
    else
        echo "Warning: setcap not found — cannot enable port 80 binding."
    fi
else
    echo "Skipping port 80 capability setup."
fi

# ----------------------------
# Create tar.gz package
# ----------------------------
package="trainboard-$version.tar.gz"

echo "Packaging into $package..."
(
    cd build
    tar -czf "$package" "$version"
)

echo "Package created at: build/$package"
