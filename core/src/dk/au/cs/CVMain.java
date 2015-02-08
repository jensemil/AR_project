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
import com.sun.tools.javac.util.Assert;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;

import java.util.List;

import static org.opencv.calib3d.Calib3d.*;

public class CVMain extends ApplicationAdapter {

    // 3D graphics
    PerspectiveCamera cam;
    Model cube;
    ModelBuilder modelBuilder;
    ModelBatch modelBatch;

    Environment environment;
    Material mat;
    Vector3 cubePosition;



    // OpenCV

    VideoCapture cap;
    MatOfPoint2f eye;
    MatOfPoint2f corners;

    MatOfPoint3f objectCoords;
    MatOfPoint2f imgCoords;

    Mat intrinsics;
    MatOfDouble distortion;


    @Override
	public void create () {

        // Graphics

        cubePosition = new Vector3(1.0f, 0.5f, 0.75f);


        // init model batch - used for rendering
        modelBatch = new ModelBatch();
        // setup model and build cube
        modelBuilder = new ModelBuilder();

        setupEnvironment();
        setupCamera();
        setupCube();



        // OpenCV

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);


        eye = new MatOfPoint2f(); //.eye(128, 128, CvType.CV_8UC1);
        corners = new MatOfPoint2f();

        eye.alloc(1);
        corners.alloc(1);


        objectCoords = new MatOfPoint3f();
        imgCoords = new MatOfPoint2f();
        objectCoords.alloc(4);
        imgCoords.alloc(4);
        intrinsics = UtilAR.getDefaultIntrinsicMatrix(eye.width(), eye.height());
        distortion = UtilAR.getDefaultDistortionCoefficients();



        // setup video capture
        cap = new VideoCapture(0);
        cap.set(Highgui.CV_CAP_PROP_FRAME_WIDTH,640);
        cap.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT,480);
        cap.read(eye);

        System.out.println("eye = " + eye);

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

        // find chessboard in the rendered image
        Size patternSize = new Size(9, 6);
        boolean found = findChessboardCorners(eye, patternSize, corners, CALIB_CB_ADAPTIVE_THRESH + CALIB_CB_NORMALIZE_IMAGE + CALIB_CB_FAST_CHECK);

        /*if (found) {
            System.out.println("chessboard identified");
        } else {
            System.err.println("chessboard not identified");
        }                          */

        // draw on chessboard
        drawChessboardCorners(eye, patternSize, corners, found);

        // render eye texture to screen
        UtilAR.imDrawBackground(eye);

        if (corners.size().height > 0) {


            // printCorners();


            /*Mat m = corners.rowRange(0, 0);
            imgCoords.push_back(m);
            m = corners.rowRange(1, 1);
            imgCoords.push_back(m);
            m = corners.rowRange(2, 2);
            imgCoords.push_back(m);
            m = corners.rowRange(3, 3);
            imgCoords.push_back(m);*/

            //System.out.println("imgCoords = " + imgCoords );


            imgCoords = new MatOfPoint2f(corners.rowRange(0,4));


            //objectCoords = new MatOfPoint3f(Mat.zeros(3, 1, CvType.CV_32F));

            //System.out.println("imgSize=" + imgCoords.height());
            //System.out.println("coordSize=" + objectCoords.height());

            Mat rotation = new Mat();
            Mat translation = new Mat();



            solvePnP(objectCoords, imgCoords, intrinsics, distortion, rotation, translation, false, ITERATIVE);

            //printMat(rotation);
            //System.out.println(rotation.toString());

            UtilAR.setCameraByRT(rotation, translation, cam);

        }



        renderGraphics();

	}

    private void printMat(Mat mat) {
        double[] p0 = mat.get(0, 0);
        double[] p1 = mat.get(1, 0);
        double[] p2 = mat.get(2, 0);
        //double[] p3 = mat.get(3, 0);


        String p0s, p1s, p2s, p3s;
        p0s = p1s = p2s = p3s = "";
        for (int i = 0; i < p0.length; i++) {
            p0s += p0[i] + ",";
            p1s += p1[i] + ",";
            p2s += p2[i] + ",";
            //p3s += p3[i] + ",";
        }

        System.out.println("p0 = " + p0s);
        System.out.println("p1 = " + p1s);
        System.out.println("p2 = " + p2s);
        //System.out.println("p3 = " + p3s);
    }

    private void renderGraphics() {


        cam.lookAt(cubePosition);
        cam.up.set(0, 1, 0);
        cam.update();


        // render model objects
        modelBatch.begin(cam);
        ModelInstance cubeInstance = new ModelInstance(cube);
        cubeInstance.transform.translate(cubePosition);
        modelBatch.render(cubeInstance, environment);
        modelBatch.end();


    }

    @Override
    public void dispose() {
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
        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
        cam.position.set(3f, 3f, 3f);
        cam.lookAt(cubePosition);
        cam.up.set(0, 1, 0);
        System.out.println("up vector = " + cam.up);
        cam.near = 1f;
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



    }
}
