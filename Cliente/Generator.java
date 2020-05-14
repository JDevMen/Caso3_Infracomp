package Cliente;

import uniandes.gload.core.LoadGenerator;
import uniandes.gload.examples.clientserver.generator.ClientServerTask;
import uniandes.gload.core.*;

public class Generator {

	/**
	 * Load Generator Service (From GLoad 1.0)
	 */
	private LoadGenerator generator;

	/**
	 * Constructor de la clase generator
	 */
	public Generator(){
		Task work = createTask();
		int numberOfTasks =400;
		int gapBetweenTasks = 20;
		generator = new LoadGenerator("Client -Server Load Test", numberOfTasks, work, gapBetweenTasks);
		generator.generate();
	}
	/**
	 * Ayuda a crear una tarea
	 * @return La tarea nueva creada
	 */
	public Task createTask(){
		return new ClienteCorrer();
	}

	public static void main(String ... args)
	{
		@SuppressWarnings("unused")
		Generator gen = new Generator();

	}
}