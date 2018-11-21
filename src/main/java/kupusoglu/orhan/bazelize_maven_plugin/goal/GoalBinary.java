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
 * Goal <strong>binary</strong>
 * <br>
 * Adds a <strong>java_binary()</strong> rule to the root BUILD script
 * <br><br>
 * <pre>
 * mvn kupusoglu.orhan:bazelize-maven-plugin:binary -DmainClass=com.mycompany.app.App
 * mvn bazelize:binary -DmainClass=com.mycompany.app.App
 * </pre>
 */
@Mojo(
    name = "binary",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    requiresDependencyResolution = ResolutionScope.TEST
)
public class GoalBinary extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * if true back up the 'BUILD' file
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

    /**
     * if empty use name of the main class as rule's name
     * @parameter
     */
    @Parameter(property = "binName", defaultValue = "")
    private String binName;

    /**
     * name of the main class
     * @parameter
     */
    @Parameter(property = "mainClass", required = true)
    private String mainClass;


    public void execute() throws MojoExecutionException {
        if (project.isExecutionRoot()) {
            String finalSuffix = null;
            String ctxSuffix = (String)project.getContextValue("suffix");

            if (backup) {
                finalSuffix = Common.getBackupSuffix(suffix, ctxSuffix);
            }

            if (mainClass == null || mainClass.isEmpty()) {
                getLog().error("Unknown main class: use -DmainClass=com.mycompany.app.App\n");
                System.exit(1);
            } else {
                if (binName == null || binName.isEmpty()) {
                    binName = mainClass;
                    getLog().warn("binary name is set to main class: -DbinName=" + binName);
                }

                try {
                    Common.generateBinary(getLog(), project.getBasedir().getAbsolutePath(), mainClass, binName, finalSuffix);
                } catch (MojoExecutionException e) {
                    getLog().error(e.getMessage());
                }
            }
        } else {
            getLog().info("skipping");
        }
    }
}
