package org.moboxlab.MoBoxProxyPool;

import org.moboxlab.MoBoxProxyPool.Cache.CacheECS;
import org.moboxlab.MoBoxProxyPool.Command.CommandDebug;
import org.moboxlab.MoBoxProxyPool.Command.CommandExit;
import org.moboxlab.MoBoxProxyPool.Command.CommandReload;
import org.moboxlab.MoBoxProxyPool.Command.CommandStatus;
import org.moboxlab.MoBoxProxyPool.Request.AliyunSDK;
import org.moboxlab.MoBoxProxyPool.Tick.TickDeploy;
import org.moboxlab.MoBoxProxyPool.Tick.TickMain;
import org.moboxlab.MoBoxProxyPool.Web.WebMain;
import org.mossmc.mosscg.MossLib.Command.CommandManager;
import org.mossmc.mosscg.MossLib.Config.ConfigManager;
import org.mossmc.mosscg.MossLib.File.FileCheck;
import org.mossmc.mosscg.MossLib.File.FileDependency;
import org.mossmc.mosscg.MossLib.Object.ObjectLogger;

public class Main {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        FileCheck.checkDirExist("./MBProxyPool");
        FileCheck.checkFileExist("./MBProxyPool/squid.conf","squid.conf");
        ObjectLogger logger = new ObjectLogger("./MBProxyPool/logs");
        BasicInfo.logger = logger;

        //初始化依赖
        FileDependency.loadDependencyDir("./MBProxyPool/dependency","dependency");

        //基础信息输出
        logger.sendInfo("欢迎使用MBProxyPool软件");
        logger.sendInfo("软件版本：" + BasicInfo.version);
        logger.sendInfo("软件作者：" + BasicInfo.author);

        //配置读取
        logger.sendInfo("正在读取配置文件......");
        BasicInfo.config = ConfigManager.getConfigObject("./MBProxyPool", "config.yml", "config.yml");
        if (!BasicInfo.config.getBoolean("enable")) {
            logger.sendInfo("你还没有完成配置文件的设置哦~");
            logger.sendInfo("快去配置一下吧~");
            logger.sendInfo("配置文件位置：./MBProxyPool/config.yml");
            System.exit(0);
        }
        BasicInfo.debug = BasicInfo.config.getBoolean("debug");

        //MySQL连接及初始化（暂时用不到数据库功能）
        //logger.sendInfo("正在初始化MySQL模块......");
        //MySQLMain.initMySQL();

        //Aliyun连接及初始化
        logger.sendInfo("正在初始化Aliyun模块......");
        AliyunSDK.loadAliyunClient();

        //Tick循环初始化
        logger.sendInfo("正在初始化Tick模块......");
        CacheECS.updateAmount(BasicInfo.config.getInteger("IPAmount"));
        TickMain.runTick();
        TickDeploy.runTick();

        //Http服务初始化
        logger.sendInfo("正在初始化Web模块......");
        WebMain.initWeb();

        //命令行初始化
        CommandManager.initCommand(BasicInfo.logger,true);
        CommandManager.registerCommand(new CommandDebug());
        CommandManager.registerCommand(new CommandExit());
        CommandManager.registerCommand(new CommandReload());
        CommandManager.registerCommand(new CommandStatus());

        long completeTime = System.currentTimeMillis();
        logger.sendInfo("启动完成！耗时："+(completeTime-startTime)+"毫秒！");
    }

    public static void reloadConfig() {
        BasicInfo.logger.sendInfo("正在重载配置文件......");
        BasicInfo.config = ConfigManager.getConfigObject("./MBProxyPool", "config.yml", "config.yml");
        BasicInfo.debug = BasicInfo.config.getBoolean("debug");
        CacheECS.updateAmount(BasicInfo.config.getInteger("IPAmount"));
        BasicInfo.logger.sendInfo("正在刷新缓存......");
        // CacheECS.ecsMap.forEach((s, ecs) -> ecs.sessionMap.clear());
        BasicInfo.logger.sendInfo("重载完成！");
    }
}
