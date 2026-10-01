package com.novacut.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.novacut.editor.ui.VidoraTestTags
import com.novacut.editor.engine.AppearanceMode
import com.novacut.editor.engine.DesktopOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class VidoraSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>().apply {
        enableAccessibilityChecks()
    }

    @Before
    fun waitForInitialComposeHierarchy() {
        compose.waitForComposeHierarchy()
    }

    @Test
    fun projectEditorExportAndSettingsSurfacesOpen() {
        compose.onNodeWithTag(VidoraTestTags.PROJECTS_SCREEN).assertIsDisplayed()
        compose.assertAccessibilityChecksPass()

        compose.onNodeWithTag(VidoraTestTags.PROJECTS_CREATE_PROJECT).performClick()
        compose.onNodeWithTag(VidoraTestTags.TEMPLATE_SHEET).assertIsDisplayed()
        compose.onNodeWithTag(VidoraTestTags.TEMPLATE_BLANK)
            .performScrollTo()
            .performClick()

        compose.waitUntilAtLeastOneExists(VidoraTestTags.EDITOR_SCREEN)
        compose.waitForIdle()
        assertTrue(
            compose.onAllNodesWithTag(VidoraTestTags.TUTORIAL_SCREEN)
                .fetchSemanticsNodes()
                .isEmpty()
        )
        dismissTutorialIfPresent()
        compose.assertAccessibilityChecksPass()

        compose.onNodeWithTag(VidoraTestTags.EDITOR_EMPTY_ADD_MEDIA).assertIsDisplayed().performClick()
        compose.onNodeWithTag(VidoraTestTags.MEDIA_PICKER_SHEET).assertIsDisplayed()
        compose.assertAccessibilityChecksPass()
        compose.onNodeWithTag(VidoraTestTags.MEDIA_PICKER_CLOSE).performClick()

        compose.onNodeWithTag(VidoraTestTags.EDITOR_EXPORT).assertIsDisplayed().performClick()
        compose.onNodeWithTag(VidoraTestTags.EXPORT_SHEET).assertIsDisplayed()
        compose.assertAccessibilityChecksPass()
        compose.onNodeWithTag(VidoraTestTags.EXPORT_CLOSE).performClick()

        compose.onNodeWithTag(VidoraTestTags.EDITOR_BACK).performClick()
        compose.waitUntilAtLeastOneExists(VidoraTestTags.PROJECTS_SCREEN)
        compose.waitUntilNoNodesExist(VidoraTestTags.EDITOR_SCREEN)

        compose.onNodeWithTag(VidoraTestTags.PROJECTS_SETTINGS).performClick()
        compose.onNodeWithTag(VidoraTestTags.SETTINGS_SCREEN).assertIsDisplayed()
        compose.assertAccessibilityChecksPass()
        compose.onNodeWithTag(VidoraTestTags.SETTINGS_PRIVACY_OPEN)
            .performScrollTo()
            .performClick()
        compose.onNodeWithTag(VidoraTestTags.SETTINGS_PRIVACY_DASHBOARD).assertIsDisplayed()
        compose.assertAccessibilityChecksPass()
        compose.onNodeWithTag(VidoraTestTags.SETTINGS_PRIVACY_CLOSE).performClick()
        compose.onNodeWithTag(VidoraTestTags.SETTINGS_LICENSES_OPEN)
            .performScrollTo()
            .performClick()
        compose.onNodeWithTag(VidoraTestTags.SETTINGS_LICENSES_DIALOG).assertIsDisplayed()
        compose.assertAccessibilityChecksPass()
        compose.onNodeWithTag(VidoraTestTags.SETTINGS_LICENSES_CLOSE).performClick()

        compose.onNodeWithTag(VidoraTestTags.SETTINGS_REPLAY_TUTORIAL)
            .performScrollTo()
            .performClick()
        compose.waitUntilAtLeastOneExists(VidoraTestTags.EDITOR_SCREEN)
        compose.onNodeWithTag(VidoraTestTags.TUTORIAL_SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(VidoraTestTags.TUTORIAL_SKIP).performClick()
        compose.onNodeWithTag(VidoraTestTags.EDITOR_SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(VidoraTestTags.EDITOR_BACK).performClick()
        compose.waitUntilAtLeastOneExists(VidoraTestTags.SETTINGS_SCREEN)
        compose.waitUntilNoNodesExist(VidoraTestTags.EDITOR_SCREEN)

        compose.onNodeWithTag(VidoraTestTags.SETTINGS_BACK)
            .performScrollTo()
            .performClick()

        compose.waitUntilAtLeastOneExists(VidoraTestTags.PROJECTS_SCREEN)
        compose.waitUntilNoNodesExist(VidoraTestTags.SETTINGS_SCREEN)
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun pseudoLocalesRenderExpandedAndRtlExportSurfaces() {
        try {
            setApplicationLocale("en-XA")
            compose.waitUntilAtLeastOneExists(VidoraTestTags.PROJECTS_SCREEN)
            compose.onNodeWithTag(VidoraTestTags.PROJECTS_SCREEN).assertIsDisplayed()
            compose.onNodeWithTag(VidoraTestTags.PROJECTS_CREATE_PROJECT).performClick()
            compose.onNodeWithTag(VidoraTestTags.TEMPLATE_BLANK)
                .performScrollTo()
                .performClick()
            compose.waitUntilAtLeastOneExists(VidoraTestTags.EDITOR_SCREEN)
            dismissTutorialIfPresent()
            compose.onNodeWithTag(VidoraTestTags.EDITOR_EXPORT).performClick()
            compose.onNodeWithTag(VidoraTestTags.EXPORT_SHEET).assertIsDisplayed()
            compose.assertAccessibilityChecksPass()
            assertTrue(compose.onRoot().captureToImage().width > 0)

            setApplicationLocale("ar-XB")
            compose.onNodeWithTag(VidoraTestTags.EXPORT_SHEET).assertIsDisplayed()
            compose.assertAccessibilityChecksPass()
            assertEquals(
                android.view.View.LAYOUT_DIRECTION_RTL,
                compose.activity.resources.configuration.layoutDirection,
            )
            assertTrue(compose.onRoot().captureToImage().width > 0)
        } finally {
            compose.activity
                .getSystemService(android.app.LocaleManager::class.java)
                .applicationLocales = android.os.LocaleList.getEmptyLocaleList()
        }
    }

    @Test
    fun highContrastPhoneAndDesktopEditorSurfacesRender() {
        try {
            updateAppearance(AppearanceMode.HIGH_CONTRAST_DARK, DesktopOverride.FORCE_OFF)
            compose.waitUntilAtLeastOneExists(VidoraTestTags.PROJECTS_SCREEN)
            compose.onNodeWithTag(VidoraTestTags.PROJECTS_CREATE_PROJECT).performClick()
            compose.onNodeWithTag(VidoraTestTags.TEMPLATE_BLANK)
                .performScrollTo()
                .performClick()
            compose.waitUntilAtLeastOneExists(VidoraTestTags.EDITOR_SCREEN)
            dismissTutorialIfPresent()
            compose.assertAccessibilityChecksPass()
            assertTrue(compose.onRoot().captureToImage().width > 0)

            compose.onNodeWithTag(VidoraTestTags.EDITOR_EMPTY_ADD_MEDIA).performClick()
            compose.onNodeWithTag(VidoraTestTags.MEDIA_PICKER_SHEET).assertIsDisplayed()
            compose.assertAccessibilityChecksPass()
            assertTrue(compose.onRoot().captureToImage().width > 0)
            compose.onNodeWithTag(VidoraTestTags.MEDIA_PICKER_CLOSE).performClick()

            compose.onNodeWithTag(VidoraTestTags.EDITOR_EXPORT).performClick()
            compose.onNodeWithTag(VidoraTestTags.EXPORT_SHEET).assertIsDisplayed()
            compose.assertAccessibilityChecksPass()
            assertTrue(compose.onRoot().captureToImage().width > 0)
            compose.onNodeWithTag(VidoraTestTags.EXPORT_CLOSE).performClick()

            updateAppearance(AppearanceMode.HIGH_CONTRAST_DARK, DesktopOverride.FORCE_ON)
            compose.waitUntilAtLeastOneExists(VidoraTestTags.EDITOR_DESKTOP_SIDEBAR)
            compose.onNodeWithTag(VidoraTestTags.EDITOR_DESKTOP_SIDEBAR).assertIsDisplayed()
            compose.assertAccessibilityChecksPass()
            assertTrue(compose.onRoot().captureToImage().width > 0)
            compose.onNodeWithTag(VidoraTestTags.EDITOR_EXPORT).performClick()
            compose.onNodeWithTag(VidoraTestTags.EXPORT_SHEET).assertIsDisplayed()
            compose.assertAccessibilityChecksPass()
            assertTrue(compose.onRoot().captureToImage().width > 0)
        } finally {
            updateAppearance(AppearanceMode.DARK, DesktopOverride.AUTO)
        }
    }

    private fun updateAppearance(appearanceMode: AppearanceMode, desktopOverride: DesktopOverride) {
        runBlocking {
            compose.activity.settingsRepository.updateAppearanceMode(appearanceMode)
            compose.activity.settingsRepository.updateDesktopOverride(desktopOverride)
            compose.activity.settingsRepository.settings.first { settings ->
                settings.appearanceMode == appearanceMode &&
                    settings.desktopModeOverride == desktopOverride
            }
        }
        compose.waitForIdle()
        compose.waitForComposeHierarchy()
    }

    private fun setApplicationLocale(languageTag: String) {
        compose.activity
            .getSystemService(android.app.LocaleManager::class.java)
            .applicationLocales = android.os.LocaleList.forLanguageTags(languageTag)
        compose.waitUntil(timeoutMillis = 20_000L) {
            compose.activity.resources.configuration.locales[0].toLanguageTag() == languageTag
        }
        compose.waitForComposeHierarchy()
    }

    private fun dismissTutorialIfPresent() {
        runCatching {
            compose.waitUntil(timeoutMillis = 1_200L) {
                compose.onAllNodesWithTag(VidoraTestTags.TUTORIAL_SKIP)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        }
        val tutorialNodes = runCatching {
            compose.onAllNodesWithTag(VidoraTestTags.TUTORIAL_SKIP).fetchSemanticsNodes()
        }.getOrDefault(emptyList())
        if (tutorialNodes.isNotEmpty()) {
            compose.onNodeWithTag(VidoraTestTags.TUTORIAL_SKIP).performClick()
        }
    }

}
