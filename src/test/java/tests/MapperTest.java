package tests;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itranga.cav.aws.lambda.paypal.model.InvoiceDAO;

public class MapperTest {
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MapperTest.class);
	
	private static final ObjectMapper mapper = new ObjectMapper();
	
	@Test
	public void mapInvoice() throws JsonParseException, JsonMappingException, IOException{
		String json = "{\"invoiceId\":\"MARK.ME\"}";
		InvoiceDAO invoiceDao = mapper.readValue(json, InvoiceDAO.class);
		assertThat(invoiceDao.getInvoiceId(), equalTo("MARK.ME"));
	}

}
