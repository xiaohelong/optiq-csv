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

import net.hydromatic.linq4j.*;
import net.hydromatic.linq4j.expressions.*;

import net.hydromatic.optiq.*;
import net.hydromatic.optiq.impl.java.JavaTypeFactory;
import net.hydromatic.optiq.rules.java.EnumerableConvention;
import net.hydromatic.optiq.rules.java.JavaRules;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.eigenbase.rel.RelNode;
import org.eigenbase.relopt.RelOptTable;
import org.eigenbase.reltype.RelDataType;
import org.eigenbase.util.Pair;

import java.io.IOException;
import java.util.*;

/**
 * Table based on a CSV file.
 */
public class SolrTable extends AbstractQueryable<Object[]>
        implements TranslatableTable<Object[]> {
    private final Schema schema;
    private final String tableName;
    private final RelDataType rowType;
    private final List<SolrFieldType> fieldTypes;
    private final SolrDocumentList doc;
    private static List<String> names;

    /** Creates a CsvTable. */
    SolrTable(Schema schema, String tableName, SolrDocumentList doc, RelDataType rowType,
             List<SolrFieldType> fieldTypes) {
        this.schema = schema;
        this.tableName = tableName;
        this.rowType = rowType;
        this.fieldTypes = fieldTypes;
        this.doc = doc;

        assert rowType != null;
        assert schema != null;
        assert tableName != null;
    }

    public String toString() {
        return "SolrTable {" + tableName + "}";
    }

    public QueryProvider getProvider() {
        return schema.getQueryProvider();
    }

    public DataContext getDataContext() {
        return schema;
    }

    public Class getElementType() {
        return Object[].class;
    }

    public RelDataType getRowType() {
        return rowType;
    }

    public Statistic getStatistic() {
        return Statistics.UNKNOWN;
    }

    public Expression getExpression() {
        return Expressions.convert_(
                Expressions.call(
                        schema.getExpression(),
                        "getTable",
                        Expressions.<Expression>list()
                                .append(Expressions.constant(tableName))
                                .append(Expressions.constant(getElementType()))),
                SolrTable.class);
    }

    public Iterator<Object[]> iterator() {
        return Linq4j.enumeratorIterator(enumerator());
    }

    public Enumerator<Object[]> enumerator() {
        SolrDocumentList c = (SolrDocumentList) doc.clone();
        return new SolrEnumerator(c.iterator(),
                fieldTypes.toArray(new SolrFieldType[fieldTypes.size()]), names);
    }

    /** Returns an enumerable over a given projection of the fields. */
    public Enumerable<Object[]> project(final int[] fields) {
        return new AbstractEnumerable<Object[]>() {
            public Enumerator<Object[]> enumerator() {
                SolrDocumentList c = (SolrDocumentList) doc.clone();

                return new SolrEnumerator(c.iterator(),
                        fieldTypes.toArray(new SolrFieldType[fieldTypes.size()]), fields);
            }
        };
    }

    public RelNode toRel(
            RelOptTable.ToRelContext context,
            RelOptTable relOptTable) {
        return new JavaRules.EnumerableTableAccessRel(
                context.getCluster(),
                context.getCluster().traitSetOf(EnumerableConvention.INSTANCE),
                relOptTable,
                getExpression(),
                getElementType());
    }

    /** Deduces the names and types of a table's columns by reading the first line
     * of a CSV file. */
    static RelDataType deduceRowType(JavaTypeFactory typeFactory, SolrServer srv,
                                     List<SolrFieldType> fieldTypes) {
        final List<RelDataType> types = new ArrayList<RelDataType>();
        names = new ArrayList<String>();

        LukeRequest lukeRequest = new LukeRequest();
        lukeRequest.setNumTerms(1);
        LukeResponse lukeResponse = null;
        try {
            lukeResponse = lukeRequest.process(srv );
        } catch (SolrServerException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        List<LukeResponse.FieldInfo> sorted = new ArrayList<LukeResponse.FieldInfo>(lukeResponse.getFieldInfo().values());

        for (LukeResponse.FieldInfo infoEntry : sorted) {

            SolrFieldType fieldType = SolrFieldType.of(infoEntry.getType().toUpperCase());
            String name = infoEntry.getName();

                final RelDataType type;
                if (fieldType == null) {
                    type = typeFactory.createJavaType(String.class);
                } else {
                    type = fieldType.toType(typeFactory);
                }
                names.add(name);
                types.add(type);
                fieldTypes.add(fieldType);
            }


        if (names.isEmpty()) {
            names.add("line");
            types.add(typeFactory.createJavaType(String.class));
        }
        return typeFactory.createStructType(Pair.zip(names, types));
    }
}
// End SolrTable.java
