package com.quicklist.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.quicklist.app.R
import com.quicklist.app.data.ListEntity
import com.quicklist.app.data.Prefs
import com.quicklist.app.data.Repository
import com.quicklist.app.data.TYPE_SHOPPING
import com.quicklist.app.ui.listIconFor
import com.quicklist.app.ui.theme.QuickListTheme
import kotlinx.coroutines.launch

/** Lets the user pick which list a widget shows (also reachable via "reconfigure" on Android 12+). */
class WidgetConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val repo = Repository.get(this)
        setContent {
            QuickListTheme(dynamic = Prefs(this).dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var lists by remember { mutableStateOf<List<ListEntity>?>(null) }
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        repo.ensureDefaults()
                        lists = repo.allLists()
                    }
                    Column(Modifier.padding(top = 48.dp)) {
                        Text(
                            stringResource(R.string.widget_pick),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(24.dp)
                        )
                        val ls = lists
                        if (ls != null && ls.isEmpty()) {
                            Text(stringResource(R.string.widget_no_lists), modifier = Modifier.padding(24.dp))
                        } else if (ls != null) {
                            LazyColumn {
                                items(ls, key = { it.id }) { l ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { pick(l.id) }
                                            .padding(horizontal = 24.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Icon(
                                            listIconFor(l),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(16.dp))
                                        Text(l.name, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun pick(listId: Long) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(widgetId)
            updateAppWidgetState(this@WidgetConfigActivity, glanceId) { prefs ->
                prefs[ListWidget.LIST_ID_KEY] = listId
            }
            ListWidget().update(this@WidgetConfigActivity, glanceId)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}
