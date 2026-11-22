==================================================
XABARCHI — iOS "LIQUID GLASS" MESSENJER (GITHUB PAKETI)
==================================================
Ushbu arxiv Telegram for Android (https://github.com/DrKLO/Telegram)
ochiq kodli loyihasi uchun tayyor Xabarchi (com.xabarchi.app) paketi hisoblanadi.
Litsenziya: GNU GPL v2.

QANDAY ISHLATISH KERAK (Kompyutersiz, faqat GitHub saytida):
1. https://my.telegram.org saytidan o'zingizning api_id va api_hash kalitlaringizni oling.
2. https://github.com/DrKLO/Telegram sahifasini oching va "Fork" tugmasini bosing
   (nomini Xabarchi deb qo'ying va Public holatda qoldiring).
3. O'zingizning Xabarchi repozitoriyangizda:
   Settings -> Secrets and variables -> Actions -> New repository secret:
   - TELEGRAM_API_ID   (faqat raqamlar)
   - TELEGRAM_API_HASH (harf va raqamlar)
4. Ushbu ZIP faylni arxivdan chiqaring (extract qiling).
5. GitHub repozitoriyangiz bosh sahifasida "Add file" -> "Upload files" ni bosing
   va arxiv ichidagi BARCHA papka va fayllarni (.github, scripts, TMessagesProj,
   settings.gradle, README.txt) birdaniga yuklab, "Commit changes" ni bosing.
   Eslatma: Mavjud Telegram fayllarini qo'lda tahrirlashingiz SHART EMAS —
   scripts/apply_xabarchi_patches.py skripti barchasini bulutda avtomatik bajaradi!
6. Repozitoriyaning "Actions" bo'limiga o'ting -> "Build Xabarchi APK (arm64-v8a)" ->
   "Run workflow" tugmasini bosing.
7. 25-45 daqiqadan so'ng yashil belgi chiqadi. Pastdagi "Artifacts" bo'limidan
   "Xabarchi-arm64-debug-apk" faylini yuklab oling, ZIPdan chiqaring va telefonga o'rnating!
