package kupusoglu.orhan.bazelize_maven_plugin.model;

import java.util.Optional;


/**
 * A Bazel Workspace script downloads Maven JARs with <strong>maven_jar()"</strong> function calls from Maven JAR archive servers
 * <br>
 * @see <a href="https://docs.bazel.build/versions/master/be/workspace.html#maven_server">Bazel maven_server</a>
 * <br>
 * The default settings file is: "~/.m2/settings.xml"
 * <br>
 * A sample is given below:
 * <br><br>
 * <pre>
 * maven_server(
 *     name = "internal",
 *     url = "http://192.168.20.202:8090/repository/internal",
 *     settings_file = "/home/orhanku/.m2/settings.xml",
 * )
 * </pre>
 */
public class MavenServer implements Comparable<MavenServer> {
    private String name;
    private String url;
    private String settings_file;


    public MavenServer() {
        super();
    }

    public MavenServer(String name, String url) {
        this();
        this.name = name;
        this.url = url;
    }

    public MavenServer(String name, String url, String settings_file) {
        this(name, url);
        this.settings_file = settings_file;
    }

    public String getName() {
        return Optional.of(this.name).orElse("");
    }

    public String setName(String name) {
        return this.name = name;
    }

    public String getUrl() {
        return this.url;
    }

    public String setUrl(String url) {
        return this.url = url;
    }

    public String getSettingsFile() {
        return this.settings_file;
    }

    public String setSettingsFile(String settings_file) {
        return this.settings_file = settings_file;
    }

    public String outputAsBazelServer() {
        String contentServer = Common.getTemplateServer();

        contentServer = contentServer.replaceFirst("#SERVER_NAME#", Common.sanitize(this.name))
                                     .replaceFirst("#SERVER_URL#", this.url);

        if (this.settings_file == null || this.settings_file.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            String[] lines = contentServer.split("\n");

            for (String line : lines) {
                if (!line.contains("#SETTINGS_FILE#")) {
                    sb.append(line);
                }
            }

            contentServer = sb.toString();
        } else {
            contentServer = contentServer.replaceFirst("#SETTINGS_FILE#", this.settings_file);
        }

        return contentServer;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof MavenServer) && (((MavenServer) o).getName()).equals(this.getName());
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return this.name + '-' + this.url;
    }

    @Override
    public int compareTo(MavenServer m) {
        return this.name.compareTo(m.getName());
    }
}
