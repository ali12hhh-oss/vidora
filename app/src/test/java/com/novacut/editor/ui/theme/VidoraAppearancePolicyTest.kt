package com.novacut.editor.ui.theme

import com.novacut.editor.engine.AppearanceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VidoraAppearancePolicyTest {

    @Test
    fun theAppearanceMenuOffersOnlySchemesThatExist() {
        // "System" used to sit in this list and resolve to DARK in both branches --
        // a control that read the platform preference and ignored it.
        assertEquals(
            listOf(AppearanceMode.DARK, AppearanceMode.HIGH_CONTRAST_DARK),
            AppearanceMode.entries.toList(),
        )
        AppearanceMode.entries.forEach { mode ->
            assertEquals(mode, VidoraThemeDefaults.resolveMode(mode))
        }
    }

    @Test
    fun highContrastSemanticTextMeetsWcagAa() {
        val colors = VidoraThemeDefaults.colorsFor(AppearanceMode.HIGH_CONTRAST_DARK)

        listOf(
            colors.background,
            colors.backgroundMid,
            colors.panel,
            colors.panelRaised,
            colors.panelHighest,
            colors.surfaceBase,
            colors.surfaceLow,
            colors.surface,
            colors.surfaceHigh,
        ).forEach { surface ->
            assertTrue(VidoraThemeDefaults.contrastRatio(colors.text, surface) >= 4.5)
            assertTrue(VidoraThemeDefaults.contrastRatio(colors.subtext, surface) >= 4.5)
        }
        assertTrue(VidoraThemeDefaults.contrastRatio(colors.disabledText, colors.disabledSurface) >= 4.5)
    }

    @Test
    fun highContrastSemanticStrokesMeetNonTextFloor() {
        val colors = VidoraThemeDefaults.colorsFor(AppearanceMode.HIGH_CONTRAST_DARK)

        assertTrue(VidoraThemeDefaults.contrastRatio(colors.cardStroke, colors.panel) >= 3.0)
        assertTrue(VidoraThemeDefaults.contrastRatio(colors.cardStrokeStrong, colors.panel) >= 3.0)
        assertTrue(VidoraThemeDefaults.contrastRatio(colors.cardStrokeStrong, colors.panelHighest) >= 3.0)
    }

    @Test
    fun lowEmphasisMochaTokensStayBelowSemanticIndicatorFloor() {
        val overlayOnPanel = VidoraThemeDefaults.contrastRatio(Mocha.Overlay0, Mocha.PanelHighest)
        val strokeOnPanel = VidoraThemeDefaults.contrastRatio(Mocha.CardStrokeStrong, Mocha.Panel)
        val highContrast = VidoraThemeDefaults.colorsFor(AppearanceMode.HIGH_CONTRAST_DARK)

        assertTrue(overlayOnPanel < 3.0)
        assertTrue(strokeOnPanel < 3.0)
        assertTrue(VidoraThemeDefaults.contrastRatio(highContrast.cardStrokeStrong, highContrast.panel) >= 3.0)
    }

    @Test
    fun selectedHighContrastChipHasReadableLabelForCommonAccents() {
        listOf(
            VidoraAccents.Lavender,
            VidoraAccents.Blue,
            VidoraAccents.Sapphire,
            VidoraAccents.Sky,
            VidoraAccents.Teal,
            VidoraAccents.Green,
            VidoraAccents.Yellow,
            VidoraAccents.Peach,
            VidoraAccents.Maroon,
            VidoraAccents.Red,
            VidoraAccents.Mauve,
            VidoraAccents.Pink,
            VidoraAccents.Flamingo,
            VidoraAccents.Rosewater,
        ).forEach { accent ->
            assertTrue(
                "Chip label contrast failed for $accent",
                VidoraThemeDefaults.contrastRatio(
                    VidoraThemeDefaults.colorsFor(AppearanceMode.HIGH_CONTRAST_DARK).onAccent,
                    accent,
                ) >= 4.5,
            )
        }
    }

    @Test
    fun highContrastIndicatorsMeetNonTextFloor() {
        val colors = VidoraThemeDefaults.colorsFor(AppearanceMode.HIGH_CONTRAST_DARK)

        listOf(colors.overlay, colors.overlayStrong, colors.focusRing).forEach { indicator ->
            assertTrue(VidoraThemeDefaults.contrastRatio(indicator, colors.panel) >= 3.0)
            assertTrue(VidoraThemeDefaults.contrastRatio(indicator, colors.panelHighest) >= 3.0)
        }
    }

    @Test
    fun darkModeKeepsPrimaryTextReadable() {
        val colors = VidoraThemeDefaults.colorsFor(AppearanceMode.DARK)

        assertTrue(VidoraThemeDefaults.contrastRatio(colors.text, colors.panel) >= 4.5)
        assertTrue(VidoraThemeDefaults.contrastRatio(colors.subtext, colors.panel) >= 4.5)
    }

    @Test
    fun radiusTokensStayWithinProfessionalGeometryCap() {
        listOf(Radius.xs, Radius.sm, Radius.md, Radius.lg, Radius.xl, Radius.xxl).forEach { radius ->
            assertTrue("Radius token exceeded 12dp: $radius", radius.value <= 12f)
        }
    }
}
