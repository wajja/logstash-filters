package eu.wajja.web.fetcher.config;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class NullKeySerializer extends StdSerializer<Object> {
	
	public NullKeySerializer() {
		this(null);
	}

	public NullKeySerializer(Class<Object> t) {
		super(t);
	}

	@Override
	public void serialize(Object nullKey, JsonGenerator jsonGenerator, SerializerProvider unused) throws IOException, JsonProcessingException {
		jsonGenerator.writeFieldName("EMPTY");
	}
}
