package it.fadeout.providers;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
public class JerseyMapperProvider implements ContextResolver<ObjectMapper> {
    private static ObjectMapper s_oApiMapper = new ObjectMapper();
    
    public JerseyMapperProvider() {
        // allow only non-null fields to be serialized
    	s_oApiMapper.setSerializationInclusion(Include.NON_NULL);
    	s_oApiMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    	//s_oApiMapper.setDateFormat()
    }    
    
    @Override
    public ObjectMapper getContext(Class<?> type)
    {
        return s_oApiMapper;
    }
}
