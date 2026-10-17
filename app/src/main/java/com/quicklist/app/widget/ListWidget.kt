package com.quicklist.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.quicklist.app.MainActivity
import com.quicklist.app.R
import com.quicklist.app.data.ItemEntity
import com.quicklist.app.data.ListEntity
import com.quicklist.app.data.Repository
import com.quicklist.app.util.SortMode
import com.quicklist.app.util.sortItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = Repository.get(context)

        // Initial values so the widget is not empty for a moment.
        val initialLists = repo.allLists()
        val initialPrefs: Preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val initialList = initialLists.firstOrNull { it.id == initialPrefs[LIST_ID_KEY] } ?: initialLists.firstOrNull()
        val initialItems = if (initialList != null) repo.itemsOf(initialList.id) else emptyList()

        provideContent {
            GlanceTheme {
                // The widget observes the database itself, so every change (ticking a box,
                // adding an item in the app) is reflected immediately.
                val lists by repo.observeLists().collectAsState(initial = initialLists)
                val prefs = currentState<Preferences>()
                val list = lists.firstOrNull { it.id == prefs[LIST_ID_KEY] } ?: lists.firstOrNull()
                val itemsFlow: Flow<List<ItemEntity>> = remember(list?.id) {
                    if (list != null) repo.observeItems(list.id) else flowOf(emptyList())
                }
                val raw by itemsFlow.collectAsState(initial = initialItems)
                val mode = SortMode.fromOrdinal(list?.sortMode ?: 2)
                val shown = remember(raw, mode) {
                    (sortItems(raw.filter { !it.done }, mode) +
                            raw.filter { it.done }.sortedByDescending { it.doneAt ?: 0L }).take(80)
                }
                WidgetContent(context, list, shown)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, list: ListEntity?, items: List<ItemEntity>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(20.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = list?.name ?: context.getString(R.string.app_name),
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(openIntent(context, list?.id, false))),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Image(
                    provider = ImageProvider(R.drawable.ic_add),
                    contentDescription = context.getString(R.string.widget_add),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                    modifier = GlanceModifier
                        .size(40.dp)
                        .padding(8.dp)
                        .clickable(actionStartActivity(openIntent(context, list?.id, true)))
                )
            }

            if (items.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = context.getString(R.string.widget_empty),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(items, itemId = { it.id }) { item ->
                        val label = if (item.quantity.isNotBlank()) "${item.title} · ${item.quantity}" else item.title
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .clickable(
                                    actionRunCallback<ToggleItemAction>(actionParametersOf(ItemIdKey to item.id))
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(
                                    if (item.done) R.drawable.ic_checkbox_on else R.drawable.ic_checkbox_off
                                ),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(
                                    if (item.done) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.primary
                                ),
                                modifier = GlanceModifier.size(22.dp)
                            )
                            Text(
                                text = label,
                                maxLines = 2,
                                modifier = GlanceModifier.padding(start = 12.dp),
                                style = TextStyle(
                                    color = if (item.done) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                                    fontSize = 15.sp,
                                    textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openIntent(context: Context, listId: Long?, add: Boolean): Intent {
        val uri = when {
            listId == null -> "quicklist://home"
            add -> "quicklist://list/$listId/add"
            else -> "quicklist://list/$listId"
        }
        return Intent(context, MainActivity::class.java)
            .setData(Uri.parse(uri))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    companion object {
        val LIST_ID_KEY = longPreferencesKey("list_id")
    }
}

val ItemIdKey = ActionParameters.Key<Long>("item_id")

class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ItemIdKey] ?: return
        Repository.get(context).toggle(id)
    }
}

class ListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ListWidget()
}
