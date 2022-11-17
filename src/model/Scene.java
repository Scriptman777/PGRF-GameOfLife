package model;

import solids.AbstractRenderable;

import java.util.ArrayList;

public class Scene {
    private ArrayList<AbstractRenderable> solids = new ArrayList<>();

    public void add(AbstractRenderable solid){
        solids.add(solid);
    }

    public ArrayList<AbstractRenderable> getSolids() {
        return solids;
    }
}
