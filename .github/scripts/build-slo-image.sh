#!/usr/bin/env bash
#
# Builds the Docker image for the YDB Java SDK SLO workload.
#
# The script assembles a temporary build context containing two checkouts
# side by side — the SDK source tree and the ydb-java-examples checkout —
# and feeds that context to `docker build` using the Dockerfile shipped
# inside `ydb-java-examples/slo/`.
#
# The Dockerfile takes care of building the SDK from source, installing it
# into an in-image local Maven repository, and then building the workload
# against that exact SDK version. So the script does not need any Maven /
# JDK setup on the host; only `docker` and standard POSIX tools.
#
# If the initial build fails and `--fallback-image` is provided, the script
# tags the fallback image as `--tag` and exits successfully. This mirrors
# the behaviour of the equivalent script in `ydb-go-sdk` and is used by the
# baseline build, where we want to keep going even if the historical commit
# can't be built any more.

set -euo pipefail

usage() {
    cat >&2 <<'EOF'
Usage:
  build-slo-image.sh \
    --sdk <path> \
    --examples <path> \
    --tag <docker-tag> \
    [--fallback-image <docker-tag>]

Options:
  --sdk             Path to the ydb-java-sdk checkout to build against.
  --examples        Path to the ydb-java-examples checkout that owns the
                    SLO workload sources (must contain slo/Dockerfile).
  --tag             Docker tag to assign to the built image
                    (e.g. ydb-app-current).
  --fallback-image  If the initial Docker build fails, tag this image as
                    --tag and exit successfully. Useful for the baseline
                    build, which may be unable to compile a historical
                    SDK commit.
EOF
}

die() {
    echo "ERROR: $*" >&2
    exit 1
}

sdk_dir=""
examples_dir=""
tag=""
fallback_image=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --sdk)
            sdk_dir="${2:-}"
            shift 2
            ;;
        --examples)
            examples_dir="${2:-}"
            shift 2
            ;;
        --tag)
            tag="${2:-}"
            shift 2
            ;;
        --fallback-image)
            fallback_image="${2:-}"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            die "Unknown argument: $1 (use --help)"
            ;;
    esac
done

if [[ -z "$sdk_dir" || -z "$examples_dir" || -z "$tag" ]]; then
    usage
    die "Incomplete argument set"
fi

[[ -d "$sdk_dir" ]] || die "--sdk does not exist: $sdk_dir"
[[ -d "$examples_dir" ]] || die "--examples does not exist: $examples_dir"

sdk_dir="$(cd "$sdk_dir" && pwd)"
examples_dir="$(cd "$examples_dir" && pwd)"

dockerfile="${examples_dir}/slo/Dockerfile"
[[ -f "$dockerfile" ]] || die "Dockerfile not found: $dockerfile"

# Assemble a build context that contains the two checkouts side by side.
# We use hard links where possible to avoid copying large amounts of data;
# `cp -al` falls back to a regular copy when hard links aren't supported
# (e.g. across filesystems on the GitHub runner cache).
context_dir="$(mktemp -d)"
trap 'rm -rf "$context_dir"' EXIT

echo "Assembling build context in $context_dir"
echo "  ydb-java-sdk:      $sdk_dir"
echo "  ydb-java-examples: $examples_dir"
echo "  tag:               $tag"

copy_tree() {
    local src="$1"
    local dst="$2"
    if cp -al "$src" "$dst" 2>/dev/null; then
        return 0
    fi
    cp -a "$src" "$dst"
}

copy_tree "$sdk_dir" "$context_dir/ydb-java-sdk"
copy_tree "$examples_dir" "$context_dir/ydb-java-examples"

# Drop any leftover .git directories from the build context so we don't ship
# them into image layers and don't confuse Maven plugins that probe for git.
find "$context_dir" -maxdepth 3 -type d -name '.git' -prune -exec rm -rf {} +

set +e
docker build \
    --platform linux/amd64 \
    -t "$tag" \
    -f "$dockerfile" \
    "$context_dir"
exit_code=$?
set -e

if [[ $exit_code -eq 0 ]]; then
    echo "Docker image $tag built successfully"
    exit 0
fi

echo "Docker build for $tag failed (exit code $exit_code)" >&2

if [[ -z "$fallback_image" ]]; then
    die "Docker build failed and --fallback-image is not set"
fi

echo "Falling back to image $fallback_image, tagging as $tag"
docker tag "$fallback_image" "$tag"
