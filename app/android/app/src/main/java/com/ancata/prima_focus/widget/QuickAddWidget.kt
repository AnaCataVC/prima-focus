package com.ancata.prima_focus.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ancata.prima_focus.MainActivity
import com.ancata.prima_focus.utils.Constants
import android.content.Intent

class QuickAddWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            QuickAddContent(context = context)
        }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}

class LaunchQuickAddAction : ActionCallback {
    companion object {
        val CategoryKey = ActionParameters.Key<String>("category_key")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val category = parameters[CategoryKey]
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.ancata.prima_focus.ACTION_ADD_TASK"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (category != null) {
                putExtra(Constants.EXTRA_PREFILLED_CATEGORY, category)
            }
        }
        context.startActivity(intent)
    }
}

@Composable
fun QuickAddContent(context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF1E1E2E)))
            .cornerRadius(16.dp)
            .padding(10.dp)
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ Nueva Tarea",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFBD93F9)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            // Generic '+' add button
            Box(
                modifier = GlanceModifier
                    .size(24.dp)
                    .background(ColorProvider(Color(0xFFBD93F9)))
                    .cornerRadius(12.dp)
                    .clickable(
                        actionRunCallback<LaunchQuickAddAction>()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF1E1E2E)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Quick Category Action Chips
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryChip(
                label = "💼 Trabajo",
                color = Color(0xFFFF79C6),
                category = "trabajo",
                modifier = GlanceModifier.defaultWeight()
            )

            Spacer(modifier = GlanceModifier.width(4.dp))

            CategoryChip(
                label = "🏥 Salud",
                color = Color(0xFF8BE9FD),
                category = "salud",
                modifier = GlanceModifier.defaultWeight()
            )

            Spacer(modifier = GlanceModifier.width(4.dp))

            CategoryChip(
                label = "🏠 Casa",
                color = Color(0xFF50FA7B),
                category = "casa",
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    color: Color,
    category: String,
    modifier: GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .background(ColorProvider(Color(0xFF282A36)))
            .cornerRadius(8.dp)
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .clickable(
                actionRunCallback<LaunchQuickAddAction>(
                    actionParametersOf(LaunchQuickAddAction.CategoryKey to category)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(color),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
