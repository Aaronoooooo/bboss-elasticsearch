package org.frameworkset.spi.remote.http;

import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.frameworkset.elasticsearch.client.ConfigHolder;
import org.frameworkset.elasticsearch.template.BaseTemplateContainerImpl;
import org.frameworkset.elasticsearch.template.ConfigDSLUtil;
import org.frameworkset.elasticsearch.template.ESTemplateHelper;
import org.frameworkset.spi.remote.http.proxy.HttpProxyRequestException;
import org.frameworkset.spi.remote.http.proxy.InvokeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author yinbp
 * @Date:2016-11-20 11:39:59
 */
public class ConfigHttpRequestProxy {

    private static Logger logger = LoggerFactory.getLogger(ConfigHttpRequestProxy.class);
    protected String configFile;
    protected ConfigDSLUtil configDSLUtil;
    private ClientConfiguration clientConfiguration;
    private ConfigHolder configHolder = null;
    public ConfigHttpRequestProxy(String poolName,ConfigHolder configHolder, String configFile){
        this.configHolder = configHolder;
        this.configFile = configFile;
        configDSLUtil = configHolder.getConfigDSLUtil(configFile);
        clientConfiguration = ClientConfiguration.getClientConfiguration(poolName);
        
    }
    public ConfigHttpRequestProxy(String poolName,ConfigHolder configHolder, BaseTemplateContainerImpl templateContainer){
        templateContainer.setConfigHolder(configHolder);
        configDSLUtil = configHolder.getConfigDSLUtil(templateContainer);
        clientConfiguration = ClientConfiguration.getClientConfiguration(poolName);
    }

    public ConfigHttpRequestProxy(ConfigHolder configHolder, String configFile){
        this.configHolder = configHolder;
        this.configFile = configFile;
        configDSLUtil = configHolder.getConfigDSLUtil(configFile);
        clientConfiguration = ClientConfiguration.getDefaultClientConfiguration();
    }
    public ConfigHttpRequestProxy(ConfigHolder configHolder, BaseTemplateContainerImpl templateContainer){
        templateContainer.setConfigHolder(configHolder);
        configDSLUtil = configHolder.getConfigDSLUtil(templateContainer);
        clientConfiguration = ClientConfiguration.getDefaultClientConfiguration();
    }
    protected String evalTemplate(String templateName, Object params){
        return ESTemplateHelper.evalTemplate(configDSLUtil,templateName, params);
    }

 
    public String sendJsonBody(String poolname,String url, String queryDslName,Object params) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(poolname, requestBody, url);
    }

    public <T> T sendJsonBody(String poolname,String url, String queryDslName,Object params,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(poolname, requestBody, url, resultType);
    }

    public <T> T sendJsonBody(ClientConfiguration clientConfiguration,String url, String queryDslName,Object params,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(clientConfiguration, requestBody, url, resultType);
    }
 

    public <T> T sendJsonBody(String poolname, String url, String queryDslName,Map<String,Object> params,Map<String,String> headers,Class<T> resultType) throws HttpProxyRequestException {
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(poolname, url, requestBody, params, headers, resultType);
    }
    public <T> List<T> sendJsonBodyForList(String poolname,String url, String queryDslName,Object params,Map headers,Class<T> resultType) throws HttpProxyRequestException {
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBodyForList(poolname, requestBody, url, headers, resultType);
    }
        
    public <T> List<T> sendJsonBodyForList(String poolname,String url, String queryDslName,Object params,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBodyForList(poolname, requestBody, url, resultType);
    }
    public <T> List<T> sendJsonBodyForList(String url, String queryDslName,Object params,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBodyForList(clientConfiguration.getDatasource(),requestBody, url, resultType);
    }


    public <D,T> D sendJsonBodyTypeObject(String poolname, String url, String queryDslName,Object params,Map headers,Class<D> containerType,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBodyTypeObject(poolname, url, requestBody, headers, containerType, resultType);
    }
    public <D,T> D sendJsonBodyTypeObject(String poolname, String url, String queryDslName,Object params,Class<D> containerType,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBodyTypeObject(poolname, url, requestBody, containerType, resultType);
    }
    public <D,T> D sendJsonBodyTypeObject( String url, String queryDslName,Object params,Class<D> containerType,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBodyTypeObject(clientConfiguration.getDatasource(),url, requestBody, containerType, resultType);
    }


    public <T> Set<T> sendJsonBodyForSet(String poolname,String url, String queryDslName,Object params,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBodyForSet(poolname, requestBody, url, resultType);
    }

    public <T> Set<T> sendJsonBodyForSet(String url, String queryDslName,Object params,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBodyForSet(clientConfiguration.getDatasource(),requestBody, url, resultType);
    }


    public <K,T> Map<K,T> sendJsonBodyForMap(String poolname,String url, String queryDslName,Object params,Class<K> keyType,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBodyForMap(poolname, requestBody, url, keyType, resultType);
    }
    public <K,T> Map<K,T> sendJsonBodyForMap(String url, String queryDslName,Object params,Class<K> keyType,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBodyForMap(clientConfiguration.getDatasource(),requestBody, url, keyType, resultType);
    }
    
    public <T> T sendJsonBody(String poolname, String url, String queryDslName,Object params, Map headers,Class<T> type) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(poolname, requestBody, url, headers, type);
    }
    public String sendJsonBody(String poolname, String url, String queryDslName,Object params, Map headers) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(poolname, requestBody, url, headers);
    }
 

     

    public String sendJsonBody(String url, String queryDslName,Object params) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(clientConfiguration.getDatasource(),requestBody, url);
    }

    public <T> T sendJsonBody(String url, String queryDslName,Object params, Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(clientConfiguration.getDatasource(),requestBody, url, resultType);
    }

    public <T> T sendJsonBody(String url, String queryDslName,Object params, Map headers,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(clientConfiguration.getDatasource(),requestBody, url, headers, resultType);
    }

    public <T> T sendJsonBody(String url, String queryDslName,Object params, InvokeContext invokeContext,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(clientConfiguration,requestBody, url, invokeContext, resultType);
    }



    public <D,T> D sendJsonBody(String url, String queryDslName,Object params,Class<D> containType,Class<T> resultType) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(clientConfiguration,requestBody, url, containType, resultType);
    }

 
    public String sendJsonBody(String url, String queryDslName,Object params, Map headers ) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(clientConfiguration.getDatasource(),requestBody, url, headers);
    }
    public String sendJsonBody(String url, String queryDslName,Object params, InvokeContext invokeContext ) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(clientConfiguration,requestBody, url, invokeContext);
    }
    public <T> T sendJsonBody(String url, String queryDslName,Object params, Map headers  ,HttpClientResponseHandler<T> responseHandler) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(clientConfiguration,requestBody, url, headers, responseHandler);
    }

    public <T> T sendJsonBody(String poolname,String url, String queryDslName,Object params, Map headers  ,HttpClientResponseHandler<T> responseHandler) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(poolname, requestBody, url, headers, responseHandler);
    }
    public <T> T sendJsonBody(String poolname,String url, String queryDslName,Object params,
                              HttpClientResponseHandler<T> responseHandler) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        ClientConfiguration clientConfiguration = ClientConfiguration.getClientConfiguration(poolname);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(poolname, requestBody, url, responseHandler);
    }
    public <T> T sendJsonBody(ClientConfiguration clientConfiguration,String url, String queryDslName,Object params,
                              HttpClientResponseHandler<T> responseHandler) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(clientConfiguration, requestBody, url, responseHandler);
    }
    public <T> T sendJsonBody(ClientConfiguration clientConfiguration,String url, String queryDslName,Object params, Map headers  ,HttpClientResponseHandler<T> responseHandler) throws HttpProxyRequestException {
        String requestBody = this.evalTemplate(queryDslName,params);
        if(logger.isInfoEnabled() && clientConfiguration.isShowDsl()){
            logger.info(requestBody);
        }
        return HttpRequestProxy.sendJsonBody(clientConfiguration, requestBody, url, headers, responseHandler);
    }

 


 
 
}