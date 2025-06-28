package org.moboxlab.MoBoxProxyPool.Cache;

import org.moboxlab.MoBoxProxyPool.BasicInfo;
import org.moboxlab.MoBoxProxyPool.Object.ObjectECS;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CacheProxy {
    public static Random random = new Random();
    public static ObjectECS getProxy(String sessionID) {
        try {
            //建立初始可用池
            List<String> pool = getAvailablePool();
            if (pool.size() == 0) return null;
            //session检查
            pool = getSessionFreePool(pool,sessionID);
            //可用池直接分配
            if (pool.size() != 0) return getRandomECS(pool,sessionID);
            //可用池空则尝试触发保底
            if (BasicInfo.config.getBoolean("enableBackup")) {
                ObjectECS backupECS = new ObjectECS();
                backupECS.createTime = System.currentTimeMillis()-30*60*1000L;
                backupECS.IP = BasicInfo.config.getString("backupHost");
                backupECS.proxyPort = BasicInfo.config.getString("backupPort");
                backupECS.proxyAccount = BasicInfo.config.getString("backupUser");
                backupECS.proxyPassword = BasicInfo.config.getString("backupPassword");
                return backupECS;
            }
        } catch (Exception e) {
            BasicInfo.logger.sendException(e);
        }
        if (CacheECS.targetAmount == 0) return null;
        return getRandomECS(getAvailablePool(),sessionID);
    }

    public static List<String> getAvailablePool() {
        List<String> resultPool = new ArrayList<>();
        List<String> cachePool = new ArrayList<>(CacheECS.ecsMap.keySet());
        cachePool.forEach(id -> {
            if (CacheECS.ecsMap.get(id).available) resultPool.add(id);
        });
        return resultPool;
    }

    public static List<String> getSessionFreePool(List<String> cachePool, String sessionID) {
        List<String> resultPool = new ArrayList<>();
        cachePool.forEach(id -> {
            if (!CacheECS.ecsMap.get(id).sessionMap.containsKey(sessionID)) resultPool.add(id);
        });
        return resultPool;
    }

    public static ObjectECS getRandomECS(List<String> pool,String sessionID) {
        int loc = random.nextInt(pool.size());
        ObjectECS ecs = CacheECS.ecsMap.get(pool.get(loc));
        ecs.sessionMap.put(sessionID,System.currentTimeMillis());
        return ecs;
    }
}
