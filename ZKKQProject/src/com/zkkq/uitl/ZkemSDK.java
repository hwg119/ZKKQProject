package com.zkkq.uitl;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

public class ZkemSDK 
{
	private static ActiveXComponent zkem ;
	
	public void initSTA(){
		//静态加载zkemkeeper.dll,  zkemkeeper.ZKEM.1为注册表中的ProgID数值
		//构建ActiveX组件实例
		zkem = new ActiveXComponent("zkemkeeper.ZKEM.1");
		ComThread.InitSTA();//调用初始化并放入内存中等待调用new ActiveXComponent("zkemkeeper.ZKEM.1")
	}
	
	public void release(){
		//释放占用的内存空间但是这样调用会出现一个严重的问题，当访问量过大时，初始化的内存太多而不能及时释放，这样就会导致内存溢出，这个应用程序就会崩溃，最好还得重新启动服务，重新发布项目。
		//长连接本来原本就是在Windows平台上运行的，但是经过一些技术加工之后，在Java中也能够调用，此问题就出现了。解决的方法还是有的，Net开发webService调用，然后生成Java客户端代码，
		//再用java调用，这样问题就解决了，而且效率也很好，使用方便。
		ComThread.Release();
	}

	/**
	 * 连接到考勤机
	 * @param address
	 * @param port
	 * @return
	 * @throws Exception 
	 */
	public boolean connect(String address,int port, int machineNum) throws Exception{
		//连接考勤机，返回是否连接成功，成功返回true，失败返回false。
		//1、Connect_NET：zkem中方法，通过网络连接中控考勤机。
		//2、address：中控考勤机IP地址。
		//3、port：端口号
		boolean result = zkem.invoke("Connect_NET",address,port).getBoolean();
		if(result){
			try {
				
				String sns = ResourceUtils.getResourceByKey("SerialNumber");
				//encrypt：MD5加密方法，传入中控考勤机编号，进行MD5加密，返回加密机器编号
				String sn = encrypt(getSerialNumber(machineNum));
				//加密机器编号，是否与读取的编号一致
				if(sns.contains(sn)){
					result = true;
				}else{
					throw new Exception("序列码异常!");
				}
			} catch (Exception e) {
				throw new Exception("序列码异常!");
			} 
			
		}
		return result;
	}
	
	/**
	 * 断开考勤机连接
	 */
	public void disConnect(){
		zkem.invoke("Disconnect");
	}
	
	/**
	 * 读取考勤所有数据到缓存中。配合getGeneralLogData使用。
	 * @return
	 */
	public boolean readGeneralLogData(int machineNum){
		//调用zkem中的ReadGeneralLogData方法，传入参数，机器号
		boolean result = zkem.invoke("ReadGeneralLogData",new Variant[]{new Variant(machineNum)}).getBoolean();
		return result;
	}
	
	/**
	 * 读取该时间之后的最新考勤数据。 配合getGeneralLogData使用。
	 * @param lastest
	 * @return
	 */
	public boolean readLastestLogData(int machineNum,Date lastest){
		Calendar c = Calendar.getInstance();
		c.setTime(lastest);
		
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH) + 1;
		int day = c.get(Calendar.DAY_OF_MONTH) ;
		int hours = c.get(Calendar.HOUR_OF_DAY);
		int minutes = c.get(Calendar.MINUTE);
		int seconds = c.get(Calendar.SECOND);
		
		
		Variant v0 = new Variant(machineNum);
		Variant vLog = new Variant(1);
		Variant dwYear = new Variant(year,true);
		Variant dwMonth = new Variant(month,true);
		Variant dwDay = new Variant(day,true);
		Variant dwHour = new Variant(hours,true);
		Variant dwMinute = new Variant(minutes,true);
		Variant dwSecond = new Variant(seconds,true);
		
		boolean result = zkem.invoke("ReadLastestLogData",v0,vLog,dwYear,dwMonth,dwDay,dwHour,dwMinute,dwSecond).getBoolean();
		return result;
	}
	
	/**
	 * 获取缓存中的考勤数据。配合readGeneralLogData / readLastestLogData使用。
	 * @return 返回的map中，包含以下键值：
	 	"EnrollNumber"   人员编号
		"Time"           考勤时间串，格式: yyyy-MM-dd HH:mm:ss
		"VerifyMode"    验证方式：0为密码验证，1为指纹验证，2为卡验 ...
		"InOutMode"     默认 0—Check-In   1—Check-Out  2—Break-O 3—Break-In   4—OT-In   5—OT-Out 
	 */
	public List<Map<String,Object>> getGeneralLogData(int machineNum){
		Variant v0 = new Variant(machineNum);
		Variant dwEnrollNumber = new Variant("",true);
		Variant dwVerifyMode = new Variant(0,true);
		Variant dwInOutMode = new Variant(0,true);
		Variant dwYear = new Variant(0,true);
		Variant dwMonth = new Variant(0,true);
		Variant dwDay = new Variant(0,true);
		Variant dwHour = new Variant(0,true);
		Variant dwMinute = new Variant(0,true);
		Variant dwSecond = new Variant(0,true);
		Variant dwWorkCode = new Variant(0,true);
		List<Map<String,Object>> strList = new ArrayList<Map<String,Object>>();
		boolean newresult = false;
		do{
			Variant vResult = Dispatch.call(zkem, "SSR_GetGeneralLogData", v0,dwEnrollNumber,dwVerifyMode,dwInOutMode,dwYear,dwMonth,dwDay,dwHour,
					dwMinute,dwSecond,dwWorkCode);	
			newresult = vResult.getBoolean();
			if(newresult)
			{
				String enrollNumber = dwEnrollNumber.getStringRef();
				
				//如果没有编号，则跳过。
				if(enrollNumber == null || enrollNumber.trim().length() == 0)
					continue;
				Map<String,Object> m = new HashMap<String, Object>();
				m.put("EnrollNumber", enrollNumber);
				m.put("Time", dwYear.getIntRef() + "-" + dwMonth.getIntRef() + "-" + dwDay.getIntRef() + " " + dwHour.getIntRef() + ":" + dwMinute.getIntRef() + ":" + dwSecond.getIntRef());
				m.put("VerifyMode", dwVerifyMode.getIntRef());
				m.put("InOutMode", dwInOutMode.getIntRef());
				strList.add(m);
			}
		}while(newresult == true);
		return strList;
	}
	
	/**
	 * 获取考勤机序列码
	 */
	public String getSerialNumber(int machineNum){
		//Variant：变体类型，能够在运行期间动态的改变类型。
		//变体类型能支持所有简单的数据类型，如整型、浮点、字符串、布尔型、日期时间、货币及OLE自动化对象等，不能够表达Object Pascal对象。
		Variant v0 = new Variant(machineNum);
		Variant dwSerialNumber = new Variant("",true);
		//zkem.invoke：调用Method类代表的方法，此处表明调用zkem中的GetSerialNumber方法，
		boolean result = zkem.invoke("GetSerialNumber",v0,dwSerialNumber).getBoolean();
		if(result){
			return dwSerialNumber.getStringRef();
		}
		return null;
	}
	
	/**
	 * MD5加密
	 * @return
	 */
	public String encrypt(String s) throws Exception{
		char hexDigits[]={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};       
        byte[] btInput = s.getBytes();
        MessageDigest mdInst;
		mdInst = MessageDigest.getInstance("MD5");
        mdInst.update(btInput);
        byte[] md = mdInst.digest();
        int j = md.length;
        char str[] = new char[j * 2];
        int k = 0;
        for (int i = 0; i < j; i++) {
            byte byte0 = md[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(str);
	}
}
