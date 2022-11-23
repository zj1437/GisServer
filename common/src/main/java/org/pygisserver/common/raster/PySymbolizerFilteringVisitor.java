package org.pygisserver.common.raster;

import org.geotools.styling.Graphic;
import org.geotools.styling.Rule;
import org.geotools.styling.StyleFactoryImpl;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.opengis.filter.Filter;
import org.opengis.style.Description;

import java.util.ArrayList;
import java.util.List;

public class PySymbolizerFilteringVisitor extends DuplicatingStyleVisitor {

    public void visit(Rule rule) {
        Rule copy = null;

        Filter filterCopy = null;

        if (rule.getFilter() != null) {
            Filter filter = rule.getFilter();
            filterCopy = copy(filter);
        }

        // modified to deal with null copies that should be skipped
        List<Symbolizer> symArray = new ArrayList<>();
        for (Symbolizer sym : rule.symbolizers()) {
            Symbolizer symcopy = copy(sym);
            if (symcopy != null) symArray.add(symcopy);
        }
        Symbolizer[] symsCopy = symArray.toArray(new Symbolizer[symArray.size()]);

        Graphic legendCopy = copy((Graphic) rule.getLegend());
        Description descCopy = rule.getDescription();
        descCopy = copy(descCopy);

        copy =
                new StyleFactoryImpl()
                        .createRule(
                                symsCopy,
                                descCopy,
                                legendCopy,
                                rule.getName(),
                                filterCopy,
                                rule.isElseFilter(),
                                rule.getMaxScaleDenominator(),
                                rule.getMinScaleDenominator());

        if (STRICT && !copy.equals(rule)) {
            throw new IllegalStateException("Was unable to duplicate provided Rule:" + rule);
        }
        pages.push(copy);
    }
}
