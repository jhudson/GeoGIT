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

    private static final String REFS_REMOTES = "refs/remotes/";

    public static Map<String, String> readRef(final File repoLocation, final RemoteConfigObject ref) {

        Map<String, String> retMap = new HashMap<String, String>();
        File file = new File(repoLocation, REFS_REMOTES+ref.getName());
        
        if (file.exists()){
            for (File refFile : file.listFiles()){
                try {
                    String refHead = readFileAsString(refFile);
                    retMap.put(file.getName(),refHead);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return retMap;
    }
    
    public static void writeRef(final File repoLocation, final String refName, final ObjectId id){
        Writer out = null;
        File directory = new File(repoLocation+"/"+REFS_REMOTES);
        File file = new File(directory, refName);

        if (!directory.exists()){
            directory.mkdirs();
        }

        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Could not open refs file: " + file.getAbsolutePath());
            }
        } else {
            return;
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
    
    private static String readFileAsString(File file) throws java.io.IOException{
        byte[] buffer = new byte[(int) file.length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(file));
            f.read(buffer);
        } finally {
            if (f != null) try { f.close(); } catch (IOException ignored) { }
        }
        return new String(buffer);
    }
}
