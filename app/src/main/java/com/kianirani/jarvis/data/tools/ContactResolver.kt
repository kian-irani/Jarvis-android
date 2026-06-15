package com.kianirani.jarvis.data.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a spoken contact name (e.g. "mother", "مامان", "Ali") to a phone number
 * via the device address book. Used by [CallTool]/[SmsTool] so an action runs against
 * a *real* number — never a hallucinated "done".
 *
 * Returns a typed [Lookup] so callers can tell apart "no permission" from "not found"
 * and report honestly instead of pretending success (the v20 bug).
 */
@Singleton
class ContactResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed interface Lookup {
        data class Found(val number: String, val displayName: String) : Lookup
        data object NoPermission : Lookup
        data object NotFound : Lookup
    }

    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun find(name: String): Lookup {
        if (!hasContactsPermission()) return Lookup.NoPermission
        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%${name.trim()}%")
        return runCatching {
            resolver.query(uri, projection, selection, args, null)?.use { c ->
                if (c.moveToFirst()) {
                    Lookup.Found(c.getString(0), c.getString(1) ?: name)
                } else {
                    Lookup.NotFound
                }
            } ?: Lookup.NotFound
        }.getOrDefault(Lookup.NotFound)
    }
}
