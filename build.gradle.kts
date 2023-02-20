
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
	application

	alias(libs.plugins.kotlin)
	alias(libs.plugins.kotlinx.serialization)
	alias(libs.plugins.shadow)
	alias(libs.plugins.detekt)
	alias(libs.plugins.git.hooks)
	alias(libs.plugins.licenser)
}

group = "org.quiltmc"
version = "1.0-SNAPSHOT"
val javaTarget = 17

repositories {
	mavenCentral()

	maven {
		name = "Sonatype Snapshots (Legacy)"
		url = uri("https://oss.sonatype.org/content/repositories/snapshots")
	}

	maven {
		name = "Sonatype Snapshots"
		url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
	}
}

dependencies {
	detektPlugins(libs.detekt)
	detektPlugins(libs.detekt.libraries)

	implementation(libs.kord.extensions.core)
	implementation(libs.kord.extensions.unsafe)
	implementation(libs.logging)
	implementation(libs.logback)
	implementation(libs.kmongo)
}

application {
	mainClass.set("org.quiltmc.community.ama.AppKt")
}

gitHooks {
	setHooks(
		mapOf("pre-commit" to "updateLicense detekt")
	)
}

kotlin {
	jvmToolchain(javaTarget)
}

tasks {
	afterEvaluate {
		withType<KotlinCompile> {
			compilerOptions {
				jvmTarget.set(JvmTarget.fromTarget(javaTarget.toString()))
				languageVersion.set(
					KotlinVersion.fromVersion(libs.plugins.kotlin.get().version.requiredVersion.substringBeforeLast("."))
				)
				incremental = true
				freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
			}
		}

		jar {
			manifest {
				attributes(
					"Main-Class" to "org.quiltmc.community.ama.AppKt"
				)
			}
		}

		java {
			sourceCompatibility = JavaVersion.toVersion(javaTarget)
			targetCompatibility = JavaVersion.toVersion(javaTarget)
		}

		wrapper {
			distributionType = Wrapper.DistributionType.BIN
		}
	}
}

detekt {
	buildUponDefaultConfig = true
	config = files("$rootDir/detekt.yml")

	autoCorrect = true
}
