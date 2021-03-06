/*
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU General Public License for more details. 
 * 	You should have received a copy of the GNU General Public License along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.meidusa.amoeba.parser.function;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.meidusa.amoeba.parser.expression.Expression;
import com.meidusa.amoeba.sqljep.ParseException;

/**
 * 把时间转换成想要的类型类
 * @author Li Hui
 *
 */
public class TimeForType extends AbstractFunction{
	public static final int YEAR = 1;
	public static final int MONTH = 2;
	public static final int WEEK_OF_YEAR = 3;
	public static final int DAY_OF_MONTH = 5;
	public static final int HOUR_OF_DAY = 11;
	public static final int MINUTE = 12;
	public static final int SECOND = 13;
	public static final int MILLISECOND = 14;
	public static final int MICROSECOND = 21;
	private int field;
	/**
	 * 根据需要（以上选择的field不同）得到不同的时间类型
	 */
	@SuppressWarnings("unchecked")
	public Comparable evaluate(List<Expression> list, Object[] parameters)
			throws ParseException {
		if(list.size()==1){
			Comparable param1 = list.get(0).evaluate(parameters);
			Date time = null;
			if(param1 instanceof String){
				time = TimeConverter.converter((String)param1);
			}else if(param1 instanceof Date){
				time = (Date)param1;
			}else{
				return null;
			}
			Calendar cal = Calendar.getInstance();
			cal.setTime(time);
			if(getField() == MICROSECOND){
				int result = cal.get(MILLISECOND);
				return result * 1000;
			}
			
			return cal.get(field);
		}
		return null;
	}
	/**
	 * 返回field的值
	 * @return
	 */
	public int getField() {
		return field;
	}
	/**
	 * 重试设置field的值
	 * @param field 时间的filed字段
	 */
	public void setField(int field) {
		this.field = field;
	}
	
	public static void main(String args[]){
		Calendar cal = Calendar.getInstance();
		int result = cal.get(MILLISECOND);
		System.out.println(cal.getTime());
		System.out.println(result);
	}
}
