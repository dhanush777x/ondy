package com.ondy.app.domain.model

/**
 * How Ondy chooses its color palette.
 *
 * - [MATERIAL] follows the system light/dark setting and, on Android 12+, uses
 *   the wallpaper-derived Material You palette.
 * - [LIGHT] and [DARK] force the built-in Ondy palette regardless of the system
 *   setting, which also makes the choice predictable on OEM builds.
 */
enum class ThemeMode {
    MATERIAL,
    LIGHT,
    DARK;

    companion object {
        fun fromStorage(value: String?): ThemeMode {
            return values().firstOrNull { it.name == value } ?: MATERIAL
        }
    }
}
