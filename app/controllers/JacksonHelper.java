package controllers;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by manish on 10/4/2017.
 */
public class JacksonHelper {
    ObjectMapper mapper;

    public JacksonHelper() {
        mapper = new ObjectMapper();
    }


    public String pojoToJson(Object object){
        try {
            //Convert object to JSON string
            return mapper.writeValueAsString(object);

        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "PARSINGFAILED";
    }

}
