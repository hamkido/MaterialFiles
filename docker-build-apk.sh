#!/bin/bash
set -e

# 构建类型，默认为 release
BUILD_TYPE="${1:-release}"

echo "==> 构建 Docker 镜像..."
docker build -t materialfiles-builder .

echo "==> 在 Docker 容器中构建 APK (${BUILD_TYPE})..."
mkdir -p apk

docker run --rm \
    -v "$(pwd):/project" \
    -v "$(pwd)/apk:/output" \
    -e VERSION="${VERSION:-}" \
    -e VERSIONL="${VERSIONL:-}" \
    -e STORE_FILE="${STORE_FILE:-}" \
    -e STORE_PASSWORD="${STORE_PASSWORD:-}" \
    -e KEY_ALIAS="${KEY_ALIAS:-}" \
    -e KEY_PASSWORD="${KEY_PASSWORD:-}" \
    materialfiles-builder \
    bash -c "
        cd /project
        if [ '${BUILD_TYPE}' = 'release' ]; then
            ./gradlew assembleRelease --no-daemon
            cp -f app/build/outputs/apk/release/*.apk /output/ 2>/dev/null || true
        else
            ./gradlew assembleDebug --no-daemon
            cp -f app/build/outputs/apk/debug/*.apk /output/ 2>/dev/null || true
        fi
    "

echo "==> 构建完成！APK 文件位于 apk/ 目录"
ls -la apk/

