package dk.au.cs;


import com.badlogic.gdx.graphics.g3d.ModelInstance;
import org.opencv.core.Mat;

public class Actor {

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

    public void setModelInstance(ModelInstance model) {
        this.model = model;
    }

    private Mat rotation;
    private Mat translation;
    private ModelInstance model;
    private int id;

    public Actor(ModelInstance model, int id) {
        this.model = model;
        this.id = id;
    }

    public boolean isActive() {
        return rotation != null;
    }

    public int getId() {
        return id;
    }
}
