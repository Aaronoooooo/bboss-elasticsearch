package org.frameworkset.elasticsearch.serial;

import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import org.frameworkset.elasticsearch.entity.ESBaseData;
import org.frameworkset.elasticsearch.entity.ESId;
import org.frameworkset.util.ClassUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class EntityCustomSerializationFactory extends BeanSerializerFactory {
	private static final long serialVersionUID = 1L;



	public EntityCustomSerializationFactory() {
		this(null);
	}
	protected EntityCustomSerializationFactory(SerializerFactoryConfig config) {
		super(config);
	}
	private String[] _getFilterFields(ClassUtil.ClassInfo classInfo){
		if(ESBaseData.class.isAssignableFrom(classInfo.getClazz()) || ESId.class.isAssignableFrom(classInfo.getClazz()))
			return getFilterFields(classInfo);
		return null;
	}
	protected abstract  String[] getFilterFields(ClassUtil.ClassInfo classInfo);
	private boolean isFilterField(ClassUtil.ClassInfo classInfo,String propName,String[] fs){
		boolean find = false;
		if(fs != null && fs.length > 0) {
			for (String f : fs) {
				if (f.equals(propName)) {
					find = true;
					break;
				}
			}
		}
		if(find)
			return true;
		List<ClassUtil.PropertieDescription> esAnnonationProperties = classInfo.getEsAnnonationProperties();
		if(esAnnonationProperties != null && esAnnonationProperties.size() > 0) {
			for (ClassUtil.PropertieDescription esAnnonationProperty : esAnnonationProperties) {
//				if (esAnnonationProperty.getName().equals(propName) ) {
//					if(!esAnnonationProperty.isPersistentESId()
//							&& !esAnnonationProperty.isPersistentESParentId()
//							&& !esAnnonationProperty.isPersistentESDocAsUpsert()
//							&& !esAnnonationProperty.isPersistentESRetryOnConflict()
//							&& !esAnnonationProperty.isPersistentESRouting()
//							&& !esAnnonationProperty.isPersistentESSource()
//							&& !esAnnonationProperty.isPersistentESVersion()
//							&& !esAnnonationProperty.isPersistentESVersionType()
//							)
				if (esAnnonationProperty.getName().equals(propName) && !esAnnonationProperty.isESPersistent()) {
					find = true;
					break;
				}
			}
		}
		return find;

	}

	// ignored fields
	@Override
	protected void processViews(SerializationConfig config, BeanSerializerBuilder builder) {
		super.processViews(config, builder);
		// ignore fields only for concrete class
		// note, that you can avoid or change this check
		Class<?> beanClass = builder.getBeanDescription().getBeanClass();
		ClassUtil.ClassInfo classInfo = ClassUtil.getClassInfo(beanClass);
		// if (builder.getBeanDescription().getBeanClass().equals(Entity.class))
		// {
		// get original writer
		List<BeanPropertyWriter> originalWriters = builder.getProperties();
		// create actual writers
		List<BeanPropertyWriter> writers = new ArrayList<BeanPropertyWriter>();
		String[] fs = this._getFilterFields(classInfo);
		for (BeanPropertyWriter writer : originalWriters) {
			final String propName = writer.getName();
			// if it isn't ignored field, add to actual writers list
			boolean find = isFilterField(  classInfo,  propName,  fs);

			if(!find){
				writers.add(writer);
			}
		}
		builder.setProperties(writers);
	}
}
