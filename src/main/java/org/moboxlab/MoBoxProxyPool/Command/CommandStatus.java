package org.moboxlab.MoBoxProxyPool.Command;

import org.moboxlab.MoBoxProxyPool.BasicInfo;
import org.moboxlab.MoBoxProxyPool.Cache.CacheECS;
import org.moboxlab.MoBoxProxyPool.Tick.TickDeploy;
import org.moboxlab.MoBoxProxyPool.Tick.TickMain;
import org.mossmc.mosscg.MossLib.Object.ObjectCommand;
import org.mossmc.mosscg.MossLib.Object.ObjectLogger;

import java.util.ArrayList;
import java.util.List;

public class CommandStatus extends ObjectCommand {
    @Override
    public List<String> prefix() {
        List<String> prefixList = new ArrayList<>();
        prefixList.add("status");
        return prefixList;
    }

    @Override
    public boolean execute(String[] args, ObjectLogger logger) {
        BasicInfo.logger.sendInfo("MoBoxProxyPool 统计信息");
        BasicInfo.logger.sendInfo("=======================");
        BasicInfo.logger.sendInfo("MainLoop MSPT时间："+ TickMain.mspt);
        BasicInfo.logger.sendInfo("DeployLoop MSPT时间："+ TickDeploy.mspt);
        BasicInfo.logger.sendInfo("代理池数量："+ CacheECS.ecsMap.size());
        BasicInfo.logger.sendInfo("=======================");
        BasicInfo.logger.sendInfo("      代理池信息");
        BasicInfo.logger.sendInfo(" 实例ID | IP地址 | 状态 | session");
        CacheECS.ecsMap.forEach((instanceID, objectECS) -> {
            BasicInfo.logger.sendInfo(instanceID+" | "+objectECS.IP+" | "+objectECS.status+" | "+objectECS.sessionMap.size());
        });
        return true;
    }
}
