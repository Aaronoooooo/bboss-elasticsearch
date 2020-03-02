package org.frameworkset.elasticsearch.handler;/*
 *  Copyright 2008 biaoping.yin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.frameworkset.elasticsearch.ElasticSearchException;
import org.frameworkset.spi.remote.http.URLResponseHandler;

import java.io.IOException;

public class GetDocumentSourceResponseHandler extends BaseExceptionResponseHandler implements URLResponseHandler {
	private Class type;
	public GetDocumentSourceResponseHandler(Class type){
		this.type = type;
	}
	@Override
	public Object handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
		int status = initStatus(  response);
		if (status >= 200 && status < 300) {
			HttpEntity entity = response.getEntity();
			try {

				if(entity != null)
					return super.converJson(entity,type);
			}
			catch (Exception e){
				throw new ElasticSearchException(new StringBuilder().append("Request url:").append(url).toString(),e,status);
			}
			return null;
		} else {
			HttpEntity entity = response.getEntity();
//			if (entity != null ) {
//				throw new ElasticSearchException(EntityUtils.toString(entity),status);
//			}
//			else
//				throw new ElasticSearchException("Unexpected response status: " + status,status);
			return super.handleException(url,entity,status);
		}
	}
}
