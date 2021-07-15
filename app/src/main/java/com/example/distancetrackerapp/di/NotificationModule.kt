package com.example.distancetrackerapp.di

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.distancetrackerapp.ui.MainActivity
import com.example.distancetrackerapp.R
import com.example.distancetrackerapp.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.distancetrackerapp.util.Constants.PENDING_INTENT_REQUEST_CODE
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

/**
 * this obj
 */

//Modules provide a container for your app's source code, resource files, and app level settings.
@Module
@InstallIn(ServiceComponent::class)
object NotificationModule {

    //Scope annotation for bindings that should exist for the life of a service.
    @ServiceScoped
    //Use @Provides to tell Dagger how to provide classes that are not owned by your project
    @Provides

            /**
             * provide  info to  notification
             */
    fun providePendingIntent(
        @ApplicationContext context: Context
    ): PendingIntent {
        return PendingIntent.getActivity(
            context,
            PENDING_INTENT_REQUEST_CODE,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @ServiceScoped
    @Provides
            /**
             * create wt the user will see
             */
    fun provideNotificationBuilder(
        @ApplicationContext context: Context,
        pendingIntent: PendingIntent
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_baseline_directions_run_24)
            .setContentIntent(pendingIntent)
    }

    @ServiceScoped
    @Provides
            /**
             * identifies this notification from your app to the system
             */
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

}