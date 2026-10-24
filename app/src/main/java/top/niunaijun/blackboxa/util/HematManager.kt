package top.niunaijun.blackboxa.util

import android.content.Context
import android.util.Log
import top.niunaijun.blackbox.BlackBoxCore

/**
 * RuangKlon v0.3.0 — Mode hemat per clone.
 *
 * Prinsip: hemat dievaluasi setiap kali RuangKlon dibuka (onStart daftar
 * clone), BUKAN lewat timer/service latar — service latar justru menguras
 * baterai, yang ingin dihemat. Satu clone yang ditandai hemat akan
 * dihentikan (stopPackage, sama seperti tombol "Hentikan" manual) bila
 * sudah >= 30 menit sejak terakhir diluncurkan dari RuangKlon.
 *
 * Batasan jujur: "terakhir diluncurkan" adalah proksi waktu menganggur,
 * bukan waktu terakhir clone benar-benar di-foreground (itu butuh izin
 * UsageStats yang tidak ingin kita minta). Clone yang dibuka lewat
 * pintasan Beranda ikut tercatat, karena ShortcutActivity juga memanggil
 * recordLaunch(). Mode hemat default NONAKTIF per clone, karena
 * menghentikan clone berarti notifikasinya tertunda sampai dibuka lagi.
 */
object HematManager {
    private const val TAG = "HematManager"

    /** Clone hemat dianggap menganggur setelah 30 menit sejak terakhir dibuka. */
    const val IDLE_TIMEOUT_MS = 30L * 60L * 1000L

    private const val PREFS = "ruangklon_hemat"

    private fun ctx(): Context? {
        return try {
            BlackBoxCore.get().getContext()
        } catch (e: Exception) {
            Log.w(TAG, "BlackBoxCore context belum siap: ${e.message}")
            null
        }
    }

    private fun key(prefix: String, packageName: String, userId: Int) =
        "$prefix:$packageName|$userId"

    fun isEnabled(packageName: String, userId: Int): Boolean {
        val c = ctx() ?: return false
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key("hemat", packageName, userId), false)
    }

    fun setEnabled(packageName: String, userId: Int, enabled: Boolean) {
        val c = ctx() ?: return
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(key("hemat", packageName, userId), enabled)
            .apply()
    }

    fun recordLaunch(packageName: String, userId: Int) {
        val c = ctx() ?: return
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(key("last", packageName, userId), System.currentTimeMillis())
            .apply()
    }

    fun lastLaunch(packageName: String, userId: Int): Long {
        val c = ctx() ?: return 0L
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(key("last", packageName, userId), 0L)
    }

    fun clear(packageName: String, userId: Int) {
        val c = ctx() ?: return
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(key("hemat", packageName, userId))
            .remove(key("last", packageName, userId))
            .apply()
    }

    /**
     * Hentikan semua clone bertanda hemat yang sudah menganggur.
     * Mengembalikan daftar package yang dihentikan (kosong bila tidak ada).
     * Clone yang belum pernah tercatat peluncurannya (mis. dipasang sebelum
     * v0.3.0) tidak langsung dihentikan: peluncuran saat ini dicatat sebagai
     * baseline, dan baru dievaluasi pada pembukaan RuangKlon berikutnya.
     */
    fun sweep(userId: Int): List<String> {
        val stopped = mutableListOf<String>()
        return try {
            val now = System.currentTimeMillis()
            val installed = BlackBoxCore.get().getInstalledPackages(0, userId)
            for (info in installed) {
                val pkg = info.packageName
                if (!isEnabled(pkg, userId)) continue
                val last = lastLaunch(pkg, userId)
                if (last == 0L) {
                    recordLaunch(pkg, userId)
                    continue
                }
                if (now - last >= IDLE_TIMEOUT_MS) {
                    try {
                        BlackBoxCore.get().stopPackage(pkg, userId)
                        stopped.add(pkg)
                    } catch (e: Exception) {
                        Log.w(TAG, "Gagal menghentikan $pkg: ${e.message}")
                    }
                }
            }
            stopped
        } catch (e: Exception) {
            Log.w(TAG, "Sweep mode hemat gagal: ${e.message}")
            stopped
        }
    }
}
