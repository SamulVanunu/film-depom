cloudstreamPlugin {
    // Plugin name displayed in CloudStream
    name.set("HDFilmCehennemi")
    // Plugin description
    description.set("HDFilmCehennemi - Türkçe film izleme sitesi")
    // Plugin authors
    authors.set(setOf("SamuelVANUNU"))
    // Plugin version (increment when making changes)
    version.set(1)
    // Main class path
    entryClass.set("com.HDFilmCehennemiPlugin.HDFilmCehennemiProvider")
    // Plugin icon (optional, place icon.png in src/main/res)
    // iconUrl.set("https://example.com/icon.png")
    // Classpath for the plugin
    classpath()
}

android {
    namespace = "com.hdfilmcehennemi.plugin"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }
}

dependencies {
    implementation("com.lagradost.cloudstream3:cloudstream3-core:3.1.8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
}
