plugins {
    java
    id("com.github.johnrengelman.shadow").version("6.0.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("com.github.ben-manes.caffeine", "caffeine", "2.8.5") {
        exclude(module = "error_prone_annotations")
        exclude(module = "checker-qual")
    }
    implementation("net.kyori", "adventure-platform-bukkit", "4.0.0-SNAPSHOT") {
        exclude(module = "checker-qual")
    }
    implementation("org.spongepowered", "configurate-hocon", "3.7.1") {
        exclude(module = "checker-qual")
        exclude(module = "guava")
        exclude(module = "guice")
    }
    implementation("org.jdbi", "jdbi3-core", "3.14.4") {
        exclude(module = "caffeine")
        exclude(module = "slf4j-api")
    }
    implementation("com.zaxxer", "HikariCP", "3.4.5") {
        exclude(module = "slf4j-api")
    }
    implementation("org.postgresql", "postgresql", "42.2.16.jre7")
    implementation("com.h2database", "h2", "1.4.200")
    implementation("org.bstats", "bstats-bukkit-lite", "1.7")
    implementation("co.aikar", "taskchain-bukkit", "3.7.2")
    implementation("co.aikar","acf-paper", "0.5.0-SNAPSHOT")
    implementation("org.apache.commons", "commons-math3", "3.6.1")
    implementation("net.jodah", "expiringmap", "0.5.9")
    compileOnly("com.destroystokyo.paper", "paper-api", "1.16.3-R0.1-SNAPSHOT")
    compileOnly("com.github.TechFortress", "GriefPrevention", "16.7.1")
    compileOnly("com.github.TownyAdvanced", "Towny", "0.96.2.0")
    compileOnly("com.sk89q.worldedit", "worldedit-core", "7.0.0-SNAPSHOT")
    compileOnly("com.sk89q.worldedit", "worldedit-bukkit", "7.0.0-SNAPSHOT")
    compileOnly("com.sk89q.worldguard", "worldguard-core", "7.0.0-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set(rootProject.name)
        dependencies {
            relocate("org.bstats", "me.moros.bending.bstats")
            relocate("com.zaxxer", "me.moros.bending.internal.hikari")
            relocate("net.kyori", "me.moros.bending.internal.kyori")
            relocate("ninja.leaping", "me.moros.bending.internal.configurate")
            relocate("com.typesafe", "me.moros.bending.internal.typesafe")
            relocate("co.aikar.taskchain", "me.moros.bending.internal.taskchain")
            relocate("co.aikar.commands", "me.moros.bending.internal.acf")
            relocate("co.aikar.locales", "me.moros.bending.internal.locales")
            relocate("org.jdbi", "me.moros.bending.internal.jdbi")
            relocate("org.postgresql", "me.moros.bending.internal.h2")
            relocate("org.h2", "me.moros.bending.internal.postgresql")
            relocate("io.leangen", "me.moros.bending.internal.jdbi-leangen")
            relocate("org.antlr", "me.moros.bending.internal.jdbi-antlr")
            relocate("com.github.benmanes", "me.moros.bending.internal.caffeine")
            relocate("org.apache.commons.math3", "me.moros.bending.internal.apachemath")
            relocate("net.jodah", "me.moros.bending.internal.expiringmap")
        }
        minimize {
            exclude(dependency("com.github.ben-manes.caffeine:.*:.*"))
            exclude(dependency("org.postgresql:.*:.*"))
            exclude(dependency("com.h2database:.*:.*"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
        options.isFork = true
        options.forkOptions.executable = "javac"
    }
    named<Copy>("processResources") {
        filesMatching("plugin.yml") {
            expand("pluginVersion" to project.version)
        }
    }
}
