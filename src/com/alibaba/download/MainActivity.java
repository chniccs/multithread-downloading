package com.alibaba.download;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

public class MainActivity extends Activity {
	
	private ProgressBar pb0;
	private ProgressBar pb1;
	private ProgressBar pb2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		pb0=(ProgressBar) findViewById(R.id.pb0);
		pb1=(ProgressBar) findViewById(R.id.pb1);
		pb2=(ProgressBar) findViewById(R.id.pb2);
		
	}
	//资源地址
	String path="http://192.168.1.109:8080/myday12/1.zip";
	//点击事件方法
	public void down(View v){
		//开户新线程
		new Thread(){
			public void run() {
				
				//设置线程总数
				int ThreadCount=3;
				try {
					//建立连接
					URL url = new URL(path);
					
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					
					conn.setConnectTimeout(5000);
					
					conn.setRequestMethod("GET");
					
					int code = conn.getResponseCode();
					
					if(code==200){
						
						//拿到文件长度
						int length = conn.getContentLength();
						System.out.println(length+"");
						//拿到文件名
						int i=path.lastIndexOf("/");
						String fileName=path.substring(i);
						
						//创建一个本地的空间用于等待存储此文件
						RandomAccessFile raf = new RandomAccessFile(Environment.getExternalStorageDirectory().getAbsolutePath()+
								fileName,"rw");
						//设置本地空间的大小 
						raf.setLength(length);
						raf.close();
						
						//获取每段文件的大小
						int partSize=length/ThreadCount;
						//根本每段大小开启线程下载
						for(int n=0;n<ThreadCount;n++){
							
							int StartIndex=(n*partSize);
							int EndIndex=((n+1)*partSize-1);
							//最后一段文件大小
							if(n==ThreadCount-1){
								
								EndIndex=length-1;
							}
							
							int ThreadId=n;
							System.out.println(n+"开始下载了");
							//开启下载线程
							new DownLoad(ThreadId,StartIndex,EndIndex).start();
						}
						
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
			
		}.start();
	}
	
	private int runningThread=3;
	private int threadCount=3;
	private class DownLoad extends Thread{
		
		private int ThreadId;
		private int StartIndex;
		private int EndIndex;
		
		
		public DownLoad(int ThreadId,int StartIndex,int EndIndex){
			
			this.ThreadId=ThreadId;
			this.StartIndex=StartIndex;
			this.EndIndex=EndIndex;
			
		}
		@Override
		public void run() {
			
			try {
				URL url = new URL(path);
				
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				
				conn.setConnectTimeout(5000);
				
				conn.setRequestMethod("GET");
				
				
				//通过RandomAccessFile类来创建一个文件,其可以设置文件从哪里开始写
				
				RandomAccessFile raf = new RandomAccessFile(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+
				getFileName(),"rwd");
				
				//此处是验证之前是否有下载文件,有的话会生成一个名为:线程id.position的文件
				File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ThreadId + ".position");
				//创建一个变量currentPosition,其用于获得之前写入的位置信息,默认初始值是文件的起始位置
				int currentPosition=StartIndex;
				//判断文件是否存在,若存在说明此前已经下载过,不存在则从头开始下载
				if(file.exists()&&file.length()>0){
					
					BufferedReader br = new BufferedReader(new FileReader(file));
					//获得当前的位置的字符串信息
					String vl = br.readLine();
					//转换成int类型数据
					currentPosition = Integer.parseInt(vl);
					//设置获得数据的位置
					conn.setRequestProperty("range", "bytes=" + currentPosition
							+ "-" + EndIndex);
					//设置开始写入的位置
					raf.seek(currentPosition);
				}else{
					
					conn.setRequestProperty("range", "bytes=" + StartIndex
							+ "-" + EndIndex);
					raf.seek(StartIndex);
				}
				
				int code = conn.getResponseCode();
				//此处状态码是206
				if(code==206){
					
					InputStream in = conn.getInputStream();
					
					int len=-1;
					
					byte[] buf=new byte[1024];
					
					while((len=in.read(buf))>0){
						
						raf.write(buf, 0, len);
						
						currentPosition+=len;
						//创建临时文件,用于实时存储文件写入到哪里了
						RandomAccessFile raff = new RandomAccessFile(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ThreadId+".position", "rwd");
						//记录当前写入位置
						raff.write((currentPosition + "").getBytes());
						raff.close();
						
						//设置进度条信息
						int max=EndIndex-StartIndex;
						int progress=currentPosition-StartIndex;
						//判断是哪个线程的进度条
						if(ThreadId==0){
							pb0.setMax(max);
							pb0.setProgress(progress);
							
						}else if(ThreadId==1){
							
							pb1.setMax(max);
							pb1.setProgress(progress);
							
						}else if(ThreadId==2){
							
							pb2.setMax(max);
							pb2.setProgress(progress);
							
						}
					}
					in.close();
					raf.close();
				}
				System.out.println(ThreadId+"下载完成了");
				
				//执行到此处说明文件下载已经 完成了
				//重命名临时文件
				File finish=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ThreadId+".position");
				finish.renameTo(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ThreadId+".fhinish"));
				
				//删除临时文件
				synchronized(MainActivity.class){
					//此处用于记录还在执行的线程数
					runningThread--;
					//当线程都执行完成后才删除文件
					if(runningThread<=0){
						for(int i=0;i<threadCount;i++){
							File temp=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+i+".finish");
							temp.delete();
							System.out.println("删除成功"+i);
						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		//创建一个文件用于获得文件名
		private String getFileName() {
			int i=path.lastIndexOf("/");
			String fileName=path.substring(i);
			return fileName;
		}
		
	}
	

}
