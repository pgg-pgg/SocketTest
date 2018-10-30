package com.example.pgg.sockettest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{


    EditText edit_ip,edit_port,edit_msg;
    Button btn_send;
    TextView tv_recv_msg;

    Spinner net_type;
    private int TCPC_TYPE=0;
    private int UDP_TYPE=1;
    private int TCPS_TYPE=2;
    private int CURRENT_TYPE;

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            tv_recv_msg.setText(msg.obj+"");
        }
    };


    public static String getIPAddress(Context context){

        NetworkInfo info=((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (info!=null&&info.isConnected()){
            if (info.getType()==ConnectivityManager.TYPE_MOBILE){
                //当前使用2G/3G/4G网络
                try {
                    for (Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();){
                        NetworkInterface networkInterface = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr=networkInterface.getInetAddresses();enumIpAddr.hasMoreElements();){
                            InetAddress inetAddress=enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress()&&inetAddress instanceof Inet4Address){
                                return "非局域网";
                            }
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else if (info.getType()==ConnectivityManager.TYPE_WIFI){
                //当前使用的是WIFI
                WifiManager wifiManager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                String ipAddress=intIP2StringIP(connectionInfo.getIpAddress());//获取ipv4的地址
                return ipAddress;
            }
        }else {
            //当前无网络
            Toast.makeText(context,"当前无网络，请在设置中进行设置！！！",Toast.LENGTH_LONG).show();
            return null;
        }
        return null;
    }

    /**
     * 将得到的int类型的IP转换为String类型
     *
     * @param ip
     * @return
     */
    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        btn_send.setOnClickListener(this);

        net_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CURRENT_TYPE=position;
                if (position==2){
                    edit_ip.setText(getIPAddress(MainActivity.this));
                    edit_ip.setEnabled(false);
                    btn_send.setText("开启服务器");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                CURRENT_TYPE=0;
            }
        });
    }

    private void initTCPSSocket(int port){
        try {
            ServerSocket serverSocket=new ServerSocket(port);
            while (true){
                Socket accept = serverSocket.accept();
                new Thread(new ClientHandler(accept)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String initTCPCSocket(String ip, int port, String msg) {
        try {
            Socket socket=new Socket(ip,port);
            OutputStream outputStream=socket.getOutputStream();
            outputStream.write(msg.getBytes());
            BufferedReader br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            socket.shutdownOutput();
            //接收服务器的相应
            String reply=null;
            while(!((reply=br.readLine())==null)){
                String recv_msg="["+socket.getInetAddress()+"  "+System.currentTimeMillis()+"]: "+reply;
                return recv_msg;
            }
            return "未收到消息";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String initUDPSocket(String ip,int port,String msg){
        try {
            DatagramSocket datagramSocket=new DatagramSocket();
            InetAddress address=InetAddress.getByName(ip);
            while (!TextUtils.isEmpty(msg)) {
                DatagramPacket packet = new DatagramPacket(msg.getBytes(),
                        msg.length(), address, port);
                datagramSocket.send(packet);

                DatagramPacket inputPacket = new DatagramPacket(new byte[512], 512);
                datagramSocket.receive(inputPacket);
                String s=new String(inputPacket.getData(), 0, inputPacket.getLength());
                System.out.println(new String(inputPacket.getData(), 0, inputPacket.getLength()));
                datagramSocket.close();
                return s;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initView() {
        edit_ip=findViewById(R.id.edit_ip);
        edit_msg=findViewById(R.id.edit_msg);
        edit_port=findViewById(R.id.edit_port);
        btn_send=findViewById(R.id.btn_send);
        tv_recv_msg=findViewById(R.id.tv_recv_msg);
        net_type=findViewById(R.id.net_type);
        CURRENT_TYPE=0;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_send:
                final String ip=edit_ip.getText().toString();
                final String port=edit_port.getText().toString();
                final String msg=edit_msg.getText().toString();
                if (TextUtils.isEmpty(ip)||TextUtils.isEmpty(port)||TextUtils.isEmpty(msg)){
                    Toast.makeText(this,"请填写完整信息!",Toast.LENGTH_LONG).show();
                }else {
                    if (CURRENT_TYPE==0){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String s = initTCPCSocket(ip, Integer.valueOf(port), msg);
                                Message message=new Message();
                                message.what=1;
                                message.obj=s;
                                handler.sendMessage(message);
                            }
                        }).start();
                    }else if (CURRENT_TYPE==1){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String s=initUDPSocket(ip,Integer.valueOf(port),msg);
                                Message message=Message.obtain();
                                message.what=2;
                                message.obj=s;
                                handler.sendMessage(message);
                            }
                        }).start();
                    }else {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                initTCPSSocket(Integer.valueOf(port));
                            }
                        }).start();

                    }
                }
                break;
        }
    }

    private class ClientHandler implements Runnable{
        private Socket client;
        public ClientHandler(Socket client) {
            this.client=client;
        }
        @Override
        public void run() {
            // TODO Auto-generated method stub
            try(BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(client.getInputStream()))){
                PrintWriter pWriter=new PrintWriter(client.getOutputStream());
                String msg=bufferedReader.readLine();
                System.out.println(msg+"由"+client.getInetAddress()+"发出");
                Message message=Message.obtain();
                message.what=3;
                message.obj=msg;
                handler.sendMessage(message);
                pWriter.println(msg);
                pWriter.flush();
            }catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }finally {
                try {
                    client.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}
