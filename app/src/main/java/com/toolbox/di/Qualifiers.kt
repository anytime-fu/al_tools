package com.toolbox.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EncryptedPrefs

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlainPrefs
