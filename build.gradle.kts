import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm") version "2.4.21"
}

base {
	archivesName.set("marguhl-hideandseek")
}

repositories {
	maven("https://maven.wispforest.io/releases/")
	maven("https://jitpack.io")
	maven("https://api.modrinth.com/maven")
	maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register("hideandseek") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}

	runs {
		named("client") {
			vmArgs("-XX:+IgnoreUnrecognizedVMOptions", "-XX:+AllowEnhancedClassRedefinition")

			vmArgs("-Ddevauth.enabled=true")

			vmArgs("-Ddevauth.configDir=devauth")
		}

		create("clientAlt") {
			client()
			configName = "Minecraft Client (Alt)"

			runDir = "run-alt"

			vmArgs("-XX:+IgnoreUnrecognizedVMOptions", "-XX:+AllowEnhancedClassRedefinition")
			vmArgs("-Ddevauth.enabled=true")

			vmArgs("-Ddevauth.configDir=../run/devauth")

			vmArgs("-Ddevauth.account=alt")
		}
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

    implementation("maven.modrinth:geckolib:${providers.gradleProperty("geckolib_version").get()}")

    implementation("io.wispforest:owo-lib:${providers.gradleProperty("owo_version").get()}")

    include("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

    include("maven.modrinth:geckolib:${providers.gradleProperty("geckolib_version").get()}")

    include("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")

    include("io.wispforest:owo-lib:${providers.gradleProperty("owo_version").get()}")

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25

	options.encoding = "UTF-8"
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_25
	}
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	repositories {
	}
}
