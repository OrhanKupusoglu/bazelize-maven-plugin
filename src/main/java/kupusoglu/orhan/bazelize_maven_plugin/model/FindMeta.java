package kupusoglu.orhan.bazelize_maven_plugin.model;

import org.apache.maven.plugin.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * A {@code FileVisitor} that finds all the meta data of a module
 * <br>
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/io/walk.html">Walking the File Tree</a>
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/io/examples/Find.java">Find.java</a>
 */
public class FindMeta extends SimpleFileVisitor<Path> {
    private Log log;
    private Path root;
    private PathMatcher matcherMeta;
    private File fileMeta;
    private FileWriter metaWriter;

    private StringBuilder sbMeta = new StringBuilder();
    private int numMetaMatches = 0;
    private SortedSet<MavenMeta> setMeta = new TreeSet<>();

    private static final String KEY_ROOT_DIR = "rootDir";
    private static final String KEY_REL_DIR = "relDir";
    private static final String KEY_GROUP_ID = "groupId";
    private static final String KEY_ARTIFACT_ID = "artifactId";
    private static final String KEY_VERSION = "version";
    private static final String KEY_PACKAGING = "packaging";
    private static final String KEY_SOURCE = "source";


    public FindMeta(Log log, Path root, String patternMeta) {
        this.log = log;
        this.root = root;
        this.matcherMeta = FileSystems.getDefault().getPathMatcher("glob:" + patternMeta);
        this.fileMeta = new File(this.root + File.separator + Common.OUTPUT_FILES.JSON_META);

        try {
            this.metaWriter = new FileWriter(fileMeta);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void collect(Path file) {
        Path name = file.getFileName();

        if (name != null) {
            if (matcherMeta.matches(name)) {
                numMetaMatches++;

                Path absolutePath = file.toAbsolutePath().normalize();

                log.info("found meta data: " + absolutePath);

                processMetaData(absolutePath);
            }
        }
    }

    public void processMetaData(Path absolutePath) {
        String data = Common.readTextFile(absolutePath);

        JSONObject item = new JSONObject(data);

        MavenMeta meta = new MavenMeta(this.log,
                                       Paths.get(item.getString(KEY_ROOT_DIR)),
                                       Paths.get(item.getString(KEY_REL_DIR)),
                                       item.getString(KEY_GROUP_ID),
                                       item.getString(KEY_ARTIFACT_ID),
                                       item.getString(KEY_VERSION),
                                       item.getString(KEY_PACKAGING));

        JSONArray arr = item.getJSONArray(KEY_SOURCE);
        List<String> source = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++){
            source.add(arr.getString(i));
        }

        String[] sourceArray = new String[source.size()];
        sourceArray = source.toArray(sourceArray);

        meta.setSource(sourceArray);

        setMeta.add(meta);
    }

    public void metaData() {
        try {
            sbMeta.append("[");

            for (MavenMeta meta : setMeta) {
                sbMeta.append(MavenMeta.outputAsMetaData(meta));
                sbMeta.append(",");
            }

            if (sbMeta.length() > 1) {
                sbMeta.setLength(sbMeta.length() - 1);
            }

            sbMeta.append("]");

            metaWriter.append(sbMeta.toString());
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        try {
            metaWriter.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void done() {
        log.info("completed\n\tmatched: " + numMetaMatches
                                             + " meta files matched");
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (attrs.isRegularFile()) {
            collect(file);
        } else {
            log.warn("ignored - not a regular file: " + file);
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) {
        log.error(e.getMessage());
        return FileVisitResult.CONTINUE;
    }
}
