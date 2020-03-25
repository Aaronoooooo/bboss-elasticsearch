package org.frameworkset.elasticsearch.template;

import com.frameworkset.util.VariableHandler;

public class ESInfo {
	private TemplateMeta templatePro;
	private boolean tpl ;
	private ESTemplate estpl;
	private String templateName;
	private String template;
	private boolean multiparser;
	private ESUtil esUtil;
	private boolean cache;
	public ESInfo(String templateName, String template, boolean istpl, boolean multiparser, TemplateMeta templatePro, boolean cache) {
		this.template = template;
		this.templateName = templateName;
		this.tpl = istpl;
		this.multiparser = multiparser;
		this.templatePro = templatePro;
		this.cache = cache;
	}
	public String getDslFile(){
		return this.esUtil.templateFile;
	}
	public String getTemplate() {
		return template;
	}
	public void setTemplate(String template) {
		this.template = template;
	}
	public boolean isTpl() {
		return tpl;
	}
	public void setTpl(boolean tpl) {
		this.tpl = tpl;
	}
	public ESTemplate getEstpl() {
		return estpl;
	}
	public void setEstpl(ESTemplate estpl) {
		this.estpl = estpl;
	}
	public TemplateMeta getTemplatePro() {
		return templatePro;
	}
	public void setTemplatePro(TemplateMeta templatePro) {
		this.templatePro = templatePro;
	}
	public String getTemplateName() {
		return templateName;
	}
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	public boolean isMultiparser() {
		return multiparser;
	}
	public void setMultiparser(boolean multiparser) {
		this.multiparser = multiparser;
	}
	public ESUtil getEsUtil() {
		return esUtil;
	}
	public void setEsUtil(ESUtil esUtil) {
		this.esUtil = esUtil;
	}
	
	public boolean equals(Object obj)
	{
		if(obj == null)
			return false;
		if(obj instanceof ESInfo)
		{
			ESInfo o = (ESInfo)obj;
			return this.getTemplate().equals(o.getTemplate());
		}
		else
		{
			return false;
		}
	}
	public boolean fromConfig()
	{
		return this.esUtil != null && this.esUtil.fromConfig();
	}
	public int compareTo(ESInfo queryDSL)
	{
		return this.template.compareTo(queryDSL.getTemplate());
	}
	
	public ESInfo getESInfo(String sqlname)
	{
		return this.esUtil.getESInfo( sqlname);
	}
	
	public String getPlainQueryDSL(String sqlname)
	{
		return this.esUtil.getPlainTemplate( sqlname);
	}

	public VariableHandler.URLStruction getTemplateStruction(String template){
		return this.esUtil.getTempateStruction(this,template);
	}

	public int hashCode(){
		return this.getTemplate().hashCode();
	}


	public boolean isCache() {
		return cache;
	}
}
