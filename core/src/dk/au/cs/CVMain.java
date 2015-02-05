package dk.au.cs;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import org.opencv.core.*;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.blur;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class CVMain extends ApplicationAdapter {

    VideoCapture camera;
    private Mat videoInput;
    private Mat detectedEdges;

	@Override
	public void create () {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        detectedEdges = Mat.eye(128, 128, CvType.CV_8UC1);
        videoInput = Mat.eye(128, 128, CvType.CV_8UC1);

        camera = new VideoCapture(0);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(!camera.isOpened()){
            System.out.println("Camera Error");
        }
        else {
            System.out.println("Camera OK?");
        }
    }

	@Override
	public void render () {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.read(videoInput);
        UtilAR.imDrawBackground(videoInput);
        doCanny();
	}

    @Override
    public void dispose() {
        camera.release();
    }

    private void doCanny() {
        Imgproc.cvtColor(videoInput, detectedEdges, Imgproc.COLOR_RGB2GRAY);
        blur(detectedEdges, detectedEdges, new Size(3,3));
        Imgproc.Canny(detectedEdges, detectedEdges, 50,100);
        UtilAR.imShow(detectedEdges);
    }
}
