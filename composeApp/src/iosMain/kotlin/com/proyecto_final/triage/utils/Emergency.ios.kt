package com.proyecto_final.triage.utils

import platform.UIKit.UIApplication
import platform.Foundation.NSURL

actual fun dialEmergency() {
    val url = NSURL.URLWithString("tel:911")!!
    UIApplication.sharedApplication.openURL(url)
}