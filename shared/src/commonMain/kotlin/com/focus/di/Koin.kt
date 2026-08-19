package com.focus.di

import com.focus.data.local.DatabaseDriverFactory
import com.focus.data.local.createDatabase
import com.focus.data.repository.*
import com.focus.sync.SqlDelightSyncQueue
import com.focus.sync.SyncOrchestrator
import com.focus.sync.SyncQueue
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    single { createDatabase(get<DatabaseDriverFactory>()) }
    single<MoodRepository> { SqlDelightMoodRepository(get()) }
    single<JournalRepository> { SqlDelightJournalRepository(get()) }
    single<PrivateJournalRepository> { SqlDelightPrivateJournalRepository(get()) }
    single<QuoteRepository> { SqlDelightQuoteRepository(get()) }
    single<AchievementRepository> { SqlDelightAchievementRepository(get()) }
    single<FocusGuardRepository> { SqlDelightFocusGuardRepository(get()) }
    single<SyncQueue> { SqlDelightSyncQueue(get()) }
    single { SyncOrchestrator(get()) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule)
    }
