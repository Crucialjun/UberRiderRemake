package com.example.uberriderremake

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.app.NotificationCompat

object Common {
    fun buildWelcomeMessage(): CharSequence? {
        return "Welcome ${currentRider?.firstName} ${currentRider?.lastName} "
    }

    val driversFound: HashSet<DriverGeoModel>()
    var currentRider : RiderInfoModel? = null

    fun showNotification(
        context : Context,
        id : Int,
        title : String?,
        body: String?,
        intent: Intent?
    ) {
        var  pendingIntent : PendingIntent? = null

        if(pendingIntent !=null){
            pendingIntent = PendingIntent
                .getActivity(context,id,intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val NOTIFICATION_CHANNEL_ID = "crucial_tech_uber_remake"
            val notificationManager : NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val notificationChannel
                        = NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Uber Remake",
                    NotificationManager.IMPORTANCE_HIGH)
                notificationChannel.description = "Uber Remake"
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.RED
                notificationChannel.vibrationPattern = longArrayOf(0,1000,500,1000,)
                notificationChannel.enableVibration(true)

                notificationManager.createNotificationChannel(notificationChannel)


            }
            val builder = NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
            builder
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(false)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.ic_baseline_directions_car_24)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources,R.drawable.ic_baseline_directions_car_24))
                .priority = NotificationCompat.PRIORITY_HIGH

            if(pendingIntent != null){
                builder.setContentIntent(pendingIntent)
            }

            val notificationBuilder = builder.build()

            notificationManager.notify(id,notificationBuilder)


        }

    }
}