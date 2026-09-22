package com.music.bitchord.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

/**
 * Bridges Xbox, PlayStation and Nintendo Switch Pro controllers — and TV
 * remotes — onto input the rest of the app already understands, rather than
 * teaching every screen a new input type.
 *
 * A TV remote's D-pad, its OK button and its Back button already drive three
 * things that exist in the app for free: Compose's own focus-search moves
 * between focusable elements on [KeyEvent.KEYCODE_DPAD_UP]/`DOWN`/`LEFT`/
 * `RIGHT`, `Modifier.clickable`'s built-in key handling fires a click on
 * [KeyEvent.KEYCODE_DPAD_CENTER] or [KeyEvent.KEYCODE_ENTER], and every
 * [androidx.activity.compose.BackHandler] in the app already answers
 * [KeyEvent.KEYCODE_BACK]. That is the whole reason a remote mostly works
 * here with zero app code — Android reports its buttons as exactly those
 * keys.
 *
 * A game controller is the part that doesn't: A/B/X/Y, the bumpers and Start
 * are a second, physically separate set of buttons Compose has no built-in
 * opinion about, reported as [KeyEvent.KEYCODE_BUTTON_A] and siblings. Rather
 * than add gamepad-specific handling to every screen, [remapKeyCode] rewrites
 * those buttons into the keycodes above at the point they're dispatched, so
 * the existing focus/click/back plumbing just runs. The media transport keys
 * land on the same [androidx.media3.session.MediaSession] callback a wired or
 * Bluetooth headset's buttons already reach
 * ([com.music.bitchord.playback.PlaybackService] never has to know a
 * controller exists either).
 *
 * Android normalizes an Xbox, a DualShock/DualSense and a Switch Pro
 * controller's face buttons to the same logical `BUTTON_A`/`BUTTON_B`
 * keycodes regardless of the label printed on the button, which is what lets
 * one mapping cover all three. The one documented exception: some Switch Pro
 * controllers report their physically-bottom button (labelled B, in
 * Nintendo's own A-right/B-bottom/X-top/Y-left layout) as `BUTTON_A` over
 * Bluetooth, since Android's mapping follows button *position* on a
 * standard layout rather than the label silkscreened on it. Confirm still
 * lands on the bottom face button either way, which is where a Switch user's
 * thumb already expects "select" to be — it just isn't the one lettered A.
 */
object ControllerInput {

    private const val GAMEPAD_SOURCES = InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK

    private fun KeyEvent.isFromGameController(): Boolean {
        val sources = device?.sources ?: source
        return (sources and GAMEPAD_SOURCES) != 0
    }

    /**
     * The keycode [event] should be redispatched as, or null to leave it
     * alone. Only ever remaps events actually sourced from a controller — a
     * physical keyboard that happens to send [KeyEvent.KEYCODE_BUTTON_A] is
     * not one, and TV remotes need no remapping at all, since they already
     * send the keys on the right of each mapping below.
     */
    fun remapKeyCode(event: KeyEvent): Int? {
        if (!event.isFromGameController()) return null
        return when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_DPAD_CENTER
            KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BACK
            KeyEvent.KEYCODE_BUTTON_START -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            KeyEvent.KEYCODE_BUTTON_L1 -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            KeyEvent.KEYCODE_BUTTON_R1 -> KeyEvent.KEYCODE_MEDIA_NEXT
            else -> null
        }
    }

    // ---- Left stick -> D-pad ----
    //
    // A controller's face buttons arrive as KeyEvents, but its analog sticks
    // arrive as a continuous stream of MotionEvents instead — there is no
    // "stick pushed left" key to remap. So a thumbstick is only useful for
    // the same focus navigation the D-pad already drives if something turns
    // that stream back into discrete steps: past a deadzone, one direction,
    // debounced so a held stick repeats at a readable pace instead of
    // flooding focus-search with a new move every frame.
    //
    // Some controllers additionally report their physical D-pad through
    // this same axis stream, as AXIS_HAT_X/AXIS_HAT_Y, rather than as
    // KeyEvents — those are checked first for exactly that reason.

    private const val AXIS_DEAD_ZONE = 0.5f
    private const val REPEAT_DELAY_MS = 220L

    private var lastAxisDirection: Int? = null
    private var lastAxisEventTime = 0L

    /**
     * The D-pad keycode a stick deflection past the deadzone corresponds to,
     * or null if [event] isn't a joystick axis move or the stick is centered.
     * Debounced internally — a held stick yields a new keycode at most once
     * per [REPEAT_DELAY_MS].
     */
    fun axisToDpadKeyCode(event: MotionEvent): Int? {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) == 0) return null
        if (event.action != MotionEvent.ACTION_MOVE) return null

        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val x = if (abs(hatX) > AXIS_DEAD_ZONE) hatX else event.getAxisValue(MotionEvent.AXIS_X)
        val y = if (abs(hatY) > AXIS_DEAD_ZONE) hatY else event.getAxisValue(MotionEvent.AXIS_Y)

        val direction = when {
            y <= -AXIS_DEAD_ZONE -> KeyEvent.KEYCODE_DPAD_UP
            y >= AXIS_DEAD_ZONE -> KeyEvent.KEYCODE_DPAD_DOWN
            x <= -AXIS_DEAD_ZONE -> KeyEvent.KEYCODE_DPAD_LEFT
            x >= AXIS_DEAD_ZONE -> KeyEvent.KEYCODE_DPAD_RIGHT
            else -> null
        }

        if (direction == null) {
            // Centered: the very next deflection should fire immediately
            // rather than waiting out a stale debounce window.
            lastAxisDirection = null
            return null
        }
        if (direction == lastAxisDirection && event.eventTime - lastAxisEventTime < REPEAT_DELAY_MS) {
            return null
        }
        lastAxisDirection = direction
        lastAxisEventTime = event.eventTime
        return direction
    }
}
