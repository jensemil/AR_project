package dk.au.cs;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;

import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.opencv.imgproc.Imgproc.*;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;

public class TrainMarkers extends ApplicationAdapter {

    private static int SCREEN_WIDTH = 640;
    private static int SCREEN_HEIGHT = 480;
    private PerspectiveCamera cam;


    //OPENCV

    private VideoCapture cap;
    private MatOfPoint2f eye;

    @Override
    public void create () {
        setupCamera();
        setupEventHandling();
        eye = new MatOfPoint2f();
        cap = new VideoCapture(0);
        cap.set(Highgui.CV_CAP_PROP_FRAME_WIDTH,SCREEN_WIDTH);
        cap.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT,SCREEN_HEIGHT);
        Gdx.graphics.setContinuousRendering(false);
        Gdx.graphics.requestRendering();
    }


    @Override
    public void render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        UtilAR.imDrawBackground(eye);
    }


    @Override
    public void resize (int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    @Override
    public void dispose() {
        cap.release();
    }


    private void setupCamera() {
        cam = new PerspectiveCamera(40, Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
        cam.position.set(3f, 3f, 3f);
        cam.lookAt(0f, 0f, 0f);
        cam.up.set(0, 1, 0);
        cam.near = .0001f;
        cam.far = 300f;
        cam.update();
        UtilAR.setNeutralCamera(cam);
    }



    //JUST THE BODY NO USE ATM
    private void setupEventHandling() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyUp(final int keycode) {
                switch (keycode) {
                    case Input.Keys.SPACE:
                        System.out.println("took picture");
                        cap.read(eye);
                        Gdx.graphics.requestRendering();
                        break;
                    case Input.Keys.Z:
                        System.out.println("found corners");
                        prepareImage();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }


    private void prepareImage() {
        Mat preparedImage = Mat.eye(128, 128, CvType.CV_8UC1);
        Imgproc.cvtColor(eye, preparedImage, Imgproc.COLOR_RGB2GRAY);
        threshold(preparedImage, preparedImage, 130, 255, THRESH_BINARY);
        Mat kernel = getStructuringElement(0, new Size(5,5));
        morphologyEx(preparedImage, preparedImage,MORPH_OPEN , kernel);
        morphologyEx(preparedImage, preparedImage,MORPH_CLOSE , kernel);
        findShapes(preparedImage);
        UtilAR.imShow(preparedImage);
    }

    private void findShapes(Mat preparedImage) {
        //Find Contours
        Mat contourImage = preparedImage.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierachy = new Mat();
        findContours(contourImage, contours, hierachy, RETR_LIST, CHAIN_APPROX_SIMPLE);

        //Estimate Polygons
        List<MatOfPoint> contoursCvt = new ArrayList<MatOfPoint>();
        for(MatOfPoint cont : contours) {
            MatOfPoint2f cont2f = new MatOfPoint2f(cont.toArray());
            MatOfPoint2f polygon = new MatOfPoint2f();
            approxPolyDP(cont2f, polygon, 8, true);
            contoursCvt.add(new MatOfPoint(polygon.toArray()));
        }

        //Draw the result
        drawContours(eye, contoursCvt, -1, new Scalar(0, 0, 255));
    }

    //Run this to compare the improvements on our new preparation algorithm
    private void prepareImageOld() {
        Mat detectedEdges = Mat.eye(128, 128, CvType.CV_8UC1);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.cvtColor(eye, detectedEdges, Imgproc.COLOR_RGB2GRAY);
        threshold(detectedEdges, detectedEdges, 100, 255, THRESH_BINARY);
        UtilAR.imShow(detectedEdges);
    }
}