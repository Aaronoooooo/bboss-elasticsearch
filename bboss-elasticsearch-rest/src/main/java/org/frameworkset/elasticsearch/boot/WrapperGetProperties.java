package org.frameworkset.elasticsearch.boot;
/**
 * Copyright 2020 bboss
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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.elasticsearch.ElasticSearchHelper;
import org.frameworkset.spi.assemble.GetProperties;
import org.frameworkset.spi.remote.http.ClientConfiguration;

import java.util.Map;

/**
 * <p>Description: </p>
 * <p></p>
 * <p>Copyright (c) 2020</p>
 * @Date 2020/5/14 0:41
 * @author biaoping.yin
 * @version 1.0
 */
public class WrapperGetProperties implements GetProperties {
	private GetProperties context;
	public WrapperGetProperties(GetProperties context){
		this.context = context;
	}

	@Override
	public void reset() {
		context.reset();
	}

	@Override
	public String getExternalProperty(String property) {
		return context.getExternalProperty(property);
	}
    /**
     * 根据属性名称前缀获取属性集
     * @param namespace
     * @param propertyPrex 属性名称前缀
     * @param truncated 返回的key是否截取掉前缀
     * @return
     */
    @Override
    public Map<String,Object> getExternalProperties(String namespace,String propertyPrex,boolean truncated){
        return context.getExternalProperties(namespace,propertyPrex,truncated);
    }
	@Override
	public String getSystemEnvProperty(String property) {
		return context.getSystemEnvProperty(property);
	}

	@Override
	public String getExternalProperty(String property, String defaultValue) {
		return context.getExternalProperty(property,defaultValue);
	}

	@Override
	public Object getExternalObjectProperty(String property) {
		return context.getExternalObjectProperty(property);
	}

	@Override
	public Object getExternalObjectProperty(String property, Object defaultValue) {
		return context.getExternalObjectProperty(property,defaultValue);
	}

	@Override
	public boolean getExternalBooleanProperty(String property, boolean defaultValue) {
		return context.getExternalBooleanProperty(property,defaultValue);
	}

	/**
	 * 如果没有配置http连接池账号和口令，则将es账号和口令转为http协议账号和口令
	 * @param namespace
	 * @param property
	 * @return
	 */
	@Override
	public String getExternalPropertyWithNS(String namespace, String property) {
		String value = null;
		if(property.endsWith(ClientConfiguration.http_authAccount)) {
			value = context.getExternalProperty(property);
            if(SimpleStringUtil.isEmpty(value)){
                if(namespace.equals("default")){
                    value = context.getExternalProperty(namespace + "."+property);
                }

            }
			if(SimpleStringUtil.isEmpty(value)){

				value =	ElasticSearchHelper._getStringValue(namespace,"elasticUser",context,null);

			}
		}
		else if(property.endsWith(ClientConfiguration.http_authPassword)) {
			value = context.getExternalProperty(property);
            if(SimpleStringUtil.isEmpty(value)){
                if(namespace.equals("default")){
                    value = context.getExternalProperty(namespace + "."+property);
                }

            }
			if(SimpleStringUtil.isEmpty(value)){

				value =	ElasticSearchHelper._getStringValue(namespace,"elasticPassword",context,null);

			}
		}
        else if(property.endsWith(ClientConfiguration.http_hosts)) {
            value = context.getExternalProperty(property);
            if(SimpleStringUtil.isEmpty(value)){
                if(namespace.equals("default")){
                    value = context.getExternalProperty(namespace + "."+property);
                }

            }
            if(SimpleStringUtil.isEmpty(value)){

                value =	ElasticSearchHelper._getStringValue(namespace,"elasticsearch.rest.hostNames",context,null);

            }
        }
        else if(property.endsWith(ClientConfiguration.http_health)) {
            value = context.getExternalProperty(property);
            if(SimpleStringUtil.isEmpty(value)){
                if(namespace.equals("default")){
                    value = context.getExternalProperty(namespace + "."+property);
                }

            }
            if(SimpleStringUtil.isEmpty(value)){

                value =	"/";

            }
        }
        
        else if(property.endsWith(ClientConfiguration.http_fail_all_continue)) {
            value = context.getExternalProperty(property);
            if(SimpleStringUtil.isEmpty(value)){
                if(namespace.equals("default")){
                    value = context.getExternalProperty(namespace + "."+property);
                }

            }
            if(SimpleStringUtil.isEmpty(value)){
                value = ElasticSearchHelper._getStringValue(namespace,"elasticsearch.failAllContinue",context,null);
            }
        }
        else if(property.endsWith(ClientConfiguration.http_discover_service)) {
            value = context.getExternalProperty(property);
            if(SimpleStringUtil.isEmpty(value)){
                if(namespace.equals("default")){
                    value = context.getExternalProperty(namespace + "."+property);
                }

            }
            if(SimpleStringUtil.isEmpty(value)){
                String elasticsearchDiscoverHost = ElasticSearchHelper._getStringValue(namespace,"elasticsearch.discoverHost",context,null);
                if(elasticsearchDiscoverHost != null && elasticsearchDiscoverHost.equals("true")) {
                    value = "org.frameworkset.elasticsearch.client.ElasticsearchHostNodeDiscover";
                }
            }
        }
        else if(property.endsWith(ClientConfiguration.http_exception_ware)) {
            value = context.getExternalProperty(property);
            if(SimpleStringUtil.isEmpty(value)){
                if(namespace.equals("default")){
                    value = context.getExternalProperty(namespace + "."+property);
                }

            }
            if(SimpleStringUtil.isEmpty(value)){
                 value = "org.frameworkset.elasticsearch.client.ElasticsearchExceptionWare";
            }
        }
		else{
			value = context.getExternalProperty(property);
		}
		return value;
	}
    @Override
    public Object getExternalObjectPropertyWithNS(String namespace, String property) {
        Object value = null;
        if(property.endsWith(ClientConfiguration.http_discover_service)) {
            value = context.getExternalObjectProperty(property);
            if(SimpleStringUtil.isEmpty(value)){
                if(namespace.equals("default")){
                    value = context.getExternalObjectProperty(namespace + "."+property);
                }

            }
            if(SimpleStringUtil.isEmpty(value)){                 
                String elasticsearchDiscoverHost = ElasticSearchHelper._getStringValue(namespace,"elasticsearch.discoverHost",context,null);
                if(elasticsearchDiscoverHost != null && elasticsearchDiscoverHost.equals("true")) {
                    value = "org.frameworkset.elasticsearch.client.ElasticsearchHostNodeDiscover";
                }
            }
        }
        else if(property.endsWith(ClientConfiguration.http_exception_ware)) {
            value = context.getExternalObjectProperty(property);
            if(SimpleStringUtil.isEmpty(value)){
                if(namespace.equals("default")){
                    value = context.getExternalObjectProperty(namespace + "."+property);
                }

            }
            if(SimpleStringUtil.isEmpty(value)){
                value = "org.frameworkset.elasticsearch.client.ElasticsearchExceptionWare";
            }
        }
        else{
            value = context.getExternalObjectProperty(property);
        }
        return value;
    }

	/**
	 * 如果没有配置http连接池账号和口令，则将es账号和口令转为http协议账号和口令
	 * @param namespace
	 * @param property
	 * @param defaultValue
	 * @return
	 */
	@Override
	public String getExternalPropertyWithNS(String namespace, String property, String defaultValue) {
		String value = getExternalPropertyWithNS(  namespace,   property);
//		if(property.endsWith(ClientConfiguration.http_authAccount)) {
//			value = context.getExternalProperty(property);
//			if(SimpleStringUtil.isEmpty(value)){
//
//				value =	ElasticSearchHelper._getStringValue(namespace,"elasticUser",context,null);
//
//			}
//		}
//		else if(property.endsWith(ClientConfiguration.http_authPassword)) {
//			value = context.getExternalProperty(property);
//			if(SimpleStringUtil.isEmpty(value)){
//
//				value =	ElasticSearchHelper._getStringValue(namespace,"elasticPassword",context,null);
//
//			}
//		}
//		else{
//			value = context.getExternalProperty(property);
//		}
		if(value != null)
			return value;
		else
			return defaultValue;
	}


	@Override
	public Object getExternalObjectPropertyWithNS(String namespace, String property, Object defaultValue) {
		return context.getExternalObjectProperty(property,defaultValue);
	}

	@Override
	public Map getAllExternalProperties() {
		return context.getAllExternalProperties();
	}
}
