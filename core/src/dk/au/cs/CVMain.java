package dk.au.cs;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;

import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.blur;
import static org.opencv.imgproc.Imgproc.cvtColor;
import java.util.List;

import static org.opencv.calib3d.Calib3d.*;

public class CVMain extends ApplicationAdapter {

    // 3D graphics
    private PerspectiveCamera cam;
    private Model cube;
    private ModelBuilder modelBuilder;
    private ModelBatch modelBatch;
    private ModelInstance cubeInstance;

    private Mat videoInput;
    private Mat detectedEdges;
    private Environment environment;
    private Material mat;
    private Vector3 cubePosition;



    // OpenCV

    private VideoCapture cap;
    private MatOfPoint2f eye;
    private MatOfPoint2f corners;

    private MatOfPoint3f objectCoords;

    private Mat intrinsics;
    private MatOfDouble distortion;

    private Size chessboardSize = new Size(9,6);
    private double numOfCoords = chessboardSize.width*chessboardSize.height;

    private static int SCREEN_WIDTH = 640;
    private static int SCREEN_HEIGHT = 480;



    @Override
	public void create () {

        // Graphics

        //cubePosition = new Vector3(0.f, 0.f, 0.f);
        cubePosition = new Vector3(0.5f, 0.5f, 0.5f);


        // init model batch - used for rendering
        modelBatch = new ModelBatch();
        // setup model and build cube
        modelBuilder = new ModelBuilder();

        setupEnvironment();
        setupCamera();
        setupCube();



        // OpenCV

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        detectedEdges = Mat.eye(128, 128, CvType.CV_8UC1);
        videoInput = Mat.eye(128, 128, CvType.CV_8UC1);


        eye = new MatOfPoint2f(); //.eye(128, 128, CvType.CV_8UC1);
        corners = new MatOfPoint2f();


        // setup video capture
        cap = new VideoCapture(0);
        cap.set(Highgui.CV_CAP_PROP_FRAME_WIDTH,SCREEN_WIDTH);
        cap.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT,SCREEN_HEIGHT);


        // get intrinsics after view capture dimensions set
        objectCoords = new MatOfPoint3f();
        objectCoords.alloc((int)numOfCoords);
        intrinsics = UtilAR.getDefaultIntrinsicMatrix(SCREEN_WIDTH, SCREEN_HEIGHT);
        distortion = UtilAR.getDefaultDistortionCoefficients();

        //Ensure Camera is ready!
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(!cap.isOpened()){
            System.out.println("Camera Error");
        }
        else{
            System.out.println("Camera OK");
        }
    }

	@Override
	public void render () {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);


        // read camera data into "eye matrix"
        cap.read(eye);
        // render eye texture
        UtilAR.imDrawBackground(eye);
        handleCheckboard();
        renderGraphics();

        //doCanny();

	}

    private void handleCheckboard() {
        // find chessboard in the rendered image
        boolean found = findChessboardCorners(eye, chessboardSize, corners, CALIB_CB_ADAPTIVE_THRESH + CALIB_CB_NORMALIZE_IMAGE + CALIB_CB_FAST_CHECK);

        // draw on chessboard
        //drawChessboardCorners(eye, chessboardSize, corners, found);

        if (corners.size().height > 0) {

            double scale = 1.0; // the unit of the chessboard


            for (int j = 0; j < corners.size().height; j++) {
                double row = Math.floor(j / chessboardSize.width);
                double col = j % chessboardSize.width;

                // set Y = 0, because we usually draw it with Y as the up-axis
                objectCoords.put(j, 0, scale * col, 0.0, scale * row);
            }

            Mat rotation = new Mat();
            Mat translation = new Mat();

            if (corners.size().height < numOfCoords) {
                System.err.println("Not all of the chessboard is visible");
            } else {
                solvePnP(objectCoords, corners, intrinsics, distortion, rotation, translation, false, ITERATIVE);


                UtilAR.setCameraByRT(rotation, translation, cam);

            }

        }
    }

    private void renderGraphics() {


        // render model objects
        modelBatch.begin(cam);
        cubeInstance.transform.idt();
        cubeInstance.transform.translate(cubePosition);
        modelBatch.render(cubeInstance, environment);
        modelBatch.end();


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
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f,
                -0.8f, -0.2f));

    }

    private void setupCamera() {
        // setup camera
        cam = new PerspectiveCamera(40, Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
        cam.position.set(3f, 3f, 3f);
        cam.lookAt(cubePosition);
        cam.up.set(0, 1, 0);
        System.out.println("up vector = " + cam.up);
        cam.near = .0001f;
        cam.far = 300f;
        cam.update();
    }

    private void setupCube() {

        // setup material with texture
        //img = new Texture("emo2.jpg");
        //img.bind();
        mat = new Material(ColorAttribute.createDiffuse(new Color(0.9f, 0.9f,
                0.9f, 1.0f)));
        //mat.set(new TextureAttribute(TextureAttribute.Diffuse, img));
        // blending
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA, 1.0f));

        cube = modelBuilder.createBox(1f, 1f, 1f, mat, Usage.Position
                | Usage.Normal | Usage.TextureCoordinates);

        cubeInstance = new ModelInstance(cube);


    }

    private void doCanny() {
        cap.read(videoInput);
        Imgproc.cvtColor(videoInput, detectedEdges, Imgproc.COLOR_RGB2GRAY);
        blur(detectedEdges, detectedEdges, new Size(3,3));
        Imgproc.Canny(detectedEdges, detectedEdges, 50,100);
        UtilAR.imShow(detectedEdges);
    }



    private void printMat(Mat mat) {

        System.out.println("------------------------ Print matrix: -------");



        for (int j = 0; j < mat.size().height; j++) {

            String p0s = "";
            double[] p0 = mat.get(j , 0);
            for (int i = 0; i < p0.length; i++) {
                p0s += p0[i] + ",";

            }
            System.out.println("Row " + j + ": " + p0s);
        }



        //System.out.println("p3 = " + p3s);
    }

}


