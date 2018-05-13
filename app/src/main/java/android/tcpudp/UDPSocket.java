package android.tcpudp;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;


public class UDPSocket {
    private int LocalPort = 80;
    private final Handler mHandler;
    private UDPThread SocketRecv;

    UDPSocket(Handler handler, int localport){
        LocalPort = localport;
        mHandler = handler;
        if(localport <= 1024){
            Log.e("UDP", "UDPSocket:端口号小于1024 ");
        }
    }

    public void startRecv(){      // 监听端口
        SocketRecv = new UDPThread();
        SocketRecv.setRecv_Flag(true);
        SocketRecv.start();
    }

    public void Send(String message,String address,int port){
        SocketRecv.setRecv_Flag(false);
        UDPThread SocketSend = new UDPThread();
        SocketSend.write(message,address,port);
        SocketSend.start();
    }

    private class UDPThread extends Thread{
        private DatagramSocket mm_socket;
        private byte[] mm_data;
        private boolean Send_Flag = false;
        private boolean Recv_Flag = false;
        private String des_Address;
        private int desPort;

        UDPThread( ){   // 本地端口
            if(mm_socket == null){
                try {
                    mm_socket = new DatagramSocket(null);
                    mm_socket.setReuseAddress(true);
                    mm_socket.bind(new InetSocketAddress(LocalPort));
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override public void run() {
            if(Send_Flag){
                SendDataToServer();
                Send_Flag = false;
            }
            while (Recv_Flag) {
                ReceiveServerSocketData();
            }
        }

        private   void setRecv_Flag(boolean recv_Flag){
            Recv_Flag = recv_Flag;
        }

        private void write(String command,String address,int port){
            des_Address = address;
            desPort     = port;
            mm_data     = command.getBytes();
            Send_Flag   = true;
        }

        private void SendDataToServer() {
            try{
                InetAddress serverAddress = InetAddress.getByName(des_Address);
                DatagramPacket packet = new DatagramPacket(mm_data,mm_data.length,
                        serverAddress,desPort);
                mm_socket.send(packet);//把数据发送到服务端。 
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE,packet.getLength(),
                        -1,packet.getData()).sendToTarget();
            }catch(SocketException e){
                e.printStackTrace();
            } catch(IOException e){
                e.printStackTrace();
            }
        }

        private void ReceiveServerSocketData() {
            try {
                //实例化的端口号要和发送时的socket一致，否则收不到data  
                byte data[]=new byte[4*1024];
                //参数一:要接受的data 参数二：data的长度  
                DatagramPacket packet = new DatagramPacket(data,data.length);
                mm_socket.receive(packet);
                mHandler.obtainMessage(MainActivity.MESSAGE_READ,packet.getLength(),
                        -1,packet.getData()).sendToTarget();
            }catch(SocketException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    // 使用TCP协议通信
    protected void connectServerWithTCPSocket(String msg,String addrServer,int localPort) {
        Socket socket;
        try {
            socket = new Socket(addrServer, localPort);
            // 获取Socket的OutputStream对象用于发送数据。  
            OutputStream outputStream = socket.getOutputStream();
            byte buffer[] = msg.getBytes();
            // 发送读取的数据到服务端  
            outputStream.flush();
        }  catch (UnknownHostException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    // TCP服务器
    public void ServerReceviedByTcp() {
        // 声明一个ServerSocket对象  
        ServerSocket serverSocket = null;
        try{
            // 创建一个ServerSocket对象，并让这个Socket在1989端口监听  
            serverSocket = new ServerSocket(1989);
            // 调用ServerSocket的accept()方法，接受客户端所发送的请求，  
            // 如果客户端没有发送数据，那么该线程就停滞不继续  
            Socket socket = serverSocket.accept();
            // 从Socket当中得到InputStream对象  
            InputStream inputStream = socket.getInputStream();
            byte buffer[] = new byte[1024 * 4];
            int temp = 0;
            // 从InputStream当中读取客户端所发送的数据  
            while ((temp = inputStream.read(buffer)) != -1) {
                System.out.println(new String(buffer, 0, temp));
            }
            serverSocket.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
