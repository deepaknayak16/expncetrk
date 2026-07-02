package com.example.expncetracker.exptkr.core.recurring

import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.core.notification.AppNotificationManager
import com.example.expncetracker.exptkr.data.db.AppDatabase
import com.example.expncetracker.exptkr.data.db.entity.RecurringTemplateEntity
import com.example.expncetracker.exptkr.domain.model.RecurringState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@HiltWorker
class BillReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val db: AppDatabase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Logger.d("BillReminderWorker", "Guardian waking up: Checking for due bills...")
        
        return try {
            checkAndNotifyDueBills()
            Result.success()
        } catch (e: Exception) {
            Logger.e("BillReminderWorker", "Guardian failed: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun checkAndNotifyDueBills() {
        val today = LocalDate.now()
        // PHASE 6: Wake up and query recurring_templates
        val templates: List<RecurringTemplateEntity> = db.recurringTemplateDao().getDueTemplatesSync(
            System.currentTimeMillis() + (2L * 24 * 60 * 60 * 1000) // Check bills due in next 2 days
        )
        
        for (template in templates) {
            if (template.state != RecurringState.ACTIVE.name) continue
            
            val dueDate = Instant.ofEpochMilli(template.nextDueDate).atZone(ZoneId.systemDefault()).toLocalDate()
            
            // 1. Stale Bill Check (Unpaid for > 60 days)
            val lastDetected = Instant.ofEpochMilli(template.lastDetectedDate).atZone(ZoneId.systemDefault()).toLocalDate()
            if (ChronoUnit.DAYS.between(lastDetected, today) > 60) {
                db.recurringTemplateDao().updateState(template.id, RecurringState.PAUSED.name)
                Logger.d("BillReminderWorker", "Paused stale bill: ${template.merchantName}")
                continue
            }

            // 2. PHASE 7: Ghost Payment Intercept (Early payment detection)
            // Strictly check for EXPENSE in current cycle
            val startOfCycle = dueDate.minusDays(15).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val ghostPayment = db.transactionDao().findPaymentInCurrentCycle(template.cleanMerchantName, startOfCycle)
            
            if (ghostPayment != null) {
                // Price Hike Check: If amount changed by > 10%, update template
                val paymentAmount = ghostPayment.amount.toDouble()
                val templateAmount = template.amount.toDouble()
                if (abs(paymentAmount - templateAmount) > templateAmount * 0.10) {
                    db.recurringTemplateDao().updateExpectedAmount(template.id, ghostPayment.amount)
                    Logger.d("BillReminderWorker", "Updated expected amount for ${template.merchantName} due to price hike")
                }

                // ATOMIC SQL BUMP: Prevent notification by moving due date forward
                val nextMonth = dueDate.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                db.recurringTemplateDao().atomicallyBumpDate(template.id, nextMonth, ghostPayment.timestamp)
                Logger.d("BillReminderWorker", "Ghost Payment Intercepted: Silent bump for ${template.merchantName}")
                continue
            }

            // 3. Notification Delivery (If not intercepted)
            if (dueDate == today || (dueDate.isBefore(today) && ChronoUnit.DAYS.between(dueDate, today) < 3)) {
                // FIX BUG-022: Check notification permission (Android 13+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && 
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Logger.d("BillReminderWorker", "Notification permission not granted. Skipping notification.")
                } else {
                    AppNotificationManager.showBillReminderNotification(
                        context,
                        template.merchantName,
                        "₹${template.amount}"
                    )
                    Logger.d("BillReminderWorker", "Sent notification for ${template.merchantName}")
                }
            }
        }
    }
}
