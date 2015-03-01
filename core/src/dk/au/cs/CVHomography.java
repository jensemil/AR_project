package dk.au.cs;

//import apple.laf.JRSUIConstants;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.calib3d.Calib3d.*;
import static org.opencv.imgproc.Imgproc.*;

public class CVHomography extends ApplicationAdapter {

    // 3D graphics
    private PerspectiveCamera cam;
    private Model cube;
    private ModelBuilder modelBuilder;
    private ModelBatch modelBatch;
    private ModelInstance[][] cubes;


    private Mat detectedEdges;
    private Mat warpedImage;
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

    private Size chessboardSize = new Size(9,6);
    //private double numOfCoords = chessboardSize.width*chessboardSize.height;
    private Size rectSize = new Size(2,2);
    private double numOfCoords = rectSize.width*rectSize.height;

    private static int SCREEN_WIDTH = 640;
    private static int SCREEN_HEIGHT = 480;

    private double width = Math.floor(chessboardSize.width / 2);
    private double height = chessboardSize.height - 1;
    private List<MatOfPoint2f> rects = new ArrayList<MatOfPoint2f>();
    private ModelInstance cubeInstance;

    private MatOfPoint3f rectObj;

    int count = 0;

    @Override
	public void create () {

        // Graphics

        originPosition = new Vector3(0.5f, 0.5f, 0.5f);

        // init model batch - used for rendering
        modelBatch = new ModelBatch();
        // setup model and build cube
        modelBuilder = new ModelBuilder();
        cubes = new ModelInstance[(int)Math.floor(chessboardSize.width / 2)][(int)chessboardSize.height - 1];
        setupCamera();
        setupEnvironment();
        setupCube();


        // OpenCV

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        detectedEdges = Mat.eye(128, 128, CvType.CV_8UC1);
        warpedImage = Mat.eye(SCREEN_WIDTH, SCREEN_WIDTH, CvType.CV_8UC1);

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


        rectObj = new MatOfPoint3f();
        rectObj.alloc((int)numOfCoords);
        rectObj.put(0,0, 0, 0, 0);
        rectObj.put(1,0, 1, 0, 0);
        rectObj.put(2,0, 1, 0, 1);
        rectObj.put(3,0, 0, 0, 1);

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

	@Override
	public void render () {
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

        MatOfPoint2f output = new MatOfPoint2f();
        output.alloc(4);
        output.put(0, 0, 0, 0);
        output.put(1,0,SCREEN_WIDTH,0);
        output.put(2,0,SCREEN_WIDTH,SCREEN_WIDTH);
        output.put(3,0,0,SCREEN_WIDTH);
        Mat homography = findHomography(src, output);
        warpPerspective(eye,warpedImage,homography,new Size(SCREEN_WIDTH, SCREEN_WIDTH));
        UtilAR.imShow(warpedImage);

    }

    private void findRectangles() {
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierachy = new Mat();
        Imgproc.cvtColor(eye, detectedEdges, Imgproc.COLOR_RGB2GRAY);
        threshold(detectedEdges, detectedEdges, 100, 255, THRESH_BINARY);
        findContours(detectedEdges, contours, hierachy, RETR_LIST, CHAIN_APPROX_SIMPLE);
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

    private void handleRectangles() {

        count++;


        for (MatOfPoint2f rect : rects) {
            
            Mat rotation = new Mat();
            Mat translation = new Mat();
            solvePnP(rectObj, rect, intrinsics, distortion, rotation, translation, false, ITERATIVE);

            if (count % 100 == 0) {
                //System.out.println("Coords:");
                //System.out.println("obj: " + rectObj.dump());
                //System.out.println("img: " + rect.dump());
                //System.out.println();
                //System.out.println("--------------------");
                //System.out.println("RT:");
                //System.out.println("R: " + rotation.dump());
                //System.out.println("T: " + translation.dump());
                //System.out.println("--------------------");
            }

            UtilAR.setCameraByRT(rotation, translation, cam);
            renderGraphics();
            drawHomography(rect);
        }


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



    private void renderGraphics() {
        // render model objects
        modelBatch.begin(cam);


        cubeInstance.transform.idt();
        //Vector3 position = new Vector3(0, 0.5f, 0);
        cubeInstance.transform.translate(originPosition);

        modelBatch.render(cubeInstance, environment);
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
        System.out.println("up vector = " + cam.up);
        cam.near = .0001f;
        cam.far = 300f;
        cam.update();
    }

    private void setupCube() {

        // setup material with texture
        mat = new Material(ColorAttribute.createDiffuse(new Color(0.3f, 0.3f,
                0.3f, 1.0f)));
        // blending
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA, 0.9f));

        cube = modelBuilder.createBox(1f, 1f, 1f, mat, Usage.Position
                | Usage.Normal | Usage.TextureCoordinates);


        cubeInstance = new ModelInstance(cube);
        cubeInstance.materials.get(0).set(ColorAttribute.createDiffuse(new Color(5 / (float)width, 4 / (float)height, 0.1f, 1.0f)));


    }

    private void doCanny() {
        Imgproc.cvtColor(eye, detectedEdges, Imgproc.COLOR_RGB2GRAY);
        blur(detectedEdges, detectedEdges, new Size(3,3));
        Imgproc.Canny(detectedEdges, detectedEdges, 50,100);
        UtilAR.imShow(detectedEdges);
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