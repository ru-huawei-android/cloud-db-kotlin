package com.huawei.agc.clouddb.myquickstart.ui

import android.app.Application
import com.huawei.agconnect.cloud.database.AGConnectCloudDB

/**
 * 1st step!
 *
 * Global initialization CloudDB.
 * Do not forget to add name to the manifest
 */
class CloudDBQuickStartApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        AGConnectCloudDB.initialize(this)
    }
}