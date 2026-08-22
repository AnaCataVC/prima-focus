package com.ancata.prima_focus.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.layout.fillMaxHeight
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

// Widget surface colors — match existing widget_rounded_bg (white + gray border)
private val WidgetBackground = Color(0xFFFFFFFF)
private val ChipBackground = Color(0xFFF5F5F5)
private val TextPrimary = Color(0xFF000000)
private val TextMuted = Color(0xFF666666)
private val AccentRose = Color(0xFFF472B6)  // PrimaryRose from Color.kt

private val ContentPadding = 12.dp

class QuickAddWidget : GlanceAppWidget() {

    // Exact so LocalSize reports the real cell size and the layout can scale to it.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            QuickAddContent()
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
fun QuickAddContent() {
    // Sizes derived from the real cell height so the chip row fills what's left.
    val usableHeight = LocalSize.current.height.value - (ContentPadding.value * 2)
    val headerSize = (usableHeight * 0.13f).coerceIn(12f, 18f)
    val buttonSize = (usableHeight * 0.32f).coerceIn(28f, 52f)
    val chipSize = (usableHeight * 0.14f).coerceIn(12f, 20f)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .cornerRadius(16.dp)
            .padding(ContentPadding)
    ) {
        // Header row — label + rose add button
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Agregar Tarea",
                style = TextStyle(
                    color = ColorProvider(TextMuted),
                    fontSize = headerSize.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            // Generic add button (no category)
            Box(
                modifier = GlanceModifier
                    .size(buttonSize.dp)
                    .background(ColorProvider(AccentRose))
                    .cornerRadius((buttonSize / 2).dp)
                    .clickable(actionRunCallback<LaunchQuickAddAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = (buttonSize * 0.6f).sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Category chips — take all the remaining height
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryChip(
                label = "Trabajo",
                category = "trabajo",
                fontSize = chipSize,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            CategoryChip(
                label = "Salud",
                category = "salud",
                fontSize = chipSize,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            CategoryChip(
                label = "Casa",
                category = "casa",
                fontSize = chipSize,
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    category: String,
    fontSize: Float,
    modifier: GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(ColorProvider(ChipBackground))
            .cornerRadius(8.dp)
            .padding(horizontal = 4.dp)
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
                color = ColorProvider(TextPrimary),
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
