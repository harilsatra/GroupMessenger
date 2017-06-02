package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

//import static edu.buffalo.cse.cse486586.groupmessenger2.R.id.editText1;
import static java.lang.Integer.parseInt;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    //Hard-coding the 5 redirection ports.
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;

    int delivery_sequence = 0;
    double proposed_sequence = 1;
    int counter = 0;
    int failed_port = 0;
    int port = 0;
    Comparator<Message> comparator = new MessageComparator();
    public PriorityQueue<Message> Queue = new PriorityQueue(25,comparator);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);


        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());


        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((parseInt(portStr) * 2));
        //msgObject.type = "";

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.editText1);
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                Message msgObject = new Message();
                msgObject.msg = msg;
                msgObject.type = "new_message";
                msgObject.count = counter;
                counter = counter + 1;
                msgObject.sender_port = parseInt(myPort);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgObject);
            }
        });
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try {
                Socket socket;
                ObjectInputStream objFromClient;
                ObjectOutputStream ServerToClient;
                //to keep listening over the socket
                while(true) {
                    //Wait for incoming connections
                    socket = serverSocket.accept();
                    //Read the message over the socket
                    objFromClient = new ObjectInputStream(socket.getInputStream());
                    ServerToClient = new ObjectOutputStream(socket.getOutputStream());
                    Message in_msg = null;
                    try {
                        //Log.v(TAG,"Waiting to get a message");
                        in_msg = (Message) objFromClient.readObject();
                        //Log.v(TAG,"Read the object from client"+in_msg.msg);
                    } catch (ClassNotFoundException e) {
                        Log.e(TAG,"ClassNotFoundException in Server Class");
                        e.printStackTrace();
                    }
                    if (in_msg.type.equals("new_message")) {
                        //Log.v(TAG,"New message received from: "+in_msg.sender_port);
                        in_msg.type = "proposed";
                        in_msg.suggested_seq = proposed_sequence;
                        proposed_sequence = proposed_sequence + ((int )(Math.random() * 100 + 1));
                        in_msg.deliverable = false;
                        Queue.add(in_msg);
                        //Log.v(TAG,Queue.peek().msg+" : "+Queue.peek().suggested_seq);
                        ServerToClient.writeObject(in_msg);
                        ServerToClient.flush();
                        //Log.v(TAG,"Proposed Sequence sent to: "+in_msg.sender_port);
                    }
                    //Log.v(TAG,Queue.size()+"");
                    if(in_msg.failed_port!=0){
                        Iterator<Message> iter = Queue.iterator();
                        while (iter.hasNext()) {
                            Message current = iter.next();
                            // do something with current
                            if(current.sender_port == in_msg.failed_port && current.deliverable == false){
                                /*current.max_agreed = in_msg.max_agreed;
                                current.deliverable = true;*/
                                Queue.remove(current);
                                Log.v(TAG,"Inside if loop");
                            }
                        }
                    }
                    //Log.v(TAG,Queue.size()+"");
                    if( in_msg.type.equals("deliver")) {
                        //Log.v(TAG,"Received Final agreed sequence");
                        if(in_msg.max_agreed >= proposed_sequence){
                            proposed_sequence = in_msg.max_agreed + 1;
                        }
                        //http://stackoverflow.com/questions/13758640/how-should-i-iterate-a-priority-queue-properly
                        Iterator<Message> iter = Queue.iterator();
                        while (iter.hasNext()) {
                            Message current = iter.next();
                            // do something with current
                            if(in_msg.msg.equals(current.msg)){
                                /*current.max_agreed = in_msg.max_agreed;
                                current.deliverable = true;*/
                                Queue.remove(current);
                            }
                        }
                        in_msg.deliverable = true;
                        Queue.add(in_msg);
                        Message ack = new Message();
                        ack.type="ack";
                        ServerToClient.writeObject(ack);
                        ServerToClient.flush();
                        //Log.v(TAG,"Sending Acknowledgement for the deliver message");
                            while(Queue.peek()!=null){
                                if(Queue.peek().deliverable){
                                    Message msg = Queue.poll();
                                    //Publish the results which passes the arguments to onProgressUpdate().
                                    publishProgress(msg.msg);
                                    //Log.v(TAG, "Reading message : " + msg);
                                    //socket.close();
                                    //Following snippet of code to build a URI is taken from OnPTestClickListener.
                                    String scheme = "content";
                                    String authority = "edu.buffalo.cse.cse486586.groupmessenger2.provider";
                                    Uri.Builder uriBuilder = new Uri.Builder();
                                    uriBuilder.authority(authority);
                                    uriBuilder.scheme(scheme);
                                    Uri providerUri = uriBuilder.build();
                                    //Following snippet of code to insert the key value pair into the content provider is taken from PA2 Part A Specification.
                                    ContentValues keyValueToInsert = new ContentValues();
                                    // inserting <”key-to-insert”, “value-to-insert”>
                                    keyValueToInsert.put("key", delivery_sequence); //assign a sequence number to each message it receives.
                                    keyValueToInsert.put("value", msg.msg);
                                    //Log.v(TAG,delivery_sequence+" "+msg.msg);
                                    Uri newUri = getContentResolver().insert(
                                            providerUri, // assume we already created a Uri object with our provider URI
                                            keyValueToInsert
                                    );
                                    delivery_sequence++;
                                }
                                else{
                                    break;
                                }
                            }

                    }
                    objFromClient.close();
                    ServerToClient.close();
                    socket.close();
                }

            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");

            return;
        }
    }

    private class ClientTask extends AsyncTask<Message, Void, Void> {

        @Override
        protected Void doInBackground(Message... msgs) {
            Message msgToSend = msgs[0];

            if(msgToSend.type.equals("new_message")) {
                try {
                    String[] remotePorts = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
                    double max = 0;
                    Socket socket = null;
                    Message proposed_msg=null;
                    ObjectOutputStream objToServer = null;
                    ObjectInputStream objFromServer = null;
                    Message[] proposed_messages = new Message[5];
                    //To send the message to all the avd's
                    for (int i = 0; i < remotePorts.length; i++) {
                        if (failed_port != parseInt(remotePorts[i])) {
                            //Connect to server
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    parseInt(remotePorts[i]));
                            socket.setSoTimeout(500);
                            //Send a message over the socket
                            //http://stackoverflow.com/questions/19217420/sending-an-object-through-a-socket-in-java
                            try {
                                objToServer = new ObjectOutputStream(socket.getOutputStream());
                                objFromServer = new ObjectInputStream(socket.getInputStream());
                            } catch(IOException e){
                                failed_port = parseInt(remotePorts[i]);
                                socket.close();
                                continue;
                            }
                            //Thread.sleep(100);
                            //https://docs.oracle.com/javase/8/docs/api/java/io/ObjectOutputStream.html
                            port = parseInt(remotePorts[i]);
                            msgToSend.suggester_port = parseInt(remotePorts[i]);
                            msgToSend.failed_port = failed_port;
                            objToServer.writeObject(msgToSend);
                            objToServer.flush();
                            //Log.v(TAG, "Sending message to " + remotePorts[i]);

                            try {
                                proposed_msg = (Message) objFromServer.readObject();
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                            if (proposed_msg.suggester_port == 11108) {
                                proposed_messages[0] = proposed_msg;
                            } else if (proposed_msg.suggester_port == 11112) {
                                proposed_messages[1] = proposed_msg;
                            } else if (proposed_msg.suggester_port == 11116) {
                                proposed_messages[2] = proposed_msg;
                            } else if (proposed_msg.suggester_port == 11120) {
                                proposed_messages[3] = proposed_msg;
                            } else if (proposed_msg.suggester_port == 11124) {
                                proposed_messages[4] = proposed_msg;
                            }

                            if (proposed_msg.type.equals("proposed")) {
                                if (proposed_msg.suggested_seq > max) {
                                    max = proposed_msg.suggested_seq;
                                }
                                //Log.v(TAG,"Checked proposed with max");
                            }
                            if (proposed_msg != null) {
                                //Log.v(TAG,"Closing all the resources");
                                objToServer.close();
                                objFromServer.close();
                                socket.close();
                            }
                        }
                    }
                    Socket socket1;
                    Message ack=null;
                    ObjectOutputStream objToServer1 = null;
                    ObjectInputStream objFromServer1 = null;
                    for(int j = 0; j < remotePorts.length; j++) {
                        if (failed_port != parseInt(remotePorts[j])) {
                            socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    parseInt(remotePorts[j]));
                            try {
                                objToServer1 = new ObjectOutputStream(socket1.getOutputStream());
                                objFromServer1 = new ObjectInputStream(socket1.getInputStream());
                            }catch (IOException e){
                                failed_port = parseInt(remotePorts[j]);
                                socket1.close();
                                continue;
                            }
                            port = parseInt(remotePorts[j]);
                            /*if(proposed_messages[j].failed_port==0){
                                proposed_messages[j].failed_port = failed_port;
                            }*/
                            if (proposed_messages[j].type.equals("proposed")) {
                                //Log.v(TAG,"Message: "+proposed_messages[j].msg+" Final Sequence"+max);
                                proposed_messages[j].max_agreed = max;
                                proposed_messages[j].type = "deliver";
                                proposed_messages[j].failed_port = failed_port;
                                objToServer1.writeObject(proposed_messages[j]);
                                objToServer1.flush();
                            }
                            try {
                                ack = (Message) objFromServer1.readObject();
                                //Log.v(TAG,"Reading the acknowledgement");
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                            if (ack.type.equals("ack")) {
                                //Log.v(TAG,"Releasing all the resources");
                                //Release resources
                                objToServer1.close();
                                objFromServer1.close();
                                socket1.close();
                            }

                        }
                    }

                }catch (SocketTimeoutException e) {
                    Log.e(TAG, "ClientTask SocketTimeoutException");
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                    Log.v(TAG,"Port "+port+" has crashed");
                    failed_port = port;
                    e.printStackTrace();
                }
            }
            return null;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
