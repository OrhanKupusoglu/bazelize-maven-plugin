package kupusoglu.orhan.bazelize_maven_plugin.goal;

import kupusoglu.orhan.bazelize_maven_plugin.model.Common;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;


/**
 * Participate in Maven build session lifecycle
 * <br>
 * @see <a href="https://maven.apache.org/examples/maven-3-lifecycle-extensions.html">Using Maven 3 lifecycle extension</a>
 */
@Component(
    role = AbstractMavenLifecycleParticipant.class
)
public class LifeCycle extends AbstractMavenLifecycleParticipant {
    @Requirement
    private Logger logger;


    private Log getLog(Object objLog) {
        Log log = null;

        if (objLog == null) {
            logger.error("Log failure: log is NULL\n");
            System.exit(3);
        } else {
            log = (Log)objLog;
        }

        return log;
    }

    private void display(Properties props, boolean backup, String rootDir, String suffix) {
        logger.info("Session - properties: " + (props == null ? "<>" : props.toString()));
        logger.info("Session - root: " + rootDir);
        logger.info("Session - backup: " + backup);
        logger.info("Session - suffix: " + (suffix == null || suffix.isEmpty() ? "<>" : suffix ));
    }

    @Override
    public void afterProjectsRead(MavenSession session)
        throws MavenExecutionException {
        // do nothing
    }

    @Override
    public void afterSessionStart(MavenSession session)
        throws MavenExecutionException  {
        // do nothing
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException  {
        List<String> goals = session.getRequest().getGoals();
        String goal = goals.get(0) == null ? "" : goals.get(0);

        logger.info("Event - afterSessionEnd - goal: " + goal);

        Object objLog = session.getTopLevelProject().getContextValue("log");
        Object objRootDir = session.getTopLevelProject().getContextValue("rootDir");
        Object objBackup = session.getTopLevelProject().getContextValue("backup");
        Object objSuffix = session.getTopLevelProject().getContextValue("suffix");

        Log log = null;
        String rootDir;
        boolean backup;
        String suffix = "";

        Properties props = session.getRequest().getUserProperties();

        String propRootDir = props.getProperty("rootDir");
        String propBackup = props.getProperty("backup");

        if (propRootDir == null) {
            if (objRootDir == null) {
                rootDir = Common.getDirCurrent();
            } else {
                rootDir = objRootDir.toString();
            }
        } else {
            rootDir = propRootDir;
        }

        if (propBackup == null) {
            if (objBackup == null) {
                backup = false;
            } else {
                backup = (Boolean)objBackup;
            }
        } else {
            backup = Boolean.parseBoolean(propBackup);
        }

        if (backup) {
            if (objSuffix == null) {
                suffix = Common.getFormattedTimestamp(LocalDateTime.now());
            } else {
                suffix = objSuffix.toString();
            }
        }

        if (goal.endsWith("module")) {
            log = getLog(objLog);

            display(props, backup, rootDir, suffix);

            try {
                Common.generateMetaFile(log, rootDir, suffix);
            } catch (MojoExecutionException e) {
                logger.error(e.getMessage());
            }
        } else if (goal.endsWith("build")) {
            log = getLog(objLog);
            String workspaceName = props.getProperty("workspaceName");

            display(props, backup, rootDir, suffix);

            try {
                Common.generateWorkspace(log, rootDir, workspaceName, suffix);
            } catch (MojoExecutionException e) {
                logger.error(e.getMessage());
            }
        } else {
            logger.info("Event - afterSessionEnd - ignored");
        }
    }
}
