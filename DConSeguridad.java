
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.security.KeyPair;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.bind.DatatypeConverter;

import com.opencsv.CSVWriter;

/*
 * En esta clase se define el protocolo con el que se comunica el servidor 
 * con el cliente. 
 * Ante todo es la estructura del thread delegado.
 */
public class DConSeguridad implements Runnable {
	// Constantes de respuesta 
	public static final String OK = "OK";
	public static final String ALGORITMOS = "ALGORITMOS";
	public static final String CERTSRV = "CERTSRV";
	public static final String CERCLNT = "CERCLNT";
	public static final String SEPARADOR = ":";
	public static final String HOLA = "HOLA";
	public static final String INICIO = "INICIO";
	public static final String ERROR = "ERROR";
	public static final String REC = "recibio-";
	public static final String ENVIO = "envio-";

	// Atributos
	private Socket sc = null;
	private String dlg;
	private byte[] mybyte;
	private static X509Certificate certSer;
	private static KeyPair keyPairServidor;
	private static File file;
	private static File fileCsv;
	public static final int numCadenas = 13;
	private long id = 0;
	private String[] datos = new String[4];
	private ArrayList<Double> usoCPU = new ArrayList<>();

	//Cambiar socket del servidor delegado
	public void cambiarSocket(Socket pSc)
	{
		sc = pSc;
	}

	public boolean noHaySocket()
	{
		return sc ==null;
	}


	//Define lo básico del servidor para manejar criptografía con el cliente
	public static void init(X509Certificate pCertSer, KeyPair pKeyPairServidor, File pFile, File pFileCsv) {
		certSer = pCertSer;
		keyPairServidor = pKeyPairServidor;
		file = pFile;
		fileCsv = pFileCsv;
	}

	/*
	 * Método principal para iniciar el thread, recibe el socket por el cual
	 * se va a comunidar y un id que lo identifique (asignado por el servidor)
	 * Abrá que hacer ajustes para que el log quede por bloques de cada delegado
	 */
	public DConSeguridad (Socket csP, int idP) {
		sc = csP;
//		long idP = Thread.currentThread().getId();
		System.out.println("LLEGUE UNA VEZ " + idP);
		this.id = idP;
		dlg = new String("delegado " + idP + ": ");
		try {
			mybyte = new byte[520]; 
			mybyte = certSer.getEncoded();
		} catch (Exception e) {
			System.out.println("Error creando el thread" + dlg);
			e.printStackTrace();
		}
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	//Solo revisa si la respuesta corresponde a un HMAC valido
	private boolean validoAlgHMAC(String nombre) {
		return ((nombre.equals(S.HMACMD5) || 
				nombre.equals(S.HMACSHA1) ||
				nombre.equals(S.HMACSHA256) ||
				nombre.equals(S.HMACSHA384) ||
				nombre.equals(S.HMACSHA512)
				));
	}

	/*
	 * Generacion del archivo log. 
	 * Nota: 
	 * - Debe conservar el metodo . 
	 * - Es el ÃƒÂºnico metodo permitido para escribir en el log.
	 */
	//Tal cual, este método lo único que hace es escribir sobre el log.
	private void escribirMensaje(String pCadena) {

		try {
			FileWriter fw = new FileWriter(file,true);
			fw.write(pCadena + "\n");
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private void escribirMensajeCSV(String[] pCadena) {

		try {
			FileWriter fw = new FileWriter(fileCsv,true);
			CSVWriter writer = new CSVWriter(fw);
			writer.writeNext(pCadena);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/*
	 * Método run, tu deberías acordarte pero sino lo que tiene es el hilo de 
	 * ejecución del thread. Básicamente como va a ejecutar los demás métodos
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		
		String[] cadenas;
		cadenas = new String[numCadenas];

		//variable toma de datos
		long tiempoInicio = 0;
		long tiempoFin = 0;
		long tiempoTransaccion = 0;
		int  transaccionTerminada = 0;
		
		String linea;
		System.out.println(dlg + "Empezando atencion.");
		try {

			PrintWriter ac = new PrintWriter(sc.getOutputStream() , true);
			BufferedReader dc = new BufferedReader(new InputStreamReader(sc.getInputStream()));
			usoCPU.add(getSystemCpuLoad());
			/***** Fase 1:  *****/
			//Esta es la parte de conexión inicial con el cliente
			linea = dc.readLine();
			if (!linea.equals(HOLA)) {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
			} else {
				ac.println(OK);
				cadenas[0] = dlg + REC + linea + "-continuando.";
				System.out.println(cadenas[0]);
			}
			
			/***** Fase 2:  *****/
			/* Empieza lo bueno, recibe los algoritmos para llaves
			 * simétrica, asimétrica y el hash
			 */
			linea = dc.readLine();
			if (!(linea.contains(SEPARADOR) && linea.split(SEPARADOR)[0].equals(ALGORITMOS))) {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
			}

			//Acuerdo de algoritmo simétrico
			String[] algoritmos = linea.split(SEPARADOR);
			if (!algoritmos[1].equals(S.DES) && !algoritmos[1].equals(S.AES) &&
					!algoritmos[1].equals(S.BLOWFISH) && !algoritmos[1].equals(S.RC4)){
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + "Alg.Simetrico" + REC + algoritmos + "-terminando.");
			}
			//Acuerdo de algoritmo asimétrico
			if (!algoritmos[2].equals(S.RSA) ) {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + "Alg.Asimetrico." + REC + algoritmos + "-terminando.");
			}
			//Acuerdo de algoritmo de HMAC
			if (!validoAlgHMAC(algoritmos[3])) {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + "AlgHash." + REC + algoritmos + "-terminando.");
			}
			cadenas[1] = dlg + REC + linea + "-continuando.";
			System.out.println(cadenas[1]);
			ac.println(OK);
			cadenas[2] = dlg + ENVIO + OK + "-continuando.";
			System.out.println(cadenas[2]);
			
			 tiempoInicio = System.currentTimeMillis();

			/***** Fase 3: Recibe certificado del cliente *****/				
			String strCertificadoCliente = dc.readLine();
			byte[] certificadoClienteBytes = new byte[520];
			certificadoClienteBytes = toByteArray(strCertificadoCliente);
			CertificateFactory creador = CertificateFactory.getInstance("X.509");
			InputStream in = new ByteArrayInputStream(certificadoClienteBytes);
			X509Certificate certificadoCliente = (X509Certificate)creador.generateCertificate(in);
			cadenas[3] = dlg + REC + "certificado del cliente. continuando.";
			System.out.println(cadenas[3]);
			ac.println(OK);
			cadenas[4] = dlg + ENVIO + OK + "-continuando.";
			System.out.println(cadenas[4]);
			usoCPU.add(getSystemCpuLoad());
			/*
			 * De aquí en adelante las fases son claras,
			 * si algo te confunde revisa el protocolo del caso 2
			 */


			/***** Fase 4: Envia certificado del servidor *****/
			String strSerCert = toHexString(mybyte);
			ac.println(strSerCert);
			cadenas[5] = dlg + ENVIO + " certificado del servidor. continuando.";
			System.out.println(cadenas[5]);	
			linea = dc.readLine();
			if (!linea.equals(OK)) {
				sc.close();
				throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
			} else {
				cadenas[6] = dlg + REC + linea + "-continuando.";
				System.out.println(cadenas[6]);
			}

			/***** Fase 5: Envia llave simetrica *****/
			SecretKey simetrica = S.kgg(algoritmos[1]);
			String llaveS = simetrica.toString();
			
			ac.println(llaveS);
			cadenas[7] = dlg +  ENVIO + "llave simetrica al cliente. continuado.";
			System.out.println(cadenas[7]);

			/***** Fase 5: Envia reto *****/
			Random rand = new Random(); 
			int intReto = rand.nextInt(999);
			String strReto = intReto+"";
			while (strReto.length()%4!=0) strReto += "0";

			String reto = strReto;
			ac.println(reto);
			cadenas[8] = dlg + ENVIO + reto + "-reto al cliente. continuando ";
			System.out.println(cadenas[8]);

			/***** Fase 6: Recibe reto del cliente *****/
			linea = dc.readLine();
			String retoCliente = linea;
			if (retoCliente.equals(reto)) {
				cadenas[9] = dlg + REC + retoCliente + "-reto correcto. continuado.";
				System.out.println(cadenas[9]);
				ac.println("OK");
			} else {
				ac.println("ERROR");
				sc.close();
				throw new Exception(dlg + REC + retoCliente + "-ERROR en reto. terminando");
			}
			usoCPU.add(getSystemCpuLoad());
			/***** Fase 7: Recibe identificador de usuario *****/
			linea = dc.readLine();
			String nombre = linea;
			cadenas[10] = dlg + REC + nombre + "-continuando";
			System.out.println(cadenas[10]);

			/***** Fase 8: Envia hora de registro *****/
			Calendar rightNow = Calendar.getInstance();
			int hora = rightNow.get(Calendar.HOUR_OF_DAY);
			int minuto = rightNow.get(Calendar.MINUTE);
			String strvalor;
			if (hora<10)
				strvalor = "0" + ((hora) * 100 + minuto);
			else
				strvalor = ((hora) * 100 + minuto) + "";
			
			
			ac.println(strvalor);
			cadenas[11] = dlg + ENVIO + strvalor + ". continuado.";
			System.out.println(cadenas[11]);

			linea = dc.readLine();	
			if (linea.equals(OK)) {
				cadenas[12] = dlg + REC + linea + "-Terminando exitosamente.";
				System.out.println(cadenas[12]);
			} else {
				cadenas[12] = dlg + REC + linea + "-Terminando con error";
				System.out.println(cadenas[12]);
			}
			 tiempoFin = System.currentTimeMillis();
			
			tiempoTransaccion = tiempoFin - tiempoInicio;
			usoCPU.add(getSystemCpuLoad());
			sc.close();
			
			synchronized (file) {
				for (int i=0;i<numCadenas;i++) {
					escribirMensaje(cadenas[i]);
				}
				file.notify();
			}
			
		} catch (Exception e) {
			
			transaccionTerminada =1;
			if(tiempoInicio==0)
			{
				tiempoTransaccion =0;
			}else
			{
				tiempoFin = System.currentTimeMillis();
				tiempoTransaccion = tiempoFin - tiempoInicio;
			}
			try {
				usoCPU.add(getSystemCpuLoad());
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			System.out.println("Se desconecto esta mierda :(");
			e.printStackTrace();
		}
		
		
		synchronized (fileCsv) {
			datos[0]= Long.toString(getId());
			datos[1]= Long.toString(tiempoTransaccion);
			datos[2]= Double.toString(cargaCPU(usoCPU));
			datos[3]= Integer.toString(transaccionTerminada);
			escribirMensajeCSV(datos);
			fileCsv.notify();
		}
	}
	
	public double cargaCPU(ArrayList<Double>pCarga)
	{
		double mayor = -1;
		for (int i = 0; i < pCarga.size(); i++) {
			if(pCarga.get(i)>mayor)mayor=pCarga.get(i);
		}
		return mayor;
	}

	public static String toHexString(byte[] array) {
		return DatatypeConverter.printBase64Binary(array);
	}

	public static byte[] toByteArray(String s) {
		return DatatypeConverter.parseBase64Binary(s);
	}
	
	public double getSystemCpuLoad() throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
		AttributeList list = mbs.getAttributes(name, new String[]{ "SystemCpuLoad" });
		if (list.isEmpty()) return Double.NaN;
		Attribute att = (Attribute)list.get(0);
		Double value = (Double)att.getValue();
		// usually takes a couple of seconds before we get real values
		if (value == -1.0) return Double.NaN;
		// returns a percentage value with 1 decimal point precision
		return value*100.00;
		}
}
