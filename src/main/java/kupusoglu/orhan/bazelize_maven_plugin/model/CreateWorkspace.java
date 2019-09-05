package kupusoglu.orhan.bazelize_maven_plugin.model;

import org.apache.maven.plugin.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;


/**
 * A {@code FileVisitor} to generate the <strong>Bazel WORKSPACE</strong> script
 * <br>
 * @see <a href="https://docs.bazel.build/versions/master/be/workspace.html">Bazel WORKSPACE</a>
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/io/walk.html">Walking the File Tree</a>
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/io/examples/Find.java">Find.java</a>
 */
public class CreateWorkspace extends SimpleFileVisitor<Path> {
    private Log log;
    private Path root;
    private String bzlWorkspaceName;
    private PathMatcher matcherDependency;
    private PathMatcher matcherServer;

    private File workspace;
    private String workspacePrepend;
    private String workspaceAppend;
    private FileWriter workspaceWriter;
    private StringBuilder sbDependency = new StringBuilder();
    private StringBuilder sbServer = new StringBuilder();
    private int numDepMatches = 0;
    private int numSrvMatches = 0;
    private TreeMap<String, MavenDependency> mapDependency = new TreeMap<>();
    private TreeMap<String, MavenServer> mapServer = new TreeMap<>();

    private static final String KEY_DEP_NAME = "name";
    private static final String KEY_GROUP_ID = "groupId";
    private static final String KEY_ARTIFACT_ID = "artifactId";
    private static final String KEY_VERSION = "version";
    private static final String KEY_SCOPE = "scope";
    private static final String KEY_HASH = "hash";
    private static final String KEY_SERVER = "server";
    private static final String KEY_SRV_NAME = "name";
    private static final String KEY_URL = "url";
    private static final String KEY_SETTINGS_FILE = "settingsFile";


    public CreateWorkspace(Log log, Path root, String bzlWorkspaceName, String fileJar, String fileServer) {
        this.log = log;
        this.root = root;
        this.bzlWorkspaceName = bzlWorkspaceName;
        this.matcherDependency = FileSystems.getDefault().getPathMatcher("glob:" + fileJar);
        this.matcherServer = FileSystems.getDefault().getPathMatcher("glob:" + fileServer);

        String pathBase = this.root + File.separator;

        this.workspace = new File(pathBase + Common.OUTPUT_FILES.WORKSPACE);

        try {
            this.workspaceWriter = new FileWriter(workspace);
            this.workspacePrepend = Common.readTextFile(Paths.get(pathBase + Common.INPUT_FILES.WORKSPACE_PREPEND));
            this.workspaceAppend = Common.readTextFile(Paths.get(pathBase + Common.INPUT_FILES.WORKSPACE_APPEND));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void collect(Path file) {
        Path name = file.getFileName();

        if (name != null) {
            if (matcherDependency.matches(name)) {
                numDepMatches++;

                Path absolutePath = file.toAbsolutePath().normalize();

                log.info("found DEPENDENCY: " + absolutePath);

                processDependency(absolutePath);
            } else if (matcherServer.matches(name)) {
                numSrvMatches++;

                Path absolutePath = file.toAbsolutePath().normalize();

                log.info("found SERVER: " + absolutePath);

                processServer(absolutePath);
            }
        }
    }

    public void processDependency(Path absolutePath) {
        String data = Common.readTextFile(absolutePath);

        JSONArray jsonArray = new JSONArray(data);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);

            String name = item.getString(KEY_DEP_NAME);
            String version = item.getString(KEY_VERSION);

            if (!mapDependency.containsKey(name + ":" + version)) {
                MavenDependency dep = new MavenDependency(item.getString(KEY_GROUP_ID),
                                                          item.getString(KEY_ARTIFACT_ID),
                                                          version,
                                                          item.isNull(KEY_SCOPE) ? "" : item.getString(KEY_SCOPE),
                                                          item.isNull(KEY_HASH) ? "" : item.getString(KEY_HASH),
                                                          item.isNull(KEY_SERVER) ? "" : item.getString(KEY_SERVER));

                mapDependency.put(name, dep);
            }
        }
    }

    public void processServer(Path absolutePath) {
        String data = Common.readTextFile(absolutePath);

        JSONArray jsonArray = new JSONArray(data);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);

            String name = item.getString(KEY_SRV_NAME);

            if (!mapServer.containsKey(name)) {
                MavenServer server = new MavenServer(name,
                                                     item.getString(KEY_URL),
                                                     item.getString(KEY_SETTINGS_FILE));

                mapServer.put(name, server);
            }
        }
    }

    public void workspace() {
        Set<Map.Entry<String, MavenDependency>> setDependency = mapDependency.entrySet();
        Iterator<Map.Entry<String, MavenDependency>> iteratorDependency = setDependency.iterator();

        // maven rules boilerplate
        Optional<String> template = Common.readResourceFile("rules_jvm_external_boilerplate");
        if(!template.isPresent()) throw new IllegalStateException("Cannot find template: " + "rules_jvm_external_boilerplate");
        sbDependency.append(template.get());

        // fetch all maven dependencies/artifacts
        sbDependency.append(
                "maven_install(\n" +
                Common.getIndentOne() +"artifacts = [\n"
        );
        // define artifacts
        while (iteratorDependency.hasNext()) {
            Map.Entry<String, MavenDependency> me = iteratorDependency.next();
            MavenDependency srv = me.getValue();

            sbDependency.append(
                    Common.getIndentTwo() + srv.getMavenDependencyString(true, true) + ",\n"
            );
        }
        // define repositories
        sbDependency.append(
                Common.getIndentOne() + "],\n" +
                Common.getIndentOne() + "repositories = [\n"
        );

        for (MavenServer server : mapServer.values()) {
            sbDependency.append(
                    Common.getIndentTwo() + "\"" + server.getUrl() + "\",\n"
            );
        }

        sbDependency.append(
                Common.getIndentOne() + "],\n" +
                ")\n"
        );

        try {
            if (bzlWorkspaceName != null && !bzlWorkspaceName.isEmpty()) {
                workspaceWriter.append("workspace(name = \"");
                workspaceWriter.append(bzlWorkspaceName);
                workspaceWriter.append("\")");
                workspaceWriter.append("\n");
                workspaceWriter.append("\n");
            }

            if (!workspacePrepend.isEmpty()) {
                workspaceWriter.append(workspacePrepend);
                workspaceWriter.append("\n");
            }

            if (sbServer.length() > 0) {
                workspaceWriter.append(sbServer);
                workspaceWriter.append("\n");
            }

            workspaceWriter.append(sbDependency);

            if (!workspaceAppend.isEmpty()) {
                workspaceWriter.append("\n");
                workspaceWriter.append(workspaceAppend);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        try {
            workspaceWriter.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void done() {
        log.info("completed\n\tmatched: " + numDepMatches
                                          + " JAR files\n\tmatched: "
                                          + numSrvMatches
                                          + " Server files");
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
