package dk.au.cs;

import com.badlogic.gdx.audio.Sound;

import java.io.*;
import java.net.Socket;

/**
 * Created by Birk on 03-03-2015.
 */
public class SoundHandler {
    private BufferedWriter writer;
    private OutputStream os;

    public SoundHandler() {
        Socket socket = null;
        try {
            socket = new Socket("localhost", 7778);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            os = socket.getOutputStream();
            writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            writer.write("start" + ";" + "\r\n");
            writer.flush();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setInstrumentState(String instrument, String state) {
        try {
            writer.write(state + " " + instrument + ";" + "\r\n");
            writer.flush();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
