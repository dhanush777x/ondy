package com.ondy.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ondy.app.R

/**
 * Ondy's brand mark, sourced from `assets/logo.png`.
 *
 * The PNGs in `res/drawable-*` keep the original artwork (bell, orbit, motion
 * lines) at every density. It is tinted so the mark stays legible on both the
 * built-in palettes and Material You dynamic colors.
 *
 * The mark is decorative; the app name is always available as text next to it.
 */
@Composable
fun OndyLogo(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        painter = painterResource(R.drawable.ic_ondy_logo),
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

/** Logo sized for use inside a top app bar. */
@Composable
fun OndyLogoSmall() {
    OndyLogo(modifier = Modifier.size(28.dp))
}
