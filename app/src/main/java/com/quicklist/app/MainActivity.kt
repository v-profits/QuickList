package com.quicklist.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.quicklist.app.ui.AppNav
import com.quicklist.app.ui.AppViewModel
import com.quicklist.app.ui.NavTarget
import com.quicklist.app.ui.theme.QuickListTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val vm: AppViewModel by viewModels()
    private var target by mutableStateOf<NavTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            QuickListTheme(dynamic = vm.dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNav(vm = vm, target = target, onTargetHandled = { target = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // Text shared from another app -> added to the last used list.
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            lifecycleScope.launch {
                val id = vm.addShared(text) ?: return@launch
                val name = vm.lists.value?.firstOrNull { it.list.id == id }?.list?.name.orEmpty()
                Toast.makeText(this@MainActivity, getString(R.string.added_to, name), Toast.LENGTH_SHORT).show()
                target = NavTarget(id, false)
            }
            return
        }

        val data = intent.data ?: return
        if (data.scheme != "quicklist") return
        when (data.host) {
            "list" -> {
                val id = data.pathSegments.firstOrNull()?.toLongOrNull() ?: return
                target = NavTarget(id, data.pathSegments.getOrNull(1) == "add")
            }
            "new" -> {
                val type = data.pathSegments.firstOrNull()?.toIntOrNull() ?: return
                lifecycleScope.launch {
                    vm.firstListOfType(type)?.let { target = NavTarget(it, true) }
                }
            }
        }
    }
}
