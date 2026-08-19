package com.focus.di

import com.focus.data.local.DatabaseDriverFactory
import com.focus.security.BiometricProvider
import com.focus.security.SecurityValidator
import org.koin.dsl.module

val androidPlatformModule = module {
    single { DatabaseDriverFactory(get()) }
    single { BiometricProvider(get()) }
    single { SecurityValidator() }
}
