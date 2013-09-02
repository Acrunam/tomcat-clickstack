package com.cloudbees.clickstack.plugins

import org.gradle.api.Project
import org.gradle.api.file.CopySpec

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
class ClickstackPluginConvention {
    /**
     * The name of the clickstack.
     */
    String clickstackName

    /**
     * The fully qualified name of the application's main class.
     */
    String mainClassName

    /**
     * Array of string arguments to pass to the JVM when running the application
     */
    Iterable<String> clickstackDefaultJvmArgs = []

    /**
     * <p>The specification of the contents of the distribution.</p>
     * <p>
     * Use this {@link org.gradle.api.file.CopySpec} to include extra files/resource in the application distribution.
     * <pre autoTested=''>
     * apply plugin: 'clickstack'
     *
     * clickstackDistribution.from("some/dir") {
     *   include "*.txt"
     * }
     * </pre>
     * <p>
     * Note that the clickstack plugin pre configures this spec to; include the contents of "{@code src/dist}",
     * copy the clickstack "{@code setup}" scripts into the "{@code .}" directory, and copy the built 'fat' jar
     * into the "{@code .}" directory.
     */
    CopySpec clickstackDistribution

    final Project project

    ClickstackPluginConvention(Project project) {
        this.project = project
        clickstackDistribution = project.copySpec {}
    }
}
