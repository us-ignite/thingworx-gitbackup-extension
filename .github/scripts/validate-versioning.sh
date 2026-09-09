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
    apps/thingworx-jgit-extension/.version|extensions/jgit/.version) printf '6.0.6' ;;
    libraries/thingworx-dap/.version|libraries/thingworx-dap-runtime/.version) printf '0.1.0' ;;
    apps/thingworx-operator/.version|charts/thingworx-operator/.version) printf '0.1.0' ;;
    *) fail "no bootstrap version is defined for $1" ;;
  esac
}

resolve_jgit_version_at() {
  local ref=$1
  if git cat-file -e "$ref:apps/thingworx-jgit-extension/.version" 2>/dev/null; then
    git show "$ref:apps/thingworx-jgit-extension/.version" | tr -d '[:space:]'
  elif git cat-file -e "$ref:extensions/jgit/.version" 2>/dev/null; then
    git show "$ref:extensions/jgit/.version" | tr -d '[:space:]'
  elif git cat-file -e "$ref:.version" 2>/dev/null; then
    git show "$ref:.version" | tr -d '[:space:]'
  else
    return 1
  fi
}

has_jgit_version_at() {
  git cat-file -e "$1:apps/thingworx-jgit-extension/.version" 2>/dev/null \
    || git cat-file -e "$1:extensions/jgit/.version" 2>/dev/null \
    || git cat-file -e "$1:.version" 2>/dev/null
}

resolve_operator_version_at() {
  local ref=$1
  if git cat-file -e "$ref:apps/thingworx-operator/.version" 2>/dev/null; then
    git show "$ref:apps/thingworx-operator/.version" | tr -d '[:space:]'
  elif git cat-file -e "$ref:charts/thingworx-operator/.version" 2>/dev/null; then
    git show "$ref:charts/thingworx-operator/.version" | tr -d '[:space:]'
  else
    return 1
  fi
}

has_operator_version_at() {
  git cat-file -e "$1:apps/thingworx-operator/.version" 2>/dev/null \
    || git cat-file -e "$1:charts/thingworx-operator/.version" 2>/dev/null
}

baseline_version() {
  case "$1" in
    apps/thingworx-jgit-extension/.version|extensions/jgit/.version)
      if resolve_jgit_version_at "$parent" >/dev/null; then
        resolve_jgit_version_at "$parent"
      else
        bootstrap_version "$1"
      fi
      ;;
    apps/thingworx-operator/.version|charts/thingworx-operator/.version)
      if resolve_operator_version_at "$parent" >/dev/null; then
        resolve_operator_version_at "$parent"
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
  jgit=false; dap=false; operator=false
  for path in "${paths[@]}"; do
    case "$path" in
      apps/thingworx-jgit-extension/.version|extensions/jgit/.version|libraries/thingworx-dap/.version|libraries/thingworx-dap-runtime/.version|apps/thingworx-operator/.version|charts/thingworx-operator/.version) ;;
      apps/thingworx-jgit-extension/*|extensions/jgit/*) jgit=true ;;
      libraries/thingworx-dap/*|libraries/thingworx-dap-runtime/*) dap=true ;;
      apps/thingworx-operator/*|charts/thingworx-operator/*|images/thingworx-operator/*) operator=true ;;
    esac
  done
  $jgit || $dap || $operator || continue
  [[ $header =~ $conventional_header ]] || fail "$commit has component changes but an invalid Conventional Commit header: $header"
  bump=none
  if [[ $header =~ !: ]] || grep -qE '^BREAKING([[:space:]-])CHANGE:[[:space:]]+' <<<"$message"; then bump=major
  elif [[ $header =~ $feat_header ]]; then bump=minor
  elif [[ $header =~ $patch_header ]]; then bump=patch; fi
  targets=(); $jgit && targets+=(apps/thingworx-jgit-extension/.version); $dap && targets+=(libraries/thingworx-dap/.version libraries/thingworx-dap-runtime/.version); $operator && targets+=(apps/thingworx-operator/.version)
  for target in "${targets[@]}"; do
    changed=false
    legacy="extensions/jgit/.version"
    chartVersion="charts/thingworx-operator/.version"
    for path in "${paths[@]}"; do [[ $path == "$target" ]] && changed=true; done
    # For jgit, also consider legacy path renames as change
    if [[ $target == "apps/thingworx-jgit-extension/.version" ]]; then
      for path in "${paths[@]}"; do [[ $path == "$legacy" ]] && changed=true; done
      # If target is new path but only legacy changed (rename), treat as changed
      # Need to handle first commit after refactor where file moved
    fi
    if [[ $target == "apps/thingworx-operator/.version" ]]; then
      for path in "${paths[@]}"; do [[ $path == "$chartVersion" ]] && changed=true; done
    fi
    # Determine if parent had any version (new, legacy, or root)
    has_parent_version=false
    if [[ $target == "apps/thingworx-jgit-extension/.version" ]]; then
      has_jgit_version_at "$parent" && has_parent_version=true
    elif [[ $target == "apps/thingworx-operator/.version" ]]; then
      has_operator_version_at "$parent" && has_parent_version=true
    else
      git cat-file -e "$parent:$target" 2>/dev/null && has_parent_version=true
    fi
    if ! $has_parent_version; then
      $changed || fail "$commit is missing bootstrap $target."
      # Get new version from commit: prefer new path, fallback to legacy
      new=""
      if git cat-file -e "$commit:$target" 2>/dev/null; then
        new=$(git show "$commit:$target" | tr -d '[:space:]')
      elif [[ $target == "apps/thingworx-jgit-extension/.version" ]] && git cat-file -e "$commit:$legacy" 2>/dev/null; then
        new=$(git show "$commit:$legacy" | tr -d '[:space:]')
      elif [[ $target == "apps/thingworx-operator/.version" ]] && git cat-file -e "$commit:$chartVersion" 2>/dev/null; then
        new=$(git show "$commit:$chartVersion" | tr -d '[:space:]')
      else
        fail "$commit is missing $target content."
      fi
      base=$(baseline_version "$target")
      expected=$base
      [[ $bump == none ]] || expected=$(next_version "$base" "$bump")
      [[ $new == "$expected" ]] || fail "$commit bootstraps $target as $new; expected $expected."
      if [[ $target == "apps/thingworx-operator/.version" ]]; then
        # bootstrap must also mirror charts/.version
        if [[ $bump != "none" ]] && ! printf '%s\n' "${paths[@]}" | grep -qx "$chartVersion"; then
          fail "$commit is missing required $chartVersion update (must mirror canonical)."
        fi
        if [[ $bump == "none" ]] && printf '%s\n' "${paths[@]}" | grep -qx "$chartVersion"; then
          fail "$commit changes $chartVersion for a non-releasing type."
        fi
      fi
      continue
    fi
    if [[ $bump == none ]]; then
      # Allow rename-only changes without version bump during refactor
      is_rename_only=false
      if [[ $target == "apps/thingworx-jgit-extension/.version" ]]; then
        # If commit only moves file without version change, legacy deletion + new addition with same version is ok for non-releasing? But policy says non-releasing should not change version file.
        # During refactor, a non-releasing commit that only renames path should be allowed if version stays same.
        # Check if both legacy deletion and new addition present with same content as parent.
        if $changed; then
          old=$(resolve_jgit_version_at "$parent" | tr -d '[:space:]' || true)
          new=""
          if git cat-file -e "$commit:$target" 2>/dev/null; then new=$(git show "$commit:$target" | tr -d '[:space:]')
          elif git cat-file -e "$commit:$legacy" 2>/dev/null; then new=$(git show "$commit:$legacy" | tr -d '[:space:]'); fi
          if [[ $new == "$old" ]]; then
            is_rename_only=true
          fi
        fi
      fi
      if [[ $target == "apps/thingworx-operator/.version" ]] && $changed; then
        old=$(resolve_operator_version_at "$parent" | tr -d '[:space:]' || true)
        new=""
        if git cat-file -e "$commit:$target" 2>/dev/null; then new=$(git show "$commit:$target" | tr -d '[:space:]')
        elif git cat-file -e "$commit:$chartVersion" 2>/dev/null; then new=$(git show "$commit:$chartVersion" | tr -d '[:space:]'); fi
        if [[ $new == "$old" ]]; then is_rename_only=true; fi
      fi
      if $is_rename_only; then
        continue
      fi
      $changed && fail "$commit changes $target for a non-releasing type."
      if [[ $target == "apps/thingworx-operator/.version" ]] && printf '%s\n' "${paths[@]}" | grep -qx "$chartVersion"; then
        fail "$commit changes $chartVersion for a non-releasing type."
      fi
    else
      $changed || fail "$commit is missing required $target update."
      old=""
      if [[ $target == "apps/thingworx-jgit-extension/.version" ]]; then
        old=$(resolve_jgit_version_at "$parent" | tr -d '[:space:]')
      elif [[ $target == "apps/thingworx-operator/.version" ]]; then
        old=$(resolve_operator_version_at "$parent" | tr -d '[:space:]')
      else
        old=$(git show "$parent:$target" | tr -d '[:space:]')
      fi
      new=""
      if git cat-file -e "$commit:$target" 2>/dev/null; then new=$(git show "$commit:$target" | tr -d '[:space:]')
      elif [[ $target == "apps/thingworx-jgit-extension/.version" ]] && git cat-file -e "$commit:$legacy" 2>/dev/null; then new=$(git show "$commit:$legacy" | tr -d '[:space:]')
      elif [[ $target == "apps/thingworx-operator/.version" ]] && git cat-file -e "$commit:$chartVersion" 2>/dev/null; then new=$(git show "$commit:$chartVersion" | tr -d '[:space:]'); fi
      expected=$(next_version "$old" "$bump")
      [[ $new == "$expected" ]] || fail "$commit has $target=$new; expected $expected."
      if [[ $target == "apps/thingworx-operator/.version" ]]; then
        if ! printf '%s\n' "${paths[@]}" | grep -qx "$chartVersion"; then
          fail "$commit is missing required $chartVersion update (must mirror canonical)."
        fi
        chartVer=$(git show "$commit:$chartVersion" | tr -d '[:space:]')
        [[ $chartVer == "$expected" ]] || fail "$commit has $chartVersion=$chartVer; expected $expected."
      fi
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
