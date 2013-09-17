package net.hydromatic.optiq.impl.csv;

import org.eigenbase.rel.RelNode;
import org.eigenbase.relopt.Convention;
import org.eigenbase.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: bugg
 * Date: 16/09/13
 * Time: 19:01
 * To change this template use File | Settings | File Templates.
 */
public interface SolrRel extends RelNode {
    void implement(Implementor implementor);

    /** Calling convention for relational operations that occur in MongoDB. */
    final Convention CONVENTION = new Convention.Impl("SOLR", SolrRel.class);

    class Implementor {
        final List<Pair<String, String>> list =
                new ArrayList<Pair<String, String>>();

        SolrTable table;

        public void add(String findOp, String aggOp) {
            list.add(Pair.of(findOp, aggOp));
        }

        public void visitChild(int ordinal, RelNode input) {
            assert ordinal == 0;
            ((SolrRel) input).implement(this);
        }
    }
}
