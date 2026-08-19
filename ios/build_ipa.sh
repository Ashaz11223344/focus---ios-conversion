#!/bin/bash
# ==============================================================================
# Focus iOS Build & IPA Sideload Packaging Script
# ==============================================================================

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IOS_DIR="$PROJECT_ROOT/ios"
BUILD_DIR="$IOS_DIR/build"
ARCHIVE_PATH="$BUILD_DIR/Focus.xcarchive"
IPA_DIR="$BUILD_DIR/ipa"

echo "==> Step 1: Building Kotlin Multiplatform Shared XCFramework..."
cd "$PROJECT_ROOT"
./gradlew :shared:assembleSharedFocusXCFramework

echo "==> Step 2: Cleaning output build directory..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
mkdir -p "$IPA_DIR"

echo "==> Step 3: Archiving Xcode Project..."
xcodebuild archive \
    -project "$IOS_DIR/Focus.xcodeproj" \
    -scheme "Focus" \
    -configuration Release \
    -archivePath "$ARCHIVE_PATH" \
    -destination "generic/platform=iOS" \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGNING_ALLOWED=NO

echo "==> Step 4: Exporting Unsigned / Sideloadable IPA..."
mkdir -p "$IPA_DIR/Payload"
cp -R "$ARCHIVE_PATH/Products/Applications/Focus.app" "$IPA_DIR/Payload/"
cd "$IPA_DIR"
zip -r "$BUILD_DIR/Focus_sideload.ipa" Payload

echo "=============================================================================="
echo "SUCCESS: Sideloadable IPA generated at: $BUILD_DIR/Focus_sideload.ipa"
echo "You can sideload this IPA to your iPhone using AltStore, Sideloadly, or TrollStore."
echo "=============================================================================="
