package pl.edu.icm.synat.sdk.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.model.bwmeta.y.YCategoryRef;
import pl.edu.icm.model.bwmeta.y.YElement;
import pl.edu.icm.synat.api.services.SynatServiceRef;
import pl.edu.icm.synat.api.services.store.StatelessStore;
import pl.edu.icm.synat.api.services.store.model.Record;
import pl.edu.icm.synat.api.services.store.model.RecordId;
import pl.edu.icm.synat.logic.model.utils.RecordBwmetaExtractor;
import pl.edu.icm.synat.logic.model.utils.impl.RecordBwmetaExtractorImpl;
import pl.edu.icm.synat.process.common.enrich.MetadataTransformer;
import pl.edu.icm.synat.process.common.enrich.impl.sonca.SoncaMetadataWithChildrenTransformer;

/**
 * {@code MetadataWithChildrenTransformer} example.
*
* @author Gra <Gołębiewski Radosław A.> google.com/+RadoslawGolebiewski
*
*/
@Component
public class MetadataWithChildrenTransformerExample {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataWithChildrenTransformerExample.class);

    @SynatServiceRef(serviceId = "Store")
    private StatelessStore store;

    public void transform(final RecordBwmetaExtractor recordBwmetaExtractor, final MetadataTransformer transformer, final String...ids) {
        for (final String id : ids) {
            final Record record = store.fetchRecord(new RecordId(id));
            final YElement yElement = recordBwmetaExtractor.extractElement(record);
            if (null != yElement && transformer.modify(yElement)) {
                for (YCategoryRef yCategoryRef : yElement.getCategoryRefs()) {
                    LOGGER.info("id: {}, code: {}, classification: {}"
                            , new Object[] {yElement.getId(), yCategoryRef.getCode(), yCategoryRef.getClassification()});
                }
            }
        }
    }

    public static void main(final String[] args) {
        final ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("transform.xml");

        final MetadataWithChildrenTransformerExample client = applicationContext.getBean(MetadataWithChildrenTransformerExample.class);
        final RecordBwmetaExtractor recordBwmetaExtractor = applicationContext.getBean(RecordBwmetaExtractorImpl.class);
        final MetadataTransformer transformer = applicationContext.getBean(SoncaMetadataWithChildrenTransformer.class);

        final String[] bwmetaIds = new String[] {
                  "bwmeta1.element.bwnjournal-journal-sm"
                  , "bwmeta1.element.agro-journal-72fbb33f-ddd8-460a-8350-ce9f3088323f"
                  , "bwmeta1.element.agro-journal-dac3afb8-e0d2-411c-8d3a-9d0421dc5f26"
        };

        client.transform(recordBwmetaExtractor, transformer, bwmetaIds);

        applicationContext.close();
    }
}
