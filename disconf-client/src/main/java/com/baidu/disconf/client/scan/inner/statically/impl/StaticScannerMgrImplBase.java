package com.baidu.disconf.client.scan.inner.statically.impl;

import com.baidu.disconf.client.common.model.DisConfCommonModel;
import com.baidu.disconf.client.config.DisClientConfig;

/**
 * @author liaoqiqi
 * @version 2014-9-9
 */
public class StaticScannerMgrImplBase {

    /**
     * env/version 默认是应用整合设置的，但用户可以在配置中更改它
     */
    protected static DisConfCommonModel makeDisConfCommonModel(String env, String version) {

        DisConfCommonModel disConfCommonModel = new DisConfCommonModel();

        // app
        disConfCommonModel.setApp(DisClientConfig.getInstance().APP);

        // env
        if (!env.isEmpty()) {
            disConfCommonModel.setEnv(env);
        } else {
            disConfCommonModel.setEnv(DisClientConfig.getInstance().ENV);
        }

        // version
        if (!version.isEmpty()) {
            disConfCommonModel.setVersion(version);
        } else {
            disConfCommonModel.setVersion(DisClientConfig.getInstance().VERSION);
        }

        return disConfCommonModel;
    }
    protected static String buildZkFilePath(String zkPrefix,String app,String version,String env,String filename) {
    	 StringBuffer sb = new StringBuffer();
    	 if (zkPrefix!=null && zkPrefix.length()>0) {
    		 sb.append(zkPrefix);
    	 }
    	 if (app==null || app.length()<=0) {
    		 return null;
    	 }
    	 sb.append(app);
    	 if (version!=null && version.length()>0) {
    		 sb.append("_");
    		 sb.append(version);
    	 }
    	 
    	 if (env!=null && env.length()>0) {
    		 sb.append("_");
    		 sb.append(env);
    	 }
    	 sb.append("/");
    	 if (filename==null || filename.length()<=0) {
    		 return null;
    	 }
    	 sb.append(filename);
    	 return sb.toString();
    	 
    }
    protected static String buildDBZkFilePath(String dbpath,String filename) {
    	StringBuffer sb = new StringBuffer();
    	if (dbpath!=null && dbpath.length()>0) {
    		sb.append(dbpath);
    	}
    	sb.append("/");
    	if (filename==null||filename.length()<=0) {
    		return null;
    	} 
    	sb.append(filename);
    	return sb.toString();
    }

}
