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
            //Since we could not connect we expect puredata to be not running
            System.out.println("Starting up puredata");
            try {
                //Start up puredata
                Runtime.getRuntime().exec("pd ../../SoundPlayer.pd");
                //Wait for puredata to be ready
                while(socket == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        System.err.println("Failed to sleep");
                    }
                    try {
                        socket = new Socket("localhost", 7778);
                        System.out.println("Socket is ready");
                    } catch (IOException e1) {
                        System.out.println("Socket still not ready trying again");
                    }
                }
            } catch (IOException e1) {
                System.err.println("Could not start puredata");
            }
        }
        try {
            os = socket.getOutputStream();
            writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
        } catch (IOException e1) {
            e1.printStackTrace();
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
