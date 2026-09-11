package com.hhkungfu

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class HHKungfuPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(HHKungfuProvider())
    }
}
