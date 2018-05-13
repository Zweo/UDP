package android.tcpudp;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    private ListView mConversationView;
    private EditText mOutEditText;
    private EditText mServerAddress;
    private Button mSendButton;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private UDPSocket udpSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupChat();
    }

    private void setupChat(){
        udpSocket = new UDPSocket(mHandler,1985);
        udpSocket.startRecv();         // 开始监听
        // 界面初始化
        String address = "192.168.137.1:80";
        mConversationArrayAdapter=new ArrayAdapter<String>(this, R.layout.list_item);
        mConversationView=(ListView)findViewById(R.id.list_conversation);
        mConversationView.setAdapter(mConversationArrayAdapter);
        mOutEditText=(EditText)findViewById(R.id.edit_text_out);
        mServerAddress = (EditText)findViewById(R.id.edit_server_address);
        mServerAddress.setText(address);
        mSendButton = (Button)findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {               
                String message = mOutEditText.getText().toString();
                String [] addr = mServerAddress.getText().toString().split(":");
                udpSocket.Send(message,addr[0],Integer.parseInt(addr[1]));
            }
        });
    }

    // handler用于Activity之间传递消息
    private final Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case MESSAGE_WRITE:
                    byte[]writeBuf =(byte[])msg.obj;
                    String writeMessage=new String(writeBuf);
                    mConversationArrayAdapter.add("我： " + writeMessage);
                    udpSocket.startRecv();
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