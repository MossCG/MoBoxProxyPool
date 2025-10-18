package org.moboxlab.MoBoxProxyPool.Tick;

import org.moboxlab.MoBoxProxyPool.BasicInfo;
import org.moboxlab.MoBoxProxyPool.Cache.CacheECS;
import org.moboxlab.MoBoxProxyPool.Cache.CacheLoad;
import org.moboxlab.MoBoxProxyPool.SSH.SSHMain;

import java.util.HashSet;
import java.util.Set;

public class TickDeploy {
    public static long mspt = 0;
    public static Thread tickThread;
    public static void runTick() {
        CacheLoad.loadCacheECS();
        tickThread = new Thread(TickDeploy::ticker);
        tickThread.start();

    }

    @SuppressWarnings({"BusyWait", "InfiniteLoopStatement", "SwitchStatementWithTooFewBranches"})
    public static void ticker() {
        try {
            BasicInfo.logger.sendInfo("TickDeployLoop线程已启动！");
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            BasicInfo.logger.sendException(e);
        }
        while (true) {
            long start = System.currentTimeMillis();
            try {
                BasicInfo.sendDebug("正在执行Tick......");
                // 遍历ECS缓存，检查ECS状态
                Set<String> keys = new HashSet<>(CacheECS.ecsMap.keySet());
                keys.forEach(instanceID -> {
                    //根据状态，进行对应操作
                    switch (CacheECS.ecsMap.get(instanceID).status) {
                        case CREATED:
                            //已创建状态，部署脚本
                            SSHMain.asyncInit(CacheECS.ecsMap.get(instanceID));
                            break;
                        default:
                            break;
                    }
                });
            } catch (Exception e) {
                BasicInfo.logger.sendException(e);
                BasicInfo.logger.sendWarn("执行TickDeploy时出现错误！");
            }
            long end = System.currentTimeMillis();
            mspt = end-start;
            try {
                Thread.sleep(10000L);
            } catch (Exception e) {
                BasicInfo.logger.sendWarn("执行Tick等待时出现错误！");
            }
        }
    }
}
