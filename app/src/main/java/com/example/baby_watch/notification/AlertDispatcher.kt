package com.example.baby_watch.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.baby_watch.data.server.SafetyEvent
import com.example.baby_watch.data.server.alertTierForSeverity
import com.example.baby_watch.service.log.LogManager

object AlertDispatcher {
    fun dispatch(context: Context, event: SafetyEvent) {
        val tier = alertTierForSeverity(event.severity)
        if (tier == 0) return

        val appContext = context.applicationContext
        val title = event.title
        val detail = event.detail
        LogManager.alert(tier, title, detail)

        runCatching {
            com.example.baby_watch.notification.phone_notice.phone_notice.show(
                appContext,
                title,
                detail,
            )
        }.onFailure {
            LogManager.system("通知失败", it.message.orEmpty())
        }

        when (tier) {
            1 -> runCatching {
                com.example.baby_watch.notification.vibration.vibration.start(appContext)
            }.onFailure {
                LogManager.system("震动失败", it.message.orEmpty())
            }

            2 -> {
                runCatching {
                    com.example.baby_watch.notification.ring.ring.start(appContext, 5000)
                }.onFailure {
                    LogManager.system("响铃失败", it.message.orEmpty())
                }
                if (hasPermission(appContext, Manifest.permission.SEND_SMS)) {
                    runCatching {
                        com.example.baby_watch.notification.text.text.send(appContext)
                    }.onFailure {
                        LogManager.system("短信发送失败", it.message.orEmpty())
                    }
                } else {
                    LogManager.system("短信未发送", "缺少短信权限")
                }
            }

            3 -> {
                runCatching {
                    com.example.baby_watch.notification.ring_and_vibration.ring_and_vibration.start(
                        appContext
                    )
                }.onFailure {
                    LogManager.system("紧急响铃失败", it.message.orEmpty())
                }
                if (hasPermission(appContext, Manifest.permission.CALL_PHONE)) {
                    runCatching {
                        com.example.baby_watch.notification.call.call.dial(appContext)
                    }.onFailure {
                        LogManager.system("自动拨号失败", it.message.orEmpty())
                    }
                } else {
                    LogManager.system("未自动拨号", "缺少电话权限")
                }
            }
        }
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}
