package com.baidu.disconf.client.fetcher.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.fetcher.FetcherMgr;
import com.baidu.disconf.client.store.DisconfStoreProcessor;
import com.baidu.disconf.client.store.DisconfStoreProcessorFactory;
import com.baidu.disconf.core.common.utils.ClassLoaderUtil;
import com.baidu.disconf.core.common.utils.MyStringUtils;
import com.baidu.disconf.core.common.utils.OsUtil;
import com.baidu.disconf.core.common.zookeeper.ZookeeperMgr;

public class FetcherMgrZkImpl implements FetcherMgr{
	protected static final Logger LOGGER = LoggerFactory.getLogger(FetcherMgrZkImpl.class);
    // 获取远程配置 重试次数
    private int retryTime = 3;

    // 获取远程配置 重试时休眠时间
    private int retrySleepSeconds = 5;

    // 下载的文件会被迁移到classpath根路径下
    private boolean enableLocalDownloadDirInClassPath = true;

    // 下载文件夹, 远程文件下载后会放在这里
    private String localDownloadDir;

    // temp 临时目录
    private String localDownloadDirTemp;
    // 仓库算子
    private DisconfStoreProcessor disconfStoreProcessor = DisconfStoreProcessorFactory.getDisconfStoreFileProcessor();
    
    //
    // 创建对象
    //
    public FetcherMgrZkImpl(int retryTime, int retrySleepSeconds,
                          boolean enableLocalDownloadDirInClassPath, String localDownloadDir, String
                                  localDownloadDirTemp) {

        this.retrySleepSeconds = retrySleepSeconds;
        this.retryTime = retryTime;
        this.enableLocalDownloadDirInClassPath = enableLocalDownloadDirInClassPath;
        this.localDownloadDir = localDownloadDir;
        this.localDownloadDirTemp = localDownloadDirTemp;
        OsUtil.makeDirs(this.localDownloadDir);
    }
    
    public String getValueFromServer(String url) throws Exception {
    	return "";
    }
    
    
    public String downloadFileFromServer(String url, String fileName, String targetFileDir) throws Exception {
    	
    	String localDir = getLocalDownloadDirPath();
    	return downloadFromServer(url,fileName,localDir,localDownloadDirTemp,targetFileDir,
    			enableLocalDownloadDirInClassPath,retryTime,retrySleepSeconds);
    }
    
    
    /**
     * 获取本地下载的路径DIR, 通过参数判断是否是临时路径
     *
     * @throws Exception
     */
    private String getLocalDownloadDirPath() throws Exception {

        String localUrl = localDownloadDir;

        if (!new File(localUrl).exists()) {
            new File(localUrl).mkdirs();
        }

        return localUrl;
    }
    
    @Override
    public void release() {

       System.out.println("release");
    }
    
    private File retryDownload(String localFileDirTemp, String fileName, String zkNode, int retryTimes, int
            retrySleepSeconds)
            throws Exception {

        if (localFileDirTemp == null) {
            localFileDirTemp = "./disconf/download";
        }
        DisconfCenterFile disconfCenterFile = (DisconfCenterFile) disconfStoreProcessor.getConfData(fileName);
        String tmpFilePath = OsUtil.pathJoin(localFileDirTemp, fileName);
        String tmpFilePathUnique = MyStringUtils.getRandomName(tmpFilePath);
        File tmpFilePathUniqueFile = new File(tmpFilePathUnique);
        //retry4ConfDownload(remoteUrl, tmpFilePathUniqueFile, retryTimes, retrySleepSeconds);
        if (!tmpFilePathUniqueFile.exists()) {
        	tmpFilePathUniqueFile.createNewFile();
        }
        FileWriter fw = new FileWriter(tmpFilePathUniqueFile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        String filecontent = ZookeeperMgr.getInstance().readUrl(zkNode, null);
        if (disconfCenterFile.isDbConfFile()) {
        	String itemprefix = disconfCenterFile.getItemPrefix()+".";
            JSONObject jo = new JSONObject(filecontent);
            String port  = jo.getString("port");
            String host = jo.getString("host");
            String username = jo.getString("user");
            String password = jo.getString("password");
            String db = jo.getString("db");
            LOGGER.debug("host:"+host+" port:"+port+" username:"+username+" password:"+password+" db:"+db);
            String url = itemprefix+"url="+"jdbc:mysql://"+host+":"+port+"/"+db+"?"+"zeroDateTimeBehavior=convertToNull";
            bw.write(url);
            bw.newLine();//换行 
            bw.flush();
            String cusername = itemprefix+"username="+username;
            bw.write(cusername);
            bw.newLine();
            bw.flush();
            String cpassword = itemprefix+"password="+password;
            bw.write(cpassword);
            bw.flush();
            bw.close();
        } else {
        	bw.write(filecontent);
        	bw.close();
        }
        return tmpFilePathUniqueFile;
    }
    private File transfer2SpecifyDir(File srcFile, String copy2TargetDirPath, String fileName,
            boolean isMove) throws Exception {

		// make dir
		OsUtil.makeDirs(copy2TargetDirPath);
		
		File targetPath = new File(OsUtil.pathJoin(copy2TargetDirPath, fileName));
		// 从下载文件 复制/mv 到targetPath 原子性的做转移
		OsUtil.transferFileAtom(srcFile, targetPath, isMove);
		return targetPath;

    }
    public String downloadFromServer(String zkNode, String fileName, String localFileDir, String localFileDirTemp,
            String copy2TargetDirPath, boolean enableLocalDownloadDirInClassPath,
            int retryTimes, int retrySleepSeconds)
		throws Exception {
			
			// 目标地址文件
			File localFile = null;
			
			//
			// 进行下载、mv、copy
			//
			
			try {
			
				// 可重试的下载
				File tmpFilePathUniqueFile = retryDownload(localFileDirTemp, fileName, zkNode, retryTimes,
				retrySleepSeconds);
				
				// 将 tmp file copy localFileDir
				localFile = transfer2SpecifyDir(tmpFilePathUniqueFile, localFileDir, fileName, false);
				
				// mv 到指定目录
				if (copy2TargetDirPath != null) {
				
				//
				if (enableLocalDownloadDirInClassPath || !copy2TargetDirPath.equals(ClassLoaderUtil.getClassPath
				())) {
				localFile = transfer2SpecifyDir(tmpFilePathUniqueFile, copy2TargetDirPath, fileName, true);
				}
				}
				
				LOGGER.debug("Move to: " + localFile.getAbsolutePath());
			
			} catch (Exception e) {
			
				LOGGER.warn("download file failed, using previous download file.", e);
			}
			
			//
			// 判断是否下载失败
			//
			
			if (!localFile.exists()) {
				throw new Exception("target file cannot be found! " + fileName);
			}
			
			//
			// 下面为下载成功
			//
			
			// 返回相对路径
			if (localFileDir != null) {
				String relativePathString = OsUtil.getRelativePath(localFile, new File(localFileDir));
				if (relativePathString != null) {
					if (new File(relativePathString).isFile()) {
						return relativePathString;
					}
				}
			}
			
			// 否则, 返回全路径
			return localFile.getAbsolutePath();
		}

}
