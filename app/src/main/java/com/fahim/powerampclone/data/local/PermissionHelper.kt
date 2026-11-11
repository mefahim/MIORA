package com.fahim.powerampclone.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {

    /** Optional test hook to override permission checks in unit tests */
    var permissionChecker: ((Context) -> Boolean)? = null

    /** Returns the exact permission string required for reading audio files on this device */
    fun requiredMediaPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /** Returns true if the app has permission to read audio files from device storage */
    fun hasMediaPermission(context: Context): Boolean {
        permissionChecker?.let { return it(context) }
        return try {
            val permission = requiredMediaPermission()
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }
}
