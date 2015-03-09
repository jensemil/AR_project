package dk.au.cs;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import org.opencv.core.Mat;

public class Actor {

    private final ModelInstance levelModel;
    private Mat rotation;
    private Mat translation;
    private ModelInstance model;
    private int id;
    private double level;

    public Actor(int id, ModelBuilder modelBuilder) {
        this.model = new ModelInstance(createSquareModel(id, modelBuilder));
        this.id = id;
        this.levelModel = new ModelInstance(createLevelModel(id, modelBuilder));
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
