package com.cloudbees.clickstack.plugins

import com.cloudbees.clickstack.tasks.CreateSetupScripts
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip

class ClickstackPlugin implements Plugin<Project> {

    static final String CLICKSTACK_PLUGIN_NAME = "clickstack"
    static final String CLICKSTACK_GROUP = CLICKSTACK_PLUGIN_NAME

    static final String TASK_RUN_NAME = "run"
    static final String TASK_SETUP_SCRIPTS_NAME = "setupScripts"
    static final String TASK_INSTALL_NAME = "installClickstack"
    static final String TASK_DIST_CLICKSTACK_NAME = "distClickstack"

    private Project project
    private ClickstackPluginConvention pluginConvention

    void apply(final Project project) {
        this.project = project
        project.plugins.apply(org.gradle.api.plugins.JavaPlugin)

        addPluginConvention()
        addRunTask()
        addCreateScriptsTask()

        configureDistSpec(pluginConvention.clickstackDistribution)

        addInstallTask()
        addDistClickstackTask()
    }

    private void addPluginConvention() {
        pluginConvention = new ClickstackPluginConvention(project)
        pluginConvention.clickstackName = project.name
        project.convention.plugins.clickstack = pluginConvention
    }

    private void addRunTask() {
        def run = project.tasks.create(TASK_RUN_NAME, JavaExec)
        run.description = "Runs this clickstack"
        run.group = CLICKSTACK_GROUP
        run.classpath = project.sourceSets.main.runtimeClasspath
        run.conventionMapping.main = { pluginConvention.mainClassName }
        run.conventionMapping.jvmArgs = { pluginConvention.clickstackDefaultJvmArgs }
    }

    // @Todo: refactor this task configuration to extend a copy task and use replace tokens
    private void addCreateScriptsTask() {
        def startScripts = project.tasks.create(TASK_SETUP_SCRIPTS_NAME, CreateSetupScripts)
        startScripts.description = "Creates OS specific scripts to run the clickstack."
        startScripts.classpath = project.tasks[org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME].outputs.files + project.configurations.runtime
        startScripts.conventionMapping.mainClassName = { pluginConvention.mainClassName }
        startScripts.conventionMapping.applicationName = { pluginConvention.clickstackName }
        startScripts.conventionMapping.outputDir = { new File(project.buildDir, 'scripts') }
        startScripts.conventionMapping.defaultJvmOpts = { pluginConvention.clickstackDefaultJvmArgs }
    }

    private void addInstallTask() {
        def installTask = project.tasks.create(TASK_INSTALL_NAME, Sync)
        installTask.description = "Installs the Clickstack."
        installTask.group = CLICKSTACK_GROUP
        installTask.with pluginConvention.clickstackDistribution
        installTask.into { project.file("${project.buildDir}/install/${pluginConvention.clickstackName}") }
        installTask.doFirst {
            if (destinationDir.directory) {
                // TODO CLC directories names
                if (!new File(destinationDir, 'lib').directory || !new File(destinationDir, 'deps').directory) {
                    throw new GradleException("The specified installation directory '${destinationDir}' is neither empty nor does it contain an installation for '${pluginConvention.clickstackName}'.\n" +
                            "If you really want to install to this directory, delete it and run the install task again.\n" +
                            "Alternatively, choose a different installation directory."
                    )
                }
            }
        }
        installTask.doLast {
            project.ant.chmod(file: "${destinationDir.absolutePath}/setup", perm: 'ugo+x')
        }
    }

    private void addDistClickstackTask() {
        def archiveTask = project.tasks.create(TASK_DIST_CLICKSTACK_NAME, Zip)
        archiveTask.description = "Bundles the project as a JVM application with libs and OS specific scripts."
        archiveTask.group = CLICKSTACK_GROUP
        archiveTask.conventionMapping.baseName = { pluginConvention.clickstackName }
        // def baseDir = { archiveTask.archiveName - ".${archiveTask.extension}" }
        def baseDir = ""
        archiveTask.into(baseDir) {
            with(pluginConvention.clickstackDistribution)
        }
    }

    private CopySpec configureDistSpec(CopySpec distSpec) {
        def jar = project.tasks[org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME]
        def startScripts = project.tasks[TASK_SETUP_SCRIPTS_NAME]

        distSpec.with {
            into("dist") {
                from(project.file("src/dist"))
            }

            into("lib") {
                from(jar)
                from(project.configurations.runtime)
            }
            into("") {
                from(startScripts)
                fileMode = 0755
            }
        }

        distSpec
    }
}