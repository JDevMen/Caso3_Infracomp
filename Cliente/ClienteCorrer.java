package Cliente;

import uniandes.gload.core.Task;
import uniandes.gload.examples.clientserver.Client;

public class ClienteCorrer extends Task{
	

	public void execute() 
	{
		try{
		Cliente cliente = new Cliente();
		cliente.run();
		}
		catch (Exception e) {
		}
	}

	@Override
	public void fail() {
		// TODO Auto-generated method stub
		System.out.println(Task.MENSAJE_FAIL);
		
	}

	@Override
	public void success() {
		System.out.println(Task.OK_MESSAGE);
		
	}
}