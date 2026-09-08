package app.focus.android.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.focus.android.R
import app.focus.domain.usecase.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileShortcutsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
) {
    suspend fun refresh() {
        val profiles = profileRepository.observeProfiles().first().take(MAX_SHORTCUTS)
        val shortcuts = profiles.map { profile ->
            ShortcutInfoCompat.Builder(context, shortcutId(profile.id))
                .setShortLabel(context.getString(R.string.shortcut_start, profile.name))
                .setLongLabel(context.getString(R.string.shortcut_start, profile.name))
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(
                    Intent(context, app.focus.android.MainActivity::class.java).apply {
                        action = ACTION_START_PROFILE
                        putExtra(EXTRA_PROFILE_ID, profile.id)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                )
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    companion object {
        const val ACTION_START_PROFILE = "app.focus.android.ACTION_START_PROFILE"
        const val EXTRA_PROFILE_ID = "profileId"
        private const val MAX_SHORTCUTS = 3

        fun shortcutId(profileId: String): String = "start_profile_$profileId"
    }
}
