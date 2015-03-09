package dk.au.cs;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.opencv.calib3d.Calib3d.ITERATIVE;
import static org.opencv.calib3d.Calib3d.findHomography;
import static org.opencv.calib3d.Calib3d.solvePnP;
import static org.opencv.imgproc.Imgproc.*;

/**
 * Created by Birk on 07-03-2015.
 * This class encapsulates all the work of
 * finding the correct markers and map according to them.
 */
public class MarkerHandler {

    private final CVMain delegate;
    private List<MatOfPoint2f> rects = new ArrayList<MatOfPoint2f>();
    private RotationHandler rotationHandler;
    private MatOfPoint2f homoWorld;
    private MatOfPoint2f warpedImage; //The image result of the homography
    private Size rectSize = new Size(2,2);
    private double numOfCoords = rectSize.width*rectSize.height;
    private int SCREEN_WIDTH;
    private int SCREEN_HEIGHT;

    private VideoCapture cap;
    private MatOfPoint2f eye;
    private MatOfPoint2f corners;
    private Mat intrinsics;
    private MatOfDouble distortion;
    private HashMap<Integer, Actor> actorMap;

    private ClassifySquareStrategy squareStrategy;

    public MarkerHandler(int SCREEN_WIDTH, int SCREEN_HEIGHT, HashMap<Integer, Actor> actorMap, CVMain delegate) {
        this.delegate = delegate;
        this.SCREEN_WIDTH = SCREEN_WIDTH;
        this.SCREEN_HEIGHT = SCREEN_HEIGHT;
        this.actorMap = actorMap;
        squareStrategy = new SimpleClassifySquareStrategy();
        //The homography matrix
        homoWorld = new MatOfPoint2f();
        homoWorld.alloc(4);
        homoWorld.put(0, 0, 0, 0);
        homoWorld.put(1, 0, SCREEN_WIDTH, 0);
        homoWorld.put(2, 0, SCREEN_WIDTH, SCREEN_WIDTH);
        homoWorld.put(3, 0, 0, SCREEN_WIDTH);

        warpedImage = new MatOfPoint2f();
        rotationHandler = new RotationHandler((int)numOfCoords, homoWorld);

        eye = new MatOfPoint2f();
        corners = new MatOfPoint2f();

        // setup video capture
        cap = new VideoCapture(0);
        cap.set(Highgui.CV_CAP_PROP_FRAME_WIDTH,SCREEN_WIDTH);
        cap.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT,SCREEN_HEIGHT);

        // get intrinsics after view capture dimensions are set
        corners.alloc((int)numOfCoords);
        intrinsics = UtilAR.getDefaultIntrinsicMatrix(SCREEN_WIDTH, SCREEN_HEIGHT);
        distortion = UtilAR.getDefaultDistortionCoefficients();

        //Ensure Camera is ready!
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(!cap.isOpened()){
            System.out.println("Video Camera Error");
        }
        else{
            System.out.println("Video Camera OK");
        }
    }

    public void readCam() {
        cap.read(eye);
    }

    //<---- Finding our rectangles --->

    //This is where opecv find the actual contours
    private List<MatOfPoint> findContoursFromEdges(MatOfPoint2f input) {
        Mat detectedEdges = Mat.eye(128, 128, CvType.CV_8UC1);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierachy = new Mat();
        //Make Binary
        Imgproc.cvtColor(input, detectedEdges, Imgproc.COLOR_RGB2GRAY);
        threshold(detectedEdges, detectedEdges, 140, 255, THRESH_BINARY);
        //Remove noise, and holes in shapes
        Mat kernel = getStructuringElement(0, new Size(5,5));
        morphologyEx(detectedEdges, detectedEdges,MORPH_OPEN , kernel);
        morphologyEx(detectedEdges, detectedEdges,MORPH_CLOSE , kernel);
        //Find contours
        findContours(detectedEdges, contours, hierachy, RETR_LIST, CHAIN_APPROX_SIMPLE);
        return contours;
    }

    //This is where we find our rectangles and put them in the field -> rects
    public void findRectangles() {
        List<MatOfPoint> contours = findContoursFromEdges(eye);
        List<MatOfPoint> rectContours = new ArrayList<MatOfPoint>();
        rects = new ArrayList<MatOfPoint2f>();
        for(MatOfPoint cont : contours) {
            MatOfPoint2f cont2f = new MatOfPoint2f(cont.toArray());
            MatOfPoint2f polygon = new MatOfPoint2f();
            approxPolyDP(cont2f, polygon, 8, true);
            MatOfPoint polygonCvt = new MatOfPoint(polygon.toArray());
            // check for rectangles

            if(polygonCvt.size().height >= 4 && squareStrategy.isSquare(polygonCvt, polygon) && isClockwise(polygon)) {
                rectContours.add(polygonCvt);
                rects.add(polygon);
            }
        }
        drawContours(eye, rectContours, -1, new Scalar(0, 0, 255));
        UtilAR.imDrawBackground(eye);
    }

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

    //This method is a lot like findRectangles, but now for the id polygons in the homography
    private List<MatOfPoint2f> findIdPolygon() {
        List<MatOfPoint> contours = findContoursFromEdges(warpedImage);
        List<MatOfPoint2f> idPolygons = new ArrayList<MatOfPoint2f>();
        List<MatOfPoint> idPolygonsCvt = new ArrayList<MatOfPoint>();
        for(MatOfPoint cont : contours) {
            MatOfPoint2f cont2f = new MatOfPoint2f(cont.toArray());
            MatOfPoint2f polygon = new MatOfPoint2f();
            approxPolyDP(cont2f, polygon, 8, true);
            MatOfPoint polygonCvt = new MatOfPoint(polygon.toArray());
            // check for ids
            if (!isContourConvex(polygonCvt) && polygon.size().height >= 6) {
                idPolygons.add(polygon);
                idPolygonsCvt.add(polygonCvt);
            }
        }
        // DEBUGGING --
        //drawContours(warpedImage, idPolygonsCvt, -1, new Scalar(255, 0, 0));
        //UtilAR.imShow(warpedImage);
        return idPolygons;
    }


    //<---- Handle Found Rectangles --->


    private Mat drawHomography(MatOfPoint2f src) {
        Mat homography = findHomography(src, homoWorld);
        warpPerspective(eye, warpedImage, homography, new Size(SCREEN_WIDTH, SCREEN_WIDTH));
        return homography;
    }

    public void handleRectangles() {
        clearActorMapRT();
        for (MatOfPoint2f rect : rects) {
            Mat homography = drawHomography(rect); // side effect - warps using homography on warpedImage
            List<MatOfPoint2f> idPolygons = findIdPolygon(); // should only contain 1 element!

            //Extract the id from the polygon
            MatOfPoint3f rectObjCoords = null;
            int theId = -1;
            if (idPolygons.size() == 1) {
                MatOfPoint2f polygon = idPolygons.get(0);
                rectObjCoords = rotationHandler.getObjCoords(polygon);
                double id = polygon.size().height;
                theId = (int) ((id - 6) / 4.0); // this works!
            }
            //Set the rotation and translation of the actor of that id.
            Mat rotation = new Mat();
            Mat translation = new Mat();
            if (rectObjCoords != null && theId <= 4) {
                solvePnP(rectObjCoords, rect, intrinsics, distortion, rotation, translation, false, ITERATIVE);
                actorMap.get(theId).setTranslation(translation);
                actorMap.get(theId).setRotation(rotation);

                // the id that is the spinner
                handleSpinner(rotation, theId);

            }
        }
    }

    private void handleSpinner(Mat rotation, int theId) {
        // -- not working ---
        //double cosTheta = homography.get(0,0)[0];
        //double theta = Math.acos(cosTheta);
        //System.out.println("homo angle = " + theta);

        // get rotation matrix
        Matrix4 rotMat = new Matrix4();
        Mat idVec = Mat.ones(3, 1, rotation.type());
        UtilAR.setTransformByRT(rotation, idVec, rotMat);
        float[] r = rotMat.getValues();

        // Euler angle - find r31 and r11 - rotation around y-axis
        double Ry = Math.atan2(r[6], r[0]);
        /*System.out.println("------");
        System.out.println();
        for (int i = 0; i < 9; i++) {
            System.out.print(r[i] + " ");
            if (i % 3 == 2) System.out.println();
        }
        System.out.println();
        System.out.println("from " + r[6] + " and " + r[0]);
        System.out.println("rotation = " + Ry);
         */
        delegate.setSoundLevel(Ry, theId);
    }

    //Resets translation and rotation of all Actors.
    private void clearActorMapRT() {
        for(Actor actor : actorMap.values()) {
            actor.setTranslation(null);
            actor.setTranslation(null);
            actor.setRotation(null);
        }
    }

    public void releaseCamera() {
        cap.release();
    }
}

