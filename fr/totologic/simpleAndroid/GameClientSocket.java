package fr.totologic.simpleAndroid;

import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;

public class GameClientSocket {

    ArrayList<String> _logArray;

    private Socket _clientSocket;
    private boolean _clientIsConnected;
    private String _toSendBuffer;
    private ClientReadThread _clientReadThread;
    private ClientPrintThread _clientPrintThread;
    private ClientConnectionStatusListener _clientConnectionStatusListener;
    private MessageFromServerReceivedListener _messageFromServerReceivedListener;

    public GameClientSocket() {
        _logArray = new ArrayList<>();

        _clientIsConnected = false;
        _toSendBuffer = "";
    }

    // STATUS
    public interface ClientConnectionStatusListener
    {
        public enum ClientConnectionStatus
        {
            CONNECTED, DISCONNECTED;
        }

        void onClientConnectionStatus(ClientConnectionStatus status);
    }
    public void setOnClientConnectionStatus(ClientConnectionStatusListener listener)
    {
        _clientConnectionStatusListener = listener;
    }

    // MESSAGE LISTENER
    public interface MessageFromServerReceivedListener
    {
        void onMessageFromServerReceived(String message);
    }
    public void setOnMessageFromServerReceivedListener(MessageFromServerReceivedListener listener)
    {
        _messageFromServerReceivedListener = listener;
    }


    private void addlog(String message)
    {
        _logArray.add(0, message);
    }
    public ArrayList<String> get_logArray() {
        return _logArray;
    }

    public void start(String toIP) {
        ClientConnectThread clientConnectThread = new ClientConnectThread();
        clientConnectThread.toIP = toIP;
        clientConnectThread.toPORT = SocketConsts.PORT;
        clientConnectThread.start();
    }

    public void stop() {
        if (_clientSocket != null)
        {
            if (_clientSocket.isConnected())
            {
                try {
                    addlog("try to close client");
                    _clientSocket.close();
                    addlog("close client success");
                }
                catch (Exception e) {
                    addlog("close client failed");
                }
            }
            _clientSocket = null;
        }
    }

    public void sendToServer(String message)
    {
        _toSendBuffer = message;
    }

    ///// THREAD /////

    private class ClientConnectThread extends Thread {
        public String toIP;
        public int toPORT;
        @Override
        public void run() {
            addlog("try to connect on " + toIP + ":" + toPORT);
            try {
                _clientSocket = new Socket(toIP, toPORT);
                _clientIsConnected = true;
                if (_clientConnectionStatusListener != null)
                    _clientConnectionStatusListener.onClientConnectionStatus(ClientConnectionStatusListener.ClientConnectionStatus.CONNECTED);
            } catch(Exception e) {
                addlog("connection failed !");
                if (_clientConnectionStatusListener != null)
                    _clientConnectionStatusListener.onClientConnectionStatus(ClientConnectionStatusListener.ClientConnectionStatus.DISCONNECTED);
                return;
            }
            addlog("connected");
            _clientPrintThread = new ClientPrintThread();
            _clientPrintThread.start();

            _clientReadThread = new ClientReadThread();
            _clientReadThread.start();
        }
    }

    private class ClientPrintThread extends Thread {
        private PrintStream _printStream;
        private long _prevTime;
        private long _currTime;

        @Override
        public void run() {
            Socket socket = _clientSocket;
            try {
                _printStream = new PrintStream(socket.getOutputStream());
            } catch(Exception e) {
                addlog("printStream failed !");
                return;
            }
            _prevTime = System.currentTimeMillis();
            addlog("printStream success");
            while (true)
            {
                if (!_clientIsConnected)
                    break;

                if (_toSendBuffer != "")
                {
                    addlog("send to server: -" + _toSendBuffer + "-");
                    try {
                        _printStream.println(_toSendBuffer);
                        _prevTime = System.currentTimeMillis();
                    } catch(Exception e) {

                    }
                    _toSendBuffer = "";
                }
                else
                {
                    _currTime = System.currentTimeMillis();
                    if (_currTime - _prevTime > SocketConsts.PING_TIME)
                    {
                        addlog("ping to server");
                        try {
                            _printStream.println("ping");
                        } catch(Exception e) {

                        }
                        _prevTime = _currTime;
                    }
                }
            }

            addlog("printStream aborted");
            _clientPrintThread = null;
        }
    }

    private class ClientReadThread extends Thread {

        @Override
        public void run() {
            Socket socket = _clientSocket;
            byte[] bytesBuffer = new byte[1024];
            int bytesToRead;
            String message = "";
            int index;
            int i;
            while (true) {
                try {
                    socket.setSoTimeout(SocketConsts.PING_TIME * 2);
                    addlog("wait for input");
                    bytesToRead = socket.getInputStream().read(bytesBuffer);
                } catch (Exception e) {
                    bytesToRead = -1;
                }
                if (bytesToRead < 0)
                    break;

                index = -1;
                for (i = 0; i < bytesToRead; i++) {
                    if (bytesBuffer[i] == (byte) 10) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    message = message.concat(new String(bytesBuffer, 0, bytesToRead));
                } else {
                    message = message.concat(new String(bytesBuffer, 0, index));
                    if (message.equalsIgnoreCase(SocketConsts.PING_MESSAGE)) {
                        addlog("ping received !");
                    }
                    else {
                        addlog("received: -" + message + "-");
                        if (_messageFromServerReceivedListener != null)
                            _messageFromServerReceivedListener.onMessageFromServerReceived(message);
                    }
                    message = new String(bytesBuffer, index + 1, bytesToRead - index - 1);
                }
            }

            addlog("readStream aborted");
            _clientReadThread = null;
            _clientIsConnected = false;
            if (_clientConnectionStatusListener != null)
                _clientConnectionStatusListener.onClientConnectionStatus(ClientConnectionStatusListener.ClientConnectionStatus.DISCONNECTED);
        }
    }

}
