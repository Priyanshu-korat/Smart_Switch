package com.smarthome.app

import android.app.Application
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.smarthome.core.work.SmartHomeWorkManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SmartHomeApplication : Application() {

    @Inject lateinit var workManager: SmartHomeWorkManager

    override fun onCreate() {
        super.onCreate()
        
        // Basic Timber setup
        Timber.plant(Timber.DebugTree())

        // Initialize Amplify for AWS Cognito
        try {
            // Amplify.addPlugin(AWSCognitoAuthPlugin())
            // Amplify.configure(applicationContext)
            Timber.i("Amplify initialization mocked")
        } catch (e: Exception) {
            Timber.e(e, "Could not initialize Amplify")
        }

        // Start background periodic sync
        // workManager.initializePeriodicSync(this)
    }
}
