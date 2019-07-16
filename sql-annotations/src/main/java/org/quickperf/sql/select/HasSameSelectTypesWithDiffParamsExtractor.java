/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2019-2019 the original author or authors.
 */

package org.quickperf.sql.select;

import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.QueryType;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import org.quickperf.ExtractablePerformanceMeasure;
import org.quickperf.measure.BooleanMeasure;
import org.quickperf.sql.QueryTypeRetriever;
import org.quickperf.sql.SqlExecution;
import org.quickperf.sql.SqlExecutions;

import java.util.*;

public class HasSameSelectTypesWithDiffParamsExtractor implements ExtractablePerformanceMeasure<SqlExecutions, BooleanMeasure> {

    public static final HasSameSelectTypesWithDiffParamsExtractor INSTANCE =
            new HasSameSelectTypesWithDiffParamsExtractor();

    private HasSameSelectTypesWithDiffParamsExtractor() {}

    @Override
    public BooleanMeasure extractPerfMeasureFrom(SqlExecutions sqlExecutions) {
        HasSameSelectTypesWithDiffParamsExtractor.SqlSelects sqlSelects = new HasSameSelectTypesWithDiffParamsExtractor.SqlSelects();
        for (SqlExecution sqlExecution : sqlExecutions) {
            for (QueryInfo query : sqlExecution.getQueries()) {
                if (   isSelectType(query)
                    && sqlSelects.sameSqlQueryWithDifferentParams(query)) {
                        return BooleanMeasure.TRUE;
                    }
                if (isSelectType(query)) {
                    sqlSelects.add(query);
                }
            }
        }
        return BooleanMeasure.FALSE;
    }

    private boolean isSelectType(QueryInfo query) {
        QueryType queryType = QueryTypeRetriever.INSTANCE.typeOf(query);
        return QueryType.SELECT.equals(queryType);
    }

    private static class SqlSelects {

        private final Map<String, ParamsCalls> callsParamsByQuery = new HashMap<>();

        private List<Object> getParamsOf(QueryInfo query) {

            List<List<ParameterSetOperation>> parametersList = query.getParametersList();
            List<ParameterSetOperation> parameterSetOperations =
                    retrieveParameterSetOperations(parametersList);

            List<Object> paramsList = new ArrayList<>();
            for (ParameterSetOperation parameterSetOperation : parameterSetOperations) {
                Object[] paramsOfThisQuery = parameterSetOperation.getArgs();
                paramsList.add(paramsOfThisQuery[1]);
            }

            return paramsList;

        }

        private List<ParameterSetOperation> retrieveParameterSetOperations(List<List<ParameterSetOperation>> parametersList) {
            if(parametersList.isEmpty()) {
                return Collections.emptyList();
            }
            if(parametersList.size() > 1) {
                String message = "Several parameter set not managed, please create an issue"
                        + " on https://github.com/quick-perf/quickperf/issues describing your"
                        + " use case.";
                throw new IllegalStateException(message);
            }
            return parametersList.get(0);
        }

        void add(QueryInfo query) {
            String queryAsString = query.getQuery();
            HasSameSelectTypesWithDiffParamsExtractor.SqlSelects.ParamsCalls paramsCalls = callsParamsByQuery.get(queryAsString);
            if (paramsCalls == null) {
                paramsCalls = new HasSameSelectTypesWithDiffParamsExtractor.SqlSelects.ParamsCalls();
            }
            List<Object> paramsList = getParamsOf(query);
            paramsCalls.addParams(paramsList);
            callsParamsByQuery.put(queryAsString, paramsCalls);
        }

        boolean sameSqlQueryWithDifferentParams(QueryInfo query) {
            String queryAsString = query.getQuery();
            HasSameSelectTypesWithDiffParamsExtractor.SqlSelects.ParamsCalls paramsCalls = callsParamsByQuery.get(queryAsString);
            if (paramsCalls == null) {
                return false;
            }
            List<Object> paramsList = getParamsOf(query);
            return !paramsCalls.alreadySameParamsCalled(paramsList);
        }

        private static class ParamsCalls {

            private final List<List<Object>> paramsCalls = new ArrayList<>();

            boolean alreadySameParamsCalled(List<Object> params) {
                return paramsCalls.contains(params);
            }

            void addParams(List<Object> params) {
                paramsCalls.add(params);
            }

        }

    }

}
