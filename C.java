
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.opencsv.CSVWriter;

import javafx.util.converter.LocalDateTimeStringConverter;
import sun.util.resources.LocaleData;

/*
 * Clase principal, esta es la clase que tiene el ejecutable
 * Aquí se genera inicializa el puerto que va a usar para comunicarse
 * Además, aquí es que se generan los threads delegados, cada uno atiende 
 * a solo un cliente de inicio a fin.
 * Por el momento trabaja como un for infinito que crea delegados a medida que
 * van llegando clientes
 */
public class C {
	private static ServerSocket ss;	
	private static final String MAESTRO = "MAESTRO: ";
	private static X509Certificate certSer; /* acceso default */
	private static KeyPair keyPairServidor; /* acceso default */

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// Se inicializa el reader y el writer, además define el puerto de comunicación
		System.out.println("Cuantos servidores delegados quiere?");
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		int cantServidores = Integer.parseInt(br.readLine());
		System.out.println(MAESTRO + "Establezca puerto de conexion:");
		int ip = Integer.parseInt(br.readLine());
		System.out.println(MAESTRO + "Empezando servidor maestro en puerto " + ip+", con "+cantServidores+" servidores delegados.");
		// Adiciona la libreria como un proveedor de seguridad.
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());		

		
		//Hora de la prueba
		
		long ts = System.currentTimeMillis();
		LocalDateTime fecha = LocalDateTime.now();
		String hora = Integer.toString(fecha.getHour());
		String minuto = Integer.toString(fecha.getMinute());
		// Crea el archivo de log
		File file = null;
		File csvFile = null;
		keyPairServidor = S.grsa();
		certSer = S.gc(keyPairServidor); 
		String ruta = "./resultados.txt";
		String rutaCsv = "./resultadosCSV"+hora+"-"+minuto+".csv";

		file = new File(ruta);
		if (!file.exists()) {
			file.createNewFile();
		}
		
		csvFile = new File (rutaCsv);
		if(!csvFile.exists())
		{
			csvFile.createNewFile();
		}
		
		FileWriter fw = new FileWriter(file);
		fw.close();
		
		FileWriter fwCsv = new FileWriter(csvFile);
		System.out.println("Aquí cree el triple perro csv ");
		CSVWriter writerCsv = new CSVWriter(fwCsv);
		String[] header = { "Cliente", "Tiempo respuesta", "Uso CPU", "Transacciones perdidas" };
		writerCsv.writeNext(header);
		writerCsv.close();

		D.init(certSer, keyPairServidor,file,csvFile);

		// Crea el socket que escucha en el puerto seleccionado.
		ss = new ServerSocket(ip);
		System.out.println(MAESTRO + "Socket creado.");



		/*
		 * Ciclo para crear los servidores delegados
		 */

		//		for (int i = 0; i < cantServidores-1; i++) 
		//		{
		//			D d = new D(null,i);
		//			listaServDelegados.add(d);
		//		}

		ThreadPoolExecutor deadpools = (ThreadPoolExecutor) Executors.newFixedThreadPool(cantServidores);

		/*
		 * Aquí estoy poniendo el ciclo para aceptar conexiones a los servidores delegados
		 */
		for(int i= 0 ; true; i++)
		{
			try { 
				Socket sc = ss.accept();
				D dingo = new D(sc,i);
				deadpools.execute(dingo);
				System.out.println(MAESTRO + "Cliente aceptado.");

			} catch (IOException e) {
				System.out.println(MAESTRO + "Error creando el socket cliente.");
				e.printStackTrace();
			}
		}
		/*(
		 * Aquí se crean los threads delegados. 
		 * Esto lo tenemos que modificar para que sea un pool de threads
		 * como en el CASO 1
		 */
		//		for (int i=0;true;i++) {
		//			try { 
		//				Socket sc = ss.accept();
		//				System.out.println(MAESTRO + "Cliente " + i + " aceptado.");
		//				D d = new D(sc,i);
		//				d.start();
		//			} catch (IOException e) {
		//				System.out.println(MAESTRO + "Error creando el socket cliente.");
		//				e.printStackTrace();
		//			}
		//			
		//		}


	}
}
