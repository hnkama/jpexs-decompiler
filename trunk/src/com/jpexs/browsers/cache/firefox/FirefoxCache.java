package com.jpexs.browsers.cache.firefox;

import com.jpexs.browsers.cache.CacheEntry;
import com.jpexs.browsers.cache.CacheImplementation;
import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author JPEXS
 */
public class FirefoxCache implements CacheImplementation {

    private static FirefoxCache instance;

    private FirefoxCache() {
    }

    public static FirefoxCache getInstance() {
        if (instance == null) {
            instance = new FirefoxCache();
        }
        return instance;
    }
    private boolean loaded = false;
    private CacheMap map;

    @Override
    public void refresh() {
        File dir = getCacheDirectory();
        File cacheMapFile = new File(dir, "_CACHE_MAP_");
        try {
            map = new CacheMap(cacheMapFile);
        } catch (IOException ex) {
            Logger.getLogger(FirefoxCache.class.getName()).log(Level.SEVERE, null, ex);
        }
        loaded = true;
    }

    @Override
    public List<CacheEntry> getEntries() {
        if (!loaded) {
            refresh();
        }
        if (map == null) {
            return null;
        }
        List<CacheEntry> ret = new ArrayList<>();

        ret.addAll(map.mapBuckets);
        return ret;
    }

    private enum OSId {

        WINDOWS, OSX, UNIX
    }

    private static OSId getOSId() {
        PrivilegedAction<String> doGetOSName = new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("os.name");
            }
        };
        OSId id = OSId.UNIX;
        String osName = AccessController.doPrivileged(doGetOSName);
        if (osName != null) {
            if (osName.toLowerCase().startsWith("mac os x")) {
                id = OSId.OSX;
            } else if (osName.contains("Windows")) {
                id = OSId.WINDOWS;
            }
        }
        return id;
    }

    public static File getProfileDirectory() {
        File profilesDir = getProfilesDirectory();
        if (profilesDir == null) {
            return null;
        }
        File profiles[] = profilesDir.listFiles();
        File profileDir = null;
        for (File f : profiles) {
            if (f.isDirectory()) {
                if (f.getName().matches("[a-z0-9]+\\.default")) {
                    profileDir = f;
                    break;
                }
            }
        }
        return profileDir;
    }

    public static File getCacheDirectory() {
        File profileDir = getProfileDirectory();
        File cacheDir = null;
        if (profileDir != null) {
            cacheDir = new File(profileDir, "Cache");
        }
        return cacheDir;
    }

    public static File getProfilesDirectory() {
        String userHome = null;
        File profilesDir = null;
        try {
            userHome = System.getProperty("user.home");
        } catch (SecurityException ignore) {
        }
        if (userHome != null) {
            OSId osId = getOSId();
            if (osId == OSId.WINDOWS) {
                profilesDir = new File(userHome + "\\AppData\\Local\\Mozilla\\Firefox\\Profiles");
                if (!profilesDir.exists()) {
                    profilesDir = new File(userHome + "\\Local Settings\\Application Data\\Mozilla\\Firefox\\Profiles");
                }
            } else if (osId == OSId.OSX) {
                profilesDir = new File(userHome + "/Library/Caches/Firefox/Profiles");
            } else {
                profilesDir = new File(userHome + "/.mozilla/firefox");
            }
        }
        if ((profilesDir == null) || !profilesDir.exists()) {
            return null;
        }
        return profilesDir;
    }
}