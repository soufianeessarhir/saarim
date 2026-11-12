# Sarim — build, test, install, become Device Owner.
#
# One target for the whole job:
#
#make setup           # build + install + Device Owner + accessibility + launch
#             then first-run in-app: onboarding, recovery code, protection review, commitment
#
# The device defaults to the Waydroid container; pass DEVICE=adb for a USB phone.
# Full walkthrough: docs/BUILD_AND_RUN.md

SHELL := /bin/bash
.DEFAULT_GOAL := help

APP_ID        := com.sarim.app
ADMIN         := $(APP_ID)/.SarimDeviceAdminReceiver
ACCESSIBILITY := $(APP_ID)/.SarimAccessibilityService
APK_DEBUG     := app/build/outputs/apk/debug/app-debug.apk
APK_RELEASE   := app/build/outputs/apk/release/app-release.apk
DEVICE        ?= waydroid
# Waydroid's shell needs root on the host; a phone's does not.
SUDO          ?= sudo

# AGP accepts JDK 17-21 only. Override with `make build JAVA_HOME=/path/to/jdk-21`
# if the guess below picks the wrong one; gradle.properties has the fallback.
JAVA_HOME ?= $(firstword $(wildcard $(HOME)/.local/share/jdks/jdk-21* $(HOME)/.jdks/*21*))
GRADLE    := ./gradlew $(if $(JAVA_HOME),-Dorg.gradle.java.home=$(JAVA_HOME))

ifeq ($(DEVICE),waydroid)
  DSH     := $(SUDO) waydroid shell
  DPROBE  := waydroid status
  DINSTALL:= waydroid app install
  DLAUNCH := waydroid app launch $(APP_ID)
  DREMOVE := waydroid app remove $(APP_ID)
else ifeq ($(DEVICE),adb)
  DSH     := adb shell
  DPROBE  := adb devices
  DINSTALL:= adb install -r
  DLAUNCH := adb shell am start -n $(APP_ID)/.activities.MainActivity
  DREMOVE := adb uninstall $(APP_ID)
else
  $(error DEVICE must be waydroid or adb, got "$(DEVICE)")
endif

# ---------------------------------------------------------------- build & test

build: ## Build the debug APK
	$(GRADLE) :app:assembleDebug
	@echo "APK: $(APK_DEBUG)"

release: ## Build the minified release APK (signed only if dechainer.store* keys exist)
	$(GRADLE) :app:assembleRelease
	@echo "APK: $(APK_RELEASE)"

playstore: ## Build the Play-policy debug APK (drops the self-update path)
	$(GRADLE) :app:assembleDebug -PplayStoreBuild=true

test: ## Run the JVM unit tests
	$(GRADLE) :app:testDebugUnitTest
	@echo "Report: app/build/reports/tests/testDebugUnitTest/index.html"

check: test build ## Tests, then the APK

clean: ## Delete build outputs
	$(GRADLE) clean
	rm -rf build app/build

# ------------------------------------------------------------------- the device

doctor: ## Check the toolchain and the device before anything else
	@echo "== device ($(DEVICE)) =="
	@$(DPROBE) 2>/dev/null | sed 's/^/  /' \
		|| echo "  not reachable — start it with 'make waydroid-up' (or plug the phone in)"
	@echo "== JDK =="
	@if [ -n "$(JAVA_HOME)" ]; then "$(JAVA_HOME)/bin/java" -version 2>&1 | sed 's/^/  /'; \
		else echo "  no JDK 21 found — set JAVA_HOME=/path/to/jdk-21"; fi
	@echo "== Android SDK =="
	@if [ -f local.properties ]; then sed 's/^/  /' local.properties; \
		else echo "  local.properties missing — echo \"sdk.dir=/path/to/Android/Sdk\" > local.properties"; fi
	@echo "== Gradle =="
	@$(GRADLE) --version -q 2>/dev/null | grep -E "^(Gradle|Launcher JVM)" | sed 's/^/  /'

install: build ## Install the debug APK on the device
	$(DINSTALL) $(APK_DEBUG)

launch: ## Start Sarim on the device
	$(DLAUNCH)

# The per-device commands live in scripts/device-cmds.sh and are fed to the shell on
# stdin: `waydroid` parses argv itself, so a bare `-c` after `shell` is swallowed by
# waydroid's option parser and dies with "unrecognized arguments: -c set -e; ...".
# One `$(DSH)` call per target: sudo then prompts for the password once, not per line.
SCRIPTS   := scripts/device-cmds.sh

owner: ## Register Sarim as Device Owner (needs one user, no account, no other owner)
	@echo "==> Checking eligibility, then dpm set-device-owner $(ADMIN)"
	@$(DSH) sh -s -- owner $(ADMIN) < $(SCRIPTS)

privileges: ## Enable the accessibility service and grant the system permissions
	@$(DSH) sh -s -- privileges $(ACCESSIBILITY) $(APP_ID) < $(SCRIPTS)

status: ## Show owner registration, accessibility and app state
	@$(DSH) sh -s -- status $(APP_ID) < $(SCRIPTS)

logcat: ## Follow Sarim's logs (debug builds log through Timber)
	$(DSH) logcat -s Sarim

uninstall: ## Remove Sarim from the device (releases Device Owner first)
	@echo "==> Releasing device-owner $(ADMIN)"; \
	-$(DSH) dpm remove-active-admin $(ADMIN) --user 0 2>/dev/null || true; \
	$(DREMOVE) 2>/dev/null || true; \
	if waydroid app list 2>/dev/null | grep -qi "^packageName: $(APP_ID)$$"; then \
		echo "!! Sarim still installed — device-owner apps resist shell removal."; \
		echo "   Total wipe (drops owner, accounts, app data): make waydroid-reset  (needs sudo)"; \
	else \
		echo "Sarim removed."; \
	fi

# ---------------------------------------------------------------------- setup

setup: ## First-time setup: build, install, Device Owner, accessibility, launch
	$(MAKE) build
	$(MAKE) install
	$(MAKE) owner
	$(MAKE) privileges
	$(MAKE) launch
	@echo
	@$(MAKE) status

deploy-waydroid: ## Same as setup, inside the Waydroid container
	$(MAKE) setup DEVICE=waydroid

deploy-adb: ## Same as setup, on the USB phone
	$(MAKE) setup DEVICE=adb

# ------------------------------------------------------------------- waydroid

waydroid-up: ## Start Waydroid (running `sudo waydroid init` first if never initialised)
	@if [ ! -f /var/lib/waydroid/waydroid.cfg ]; then \
		echo "==> first run: downloading the Android image"; sudo waydroid init; fi
	@sudo systemctl enable --now waydroid-container
	@waydroid session start >/dev/null 2>&1 || true
	@sleep 2
	@waydroid status

waydroid-restart: ## Reboot Android inside the container (checks that state survives)
	sudo waydroid container restart
	@sleep 3
	@waydroid status

waydroid-down: ## Stop the session and the container
	-waydroid session stop
	-sudo waydroid container stop

waydroid-reset: ## Wipe Waydroid back to a fresh Android (drops owner, accounts, app data)
	@echo "==> Stopping session and container"
	-waydroid session stop 2>/dev/null || true
	-sudo waydroid container stop 2>/dev/null || true
	-sudo systemctl stop waydroid-container 2>/dev/null || true
	@mount | grep -q waydroid && { echo "!! waydroid still mounted — stopping service"; sudo systemctl stop waydroid-container; } || true
	@echo "==> Wiping Android userdata (apps, device owner, accounts)"
	rm -rf ~/.local/share/waydroid/data
	mkdir -p ~/.local/share/waydroid/data
	@echo "==> Wiping container runtime data and overlays"
	sudo rm -rf /var/lib/waydroid/data /var/lib/waydroid/overlay_rw /var/lib/waydroid/overlay_work
	sudo mkdir -p /var/lib/waydroid/data \
		/var/lib/waydroid/overlay_rw/system /var/lib/waydroid/overlay_rw/vendor \
		/var/lib/waydroid/overlay_work/system /var/lib/waydroid/overlay_work/vendor
	$(MAKE) waydroid-up

reset: waydroid-reset

help: ## Show this help
	@echo "Sarim — build, run and Device Owner automation"
	@echo "device: $(DEVICE)   (override with DEVICE=waydroid|adb)"
	@echo
	@grep -hE '^[a-zA-Z0-9_-]+:.*?## ' $(MAKEFILE_LIST) \
		| sort \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

.PHONY: build release playstore test check clean doctor install launch owner \
	privileges status logcat uninstall setup deploy-waydroid deploy-adb \
	waydroid-up waydroid-restart waydroid-down waydroid-reset reset help
