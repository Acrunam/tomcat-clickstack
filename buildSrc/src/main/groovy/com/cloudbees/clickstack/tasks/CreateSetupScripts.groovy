package com.cloudbees.clickstack.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GUtil

/**
 * <p>A {@link org.gradle.api.Task} for creating OS dependent setup scripts.</p>
 *
 * @author Rene Groeschke
 */
public class CreateSetupScripts extends ConventionTask {

    /**
     * The directory to write the scripts into.
     */
    File outputDir

    /**
     * The application's main class.
     */
    @Input
    String mainClassName

    /**
     * The application's default JVM options.
     */
    @Input
    @Optional
    Iterable<String> defaultJvmOpts = []

    /**
     * The application's name.
     */
    @Input
    String applicationName

    String scriptName = "setup"

    String optsEnvironmentVar

    String exitEnvironmentVar

    /**
     * The class path for the application.
     */
    @InputFiles
    FileCollection classpath

    /**
     * Returns the name of the application's OPTS environment variable.
     */
    @Input
    String getOptsEnvironmentVar() {
        if (optsEnvironmentVar) {
            return optsEnvironmentVar
        }
        if (!getApplicationName()) {
            return null
        }
        return "${GUtil.toConstant(getApplicationName())}_OPTS"
    }

    @Input
    String getExitEnvironmentVar() {
        if (exitEnvironmentVar) {
            return exitEnvironmentVar
        }
        if (!getApplicationName()) {
            return null
        }
        return "${GUtil.toConstant(getApplicationName())}_EXIT_CONSOLE"
    }

    @OutputFile
    File getUnixScript() {
        // return new File(getOutputDir(), getApplicationName())
        return new File(getOutputDir(), getScriptName())

    }

    @OutputFile
    File getWindowsScript() {
        //return new File(getOutputDir(), "${getApplicationName()}.bat")
        return new File(getOutputDir(), "${getScriptName()}.bat")
    }

    @TaskAction
    void generate() {
        def generator = new SetupScriptGenerator()
        generator.applicationName = getApplicationName()
        generator.mainClassName = getMainClassName()
        generator.defaultJvmOpts = getDefaultJvmOpts()
        generator.optsEnvironmentVar = getOptsEnvironmentVar()
        generator.exitEnvironmentVar = getExitEnvironmentVar()
        generator.classpath = getClasspath().collect { "lib/${it.name}" }
        // generator.scriptRelPath = "bin/${getUnixScript().name}"
        generator.scriptRelPath = "${getUnixScript().name}"
        generator.generateUnixScript(getUnixScript())
        generator.generateWindowsScript(getWindowsScript())
    }
}
