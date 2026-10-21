#!/usr/bin/env bash
# NexaTV — interactive release wizard.
#
# Walks the owner through the sideload release pipeline (ADR-0003): one-time signing setup,
# cutting a release (CHANGELOG -> tag -> CI -> GitHub Release), installing on the TV, and
# verifying a published APK. It tells you exactly what to do and what to copy at each step.
#
# Run from the repo root:   bash tools/release.sh
# Nothing here is destructive without an explicit y/N confirmation. No secrets are written to Git.
set -uo pipefail

# ---------- pretty output ----------
if [ -t 1 ]; then
  B=$'\e[1m'; DIM=$'\e[2m'; GRN=$'\e[32m'; YEL=$'\e[33m'; RED=$'\e[31m'; CYN=$'\e[36m'; RST=$'\e[0m'
else
  B=""; DIM=""; GRN=""; YEL=""; RED=""; CYN=""; RST=""
fi
hr()   { printf '%s\n' "${DIM}────────────────────────────────────────────────────────────${RST}"; }
head1(){ echo; hr; printf '%s\n' "${B}$*${RST}"; hr; }
say()  { printf '%s\n' "$*"; }
ok()   { printf '%s\n' "${GRN}✓ $*${RST}"; }
warn() { printf '%s\n' "${YEL}! $*${RST}"; }
err()  { printf '%s\n' "${RED}✗ $*${RST}"; }
copy() { echo; printf '%s\n' "${CYN}${B}  ▼ COPY THIS ▼${RST}"; printf '%s\n' "${CYN}$*${RST}"; echo; }
pause(){ read -r -p "${DIM}Press Enter to continue…${RST}" _; }
ask()  { local p="$1" d="${2:-}" a; if [ -n "$d" ]; then read -r -p "$p [$d]: " a; echo "${a:-$d}"; else read -r -p "$p: " a; echo "$a"; fi; }
confirm(){ local a; read -r -p "$1 ${DIM}(y/N)${RST} " a; [[ "$a" =~ ^[Yy]$ ]]; }
have() { command -v "$1" >/dev/null 2>&1; }

# ---------- environment ----------
cd "$(git rev-parse --show-toplevel 2>/dev/null)" 2>/dev/null || { err "Run this from inside the NexaTV git repo."; exit 1; }
REPO_ROOT="$(pwd)"
SLUG="$(gh repo view --json nameWithOwner --jq .nameWithOwner 2>/dev/null || true)"

find_adb() {
  if have adb; then echo adb; return; fi
  local p="${LOCALAPPDATA:-}/Android/Sdk/platform-tools/adb.exe"
  [ -x "$p" ] && { echo "$p"; return; }
  echo ""
}
find_apksigner() {
  local bt
  bt="$(ls -d "${LOCALAPPDATA:-}/Android/Sdk/build-tools/"*/ 2>/dev/null | sort -V | tail -1)"
  [ -n "$bt" ] && [ -x "${bt}apksigner.bat" ] && { echo "${bt}apksigner.bat"; return; }
  have apksigner && { echo apksigner; return; }
  echo ""
}

# versionName (e.g. 1.2.3 or 1.2.3-rc) -> versionCode (MAJOR*10000 + MINOR*100 + PATCH)
ver_to_code() {
  local core="${1%%-*}" a b c
  IFS=. read -r a b c <<<"$core"
  [[ "$a" =~ ^[0-9]+$ && "$b" =~ ^[0-9]+$ && "$c" =~ ^[0-9]+$ ]] || { echo ""; return 1; }
  echo $(( 10#$a*10000 + 10#$b*100 + 10#$c ))
}

# =====================================================================================
# 1) FIRST-TIME SETUP — keystore + GitHub secrets + branch protection
# =====================================================================================
cmd_setup() {
  head1 "First-time setup — signing keystore & GitHub secrets"
  say "You do this ${B}once${RST}. The signing key becomes the app's identity forever:"
  say "  • same key  -> in-place updates that keep user data"
  say "  • lost key  -> ${RED}fatal${RST}: the app can never be updated in place again."
  say "Full background: docs/technical/RELEASE-SIGNING.md"
  echo
  have keytool || { err "keytool not found (install a JDK / add it to PATH)."; return; }

  # --- keystore ---
  local KS
  KS="$(ask "Where should the keystore live? (absolute path)" "$HOME/nexatv-release.jks")"
  if [ -f "$KS" ]; then
    warn "A file already exists at: $KS"
    confirm "Reuse that existing keystore (skip generation)?" || { err "Pick a different path and re-run."; return; }
  else
    say "PKCS12 keystores use ${B}one${RST} password for both the store and the key."
    local P1 P2
    while :; do
      read -r -s -p "Choose a strong keystore password: " P1; echo
      read -r -s -p "Repeat it: " P2; echo
      [ -n "$P1" ] && [ "$P1" = "$P2" ] && break
      err "Empty or mismatched — try again."
    done
    say "Generating RSA-4096, ~30-year keystore (alias: nexatv)…"
    if ! keytool -genkeypair -v -keystore "$KS" -alias nexatv -keyalg RSA -keysize 4096 \
        -validity 10950 -storetype PKCS12 -storepass "$P1" -keypass "$P1" -dname "CN=NexaTV"; then
      err "keytool failed."; return
    fi
    STOREPASS="$P1"
    ok "Keystore created: $KS"
  fi
  # need the password for the secret even when reusing
  if [ -z "${STOREPASS:-}" ]; then
    read -r -s -p "Enter the keystore password (for the GitHub secret): " STOREPASS; echo
  fi

  warn "BACK UP THIS KEYSTORE NOW, in two places (password manager + an encrypted offline drive)."
  warn "If you lose it, you can never update the installed app in place again."
  pause

  # --- GitHub secrets ---
  head1 "GitHub secrets (protected 'release' Environment)"
  if have gh && [ -n "$SLUG" ] && gh auth status >/dev/null 2>&1 && confirm "Set the 4 secrets automatically with gh (recommended)?"; then
    say "Ensuring the 'release' Environment exists on $SLUG…"
    gh api -X PUT "repos/$SLUG/environments/release" >/dev/null 2>&1 && ok "Environment 'release' ready." || warn "Could not create it via API — create it in the web UI, then re-run."
    base64 -w0 "$KS" | gh secret set KEYSTORE_BASE64   --env release --repo "$SLUG" && ok "KEYSTORE_BASE64 set"
    printf %s "$STOREPASS" | gh secret set KEYSTORE_PASSWORD --env release --repo "$SLUG" && ok "KEYSTORE_PASSWORD set"
    printf %s "nexatv"     | gh secret set KEY_ALIAS         --env release --repo "$SLUG" && ok "KEY_ALIAS set"
    printf %s "$STOREPASS" | gh secret set KEY_PASSWORD      --env release --repo "$SLUG" && ok "KEY_PASSWORD set (same as store for PKCS12)"
    ok "All four secrets set in the 'release' Environment."
  else
    say "Manual path. In GitHub: ${B}Settings → Environments → New environment → 'release'${RST},"
    say "then add these four ${B}secrets${RST}:"
    local B64="$REPO_ROOT/nexatv-keystore.b64.txt"
    base64 -w0 "$KS" > "$B64"
    echo
    say "  1) ${B}KEYSTORE_BASE64${RST} — the base64 is in this file (open it, copy ALL, paste):"
    copy "$B64"
    warn "DELETE that .b64 file after pasting:  rm \"$B64\""
    say "  2) ${B}KEYSTORE_PASSWORD${RST} — your keystore password"
    say "  3) ${B}KEY_ALIAS${RST}:"; copy "nexatv"
    say "  4) ${B}KEY_PASSWORD${RST} — the same password as KEYSTORE_PASSWORD (PKCS12)"
  fi
  pause

  # --- branch protection ---
  head1 "Protect 'main' and the 'release' Environment (web UI)"
  say "In ${B}Settings → Environments → release${RST}: add yourself as a required reviewer, and"
  say "restrict deployment branches to tags/main so secrets aren't exposed from random branches."
  echo
  say "In ${B}Settings → Branches → Add rule${RST} for ${B}main${RST}:"
  say "  • Require a pull request before merging"
  say "  • Require status checks to pass  → select the ${B}CI${RST} checks"
  say "  • Require linear history"
  ok "Setup complete. You're ready to cut a release (menu option 2)."
}

# =====================================================================================
# 2) CUT A RELEASE — changelog -> tag -> CI -> GitHub Release
# =====================================================================================
cmd_release() {
  head1 "Cut a release"
  have gh || warn "gh CLI not found — you'll push the tag, but can't watch CI from here."

  local PREV="" PREVCODE=0
  if have gh && [ -n "$SLUG" ]; then
    PREV="$(gh release list --limit 1 --json tagName --jq '.[0].tagName' 2>/dev/null || true)"
    [ -n "$PREV" ] && { PREVCODE="$(ver_to_code "${PREV#v}")"; say "Latest published release: ${B}$PREV${RST} (versionCode $PREVCODE)"; } \
                   || say "No prior GitHub Release found — this will be the first."
  fi

  local VER TAG CODE
  VER="$(ask "New version (SemVer, no leading v; e.g. 0.1.0 or 0.1.0-rc)")"
  [ -n "$VER" ] || { err "No version entered."; return; }
  TAG="v$VER"
  CODE="$(ver_to_code "$VER")" || { err "Can't parse '$VER' as MAJOR.MINOR.PATCH."; return; }
  say "  tag = ${B}$TAG${RST}   versionCode = ${B}$CODE${RST}"
  if [ -n "$PREV" ] && [ "$CODE" -le "$PREVCODE" ]; then
    err "versionCode $CODE is not greater than the last release ($PREVCODE). CI would reject this. Bump the version."
    return
  fi
  if git rev-parse "$TAG" >/dev/null 2>&1; then err "Tag $TAG already exists locally."; return; fi

  # --- CHANGELOG move ---
  head1 "Step 1 — move CHANGELOG '[Unreleased]' to '[$VER]'"
  local DATE; DATE="$(date +%F)"
  if ! grep -q '^## \[Unreleased\]' CHANGELOG.md; then
    warn "No '## [Unreleased]' heading found — edit CHANGELOG.md by hand, then re-run."
  elif [ "$(grep -c '^## \[Unreleased\]' CHANGELOG.md)" -ne 1 ]; then
    warn "Multiple '## [Unreleased]' headings — fix CHANGELOG.md by hand, then re-run."
  else
    local TMP; TMP="$(mktemp)"
    awk -v ver="$VER" -v d="$DATE" '
      !done && /^## \[Unreleased\]/ { print "## [Unreleased]"; print ""; print "## [" ver "] - " d; done=1; next }
      { print }
    ' CHANGELOG.md > "$TMP"
    echo "Preview (top of file):"; hr; head -n 14 "$TMP"; hr
    if confirm "Apply this CHANGELOG change and commit it?"; then
      mv "$TMP" CHANGELOG.md
      git -c user.name="ViacheslavMelnichenko" -c user.email="melnichenkoviatcheslav@gmail.com" \
        commit -q -am "docs(changelog): release $VER" && ok "CHANGELOG updated & committed."
      warn "Push this commit (PR to main per your branch rules) before tagging, so the tag points at it."
      confirm "Push current branch to origin now?" && git push
    else
      rm -f "$TMP"; warn "Skipped. The release notes are taken from the [$VER] CHANGELOG section — make sure it exists."
    fi
  fi

  # --- CI check ---
  head1 "Step 2 — confirm CI is green"
  if have gh && [ -n "$SLUG" ]; then
    gh run list --branch main --limit 3 2>/dev/null || true
    confirm "Is CI green on the commit you're about to tag?" || { warn "Wait for CI, then re-run."; return; }
  else
    warn "Check the Actions tab: the CI workflow must be green on this commit."
    confirm "Is CI green?" || return
  fi

  # --- tag & push ---
  head1 "Step 3 — tag & push (this triggers release.yml)"
  say "About to run:"
  copy "git tag -a $TAG -m \"NexaTV $VER\"  &&  git push origin $TAG"
  confirm "Create and push $TAG now?" || { warn "Aborted — no tag pushed."; return; }
  git tag -a "$TAG" -m "NexaTV $VER" || { err "git tag failed."; return; }
  if git push origin "$TAG"; then ok "Pushed $TAG — the Release workflow is starting."; else
    err "Push failed. Remove the local tag with:  git tag -d $TAG"; return; fi

  # --- watch ---
  head1 "Step 4 — watch the release build"
  if have gh && [ -n "$SLUG" ]; then
    say "Streaming the run (Ctrl-C to stop watching; the build keeps going)…"
    sleep 4
    local RID; RID="$(gh run list --workflow=release.yml --limit 1 --json databaseId --jq '.[0].databaseId' 2>/dev/null || true)"
    [ -n "$RID" ] && gh run watch "$RID" --exit-status || warn "Open the Actions tab to watch it."
    echo
    say "When it's green, the signed APK is attached to the GitHub Release."
    confirm "Open the Releases page now?" && gh release view "$TAG" --web 2>/dev/null || true
  else
    say "Open the Actions tab and watch the 'Release' workflow for tag $TAG."
  fi
  ok "Release cut. Next: install on the TV (menu option 3) and run the on-device smoke test."
  say "${DIM}Checklist: docs/testing/RELEASE-CHECKLIST.md${RST}"
}

# =====================================================================================
# 3) INSTALL / UPDATE ON THE TV
# =====================================================================================
cmd_install() {
  head1 "Install / update NexaTV on the TV"
  local ADB; ADB="$(find_adb)"
  [ -n "$ADB" ] || { err "adb not found. Expected at %LOCALAPPDATA%/Android/Sdk/platform-tools/adb.exe"; return; }

  say "Devices currently visible to adb:"; "$ADB" devices; echo
  if ! "$ADB" get-state >/dev/null 2>&1; then
    warn "No device connected. On the TV enable wireless debugging, then run:"
    copy "\"$ADB\" connect <TV-IP>:<PORT>"
    say "(The IP/port are runtime data — never commit them.)"
    confirm "Try again now?" || return
    "$ADB" devices
  fi

  local APK; APK="$(ask "Path to the downloaded release APK (nexatv-X.Y.Z.apk)")"
  [ -f "$APK" ] || { err "No file at: $APK"; return; }

  head1 "Installing (in-place update, data preserved)"
  say "Running:"; copy "\"$ADB\" install -r \"$APK\""
  confirm "Install now?" || return
  if "$ADB" install -r "$APK"; then ok "Installed."; else
    err "Install failed. If it's a signer mismatch, this APK was signed with a different key."; return; fi

  head1 "First release only — switch off the .debug build"
  say "The release package is ${B}com.nexatv.launcher${RST}; the build currently acting as HOME is the"
  say "${B}.debug${RST} one. The first release installs ${B}alongside${RST} it, so you must re-point HOME:"
  say "  1) On the TV: ${B}Settings → Accessibility${RST} → enable the ${B}release${RST} NexaTV override service."
  say "  2) Reboot and confirm the release app comes up as HOME:"; copy "\"$ADB\" reboot"
  say "  3) (optional) remove the old debug build:"; copy "\"$ADB\" uninstall com.nexatv.launcher.debug"
  echo
  head1 "Smoke test (mandatory — the release build is R8-minified)"
  say "  □ Home grid renders, no crash / stuck Loading"
  say "  □ NexaTV is HOME (stock launcher is replaced via the accessibility override)"
  say "  □ Survives a reboot as HOME"
  say "  □ App tiles (icons+labels), favorites, and the weather widget all render"
  say "  □ Cold start feels snappy"
  warn "If anything is wrong, roll back:  \"$ADB\" install -r nexatv-<previous>.apk"
  ok "Done. Record any perf numbers in the Release / CHANGELOG (perf is recorded, not gated, for now)."
}

# =====================================================================================
# 4) VERIFY A PUBLISHED APK
# =====================================================================================
cmd_verify() {
  head1 "Verify a published APK (signature + hashes)"
  local APK; APK="$(ask "Path to the APK to verify")"
  [ -f "$APK" ] || { err "No file at: $APK"; return; }
  local AS; AS="$(find_apksigner)"
  if [ -n "$AS" ]; then
    say "Signing certificate(s):"; "$AS" verify --print-certs "$APK" | grep -i 'SHA-256\|signer' || true
  else
    warn "apksigner not found — skipping cert print (expected under Android SDK build-tools)."
  fi
  echo; say "APK SHA-256 (must match the GitHub Release notes):"
  if have sha256sum; then sha256sum "$APK"; else certutil -hashfile "$APK" SHA256 2>/dev/null || warn "No sha256 tool available."; fi
  ok "Compare both values against the Release notes before trusting this APK on the TV."
}

# =====================================================================================
# MENU
# =====================================================================================
menu() {
  head1 "NexaTV release wizard"
  [ -n "$SLUG" ] && say "Repo: ${B}$SLUG${RST}" || warn "gh not authenticated — some steps will be manual."
  echo
  say "  ${B}1${RST}) First-time setup   — keystore + GitHub secrets + branch protection (once)"
  say "  ${B}2${RST}) Cut a release      — CHANGELOG → tag → CI → GitHub Release"
  say "  ${B}3${RST}) Install on the TV  — adb install -r + smoke-test checklist"
  say "  ${B}4${RST}) Verify an APK      — signature + SHA-256"
  say "  ${B}q${RST}) Quit"
  echo
  local c; c="$(ask "Choose" "")"
  case "$c" in
    1) cmd_setup ;;
    2) cmd_release ;;
    3) cmd_install ;;
    4) cmd_verify ;;
    q|Q) exit 0 ;;
    *) warn "Unknown choice." ;;
  esac
  echo; pause
}

while :; do menu; done
