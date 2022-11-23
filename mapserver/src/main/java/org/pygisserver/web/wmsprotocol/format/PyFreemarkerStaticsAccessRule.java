package org.pygisserver.web.wmsprotocol.format;

import org.apache.log4j.Logger;

import javax.lang.model.SourceVersion;
import java.util.*;

class PyFreemarkerStaticsAccessRule {

    public static final class RuleItem {
        /** the class to allow access to */
        private Class<?> clazz;
        /** the alias (variable name) used to expose the statics of the class */
        private String alias;
        /**
         * true, in case an number prefix was appended to keep the names distinct. Unlikely to occur
         * in real world
         */
        private boolean numberedAlias;

        public RuleItem(Class<?> clazz, String alias, boolean numberedAlias) {
            super();
            this.clazz = clazz;
            this.alias = alias;
            this.numberedAlias = numberedAlias;
        }

        public String getClassName() {
            return clazz.getName();
        }

        public String getAlias() {
            return alias;
        }

        public boolean isNumberedAlias() {
            return numberedAlias;
        }
    }

    /** Instance signals unrestricted access */
    private static final PyFreemarkerStaticsAccessRule UNRESTRICTED =
            new PyFreemarkerStaticsAccessRule(true);

    /** Instance signals access is disabled */
    private static final PyFreemarkerStaticsAccessRule DISABLED =
            new PyFreemarkerStaticsAccessRule(false);

    public static PyFreemarkerStaticsAccessRule fromPattern(String aPattern) {
        if (aPattern == null || aPattern.trim().isEmpty()) {
            return DISABLED;
        } else if ("*".equals(aPattern)) {
            return UNRESTRICTED;
        }

        // check for validity
        List<Class<?>> tmpClasses = new ArrayList<>();
        for (StringTokenizer tmpTokenizer = new StringTokenizer(aPattern, ",");
             tmpTokenizer.hasMoreTokens(); ) {
            String tmpCandidate = tmpTokenizer.nextToken().trim();
            if (SourceVersion.isName(tmpCandidate)) {
                try {
                    tmpClasses.add(Class.forName(tmpCandidate));
                } catch (ClassNotFoundException e) {
                    logger.warn(
                            "Denying access to static members of '"
                                    + tmpCandidate
                                    + "': Class not found.");
                }
            } else {
                logger.warn(
                        "Denying access to static members of '"
                                + tmpCandidate
                                + "': Not a valid class name.");
            }
        }
        if (tmpClasses.isEmpty()) {
            return DISABLED;
        }

        // determine alias names (=variable names for template) and detect duplicates
        List<RuleItem> tmpItems = new ArrayList<>();
        Map<String, Integer> tmpCounts = new HashMap<>();
        for (Class<?> tmpTarget : tmpClasses) {
            String tmpSimpleAlias = tmpTarget.getSimpleName();
            Integer tmpCount = tmpCounts.get(tmpSimpleAlias);

            if (tmpCount == null) {
                tmpCount = 1;
                tmpItems.add(new RuleItem(tmpTarget, tmpTarget.getSimpleName(), false));
            } else {
                tmpCount += 1;
                tmpItems.add(new RuleItem(tmpTarget, tmpTarget.getSimpleName() + tmpCount, true));
            }
            tmpCounts.put(tmpSimpleAlias, tmpCount);
        }
        return new PyFreemarkerStaticsAccessRule(tmpItems);
    }

    private static Logger logger = Logger.getLogger(PyFreemarkerStaticsAccessRule.class);

    /** "true" for rules representing unrestricted access */
    private boolean unrestricted;

    /** Classes allowed to be accessed */
    private List<RuleItem> rulesItems = Collections.emptyList();

    public PyFreemarkerStaticsAccessRule(boolean anUnRestricted) {
        unrestricted = anUnRestricted;
    }

    public PyFreemarkerStaticsAccessRule(List<RuleItem> someItems) {
        this(false);
        rulesItems = someItems;
    }

    /** @return the classes allowed to be accessed */
    public List<RuleItem> getAllowedItems() {
        return rulesItems;
    }

    /** @return true, if unrestricted access to static members is allowed. */
    public boolean isUnrestricted() {
        return unrestricted;
    }

    @Override
    public String toString() {
        return "FreemarkerStaticsAccessRule [unrestricted="
                + unrestricted
                + ", rulesItems="
                + rulesItems
                + "]";
    }
}
