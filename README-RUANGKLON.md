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

## Batasan v0.1 yang diketahui

- Build debug pertama memakai debug keystore bawaan runner (belum di-pin).
  Keystore tetap (seperti pola RDPfread XD v0.3.1) dipasang sebelum rilis
  yang ditujukan untuk update menimpa.
- WhatsApp/Telegram terbaru bisa menolak berjalan di virtual env
  (deteksi virtualisasi / flag REQUIRE_SECURE_ENV). Ini tolok ukur go/no-go
  uji lapangan, bukan janji.
