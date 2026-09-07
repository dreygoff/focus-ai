package app.focus.system

object VendorHintsProvider {

    fun getVendorByBuildManufacturer(): String {
        return when (android.os.Build.MANUFACTURER.lowercase()) {
            "xiaomi", "redmi", "poco" -> "MIUI"
            "huawei", "honor" -> "HUAWEI"
            "samsung", "sam" -> "Samsung"
            else -> "OTHER"
        }
    }

    fun getDeepLinkForBatteryConfig(vendor: String): String? {
        return when (vendor) {
            "MIUI" -> "miui://setting/thirdapp/?action=appprivacely&pkgName="
            "HUAWEI" -> "hwps://com.huawei.systemshell/settings/ignore_battery_optimizations?vendor=huaWei"
            "Samsung" -> "samsung://com.samsung.android.Settings.Battery/"
            "ONEUI" -> "oneui://com.samsung.android(Settings.Battery)"
            else -> null
        }
    }

    fun getSkipHint(vendor: String): String {
        return when (vendor) {
            "MIUI" -> "To allow Focus to work correctly, please enable Auto-start in MIUI battery settings."
            "HUAWEI" -> "To allow Focus to work correctly, please add it to the Protected apps list in Huawei battery optimization settings."
            "Samsung" -> "Put Focus in the Unmonitored Apps list to prevent Samsung from killing it."
            else -> "Consider disabling battery optimization for Focus on this device."
        }
    }

    fun isMIUI(): Boolean = getVendorByBuildManufacturer() == "MIUI"

    fun isHuawei(): Boolean = getVendorByBuildManufacturer() == "HUAWEI"

    fun isSamsung(): Boolean = getVendorByBuildManufacturer() in setOf("Samsung", "ONEUI")
}
