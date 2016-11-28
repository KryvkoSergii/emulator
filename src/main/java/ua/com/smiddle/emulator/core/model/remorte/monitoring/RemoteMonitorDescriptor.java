package ua.com.smiddle.emulator.core.model.remorte.monitoring;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * @author ksa on 28.11.16.
 * @project emulator
 */
public class RemoteMonitorDescriptor {
    private Socket socket;
    private ObjectOutputStream outputStream;


    //Constructors
    public RemoteMonitorDescriptor() {
    }

    public RemoteMonitorDescriptor(Socket socket) {
        this.socket = socket;
    }


    //Getters and setters
    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public ObjectOutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(ObjectOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void write(Object data) throws IOException {
        if (outputStream != null) {
            outputStream.writeObject(data);
            outputStream.flush();
        } else
            outputStream = new ObjectOutputStream(socket.getOutputStream());
    }
}
