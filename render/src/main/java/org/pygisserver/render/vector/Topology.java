package org.pygisserver.render.vector;

import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Map;

import org.pygisserver.render.vector.TopoGeom.GeometryColleciton;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;

public class Topology {

    private List<LineString> arcs;

    private AffineTransform screenToWorld;

    private Envelope envelope;

    private Map<String, GeometryColleciton> layers;

    public Topology(
            AffineTransform screenToWorld,
            List<LineString> arcs,
            Map<String, TopoGeom.GeometryColleciton> layers) {

        this.screenToWorld = screenToWorld;
        this.arcs = arcs;
        this.layers = layers;
    }

    public AffineTransform getScreenToWorldTransform() {
        return screenToWorld;
    }

    public List<LineString> getArcs() {
        return arcs;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public Map<String, TopoGeom.GeometryColleciton> getLayers() {
        return layers;
    }
}
