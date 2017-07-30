package com.itranga.cav.aws.lambda.paypal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.itranga.aws.lambda.ServerlessInput;
import com.itranga.aws.lambda.ServerlessOutput;

public class IPNListener implements RequestHandler<ServerlessInput, ServerlessOutput> {
	
	@Override
	public ServerlessOutput handleRequest(ServerlessInput serverlessInput, Context context){
		// For a full list of configuration parameters refer in wiki page. 
		// (https://github.com/paypal/sdk-core-java/blob/master/README.md)
		Map<String,String> configurationMap =  Configuration.getConfig();
		IPNMessage ipnlistener = new IPNMessage(request,configurationMap);
		boolean isIpnVerified = ipnlistener.validate();
		String transactionType = ipnlistener.getTransactionType();
		Map<String,String> map = ipnlistener.getIpnMap();
		LoggingManager.info(IPNListenerServlet.class, "******* IPN (name:value) pair : "+ map + "  " +
						"######### TransactionType : "+transactionType+"  ======== IPN verified : "+ isIpnVerified);
		
	}
}
