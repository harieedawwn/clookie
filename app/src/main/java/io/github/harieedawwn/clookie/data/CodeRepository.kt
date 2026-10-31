package io.github.harieedawwn.clookie.data

import android.content.Context
import io.github.harieedawwn.clookie.data.db.ClookieDatabase
import io.github.harieedawwn.clookie.data.db.CookieCodeEntity
import io.github.harieedawwn.clookie.data.db.ScanCodeEntity
import io.github.harieedawwn.clookie.model.CookieCode
import io.github.harieedawwn.clookie.model.ScanCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The user's Cookie Codes and registered QR codes / barcodes (device-protected storage). */
class CodeRepository private constructor(context: Context) {
    private val dao = ClookieDatabase.get(context).codes()

    val cookieCodes: Flow<List<CookieCode>> = dao.observeCookieCodes().map { l -> l.map { CookieCode(it.id, it.name, it.pattern) } }
    val scanCodes: Flow<List<ScanCode>> = dao.observeScanCodes().map { l -> l.map { ScanCode(it.id, it.name, it.format, it.value) } }

    suspend fun cookieCodesNow(): List<CookieCode> = dao.cookieCodes().map { CookieCode(it.id, it.name, it.pattern) }
    suspend fun scanCodesNow(): List<ScanCode> = dao.scanCodes().map { ScanCode(it.id, it.name, it.format, it.value) }

    suspend fun addCookieCode(name: String, pattern: Int): Long =
        dao.insertCookieCode(CookieCodeEntity(name = name, pattern = pattern, createdAt = System.currentTimeMillis()))

    suspend fun deleteCookieCode(id: Long) = dao.deleteCookieCode(id)

    suspend fun addScanCode(name: String, format: String, value: String): Long =
        dao.insertScanCode(ScanCodeEntity(name = name, format = format, value = value, createdAt = System.currentTimeMillis()))

    suspend fun deleteScanCode(id: Long) = dao.deleteScanCode(id)

    companion object {
        @Volatile private var instance: CodeRepository? = null

        fun get(context: Context): CodeRepository = instance ?: synchronized(this) {
            instance ?: CodeRepository(context.applicationContext).also { instance = it }
        }
    }
}
