package controllers;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dealer.Dealer;
import mongoModels.Player;
import mongoModels.SessionState;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.DefaultCreator;
import org.mongodb.morphia.query.Query;
import scala.Int;

import java.util.List;

/**
 * Created by manish on 10/4/2017.
 */
public class MongoHelper {

    /* All the constants to connect with the database */
    public static final String DB_USERNAME = "manish";
    public static final String DB_PASSWORD = "manish";
    public static final String DB_NAME = "21game";
    public static final String DB_URL = "ds159024.mlab.com";
    public static final int DB_PORT = 59024;

    private MongoDatabase connectionToDb=null;
    Datastore datastore=null;
    Morphia morphia = new Morphia();

    public MongoHelper(){
        connectionToDb=getMongoDb();

        //custom classloader for morphia for compatibiliy with play
        morphia.getMapper().getOptions().setObjectFactory(new DefaultCreator() {
            @Override
            protected ClassLoader getClassLoaderForClass() {
                return getClass().getClassLoader();
            }
        });
    }

    private MongoDatabase getMongoDb(){
        MongoClientOptions settings = MongoClientOptions.builder().readPreference(ReadPreference.nearest())
                .codecRegistry(com.mongodb.MongoClient.getDefaultCodecRegistry()).build();

        MongoClientURI url = new MongoClientURI("mongodb://manish:manish@ds163034.mlab.com:63034/21game");
        MongoClient mongo = new MongoClient(url);
        MongoDatabase db = mongo.getDatabase(DB_NAME);

        //morphia configuration
        // tell Morphia where to find your classes
        // can be called multiple times with different packages or classes
        morphia.mapPackage("mongoModels");
        morphia.mapPackage("dealer");
        // create the Datastore connecting to the default port on the local host
        this.datastore = morphia.createDatastore(mongo, DB_NAME);
        this.datastore.ensureIndexes();

        return db;
    }

    public MongoDatabase getDataBase(){
        return this.connectionToDb;
    }

    public SessionState getSessionStateByIndex(int index){
        Query<SessionState> query = this.datastore.createQuery(SessionState.class);
        List<SessionState> sessionStates = query.asList();
        return sessionStates.get(index);
    }

    public Dealer getDealerByIndex(int index){
        Query<Dealer> query = this.datastore.createQuery(Dealer.class);
        List<Dealer> dealers = query.asList();
        return dealers.get(index);
    }


}
