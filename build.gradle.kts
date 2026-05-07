import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.AbstractArchiveTask

val isUnobfuscated = sc.current.parsed >= "26.1"

if (isUnobfuscated) {
    apply(plugin = "net.fabricmc.fabric-loom")
} else {
    apply(plugin = "net.fabricmc.fabric-loom-remap")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

val loomApi: LoomGradleExtensionAPI get() = extensions.getByType<LoomGradleExtensionAPI>()

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    add("minecraft", "com.mojang:minecraft:${sc.current.version}")
    if (!isUnobfuscated) {
        add("mappings", loomApi.officialMojangMappings())
    }

    val fabricApiVersion = property("deps.fabric_api") as String
    if (isUnobfuscated) {
        // 26.1+: 非難読化のため standard implementation を使用
        add("implementation", "net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
        add("implementation", "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    } else {
        add("modImplementation", "net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
        add("modImplementation", "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    }
}

configure<LoomGradleExtensionAPI> {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true")
        runDir = "../../run"
    }
}

configure<JavaPluginExtension> {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks {
    named<Copy>("processResources") {
        val fabricApiModId = if (sc.current.parsed < "1.18") "fabric" else "fabric-api"

        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))
        inputs.property("fabric_api_mod", fabricApiModId)

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep"),
            "fabric_api_mod" to fabricApiModId
        )

        filesMatching("fabric.mod.json") { expand(props) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        val jarFile = if (isUnobfuscated) {
            named<AbstractArchiveTask>("jar").map { it.archiveFile }
        } else {
            named<AbstractArchiveTask>("remapJar").map { it.archiveFile }
        }
        val sourcesFile = if (isUnobfuscated) {
            named<AbstractArchiveTask>("sourcesJar").map { it.archiveFile }
        } else {
            named<AbstractArchiveTask>("remapSourcesJar").map { it.archiveFile }
        }
        from(jarFile, sourcesFile)
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

/*
// Publishes builds to Modrinth and Curseforge with changelog from the CHANGELOG.md file
publishMods {
    file = if (isUnobfuscated) tasks.named<AbstractArchiveTask>("jar").map { it.archiveFile.get() }
           else tasks.named<AbstractArchiveTask>("remapJar").map { it.archiveFile.get() }
    additionalFiles.from(if (isUnobfuscated) tasks.named<AbstractArchiveTask>("sourcesJar").map { it.archiveFile }
                         else tasks.named<AbstractArchiveTask>("remapSourcesJar").map { it.archiveFile })
    displayName = "${property("mod.name")} ${property("mod.version")} for ${property("mod.mc_title")}"
    version = property("mod.version") as String
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    dryRun = providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null
        || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
    }
}
 */
/*
// Publishes builds to a maven repository under `com.example:template:0.1.0+mc`
publishing {
    repositories {
        maven("https://maven.example.com/releases") {
            name = "myMaven"
            credentials(PasswordCredentials::class.java)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${property("mod.id")}"
            artifactId = property("mod.id") as String
            version = project.version

            from(components["java"])
        }
    }
}
 */
