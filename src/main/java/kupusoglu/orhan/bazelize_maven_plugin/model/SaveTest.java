package kupusoglu.orhan.bazelize_maven_plugin.model;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * For each Java test class appends a <strong>Bazel java_test()</strong> rule to the BUILD script
 */
public class SaveTest {
    private Log log;
    private String baseDir;
    private String srcTest;
    private String resTest;


    public SaveTest() {
        super();
    }

    public SaveTest(Log log, String baseDir, String srcTest, String resTest) {
        this();

        this.log = log;
        this.baseDir = baseDir;
        this.srcTest = srcTest;
        this.resTest = resTest;
    }

    public void execute() throws MojoExecutionException {
        Path root = Paths.get(baseDir).normalize().toAbsolutePath();
        File fileBuild = new File(root + File.separator + Common.OUTPUT_FILES.BUILD);

        if (fileBuild.exists()) {
            log.info("root BUILD:\n" + Common.getIndentOne() + fileBuild);

            try {
                CreateTest createTest = new CreateTest(log,
                                                       root,
                                                       srcTest,
                                                       resTest);

                if (createTest.isLibraryFound()) {
                    java.nio.file.Files.walkFileTree(Paths.get(root + File.separator + srcTest).toAbsolutePath(), createTest);
                    createTest.done();
                } else {
                    log.warn("Bazel rule for Java library does not exist\n");
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        } else {
            log.warn("Bazel root BUILD file does not exist\n");
        }
    }
}
