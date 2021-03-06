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
package com.meidusa.amoeba.parser.expression;

/**
 * 参数表达式
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public class ParameterExpression extends Expression {
	
	private int index;
	/**
	 * 构造函数，给成员变量index赋值
	 * @param index
	 */
	public ParameterExpression(int index){
		this.index = index;
	}
	/**
	 * 是否可计算，如果parameters为空，返回null，否则parameters的第index个元素
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Comparable evaluate(Object[] parameters) {
		if(parameters == null){
			return null;
		}
		return (Comparable)parameters[index];
	}
	/**
	 * 参数表达式需要实时计算
	 */
	public boolean isRealtime(){
		return true;
	}
	
	@Override
	public Expression reverse() {
		return this;
	}
	/**
	 * 在builder的后面加一个"?"
	 */
	@Override
	protected void toString(StringBuilder builder) {
		builder.append("?");
	}

}
