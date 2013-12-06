package org.apache.usergrid.perftest.plugin;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;


@Mojo( name = "war", requiresDependencyResolution = ResolutionScope.TEST,
        requiresDependencyCollection = ResolutionScope.TEST )
public class PerftestWarMojo extends PerftestMojo {
    /**
     *
     */
    protected String gitConfigDirectory;

    /**
     * Full path to the war file you are going to upload
     *
     * defaultValue assumes the module which calls this plugin builds the war to be deployed
     */
    @Parameter( property = "perftest.sourceFile",
            defaultValue = "${pom.basedir}/target/${project.build.finalName}.${project.packaging}" )
    protected String sourceFile;

    /**
     * sourceFile will be uploaded as destinationParentDir/commitUUID-Timestamp/sourceFileName in S3 bucket
     *
     * defaultValue is "tests/"
     */
    @Parameter( property = "perftest.destinationParentDir", defaultValue = "tests/" )
    protected String destinationParentDir;

    /**
     *
     */
    @Parameter( property = "perftest.perftest.formation", required = true )
    protected String perftestFormation;

    /**
     * Fully qualified CN property of the app once it's deployed to its container. This paramater will be put to the
     * config.properties file inside the WAR to be uploaded
     */
    @Parameter( property = "perftest.test.module.fqcn", required = true )
    protected String testModuleFQCN;

    /**
     * Container's (probably Tomcat) Manager user name. This paramater will be put to the config.properties file inside
     * the WAR to be uploaded
     */
    @Parameter( property = "perftest.manager.app.username", required = true )
    protected String managerAppUsername;

    /**
     * Container's (probably Tomcat) Manager user name. This paramater will be put to the config.properties file inside
     * the WAR to be uploaded
     */
    @Parameter( property = "perftest.manager.app.password", required = true )
    protected String managerAppPassword;

    /**
     * Leaving this parameter with the default 'true' value causes the plugin goal to fail when there are modified
     * sources in the local git repository.
     */
    @Parameter( property = "perftest.failIfCommitNecessary", defaultValue = "true" )
    protected boolean failIfCommitNecessary;

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    @Parameter( defaultValue = "${settings.localRepository}" )
    private String localRepository;

    @Override
    public void execute() throws MojoExecutionException {

        // Ugly solution to retrieve war file
        String webappWarPath = localRepository +
                "/org/apache/usergrid/perftest/perftest-webapp/1.0-SNAPSHOT/perftest-webapp-1.0-SNAPSHOT.war";

        String projectBaseDirectory = project.getBasedir().getAbsolutePath();
        if( projectBaseDirectory.charAt( projectBaseDirectory.length() - 1 ) == '/' )
        {
            projectBaseDirectory = projectBaseDirectory.substring( 0, projectBaseDirectory.length() - 1 );
        }

        // Setup gitConfigDirectory
        gitConfigDirectory = projectBaseDirectory;
        while ( ! FileUtils.fileExists( gitConfigDirectory + "/.git" ) )
        {
            int lastSlashIndex = gitConfigDirectory.lastIndexOf( "/" );
            if ( lastSlashIndex < 1 )
            {
                throw new MojoExecutionException( "There are no local git repository associated with this project") ;
            }
            gitConfigDirectory = gitConfigDirectory.substring( 0, lastSlashIndex );
        }
        gitConfigDirectory += "/.git";

        String commitId = getLastCommitUuid( gitConfigDirectory );

        if ( failIfCommitNecessary && isCommitNecessary( gitConfigDirectory ) ) {
            String failMsg = "There are modified sources, commit changes before calling the plugin or set "
                    + "failIfCommitNecessary parameter as false in your plugin configuration field inside the pom.xml";

            throw new MojoExecutionException( failMsg );
        }

        try {
            // Check and setup plugin configuration parameters

            SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy.MM.dd.hh.mm.ss" );
            String timeStamp = dateFormat.format( new Date() );

            // Extract the war file
            File warFile = new File( webappWarPath );
            String extractedWarRoot = projectBaseDirectory + "/target/perftest/";
            if ( FileUtils.fileExists( extractedWarRoot ) ) {
                FileUtils.cleanDirectory( extractedWarRoot );
            }
            else {
                FileUtils.mkdir( extractedWarRoot );
            }
            extractWar( warFile, extractedWarRoot );

            // Copy caller project jar and its dependency jars to WEB-INF/lib folder
            String libPath = extractedWarRoot + "WEB-INF/lib/";
            FileUtils.copyFileToDirectory( sourceFile, libPath );
            copyArtifactsTo( libPath, true );

            // Create config.properties file
            InputStream inputStream;
            Properties prop = new Properties();
            String configPropertiesFilePath = extractedWarRoot + "WEB-INF/classes/config.properties";
            if ( FileUtils.fileExists( configPropertiesFilePath ) ) {
                // Existing config.properties of webapp-perftest
                inputStream = new FileInputStream( configPropertiesFilePath );
                prop.load( inputStream );
                inputStream.close();
            }

            // If exists, properties in this file can overwrite the ones from webapp-perftest
            if ( getClass().getResource( "config.properties" ) != null ) {
                inputStream = getClass().getResourceAsStream( "config.properties" );
                Properties propCurrent = new Properties();
                propCurrent.load( inputStream );
                inputStream.close();

                String key;
                while ( propCurrent.propertyNames().hasMoreElements() ) {
                    key = propCurrent.propertyNames().nextElement().toString();
                    prop.setProperty( key, propCurrent.getProperty( key ) );

                }
            }

            // Insert or overwrite all properties acquired in runtime
            prop.setProperty( "git.uuid", commitId );
            prop.setProperty( "git.url", getGitRemoteUrl( gitConfigDirectory ) );
            prop.setProperty( "create.timestamp", timeStamp );
            prop.setProperty( "group.id", project.getGroupId() );
            prop.setProperty( "artifact.id", project.getArtifactId() );
            prop.setProperty( "perftest.formation", perftestFormation );
            prop.setProperty( "test.module.fqcn", testModuleFQCN );
            prop.setProperty( "aws.s3.bucket", bucketName );
            prop.setProperty( "aws.s3.key", accessKey );
            prop.setProperty( "aws.s3.secret", secretKey );
            prop.setProperty( "manager.app.username", managerAppUsername );
            prop.setProperty( "manager.app.password", managerAppPassword );

            // Save the newly formed properties file under WEB-INF/classes/config.properties
            FileUtils.mkdir( configPropertiesFilePath.substring( 0, configPropertiesFilePath.lastIndexOf('/') ) );
            FileWriter writer = new FileWriter( configPropertiesFilePath );
            prop.store( writer, null );

            // Create the final WAR
            String finalWarPath = projectBaseDirectory + "/target/perftest.war";
            File finalWarFile = new File( finalWarPath );
            archiveWar( finalWarFile, extractedWarRoot );

            // Upload the created WAR to S3 bucket

            // Mojo doesn't allow overriding Parameters, so I set this manually
            super.sourceFile = finalWarPath;

            destinationFile = destinationParentDir + commitId + "-" + timeStamp + "/" + finalWarFile.getName();

            super.execute();
        }
        catch ( Exception e ) {
            throw new MojoExecutionException( "Error while executing plugin", e );
        }
    }


    /**
     * @param warFile War file to be extracted
     * @param destinationFolder Folder which the warFile will be extracted to. War file's root will be this folder once
     * it is extracted.
     */
    protected void extractWar( File warFile, String destinationFolder ) throws MojoExecutionException {
        try {
            ZipUnArchiver unArchiver = new ZipUnArchiver( warFile );
            unArchiver.enableLogging( new ConsoleLogger( org.codehaus.plexus.logging.Logger.LEVEL_INFO, "console" ) );
            unArchiver.setDestDirectory( new File( destinationFolder ) );
            unArchiver.extract();
        }
        catch ( Exception e ) {
            throw new MojoExecutionException( "Error while extracting WAR file", e );
        }
    }


    /**
     * @param warFile War file to be created
     * @param sourceFolder War file will be created out of the contents of this folder. This corresponds to the root
     * folder of the war file once it is created.
     */
    protected void archiveWar( File warFile, String sourceFolder ) throws MojoExecutionException {
        try {
            ZipArchiver archiver = new ZipArchiver();
            archiver.enableLogging( new ConsoleLogger( org.codehaus.plexus.logging.Logger.LEVEL_INFO, "console" ) );
            archiver.setDestFile( warFile );
            archiver.addDirectory( new File( sourceFolder ), "", new String[] { "**/*" }, null );
            archiver.createArchive();
        }
        catch ( Exception e ) {
            throw new MojoExecutionException( "Error while creating WAR file", e );
        }
    }


    /**
     * Gets all dependency jars of the project specified by 'project' parameter from the local mirror and copies them
     * under targetFolder
     *
     * @param targetFolder The folder which the dependency jars will be copied to
     */
    protected void copyArtifactsTo( String targetFolder, boolean skipTestScope ) throws MojoExecutionException {
        File targetFolderFile = new File( targetFolder );
        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); ) {
            Artifact artifact = ( Artifact ) it.next();
            if ( skipTestScope && artifact.getScope() == "test" ) {
                continue;
            }

            File f = artifact.getFile();

            if ( f == null ) {
                throw new MojoExecutionException( "Cannot locate artifact file of " + artifact.getArtifactId() );
            }

            // Check already existing artifacts and replace them if they are of a lower version
            try {

                List<String> existing = FileUtils.getFileNames(targetFolderFile, artifact.getArtifactId() + "-*.jar",
                        null, false);

                if ( existing.size() != 0 ) {
                    String version = existing.get(0).split("(" + artifact.getArtifactId() + "-)")[1]
                            .split("(.jar)") [0];
                    DefaultArtifactVersion existingVersion = new DefaultArtifactVersion( version );
                    DefaultArtifactVersion artifactVersion = new DefaultArtifactVersion( artifact.getVersion() );

                    if ( existingVersion.compareTo( artifactVersion ) < 0 ) { // Remove existing version
                        FileUtils.forceDelete( targetFolder + existing.get(0) );
                    }
                    else {
                        System.out.println("Artifact " + artifact.getArtifactId() + " with the same or higher " +
                                "version already exists in lib folder, skipping copy");
                        continue;
                    }
                }

                System.out.println( "Copying " + f.getName() + " to " + targetFolder );
                FileUtils.copyFileToDirectory( f.getAbsolutePath(), targetFolder );
            }
            catch ( IOException e ) {
                throw new MojoExecutionException( "Error while copying artifact file of " + artifact.getArtifactId(),
                        e );
            }
        }
    }


    /**
     * @param gitConfigFolder e.g. /your/project/root/.git
     *
     * @return Returns last commit's UUID, "nocommit" if there are no commits and returns null if an exception occured
     */
    protected static String getLastCommitUuid( String gitConfigFolder ) throws MojoExecutionException {
        try {
            Repository repo =
                    new RepositoryBuilder().setGitDir( new File( gitConfigFolder ) ).readEnvironment().findGitDir()
                                           .build();
            RevWalk walk = new RevWalk( repo );
            ObjectId head = repo.resolve( "HEAD" );
            if ( head != null ) {
                RevCommit lastCommit = walk.parseCommit( head );
                return lastCommit.getId().getName();
            }
            else {
                return "nocommit";
            }
        }
        catch ( Exception e ) {
            throw new MojoExecutionException( "Error trying to get the last git commit uuid", e );
        }
    }


    /**
     * @param gitConfigFolder e.g. /your/project/root/.git
     *
     * @return Returns git config remote.origin.url field of the repository located at gitConfigFolder
     */
    protected String getGitRemoteUrl( String gitConfigFolder ) throws MojoExecutionException {
        try {
            Repository repo =
                    new RepositoryBuilder().setGitDir( new File( gitConfigFolder ) ).readEnvironment().findGitDir()
                                           .build();
            Config config = repo.getConfig();
            return config.getString( "remote", "origin", "url" );
        }
        catch ( Exception e ) {
            throw new MojoExecutionException( "Error trying to get remote origin url of git repository", e );
        }
    }


    /**
     * @param gitConfigFolder e.g. /your/project/root/.git
     *
     * @return Returns true if 'git status' has modified files inside the 'Changes to be committed' section
     */
    protected boolean isCommitNecessary( String gitConfigFolder ) throws MojoExecutionException {
        try {
            Repository repo = new FileRepository( gitConfigFolder );
            Git git = new Git( repo );

            Status status = git.status().call();
            Set<String> modified = status.getModified();

            return ( modified.size() != 0 );
        }
        catch ( Exception e ) {
            throw new MojoExecutionException( "Error trying to find out if git commit is needed", e );
        }
    }
}
