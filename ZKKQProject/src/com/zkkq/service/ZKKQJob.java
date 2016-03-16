package com.zkkq.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.zkkq.dao.ZKKQDao;
import com.zkkq.dao.impl.ZKKQDaoImpl;
import com.zkkq.uitl.ZkemSDK;

public class ZKKQJob implements Job{
	
	private ZKKQDao dao;
	
	private Logger logger = Logger.getLogger(ZKKQJob.class);
	private static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**
	 * 初始化启动
	 */
	private void initApp(){
		try{
			ApplicationContext context = new ClassPathXmlApplicationContext("ApplicationContext.xml");  
		    dao = (ZKKQDaoImpl)context.getBean("dao");
		    logger.info("准备环境成功!");
		}catch (Exception e) {
			logger.error("准备环境失败:"+e.getMessage());
		}
	}
	
	/**
	 * 执行
	 * @param args
	 */
	private void executeApp(){
		try{
			ZkemSDK sdk = new ZkemSDK();
			Date date = getLastDate(new Date());
			
			int count = 0;
			int machineNum = 0;
			boolean flag = false;
			List<Map<String,Object>> resultList = null;
			
			String enrollNumber;
			String signTime;
			int inOutMode;
			int verifyMode;
			
			sdk.initSTA();
			List<Map<String,Object>> equipList = dao.listEquips(); //查询设备
			for (Map<String, Object> equip : equipList) {
				count = 0;
				flag = false;
				logger.info("开始连接考勤机"+equip.get("ip"));
				machineNum = Integer.parseInt(equip.get("num")+"");
				try{
					flag = sdk.connect(equip.get("ip")+"", Integer.parseInt(equip.get("port")+""),machineNum);//连接考勤机
				}catch(Exception e){
					logger.error("连接考勤机"+equip.get("ip")+"失败:"+e.getMessage());
					continue;
				}
				if(flag){
					try{
						flag = sdk.readLastestLogData(machineNum,date);//读取数据到缓存中
						resultList = sdk.getGeneralLogData(machineNum);//读取该时间之后的最新考勤数据
						logger.info(equip.get("ip")+" 数据记录条数："+resultList.size());
						
						//解析并插入数据
						for(Map<String, Object> map : resultList){
							enrollNumber = map.get("EnrollNumber")+"";
							signTime = SDF.format(SDF.parse(map.get("Time")+""));
							inOutMode = Integer.parseInt(map.get("InOutMode")+"");
							verifyMode = Integer.parseInt(map.get("VerifyMode")+"");
							try{
								if(dao.searchRec(enrollNumber, signTime)==0){
									dao.insertRec(enrollNumber, signTime, inOutMode, verifyMode);
									count++;
								}
							}catch (Exception e) {
								logger.info(equip.get("ip")+" 保存数据失败： "+e.getMessage());
							}
						}
						logger.info(equip.get("ip")+"处理结束,成功数据:"+count);
					}catch(Exception e){
						logger.info("读取考勤机 "+equip.get("ip")+" 数据失败!");
						continue;
					}
				}else{
					logger.info("连接考勤机 "+equip.get("ip")+" 失败!");
				}
			}
			sdk.disConnect();
			sdk.release();
		}catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
	
	@Override
	public void execute(JobExecutionContext jec) throws JobExecutionException {
		initApp();
		executeApp();
	}
	
	public static void main(String[] args) {
		ZKKQJob job = new ZKKQJob();
		job.initApp();
		job.executeApp();
	}
	
	/**
	 * 得到前一天数据
	 */
	private static Date getLastDate(Date date){
		Calendar calendar = Calendar.getInstance();		
		calendar.setTime(date);		
		calendar.add(Calendar.DAY_OF_MONTH, -1);		
		date = calendar.getTime();		
		return date;
	}
}
