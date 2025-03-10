package org.frameworkset.elasticsearch.handler;
/**
 * Copyright 2008 biaoping.yin
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;
import org.frameworkset.spi.remote.http.URLResponseHandler;

import java.io.IOException;

/**
 * <p>Description: </p>
 * <p></p>
 * <p>Copyright (c) 2018</p>
 * @Date 2018/7/19 15:20
 * @author biaoping.yin
 * @version 1.0
 */
public class ESStringResponseHandler extends BaseExceptionResponseHandler<String> implements URLResponseHandler<String>,ESExceptionWrapper {

	public ESStringResponseHandler() {
		// TODO Auto-generated constructor stub
	}
	private String charSet;
	public ESStringResponseHandler(String charSet) {
		// TODO Auto-generated constructor stub
		this.charSet = charSet;
	}

	@Override
	public String handleResponse(final HttpResponse response)
			throws ClientProtocolException, IOException {
		int status = initStatus(  response);

		if (org.frameworkset.spi.remote.http.ResponseUtil.isHttpStatusOK( status)) {
			HttpEntity entity = response.getEntity();

			if( entity != null) {
				if(charSet == null)
					return EntityUtils.toString(entity);
				else
					return EntityUtils.toString(entity,charSet);
			}
			else {
				return null;
			}
		} else {
			HttpEntity entity = response.getEntity();
//			if(status == 404){
//				this.elasticSearchException = new ElasticSearchException(EntityUtils.toString(entity),status);
//				return null;
//			}
//			else {
//				if (entity != null)
//					throw new ElasticSearchException(EntityUtils.toString(entity), status);
//				else
//					throw new ElasticSearchException("Unexpected response status: " + status, status);
//			}
			return (String)handleException(url,entity ,status,charSet);
		}
	}


}
