plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.20"
    id("app.cash.sqldelight")
}

kotlin {
    jvm()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                
                // SQLDelight
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
                
                // Ktor Client
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
                
                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-java:2.3.7")
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            }
        }
    }
}

sqldelight {
    databases {
        create("ToolboxDatabase") {
            packageName.set("com.toolbox.data.local")
        }
    }
}
