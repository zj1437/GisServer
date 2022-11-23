package org.pygisserver.render.style;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pygisserver.render.style.StyleUtils.LegendLayout;
import org.geotools.styling.Rule;

public class ServerLegendMerger {

    public static class MergeOptions {
        List<RenderedImage> imageStack;
        int dx;
        int dy;
        int margin;
        int labelMargin;
        Color backgroundColor;
        boolean transparent;
        boolean antialias;
        LegendLayout layout;
        int rowWidth;
        int rows;
        int columnHeight;
        int columns;
        Font labelFont;

        public MergeOptions(
                List<RenderedImage> imageStack,
                int dx,
                int dy,
                int margin,
                int labelMargin,
                Color backgroundColor,
                boolean transparent,
                boolean antialias,
                LegendLayout layout,
                int rowWidth,
                int rows,
                int columnHeight,
                int columns,
                Font labelFont) {
            super();
            this.imageStack = imageStack;
            this.dx = dx;
            this.dy = dy;
            this.margin = margin;
            this.labelMargin = labelMargin;
            this.backgroundColor = backgroundColor;
            this.transparent = transparent;
            this.antialias = antialias;
            this.layout = layout;
            this.rowWidth = rowWidth;
            this.rows = rows;
            this.columnHeight = columnHeight;
            this.columns = columns;
            this.labelFont = labelFont;
        }
        
        public MergeOptions(
                List<RenderedImage> imageStack,
                int dx,
                int dy,
                int margin,
                int labelMargin,
                LegendGraphicEntity req) {
            this(
                    imageStack,
                    dx,
                    dy,
                    margin,
                    labelMargin,
                    StyleUtils.getBackgroundColor(req),
                    req.isTransparent(),
                    StyleUtils.isFontAntiAliasing(req),
                    StyleUtils.getLayout(req),
                    StyleUtils.getRowWidth(req),
                    StyleUtils.getRows(req),
                    StyleUtils.getColumnHeight(req),
                    StyleUtils.getColumns(req),
                    StyleUtils.getLabelFont(req));
        }

        public List<RenderedImage> getImageStack() {
            return imageStack;
        }

        public void setImageStack(List<RenderedImage> imageStack) {
            this.imageStack = imageStack;
        }

        public int getDx() {
            return dx;
        }

        public void setDx(int dx) {
            this.dx = dx;
        }

        public int getDy() {
            return dy;
        }

        public void setDy(int dy) {
            this.dy = dy;
        }

        public int getMargin() {
            return margin;
        }

        public void setMargin(int margin) {
            this.margin = margin;
        }

        public Color getBackgroundColor() {
            return backgroundColor;
        }

        public void setBackgroundColor(Color backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        public boolean isTransparent() {
            return transparent;
        }

        public void setTransparent(boolean transparent) {
            this.transparent = transparent;
        }

        public boolean isAntialias() {
            return antialias;
        }

        public void setAntialias(boolean antialias) {
            this.antialias = antialias;
        }

        public LegendLayout getLayout() {
            return layout;
        }

        public void setLayout(LegendLayout layout) {
            this.layout = layout;
        }

        public int getRowWidth() {
            return rowWidth;
        }

        public void setRowWidth(int rowWidth) {
            this.rowWidth = rowWidth;
        }

        public int getRows() {
            return rows;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }

        public int getColumnHeight() {
            return columnHeight;
        }

        public void setColumnHeight(int columnHeight) {
            this.columnHeight = columnHeight;
        }

        public int getColumns() {
            return columns;
        }

        public void setColumns(int columns) {
            this.columns = columns;
        }

        public Font getLabelFont() {
            return labelFont;
        }

        public void setLabelFont(Font labelFont) {
            this.labelFont = labelFont;
        }

        public int getLabelMargin() {
            return labelMargin;
        }

        public void setLabelMargin(int labelMargin) {
            this.labelMargin = labelMargin;
        }

        public static MergeOptions createFromRequest(
                List<RenderedImage> imageStack,
                int dx,
                int dy,
                int margin,
                int labelMargin,
                LegendGraphicEntity req) {
            return new ServerLegendMerger.MergeOptions(
                    imageStack,
                    dx,
                    dy,
                    margin,
                    labelMargin,
                    req);
        }
    }

    public static BufferedImage mergeRasterLegends(MergeOptions mergeOptions) {
        List<RenderedImage> imageStack = mergeOptions.getImageStack();
        LegendLayout layout = mergeOptions.getLayout();

        List<BufferedImage> nodes = new ArrayList<BufferedImage>();
        final int imgCount = imageStack.size();
        for (int i = 0; i < imgCount; i++) {
            nodes.add((BufferedImage) imageStack.get(i));
        }

        BufferedImage finalLegend = null;
        if (layout == LegendLayout.HORIZONTAL) {
            Row[] rows = createRows(nodes, mergeOptions.getRowWidth(), mergeOptions.getRows());
            finalLegend = buildFinalHLegend(rows, mergeOptions);
        }

        if (layout == LegendLayout.VERTICAL) {
            Column[] columns =
                    createColumns(
                            nodes,
                            mergeOptions.getColumnHeight(),
                            mergeOptions.getColumns(),
                            null,
                            false);
            finalLegend = buildFinalVLegend(columns, mergeOptions);
        }

        return finalLegend;
    }

    public static BufferedImage mergeLegends(
            Rule[] rules, LegendGraphicEntity req, MergeOptions mergeOptions) {
        List<RenderedImage> imageStack = mergeOptions.getImageStack();

        // Builds legend nodes (graphics + label)
        final int imgCount = imageStack.size();
        List<BufferedImage> nodes = new ArrayList<BufferedImage>();
        // Single legend, no rules, no force label
        if (imgCount == 1 && rules == null) {
            return (BufferedImage) imageStack.get(0);
        } else {
            for (int i = 0; i < imgCount; i++) {
                BufferedImage img = (BufferedImage) imageStack.get(i);
                if (rules != null && rules[i] != null) {
                    BufferedImage label = renderLabel(img, rules[i], req, mergeOptions);
                    if (label != null) {
                        img =
                                joinBufferedImageHorizzontally(
                                        img,
                                        label,
                                        mergeOptions.getLabelFont(),
                                        mergeOptions.isAntialias(),
                                        mergeOptions.isTransparent(),
                                        mergeOptions.getBackgroundColor(),
                                        mergeOptions.getLabelMargin());
                    }
                    nodes.add(img);
                } else {
                    nodes.add(img);
                }
            }
        }

        // Sets legend nodes into a matrix according to layout rules
        LegendLayout layout = mergeOptions.getLayout();
        BufferedImage finalLegend = null;
        if (layout == LegendLayout.HORIZONTAL) {
            Row[] rows = createRows(nodes, mergeOptions.getRowWidth(), mergeOptions.getRows());
            finalLegend = buildFinalHLegend(rows, mergeOptions);
        }

        if (layout == LegendLayout.VERTICAL) {
            Column[] columns =
                    createColumns(
                            nodes,
                            mergeOptions.getColumnHeight(),
                            mergeOptions.getColumns(),
                            req,
                            true);
            finalLegend = buildFinalVLegend(columns, mergeOptions);
        }

        return finalLegend;
    }

    public static BufferedImage mergeGroups(Rule[] rules, MergeOptions mergeOptions) {
        List<RenderedImage> imageStack = mergeOptions.getImageStack();

        final int imgCount = imageStack.size();
        if (imgCount == 1 && rules == null) {
            return (BufferedImage) imageStack.get(0);
        }

        List<BufferedImage> nodes = new ArrayList<BufferedImage>(imgCount / 2);

        // merge layer titles with legend images
        for (int i = 0; i < imgCount; i = i + 2) {
            BufferedImage lbl = (BufferedImage) imageStack.get(i);
            BufferedImage img = (BufferedImage) imageStack.get(i + 1);
            img =
                    joinBufferedImageVertically(
                            lbl,
                            img,
                            mergeOptions.getLabelFont(),
                            mergeOptions.isAntialias(),
                            mergeOptions.isTransparent(),
                            mergeOptions.getBackgroundColor());
            nodes.add(img);
        }

        // Sets legend nodes into a matrix according to layout rules
        LegendLayout layout = mergeOptions.getLayout();
        BufferedImage finalLegend = null;
        if (layout == LegendLayout.HORIZONTAL) {
            Row[] rows = createRows(nodes, 0, 0);
            finalLegend = buildFinalHLegend(rows, mergeOptions);
        }

        if (layout == LegendLayout.VERTICAL) {
            Column[] columns = createColumns(nodes, 0, 0, null, false);
            finalLegend = buildFinalVLegend(columns, mergeOptions);
        }

        return finalLegend;
    }

    /** Represents a column of legends images */
    private static class Column {
        private int width;

        private int height;

        private List<BufferedImage> nodes = new ArrayList<BufferedImage>();

        public void addNode(BufferedImage img) {
            nodes.add(img);
            width = Math.max(width, img.getWidth());
            height = height + img.getHeight();
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public List<BufferedImage> getNodes() {
            return nodes;
        }
    }

    /** Represents a row of legends images */
    private static class Row {
        private int width;

        private int height;

        private List<BufferedImage> nodes = new ArrayList<BufferedImage>();

        public void addNode(BufferedImage img) {
            nodes.add(img);
            height = Math.max(height, img.getHeight());
            width = width + img.getWidth();
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public List<BufferedImage> getNodes() {
            return nodes;
        }
    }

    private static Column[] createColumns(List<BufferedImage> nodes,int maxHeight,int maxColumns,LegendGraphicEntity req,boolean checkColor) {
        Column[] legendMatrix = new Column[0];
        /*
         * Limit max height
         */
        if (maxHeight > 0) {
            /*
             * Limit max column
             */
            int cnLimit = maxColumns > 0 ? maxColumns : nodes.size();
            legendMatrix = new Column[cnLimit];
            legendMatrix[0] = new Column();
            int cn = 0;
            int columnHeight = 0;
            for (int i = 0; i < nodes.size(); i++) {
                BufferedImage node = nodes.get(i);
                if (columnHeight <= maxHeight) {
                    // Fill current column
                    legendMatrix[cn].addNode(node);
                    columnHeight = columnHeight + node.getHeight();
                } else {
                    // Add current node to next column
                    i--;
                    cn++;
                    // Stop if column limits is reached
                    if (cn == cnLimit) {
                        break;
                    }
                    // Reset column counter
                    columnHeight = 0;
                    // Create new column
                    legendMatrix[cn] = new Column();
                }
            }
        } else {
            /*
             * Limit max column, if no limit set it to 1
             */
            int colNumber = maxColumns > 0 ? maxColumns : 1;
            legendMatrix = new Column[colNumber];
            legendMatrix[0] = new Column();
            int rowNumber = (int) Math.ceil((float) nodes.size() / colNumber);
            int cn = 0;
            int rc = 0;
            boolean colourPresent = false;
            for (int i = 0; i < nodes.size(); i++) {
                if (rc < rowNumber) {
                    if (checkColor) {
                        // check for presence of colour (ie. non-empty legend row)
                        colourPresent = checkColor(nodes.get(i), req);
                        if (colourPresent) {
                            legendMatrix[cn].addNode(nodes.get(i));
                            rc++;
                        }
                    } else {
                        legendMatrix[cn].addNode(nodes.get(i));
                        rc++;
                    }
                } else {
                    i--;
                    cn++;
                    rc = 0;
                    legendMatrix[cn] = new Column();
                }
            }
        }

        return legendMatrix;
    }

    public static boolean checkColor(BufferedImage img, LegendGraphicEntity req) {
        int w = img.getWidth();
        int h = img.getHeight();
        boolean colourPresent = false;
        for (int j = 0; j < w; j++) {
            for (int k = 0; k < h; k++) {
                if (img.getRGB(j, k) != StyleUtils.getBackgroundColor(req).getRGB()) {
                    colourPresent = true;
                }
            }
        }
        return colourPresent;
    }

    private static Row[] createRows(List<BufferedImage> nodes, int maxWidth, int maxRows) {
        Row[] legendMatrix = new Row[0];
        /*
         * Limit max height
         */
        if (maxWidth > 0) {
            /*
             * Limit max column
             */
            int rnLimit = maxRows > 0 ? maxRows : nodes.size();
            legendMatrix = new Row[rnLimit];
            legendMatrix[0] = new Row();
            int rn = 0;
            int rowWidth = 0;
            for (int i = 0; i < nodes.size(); i++) {
                BufferedImage node = nodes.get(i);
                if (rowWidth <= maxWidth) {
                    // Fill current column
                    legendMatrix[rn].addNode(node);
                    rowWidth = rowWidth + node.getWidth();
                } else {
                    // Add current node to next column
                    i--;
                    rn++;
                    // Stop if column limits is reached
                    if (rn == rnLimit) {
                        break;
                    }
                    // Reset column counter
                    rowWidth = 0;
                    // Create new column
                    legendMatrix[rn] = new Row();
                }
            }
        } else {
            /*
             * Limit max column, if no limit set it to 1
             */
            int rowNumber = maxRows > 0 ? maxRows : 1;
            legendMatrix = new Row[rowNumber];
            legendMatrix[0] = new Row();
            int colNumber = (int) Math.ceil((float) nodes.size() / rowNumber);
            int rn = 0;
            int cc = 0;
            for (int i = 0; i < nodes.size(); i++) {
                if (cc < colNumber) {
                    legendMatrix[rn].addNode(nodes.get(i));
                    cc++;
                } else {
                    i--;
                    rn++;
                    cc = 0;
                    legendMatrix[rn] = new Row();
                }
            }
        }

        return legendMatrix;
    }

    private static BufferedImage buildFinalVLegend(Column[] columns, MergeOptions options) {

        int totalWidth = 0;
        int totalHeight = 0;

        for (Column c : columns) {
            if (c != null) {
                if (totalWidth > 0) {
                    totalWidth = totalWidth + options.getDx();
                }
                totalWidth = totalWidth + c.getWidth();
                int h = c.getHeight() + (c.nodes.size() - 1) * options.getDy();
                totalHeight = Math.max(totalHeight, h);
            }
        }
        totalWidth = totalWidth + options.getMargin() * 2;
        totalHeight = totalHeight + options.getMargin() * 2;
        // buffer the width a bit
        totalWidth += 2;
        final BufferedImage finalLegend =
        		StyleUtils.createImage(
                        totalWidth, totalHeight, (IndexColorModel) null, options.isTransparent());
        final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
        Graphics2D finalGraphics =
        		StyleUtils.prepareTransparency(
                        options.isTransparent(),
                        options.getBackgroundColor(),
                        finalLegend,
                        hintsMap);
        // finalGraphics.setFont(labelFont);
        if (options.isAntialias()) {
            finalGraphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            finalGraphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }

        int vOffset = options.getMargin();
        int hOffset = options.getMargin();
        for (Column c : columns) {
            if (c != null) {
                for (BufferedImage n : c.getNodes()) {
                    finalGraphics.drawImage(n, hOffset, vOffset, null);
                    vOffset = vOffset + n.getHeight() + options.getDy();
                }
                hOffset = hOffset + c.getWidth() + options.getDx();
                vOffset = options.getMargin();
            }
        }

        finalGraphics.dispose();

        return finalLegend;
    }

    private static BufferedImage buildFinalHLegend(Row[] rows, MergeOptions options) {

        int totalWidth = 0;
        int totalHeight = 0;

        for (Row r : rows) {
            if (r != null) {
                if (totalHeight > 0) {
                    totalHeight = totalHeight + options.getDy();
                }
                totalHeight = totalHeight + r.getHeight();
                int w = r.getWidth() + (r.nodes.size() - 1) * options.getDx();
                totalWidth = Math.max(totalWidth, w);
            }
        }
        totalWidth = totalWidth + options.getMargin() * 2;
        totalHeight = totalHeight + options.getMargin() * 2;
        // buffer the width a bit
        totalWidth += 2;
        final BufferedImage finalLegend =
        		StyleUtils.createImage(
                        totalWidth, totalHeight, (IndexColorModel) null, options.isTransparent());
        final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
        Graphics2D finalGraphics =
        		StyleUtils.prepareTransparency(
                        options.isTransparent(),
                        options.getBackgroundColor(),
                        finalLegend,
                        hintsMap);
        // finalGraphics.setFont(labelFont);
        if (options.isAntialias()) {
            finalGraphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            finalGraphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }

        int vOffset = options.getMargin();
        int hOffset = options.getMargin();
        for (Row r : rows) {
            if (r != null) {
                for (BufferedImage n : r.getNodes()) {
                    finalGraphics.drawImage(n, hOffset, vOffset, null);
                    hOffset = hOffset + n.getWidth() + options.getDx();
                }
                vOffset = vOffset + r.getHeight() + options.getDy();
                hOffset = options.getMargin();
            }
        }

        finalGraphics.dispose();

        return finalLegend;
    }

    private static BufferedImage joinBufferedImageHorizzontally(
            BufferedImage img,
            BufferedImage label,
            Font labelFont,
            boolean useAA,
            boolean transparent,
            Color backgroundColor,
            int labelXOffset) {
        // do some calculate first
        int wid = img.getWidth() + label.getWidth() + labelXOffset;
        int height = Math.max(img.getHeight(), label.getHeight());
        // create a new buffer and draw two image into the new image
        BufferedImage newImage =
        		StyleUtils.createImage(wid, height, (IndexColorModel) null, transparent);
        final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
        Graphics2D g2 =
        		StyleUtils.prepareTransparency(transparent, backgroundColor, newImage, hintsMap);
        g2.setFont(labelFont);
        if (useAA) {
            g2.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            g2.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        // move the images to the vertical center of the row
        int imgOffset = (int) Math.round((height - img.getHeight()) / 2d);
        int labelYOffset = (int) Math.round((height - label.getHeight()) / 2d);
        g2.drawImage(img, null, 0, imgOffset);
        g2.drawImage(label, null, img.getWidth() + labelXOffset, labelYOffset);
        g2.dispose();
        return newImage;
    }

    private static BufferedImage joinBufferedImageVertically(
            BufferedImage label,
            BufferedImage img,
            Font labelFont,
            boolean useAA,
            boolean transparent,
            Color backgroundColor) {
        // do some calculate first
        int offset = 0;
        int height = img.getHeight() + label.getHeight() + offset;
        int wid = Math.max(img.getWidth(), label.getWidth()) + offset;
        // create a new buffer and draw two image into the new image
        BufferedImage newImage =
        		StyleUtils.createImage(wid, height, (IndexColorModel) null, transparent);
        final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
        Graphics2D g2 =
        		StyleUtils.prepareTransparency(transparent, backgroundColor, newImage, hintsMap);
        g2.setFont(labelFont);
        if (useAA) {
            g2.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            g2.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        // move the images to the vertical center of the row
        g2.drawImage(label, null, 0, 0);
        g2.drawImage(img, null, 0, label.getHeight());
        g2.dispose();
        return newImage;
    }

    private static BufferedImage renderLabel(
            RenderedImage img, Rule rule, LegendGraphicEntity req, MergeOptions options) {
        BufferedImage labelImg = null;
        if (rule != null) {
            String label = StyleUtils.getRuleLabel(rule, req);
            if (label != null && label.length() > 0) {
                final BufferedImage renderedLabel =
                        getRenderedLabel((BufferedImage) img, label, req);
                labelImg = renderedLabel;
            }
        }
        return labelImg;
    }

    protected static BufferedImage getRenderedLabel(
            BufferedImage image, String label, LegendGraphicEntity req) {
        final Graphics2D graphics = image.createGraphics();
        Font labelFont = StyleUtils.getLabelFont(req);
        boolean useAA = StyleUtils.isFontAntiAliasing(req);

        graphics.setFont(labelFont);
        if (useAA) {
            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        return StyleUtils.renderLabel(label, graphics, req);
    }
}
