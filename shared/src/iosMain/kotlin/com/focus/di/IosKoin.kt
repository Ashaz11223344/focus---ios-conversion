package com.focus.di

import com.focus.data.local.DatabaseDriverFactory
import com.focus.security.BiometricProvider
import com.focus.security.SecurityValidator
import org.koin.dsl.module

val iosPlatformModule = module {
    single { DatabaseDriverFactory() }
    single { BiometricProvider() }
    single { SecurityValidator() }
}

fun initKoinIos() = initKoin {
    modules(iosPlatformModule)
}
