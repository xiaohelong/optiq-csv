/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package net.hydromatic.optiq.impl.csv;

import net.hydromatic.linq4j.expressions.Expression;

import net.hydromatic.optiq.*;
import net.hydromatic.optiq.impl.TableInSchemaImpl;
import net.hydromatic.optiq.impl.java.MapSchema;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrDocumentList;
import org.eigenbase.reltype.RelDataType;

import java.io.*;
import java.util.*;

/**
 * Schema mapped onto a directory of CSV files. Each table in the schema
 * is a CSV file in that directory.
 */
public class SolrSchema extends MapSchema {
    private final String host;
    private final String core;
    public SolrServer server;
    public SolrSchema(
            Schema parentSchema,
            String host,
            String core,
            Expression expression) {
        super(parentSchema, core, expression);
        this.host = host;
        this.core = core;
    }

    @Override
    protected Collection<TableInSchema> initialTables() {
        final List<TableInSchema> list = new ArrayList<TableInSchema>();

         server = new HttpSolrServer(host);


            final List<SolrFieldType> fieldTypes = new ArrayList<SolrFieldType>();
        SolrQuery query = new SolrQuery();
        query.setQuery( "*:*" );
        query.setFacetLimit(100000);
        query.setParam("rows","10000");

        SolrDocumentList res = null;
        try {
             res = server.query(query).getResults();
        } catch (SolrServerException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        assert res != null;
        final RelDataType rowType =
                    SolrTable.deduceRowType(typeFactory, server, fieldTypes);
            final SolrTable table;

                table = new SolrTable(this, core, res, rowType, fieldTypes);

            list.add(new TableInSchemaImpl(this, core, TableType.TABLE, table));

        return list;
    }
}

// End SolrSchema.java
