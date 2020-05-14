package Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {
	
	public static final int PUERTO = 80;
	public static final String SERVIDOR = "172.24.99.191";
	
	public void run () throws IOException{
		Socket socket = null;
		PrintWriter escritor = null;
		BufferedReader lector = null;
		
		System.out.println("Cliente...");
		try{
			socket = new Socket(SERVIDOR, PUERTO);
			escritor = new PrintWriter(socket.getOutputStream(), true);
			lector = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		}
		catch (IOException e){
			e.printStackTrace();
			System.exit(-1);
		}
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		ClienteProtocoloSinSeguridad.procesar(stdIn, lector, escritor);
		
		stdIn.close();
		escritor.close();
		lector.close();
		socket.close();
	}
}
