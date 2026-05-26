package com.ztros.ztrosu.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
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
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.dropUnlessResumed
import com.ztros.ztrosu.ui.MainActivity
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
import com.ztros.ztrosu.ui.LocalWallpaperUri
import com.ztros.ztrosu.ui.LocalCardTransparency
import com.ztros.ztrosu.ui.component.SwitchItem
import com.ztros.ztrosu.ui.component.rememberCustomDialog
import com.ztros.ztrosu.ui.component.rememberConfirmDialog
import com.ztros.ztrosu.ui.component.ConfirmResult
import com.ztros.ztrosu.ui.util.refreshActivity
import com.ztros.ztrosu.ui.util.LocalSnackbarHost
import com.ztros.ztrosu.ui.util.LocaleHelper
import com.ztros.ztrosu.ui.util.ShellUtils

/**
 * @author twj
 * @date 2025/6/1.
 * ZTR_OS SU UI-Only Mode - Enhanced Customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CustomizationScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    // Bottom bar scroll tracking
    val bottomBarScrollState = LocalScrollState.current
    val bottomBarScrollConnection = if (bottomBarScrollState != null) {
        com.ztros.ztrosu.ui.rememberScrollConnection(
            isScrollingDown = bottomBarScrollState.isScrollingDown,
            scrollOffset = bottomBarScrollState.scrollOffset,
            previousScrollOffset = bottomBarScrollState.previousScrollOffset,
            threshold = 30f
        )
    } else null
    val snackBarHost = LocalSnackbarHost.current

    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null

    val scrollState = LocalScrollState.current
    val isNavBarHidden = scrollState?.isScrollingDown?.value ?: false
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (isNavBarHidden) 0.dp else 112.dp

    // Card transparency state
    val cardTransparency = LocalCardTransparency.current
    val prefs = LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    // Wallpaper state
    val wallpaperUri = LocalWallpaperUri.current

    // Image picker for wallpaper
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Save wallpaper URI to preferences
            prefs.edit { putString("wallpaper_uri", uri.toString()) }
            // Update wallpaper state to trigger recomposition
            (LocalContext.current as? MainActivity)?.setWallpaperUri(uri)
        }
    }

    // Clear wallpaper confirmation dialog
    val clearWallpaperDialog = rememberConfirmDialog()
    val clearConfirmText = stringResource(R.string.confirm)
    val cancelText = stringResource(R.string.cancel)
    val clearWallpaperTitle = stringResource(R.string.clear_wallpaper_title)
    val clearWallpaperContent = stringResource(R.string.clear_wallpaper_content)
    val snackbarMessage = stringResource(R.string.wallpaper_cleared)

    Scaffold(
        topBar = {
            TopBar(
                onBack = dropUnlessResumed {
                    navigator.popBackStack()
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost, modifier = Modifier.padding(bottom = navBarPadding)) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
    
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .let { modifier ->
                    if (bottomBarScrollConnection != null) {
                        modifier
                            .nestedScroll(bottomBarScrollConnection)
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                    } else {
                        modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    }
                }
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ============ 界面设置 Section ============
            Text(
                text = stringResource(R.string.interface_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Card 1: UI Card Transparency
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.FilledOpacity,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.card_transparency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.card_transparency_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Transparency slider
                    val transparencyValue = rememberSaveable { 
                        mutableFloatStateOf(prefs.getFloat("card_transparency", 0.95f))
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.opaque),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = transparencyValue.value,
                            onValueChange = { newValue ->
                                transparencyValue.value = newValue
                            },
                            onValueChangeFinished = {
                                // Save to preferences
                                prefs.edit { putFloat("card_transparency", transparencyValue.value) }
                                // Update global state
                                (LocalContext.current as? MainActivity)?.setCardTransparency(transparencyValue.value)
                            },
                            valueRange = 0.5f..1.0f,
                            steps = 9,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.transparent),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Preview card with current transparency
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
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = transparencyValue.value)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.preview),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Card 2: Custom Wallpaper
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wallpaper,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.custom_wallpaper),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.custom_wallpaper_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Wallpaper preview
                    val wallpaperBitmap = remember(wallpaperUri) {
                        wallpaperUri?.let { uri ->
                            try {
                                val bitmap = LocalContext.current.contentResolver.openInputStream(uri)?.use {
                                    android.graphics.BitmapFactory.decodeStream(it)
                                }
                                bitmap?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }

                    if (wallpaperBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                bitmap = wallpaperBitmap,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Wallpaper controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.choose_image))
                        }

                        if (wallpaperUri != null) {
                            OutlinedButton(
                                onClick = {
                                    kotlinx.coroutines.MainScope().launch {
                                        val result = clearWallpaperDialog.awaitConfirm(
                                            title = clearWallpaperTitle,
                                            content = clearWallpaperContent,
                                            confirm = clearConfirmText,
                                            dismiss = cancelText
                                        )
                                        if (result == ConfirmResult.Confirmed) {
                                            prefs.edit { remove("wallpaper_uri") }
                                            (LocalContext.current as? MainActivity)?.clearWallpaper()
                                            snackBarHost.showSnackbar(snackbarMessage)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.clear))
                            }
                        }
                    }

                    // Try to set system wallpaper with SU
                    if (Natives.isManager) {
                        OutlinedButton(
                            onClick = {
                                val shellResult = ShellUtils.fastCmd("su -c 'settings put wallpaper_set 1'")
                                // Try to get current wallpaper
                                val wallpaperCmd = ShellUtils.fastCmd("su -c 'content query --uri content://media/external/images/media --projection _id,_data --sort _id DESC --limit 1'")
                                snackBarHost.showSnackbar("Wallpaper shell: $wallpaperCmd")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.set_system_wallpaper))
                        }
                    }
                }
            }

            // ============ 个性化设置 Section ============
            Text(
                text = stringResource(R.string.personalization),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Language setting
                    val context = LocalContext.current
                    var currentAppLocale by remember { mutableStateOf(LocaleHelper.getCurrentAppLocale(context)) }
                    
                    LaunchedEffect(Unit) {
                        currentAppLocale = LocaleHelper.getCurrentAppLocale(context)
                    }

                    // Language dialog
                    val languageDialog = rememberCustomDialog { dismiss ->
                        if (LocaleHelper.useSystemLanguageSettings) {
                            LocaleHelper.launchSystemLanguageSettings(context)
                            dismiss()
                        } else {
                            val supportedLocales = remember {
                                val locales = mutableListOf(java.util.Locale.ROOT)
                                listOf("ar", "bg", "de", "fa", "fr", "hu", "in", "it", 
                                    "ja", "ko", "pl", "pt-rBR", "ru", "th", "tr", 
                                    "uk", "vi", "zh-rCN", "zh-rTW").forEach { dir ->
                                    try {
                                        val locale = if (dir.contains("-r")) {
                                            val parts = dir.split("-r")
                                            java.util.Locale(parts[0], parts[1])
                                        } else {
                                            java.util.Locale(dir)
                                        }
                                        locales.add(locale)
                                    } catch (_: Exception) { }
                                }
                                locales
                            }
                            
                            val allOptions = supportedLocales.map { locale ->
                                val tag = if (locale == java.util.Locale.ROOT) "system" 
                                    else if (locale.country.isEmpty()) locale.language
                                    else "${locale.language}_${locale.country}"
                                val displayName = if (locale == java.util.Locale.ROOT) 
                                    context.getString(R.string.system_default) else locale.getDisplayName(locale)
                                tag to displayName
                            }
                            
                            val currentLocale = prefs.getString("app_locale", "system") ?: "system"
                            val options = allOptions.map { (tag, displayName) ->
                                ListOption(titleText = displayName, selected = currentLocale == tag)
                            }
                            
                            var selectedIndex by remember { 
                                mutableIntStateOf(allOptions.indexOfFirst { it.first == currentLocale }) 
                            }
                            
                            ListDialog(
                                state = rememberUseCaseState(
                                    visible = true,
                                    onFinishedRequest = {
                                        if (selectedIndex >= 0) {
                                            prefs.edit { putString("app_locale", allOptions[selectedIndex].first) }
                                            refreshActivity(context)
                                        }
                                        dismiss()
                                    },
                                    onCloseRequest = { dismiss() }
                                ),
                                header = Header.Default(title = stringResource(R.string.settings_language)),
                                selection = ListSelection.Single(showRadioButtons = true, options = options) { index, _ ->
                                    selectedIndex = index
                                }
                            )
                        }
                    }

                    val currentLanguageDisplay = remember(currentAppLocale) {
                        currentAppLocale?.getDisplayName(currentAppLocale) 
                            ?: context.getString(R.string.system_default)
                    }

                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { languageDialog.show() },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Icon(Icons.Filled.Translate, null) },
                        headlineContent = {
                            Text(text = stringResource(R.string.settings_language), fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = { Text(currentLanguageDisplay) }
                    )

                    // Theme style selection
                    val themeStyles = listOf(
                        "wild" to stringResource(R.string.theme_style_wild),
                        "md3" to stringResource(R.string.theme_style_md3),
                        "miui_x" to stringResource(R.string.theme_style_miui_x)
                    )
                    val currentThemeStyle = prefs.getString("theme_style", "wild") ?: "wild"
                    val currentThemeDisplay = themeStyles.firstOrNull { it.first == currentThemeStyle }?.second
                        ?: themeStyles[0].second

                    val themeStyleDialog = rememberCustomDialog { dismiss ->
                        val options = themeStyles.map { (_, displayName) ->
                            ListOption(titleText = displayName, selected = currentThemeStyle == themeStyles.first { it.second == displayName }.first)
                        }
                        var selectedIndex by remember { mutableIntStateOf(themeStyles.indexOfFirst { it.first == currentThemeStyle }.coerceAtLeast(0)) }

                        ListDialog(
                            state = rememberUseCaseState(
                                visible = true,
                                onFinishedRequest = {
                                    if (selectedIndex >= 0) {
                                        prefs.edit { putString("theme_style", themeStyles[selectedIndex].first) }
                                        refreshActivity(context)
                                    }
                                    dismiss()
                                },
                                onCloseRequest = { dismiss() }
                            ),
                            header = Header.Default(title = stringResource(R.string.theme_style_title)),
                            selection = ListSelection.Single(showRadioButtons = true, options = options) { index, _ ->
                                selectedIndex = index
                            }
                        )
                    }

                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { themeStyleDialog.show() },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Icon(Icons.Filled.Palette, null) },
                        headlineContent = {
                            Text(text = stringResource(R.string.theme_style_title), fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = { Text(currentThemeDisplay) }
                    )

                    var useBanner by rememberSaveable { mutableStateOf(prefs.getBoolean("use_banner", true)) }
                    if (ksuVersion != null) {
                        SwitchItem(
                            icon = Icons.Filled.ViewCarousel,
                            title = stringResource(R.string.settings_banner),
                            summary = stringResource(R.string.settings_banner_summary),
                            checked = useBanner,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) {
                            prefs.edit { putBoolean("use_banner", it) }
                            useBanner = it
                        }
                    }

                    var enableAmoled by rememberSaveable { mutableStateOf(prefs.getBoolean("enable_amoled", false)) }
                    if (isSystemInDarkTheme()) {
                        val activity = LocalContext.current as? MainActivity
                        SwitchItem(
                            icon = Icons.Filled.Contrast,
                            title = stringResource(R.string.settings_amoled_mode),
                            summary = stringResource(R.string.settings_amoled_mode_summary),
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

            Spacer(Modifier.height(80.dp)) // Bottom padding for nav bar
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = { Text(
                text = stringResource(R.string.customization),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            ) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}

@Preview
@Composable
private fun CustomizationPreview() {
    CustomizationScreen(EmptyDestinationsNavigator)
}
