package kupusoglu.orhan.bazelize_maven_plugin.goal;

import kupusoglu.orhan.bazelize_maven_plugin.model.Common;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.time.LocalDateTime;


/**
 * Goal <strong>meta</strong>
 * <br>
 * Order: 2
 * <br>
 * Processes the current module's meta data, and consolidates data into a single JSON
 * <br><br>
 * <pre>
 * mvn kupusoglu.orhan:bazelize-maven-plugin:meta
 * mvn bazelize:meta
 * </pre>
 */
@Mojo(
    name = "meta",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    requiresDependencyResolution = ResolutionScope.TEST
)
public class GoalMeta extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * if true back up the 'tmp-bzl-meta.json' files
     * @parameter
     */
    @Parameter(property = "backup", defaultValue = "false")
    private Boolean backup;

    /**
     * if empty set current timestamp as suffix
     * @parameter
     */
    @Parameter(property = "suffix", defaultValue = "")
    private String suffix;


    public void execute() throws MojoExecutionException {
        if (project.isExecutionRoot()) {
            String finalSuffix = null;

            if (backup) {
                LocalDateTime localDateTime = LocalDateTime.now();

                if (suffix == null || suffix.isEmpty()) {
                    finalSuffix = Common.getFormattedTimestamp(localDateTime);
                } else {
                    finalSuffix = suffix;
                }
            }

            try {
                Common.generateMetaFile(getLog(), project.getBasedir().getAbsolutePath(), finalSuffix);
            } catch (MojoExecutionException e) {
                getLog().error(e.getMessage());
            }
        } else {
            getLog().info("skipping");
        }
    }
}
