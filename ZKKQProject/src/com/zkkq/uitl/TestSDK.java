package com.zkkq.uitl;

public class TestSDK {
	
	public static void main(String[] args) {
		ZkemSDK sdk = new ZkemSDK();
		sdk.initSTA();
		try{
			boolean  flag = sdk.connect("192.168.1.201", 4370,1);
			System.out.println(flag);
		}catch(Exception e){
			e.printStackTrace();
		}
		sdk.disConnect();
		sdk.release();
	}
}
