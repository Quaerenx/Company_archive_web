# Frog2 visual regression baseline

## Purpose

Detect unintended changes to the authenticated Archive layout after shared CSS edits.
The baseline covers dashboard, customers, maintenance, meetings, troubleshooting,
file repository, and my page at 360, 768, 1024, and 1440 pixel widths.

## Safety contract

- The runner accepts loopback HTTP development URLs only.
- It performs browser GET navigation only; it does not submit login or business forms.
- It never reads database configuration and never targets the production 8080 service.
- It copies an already authenticated Firefox profile to a temporary directory and
  removes that copy when the run finishes.
- Do not place a Firefox profile, cookies, credentials, or generated baselines in Git.
- Reduced motion is forced so the decorative canvas cannot create random pixel diffs.

## Create the first baseline

Use a Firefox profile that is already signed in to the development 18081 service.
The profile path is not printed or persisted by the runner.

```bash
FROG2_VISUAL_FIREFOX_PROFILE=/path/to/development-only/firefox-profile \
bash src/tools/visual-regression.sh baseline
```

The default output is:

```text
/opt/frog2-dev/visual-baselines/<frog2AssetVersion>/
```

## Compare after a CSS change

```bash
FROG2_VISUAL_FIREFOX_PROFILE=/path/to/development-only/firefox-profile \
bash src/tools/visual-regression.sh compare
```

The default tolerance is 1% of pixels with a 2% color fuzz. Override only when a
reviewed dynamic data change causes a known false positive:

```bash
FROG2_VISUAL_THRESHOLD_PERCENT=1.5 \
FROG2_VISUAL_FIREFOX_PROFILE=/path/to/development-only/firefox-profile \
bash src/tools/visual-regression.sh compare
```

Review every generated difference image instead of raising the threshold to make a
failure disappear.

## Current status

The route manifest and comparison runner are ready. Initial PNG files must be made
only after the current source is built, deployed to the development Tomcat, and
reviewed as the approved visual state. The current restricted execution environment
does not provide a safe authenticated Firefox profile, so no fabricated baseline
images are checked in.
