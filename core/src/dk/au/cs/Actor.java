package dk.au.cs;


import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.UBJsonReader;
import org.opencv.core.Mat;

public class Actor {

    private final ModelInstance levelModel;
    private Mat rotation;
    private Mat translation;
    private ModelInstance model;
    private int id;
    private double level;
    private AnimationController controller;

    public Actor(int id, ModelBuilder modelBuilder, String modelFileName) {
        this.id = id;
        if(modelFileName.equals("")) {
            this.model = new ModelInstance(createSquareModel(id, modelBuilder));
            this.levelModel = new ModelInstance(createLevelModel(id, modelBuilder));
        } else {
            this.model = new ModelInstance(createModel(modelFileName));
            controller = new AnimationController(this.model);
            animateSquare();
            this.levelModel = new ModelInstance(createLevelModel(id, modelBuilder));
        }

    }

    public void animate() {
        if(controller != null) {
            controller.update(Gdx.graphics.getDeltaTime());
        }
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

    //Used for animations. Not used at the moment
    private void animateSquare() {
        controller.setAnimation("Cube|cubeAction", 1, new AnimationController.AnimationListener() {
            @Override
            public void onEnd(AnimationController.AnimationDesc animation) {
                controller.queue("Cube|cubeAction", -1, 1f, null, 0f);
            }
            @Override
            public void onLoop(AnimationController.AnimationDesc animation) {
            }
        });
    }

    private Model createLevelModel(int id, ModelBuilder modelBuilder) {
        Material mat = new Material(ColorAttribute.createDiffuse(new Color(id / (float) 5, id / (float) 5, 0.1f, 1.0f)));
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA, 0.9f));


        Model model = modelBuilder.createBox(1f, 1f, 1f, mat, VertexAttributes.Usage.Position
                | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        return model;
    }

    //Creates a square model for an actor
    private Model createSquareModel(int id, ModelBuilder modelBuilder) {
        Material mat = new Material(ColorAttribute.createDiffuse(new Color(id / (float) 5, id / (float) 5, 0.1f, 1.0f)));
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA, 0.9f));

        Model model = modelBuilder.createBox(1f, 1f, 1f, mat, VertexAttributes.Usage.Position
                | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        return model;
    }

    public boolean checkForCollision(Actor other) {
        Vector3 pos1 = new Vector3();
        Vector3 pos2 = new Vector3();
        this.getModelInstance().transform.getTranslation(pos1);
        other.getModelInstance().transform.getTranslation(pos2);

        float dist = pos1.dst(pos2);
        //System.out.println("distance between " + this.getId() + " and " + other.getId() + ": " + dist);

        return dist < 2.f;
    }

    public boolean isActive() {
        return rotation != null;
    }

    public int getId() {
        return id;
    }

    public boolean equals(Actor o) {
        return this.id == o.id;
    }

    public Mat getRotation() {
        return rotation;
    }

    public void setRotation(Mat rotation) {
        this.rotation = rotation;
    }

    public Mat getTranslation() {
        return translation;
    }

    public void setTranslation(Mat translation) {
        this.translation = translation;
    }

    public ModelInstance getModelInstance() {
        return model;
    }

    public void setLevel(double value) {
        this.level = value;
    }

    public double getLevel() {
        return this.level;
    }

    public ModelInstance getLevelModelInstance() {
        return levelModel;
    }

}
