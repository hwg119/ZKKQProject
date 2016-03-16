package com.zkkq.service;

import org.apache.log4j.Logger;

public class ZKKQService {
	
	private static Logger logger = Logger.getLogger(ZKKQService.class);
	
	public static void main(String[] args) {
		ZKKQJob job = new ZKKQJob();
		String job_name ="zkkkq_job"; 
		try {  
			logger.info("【定时同步考勤数据系统启动】");
			/**
			 * 每天23:30触发  0 30 23 * * ?
			 * 每分钟 0 0/1 * * * ?
			 * 每 2个小时执行 0 0 0/2 * * ?
			 */
            QuartzManager.addJob(job_name,job,"0 0/1 * * * ?");
		}catch (Exception e) {
			logger.info("【定时同步考勤数据系统启动失败】");
		}
	}
}
