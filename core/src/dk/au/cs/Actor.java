package dk.au.cs;


import com.badlogic.gdx.graphics.g3d.Model;
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

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    private Mat rotation;
    private Mat translation;
    private Model model;
    private int id;

    public Actor(Model model, int id) {
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
