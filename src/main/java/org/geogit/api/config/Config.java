/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RemoteAddOp;
import org.geogit.repository.Repository;

import com.google.common.base.Preconditions;

/**
 * The config object for this GeoGIT repository, this represents the storage of: CORE, REMOTE and
 * BRANCH config elements in the git format: [core] [remote "origin"] fetch =
 * refs/heads/*:refs/remotes/origin url = C:\java\GeoGIT\target\mockblobstore TODO: current
 * limitations: there is no CORE elements read or written.
 * 
 * @author jhudson
 */
public class Config {

    private static final String URL = "url";
    private static String CONFIG_FILE = "config";
    private static final String CORE = "core";
    private static final String REMOTE = "remote";
    private static final String BRANCH = "branch";
    private static final String FETCH = "fetch";

    public static final String NEW_LINE = System.getProperty("line.separator");
    public static final String TAB = "\t";

    private String owner;

    private Map<String, RemoteConfigObject> remotes;
    private Map<String, BranchConfigObject> branches;

    private File configFile;
    private Repository repo;

    public Config( final Repository repo ) {
        Preconditions.checkNotNull(repo);
        this.repo = repo;
        this.remotes = new HashMap<String, RemoteConfigObject>();
        this.branches = new HashMap<String, BranchConfigObject>();
        this.configFile = new File(repo.getRepositoryHome(), CONFIG_FILE);

        try {
            if (!this.configFile.exists()) {
                this.configFile.createNewFile();
                writeConfig();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        readConfig();
    }
    private void addBranch( BranchConfigObject branchConfigObject ) {
        this.branches.put(branchConfigObject.getName(), branchConfigObject);
        writeConfig();
    }

    public RemoteConfigObject getRemote( String remoteName ) {
        return remotes.get(remoteName);
    }

    public void addRemoteConfigObject( final RemoteConfigObject rs ) {
        this.remotes.put(rs.getName(), rs);
        writeConfig();
    }

    /**
     * ===== FILE IO ====
     */

    public void readConfig() {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new FileReader(configFile));
            while( scanner.hasNextLine() ) {
                String line = scanner.nextLine();
                processLine: {
                    if (line.startsWith("[")) {

                        if (line.contains(CORE)) {
                            // processCoreConfig(line, scanner);
                            break processLine;
                        } else

                        if (line.contains(REMOTE)) {
                            processRemoteConfig(line, scanner);
                            break processLine;
                        } else

                        if (line.contains(BRANCH)) {
                            // processBrenchConfig(line, scanner);
                            break processLine;
                        }
                    }
                }
            }
        } catch (FileNotFoundException fnfe) {
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    /**
     * Process the [remote "REMOTE_NAME"] config from a git config file. Remember this using the
     * original scanner which pushes the read head further along, dont realy on it being at the same
     * place when you get it back.
     * 
     * @param scanner
     */
    protected void processBranchConfig( final String header, final Scanner scanner ) {
        String remoteName = extractHeaderName(header);

        String remote = extractValue(scanner.nextLine().trim());
        String merge = extractValue(scanner.nextLine().trim());

        BranchConfigObject rs = new BranchConfigObject(remoteName, remote, merge);
        this.branches.put(remoteName, rs);

    }

    /**
     * Process the [remote "REMOTE_NAME"] config from a git config file. Remember this using the
     * original scanner which pushes the read head further along, dont realy on it being at the same
     * place when you get it back.
     * 
     * @param scanner
     */
    protected void processRemoteConfig( final String header, final Scanner scanner ) {
        String remoteName = extractHeaderName(header);

        String fetch = extractValue(scanner.nextLine().trim());
        String url = extractValue(scanner.nextLine().trim());

        RemoteConfigObject rs = new RemoteConfigObject(remoteName, fetch, url);
        this.remotes.put(remoteName, rs);

    }

    private String extractValue( String line ) {
        Scanner valueScanner = new Scanner(line.trim());
        valueScanner.useDelimiter("=");
        valueScanner.next(); // skip value
        String retStr = valueScanner.next();
        valueScanner.close();
        return retStr.trim();
    }

    private String extractHeaderName( final String header ) {
        Pattern pattern = Pattern.compile("\"(.*?)\"");
        Matcher matcher = pattern.matcher(header);
        String remoteName = "";

        while( matcher.find() ) {
            remoteName = header.substring(matcher.start() + 1, matcher.end() - 1);
        }
        return remoteName;
    }

    public synchronized void writeConfig() {
        Writer out = null;
        try {
            out = new BufferedWriter(new FileWriter(configFile));

            // TODO: write Core config
            out.write("[core]" + NEW_LINE);

            // write branches
            for( BranchConfigObject branch : branches.values() ) {
                out.write("[branch \"" + branch.getName() + "\"]" + NEW_LINE);
                out.write(TAB + "fetch = " + branch.getRemote() + NEW_LINE);
                out.write(TAB + "merge = " + branch.getMerge() + NEW_LINE);

                /*
                 * Now we must write out the local branches into the origin remote,
                 * special case here where HEAD is the current head and all other 
                 * local branches are placed here.
                 */
                Ref ref = repo.getRef(Ref.REMOTES_PREFIX + "origin");
                RefIO.writeRef(repo.getRepositoryHome(), "origin", branch.getName(), ref == null ? ObjectId.NULL : ref.getObjectId());
            }

            // write remotes
            for( RemoteConfigObject remote : remotes.values() ) {
                out.write("[remote \"" + remote.getName() + "\"]" + NEW_LINE);
                out.write(TAB + "fetch = " + remote.getFetch() + NEW_LINE);
                out.write(TAB + "url = " + remote.getUrl() + NEW_LINE);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public Map<String, RemoteConfigObject> getRemotes() {
        return this.remotes;
    }
    public Map<String, BranchConfigObject> getBranches() {
        return this.branches;
    }
}
