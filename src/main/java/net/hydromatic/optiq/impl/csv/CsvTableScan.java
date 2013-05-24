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

import net.hydromatic.linq4j.expressions.*;
import net.hydromatic.optiq.impl.java.JavaTypeFactory;
import net.hydromatic.optiq.rules.java.*;

import org.eigenbase.rel.RelNode;
import org.eigenbase.rel.TableAccessRelBase;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.RelDataType;
import org.eigenbase.reltype.RelDataTypeFactory;
import org.eigenbase.util.Pair;

import java.util.*;

/**
 * Relational expression representing a scan of a CSV file.
 *
 * <p>Like any table scan, it serves as a leaf node of a query tree.</p>
 */
public class CsvTableScan
    extends TableAccessRelBase
    implements EnumerableRel
{
  final CsvTable csvTable;
  final List<String> fieldList;
  final PhysType physType;

  protected CsvTableScan(
      RelOptCluster cluster,
      RelOptTable table,
      CsvTable csvTable,
      List<String> fieldList)
  {
    super(
        cluster, cluster.traitSetOf(EnumerableConvention.ARRAY), table);
    this.csvTable = csvTable;
    this.fieldList = fieldList;
    this.physType =
        PhysTypeImpl.of(
            (JavaTypeFactory) cluster.getTypeFactory(),
            getRowType(),
            (EnumerableConvention) getConvention());

    assert csvTable != null;
  }

  public PhysType getPhysType() {
    return physType;
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    assert inputs.isEmpty();
    return new CsvTableScan(getCluster(), table, csvTable, fieldList);
  }

  @Override
  public void explain(RelOptPlanWriter pw) {
    pw.explain(
        this,
        Collections.singletonList(
            Pair.<String, Object>of("table", table.getQualifiedName())));
  }

  @Override
  public RelDataType deriveRowType() {
    final RelDataTypeFactory.FieldInfoBuilder builder =
        new RelDataTypeFactory.FieldInfoBuilder();
    for (String field : fieldList) {
      builder.add(table.getRowType().getField(field));
    }
    return getCluster().getTypeFactory().createStructType(builder);
  }

  public BlockExpression implement(EnumerableRelImplementor implementor) {
    Expression expression = null;
    return Blocks.toBlock(expression);
  }
}

// End CsvTableScan.java