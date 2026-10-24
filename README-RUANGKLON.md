# RuangKlon v0.1 (kerja dalam proses)

Remake Premium BD Cloner versi kita: ruang paralel untuk akun ganda,
tanpa iklan, tanpa analytics, tanpa server.

## Fondasi (atribusi)

Mesin virtualisasi: **BlackBox**, fork `ALEX5402/NewBlackbox`,
commit `89b5983` (merge PR #54), lisensi **Apache-2.0** (lihat `LICENSE`).
BlackBox sendiri turunan VirtualApp / VirtualAPK.
Kode host di repo ini belum diubah dari upstream pada tahap bukti-mesin v0.1,
kecuali penambahan workflow CI ini dan dokumen ini — rebrand UI menyusul
setelah build pertama hijau dan clone pertama terbukti jalan di HP.

## Hasil audit (2026-10-24)

- Lisensi: Apache-2.0 — boleh dipakai, diubah, dan didistribusikan dengan atribusi.
- Upstream sendiri **tanpa iklan** (tidak ada AdMob/AppLovin/Pangle/Vungle di dependensi).
- Izin host upstream hanya: `QUERY_ALL_PACKAGES`, baca/tulis storage eksternal.
- API inti yang dipakai v0.1: `BlackBoxCore.get().installPackageAsUser()`,
  `launchApk(packageName, userId)`, `uninstallPackageAsUser()`, `getInstalledApps()`.
- **Koreksi terhadap RANCANGAN awal:** upstream memakai `compileSdk 35`
  tapi `targetSdk 28`, bukan 34. Target 28 dipertahankan untuk v0.1 karena
  menaikkan target berisiko merusak hooking/hidden-API virtualisasi.
  Target modern diuji terpisah setelah mesin terbukti jalan, bukan dijanjikan di v0.1.
- Gradle 8.14.5, AGP 8.13.2, Kotlin 1.9.23, JDK 21, NDK 29.0.13846066,
  ABI: arm64-v8a + armeabi-v7a, APK split + universal.

## Build

Hanya lewat GitHub Actions (`.github/workflows/build.yml`):
`./gradlew :app:assembleDebug` di runner ubuntu-24.04.
Build lokal di VM Muse tidak bisa (loopback JVM rusak — lihat AGENTS.md repo utama).

## Perubahan RuangKlon v0.1.1 (patch VPN)

Upstream `MainActivity` memanggil `checkVpnPermission()` tanpa syarat di
setiap `onCreate`, sehingga aplikasi langsung terdaftar sebagai aplikasi VPN
di sistem dan NordVPN menganggap slot VPN Android terisi — padahal mode
jaringan VPN BlackBox bawaan NONAKTIF (`useVpnNetwork` default false,
`ProxyVpnService` hanya jalan bila setting itu aktif).

Patch: `MainActivity` hanya memanggil `checkVpnPermission()` bila
`AppManager.mBlackBoxLoader.useVpnNetwork()` true, dan fungsi itu sendiri
menolak bila mode tersebut nonaktif (pertahanan kedua, fail-closed ke
"tidak minta izin"). Tidak ada pemanggil `VpnService.prepare` lain di
kode aplikasi. Catatan: patch ini mencegah BlackBox versi baru muncul di
daftar NordVPN; ia TIDAK memperbaiki NordVPN yang sudah diblokir oleh
aplikasi VPN lain / Always-on di HP pengguna — itu tetap harus
diselesaikan di Setelan VPN (Forget / matikan Always-on aplikasi tersebut).

## Batasan v0.1 yang diketahui

- Build debug pertama memakai debug keystore bawaan runner (belum di-pin).
  Keystore tetap (seperti pola RDPfread XD v0.3.1) dipasang sebelum rilis
  yang ditujukan untuk update menimpa.
- WhatsApp/Telegram terbaru bisa menolak berjalan di virtual env
  (deteksi virtualisasi / flag REQUIRE_SECURE_ENV). Ini tolok ukur go/no-go
  uji lapangan, bukan janji.

## Rebrand RuangKlon v0.2.0 (jalur aman, atas perintah user 2026-10-24)

- `applicationId` baru `id.ruangklon` (nama launcher: RuangKlon, versi
  0.2.0 code 200). Paket kode internal (`top.niunaijun.*`) TIDAK diubah:
  ia muncul di 570 file termasuk komponen manifest Bcore, taskAffinity,
  dan jalur refleksi mesin virtual — rename total berisiko merusak mesin
  clone dan baru ketahuan di lapangan. Atribusi Apache-2.0 ke upstream
  tetap dipertahankan di README ini.
- String utama diterjemahkan ke Bahasa Indonesia; tema soft neobrutalism
  (kertas #FFFDF7, tinta #161616, kuning #FFD02E); ikon launcher baru
  (motif dua jendela tumpang = clone); splash latar kertas.
- Menu grup Telegram upstream DICABUT (tidak ada afiliasi); menu Kode
  Sumber kini mengarah ke `github.com/qioceky/ruangklon`.
- PRIVASI — temuan audit v0.2.0: upstream `ClientConfiguration`
  mengembalikan chatId Telegram hardcoded dan `BlackBoxCore.sendLogs()`
  mengunggah logcat + info perangkat ke API pihak ketiga (Vercel) saat
  crash maupun lewat preferensi "Send Logs" di Pengaturan. Di RuangKlon
  chatId dikosongkan (pengiriman langsung return, tanpa upload) dan
  preferensi "Send Logs" dicabut dari Pengaturan.
- Keystore tetap (JKS, alias androiddebugkey, berlaku s.d. 2056) dibuat
  khusus RuangKlon; base64-nya disimpan sebagai secret repo
  `RUANGKLON_KEYSTORE_BASE64`, dipulihkan CI sebelum build, dan sidik
  jari signer dicetak di log build. Master keystore TIDAK ikut commit.
- KONSEKUENSI: v0.2.0 adalah aplikasi baru di mata Android — uninstall
  v0.1.1 dulu, dan clone lama harus ditambah ulang (data clone tidak
  ikut pindah). Setelah v0.2.0, update berikutnya menimpa tanpa uninstall.

## Mode hemat v0.3.0 (per clone, atas perintah user 2026-10-24)

- Cara pakai: tahan lama (long-press) pada clone di daftar → "Aktifkan
  mode hemat". Sekali aktif, clone itu dihentikan otomatis bila tidak
  dibuka selama 30 menit. Menonaktifkan cukup lewat menu yang sama.
- Mekanisme: `HematManager` (SharedPreferences `ruangklon_hemat`)
  mencatat waktu terakhir clone diluncurkan (dari daftar RuangKlon
  maupun dari pintasan Beranda) dan menandai clone yang hemat.
  Setiap kali RuangKlon dibuka (`AppsFragment.onStart`), clone hemat
  yang sudah menganggur >= 30 menit dihentikan lewat
  `BlackBoxCore.stopPackage()` — API yang sama dengan tombol
  "Hentikan" manual. Jumlah yang dihentikan dilaporkan lewat toast.
- Keputusan desain (jujur): hemat TIDAK memakai timer/service latar,
  karena service latar justru menguras baterai yang ingin dihemat —
  evaluasi terjadi tiap RuangKlon dibuka. "Menganggur" dihitung dari
  terakhir diluncurkan, bukan terakhir di-foreground (itu butuh izin
  UsageStats yang sengaja tidak diminta). Clone yang dipasang sebelum
  v0.3.0 tidak langsung dihentikan pada pembukaan pertama: pembukaan
  itu dicatat sebagai baseline dulu.
- Default: NONAKTIF per clone, karena clone yang dihentikan tidak
  menerima notifikasi sampai dibuka lagi — dialog peringatan
  menyampaikan ini sebelum mengaktifkan. Modul Xposed tidak bisa
  ditandai hemat. Data hemat dibersihkan saat clone di-uninstall.

## Font v0.3.1 (Space Grotesk + JetBrains Mono)

- Font dibundel di `res/font/` (bukan unduhan saat jalan): Space Grotesk
  untuk teks utama & judul toolbar (tebal), JetBrains Mono untuk label
  nama clone — sesuai token desain soft neobrutalism.
- Kedua font berlisensi SIL Open Font License 1.1; teks lisensinya ada
  di `fonts-licenses/`. File TTF statis (400/700) dibuat dari font
  variabel resmi Google Fonts dengan fontTools instancer (bukan file
  dari endpoint legacy Google yang ternyata bukan TTF valid).
- v0.3.1 menimpa v0.3.0 (package & keystore sama, tidak perlu uninstall).
