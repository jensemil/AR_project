package dk.au.cs;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Birk on 07-03-2015.
 * This class encapsulates the handling of,
 * fixing the rotation to allways have upper left
 * corner as the upper left.
 */
public class RotationHandler {
    private List<MatOfPoint3f> rectObjs;
    private MatOfPoint2f homoWorld;

    public RotationHandler(int numOfCoords, MatOfPoint2f homoWorld) {
        rectObjs = new ArrayList<MatOfPoint3f>();
        for (int i = 0; i < 4; i++) {
            MatOfPoint3f rectObj = new MatOfPoint3f();
            rectObj.alloc(numOfCoords);

            // JEG ---- now counter-clockwise!!
            rectObj.put(i % 4,0, 0, 0, 0);
            rectObj.put((i+1) % 4,0, 1, 0, 0);
            rectObj.put((i+2) % 4,0, 1, 0, 1);
            rectObj.put((i+3) % 4,0, 0, 0, 1);
            rectObjs.add(rectObj);
        }
        this.homoWorld = homoWorld;
    }

    //Gives the correct orientation of our square
    public MatOfPoint3f getObjCoords(MatOfPoint2f polygon) {
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
}
