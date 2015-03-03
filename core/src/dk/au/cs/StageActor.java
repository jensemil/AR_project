package dk.au.cs;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

import java.util.ArrayList;
import java.util.List;

public class StageActor extends Actor {

    private List<Actor> actors;

    public StageActor(ModelInstance model, int id) {
        super(model, id);
        actors = new ArrayList<Actor>();
    }

    public void addActor(Actor actor) {
        actors.add(actor);
    }

    public void removeActor(Actor actor) {
        actors.remove(actor);
    }

    public boolean hasActor(Actor actor) {
        return actors.indexOf(actor) > -1;
    }

}
