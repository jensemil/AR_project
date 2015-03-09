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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.UBJsonReader;

import java.util.HashMap;

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


    private static int SCREEN_WIDTH = 640;
    private static int SCREEN_HEIGHT = 480;



    private MarkerHandler markerHandler;


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
        markerHandler = new MarkerHandler(SCREEN_WIDTH, SCREEN_HEIGHT, actorMap, this);
        setupCamera();
        setupEnvironment();

        soundHandler.start();
    }

    //Creates a hashmap containing all the actors.
    private void setupActorMap() {
        actorMap = new HashMap<Integer, Actor>();
        actorMap.put(0, new StageActor(new ModelInstance(createSquareModel(0)),0 ));
        actorMap.put(1, new Actor(new ModelInstance(createSquareModel(1)),1));
        actorMap.put(2, new Actor(new ModelInstance(createSquareModel(2)), 2));
        actorMap.put(3, new Actor(new ModelInstance(createSquareModel(3)), 3));
        actorMap.put(4, new Actor(new ModelInstance(createSquareModel(4)), 4));
    }


    //Creates a square model for an actor
    private Model createSquareModel(int id) {
        mat = new Material(ColorAttribute.createDiffuse(new Color(id / (float) 5, id / (float) 5, 0.1f, 1.0f)));
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA, 0.9f));

        Model model = modelBuilder.createBox(1f, 1f, 1f, mat, VertexAttributes.Usage.Position
                | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        return model;
    }




    @Override
	public void render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        dirLight = getDirectionToCubes();
        markerHandler.readCam();
        markerHandler.findRectangles();
        markerHandler.handleRectangles();
        renderGraphics();
    }



    private void renderGraphics() {
        Array<ModelInstance> instancesToRender = new Array<ModelInstance>();
        for(Actor actor : actorMap.values()) {
            if(actor.isActive()) {
                ModelInstance modelInstance = actor.getModelInstance();
                UtilAR.setTransformByRT(actor.getRotation(), actor.getTranslation(), modelInstance.transform);
                modelInstance.transform.translate(originPosition);
                instancesToRender.add(modelInstance);
            }
        }
        modelBatch.begin(cam);
        modelBatch.render(instancesToRender);
        modelBatch.end();

        StageActor stageActor = (StageActor) actorMap.get(0);
        for(Actor actor : actorMap.values()) {
            handleCollision(stageActor, actor);
        }
    }

    private void handleCollision(StageActor stageActor, Actor actor) {
        boolean actorIsCollidingWithStage = stageActor.isActive() &&
                actor.isActive() &&
                stageActor.checkForCollision(actor) &&
                stageActor.getId() < actor.getId();

        if (actorIsCollidingWithStage) {

            // if not already added
            if (!stageActor.hasActor(actor)) {
                stageActor.addActor(actor);
                // start music of actor
                System.out.println("start music for " + actor.getId());
                soundHandler.setInstrumentState(actor.getId() + "", "on");
            }


        } else {

            // if actor was already added
            if (stageActor.hasActor(actor)) {
                stageActor.removeActor(actor);
                //stop music of actor
                System.out.println("stop music for " + actor.getId());
                soundHandler.setInstrumentState(actor.getId() + "", "off");
            }
        }
    }

    public void setSoundLevel(double value) {
        double level = value / Math.PI + 0.2; // conversion
        soundHandler.setSoundLevel(level);
        System.out.println("soundLevel = " + soundHandler.getSoundLevel());
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
        markerHandler.releaseCamera();
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


    //Create a blender 3d model for an actor
    private Model createModel(String modelFileName) {
        // Model loader needs a binary json reader to decode
        UBJsonReader jsonReader = new UBJsonReader();
        // Create a model loader passing in our json reader
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
        Model model = modelLoader.loadModel(Gdx.files.getFileHandle(modelFileName, Files.FileType.Internal));
        return model;
    }

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