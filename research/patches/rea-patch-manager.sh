#!/bin/bash
# Reversible backup and restore script for REA PRoot stat patch
# Target package: /usr/local/lib/nodejs/lib/node_modules/rea-agents

REA_DIR="/usr/local/lib/nodejs/lib/node_modules/rea-agents"

case "$1" in
  revert)
    echo "Reverting REA files to upstream strict stat checking (ctimeMs, mtimeMs, mode)..."
    sed -i 's/expected.size === observed.size;/expected.mode === observed.mode \&\& expected.size === observed.size \&\& expected.mtimeMs === observed.mtimeMs \&\& expected.ctimeMs === observed.ctimeMs;/' "$REA_DIR/dist/filesystem/RegularFile.js"
    sed -i 's/expected.size === observed.size;/expected.mode === observed.mode \&\& expected.size === observed.size \&\& expected.mtimeMs === observed.mtimeMs \&\& expected.ctimeMs === observed.ctimeMs;/' "$REA_DIR/dist/cli-runtime/OwnedRegularFile-D1f5nUCx.js"
    sed -i 's/expected.size === observed.size;/expected.mode === observed.mode \&\& expected.size === observed.size \&\& expected.mtimeMs === observed.mtimeMs \&\& expected.ctimeMs === observed.ctimeMs;/' "$REA_DIR/dist/mcp/machoContainer-CMSxUBlK.js"
    echo "Revert complete."
    ;;
  apply)
    echo "Applying PRoot-compatible stat patch (dev, ino, size)..."
    sed -i 's/expected.mode === observed.mode && expected.size === observed.size && expected.mtimeMs === observed.mtimeMs && expected.ctimeMs === observed.ctimeMs;/expected.size === observed.size;/' "$REA_DIR/dist/filesystem/RegularFile.js"
    sed -i 's/expected.mode === observed.mode && expected.size === observed.size && expected.mtimeMs === observed.mtimeMs && expected.ctimeMs === observed.ctimeMs;/expected.size === observed.size;/' "$REA_DIR/dist/cli-runtime/OwnedRegularFile-D1f5nUCx.js"
    sed -i 's/expected.mode === observed.mode && expected.size === observed.size && expected.mtimeMs === observed.mtimeMs && expected.ctimeMs === observed.ctimeMs;/expected.size === observed.size;/' "$REA_DIR/dist/mcp/machoContainer-CMSxUBlK.js"
    echo "Apply complete."
    ;;
  *)
    echo "Usage: $0 {apply|revert}"
    exit 1
    ;;
esac
