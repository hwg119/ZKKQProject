package com.zkkq.uitl;

public class TestMD {
	
	public static void main(String[] args) throws Exception {
		String str = "0218141400200";
		ZkemSDK sdk = new ZkemSDK();
		String sn = sdk.encrypt(str);
		System.out.println(sn);
		
		String sns = ResourceUtils.getResourceByKey("SerialNumber");
		if(sns.contains(sn)){
			System.out.println("存在");
		}else{
			System.out.println("不存在");
		}
	}
}
