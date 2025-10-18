package org.moboxlab.MoBoxProxyPool.Tick;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.moboxlab.MoBoxProxyPool.BasicInfo;
import org.moboxlab.MoBoxProxyPool.Cache.CacheECS;
import org.moboxlab.MoBoxProxyPool.Cache.CacheLoad;
import org.moboxlab.MoBoxProxyPool.Request.*;

import java.util.HashSet;
import java.util.Set;

public class TickMain {
    public static long mspt = 0;
    public static Thread tickThread;
    public static void runTick() {
        CacheLoad.loadCacheECS();
        tickThread = new Thread(TickMain::ticker);
        tickThread.start();

    }

    @SuppressWarnings({"BusyWait", "InfiniteLoopStatement"})
    public static void ticker() {
        try {
            BasicInfo.logger.sendInfo("TickLoop线程已启动！");
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
                    JSONObject info = AliyunGetInstanceInfo.getInstanceInfo(instanceID);
                    if (info == null) {
                        //空数据则标记ECS已删除
                        CacheECS.ecsMap.remove(instanceID);
                        BasicInfo.logger.sendInfo("查询实例"+instanceID+"时返回空数据，实例已删除！");
                    } else {
                        //有数据则ECS存活
                        JSONArray ip = info.getJSONObject("PublicIpAddress").getJSONArray("IpAddress");
                        CacheECS.ecsMap.get(instanceID).setCreateTime(info.getString("CreationTime"));
                        if (ip.size() != 0) CacheECS.ecsMap.get(instanceID).IP = ip.getString(0);
                        //根据状态，进行对应操作
                        switch (CacheECS.ecsMap.get(instanceID).status) {
                            case RUNNING:
                                //运行中状态
                                boolean timeCheck = CacheECS.ecsMap.get(instanceID).timeCheck();
                                if (timeCheck && ((CacheECS.ecsMap.size()-1) < CacheECS.targetAmount)) createNewECS();
                                CacheECS.ecsMap.get(instanceID).checkSessionMap();
                                break;
                            case CYCLING:
                                //即将回收状态
                                releaseECS(instanceID);
                                break;
                            default:
                                break;
                        }
                    }
                });
                //ECS不足时进行补足
                int remain = CacheECS.targetAmount - CacheECS.ecsMap.size();
                if (remain > 0) {
                    BasicInfo.logger.sendInfo("检测到IP数量不足，正在自动新建ECS......");
                    BasicInfo.logger.sendInfo("缺少数量："+remain);
                    for (int i = 0; i < remain; i++) {
                        asyncCreate();
                    }
                }
            } catch (Exception e) {
                BasicInfo.logger.sendException(e);
                BasicInfo.logger.sendWarn("执行Tick时出现错误！");
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

    public static void asyncCreate() {
        Thread thread = new Thread(TickMain::createNewECS);
        thread.start();
    }

    public static void createNewECS() {
        try {
            String rid = BasicInfo.config.getString("regionID");
            String zid = BasicInfo.config.getString("zoneID");
            String tid = BasicInfo.config.getString("instanceType");
            JSONObject create = AliyunCreateInstance.createInstance(rid,zid,tid);
            String instanceID = create.getString("InstanceId");
            JSONObject allocate = AliyunAllocateIP.allocateIP(instanceID);
            JSONObject start = AliyunStartInstance.startInstance(instanceID);
            BasicInfo.logger.sendInfo("实例创建成功！实例ID："+instanceID);
            CacheECS.addECS(instanceID);
        } catch (Exception e) {
            BasicInfo.logger.sendException(e);
            BasicInfo.logger.sendWarn("创建实例时出现错误！");
        }
    }

    public static void releaseECS(String instanceID) {
        BasicInfo.logger.sendInfo("正在释放实例！实例ID："+instanceID);
        JSONObject delete = AliyunDeleteInstance.deleteInstance(instanceID);
        CacheECS.removeECS(instanceID);
        BasicInfo.logger.sendInfo("实例释放成功！实例ID："+instanceID);
    }
}
