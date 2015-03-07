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
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.UBJsonReader;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.opencv.calib3d.Calib3d.*;
import static org.opencv.imgproc.Imgproc.*;

public class CVMain extends ApplicationAdapter {


    private SoundHandler soundHandler = new SoundHandler();
    // 3D graphics
    private PerspectiveCamera cam;
    private ModelBuilder modelBuilder;
    private ModelBatch modelBatch;
    private ModelInstance modelInstance;
    private AnimationController controller;
    private Environment environment;
    private Material mat;
    private Vector3 originPosition;
    private DirectionalLight dirLight;

    private HashMap<Integer, Actor> actorMap;


    // OpenCV
    private VideoCapture cap;
    private MatOfPoint2f eye;
    private MatOfPoint2f corners;
    private MatOfPoint2f warpedImage; //The image result of the homography
    private Mat intrinsics;
    private MatOfDouble distortion;
    private Size rectSize = new Size(2,2);
    private double numOfCoords = rectSize.width*rectSize.height;
    private static int SCREEN_WIDTH = 640;
    private static int SCREEN_HEIGHT = 480;

    private List<MatOfPoint2f> rects = new ArrayList<MatOfPoint2f>();
    private List<MatOfPoint3f> rectObjs;
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
        setupActorMap();
        setupCamera();
        setupEnvironment();

        // OpenCV

        //The homography matrix
        homoWorld = new MatOfPoint2f();
        homoWorld.alloc(4);
        homoWorld.put(0, 0, 0, 0);
        homoWorld.put(1, 0, SCREEN_WIDTH, 0);
        homoWorld.put(2, 0, SCREEN_WIDTH, SCREEN_WIDTH);
        homoWorld.put(3, 0, 0, SCREEN_WIDTH);

        warpedImage = new MatOfPoint2f();

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

        setupRectObjs();

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
        soundHandler.start();
    }

    //Creates a hashmap containing all the actors.
    private void setupActorMap() {
        actorMap = new HashMap<Integer, Actor>();
        actorMap.put(0, new StageActor(new ModelInstance(createSquareModel()),0 ));
        actorMap.put(1, new Actor(new ModelInstance(createSquareModel()),1));
        actorMap.put(2, new Actor(new ModelInstance(createSquareModel()), 2));
        actorMap.put(3, new Actor(new ModelInstance(createSquareModel()), 3));
        actorMap.put(4, new Actor(new ModelInstance(createSquareModel()), 4));
    }


    //Creates a square model for an actor
    private Model createSquareModel() {
        mat = new Material(ColorAttribute.createDiffuse(new Color(0.3f, 0.3f,
                0.3f, 1.0f)));
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA, 0.9f));

        Model model = modelBuilder.createBox(1f, 1f, 1f, mat, VertexAttributes.Usage.Position
                | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        return model;
    }

    //Create a blender 3d model for an actor
    private Model createModel(String modelFileName) {
        // Model loader needs a binary json reader to decode
        UBJsonReader jsonReader = new UBJsonReader();
        // Create a model loader passing in our json reader
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
        Model model = modelLoader.loadModel(Gdx.files.getFileHandle(modelFileName, Files.FileType.Internal));
        return model;
    }

    //This is used to get the right orientation
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

    @Override
	public void render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        dirLight = getDirectionToCubes();
        cap.read(eye);
        findRectangles();
        handleRectangles();
    }

    //<---- Finding our rectangles --->

    //This is where opecv find the actual contours
    private List<MatOfPoint> findContoursFromEdges(MatOfPoint2f input) {
        Mat detectedEdges = Mat.eye(128, 128, CvType.CV_8UC1);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierachy = new Mat();
        Imgproc.cvtColor(input, detectedEdges, Imgproc.COLOR_RGB2GRAY);
        threshold(detectedEdges, detectedEdges, 100, 255, THRESH_BINARY);
        findContours(detectedEdges, contours, hierachy, RETR_LIST, CHAIN_APPROX_SIMPLE);
        return contours;
    }

    //This is where we find our rectangles and put them in the field -> rects
    private void findRectangles() {
        List<MatOfPoint> contours = findContoursFromEdges(eye);
        List<MatOfPoint> rectContours = new ArrayList<MatOfPoint>();
        rects = new ArrayList<MatOfPoint2f>();
        for(MatOfPoint cont : contours) {
            MatOfPoint2f cont2f = new MatOfPoint2f(cont.toArray());
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

    //<---- Handle Found Rectangles --->


    private void drawHomography(MatOfPoint2f src) {
        Mat homography = findHomography(src, homoWorld);
        warpPerspective(eye, warpedImage, homography, new Size(SCREEN_WIDTH, SCREEN_WIDTH));
    }

    private void handleRectangles() {
        clearActorMapRT();
        for (MatOfPoint2f rect : rects) {
            drawHomography(rect); //puts homography onto warpedImage
            List<MatOfPoint2f> idPolygons = findIdPolygon(); // should only contain 1 element!
            //Extract the id from the polygon
            MatOfPoint3f rectObjCoords = null;
            int theId = -1;
            if (idPolygons.size() == 1) {
                MatOfPoint2f polygon = idPolygons.get(0);
                rectObjCoords = getObjCoords(polygon);
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
            }
        }
        renderGraphics();
    }

    //Resets translation and rotation of all Actors.
    private void clearActorMapRT() {
        for(Actor actor : actorMap.values()) {
            actor.setTranslation(null);
            actor.setTranslation(null);
            actor.setRotation(null);
        }
    }

    //Gives the correct orientation of our square
    private MatOfPoint3f getObjCoords(MatOfPoint2f polygon) {
        int idx = findOrigin(polygon);
        return rectObjs.get(idx);
    }

    //This returns the index of the squares upper left corner (closest to the left corner in our figure)
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

    //Find the upper left corner in our figure
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
        return maxP;
    }


    private void renderGraphics() {
        Array<ModelInstance> modelInstances = new Array<ModelInstance>();
        for(Actor actor : actorMap.values()) {
            if(actor.isActive()) {
                ModelInstance modelInstance = actor.getModelInstance();
                UtilAR.setTransformByRT(actor.getRotation(), actor.getTranslation(), modelInstance.transform);
                modelInstance.transform.translate(originPosition);
                modelInstance.materials.get(0).set(ColorAttribute.createDiffuse(new Color(actor.getId() / (float) 5, actor.getId() / (float) 5, 0.1f, 1.0f))); //Måske vi bare skulle sætte dette når actor laves?
                modelInstances.add(modelInstance);
            }
        }
        modelBatch.begin(cam);
        modelBatch.render(modelInstances);
        modelBatch.end();

        StageActor stageActor = (StageActor) actorMap.get(0);
        for(Actor actor : actorMap.values()) {
            handleCollision(stageActor, actor);
        }
    }

    private void handleCollision(StageActor stageActor, Actor actor) {
        if (stageActor.isActive() &&
                actor.isActive() &&
                stageActor.checkForCollision(actor) &&
                stageActor.getId() < actor.getId()) {

            if (!stageActor.hasActor(actor)) {
                stageActor.addActor(actor);
                // start music of actor
                System.out.println("start music for " + actor.getId());
                soundHandler.setInstrumentState(actor.getId() + "", "on");
            }


        } else {
            if (stageActor.hasActor(actor)) {
                stageActor.removeActor(actor);
                //stop music of actor
                System.out.println("stop music for " + actor.getId());
                soundHandler.setInstrumentState(actor.getId() + "", "off");
            }
        }
    }

    //<---- LIBGDX main class stuff --->

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

    //<---- GRAPHICS --->

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
        cam = new PerspectiveCamera(40, Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
        cam.position.set(3f, 3f, 3f);
        cam.lookAt(originPosition);
        cam.up.set(0, 1, 0);
        cam.near = .0001f;
        cam.far = 300f;
        cam.update();
        UtilAR.setNeutralCamera(cam);
    }

    //<---- UNUSED --->

    //Used for animations. Not used at the moment
    private void animateSquare() {
        controller.setAnimation("Cube|fadeOut", 1, new AnimationController.AnimationListener() {
            @Override
            public void onEnd(AnimationController.AnimationDesc animation) {
                controller.queue("Cube|fadeIn", 1, 1f, null, 0f);
            }
            @Override
            public void onLoop(AnimationController.AnimationDesc animation) {
            }
        });
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