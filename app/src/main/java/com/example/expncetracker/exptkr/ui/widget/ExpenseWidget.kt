package com.example.expncetracker.exptkr.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.room.Room
import com.example.expncetracker.exptkr.MainActivity
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

class ExpenseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Initialize SQLCipher
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: Exception) {
            // Log error or handle failure
        }

        val database = provideDatabase(context)
        val transactionDao = database.transactionDao()
        val budgetDao = database.budgetDao()

        val now = LocalDateTime.now()
        val startOfDay = now.withHour(0).withMinute(0).withSecond(0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = now.withHour(23).withMinute(59).withSecond(59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val todayTransactions = transactionDao.getTransactionsInRange(startOfDay, endOfDay).first()
        val todaySpending = todayTransactions.filter { it.type == "DEBIT" }.sumOf { it.amount }
        
        val budgets = budgetDao.getAllBudgets().first()
        val totalBudget = budgets.sumOf { it.limitAmount }
        val remainingBudget = totalBudget - todaySpending 

        provideContent {
            WidgetContent(todaySpending, remainingBudget)
        }
    }

    private fun provideDatabase(context: Context): AppDatabase {
        val passphrase = "expense_tracker_secure_key".toByteArray()
        val factory = SupportOpenHelperFactory(passphrase)
        return Room.databaseBuilder(context, AppDatabase::class.java, Constants.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .openHelperFactory(factory)
            .build()
    }

    @Composable
    private fun WidgetContent(todaySpending: Double, remaining: Double) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(com.example.expncetracker.R.drawable.glance_appwidget_background))
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
                        style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.Gray))
                    )
                    Text(
                        text = "₹${todaySpending.toInt()}",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.White))
                    )
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Left",
                        style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.Gray))
                    )
                    Text(
                        text = "₹${remaining.toInt()}",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorProvider(if (remaining < 0) Color.Red else Color.Green))
                    )
                }
            }
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Button(
                text = "+ Add Expense",
                onClick = actionStartActivity<MainActivity>(),
                modifier = GlanceModifier.fillMaxWidth().height(36.dp)
            )
        }
    }
}

class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()
}
