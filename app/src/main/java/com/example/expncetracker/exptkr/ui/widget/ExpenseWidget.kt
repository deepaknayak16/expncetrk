package com.example.expncetracker.exptkr.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.GlanceTheme
import com.example.expncetracker.exptkr.R
import com.example.expncetracker.exptkr.MainActivity
import com.example.expncetracker.exptkr.di.DatabaseModule
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import android.content.Intent
class ExpenseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: Exception) {
            provideContent { Text("Database unavailable") }
            return
        }

        val database = try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                DatabaseModule.DatabaseEntryPoint::class.java
            )
            entryPoint.database()
        } catch (e: Exception) {
            provideContent {
                GlanceTheme{
                    Text("Database error",
                        modifier = GlanceModifier.padding(16.dp))
                }
            }
            return
        }
        // Safe database access via Singleton pattern
        //val database = AppDatabase.getInstance(context)
        val transactionDao = database.transactionDao()
        val budgetDao = database.budgetDao()

        // Time Calculations
        val now = LocalDateTime.now()
        val startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Fetching Data safely inside a suspend context
        val todayTransactions = transactionDao.getTransactionsInRange(startOfDay, endOfDay).first()
        val todaySpending = todayTransactions.filter { !it.isRecurring && it.type == "DEBIT" }.sumOf { it.amount }

        val monthTransactions = transactionDao.getTransactionsInRange(startOfMonth, endOfDay).first()
        val monthSpending = monthTransactions.filter { !it.isRecurring && it.type == "DEBIT" }.sumOf { it.amount }

        val budgets = budgetDao.getAllBudgets().first()
        val totalMonthlyBudget = budgets.sumOf { it.limitAmount }
        val remainingMonthlyBudget = totalMonthlyBudget - monthSpending

        provideContent {
            GlanceTheme {
                WidgetContent(todaySpending, remainingMonthlyBudget)
            }
        }
    }

    @Composable
    private fun WidgetContent(todaySpending: BigDecimal, remaining: BigDecimal) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                // Fixed the package path for R.drawable
                .background(ImageProvider(R.drawable.glance_appwidget_background))
                .appWidgetBackground()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Today",
                        style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                    )
                    Text(
                        text = "₹${"%.2f".format(todaySpending)}",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface)
                    )
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Left",
                        style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                    )
                    Text(
                        text = "₹${"%.2f".format(remaining)}",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (remaining.signum() < 0) GlanceTheme.colors.error else androidx.glance.color.ColorProvider(Color.Green, Color.Green))
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Button(
                text = context.getString(R.string.widget_add_expense),
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("navigate_to", "add_transaction")
                    }
                ),
            )
        }
    }
}

class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()
}
