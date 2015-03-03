package dk.au.cs;


import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
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


}
