package com.example.motivation.viewmodel

import android.app.AppOpsManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motivation.data.local.AppDatabase
import com.example.motivation.data.local.AppBlockRuleEntity
import com.example.motivation.data.local.DndScheduleEntity
import com.example.motivation.focusguard.AppBlockerService
import com.example.motivation.focusguard.DndScheduleManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.motivation.data.SettingsDataStore
import java.util.Calendar

class FocusGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val focusGuardDao = database.focusGuardDao()
    private val dndScheduleManager = DndScheduleManager(application)
    private val context = application.applicationContext
    private val settingsDataStore = SettingsDataStore(application)

    init {
        viewModelScope.launch {
            focusGuardDao.getAllDndSchedules().collect { schedules ->
                dndScheduleManager.checkAndApplyCurrentDndState(schedules)
            }
        }
    }

    private val timeTicker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30000L) // emit every 30 seconds to refresh time-dependent states
        }
    }

    val isDndActive: StateFlow<Boolean> = settingsDataStore.isDndActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appBlockerConsentGranted: StateFlow<Boolean> = settingsDataStore.appBlockerConsentGranted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activeBlockedApps: StateFlow<List<String>> = combine(
        focusGuardDao.getAllAppBlockRules(),
        timeTicker
    ) { rules, _ ->
        rules.filter { it.isEnabled && isCurrentlyInBlockWindow(it) }
             .map { it.appName }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun isCurrentlyInBlockWindow(rule: AppBlockRuleEntity): Boolean {
        val cal = Calendar.getInstance()
        val dayBit = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        if (rule.daysOfWeek and (1 shl dayBit) == 0) return false
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = rule.startHour * 60 + rule.startMinute
        val endMinutes = rule.endHour * 60 + rule.endMinute
        return if (endMinutes > startMinutes) nowMinutes in startMinutes until endMinutes
               else nowMinutes >= startMinutes || nowMinutes < endMinutes
    }

    val dndSchedules: StateFlow<List<DndScheduleEntity>> =
        focusGuardDao.getAllDndSchedules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appBlockRules: StateFlow<List<AppBlockRuleEntity>> =
        focusGuardDao.getAllAppBlockRules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addDndSchedule(schedule: DndScheduleEntity) = viewModelScope.launch {
        val newId = focusGuardDao.insertDndSchedule(schedule)
        val insertedSchedule = schedule.copy(id = newId.toInt())
        if (insertedSchedule.isEnabled) {
            dndScheduleManager.scheduleDndStart(insertedSchedule)
            dndScheduleManager.scheduleDndEnd(insertedSchedule)
        }
    }

    fun toggleDndSchedule(schedule: DndScheduleEntity) = viewModelScope.launch {
        val updated = schedule.copy(isEnabled = !schedule.isEnabled)
        focusGuardDao.updateDndSchedule(updated)
        if (updated.isEnabled) {
            dndScheduleManager.scheduleDndStart(updated)
            dndScheduleManager.scheduleDndEnd(updated)
        } else {
            dndScheduleManager.cancelSchedule(updated)
        }
    }

    fun deleteDndSchedule(schedule: DndScheduleEntity) = viewModelScope.launch {
        dndScheduleManager.cancelSchedule(schedule)
        focusGuardDao.deleteDndSchedule(schedule)
    }

    fun addAppBlockRule(rule: AppBlockRuleEntity) = viewModelScope.launch {
        focusGuardDao.insertAppBlockRule(rule)
        ensureBlockerServiceRunning()
    }

    fun toggleAppBlockRule(rule: AppBlockRuleEntity) = viewModelScope.launch {
        focusGuardDao.updateAppBlockRule(rule.copy(isEnabled = !rule.isEnabled))
        ensureBlockerServiceRunning()
    }

    fun deleteAppBlockRule(packageName: String) = viewModelScope.launch {
        focusGuardDao.deleteAppBlockRule(packageName)
        val remaining = focusGuardDao.getEnabledAppBlockRules()
        if (remaining.isEmpty()) {
            AppBlockerService.stop(context)
        }
    }

    fun setAppBlockerConsentGranted(granted: Boolean) = viewModelScope.launch {
        settingsDataStore.setAppBlockerConsentGranted(granted)
    }

    private fun ensureBlockerServiceRunning() {
        AppBlockerService.start(context)
    }

    fun isUsageStatsGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isDndAccessGranted(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
}
