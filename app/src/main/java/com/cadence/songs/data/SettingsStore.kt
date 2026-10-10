package com.cadence.songs.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * theme : 0 = automatique, 1 = clair, 2 = sombre, 3 = scène (noir pur)
 * chordStyle : 0 = lettres, 1 = numéros (Nashville)
 */
data class Settings(
    val theme: Int = 0,
    val fontSize: Float = 22f,
    val chordStyle: Int = 0,
    val columns: Int = 1,
    val chordColor: Int = 0,
    val showChords: Boolean = true,
    val keepScreenOn: Boolean = true,
    /** 0 = guitare, 1 = ukulélé, 2 = piano */
    val instrument: Int = 0
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val THEME = intPreferencesKey("theme")
        val FONT = floatPreferencesKey("font_size")
        val STYLE = intPreferencesKey("chord_style")
        val COLUMNS = intPreferencesKey("columns")
        val CHORD_COLOR = intPreferencesKey("chord_color")
        val SHOW_CHORDS = booleanPreferencesKey("show_chords")
        val KEEP_ON = booleanPreferencesKey("keep_screen_on")
        val INSTRUMENT = intPreferencesKey("instrument")
    }

    private fun read(p: Preferences) = Settings(
        theme = p[Keys.THEME] ?: 0,
        fontSize = p[Keys.FONT] ?: 22f,
        chordStyle = p[Keys.STYLE] ?: 0,
        columns = p[Keys.COLUMNS] ?: 1,
        chordColor = p[Keys.CHORD_COLOR] ?: 0,
        showChords = p[Keys.SHOW_CHORDS] ?: true,
        keepScreenOn = p[Keys.KEEP_ON] ?: true,
        instrument = p[Keys.INSTRUMENT] ?: 0
    )

    val flow: Flow<Settings> = context.dataStore.data.map { read(it) }

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { p ->
            val s = transform(read(p))
            p[Keys.THEME] = s.theme
            p[Keys.FONT] = s.fontSize
            p[Keys.STYLE] = s.chordStyle
            p[Keys.COLUMNS] = s.columns
            p[Keys.CHORD_COLOR] = s.chordColor
            p[Keys.SHOW_CHORDS] = s.showChords
            p[Keys.KEEP_ON] = s.keepScreenOn
            p[Keys.INSTRUMENT] = s.instrument
        }
    }
}
