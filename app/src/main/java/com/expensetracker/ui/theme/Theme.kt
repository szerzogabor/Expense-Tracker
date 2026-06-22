package com.expensetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF00210B),
    secondary = Color(0xFF4E6A50),
    tertiary = Color(0xFF00696E),
    background = Color(0xFFFBFDF7),
    surface = Color(0xFFFBFDF7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD68F),
    onPrimary = Color(0xFF003912),
    primaryContainer = Color(0xFF1F4E22),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFFB4CCB5),
    tertiary = Color(0xFF4FD8E0),
    background = Color(0xFF191C19),
    surface = Color(0xFF191C19),
)

val IncomeGreen = Color(0xFF2E7D32)
val ExpenseRed = Color(0xFFC62828)

@Composable
fun ExpenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
