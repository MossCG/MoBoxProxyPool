package org.moboxlab.MoBoxProxyPool.SSH;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import org.moboxlab.MoBoxProxyPool.BasicInfo;
import org.moboxlab.MoBoxProxyPool.Object.ObjectECS;

import java.io.*;

public class SSHMain {
    public static void asyncInit(ObjectECS ecs) {
        Thread thread = new Thread(() -> initServer(ecs));
        thread.start();
    }

    public static boolean initServer(ObjectECS ecs) {
        ecs.status = BasicInfo.ECSStatus.INSTALLING;
        try {
            //初始化连接
            BasicInfo.logger.sendInfo(ecs.instanceID+"尝试部署中......");
            Session session = ecs.getSSHSession();
            if (session == null) {
                BasicInfo.sendDebug(ecs.instanceID+"建立SSH链接失败！");
                ecs.status = BasicInfo.ECSStatus.CREATED;
                return false;
            }
            boolean deployed = verifyDeployed(session);
            if (deployed) {
                ecs.status = BasicInfo.ECSStatus.RUNNING;
                ecs.available = true;
                BasicInfo.logger.sendInfo(ecs.instanceID+"已部署！跳过部署程序！");
                return true;
            }
            BasicInfo.logger.sendInfo(ecs.instanceID+"未部署！执行部署程序！");
            execCommand(session,"yum install openssl -y");
            execCommand(session,"yum install squid -y");
            execCommand(session,"yum install net-tools -y");
            execCommand(session,"yum install httpd-tools -y");
            execCommand(session,"touch /etc/squid/passwd && chown squid /etc/squid/passwd");
            execCommand(session,"htpasswd -b -c /etc/squid/passwd "+ecs.proxyAccount+" "+ecs.proxyPassword);
            execCommand(session,"sudo systemctl stop firewalld");
            uploadFile(session,"/etc/squid/","squid.conf","./MBProxyPool/squid.conf");
            execCommand(session,"rm /etc/hosts -f");
            uploadFile(session,"/etc/","hosts","./MBProxyPool/hosts");
            execCommand(session,"systemctl start squid");
            execCommand(session, "mkdir /mbpp");
            ecs.status = BasicInfo.ECSStatus.RUNNING;
            ecs.available = true;
            BasicInfo.logger.sendInfo(ecs.instanceID+"部署成功！");
            return true;
        } catch (Exception e) {
            BasicInfo.logger.sendException(e);
        }
        BasicInfo.logger.sendWarn(ecs.instanceID+"部署失败，等待重试！");
        ecs.status = BasicInfo.ECSStatus.CREATED;
        return false;
    }

    public static void execCommand(Session session, String command) throws Exception{
        ChannelExec exec = (ChannelExec) session.openChannel("exec");
        exec.setCommand(command);
        exec.connect();
        InputStream inputStream = exec.getInputStream();
        String result;
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        while ((result = in.readLine()) != null) {
            BasicInfo.sendDebug(result);
        }
    }

    public static void uploadFile(Session session,String directory, String fileName, String localFile) throws Exception{
        BasicInfo.sendDebug("upload file: "+fileName);
        File file = new File(localFile);
        ChannelSftp channelSftp= (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
        channelSftp.cd(directory);
        FileInputStream fileInputStream = new FileInputStream(file);
        channelSftp.put(fileInputStream, fileName);
    }

    public static boolean verifyDeployed(Session session) throws Exception{
        String command = "cd /mbpp";
        ChannelExec exec = (ChannelExec) session.openChannel("exec");
        exec.setCommand(command);
        exec.connect();
        InputStream inputStream = exec.getErrStream();
        String result;
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        while ((result = in.readLine()) != null) {
            BasicInfo.sendDebug(result);
            if (result.contains("No")) {
                return false;
            }
        }
        return true;
    }
}
