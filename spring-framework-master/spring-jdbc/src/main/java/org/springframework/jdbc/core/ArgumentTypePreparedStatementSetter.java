/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;

/**
 * Simple adapter for {@link PreparedStatementSetter} that applies
 * given arrays of arguments and JDBC argument types.
 *
 * @author Juergen Hoeller
 * @since 3.2.3
 */
public class ArgumentTypePreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {

	@Nullable
	private final Object[] args;

	@Nullable
	private final int[] argTypes;


	/**
	 * Create a new ArgTypePreparedStatementSetter for the given arguments.
	 * @param args the arguments to set
	 * @param argTypes the corresponding SQL types of the arguments
	 */
	public ArgumentTypePreparedStatementSetter(@Nullable Object[] args, @Nullable int[] argTypes) {
		if ((args != null && argTypes == null) || (args == null && argTypes != null) ||
				(args != null && args.length != argTypes.length)) {
			throw new InvalidDataAccessApiUsageException("args and argTypes parameters must match");
		}
		this.args = args;
		this.argTypes = argTypes;
	}


	/***
	 *
	 * 其中用于真正的 SQl 的 ps.executeUpdate 没有太多的需要讲解的，因为我们平时直接使用 JDBC 方式调用的方式的时候经常使用此方法，
	 * 但是，对于设置输入参数的函数 pss.setValues(ps)，	我们有必要深入研究一下，在没有分析源码之前，我们至少可以知道其功能，不妨再
	 * 回顾一下 Spring 使用 SQL 的执行过程，直接使用
	 *  jdbcTemplate.update("insert into user(name,age,sex) value(?,?,?)",
	 *  new Object[]{user.getUserName(),user.getpassword(),user.getSex(),
	 *  new int[]{Types.VARCHAR,TYPES.INTEGER,TYPES.VARCHAR}})
	 *
	 *  SQL语句对应的参数，对应参数的类型清楚明了，这都归于 Spring 为我们做了封装，而真正的 JDBC 调用其实非常的繁琐，你需要这么做
	 *
	 *  PreparedStatement updateSales = con.prepareStatement("insert into user(name,age,sex) values(?,?,?)");
	 *  udpateSales.setString(1,user.getName());
	 *  updateSales.setInt(2,user.getAge());
	 *  updateSales.setString(3,user.getSex());
	 *
	 *  那么我们看看 Spring 是如何做到封装上面的操作的呢。
	 *
	 *  首先，所有操作都是以 pss.setValues(ps) 为入口的，还记得之前分析的路程吗？这个 pss 所代表的当前类正是 ArgPreparedStatementSetter，
	 *  其中的setValues() 如下
	 *
	 *
	 */
	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		int parameterPosition = 1;
		if (this.args != null && this.argTypes != null) {
			//遍历每个参数作为类型的匹配及转换
			for (int i = 0; i < this.args.length; i++) {
				Object arg = this.args[i];
				//如果是集合的类，则需要进入集合类的内部递归解析集合的内部属性
				if (arg instanceof Collection && this.argTypes[i] != Types.ARRAY) {
					Collection<?> entries = (Collection<?>) arg;
					for (Object entry : entries) {
						if (entry instanceof Object[]) {
							Object[] valueArray = ((Object[]) entry);
							for (Object argValue : valueArray) {
								doSetValue(ps, parameterPosition, this.argTypes[i], argValue);
								parameterPosition++;
							}
						}
						else {
							doSetValue(ps, parameterPosition, this.argTypes[i], entry);
							parameterPosition++;
						}
					}
				}
				else {
					//解析当前属性
					doSetValue(ps, parameterPosition, this.argTypes[i], arg);
					parameterPosition++;
				}
			}
		}
	}

	/**
	 * Set the value for the prepared statement's specified parameter position using the passed in
	 * value and type. This method can be overridden by sub-classes if needed.
	 * @param ps the PreparedStatement
	 * @param parameterPosition index of the parameter position
	 * @param argType the argument type
	 * @param argValue the argument value
	 * @throws SQLException
	 * 对单个参数以及类型匹配处理
	 */
	protected void doSetValue(PreparedStatement ps, int parameterPosition, int argType, Object argValue)
			throws SQLException {

		StatementCreatorUtils.setParameterValue(ps, parameterPosition, argType, argValue);
	}

	@Override
	public void cleanupParameters() {
		StatementCreatorUtils.cleanupParameters(this.args);
	}

}
