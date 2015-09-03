package fr.totologic.simpleAndroid;

import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

public class GameServerSocket {

    ArrayList<String> _logArray;

    private ServerSocket _serverSocket;
    private String _myIP;
    private Boolean _serverAlive;
    private ServerAcceptThread _serverAcceptThread;
    private Object _lockAcceptedClients;
    private int _countAcceptedClients;
    private Vector<Socket> _vAcceptedClientSocket;
    private Vector<String> _vAcceptedClientID; private int __incID = 0;
    private Vector<AcceptedClientReadThread> _vAcceptedClientReadThread;
    private Vector<AcceptedClientPrintThread> _vAcceptedClientPrintThread;

    private GameServerEventListener _serverEventListener;

    public GameServerSocket(){
        _logArray = new ArrayList<>();

        _serverAlive = false;
        _lockAcceptedClients = new Object();
        _countAcceptedClients = 0;
        _vAcceptedClientSocket = new Vector<>();
        _vAcceptedClientID = new Vector<>();
        _vAcceptedClientPrintThread = new Vector<>();
        _vAcceptedClientReadThread = new Vector<>();
    }

    public int getCountAcceptedClients()    {   return _countAcceptedClients;                           }
    public Vector<String> getClientsIDs()   {   return (Vector<String>)_vAcceptedClientID.clone();      }
    public String getClientID(int index)    {   return _vAcceptedClientID.get(index);                   }
    public int getClientIndex(String id)    {   return _vAcceptedClientID.indexOf(id);                  }

    public interface GameServerEventListener
    {
        public enum ServerConnectionStatus
        {
            CONNECTED, DISCONNECTED;
        }
        public enum ClientEventType
        {
            NEW_CLIENT, CLIENT_LOST;
        }

        void onServerConnectionStatus(ServerConnectionStatus status);
        void onClientEvent(ClientEventType type, String clientID, int clientsCount);
        void onMessageFromClientReceived(String clientID, String message);
    }
    public void setOnGameServerEvent(GameServerEventListener listener)
    {
        _serverEventListener = listener;
    }

    private void addlog(String message)
    {
        _logArray.add(0, message);
    }
    public ArrayList<String> get_logArray() {
        return _logArray;
    }

    public void start()
    {
        _serverAlive = true;
        _serverAcceptThread = new ServerAcceptThread();
        _serverAcceptThread.start();
    }

    public void stop()
    {
        if (_serverSocket != null)
        {
            if (_serverAlive)
            {
                _serverAlive = false;
                if (_serverAcceptThread != null)
                    _serverAcceptThread.interrupt();
                _serverAcceptThread = null;
                for (int i= _countAcceptedClients -1 ; i>=0 ; i--)
                {
                    _vAcceptedClientReadThread.elementAt(i).interrupt();
                    _vAcceptedClientPrintThread.elementAt(i).interrupt();
                    try {
                        addlog("try to eject client" + String.valueOf(i));
                        _vAcceptedClientSocket.elementAt(i).close();
                    } catch (Exception e) {
                    }
                }
                _vAcceptedClientReadThread.removeAllElements();
                _vAcceptedClientPrintThread.removeAllElements();
                _vAcceptedClientSocket.removeAllElements();
                _vAcceptedClientID.removeAllElements();
                //
                try {
                    addlog("try to close server");
                    _serverSocket.close();
                    addlog("close server success");
                } catch (Exception e) {
                    addlog("close server failed");
                }
            }
            _serverSocket = null;
            _serverEventListener.onServerConnectionStatus(GameServerEventListener.ServerConnectionStatus.DISCONNECTED);
        }
    }

    public void sendToClient(String message, String clientID)
    {
        for (int i=0 ; i<_vAcceptedClientID.size() ; i++)
        {
            if (_vAcceptedClientID.get(i) == clientID)
            {
                _vAcceptedClientPrintThread.get(i).fillPrintBuffer(message);
                break;
            }
        }
    }

    public void sendToClients(String message)
    {
        for (int i=0 ; i<_vAcceptedClientID.size() ; i++)
        {
            _vAcceptedClientPrintThread.get(i).fillPrintBuffer(message);
        }
    }

    ///// THREADS /////

    private class ServerAcceptThread extends Thread
    {

        @Override
        public void run() {
            addlog("run ServerAcceptThread");
            try
            {
                _serverSocket = new ServerSocket(SocketConsts.PORT);
                _serverAlive = true;
                if (_serverEventListener != null)
                    _serverEventListener.onServerConnectionStatus(GameServerEventListener.ServerConnectionStatus.CONNECTED);
            }
            catch (Exception e)
            {
                _serverEventListener.onServerConnectionStatus(GameServerEventListener.ServerConnectionStatus.DISCONNECTED);
                addlog("server can't be instanciated");
            }

            Socket acceptedClient;
            AcceptedClientReadThread readThreadClient;
            AcceptedClientPrintThread printThreadClient;
            String clientID;
            while (_serverAlive) {
                try {
                    addlog("server wait for a connection...");
                    acceptedClient = _serverSocket.accept();

                    synchronized (_lockAcceptedClients) {
                        clientID = String.valueOf(__incID);
                        __incID++;
                        readThreadClient = new AcceptedClientReadThread(acceptedClient, clientID);
                        printThreadClient = new AcceptedClientPrintThread(acceptedClient);
                        _vAcceptedClientSocket.add(acceptedClient);
                        _vAcceptedClientID.add(clientID);
                        _vAcceptedClientReadThread.add(readThreadClient);
                        _vAcceptedClientPrintThread.add(printThreadClient);
                        _countAcceptedClients++;
                    }

                    readThreadClient.start();
                    printThreadClient.start();

                    if (_serverEventListener != null)
                        _serverEventListener.onClientEvent(GameServerEventListener.ClientEventType.NEW_CLIENT, clientID, _countAcceptedClients);

                    addlog("new client connected !");
                } catch (Exception e) {
                    addlog("server can't accept client");
                }
            }
            addlog("terminate ServerAcceptThread");
        }
    }

    private class AcceptedClientPrintThread extends Thread
    {
        private Socket socket;
        private PrintStream _printStream;
        private String _toPrintBuffer;
        private long _prevTime;
        private long _currTime;

        public AcceptedClientPrintThread(Socket cSocket)
        {
            socket = cSocket;
            _toPrintBuffer = "";
        }

        public void fillPrintBuffer(String value)
        {
            _toPrintBuffer = value;
        }

        @Override
        public void run() {
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
                if (!socket.isConnected() || socket.isClosed())
                    break;

                if (_toPrintBuffer != "")
                {
                    addlog("send to client: -" + _toPrintBuffer + "-");
                    try {
                        _printStream.println(_toPrintBuffer);
                        _prevTime = System.currentTimeMillis();
                    } catch(Exception e) {

                    }
                    _toPrintBuffer = "";
                }
                else
                {
                    _currTime = System.currentTimeMillis();
                    if (_currTime - _prevTime > SocketConsts.PING_TIME)
                    {
                        addlog("ping to client");
                        try {
                            _printStream.println("ping");
                        } catch(Exception e) {

                        }
                        _prevTime = _currTime;
                    }
                }
            }
            addlog("printStream aborted");
            synchronized (_lockAcceptedClients) {
                int index = _vAcceptedClientPrintThread.indexOf(this);
                _vAcceptedClientPrintThread.set(index, null);
                if (_vAcceptedClientReadThread.get(index) == null) {
                    addlog("remove client from list");
                    _vAcceptedClientPrintThread.remove(index);
                    _vAcceptedClientReadThread.remove(index);
                    _vAcceptedClientSocket.remove(index);
                    String clientID = _vAcceptedClientID.elementAt(index);
                    _vAcceptedClientID.remove(index);
                    _countAcceptedClients--;
                    addlog("remaining connected clients: " + String.valueOf(_countAcceptedClients));
                    if (_serverEventListener != null)
                        _serverEventListener.onClientEvent(GameServerEventListener.ClientEventType.CLIENT_LOST, clientID, _countAcceptedClients);
                }
            }
        }
    }

    private class AcceptedClientReadThread extends Thread
    {
        private Socket socket;
        private String clientID;

        public AcceptedClientReadThread(Socket socket, String clientID)
        {
            this.socket = socket;
            this.clientID = clientID;
        }

        @Override
        public void run() {
            byte[] bytesBuffer = new byte[1024];
            int bytesToRead;
            String message = "";
            int index;
            int i;
            while (true)
            {
                try{
                    socket.setSoTimeout(SocketConsts.PING_TIME * 2);
                    addlog("wait for input");
                    bytesToRead = socket.getInputStream().read(bytesBuffer);
                }catch (Exception e) {
                    bytesToRead = -1;
                }
                if (bytesToRead < 0)
                    break;

                index = -1;
                for (i=0 ; i<bytesToRead ; i++)
                {
                    if (bytesBuffer[i] == (byte)10)
                    {
                        index = i;
                        break;
                    }
                }
                if (index == -1)
                {
                    message = message.concat(new String(bytesBuffer, 0, bytesToRead));
                }
                else
                {
                    message = message.concat(new String(bytesBuffer, 0, index));
                    if (message.equalsIgnoreCase(SocketConsts.PING_MESSAGE)) {
                        addlog("ping received !");
                    }
                    else {
                        addlog("received: -" + message + "-");
                        if (_serverEventListener != null)
                            _serverEventListener.onMessageFromClientReceived(clientID, message);
                    }
                    message = new String(bytesBuffer, index+1, bytesToRead-index-1);
                }
            }

            try{
                socket.close();
            }
            catch (Exception e) {

            }
            addlog("readStream aborted");
            addlog("connected client is lost");
            synchronized (_lockAcceptedClients) {
                index = _vAcceptedClientReadThread.indexOf(this);
                _vAcceptedClientReadThread.set(index, null);
                if (_vAcceptedClientPrintThread.get(index) == null) {
                    addlog("remove client from list");
                    _vAcceptedClientPrintThread.remove(index);
                    _vAcceptedClientReadThread.remove(index);
                    _vAcceptedClientSocket.remove(index);
                    String clientID = _vAcceptedClientID.elementAt(index);
                    _vAcceptedClientID.remove(index);
                    _countAcceptedClients--;
                    addlog("remaing connected clients: " + String.valueOf(_countAcceptedClients));
                    if (_serverEventListener != null)
                        _serverEventListener.onClientEvent(GameServerEventListener.ClientEventType.CLIENT_LOST, clientID, _countAcceptedClients);
                }
            }
        }

    }

}
