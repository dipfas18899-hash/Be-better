# Game controller and TV remote support

## Overview

BitChord can be navigated with an Xbox controller, a PlayStation
DualShock/DualSense controller, a Nintendo Switch Pro controller, or a TV
remote — paired over Bluetooth or plugged in over USB — with no per-screen
changes required. This is implemented as a single input-translation layer,
[`ControllerInput`](../app/src/main/java/com/music/bitchord/input/ControllerInput.kt),
rather than as custom handling scattered across the UI.

## Why one file is enough

A TV remote's D-pad, OK button and Back button are already, by default,
exactly the keys that drive three things already built into the app: Compose's
focus search (D-pad), `clickable`'s built-in key handling (OK/Enter), and
every `BackHandler` in the app (Back). A remote therefore already mostly works
without any app-specific code — Android reports its buttons as those same
keys.

A game controller's face buttons, bumpers and Start button are a second set of
buttons Android reports under different keycodes (`BUTTON_A`, `BUTTON_B`,
`BUTTON_L1`, and so on), which Compose has no built-in opinion about. Rather
than teach every screen to also listen for those, `MainActivity` intercepts
them in `dispatchKeyEvent`/`onGenericMotionEvent` and rewrites them into the
keycodes above before they reach the rest of the app, so the same
focus/click/back plumbing a remote already uses handles a controller too.

## Mapping

| Input                      | Action                              |
| --------------------------- | ------------------------------------ |
| D-pad / left stick           | Move focus between on-screen items   |
| A                             | Select / activate the focused item   |
| B                             | Back                                 |
| Start                         | Play / pause                         |
| Left bumper (L1)              | Previous track                       |
| Right bumper (R1)             | Next track                           |

The play/pause and skip mappings are dispatched as standard Android media
keys, which land on the same `MediaSession` callback a wired or Bluetooth
headset's buttons already reach — `PlaybackService` needs no changes to
support them.

## Known limitation

Some Switch Pro controllers report their physically-bottom face button
(labelled B on the controller) as `BUTTON_A` over Bluetooth, because Android's
mapping follows button position on a standard layout rather than the label
printed on it. Select still lands on the bottom face button either way, which
is where a Switch user's thumb already expects it — it just isn't the button
lettered A.

## Scope

This covers input, not layout: BitChord's UI is not a 10-foot/leanback
redesign, and the manifest does not declare a
`LEANBACK_LAUNCHER` entry, so it won't appear in an Android TV home screen's
app row on its own. It runs, and is fully navigable by remote or controller,
once launched — by sideloading directly or through a third-party TV launcher.
The `android.hardware.touchscreen` feature is declared as not required
specifically so it can be installed on a device with no touchscreen at all.
