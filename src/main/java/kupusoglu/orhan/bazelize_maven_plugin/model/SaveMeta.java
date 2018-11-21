package kupusoglu.orhan.bazelize_maven_plugin.model;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Saves a module's <strong>meta data</strong> to JSON files
 */
public class SaveMeta {
    private Log log;
    private String baseDir;


    public SaveMeta() {
        super();
    }

    public SaveMeta(Log log, String baseDir) {
        this();

        this.log = log;
        this.baseDir = baseDir;
    }

    public void execute() throws MojoExecutionException {
        Path root = Paths.get(baseDir).normalize().toAbsolutePath();
        File metaFile = new File(root + File.separator + Common.OUTPUT_FILES.JSON_META);

        try {
            FindMeta findMeta = new FindMeta(log,
                                             root,
                                             Common.OUTPUT_FILES.JSON_MODULE.toString());

            java.nio.file.Files.walkFileTree(root, findMeta);
            findMeta.metaData();
            findMeta.done();

            log.info("output:\n" + Common.getIndentOne() + metaFile);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
