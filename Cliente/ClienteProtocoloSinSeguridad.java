package Cliente;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class ClienteProtocoloSinSeguridad {
	public static void procesar(BufferedReader stdIn, BufferedReader pIn, PrintWriter pOut) throws IOException {
		//Se manda HOLA para comenzar el protocolo
		System.out.println("Escriba 'HOLA'");
		String fromUser = "HOLA";

		pOut.println(fromUser);

		String fromServer = "";

		if((fromServer = pIn.readLine())!= null){
			System.out.println("Respuesta del servidor: " + fromServer);
		}

		// Se elije el algoritmo para el cifrado simétrico
//		System.out.println("Elija su algoritmo para cifrado simétrico : 1. AES , 2. Blowfish");
//
		String algSimetrico ="AES";
//
//		switch (fromUser = stdIn.readLine()) {
//		case "1":
//			algSimetrico = "AES";
//			break;
//		case "2":
//			algSimetrico = "Blowfish";
//			break;
//		default:
//			break;
//		}
//		
//		// Se elije el algoritmo para el cifrado asimétrico
//		System.out.println("Elija su algoritmo para cifrado asimétrico : 1. RSA");
//
		String algAsimetrico ="RSA";
//
//		switch (fromUser = stdIn.readLine()) {
//		case "1":
//			algAsimetrico = "RSA";
//			break;
//		default:
//			break;
//		}
//		
//		// Se elije el algoritmo para el certificado HMAC
//		System.out.println("Elija su algoritmo para certificado HMAC : 1. HMACSHA1, 2. HMACSHA256, 3. HMACSHA384, 4. HMACSHA512");
//
		String algHMAC ="HMACSHA1";
//
//		switch (fromUser = stdIn.readLine()) {
//		case "1":
//			algHMAC = "HMACSHA1";
//			break;
//		case "2":
//			algHMAC = "HMACSHA256";
//			break;
//		case "3":	
//			algHMAC = "HMACSHA384";
//			break;
//		case "4":
//			algHMAC = "HMACSHA512";
//			break;
//		default:
//			break;
//		}

		//Se manda el string con los algoritmos correspondientes

		fromUser = "ALGORITMOS:AES:RSA:HMACSHA1";
		System.out.println("Se envían los algoritmos de cifrado al servidor");
		pOut.println(fromUser);
		
		System.out.println("Algoritmos enviados al servidor: "+fromUser);

		if((fromServer = pIn.readLine())!= null){
			System.out.println("Respuesta del servidor: " + fromServer);
		}
		System.out.println("Los algoritmos fueron aceptados por el servidor");

		//Se generan el par de llaves asimétricas del cliente

		KeyPairGenerator generator;
		try {
			generator = KeyPairGenerator.getInstance(algAsimetrico);
			generator.initialize(1024);
			KeyPair keyPair = generator.generateKeyPair();
			PrivateKey llavePrivada= keyPair.getPrivate();

			//Se genera y se envía el certificado correspondiente del cliente al servidor

			java.security.cert.X509Certificate certificado = generarCertificado(keyPair,algAsimetrico,algHMAC);
			byte[] certificadoEnBytes = certificado.getEncoded( );

			String certificadoEnString = DatatypeConverter.printBase64Binary(certificadoEnBytes);
			System.out.println("Se envía el certificado ");
			pOut.println(certificadoEnString);
			//Ok del servidor
			System.out.println("El servidor ha aceptado el certificado del cliente");
			if((fromServer = pIn.readLine())!= null){
				System.out.println("Respuesta del servidor: " + fromServer);
			}

			//Certificado del servidor
			System.out.println("Se recibe el certificado del servidor");
			if((fromServer = pIn.readLine())!= null){
				System.out.println("Respuesta del servidor: " + fromServer);
			}

			while(fromServer.length()%4!=0){
				fromServer+="0";
			}
			byte[] certificadoServidor = DatatypeConverter.parseBase64Binary(fromServer);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			InputStream is = new ByteArrayInputStream(certificadoServidor);
			X509Certificate certSer = (X509Certificate) cf.generateCertificate(is);

			if(validacionCertificado(certSer)){
				pOut.println("OK");
			}
			else pOut.println("ERROR");		
//----------------------------------------------------
			System.out.println("Se recibe la llave");
			if((fromServer = pIn.readLine())!= null){
				System.out.println("Respuesta del servidor: " + fromServer);
			}
			while(fromServer.length()%4!=0){
				fromServer+="0";
			}
			
			System.out.println("Llega el reto del servidor ");
			if((fromServer = pIn.readLine())!= null){
				System.out.println("Respuesta del servidor: " + fromServer);
			}
			
			String retoString = fromServer;

			System.out.println("El reto es: " + retoString);
			
			System.out.println("Se manda el reto");
			pOut.println(retoString);

			if((fromServer = pIn.readLine())!= null){
				System.out.println("Respuesta del servidor: " + fromServer);
			}

			String idUsuario = "1811";

			System.out.println("Se manda el id del usuario");
			pOut.println(idUsuario);

			if((fromServer = pIn.readLine())!= null){
				System.out.println("Respuesta del servidor: " + fromServer);
			}

			while(fromServer.length()%4!=0){
				fromServer+="0";
			}

			String hhmm = fromServer;

			System.out.println("Hora militar recibida: " + hhmm);

			long fecha = System.currentTimeMillis();
			Date fecha2 = new Date(fecha);
			int hora = fecha2.getHours();
			int mins = fecha2.getMinutes();
			String horaMil = ""+hora+mins;
			if(hhmm.equals(hhmm)){
				pOut.println("OK");
				System.out.println("Respuesta mandada al servidor: OK" );
			}
			else{
				pOut.println("ERROR");
				System.out.println("Respuesta mandada al servidor: ERROR" );
			}



		} catch (NoSuchAlgorithmException | OperatorCreationException | CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}




	}

	public static X509Certificate generarCertificado(KeyPair keyPair,String pAlgoritmoAsimetrico, String pAlgoritmoHMAC) throws OperatorCreationException, CertificateException{
		Calendar endCalendar = Calendar.getInstance();
		endCalendar.add(Calendar.YEAR, 10);
		X509v3CertificateBuilder X509v3CertificateBuilder = new X509v3CertificateBuilder(
				new X500Name("CN=localhost"),
				BigInteger.valueOf(1),
				Calendar.getInstance().getTime(),
				endCalendar.getTime(),
				new X500Name("CN=localhost"),
				SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));
		
		String algoritmoHMAC = pAlgoritmoHMAC.substring(4);
		System.out.println("Algoritmo HMAC del certificado "+ algoritmoHMAC);
		ContentSigner contentSigner = new JcaContentSignerBuilder(algoritmoHMAC+"with"+pAlgoritmoAsimetrico).build(keyPair.getPrivate());
		X509CertificateHolder x509CertificateHolder = X509v3CertificateBuilder.build(contentSigner);
		return new JcaX509CertificateConverter().setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()).getCertificate(x509CertificateHolder);
	}

	public static boolean validacionCertificado(X509Certificate certificado){

		try {
			PublicKey llavePublica = certificado.getPublicKey();
			certificado.verify(llavePublica);
			return true;
		} catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException | NoSuchProviderException
				| SignatureException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static byte [] descifrarAsimetrico(Key llave, String algoritmo, byte[] texto) {
		byte[] textoClaro;

		try {
			Cipher cifrador = Cipher.getInstance(algoritmo);
			cifrador.init(Cipher.DECRYPT_MODE, llave); 
			textoClaro = cifrador.doFinal(texto);
		} catch (Exception e) {
			System.out.println("Excepcion: " + e.getMessage());
			return null;
		}

		return textoClaro;
	}

	public static byte[] cifrarAsimetrico(Key llave, String algoritmo, String texto){
		byte[] textoCifrado;

		try {
			Cipher cifrador = Cipher.getInstance(algoritmo);
			byte[] textoClaro = DatatypeConverter.parseBase64Binary(texto);

			cifrador.init(Cipher.ENCRYPT_MODE, llave);
			textoCifrado = cifrador.doFinal(textoClaro);

			return textoCifrado;
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
			return null;
		}

	}

	public static byte[] cifrarSimetrico(SecretKey llave, String texto, String pAlgoritmo) {
		byte[] textoCifrado;

		try {
			Cipher cifrador = Cipher.getInstance(pAlgoritmo+"/ECB/PKCS5Padding");
			byte[] textoClaro = texto.getBytes();

			cifrador.init(Cipher.ENCRYPT_MODE, llave);
			textoCifrado = cifrador.doFinal(textoClaro);

			return textoCifrado;
		} catch (Exception e) {
			System.out.println("Excepcion: " + e.getMessage());
			return null;
		}
	}

	public static byte[] descifrarSimetrico(SecretKey llave, byte[] texto, String pAlgoritmo) {
		byte[] textoClaro;

		try {
			Cipher cifrador = Cipher.getInstance(pAlgoritmo+"/ECB/PKCS5Padding");
			cifrador.init(Cipher.DECRYPT_MODE, llave);
			textoClaro = cifrador.doFinal(texto);
		} catch (Exception e) {
			System.out.println("Excepcion: "+ e.getMessage());
			return null;	
		}
		return textoClaro;
	}
}
