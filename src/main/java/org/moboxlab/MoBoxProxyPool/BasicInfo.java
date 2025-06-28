package org.moboxlab.MoBoxProxyPool;

import org.mossmc.mosscg.MossLib.Mysql.MysqlManager;
import org.mossmc.mosscg.MossLib.Object.ObjectConfig;
import org.mossmc.mosscg.MossLib.Object.ObjectLogger;

public class BasicInfo {
    public static String version = "V1.1.1.2.0815";
    public static String author = "MossCG";

    public static ObjectLogger logger;
    public static ObjectConfig config;
    public static MysqlManager mysql;

    /**
     * CREATED 创建状态，不确定是否部署代理脚本
     * INSTALLING 部署中状态，部署失败的话返回CREATED状态
     * RUNNING 已完成脚本部署
     * CYCLING 标记到期，即将被回收
     */
    public enum ECSStatus {
        CREATED,INSTALLING,RUNNING,CYCLING
    }

    public static boolean debug = false;
    public static void sendDebug(String message) {
        logger.sendAPI(message,debug);
    }
}
