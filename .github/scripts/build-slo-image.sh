#!/usr/bin/env bash

set -euo pipefail

usage() {
    cat >&2 <<'EOF'
Usage:
  build-slo-image.sh \
    --sdk <path> \
    --examples <path> \
    --workload <query|jdbc|spring-data-jdbc|spring-data-jpa> \
    --tag <docker-tag> \
    [--fallback-image <docker-tag>]

Options:
  --sdk             Path to the ydb-java-sdk checkout to build against.
  --examples        Path to the ydb-java-examples checkout.
  --workload        SLO workload module to build.
  --tag             Docker tag to assign to the built image.
  --fallback-image  If the initial Docker build fails, tag this image as
                    --tag and exit successfully.
EOF
}

die() {
    echo "ERROR: $*" >&2
    exit 1
}

sdk_dir=""
examples_dir=""
workload=""
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
        --workload)
            workload="${2:-}"
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

if [[ -z "$sdk_dir" || -z "$examples_dir" || -z "$workload" || -z "$tag" ]]; then
    usage
    die "Incomplete argument set"
fi

workload_module=""
workload_jar=""
copy_libs="false"

case "$workload" in
    query)
        workload_module="slo-workload/query"
        workload_jar="ydb-slo-query-workload.jar"
        copy_libs="true"
        ;;
    jdbc)
        workload_module="slo-workload/jdbc"
        workload_jar="ydb-slo-jdbc-workload.jar"
        copy_libs="true"
        ;;
    spring-data-jdbc)
        workload_module="slo-workload/spring-data-jdbc"
        workload_jar="ydb-slo-spring-data-jdbc-workload.jar"
        ;;
    spring-data-jpa)
        workload_module="slo-workload/spring-data-jpa"
        workload_jar="ydb-slo-spring-data-jpa-workload.jar"
        ;;
    *)
        die "Unsupported workload: $workload"
        ;;
esac

[[ -d "$sdk_dir" ]] || die "--sdk does not exist: $sdk_dir"
[[ -d "$examples_dir" ]] || die "--examples does not exist: $examples_dir"

sdk_dir="$(cd "$sdk_dir" && pwd)"
examples_dir="$(cd "$examples_dir" && pwd)"

dockerfile="${examples_dir}/slo-workload/docker/Dockerfile.sdk"
[[ -f "$dockerfile" ]] || die "Dockerfile not found: $dockerfile"

context_dir="$(mktemp -d)"
trap 'rm -rf "$context_dir"' EXIT

echo "Assembling build context in $context_dir"
echo "  ydb-java-sdk:      $sdk_dir"
echo "  ydb-java-examples: $examples_dir"
echo "  workload:          $workload"
echo "  tag:               $tag"

copy_tree() {
    local src="$1"
    local dst="$2"
    mkdir -p "$dst"
    if cp -al "$src"/. "$dst"/ 2>/dev/null; then
        return 0
    fi
    cp -a "$src"/. "$dst"/
}

copy_tree "$sdk_dir" "$context_dir/ydb-java-sdk"
copy_tree "$examples_dir" "$context_dir/ydb-java-examples"

rm -rf "$context_dir/ydb-java-sdk/.git" "$context_dir/ydb-java-examples/.git"

for required in \
    "$context_dir/ydb-java-sdk/pom.xml" \
    "$context_dir/ydb-java-examples/slo-workload/docker/Dockerfile.sdk"
do
    [[ -f "$required" ]] || die "Build context missing required file: $required"
done

set +e
docker build \
    --platform linux/amd64 \
    --build-arg "WORKLOAD_MODULE=${workload_module}" \
    --build-arg "WORKLOAD_JAR=${workload_jar}" \
    --build-arg "COPY_LIBS=${copy_libs}" \
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
