package dk.au.cs;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.VideoCapture;

public class CVMain extends ApplicationAdapter {

    VideoCapture camera;

	@Override
	public void create () {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        camera = new VideoCapture(0);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(!camera.isOpened()){
            System.out.println("Camera Error");
        }
        else{
            System.out.println("Camera OK?");
        }

    }

	@Override
	public void render () {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        Mat eye = Mat.eye(128, 128, CvType.CV_8UC1);

        Core.multiply(eye, new Scalar(255), eye);

        camera.read(eye);

        UtilAR.imDrawBackground(eye);

	}

    @Override
    public void dispose() {
        camera.release();
    }
}
