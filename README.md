# 🎬 Film Depom - CloudStream Plugins

Bu proje, CloudStream için özel plugin'ler içerir:
- **AsyaWatch** - Anime izleme sitesi
- **HDFilmCehennemi** - Film izleme sitesi
- **OpenAnime** - Anime izleme sitesi

## 👨‍💻 Geliştirici
**SamuelVanunu**

## 📦 Repository
**film-depom**

## 🔗 Raw URL
```
https://raw.githubusercontent.com/SamuelVanunu/film-depom/main/index.json
```

## 📋 Gereksinimler
- Java 17 veya üzeri
- Android SDK 34
- Git

## 🚀 Kurulum

### 1. Repository'yi CloudStream'e Ekleme
1. CloudStream'i açın
2. **Settings** → **Extensions** → **Add repository**
3. Şu URL'yi yapıştırın:
   ```
   https://raw.githubusercontent.com/SamuelVanunu/film-depom/main/index.json
   ```
4. **Download** butonuna tıklayın
5. Plugin'leri yükleyin

### 2. Plugin'leri Doğrudan Yükleme (Alternatif)
1. GitHub repo sayfasında **Releases** sekmesine gidin
2. En son release'e tıklayın
3. `.aar` dosyalarını indirin
4. CloudStream'de **Settings** → **Extensions** → **Install from file**
5. İndirdiğiniz `.aar` dosyasını seçin

## 🔧 Geliştirme

### Plugin'leri Derleyin
```bash
# Linux/Mac
./gradlew AsyaWatchPlugin:make
./gradlew HDFilmCehennemiPlugin:make
./gradlew OpenAnimePlugin:make

# Windows
.\gradlew.bat AsyaWatchPlugin:make
.\gradlew.bat HDFilmCehennemiPlugin:make
.\gradlew.bat OpenAnimePlugin:make
```

### Test İçin Telefona Kurun
```bash
# Linux/Mac
./gradlew AsyaWatchPlugin:deployWithAdb

# Windows
.\gradlew.bat AsyaWatchPlugin:deployWithAdb
```

### Dosya Erişim İznini Verin (Android 11+)
```bash
adb shell appops set --uid com.lagradost.cloudstream3.prerelease MANAGE_EXTERNAL_STORAGE allow
```

## 🎯 Plugin Özellikleri

### AsyaWatch
- ✅ Anime listeleme
- ✅ Anime arama
- ✅ Bölüm listeleme
- ✅ Video kaynaklarını bulma
- ✅ Altyazı desteği

### HDFilmCehennemi
- ✅ Film ve dizi listeleme
- ✅ Film/dizi arama
- ✅ Bölüm listeleme (diziler için)
- ✅ Video kaynaklarını bulma
- ✅ Altyazı desteği

### OpenAnime
- ✅ Anime listeleme
- ✅ Anime arama
- ✅ Bölüm listeleme
- ✅ Video kaynaklarını bulma
- ✅ Altyazı desteği

## ❓ Sorun Giderme

### "No links found" Hatası
- Site yapısı değişmiş olabilir
- CSS selector'ları kontrol edin
- Siteyi tarayıcıda açarak test edin

### "Repository not found" Hatası
- URL'nin doğru olduğundan emin olun:
  ```
  https://raw.githubusercontent.com/SamuelVanunu/film-depom/main/index.json
  ```
- `film-depom` repository'sinin **Public** olduğundan emin olun

### Build Hataları
- `gradle.properties` dosyasını kontrol edin
- Android SDK versiyonunu kontrol edin
- Internet bağlantısını kontrol edin

## 🔗 Faydalı Kaynaklar
- [CloudStream Docs](https://recloudstream.github.io/csdocs/)
- [CloudStream Wiki](https://cloudstream.miraheze.org/wiki/Extension)
- [Discord](https://discord.gg/aKH7B9KvrN)

## 📄 Lisans
Bu proje公众 domain olarak yayınlanmaktadır.

---
**SamuelVanunu** tarafından yapılmıştır 🎬
