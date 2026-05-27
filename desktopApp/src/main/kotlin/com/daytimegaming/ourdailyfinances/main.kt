package com.daytimegaming.ourdailyfinances

import android.app.Application
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.daytimegaming.ourdailyfinances.di.initKoin
import com.google.firebase.FirebasePlatform
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize

fun main() {
    initFirebase()
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "OurDailyFinances",
        ) {
            App()
        }
    }
}

private fun initFirebase() {
    FirebasePlatform.initializeFirebasePlatform(
        platform = object : FirebasePlatform() {
            val storage = mutableMapOf<String, String>()

            override fun clear(key: String) {
                storage.remove(key)
            }

            override fun log(msg: String) {
                println(msg)
            }

            override fun retrieve(key: String): String? = storage[key]

            override fun store(key: String, value: String) {
                storage[key] = value
            }
        }
    )

    val options = FirebaseOptions(
        projectId = "ourdailyfinances",
        applicationId = "1:861129828943:web:6fef67d9b570aa877e183a",
        apiKey = "AIzaSyAPOICLHlGGgHDDEwNRRreQe5T8jqXiwuU",
        authDomain = "ourdailyfinances.firebasestorage.appm",
        storageBucket = "ourdailyfinances.firebasestorage.app",
    )

    Firebase.initialize(Application(), options)

//    runBlocking {
//        Firebase.firestore.clearPersistence()
//    }
}