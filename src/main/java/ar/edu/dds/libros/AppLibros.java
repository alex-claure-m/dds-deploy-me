package ar.edu.dds.libros;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.net.URI;
import java.net.URISyntaxException;


public class AppLibros {

	public static EntityManagerFactory entityManagerFactory;

	public static void main(String[] args) throws Exception {
		
		Map<String, String> env = System.getenv();
		for (String string : env.keySet()) {
			System.out.println(string + ": " + env.get(string));
		}
		
		entityManagerFactory =  createEntityManagerFactory();
		String strport = System.getenv("PORT");
		if (strport == null){
			strport = "8080";
		}
		Integer port = Integer.parseInt(strport);

		Javalin app = Javalin.create().start(port);
		
		LibrosController controller = new LibrosController(entityManagerFactory); 

		//uno para listar y otro para agregar libro
		app.get("/libros", controller::listLibros);
		app.post("/libros", controller::addLibro);
		
	}
	
	// este cacho de codigo es el que crea el entitymanager
	// que basicamente es como nosotros tenemos en entity manager
	// pero claro, nosotros teniamos un xml de persistence a nivel local
	// debemos tener una clase o metodo que haga que creemos un entity manager para cada microservicio
	//ya sea para la base de datos, para el deploy , otro para el local etc.
	// entonces lo que hace esta funcion
	// es agarrar del array de todas las persistencias.xml y se si existe una variable de entorno que se llame
	//javax__persistence__jdbc__driver
	//otra para: javax__persistence__jdbc__password
	// etc
	// si existe, lo que hacemos es llenar esta configuracion que es un hashmap (configOverride)
	/*
	* lo que hace este configOverride
	* es que retornara un : return Persistence.createEntityManagerFactory("db", configOverrides);
	* donde db seria en mi caso el "Simple_etc" de mi db
	*
	* de donde sale las variables de entorno?
	* 	-> se la debemos pasar. pero Como? configurandolo
	* 	-> en intellij y eclipse es en Run Configuration (pero antes, hacer run por mas que se rompa)
	* 	-> MIRAR WORD COMO ESTA CONFIGURADO:
	* */
	public static EntityManagerFactory createEntityManagerFactory() throws Exception {
		// https://stackoverflow.com/questions/8836834/read-environment-variables-in-persistence-xml-file
		Map<String, String> env = System.getenv();
		Map<String, Object> configOverrides = new HashMap<String, Object>();

		String[] keys = new String[] { 
			"DATABASE_URL",
			"javax.persistence.jdbc.driver",
			"javax.persistence.jdbc.password",
			"javax.persistence.jdbc.url",
			"javax.persistence.jdbc.user",
			"hibernate.hbm2ddl.auto",
			"hibernate.connection.pool_size",
			"hibernate.show_sql" };

		for (String key : keys) {
               
		    try{
			if (key.equals("DATABASE_URL")) {
 

				//esto mepa que es para que guarde en  local!

				// https://devcenter.heroku.com/articles/connecting-heroku-postgres#connecting-in-java
				String value = env.get(key);
				URI dbUri = new URI(value);
				String username = dbUri.getUserInfo().split(":")[0];
				String password = dbUri.getUserInfo().split(":")[1];
				//javax.persistence.jdbc.url=jdbc:postgresql://localhost/dblibros
				value = "jdbc:mysql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();// + "?sslmode=require";
				configOverrides.put("javax.persistence.jdbc.url", value);
				configOverrides.put("javax.persistence.jdbc.user", username);
				configOverrides.put("javax.persistence.jdbc.password", password);
				configOverrides.put("javax.persistence.jdbc.driver", "com.mysql.jdbc.Driver");
				
				//  configOverrides.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
			}
			// no se pueden poner variables de entorno con "." en la key
			String key2 = key.replace("__",".");
			if (env.containsKey(key)) {
				String value = env.get(key);
				configOverrides.put(key2, value);
			}
		    } catch(Exception ex){
			System.out.println("Error configurando " + key);    
		    }
		}
		System.out.println("Config overrides ----------------------");
		for (String key : configOverrides.keySet()) {
			System.out.println(key + ": " + configOverrides.get(key));
		}
		return Persistence.createEntityManagerFactory("db", configOverrides);
	}

}
