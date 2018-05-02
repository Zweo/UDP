package android.tcpudp;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    private final static String SERVER_ADDRESS = "192.168.137.1";
    private final static int PORT = 1985;
    private ListView mConversationView;
    private EditText mOutEditText;
    private EditText mServerAddress;
    private Button mSendButton;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private UDPSocket RecvUdp;
    private UDPSocket SendUdp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupChat();
    }

    private void setupChat(){
        RecvUdp = new UDPSocket();       // 监听数据，阻塞状态
        RecvUdp.setRecv_Flag(true);
        RecvUdp.start();
        mConversationArrayAdapter=new ArrayAdapter<String>(this, R.layout.list_item);
        mConversationView=(ListView)findViewById(R.id.list_conversation);
        mConversationView.setAdapter(mConversationArrayAdapter);
        mOutEditText=(EditText)findViewById(R.id.edit_text_out);
        mServerAddress = (EditText)findViewById(R.id.edit_server_address);
        mServerAddress.setText(SERVER_ADDRESS);
        mSendButton = (Button)findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = mOutEditText.getText().toString();
                RecvUdp.setRecv_Flag(false);  // 退出监听模式
                SendUdp = new UDPSocket();
                SendUdp.start();
                SendUdp.write(message);    // 发送完毕线程即退出
            }
        });
    }

    private class UDPSocket extends Thread{
        private DatagramSocket mm_socket;
        private byte[] mm_data;
        private boolean Send_Flag = false;
        private boolean Recv_Flag = false;

        UDPSocket(){
            if(mm_socket == null){
                try {
                    mm_socket = new DatagramSocket(null);
                    mm_socket.setReuseAddress(true);
                    mm_socket.bind(new InetSocketAddress(PORT));
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

        public  void setRecv_Flag(boolean recv_Flag){
            Recv_Flag = recv_Flag;
        }

        public void write(String command){
            mm_data = command.getBytes();
            Send_Flag = true;
        }

        private void SendDataToServer() {
            try{
                InetAddress serverAddress = InetAddress.getByName(mServerAddress.getText().toString());
                DatagramPacket packet = new DatagramPacket(mm_data,mm_data.length,
                        serverAddress,80);
                mm_socket.send(packet);//把数据发送到服务端。 
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE,packet.getLength(),
                        -1,packet.getData()).sendToTarget();//   
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

    private final Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case MESSAGE_WRITE:
                    byte[]writeBuf =(byte[])msg.obj;
                    String writeMessage=new String(writeBuf);
                    mConversationArrayAdapter.add("我： " + writeMessage);
                    RecvUdp = new UDPSocket();       // 重新开始监听
                    RecvUdp.setRecv_Flag(true);
                    RecvUdp.start();
                    break;
                case MESSAGE_READ:
                    byte[]readBuf =(byte[])msg.obj;
                    String readMessage=new String(readBuf,0,msg.arg1);
                    mConversationArrayAdapter.add("服务器"+": " +readMessage);
                    break;
            }
        }
    };

}
