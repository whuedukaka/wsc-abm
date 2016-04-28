package netlogo;

import java.util.List;

import org.nlogo.api.WorldDimensions;
import org.nlogo.headless.HeadlessWorkspace;

/**
 * Multi-threaded NetLogo Java executor
 * 
 * @author Peng
 * 
 */
public class Execution implements Runnable {
	// Path (including the name of model file) to the model
	private String model_path;
	// A list of commands to be executed in order after the model initialization
	private List<String> cmds;

	private boolean setDimension = false;
	// NetLog 'world' dimension
	private int minPxcor, minPycor, maxPxcor, maxPycor;

	public Execution(String model_path, String[] cmds) {
		this.model_path = model_path;
	}

	public Execution(String model_path, int minPxcor, int minPycor,
			int maxPxcor, int maxPycor, List<String> cmds) {
		this.model_path = model_path;

		this.setDimension = true;
		this.maxPxcor = maxPxcor;
		this.maxPycor = maxPycor;
		this.minPxcor = minPxcor;
		this.minPycor = minPycor;

		this.cmds = cmds;
	}

	/**
	 * Method invoked by each thread to run an instance of NetLogo with given
	 * model and cmds
	 */
	@Override
	public void run() {
		HeadlessWorkspace workspace = HeadlessWorkspace.newInstance();

		try {
			try {
				/*
				 * Load the model into NetLogo workspace
				 */
				workspace.open(model_path);

				/*
				 * Set the model's world dimension
				 */
				if (setDimension) {
					WorldDimensions dim = new WorldDimensions(minPxcor,
							maxPxcor, minPycor, maxPycor);
					workspace.setDimensions(dim);
				}

				// might need to clear
				// workspace.command("ca");

				/*
				 * Execute the commands one by one
				 */
				for (String cmd : cmds) {
					workspace.command(cmd);
				}
			} finally {
				workspace.dispose();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {

		}
	}
}
