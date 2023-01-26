package ogc.wasdi.processes.providers;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
public class JerseyMapperProvider implements ContextResolver<ObjectMapper> {
    private static ObjectMapper s_oApiMapper = new ObjectMapper();
    
    @Override
    public ObjectMapper getContext(Class<?> type)
    {
        return s_oApiMapper;
    }
}
