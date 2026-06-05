package com.ztros.ztrosu.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.EmptyDestinationsNavigator
import com.ztros.ztrosu.ui.theme.ZtrosuTheme

/**
 * VFS Debug 独立 Activity（UI-Only Mode）
 *
 * 从 Developer 页面启动，用于压测 VFS 控制中心 UI
 */
class VFSDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ZtrosuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VFSDebugScreen(
                        navigator = EmptyDestinationsNavigator
                    )
                }
            }
        }
    }
}
