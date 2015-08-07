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
	//��Դ��ַ
	String path="http://192.168.1.109:8080/myday12/1.zip";
	//����¼�����
	public void down(View v){
		//�������߳�
		new Thread(){
			public void run() {
				
				//�����߳�����
				int ThreadCount=3;
				try {
					//��������
					URL url = new URL(path);
					
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					
					conn.setConnectTimeout(5000);
					
					conn.setRequestMethod("GET");
					
					int code = conn.getResponseCode();
					
					if(code==200){
						
						//�õ��ļ�����
						int length = conn.getContentLength();
						System.out.println(length+"");
						//�õ��ļ���
						int i=path.lastIndexOf("/");
						String fileName=path.substring(i);
						
						//����һ�����صĿռ����ڵȴ��洢���ļ�
						RandomAccessFile raf = new RandomAccessFile(Environment.getExternalStorageDirectory().getAbsolutePath()+
								fileName,"rw");
						//���ñ��ؿռ�Ĵ�С 
						raf.setLength(length);
						raf.close();
						
						//��ȡÿ���ļ��Ĵ�С
						int partSize=length/ThreadCount;
						//����ÿ�δ�С�����߳�����
						for(int n=0;n<ThreadCount;n++){
							
							int StartIndex=(n*partSize);
							int EndIndex=((n+1)*partSize-1);
							//���һ���ļ���С
							if(n==ThreadCount-1){
								
								EndIndex=length-1;
							}
							
							int ThreadId=n;
							System.out.println(n+"��ʼ������");
							//���������߳�
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
				
				
				//ͨ��RandomAccessFile��������һ���ļ�,����������ļ������￪ʼд
				
				RandomAccessFile raf = new RandomAccessFile(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+
				getFileName(),"rwd");
				
				//�˴�����֤֮ǰ�Ƿ��������ļ�,�еĻ�������һ����Ϊ:�߳�id.position���ļ�
				File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ThreadId + ".position");
				//����һ������currentPosition,�����ڻ��֮ǰд���λ����Ϣ,Ĭ�ϳ�ʼֵ���ļ�����ʼλ��
				int currentPosition=StartIndex;
				//�ж��ļ��Ƿ����,������˵����ǰ�Ѿ����ع�,���������ͷ��ʼ����
				if(file.exists()&&file.length()>0){
					
					BufferedReader br = new BufferedReader(new FileReader(file));
					//��õ�ǰ��λ�õ��ַ�����Ϣ
					String vl = br.readLine();
					//ת����int��������
					currentPosition = Integer.parseInt(vl);
					//���û�����ݵ�λ��
					conn.setRequestProperty("range", "bytes=" + currentPosition
							+ "-" + EndIndex);
					//���ÿ�ʼд���λ��
					raf.seek(currentPosition);
				}else{
					
					conn.setRequestProperty("range", "bytes=" + StartIndex
							+ "-" + EndIndex);
					raf.seek(StartIndex);
				}
				
				int code = conn.getResponseCode();
				//�˴�״̬����206
				if(code==206){
					
					InputStream in = conn.getInputStream();
					
					int len=-1;
					
					byte[] buf=new byte[1024];
					
					while((len=in.read(buf))>0){
						
						raf.write(buf, 0, len);
						
						currentPosition+=len;
						//������ʱ�ļ�,����ʵʱ�洢�ļ�д�뵽������
						RandomAccessFile raff = new RandomAccessFile(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ThreadId+".position", "rwd");
						//��¼��ǰд��λ��
						raff.write((currentPosition + "").getBytes());
						raff.close();
						
						//���ý�������Ϣ
						int max=EndIndex-StartIndex;
						int progress=currentPosition-StartIndex;
						//�ж����ĸ��̵߳Ľ�����
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
				System.out.println(ThreadId+"���������");
				
				//ִ�е��˴�˵���ļ������Ѿ� �����
				//��������ʱ�ļ�
				File finish=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ThreadId+".position");
				finish.renameTo(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ThreadId+".fhinish"));
				
				//ɾ����ʱ�ļ�
				synchronized(MainActivity.class){
					//�˴����ڼ�¼����ִ�е��߳���
					runningThread--;
					//���̶߳�ִ����ɺ��ɾ���ļ�
					if(runningThread<=0){
						for(int i=0;i<threadCount;i++){
							File temp=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+i+".finish");
							temp.delete();
							System.out.println("ɾ���ɹ�"+i);
						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		//����һ���ļ����ڻ���ļ���
		private String getFileName() {
			int i=path.lastIndexOf("/");
			String fileName=path.substring(i);
			return fileName;
		}
		
	}
	

}
