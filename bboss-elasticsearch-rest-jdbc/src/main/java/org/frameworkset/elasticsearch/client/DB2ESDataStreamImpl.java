package org.frameworkset.elasticsearch.client;/*
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

import com.frameworkset.common.poolman.ConfigSQLExecutor;
import com.frameworkset.common.poolman.SQLExecutor;
import com.frameworkset.common.poolman.handle.ResultSetHandler;
import com.frameworkset.orm.transaction.TransactionManager;
import org.frameworkset.elasticsearch.client.schedule.ScheduleService;
import org.frameworkset.persitent.util.SQLInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 数据库同步到Elasticsearch
 */
public class DB2ESDataStreamImpl extends DBESDataStream{
	private ESJDBC esjdbc;
	private ScheduleService scheduleService;
	private static Logger logger = LoggerFactory.getLogger(DataStream.class);
	private boolean inited;
	public void setExternalTimer(boolean externalTimer) {
		this.esjdbc.setExternalTimer(externalTimer);
	}
	private Lock lock = new ReentrantLock();
	public void init(){
		if(inited )
			return;
		if(esjdbc == null){
			throw new ESDataImportException("ESJDBC is null.");
		}

		try {
			lock.lock();
			this.initES(esjdbc.getApplicationPropertiesFile());
			this.initDS(esjdbc.getDbConfig());
			initOtherDSes(esjdbc.getConfigs());
			this.initSQLInfo();
			this.initSchedule();
			inited = true;
		}
		catch (Exception e) {
			inited = true;
			throw new ESDataImportException(e);
		}
		finally{


			lock.unlock();
		}
	}

	@Override
	public void stop() {
		if(esjdbc != null)
			this.esjdbc.stop();
	}

	@Override
	public String getConfigString() {
		return this.toString();
	}

	/**
	 *
	 * @throws ESDataImportException
	 */
	public void execute() throws ESDataImportException{

		try {
			this.init();
			if(this.scheduleService == null) {//一次性执行数据导入操作
				long importStartTime = System.currentTimeMillis();
				firstImportData();
				long importEndTime = System.currentTimeMillis();
				if(esjdbc != null && this.esjdbc.isPrintTaskLog() && logger.isInfoEnabled())
					logger.info(new StringBuilder().append("Execute job Take ").append((importEndTime - importStartTime)).append(" ms").toString());
			}
			else{//定时增量导入数据操作
				if(!scheduleService.isExternalTimer()) {//内部定时任务引擎
					scheduleService.timeSchedule();
				}
				else{ //外部定时任务引擎执行的方法，比如quartz之类的
					scheduleService.externalTimeSchedule();
				}
			}
		}
		catch (Exception e) {
			throw new ESDataImportException(e);
		}
		finally{

		}
	}
	public void initSQLInfo(){

		if(esjdbc.getSql() == null || esjdbc.getSql().equals("")){

			try {
				ConfigSQLExecutor executor = new ConfigSQLExecutor(esjdbc.getSqlFilepath());
				SQLInfo sqlInfo = executor.getSqlInfo(esjdbc.getSqlName());
				esjdbc.setSql(sqlInfo.getSql());
				esjdbc.setExecutor(executor);
			}
			catch (SQLException e){
				throw new ESDataImportException(e);
			}

		}
		esjdbc.setStatusTableId(esjdbc.getSql().hashCode());
	}
	public void setEsjdbc(ESJDBC esjdbc){
		this.esjdbc = esjdbc;
	}

	public void initSchedule(){
		if(this.esjdbc.getScheduleConfig() != null) {
			this.scheduleService = new ScheduleService();
			this.scheduleService.init(this.esjdbc);
		}
	}



	private void firstImportData() throws Exception {
		ResultSetHandler resultSetHandler = new DefaultResultSetHandler(esjdbc,esjdbc.getBatchSize());
		if(esjdbc.getDataRefactor() == null || !esjdbc.getDbConfig().isEnableDBTransaction()){
			if (esjdbc.getExecutor() == null) {
				SQLExecutor.queryWithDBNameByNullRowHandler(resultSetHandler, esjdbc.getDbConfig().getDbName(), esjdbc.getSql());
			} else {
				esjdbc.getExecutor().queryWithDBNameByNullRowHandler(resultSetHandler, esjdbc.getDbConfig().getDbName(), esjdbc.getSqlName());
			}
		}
		else {

			TransactionManager transactionManager = new TransactionManager();
			try {
				transactionManager.begin(TransactionManager.RW_TRANSACTION);
				if (esjdbc.getExecutor() == null) {
					SQLExecutor.queryWithDBNameByNullRowHandler(resultSetHandler, esjdbc.getDbConfig().getDbName(), esjdbc.getSql());
				} else {
					esjdbc.getExecutor().queryWithDBNameByNullRowHandler(resultSetHandler, esjdbc.getDbConfig().getDbName(), esjdbc.getSqlName());
				}
				transactionManager.commit();
			} finally {
				transactionManager.releasenolog();
			}
		}
	}






}
