/*
 * Quasseldroid - Quassel client for Android
 *
 * Copyright (c) 2018 Janne Koschinski
 * Copyright (c) 2018 The Quassel Project
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.kuschku.quasseldroid.service

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.RemoteInput
import de.kuschku.libquassel.protocol.Buffer_Type
import de.kuschku.libquassel.quassel.BufferInfo
import de.kuschku.libquassel.util.flag.hasFlag
import de.kuschku.quasseldroid.R
import de.kuschku.quasseldroid.settings.NotificationSettings
import de.kuschku.quasseldroid.ui.chat.ChatActivity
import de.kuschku.quasseldroid.util.NotificationMessage
import de.kuschku.quasseldroid.util.helper.getColorCompat
import javax.inject.Inject

class QuasseldroidNotificationManager @Inject constructor(private val context: Context) {
  private val notificationManagerCompat = NotificationManagerCompat.from(context)

  fun init() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      prepareChannels()
  }

  @TargetApi(Build.VERSION_CODES.O)
  private fun prepareChannels() {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannels(
      listOf(
        NotificationChannel(
          context.getString(R.string.notification_channel_background),
          context.getString(R.string.notification_channel_connection_title),
          NotificationManager.IMPORTANCE_LOW
        ),
        NotificationChannel(
          context.getString(R.string.notification_channel_highlight),
          context.getString(R.string.notification_channel_highlight_title),
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          enableLights(true)
          enableVibration(true)
          lightColor = context.getColorCompat(R.color.colorPrimary)
          lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
      )
    )
  }

  private fun bitmapFromDrawable(drawable: Drawable): Bitmap {
    val bitmap = Bitmap.createBitmap(
      context.resources.getDimensionPixelSize(R.dimen.notification_avatar_width),
      context.resources.getDimensionPixelSize(R.dimen.notification_avatar_height),
      Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
  }

  fun notificationMessage(notificationSettings: NotificationSettings, bufferInfo: BufferInfo,
                          notifications: List<NotificationMessage>): Handle {
    val pendingIntentOpen = PendingIntent.getActivity(
      context.applicationContext,
      System.currentTimeMillis().toInt(),
      ChatActivity.intent(context.applicationContext, bufferId = bufferInfo.bufferId).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
      },
      0
    )

    val remoteInput = RemoteInput.Builder("reply_content")
      .setLabel(context.getString(R.string.label_reply))
      .build()

    val replyPendingIntent = PendingIntent.getService(
      context.applicationContext,
      System.currentTimeMillis().toInt(),
      QuasselService.intent(
        context,
        bufferId = bufferInfo.bufferId,
        markReadMessage = notifications.last().messageId
      ),
      0
    )

    val markReadPendingIntent = PendingIntent.getService(
      context.applicationContext,
      System.currentTimeMillis().toInt(),
      QuasselService.intent(
        context,
        bufferId = bufferInfo.bufferId,
        markReadMessage = notifications.last().messageId
      ),
      0
    )

    val deletePendingIntent = PendingIntent.getService(
      context.applicationContext,
      System.currentTimeMillis().toInt(),
      QuasselService.intent(
        context,
        bufferId = bufferInfo.bufferId,
        markReadMessage = notifications.last().messageId
      ),
      0
    )

    val notification = NotificationCompat.Builder(
      context.applicationContext,
      context.getString(R.string.notification_channel_highlight)
    )
      .setContentIntent(pendingIntentOpen)
      .setDeleteIntent(deletePendingIntent)
      .setSmallIcon(R.mipmap.ic_logo)
      .setColor(context.getColorCompat(R.color.colorPrimary))
      .setLights(context.getColorCompat(R.color.colorPrimary), 200, 200)
      .apply {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
          var defaults = 0
          if (!notificationSettings.sound.isEmpty()) {
            setSound(Uri.parse(notificationSettings.sound))
          }
          if (notificationSettings.vibrate) {
            defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
          }
          setDefaults(defaults)
        }
      }
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setStyle(NotificationCompat.MessagingStyle("")
                  .setConversationTitle(bufferInfo.bufferName)
                  .also {
                    for (notification in notifications) {
                      it.addMessage(
                        notification.content,
                        notification.time.toEpochMilli(),
                        notification.sender
                      )
                    }
                  }
      )
      .addAction(0, context.getString(R.string.label_mark_read), markReadPendingIntent)
      .addAction(
        NotificationCompat.Action.Builder(
          0,
          context.getString(R.string.label_reply),
          replyPendingIntent
        ).addRemoteInput(remoteInput).build()
      )
      .setWhen(notifications.last().time.toEpochMilli())
      .apply {
        if (bufferInfo.type.hasFlag(Buffer_Type.QueryBuffer)) {
          notifications.lastOrNull()?.avatar?.let {
            setLargeIcon(bitmapFromDrawable(it))
          }
        }
      }
    return Handle(bufferInfo.bufferId, notification)
  }

  fun notificationBackground(): Handle {
    val pendingIntentOpen = PendingIntent.getActivity(
      context.applicationContext,
      System.currentTimeMillis().toInt(),
      ChatActivity.intent(context.applicationContext).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
      },
      0
    )

    val pendingIntentDisconnect = PendingIntent.getService(
      context,
      System.currentTimeMillis().toInt(),
      QuasselService.intent(context.applicationContext, disconnect = true),
      0
    )

    val notification = NotificationCompat.Builder(
      context.applicationContext,
      context.getString(R.string.notification_channel_background)
    )
      .setContentIntent(pendingIntentOpen)
      .addAction(0, context.getString(R.string.label_open), pendingIntentOpen)
      .addAction(0, context.getString(R.string.label_disconnect), pendingIntentDisconnect)
      .setSmallIcon(R.mipmap.ic_logo)
      .setColor(context.getColorCompat(R.color.colorPrimary))
      .setPriority(NotificationCompat.PRIORITY_MIN)
    return Handle(BACKGROUND_NOTIFICATION_ID, notification)
  }

  fun notify(handle: Handle) {
    notificationManagerCompat.notify(handle.id, handle.builder.build())
  }

  fun remove(handle: Handle) {
    notificationManagerCompat.cancel(handle.id)
  }

  fun remove(id: Int) {
    notificationManagerCompat.cancel(id)
  }

  companion object {
    const val BACKGROUND_NOTIFICATION_ID = Int.MAX_VALUE
  }

  data class Handle(
    val id: Int,
    val builder: NotificationCompat.Builder
  )
}
