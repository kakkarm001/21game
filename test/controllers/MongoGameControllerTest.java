package controllers;

import org.junit.Test;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;
import play.test.WithServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Limited functional testing to ensure health checks of build
 */
public class MongoGameControllerTest extends WithServer {

    private AsyncHttpClient asyncHttpClient;


    // Functional test to run through the server and check the page comes ups
    @Test
    public void testMongoConnection() throws Exception {
        MongoHelper helper = new MongoHelper();
        assertNotEquals(null, helper.getDataBase());
    }



}