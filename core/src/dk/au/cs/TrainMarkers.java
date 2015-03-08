package dk.au.cs;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import weka.core.*;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.NonSparseToSparse;

import javax.rmi.CORBA.Util;
import java.io.File;
import java.io.IOException;
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
    private Mat preparedImage;
    private Mat showContour;
    private int indexShape = -1;
    private int positiveMatchIndex = 0;
    private List<MatOfPoint2f> polygons;
    private List<MatOfPoint> contoursCvt;
    private List<ImageData> collectedData;

    //OPENCV

    private VideoCapture cap;
    private MatOfPoint2f eye;

    @Override
    public void create () {
        preparedImage = Mat.eye(128, 128, CvType.CV_8UC1);
        showContour = Mat.eye(128, 128, CvType.CV_8UC1);
        collectedData = new ArrayList<ImageData>();

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
                        indexShape = -1;
                        cap.read(eye);
                        cap.read(eye);
                        prepareImage();
                        //prepareImageOld();
                        Gdx.graphics.requestRendering();
                        break;
                    case Input.Keys.ENTER:
                        indexShape = (indexShape + 1) % (contoursCvt.size());
                        showShapeAtIndex(indexShape);
                        Gdx.graphics.requestRendering();
                        break;
                    case Input.Keys.Q:
                        savePositiveMatchIndex();
                        break;
                    case Input.Keys.W:
                        writeData();
                        System.out.println("Saved the data");
                        break;
                    case Input.Keys.F:
                        System.out.println("Wrote data to file");
                        writeToFile();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }

    private void writeData() {
        collectedData.add(new ImageData(polygons, contoursCvt, positiveMatchIndex));
    }

    private void writeToFile() {
        Instances sparseDataset = createArff(collectedData);
        //Save the dataset
        ArffSaver arffSaverInstance = new ArffSaver();
        arffSaverInstance.setInstances(sparseDataset);
        File file = new File("./test.arff");

        // saving file
        try {
            arffSaverInstance.setFile(file);
            arffSaverInstance.writeBatch();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Instances createArff(List<ImageData> collectedData) {
        //Create the dataset
        FastVector classNameList = new FastVector();
        classNameList.addElement("noSquare");
        classNameList.addElement("square");

        FastVector attributes = new FastVector();
        attributes.addElement(new Attribute("numberOfPoints"));
        attributes.addElement(new Attribute("area"));
        attributes.addElement(new Attribute("isConvex"));

        Attribute classNames = new Attribute("class", classNameList);
        attributes.addElement(classNames);

        Instances dataSet = new Instances("imageData", attributes, 0);

        for(ImageData data : collectedData) {
            List<MatOfPoint2f> polygons = data.getPolygons();
            List<MatOfPoint> contoursCvt = data.getContoursCvt();
            int positiveMatchIndex = data.getIndexOfPositiveMatch();
            for (int index = 0; index < contoursCvt.size(); index++) {
                double[] values = new double[dataSet.numAttributes()];
                values[0] = polygons.get(index).size().height;
                values[1] = contourArea(contoursCvt.get(index));
                if (isContourConvex(contoursCvt.get(index))) {
                    values[2] = 1;
                } else {
                    values[2] = 0;
                }
                if (index == positiveMatchIndex) {
                    values[3] = 1;
                } else {
                    values[3] = 0;
                }
                Instance instance = new SparseInstance(1.0, values);
                dataSet.add(instance);
            }
        }
        NonSparseToSparse nonSparseToSparseInstance = new NonSparseToSparse();
        Instances sparseDataset = null;
        try {
            nonSparseToSparseInstance.setInputFormat(dataSet);
            sparseDataset = Filter.useFilter(dataSet, nonSparseToSparseInstance);

            return sparseDataset;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void savePositiveMatchIndex() {
        positiveMatchIndex = indexShape;
        System.out.println("Saved positive index " + positiveMatchIndex);
    }

    private void showShapeAtIndex(int index) {
        //Draw and let me tell if it is a square
        List<MatOfPoint> currentContour = new ArrayList<MatOfPoint>();
        currentContour.add(contoursCvt.get(index));
        drawContours(eye, currentContour, -1, new Scalar(0,255,255));
    }


    private void prepareImage() {
        Imgproc.cvtColor(eye, preparedImage, Imgproc.COLOR_RGB2GRAY);
        threshold(preparedImage, preparedImage, 100, 255, THRESH_BINARY);
        Mat kernel = getStructuringElement(0, new Size(5,5));
        morphologyEx(preparedImage, preparedImage,MORPH_OPEN , kernel);
        morphologyEx(preparedImage, preparedImage,MORPH_CLOSE , kernel);
        findShapes();
        UtilAR.imShow(preparedImage);
    }

    private void findShapes() {
        //Find Contours
        Mat contourImage = preparedImage.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierachy = new Mat();
        findContours(contourImage, contours, hierachy, RETR_LIST, CHAIN_APPROX_SIMPLE);

        //Estimate Polygons
        contoursCvt = new ArrayList<MatOfPoint>();
        polygons = new ArrayList<MatOfPoint2f>();
        for(MatOfPoint cont : contours) {
            MatOfPoint2f cont2f = new MatOfPoint2f(cont.toArray());
            MatOfPoint2f polygon = new MatOfPoint2f();
            approxPolyDP(cont2f, polygon, 8, true);
            if(polygon.size().height >= 3 && isClockwise(polygon)) {
                contoursCvt.add(new MatOfPoint(polygon.toArray()));
                polygons.add(polygon);
            }
        }


        //Draw the result
        drawContours(eye, contoursCvt, -1, new Scalar(0, 0, 255));
    }

    //Run this to compare the improvements on our new preparation algorithm
    private void prepareImageOld() {
        Mat detectedEdges = Mat.eye(128, 128, CvType.CV_8UC1);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.cvtColor(eye, detectedEdges, Imgproc.COLOR_RGB2GRAY);
        threshold(detectedEdges, detectedEdges, 80, 255, THRESH_BINARY);
        UtilAR.imShow(detectedEdges);
    }


    //============HELPERS==========

    //Tell if clockwise or not == is it black or white square
    private boolean isClockwise(MatOfPoint2f rect) {
        double[] p1 = rect.get(0, 0);
        double[] p2 = rect.get(1, 0);
        double[] p3 = rect.get(2, 0);
        Vector3 v1 = new Vector3((float)(p1[0]-p2[0]), 0.0f, (float)(p1[1]-p2[1]));
        Vector3 v2 = new Vector3((float)(p3[0]-p2[0]), 0.0f, (float)(p3[1]-p2[1]));
        Vector3 crs = v1.crs(v2);
        return (crs.y > 0);
    }
}