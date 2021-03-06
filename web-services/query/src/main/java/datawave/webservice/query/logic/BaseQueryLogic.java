package datawave.webservice.query.logic;

import datawave.audit.SelectorExtractor;
import datawave.marking.MarkingFunctions;
import datawave.webservice.common.audit.Auditor.AuditType;
import datawave.webservice.query.Query;
import datawave.webservice.query.configuration.GenericQueryConfiguration;
import datawave.webservice.query.configuration.QueryData;
import datawave.webservice.query.iterator.DatawaveTransformIterator;
import datawave.webservice.query.result.event.ResponseObjectFactory;
import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.springframework.beans.factory.annotation.Required;

import javax.inject.Inject;
import java.security.Principal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseQueryLogic<T> implements QueryLogic<T> {
    
    protected GenericQueryConfiguration baseConfig = new GenericQueryConfiguration() {
        @Override
        public Iterator<QueryData> getQueries() {
            return super.getQueries();
        }
    };
    
    private String logicName = "No logicName was set";
    private String logicDescription = "Not configured";
    private AuditType auditType = null;
    protected long maxResults;
    protected ScannerBase scanner;
    @SuppressWarnings("unchecked")
    protected Iterator<T> iterator = (Iterator<T>) Collections.emptyList().iterator();
    private int maxPageSize = 0;
    private long pageByteTrigger = 0;
    private boolean collectQueryMetrics = true;
    private String _connPoolName;
    protected Principal principal;
    protected RoleManager roleManager;
    protected MarkingFunctions markingFunctions;
    @Inject
    protected ResponseObjectFactory responseObjectFactory;
    protected SelectorExtractor selectorExtractor;
    
    public static final String BYPASS_ACCUMULO = "rfile.debug";
    
    public BaseQueryLogic() {
        this.baseConfig.setBaseIteratorPriority(100);
    }
    
    public BaseQueryLogic(BaseQueryLogic<T> other) {
        // Generic Query Config variables
        setTableName(other.getTableName());
        setMaxRowsToScan(other.getMaxRowsToScan());
        setMaxResults(other.getMaxResults());
        setUndisplayedVisibilities(other.getUndisplayedVisibilities());
        setBaseIteratorPriority(other.getBaseIteratorPriority());
        setBypassAccumulo(other.getBypassAccumulo());
        
        // Other variables
        setMarkingFunctions(other.getMarkingFunctions());
        setResponseObjectFactory(other.getResponseObjectFactory());
        setLogicName(other.getLogicName());
        setLogicDescription(other.getLogicDescription());
        setAuditType(other.getAuditType(null));
        this.scanner = other.scanner;
        this.iterator = other.iterator;
        setMaxPageSize(other.getMaxPageSize());
        setPageByteTrigger(other.getPageByteTrigger());
        setCollectQueryMetrics(other.getCollectQueryMetrics());
        setConnPoolName(other.getConnPoolName());
        setPrincipal(other.getPrincipal());
        setRoleManager(other.getRoleManager());
        setMarkingFunctions(other.getMarkingFunctions());
        setResponseObjectFactory(other.getResponseObjectFactory());
        setSelectorExtractor(other.getSelectorExtractor());
    }
    
    public MarkingFunctions getMarkingFunctions() {
        return markingFunctions;
    }
    
    public void setMarkingFunctions(MarkingFunctions markingFunctions) {
        this.markingFunctions = markingFunctions;
    }
    
    public ResponseObjectFactory getResponseObjectFactory() {
        return responseObjectFactory;
    }
    
    public void setResponseObjectFactory(ResponseObjectFactory responseObjectFactory) {
        this.responseObjectFactory = responseObjectFactory;
    }
    
    public Principal getPrincipal() {
        return principal;
    }
    
    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }
    
    @Override
    public String getTableName() {
        return this.baseConfig.getTableName();
    }
    
    @Override
    public long getMaxResults() {
        return this.maxResults;
    }
    
    @Override
    public long getMaxRowsToScan() {
        return this.baseConfig.getMaxRowsToScan();
    }
    
    @Override
    public void setTableName(String tableName) {
        this.baseConfig.setTableName(tableName);
    }
    
    @Override
    public void setMaxResults(long maxResults) {
        this.maxResults = maxResults;
    }
    
    @Override
    public void setMaxRowsToScan(long maxRowsToScan) {
        this.baseConfig.setMaxRowsToScan(maxRowsToScan);
    }
    
    @Override
    public int getMaxPageSize() {
        return maxPageSize;
    }
    
    @Override
    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
    
    @Override
    public long getPageByteTrigger() {
        return pageByteTrigger;
    }
    
    @Override
    public void setPageByteTrigger(long pageByteTrigger) {
        this.pageByteTrigger = pageByteTrigger;
    }
    
    @Override
    public int getBaseIteratorPriority() {
        return this.baseConfig.getBaseIteratorPriority();
    }
    
    @Override
    public void setBaseIteratorPriority(final int baseIteratorPriority) {
        this.baseConfig.setBaseIteratorPriority(baseIteratorPriority);
    }
    
    @Override
    public Iterator<T> iterator() {
        return iterator;
    }
    
    @Override
    public TransformIterator getTransformIterator(Query settings) {
        return new DatawaveTransformIterator(this.iterator(), this.getTransformer(settings));
    }
    
    @Override
    public String getLogicName() {
        return logicName;
    }
    
    @Override
    public void setLogicName(String logicName) {
        this.logicName = logicName;
    }
    
    public boolean getBypassAccumulo() {
        return this.baseConfig.getBypassAccumulo();
    }
    
    public void setBypassAccumulo(boolean bypassAccumulo) {
        this.baseConfig.setBypassAccumulo(bypassAccumulo);
    }
    
    @Override
    public Set<String> getUndisplayedVisibilities() {
        return baseConfig.getUndisplayedVisibilities();
    }
    
    public void setUndisplayedVisibilities(Set<String> undisplayedVisibilities) {
        this.baseConfig.setUndisplayedVisibilities(undisplayedVisibilities);
    }
    
    @Override
    public abstract Object clone() throws CloneNotSupportedException;
    
    public void close() {
        if (null == scanner)
            return;
        if (scanner instanceof BatchScanner) {
            scanner.close();
        }
    }
    
    /*
     * Implementations must override if they want a query-specific value returned
     */
    @Override
    public AuditType getAuditType(Query query) {
        return auditType;
    }
    
    @Override
    public AuditType getAuditType() {
        return auditType;
    }
    
    @Override
    @Required
    // enforces that the unit tests will fail and the application will not deploy unless this property is set
    public void setAuditType(AuditType auditType) {
        this.auditType = auditType;
    }
    
    @Override
    public void setLogicDescription(String logicDescription) {
        this.logicDescription = logicDescription;
    }
    
    @Override
    public String getLogicDescription() {
        return logicDescription;
    }
    
    public boolean getCollectQueryMetrics() {
        return collectQueryMetrics;
    }
    
    public void setCollectQueryMetrics(boolean collectQueryMetrics) {
        this.collectQueryMetrics = collectQueryMetrics;
    }
    
    public RoleManager getRoleManager() {
        return roleManager;
    }
    
    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }
    
    /** {@inheritDoc} */
    @Override
    public String getConnPoolName() {
        return _connPoolName;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setConnPoolName(final String connPoolName) {
        _connPoolName = connPoolName;
    }
    
    public boolean canRunQuery() {
        return this.canRunQuery(this.getPrincipal());
    }
    
    /** {@inheritDoc} */
    public boolean canRunQuery(Principal principal) {
        return this.roleManager == null || this.roleManager.canRunQuery(this, principal);
    }
    
    @Override
    public final void validate(Map<String,List<String>> parameters) throws IllegalArgumentException {
        Set<String> requiredParams = getRequiredQueryParameters();
        for (String required : requiredParams) {
            List<String> values = parameters.get(required);
            if (null == values) {
                throw new IllegalArgumentException("Required parameter " + required + " not found");
            }
        }
    }
    
    @Override
    public List<String> getSelectors(Query settings) throws IllegalArgumentException {
        List<String> selectorList = null;
        if (this.selectorExtractor != null) {
            try {
                selectorList = this.selectorExtractor.extractSelectors(settings);
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        return selectorList;
    }
    
    public void setSelectorExtractor(SelectorExtractor selectorExtractor) {
        this.selectorExtractor = selectorExtractor;
    }
    
    public SelectorExtractor getSelectorExtractor() {
        return selectorExtractor;
    }
}
