package pl.edu.icm.synat.sdk.client.tags.remover;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.model.bwmeta.y.YElement;
import pl.edu.icm.model.transformers.bwmeta.y.BwmetaTransformerConstants;
import pl.edu.icm.synat.api.services.SynatServiceRef;
import pl.edu.icm.synat.api.services.store.StatelessStore;
import pl.edu.icm.synat.api.services.store.StoreClient;
import pl.edu.icm.synat.api.services.store.model.Record;
import pl.edu.icm.synat.api.services.store.model.RecordConditions;
import pl.edu.icm.synat.api.services.store.model.RecordId;
import pl.edu.icm.synat.api.services.store.model.batch.BatchBuilder;
import pl.edu.icm.synat.api.services.store.model.batch.impl.DefaultStoreClient;
import pl.edu.icm.synat.application.model.bwmeta.utils.BWMetaDeserializerImpl;
import pl.edu.icm.synat.application.model.bwmeta.utils.BwmetaConverterUtils;
import pl.edu.icm.synat.common.ListingResult;
import pl.edu.icm.synat.logic.model.utils.RecordBwmetaExtractor;
import pl.edu.icm.synat.logic.model.utils.impl.RecordBwmetaExtractorImpl;
import pl.edu.icm.synat.logic.services.licensing.titlegroups.constants.TitlegroupLicenseConstants;
import pl.edu.icm.synat.logic.services.repository.constants.RepositoryStoreConstants;

/**
 * Move on tags and remove them example.
 * TODO finish me ;)
*
* @author Gra <Gołębiewski Radosław A.> google.com/+RadoslawGolebiewski
*
*/
@Component
public class StoreClientTagsRemover {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreClientTagsRemover.class);

    private static final String SAVE_FILE_NAME_SUFFIX = ".save";
    private static final String SAVE_FILE_NAME_PREFIX = "storeclienttagsremover";
    private static final String CONTEXT_PATH = "store-client-tags-remover.xml";
    private static final String STORE_ID = "Store";
    private static final String SEARCHED_TAG_STRING = RepositoryStoreConstants.TAG_PREFIX_LICENSING_POLICY
            +TitlegroupLicenseConstants.TITLEGRUPS_FILE;
    private static final int STORE_LIST_RECORDS_LIMIT = 30;

    @SynatServiceRef(serviceId = STORE_ID)
    private StatelessStore store;

    private Long withTag = new Long(0);
    private Long withoutTag = new Long(0);
    private ListingResult<RecordId> storeResults = null;
    private RecordConditions conditions = null;
    private String nextToken = null;
    private RecordBwmetaExtractor recordBwmetaExtractor = null;

    public static void main(final String[] args) throws IOException {
        final ClassPathXmlApplicationContext applicationContext
                = new ClassPathXmlApplicationContext(CONTEXT_PATH);
        final StoreClientTagsRemover client = applicationContext.getBean(StoreClientTagsRemover.class);

        client.loadNextToken();
        try {
            client.removeTags();
        } catch (Exception e) {
            LOGGER.error("Error:", e);
        } finally {
            client.saveNextToken();
        }
        applicationContext.close();
    }

    public void loadNextToken() throws IOException {
        final File file = new File(SAVE_FILE_NAME_PREFIX+SAVE_FILE_NAME_SUFFIX);
        if (null != file && file.exists()) {
            nextToken = FileUtils.readFileToString(file);
            FileUtils.deleteQuietly(file);
        }
    }

    public void saveNextToken() throws IOException {
        if (null != storeResults && 0 != storeResults.getTotalCount() && null != nextToken) {
            final File save = new File(SAVE_FILE_NAME_PREFIX+SAVE_FILE_NAME_SUFFIX);
            save.createNewFile();
            FileUtils.write(save, nextToken);
        }
    }

    private void removeTags() {

        recordBwmetaExtractor = new RecordBwmetaExtractorImpl();
        ((RecordBwmetaExtractorImpl)recordBwmetaExtractor).setBwmetaDeserializer(new BWMetaDeserializerImpl());
        
        
        final Date timestampFrom = new Date(0);
        final Date timestampTo = new Date(System.currentTimeMillis());
        conditions = new RecordConditions().emptyCondition()
                .withTimestampFrom(timestampFrom).withTimestampTo(timestampTo);

        storeResults = store.listRecords(conditions, nextToken, STORE_LIST_RECORDS_LIMIT);

        do {
            LOGGER.info("Total count listed records in one fetch: "+storeResults.getTotalCount()
                    +", with changed tags: "+withTag+" and without changed tags: "+withoutTag+".");
        } while (0 != onRecordsRemoveTag());
    }

    private long onRecordsRemoveTag() {
        final StoreClient storeClient = new DefaultStoreClient(store);
        final BatchBuilder batchBuilder = storeClient.createBatchBuilder();

        return removeTagsOnItems(batchBuilder);
    }

    private long removeTagsOnItems(final BatchBuilder batchBuilder) {
        for (final RecordId recordId : storeResults.getItems()) {
            removeTags(batchBuilder, fetchRecord(recordId), recordId);
        }
        nextToken = storeResults.getNextToken();
        storeResults = store.listRecords(conditions, nextToken, STORE_LIST_RECORDS_LIMIT);
        return storeResults.getTotalCount();
    }

    private void removeTags(final BatchBuilder batchBuilder, final Record record, final RecordId recordId) {
        if (null != record && ! record.isDeleted()) {

            final YElement yElement = recordBwmetaExtractor.extractElement(record);
            if (null != yElement) {
                String bwmeta = BwmetaConverterUtils.YElementToBwmeta(yElement, BwmetaTransformerConstants.BWMETA_2_1);
                LOGGER.info("bwmeta: {}", bwmeta);
            }
            batchBuilder.onRecord(recordId).removeTags(SEARCHED_TAG_STRING);
            batchBuilder.execute();
            LOGGER.info("Changed tags in: "+record.getIdentifier());
            withTag++;
        }
    }

    private Record fetchRecord(final RecordId recordId) {
        final Record record = store.fetchRecord(recordId, (String[]) null);
        if (null != record) {
            if (record.getTags().contains(SEARCHED_TAG_STRING)) {
                return record;
            }
            ++withoutTag;
        }
        return null;
    }

    public String getNextToken() {
        return nextToken;
    }

    public void setNextToken(final String nextToken) {
        this.nextToken = nextToken;
    }
}
