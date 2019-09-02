package kupusoglu.orhan.bazelize_maven_plugin.model;

import java.util.Optional;


/**
 * Generate dependency data
 * <br>
 * This class is intented for use in ordered sets, therefore it implements the "Comparable" interface.
 * <br>
 * The order depends on the "name" field.
 * <br>
 * @see <a href="https://docs.bazel.build/versions/master/be/workspace.html#maven_jar">Bazel maven_jar</a>
 * <br>
 * A Bazel CreateWorkspace file downloads Maven JARs with "maven_jar()" function calls.
 * <br>
 * A sample is given below:
 * <br><br>
 * <pre>
 * maven_jar(
 *     name = "aopalliance_aopalliance",
 *     artifact = "aopalliance:aopalliance:1.0",
 *     server = "internal",
 *     sha1 = "0235ba8b489512805ac13a8f9ea77a1ca5ebe3e8",
 * )
 * </pre>
 */
public class MavenDependency implements Comparable<MavenDependency> {
    private String name;
    private String groupId;
    private String artifactId;
    private String version;
    private Optional<String> scope;
    private Optional<String> server;
    private Optional<String> hash;

    private static final String BZL_MAVEN_JAR = "maven_jar";
    private static final String BZL_NAME = "name";
    private static final String BZL_ARTIFACT = "artifact";
    private static final String BZL_SERVER = "server";
    private static final String BZL_SHA1 = "sha1";
    private static final String BZL_CENTRAL = "central";


    public MavenDependency() {
        super();
    }

    public MavenDependency(String groupId, String artifactId, String version) {
        this();
        this.name = Common.sanitize(groupId + Common.getSepSanitize() + artifactId + Common.getSepSanitize() + version);
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public MavenDependency(String groupId, String artifactId, String version, String scope) {
        this(groupId, artifactId, version);
        this.scope = Optional.of(scope);
    }

    public MavenDependency(String groupId, String artifactId, String version, String scope, String hash) {
        this(groupId, artifactId, version, scope);
        this.hash = Optional.of(hash);
    }

    public MavenDependency(String groupId, String artifactId, String version, String scope, String hash, String server) {
        this(groupId, artifactId, version, scope, hash);
        this.server = Optional.of(server);
    }

    public String getName() {
        return Optional.of(this.name).orElse("");
    }

    public void setName(String name) {;
        this.name = name;
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

    public String getScope() {
        return this.scope.orElse("");
    }

    public void setScope(String scope) {
        this.scope = Optional.of(scope);
    }

    public String getServer() {
        return this.server.orElse("");
    }

    public void setServer(String server) {
        this.server = Optional.of(server);
    }

    public String getHash() {
        return this.hash.orElse("");
    }

    public void setHash(String hash) {
        this.hash = Optional.of(hash);
    }

    public String getArtifact() {
        return this.groupId + ":" + this.artifactId + ":" + this.version;
    }

    public String getMavenDependencyString(boolean includeVersion, boolean includeQuotationMarks) {
        StringBuilder sb = new StringBuilder();

        if(includeQuotationMarks) sb.append('"');

        sb.append(this.groupId)
                .append(':')
                .append(this.artifactId);

        if(includeVersion) sb.append(":").append(this.version);

        if(includeQuotationMarks) sb.append('"');

        return sb.toString();
    }

    public String outputAsBazelJar() {
        StringBuilder sb = new StringBuilder();

        sb.append(BZL_MAVEN_JAR)
          .append("(\n")
          .append(Common.getIndentOne())
          .append(BZL_NAME)
          .append(" = ")
          .append("\"")
          .append(this.name)
          .append("\"")
          .append(",\n")
          .append(Common.getIndentOne())
          .append(BZL_ARTIFACT)
          .append(" = ")
          .append("\"")
          .append(this.groupId)
          .append(":")
          .append(this.artifactId)
          .append(":")
          .append(this.version)
          .append("\"")
          .append(",\n");

        if (this.server != null && !this.server.orElse("").isEmpty()) {
            if (!this.server.get().equals(BZL_CENTRAL)) {
                sb.append(Common.getIndentOne())
                        .append(BZL_SERVER)
                        .append(" = ")
                        .append("\"")
                        .append(this.server.get())
                        .append("\"")
                        .append(",\n");
            }
        }

        if (this.hash != null && !this.hash.orElse("").isEmpty()) {
            sb.append(Common.getIndentOne())
              .append(BZL_SHA1)
              .append(" = ")
              .append("\"")
              .append(this.hash)
              .append("\"")
              .append(",\n");
        }

        return sb.append(")\n")
                 .toString();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof MavenDependency) && (((MavenDependency)o).getName()).equals(this.getName());
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return this.name + '-' + this.getArtifact();
    }

    @Override
    public int compareTo(MavenDependency m) {
        return this.name.compareTo(m.getName());
    }
}
