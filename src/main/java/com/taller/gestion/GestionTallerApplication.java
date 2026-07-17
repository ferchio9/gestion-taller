package com.taller.gestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GestionTallerApplication {

	// Comprobacion explicita en vez de confiar en "${DB_PASSWORD}" sin valor por
	// defecto en application.properties: el binder de @ConfigurationProperties de
	// Spring Boot resuelve placeholders no encontrados de forma tolerante (deja el
	// texto literal "${DB_PASSWORD}" en vez de fallar), asi que sin esta comprobacion
	// la app no fallaria limpiamente al arrancar sin las variables, sino con un error
	// críptico de autenticacion contra Oracle mas tarde.
	private static final String[] VARIABLES_OBLIGATORIAS = {"DB_PASSWORD", "ADMIN_PASSWORD"};

	public static void main(String[] args) {
		for (String variable : VARIABLES_OBLIGATORIAS) {
			if (System.getenv(variable) == null) {
				System.err.println("Falta la variable de entorno obligatoria " + variable
						+ ": definela antes de arrancar (ver README).");
				System.exit(1);
			}
		}
		SpringApplication.run(GestionTallerApplication.class, args);
	}

}
