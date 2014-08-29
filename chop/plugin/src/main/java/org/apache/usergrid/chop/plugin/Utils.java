/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.plugin;


import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;


public class Utils {

    private final static Logger LOG = LoggerFactory.getLogger( Utils.class );


    /**
     * @param jarFile Jar file to be extracted
     * @param destinationFolder Folder which the jarFile will be extracted to. Jar file's root will be this folder once
     * it is extracted.
     */
    public static void extractJar( File jarFile, String destinationFolder ) throws MojoExecutionException {
        try {
            ZipUnArchiver unArchiver = new ZipUnArchiver( jarFile );
            unArchiver.enableLogging( new ConsoleLogger( org.codehaus.plexus.logging.Logger.LEVEL_INFO, "console" ) );
            unArchiver.setDestDirectory( new File( destinationFolder ) );
            unArchiver.extract();
        }
        catch ( Exception e ) {
            throw new MojoExecutionException( "Error while extracting JAR file", e );
        }
    }


    /**
     * @param jarFile Jar file to be created
     * @param sourceFolder Jar file will be created out of the contents of this folder. This corresponds to the root
     * folder of the jar file once it is created.
     */
    public static void archiveWar( File jarFile, String sourceFolder ) throws MojoExecutionException {
        try {
            ZipArchiver archiver = new ZipArchiver();
            archiver.enableLogging( new ConsoleLogger( org.codehaus.plexus.logging.Logger.LEVEL_INFO, "console" ) );
            archiver.setDestFile( jarFile );
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
    public static void copyArtifactsTo( MavenProject project, String targetFolder )
            throws MojoExecutionException {
        File targetFolderFile = new File( targetFolder );
        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); ) {
            Artifact artifact = ( Artifact ) it.next();

            File f = artifact.getFile();

            LOG.info( "Artifact {} found.", f.getAbsolutePath() );

            if ( f == null ) {
                throw new MojoExecutionException( "Cannot locate artifact file of " + artifact.getArtifactId() );
            }

            // Check already existing artifacts and replace them if they are of a lower version
            try {

                List<String> existing =
                        FileUtils.getFileNames( targetFolderFile, artifact.getArtifactId() + "-*.jar", null, false );

                if ( existing.size() != 0 ) {
                    String version =
                            existing.get( 0 ).split( "(" + artifact.getArtifactId() + "-)" )[1].split( "(.jar)" )[0];
                    DefaultArtifactVersion existingVersion = new DefaultArtifactVersion( version );
                    DefaultArtifactVersion artifactVersion = new DefaultArtifactVersion( artifact.getVersion() );

                    if ( existingVersion.compareTo( artifactVersion ) < 0 ) { // Remove existing version
                        FileUtils.forceDelete( targetFolder + existing.get( 0 ) );
                    }
                    else {
                        LOG.info( "Artifact " + artifact.getArtifactId() + " with the same or higher " +
                                "version already exists in lib folder, skipping copy" );
                        continue;
                    }
                }

                LOG.info( "Copying {} to {}", f.getName(), targetFolder );
                FileUtils.copyFileToDirectory( f.getAbsolutePath(), targetFolder );
            }
            catch ( IOException e ) {
                throw new MojoExecutionException( "Error while copying artifact file of " + artifact.getArtifactId(),
                        e );
            }
        }
    }


    /**
     * Copies all found resource files, including test resources to the <code>targetFolder</code>.
     * <p>
     * Resource files to be copied are filtered or included according to the configurations inside
     * <code>project</code>'s pom.xml file.
     *
     * @param project       project whose resource files to be copied
     * @param targetFolder  matching resource files are stored in this directory
     * @return
     */
    public static boolean copyResourcesTo( MavenProject project, String targetFolder )  {
        File targetFolderFile = new File( targetFolder );
        String includes;
        String excludes;
        List allResources = project.getResources();
        allResources.addAll( project.getTestResources() );

        // If there is no resource folder under project, mvn chop:runner goal should fail
        if ( ! hasResourceFolders( project ) ){
            return false;
        }
        else{
            LOG.info( "Copying resource files to runner.jar" );

            for( Object res: allResources ) {
                if( ! ( res instanceof Resource ) ) {
                    continue;
                }
                Resource resource = ( Resource ) res;
                try {
                    File baseDir = new File( resource.getDirectory() );
                    includes = resource.getIncludes().toString().replace( "[", "" ).replace( "]", "" ).replace( " ", "" );
                    excludes = resource.getExcludes().toString().replace( "[", "" ).replace( "]", "" ).replace( " ", "" );

                    List<String> resFiles = FileUtils.getFileNames( baseDir, includes, excludes, true, true );
                    for( String resFile: resFiles ) {
                        File resourceFile = new File( resFile );
                        LOG.info( "Copying {} to {}", resourceFile.getName(), targetFolder );
                        FileUtils.copyFileToDirectory( resourceFile, targetFolderFile );
                    }
                }
                catch ( IOException e ){
                    LOG.info( "Error while trying to copy resource files.", e );
                }
                catch ( IllegalStateException e ) {
                    String path = resource.getDirectory();
                    path = path.substring( 0, path.lastIndexOf( "/" ) );
                    LOG.info( "There is no resource folder under {} folder.", path );
                }
            }
            return true;
        }
    }

    /**
     * Returns true if there is at least one resource folder inside the project.
     *
     * @param project
     * @return
     */
    public static boolean hasResourceFolders( MavenProject project ){
        List<Resource> resources = project.getResources();
        for ( Resource res : resources ){
            if ( FileUtils.fileExists( res.getDirectory() ) ){
                return true;
            }
        }
        return false;
    }


    /**
     * @param projectPath
     * @return
     * @throws MojoExecutionException
     */
    public static String getGitConfigFolder( String projectPath ) throws MojoExecutionException {
        projectPath = forceNoSlashOnDir( projectPath );

        while ( !FileUtils.fileExists( projectPath + File.separator + ".git" ) ) {
            int lastSlashIndex = projectPath.lastIndexOf( File.separator );
            if ( lastSlashIndex < 1 ) {
                throw new MojoExecutionException( "There are no local git repository associated with this project" );
            }
            projectPath = projectPath.substring( 0, lastSlashIndex );
        }
        return projectPath + File.separator + ".git";
    }


    /**
     * @param gitConfigFolder e.g. /your/project/root/.git
     *
     * @return Returns last commit's UUID, "nocommit" if there are no commits and returns null if an exception occured
     */
    public static String getLastCommitUuid( String gitConfigFolder ) throws MojoExecutionException {
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
    public static String getGitRemoteUrl( String gitConfigFolder ) throws MojoExecutionException {
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
    public static boolean isCommitNecessary( String gitConfigFolder ) throws MojoExecutionException {
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


    /**
     * Concatenates provided timestamp and commitUUID strings and returns their calculated MD5 in hexadecimal format
     *
     * @return Returns the hexadecimal representation of calculated MD5
     *
     * @throws MojoExecutionException This will probably never thrown, cause UTF-8 encoding and MD5 is defined in each
     * system
     */
    public static String getMD5( String timestamp, String commitUUID ) throws MojoExecutionException {
        try {
            MessageDigest digest = MessageDigest.getInstance( "MD5" );
            byte[] hash = digest.digest( ( timestamp + commitUUID ).getBytes( "UTF-8" ) );

            StringBuilder result = new StringBuilder( hash.length * 2 );
            for ( int i = 0; i < hash.length; i++ ) {
                result.append( String.format( "%02x", hash[i] & 0xff ) );
            }

            return result.toString();
        }
        catch ( NoSuchAlgorithmException e ) {
            throw new MojoExecutionException( "MD5 algorithm could not be found", e );
        }
        catch ( UnsupportedEncodingException e ) {
            throw new MojoExecutionException( "UTF-8 encoding is not supported", e );
        }
    }


    /** @return Returns the given date in a 'yyyy.MM.dd.HH.mm.ss' format, UTC timezone */
    public static String getTimestamp( Date date ) {
        SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy.MM.dd.HH.mm.ss" );
        dateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return dateFormat.format( date );
    }


    /**
     * @param directory
     * @return
     */
    public static String forceSlashOnDir( String directory ) {
        return directory.endsWith( File.separator ) ? directory : directory + File.separator;
    }


    /**
     * @param directory
     * @return
     */
    public static String forceNoSlashOnDir( String directory ) {
        return directory.endsWith( File.separator ) ? directory.substring( 0, directory.length() - 1 ) : directory;
    }
}