package org.frameworkset.elasticsearch.handler;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.frameworkset.elasticsearch.ElasticSearchException;
import org.frameworkset.elasticsearch.entity.MapSearchHit;
import org.frameworkset.spi.remote.http.URLResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GetDocumentHitResponseHandler extends BaseExceptionResponseHandler<MapSearchHit> implements URLResponseHandler<MapSearchHit> {
	private static Logger logger = LoggerFactory.getLogger(GetDocumentHitResponseHandler.class);



	 @Override
     public MapSearchHit handleResponse(final HttpResponse response)
             throws ClientProtocolException, IOException {
         int status = initStatus(  response);

         if (org.frameworkset.spi.remote.http.ResponseUtil.isHttpStatusOK( status)) {
             HttpEntity entity = response.getEntity();
             try {

                 if(entity != null )
                     return super.converJson(entity, MapSearchHit.class);
//                 String content = EntityUtils.toString(entity);
//                 System.out.println(content);
//                 searchResponse = entity != null ? SimpleStringUtil.json2Object(content, RestResponse.class) : null;
             }
             catch (Exception e){
//                 logger.error("",e);
                 throw new ElasticSearchException(new StringBuilder().append("Request url:").append(url).toString(),e,status);
             }

//             ClassUtil.ClassInfo classInfo = ClassUtil.getClassInfo(TransportClient.class);
//             NamedWriteableRegistry namedWriteableRegistry = (NamedWriteableRegistry)classInfo.getPropertyValue(clientUtil.getClient(),"namedWriteableRegistry");

             return null;

         } else {
             HttpEntity entity = response.getEntity();
//             if (entity != null ) {
//				throw new ElasticSearchException(EntityUtils.toString(entity),status);
//             }
//             else
//                 throw new ElasticSearchException("Unexpected response status: " + status,status);
             return (MapSearchHit)super.handleException(url,entity,status);
         }
     }

}
