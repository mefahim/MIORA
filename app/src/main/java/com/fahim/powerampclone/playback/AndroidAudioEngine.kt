package com.fahim.powerampclone.playback

import android.content.Context

/**
 * Backward-compatible audio engine wrapper extending [Media3AudioEngine].
 *
 * Ensures all existing invocations and call sites targeting [AndroidAudioEngine]
 * automatically leverage the upgraded AndroidX Media3 / ExoPlayer infrastructure.
 */
class AndroidAudioEngine(
    context: Context,
) : Media3AudioEngine(context)
