import com.beust.kobalt.*
import com.beust.kobalt.api.Project
import com.beust.kobalt.plugin.application.*
import com.beust.kobalt.plugin.kotlin.*
import com.beust.kobalt.plugin.packaging.*
import com.beust.kobalt.plugin.publish.*
import com.beust.kobalt.maven.Developer as KobaltDeveloper
import org.apache.maven.model.Developer as PomDeveloper
import org.apache.maven.model.License as PomLicense
import org.apache.maven.model.Model as PomModel
import org.apache.maven.model.Scm as PomScm

val kotlin_version = "1.1.1"

val p = project {
    name = "getoptk"
    group = "com.empowerops"
    artifactId = name
    version = "0.2"

    pom = createPom()
    
    dependencies {
        compile(
                "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version",
                "com.google.guava:guava:19.0"
        )
    }

    dependenciesTest {
        compile("junit:junit:4.11", "org.assertj:assertj-core:3.5.2")
    }

    assemble {
        jar {}
    }

    bintray {
        publish = true
    }
}

fun Project.createPom() = PomModel().also { pom ->

    pom.name = name
    pom.description = description
    pom.url = url

    pom.licenses = listOf(PomLicense().apply {
        name = "Apache-2.0"
        url = "http://www.apache.org/licenses/LICENSE-2.0"
    })
    pom.scm = PomScm().apply {
        url = "https://github.com/EmpowerOperations/getoptk/"
        connection = "https://github.com/EmpowerOperations/getoptk.git"
        developerConnection = "git@github.com:EmpowerOperations/getoptk.git"
    }
    pom.developers = listOf(PomDeveloper().apply {
        name = """Geoff "Groostav" Groos"""
        email = "geoff.groos@empowerops.com"
    })
}