package com.example.eomix.config;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;

/**
 * The type Couch db config.
 */
@Configuration
public class CouchDBConfig {


    @Value("${couchdb.username}")
    private String username;

    @Value("${couchdb.password}")
    private String password;

    @Value("${couchdb.url}")
    private String couchdbUrl;

    @Value("${couchdb.database.main}")
    private String mainDatabase;

    @Value("${couchdb.database.attachment}")
    private String attachmentDatabase;
    
    @Value("${couchdb.database.experiment}")
    private String experimentDatabase;

    /**
     * Couch db instance couch db instance.
     *
     * @return the couch db instance
     * @throws MalformedURLException the malformed url exception
     */
    @Bean
    public CouchDbInstance couchDbInstance() throws MalformedURLException {
        HttpClient httpClient = new StdHttpClient.Builder().url(couchdbUrl).username(username).password(password).build();

        return new StdCouchDbInstance(httpClient);
    }

    /**
     * Couch db connector 1 : mainDatabase  and returns a CouchDbConnector instance.
     * This method creates a CouchDbConnector for the mainDatabase database,
     * allowing for interaction with the CouchDB instance.
     *
     * @param couchDbInstance the couch db instance
     * @return the couch db connector
     * @implNote This connector is used for storing and retrieving data related to the eomix application.
     * @implSpec The connector is created with the mainDatabase database name and is set to create the database if it does not exist.
     */
    @Bean
    public CouchDbConnector couchDbConnector1(CouchDbInstance couchDbInstance) {
        return couchDbInstance.createConnector(mainDatabase, true);
    }

    /**
     * Couch db connector 2 : experiment and returns a CouchDbConnector instance.
     * This method creates a CouchDbConnector for the experimentDatabase database,
     * allowing for interaction with the CouchDB instance.
     *
     * @param couchDbInstance the couch db instance
     * @return the couch db connector
     * @implNote This connector is used for storing and retrieving experiment-related data.
     * @implSpec The connector is created with the experimentDatabase database name and is set to create the database if it does not exist.
     */
    @Bean
    public CouchDbConnector couchDbConnector2(CouchDbInstance couchDbInstance) {
        return couchDbInstance.createConnector(experimentDatabase, true);
    }

    /**
     * Couch db connector 3 : attachment and returns a CouchDbConnector instance.
     * This method creates a CouchDbConnector for the attachmentDatabase database,
     * allowing for interaction with the CouchDB instance.
     *
     * @param couchDbInstance the couch db instance
     * @return the couch db connector
     * @implNote This connector is used for storing and retrieving attachment-related data.
     * @implSpec The connector is created with the attachmentDatabase database name and is set to create the database if it does not exist.
     */
    @Bean
    public CouchDbConnector couchDbConnector3(CouchDbInstance couchDbInstance) {
        return couchDbInstance.createConnector(attachmentDatabase, true);
    }
}
