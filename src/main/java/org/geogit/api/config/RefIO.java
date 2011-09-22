package org.geogit.api.config;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.geogit.api.ObjectId;

/**
 * Class to read and write ref files when adding a remote or fetching
 * 
 * @author jhudson
 */
public class RefIO {

    private static final String REFS_REMOTES = "/refs/remotes/";

    /**
     * Write out the remote refs files
     * @param repoLocation
     * @param remoteName
     * @param branchName
     * @param id
     */
    public static void writeRemoteRefs( final File repoLocation, final String remoteName,
            final String branchName, final ObjectId id ) {
        Writer out = null;
        File directory = new File(repoLocation + REFS_REMOTES + remoteName);
        File file = new File(directory, branchName);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Could not open refs file: " + file.getAbsolutePath());
            }
        }

        try {
            out = new BufferedWriter(new FileWriter(file));
            out.write(id.toString());
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

    /**
     * Retrieve a list of <branch_name, branch_head_id>'s which can be sent to the remote server to retrive commit updates
     * @param repoLocation
     * @param remoteName
     * @return
     */
	public static Map<String, String> getRemoteList(File repoLocation, String remoteName) {
        Map<String, String> retMap = new HashMap<String, String>();
        File file = new File(repoLocation + REFS_REMOTES + remoteName);

        if (file.exists()) {
            for( File refFile : file.listFiles() ) {
                try {
                    String refHead = readFileAsString(refFile);
                    retMap.put(refFile.getName(), refHead);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return retMap;
	}

	/**
	 * Convenience method to read a file as a single line
	 * @param file
	 * @return
	 * @throws java.io.IOException
	 */
    private static String readFileAsString( File file ) throws java.io.IOException {
        byte[] buffer = new byte[(int) file.length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(file));
            f.read(buffer);
        } finally {
            if (f != null) try {
                f.close();
            } catch (IOException ignored) {
            }
        }
        return new String(buffer);
    }
}
