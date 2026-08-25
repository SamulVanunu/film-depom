# Samuelnyzor CloudStream Plugins

Bu proje, CloudStream için özel plugin'ler içerir:
- **AsyaWatch** - Anime izleme sitesi
- **HDFilmCehennemi** - Film izleme sitesi
- **OpenAnime** - Anime izleme sitesi

## Geliştirici
**Samuelnyzor**

## Gereksinimler
- Java 17 veya üzeri
- Android SDK 34
- Git

## Kurulum

### 1. GitHub Reposunu Fork Edin
Bu repoyu kendi GitHub hesabınıza fork edin.

### 2. GitHub Actions Ayarlarını Yapın
- Settings → Actions → General → "Allow all actions and reusable workflows"
- Settings → Actions → General → "Read and write permissions"

### 3. build.gradle.kts Dosyasını Düzenleyin
Her plugin klasöründeki `build.gradle.kts` dosyasını açın:
- `name.set("PluginName")` kısmını değiştirin
- `description.set("Açıklama")` kısmını değiştirin
- `authors.set(setOf("Samuelnyzor"))` zaten ayarlı

### 4. Provider Sınıflarını Düzenleyin
Her provider dosyasında (`*Provider.kt`):
- `mainUrl` değerini kendi sitenizin URL'si olarak değiştirin
- CSS selector'ları sitenizin yapısına göre güncelleyin
- Hata ayıklama için test edin

### 5. Plugin'leri Derleyin
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

### 6. Test İçin Telefona Kurun
```bash
# Linux/Mac
./gradlew AsyaWatchPlugin:deployWithAdb

# Windows
.\gradlew.bat AsyaWatchPlugin:deployWithAdb
```

### 7. Dosya Erişim İznini Verin
Android 11 ve üzeri cihazlarda:
```bash
adb shell appops set --uid com.lagradost.cloudstream3.prerelease MANAGE_EXTERNAL_STORAGE allow
```

## Eklentileri CloudStream'e Yükleme

### Yöntem 1: Doğrudan Yükleme
1. CloudStream'i açın
2. Settings → Extensions → Add repository
3. Repository URL'nizi girin (veya shortcode)
4. Plugin'leri yükleyin

### Yöntem 2: Megarepo ile
Settings → Extensions → Add repository → "megarepo" yazın

## Dağıtım

### GitHub Releases
1. GitHub'da yeni bir release oluşturun
2. Build edilmiş `.aar` dosyasını yükleyin
3. `index.json` dosyasını güncelleyin

### Repository Oluşturma
`index.json` dosyasını güncelleyerek kendi repository'nizi oluşturun.

## Plugin Özellikleri

### AsyaWatch
- Anime listeleme
- Anime arama
- Bölüm listeleme
- Video kaynaklarını bulma
- Altyazı desteği

### HDFilmCehennemi
- Film ve dizi listeleme
- Film/dizi arama
- Bölüm listeleme (diziler için)
- Video kaynaklarını bulma
- Altyazı desteği

### OpenAnime
- Anime listeleme
- Anime arama
- Bölüm listeleme
- Video kaynaklarını bulma
- Altyazı desteği

## Sorun Giderme

### "No links found" Hatası
- Site yapısı değişmiş olabilir
- CSS selector'ları kontrol edin
- Siteyi tarayıcıda açarak test edin

### Build Hataları
- `gradle.properties` dosyasını kontrol edin
- Android SDK versiyonunu kontrol edin
- Internet bağlantısını kontrol edin

## Faydalı Kaynaklar
- [CloudStream Docs](https://recloudstream.github.io/csdocs/)
- [CloudStream Wiki](https://cloudstream.miraheze.org/wiki/Extension)
- [Discord](https://discord.gg/aKH7B9KvrN)

## Lisans
Bu proje公众 domain olarak yayınlanmaktadır.

---
**Samuelnyzor** tarafından yapılmıştır 🎬
