package com.ztros.ztrosu.ui.screen

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.dropUnlessResumed
import com.ztros.ztrosu.ui.MainActivity
import com.ztros.ztrosu.ui.LocalScrollState
import com.ztros.ztrosu.ui.LocalCardTransparency
import com.ztros.ztrosu.ui.LocalWallpaperUri
import com.ztros.ztrosu.ui.component.SwitchItem
import com.ztros.ztrosu.ui.component.rememberCustomDialog
import com.ztros.ztrosu.ui.component.rememberConfirmDialog
import com.ztros.ztrosu.ui.component.ConfirmResult
import com.ztros.ztrosu.ui.util.refreshActivity
import com.ztros.ztrosu.ui.util.LocalSnackbarHost
import com.ztros.ztrosu.ui.util.LocaleHelper
import com.ztros.ztrosu.ui.util.ShellUtils
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.ztros.ztrosu.Natives
import com.ztros.ztrosu.R
import kotlinx.coroutines.launch

/**
 * ZTR_OS SU UI-Only Mode - Enhanced Customization Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CustomizationScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current
    val coroutineScope = rememberCoroutineScope()
    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    // Card transparency state
    var transparencyValue by rememberSaveable { mutableFloatStateOf(0.95f) }
    LaunchedEffect(Unit) {
        transparencyValue = prefs.getFloat("card_transparency", 0.95f)
    }
    
    // Wallpaper state
    var wallpaperUri by remember { mutableStateOf<Uri?>(null) }
    var wallpaperBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(Unit) {
        val uriString = prefs.getString("wallpaper_uri", null)
        if (uriString != null) {
            wallpaperUri = Uri.parse(uriString)
        }
    }
    
    LaunchedEffect(wallpaperUri) {
        wallpaperUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    wallpaperBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {}
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            prefs.edit { putString("wallpaper_uri", it.toString()) }
            wallpaperUri = it
            (context as? MainActivity)?.setWallpaperUri(it)
        }
    }

    // Clear wallpaper dialog
    val clearDialog = rememberConfirmDialog()

    val bottomBarScrollState = LocalScrollState.current
    val bottomBarScrollConnection = if (bottomBarScrollState != null) {
        com.ztros.ztrosu.ui.rememberScrollConnection(
            isScrollingDown = bottomBarScrollState.isScrollingDown,
            scrollOffset = bottomBarScrollState.scrollOffset,
            previousScrollOffset = bottomBarScrollState.previousScrollOffset,
            threshold = 30f
        )
    } else null

    val scrollState = LocalScrollState.current
    val isNavBarHidden = scrollState?.isScrollingDown?.value ?: false
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (isNavBarHidden) 0.dp else 112.dp

    // Pre-resolve all string resources
    val interfaceSettingsText = stringResource(R.string.interface_settings)
    val personalizationText = stringResource(R.string.personalization)
    val cardTransparencyText = stringResource(R.string.card_transparency)
    val cardTransparencySummaryText = stringResource(R.string.card_transparency_summary)
    val customWallpaperText = stringResource(R.string.custom_wallpaper)
    val customWallpaperSummaryText = stringResource(R.string.custom_wallpaper_summary)
    val opaqueText = stringResource(R.string.opaque)
    val transparentText = stringResource(R.string.transparent)
    val previewText = stringResource(R.string.preview)
    val chooseImageText = stringResource(R.string.choose_image)
    val clearText = stringResource(R.string.clear)
    val setSystemWallpaperText = stringResource(R.string.set_system_wallpaper)
    val languageText = stringResource(R.string.settings_language)
    val systemDefaultText = stringResource(R.string.system_default)
    val themeStyleTitleText = stringResource(R.string.theme_style_title)
    val bannerText = stringResource(R.string.settings_banner)
    val bannerSummaryText = stringResource(R.string.settings_banner_summary)
    val amoledModeText = stringResource(R.string.settings_amoled_mode)
    val amoledModeSummaryText = stringResource(R.string.settings_amoled_mode_summary)
    val customizationTitleText = stringResource(R.string.customization)
    val clearTitle = stringResource(R.string.clear_wallpaper_title)
    val clearContent = stringResource(R.string.clear_wallpaper_content)
    val clearedMsg = stringResource(R.string.wallpaper_cleared)
    val confirmText = stringResource(R.string.confirm)
    val cancelText = stringResource(R.string.cancel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customizationTitleText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
                navigationIcon = { IconButton(onClick = dropUnlessResumed { navigator.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost, modifier = Modifier.padding(bottom = navBarPadding)) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .then(if (bottomBarScrollConnection != null) Modifier.nestedScroll(bottomBarScrollConnection).nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ============ 界面设置 Section ============
            Text(
                text = interfaceSettingsText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Card 1: UI Card Transparency
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FormatColorFill, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(cardTransparencyText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(cardTransparencySummaryText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(opaqueText, style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = transparencyValue,
                            onValueChange = { transparencyValue = it },
                            onValueChangeFinished = {
                                prefs.edit { putFloat("card_transparency", transparencyValue) }
                                (context as? MainActivity)?.setCardTransparency(transparencyValue)
                            },
                            valueRange = 0.5f..1.0f,
                            steps = 9,
                            modifier = Modifier.weight(1f)
                        )
                        Text(transparentText, style = MaterialTheme.typography.bodySmall)
                    }

                    // Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = transparencyValue))
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(previewText, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Card 2: Custom Wallpaper
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Wallpaper, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(customWallpaperText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(customWallpaperSummaryText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Wallpaper preview
                    wallpaperBitmap?.let { bmp ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                    }

                    // Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Image, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(chooseImageText)
                        }

                        if (wallpaperUri != null) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val result = clearDialog.awaitConfirm(
                                            title = clearTitle,
                                            content = clearContent,
                                            confirm = confirmText,
                                            dismiss = cancelText
                                        )
                                        if (result == ConfirmResult.Confirmed) {
                                            prefs.edit { remove("wallpaper_uri") }
                                            wallpaperUri = null
                                            wallpaperBitmap = null
                                            (context as? MainActivity)?.clearCustomWallpaper()
                                            snackBarHost.showSnackbar(clearedMsg)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Clear, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(clearText)
                            }
                        }
                    }

                    // SU wallpaper button
                    if (Natives.isManager) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val result = ShellUtils.fastCmd("su -c 'echo ZTR_OS_SU_TEST'")
                                    snackBarHost.showSnackbar("SU Test: $result")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.SystemUpdate, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(setSystemWallpaperText)
                        }
                    }
                }
            }

            // ============ 个性化设置 Section ============
            Text(
                text = personalizationText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    // Language setting
                    var currentAppLocale by remember { mutableStateOf(LocaleHelper.getCurrentAppLocale(context)) }
                    LaunchedEffect(Unit) { currentAppLocale = LocaleHelper.getCurrentAppLocale(context) }

                    val languageDialog = rememberCustomDialog { dismiss ->
                        if (LocaleHelper.useSystemLanguageSettings) {
                            LocaleHelper.launchSystemLanguageSettings(context)
                            dismiss()
                        } else {
                            val supportedLocales = remember {
                                val locales = mutableListOf(java.util.Locale.ROOT)
                                listOf("ar", "bg", "de", "fa", "fr", "hu", "in", "it", "ja", "ko", "pl", "pt-rBR", "ru", "th", "tr", "uk", "vi", "zh-rCN", "zh-rTW").forEach { dir ->
                                    try {
                                        val locale = if (dir.contains("-r")) {
                                            val parts = dir.split("-r")
                                            java.util.Locale(parts[0], parts[1])
                                        } else java.util.Locale(dir)
                                        locales.add(locale)
                                    } catch (_: Exception) {}
                                }
                                locales
                            }
                            
                            val allOptions = supportedLocales.map { locale ->
                                val tag = if (locale == java.util.Locale.ROOT) "system" else if (locale.country.isEmpty()) locale.language else "${locale.language}_${locale.country}"
                                val displayName = if (locale == java.util.Locale.ROOT) systemDefaultText else locale.getDisplayName(locale)
                                tag to displayName
                            }
                            
                            val currentLocale = prefs.getString("app_locale", "system") ?: "system"
                            val options = allOptions.map { (tag, displayName) ->
                                ListOption(titleText = displayName, selected = currentLocale == tag)
                            }
                            
                            var selectedIndex by remember { mutableIntStateOf(allOptions.indexOfFirst { it.first == currentLocale }) }

                            ListDialog(
                                state = rememberUseCaseState(visible = true, onFinishedRequest = {
                                    if (selectedIndex >= 0) {
                                        prefs.edit { putString("app_locale", allOptions[selectedIndex].first) }
                                        refreshActivity(context)
                                    }
                                    dismiss()
                                }, onCloseRequest = { dismiss() }),
                                header = Header.Default(title = languageText),
                                selection = ListSelection.Single(showRadioButtons = true, options = options) { index, _ -> selectedIndex = index }
                            )
                        }
                    }

                    val languageDisplay = remember(currentAppLocale) {
                        currentAppLocale?.getDisplayName(currentAppLocale) ?: systemDefaultText
                    }

                    ListItem(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { languageDialog.show() },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Icon(Icons.Filled.Translate, null) },
                        headlineContent = { Text(languageText, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(languageDisplay) }
                    )

                    // Theme style
                    val themeStyles = listOf(
                        "wild" to stringResource(R.string.theme_style_wild),
                        "md3" to stringResource(R.string.theme_style_md3),
                        "miui_x" to stringResource(R.string.theme_style_miui_x)
                    )
                    val currentThemeStyle = prefs.getString("theme_style", "wild") ?: "wild"
                    val currentThemeDisplay = themeStyles.firstOrNull { it.first == currentThemeStyle }?.second ?: themeStyles[0].second

                    val themeDialog = rememberCustomDialog { dismiss ->
                        val options = themeStyles.map { (_, displayName) ->
                            ListOption(titleText = displayName, selected = currentThemeStyle == themeStyles.first { it.second == displayName }.first)
                        }
                        var selectedIndex by remember { mutableIntStateOf(themeStyles.indexOfFirst { it.first == currentThemeStyle }.coerceAtLeast(0)) }

                        ListDialog(
                            state = rememberUseCaseState(visible = true, onFinishedRequest = {
                                if (selectedIndex >= 0) {
                                    prefs.edit { putString("theme_style", themeStyles[selectedIndex].first) }
                                    refreshActivity(context)
                                }
                                dismiss()
                            }, onCloseRequest = { dismiss() }),
                            header = Header.Default(title = themeStyleTitleText),
                            selection = ListSelection.Single(showRadioButtons = true, options = options) { index, _ -> selectedIndex = index }
                        )
                    }

                    ListItem(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { themeDialog.show() },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Icon(Icons.Filled.Palette, null) },
                        headlineContent = { Text(themeStyleTitleText, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(currentThemeDisplay) }
                    )

                    // Banner switch
                    var useBanner by rememberSaveable { mutableStateOf(prefs.getBoolean("use_banner", true)) }
                    if (ksuVersion != null) {
                        SwitchItem(
                            icon = Icons.Filled.ViewCarousel,
                            title = bannerText,
                            summary = bannerSummaryText,
                            checked = useBanner,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) {
                            prefs.edit { putBoolean("use_banner", it) }
                            useBanner = it
                        }
                    }

                    // AMOLED mode
                    var enableAmoled by rememberSaveable { mutableStateOf(prefs.getBoolean("enable_amoled", false)) }
                    if (isSystemInDarkTheme()) {
                        val activity = context as? MainActivity
                        SwitchItem(
                            icon = Icons.Filled.Contrast,
                            title = amoledModeText,
                            summary = amoledModeSummaryText,
                            checked = enableAmoled,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) { checked ->
                            activity?.setAmoledMode(checked)
                            prefs.edit { putBoolean("enable_amoled", checked) }
                            enableAmoled = checked
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Preview
@Composable
private fun CustomizationPreview() {
    CustomizationScreen(EmptyDestinationsNavigator)
}
