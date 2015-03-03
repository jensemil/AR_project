package dk.au.cs.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import dk.au.cs.CVChessboard;
import dk.au.cs.CVMain;
import org.opencv.core.Core;

public class DesktopLauncher {
	public static void main (String[] arg) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(new CVMain(), config);
	}
}
