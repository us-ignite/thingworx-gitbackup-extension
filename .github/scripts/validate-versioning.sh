#!/usr/bin/env bash
# Replays the commit policy in CI, catching commits created with --no-verify.
set -euo pipefail

range=${1:?Usage: validate-versioning.sh <base..head>}
fail() { printf 'versioning validation: %s\n' "$*" >&2; exit 1; }
conventional_header='^[a-z][a-z0-9-]*(\([^)]+\))?(!)?:[[:space:]]+.+$'
feat_header='^feat(\(|:|!)'
patch_header='^(fix|perf)(\(|:|!)'
[[ $range == *..* ]] || fail "range must be base..head: $range"
base=${range%%..*}
head=${range#*..}
git rev-parse --verify -q "$base^{commit}" >/dev/null || fail "unknown range base: $base"
git rev-parse --verify -q "$head^{commit}" >/dev/null || fail "unknown range head: $head"

next_version() {
  local current=$1 part=$2 major minor patch
  [[ $current =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]] || fail "invalid numeric SemVer: $current"
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
      if git cat-file -e "$parent:.version" 2>/dev/null; then
        git show "$parent:.version" | tr -d '[:space:]'
      else
        bootstrap_version "$1"
      fi
      ;;
    *) bootstrap_version "$1" ;;
  esac
}

mapfile -t commits < <(git rev-list --reverse "$range")
for commit in "${commits[@]}"; do
  parent=$(git rev-parse "$commit^")
  header=$(git log -1 --format=%s "$commit")
  message=$(git log -1 --format=%B "$commit")
  mapfile -t paths < <(git diff-tree --no-commit-id --name-only -r "$parent" "$commit")
  jgit=false; dap=false
  for path in "${paths[@]}"; do
    case "$path" in
      extensions/jgit/.version|libraries/thingworx-dap/.version|libraries/thingworx-dap-runtime/.version) ;;
      extensions/jgit/*) jgit=true ;;
      libraries/thingworx-dap/*|libraries/thingworx-dap-runtime/*) dap=true ;;
    esac
  done
  $jgit || $dap || continue
  [[ $header =~ $conventional_header ]] || fail "$commit has component changes but an invalid Conventional Commit header: $header"
  bump=none
  if [[ $header =~ !: ]] || grep -qE '^BREAKING([[:space:]-])CHANGE:[[:space:]]+' <<<"$message"; then bump=major
  elif [[ $header =~ $feat_header ]]; then bump=minor
  elif [[ $header =~ $patch_header ]]; then bump=patch; fi
  targets=(); $jgit && targets+=(extensions/jgit/.version); $dap && targets+=(libraries/thingworx-dap/.version libraries/thingworx-dap-runtime/.version)
  for target in "${targets[@]}"; do
    changed=false
    for path in "${paths[@]}"; do [[ $path == "$target" ]] && changed=true; done
    if ! git cat-file -e "$parent:$target" 2>/dev/null; then
      $changed || fail "$commit is missing bootstrap $target."
      new=$(git show "$commit:$target" | tr -d '[:space:]')
      base=$(baseline_version "$target")
      expected=$base
      [[ $bump == none ]] || expected=$(next_version "$base" "$bump")
      [[ $new == "$expected" ]] || fail "$commit bootstraps $target as $new; expected $expected."
      continue
    fi
    if [[ $bump == none ]]; then
      $changed && fail "$commit changes $target for a non-releasing type."
    else
      $changed || fail "$commit is missing required $target update."
      old=$(git show "$parent:$target" | tr -d '[:space:]')
      new=$(git show "$commit:$target" | tr -d '[:space:]')
      expected=$(next_version "$old" "$bump")
      [[ $new == "$expected" ]] || fail "$commit has $target=$new; expected $expected."
    fi
  done
  if $dap; then
    core=$(git show "$commit:libraries/thingworx-dap/.version" | tr -d '[:space:]')
    runtime=$(git show "$commit:libraries/thingworx-dap-runtime/.version" | tr -d '[:space:]')
    [[ $core == "$runtime" ]] || fail "$commit leaves DAP core ($core) and runtime ($runtime) mismatched."
  fi
done

core=$(tr -d '[:space:]' < libraries/thingworx-dap/.version)
runtime=$(tr -d '[:space:]' < libraries/thingworx-dap-runtime/.version)
[[ $core == "$runtime" ]] || fail "working tree leaves DAP core ($core) and runtime ($runtime) mismatched."
printf 'versioning validation passed for %s\n' "$range"
