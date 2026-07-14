package org.frameworkset.elasticsearch.schedule.timer;

import java.util.ArrayList;
import java.util.List;

/**
 * 节假日调度配置类
 * 用于指定星期六、星期天及特定节假日不执行调度任务
 * <p>Copyright (c) 2025</p>
 * @Date 2025/6/8
 * @author biaoping.yin
 * @version 1.0
 */
public class HolidayScheduleConfig {

	/**
	 * 是否跳过星期六
	 */
	private boolean skipSaturday;
	/**
	 * 是否跳过星期天
	 */
	private boolean skipSunday;
	/**
	 * 是否跳过元旦（1月1日起，放假3天）
	 */
	private boolean skipNewYearsDay;
	/**
	 * 是否跳过劳动节（5月1日起，放假5天）
	 */
	private boolean skipLaborDay;
	/**
	 * 是否跳过端午节（放假3天）
	 */
	private boolean skipDragonBoatFestival;
	/**
	 * 是否跳过中秋节（放假3天）
	 */
	private boolean skipMidAutumnFestival;
	/**
	 * 是否跳过国庆节（10月1日起，放假7天）
	 */
	private boolean skipNationalDay;
	/**
	 * 是否跳过春节（放假9天）
	 */
	private boolean skipSpringFestival;
	/**
	 * 自定义节假日日期列表，格式：yyyy-MM-dd
	 */
	private List<String> customHolidays;

	public boolean isSkipSaturday() {
		return skipSaturday;
	}

	public void setSkipSaturday(boolean skipSaturday) {
		this.skipSaturday = skipSaturday;
	}

	public boolean isSkipSunday() {
		return skipSunday;
	}

	public void setSkipSunday(boolean skipSunday) {
		this.skipSunday = skipSunday;
	}

	public boolean isSkipNewYearsDay() {
		return skipNewYearsDay;
	}

	public void setSkipNewYearsDay(boolean skipNewYearsDay) {
		this.skipNewYearsDay = skipNewYearsDay;
	}

	public boolean isSkipLaborDay() {
		return skipLaborDay;
	}

	public void setSkipLaborDay(boolean skipLaborDay) {
		this.skipLaborDay = skipLaborDay;
	}

	public boolean isSkipDragonBoatFestival() {
		return skipDragonBoatFestival;
	}

	public void setSkipDragonBoatFestival(boolean skipDragonBoatFestival) {
		this.skipDragonBoatFestival = skipDragonBoatFestival;
	}

	public boolean isSkipMidAutumnFestival() {
		return skipMidAutumnFestival;
	}

	public void setSkipMidAutumnFestival(boolean skipMidAutumnFestival) {
		this.skipMidAutumnFestival = skipMidAutumnFestival;
	}

	public boolean isSkipNationalDay() {
		return skipNationalDay;
	}

	public void setSkipNationalDay(boolean skipNationalDay) {
		this.skipNationalDay = skipNationalDay;
	}

	public boolean isSkipSpringFestival() {
		return skipSpringFestival;
	}

	public void setSkipSpringFestival(boolean skipSpringFestival) {
		this.skipSpringFestival = skipSpringFestival;
	}

	public List<String> getCustomHolidays() {
		return customHolidays;
	}

	public void setCustomHolidays(List<String> customHolidays) {
		this.customHolidays = customHolidays;
	}

	/**
	 * 设置跳过星期六
	 * @return
	 */
	public HolidayScheduleConfig skipSaturday(){
		this.skipSaturday = true;
		return this;
	}

	/**
	 * 设置跳过星期天
	 * @return
	 */
	public HolidayScheduleConfig skipSunday(){
		this.skipSunday = true;
		return this;
	}

	/**
	 * 设置跳过周末（星期六和星期天）
	 * @return
	 */
	public HolidayScheduleConfig skipWeekends(){
		this.skipSaturday = true;
		this.skipSunday = true;
		return this;
	}

	/**
	 * 设置跳过元旦（1月1日起，放假3天）
	 * @return
	 */
	public HolidayScheduleConfig skipNewYearsDay(){
		this.skipNewYearsDay = true;
		return this;
	}

	/**
	 * 设置跳过劳动节（5月1日起，放假5天）
	 * @return
	 */
	public HolidayScheduleConfig skipLaborDay(){
		this.skipLaborDay = true;
		return this;
	}

	/**
	 * 设置跳过端午节（放假3天）
	 * @return
	 */
	public HolidayScheduleConfig skipDragonBoatFestival(){
		this.skipDragonBoatFestival = true;
		return this;
	}

	/**
	 * 设置跳过中秋节（放假3天）
	 * @return
	 */
	public HolidayScheduleConfig skipMidAutumnFestival(){
		this.skipMidAutumnFestival = true;
		return this;
	}

	/**
	 * 设置跳过国庆节（10月1日起，放假7天）
	 * @return
	 */
	public HolidayScheduleConfig skipNationalDay(){
		this.skipNationalDay = true;
		return this;
	}

	/**
	 * 设置跳过春节（放假9天）
	 * @return
	 */
	public HolidayScheduleConfig skipSpringFestival(){
		this.skipSpringFestival = true;
		return this;
	}

	/**
	 * 设置跳过所有内置节假日（元旦3天、劳动节5天、端午节3天、中秋节3天、国庆节7天、春节9天）及周末
	 * @return
	 */
	public HolidayScheduleConfig skipAllHolidays(){
		this.skipNewYearsDay = true;
		this.skipLaborDay = true;
		this.skipDragonBoatFestival = true;
		this.skipMidAutumnFestival = true;
		this.skipNationalDay = true;
		this.skipSpringFestival = true;
		return this;
	}

	/**
	 * 添加自定义节假日日期
	 * @param holidayDate 格式：yyyy-MM-dd
	 * @return
	 */
	public HolidayScheduleConfig addCustomHoliday(String holidayDate){
		if(customHolidays == null){
			customHolidays = new ArrayList<>();
		}
		if(holidayDate != null && !holidayDate.trim().equals("")){
			customHolidays.add(holidayDate.trim());
		}
		return this;
	}

}
