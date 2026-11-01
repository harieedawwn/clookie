package io.github.harieedawwn.clookie.reliability

/**
 * OEM autostart / background pages common in Vietnam. Each brand lists known activities
 * across ROM versions, tried in order; every one can be missing, so callers always fall
 * back to the app details page.
 */
enum class Oem(
    val brands: List<String>,
    val pages: List<Pair<String, String>>,
    /** "Show on lock screen" / "pop-up windows while in background" toggles, where the ROM has its own. */
    val lockScreenPages: List<Pair<String, String>> = emptyList(),
) {
    Xiaomi(
        listOf("xiaomi", "redmi", "poco"),
        listOf(
            "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.miui.securitycenter" to "com.miui.powercenter.PowerSettings",
        ),
        listOf(
            "com.miui.securitycenter" to "com.miui.permcenter.permissions.PermissionsEditorActivity",
            "com.miui.securitycenter" to "com.miui.permcenter.permissions.AppPermissionsEditorActivity",
        ),
    ),
    Oppo(
        listOf("oppo", "realme", "oneplus"),
        listOf(
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
            "com.coloros.oppoguardelf" to "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity",
        ),
    ),
    Vivo(
        listOf("vivo", "iqoo"),
        listOf(
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        ),
    ),
    Samsung(
        listOf("samsung"),
        listOf(
            "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.BatteryActivity",
            "com.samsung.android.sm" to "com.samsung.android.sm.ui.battery.BatteryActivity",
        ),
    ),
    Huawei(
        listOf("huawei", "honor"),
        listOf(
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
        ),
    );

    companion object {
        /** Brand from Build.MANUFACTURER / Build.BRAND, or null for stock-like ROMs. */
        fun detect(manufacturer: String, brand: String): Oem? {
            val m = manufacturer.lowercase()
            val b = brand.lowercase()
            return entries.firstOrNull { oem -> oem.brands.any { it == m || it == b } }
        }
    }
}
