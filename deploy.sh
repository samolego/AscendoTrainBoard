#!/bin/bash

set -e

if [ $# -ne 2 ]; then
    echo "Usage: $0 <trainboard-*.tar.gz> <user@host>"
    exit 1
fi

TARFILE="$1"
DEST="$2"

if [ ! -f "$TARFILE" ]; then
    echo "Error: file '$TARFILE' does not exist"
    exit 1
fi

# --- Extract version ---
FILENAME="$(basename "$TARFILE")"

if [[ "$FILENAME" =~ trainboard-(.*)\.tar\.gz ]]; then
    VERSION="${BASH_REMATCH[1]}"
else
    VERSION="${FILENAME%.tar.gz}"
    echo "Warning: filename does not match trainboard-<version>.tar.gz, using '$VERSION' as version"
fi

echo "Version detected: $VERSION"

# --- Upload file first ---
scp "$TARFILE" "$DEST:/tmp/$FILENAME"

# --- Run everything in one single SSH session ---
ssh "$DEST" bash <<EOF
set -e

BASE="/mnt/dietpi_userdata/trainboard"
TARGET="\$BASE/$VERSION"
SYMLINK="\$HOME/ascendo_trainboard"

echo "Extracting archive..."
tar -xzf "/tmp/$FILENAME" -C "\$BASE"

echo "Cleaning uploaded tar..."
rm "/tmp/$FILENAME"

# Copy previous data directory if exists
if [ -L "\$SYMLINK" ] || [ -d "\$SYMLINK" ]; then
    if [ -d "\$SYMLINK/data" ]; then
        echo "Copying existing data directory..."
        cp -r "\$SYMLINK/data" "\$TARGET/"
    else
        echo "No old data directory found."
    fi
else
    echo "No previous installation found."
fi

# Create sectors symlink
echo "Creating sectors symlink..."
ln -s ../sectors "\$TARGET/sectors" 2>/dev/null || true

# TODO : folder is nested

# Allow execution
sudo setcap 'cap_net_bind_service=+ep' "\$TARGET/trainboard_service"

# Update main symlink
echo "Updating symlink \$SYMLINK → \$TARGET"
rm -f "\$SYMLINK"
ln -s "\$TARGET" "\$SYMLINK"

echo "Deployment completed successfully."
EOF
