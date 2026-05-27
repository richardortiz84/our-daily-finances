package com.daytimegaming.ourdailyfinances.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java

actual fun httpClientEngine(): HttpClientEngine = Java.create()
