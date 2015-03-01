package dk.au.cs;

//import apple.laf.JRSUIConstants;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;

import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.UBJsonReader;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.calib3d.Calib3d.*;
import static org.opencv.imgproc.Imgproc.*;

public class CVMain extends ApplicationAdapter {

    // 3D graphics
    private PerspectiveCamera cam;
    private ModelBuilder modelBuilder;
    private ModelBatch modelBatch;
    private ModelInstance modelInstance;



    private MatOfPoint2f warpedImage;
    private Environment environment;
    private Material mat;
    private Vector3 originPosition;

    private boolean foundBoard = false;
    private DirectionalLight dirLight;

    // OpenCV

    private VideoCapture cap;
    private MatOfPoint2f eye;
    private MatOfPoint2f corners;

    private List<Mat> calibrationImgs = new ArrayList<Mat>();
    private List<Mat> calibrationObjectPoints = new ArrayList<Mat>();

    private MatOfPoint3f objectCoords;
    private MatOfPoint3f calibObjectCoords;

    private Mat intrinsics;
    private MatOfDouble distortion;

    //private double numOfCoords = chessboardSize.width*chessboardSize.height;
    private Size rectSize = new Size(2,2);
    private double numOfCoords = rectSize.width*rectSize.height;

    private static int SCREEN_WIDTH = 640;
    private static int SCREEN_HEIGHT = 480;


    private List<MatOfPoint2f> rects = new ArrayList<MatOfPoint2f>();

    private List<MatOfPoint3f> rectObjs;

    int count = 0;
    private MatOfPoint2f homoWorld;


    @Override
	public void create () {

        // Graphics

        originPosition = new Vector3(0.5f, 0.5f, 0.5f);
        //originPosition = new Vector3(0,0,0);

        // init model batch - used for rendering
        modelBatch = new ModelBatch();
        // setup model and build cube
        modelBuilder = new ModelBuilder();
        createModel();
        setupCamera();
        setupEnvironment();



        // OpenCV

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        homoWorld = new MatOfPoint2f();
        homoWorld.alloc(4);
        homoWorld.put(0, 0, 0, 0);
        homoWorld.put(1, 0, SCREEN_WIDTH, 0);
        homoWorld.put(2, 0, SCREEN_WIDTH, SCREEN_WIDTH);
        homoWorld.put(3, 0, 0, SCREEN_WIDTH);

        warpedImage = new MatOfPoint2f(); // Mat.eye(SCREEN_WIDTH, SCREEN_WIDTH, CvType.CV_8UC1);

        eye = new MatOfPoint2f(); //.eye(128, 128, CvType.CV_8UC1);
        corners = new MatOfPoint2f();

        // setup video capture
        cap = new VideoCapture(0);
        cap.set(Highgui.CV_CAP_PROP_FRAME_WIDTH,SCREEN_WIDTH);
        cap.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT,SCREEN_HEIGHT);

        // get intrinsics after view capture dimensions are set
        objectCoords = new MatOfPoint3f();
        calibObjectCoords = new MatOfPoint3f();
        objectCoords.alloc((int)numOfCoords);
        calibObjectCoords.alloc((int)numOfCoords);
        corners.alloc((int)numOfCoords);
        intrinsics = UtilAR.getDefaultIntrinsicMatrix(SCREEN_WIDTH, SCREEN_HEIGHT);
        distortion = UtilAR.getDefaultDistortionCoefficients();

        setupRectObjs();
        for (MatOfPoint3f rect : rectObjs) {
            System.out.println(rect.dump());
        }


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

    private void setupRectObjs() {

        rectObjs = new ArrayList<MatOfPoint3f>();

        for (int i = 0; i < 4; i++) {
            MatOfPoint3f rectObj = new MatOfPoint3f();
            rectObj.alloc((int)numOfCoords);
            rectObj.put(i % 4,0, 0, 0, 0);
            rectObj.put((i+1) % 4,0, 1, 0, 0);
            rectObj.put((i+2) % 4,0, 1, 0, 1);
            rectObj.put((i+3) % 4,0, 0, 0, 1);
            rectObjs.add(rectObj);
        }
    }


    private void createModel() {
        // Model loader needs a binary json reader to decode
        UBJsonReader jsonReader = new UBJsonReader();
        // Create a model loader passing in our json reader
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
        Model model;
        // Now load the model by name
        // Note, the model (g3db file ) and textures need to be added to the assets folder of the Android proj
        //model = modelLoader.loadModel(Gdx.files.getFileHandle("first.g3db", Files.FileType.Internal));

        // setup material with texture
        mat = new Material(ColorAttribute.createDiffuse(new Color(0.3f, 0.3f,
                0.3f, 1.0f)));
        // blending
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA, 0.9f));

        model = modelBuilder.createBox(1f, 1f, 1f, mat, VertexAttributes.Usage.Position
                | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);

        // Now create an instance.  Instance holds the positioning data, etc of an instance of your model
        modelInstance = new ModelInstance(model);


        // setup material with texture
        mat = new Material(ColorAttribute.createDiffuse(new Color(0.3f, 0.3f,
                0.3f, 1.0f)));
        // blending
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA, 0.9f));

        modelInstance.materials.add(mat);

    }

    @Override
	public void render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);


        dirLight = getDirectionToCubes();

        // read camera data into "eye matrix"
        cap.read(eye);
        // render eye texture
        //UtilAR.imDrawBackground(eye);
        //handleChessboard();


        findRectangles();
        handleRectangles();
	}

    private void drawHomography(MatOfPoint2f src) {
        Mat homography = findHomography(src, homoWorld);
        warpPerspective(eye,warpedImage,homography,new Size(SCREEN_WIDTH, SCREEN_WIDTH));
    }

    private List<MatOfPoint> findContoursFromEdges(MatOfPoint2f input) {
        Mat detectedEdges = Mat.eye(128, 128, CvType.CV_8UC1);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierachy = new Mat();
        Imgproc.cvtColor(input, detectedEdges, Imgproc.COLOR_RGB2GRAY);
        threshold(detectedEdges, detectedEdges, 100, 255, THRESH_BINARY);
        findContours(detectedEdges, contours, hierachy, RETR_LIST, CHAIN_APPROX_SIMPLE);
        return contours;
    }

    private void findRectangles() {
        List<MatOfPoint> contours = findContoursFromEdges(eye);
        List<MatOfPoint> rectContours = new ArrayList<MatOfPoint>();
        rects = new ArrayList<MatOfPoint2f>();
        for(MatOfPoint cont : contours) {
            MatOfPoint2f cont2f = new MatOfPoint2f(cont.toArray());
            //double cont_len = arcLength(cont2f, true);
            MatOfPoint2f polygon = new MatOfPoint2f();
            approxPolyDP(cont2f, polygon, 8, true);

            MatOfPoint polygonCvt = new MatOfPoint(polygon.toArray());

            // check for rectangles
            if(polygon.size().height == 4 && contourArea(polygonCvt) > 4000 && isContourConvex(polygonCvt) && isClockwise(polygon)) {
                rectContours.add(polygonCvt);
                rects.add(polygon);
            }
        }
        drawContours(eye, rectContours, -1, new Scalar(0, 0, 255));
        UtilAR.imDrawBackground(eye);
    }


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

    private MatOfPoint3f getObjCoords(MatOfPoint2f polygon) {

        int idx = findOrigin(polygon);
        System.out.println("Origin index: " + idx);

        return rectObjs.get(idx);


    }

    private void handleRectangles() {

        count++;


        for (MatOfPoint2f rect : rects) {

            drawHomography(rect);
            List<MatOfPoint2f> idPolygons = findIdPolygon(); // should only contain 1 element!

            MatOfPoint3f rectObjCoords = null;
            int theId = -1;
            if (idPolygons.size() == 1) {
                MatOfPoint2f polygon = idPolygons.get(0);
                rectObjCoords = getObjCoords(polygon);


                double id = polygon.size().height;
                id = (id - 6) / 4.0;       // this works!

                theId = (int)id;
            }
            System.out.println("ID: " + theId);

            Mat rotation = new Mat();
            Mat translation = new Mat();

            if (rectObjCoords != null) {
                solvePnP(rectObjCoords, rect, intrinsics, distortion, rotation, translation, false, ITERATIVE);

                UtilAR.setCameraByRT(rotation, translation, cam);
                renderGraphics(theId);
            }


        }

    }

    private int findOrigin(MatOfPoint2f polygon) {
        Vector2 polyOrigin = findOrientation(polygon);
        float nearestDist = Float.MAX_VALUE;
        int idx = 0;

        for (int i = 0; i < homoWorld.size().height; i++) {
            double[] coords = homoWorld.get(i, 0);
            Vector2 p = new Vector2((float)coords[0], (float)coords[1]);
            float currentDist = p.dst(polyOrigin);

            if (nearestDist > currentDist) {
                nearestDist = currentDist;
                idx = i;
            }
        }

        return idx;
    }

    private Vector2 findOrientation(MatOfPoint2f polygon) {

        float maxSum = 0.0f;
        Vector2 maxP = new Vector2();
        int size = (int)polygon.size().height;

        for (int i = 0; i < size; i++) {
            double[] p1 = polygon.get(i, 0);
            int i2 = (i+1) % size;
            double[] p2 = polygon.get(i2, 0);
            int i3 = (i+2) % size;
            double[] p3 = polygon.get(i3, 0);

            Vector3 v1 = new Vector3((float)(p1[0]-p2[0]), 0.0f, (float)(p1[1]-p2[1]));
            Vector3 v2 = new Vector3((float)(p3[0]-p2[0]), 0.0f, (float)(p3[1]-p2[1]));

            float sum = v1.len() + v2.len();
            if (sum > maxSum) {
                maxSum = sum;
                maxP = new Vector2((float)p2[0], (float)p2[1]);
            }
        }

        System.out.println(maxP);
        return maxP;
    }

    private boolean isClockwise(MatOfPoint2f rect) {
        double[] p1 = rect.get(0, 0);
        double[] p2 = rect.get(1, 0);
        double[] p3 = rect.get(2, 0);

        Vector3 v1 = new Vector3((float)(p1[0]-p2[0]), 0.0f, (float)(p1[1]-p2[1]));
        Vector3 v2 = new Vector3((float)(p3[0]-p2[0]), 0.0f, (float)(p3[1]-p2[1]));

        Vector3 crs = v1.crs(v2);


        return (crs.y > 0);
    }



    private void renderGraphics(int theId) {
        // render model objects
        modelBatch.begin(cam);


        modelInstance.transform.idt();
        //Vector3 position = new Vector3(0, 0.5f, 0);
        modelInstance.transform.translate(originPosition);

        modelBatch.render(modelInstance, environment);
        modelBatch.end();

    }


    @Override
    public void resize (int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }



    @Override
    public void dispose() {
        modelBatch.dispose();
        cap.release();
    }



    /****** GRAPHICS ******/


    private void setupEnvironment() {
        // setup environment
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f,
                0.4f, 0.4f, 1f));

        dirLight = new DirectionalLight();
        dirLight = getDirectionToCubes();
        environment.add(dirLight); //-1f, -0.8f, -0.2f)); //

    }

    private DirectionalLight getDirectionToCubes() {
        Vector3 dir = new Vector3(originPosition.x - cam.position.x, originPosition.y - cam.position.y, originPosition.z - cam.position.z);
        dir = dir.nor();
        return dirLight.set(0.8f, 0.8f, 0.8f, dir.x, dir.y, dir.z);
    }



    private void setupCamera() {
        // setup camera
        cam = new PerspectiveCamera(40, Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
        cam.position.set(3f, 3f, 3f);
        cam.lookAt(originPosition);
        cam.up.set(0, 1, 0);
        //System.out.println("up vector = " + cam.up);
        cam.near = .0001f;
        cam.far = 300f;
        cam.update();
    }



    //JUST THE BODY NO USE ATM
    private void setupEventHandling() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyUp(final int keycode) {
                switch (keycode) {
                    default:
                        break;
                }
                return true;
            }
        });
    }
      


}