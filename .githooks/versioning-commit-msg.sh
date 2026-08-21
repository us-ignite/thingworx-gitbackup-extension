#!/usr/bin/env bash
# Maintained source for the Gradle-installed prepare-commit-msg hook.
set -euo pipefail

dry_run=false
if [[ ${1:-} == '--dry-run' ]]; then
  dry_run=true
  shift
fi
message_file=${1:?Usage: versioning-commit-msg.sh [--dry-run] <commit-message-file>}

fail() { printf 'versioning hook: %s\n' "$*" >&2; exit 1; }

header=$(sed -n '1p' "$message_file" | tr -d '\r')
message=$(tr -d '\r' < "$message_file")
conventional_header='^[a-z][a-z0-9-]*(\([^)]+\))?(!)?:[[:space:]]+.+$'
feat_header='^feat(\(|:|!)'
patch_header='^(fix|perf)(\(|:|!)'
if [[ ! $header =~ $conventional_header ]]; then
  # Only component-changing commits need a Conventional Commit header. Root-only
  # commits intentionally remain outside this release policy.
  conventional=false
else
  conventional=true
fi

mapfile -d '' staged_paths < <(git diff --cached --name-only -z)
jgit_changed=false
dap_changed=false
for path in "${staged_paths[@]}"; do
  case "$path" in
    extensions/jgit/.version|libraries/thingworx-dap/.version|libraries/thingworx-dap-runtime/.version) ;;
    extensions/jgit/*) jgit_changed=true ;;
    libraries/thingworx-dap/*|libraries/thingworx-dap-runtime/*) dap_changed=true ;;
  esac
done

targets=()
$jgit_changed && targets+=(extensions/jgit/.version)
$dap_changed && targets+=(libraries/thingworx-dap/.version libraries/thingworx-dap-runtime/.version)

# Version files are hook-owned. A manually staged version edit is never valid
# by itself and, for a release commit, must equal the calculation below.
all_version_files=(extensions/jgit/.version libraries/thingworx-dap/.version libraries/thingworx-dap-runtime/.version)
if ((${#targets[@]})); then
  $conventional || fail 'component changes require a Conventional Commit header (for example: fix: repair clone handling).'
fi

for version_file in "${all_version_files[@]}"; do
  if git diff --cached --quiet -- "$version_file"; then
    continue
  fi
  found=false
  for target in "${targets[@]}"; do [[ $target == "$version_file" ]] && found=true; done
  $found || fail "$version_file is managed by the hook and cannot be edited manually."
done

bump='none'
if ((${#targets[@]})); then
  if [[ $header =~ !: ]] || grep -qE '^BREAKING([[:space:]-])CHANGE:[[:space:]]+' <<<"$message"; then
    bump='major'
  elif [[ $header =~ $feat_header ]]; then
    bump='minor'
  elif [[ $header =~ $patch_header ]]; then
    bump='patch'
  fi
fi

next_version() {
  local current=$1 part=$2 major minor patch
  if [[ ! $current =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    fail "version '$current' must be numeric SemVer (X.Y.Z)."
  fi
  major=${BASH_REMATCH[1]}; minor=${BASH_REMATCH[2]}; patch=${BASH_REMATCH[3]}
  case "$part" in
    major) printf '%s.0.0' "$((major + 1))" ;;
    minor) printf '%s.%s.0' "$major" "$((minor + 1))" ;;
    patch) printf '%s.%s.%s' "$major" "$minor" "$((patch + 1))" ;;
  esac
}

bootstrap_version() {
  case "$1" in
    extensions/jgit/.version) printf '6.0.6' ;;
    libraries/thingworx-dap/.version|libraries/thingworx-dap-runtime/.version) printf '0.1.0' ;;
    *) fail "no bootstrap version is defined for $1" ;;
  esac
}

baseline_version() {
  case "$1" in
    extensions/jgit/.version)
      if git cat-file -e 'HEAD:.version' 2>/dev/null; then
        git show 'HEAD:.version' | tr -d '[:space:]'
      else
        bootstrap_version "$1"
      fi
      ;;
    *) bootstrap_version "$1" ;;
  esac
}

if [[ $dap_changed ]]; then
  core=$(git show ':libraries/thingworx-dap/.version' 2>/dev/null || git show 'HEAD:libraries/thingworx-dap/.version')
  runtime=$(git show ':libraries/thingworx-dap-runtime/.version' 2>/dev/null || git show 'HEAD:libraries/thingworx-dap-runtime/.version')
  core=$(tr -d '[:space:]' <<<"$core")
  runtime=$(tr -d '[:space:]' <<<"$runtime")
  [[ $core == "$runtime" ]] || fail 'DAP core and runtime versions must remain identical.'
fi

if [[ $bump == none ]]; then
  for target in "${targets[@]}"; do
    if git cat-file -e "HEAD:$target" 2>/dev/null; then
      git diff --cached --quiet -- "$target" || fail "$target must not change for a non-releasing commit type."
    else
      staged=$(git show ":$target" | tr -d '[:space:]')
      expected=$(bootstrap_version "$target")
      [[ $staged == "$expected" ]] || fail "$target bootstraps as $staged; expected $expected."
    fi
  done
  ((${#targets[@]})) && printf 'versioning hook: no release version update (%s).\n' "$header"
  exit 0
fi

for target in "${targets[@]}"; do
  if ! git cat-file -e "HEAD:$target" 2>/dev/null; then
    base=$(baseline_version "$target")
    expected=$base
    [[ $bump == none ]] || expected=$(next_version "$base" "$bump")
    printf 'versioning hook: %s -> %s (%s)\n' "$target" "$expected" "$bump"
    if ! $dry_run; then
      printf '%s\n' "$expected" > "$target"
      git add -- "$target"
    fi
    continue
  fi
  current=$(git show "HEAD:$target" | tr -d '[:space:]')
  proposed=$(next_version "$current" "$bump")
  if ! git diff --cached --quiet -- "$target"; then
    staged=$(git show ":$target" | tr -d '[:space:]')
    [[ $staged == "$proposed" ]] || fail "$target is staged as $staged; expected $proposed."
  fi
  printf 'versioning hook: %s -> %s (%s)%s\n' "$target" "$proposed" "$bump" "$($dry_run && printf ' [dry run]')"
  if ! $dry_run; then
    printf '%s\n' "$proposed" > "$target"
    git add -- "$target"
  fi
done
