/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package com.moparisthebest.poi.ss.formula.functions;

import com.moparisthebest.poi.ss.formula.eval.AreaEval;
import com.moparisthebest.poi.ss.formula.eval.BlankEval;
import com.moparisthebest.poi.ss.formula.eval.BoolEval;
import com.moparisthebest.poi.ss.formula.eval.ErrorEval;
import com.moparisthebest.poi.ss.formula.eval.EvaluationException;
import com.moparisthebest.poi.ss.formula.eval.NumberEval;
import com.moparisthebest.poi.ss.formula.eval.OperandResolver;
import com.moparisthebest.poi.ss.formula.eval.RefEval;
import com.moparisthebest.poi.ss.formula.eval.StringEval;
import com.moparisthebest.poi.ss.formula.eval.ValueEval;

/**
 * Implementation of the various ISxxx Logical Functions, which
 *  take a single expression argument, and return True or False.
 */
public abstract class LogicalFunction extends Fixed1ArgFunction {

    @SuppressWarnings("unused")
    public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
		ValueEval ve;
		try {
			ve = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
		} catch (EvaluationException e) {
			// Note - it is more usual to propagate error codes straight to the result like this:
            // but logical functions behave a little differently
			// return e.getErrorEval();

		    // this will usually cause a 'FALSE' result except for ISNONTEXT()
			ve = e.getErrorEval();
		}
		return BoolEval.valueOf(evaluate(ve));

	}
	/**
	 * @param arg any {@link ValueEval}, potentially {@link BlankEval} or {@link ErrorEval}.
	 */
	protected abstract boolean evaluate(ValueEval arg);

	public static final Function ISLOGICAL = new LogicalFunction() {
		protected boolean evaluate(ValueEval arg) {
			return arg instanceof BoolEval;
		}
	};
	public static final Function ISNONTEXT = new LogicalFunction() {
		protected boolean evaluate(ValueEval arg) {
			return !(arg instanceof StringEval);
		}
	};
	public static final Function ISNUMBER = new LogicalFunction() {
		protected boolean evaluate(ValueEval arg) {
			return arg instanceof NumberEval;
		}
	};
	public static final Function ISTEXT = new LogicalFunction() {
		protected boolean evaluate(ValueEval arg) {
			return arg instanceof StringEval;
		}
	};

	public static final Function ISBLANK = new LogicalFunction() {

		protected boolean evaluate(ValueEval arg) {
			return arg instanceof BlankEval;
		}
	};

	public static final Function ISERROR = new LogicalFunction() {

		protected boolean evaluate(ValueEval arg) {
			return arg instanceof ErrorEval;
		}
	};

    /**
     * Implementation of Excel <tt>ISERR()</tt> function.<p/>
     *
     * <b>Syntax</b>:<br/>
     * <b>ISERR</b>(<b>value</b>)<p/>
     *
     * <b>value</b>  The value to be tested<p/>
     *
     * Returns the logical value <tt>TRUE</tt> if value refers to any error value except
     * <tt>'#N/A'</tt>; otherwise, it returns <tt>FALSE</tt>.
     */
    public static final Function ISERR = new LogicalFunction() {
        @Override
        protected boolean evaluate(ValueEval arg) {
            if (arg instanceof ErrorEval) {
                return arg != ErrorEval.NA;
            }
            return false;
        }
    };

	/**
	 * Implementation for Excel ISNA() function.<p/>
	 *
	 * <b>Syntax</b>:<br/>
	 * <b>ISNA</b>(<b>value</b>)<p/>
	 *
	 * <b>value</b>  The value to be tested<br/>
	 * <br/>
	 * Returns <tt>TRUE</tt> if the specified value is '#N/A', <tt>FALSE</tt> otherwise.
	 */
	public static final Function ISNA = new LogicalFunction() {

		protected boolean evaluate(ValueEval arg) {
			return arg == ErrorEval.NA;
		}
	};

	public static final Function ISREF = new Fixed1ArgFunction() {

		public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
			if (arg0 instanceof RefEval || arg0 instanceof AreaEval) {
				return BoolEval.TRUE;
			}
			return BoolEval.FALSE;
		}
	};
}
