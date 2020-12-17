package com.enes5519.rhythm.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {
    fun checkAndRequestPermission(activity: Activity, permission: String): Boolean {
        if(ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)){
                val builder = AlertDialog.Builder(activity)
                builder.setTitle("Neden İzin Vermelisin?")
                builder.setMessage("Müzik indirmek ve indirdiğin müzikleri listelemek için bu izinler gereklidir.")
                builder.setPositiveButton("İzin Ver") { _, _ ->
                    ActivityCompat.requestPermissions(
                        activity,
                        listOf(permission).toTypedArray(),
                        100
                    )
                }
                builder.show()
            }else{
                ActivityCompat.requestPermissions(
                    activity,
                    listOf(permission).toTypedArray(),
                    100
                )
            }

            return false
        }

        return true
    }

    fun onRequestPermissionsResult(grantResults: IntArray, context: Context, packageName: String){
        var permission = PackageManager.PERMISSION_GRANTED
        for(res in grantResults){
            permission += res
        }

        if(grantResults.isEmpty() && permission != PackageManager.PERMISSION_GRANTED){
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            builder.setTitle("İzin Gerekli")
            builder.setMessage("Uygulama ayarlarından tüm izinleri vermelisin.")
            builder.setPositiveButton(
                "Ayarlara Git"
            ) { _,_ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.data = Uri.parse("package:$packageName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                context.startActivity(intent)
            }
            builder.show()
        }
    }
}