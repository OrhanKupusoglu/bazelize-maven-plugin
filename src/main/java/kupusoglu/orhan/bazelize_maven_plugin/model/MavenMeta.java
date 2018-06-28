package kupusoglu.orhan.bazelize_maven_plugin.model;

import org.apache.maven.plugin.logging.Log;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Generate meta data
 */
public class MavenMeta implements Comparable<MavenMeta> {
    private Log log;
    private Path rootDir;
    private Path relDir;
    private String groupId;
    private String artifactId;
    private String version;
    private String packaging;
    private String whiteList;
    private String blackList;
    private String[] source;


    public MavenMeta() {
        super();
    }

    public MavenMeta(Log log, Path rootDir, Path relDir, String groupId, String artifactId) {
        this();

        this.log = log;
        this.rootDir = rootDir;
        this.relDir = relDir;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public MavenMeta(Log log, Path rootDir, Path relDir, String groupId, String artifactId, String version) {
        this(log, rootDir, relDir, groupId, artifactId);
        this.version = version;
    }

    public MavenMeta(Log log, Path rootDir, Path relDir, String groupId, String artifactId, String version, String packaging) {
        this(log, rootDir, relDir, groupId, artifactId, version);
        this.packaging = packaging;
    }

    public MavenMeta(Log log, Path rootDir, Path relDir, String groupId, String artifactId, String version, String packaging, String whiteList) {
        this(log, rootDir, relDir, groupId, artifactId, version, packaging);
        this.whiteList = whiteList;
    }

    public MavenMeta(Log log, Path rootDir, Path relDir, String groupId, String artifactId, String version, String packaging, String whiteList, String blackList) {
        this(log, rootDir, relDir, groupId, artifactId, version, packaging, whiteList);
        this.blackList = blackList;
    }

    public Path getRootDir() {
        return this.rootDir;
    }

    public void setRootDir(Path rootDir) {
        this.rootDir = rootDir;
    }

    public Path getRelDir() {
        return this.relDir;
    }

    public void setRelDir(Path relDir) {
        this.relDir = relDir;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return this.artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPackaging() {
        return this.packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String[] getSource() {
        return this.source;
    }

    public void setSource(String[] source) {
        this.source = source;
    }

    public String retrieveName() {
        return Common.sanitize(groupId + Common.getSepSanitize() + artifactId + Common.getSepSanitize() + version);
    }

    private boolean isAnySource() {
        return this.getSource() != null && this.getSource().length > 0;
    }

    public String retrieveArtifact() {
        if (isAnySource()) {
            return artifactId + '-' + version + ".jar";
        } else {
            return "";
        }
    }

    public Path retrieveAbsDir() {
        return Paths.get(this.rootDir.toString(), this.getRelDir().toString()).normalize().toAbsolutePath();
    }

    public java.lang.String retrieveLabel() {
        if (isAnySource()) {
            return "//"
                   + this.getRelDir().toString()
                   + ":"
                   + this.retrieveName();
        } else {
            return "";
        }
    }

    public String recordBazelSources() {
        try {
            SourceMeta sourceMeta = new SourceMeta(this.log,
                                                   retrieveAbsDir(),
                                                   "*.java",
                                                   "pom.xml",
                                                   this.whiteList,
                                                   this.blackList);

            java.nio.file.Files.walkFileTree(retrieveAbsDir(), sourceMeta);

            this.setSource(sourceMeta.getMetaData());

            return sourceMeta.done();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String outputAsMetaData(MavenMeta m) {
        Common.Dependency dep = new Common.Dependency(m.retrieveName(),
                                                      m.retrieveLabel(),
                                                      m.getSource(),
                                                      m.getRelDir().toString(),
                                                      m.retrieveArtifact());
        JSONObject jsonObject = new JSONObject(dep);
        return jsonObject.toString();
    }

    public static String getMetaData(MavenMeta m) {
        JSONObject jsonObject = new JSONObject(m);
        return jsonObject.toString();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof MavenMeta) && (((MavenMeta) o).retrieveName()).equals(this.retrieveName());
    }

    @Override
    public int hashCode() {
        return this.retrieveName().hashCode();
    }

    @Override
    public String toString() {
        return this.retrieveName();
    }

    @Override
    public int compareTo(MavenMeta m) {
        return this.retrieveName().compareTo(m.retrieveName());
    }
}
