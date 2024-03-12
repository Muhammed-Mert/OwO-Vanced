import org.gradle.kotlin.dsl.support.listFilesOrdered

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.binary.compatibility.validator)
    `maven-publish`
    signing
}

group = "app.owovanced"

repositories {
    mavenCentral()
    mavenLocal()
    google()
    maven {
        // A repository must be speficied for some reason. "registry" is a dummy.
        url = uri("https://maven.pkg.github.com/revanced/registry")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation(libs.revanced.patcher)
    implementation(libs.smali)
}

kotlin {
    jvmToolchain(11)
}

tasks {
    withType(Jar::class) {
        manifest {
            attributes["Name"] = "OwOVanced"
            attributes["Description"] = "OwOVanced"
            attributes["Version"] = version
            attributes["Timestamp"] = System.currentTimeMillis().toString()
            attributes["Source"] = "git@github.com:Muhammed-Mert/OwO-Vanced.git"
            attributes["Author"] = "OwOVanced"
            attributes["Contact"] = "oltivi1335@gmail.com"
            attributes["Origin"] = "-"
            attributes["License"] = "GNU General Public License v3.0"
        }
    }

    register("buildDexJar") {
        description = "Build and add a DEX to the JAR file"
        group = "build"

        dependsOn(build)

        doLast {
            val d8 = File(System.getenv("ANDROID_HOME")).resolve("build-tools")
                .listFilesOrdered().last().resolve("d8").absolutePath

            val patchesJar = configurations.archives.get().allArtifacts.files.files.first().absolutePath
            val workingDirectory = layout.buildDirectory.dir("libs").get().asFile

            exec {
                workingDir = workingDirectory
                commandLine = listOf(d8, "--release", patchesJar)
            }

            exec {
                workingDir = workingDirectory
                commandLine = listOf("zip", "-u", patchesJar, "classes.dex")
            }
        }
    }

    // Needed by gradle-semantic-release-plugin.
    // Tracking: https://github.com/KengoTODA/gradle-semantic-release-plugin/issues/435
    publish {
        dependsOn("buildDexJar")
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Muhammed-Mert/OwO-Vanced")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("revanced-patches-publication") {
            from(components["java"])

            pom {
                name = "OwOVanced"
                description = "OwOVanced"
                url = "-"

                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
                    }
                }
                developers {
                    developer {
                        id = "OwOVanced"
                        name = "OwOVanced"
                        email = "oltivi1335@gmail.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/Muhammed-Mert/OwO-Vanced.git"
                    developerConnection = "scm:git:git@github.com:Muhammed-Mert/OwO-Vanced.git"
                    url = "https://github.com/Muhammed-Mert/OwO-Vanced"
                }
            }
        }
    }
}

signing {
    useGpgCmd()

    sign(publishing.publications["revanced-patches-publication"])
}
